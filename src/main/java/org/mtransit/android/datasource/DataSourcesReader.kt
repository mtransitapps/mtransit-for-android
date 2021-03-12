package org.mtransit.android.datasource

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.getAppLongVersionCode
import org.mtransit.android.commons.getInstalledProvidersWithMetaData
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.isAppInstalled
import org.mtransit.android.data.AgencyProperties.Companion.DEFAULT_VERSION_CODE

class DataSourcesReader(
    app: IApplication,
    private val pm: PackageManager,
    private val dataSourcesDatabase: DataSourcesDatabase,
) : MTLog.Loggable {

    companion object {
        val LOG_TAG: String = DataSourcesReader::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val agencyProviderMetaData by lazy { app.requireApplication().getString(R.string.agency_provider) }

    // TODO
    // 1- remove wrong know data (uninstalled/disabled/updated properties)
    //    - ...
    // 2- look for new pkg...
    suspend fun update() {
        withContext(Dispatchers.IO) {
            updateKnownDataSources()
            // TOOD ...
        }
    }

    private fun updateKnownDataSources() {
        // 1 - UPDATE VISIBLE KNOW DATA SOURCES (installed & enabled & check version)
        dataSourcesDatabase.agencyPropertiesDao().getAllInstalledAndEnabledAgencies().forEach { agencyProperties ->
            val pkg = agencyProperties.pkg
            if (!pm.isAppInstalled(pkg)) { // APP UNINSTALLED
                if (agencyProperties.isInstalled) {
                    MTLog.d(this, "Agency '${agencyProperties.authority}' uninstalled.")
                    dataSourcesDatabase.agencyPropertiesDao().update(
                        agencyProperties.copy(isInstalled = false)
                    )
                }
            } else if (!pm.isAppEnabled(pkg)) { // APP DISABLED #DontKillMyApp
                if (agencyProperties.isEnabled) {
                    MTLog.d(this, "Agency '${agencyProperties.authority}' disabled.")
                    dataSourcesDatabase.agencyPropertiesDao().update(
                        agencyProperties.copy(isEnabled = false)
                    )
                }
            } else if (pm.getAppLongVersionCode(pkg, DEFAULT_VERSION_CODE) != agencyProperties.longVersionCode) {
                // val isRTS = false
                val providers = pm.getInstalledProvidersWithMetaData(pkg)
                if (providers.isNullOrEmpty()) {
                    MTLog.d(this, "Agency '${agencyProperties.authority}' invalid (no content providers).")
                    dataSourcesDatabase.agencyPropertiesDao().delete(agencyProperties)
                    return@forEach
                }
                val thisProvider = providers
                    .filter { it.authority == agencyProperties.authority }
                    .singleOrNull {
                        it.metaData?.getString(agencyProviderMetaData) == agencyProviderMetaData
                    }
                if (thisProvider == null) {
                    MTLog.d(this, "Agency '${agencyProperties.authority}' invalid (no agency meta-data).")
                    dataSourcesDatabase.agencyPropertiesDao().delete(agencyProperties)
                    return@forEach
                }
                // TODO ...
            }
        }
    }
}