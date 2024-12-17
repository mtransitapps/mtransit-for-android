package org.mtransit.android.ui

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.Px
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.MapView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.mtransit.android.R
import org.mtransit.android.commons.dpToPx
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.ui.view.common.end
import org.mtransit.android.ui.view.common.endMargin
import org.mtransit.android.ui.view.common.height
import org.mtransit.android.ui.view.common.start
import org.mtransit.android.ui.view.common.startMargin
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

@Suppress("unused")
private const val LOG_TAG = "EdgeToEdge"

fun ComponentActivity.enableEdgeToEdgeMT() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)
        return
    }
    enableEdgeToEdge()
}

@Deprecated("bottom is only used with anchored banner ads and called from ads code")
fun View.setUpEdgeToEdgeBottomAndTop(
    @DimenRes marginTopDimenRes: Int? = null,
    @DimenRes marginBottomDimenRes: Int? = null,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            startMargin = insets.start
            topMargin = insets.top + (marginTopDimenRes?.let { resources.getDimensionPixelSize(it) } ?: 0)
            endMargin = insets.end
            bottomMargin = insets.bottom + (marginBottomDimenRes?.let { resources.getDimensionPixelSize(it) } ?: 0)
        }
        windowInsets
    }
}

@JvmOverloads
fun View.setUpEdgeToEdgeTop(
    @DimenRes marginTopDimenRes: Int? = null,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            startMargin = insets.start
            topMargin = insets.top + (marginTopDimenRes?.let { resources.getDimensionPixelSize(it) } ?: 0)
            endMargin = insets.end
        }
        windowInsets
    }
}

fun View.setUpEdgeToEdgeBottom(
    @Px originalStartMargin: Int = 0,
    @Px originalEndMargin: Int = 0,
    @Px originalBottomMargin: Int = 0,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            startMargin = originalStartMargin + insets.start
            bottomMargin = originalBottomMargin + insets.bottom
            endMargin = originalEndMargin + insets.end
        }
        windowInsets
    }
}

// STATUS BAR = TOP BAR
fun Activity.setStatusBarColor(transparent: Boolean) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    findViewById<View?>(R.id.status_bar_bg)?.setStatusBarColor(transparent)
    val isDarkMode = isDarkMode(resources) // always dark top bar?
    setStatusBarTheme(!(transparent && !isDarkMode))
}


fun Activity.setStatusBarColor(@ColorInt colorInt: Int?) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    colorInt?.let {
        findViewById<View?>(R.id.status_bar_bg)?.setBackgroundColor(colorInt)
    }
}

@JvmOverloads
fun Activity.setStatusBarTheme(isDark: Boolean = isDarkMode(resources)) {
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
        if (transparent) {
            ResourcesCompat.getColor(resources, android.R.color.transparent, context.theme)
        } else {
            ResourcesCompat.getColor(resources, R.color.color_primary_dark, context.theme)
        }
    )
    setStatusBarHeight()
}

@JvmOverloads
fun View.setStatusBarHeight(@Px additionalHeightPx: Int = 0) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            height = insets.height + additionalHeightPx
        }
        windowInsets
    }
    isVisible = true
}

@JvmOverloads
fun MapView.setUpEdgeToEdgeTopMap(
    mapViewController: MapViewController,
    topPaddingSp: Int?,
    bottomPaddingSp: Int?,
    @Px originalHeightPx: Int? = null,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        topPaddingSp?.takeIf { it > 0 }?.let {
            mapViewController.setPaddingTopSp(topPaddingSp + insets.top.dpToPx)
        }
        originalHeightPx?.let {
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = originalHeightPx + insets.height
            }
        }
        bottomPaddingSp
            ?.takeIf { UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW }
            ?.let {
                mapViewController.setPaddingBottomSp(it + insets.bottom.dpToPx)
            }
        mapViewController.applyPaddings()
        windowInsets
    }
}

fun RecyclerView.setUpEdgeToEdgeList(
    @DimenRes marginTopDimenRes: Int?,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginTopDimenRes?.let {
                topMargin = resources.getDimensionPixelSize(it) + insets.top
            }
        }
        view.updatePadding(
            left = insets.start,
            right = insets.end,
            bottom = (insets.bottom.takeIf { UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW } ?: 0),
        )
        windowInsets
    }
    clipToPadding = false
}

fun ListView.setUpEdgeToEdgeList(
    @DimenRes marginTopDimenRes: Int?,
    @DimenRes marginBottomDimenRes: Int?,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginTopDimenRes?.let {
                topMargin = resources.getDimensionPixelSize(it) + insets.top
            }
        view.updatePadding(
            left = insets.start,
            right = insets.end,
            bottom = (insets.bottom.takeIf { UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW } ?: 0)
                    + (marginBottomDimenRes?.let { resources.getDimensionPixelSize(it) } ?: 0),
        )
        windowInsets
    }
    clipToPadding = false
}

fun FloatingActionButton.setUpEdgeToEdge() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    if (!UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW) {
        return // !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
    }
    setUpEdgeToEdgeBottom(
        originalEndMargin = context.resources.getDimensionPixelSize(R.dimen.fab_margin_end),
        originalBottomMargin = context.resources.getDimensionPixelSize(R.dimen.fab_margin_bottom)
    )
}

// NAVIGATION BAR = BOTTOM BAR
// !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
@JvmOverloads
fun Activity.setNavBarTheme(isDark: Boolean = isDarkMode(resources)) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    if (!UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW) {
        return // !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
    }
    WindowCompat.getInsetsController(window, findViewById(android.R.id.content)).apply {
        isAppearanceLightNavigationBars = !isDark
    }
}

private const val ALWAYS_DISABLE_CONTRAST = false
// private const val ALWAYS_DISABLE_CONTRAST = true // DEBUG

fun Activity.setUpNavBarProtection(contrastEnforced: Boolean = true) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    if (!UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW) {
        return // !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.window.isNavigationBarContrastEnforced = contrastEnforced && !ALWAYS_DISABLE_CONTRAST
    }
}

private fun isDarkMode(resources: Resources): Boolean {
    return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
