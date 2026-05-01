package org.mtransit.android.common.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.data.RouteDirection;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class LocalPreferenceRepository extends PreferenceRepository implements MTLog.Loggable {

	public static final String PREFS_LCL_ROOT_SCREEN_ITEM_ID = "pRootScreenItemId";

	public static final String PREFS_LCL_MAP_FILTER_TYPE_IDS = "pMapFilterTypeIds";
	public static final Set<String> PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT = new HashSet<>();

	public static final String PREFS_LCL_NEARBY_TAB_TYPE = "pNearbyTabType";
	public static final int PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT = -1;

	public static final String PREFS_LCL_IP_LOCATION_LAT = "pIpLocationLat";
	public static final String PREFS_LCL_IP_LOCATION_LNG = "pIpLocationLng";
	public static final String PREFS_LCL_IP_LOCATION_TIMESTAMP = "pIpLocationTimestamp";

	public static final String PREF_USER_SEEN_APP_DISABLED = "pSeenDisabledModule";
	public static final boolean PREF_USER_SEEN_APP_DISABLED_DEFAULT = false;

	public static final String PREF_LCL_HIDE_BOOKING_REQUIRED = "pHideBookingReq";
	public static final boolean PREF_LCL_HIDE_BOOKING_REQUIRED_DEFAULT = true;

	public static final String PREFS_LCL_DEV_MODE_ENABLED = "pDevModeEnabled";
	public static final boolean PREFS_LCL_DEV_MODE_ENABLED_DEFAULT = false;

	public static final long PREFS_LCL_RDS_ROUTE_DIRECTION_ID_TAB_DEFAULT = -1L;
	private static final String PREFS_LCL_RDS_ROUTE_DIRECTION_ID_TAB = "pRTSRouteTripIdTab"; // do not change to avoid breaking compat w/ old modules

	@NonNull
	public static String getPREFS_LCL_RDS_ROUTE_DIRECTION_ID_TAB(@NonNull String authority, long routeId) {
		return PREFS_LCL_RDS_ROUTE_DIRECTION_ID_TAB + authority + routeId;
	}

	public static final boolean PREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = true;
	private static final String PREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY = "pRTSRouteTripIdKey"; // do not change to avoid breaking compat w/ old modules

	@NonNull
	public static String getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(@NonNull RouteDirection routeDirection) {
		return getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(
				routeDirection.getAuthority(),
				routeDirection.getRoute().getId(),
				routeDirection.getDirection().getId()
		);
	}

	@NonNull
	private static String getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(@NonNull String authority, long routeId, long directionId) {
		return PREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY + authority + routeId + "-" + directionId;
	}

	public static final String PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT = "";
	private static final String PREFS_LCL_AGENCY_TYPE_TAB_AGENCY = "pAgencyTypeTabAgency";

	@NonNull
	public static String getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(int typeId) {
		return PREFS_LCL_AGENCY_TYPE_TAB_AGENCY + typeId;
	}

	private static final String PREFS_LCL_AGENCY_LAST_OPENED = "pAgencyLastOpened";

	@NonNull
	public static String getPREFS_LCL_AGENCY_LAST_OPENED_DEFAULT(@NonNull String authority) {
		return PREFS_LCL_AGENCY_LAST_OPENED + authority;
	}

	private static final String LOG_TAG = LocalPreferenceRepository.class.getSimpleName();

	@NonNull
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	private SharedPreferences _prefs;

	@Inject
	public LocalPreferenceRepository(@NonNull @ApplicationContext Context appContext) {
		super(appContext);
		TaskUtils.THREAD_POOL_EXECUTOR.execute(() -> _prefs = loadPrefs());
	}

	@SuppressLint("ThreadConstraint") // should almost never call loadPrefs()
	@AnyThread // @WorkerThread
	@NonNull
	public SharedPreferences getPref() {
		if (_prefs == null) {
			_prefs = loadPrefs();
		}
		return _prefs;
	}

	@WorkerThread
	@NonNull
	private SharedPreferences loadPrefs() {
		return makePref(requireContext());
	}

	@WorkerThread
	@NonNull
	public static SharedPreferences makePref(@NonNull Context context) {
		return PreferenceUtils.getPrefLcl(context);
	}
}
