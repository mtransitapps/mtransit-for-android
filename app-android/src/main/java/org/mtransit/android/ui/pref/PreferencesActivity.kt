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
import org.mtransit.android.ui.edgeToEdgeOptOut
import org.mtransit.android.ui.enableEdgeToEdgeMT
import org.mtransit.android.ui.setUpEdgeToEdgeBottomAndTop
import org.mtransit.android.util.NightModeUtils
import org.mtransit.android.util.UIFeatureFlags
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
        if (UIFeatureFlags.F_EDGE_TO_EDGE) {
            enableEdgeToEdgeMT()
        }
        super.onCreate(savedInstanceState)
        if (!UIFeatureFlags.F_EDGE_TO_EDGE) {
            edgeToEdgeOptOut()
        }
        this.currentUiMode = resources.configuration.uiMode
        LocaleUtils.onCreateActivity(this)
        @Suppress("DEPRECATION", "DeprecatedCall") // no bottom anchored banner ad in this screen
        findViewById<View>(R.id.preferences_fragment).setUpEdgeToEdgeBottomAndTop() // after super.onCreate()
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

    override fun onPostResume() {
        super.onPostResume()
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