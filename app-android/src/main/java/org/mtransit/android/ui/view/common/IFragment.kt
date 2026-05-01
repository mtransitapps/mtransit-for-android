package org.mtransit.android.ui.view.common

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import org.mtransit.android.common.IContext

interface IFragment : IContext, IViewFinder, LifecycleOwner {

    fun getActivity(): FragmentActivity? // same as SDK

    @Throws(IllegalStateException::class)
    fun requireActivity(): FragmentActivity // same as SDK

    fun getViewLifecycleOwner(): LifecycleOwner // same as SDK

    fun getView(): View? // same as SDK
}