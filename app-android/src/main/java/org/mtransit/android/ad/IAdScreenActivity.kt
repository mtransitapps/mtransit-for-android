package org.mtransit.android.ad

import org.mtransit.android.ui.view.common.IActivity

interface IAdScreenActivity : IActivity {

    val currentAdFragment: IAdScreenFragment?
        get() = currentFragment as? IAdScreenFragment

    fun onPrivacyOptionsRequiredChanged()
}