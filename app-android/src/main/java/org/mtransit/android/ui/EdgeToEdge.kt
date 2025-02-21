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
import androidx.annotation.ColorRes
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.gms.maps.MapView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.mtransit.android.BuildConfig
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
        edgeToEdgeOptOut()
        return
    }
    enableEdgeToEdge()
}

private fun ComponentActivity.edgeToEdgeOptOut() {
    if (UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    if (BuildConfig.TARGET_SDK_VERSION < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return
    }
    // Call before the DecorView is accessed in setContentView
    theme.applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)
}

fun ViewBinding.applyStatusBarsInsetsEdgeToEdge() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    root.applyStatusBarsInsetsEdgeToEdge()
}

fun View.applyStatusBarsInsetsEdgeToEdge() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    applyWindowInsetsEdgeToEdge(WindowInsetsCompat.Type.statusBars(), consumed = true) { insets ->
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = insets.top
        }
    }
}

fun View.applyWindowInsetsEdgeToEdge(
    @WindowInsetsCompat.Type.InsetsType insetsTypeMask: Int = WindowInsetsCompat.Type.systemBars(),
    consumed: Boolean = false,
    applyInsets: View.(insets: Insets) -> Unit,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(insetsTypeMask)
        applyInsets(view, insets)
        if (consumed) {
            WindowInsetsCompat.CONSUMED
        } else {
            windowInsets
        }
    }
}

// STATUS BAR = TOP BAR
fun Activity.setUpStatusBarBgEdgeToEdge(transparent: Boolean = false) {
    setUpStatusBarBgEdgeToEdge(
        if (transparent) {
            android.R.color.transparent
        } else {
            R.color.color_primary_dark
        }
    )
}

fun Activity.setUpStatusBarBgEdgeToEdge(@ColorRes bgColorRes: Int) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    findViewById<View?>(R.id.status_bar_bg)?.apply {
        setBackgroundColor(ContextCompat.getColor(context, bgColorRes))
        applyStatusBarsHeightEdgeToEdge()
    }
    setStatusBarsThemeEdgeToEdge(isDark = bgColorRes != android.R.color.transparent || isDarkMode(resources))
}

fun Activity.setStatusBarBgColorEdgeToEdge(@ColorInt colorInt: Int) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    findViewById<View?>(R.id.status_bar_bg)?.setBackgroundColor(colorInt)
}

@JvmOverloads
fun Activity.setStatusBarsThemeEdgeToEdge(isDark: Boolean = isDarkMode(resources)) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    WindowCompat.getInsetsController(window, findViewById(android.R.id.content)).apply {
        isAppearanceLightStatusBars = !isDark && false // top bar is always dark
    }
}

@JvmOverloads
fun View.applyStatusBarsHeightEdgeToEdge(@Px initialHeightPx: Int = 0) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    applyWindowInsetsEdgeToEdge(WindowInsetsCompat.Type.statusBars(), consumed = false) { insets ->
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            height = initialHeightPx + insets.height
        }
        isVisible = true
    }
}

@Suppress("DeprecatedCall")
@JvmOverloads
fun MapView.setUpMapEdgeToEdge(
    mapViewController: MapViewController,
    topPaddingSp: Int?,
    bottomPaddingSp: Int?,
    @Px originalHeightPx: Int? = null,
) {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    applyWindowInsetsEdgeToEdge(WindowInsetsCompat.Type.systemBars(), consumed = true) { insets ->
        mapViewController.apply {
            topPaddingSp?.takeIf { it > 0 }?.let {
                setPaddingTopSp(topPaddingSp + insets.top.dpToPx)
            }
            bottomPaddingSp?.takeIf { UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW }?.let {
                setPaddingBottomSp(it + insets.bottom.dpToPx)
            }
            applyPaddings()
        }
        originalHeightPx?.let {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = originalHeightPx + insets.height
            }
        }
    }
}

fun RecyclerView.setUpListEdgeToEdge() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    clipToPadding = false
}

fun ListView.setUpListEdgeToEdge() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    clipToPadding = false
}

fun FloatingActionButton.setUpFabEdgeToEdge() {
    if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    if (!UIFeatureFlags.F_EDGE_TO_EDGE_NAV_BAR_BELOW) {
        return // !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
    }
    // TODO? official doc recommends to use systemBars() and consumed true
    applyWindowInsetsEdgeToEdge(WindowInsetsCompat.Type.navigationBars(), consumed = false) { insets ->
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            startMargin = insets.start
            endMargin = resources.getDimensionPixelSize(R.dimen.fab_margin_end) + insets.end
            bottomMargin = resources.getDimensionPixelSize(R.dimen.fab_margin_bottom) + insets.bottom
        }
    }
}

// NAVIGATION BAR = BOTTOM BAR
// !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
@JvmOverloads
fun Activity.setNavBarThemeEdgeToEdge(isDark: Boolean = isDarkMode(resources)) {
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

fun Activity.setNavBarProtectionEdgeToEdge(contrastEnforced: Boolean = true) {
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
