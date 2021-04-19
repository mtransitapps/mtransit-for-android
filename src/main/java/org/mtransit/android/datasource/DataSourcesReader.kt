package org.mtransit.android.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.getAllInstalledProvidersWithMetaData
import org.mtransit.android.commons.getAppLongVersionCode
import org.mtransit.android.commons.getInstalledProviderWithMetaData
import org.mtransit.android.commons.getInstalledProvidersWithMetaData
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.isAppInstalled
import org.mtransit.android.commons.isKeyMT
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.AgencyProperties.Companion.DEFAULT_LONG_VERSION_CODE
import org.mtransit.android.data.DataSourceManager
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.ServiceUpdateProviderProperties
import org.mtransit.android.data.StatusProviderProperties
import org.mtransit.commons.FeatureFlags.F_APP_UPDATE
import java.util.concurrent.TimeUnit

class DataSourcesReader(
    private val app: IApplication,
    private val pm: PackageManager,
    private val dataSourcesDatabase: DataSourcesDatabase,
) : MTLog.Loggable {

    companion object {
        val LOG_TAG: String = DataSourcesReader::class.java.simpleName

        private val NOT_SUPPORTED_APPS_PKG: List<String> = if (BuildConfig.DEBUG) listOf(
            "org.mtransit.android.ca_deux_montagnes_mrcdm_bus.debug", // not supported anymore
            "org.mtransit.android.ca_joliette_ctjm_bus.debug", // not supported anymore
            "org.mtransit.android.ca_lanaudiere_crtl_bus.debug", // not supported anymore
            "org.mtransit.android.ca_maritime_bus.debug", // not supported anymore
            "org.mtransit.android.ca_montreal_amt_bus", // not supported anymore
            "org.mtransit.android.ca_vancouver_translink_ferry.debug", // migrated to v2
            "org.mtransit.android.ca_west_coast_express_bus.debug", // not supported anymore
        ) else listOf(
            "org.mtransit.android.ca_deux_montagnes_mrcdm_bus", // not supported anymore
            "org.mtransit.android.ca_joliette_ctjm_bus", // not supported anymore
            "org.mtransit.android.ca_lanaudiere_crtl_bus", // not supported anymore
            "org.mtransit.android.ca_maritime_bus", // not supported anymore
            "org.mtransit.android.ca_montreal_amt_bus", // not supported anymore
            "org.mtransit.android.ca_vancouver_translink_ferry", // migrated to v2
            "org.mtransit.android.ca_west_coast_express_bus", // not supported anymore
        )

        private const val PREFS_LCL_AVAILABLE_VERSION_LAST_CHECK_IN_MS = "pLclAvailableVersionLastCheck"
    }

    override fun getLogTag(): String = LOG_TAG

    private val agencyProviderMetaData by lazy { app.requireApplication().getString(R.string.agency_provider) }

    private val statusProviderMetaData by lazy { app.requireApplication().getString(R.string.status_provider) }
    private val scheduleProviderMetaData by lazy { app.requireApplication().getString(R.string.schedule_provider) }
    private val serviceUpdateProviderMetaData by lazy { app.requireApplication().getString(R.string.service_update_provider) }
    private val newsProviderMetaData by lazy { app.requireApplication().getString(R.string.news_provider) }

    private val agencyProviderTypeMetaData by lazy { app.requireApplication().getString(R.string.agency_provider_type) }
    private val rtsProviderMetaData by lazy { app.requireApplication().getString(R.string.rts_provider) }

    private val statusProviderTargetMetaData by lazy { app.requireApplication().getString(R.string.status_provider_target) }
    private val scheduleProviderTargetMetaData by lazy { app.requireApplication().getString(R.string.schedule_provider_target) }
    private val serviceUpdateProviderTargetMetaData by lazy { app.requireApplication().getString(R.string.service_update_provider_target) }
    private val newsProviderTargetMetaData by lazy { app.requireApplication().getString(R.string.news_provider_target) }

    fun isAProvider(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) {
            return false
        }
        if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
            MTLog.d(this, "isAProvider() > SKIP not supported '$pkg .")
            return false
        }
        pm.getInstalledProvidersWithMetaData(pkg)?.forEach { providerInfo ->
            val providerMetaData: Bundle = providerInfo.metaData ?: return@forEach
            if (providerMetaData.isKeyMT(agencyProviderMetaData)) {
                return true
            }
            if (providerMetaData.isKeyMT(statusProviderMetaData)) {
                return true
            }
            if (providerMetaData.isKeyMT(scheduleProviderMetaData)) {
                return true
            }
            if (providerMetaData.isKeyMT(serviceUpdateProviderMetaData)) {
                return true
            }
            if (providerMetaData.isKeyMT(newsProviderMetaData)) {
                return true
            }
            return false
        }
        return false
    }

    suspend fun update(forcePkg: String? = null): Boolean {
        var updated = false
        withContext(Dispatchers.IO) {
            MTLog.d(this@DataSourcesReader, "update() > updated: $updated")
            updateKnownActiveDataSources(forcePkg) { updated = true }
            MTLog.d(this@DataSourcesReader, "update() > updated: $updated")
            updateReInstalledReEnabledDataSources(forcePkg) { updated = true }
            MTLog.d(this@DataSourcesReader, "update() > updated: $updated")
            lookForNewDataSources(forcePkg) { updated = true }
            MTLog.d(this@DataSourcesReader, "update() > updated: $updated")
            refreshAvailableVersions { updated = true }
            MTLog.d(this@DataSourcesReader, "update() > updated: $updated")
        }
        MTLog.d(this, "update() > $updated")
        return updated
    }

    internal fun refreshAvailableVersions(
        skipTimeCheck : Boolean = false,
        forceRefresh: Boolean = false,
        inFocus: Boolean = false,
        markUpdated: () -> Unit,
    ) {
        if (!F_APP_UPDATE) {
            MTLog.d(this, "refreshAvailableVersions() > SKIP (feature disabled)")
            return
        }
        val context: Context = app.application ?: return
        val lastCheckInMs = PreferenceUtils.getPrefLcl(context, PREFS_LCL_AVAILABLE_VERSION_LAST_CHECK_IN_MS, -1L)
        val shortTimeAgo = TimeUtils.currentTimeMillis() - TimeUnit.HOURS.toMillis(if (inFocus) 6L else 24L)
        if (!skipTimeCheck && shortTimeAgo < lastCheckInMs) {
            val timeLapsedInHours = TimeUnit.MILLISECONDS.toHours(TimeUtils.currentTimeMillis() - lastCheckInMs)
            MTLog.d(this, "refreshAvailableVersions() > SKIP (last successful refresh too recent ($timeLapsedInHours hours)")
            return
        }
        dataSourcesDatabase.agencyPropertiesDao().getAllEnabledAgencies().forEach { agencyProperties ->
            val pkg = agencyProperties.pkg
            val authority = agencyProperties.authority
            if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
                MTLog.d(this, "refreshAvailableVersions > SKIP not supported '$pkg .")
                return@forEach // skip not supported
            }
            val newAvailableVersionCode = DataSourceManager.findAgencyAvailableVersionCode(context, authority, forceRefresh, inFocus)
            if (agencyProperties.availableVersionCode != newAvailableVersionCode) {
                MTLog.d(this, "Agency '$authority' > new version: r$newAvailableVersionCode.")
                dataSourcesDatabase.agencyPropertiesDao().update(
                    agencyProperties.copy(availableVersionCode = newAvailableVersionCode)
                )
                markUpdated()
            }
        }
        PreferenceUtils.savePrefLcl(context, PREFS_LCL_AVAILABLE_VERSION_LAST_CHECK_IN_MS, TimeUtils.currentTimeMillis(), false) // ASYNC
    }

    private fun lookForNewDataSources(
        forcePkg: String? = null,
        markUpdated: () -> Unit,
    ) {
        val knownAgencyProperties = dataSourcesDatabase.agencyPropertiesDao().getAllAgencies()
        val knownStatusProviderProperties = dataSourcesDatabase.statusProviderPropertiesDao().getAllStatusProvider()
        val knownScheduleProviderProperties = dataSourcesDatabase.scheduleProviderPropertiesDao().getAllScheduleProvider()
        val knownServiceUpdateProviderProperties = dataSourcesDatabase.serviceUpdateProviderPropertiesDao().getAllServiceUpdateProvider()
        val knownNewsProviderProperties = dataSourcesDatabase.newsProviderPropertiesDao().getAllNewsProvider()
        pm.getAllInstalledProvidersWithMetaData().forEach pkg@{ packageInfo ->
            val pkg = packageInfo.packageName
            if (!pm.isAppEnabled(pkg)) {
                return@pkg // skip unknown disabled (processed before)
            }
            if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
                MTLog.d(this, "lookForNewDataSources() > SKIP not supported '$pkg .")
                return@pkg // skip not supported
            }
            val pkgProviders: List<ProviderInfo> = packageInfo.providers?.toList() ?: return@pkg
            pkgProviders.forEach provider@{ provider ->
                val providerMetaData: Bundle = provider.metaData ?: return@provider
                val providerAuthority = provider.authority
                // AGENCY
                if (providerMetaData.isKeyMT(agencyProviderMetaData)) {
                    if (knownAgencyProperties.none { it.authority == providerAuthority }) {
                        refreshAgencyProperties(
                            pkg,
                            providerAuthority,
                            null, // NEW
                            pm.getAppLongVersionCode(pkg, DEFAULT_LONG_VERSION_CODE), // NEW
                            pkgProviders,
                            forcePkg == pkg,
                            markUpdated
                        )
                    }
                }
                // STATUS
                if (providerMetaData.isKeyMT(statusProviderMetaData)) {
                    if (knownStatusProviderProperties.none { it.authority == providerAuthority }) {
                        providerMetaData.getString(statusProviderTargetMetaData)?.let { targetAuthority ->
                            MTLog.d(this, "Status provider '${providerAuthority}' added.")
                            dataSourcesDatabase.statusProviderPropertiesDao().insert(
                                StatusProviderProperties(providerAuthority, targetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
                // SCHEDULE
                if (providerMetaData.isKeyMT(scheduleProviderMetaData)) {
                    if (knownScheduleProviderProperties.none { it.authority == providerAuthority }) {
                        providerMetaData.getString(scheduleProviderTargetMetaData)?.let { targetAuthority ->
                            MTLog.d(this, "Schedule provider '${providerAuthority}' added.")
                            dataSourcesDatabase.scheduleProviderPropertiesDao().insert(
                                ScheduleProviderProperties(providerAuthority, targetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
                // SERVICE UPDATE
                if (providerMetaData.isKeyMT(serviceUpdateProviderMetaData)) {
                    if (knownServiceUpdateProviderProperties.none { it.authority == providerAuthority }) {
                        providerMetaData.getString(serviceUpdateProviderTargetMetaData)?.let { targetAuthority ->
                            MTLog.d(this, "Service Update provider '${providerAuthority}' added.")
                            dataSourcesDatabase.serviceUpdateProviderPropertiesDao().insert(
                                ServiceUpdateProviderProperties(providerAuthority, targetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
                // NEWS
                if (providerMetaData.isKeyMT(newsProviderMetaData)) {
                    if (knownNewsProviderProperties.none { it.authority == providerAuthority }) {
                        providerMetaData.getString(newsProviderTargetMetaData)?.let { targetAuthority ->
                            MTLog.d(this, "News provider '${providerAuthority}' added.")
                            dataSourcesDatabase.newsProviderPropertiesDao().insert(
                                NewsProviderProperties(providerAuthority, targetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
            }
        }
    }

    private fun updateReInstalledReEnabledDataSources(
        forcePkg: String? = null,
        markUpdated: () -> Unit
    ) { // UPDATE NOT-VISIBLE KNOWN DATA SOURCES (uninstalled | disabled & check version)
        // AGENCY: only apply to agency (other properties are deleted when uninstalled/disabled)
        dataSourcesDatabase.agencyPropertiesDao().getAllNotInstalledOrNotEnabledAgencies().forEach { agencyProperties ->
            val pkg = agencyProperties.pkg
            if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
                agencyProperties.let {
                    MTLog.d(this, "Agency '${agencyProperties.authority}' not supported (removed).")
                    dataSourcesDatabase.agencyPropertiesDao().delete(it)
                    markUpdated()
                }
                MTLog.d(this, "updateReInstalledReEnabledDataSources() > SKIP not supported '$pkg .")
                return@forEach // skip not supported
            }
            val authority = agencyProperties.authority
            if (!pm.isAppInstalled(pkg)) { // APP UNINSTALLED
                updateUninstalledAgencyProperties(agencyProperties, authority, markUpdated)
                return@forEach
            }
            if (!pm.isAppEnabled(pkg)) { // APP DISABLED #DontKillMyApp
                updateDisabledAgencyProperties(agencyProperties, authority, markUpdated)
                return@forEach
            }
            // App RE-installed OR RE-enabled
            val longVersionCode = pm.getAppLongVersionCode(pkg, DEFAULT_LONG_VERSION_CODE)
            if (longVersionCode == agencyProperties.longVersionCode) {
                MTLog.d(this, "Agency '$authority' re-installed / re-enabled.")
                dataSourcesDatabase.agencyPropertiesDao().update(
                    agencyProperties.copy(isInstalled = true, isEnabled = true) // no need to refresh properties == same data
                )
                markUpdated()
            } else {
                refreshAgencyProperties(
                    pkg,
                    authority,
                    agencyProperties,
                    longVersionCode,
                    pm.getInstalledProvidersWithMetaData(pkg),
                    forcePkg == pkg,
                    markUpdated,
                )
            }
        }
    }

    private fun updateKnownActiveDataSources(
        forcePkg: String? = null,
        markUpdated: () -> Unit
    ) { // UPDATE KNOWN ACTIVE DATA SOURCES (installed & enabled & check version)
        val knownStatusProviderProperties = dataSourcesDatabase.statusProviderPropertiesDao().getAllStatusProvider()
        val knownScheduleProviderProperties = dataSourcesDatabase.scheduleProviderPropertiesDao().getAllScheduleProvider()
        val knownServiceUpdateProviderProperties = dataSourcesDatabase.serviceUpdateProviderPropertiesDao().getAllServiceUpdateProvider()
        val knownNewsProviderProperties = dataSourcesDatabase.newsProviderPropertiesDao().getAllNewsProvider()
        // AGENCY (only one properties kept in cache even when uninstalled/disabled to save refreshing data)
        dataSourcesDatabase.agencyPropertiesDao().getAllEnabledAgencies().forEach { agencyProperties ->
            val pkg = agencyProperties.pkg
            val authority = agencyProperties.authority
            if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
                agencyProperties.let {
                    MTLog.d(this, "Agency '$authority' not supported (removed).")
                    dataSourcesDatabase.agencyPropertiesDao().delete(it)
                    markUpdated()
                }
                MTLog.d(this, "updateKnownActiveDataSources > SKIP not supported '$pkg .")
                return@forEach // skip not supported
            }
            if (!pm.isAppInstalled(pkg)) { // APP UNINSTALLED
                updateUninstalledAgencyProperties(agencyProperties, authority, markUpdated)
                return@forEach
            }
            if (!pm.isAppEnabled(pkg)) { // APP DISABLED #DontKillMyApp
                updateDisabledAgencyProperties(agencyProperties, authority, markUpdated)
                return@forEach
            }
            val longVersionCode = pm.getAppLongVersionCode(pkg, DEFAULT_LONG_VERSION_CODE)
            if (forcePkg == pkg
                || longVersionCode != agencyProperties.longVersionCode
            ) {
                refreshAgencyProperties(
                    pkg,
                    authority,
                    agencyProperties,
                    longVersionCode,
                    pm.getInstalledProvidersWithMetaData(pkg),
                    forcePkg == pkg,
                    markUpdated,
                )
            } // ELSE no need to refresh == same data
        }
        // STATUS
        knownStatusProviderProperties.forEach { statusProviderProperties ->
            refreshStatusProviderProperties(statusProviderProperties, markUpdated)
        }
        // SCHEDULE
        knownScheduleProviderProperties.forEach { scheduleProviderProperties ->
            refreshScheduleProviderProperties(scheduleProviderProperties, markUpdated)
        }
        // SERVICE UPDATE
        knownServiceUpdateProviderProperties.forEach { serviceUpdateProviderProperties ->
            refreshServiceUpdateProviderProperties(serviceUpdateProviderProperties, markUpdated)
        }
        // NEWS
        knownNewsProviderProperties.forEach { newsProviderProperties ->
            refreshNewsProviderProperties(newsProviderProperties, markUpdated)
        }
    }

    private fun updateDisabledAgencyProperties(agencyProperties: AgencyProperties, authority: String, markUpdated: () -> Unit) {
        if (agencyProperties.isEnabled) {
            MTLog.d(this, "Agency '$authority' disabled.")
            dataSourcesDatabase.agencyPropertiesDao().update(
                agencyProperties.copy(isEnabled = false)
            )
            markUpdated()
        }
    }

    private fun updateUninstalledAgencyProperties(agencyProperties: AgencyProperties, authority: String, markUpdated: () -> Unit) {
        if (agencyProperties.isInstalled) {
            MTLog.d(this, "Agency '$authority' uninstalled.")
            dataSourcesDatabase.agencyPropertiesDao().update(
                agencyProperties.copy(isInstalled = false)
            )
            markUpdated()
        }
    }

    private fun refreshAgencyProperties(
        pkg: String,
        agencyAuthority: String,
        agencyProperties: AgencyProperties? = null, // NEW
        longVersionCode: Long = pm.getAppLongVersionCode(pkg, DEFAULT_LONG_VERSION_CODE), // NEW,
        pkgProviders: List<ProviderInfo>? = pm.getInstalledProvidersWithMetaData(pkg),
        triggerUpdate: Boolean = false,
        markUpdated: () -> Unit,
    ) {
        if (pkgProviders.isNullOrEmpty()) {
            MTLog.d(this, "Agency '$agencyAuthority' invalid (no content providers).")
            agencyProperties?.let {
                dataSourcesDatabase.agencyPropertiesDao().delete(it)
                markUpdated()
            }
            return
        }
        val thisAgencyPropertiesProvider = pkgProviders
            .filter { it.authority == agencyAuthority }
            .singleOrNull {
                it.metaData.isKeyMT(agencyProviderMetaData)
            }
        if (thisAgencyPropertiesProvider?.metaData == null) {
            MTLog.d(this, "Agency '$agencyAuthority' invalid (no agency meta-data).")
            agencyProperties?.let {
                dataSourcesDatabase.agencyPropertiesDao().delete(it)
                markUpdated()
            }
            return
        }
        val providerMetaData: Bundle = thisAgencyPropertiesProvider.metaData
        val agencyTypeId: Int = providerMetaData.getInt(agencyProviderTypeMetaData, -1)
        val agencyType = if (agencyTypeId == -1) null else DataSourceType.parseId(agencyTypeId)
        if (agencyType == null) {
            MTLog.d(this, "Agency '$agencyAuthority' invalid (type ID '$agencyTypeId' invalid).")
            agencyProperties?.let {
                dataSourcesDatabase.agencyPropertiesDao().delete(it)
                markUpdated()
            }
            return
        }
        val isRTS = providerMetaData.isKeyMT(rtsProviderMetaData)
        val logo = if (isRTS) DataSourceManager.findAgencyRTSRouteLogo(this.app.requireContext(), agencyAuthority) else null
        val trigger = if (triggerUpdate) agencyProperties?.let { it.trigger + 1 } ?: 0 else 0
        val newAgencyProperties = DataSourceManager.findAgencyProperties(
            this.app.requireContext(),
            agencyAuthority,
            agencyType,
            isRTS,
            logo,
            pkg,
            longVersionCode,
            true,
            trigger
        )
        if (newAgencyProperties == null) {
            MTLog.d(this, "Agency '$agencyAuthority' invalid (error while fetching new agency properties).")
            agencyProperties?.let {
                dataSourcesDatabase.agencyPropertiesDao().delete(it)
                markUpdated()
            }
            return
        }
        if (agencyProperties == null) {
            MTLog.d(this, "Agency '$agencyAuthority' properties added.")
            dataSourcesDatabase.agencyPropertiesDao().insert(newAgencyProperties)
            markUpdated()
        } else {
            MTLog.d(this, "Agency '$agencyAuthority' properties updated.")
            dataSourcesDatabase.agencyPropertiesDao().update(newAgencyProperties)
            markUpdated()
        }
    }

    private fun refreshStatusProviderProperties(statusProviderProperties: StatusProviderProperties, markUpdated: () -> Unit) {
        val pkg = statusProviderProperties.pkg
        val authority = statusProviderProperties.authority
        if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
            MTLog.d(this, "Status '$authority' not supported")
            dataSourcesDatabase.statusProviderPropertiesDao().delete(statusProviderProperties)
            markUpdated()
            return
        }
        if (!pm.isAppInstalled(pkg)
            || !pm.isAppEnabled(pkg)
        ) {
            MTLog.d(this, "Status '$authority' removed (uninstalled | disabled)")
            dataSourcesDatabase.statusProviderPropertiesDao().delete(statusProviderProperties)
            markUpdated()
            return
        }
        val provider = pm.getInstalledProviderWithMetaData(pkg, authority)
        val providerMetadata = provider?.metaData
        if (providerMetadata == null) {
            MTLog.d(this, "Status '$authority' removed (no provider metadata)")
            dataSourcesDatabase.statusProviderPropertiesDao().delete(statusProviderProperties)
            markUpdated()
            return
        }
        val newTargetAuthority = providerMetadata.getString(statusProviderTargetMetaData)
        if (newTargetAuthority == null) {
            MTLog.d(this, "Status '$authority' removed (invalid target authority)")
            dataSourcesDatabase.statusProviderPropertiesDao().delete(statusProviderProperties)
            markUpdated()
            return
        }
        if (newTargetAuthority != statusProviderProperties.targetAuthority) {
            MTLog.d(this, "Status '$authority' updated")
            dataSourcesDatabase.statusProviderPropertiesDao().update(
                statusProviderProperties.copy(targetAuthority = newTargetAuthority)
            )
            markUpdated()
        }
    }

    private fun refreshScheduleProviderProperties(scheduleProviderProperties: ScheduleProviderProperties, markUpdated: () -> Unit) {
        val pkg = scheduleProviderProperties.pkg
        val authority = scheduleProviderProperties.authority
        if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
            MTLog.d(this, "Schedule '$authority' not supported")
            dataSourcesDatabase.scheduleProviderPropertiesDao().delete(scheduleProviderProperties)
            markUpdated()
            return
        }
        if (!pm.isAppInstalled(pkg)
            || !pm.isAppEnabled(pkg)
        ) {
            MTLog.d(this, "Schedule '$authority' removed (uninstalled | disabled)")
            dataSourcesDatabase.scheduleProviderPropertiesDao().delete(scheduleProviderProperties)
            markUpdated()
            return
        }
        val provider = pm.getInstalledProviderWithMetaData(pkg, authority)
        val providerMetadata = provider?.metaData
        if (providerMetadata == null) {
            MTLog.d(this, "Schedule '$authority' removed (no provider metadata)")
            dataSourcesDatabase.scheduleProviderPropertiesDao().delete(scheduleProviderProperties)
            markUpdated()
            return
        }
        val newTargetAuthority = providerMetadata.getString(scheduleProviderTargetMetaData)
        if (newTargetAuthority == null) {
            MTLog.d(this, "Schedule '$authority' removed (invalid target authority)")
            dataSourcesDatabase.scheduleProviderPropertiesDao().delete(scheduleProviderProperties)
            markUpdated()
            return
        }
        if (newTargetAuthority != scheduleProviderProperties.targetAuthority) {
            MTLog.d(this, "Schedule '$authority' updated")
            dataSourcesDatabase.scheduleProviderPropertiesDao().update(
                scheduleProviderProperties.copy(targetAuthority = newTargetAuthority)
            )
            markUpdated()
        }
    }

    private fun refreshServiceUpdateProviderProperties(serviceUpdateProviderProperties: ServiceUpdateProviderProperties, markUpdated: () -> Unit) {
        val pkg = serviceUpdateProviderProperties.pkg
        val authority = serviceUpdateProviderProperties.authority
        if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
            MTLog.d(this, "Service Update '$authority' not supported")
            dataSourcesDatabase.serviceUpdateProviderPropertiesDao().delete(serviceUpdateProviderProperties)
            markUpdated()
            return
        }
        if (!pm.isAppInstalled(pkg)
            || !pm.isAppEnabled(pkg)
        ) {
            MTLog.d(this, "Service Update '$authority' removed (uninstalled | disabled)")
            dataSourcesDatabase.serviceUpdateProviderPropertiesDao().delete(serviceUpdateProviderProperties)
            markUpdated()
            return
        }
        val provider = pm.getInstalledProviderWithMetaData(pkg, authority)
        val providerMetadata = provider?.metaData
        if (providerMetadata == null) {
            MTLog.d(this, "Service Update '$authority' removed (no provider metadata)")
            dataSourcesDatabase.serviceUpdateProviderPropertiesDao().delete(serviceUpdateProviderProperties)
            markUpdated()
            return
        }
        val newTargetAuthority = providerMetadata.getString(serviceUpdateProviderTargetMetaData)
        if (newTargetAuthority == null) {
            MTLog.d(this, "Service Update '$authority' removed (invalid target authority)")
            dataSourcesDatabase.serviceUpdateProviderPropertiesDao().delete(serviceUpdateProviderProperties)
            markUpdated()
            return
        }
        if (newTargetAuthority != serviceUpdateProviderProperties.targetAuthority) {
            MTLog.d(this, "Service Update '$authority' updated")
            dataSourcesDatabase.serviceUpdateProviderPropertiesDao().update(
                serviceUpdateProviderProperties.copy(targetAuthority = newTargetAuthority)
            )
            markUpdated()
        }
    }

    private fun refreshNewsProviderProperties(newsProviderProperties: NewsProviderProperties, markUpdated: () -> Unit) {
        val pkg = newsProviderProperties.pkg
        val authority = newsProviderProperties.authority
        if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
            MTLog.d(this, "News '$authority' not supported")
            dataSourcesDatabase.newsProviderPropertiesDao().delete(newsProviderProperties)
            markUpdated()
            return
        }
        if (!pm.isAppInstalled(pkg)
            || !pm.isAppEnabled(pkg)
        ) {
            MTLog.d(this, "News '$authority' removed (uninstalled | disabled)")
            dataSourcesDatabase.newsProviderPropertiesDao().delete(newsProviderProperties)
            markUpdated()
            return
        }
        val provider = pm.getInstalledProviderWithMetaData(pkg, authority)
        val providerMetadata = provider?.metaData
        if (providerMetadata == null) {
            MTLog.d(this, "News '$authority' removed (no provider metadata)")
            dataSourcesDatabase.newsProviderPropertiesDao().delete(newsProviderProperties)
            markUpdated()
            return
        }
        val newTargetAuthority = providerMetadata.getString(newsProviderTargetMetaData)
        if (newTargetAuthority == null) {
            MTLog.d(this, "News '$authority' removed (invalid target authority)")
            dataSourcesDatabase.newsProviderPropertiesDao().delete(newsProviderProperties)
            markUpdated()
            return
        }
        if (newTargetAuthority != newsProviderProperties.targetAuthority) {
            MTLog.d(this, "News '$authority' updated")
            dataSourcesDatabase.newsProviderPropertiesDao().update(
                newsProviderProperties.copy(targetAuthority = newTargetAuthority)
            )
            markUpdated()
        }
    }
}