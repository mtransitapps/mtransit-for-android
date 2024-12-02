package org.mtransit.android.ui.view.common

import android.app.Activity
import org.mtransit.android.common.IContext

interface IFragment: IContext, IViewFinder {

    fun getActivity(): Activity?

    @Throws(IllegalStateException::class)
    fun requireActivity(): Activity

}