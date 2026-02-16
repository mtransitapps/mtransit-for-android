package org.mtransit.android.dev

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.location.Location
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.ITargetedProviderProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesCache
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.removeAllAnd
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoModeManager @Inject constructor(
    @ApplicationContext appContext: Context,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DemoModeManager::class.java.simpleName

        private const val FILTER_AGENCY_AUTHORITY = "filter_agency_authority"
        private const val FILTER_SCREEN = "filter_screen"
        private const val FORCE_LANG = "force_lang"
        // overriding current time inside main app doesn't load static/real-time schedule for overridden time
        private const val FORCE_TIMESTAMP_SEC = "force_timestamp_sec"
        private const val FORCE_TIMEZONE = "force_tz"
        private const val FORCE_TIME_FORMAT = "force_time"

        private const val FORCE_TIME_FORMAT_12 = "12"
        private const val FORCE_TIME_FORMAT_24 = "24"

        const val FILTER_SCREEN_HOME = "home"
        const val FILTER_SCREEN_POI = "poi"
        const val FILTER_SCREEN_BROWSE = "browse"

        const val MIN_POI_HOME_SCREEN = 7
    }

    override fun getLogTag(): String = LOG_TAG

    private val allowedTargeted = listOf(
        appContext.getString(R.string.favorite_authority),
        appContext.getString(R.string.module_authority),
        appContext.getString(R.string.place_authority),
    )

    var filterAgencyAuthority: String? = null
    private var filterAgency: AgencyProperties? = null
    fun isFilteringAgency() = !filterAgencyAuthority.isNullOrBlank()

    private val filterAgencyType: DataSourceType?
        get() = this.filterAgency?.getSupportedType()

    val filterAgencyTypeId: Int?
        get() = this.filterAgencyType?.id
    private var filterAgencyLocation: Location? = null

    private var filterAgencyPOIM: POIManager? = null

    private suspend fun findNearbyPOIM(
        lat: Double,
        lng: Double,
        agency: AgencyProperties,
    ): POIManager? {
        val ad = LocationUtils.getNewDefaultAroundDiff()
        var poim: POIManager?
        while (true) {
            val filter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, ad.aroundDiff).apply {
            }
            poim = this.dataSourceRequestManager.findPOIMs(agency.authority, filter)
                .removeAllAnd {
                    if (FeatureFlags.F_USE_ROUTE_TYPE_FILTER) {
                        (it.poi as? RouteDirectionStop)?.route?.type in GTFSCommons.ROUTE_TYPES_REQUIRES_BOOKING
                    } else false
                }
                .updateDistanceM(lat, lng)
                .removeTooFar(LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, ad.aroundDiff))
                .firstOrNull()
            if (poim != null) {
                break
            } else if (LocationUtils.searchComplete(lat, lng, ad.aroundDiff)) {
                break
            } else {
                LocationUtils.incAroundDiff(ad)
                continue
            }
        }
        return poim
    }

    val filterTypeId: Int?
        get() = this.filterAgencyTypeId

    val filterLocation: Location?
        get() = this.filterAgencyLocation

    fun isFilterLocation() = isFilteringAgency()

    private var filterScreen: String? = null

    val filterUUID: String?
        get() = filterAgencyPOIM?.poi?.uuid
    private var forceLang: String? = null
    fun isForceLang() = !forceLang.isNullOrBlank()

    private var forceTimestampSec: String? = null
    private val forceTimestampMs: Long? get() = forceTimestampSec?.toLongOrNull()?.let { it * 1000L }

    private var forceTimeZone: String? = null
    private val forceTZ: TimeZone get() = forceTimeZone
        ?.takeIf { it.isNotEmpty() && TimeZone.getAvailableIDs().contains(it) }
        ?.let { TimeZone.getTimeZone(it) }
        ?: TimeZone.getDefault()

    private var forceTimeFormat: String? = null
    private val isForce24HourFormat: Boolean?
        get() = when (forceTimeFormat) {
            FORCE_TIME_FORMAT_12 -> false
            FORCE_TIME_FORMAT_24 -> true
            else -> null
        }

    val enabled: Boolean
        get() = (
                BuildConfig.DEBUG
                        && (
                        !filterAgencyAuthority.isNullOrBlank()
                                || !filterScreen.isNullOrBlank()
                                || !forceLang.isNullOrBlank()
                                || !forceTimestampSec.isNullOrBlank()
                                || !forceTimeZone.isNullOrBlank()
                                || !forceTimeFormat.isNullOrBlank()
                        )
                )
                || isFullDemo()

    fun isFullDemo(): Boolean =
        !filterAgencyAuthority.isNullOrBlank()
                && !filterScreen.isNullOrBlank()
                && !forceLang.isNullOrBlank()
    // not mandatory: forceTimestampSec, forceTimeZone, forceTimeFormat

    suspend fun read(savedStateHandle: SavedStateHandle, dataSourcesCache: DataSourcesCache) {
        filterAgencyAuthority = savedStateHandle[FILTER_AGENCY_AUTHORITY]
        filterScreen = savedStateHandle[FILTER_SCREEN]
        forceLang = savedStateHandle[FORCE_LANG]
        forceTimestampSec = savedStateHandle[FORCE_TIMESTAMP_SEC]
        forceTimeZone = savedStateHandle[FORCE_TIMEZONE]
        forceTimeFormat = savedStateHandle[FORCE_TIME_FORMAT]
        if (enabled) {
            apply(dataSourcesCache)
        }
    }

    private suspend fun apply(dataSourcesCache: DataSourcesCache) = withContext(Dispatchers.IO) {
        filterAgencyAuthority?.let { authority ->
            filterAgency = dataSourcesCache.getAgency(authority)
        }

        filterAgencyPOIM = filterAgency?.let { agency ->
            val lat = agency.area.centerLat + ((agency.area.maxLat - agency.area.minLat) / 4.00)
            val lng = agency.area.centerLng
            findNearbyPOIM(lat, lng, agency)
        }

        filterAgencyLocation = filterAgency?.let { agency ->
            val isHomeScreen = filterScreen == FILTER_SCREEN_HOME
            val lat = agency.area.centerLat - if (!isHomeScreen) ((agency.area.maxLat - agency.area.minLat) / 4.00) else 0.00
            val lng = agency.area.centerLng
            findNearbyPOIM(lat, lng, agency)?.let { poim ->
                LatLngBounds.builder()
                    .include(LatLng(lat, lng))
                    .include(LatLng(poim.lat, poim.lng))
                    .build().center.let {
                        LocationUtils.getNewLocation(it.latitude, it.longitude, 77f)
                    }
            }
            LocationUtils.getNewLocation(lat, lng, 77f)
        }
    }

    fun init() {
        if (!enabled) return
        setTimestamp()
        setTimeZone()
        set24HourFormat()
    }

    fun isEnabledPOIScreen(): Boolean {
        if (!enabled) {
            return false
        }
        if (filterScreen != FILTER_SCREEN_POI) {
            return false
        }
        @Suppress("RedundantIf")
        if (filterAgencyAuthority.isNullOrBlank() || filterUUID.isNullOrBlank()) {
            return false
        }
        return true
    }

    fun isEnabledBrowseScreen(): Boolean {
        if (!enabled) {
            return false
        }
        if (filterScreen != FILTER_SCREEN_BROWSE) {
            return false
        }
        @Suppress("RedundantIf")
        if (filterAgencyTypeId == null) {
            return false
        }
        return true
    }

    fun isAllowedAnyway(agency: IAgencyProperties?) = isAllowedAnyway(agency?.type)
    fun isAllowedAnyway(dst: DataSourceType?) = dst?.isMapScreen != true

    fun isAllowedAnyway(targeted: ITargetedProviderProperties?) = this.allowedTargeted.contains(targeted?.authority)

    fun fixLocale(newBaseContext: Context): Context {
        if (!enabled && forceLang == null) return newBaseContext
        var newBase = newBaseContext
        newBase = newBase.createConfigurationContext(
            fixLocale(newBase.resources.configuration)
        )
        return newBase
    }

    @SuppressLint("AppBundleLocaleChanges")
    private fun fixLocale(newConfiguration: Configuration): Configuration {
        val defaultLocale = forceLang?.let { Locale.forLanguageTag(it) } ?: LocaleUtils.getDefaultLocale()
        LocaleUtils.setDefaultLocale(defaultLocale)
        return LocaleUtils.fixDefaultLocale(newConfiguration)
    }

    private fun setTimestamp() {
        TimeUtils.setOverrideCurrentTimeMillis(this.forceTimestampMs)
    }

    private fun setTimeZone() {
        TimeZone.setDefault(this.forceTZ)
    }

    private fun set24HourFormat() {
        TimeUtils.setOverrideIs24HourFormat(this.isForce24HourFormat)
    }
}

fun <T : IAgencyProperties> List<T>.filterDemoModeAgency(demoModeManager: DemoModeManager): List<T> {
    if (!demoModeManager.isFilteringAgency()) {
        return this
    }
    return filterTo(ArrayList()) { agency ->
        agency.authority == demoModeManager.filterAgencyAuthority || demoModeManager.isAllowedAnyway(agency)
    }
}

fun <T : IAgencyProperties> T?.takeIfDemoModeAgency(demoModeManager: DemoModeManager): T? {
    if (!demoModeManager.isFilteringAgency()) {
        return this
    }
    return if (demoModeManager.filterAgencyAuthority == this?.authority || demoModeManager.isAllowedAnyway(this)) this else null
}

fun List<DataSourceType>.filterDemoModeType(demoModeManager: DemoModeManager): List<DataSourceType> {
    if (!demoModeManager.isFilteringAgency()) {
        return this
    }
    return filterTo(ArrayList()) { type ->
        type.id == demoModeManager.filterAgencyTypeId || demoModeManager.isAllowedAnyway(type)
    }
}

fun <T : ITargetedProviderProperties> List<T>.filterDemoModeTargeted(demoModeManager: DemoModeManager): List<T> {
    if (!demoModeManager.isFilteringAgency()) {
        return this
    }
    return filterTo(ArrayList()) { targeted ->
        targeted.targetAuthority == demoModeManager.filterAgencyAuthority || demoModeManager.isAllowedAnyway(targeted)
    }
}

@Suppress("unused")
fun <T : ITargetedProviderProperties> Set<T>.filterDemoModeTargeted(demoModeManager: DemoModeManager): Set<T> {
    if (!demoModeManager.isFilteringAgency()) {
        return this
    }
    return filterTo(HashSet()) { targeted ->
        targeted.targetAuthority == demoModeManager.filterAgencyAuthority || demoModeManager.isAllowedAnyway(targeted)
    }
}

fun <T : ITargetedProviderProperties> T?.takeIfDemoModeTargeted(demoModeManager: DemoModeManager): T? {
    if (!demoModeManager.isFilteringAgency()) {
        return this
    }
    return if (demoModeManager.filterAgencyAuthority == this?.targetAuthority || demoModeManager.isAllowedAnyway(this)) this else null
}