package org.mtransit.android.ui.view.common

import android.app.Activity
import android.view.View
import androidx.lifecycle.LifecycleOwner
import org.mtransit.android.common.IContext

interface IFragment : IContext, IViewFinder, LifecycleOwner {

    fun getActivity(): Activity? // same as SDK

    @Throws(IllegalStateException::class)
    fun requireActivity(): Activity // same as SDK

    fun getViewLifecycleOwner(): LifecycleOwner // same as SDK

    fun getView(): View? // same as SDK
}