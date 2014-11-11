package org.mtransit.android.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.MenuAdapter;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.fragment.SearchFragment;
import org.mtransit.android.util.AdsUtils;
import org.mtransit.android.util.AnalyticsUtils;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

@SuppressWarnings("deprecation")
// need to switch to support-v7-appcompat
public class MainActivity extends MTActivityWithLocation implements AdapterView.OnItemClickListener, FragmentManager.OnBackStackChangedListener,
		AnalyticsUtils.Trackable, MenuAdapter.MenuUpdateListener {

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

	private static final boolean LOCATION_ENABLED = true;

	private static final String EXTRA_SELECTED_ROOT_SCREEN_POSITION = "extra_selected_root_screen";
	private static final String EXTRA_SELECTED_ROOT_SCREEN_ID = "extra_selected_root_screen_id";

	private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_NONE;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private MenuAdapter mDrawerListAdapter;
	private ActionBarDrawerToggle mDrawerToggle;
	private int mDrawerState = DrawerLayout.STATE_IDLE;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private CharSequence mDrawerSubtitle;
	private CharSequence mSubtitle;

	private int mIcon;
	private int mDrawerIcon;

	private Integer mBgColor;
	private Integer mDrawerBgColor;

	private View mCustomView;
	private View mDrawerCustomView;

	private boolean mThemeDarkInsteadOfThemeLight;

	private boolean mDisplayHomeAsUpEnabled;
	private boolean mDrawerDisplayHomeAsUpEnabled;

	private boolean mShowSearchMenuItem;

	public static Intent newInstance(Context context, int optSelectedRootScreenPosition, String optSelectedRootScreenId) {
		Intent intent = new Intent(context, MainActivity.class);
		if (optSelectedRootScreenPosition >= 0) {
			intent.putExtra(EXTRA_SELECTED_ROOT_SCREEN_POSITION, optSelectedRootScreenPosition);
		}
		if (!TextUtils.isEmpty(optSelectedRootScreenId)) {
			intent.putExtra(EXTRA_SELECTED_ROOT_SCREEN_ID, optSelectedRootScreenId);
		}
		return intent;
	}

	public MainActivity() {
		super(LOCATION_ENABLED);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle = mDrawerTitle = getTitle();
		mSubtitle = mDrawerSubtitle = getActionBar().getSubtitle();
		mIcon = mDrawerIcon = R.mipmap.ic_launcher;
		mBgColor = mDrawerBgColor = ABFragment.NO_BG_COLOR;
		mCustomView = mDrawerCustomView = getActionBar().getCustomView();
		mThemeDarkInsteadOfThemeLight = false;
		mDisplayHomeAsUpEnabled = mDrawerDisplayHomeAsUpEnabled = true;
		mShowSearchMenuItem = true;
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer); // (mDrawerList) getSupportFragmentManager().findFragmentById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerListAdapter = new MenuAdapter(this, this);
		mDrawerList.setAdapter(mDrawerListAdapter);
		mDrawerList.setOnItemClickListener(this);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ABToggle(this, mDrawerLayout);

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getSupportFragmentManager().addOnBackStackChangedListener(this);

		if (savedInstanceState == null) {
			final String itemId = PreferenceUtils.getPrefLcl(this, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
			selectItem(this.mDrawerListAdapter.getScreenItemPosition(itemId), null, false);
		} else {
			onRestoreState(savedInstanceState);
		}
		AdsUtils.setupAd(this);
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
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content_frame);
		if (f != null && f instanceof SearchFragment) {
			((SearchFragment) f).setQuery(query, false);
		} else {
			addFragmentToStack(SearchFragment.newInstance(query, null));
		}
	}



	private void onRestoreState(Bundle savedInstanceState) {
		int savedRootScreen = savedInstanceState.getInt(EXTRA_SELECTED_ROOT_SCREEN_POSITION, -1);
		if (savedRootScreen >= 0) {
			this.currentSelectedItemPosition = savedRootScreen;
		}
		String savedRootScreenId = savedInstanceState.getString(EXTRA_SELECTED_ROOT_SCREEN_ID, null);
		if (!TextUtils.isEmpty(savedRootScreenId)) {
			this.currentSelectedScreenItemId = savedRootScreenId;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		setAB();
		updateAB();
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
		if (this.mDrawerLayout != null) {
			this.mDrawerLayout.setDrawerListener(null);
			this.mDrawerLayout = null;
		}
		if (this.allMenuItems != null) {
			this.allMenuItems.clear();
		}
		if (this.fragmentsToPopWR != null) {
			this.fragmentsToPopWR.clear();
			this.fragmentsToPopWR = null;
		}
		this.mDrawerToggle = null;
		this.mCustomView = null;
		this.mDrawerCustomView = null;
		DataSourceProvider.destroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(EXTRA_SELECTED_ROOT_SCREEN_POSITION, this.currentSelectedItemPosition);
		outState.putString(EXTRA_SELECTED_ROOT_SCREEN_ID, this.currentSelectedScreenItemId);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		onRestoreState(savedInstanceState);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		selectItem(position, null, false);
	}


	private int currentSelectedItemPosition = -1;

	private String currentSelectedScreenItemId = null;

	private void selectItem(int position, ABFragment newFragmentOrNull, boolean addToStack) {
		if (position < 0) {
			return;
		}
		final FragmentManager fm = getSupportFragmentManager();
		if (position == this.currentSelectedItemPosition) {
			while (fm.getBackStackEntryCount() > 0) {
				fm.popBackStackImmediate();
			}
			this.lastUpdateAB = -1; // reset
			closeDrawer();
			return;
		}
		if (!this.mDrawerListAdapter.isRootScreen(position)) {
			if (this.currentSelectedItemPosition >= 0) {
				this.mDrawerList.setItemChecked(this.currentSelectedItemPosition, true); // keep current position
			}
			return;
		}
		final ABFragment newFragment = newFragmentOrNull != null ? newFragmentOrNull : this.mDrawerListAdapter.getNewStaticFragmentAt(position);
		if (newFragment == null) {
			return;
		}
		clearFragmentBackStackImmediate(fm); // root screen
		StatusLoader.get().clearAllTasks();
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.content_frame, newFragment);
		if (addToStack) {
			ft.addToBackStack(null);
			this.backStackEntryCount++;
		}
		ft.setTransition(FRAGMENT_TRANSITION);
		ft.commit();
		this.lastUpdateAB = -1; // reset
		setAB(newFragment);
		this.mDrawerList.setItemChecked(position, true);
		closeDrawer();
		this.currentSelectedItemPosition = position;
		this.currentSelectedScreenItemId = this.mDrawerListAdapter.getScreenItemId(position);
		if (!addToStack && this.mDrawerListAdapter.isRootScreen(position)) {
			PreferenceUtils.savePrefLcl(this, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, this.currentSelectedScreenItemId, false);
		}
	}

	@Override
	public void onMenuUpdated() {
		final String itemId = PreferenceUtils.getPrefLcl(this, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
		final int newSelectedItemPosition = this.mDrawerListAdapter.getScreenItemPosition(itemId);
		if (this.currentSelectedScreenItemId != null && this.currentSelectedScreenItemId.equals(itemId)) {
			this.currentSelectedItemPosition = newSelectedItemPosition;
			if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
				mDrawerList.setItemChecked(this.currentSelectedItemPosition, true);
			} else {
				mDrawerList.setItemChecked(this.currentSelectedItemPosition, false);
			}
			return;
		}
		selectItem(newSelectedItemPosition, null, false); // re-select, selected item
	}

	private void clearFragmentBackStackImmediate(FragmentManager fm) {
		while (fm.getBackStackEntryCount() > 0) {
			fm.popBackStackImmediate();
		}
		this.lastUpdateAB = -1; // reset
	}

	public void addFragmentToStack(ABFragment newFragment) {
		final FragmentManager fm = getSupportFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.content_frame, newFragment);
		ft.addToBackStack(null);
		this.backStackEntryCount++;
		ft.setTransition(FRAGMENT_TRANSITION);
		ft.commit();
		this.lastUpdateAB = -1; // reset
		setAB(newFragment);
		this.mDrawerList.setItemChecked(this.currentSelectedItemPosition, false);
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


	private void setAB() {
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content_frame);
		if (f != null && f instanceof ABFragment) {
			ABFragment abf = (ABFragment) f;
			setAB(abf);
		}
	}

	private void setAB(ABFragment abf) {
		setAB(abf.getABTitle(this), abf.getABSubtitle(this), abf.getABIconDrawableResId(), abf.getABBgColor(), abf.getABCustomView(),
				abf.isABThemeDarkInsteadOfThemeLight(), abf.isABDisplayHomeAsUpEnabled(), abf.isABShowSearchMenuItem());
	}

	private void setAB(CharSequence title, CharSequence subtitle, int iconResId, Integer bgColor, View customView, boolean themeDarkInsteadOfThemeLight,
			boolean displayHomeAsUpEnabled, boolean showSearchMenuItem) {
		mTitle = title;
		mSubtitle = subtitle;
		mIcon = iconResId;
		mBgColor = bgColor;
		mCustomView = customView;
		mThemeDarkInsteadOfThemeLight = themeDarkInsteadOfThemeLight;
		mDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		mShowSearchMenuItem = showSearchMenuItem;
	private boolean isCurrentFragmentVisible(Fragment fragment) {
		if (fragment == null) {
			return false;
		}
		if (fragment.isAdded() || fragment.isVisible() || fragment.isResumed()
				|| fragment.equals(getSupportFragmentManager().findFragmentById(R.id.content_frame))) {
			return true;
		}
		return false;
	}

	public void setABTitle(Fragment source, CharSequence title, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mTitle = title;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABSubtitle(Fragment source, CharSequence subtitle, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mSubtitle = subtitle;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABIcon(Fragment source, int resId, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mIcon = resId;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABBgColor(Fragment source, int bgColor, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mBgColor = bgColor;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABCustomView(Fragment source, View customView, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mCustomView = customView;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABDisplayHomeAsUpEnabled(Fragment source, boolean displayHomeAsUpEnabled, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABThemeDarkInsteadOfThemeLight(Fragment source, boolean themeDarkInsteadOfThemeLight, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mThemeDarkInsteadOfThemeLight = themeDarkInsteadOfThemeLight;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABShowSearchMenuItem(Fragment source, boolean showSearchMenuItem, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		mShowSearchMenuItem = showSearchMenuItem;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	@Override
	public void onBackStackChanged() {
		// MTLog.d(this, "onBackStackChanged() > getSupportFragmentManager().getBackStackEntryCount(): %s",
		// getSupportFragmentManager().getBackStackEntryCount());
		// MTLog.d(this, "onBackStackChanged() > this.currentSelectedItemPosition: %s", this.currentSelectedItemPosition);
		// this.mDrawerToggle.setDrawerIndicatorEnabled(isDrawerOpen() ? true : getSupportFragmentManager().getBackStackEntryCount() < 1);
		this.backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
		setAB();
		updateAB(); // up/drawer icon
		if (this.backStackEntryCount == 0) {
			mDrawerList.setItemChecked(this.currentSelectedItemPosition, true);
		} else {
			mDrawerList.setItemChecked(this.currentSelectedItemPosition, false);
		}
	}

	@Override
	public void onBackPressed() {
		if (isDrawerOpen()) {
			closeDrawer();
			return;
		}
		super.onBackPressed();
	}

	private void closeDrawer() {
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	private boolean isDrawerOpen() {
		return mDrawerLayout.isDrawerOpen(mDrawerList);
	}


	private Handler handler = new Handler();

	private long lastUpdateAB = -1l;

	private static final long MIN_DURATION_BETWEEN_UPDATE_AB_IN_MS = 100l; // 0.1 second

	private UpdateABLater updateABLater = null;


	private int backStackEntryCount = 0;

	private void updateAB() {
		final long now = System.currentTimeMillis();
		final long howLongBeforeNextUpdateABInMs = this.lastUpdateAB + MIN_DURATION_BETWEEN_UPDATE_AB_IN_MS - now;
		if (mDrawerState != DrawerLayout.STATE_IDLE || howLongBeforeNextUpdateABInMs > 0) {
			if (this.updateABLater == null) {
				this.updateABLater = new UpdateABLater();
				this.handler.postDelayed(this.updateABLater, howLongBeforeNextUpdateABInMs > 0 ? howLongBeforeNextUpdateABInMs
						: MIN_DURATION_BETWEEN_UPDATE_AB_IN_MS);
			}
			return;
		}
		this.handler.removeCallbacks(this.updateABLater);
		this.updateABLater = null;
		if (isDrawerOpen()) {
			updateABDrawerOpened();
		} else {
			updateABDrawerClosed();
		}
		this.lastUpdateAB = now;
	}

	private void updateABDrawerClosed() {
		if (mCustomView != null) {
			getActionBar().setCustomView(mCustomView);
			if (!mDisplayHomeAsUpEnabled) {
				getActionBar().getCustomView().setOnClickListener(this.upOnClickListener);
			}
			getActionBar().setDisplayShowCustomEnabled(true);
		} else {
			getActionBar().setDisplayShowCustomEnabled(false);
		}
		getActionBar().setDisplayHomeAsUpEnabled(mDisplayHomeAsUpEnabled);
		if (TextUtils.isEmpty(mTitle)) {
			getActionBar().setDisplayShowTitleEnabled(false);
		} else {
			getActionBar().setTitle(mTitle);
			getActionBar().setSubtitle(mSubtitle);
			getActionBar().setDisplayShowTitleEnabled(true);
		}
		if (mIcon > 0) {
			getActionBar().setIcon(mIcon);
			getActionBar().setDisplayShowHomeEnabled(true);
		} else {
			getActionBar().setDisplayShowHomeEnabled(false);
		}
		if (mBgColor != null) {
			getActionBar().setBackgroundDrawable(new ColorDrawable(mBgColor));
		} else {
			getActionBar().setBackgroundDrawable(null);
		}
		this.mDrawerToggle.setDrawerIndicatorEnabled(this.backStackEntryCount < 1);
		updateAllMenuItems(); // action bar icons are options menu items
	}

	private void updateABDrawerOpened() {
		if (mDrawerCustomView != null) {
			getActionBar().setCustomView(mDrawerCustomView);
			if (!mDrawerDisplayHomeAsUpEnabled) {
				getActionBar().getCustomView().setOnClickListener(this.upOnClickListener);
			}
			getActionBar().setDisplayShowCustomEnabled(true);
		} else {
			getActionBar().setDisplayShowCustomEnabled(false);
		}
		getActionBar().setDisplayHomeAsUpEnabled(mDrawerDisplayHomeAsUpEnabled);
		if (TextUtils.isEmpty(mDrawerTitle)) {
			getActionBar().setDisplayShowTitleEnabled(false);
		} else {
			getActionBar().setTitle(mDrawerTitle);
			getActionBar().setSubtitle(mDrawerSubtitle);
			getActionBar().setDisplayShowTitleEnabled(true);
		}
		if (mDrawerIcon > 0) {
			getActionBar().setIcon(mDrawerIcon);
			getActionBar().setDisplayShowHomeEnabled(true);
		} else {
			getActionBar().setDisplayShowHomeEnabled(false);
		}
		if (mDrawerBgColor != null) {
			getActionBar().setBackgroundDrawable(new ColorDrawable(mDrawerBgColor));
		} else {
			getActionBar().setBackgroundDrawable(null);
		}
		this.mDrawerToggle.setDrawerIndicatorEnabled(true);
		updateAllMenuItems(); // action bar icons are options menu items
	}

	private UpOnClickListener upOnClickListener = new UpOnClickListener();

	private HashMap<Integer, MenuItem> allMenuItems = new HashMap<Integer, MenuItem>();

	public void addMenuItem(int resId, MenuItem menuItem) {
		this.allMenuItems.put(resId, menuItem);
	}

	public MenuItem getMenuItem(int resId) {
		return this.allMenuItems.get(resId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main_activity, menu);
		this.allMenuItems.clear();
		this.allMenuItems.put(R.id.menu_search, menu.findItem(R.id.menu_search));
		final MenuItem menuToggleListGrid = menu.findItem(R.id.menu_toggle_list_grid);
		if (menuToggleListGrid != null) {
			this.allMenuItems.put(R.id.menu_toggle_list_grid, menuToggleListGrid);
		}
		final MenuItem menuAddRemoveFavorite = menu.findItem(R.id.menu_add_remove_favorite);
		if (menuAddRemoveFavorite != null) {
			this.allMenuItems.put(R.id.menu_add_remove_favorite, menuAddRemoveFavorite);
		}
		updateAllMenuItems();
		return true;
	}

	private void updateAllMenuItems() {
		if (this.allMenuItems == null || this.allMenuItems.size() == 0) {
			return;
		}
		final boolean drawerOpen = isDrawerOpen();
		final boolean showABIcons = !drawerOpen;
		if (this.allMenuItems.get(R.id.menu_toggle_list_grid) != null) {
			this.allMenuItems.get(R.id.menu_toggle_list_grid).setVisible(showABIcons);
		}
		if (this.allMenuItems.get(R.id.menu_add_remove_favorite) != null) {
			this.allMenuItems.get(R.id.menu_add_remove_favorite).setVisible(showABIcons);
		}
		this.allMenuItems.get(R.id.menu_search).setVisible(this.mShowSearchMenuItem && showABIcons);
		this.allMenuItems.get(R.id.menu_search).setIcon(
				mThemeDarkInsteadOfThemeLight ? R.drawable.ic_menu_action_search_holo_dark : R.drawable.ic_menu_action_search_holo_light);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
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
				this.lastUpdateAB = -1; // reset
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		if (item.getItemId() == android.R.id.home) {
			final FragmentManager fm = getSupportFragmentManager();
			if (fm.getBackStackEntryCount() > 0) {
				fm.popBackStackImmediate();
				this.lastUpdateAB = -1; // reset
				return true;
			}
		}
		switch (item.getItemId()) {
		case R.id.menu_search:
			onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class UpOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			final FragmentManager fm = getSupportFragmentManager();
			if (fm.getBackStackEntryCount() > 0) {
				fm.popBackStackImmediate();
				MainActivity.this.lastUpdateAB = -1; // reset
			}
		}
	}

	private class UpdateABLater implements Runnable {
		@Override
		public void run() {
			MainActivity.this.updateABLater = null;
			updateAB();
		}
	}

	private static class ABToggle extends ActionBarDrawerToggle implements MTLog.Loggable {

		private static final String TAG = MainActivity.class.getSimpleName() + ">" + ABToggle.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<MainActivity> mainActivityWR;

		public ABToggle(MainActivity mainActivity, DrawerLayout drawerLayout) {
			super(mainActivity, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
			this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
		}

		@Override
		public void onDrawerClosed(View view) {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.updateAB();
			}
		}

		@Override
		public void onDrawerOpened(View drawerView) {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.updateAB();
			}
		}

		@Override
		public void onDrawerStateChanged(int newState) {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.mDrawerState = newState;
			}
		}
	}
}
