@file:JvmName("MainActivity") // ANALYTICS
package org.mtransit.android.ui

import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.IBillingManager.OnBillingResultListener
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.receiver.ModulesReceiver
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.search.SearchFragment
import org.mtransit.android.ui.search.SearchFragment.Companion.newInstance
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.BatteryOptimizationIssueUtils
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.MapUtils
import org.mtransit.android.util.NightModeUtils
import java.lang.Exception
import java.util.WeakHashMap
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : MTActivityWithLocation(),
    FragmentManager.OnBackStackChangedListener,
    IAnalyticsManager.Trackable,
    OnBillingResultListener,
    IActivity, IAdScreenActivity,
    MTLog.Loggable,
    IAdManager.RewardedAdListener {

    companion object {
        private val LOG_TAG = "Stack-" + MainActivity::class.java.getSimpleName()

        private const val TRACKING_SCREEN_NAME = "Main"

        fun newInstance(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    override fun getLogTag() = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    private var navigationDrawerController: NavigationDrawerController? = null

    var abController: ActionBarController? = null

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface MainActivityEntryPoint {
        fun demoModeManager(): DemoModeManager // used in attachBaseContext() before @Inject dependencies are available
    }

    private fun getEntryPoint(context: Context): MainActivityEntryPoint {
        return EntryPointAccessors.fromApplication(context.applicationContext, MainActivityEntryPoint::class.java)
    }

    protected override fun attachBaseContext(newBase: Context) {
        val demoModeManager = getEntryPoint(newBase).demoModeManager()
        var fixedBase = if (demoModeManager.isForceLang()) {
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
        this.setUpEdgeToEdge()
        super.onCreate(savedInstanceState)
        adManager.init(this)
        NightModeUtils.resetColorCache() // single activity, no cache can be trusted to be from the right theme
        this.currentUiMode = getResources().configuration.uiMode
        LocaleUtils.onCreateActivity(this)
        setContentView(R.layout.activity_main_old)
        this.abController = ActionBarController(this)
        this.navigationDrawerController = NavigationDrawerController(
            this,
            this.crashReporter,
            this.analyticsManager,
            this.dataSourcesRepository,
            this.statusLoader,
            this.packageManager,
            this.serviceUpdateLoader,
            this.demoModeManager
        ).also { navigationDrawerController ->
            navigationDrawerController.onCreate(savedInstanceState)
        }
        supportFragmentManager.addOnBackStackChangedListener(this)
        this.dataSourcesRepository.readingHasAgenciesEnabled().observe(this, Observer { hasAgenciesEnabled: Boolean? ->
            this.adManager.onHasAgenciesEnabledUpdated(
                hasAgenciesEnabled,
                this,
            ) // ad-manager does not persist activity but listen for changes itself
            this.abController?.onHasAgenciesEnabledUpdated(hasAgenciesEnabled)
        })
        this.dataSourcesRepository.readingHasAgenciesAdded().observe(this, Observer { hasAgenciesAdded: Boolean? ->
            if (hasAgenciesAdded == true) {
                onHasAgenciesAddedChanged()
            }
        })
        this.billingManager.currentSubscription.observe(this, Observer { currentSubscription: String? -> })
        MapUtils.fixScreenFlickering(findViewById<ViewGroup?>(R.id.content_frame))
        ContextCompat.registerReceiver(this, ModulesReceiver(), ModulesReceiver.getIntentFilter(), ContextCompat.RECEIVER_NOT_EXPORTED) // Android 13
        findViewById<View?>(R.id.drawer_layout).setUpEdgeToEdgeTop()
    }

    override fun onBillingResult(productId: String?) {
        val hasSubscription = if (productId == null) null else !productId.isEmpty()
        if (hasSubscription != null) {
            this.adManager.setShowingAds(!hasSubscription, this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?): Boolean {
        if (intent != null && Intent.ACTION_SEARCH == intent.action) {
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
        val currentFragment = currentFragment
        if (currentFragment is SearchFragment) {
            currentFragment.setSearchQuery(query, false)
        } else {
            addFragmentToStack(newInstance(query, null), currentFragment)
        }
    }

    protected override fun onStart() {
        super.onStart()
        this.navigationDrawerController?.onStart()
    }

    protected override fun onResume() {
        super.onResume()
        this.abController?.updateAB()
        this.navigationDrawerController?.onResume()
        this.adManager.adaptToScreenSize(this)
        this.adManager.setRewardedAdListener(this) // used until POI screen is visible // need to pre-load ASAP
        this.adManager.linkRewardedAd(this)
        this.billingManager.addListener(this) // trigger onBillingResult() w/ current value
        this.billingManager.refreshPurchases()
        onLastLocationChanged(deviceLocation)
    }

    override fun onRewardedAdStatusChanged() {
        // DO NOTHING
    }

    override fun skipRewardedAd() = this.adManager.shouldSkipRewardedAd()

    protected override fun onPostResume() {
        super.onPostResume()
        this.resumed = true
        if (this.currentUiMode != getResources().configuration.uiMode) {
            Handler(Looper.getMainLooper()).post(Runnable {
                NightModeUtils.setDefaultNightMode(requireContext(), demoModeManager) // does NOT recreated because uiMode in configChanges AndroidManifest.xml
            })
        }
        this.lifecycleScope.launch(Dispatchers.IO) {
            dataSourcesRepository.updateLock()
        }
        BatteryOptimizationIssueUtils.onAppResumeInvisibleActivity(
            this,
            this,
            this.dataSourcesRepository,
            this.lclPrefRepository
        )
        updateNavigationDrawerToggleIndicator()
    }

    private var resumed = false

    fun isMTResumed(): Boolean {
        return this.resumed
    }

    protected override fun onPause() {
        super.onPause()
        this.resumed = false
        this.navigationDrawerController?.onPause()
        this.billingManager.removeListener(this)
        this.adManager.pauseAd(this)
    }

    protected override fun onStop() {
        super.onStop()
        this.navigationDrawerController?.onStop()
    }

    protected override fun onRestart() {
        super.onRestart()
        popFragmentsToPop()
    }

    protected override fun onDestroy() {
        super.onDestroy()
        this.abController?.destroy()
        this.abController = null
        this.navigationDrawerController?.destroy()
        this.navigationDrawerController = null
        this.fragmentsToPopWR.clear()
        this.adManager.destroyAd(this)
        this.adManager.unlinkRewardedAd(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        this.navigationDrawerController?.onSaveState(outState)
        this.abController?.onSaveState(outState)
        super.onSaveInstanceState(outState)
    }

    protected override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        this.navigationDrawerController?.onRestoreState(savedInstanceState)
        this.abController?.onRestoreState(savedInstanceState)
    }

    fun clearFragmentBackStackImmediate() {
        FragmentUtils.clearFragmentBackStackImmediate(this, null)
    }

    fun showNewFragment(
        newFragment: ABFragment,
        addToStack: Boolean,
        optSource: Fragment?,
    ) {
        showNewFragment(newFragment, addToStack, optSource, null, null)
    }

    private fun showNewFragment(
        newFragment: ABFragment,
        addToStack: Boolean,
        optSource: Fragment?,
        optTransitionSharedElement: View?,
        optTransitionName: String?,
    ) {
        MTLog.d(this, "showNewFragment(%s, %s, %s, %s, %s)", newFragment, addToStack, optSource, optTransitionSharedElement, optTransitionName)
        FragmentUtils.replaceFragment(this, R.id.content_frame, newFragment, addToStack, optSource, optTransitionSharedElement, optTransitionName)
        if (addToStack) {
            incBackEntryCount()
        }
        showContentFrameAsLoaded()
        this.abController?.apply {
            FragmentUtils.executePendingTransactions(this@MainActivity, null)
            setAB(newFragment)
            updateABDrawerClosed()
        }
        this.navigationDrawerController?.setCurrentSelectedItemChecked(getBackStackEntryCount() == 0)
    }

    fun showContentFrameAsLoaded() {
        findViewById<View?>(R.id.content_frame_loading)?.isVisible = false
        findViewById<View?>(R.id.content_frame)?.isVisible = true
    }

    fun addFragmentToStack(
        newFragment: ABFragment,
        optTransitionSharedElement: View?,
    ) {
        addFragmentToStack(
            newFragment,
            null,
            optTransitionSharedElement
        )
    }

    @JvmOverloads
    fun addFragmentToStack(
        newFragment: ABFragment,
        optSource: Fragment? = currentFragment,
        optTransitionSharedElement: View? = null,
        optTransitionName: String? = optTransitionSharedElement?.transitionName,
    ) {
        showNewFragment(newFragment, true, optSource, optTransitionSharedElement, optTransitionName)
    }

    override fun onLastLocationChanged(lastLocation: Location?) {
        broadcastDeviceLocationChanged(this, supportFragmentManager.fragments, lastLocation)
    }

    private var locationSettingsResolution: PendingIntent? = null

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        this.locationSettingsResolution = resolution
        broadcastLocationSettingsResolutionChanged(this, supportFragmentManager.fragments, resolution)
    }

    override fun getLastLocationSettingsResolution(): PendingIntent? {
        return this.locationSettingsResolution
    }

    fun isCurrentFragmentVisible(fragment: Fragment?): Boolean {
        return FragmentUtils.isCurrentFragmentVisible(this, R.id.content_frame, fragment)
    }
    
    override val currentFragment: Fragment?
        get() = FragmentUtils.getFragment(this, R.id.content_frame)

    private val currentABFragment: ABFragment?
        get() = currentFragment as? ABFragment

    override fun onBackStackChanged() {
        resetBackStackEntryCount()
        abController?.apply {
            setAB(currentFragment as ABFragment?)
            updateABDrawerClosed()
        }
        this.navigationDrawerController?.onBackStackChanged(getBackStackEntryCount())
        this.adManager.adaptToScreenSize(this, getResources().configuration)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (this.navigationDrawerController?.onBackPressed() == true) {
            return
        }
        if (currentABFragment?.onBackPressed() == true) {
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    fun updateNavigationDrawerToggleIndicator() {
        this.navigationDrawerController?.setDrawerToggleIndicatorEnabled(getBackStackEntryCount() < 1)
    }

    @Suppress("unused")
    fun enableNavigationDrawerToggleIndicator() {
        this.navigationDrawerController?.setDrawerToggleIndicatorEnabled(true)
    }

    @Suppress("unused")
    fun isDrawerOpen(): Boolean {
        return this.navigationDrawerController?.isDrawerOpen == true
    }

    private var backStackEntryCount: Int? = null

    fun getBackStackEntryCount(): Int {
        return this.backStackEntryCount
            ?: supportFragmentManager.backStackEntryCount.also { this.backStackEntryCount = it }
    }

    private fun resetBackStackEntryCount() {
        this.backStackEntryCount = null
    }

    private fun incBackEntryCount() {
        this.backStackEntryCount = getBackStackEntryCount() + 1
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        this.abController?.onCreateOptionsMenu(menu, menuInflater)
        return true
    }

    protected override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        this.navigationDrawerController?.onActivityPostCreate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (this.currentUiMode != newConfig.uiMode) {
            NightModeUtils.setDefaultNightMode(requireContext(), demoModeManager) // does NOT recreated because uiMode in configChanges AndroidManifest.xml
            NightModeUtils.recreate(this) // not recreated because uiMode in configChanges AndroidManifest.xml
            return
        }
        this.navigationDrawerController?.onConfigurationChanged(newConfig)
        this.adManager.adaptToScreenSize(this, newConfig)
    }

    private val fragmentsToPopWR = WeakHashMap<Fragment?, Any?>()

    fun popFragmentFromStack(fragment: Fragment?) {
        FragmentUtils.popFragmentFromStack(this, fragment, null)
    }

    private fun popFragmentsToPop() {
        try {
            for (fragment in this.fragmentsToPopWR.keys) {
                popFragmentFromStack(fragment)
            }
            this.fragmentsToPopWR.clear()
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while pop-ing fragments to pop from stack!")
        }
    }

    fun onUpIconClick(): Boolean {
        return FragmentUtils.popLatestEntryFromStack(this, null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (this.navigationDrawerController?.onOptionsItemSelected(item) == true) {
            return true // handled
        }
        if (this.abController?.onOptionsItemSelected(item) == true) {
            return true // handled
        }
        return super.onOptionsItemSelected(item)
    }
}
