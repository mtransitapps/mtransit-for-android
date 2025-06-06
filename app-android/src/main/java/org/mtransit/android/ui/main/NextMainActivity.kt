@file:JvmName("MainActivity") // ANALYTICS
package org.mtransit.android.ui.main

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.IBillingManager.OnBillingResultListener
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.databinding.ActivityMainNextBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.receiver.ModulesReceiver
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.enableEdgeToEdgeMT
import org.mtransit.android.ui.search.SearchFragment
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.BatteryOptimizationIssueUtils
import org.mtransit.android.util.NightModeUtils
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@Suppress("UNUSED_ANONYMOUS_PARAMETER", "unused", "MemberVisibilityCanBePrivate")
@AndroidEntryPoint
class NextMainActivity : MTActivityWithLocation(),
    FragmentManager.OnBackStackChangedListener,
    IAnalyticsManager.Trackable,
    OnBillingResultListener,
    IActivity, IAdScreenActivity,
    IAdManager.RewardedAdListener {

    companion object {
        private val LOG_TAG = "Stack2-" + NextMainActivity::class.java.simpleName
        private const val TRACKING_SCREEN_NAME = "Main"

        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, NextMainActivity::class.java)
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<NextMainViewModel>()

    private lateinit var binding: ActivityMainNextBinding

    val navHostFragment: NavHostFragment
        get() = supportFragmentManager.findFragmentById(R.id.main_content) as NavHostFragment

    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    private var bgDrawable: ColorDrawable? = null
    private val abBgDrawable: ColorDrawable?
        get() {
            return bgDrawable ?: run {
                supportActionBar?.let { ab ->
                    ColorDrawable().apply {
                        bgDrawable = this
                        ab.setBackgroundDrawable(bgDrawable)
                    }
                }
                bgDrawable
            }
        }

    private val defaultBgColor by lazy { ThemeUtils.resolveColorAttribute(context, android.R.attr.colorPrimary) }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MainActivityEntryPoint {
        val demoModeManager: DemoModeManager // used in attachBaseContext() before @Inject dependencies are available
    }

    private fun getEntryPoint(context: Context): MainActivityEntryPoint {
        return EntryPointAccessors.fromApplication(context.applicationContext, MainActivityEntryPoint::class.java)
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
    lateinit var adManager: IAdManager

    @Inject
    lateinit var analyticsManager: IAnalyticsManager

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var billingManager: IBillingManager

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var lclPrefRepository: LocalPreferenceRepository

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    @Inject
    lateinit var demoModeManager: DemoModeManager

    private var currentUiMode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeMT()
        window.decorView // fix random crash (gesture nav back then re-open app)
        super.onCreate(savedInstanceState)
        adManager.init(this)
        NightModeUtils.resetColorCache() // single activity, no cache can be trusted to be from the right theme
        currentUiMode = resources.configuration.uiMode
        LocaleUtils.onCreateActivity(this)
        binding = ActivityMainNextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.abToolbar)
        navController = navHostFragment.navController
        appBarConfig = AppBarConfiguration(
            viewModel.getRootScreenResId(),
            binding.drawerLayout
        )
        binding.navView?.setupWithNavController(navController)
        binding.bottomNavView?.setupWithNavController(navController) // 1st
        binding.bottomNavView?.setOnItemReselectedListener { item -> // 2nd: override to avoid re-selection
            viewModel.onItemReselected()
        }
        // TODO binding.navRailView?.setupWithNavController(navController) // 1st // required Navigation UI 2.4.0 stable https://developer.android.com/jetpack/androidx/releases/navigation
        binding.navRailView?.setOnItemSelectedListener { item -> // 1st
            viewModel.onItemSelected()
            NavigationUI.onNavDestinationSelected(item, navController)
        }
        binding.navRailView?.setOnItemReselectedListener { item -> // 2nd: override to avoid re-selection
            viewModel.onItemReselected()
        }
        setupActionBarWithNavController(navController, appBarConfig)
        navController.addOnDestinationChangedListener { controller, dest, args ->
            viewModel.onItemSelected()
            viewModel.onSelectedItemIdChanged(dest.id)
        }
        supportFragmentManager.addOnBackStackChangedListener(this)
        viewModel.selectedItemIdRes.observe(this) { selectedItemIdRes ->
            selectedItemIdRes?.let {
                val navGraph = navController.navInflater.inflate(R.navigation.nav_graph_main)
                navGraph.setStartDestination(selectedItemIdRes) // FIXME only when new value? ...
                navController.graph = navGraph
                showContentFrameAsLoaded()
            }
        }
        viewModel.showDrawerLearning.observe(this) { showDrawerLearning ->
            if (showDrawerLearning == true) {
                binding.drawerLayout?.let { drawerLayout ->
                    binding.navView?.let { navView ->
                        drawerLayout.openDrawer(navView)
                        viewModel.setUserLearnedDrawer(true)
                    }
                }
            }
        }
        viewModel.hasAgenciesEnabled.observe(this) { hasAgenciesEnabled ->
            // ad-manager does not persist activity but listen for changes itself
            adManager.onHasAgenciesEnabledUpdated(hasAgenciesEnabled, this)
        }
        viewModel.hasAgenciesAdded.observe(this) { hasAgenciesEnabled ->
            if (hasAgenciesEnabled) {
                onHasAgenciesAddedChanged()
            }
        }
        viewModel.abTitle.observe(this) {
            binding.abToolbar.title = it
        }
        viewModel.abSubtitle.observe(this) {
            binding.abToolbar.subtitle = it
        }
        viewModel.abBgColor.observe(this) { newBgColor ->
            abBgDrawable?.color = newBgColor ?: defaultBgColor
        }
        billingManager.currentSubscription.observe(this) {
            // do nothing
        }
        ContextCompat.registerReceiver(this, ModulesReceiver(), ModulesReceiver.getIntentFilter(), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onBillingResult(productId: String?) {
        productId?.isNotEmpty()?.let { hasSubscription ->
            adManager.setShowingAds(!hasSubscription, this)
        }
    }

    override fun onNewIntent(@SuppressLint("UnknownNullness") intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?): Boolean {
        if (Intent.ACTION_SEARCH == intent?.action) {
            onSearchQueryRequested(intent.getStringExtra(SearchManager.QUERY))
            return true // intent processed
        }
        return false // intent not processed
    }

    override fun onSearchRequested(): Boolean {
        onSearchQueryRequested(null)
        return true // processed
    }

    fun onSearchQueryRequested(query: String?) {
        (currentFragment as? SearchFragment)?.apply {
            setSearchQuery(query, false)
        } ?: run {
        }
    }

    override fun onResume() {
        super.onResume()
        adManager.adaptToScreenSize(this, resources.configuration)
        adManager.setRewardedAdListener(this) // used until POI screen is visible // need to pre-load ASAP
        adManager.linkRewardedAd(this)
        billingManager.addListener(this) // trigger onBillingResult() w/ current value
        billingManager.refreshPurchases()
        onLastLocationChanged(deviceLocation)

        isMTResumed = true
        if (currentUiMode != resources.configuration.uiMode) {
            lifecycleScope.launch {
                NightModeUtils.setDefaultNightMode(activity, demoModeManager) // does NOT recreated because uiMode in configChanges AndroidManifest.xml
            }
        }
        viewModel.onAppVisible()
        BatteryOptimizationIssueUtils.onAppResumeInvisibleActivity(
            this,
            this,
            this.dataSourcesRepository,
            this.lclPrefRepository
        )
    }

    override fun onPrivacyOptionsRequiredChanged() {
        // TODO later
    }

    override fun onRewardedAdStatusChanged() {
        // DO NOTHING
    }

    override fun skipRewardedAd(): Boolean {
        return adManager.shouldSkipRewardedAd()
    }

    var isMTResumed = false

    override fun onPause() {
        super.onPause()
        isMTResumed = false
        billingManager.removeListener(this)
        adManager.pauseAd(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        adManager.destroyAd(this)
        adManager.unlinkRewardedAd(this)
    }

    fun showContentFrameAsLoading() {
    }

    fun showContentFrameAsLoaded() {
    }

    override fun onLastLocationChanged(lastLocation: Location?) {
        broadcastDeviceLocationChanged(this, navHostFragment.childFragmentManager.fragments, lastLocation)
    }

    private var locationSettingsResolution: PendingIntent? = null

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        this.locationSettingsResolution = resolution
        broadcastLocationSettingsResolutionChanged(this, navHostFragment.childFragmentManager.fragments, resolution)
    }

    override fun getLastLocationSettingsResolution() = this.locationSettingsResolution

    override val currentFragment: Fragment?
        get() = supportFragmentManager.primaryNavigationFragment // TODO ?

    override fun onBackStackChanged() {
        resetBackStackEntryCount()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }

    fun updateNavigationDrawerToggleIndicator() {
    }

    fun enableNavigationDrawerToggleIndicator() {
    }

    private val _backStackEntryCount: AtomicInteger by lazy { AtomicInteger(supportFragmentManager.backStackEntryCount) }
    val backStackEntryCount: Int
        get() = _backStackEntryCount.get()

    private fun resetBackStackEntryCount() {
        _backStackEntryCount.set(supportFragmentManager.backStackEntryCount)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (this.currentUiMode != newConfig.uiMode) {
            NightModeUtils.setDefaultNightMode(context, demoModeManager) // does NOT recreated because uiMode in configChanges AndroidManifest.xml
            NightModeUtils.recreate(this) // not recreated because uiMode in configChanges AndroidManifest.xml
            return
        }
        adManager.adaptToScreenSize(this, newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }
}