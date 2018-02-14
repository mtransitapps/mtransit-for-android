package org.mtransit.android.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.fragment.AgencyTypeFragment;
import org.mtransit.android.ui.fragment.FavoritesFragment;
import org.mtransit.android.ui.fragment.HomeFragment;
import org.mtransit.android.ui.fragment.MapFragment;
import org.mtransit.android.ui.fragment.NearbyFragment;
import org.mtransit.android.ui.fragment.NewsFragment;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.android.util.MapUtils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class NavigationDrawerController implements MTLog.Loggable, NavigationView.OnNavigationItemSelectedListener, DataSourceProvider.ModulesUpdateListener {

	private static final String TAG = "Stack-" + NavigationDrawerController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String ITEM_ID_AGENCY_TYPE_START_WITH = "agencytype-";
	private static final String ITEM_ID_STATIC_START_WITH = "static-";
	private static final int ITEM_INDEX_HOME = 0;
	private static final int ITEM_INDEX_FAVORITE = 1;
	private static final int ITEM_INDEX_NEARBY = 2;
	private static final int ITEM_INDEX_MAP = 3;
	private static final int ITEM_INDEX_TRIP_PLANNER = 4;
	private static final int ITEM_INDEX_NEWS = 5;
	private static final int ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT = R.id.nav_home;
	public static final String ITEM_ID_SELECTED_SCREEN_DEFAULT = ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME;

	private WeakReference<MainActivity> mainActivityWR;
	private DrawerLayout drawerLayout;
	private ABDrawerToggle drawerToggle;
	private NavigationView navigationView;
	private Integer currentSelectedScreenItemNavId = null;
	private String currentSelectedScreenItemId = null;

	public NavigationDrawerController(MainActivity mainActivity) {
		this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
		DataSourceProvider.addModulesUpdateListener(this);
	}

	public void onCreate(Bundle savedInstanceState) {
	}

	private void setup() {
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		this.navigationView = (NavigationView) mainActivity.findViewById(R.id.nav_view);
		this.navigationView.setNavigationItemSelectedListener(this);
		this.drawerLayout = (DrawerLayout) mainActivity.findViewById(R.id.drawer_layout);
		this.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		this.drawerToggle = new ABDrawerToggle(mainActivity, this.drawerLayout);
		this.drawerLayout.addDrawerListener(this.drawerToggle);
		finishSetupAsync();
	}

	private void finishSetupAsync() {
		if (this.finishSetupTask != null && this.finishSetupTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		this.finishSetupTask = new FinishSetupTask(this);
		TaskUtils.execute(this.finishSetupTask);
	}

	@Nullable
	private FinishSetupTask finishSetupTask = null;

	private static class FinishSetupTask extends MTAsyncTask<Void, String, String> {

		private final String TAG = NavigationDrawerController.TAG + ">" + FinishSetupTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private final WeakReference<NavigationDrawerController> navigationDrawerControllerWR;

		public FinishSetupTask(NavigationDrawerController navigationDrawerController) {
			this.navigationDrawerControllerWR = new WeakReference<NavigationDrawerController>(navigationDrawerController);
		}

		@Override
		protected String doInBackgroundMT(Void... params) {
			NavigationDrawerController navigationDrawerController = this.navigationDrawerControllerWR.get();
			if (navigationDrawerController == null) {
				return null;
			}
			Context context = navigationDrawerController.mainActivityWR == null ? null : navigationDrawerController.mainActivityWR.get();
			if (context == null) {
				return null;
			}
			if (navigationDrawerController.isCurrentSelectedSet()) {
				return null;
			}
			String itemId = PreferenceUtils.getPrefLcl(context, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, ITEM_ID_SELECTED_SCREEN_DEFAULT);
			publishProgress(itemId);
			return itemId;
		}

		@Override
		protected void onPostExecute(String itemId) {
			NavigationDrawerController navigationDrawerController = this.navigationDrawerControllerWR.get();
			if (navigationDrawerController == null) {
				return;
			}
			if (isCancelled()) {
				return;
			}
			navigationDrawerController.setVisibleMenuItems();
			selectItemId(itemId);
			if (!navigationDrawerController.hasUserLearnedDrawer()) {
				navigationDrawerController.openDrawer();
				navigationDrawerController.setUserLearnedDrawer();
			}
		}

		private void selectItemId(@Nullable String itemId) {
			if (TextUtils.isEmpty(itemId)) {
				return;
			}
			NavigationDrawerController navigationDrawerController = this.navigationDrawerControllerWR.get();
			if (navigationDrawerController == null) {
				return;
			}
			Integer navItemId = navigationDrawerController.getScreenNavItemId(itemId);
			navigationDrawerController.selectItem(navItemId, false);
		}

		@Override
		protected void onProgressUpdate(String... itemIds) {
			super.onProgressUpdate(itemIds);
			if (isCancelled()) {
				return;
			}
			selectItemId(itemIds == null || itemIds.length == 0 ? null : itemIds[0]);
		}
	}

	private Boolean userLearnedDrawer = null;

	private boolean hasUserLearnedDrawer() {
		if (this.userLearnedDrawer == null) {
			MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				this.userLearnedDrawer = PreferenceUtils.getPrefDefault(mainActivity, PreferenceUtils.PREF_USER_LEARNED_DRAWER, //
						PreferenceUtils.PREF_USER_LEARNED_DRAWER_DEFAULT);
			}
		}
		return this.userLearnedDrawer == null ? false : this.userLearnedDrawer;
	}

	protected void setUserLearnedDrawer() {
		this.userLearnedDrawer = true;
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			PreferenceUtils.savePrefDefault(mainActivity, PreferenceUtils.PREF_USER_LEARNED_DRAWER, this.userLearnedDrawer, false); // asynchronous
		}
	}

	@Nullable
	private ArrayList<DataSourceType> allAgencyTypes = null;

	@Nullable
	private ArrayList<DataSourceType> getAllAgencyTypes() {
		if (this.allAgencyTypes == null) {
			initAllAgencyTypes();
		}
		return this.allAgencyTypes;
	}

	private void initAllAgencyTypes() {
		Context context = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (context != null) {
			this.allAgencyTypes = filterAgencyTypes(DataSourceProvider.get(context).getAvailableAgencyTypes());
		}
	}

	private ArrayList<DataSourceType> filterAgencyTypes(ArrayList<DataSourceType> availableAgencyTypes) {
		if (availableAgencyTypes != null) {
			Iterator<DataSourceType> it = availableAgencyTypes.iterator();
			while (it.hasNext()) {
				if (!it.next().isMenuList()) {
					it.remove();
				}
			}
		}
		return availableAgencyTypes;
	}

	private void setVisibleMenuItems() {
		if (this.navigationView == null) {
			MTLog.w(this, "setVisibleMenuItems() > skip (no navigation view)");
			return;
		}
		ArrayList<DataSourceType> allAgencyTypes = getAllAgencyTypes();
		for (DataSourceType dst : DataSourceType.values()) {
			if (allAgencyTypes != null && allAgencyTypes.contains(dst)) {
				continue;
			}
			if (!dst.isMenuList()) {
				continue;
			}
			if (this.navigationView.getMenu().findItem(dst.getNavResId()) == null) {
				continue;
			}
			this.navigationView.getMenu().findItem(dst.getNavResId()).setVisible(false);
		}
		if (allAgencyTypes != null) {
			for (DataSourceType dst : allAgencyTypes) {
				if (this.navigationView.getMenu().findItem(dst.getNavResId()) == null) {
					MenuItem newMenuItem = this.navigationView.getMenu().add(R.id.drawer_modules, dst.getNavResId(), Menu.NONE, dst.getAllStringResId());
					newMenuItem.setIcon(dst.getBlackIconResId());
				}
				this.navigationView.getMenu().findItem(dst.getNavResId()).setVisible(true);
			}
		}
		boolean hasAgencyInstalled = allAgencyTypes != null && allAgencyTypes.size() > 1; // include "+"
		this.navigationView.getMenu().findItem(R.id.nav_rate_review).setVisible(hasAgencyInstalled);
		this.navigationView.getMenu().findItem(R.id.nav_support).setVisible(hasAgencyInstalled);
	}

	private boolean menuUpdated = false;

	private void onMenuUpdated() {
		this.menuUpdated = true;
		if (this.navigationView == null) {
			return;
		}
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		if (!mainActivity.isMTResumed()) {
			return;
		}
		String itemId = PreferenceUtils.getPrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, ITEM_ID_SELECTED_SCREEN_DEFAULT);
		Integer newSelectedNavItemId = getScreenNavItemId(itemId);
		if (this.currentSelectedScreenItemNavId != null && this.currentSelectedScreenItemNavId.equals(newSelectedNavItemId)
				&& this.currentSelectedScreenItemId != null && this.currentSelectedScreenItemId.equals(itemId)) {
			this.currentSelectedScreenItemNavId = newSelectedNavItemId;
			setCurrentSelectedItemChecked(mainActivity.getBackStackEntryCount() == 0);
			return;
		}
		selectItem(newSelectedNavItemId, false);
		this.menuUpdated = false; // processed
	}

	public String getScreenItemId(Integer navItemId) {
		if (navItemId == null) {
			return null;
		}
		switch (navItemId) {
		case R.id.nav_home:
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME;
		case R.id.nav_favorites:
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_FAVORITE;
		case R.id.nav_nearby:
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEARBY;
		case R.id.nav_map:
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_MAP;
		case R.id.nav_trip_planner:
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_TRIP_PLANNER;
		case R.id.nav_news:
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEWS;
		case R.id.nav_light_rail:
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_LIGHT_RAIL.getId();
		case R.id.nav_subway:
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_SUBWAY.getId();
		case R.id.nav_rail:
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_RAIL.getId();
		case R.id.nav_bus:
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BUS.getId();
		case R.id.nav_ferry:
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_FERRY.getId();
		case R.id.nav_bike:
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BIKE.getId();
		case R.id.nav_module:
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_MODULE.getId();
		case R.id.nav_settings:
			return null;
		case R.id.nav_send_feedback:
			return null;
		case R.id.nav_rate_review:
			return null;
		case R.id.nav_support:
			return null;
		default:
			MTLog.w(this, "Unexpected screen nav item ID '%s'!", navItemId);
			return null;
		}
	}

	private Integer getScreenNavItemId(String itemId) {
		if (TextUtils.isEmpty(itemId)) {
			return ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT;
		}
		if (itemId.startsWith(ITEM_ID_STATIC_START_WITH)) {
			try {
				switch (Integer.parseInt(itemId.substring(ITEM_ID_STATIC_START_WITH.length()))) {
				case ITEM_INDEX_HOME:
					return R.id.nav_home;
				case ITEM_INDEX_FAVORITE:
					return R.id.nav_favorites;
				case ITEM_INDEX_NEARBY:
					return R.id.nav_nearby;
				case ITEM_INDEX_MAP:
					return R.id.nav_map;
				case ITEM_INDEX_TRIP_PLANNER:
					return R.id.nav_trip_planner;
				case ITEM_INDEX_NEWS:
					return R.id.nav_news;
				default:
					MTLog.w(this, "Unexpected static screen item ID '%s'!", itemId);
					return ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT;
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while finding static screen item ID '%s'!", itemId);
				return ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT;
			}
		} else if (itemId.startsWith(ITEM_ID_AGENCY_TYPE_START_WITH)) {
			try {
				DataSourceType dst = DataSourceType.parseId(Integer.parseInt(itemId.substring(ITEM_ID_AGENCY_TYPE_START_WITH.length())));
				if (dst != null) {
					ArrayList<DataSourceType> allAgencyTypes = getAllAgencyTypes();
					if (allAgencyTypes != null && allAgencyTypes.contains(dst)) {
						return dst.getNavResId();
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while finding agency type screen item ID '%s'!", itemId);
				return ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT;
			}
		}
		MTLog.w(this, "Unknown item ID'%s'!", itemId);
		return ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT;
	}

	public void forceReset() {
		if (this.currentSelectedScreenItemNavId == null) {
			return;
		}
		Integer saveCurrentSelectedScreenItemNavId = this.currentSelectedScreenItemNavId;
		this.currentSelectedScreenItemNavId = null;
		this.currentSelectedScreenItemId = null;
		selectItem(saveCurrentSelectedScreenItemNavId, true);
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
		closeDrawer();
		selectItem(menuItem.getItemId(), true);
		return true; // processed
	}

	private void selectItem(Integer navItemId, boolean clearStack) {
		if (navItemId == null) {
			return;
		}
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		if (navItemId.equals(this.currentSelectedScreenItemNavId)) {
			if (clearStack) {
				mainActivity.clearFragmentBackStackImmediate();
			}
			if (mainActivity.getBackStackEntryCount() == 0) {
				setCurrentSelectedItemChecked(true);
			}
			mainActivity.showContentFrameAsLoaded();
			return;
		}
		if (!isRootScreen(navItemId)) {
			startNewScreen(mainActivity, navItemId);
			setCurrentSelectedItemChecked(true); // keep current position
			return;
		}
		ABFragment newFragment = getNewStaticFragmentAt(navItemId);
		if (newFragment == null) {
			return;
		}
		this.currentSelectedScreenItemNavId = navItemId;
		this.currentSelectedScreenItemId = getScreenItemId(navItemId);
		mainActivity.clearFragmentBackStackImmediate(); // root screen
		StatusLoader.get().clearAllTasks();
		ServiceUpdateLoader.get().clearAllTasks();
		mainActivity.showNewFragment(newFragment, false, null);
		if (isRootScreen(navItemId)) {
			PreferenceUtils.savePrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, this.currentSelectedScreenItemId, false);
		}
	}

	private ABFragment getNewStaticFragmentAt(Integer navItemId) {
		if (navItemId == null) {
			MTLog.w(this, "getNewStaticFragmentAt() > skip (nav item ID null)");
			return null;
		}
		switch (navItemId) {
		case R.id.nav_home:
			return HomeFragment.newInstance(null);
		case R.id.nav_favorites:
			return FavoritesFragment.newInstance();
		case R.id.nav_nearby:
			return NearbyFragment.newNearbyInstance(null, null);
		case R.id.nav_map:
			return MapFragment.newInstance(null, null, null);
		case R.id.nav_news:
			return NewsFragment.newInstance(null, null, null, null, null);
		}
		DataSourceType dst = DataSourceType.parseNavResId(navItemId);
		if (dst != null) {
			return AgencyTypeFragment.newInstance(dst.getId(), dst);
		}
		MTLog.w(this, "getNewStaticFragmentAt() > Unexpected screen nav item ID: %s", navItemId);
		return null;
	}

	private void startNewScreen(Activity activity, Integer navItemId) {
		if (navItemId == null) {
			MTLog.w(this, "startNewScreen() > skip (nav item ID null)");
			return;
		}
		if (activity == null) {
			MTLog.w(this, "startNewScreen() > skip (activity null)");
			return;
		}
		switch (navItemId) {
		case R.id.nav_trip_planner:
			double optSrcLat = 0.0;
			double optSrcLng = 0.0;
			if (activity instanceof MainActivity) {
				Location lastLocation = ((MainActivity) activity).getLastLocation();
				if (lastLocation != null) {
					optSrcLat = lastLocation.getLatitude();
					optSrcLng = lastLocation.getLongitude();
				}
			}
			MapUtils.showDirection(activity, null, null, optSrcLat, optSrcLng, null);
			break;
		case R.id.nav_settings:
			activity.startActivity(PreferencesActivity.newInstance(activity));
			break;
		case R.id.nav_send_feedback:
			LinkUtils.sendEmail(activity);
			break;
		case R.id.nav_rate_review:
			StoreUtils.viewAppPage(activity, Constants.MAIN_APP_PACKAGE_NAME, activity.getString(R.string.google_play));
			break;
		case R.id.nav_support:
			activity.startActivity(PreferencesActivity.newInstance(activity, true));
			break;
		default:
			MTLog.w(this, "startNewScreen() > Unexptected screen nav item ID: %s", navItemId);
		}
	}

	private boolean isRootScreen(Integer navItemId) {
		if (navItemId == null) {
			MTLog.w(this, "isRootScreen() > null (return false)");
			return false;
		}
		switch (navItemId) {
		case R.id.nav_trip_planner:
			return false;
		case R.id.nav_settings:
			return false;
		case R.id.nav_send_feedback:
			return false;
		case R.id.nav_rate_review:
			return false;
		case R.id.nav_support:
			return false;
		default:
			return true;
		}
	}

	public void openDrawer() {
		if (this.drawerLayout != null && this.navigationView != null) {
			this.drawerLayout.openDrawer(this.navigationView);
		}
	}

	public void closeDrawer() {
		if (this.drawerLayout != null && this.navigationView != null) {
			this.drawerLayout.closeDrawer(this.navigationView);
		}
	}

	public boolean isDrawerOpen() {
		return this.drawerLayout != null && this.navigationView != null && this.drawerLayout.isDrawerOpen(this.navigationView);
	}

	public boolean onBackPressed() {
		if (isDrawerOpen()) {
			closeDrawer();
			return true; // processed
		}
		return false; // not processed
	}

	public void setDrawerToggleIndicatorEnabled(boolean enabled) {
		if (this.drawerToggle != null) {
			this.drawerToggle.setDrawerIndicatorEnabled(enabled);
		}
	}

	public void syncDrawerToggleState() {
		if (this.drawerToggle != null) {
			this.drawerToggle.syncState();
		}
	}

	public void onActivityPostCreate() {
		syncDrawerToggleState();
	}

	public void onDrawerToggleConfigurationChanged(Configuration newConfig) {
		if (this.drawerToggle != null) {
			this.drawerToggle.onConfigurationChanged(newConfig);
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		onDrawerToggleConfigurationChanged(newConfig);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (this.drawerToggle != null && this.drawerToggle.onOptionsItemSelected(item)) {
			return true; // processed
		}
		return false; // not processed
	}


	private boolean isCurrentSelectedSet() {
		return this.currentSelectedScreenItemNavId != null && !TextUtils.isEmpty(this.currentSelectedScreenItemId);
	}

	public void setCurrentSelectedItemChecked(boolean checked) {
		if (this.navigationView != null && this.currentSelectedScreenItemNavId != null) {
			if (checked) { // unchecked all others (not automatic because multiple groups not handled by navigation view)
				this.navigationView.getMenu().findItem(this.currentSelectedScreenItemNavId).setCheckable(true);
				this.navigationView.getMenu().findItem(this.currentSelectedScreenItemNavId).setChecked(true);
				uncheckOtherMenuItems();
			} else {
				this.navigationView.getMenu().findItem(this.currentSelectedScreenItemNavId).setCheckable(false);
			}
		}
	}

	private void uncheckOtherMenuItems() {
		this.navigationView.getMenu().findItem(R.id.nav_home).setCheckable(this.currentSelectedScreenItemNavId == R.id.nav_home);
		this.navigationView.getMenu().findItem(R.id.nav_favorites).setCheckable(this.currentSelectedScreenItemNavId == R.id.nav_favorites);
		this.navigationView.getMenu().findItem(R.id.nav_nearby).setCheckable(this.currentSelectedScreenItemNavId == R.id.nav_nearby);
		this.navigationView.getMenu().findItem(R.id.nav_map).setCheckable(this.currentSelectedScreenItemNavId == R.id.nav_map);
		this.navigationView.getMenu().findItem(R.id.nav_news).setCheckable(this.currentSelectedScreenItemNavId == R.id.nav_news);
		for (DataSourceType dst : DataSourceType.values()) {
			if (dst.getNavResId() == this.currentSelectedScreenItemNavId) {
				continue;
			}
			if (!dst.isMenuList()) {
				continue;
			}
			this.navigationView.getMenu().findItem(dst.getNavResId()).setCheckable(false);
		}
		this.navigationView.getMenu().findItem(R.id.nav_settings).setCheckable(this.currentSelectedScreenItemNavId == R.id.nav_settings);
	}

	public void onBackStackChanged(int backStackEntryCount) {
		setCurrentSelectedItemChecked(backStackEntryCount == 0);
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!this.resumed) {
			return;
		}
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		ArrayList<DataSourceType> newAllAgencyTypes = filterAgencyTypes(DataSourceProvider.get(mainActivity).getAvailableAgencyTypes());
		if (CollectionUtils.getSize(this.allAgencyTypes) != CollectionUtils.getSize(newAllAgencyTypes)) {
			this.allAgencyTypes = newAllAgencyTypes; // force reset
			setVisibleMenuItems();
			onMenuUpdated();
			this.modulesUpdated = false; // processed
		} else {
			this.modulesUpdated = false; // nothing to do
		}
	}

	public void onStart() {
		setup();
	}

	private boolean resumed = false;

	public void onResume() {
		this.resumed = true;
		if (this.menuUpdated) {
			onMenuUpdated();
		}
		if (this.modulesUpdated) {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					if (NavigationDrawerController.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
	}

	public void onPause() {
		this.resumed = false;
	}

	public void onStop() {
		TaskUtils.cancelQuietly(this.finishSetupTask, true);
	}

	private static final String EXTRA_SELECTED_ROOT_SCREEN_ID = "extra_selected_root_screen_id";
	private static final String EXTRA_SELECTED_ROOT_SCREEN_NAV_ITEM_ID = "extra_selected_root_screen_nav_item_id";

	public void onSaveState(Bundle outState) {
		if (this.currentSelectedScreenItemNavId != null) {
			outState.putInt(EXTRA_SELECTED_ROOT_SCREEN_NAV_ITEM_ID, this.currentSelectedScreenItemNavId);
		}
		if (!TextUtils.isEmpty(this.currentSelectedScreenItemId)) {
			outState.putString(EXTRA_SELECTED_ROOT_SCREEN_ID, this.currentSelectedScreenItemId);
		}
	}

	public void onRestoreState(Bundle savedInstanceState) {
		Integer newSavedRootScreenNavItem = BundleUtils.getInt(EXTRA_SELECTED_ROOT_SCREEN_NAV_ITEM_ID, savedInstanceState);
		if (newSavedRootScreenNavItem != null && !newSavedRootScreenNavItem.equals(this.currentSelectedScreenItemNavId)) {
			this.currentSelectedScreenItemNavId = newSavedRootScreenNavItem;
		}
		String newRootScreenId = BundleUtils.getString(EXTRA_SELECTED_ROOT_SCREEN_ID, savedInstanceState);
		if (!TextUtils.isEmpty(newRootScreenId) && !newRootScreenId.equals(this.currentSelectedScreenItemId)) {
			this.currentSelectedScreenItemId = newRootScreenId;
		}
	}

	public void destroy() {
		DataSourceProvider.removeModulesUpdateListener(this);
		if (this.mainActivityWR != null) {
			this.mainActivityWR.clear();
			this.mainActivityWR = null;
		}
		this.currentSelectedScreenItemNavId = null;
		this.currentSelectedScreenItemId = null;
		this.navigationView = null;
		if (this.drawerLayout != null) {
			if (this.drawerToggle != null) {
				this.drawerLayout.removeDrawerListener(this.drawerToggle);
			}
			this.drawerLayout = null;
		}
		this.drawerToggle = null;
	}

	private static class ABDrawerToggle extends ActionBarDrawerToggle implements MTLog.Loggable {

		private static final String TAG = MainActivity.class.getSimpleName() + ">" + ABDrawerToggle.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<MainActivity> mainActivityWR;

		public ABDrawerToggle(MainActivity mainActivity, DrawerLayout drawerLayout) {
			super(mainActivity, drawerLayout, R.string.drawer_open, R.string.drawer_close);
			this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
		}

		@Override
		public void onDrawerClosed(View view) {
			MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				ActionBarController abController = mainActivity.getAbController();
				if (abController != null) {
					abController.updateABDrawerClosed();
				}
			}
		}
	}
}
