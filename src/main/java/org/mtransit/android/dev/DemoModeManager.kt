package org.mtransit.android.dev

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.ITargetedProviderProperties
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoModeManager @Inject constructor(
    @ApplicationContext appContext: Context,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DemoModeManager::class.java.simpleName

        const val FILTER_AGENCY_AUTHORITY = "filter_agency_authority"
        const val FILTER_TYPE_ID = "filter_type"
        const val FILTER_LOCATION = "filter_location"
        const val FILTER_SCREEN = "filter_screen"
        const val FILTER_UUID = "filter_uuid"
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
    private var filterType: String? = null
    val filterTypeId: Int?
        get() = filterType?.toInt()
    var filterLocation: String? = null
    var filterScreen: String? = null
    var filterUUID: String? = null
    var forceLang: String? = null

    val enabled: Boolean
        get() = filterAgencyAuthority != null && filterType != null && filterLocation != null && filterScreen != null

    val notEnabled: Boolean
        get() = !enabled

    fun read(savedStateHandle: SavedStateHandle) {
        filterAgencyAuthority = savedStateHandle.get<String?>(FILTER_AGENCY_AUTHORITY)
        filterType = savedStateHandle.get<String?>(FILTER_TYPE_ID)
        filterLocation = savedStateHandle.get<String?>(FILTER_LOCATION)
        filterScreen = savedStateHandle.get<String?>(FILTER_SCREEN)
        filterUUID = savedStateHandle.get<String?>(FILTER_UUID)
        forceLang = savedStateHandle.get<String?>(FORCE_LANG)
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
        if (filterType.isNullOrBlank()) {
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
        MTLog.d(this, "fixLocale() > defaultLocale $defaultLocale.")
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
    return filterTo(ArrayList(), { type -> type.id == demoModeManager.filterTypeId || demoModeManager.isAllowedAnyway(type) })
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