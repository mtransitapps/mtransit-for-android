@file:JvmName("PreferencesActivity") // ANALYTICS
package org.mtransit.android.ui.pref

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import org.mtransit.android.R
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.enableEdgeToEdgeMT
import org.mtransit.android.ui.setUpStatusBarBgEdgeToEdge
import org.mtransit.android.ui.applyStatusBarsHeightEdgeToEdge
import org.mtransit.android.ui.setStatusBarsThemeEdgeToEdge
import org.mtransit.android.util.NightModeUtils
import javax.inject.Inject

@AndroidEntryPoint
class PreferencesActivity : MTActivity(R.layout.activity_preferences) {

    companion object {
        private val LOG_TAG = PreferencesActivity::class.java.simpleName

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            context: Context,
            showSupport: Boolean = false,
        ): Intent {
            return Intent(context, PreferencesActivity::class.java).apply {
                putExtra(PreferencesViewModel.EXTRA_SUPPORT, showSupport)
            }
        }
    }

    override fun getLogTag(): String = LOG_TAG

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PreferencesActivityEntryPoint {
        val demoModeManager: DemoModeManager // used in attachBaseContext() before @Inject dependencies are available
    }

    private fun getEntryPoint(context: Context): PreferencesActivityEntryPoint {
        return EntryPointAccessors.fromApplication(context.applicationContext, PreferencesActivityEntryPoint::class.java)
    }

    override fun attachBaseContext(newBase: Context) {
        val demoModeManager = getEntryPoint(newBase).demoModeManager
        val fixedBase = if (demoModeManager.isForceLang()) {
            demoModeManager.fixLocale(newBase)
        } else {
            LocaleUtils.attachBaseContextActivity(newBase)
        }
        super.attachBaseContext(fixedBase)
        LocaleUtils.attachBaseContextActivityAfter(this)
    }

    @Inject
    lateinit var demoModeManager: DemoModeManager

    private var currentUiMode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeMT()
        window.decorView // fix random crash (gesture nav back then re-open app)
        super.onCreate(savedInstanceState)
        this.currentUiMode = resources.configuration.uiMode
        LocaleUtils.onCreateActivity(this)
        setUpStatusBarBgEdgeToEdge(android.R.color.black) // good enough for Theme.MaterialComponents.DayNight.DarkActionBar
        findViewById<View?>(R.id.status_bar_bg)?.applyStatusBarsHeightEdgeToEdge()
        setStatusBarsThemeEdgeToEdge(isDark = true)
        supportActionBar?.apply {
            setTitle(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (this.currentUiMode != newConfig.uiMode) {
            NightModeUtils.setDefaultNightMode(context, demoModeManager) // does NOT recreated because uiMode in configChanges AndroidManifest.xml
            NightModeUtils.recreate(this) // not recreated because uiMode in configChanges AndroidManifest.xml
        }
    }

    override fun onResume() {
        super.onResume()
        if (this.currentUiMode != resources.configuration.uiMode) {
            lifecycleScope.launch {
                NightModeUtils.setDefaultNightMode(context, demoModeManager) // does NOT recreated because uiMode in configChanges AndroidManifest.xml
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return when {
            supportFragmentManager.popBackStackImmediate() -> true
            super.onSupportNavigateUp() -> true
            else -> {
                finish() // close screen
                true
            }
        }
    }

    override val currentFragment: Fragment?
        get() = supportFragmentManager.primaryNavigationFragment
}