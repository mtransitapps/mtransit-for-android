package org.mtransit.android.dev

import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import org.mtransit.android.commons.Constants
import javax.inject.Inject

class StrictModeImpl @Inject constructor(
) : IStrictMode {


    override fun setup(enabled: Boolean) {
        if (Constants.FORCE_STRICT_MODE_OFF) {
            return
        }
        if (enabled) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
}