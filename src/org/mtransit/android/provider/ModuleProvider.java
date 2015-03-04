package org.mtransit.android.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.R;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POI.POIUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.provider.AgencyProvider;
import org.mtransit.android.commons.provider.ContentProviderConstants;
import org.mtransit.android.commons.provider.POIDbHelper;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.POIProvider.POIColumns;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.provider.StatusProvider;
import org.mtransit.android.commons.provider.StatusProviderContract;
import org.mtransit.android.data.Module;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class ModuleProvider extends AgencyProvider implements POIProviderContract, StatusProviderContract {

	private static final String TAG = ModuleProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Override
	public String toString() {
		return getLogTag();
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	private static final String PREF_KEY_LAST_UPDATE_MS = ModuleDbHelper.PREF_KEY_LAST_UPDATE_MS;

	private static final long MODULE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(7);
	private static final long MODULE_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);

	private static final long MODULE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long MODULE_STATUS_VALIDITY_IN_MS = TimeUnit.SECONDS.toMillis(30);
	private static final long MODULE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(15);
	private static final long MODULE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.SECONDS.toMillis(20);
	private static final long MODULE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private static ModuleDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = AgencyProvider.getNewUriMatcher(authority);
		StatusProvider.append(URI_MATCHER, authority);
		POIProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.module_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	public static Uri getAUTHORITYURI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
	}

	private ModuleDbHelper getDBHelper(Context context) {
		if (dbHelper == null) { // initialize
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Exception e) { // reset
				MTLog.w(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	@Override
	public Context getContentProviderContext() {
		return getContext();
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		try {
			Cursor cursor = super.queryMT(uri, projection, selection, selectionArgs, sortOrder);
			if (cursor != null) {
				return cursor;
			}
			cursor = POIProvider.queryS(this, uri, selection);
			if (cursor != null) {
				return cursor;
			}
			cursor = StatusProvider.queryS(this, uri, selection);
			if (cursor != null) {
				return cursor;
			}
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while resolving query '%s'!", uri);
			return null;
		}
	}

	@Override
	public String getSortOrder(Uri uri) {
		String sortOrder = POIProvider.getSortOrderS(this, uri);
		if (sortOrder != null) {
			return sortOrder;
		}
		sortOrder = StatusProvider.getSortOrderS(this, uri);
		if (sortOrder != null) {
			return sortOrder;
		}
		return super.getSortOrder(uri);
	}

	@Override
	public String getTypeMT(Uri uri) {
		String type = POIProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		type = StatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		return super.getTypeMT(uri);
	}

	@Override
	public int updateMT(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insertMT(Uri uri, ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	@Override
	public int deleteMT(Uri uri, String selection, String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public Cursor getSearchSuggest(String query) {
		return ContentProviderConstants.EMPTY_CURSOR; // no search suggest for modules
	}

	@Override
	public HashMap<String, String> getSearchSuggestProjectionMap() {
		return null; // no search suggest for modules
	}

	@Override
	public String getSearchSuggestTable() {
		return null; // no search suggest for modules
	}

	@Override
	public Cursor getPOI(POIFilter poiFilter) {
		updateModuleDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public Cursor getPOIFromDB(POIFilter poiFilter) {
		return POIProvider.getDefaultPOIFromDB(poiFilter, this);
	}

	public long getMODULE_MAX_VALIDITY_IN_MS() {
		return MODULE_MAX_VALIDITY_IN_MS;
	}

	public long getMODULE_VALIDITY_IN_MS() {
		return MODULE_VALIDITY_IN_MS;
	}

	public void updateModuleDataIfRequired() {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l);
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getMODULE_MAX_VALIDITY_IN_MS() < nowInMs) { // too old to display?
			deleteAllModuleData();
			updateAllModuleDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getMODULE_VALIDITY_IN_MS() < nowInMs) { // try to refresh?
			updateAllModuleDataFromWWW(lastUpdateInMs);
		}
	}

	private int deleteAllModuleData() {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = getDBHelper(getContext()).getWritableDatabase();
			affectedRows = db.delete(ModuleDbHelper.T_MODULE, null, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all module data!");
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return affectedRows;
	}

	private synchronized void updateAllModuleDataFromWWW(long oldLastUpdatedInMs) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l) > oldLastUpdatedInMs) {
			return; // too late, another thread already updated
		}
		loadDataFromWWW();
	}

	private HashSet<Module> loadDataFromWWW() {
		try {
			long newLastUpdateInMs = TimeUtils.currentTimeMillis();
			int fileResId = R.raw.modules;
			String jsonString = FileUtils.fromFileRes(getContext(), fileResId);
			HashSet<Module> modules = new HashSet<Module>();
			JSONArray jsonArray = new JSONArray(jsonString);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jModule = jsonArray.getJSONObject(i);
				Module module = Module.fromSimpleJSONStatic(jModule, getAUTHORITY(getContext()));
				if (module == null) {
					continue; // error while converting JSON to Module
				}
				module.setId(i);
				modules.add(module);
			}
			deleteAllModuleData();
			insertModulesLockDB(this, modules);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
			return modules;
		} catch (Exception e) {
			MTLog.w(this, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static synchronized int insertModulesLockDB(POIProviderContract provider, Collection<Module> defaultPOIs) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.beginTransaction(); // start the transaction
			if (defaultPOIs != null) {
				for (DefaultPOI defaultPOI : defaultPOIs) {
					long rowId = db.insert(provider.getPOITable(), POIDbHelper.T_POI_K_ID, defaultPOI.toContentValues());
					if (rowId > 0) {
						affectedRows++;
					}
				}
			}
			db.setTransactionSuccessful(); // mark the transaction as successful
		} catch (Exception e) {
			MTLog.w(TAG, e, "ERROR while applying batch update to the database!");
		} finally {
			SqlUtils.endTransactionAndCloseQuietly(db);
		}
		return affectedRows;
	}

	@Override
	public POIStatus getNewStatus(StatusProviderContract.Filter filter) {
		if (!(filter instanceof AppStatus.AppStatusFilter)) {
			return null;
		}
		AppStatus.AppStatusFilter moduleStatusFilter = (AppStatus.AppStatusFilter) filter;
		return getNewModuleStatus(moduleStatusFilter);
	}

	public POIStatus getNewModuleStatus(AppStatus.AppStatusFilter filter) {
		long newLastUpdateInMs = TimeUtils.currentTimeMillis();
		boolean appInstalled = PackageManagerUtils.isAppInstalled(getContext(), filter.getPkg());
		return new AppStatus(filter.getTargetUUID(), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs, appInstalled);
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusProviderContract.Filter statusFilter) {
		return StatusProvider.getCachedStatusS(this, statusFilter.getTargetUUID());
	}

	@Override
	public boolean purgeUselessCachedStatuses() {
		return StatusProvider.purgeUselessCachedStatuses(this);
	}

	@Override
	public boolean deleteCachedStatus(int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(this, cachedStatusId);
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITYURI(getContext());
	}

	@Override
	public String getStatusDbTableName() {
		return ModuleDbHelper.T_MODULE_STATUS;
	}

	@Override
	public boolean isAgencyDeployed() {
		return SqlUtils.isDbExist(getContext(), getDbName());
	}

	@Override
	public boolean isAgencySetupRequired() {
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			setupRequired = true;
		} else if (!SqlUtils.isDbExist(getContext(), getDbName())) {
			setupRequired = true;
		} else if (SqlUtils.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
			setupRequired = true;
		}
		return setupRequired;
	}

	@Override
	public UriMatcher getAgencyUriMatcher() {
		return getURIMATCHER(getContext());
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_APP;
	}

	@Override
	public int getAgencyVersion() {
		return getCurrentDbVersion();
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	@Override
	public int getAgencyLabelResId() {
		return R.string.module_label;
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	@Override
	public String getAgencyColorString(Context context) {
		return null; // default
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	@Override
	public int getAgencyShortNameResId() {
		return R.string.module_short_name;
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	@Override
	public Area getAgencyArea(Context context) {
		return new Area(-90.0, +90.0, -180.0, +180.0); // the whole world
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	public String getDbName() {
		return ModuleDbHelper.DB_NAME;
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return ModuleDbHelper.getDbVersion();
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	public ModuleDbHelper getNewDbHelper(Context context) {
		return new ModuleDbHelper(context.getApplicationContext());
	}

	@Override
	public long getStatusMaxValidityInMs() {
		return MODULE_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return MODULE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return MODULE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return MODULE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return MODULE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	private static HashMap<String, String> poiProjectionMap;

	@Override
	public HashMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			poiProjectionMap = getNewPoiProjectionMap(getAUTHORITY(getContext()));
		}
		return poiProjectionMap;
	}

	public static HashMap<String, String> getNewPoiProjectionMap(String authority) {
		HashMap<String, String> poiProjectionMap = new HashMap<String, String>();
		poiProjectionMap.put(POIColumns.T_POI_K_UUID_META, SqlUtils.concatenate("'" + POIUtils.UID_SEPARATOR + "'", //
				"'" + authority + "'", //
				ModuleDbHelper.T_MODULE + "." + ModuleDbHelper.T_MODULE_K_PKG //
		) + " AS " + POIColumns.T_POI_K_UUID_META);
		poiProjectionMap.put(POIColumns.T_POI_K_DST_ID_META, Module.DST_ID + " AS " + POIColumns.T_POI_K_DST_ID_META);
		poiProjectionMap.put(POIColumns.T_POI_K_ID, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_ID + " AS " + POIColumns.T_POI_K_ID);
		poiProjectionMap.put(POIColumns.T_POI_K_NAME, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_NAME + " AS " + POIColumns.T_POI_K_NAME);
		poiProjectionMap.put(POIColumns.T_POI_K_LAT, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_LAT + " AS " + POIColumns.T_POI_K_LAT);
		poiProjectionMap.put(POIColumns.T_POI_K_LNG, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_LNG + " AS " + POIColumns.T_POI_K_LNG);
		poiProjectionMap.put(POIColumns.T_POI_K_TYPE, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_TYPE + " AS " + POIColumns.T_POI_K_TYPE);
		poiProjectionMap.put(POIColumns.T_POI_K_STATUS_TYPE, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_STATUS_TYPE + " AS "
				+ POIColumns.T_POI_K_STATUS_TYPE);
		poiProjectionMap.put(POIColumns.T_POI_K_ACTIONS_TYPE, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_ACTIONS_TYPE + " AS "
				+ POIColumns.T_POI_K_ACTIONS_TYPE);
		poiProjectionMap.put(ModuleColumns.T_MODULE_K_PKG, ModuleDbHelper.T_MODULE + "." + ModuleDbHelper.T_MODULE_K_PKG + " AS "
				+ ModuleColumns.T_MODULE_K_PKG);
		poiProjectionMap.put(ModuleColumns.T_MODULE_K_TARGET_TYPE_ID, ModuleDbHelper.T_MODULE + "." + ModuleDbHelper.T_MODULE_K_TARGET_TYPE_ID + " AS "
				+ ModuleColumns.T_MODULE_K_TARGET_TYPE_ID);
		poiProjectionMap.put(ModuleColumns.T_MODULE_K_COLOR, ModuleDbHelper.T_MODULE + "." + ModuleDbHelper.T_MODULE_K_COLOR + " AS "
				+ ModuleColumns.T_MODULE_K_COLOR);
		poiProjectionMap.put(ModuleColumns.T_MODULE_K_LOCATION, ModuleDbHelper.T_MODULE + "." + ModuleDbHelper.T_MODULE_K_LOCATION + " AS "
				+ ModuleColumns.T_MODULE_K_LOCATION);
		poiProjectionMap.put(ModuleColumns.T_MODULE_K_NAME_FR, ModuleDbHelper.T_MODULE + "." + ModuleDbHelper.T_MODULE_K_NAME_FR + " AS "
				+ ModuleColumns.T_MODULE_K_NAME_FR);
		return poiProjectionMap;
	}

	public static final String[] PROJECTION_MODULE = new String[] { ModuleColumns.T_MODULE_K_PKG, ModuleColumns.T_MODULE_K_TARGET_TYPE_ID,
			ModuleColumns.T_MODULE_K_COLOR, ModuleColumns.T_MODULE_K_LOCATION, ModuleColumns.T_MODULE_K_NAME_FR };

	public static final String[] PROJECTION_MODULE_POI = ArrayUtils.addAll(POIProvider.PROJECTION_POI, PROJECTION_MODULE);

	@Override
	public String[] getPOIProjection() {
		return PROJECTION_MODULE_POI;
	}

	@Override
	public String getPOITable() {
		return ModuleDbHelper.T_MODULE;
	}

	public static class ModuleColumns {
		public static final String T_MODULE_K_PKG = POIColumns.getFkColumnName("pkg");
		public static final String T_MODULE_K_TARGET_TYPE_ID = POIColumns.getFkColumnName("targetTypeId");
		public static final String T_MODULE_K_COLOR = POIColumns.getFkColumnName("color");
		public static final String T_MODULE_K_LOCATION = POIColumns.getFkColumnName("location");
		public static final String T_MODULE_K_NAME_FR = POIColumns.getFkColumnName("name_fr");
	}
}
