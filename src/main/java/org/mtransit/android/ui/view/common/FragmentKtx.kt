package org.mtransit.android.ui.view.common

import androidx.fragment.app.Fragment

fun Fragment.isAttached(): Boolean {
    return this.isAdded && !this.isDetached && !this.isRemoving
}
