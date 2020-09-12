package org.mtransit.android.ui;

import android.app.Activity;
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
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.di.Injection;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.fragment.POIFragment;
import org.mtransit.android.ui.fragment.SearchFragment;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.android.util.FragmentUtils;
import org.mtransit.android.util.MapUtils;
import org.mtransit.android.util.NightModeUtils;
import org.mtransit.android.util.VendingUtils;

import java.util.WeakHashMap;

@SuppressWarnings("unused")
public class MainActivity extends MTActivityWithLocation implements
		FragmentManager.OnBackStackChangedListener,
		AnalyticsManager.Trackable,
		VendingUtils.OnVendingResultListener,
		IActivity,
		IAdManager.RewardedAdListener,
		DataSourceProvider.ModulesUpdateListener {

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

	private NavigationDrawerController navigationDrawerController;

	private ActionBarController abController;

	@NonNull
	private final IAdManager adManager;
	@NonNull
	private final IAnalyticsManager analyticsManager;
	@NonNull
	private final CrashReporter crashReporter;

	private int currentUiMode = -1;

	public MainActivity() {
		super();
		adManager = Injection.providesAdManager();
		analyticsManager = Injection.providesAnalyticsManager();
		crashReporter = Injection.providesCrashReporter();
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NightModeUtils.resetColorCache(); // single activity, no cache can be trusted to be from the right theme
		this.currentUiMode = getResources().getConfiguration().uiMode;
		setContentView(R.layout.activity_main);
		this.abController = new ActionBarController(this);
		this.navigationDrawerController = new NavigationDrawerController(this, crashReporter);
		this.navigationDrawerController.onCreate(savedInstanceState);
		getSupportFragmentManager().addOnBackStackChangedListener(this);
		DataSourceProvider.addModulesUpdateListener(this);
		MapUtils.fixScreenFlickering(findViewById(R.id.content_frame));
	}

	@Override
	protected void attachBaseContext(@NonNull Context newBase) {
		newBase = LocaleUtils.fixDefaultLocale(newBase);
		super.attachBaseContext(newBase);
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!this.resumed) {
			return;
		}
		this.adManager.onModulesUpdated(this);
		this.modulesUpdated = false; // processed
	}

	@Override
	public void onVendingResult(@Nullable Boolean hasSubscription) {
		if (hasSubscription != null) {
			this.adManager.setShowingAds(!hasSubscription, this);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (!VendingUtils.onActivityResult(this, requestCode, resultCode, data)) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public ActionBarController getAbController() {
		return abController;
	}

	public NavigationDrawerController getNavigationDrawerController() {
		return navigationDrawerController;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		processIntent(intent);
	}

	private boolean processIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
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

	public void onSearchQueryRequested(String query) {
		Fragment currentFragment = getCurrentFragment();
		if (currentFragment instanceof SearchFragment) {
			((SearchFragment) currentFragment).setSearchQuery(query, false);
		} else {
			addFragmentToStack(SearchFragment.newInstance(query, null, null), currentFragment);
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
		analyticsManager.trackScreenView(this, this);
		VendingUtils.onResume(this, this);
		this.adManager.adaptToScreenSize(this, getResources().getConfiguration());
		this.adManager.setRewardedAdListener(this); // used until POI screen is visible // need to pre-load ASAP
		this.adManager.linkRewardedAd(this);
		onLastLocationChanged(getUserLocation());
	}

	@Override
	public void onRewardedAdStatusChanged() {
		// DO NOTHING
	}

	@Override
	public boolean skipRewardedAd() {
		return POIFragment.shouldSkipRewardedAd(this.adManager);
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		this.resumed = true;
		if (this.modulesUpdated) {
			new Handler().post(() -> {
				if (MainActivity.this.modulesUpdated) {
					onModulesUpdated();
				}
			});
		}
		DataSourceProvider.onResume();
		if (this.currentUiMode != getResources().getConfiguration().uiMode) {
			new Handler().post(() -> {
				NightModeUtils.resetColorCache();
				NightModeUtils.recreate(this);
			});
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
		VendingUtils.onPause();
		this.adManager.pauseAd(this);
		DataSourceProvider.onPause();
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
		DataSourceProvider.resetIfNecessary(this);
		popFragmentsToPop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		VendingUtils.destroyBilling(this);
		DataSourceProvider.removeModulesUpdateListener(this);
		if (this.abController != null) {
			this.abController.destroy();
			this.abController = null;
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.destroy();
			this.navigationDrawerController = null;
		}
		this.fragmentsToPopWR.clear();
		DataSourceProvider.destroy();
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

	public void showNewFragment(ABFragment newFragment, boolean addToStack, @Nullable Fragment optSource) {
		FragmentUtils.replaceFragment(this, R.id.content_frame, newFragment, addToStack, optSource);
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

	public void addFragmentToStack(ABFragment newFragment) {
		addFragmentToStack(newFragment, getCurrentFragment());
	}

	public void addFragmentToStack(ABFragment newFragment, @Nullable Fragment optSource) {
		showNewFragment(newFragment, true, optSource);
	}

	@Override
	public void onLastLocationChanged(@Nullable Location lastLocation) {
		MTActivityWithLocation.broadcastUserLocationChanged(this, getFragments(), lastLocation);
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

	public void enableNavigationDrawerToggleIndicator() {
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.setDrawerToggleIndicatorEnabled(true);
		}
	}

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
			NightModeUtils.resetColorCache();
			NightModeUtils.recreate(this);
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

	@NonNull
	@Override
	public Context getContext() {
		return this;
	}

	@NonNull
	@Override
	public Context requireContext() throws IllegalStateException {
		return this;
	}

	@NonNull
	@Override
	public Activity getActivity() {
		return this;
	}

	@NonNull
	@Override
	public Activity requireActivity() throws IllegalStateException {
		return this;
	}
}
