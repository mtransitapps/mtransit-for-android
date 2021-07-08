package org.mtransit.android.ui.view.common

import androidx.fragment.app.Fragment

fun Fragment.isAttached(): Boolean {
    return this.isAdded && !this.isDetached && !this.isRemoving
}

fun <T> Fragment.attached(onAttached: () -> (T?)): T? {
    return if (isAttached()) onAttached() else null
}