package org.mtransit.android.dev

import android.content.Context
import android.location.Location
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.ITargetedProviderProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesCache
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoModeManager @Inject constructor(
    @ApplicationContext appContext: Context,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DemoModeManager::class.java.simpleName

        const val FILTER_AGENCY_AUTHORITY = "filter_agency_authority"
        const val FILTER_SCREEN = "filter_screen"
        const val FORCE_LANG = "force_lang"

        const val FILTER_SCREEN_HOME = "home"
        const val FILTER_SCREEN_POI = "poi"
        const val FILTER_SCREEN_BROWSE = "browse"
    }

    override fun getLogTag(): String = LOG_TAG

    private val allowedTargeted = listOf(
        appContext.getString(R.string.favorite_authority),
        appContext.getString(R.string.module_authority),
        appContext.getString(R.string.place_authority),
    )

    var filterAgencyAuthority: String? = null
    var filterAgency: AgencyProperties? = null

    val filterAgencyType: DataSourceType?
        get() = this.filterAgency?.type

    val filterAgencyTypeId: Int?
        get() = this.filterAgencyType?.id
    val filterAgencyLocation: Location?
        get() {
            return this.filterAgency?.let { agency ->
                val lat = agency.area.centerLat - ((agency.area.maxLat - agency.area.minLat) / 4.00)
                val lng = agency.area.centerLng
                findNearbyPOIM(lat, lng, agency)?.let { poim ->
                    LatLngBounds.builder()
                        .include(LatLng(lat, lng))
                        .include(LatLng(poim.lat, poim.lng))
                        .build().center.let {
                            return LocationUtils.getNewLocation(it.latitude, it.longitude, 77f)
                        }
                }
                return LocationUtils.getNewLocation(lat, lng, 77f)
            }
        }

    val filterAgencyPOIM: POIManager?
        get() {
            return this.filterAgency?.let { agency ->
                val lat = agency.area.centerLat + ((agency.area.maxLat - agency.area.minLat) / 4.00)
                val lng = agency.area.centerLng
                findNearbyPOIM(lat, lng, agency)
            }
        }

    private fun findNearbyPOIM(
        lat: Double,
        lng: Double,
        agency: AgencyProperties,
    ): POIManager? {
        val ad = LocationUtils.getNewDefaultAroundDiff()
        var poim: POIManager?
        while (true) {
            val maxDistance = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, ad.aroundDiff)
            val agencyPOIMs = this.dataSourceRequestManager.findPOIMs(
                agency.authority, POIProviderContract.Filter.getNewAroundFilter(lat, lng, ad.aroundDiff)
            )
            agencyPOIMs?.let { agencyPOIs ->
                LocationUtils.updateDistance(agencyPOIs, lat, lng)
                LocationUtils.removeTooFar(agencyPOIs, maxDistance)
            }
            poim = agencyPOIMs?.firstOrNull()
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

    var filterScreen: String? = null

    val filterUUID: String?
        get() = filterAgencyPOIM?.poi?.uuid
    var forceLang: String? = null

    val enabled: Boolean
        get() = filterAgencyAuthority != null && filterScreen != null && forceLang != null

    val notEnabled: Boolean
        get() = !enabled

    suspend fun read(savedStateHandle: SavedStateHandle, dataSourcesCache: DataSourcesCache) {
        filterAgencyAuthority = savedStateHandle.get<String?>(FILTER_AGENCY_AUTHORITY)
        filterScreen = savedStateHandle.get<String?>(FILTER_SCREEN)
        forceLang = savedStateHandle.get<String?>(FORCE_LANG)
        if (enabled) {
            apply(dataSourcesCache)
        }
    }

    private suspend fun apply(dataSourcesCache: DataSourcesCache) = withContext(Dispatchers.IO) {
        filterAgencyAuthority?.let { authority ->
            filterAgency = dataSourcesCache.getAgency(authority)
        }
    }

    fun isEnabledPOIScreen(): Boolean {
        if (notEnabled) {
            return false
        }
        if (filterScreen != FILTER_SCREEN_POI) {
            return false
        }
        if (filterAgencyAuthority.isNullOrBlank() || filterUUID.isNullOrBlank()) {
            return false
        }
        return true
    }

    fun isEnabledBrowseScreen(): Boolean {
        if (notEnabled) {
            return false
        }
        if (filterScreen != FILTER_SCREEN_BROWSE) {
            return false
        }
        if (filterAgencyTypeId == null) {
            return false
        }
        return true
    }

    fun isAllowedAnyway(agency: IAgencyProperties?) = isAllowedAnyway(agency?.type)
    fun isAllowedAnyway(dst: DataSourceType?) = dst?.isMapScreen != true
    fun isAllowedAnyway(targeted: ITargetedProviderProperties?) = this.allowedTargeted.contains(targeted?.authority)

    fun fixLocale(_newBase: Context): Context {
        if (notEnabled && forceLang == null) {
            return _newBase
        }
        var newBase = _newBase
        val defaultLocale = forceLang?.let { Locale.forLanguageTag(it) } ?: LocaleUtils.getDefaultLocale()
        val configuration = newBase.resources.configuration
        configuration.setLocale(defaultLocale)
        newBase = newBase.createConfigurationContext(configuration)
        Locale.setDefault(defaultLocale)
        return newBase
    }
}

fun <T : IAgencyProperties> List<T>.filterDemoModeAgency(demoModeManager: DemoModeManager): List<T> {
    if (demoModeManager.notEnabled) {
        return this
    }
    return filterTo(ArrayList(), { agency -> agency.authority == demoModeManager.filterAgencyAuthority || demoModeManager.isAllowedAnyway(agency) })
}

fun <T : IAgencyProperties> T?.takeIfDemoModeAgency(demoModeManager: DemoModeManager): T? {
    if (demoModeManager.notEnabled) {
        return this
    }
    return if (demoModeManager.filterAgencyAuthority == this?.authority || demoModeManager.isAllowedAnyway(this)) this else null
}

fun List<DataSourceType>.filterDemoModeType(demoModeManager: DemoModeManager): List<DataSourceType> {
    if (demoModeManager.notEnabled) {
        return this
    }
    return filterTo(ArrayList(), { type -> type.id == demoModeManager.filterAgencyTypeId || demoModeManager.isAllowedAnyway(type) })
}

fun <T : ITargetedProviderProperties> List<T>.filterDemoModeTargeted(demoModeManager: DemoModeManager): List<T> {
    if (demoModeManager.notEnabled) {
        return this
    }
    return filterTo(ArrayList(), { targeted -> targeted.targetAuthority == demoModeManager.filterAgencyAuthority || demoModeManager.isAllowedAnyway(targeted) })
}

fun <T : ITargetedProviderProperties> Set<T>.filterDemoModeTargeted(demoModeManager: DemoModeManager): Set<T> {
    if (demoModeManager.notEnabled) {
        return this
    }
    return filterTo(HashSet(), { targeted -> targeted.targetAuthority == demoModeManager.filterAgencyAuthority || demoModeManager.isAllowedAnyway(targeted) })
}

fun <T : ITargetedProviderProperties> T?.takeIfDemoModeTargeted(demoModeManager: DemoModeManager): T? {
    if (demoModeManager.notEnabled) {
        return this
    }
    return if (demoModeManager.filterAgencyAuthority == this?.targetAuthority || demoModeManager.isAllowedAnyway(this)) this else null
}