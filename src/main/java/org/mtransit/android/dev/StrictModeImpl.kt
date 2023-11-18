package org.mtransit.android.dev

import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import org.mtransit.android.BuildConfig
import org.mtransit.android.commons.Constants
import java.util.concurrent.Executors
import javax.inject.Inject

// adb logcat -s "StrictMode"
class StrictModeImpl @Inject constructor(
    private val crashReporter: CrashReporter,
) : IStrictMode {

    override fun setup() {
        if (Constants.FORCE_STRICT_MODE_OFF) {
            return
        }
        if (!BuildConfig.DEBUG) {
            return
        }
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                // .detectCustomSlowCalls() // CustomViolation: gcore.dynamite com.google.android.gms.dynamite.DynamiteModule...
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        this.detectResourceMismatches()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        this.detectUnbufferedIo()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        this.detectExplicitGc()
                    }
                    @Suppress("ConstantConditionIf")
                    if (false) {
                        this.detectAll() // disabled because too much logs
                    }
                    if (BuildConfig.DEBUG) {
                        this.penaltyFlashScreen()
                        this.penaltyDeathOnNetwork()
                        this.penaltyLog() // DEBUG level
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        this.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            crashReporter.reportNonFatal(violation, "StrictMode ThreadPolicy violation '${violation::class.java.simpleName}'!")
                        }
                    }
                }
                .build()
        )
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectFileUriExposure()
                .apply {
                    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // this.detectCleartextNetwork() // disabled because we use clear text
                    // }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        this.detectContentUriWithoutPermission()
                        // this.detectUntaggedSockets() // disabled because we don't do this yet
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        this.detectCredentialProtectedWhileLocked()
                    }
                    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // this.detectIncorrectContextUse() // disabled becayse Ads SDK
                    // }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        this.detectUnsafeIntentLaunch()
                    }
                    @Suppress("ConstantConditionIf")
                    if (false) {
                        this.detectAll() // disabled because too much logs
                    }
                    if (BuildConfig.DEBUG) {
                        this.penaltyLog() // DEBUG level
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        this.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            crashReporter.reportNonFatal(violation, "StrictMode VmPolicy violation '${violation::class.java.simpleName}'!")
                        }
                    }
                }
                .build()
        )
    }
}