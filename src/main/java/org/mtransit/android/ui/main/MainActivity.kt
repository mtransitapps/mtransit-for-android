@file:JvmName("MainActivity") // ANALYTICS
package org.mtransit.android.ui.main

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.IBillingManager.OnBillingResultListener
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.databinding.ActivityMainBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.search.SearchFragment
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.NightModeUtils
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : MTActivityWithLocation(),
    FragmentManager.OnBackStackChangedListener,
    IAnalyticsManager.Trackable,
    OnBillingResultListener,
    IActivity,
    IAdManager.RewardedAdListener {

    companion object {
        private val LOG_TAG = "Stack2-" + MainActivity::class.java.simpleName
        private const val TRACKING_SCREEN_NAME = "Main"

        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var binding: ActivityMainBinding


    val navHostFragment: NavHostFragment
        get() = supportFragmentManager.findFragmentById(R.id.main_content) as NavHostFragment

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

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
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var currentUiMode = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adManager.init(this)
        NightModeUtils.resetColorCache() // single activity, no cache can be trusted to be from the right theme
        currentUiMode = resources.configuration.uiMode
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        navController = navHostFragment.navController
        binding.navView.setupWithNavController(navController)
        appBarConfiguration = AppBarConfiguration(
            viewModel.getRootScreenResId(),
            binding.drawerLayout
        )
        setSupportActionBar(binding.abToolbar)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { controller, dest, args ->
            MTLog.v(this@MainActivity, "onDestinationChanged($controller, $dest, $args)")
            viewModel.onSelectedItemIdChanged(dest.id, args)
        }
        supportFragmentManager.addOnBackStackChangedListener(this)

        viewModel.selectedItemIdRes.observe(this, { selectedItemIdRes ->
            selectedItemIdRes?.let {
                val navGraph = navController.navInflater.inflate(R.navigation.main_nav_graph)
                navGraph.startDestination = selectedItemIdRes // FIXME only when new value? ...
                navController.graph = navGraph
                showContentFrameAsLoaded()
            }
        })
        viewModel.userLearnedDrawer.observe(this, {
            if (it == false) {
                binding.drawerLayout.openDrawer(binding.navView)
                viewModel.setUserLearnedDrawer(true)
            }
        })

        viewModel.allAgenciesCount.observe(this, { nbAgencies ->
            // ad-manager does not persist activity but listen for changes itself
            nbAgencies?.let {
                adManager.onNbAgenciesUpdated(this, nbAgencies)
            }
        })
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.fixDefaultLocale(newBase))
    }

    override fun onBillingResult(sku: String?) {
        sku?.isNotEmpty()?.let { hasSubscription ->
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
        // val currentFragment = currentFragment
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
        onLastLocationChanged(userLocation)
    }

    override fun onRewardedAdStatusChanged() {
        // DO NOTHING
    }

    override fun skipRewardedAd(): Boolean {
        return adManager.shouldSkipRewardedAd()
    }

    override fun onPostResume() {
        super.onPostResume()
        isMTResumed = true
        if (currentUiMode != resources.configuration.uiMode) {
            lifecycleScope.launchWhenResumed {
                NightModeUtils.resetColorCache()
                NightModeUtils.recreate(this@MainActivity)
            }
        }
        viewModel.onAppVisible()
    }

    var isMTResumed = false
        private set

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
        binding.mainContent.isVisible = false
        binding.mainContentLoading.root.isVisible = true
    }

    fun showContentFrameAsLoaded() {
        binding.mainContentLoading.root.isVisible = false
        binding.mainContent.isVisible = true
    }

    override fun onLastLocationChanged(lastLocation: Location?) {
        broadcastUserLocationChanged(this, navHostFragment.childFragmentManager.fragments, lastLocation)
    }

    private val currentFragment: Fragment?
        get() = supportFragmentManager.primaryNavigationFragment // TODO ?

    override fun onBackStackChanged() {
        resetBackStackEntryCount()
    }

    override fun onSupportNavigateUp(): Boolean {
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
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (currentUiMode != newConfig.uiMode) {
            NightModeUtils.resetColorCache()
            NightModeUtils.recreate(this)
            return
        }
        adManager.adaptToScreenSize(this, newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }
}