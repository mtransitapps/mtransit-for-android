package org.mtransit.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.receiver.DataChange
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import javax.inject.Inject

/*
 TO TEST: REMOVE PERMISSION FROM MANIFEST
 adb shell am broadcast -a org.mtransit.android.intent.action.DATA_CHANGE \
 -n org.mtransit.android.debug/org.mtransit.android.receiver.ModuleDataChangeReceiver \
 --ez force true \
 --es pkg "org.mtransit.android.ca_montreal_stm_subway.debug"
 */
@AndroidEntryPoint
class ModuleDataChangeReceiver : BroadcastReceiver(),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = ModuleDataChangeReceiver::class.java.getSimpleName()
    }

    override fun getLogTag() = LOG_TAG

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: run {
            this.crashReporter.w(this, "Modules data change broadcast receiver with null context ignored!")
            return
        }
        val action = intent?.action?.takeIf { DataChange.ACTION_DATA_CHANGE == it } ?: run {
            this.crashReporter.w(this, "Modules data change broadcast receiver with unexpected action '${intent?.action}' ignored!")
            return
        }
        MTLog.i(this, "Broadcast received: %s", action)
        val extras = intent.extras
        val pkg = extras?.getString(DataChange.PKG)
            .takeIf { extras?.getBoolean(DataChange.FORCE, false) == true }
        val pendingResult = goAsync()
        ioScope.launch {
            try {
                dataSourcesRepository.updateLock(pkg)
            } catch (e: Exception) {
                MTLog.w(this, e, "Error while updating data-sources from repository!");
            } finally {
                pendingResult.finish()
            }
        }
    }
}
