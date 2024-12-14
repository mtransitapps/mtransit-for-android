package org.mtransit.android.ui

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import org.mtransit.android.R
import org.mtransit.android.util.UIFeatureFlags

/**
 * - https://developer.android.com/develop/ui/views/layout/edge-to-edge
 * - https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually
 *
 * - https://developer.android.com/about/versions/15/behavior-changes-15#:~:text=If%20your%20app%20uses%20views,true%22%20if%20using%20AppBarLayout.
 *
 *  > If your app uses views and Material Components (com.google.android.material),
 *  > most views-based Material Components such as BottomNavigationView, BottomAppBar, NavigationRailView, or NavigationView,
 *  > handle insets and require no additional work.
 *  > However, you need to add android:fitsSystemWindows="true" if using AppBarLayout.`
 *
 * - https://medium.com/androiddevelopers/is-your-app-providing-a-backward-compatible-edge-to-edge-experience-2479267073a0
 * - https://medium.com/androiddevelopers/insets-handling-tips-for-android-15s-edge-to-edge-enforcement-872774e8839b
 *
 * - https://gist.github.com/yaraki/59f72de32d33e2c1b93f702f7fe74958#file-edgetoedge-kt
 */
fun ComponentActivity.enableEdgeToEdgeMT() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)
        return
    }
    enableEdgeToEdge()
}

fun View.setUpEdgeToEdgeTop() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left
            topMargin = insets.top
            rightMargin = insets.right
        }
        WindowInsetsCompat.CONSUMED
    }
}

fun View.setUpEdgeToEdgeBottom() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left
            bottomMargin = insets.bottom
            rightMargin = insets.right
        }
        WindowInsetsCompat.CONSUMED
    }
}

// STATUS BAR = TOP BAR
fun ComponentActivity.setStatusBarColor(transparent: Boolean) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    @Suppress("DEPRECATION") // not working if edge-to-edge
    window.statusBarColor = if (transparent) {
        ResourcesCompat.getColor(resources, android.R.color.transparent, theme)
    } else {
        ResourcesCompat.getColor(resources, R.color.color_primary_dark, theme)
    }
    val isDarkMode = isDarkMode(resources) // always dark top bar
    WindowCompat.getInsetsController(window, findViewById(android.R.id.content)).apply {
        isAppearanceLightStatusBars = transparent && !isDarkMode
    }
}

// NAVIGATION BAR = BOTTOM BAR
fun Activity.setNavigationBarColor(transparent: Boolean) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    @Suppress("DEPRECATION") // not working if edge-to-edge
    window.navigationBarColor = if (transparent) {
        ResourcesCompat.getColor(resources, android.R.color.transparent, theme)
    } else {
        ResourcesCompat.getColor(resources, R.color.color_primary_dark, theme)
    }
    val isDarkMode = isDarkMode(resources)
    WindowCompat.getInsetsController(window, findViewById(android.R.id.content)).apply {
        isAppearanceLightNavigationBars = transparent && !isDarkMode
    }
}

private fun isDarkMode(resources: Resources): Boolean {
    return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}