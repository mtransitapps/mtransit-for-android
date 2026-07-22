package org.mtransit.android.ui.fragment

import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager

fun ABFragment.setupScreenToolbarK(toolbar: Toolbar) {
    abFragmentOnBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        updateScreenToolbarNavigationIcon(toolbar)
    }.also {
        activity?.supportFragmentManager?.addOnBackStackChangedListener(it)
    }
}

fun ABFragment.destroyScreenToolbarK() {
    abFragmentOnBackStackChangedListener?.let {
        activity?.supportFragmentManager?.removeOnBackStackChangedListener(it)
    }
    abFragmentOnBackStackChangedListener = null
}
