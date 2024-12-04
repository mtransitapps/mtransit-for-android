package org.mtransit.android.ui.view.common

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import org.mtransit.android.common.IContext
import java.lang.IllegalStateException

interface IActivity : IContext, LifecycleOwner {

    val activity: Activity?

    @Throws(IllegalStateException::class)
    fun requireActivity(): Activity

    val currentFragment: Fragment?

    fun finish()
}
