package org.mtransit.android.ui;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.analytics.AnalyticsManager;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.billing.IBillingManager;
import org.mtransit.android.common.MTContinuationJ;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.receiver.ModulesReceiver;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.search.SearchFragment;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.android.util.FragmentUtils;
import org.mtransit.android.util.MapUtils;
import org.mtransit.android.util.NightModeUtils;

import java.util.WeakHashMap;

import javax.inject.Inject;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

@AndroidEntryPoint
public class MainActivity extends MTActivityWithLocation implements
		FragmentManager.OnBackStackChangedListener,
		AnalyticsManager.Trackable,
		IBillingManager.OnBillingResultListener,
		IActivity,
		IAdManager.RewardedAdListener {

	private static final String TAG = "Stack-" + MainActivity.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Main";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	@NonNull
	public static Intent newInstance(@NonNull Context context) {
		return new Intent(context, MainActivity.class);
	}

	@Nullable
	private NavigationDrawerController navigationDrawerController;

	@Nullable
	private ActionBarController abController;

	@EntryPoint
	@InstallIn(SingletonComponent.class)
	interface MainActivityEntryPoint {
		DemoModeManager demoModeManager(); // used in attachBaseContext() before @Inject dependencies are available
	}

	@NonNull
	private MainActivityEntryPoint getEntryPoint(@NonNull Context context) {
		return EntryPointAccessors.fromApplication(context.getApplicationContext(), MainActivityEntryPoint.class);
	}

	@Override
	protected void attachBaseContext(@NonNull Context newBase) {
		final DemoModeManager demoModeManager = getEntryPoint(newBase).demoModeManager();
		Context fixedBase;
		if (demoModeManager.getEnabled()) {
			fixedBase = demoModeManager.fixLocale(newBase);
		} else {
			fixedBase = LocaleUtils.attachBaseContextActivity(newBase);
		}
		super.attachBaseContext(fixedBase);
		LocaleUtils.attachBaseContextActivityAfter(this);
	}

	@Inject
	IAdManager adManager;
	@Inject
	IAnalyticsManager analyticsManager;
	@Inject
	CrashReporter crashReporter;
	@Inject
	IBillingManager billingManager;
	@Inject
	DataSourcesRepository dataSourcesRepository;
	@Inject
	StatusLoader statusLoader;
	@Inject
	ServiceUpdateLoader serviceUpdateLoader;
	@Inject
	DemoModeManager demoModeManager;

	private int currentUiMode = -1;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adManager.init(this);
		NightModeUtils.resetColorCache(); // single activity, no cache can be trusted to be from the right theme
		this.currentUiMode = getResources().getConfiguration().uiMode;
		LocaleUtils.onCreateActivity(this);
		setContentView(R.layout.activity_main_old);
		this.abController = new ActionBarController(this);
		this.navigationDrawerController = new NavigationDrawerController(
				this,
				this.crashReporter,
				this.analyticsManager,
				this.dataSourcesRepository,
				this.statusLoader,
				this.serviceUpdateLoader,
				this.demoModeManager
		);
		this.navigationDrawerController.onCreate(savedInstanceState);
		getSupportFragmentManager().addOnBackStackChangedListener(this);
		this.dataSourcesRepository.readingAllAgenciesCount().observe(this, nbAgencies ->
				this.adManager.onNbAgenciesUpdated(this, nbAgencies) // ad-manager does not persist activity but listen for changes itself
		);
		MapUtils.fixScreenFlickering(findViewById(R.id.content_frame));
		registerReceiver(new ModulesReceiver(), ModulesReceiver.getIntentFilter()); // Android 12
	}

	@Override
	public void onBillingResult(@Nullable String sku) {
		Boolean hasSubscription = sku == null ? null : !sku.isEmpty();
		if (hasSubscription != null) {
			this.adManager.setShowingAds(!hasSubscription, this);
		}
	}

	@Nullable
	public ActionBarController getAbController() {
		return abController;
	}

	@Override
	protected void onNewIntent(@SuppressLint("UnknownNullness") Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		processIntent(intent);
	}

	@SuppressWarnings("UnusedReturnValue")
	private boolean processIntent(@Nullable Intent intent) {
		if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
			onSearchQueryRequested(intent.getStringExtra(SearchManager.QUERY));
			return true; // intent processed
		}
		return false; // intent not processed
	}

	@Override
	public boolean onSearchRequested() {
		onSearchQueryRequested(null);
		return true; // processed
	}

	public void onSearchQueryRequested(@Nullable String query) {
		Fragment currentFragment = getCurrentFragment();
		if (currentFragment instanceof SearchFragment) {
			((SearchFragment) currentFragment).setSearchQuery(query, false);
		} else {
			addFragmentToStack(SearchFragment.newInstance(query, null), currentFragment);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onStart();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.abController != null) {
			this.abController.updateAB();
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onResume();
		}
		this.adManager.adaptToScreenSize(this, getResources().getConfiguration());
		this.adManager.setRewardedAdListener(this); // used until POI screen is visible // need to pre-load ASAP
		this.adManager.linkRewardedAd(this);
		this.billingManager.addListener(this); // trigger onBillingResult() w/ current value
		this.billingManager.refreshPurchases();
		onLastLocationChanged(getUserLocation());
	}

	@Override
	public void onRewardedAdStatusChanged() {
		// DO NOTHING
	}

	@Override
	public boolean skipRewardedAd() {
		return this.adManager.shouldSkipRewardedAd();
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		this.resumed = true;
		if (this.currentUiMode != getResources().getConfiguration().uiMode) {
			new Handler().post(() -> {
				NightModeUtils.setDefaultNightMode(requireContext(), demoModeManager); // does NOT recreated because uiMode in configChanges AndroidManifest.xml
			});
		}
		try {
			this.dataSourcesRepository.updateLock(new MTContinuationJ<Boolean>() {

				@NonNull
				@Override
				public CoroutineContext getContext() {
					return EmptyCoroutineContext.INSTANCE;
				}

				@Override
				public void resumeWithException(@NonNull Throwable t) {
					MTLog.w(MainActivity.this, t, "Error while running update...");
				}

				@Override
				public void resume(Boolean result) {
					MTLog.d(MainActivity.this, "Update run with result: %s", result);
				}
			});
		} catch (Exception e) {
			MTLog.w(this, e, "Error while updating data-sources from repository!");
		}
	}

	private boolean resumed = false;

	public boolean isMTResumed() {
		return this.resumed;
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.resumed = false;
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onPause();
		}
		this.billingManager.removeListener(this);
		this.adManager.pauseAd(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onStop();
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		popFragmentsToPop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (this.abController != null) {
			this.abController.destroy();
			this.abController = null;
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.destroy();
			this.navigationDrawerController = null;
		}
		this.fragmentsToPopWR.clear();
		this.adManager.destroyAd(this);
		this.adManager.unlinkRewardedAd(this);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onSaveState(outState);
		}
		if (this.abController != null) {
			this.abController.onSaveState(outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onRestoreState(savedInstanceState);
		}
		if (this.abController != null) {
			this.abController.onRestoreState(savedInstanceState);
		}
	}

	public void clearFragmentBackStackImmediate() {
		FragmentUtils.clearFragmentBackStackImmediate(this, null);
	}

	public void showNewFragment(@NonNull ABFragment newFragment,
								boolean addToStack,
								@Nullable Fragment optSource) {
		showNewFragment(newFragment, addToStack, optSource, null, null);
	}

	public void showNewFragment(@NonNull ABFragment newFragment,
								boolean addToStack,
								@Nullable Fragment optSource,
								@Nullable View optTransitionSharedElement,
								@Nullable String optTransitionName) {
		MTLog.d(this, "showNewFragment(%s, %s, %s, %s, %s)", newFragment, addToStack, optSource, optTransitionSharedElement, optTransitionName);
		FragmentUtils.replaceFragment(this, R.id.content_frame, newFragment, addToStack, optSource, optTransitionSharedElement, optTransitionName);
		if (addToStack) {
			incBackEntryCount();
		}
		showContentFrameAsLoaded();
		if (this.abController != null) {
			FragmentUtils.executePendingTransactions(this, null);
			this.abController.setAB(newFragment);
			this.abController.updateABDrawerClosed();
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.setCurrentSelectedItemChecked(getBackStackEntryCount() == 0);
		}
	}

	public void showContentFrameAsLoaded() {
		if (findViewById(R.id.content_frame_loading) != null) {
			findViewById(R.id.content_frame_loading).setVisibility(View.GONE);
		}
		if (findViewById(R.id.content_frame) != null) {
			findViewById(R.id.content_frame).setVisibility(View.VISIBLE);
		}
	}

	public void addFragmentToStack(@NonNull ABFragment newFragment) {
		addFragmentToStack(newFragment, getCurrentFragment());
	}

	public void addFragmentToStack(@NonNull ABFragment newFragment, @Nullable Fragment optSource) {
		addFragmentToStack(
				newFragment,
				optSource,
				null
		);
	}

	public void addFragmentToStack(@NonNull ABFragment newFragment,
								   @Nullable View optTransitionSharedElement) {
		addFragmentToStack(
				newFragment,
				null,
				optTransitionSharedElement
		);
	}

	public void addFragmentToStack(@NonNull ABFragment newFragment,
								   @Nullable Fragment optSource,
								   @Nullable View optTransitionSharedElement) {
		addFragmentToStack(
				newFragment,
				optSource,
				optTransitionSharedElement,
				optTransitionSharedElement == null ? null : optTransitionSharedElement.getTransitionName()
		);
	}

	public void addFragmentToStack(@NonNull ABFragment newFragment,
								   @Nullable Fragment optSource,
								   @Nullable View optTransitionSharedElement,
								   @Nullable String optTransitionName) {
		showNewFragment(newFragment, true, optSource, optTransitionSharedElement, optTransitionName);
	}

	@Override
	public void onLastLocationChanged(@Nullable Location lastLocation) {
		MTActivityWithLocation.broadcastUserLocationChanged(this, getSupportFragmentManager().getFragments(), lastLocation);
	}

	public boolean isCurrentFragmentVisible(@Nullable Fragment fragment) {
		return FragmentUtils.isCurrentFragmentVisible(this, R.id.content_frame, fragment);
	}

	@Nullable
	private Fragment getCurrentFragment() {
		return FragmentUtils.getFragment(this, R.id.content_frame);
	}

	@Override
	public void onBackStackChanged() {
		resetBackStackEntryCount();
		if (this.abController != null) {
			this.abController.setAB((ABFragment) getCurrentFragment());
			this.abController.updateABDrawerClosed();
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onBackStackChanged(getBackStackEntryCount());
		}
	}

	@Override
	public void onBackPressed() {
		if (this.navigationDrawerController != null && this.navigationDrawerController.onBackPressed()) {
			return;
		}
		Fragment currentFragment = getCurrentFragment();
		if (currentFragment instanceof ABFragment && ((ABFragment) currentFragment).onBackPressed()) {
			return;
		}
		super.onBackPressed();
	}

	public void updateNavigationDrawerToggleIndicator() {
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.setDrawerToggleIndicatorEnabled(getBackStackEntryCount() < 1);
		}
	}

	@SuppressWarnings("unused")
	public void enableNavigationDrawerToggleIndicator() {
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.setDrawerToggleIndicatorEnabled(true);
		}
	}

	@SuppressWarnings("unused")
	public boolean isDrawerOpen() {
		return this.navigationDrawerController != null && this.navigationDrawerController.isDrawerOpen();
	}

	private Integer backStackEntryCount = null;

	public int getBackStackEntryCount() {
		if (this.backStackEntryCount == null) {
			initBackStackEntryCount();
		}
		return this.backStackEntryCount;
	}

	private void initBackStackEntryCount() {
		this.backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
	}

	private void resetBackStackEntryCount() {
		this.backStackEntryCount = null;
	}

	private void incBackEntryCount() {
		if (this.backStackEntryCount == null) {
			initBackStackEntryCount();
		}
		this.backStackEntryCount++;
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (this.abController != null) {
			this.abController.onCreateOptionsMenu(menu, getMenuInflater());
		}
		return true;
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onActivityPostCreate();
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (this.currentUiMode != newConfig.uiMode) {
			NightModeUtils.setDefaultNightMode(requireContext(), demoModeManager); // does NOT recreated because uiMode in configChanges AndroidManifest.xml
			NightModeUtils.recreate(this); // not recreated because uiMode in configChanges AndroidManifest.xml
			return;
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onConfigurationChanged(newConfig);
		}
		this.adManager.adaptToScreenSize(this, newConfig);
	}

	@NonNull
	private final WeakHashMap<Fragment, Object> fragmentsToPopWR = new WeakHashMap<>();

	public void popFragmentFromStack(@Nullable Fragment fragment) {
		FragmentUtils.popFragmentFromStack(this, fragment, null);
	}

	private void popFragmentsToPop() {
		try {
			for (Fragment fragment : this.fragmentsToPopWR.keySet()) {
				popFragmentFromStack(fragment);
			}
			this.fragmentsToPopWR.clear();
		} catch (Exception e) {
			MTLog.w(this, e, "Error while pop-ing fragments to pop from stack!");
		}
	}

	public boolean onUpIconClick() {
		return FragmentUtils.popLatestEntryFromStack(this, null);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (this.navigationDrawerController != null && this.navigationDrawerController.onOptionsItemSelected(item)) {
			return true; // handled
		}
		if (this.abController != null && this.abController.onOptionsItemSelected(item)) {
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}
}
