package org.mtransit.android.ui;

import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.fragment.SearchFragment;
import org.mtransit.android.util.AdsUtils;
import org.mtransit.android.util.AnalyticsUtils;
import org.mtransit.android.util.MapUtils;
import org.mtransit.android.util.VendingUtils;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

public class MainActivity extends MTActivityWithLocation implements FragmentManager.OnBackStackChangedListener, AnalyticsUtils.Trackable,
		VendingUtils.OnVendingResultListener, DataSourceProvider.ModulesUpdateListener {

	private static final String TAG = "Stack-" + MainActivity.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Main";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	public static Intent newInstance(Context context) {
		return new Intent(context, MainActivity.class);
	}

	private static final boolean LOCATION_ENABLED = true;

	private NavigationDrawerController navigationDrawerController;

	private ActionBarController abController;

	public MainActivity() {
		super(LOCATION_ENABLED);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.abController = new ActionBarController(this);
		this.navigationDrawerController = new NavigationDrawerController(this);
		this.navigationDrawerController.setup(savedInstanceState);
		getSupportFragmentManager().addOnBackStackChangedListener(this);
		DataSourceProvider.addModulesUpdateListener(this);
		MapUtils.fixScreenFlickering((FrameLayout) findViewById(R.id.content_frame));
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!this.resumed) {
			return;
		}
		AdsUtils.onModulesUpdated(this);
		this.modulesUpdated = false; // processed
	}

	@Override
	public void onVendingResult(Boolean hasSubscription) {
		if (hasSubscription != null) {
			AdsUtils.setShowingAds(!hasSubscription, this);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
		Fragment f = getCurrentFragment();
		if (f != null && f instanceof SearchFragment) {
			((SearchFragment) f).setSearchQuery(query, false);
		} else {
			addFragmentToStack(SearchFragment.newInstance(query, null, null));
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
		AnalyticsUtils.trackScreenView(this, this);
		VendingUtils.onResume(this, this);
		AdsUtils.adaptToScreenSize(this, getResources().getConfiguration());
		onUserLocationChanged(getUserLocation());
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		this.resumed = true;
		if (this.modulesUpdated) {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					if (MainActivity.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
		DataSourceProvider.onResume();
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
		AdsUtils.pauseAd(this);
		DataSourceProvider.onPause();
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
		if (this.fragmentsToPopWR != null) {
			this.fragmentsToPopWR.clear();
			this.fragmentsToPopWR = null;
		}
		DataSourceProvider.destroy();
		AdsUtils.destroyAd(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onSaveState(outState);
		}
		if (this.abController != null) {
			this.abController.onSaveState(outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onRestoreState(savedInstanceState);
		}
		if (this.abController != null) {
			this.abController.onRestoreState(savedInstanceState);
		}
	}

	public void clearFragmentBackStackImmediate() {
		clearFragmentBackStackImmediate(getSupportFragmentManager());
	}

	private void clearFragmentBackStackImmediate(FragmentManager fm) {
		try {
			fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while clearing fragment stack!");
		}
	}


	public void showNewFragment(ABFragment newFragment, boolean addToStack) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.content_frame, newFragment);
		if (addToStack) {
			ft.addToBackStack(null);
			incBackEntryCount();
		}
		ft.commit();
		showContentFrameAsLoaded();
		if (this.abController != null) {
			fm.executePendingTransactions();
			this.abController.setAB(newFragment);
			this.abController.updateABDrawerClosed();
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.setCurrentSelectedItemChecked(getBackStackEntryCount() == 0);
		}
	}

	private static final String DIALOG_TAG = "dialog";

	public void showNewDialog(DialogFragment newDialog) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prev = fm.findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		if (newDialog != null) {
			newDialog.show(ft, DIALOG_TAG);
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
		showNewFragment(newFragment, true);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		MTActivityWithLocation.broadcastUserLocationChanged(this, getSupportFragmentManager(), newLocation);
	}

	public boolean isCurrentFragmentVisible(Fragment fragment) {
		if (fragment == null) {
			return false;
		}
		if (fragment.isAdded() || fragment.isVisible() || fragment.isResumed() || fragment.equals(getCurrentFragment())) {
			return true;
		}
		return false;
	}

	private Fragment getCurrentFragment() {
		return getSupportFragmentManager().findFragmentById(R.id.content_frame);
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
		if (currentFragment != null && currentFragment instanceof ABFragment && ((ABFragment) currentFragment).onBackPressed()) {
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (this.abController != null) {
			this.abController.onCreateOptionsMenu(menu, getMenuInflater());
		}
		return true;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onActivityPostCreate();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onConfigurationChanged(newConfig);
		}
		AdsUtils.adaptToScreenSize(this, newConfig);
	}

	private WeakHashMap<Fragment, Object> fragmentsToPopWR = new WeakHashMap<Fragment, Object>();

	public void popFragmentFromStack(Fragment fragment) {
		try {
			if (fragment != null) {
				FragmentManager fm = getSupportFragmentManager();
				fm.popBackStack();
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while poping fragment '%s' from stack!", fragment);
			if (this.fragmentsToPopWR != null) {
				this.fragmentsToPopWR.put(fragment, null);
			}
		}
	}

	private void popFragmentsToPop() {
		try {
			if (this.fragmentsToPopWR != null) {
				for (Fragment fragment : this.fragmentsToPopWR.keySet()) {
					popFragmentFromStack(fragment);
				}
				this.fragmentsToPopWR.clear();
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while poping fragments to pop from stack!");
		}
	}

	public boolean onUpIconClick() {
		FragmentManager fm = getSupportFragmentManager();
		if (fm.getBackStackEntryCount() > 0) {
			fm.popBackStack();
			return true; // handled
		}
		return false; // not handled
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (this.navigationDrawerController != null && this.navigationDrawerController.onOptionsItemSelected(item)) {
			return true; // handled
		}
		if (this.abController != null && this.abController.onOptionsItemSelected(item)) {
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

}
