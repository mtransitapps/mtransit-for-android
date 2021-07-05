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
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.IBillingManager.OnBillingResultListener
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.search.SearchFragment
import org.mtransit.android.ui.search.SearchFragment.Companion.newInstance
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.MapUtils
import org.mtransit.android.util.NightModeUtils
import java.util.WeakHashMap
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
        private val LOG_TAG = "Stack-" + MainActivity::class.java.simpleName
        private const val TRACKING_SCREEN_NAME = "Main"

        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    private val _navigationDrawerController: NavigationDrawerController by lazy {
        NavigationDrawerController(
            this,
            crashReporter,
            analyticsManager,
            dataSourcesRepository,
            statusLoader,
            serviceUpdateLoader,
        )
    }
    val navigationDrawerController = _navigationDrawerController

    private val _abController: ActionBarController by lazy {
        ActionBarController(
            this
        )
    }
    val abController = _abController

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
        setContentView(R.layout.activity_main)
        //        abController = ActionBarController(this)
        //        _navigationDrawerController = NavigationDrawerController(
        //            this,
        //            crashReporter,
        //            analyticsManager,
        //            dataSourcesRepository,
        //            statusLoader,
        //            serviceUpdateLoader,
        //        )
        _navigationDrawerController.onCreate(savedInstanceState)
        supportFragmentManager.addOnBackStackChangedListener(this)
        dataSourcesRepository.readingAllAgenciesCount().observe(this, { nbAgencies ->
            // ad-manager does not persist activity but listen for changes itself
            nbAgencies?.let {
                adManager.onNbAgenciesUpdated(this, nbAgencies)
            }
        })
        MapUtils.fixScreenFlickering(findViewById(R.id.main_content))
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
            addFragmentToStack(newInstance(query))
        }
    }

    override fun onStart() {
        super.onStart()
        _navigationDrawerController.onStart()
    }

    override fun onResume() {
        super.onResume()
        _abController.updateAB()
        _navigationDrawerController.onResume()
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
        try {
            lifecycleScope.launchWhenCreated {
                withContext(Dispatchers.IO) {
                    dataSourcesRepository.updateLock()
                }
            }
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while updating data-sources from repository!")
        }
    }

    var isMTResumed = false
        private set

    override fun onPause() {
        super.onPause()
        isMTResumed = false
        _navigationDrawerController.onPause()
        billingManager.removeListener(this)
        adManager.pauseAd(this)
    }

    override fun onStop() {
        super.onStop()
        _navigationDrawerController.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        popFragmentsToPop()
    }

    override fun onDestroy() {
        super.onDestroy()
        _abController.destroy()
        // abController = null
        _navigationDrawerController.destroy()
        // _navigationDrawerController = null
        fragmentsToPopWR.clear()
        adManager.destroyAd(this)
        adManager.unlinkRewardedAd(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        _navigationDrawerController.onSaveState(outState)
        _abController.onSaveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        _navigationDrawerController.onRestoreState(savedInstanceState)
        _abController.onRestoreState(savedInstanceState)
    }

    fun clearFragmentBackStackImmediate() {
        FragmentUtils.clearFragmentBackStackImmediate(this, null)
    }

    @JvmOverloads
    fun showNewFragment(
        newFragment: ABFragment,
        addToStack: Boolean,
        optSource: Fragment?,
        optTransitionSharedElement: View? = null,
        optTransitionName: String? = null
    ) {
        MTLog.d(this, "showNewFragment(%s, %s, %s, %s, %s)", newFragment, addToStack, optSource, optTransitionSharedElement, optTransitionName)
        FragmentUtils.replaceFragment(this, R.id.main_content, newFragment, addToStack, optSource, optTransitionSharedElement, optTransitionName)
        if (addToStack) {
            incBackEntryCount()
        }
        showContentFrameAsLoaded()
        FragmentUtils.executePendingTransactions(this, null)
        _abController.setAB(newFragment)
        _abController.updateABDrawerClosed()
        _navigationDrawerController.setCurrentSelectedItemChecked(backStackEntryCount == 0)
    }

    fun showContentFrameAsLoaded() {
        findViewById<View>(R.id.main_content_loading)?.visibility = View.GONE
        findViewById<View>(R.id.main_content)?.visibility = View.VISIBLE
    }

    fun addFragmentToStack( // called from JAVA
        newFragment: ABFragment,
        optTransitionSharedElement: View?
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
        optTransitionName: String? = optTransitionSharedElement?.transitionName
    ) {
        showNewFragment(newFragment, true, optSource, optTransitionSharedElement, optTransitionName)
    }

    override fun onLastLocationChanged(lastLocation: Location?) {
        broadcastUserLocationChanged(this, fragments, lastLocation)
    }

    fun isCurrentFragmentVisible(fragment: Fragment?): Boolean {
        return FragmentUtils.isCurrentFragmentVisible(this, R.id.main_content, fragment)
    }

    private val currentFragment: Fragment?
        get() = FragmentUtils.getFragment(this, R.id.main_content)

    override fun onBackStackChanged() {
        resetBackStackEntryCount()
        _abController.setAB(currentFragment as ABFragment?)
        _abController.updateABDrawerClosed()
        _navigationDrawerController.onBackStackChanged(backStackEntryCount)
    }

    override fun onBackPressed() {
        if (_navigationDrawerController.onBackPressed()) {
            return
        }
        if ((currentFragment as? ABFragment)?.onBackPressed() == true) {
            return
        }
        super.onBackPressed()
    }

    fun updateNavigationDrawerToggleIndicator() {
        _navigationDrawerController.setDrawerToggleIndicatorEnabled(backStackEntryCount < 1)
    }

    fun enableNavigationDrawerToggleIndicator() {
        _navigationDrawerController.setDrawerToggleIndicatorEnabled(true)
    }

    val isDrawerOpen: Boolean
        get() = _navigationDrawerController.isDrawerOpen

    private val _backStackEntryCount: AtomicInteger by lazy { AtomicInteger(supportFragmentManager.backStackEntryCount) }
    val backStackEntryCount: Int
        get() = _backStackEntryCount.get()

    //    fun getBackStackEntryCount(): Int {
    //        if (backStackEntryCount == null) {
    //            initBackStackEntryCount()
    //        }
    //        return backStackEntryCount
    //    }
    //
    //    private fun initBackStackEntryCount(): Int {
    //        //        backStackEntryCount = supportFragmentManager.backStackEntryCount
    //        //        return backStackEntryCount
    //        return supportFragmentManager.backStackEntryCount.also {
    //            backStackEntryCount = it
    //        }
    //    }
    //
    private fun resetBackStackEntryCount() {
        //  backStackEntryCount = null
        _backStackEntryCount.set(supportFragmentManager.backStackEntryCount)
    }

    private fun incBackEntryCount(): Int {
        // backStackEntryCount = backStackEntryCount?.let { it + 1 } ?: initBackStackEntryCount()
        return _backStackEntryCount.incrementAndGet()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        _abController.onCreateOptionsMenu(menu, menuInflater)
        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        _navigationDrawerController.onActivityPostCreate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (currentUiMode != newConfig.uiMode) {
            NightModeUtils.resetColorCache()
            NightModeUtils.recreate(this)
            return
        }
        _navigationDrawerController.onConfigurationChanged(newConfig)
        adManager.adaptToScreenSize(this, newConfig)
    }

    private val fragmentsToPopWR = WeakHashMap<Fragment, Any>()
    fun popFragmentFromStack(fragment: Fragment?) {
        FragmentUtils.popFragmentFromStack(this, fragment, null)
    }

    private fun popFragmentsToPop() {
        try {
            for (fragment in fragmentsToPopWR.keys) {
                popFragmentFromStack(fragment)
            }
            fragmentsToPopWR.clear()
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while pop-ing fragments to pop from stack!")
        }
    }

    fun onUpIconClick(): Boolean {
        return FragmentUtils.popLatestEntryFromStack(this, null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            _navigationDrawerController.onOptionsItemSelected(item) -> {
                true // handled
            }
            _abController.onOptionsItemSelected(item) -> {
                true // handled
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}