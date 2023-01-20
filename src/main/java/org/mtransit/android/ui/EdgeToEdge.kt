package org.mtransit.android.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.ComponentActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import org.mtransit.android.R
import org.mtransit.commons.FeatureFlags

/**
 * https://medium.com/androiddevelopers/is-your-app-providing-a-backward-compatible-edge-to-edge-experience-2479267073a0
 * https://gist.github.com/yaraki/59f72de32d33e2c1b93f702f7fe74958#file-edgetoedge-kt
 */
@SuppressLint("ObsoleteSdkInt")
fun ComponentActivity.setUpEdgeToEdge() {
    if (!FeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    val impl = if (Build.VERSION.SDK_INT >= 29) {
        EdgeToEdgeApi29()
    } else if (Build.VERSION.SDK_INT >= 26) {
        EdgeToEdgeApi26()
    } else if (Build.VERSION.SDK_INT >= 23) {
        EdgeToEdgeApi23()
    } else if (Build.VERSION.SDK_INT >= 21) {
        EdgeToEdgeApi21()
    } else {
        EdgeToEdgeBase()
    }
    impl.setUp(window, findViewById(android.R.id.content), theme)
}

fun View.setUpEdgeToEdgeTop() {
    if (!FeatureFlags.F_EDGE_TO_EDGE) {
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
    if (!FeatureFlags.F_EDGE_TO_EDGE) {
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

fun ComponentActivity.setStatusBarColor(transparent: Boolean) {
    if (!FeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    window.statusBarColor = if (transparent) {
        ResourcesCompat.getColor(resources, android.R.color.transparent, theme)
    } else {
        ResourcesCompat.getColor(resources, R.color.color_primary_dark, theme)
    }
    val isDarkMode = isDarkMode(resources)
    WindowInsetsControllerCompat(window, findViewById(android.R.id.content)).apply {
        isAppearanceLightStatusBars = transparent && !isDarkMode
    }
}

fun Activity?.setNavigationBarColor(transparent: Boolean) {
    (this as? ComponentActivity)?.setNavigationBarColor(transparent)
}

fun ComponentActivity.setNavigationBarColor(transparent: Boolean) {
    if (!FeatureFlags.F_EDGE_TO_EDGE) {
        return
    }
    window.navigationBarColor = if (transparent) {
        ResourcesCompat.getColor(resources, android.R.color.transparent, theme)
    } else {
        ResourcesCompat.getColor(resources, R.color.color_primary_dark, theme)
    }
    val isDarkMode = isDarkMode(resources)
    WindowInsetsControllerCompat(window, findViewById(android.R.id.content)).apply {
        isAppearanceLightNavigationBars = transparent && !isDarkMode
    }
}

private fun isDarkMode(resources: Resources): Boolean {
    return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}

private interface EdgeToEdgeImpl {
    fun setUp(window: Window, view: View, theme: Resources.Theme)
}

@RequiresApi(29)
private class EdgeToEdgeApi29 : EdgeToEdgeImpl {

    override fun setUp(window: Window, view: View, theme: Resources.Theme) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val resources = view.resources
        val transparent = ResourcesCompat.getColor(resources, android.R.color.transparent, theme)
        val isDarkMode = isDarkMode(resources)
        window.statusBarColor = transparent
        window.navigationBarColor = transparent
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = !isDarkMode
        controller.isAppearanceLightNavigationBars = !isDarkMode
    }
}

@RequiresApi(26)
private class EdgeToEdgeApi26 : EdgeToEdgeImpl {

    override fun setUp(window: Window, view: View, theme: Resources.Theme) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val resources = view.resources
        val transparent = ResourcesCompat.getColor(resources, android.R.color.transparent, theme)
        val scrim = ResourcesCompat.getColor(resources, R.color.navigation_bar_scrim_light, theme)
        window.statusBarColor = transparent
        window.navigationBarColor = scrim
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }
}

@RequiresApi(23)
private class EdgeToEdgeApi23 : EdgeToEdgeImpl {

    override fun setUp(window: Window, view: View, theme: Resources.Theme) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val resources = view.resources
        val transparent = ResourcesCompat.getColor(resources, android.R.color.transparent, theme)
        window.statusBarColor = transparent
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = true
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(21)
private class EdgeToEdgeApi21 : EdgeToEdgeImpl {

    override fun setUp(window: Window, view: View, theme: Resources.Theme) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }
}

private class EdgeToEdgeBase : EdgeToEdgeImpl {

    override fun setUp(window: Window, view: View, theme: Resources.Theme) {
        // DO NOTHING
    }
}