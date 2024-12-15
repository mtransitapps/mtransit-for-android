package org.mtransit.android.ui

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.gms.maps.MapView
import org.mtransit.android.R
import org.mtransit.android.commons.px
import org.mtransit.android.ui.view.MapViewController
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

fun ViewGroup.setUpEdgeToEdgeClipToPadding(clipToPadding: Boolean) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    this.clipToPadding = clipToPadding
}

@Deprecated("bottom is only used with anchored banner ads and called from ads code")
fun View.setUpEdgeToEdgeBottomAndTop() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left
            topMargin = insets.top
            rightMargin = insets.right
            bottomMargin = insets.bottom
        }
        WindowInsetsCompat.CONSUMED
    }
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
    findViewById<View?>(R.id.status_bar_bg)?.setStatusBarColor(transparent)
    val isDarkMode = isDarkMode(resources) // always dark top bar?
    setStatusBarTheme(!(UIFeatureFlags.F_EDGE_TO_EDGE_TRANSLUCENT_TOP && transparent && !isDarkMode))
}

fun ComponentActivity.setStatusBarColorRes(@ColorRes colorResId: Int?) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    colorResId?.let {
        setStatusBarColor(ResourcesCompat.getColor(resources, it, theme))
    }
}

fun ComponentActivity.setStatusBarColor(@ColorInt colorInt: Int?) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    colorInt?.let {
        findViewById<View?>(R.id.status_bar_bg)?.setBackgroundColor(colorInt)
    }
}

@JvmOverloads
fun ComponentActivity.setStatusBarTheme(isDark: Boolean = isDarkMode(resources)) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    WindowCompat.getInsetsController(window, findViewById(android.R.id.content)).apply {
        isAppearanceLightStatusBars = !isDark && false // top bar is always dark
    }
}

fun View.setStatusBarColor(transparent: Boolean) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    setBackgroundColor(
        if (UIFeatureFlags.F_EDGE_TO_EDGE_TRANSLUCENT_TOP && transparent) {
            ResourcesCompat.getColor(resources, android.R.color.transparent, context.theme)
        } else {
            ResourcesCompat.getColor(resources, R.color.color_primary_dark, context.theme)
        }
    )
    setStatusBarHeight()
}

@JvmOverloads
fun View.setStatusBarHeight(additionalHeight: Int = 0) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            height = insets.height + additionalHeight
        }
        WindowInsetsCompat.CONSUMED
    }
    isVisible = true
}

@JvmOverloads
fun MapView.setUpEdgeToEdgeTopMap(
    mapViewController: MapViewController,
    topPaddingSp: Int,
    bottomPaddingSp: Int,
    originalHeight: Int? = null,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        if (UIFeatureFlags.F_EDGE_TO_EDGE_TRANSLUCENT_TOP) {
            mapViewController.setPaddingTopSp(topPaddingSp + insets.top.px)
            originalHeight?.let {
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = originalHeight + insets.height
                }
            }
        } else {
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
        }
        if (bottomPaddingSp > 0) {
            mapViewController.setPaddingBottomSp(bottomPaddingSp + insets.bottom.px)
        }
        mapViewController.applyPaddings()
        WindowInsetsCompat.CONSUMED
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

val Insets.height: Int
    get() = top - bottom