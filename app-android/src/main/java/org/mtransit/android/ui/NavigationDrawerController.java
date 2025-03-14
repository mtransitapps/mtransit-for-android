package org.mtransit.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.mtransit.android.R;
import org.mtransit.android.ad.AdsConsentManager;
import org.mtransit.android.analytics.AnalyticsEvents;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerExtKt;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.NewsProviderProperties;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.favorites.FavoritesFragment;
import org.mtransit.android.ui.feedback.FeedbackDialog;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.fragment.WebBrowserFragment;
import org.mtransit.android.ui.home.HomeFragment;
import org.mtransit.android.ui.map.MapFragment;
import org.mtransit.android.ui.nearby.NearbyFragment;
import org.mtransit.android.ui.news.NewsListDetailFragment;
import org.mtransit.android.ui.pref.PreferencesActivity;
import org.mtransit.android.ui.type.AgencyTypeFragment;
import org.mtransit.android.util.FragmentUtils;
import org.mtransit.android.util.MapUtils;
import org.mtransit.android.util.SystemSettingManager;
import org.mtransit.android.util.UIFeatureFlags;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("KotlinPairNotCreated")
class NavigationDrawerController implements MTLog.Loggable, NavigationView.OnNavigationItemSelectedListener {

	private static final String LOG_TAG = "Stack-" + NavigationDrawerController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String ITEM_ID_AGENCY_TYPE_START_WITH = "agencytype-";
	private static final String ITEM_ID_STATIC_START_WITH = "static-";
	private static final int ITEM_INDEX_HOME = 0;
	private static final int ITEM_INDEX_FAVORITE = 1;
	private static final int ITEM_INDEX_NEARBY = 2;
	private static final int ITEM_INDEX_MAP = 3;
	private static final int ITEM_INDEX_TRIP_PLANNER = 4;
	private static final int ITEM_INDEX_NEWS = 5;
	private static final int ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT = R.id.root_nav_home;
	private static final String ITEM_ID_SELECTED_SCREEN_DEFAULT = ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME;

	@NonNull
	private final WeakReference<MainActivity> mainActivityWR;
	@NonNull
	private final CrashReporter crashReporter;
	@NonNull
	private final IAnalyticsManager analyticsManager;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;
	@NonNull
	private final StatusLoader statusLoader;
	@NonNull
	private final AdsConsentManager consentManager;
	@SuppressWarnings("FieldCanBeLocal")
	@NonNull
	private final PackageManager packageManager;
	@NonNull
	private final ServiceUpdateLoader serviceUpdateLoader;
	@NonNull
	private final DemoModeManager demoModeManager;
	@Nullable
	private DrawerLayout drawerLayout;
	@Nullable
	private ABDrawerToggle drawerToggle;
	@Nullable
	private NavigationView navigationView;
	@Nullable
	private Integer currentSelectedScreenItemNavId = null;
	@Nullable
	private String currentSelectedScreenItemId = null;

	NavigationDrawerController(@NonNull MainActivity mainActivity,
							   @NonNull CrashReporter crashReporter,
							   @NonNull IAnalyticsManager analyticsManager,
							   @NonNull DataSourcesRepository dataSourcesRepository,
							   @NonNull StatusLoader statusLoader,
							   @NonNull AdsConsentManager consentManager,
							   @NonNull PackageManager packageManager,
							   @NonNull ServiceUpdateLoader serviceUpdateLoader,
							   @NonNull DemoModeManager demoModeManager) {
		this.mainActivityWR = new WeakReference<>(mainActivity);
		this.crashReporter = crashReporter;
		this.dataSourcesRepository = dataSourcesRepository;
		this.analyticsManager = analyticsManager;
		this.statusLoader = statusLoader;
		this.consentManager = consentManager;
		this.packageManager = packageManager;
		this.serviceUpdateLoader = serviceUpdateLoader;
		this.demoModeManager = demoModeManager;
		this.dataSourcesRepository.readingAllSupportedDataSourceTypes().observe(mainActivity, dataSourceTypes -> {
			this.allAgencyTypes = filterAgencyTypes(dataSourceTypes);
			setVisibleMenuItems();
			onMenuUpdated();
		});
		this.dataSourcesRepository.readingHasAgenciesAdded().observe(mainActivity, newHasAgenciesAdded -> {
			this.hasAgenciesAdded = newHasAgenciesAdded;
			setVisibleMenuItems();
			onMenuUpdated();
		});
		this.dataSourcesRepository.readingAllNewsProviders().observe(mainActivity, newsProviderProperties -> {
			ArrayList<NewsProviderProperties> newsProviderList = new ArrayList<>(newsProviderProperties);
			CollectionUtils.removeIfNN(newsProviderList, dst -> !PackageManagerExtKt.isAppEnabled(this.packageManager, dst.getPkg()));
			this.hasNewsProviderEnabled = !newsProviderList.isEmpty();
			setVisibleMenuItems();
			onMenuUpdated();
		});
	}

	public void onCreate(@SuppressWarnings("unused") @Nullable Bundle savedInstanceState) {
		// DO NOTHING
	}

	private void setup() {
		final MainActivity mainActivity = this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		this.navigationView = mainActivity.findViewById(R.id.nav_view);
		this.navigationView.setNavigationItemSelectedListener(this);
		final View headerView = this.navigationView.getHeaderView(0);
		final View drawerHeaderStatusBarBg = headerView == null ? null : headerView.findViewById(R.id.drawer_header_status_bar_bg);
		if (drawerHeaderStatusBarBg != null) {
			EdgeToEdgeKt.applyStatusBarsHeightEdgeToEdge(drawerHeaderStatusBarBg);
		}
		this.drawerLayout = mainActivity.findViewById(R.id.drawer_layout);
		try {
			this.drawerLayout.setDrawerShadow(ContextCompat.getDrawable(mainActivity, R.drawable.drawer_shadow), GravityCompat.START);
		} catch (Resources.NotFoundException nfe) { // seen on Android 4, 5 & 7
			this.crashReporter.w(this, "Error while setting drawer layout shadow!");
		}
		this.drawerToggle = new ABDrawerToggle(mainActivity, this.drawerLayout);
		this.drawerLayout.addDrawerListener(this.drawerToggle);
		if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
			mainActivity.getOnBackPressedDispatcher().addCallback(new InnerOnBackPressedCallback(this.drawerLayout));
		}
		finishSetupAsync();
	}

	private void finishSetupAsync() {
		//noinspection deprecation
		if (this.finishSetupTask != null && this.finishSetupTask.getStatus() == MTCancellableAsyncTask.Status.RUNNING) {
			return;
		}
		this.finishSetupTask = new FinishSetupTask(this);
		TaskUtils.execute(this.finishSetupTask);
	}

	@Nullable
	private FinishSetupTask finishSetupTask = null;

	@SuppressWarnings("deprecation")
	private static class FinishSetupTask extends MTCancellableAsyncTask<Void, String, Pair<String, Boolean>> {

		private final String LOG_TAG = NavigationDrawerController.LOG_TAG + ">" + FinishSetupTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private final WeakReference<NavigationDrawerController> navigationDrawerControllerWR;

		FinishSetupTask(NavigationDrawerController navigationDrawerController) {
			this.navigationDrawerControllerWR = new WeakReference<>(navigationDrawerController);
		}

		@Nullable
		@Override
		protected Pair<String, Boolean> doInBackgroundNotCancelledMT(Void... params) {
			final NavigationDrawerController navigationDrawerController = this.navigationDrawerControllerWR.get();
			if (navigationDrawerController == null) {
				return null;
			}
			final Context context = navigationDrawerController.mainActivityWR.get();
			if (context == null) {
				return null;
			}
			if (navigationDrawerController.isCurrentSelectedSet()) {
				return null;
			}
			String itemId = PreferenceUtils.getPrefLcl(context, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, ITEM_ID_SELECTED_SCREEN_DEFAULT);
			final DemoModeManager demoModeManager = navigationDrawerController.demoModeManager;
			if (demoModeManager.isFullDemo()) {
				itemId = ITEM_ID_SELECTED_SCREEN_DEFAULT;
				if (demoModeManager.isEnabledBrowseScreen()) {
					itemId = ITEM_ID_AGENCY_TYPE_START_WITH + demoModeManager.getFilterTypeId();
				}
			}
			publishProgress(itemId);
			final Boolean showDrawerLearning = navigationDrawerController.shouldShowDrawerLearning();
			return new Pair<>(itemId, showDrawerLearning);
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable Pair<String, Boolean> itemIdAndUserHasLearned) {
			final NavigationDrawerController navigationDrawerController = this.navigationDrawerControllerWR.get();
			if (navigationDrawerController == null) {
				return;
			}
			if (isCancelled()) {
				return;
			}
			navigationDrawerController.setVisibleMenuItems();
			final String itemId = itemIdAndUserHasLearned == null ? null : itemIdAndUserHasLearned.first;
			final Boolean showDrawerLearning = itemIdAndUserHasLearned == null ? null : itemIdAndUserHasLearned.second;
			selectItemId(itemId);
			if (Boolean.TRUE.equals(showDrawerLearning)) {
				navigationDrawerController.openDrawer();
				navigationDrawerController.setUserLearnedDrawer();
			}
		}

		private void selectItemId(@Nullable String itemId) {
			if (TextUtils.isEmpty(itemId)) {
				return;
			}
			final NavigationDrawerController navigationDrawerController = this.navigationDrawerControllerWR.get();
			if (navigationDrawerController == null) {
				return;
			}
			final Integer navItemId = navigationDrawerController.getScreenNavItemId(itemId);
			navigationDrawerController.selectItem(navItemId, false);
		}

		@Override
		protected void onProgressUpdateNotCancelledMT(@Nullable String... itemIds) {
			selectItemId(itemIds == null || itemIds.length == 0 ? null : itemIds[0]);
		}
	}

	@WorkerThread
	private boolean shouldShowDrawerLearning() {
		return this.dataSourcesRepository.hasAgenciesEnabled() && !hasUserLearnedDrawer();
	}

	@Nullable
	private Boolean userLearnedDrawer = null;

	@WorkerThread
	private boolean hasUserLearnedDrawer() {
		if (this.userLearnedDrawer == null) {
			final MainActivity mainActivity = this.mainActivityWR.get();
			if (mainActivity != null) {
				this.userLearnedDrawer = PreferenceUtils.getPrefDefault(mainActivity, PreferenceUtils.PREF_USER_LEARNED_DRAWER,
						PreferenceUtils.PREF_USER_LEARNED_DRAWER_DEFAULT);
			}
		}
		return this.userLearnedDrawer != null && this.userLearnedDrawer;
	}

	private void setUserLearnedDrawer() {
		this.userLearnedDrawer = true;
		Context context = this.mainActivityWR.get();
		if (context != null) {
			PreferenceUtils.savePrefDefaultAsync(context, PreferenceUtils.PREF_USER_LEARNED_DRAWER, this.userLearnedDrawer);
		}
	}

	@Nullable
	private Boolean hasAgenciesAdded = null;

	@Nullable
	private Boolean hasNewsProviderEnabled = null;

	@Nullable
	private List<DataSourceType> allAgencyTypes = null;

	@Nullable
	private List<DataSourceType> getAllAgencyTypes() {
		if (this.allAgencyTypes == null) {
			initAllAgencyTypes();
		}
		return this.allAgencyTypes;
	}

	private void initAllAgencyTypes() {
		this.allAgencyTypes = getNewFilteredAgencyTypes();
	}

	@WorkerThread
	@NonNull
	private List<DataSourceType> getNewFilteredAgencyTypes() {
		//noinspection deprecation // FIXME
		final List<DataSourceType> availableAgencyTypes = this.dataSourcesRepository.getAllSupportedDataSourceTypes();
		return filterAgencyTypes(availableAgencyTypes);
	}

	@NonNull
	private List<DataSourceType> filterAgencyTypes(@Nullable List<DataSourceType> availableAgencyTypes) {
		List<DataSourceType> filteredDataSourceTypes = new ArrayList<>();
		if (availableAgencyTypes != null) {
			for (DataSourceType type : availableAgencyTypes) {
				if (!type.isMenuList()) {
					continue;
				}
				filteredDataSourceTypes.add(type);
			}
		}
		return filteredDataSourceTypes;
	}

	public void setVisibleMenuItems() {
		if (this.navigationView == null) {
			MTLog.w(this, "setVisibleMenuItems() > skip (no navigation view)");
			return;
		}
		final boolean hasAgenciesAdded = Boolean.TRUE.equals(this.hasAgenciesAdded);
		this.navigationView.getMenu().findItem(R.id.root_nav_map).setVisible(hasAgenciesAdded);
		// TODO favorites? (favorite manager requires IO
		final boolean hasNewsProviderEnabled = Boolean.TRUE.equals(this.hasNewsProviderEnabled);
		this.navigationView.getMenu().findItem(R.id.root_nav_news).setVisible(hasNewsProviderEnabled);
		List<DataSourceType> allAgencyTypes = getAllAgencyTypes();
		for (DataSourceType dst : DataSourceType.values()) {
			if (allAgencyTypes != null && allAgencyTypes.contains(dst)) {
				continue;
			}
			if (!dst.isMenuList()) {
				continue;
			}
			final MenuItem dstMenuItem = this.navigationView.getMenu().findItem(dst.getNavResId());
			if (dstMenuItem == null) {
				continue;
			}
			dstMenuItem.setVisible(false);
		}
		if (allAgencyTypes != null) {
			for (DataSourceType dst : allAgencyTypes) {
				MenuItem dstMenuItem = this.navigationView.getMenu().findItem(dst.getNavResId());
				if (dstMenuItem == null) {
					dstMenuItem = this.navigationView.getMenu().add(R.id.drawer_modules, dst.getNavResId(), Menu.NONE, dst.getShortNamesResId());
					dstMenuItem.setIcon(dst.getIconResId());
				}
				dstMenuItem.setVisible(true);
			}
		}
		this.navigationView.getMenu().findItem(R.id.nav_rate_review).setVisible(hasAgenciesAdded);
		this.navigationView.getMenu().findItem(R.id.nav_support).setVisible(hasAgenciesAdded);
		this.navigationView.getMenu().findItem(R.id.nav_privacy_setting).setVisible(this.consentManager.isPrivacyOptionsRequired());
	}

	private boolean menuUpdated = false;

	private void onMenuUpdated() {
		this.menuUpdated = true;
		if (this.navigationView == null) {
			return;
		}
		final MainActivity mainActivity = this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		if (!mainActivity.isMTResumed()) {
			return;
		}
		String itemId = PreferenceUtils.getPrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, ITEM_ID_SELECTED_SCREEN_DEFAULT);
		if (demoModeManager.isFullDemo()) {
			itemId = ITEM_ID_SELECTED_SCREEN_DEFAULT;
			if (demoModeManager.isEnabledBrowseScreen()) {
				itemId = ITEM_ID_AGENCY_TYPE_START_WITH + demoModeManager.getFilterTypeId();
			}
		}
		Integer newSelectedNavItemId = getScreenNavItemId(itemId);
		if (this.currentSelectedScreenItemNavId != null && this.currentSelectedScreenItemNavId.equals(newSelectedNavItemId)
				&& this.currentSelectedScreenItemId != null && this.currentSelectedScreenItemId.equals(itemId)) {
			//noinspection ConstantConditions
			this.currentSelectedScreenItemNavId = newSelectedNavItemId;
			setCurrentSelectedItemChecked(mainActivity.getBackStackEntryCount() == 0);
			return;
		}
		selectItem(newSelectedNavItemId, false);
		this.menuUpdated = false; // processed
	}

	@Nullable
	private String getScreenItemId(@Nullable Integer navItemId, boolean isUsingFirebaseTestLab) {
		if (navItemId == null) {
			return null;
		}
		if (navItemId == R.id.root_nav_home) {
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME;
		} else if (navItemId == R.id.root_nav_favorites) {
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_FAVORITE;
		} else if (navItemId == R.id.root_nav_nearby) {
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEARBY;
		} else if (navItemId == R.id.root_nav_map) {
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_MAP;
		} else if (navItemId == R.id.nav_trip_planner) {
			return isUsingFirebaseTestLab ? null // NOT ROOT SCREEN
					: ITEM_ID_STATIC_START_WITH + ITEM_INDEX_TRIP_PLANNER;
		} else if (navItemId == R.id.root_nav_news) {
			return ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEWS;
		} else if (navItemId == R.id.root_nav_light_rail) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_LIGHT_RAIL.getId();
		} else if (navItemId == R.id.root_nav_tram) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_TRAM.getId();
		} else if (navItemId == R.id.root_nav_subway) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_SUBWAY.getId();
		} else if (navItemId == R.id.root_nav_rail) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_RAIL.getId();
		} else if (navItemId == R.id.root_nav_bus) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BUS.getId();
		} else if (navItemId == R.id.root_nav_ferry) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_FERRY.getId();
		} else if (navItemId == R.id.root_nav_bike) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BIKE.getId();
		} else if (navItemId == R.id.root_nav_module) {
			return ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_MODULE.getId();
		} else if (navItemId == R.id.nav_settings
				|| navItemId == R.id.nav_support
				|| navItemId == R.id.nav_privacy_setting
				|| navItemId == R.id.nav_rate_review
				|| navItemId == R.id.nav_send_feedback) {
			return null;
		}
		MTLog.w(this, "Unexpected screen nav item ID '%s'!", navItemId);
		return null;
	}

	@NonNull
	private Integer getScreenNavItemId(@Nullable String itemId) {
		if (itemId == null || itemId.isEmpty()) {
			return ITEM_ID_SELECTED_SCREEN_NAV_ITEM_DEFAULT;
		}
		if (itemId.startsWith(ITEM_ID_STATIC_START_WITH)) {
			try {
				switch (Integer.parseInt(itemId.substring(ITEM_ID_STATIC_START_WITH.length()))) {
				case ITEM_INDEX_HOME:
					return R.id.root_nav_home;
				case ITEM_INDEX_FAVORITE:
					return R.id.root_nav_favorites;
				case ITEM_INDEX_NEARBY:
					return R.id.root_nav_nearby;
				case ITEM_INDEX_MAP:
					return R.id.root_nav_map;
				case ITEM_INDEX_TRIP_PLANNER:
					return R.id.nav_trip_planner;
				case ITEM_INDEX_NEWS:
					return R.id.root_nav_news;
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
				final DataSourceType dst = DataSourceType.parseId(
						Integer.parseInt(
								itemId.substring(ITEM_ID_AGENCY_TYPE_START_WITH.length())
						)
				);
				if (dst != null) {
					final List<DataSourceType> allAgencyTypes = getAllAgencyTypes();
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

	@SuppressWarnings("unused")
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

	private void selectItem(@Nullable Integer navItemId, boolean clearStack) {
		if (navItemId == null) {
			return;
		}
		final MainActivity mainActivity = this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		final boolean isUsingFirebaseTestLab = SystemSettingManager.isUsingFirebaseTestLab(mainActivity);
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
		if (!isRootScreen(navItemId, isUsingFirebaseTestLab)) {
			startNewScreen(mainActivity, navItemId);
			setCurrentSelectedItemChecked(true); // keep current position
			return;
		}
		final ABFragment newFragment = getNewStaticFragmentAt(navItemId, mainActivity);
		if (newFragment == null) {
			return;
		}
		this.currentSelectedScreenItemNavId = navItemId;
		this.currentSelectedScreenItemId = getScreenItemId(navItemId, isUsingFirebaseTestLab);
		mainActivity.clearFragmentBackStackImmediate(); // root screen
		this.statusLoader.clearAllTasks();
		this.serviceUpdateLoader.clearAllTasks();
		mainActivity.showNewFragment(newFragment, false, null);
		if (demoModeManager.isFullDemo()) {
			return; // SKIP (demo mode ON)
		}
		if (isRootScreen(navItemId, isUsingFirebaseTestLab)) {
			PreferenceUtils.savePrefLclAsync(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, this.currentSelectedScreenItemId);
		}
	}

	@Nullable
	private ABFragment getNewStaticFragmentAt(@Nullable Integer navItemId, @NonNull Activity activity) {
		if (navItemId == null) {
			MTLog.w(this, "getNewStaticFragmentAt() > skip (nav item ID null)");
			return null;
		}
		if (navItemId == R.id.root_nav_home) {
			return HomeFragment.newInstance();
		} else if (navItemId == R.id.root_nav_favorites) {
			return FavoritesFragment.newInstance();
		} else if (navItemId == R.id.root_nav_nearby) {
			return NearbyFragment.newNearbyInstance();
		} else if (navItemId == R.id.root_nav_map) {
			return MapFragment.newInstance();
		} else if (navItemId == R.id.nav_trip_planner) {
			this.analyticsManager.logEvent(AnalyticsEvents.OPENED_GOOGLE_MAPS_TRIP_PLANNER);
			final Pair<Double, Double> srcLatLng = getTripPlannerSrcLatLng(activity);
			return WebBrowserFragment.newInstance(
					MapUtils.getMapsDirectionUrl(null, null, srcLatLng.first, srcLatLng.second, null).toString()
			);
		} else if (navItemId == R.id.root_nav_news) {
			return NewsListDetailFragment.newInstance();
		}
		final DataSourceType dst = DataSourceType.parseNavResId(navItemId);
		if (dst != null) {
			return AgencyTypeFragment.newInstance(dst);
		}
		MTLog.w(this, "getNewStaticFragmentAt() > Unexpected screen nav item ID: %s", navItemId);
		return null;
	}

	private void startNewScreen(@Nullable Activity activity, @Nullable Integer navItemId) {
		if (navItemId == null) {
			MTLog.w(this, "startNewScreen() > skip (nav item ID null)");
			return;
		}
		if (activity == null) {
			MTLog.w(this, "startNewScreen() > skip (activity null)");
			return;
		}
		if (navItemId == R.id.nav_trip_planner) {
			this.analyticsManager.logEvent(AnalyticsEvents.OPENED_GOOGLE_MAPS_TRIP_PLANNER);
			final Pair<Double, Double> srcLatLng = getTripPlannerSrcLatLng(activity);
			MapUtils.showDirection(null, activity, null, null, srcLatLng.first, srcLatLng.second, null);
		} else if (navItemId == R.id.nav_settings) {
			activity.startActivity(PreferencesActivity.newInstance(activity));
		} else if (navItemId == R.id.nav_send_feedback) {
			if (FeatureFlags.F_NAVIGATION) {
				// TODO navigate to dialog
			} else {
				if (activity instanceof MainActivity) {
					FragmentUtils.replaceDialogFragment(
							(MainActivity) activity,
							FragmentUtils.DIALOG_TAG,
							FeedbackDialog.newInstance(),
							null
					);
				}
			}
		} else if (navItemId == R.id.nav_rate_review) {
			StoreUtils.viewAppPage(activity, Constants.MAIN_APP_PACKAGE_NAME, activity.getString(org.mtransit.android.commons.R.string.google_play));
		} else if (navItemId == R.id.nav_support) {
			activity.startActivity(PreferencesActivity.newInstance(activity, true));
		} else if (navItemId == R.id.nav_privacy_setting) {
			this.consentManager.showPrivacyOptionsForm(activity, formError -> {
				if (formError != null) {
					Toast.makeText(activity, formError.getMessage(), Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			MTLog.w(this, "startNewScreen() > Unexpected screen nav item ID: %s", navItemId);
		}
	}

	@NonNull
	private Pair<Double, Double> getTripPlannerSrcLatLng(Activity activity) {
		Double optSrcLat = null;
		Double optSrcLng = null;
		if (activity instanceof MainActivity) {
			Location lastLocation = ((MainActivity) activity).getLastLocation();
			if (lastLocation != null) {
				optSrcLat = lastLocation.getLatitude();
				optSrcLng = lastLocation.getLongitude();
			}
		}
		return new Pair<>(optSrcLat, optSrcLng);
	}

	private boolean isRootScreen(@Nullable Integer navItemId, boolean isUsingFirebaseTestLab) {
		if (navItemId == null) {
			MTLog.w(this, "isRootScreen() > null (return false)");
			return false;
		}
		//noinspection RedundantIfStatement
		if ((navItemId == R.id.nav_trip_planner && isUsingFirebaseTestLab)
				|| navItemId == R.id.nav_support
				|| navItemId == R.id.nav_rate_review
				|| navItemId == R.id.nav_privacy_setting
				|| navItemId == R.id.nav_send_feedback
				|| navItemId == R.id.nav_settings) {
			return false;
		}
		return true;
	}

	private void openDrawer() {
		if (this.drawerLayout != null && this.navigationView != null) {
			this.drawerLayout.openDrawer(this.navigationView);
		}
	}

	private void closeDrawer() {
		if (this.drawerLayout != null && this.navigationView != null) {
			this.drawerLayout.closeDrawer(this.navigationView);
		}
	}

	boolean isDrawerOpen() {
		return this.drawerLayout != null && this.navigationView != null && this.drawerLayout.isDrawerOpen(this.navigationView);
	}

	boolean onBackPressed() {
		if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
			return false;
		}
		if (isDrawerOpen()) {
			closeDrawer();
			return true; // processed
		}
		return false; // not processed
	}

	private static class InnerOnBackPressedCallback extends OnBackPressedCallback implements DrawerLayout.DrawerListener {

		@NonNull
		private final WeakReference<DrawerLayout> drawerLayoutWR;

		InnerOnBackPressedCallback(@NonNull DrawerLayout drawerLayout) {
			super(false);
			this.drawerLayoutWR = new WeakReference<>(drawerLayout);
			drawerLayout.addDrawerListener(this);
		}

		@Override
		public void handleOnBackPressed() {
			final DrawerLayout drawerLayout = this.drawerLayoutWR.get();
			if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
				drawerLayout.closeDrawer(GravityCompat.START);
			}
		}


		@Override
		public void onDrawerOpened(@NonNull View drawerView) {
			setEnabled(true);
		}

		@Override
		public void onDrawerClosed(@NonNull View drawerView) {
			setEnabled(false);
		}

		@Override
		public void onDrawerStateChanged(int newState) {
			// DO NOTHING
		}

		@Override
		public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
			// DO NOTHING
		}
	}

	void setDrawerToggleIndicatorEnabled(boolean enabled) {
		if (this.drawerToggle != null) {
			this.drawerToggle.setDrawerIndicatorEnabled(enabled);
		}
	}

	private void syncDrawerToggleState() {
		if (this.drawerToggle != null) {
			this.drawerToggle.syncState();
		}
	}

	void onActivityPostCreate() {
		syncDrawerToggleState();
	}

	private void onDrawerToggleConfigurationChanged(Configuration newConfig) {
		if (this.drawerToggle != null) {
			this.drawerToggle.onConfigurationChanged(newConfig);
		}
	}

	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		onDrawerToggleConfigurationChanged(newConfig);
	}

	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		//noinspection RedundantIfStatement
		if (this.drawerToggle != null && this.drawerToggle.onOptionsItemSelected(item)) {
			return true; // processed
		}
		return false; // not processed
	}

	private boolean isCurrentSelectedSet() {
		return this.currentSelectedScreenItemNavId != null && !TextUtils.isEmpty(this.currentSelectedScreenItemId);
	}

	void setCurrentSelectedItemChecked(boolean checked) {
		if (this.navigationView != null && this.currentSelectedScreenItemNavId != null) {
			if (checked) { // unchecked all others (not automatic because multiple groups not handled by navigation view)
				this.navigationView.getMenu().findItem(this.currentSelectedScreenItemNavId).setCheckable(true);
				this.navigationView.getMenu().findItem(this.currentSelectedScreenItemNavId).setChecked(true);
				uncheckOtherMenuItems(this.navigationView, this.currentSelectedScreenItemNavId);
			} else {
				this.navigationView.getMenu().findItem(this.currentSelectedScreenItemNavId).setCheckable(false);
			}
		}
	}

	private static void uncheckOtherMenuItems(@NonNull NavigationView navigationView, int currentSelectedScreenItemNavId) {
		navigationView.getMenu().findItem(R.id.root_nav_home).setCheckable(currentSelectedScreenItemNavId == R.id.root_nav_home);
		navigationView.getMenu().findItem(R.id.root_nav_favorites).setCheckable(currentSelectedScreenItemNavId == R.id.root_nav_favorites);
		navigationView.getMenu().findItem(R.id.root_nav_nearby).setCheckable(currentSelectedScreenItemNavId == R.id.root_nav_nearby);
		navigationView.getMenu().findItem(R.id.root_nav_map).setCheckable(currentSelectedScreenItemNavId == R.id.root_nav_map);
		navigationView.getMenu().findItem(R.id.nav_trip_planner).setCheckable(currentSelectedScreenItemNavId == R.id.nav_trip_planner);
		navigationView.getMenu().findItem(R.id.root_nav_news).setCheckable(currentSelectedScreenItemNavId == R.id.root_nav_news);
		for (DataSourceType dst : DataSourceType.values()) {
			if (dst.getNavResId() == currentSelectedScreenItemNavId) {
				continue;
			}
			if (!dst.isMenuList()) {
				continue;
			}
			navigationView.getMenu().findItem(dst.getNavResId()).setCheckable(false);
		}
		navigationView.getMenu().findItem(R.id.nav_settings).setCheckable(currentSelectedScreenItemNavId == R.id.nav_settings);
		navigationView.getMenu().findItem(R.id.nav_send_feedback).setCheckable(currentSelectedScreenItemNavId == R.id.nav_send_feedback);
		navigationView.getMenu().findItem(R.id.nav_rate_review).setCheckable(currentSelectedScreenItemNavId == R.id.nav_rate_review);
		navigationView.getMenu().findItem(R.id.nav_privacy_setting).setCheckable(currentSelectedScreenItemNavId == R.id.nav_privacy_setting);
		navigationView.getMenu().findItem(R.id.nav_support).setCheckable(currentSelectedScreenItemNavId == R.id.nav_support);
	}

	void onBackStackChanged(int backStackEntryCount) {
		setCurrentSelectedItemChecked(backStackEntryCount == 0);
	}

	void onStart() {
		setup();
	}

	public void onResume() {
		if (this.menuUpdated) {
			onMenuUpdated();
		}
	}

	public void onPause() {
		// DO NOTHING
	}

	void onStop() {
		TaskUtils.cancelQuietly(this.finishSetupTask, true);
	}

	private static final String EXTRA_SELECTED_ROOT_SCREEN_ID = "extra_selected_root_screen_id";
	private static final String EXTRA_SELECTED_ROOT_SCREEN_NAV_ITEM_ID = "extra_selected_root_screen_nav_item_id";

	void onSaveState(Bundle outState) {
		if (this.currentSelectedScreenItemNavId != null) {
			outState.putInt(EXTRA_SELECTED_ROOT_SCREEN_NAV_ITEM_ID, this.currentSelectedScreenItemNavId);
		}
		if (!TextUtils.isEmpty(this.currentSelectedScreenItemId)) {
			outState.putString(EXTRA_SELECTED_ROOT_SCREEN_ID, this.currentSelectedScreenItemId);
		}
	}

	void onRestoreState(Bundle savedInstanceState) {
		Integer newSavedRootScreenNavItem = BundleUtils.getInt(EXTRA_SELECTED_ROOT_SCREEN_NAV_ITEM_ID, savedInstanceState);
		if (newSavedRootScreenNavItem != null && !newSavedRootScreenNavItem.equals(this.currentSelectedScreenItemNavId)) {
			this.currentSelectedScreenItemNavId = newSavedRootScreenNavItem;
		}
		String newRootScreenId = BundleUtils.getString(EXTRA_SELECTED_ROOT_SCREEN_ID, savedInstanceState);
		if (newRootScreenId != null && !newRootScreenId.equals(this.currentSelectedScreenItemId)) {
			this.currentSelectedScreenItemId = newRootScreenId;
		}
	}

	void destroy() {
		this.mainActivityWR.clear();
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

		private static final String LOG_TAG = MainActivity.class.getSimpleName() + ">" + ABDrawerToggle.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final WeakReference<MainActivity> mainActivityWR;

		ABDrawerToggle(MainActivity mainActivity, DrawerLayout drawerLayout) {
			super(mainActivity, drawerLayout, R.string.drawer_open, R.string.drawer_close);
			this.mainActivityWR = new WeakReference<>(mainActivity);
		}

		@Override
		public void onDrawerClosed(View view) {
			final MainActivity mainActivity = this.mainActivityWR.get();
			if (mainActivity == null) {
				return;
			}
			final ActionBarController abController = mainActivity.getAbController();
			if (abController == null) {
				return;
			}
			abController.updateABDrawerClosed();
		}
	}
}
