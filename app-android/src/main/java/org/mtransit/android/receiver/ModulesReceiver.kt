package org.mtransit.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.MainThread
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class ModulesReceiver : BroadcastReceiver(),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = ModulesReceiver::class.java.getSimpleName()

        @Suppress("deprecation")
        private val ACTIONS = listOf(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_DATA_CLEARED,
            Intent.ACTION_PACKAGE_FIRST_LAUNCH,
            Intent.ACTION_PACKAGE_FULLY_REMOVED,
            Intent.ACTION_PACKAGE_INSTALL, // deprecated (never been used)
            Intent.ACTION_PACKAGE_NEEDS_VERIFICATION,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_RESTARTED,
            Intent.ACTION_PACKAGE_VERIFIED
        )

        val INTENT_FILTER
            get() = IntentFilter().apply {
                for (action in ACTIONS) {
                    addAction(action)
                }
                addDataScheme("package")
            }
    }

    override fun getLogTag() = LOG_TAG

    private fun shouldPing(intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                return !replacing // will be followed by Intent.ACTION_PACKAGE_REPLACED
            }

            Intent.ACTION_PACKAGE_REPLACED -> return true
            else -> return false
        }
    }

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var dataSourceRequestManager: DataSourceRequestManager

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @MainThread
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: run {
            this.crashReporter.w(this, "Modules broadcast receiver with null context ignored!")
            return
        }
        val action = intent?.action?.takeIf { ACTIONS.contains(it) } ?: run {
            this.crashReporter.w(this, "Modules broadcast receiver with unexpected action '${intent?.action}' ignored!")
            return
        }
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        ioScope.launch {
            try {
                processSupportedAction(appContext, intent, action)
            } catch (e: Exception) {
                MTLog.w(this, e, "Error while processing receive broadcast!")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processSupportedAction(context: Context, intent: Intent, action: String) {
        val data = intent.data
        val pkg = data?.schemeSpecificPart
        val isAProvider = this.dataSourcesRepository.isAProvider(pkg, true)
        if (isAProvider && shouldPing(intent)) {
            if (ping(context, pkg)) { // TODO check if a GTFS provider?
                MTLog.i(this, "Received broadcast %s for %s.", action, pkg)
            }
        }
        val canBeAProvider = isAProvider
                || Intent.ACTION_PACKAGE_FULLY_REMOVED == action
                || Intent.ACTION_PACKAGE_REMOVED == action
        if (!canBeAProvider) {
            MTLog.d(this, "onReceive() > SKIP (can NOT be a provider: pkg:%s, action:%s)", pkg, action)
            return
        }
        dataSourcesRepository.updateLock(pkg)
    }

    private suspend fun ping(context: Context, pkg: String?): Boolean {
        val providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg) ?: return false
        val agencyProviderMetaData = context.getString(commonsR.string.agency_provider)
        for (provider in providers) {
            val metaData = provider?.metaData ?: continue
            if (metaData.getString(agencyProviderMetaData) == agencyProviderMetaData) {
                MTLog.i(this, "Ping: %s", provider.authority)
                dataSourceRequestManager.ping(provider.authority)
                return true
            }
        }
        return false
    }
}
