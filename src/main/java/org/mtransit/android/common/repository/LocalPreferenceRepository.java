package org.mtransit.android.common.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.mtransit.android.commons.PreferenceUtils;

import java.util.Set;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class LocalPreferenceRepository extends PreferenceRepository {

	public static final String PREFS_LCL_ROOT_SCREEN_ITEM_ID = PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID;

	public static final String PREFS_LCL_MAP_FILTER_TYPE_IDS = PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS;
	public static final Set<String> PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT = PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT;

	public static final String PREFS_LCL_NEARBY_TAB_TYPE = PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE;

	public static final String PREF_USER_SEEN_APP_DISABLED = PreferenceUtils.PREF_USER_SEEN_APP_DISABLED;
	public static final boolean PREF_USER_SEEN_APP_DISABLED_DEFAULT = PreferenceUtils.PREF_USER_SEEN_APP_DISABLED_DEFAULT;

	public static final String PREFS_LCL_DEV_MODE_ENABLED = PreferenceUtils.PREFS_LCL_DEV_MODE_ENABLED;
	public static final boolean PREFS_LCL_DEV_MODE_ENABLED_DEFAULT = PreferenceUtils.PREFS_LCL_DEV_MODE_ENABLED_DEFAULT;

	public static final long PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT = PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT;

	@NonNull
	public static String getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(@NonNull String authority, long routeId) {
		return PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(authority, routeId);
	}

	public static final boolean PREFS_LCL_RTS_TRIP_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = PreferenceUtils.PREFS_LCL_RTS_TRIP_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT;

	@NonNull
	public static String getPREFS_LCL_RTS_ROUTE_TRIP_ID_KEY(@NonNull String authority, long routeId, long tripId) {
		return PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_KEY(authority, routeId, tripId);
	}

	public static final String PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT = PreferenceUtils.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT;

	@NonNull
	public static String getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(int typeId) {
		return PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(typeId);
	}

	@Nullable
	private SharedPreferences _prefs;

	@Inject
	public LocalPreferenceRepository(@NonNull @ApplicationContext Context appContext) {
		super(appContext);
		Executors.newSingleThreadExecutor().execute(() ->
				_prefs = loadPrefs()
		);
	}

	@WorkerThread
	@NonNull
	private SharedPreferences loadPrefs() {
		return PreferenceUtils.getPrefLcl(requireContext());
	}

	@WorkerThread
	@NonNull
	public SharedPreferences getPref() {
		if (_prefs == null) {
			_prefs = loadPrefs();
		}
		return _prefs;
	}

	@WorkerThread
	@Override
	public boolean hasKey(@NonNull String key) {
		return PreferenceUtils.hasPrefLcl(requireContext(), key);
	}

	@WorkerThread
	@Override
	public boolean getValue(@NonNull String key, boolean defaultValue) {
		return PreferenceUtils.getPrefLcl(requireContext(), key, defaultValue);
	}

	@MainThread
	@Override
	public void saveAsync(@NonNull String key, @Nullable Boolean value) {
		PreferenceUtils.savePrefLclAsync(requireContext(), key, value);
	}

	@WorkerThread
	@Nullable
	@Override
	public String getValue(@NonNull String key, @Nullable String defaultValue) {
		return PreferenceUtils.getPrefLcl(requireContext(), key, defaultValue);
	}

	@WorkerThread
	@NonNull
	@Override
	public String getValueNN(@NonNull String key, @NonNull String defaultValue) {
		return PreferenceUtils.getPrefLclNN(requireContext(), key, defaultValue);
	}

	@MainThread
	@Override
	public void saveAsync(@NonNull String key, @Nullable String value) {
		PreferenceUtils.savePrefLclAsync(requireContext(), key, value);
	}

	@WorkerThread
	@Override
	public int getValue(@NonNull String key, int defaultValue) {
		return PreferenceUtils.getPrefLcl(requireContext(), key, defaultValue);
	}

	@MainThread
	@Override
	public void saveAsync(@NonNull String key, int value) {
		PreferenceUtils.savePrefLclAsync(requireContext(), key, value);
	}

	@WorkerThread
	@Override
	public long getValue(@NonNull String key, long defaultValue) {
		return PreferenceUtils.getPrefLcl(requireContext(), key, defaultValue);
	}

	@MainThread
	@Override
	public void saveAsync(@NonNull String key, long value) {
		PreferenceUtils.savePrefLclAsync(requireContext(), key, value);
	}
}