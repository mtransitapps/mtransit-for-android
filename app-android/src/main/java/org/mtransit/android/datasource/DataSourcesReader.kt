package org.mtransit.android.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.getAllInstalledProvidersWithMetaData
import org.mtransit.android.commons.getAppLongVersionCode
import org.mtransit.android.commons.getInstalledProviderWithMetaData
import org.mtransit.android.commons.getInstalledProvidersWithMetaData
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.isAppInstalled
import org.mtransit.android.commons.isKeyMT
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.ServiceUpdateProviderProperties
import org.mtransit.android.data.StatusProviderProperties
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.mtransit.android.commons.R as commonsR

@Singleton
class DataSourcesReader @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val pm: PackageManager,
    private val analyticsManager: IAnalyticsManager,
    private val dataSourcesDatabase: DataSourcesDatabase,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val lclPrefRepository: LocalPreferenceRepository,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = DataSourcesReader::class.java.simpleName

        @Suppress("SpellCheckingInspection")
        private val NOT_SUPPORTED_APPS_PKG: List<String> = if (BuildConfig.DEBUG) listOf(
            "org.mtransit.android.ca_deux_montagnes_mrcdm_bus.debug", // not supported anymore
            "org.mtransit.android.ca_fort_erie_transit_bus.debug", // not supported anymore
            "org.mtransit.android.ca_haut_st_laurent_cithsl_bus.debug", // not supported anymore
            "org.mtransit.android.ca_joliette_ctjm_bus.debug", // not supported anymore
            "org.mtransit.android.ca_lanaudiere_crtl_bus.debug", // not supported anymore
            "org.mtransit.android.ca_le_richelain_roussillon_lrrs_bus.debug", // never published
            "org.mtransit.android.ca_maritime_bus.debug", // not supported anymore
            "org.mtransit.android.ca_montreal_amt_bus", // not supported anymore
            "org.mtransit.android.ca_roussillon_citrous_bus.debug", // not supported anymore
            "org.mtransit.android.ca_ottawa_oc_transpo_train.debug", // migrated to v2
            "org.mtransit.android.ca_quebec_rtc_bus.debug", // migrated to v2
            "org.mtransit.android.ca_vancouver_translink_ferry.debug", // migrated to v2
            "org.mtransit.android.ca_west_coast_express_bus.debug", // not supported anymore
            "org.mtransit.android.us_washington_state_ferry.debug", // not supported anymore
            // DEBUG
            // "org.mtransit.android.ca_chambly_richelieu_carignan_citcrc_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_gatineau_sto_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_edmonton_ets_train.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_laurentides_citla_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_laval_stl_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_longueuil_rtl_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_montreal_amt_train.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_montreal_bixi_bike.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_montreal_rem_light_rail.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_montreal_stm_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_montreal_stm_subway.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_quebec_a_velo_bike.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_quebec_orleans_express_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_quebec_rtc_bus2.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_richelieu_citvr_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_sherbrooke_sts_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_ste_julie_omitsju_bus.debug", // FIXME DEBUG
            // "org.mtransit.android.ca_via_rail_train.debug", // FIXME DEBUG
            // DEBUG
        ) else listOf(
            "org.mtransit.android.ca_deux_montagnes_mrcdm_bus", // not supported anymore
            "org.mtransit.android.ca_fort_erie_transit_bus", // not supported anymore
            "org.mtransit.android.ca_haut_st_laurent_cithsl_bus", // not supported anymore
            "org.mtransit.android.ca_joliette_ctjm_bus", // not supported anymore
            "org.mtransit.android.ca_lanaudiere_crtl_bus", // not supported anymore
            "org.mtransit.android.ca_le_richelain_roussillon_lrrs_bus", // never published
            "org.mtransit.android.ca_maritime_bus", // not supported anymore
            "org.mtransit.android.ca_montreal_amt_bus", // not supported anymore
            "org.mtransit.android.ca_ottawa_oc_transpo_train", // migrated to v2
            "org.mtransit.android.ca_roussillon_citrous_bus", // not supported anymore
            "org.mtransit.android.ca_quebec_rtc_bus", // migrated to v2
            "org.mtransit.android.ca_vancouver_translink_ferry", // migrated to v2
            "org.mtransit.android.ca_west_coast_express_bus", // not supported anymore
            "org.mtransit.android.us_washington_state_ferry", // not supported anymore
        )

        private const val PREFS_LCL_AVAILABLE_VERSION_LAST_CHECK_IN_MS = "pLclAvailableVersionLastCheck"

        private val MIN_DURATION_BETWEEN_APP_VERSION_CHECK_IN_MS = TimeUnit.HOURS.toMillis(6L)
    }

    override fun getLogTag(): String = LOG_TAG

    @get:WorkerThread
    @set:WorkerThread
    private var hasSeenDisabledModule: Boolean = LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED_DEFAULT
        get() = lclPrefRepository.getValue(
            LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED,
            LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED_DEFAULT
        )
        set(value) {
            if (hasSeenDisabledModule != value) {
                lclPrefRepository.pref.edit {
                    putBoolean(LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED, value)
                }
                field = value
            }
        }

    private val agencyProviderMetaData by lazy { appContext.getString(commonsR.string.agency_provider) }

    private val statusProviderMetaData by lazy { appContext.getString(commonsR.string.status_provider) }
    private val scheduleProviderMetaData by lazy { appContext.getString(commonsR.string.schedule_provider) }
    private val serviceUpdateProviderMetaData by lazy { appContext.getString(commonsR.string.service_update_provider) }
    private val newsProviderMetaData by lazy { appContext.getString(commonsR.string.news_provider) }

    private val agencyProviderTypeMetaData by lazy { appContext.getString(commonsR.string.agency_provider_type) }
    private val rdsProviderMetaData by lazy { appContext.getString(commonsR.string.rds_provider) }

    private val statusProviderTargetMetaData by lazy { appContext.getString(commonsR.string.status_provider_target) }
    private val scheduleProviderTargetMetaData by lazy { appContext.getString(commonsR.string.schedule_provider_target) }
    private val serviceUpdateProviderTargetMetaData by lazy { appContext.getString(commonsR.string.service_update_provider_target) }
    private val newsProviderTargetMetaData by lazy { appContext.getString(commonsR.string.news_provider_target) }

    fun isAProvider(pkg: String?, agencyOnly: Boolean = false): Boolean {
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
            if (agencyOnly) {
                return false
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
            @Suppress("RedundantIf")
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
            refreshAvailableVersions(skipTimeCheck = false, forceAppUpdateRefresh = true) { updated = true }
            MTLog.d(this@DataSourcesReader, "update() > updated: $updated")
            if (updated) {
                analyticsManager.setUserProperty(
                    AnalyticsUserProperties.MODULES_COUNT,
                    dataSourcesDatabase.agencyPropertiesDao().getAllAgenciesInclNotInstalled().size
                )
            }
        }
        MTLog.d(this, "update() > $updated")
        return updated
    }

    internal suspend fun refreshAvailableVersions(
        forcePkg: String? = null,
        skipTimeCheck: Boolean = false,
        forceAppUpdateRefresh: Boolean = false,
        markUpdated: () -> Unit,
    ) {
        val lastCheckInMs = lclPrefRepository.getValue(PREFS_LCL_AVAILABLE_VERSION_LAST_CHECK_IN_MS, -1L)
        val shortTimeAgo = TimeUtils.currentTimeMillis() - MIN_DURATION_BETWEEN_APP_VERSION_CHECK_IN_MS
        if (!skipTimeCheck && shortTimeAgo < lastCheckInMs) {
            val timeLapsedInHours = TimeUnit.MILLISECONDS.toHours(TimeUtils.currentTimeMillis() - lastCheckInMs)
            MTLog.d(this, "refreshAvailableVersions() > SKIP (last successful refresh too recent: $timeLapsedInHours hours ago)")
            return
        }
        var updated = false
        dataSourcesDatabase.agencyPropertiesDao().getAllEnabledAgencies().filter { forcePkg == null || forcePkg == it.pkg }.forEach { agencyProperties ->
            val pkg = agencyProperties.pkg
            val authority = agencyProperties.authority
            if (NOT_SUPPORTED_APPS_PKG.contains(pkg)) {
                MTLog.d(this, "refreshAvailableVersions > SKIP not supported '$pkg .")
                return@forEach // skip not supported
            }
            val newLongVersionCode = pm.getAppLongVersionCode(pkg, agencyProperties.longVersionCode)
            if (newLongVersionCode > agencyProperties.longVersionCode) {
                MTLog.d(this, "Agency '$authority' > new version installed: r$newLongVersionCode.")
                dataSourcesDatabase.agencyPropertiesDao().update(
                    agencyProperties.copy(longVersionCode = newLongVersionCode)
                )
                markUpdated()
                updated = true
            }
            this.dataSourceRequestManager.findAgencyAvailableVersionCode(authority, forceAppUpdateRefresh, forcePkg != null)?.let { newAvailableVersionCode ->
                if (agencyProperties.availableVersionCode != newAvailableVersionCode) {
                    MTLog.d(this, "Agency '$authority' > new version available: r$newAvailableVersionCode.")
                    dataSourcesDatabase.agencyPropertiesDao().update(
                        agencyProperties.copy(availableVersionCode = newAvailableVersionCode)
                    )
                    markUpdated()
                    updated = true
                }
            }
        }
        if (!skipTimeCheck || updated) {
            lclPrefRepository.saveAsync(PREFS_LCL_AVAILABLE_VERSION_LAST_CHECK_IN_MS, TimeUtils.currentTimeMillis())
        }
    }

    private suspend fun lookForNewDataSources(
        forcePkg: String? = null,
        markUpdated: () -> Unit,
    ) {
        val knownAgencyProperties = dataSourcesDatabase.agencyPropertiesDao().getAllAgencies()
        val knownStatusProviderProperties = dataSourcesDatabase.statusProviderPropertiesDao().getAllStatusProvider()
        val knownScheduleProviderProperties = dataSourcesDatabase.scheduleProviderPropertiesDao().getAllScheduleProvider()
        val knownServiceUpdateProviderProperties = dataSourcesDatabase.serviceUpdateProviderPropertiesDao().getAllServiceUpdateProvider()
        val knownNewsProviderProperties = dataSourcesDatabase.newsProviderPropertiesDao().getAllNewsProvider()
        @Suppress("DEPRECATION") // DO request all PKG providers info
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
                            pm.getAppLongVersionCode(pkg, IAgencyProperties.DEFAULT_LONG_VERSION_CODE), // NEW
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
                            val validTargetAuthority = targetAuthority.takeIf { it.isNotEmpty() }
                                ?: pkgProviders.singleOrNull { it.metaData.isKeyMT(agencyProviderMetaData) }?.authority
                                    .orEmpty() // will never be visible!
                            MTLog.d(this, "Status provider '${providerAuthority}' added (target: '$validTargetAuthority').")
                            dataSourcesDatabase.statusProviderPropertiesDao().insert(
                                StatusProviderProperties(providerAuthority, validTargetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
                // SCHEDULE
                if (providerMetaData.isKeyMT(scheduleProviderMetaData)) {
                    if (knownScheduleProviderProperties.none { it.authority == providerAuthority }) {
                        providerMetaData.getString(scheduleProviderTargetMetaData)?.let { targetAuthority ->
                            val validTargetAuthority = targetAuthority.takeIf { it.isNotEmpty() }
                                ?: pkgProviders.singleOrNull { it.metaData.isKeyMT(agencyProviderMetaData) }?.authority
                                    .orEmpty() // will never be visible!
                            MTLog.d(this, "Schedule provider '${providerAuthority}' added (target: '$validTargetAuthority').")
                            dataSourcesDatabase.scheduleProviderPropertiesDao().insert(
                                ScheduleProviderProperties(providerAuthority, validTargetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
                // SERVICE UPDATE
                if (providerMetaData.isKeyMT(serviceUpdateProviderMetaData)) {
                    if (knownServiceUpdateProviderProperties.none { it.authority == providerAuthority }) {
                        providerMetaData.getString(serviceUpdateProviderTargetMetaData)?.let { targetAuthority ->
                            val validTargetAuthority = targetAuthority.takeIf { it.isNotEmpty() }
                                ?: pkgProviders.singleOrNull { it.metaData.isKeyMT(agencyProviderMetaData) }?.authority
                                    .orEmpty() // will never be visible!
                            MTLog.d(this, "Service Update provider '${providerAuthority}' added (target: '$validTargetAuthority').")
                            dataSourcesDatabase.serviceUpdateProviderPropertiesDao().insert(
                                ServiceUpdateProviderProperties(providerAuthority, validTargetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
                // NEWS
                if (providerMetaData.isKeyMT(newsProviderMetaData)) {
                    if (knownNewsProviderProperties.none { it.authority == providerAuthority }) {
                        providerMetaData.getString(newsProviderTargetMetaData)?.let { targetAuthority ->
                            val validTargetAuthority = targetAuthority.takeIf { it.isNotEmpty() }
                                ?: pkgProviders.singleOrNull { it.metaData.isKeyMT(agencyProviderMetaData) }?.authority
                                    .orEmpty() // will only be visible in News screen
                            dataSourcesDatabase.newsProviderPropertiesDao().insert(
                                NewsProviderProperties(providerAuthority, validTargetAuthority, pkg)
                            )
                            markUpdated()
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateReInstalledReEnabledDataSources(
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
            val longVersionCode = pm.getAppLongVersionCode(pkg, IAgencyProperties.DEFAULT_LONG_VERSION_CODE)
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

    private suspend fun updateKnownActiveDataSources(
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
                hasSeenDisabledModule = true
                updateDisabledAgencyProperties(agencyProperties, authority, markUpdated)
                return@forEach
            }
            val longVersionCode = pm.getAppLongVersionCode(pkg, IAgencyProperties.DEFAULT_LONG_VERSION_CODE)
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

    private suspend fun updateDisabledAgencyProperties(agencyProperties: AgencyProperties, authority: String, markUpdated: () -> Unit) {
        if (agencyProperties.isEnabled) {
            MTLog.d(this, "Agency '$authority' disabled.")
            dataSourcesDatabase.agencyPropertiesDao().update(
                agencyProperties.copy(isEnabled = false)
            )
            markUpdated()
        }
    }

    private suspend fun updateUninstalledAgencyProperties(agencyProperties: AgencyProperties, authority: String, markUpdated: () -> Unit) {
        if (agencyProperties.isInstalled) {
            MTLog.d(this, "Agency '$authority' uninstalled.")
            dataSourcesDatabase.agencyPropertiesDao().update(
                agencyProperties.copy(isInstalled = false)
            )
            markUpdated()
        }
    }

    private suspend fun refreshAgencyProperties(
        pkg: String,
        agencyAuthority: String,
        agencyProperties: AgencyProperties? = null, // NEW
        longVersionCode: Long = pm.getAppLongVersionCode(pkg, IAgencyProperties.DEFAULT_LONG_VERSION_CODE), // NEW,
        pkgProviders: Collection<ProviderInfo>? = pm.getInstalledProvidersWithMetaData(pkg),
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
        val isRDS = providerMetaData.isKeyMT(rdsProviderMetaData)
        val logo = if (isRDS) this.dataSourceRequestManager.findAgencyRDSRouteLogo(agencyAuthority) else null
        val trigger = if (triggerUpdate) agencyProperties?.let { it.trigger + 1 } ?: 0 else 0
        val newAgencyProperties = this.dataSourceRequestManager.findAgencyProperties(
            agencyAuthority,
            agencyType,
            isRDS,
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

    private suspend fun refreshStatusProviderProperties(statusProviderProperties: StatusProviderProperties, markUpdated: () -> Unit) {
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

    private suspend fun refreshScheduleProviderProperties(scheduleProviderProperties: ScheduleProviderProperties, markUpdated: () -> Unit) {
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

    private suspend fun refreshServiceUpdateProviderProperties(serviceUpdateProviderProperties: ServiceUpdateProviderProperties, markUpdated: () -> Unit) {
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

    private suspend fun refreshNewsProviderProperties(newsProviderProperties: NewsProviderProperties, markUpdated: () -> Unit) {
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
