package org.mtransit.android.ad

import androidx.annotation.MainThread
import org.mtransit.android.ui.view.common.IActivity

interface IAdScreenActivity : IActivity {

    @get:MainThread
    val currentAdFragment: IAdScreenFragment?
        get() = currentFragment as? IAdScreenFragment

    fun onPrivacyOptionsRequiredChanged()
}