package org.mtransit.android.provider;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.R;
import org.mtransit.android.analytics.AnalyticsEvents;
import org.mtransit.android.analytics.AnalyticsEventsParamsProvider;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POI.POIUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.provider.AgencyProvider;
import org.mtransit.android.commons.provider.ContentProviderConstants;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.provider.StatusProvider;
import org.mtransit.android.commons.provider.StatusProviderContract;
import org.mtransit.android.data.Module;
import org.mtransit.android.di.Injection;
import org.mtransit.android.util.UITimeUtils;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

@SuppressWarnings("UnusedReturnValue")
public class ModuleProvider extends AgencyProvider implements POIProviderContract, StatusProviderContract {

	private static final String LOG_TAG = ModuleProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	@Override
	public String toString() {
		return getLogTag();
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	private static final String PREF_KEY_LAST_UPDATE_MS = ModuleDbHelper.PREF_KEY_LAST_UPDATE_MS;

	private static final long MODULE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(7L);
	private static final long MODULE_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);

	private static final long MODULE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long MODULE_STATUS_VALIDITY_IN_MS = TimeUnit.SECONDS.toMillis(30L);
	private static final long MODULE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(15L);
	private static final long MODULE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.SECONDS.toMillis(20L);
	private static final long MODULE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	@Nullable
	private ModuleDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	@NonNull
	private static UriMatcher getURIMATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = AgencyProvider.getNewUriMatcher(authority);
		StatusProvider.append(URI_MATCHER, authority);
		POIProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.module_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITYURI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private IAnalyticsManager analyticsManager;

	@NonNull
	private IAnalyticsManager getAnalyticsManager() {
		if (this.analyticsManager == null) {
			analyticsManager = Injection.providesAnalyticsManager();
		}
		return this.analyticsManager;
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	@NonNull
	private ModuleDbHelper getDBHelper(@NonNull Context context) {
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

	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
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

	@Nullable
	public String getSortOrder(@NonNull Uri uri) {
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

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
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
	public int updateMT(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Nullable
	@Override
	public Uri insertMT(@NonNull Uri uri, @Nullable ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	@Override
	public int deleteMT(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@NonNull
	@Override
	public Cursor getSearchSuggest(@Nullable String query) {
		return ContentProviderConstants.EMPTY_CURSOR; // no search suggest for modules
	}

	@Nullable
	@Override
	public ArrayMap<String, String> getSearchSuggestProjectionMap() {
		return null; // no search suggest for modules
	}

	@Nullable
	@Override
	public String getSearchSuggestTable() {
		return null; // no search suggest for modules
	}

	@Nullable
	@Override
	public Cursor getPOI(@Nullable POIProviderContract.Filter poiFilter) {
		updateModuleDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Nullable
	@Override
	public Cursor getPOIFromDB(@Nullable POIProviderContract.Filter poiFilter) {
		return POIProvider.getDefaultPOIFromDB(poiFilter, this);
	}

	@Override
	public long getPOIMaxValidityInMs() {
		return MODULE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getPOIValidityInMs() {
		return MODULE_VALIDITY_IN_MS;
	}

	public void updateModuleDataIfRequired() {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0L);
		long nowInMs = UITimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getPOIMaxValidityInMs() < nowInMs) { // too old to display?
			deleteAllModuleData();
			updateAllModuleDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getPOIValidityInMs() < nowInMs) { // try to refresh?
			updateAllModuleDataFromWWW(lastUpdateInMs);
		}
	}

	private int deleteAllModuleData() {
		int affectedRows = 0;
		try {
			//noinspection ConstantConditions // TODO requireContext()
			affectedRows = getDBHelper(getContext()).getWritableDatabase().delete(ModuleDbHelper.T_MODULE, null, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all module data!");
		}
		return affectedRows;
	}

	private synchronized void updateAllModuleDataFromWWW(long oldLastUpdatedInMs) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0L) > oldLastUpdatedInMs) {
			return; // too late, another thread already updated
		}
		loadDataFromWWW();
	}

	@Nullable
	private HashSet<Module> loadDataFromWWW() {
		try {
			Context context = getContext();
			if (context == null) {
				return null;
			}
			long newLastUpdateInMs = UITimeUtils.currentTimeMillis();
			int fileResId = R.raw.modules;
			String jsonString = FileUtils.fromFileRes(context, fileResId);
			HashSet<Module> modules = new HashSet<>();
			JSONArray jsonArray = new JSONArray(jsonString);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jModule = jsonArray.getJSONObject(i);
				Module module = Module.fromSimpleJSONStatic(jModule, getAUTHORITY(context));
				if (module == null) {
					continue; // error while converting JSON to Module
				}
				module.setId(i);
				modules.add(module);
			}
			deleteAllModuleData();
			insertModulesLockDB(this, modules);
			PreferenceUtils.savePrefLcl(context, PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
			return modules;
		} catch (Exception e) {
			MTLog.w(this, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static synchronized int insertModulesLockDB(@NonNull POIProviderContract provider, Collection<Module> defaultPOIs) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.beginTransaction(); // start the transaction
			if (defaultPOIs != null) {
				for (DefaultPOI defaultPOI : defaultPOIs) {
					long rowId = db.insert(provider.getPOITable(), POIProvider.POIDbHelper.T_POI_K_ID, defaultPOI.toContentValues());
					if (rowId > 0) {
						affectedRows++;
					}
				}
			}
			db.setTransactionSuccessful(); // mark the transaction as successful
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "ERROR while applying batch update to the database!");
		} finally {
			SqlUtils.endTransaction(db);
		}
		return affectedRows;
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter filter) {
		if (!(filter instanceof AppStatus.AppStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without AppStatusFilter!");
			return null;
		}
		AppStatus.AppStatusFilter moduleStatusFilter = (AppStatus.AppStatusFilter) filter;
		return getNewModuleStatus(moduleStatusFilter);
	}

	@NonNull
	public POIStatus getNewModuleStatus(@NonNull AppStatus.AppStatusFilter filter) {
		long newLastUpdateInMs = UITimeUtils.currentTimeMillis();
		//noinspection ConstantConditions // TODO requireContext()
		final boolean appInstalled = PackageManagerUtils.isAppInstalled(getContext(), filter.getPkg());
		final boolean appEnabled = PackageManagerUtils.isAppEnabled(getContext(), filter.getPkg());
		if (appInstalled && !appEnabled) {
			getAnalyticsManager().logEvent(AnalyticsEvents.FOUND_DISABLED_MODULE, new AnalyticsEventsParamsProvider()
					.put(AnalyticsEvents.Params.PKG, filter.getPkg())
					.put(AnalyticsEvents.Params.STATE, (long) PackageManagerUtils.getAppEnabledState(getContext(), filter.getPkg())));
		}
		return new AppStatus(filter.getTargetUUID(), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
				appInstalled,
				appEnabled);
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
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

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITYURI(getContext());
	}

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return ModuleDbHelper.T_MODULE_STATUS;
	}

	@Override
	public boolean isAgencyDeployed() {
		//noinspection ConstantConditions // TODO requireContext()
		return SqlUtils.isDbExist(getContext(), getDbName());
	}

	@Override
	public boolean isAgencySetupRequired() {
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			return true; // live update required => update
		}
		//noinspection ConstantConditions // TODO requireContext()
		if (!SqlUtils.isDbExist(getContext(), getDbName())) {
			return true; // not deployed => initialization
		}
		//noinspection RedundantIfStatement
		if (SqlUtils.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
			return true; // update required => update
		}
		return false;
	}

	@NonNull
	@Override
	public UriMatcher getAgencyUriMatcher() {
		//noinspection ConstantConditions // TODO requireContext()
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
	@Nullable
	@Override
	public String getAgencyColorString(@NonNull Context context) {
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
	@NonNull
	@Override
	public LocationUtils.Area getAgencyArea(@NonNull Context context) {
		return LocationUtils.THE_WORLD;
	}

	/**
	 * Override if multiple {@link ModuleProvider} implementations in same app.
	 */
	@NonNull
	public String getDbName() {
		return ModuleDbHelper.DB_NAME;
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		//noinspection ConstantConditions // TODO requireContext()
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
	@NonNull
	public ModuleDbHelper getNewDbHelper(@NonNull Context context) {
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

	@Nullable
	private static ArrayMap<String, String> poiProjectionMap;

	@NonNull
	@Override
	public ArrayMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			//noinspection ConstantConditions // TODO requireContext()
			poiProjectionMap = getNewPoiProjectionMap(getAUTHORITY(getContext()));
		}
		return poiProjectionMap;
	}

	@NonNull
	public static ArrayMap<String, String> getNewPoiProjectionMap(@NonNull String authority) {
		// @formatter:off
		return SqlUtils.ProjectionMapBuilder.getNew()
				.appendValue(SqlUtils.concatenate( //
						SqlUtils.escapeString(POIUtils.UID_SEPARATOR), //
						SqlUtils.escapeString(authority), //
						SqlUtils.getTableColumn(ModuleDbHelper.T_MODULE, ModuleDbHelper.T_MODULE_K_PKG) //
						), POIProviderContract.Columns.T_POI_K_UUID_META) //
				.appendValue(Module.DST_ID, POIProviderContract.Columns.T_POI_K_DST_ID_META) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_ID, POIProviderContract.Columns.T_POI_K_ID) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_NAME, POIProviderContract.Columns.T_POI_K_NAME) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_LAT, POIProviderContract.Columns.T_POI_K_LAT) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_LNG, POIProviderContract.Columns.T_POI_K_LNG) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_TYPE, POIProviderContract.Columns.T_POI_K_TYPE) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_STATUS_TYPE, POIProviderContract.Columns.T_POI_K_STATUS_TYPE) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_ACTIONS_TYPE, POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE) //
				//
				.appendTableColumn(ModuleDbHelper.T_MODULE, ModuleDbHelper.T_MODULE_K_PKG, ModuleColumns.T_MODULE_K_PKG) //
				.appendTableColumn(ModuleDbHelper.T_MODULE, ModuleDbHelper.T_MODULE_K_TARGET_TYPE_ID, ModuleColumns.T_MODULE_K_TARGET_TYPE_ID) //
				.appendTableColumn(ModuleDbHelper.T_MODULE, ModuleDbHelper.T_MODULE_K_COLOR, ModuleColumns.T_MODULE_K_COLOR) //
				.appendTableColumn(ModuleDbHelper.T_MODULE, ModuleDbHelper.T_MODULE_K_LOCATION, ModuleColumns.T_MODULE_K_LOCATION) //
				.appendTableColumn(ModuleDbHelper.T_MODULE, ModuleDbHelper.T_MODULE_K_NAME_FR, ModuleColumns.T_MODULE_K_NAME_FR) //
				.build();
		// @formatter:on
	}

	public static final String[] PROJECTION_MODULE =
			new String[]{ModuleColumns.T_MODULE_K_PKG, ModuleColumns.T_MODULE_K_TARGET_TYPE_ID, ModuleColumns.T_MODULE_K_COLOR,
					ModuleColumns.T_MODULE_K_LOCATION, ModuleColumns.T_MODULE_K_NAME_FR};

	public static final String[] PROJECTION_MODULE_POI = ArrayUtils.addAllNonNull(POIProvider.PROJECTION_POI, PROJECTION_MODULE);

	@NonNull
	@Override
	public String[] getPOIProjection() {
		return PROJECTION_MODULE_POI;
	}

	@NonNull
	@Override
	public String getPOITable() {
		return ModuleDbHelper.T_MODULE;
	}

	public static class ModuleColumns {
		public static final String T_MODULE_K_PKG = POIProviderContract.Columns.getFkColumnName("pkg");
		public static final String T_MODULE_K_TARGET_TYPE_ID = POIProviderContract.Columns.getFkColumnName("targetTypeId");
		public static final String T_MODULE_K_COLOR = POIProviderContract.Columns.getFkColumnName("color");
		public static final String T_MODULE_K_LOCATION = POIProviderContract.Columns.getFkColumnName("location");
		public static final String T_MODULE_K_NAME_FR = POIProviderContract.Columns.getFkColumnName("name_fr");
	}
}
