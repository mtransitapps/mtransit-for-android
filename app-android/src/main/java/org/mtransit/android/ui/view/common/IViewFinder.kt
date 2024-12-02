package org.mtransit.android.ui.view.common

import android.view.View
import androidx.annotation.IdRes

interface IViewFinder {
    fun <T : View?> findViewById(@IdRes id: Int): T?
}
