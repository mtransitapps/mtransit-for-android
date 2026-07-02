package org.mtransit.android.data

import android.content.pm.PackageManager
import org.mtransit.android.commons.getAppLongVersionCode

interface IAgencyUpdatableProperties : IAgencyProperties {

    val updateAvailable: Boolean

    val longVersionCode: Long

    val availableVersionCode: Int

    val shouldShowUpdateLayout: Boolean

    fun isUpdateAvailableNow(pm: PackageManager): Boolean =
        updateAvailable
                && pm.getAppLongVersionCode(this.pkg, this.longVersionCode).toInt() < this.availableVersionCode
}