package org.mtransit.android.ui;

import java.util.List;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.fragment.SearchFragment;
import org.mtransit.android.util.AdsUtils;
import org.mtransit.android.util.AnalyticsUtils;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;

public class MainActivity extends MTActivityWithLocation implements FragmentManager.OnBackStackChangedListener, AnalyticsUtils.Trackable {

	private static final String TAG = MainActivity.class.getSimpleName();

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
		Intent intent = new Intent(context, MainActivity.class);
		return intent;
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
		this.navigationDrawerController.onRestoreState(savedInstanceState);
		this.navigationDrawerController.setup();

		getSupportFragmentManager().addOnBackStackChangedListener(this);
		AdsUtils.setupAd(this);
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
			onSearchRequested(intent.getStringExtra(SearchManager.QUERY));
			return true; // intent processed
		}
		return false; // intent not processed
	}

	@Override
	public boolean onSearchRequested() {
		onSearchRequested(null);
		return true; // processed
	}

	private void onSearchRequested(String query) {
		Fragment f = getCurrentFragment();
		if (f != null && f instanceof SearchFragment) {
			((SearchFragment) f).setQuery(query, false);
		} else {
			addFragmentToStack(SearchFragment.newInstance(query, null));
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		if (this.abController != null) {
			this.abController.updateAB();
		}
		AnalyticsUtils.trackScreenView(this, this);
		AdsUtils.resumeAd(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		AdsUtils.pauseAd(this);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		DataSourceProvider.reset(this);
		popFragmentsToPop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		AdsUtils.destroyAd(this);
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
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onSaveInstanceState(outState);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onRestoreState(savedInstanceState);
		}
	}

	public void clearFragmentBackStackImmediate() {
		clearFragmentBackStackImmediate(getSupportFragmentManager());
	}

	private void clearFragmentBackStackImmediate(FragmentManager fm) {
		while (fm.getBackStackEntryCount() > 0) {
			fm.popBackStackImmediate();
		}
	}

	public void showNewFragment(ABFragment newFragment, boolean addToStack) {
		showNewFragment(newFragment, addToStack, null);
	}

	public void showNewFragment(ABFragment newFragment, boolean addToStack, View clickFromView) {
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.content_frame, newFragment);
		if (addToStack) {
			ft.addToBackStack(null);
			this.backStackEntryCount++;
		}
		ft.commit();
		showContentFrameAsLoaded();
		if (this.abController != null) {
			this.abController.setAB(newFragment);
			this.abController.updateAB();
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.setCurrentSelectedItemChecked(this.backStackEntryCount == 0);
		}
	}

	private void showContentFrameAsLoaded() {
		if (findViewById(R.id.content_frame_loading) != null) {
			findViewById(R.id.content_frame_loading).setVisibility(View.GONE);
		}
		if (findViewById(R.id.content_frame) != null) {
			findViewById(R.id.content_frame).setVisibility(View.VISIBLE);
		}
	}

	private void showContentFrameAsLoading() {
		if (findViewById(R.id.content_frame) != null) {
			findViewById(R.id.content_frame).setVisibility(View.GONE);
		}
		if (findViewById(R.id.content_frame_loading) == null) {
			((ViewStub) findViewById(R.id.content_frame_loading_stub)).inflate(); // inflate
		}
		findViewById(R.id.content_frame_loading).setVisibility(View.VISIBLE);
	}

	public void addFragmentToStack(ABFragment newFragment) {
		addFragmentToStack(newFragment, null);
	}

	public void addFragmentToStack(ABFragment newFragment, View clickFromView) {
		showNewFragment(newFragment, true, clickFromView);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		final List<Fragment> fragments = getSupportFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof MTActivityWithLocation.UserLocationListener) {
					((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(newLocation);
				}
			}
		}
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
		this.backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
		if (this.abController != null) {
			this.abController.setAB((ABFragment) getCurrentFragment());
			this.abController.updateAB(); // up/drawer icon
		}
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onBackStackChanged(this.backStackEntryCount);
		}
	}

	@Override
	public void onBackPressed() {
		if (this.navigationDrawerController != null && this.navigationDrawerController.onBackPressed()) {
			return;
		}
		super.onBackPressed();
	}

	public void updateNavigationDrawerToggleIndicator() {
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.setDrawerToggleIndicatorEnabled(this.backStackEntryCount < 1);
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


	private int backStackEntryCount = 0; // because FragmentManager.getBackStackEntryCount() is not instantly up-to-date

	public int getBackStackEntryCount() {
		return backStackEntryCount;
	}

	public void updateAB() {
		if (this.abController != null) {
			this.abController.updateAB();
		}
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
			this.navigationDrawerController.onActivityPostCreate(savedInstanceState);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (this.navigationDrawerController != null) {
			this.navigationDrawerController.onConfigurationChanged(newConfig);
		}
	}

	private WeakHashMap<Fragment, Object> fragmentsToPopWR = new WeakHashMap<Fragment, Object>();

	public void popFragmentFromStack(Fragment fragment) {
		try {
			if (fragment != null) {
				final FragmentManager fm = getSupportFragmentManager();
				final FragmentTransaction ft = fm.beginTransaction();
				ft.remove(fragment);
				ft.commit();
				fm.popBackStackImmediate();
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
		final FragmentManager fm = getSupportFragmentManager();
		if (fm.getBackStackEntryCount() > 0) {
			fm.popBackStackImmediate();
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
