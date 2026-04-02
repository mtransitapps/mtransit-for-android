package org.mtransit.android.ui

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.lang.ref.WeakReference

class DrawerLayoutOnBackPressedCallback(
    drawerLayout: DrawerLayout,
) : OnBackPressedCallback(enabled = false),
    DrawerLayout.DrawerListener {

    init {
        drawerLayout.addDrawerListener(this)
    }

    private val drawerLayoutWR = WeakReference(drawerLayout)

    override fun handleOnBackPressed() {
        this.drawerLayoutWR.get()?.apply {
            if (isDrawerOpen(GravityCompat.START)) {
                closeDrawer(GravityCompat.START)
            }
        }
    }

    override fun onDrawerOpened(p0: View) {
        isEnabled = true
    }

    override fun onDrawerClosed(p0: View) {
        isEnabled = false
    }

    override fun onDrawerStateChanged(newState: Int) {
        // DO NOTHING
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        // DO NOTHING
    }
}
