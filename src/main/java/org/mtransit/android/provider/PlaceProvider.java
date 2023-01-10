package org.mtransit.android.provider;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.R;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI.POIUtils;
import org.mtransit.android.commons.provider.AgencyProvider;
import org.mtransit.android.commons.provider.ContentProviderConstants;
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.Place;
import org.mtransit.android.util.UITimeUtils;
import org.mtransit.commons.FeatureFlags;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

public class PlaceProvider extends AgencyProvider implements POIProviderContract {

	private static final String LOG_TAG = PlaceProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
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
		POIProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(requireContextCompat());
	}

	@NonNull
	@Override
	public UriMatcher getAgencyUriMatcher() {
		return getURIMATCHER(requireContextCompat());
	}

	@Nullable
	@Override
	public Cursor getSearchSuggest(@Nullable String query) {
		return null; // TODO implement Place/Query auto-complete
	}

	@Nullable
	@Override
	public ArrayMap<String, String> getSearchSuggestProjectionMap() {
		return null; // TODO implement Place/Query auto-complete
	}

	@Nullable
	@Override
	public String getSearchSuggestTable() {
		return null; // TODO implement Place/Query auto-complete
	}

	@NonNull
	@Override
	public String getPOITable() {
		return PlaceDbHelper.T_PLACE;
	}

	private static final String[] PROJECTION_PLACE = new String[]{POIProviderContract.Columns.T_POI_K_SCORE_META_OPT, //
			PlaceColumns.T_PLACE_K_PROVIDER_ID, PlaceColumns.T_PLACE_K_LANG, PlaceColumns.T_PLACE_K_READ_AT_IN_MS};

	public static final String[] PROJECTION_PLACE_POI = ArrayUtils.addAllNonNull(POIProvider.PROJECTION_POI, PROJECTION_PLACE);

	@NonNull
	@Override
	public String[] getPOIProjection() {
		return PROJECTION_PLACE_POI;
	}

	@Nullable
	private static String googlePlacesApiKey = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	@NonNull
	public static String getGOOGLE_PLACES_API_KEY(@NonNull Context context) {
		if (googlePlacesApiKey == null) {
			googlePlacesApiKey = context.getResources().getString(R.string.google_places_api_key);
		}
		return googlePlacesApiKey;
	}

	private static final String TEXT_SEARCH_URL_PART_1_BEFORE_KEY = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json" +
			"?inputtype=textquery" +
			"&fields=place_id,name,geometry/location" +
			"&key=";
	private static final String TEXT_SEARCH_URL_PART_2_BEFORE_LANG = "&language=";
	private static final String TEXT_SEARCH_URL_PART_3_BEFORE_LOCATION_BIAS_CIRCLE_RADIUS = "&locationbias=circle:";
	private static final String TEXT_SEARCH_URL_PART_4_BEFORE_LAT_LONG = "@";
	private static final String TEXT_SEARCH_URL_PART_5_BEFORE_INPUT = "&input=";
	private static final String TEXT_SEARCH_URL_LANG_DEFAULT = "en";
	private static final String TEXT_SEARCH_URL_LANG_FRENCH = "fr";
	private static final int TEXT_SEARCH_URL_RADIUS_IN_METERS_DEFAULT = 50000; // max = 50000

	@VisibleForTesting
	@Nullable
	protected static String getTextSearchUrlString(@NonNull String apiKey,
												   @Nullable Double optLat,
												   @Nullable Double optLng,
												   @Nullable Integer optRadiusInMeters,
												   @Nullable String[] searchKeywords) {
		StringBuilder sb = new StringBuilder();
		sb.append(TEXT_SEARCH_URL_PART_1_BEFORE_KEY).append(apiKey);
		sb.append(TEXT_SEARCH_URL_PART_2_BEFORE_LANG).append(LocaleUtils.isFR() ? TEXT_SEARCH_URL_LANG_FRENCH : TEXT_SEARCH_URL_LANG_DEFAULT);
		if (optLat != null && optLng != null) {
			sb.append(TEXT_SEARCH_URL_PART_3_BEFORE_LOCATION_BIAS_CIRCLE_RADIUS).append(optRadiusInMeters == null ? TEXT_SEARCH_URL_RADIUS_IN_METERS_DEFAULT : optRadiusInMeters);
			sb.append(TEXT_SEARCH_URL_PART_4_BEFORE_LAT_LONG).append(optLat).append(',').append(optLng);
		}
		int keywordMaxSize = -1;
		if (searchKeywords != null && searchKeywords.length > 0
				&& !TextUtils.isEmpty(searchKeywords[0])) {
			sb.append(TEXT_SEARCH_URL_PART_5_BEFORE_INPUT);
			boolean isFirstKeyword = true;
			for (String searchKeyword : searchKeywords) {
				if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
					continue;
				}
				String[] keywords = searchKeyword.toLowerCase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON);
				for (String keyword : keywords) {
					if (keyword == null || keyword.trim().isEmpty()) {
						continue;
					}
					if (!isFirstKeyword) {
						sb.append('+');
					}
					sb.append(keyword);
					int keywordSize = keyword.length();
					if (keywordSize < 5 && StringUtils.isDigitsOnly(keyword, false)) {
						keywordSize = -1; // ignore 4 digits
					}
					keywordMaxSize = Math.max(keywordMaxSize, keywordSize);
					isFirstKeyword = false;
				}
			}
		}
		if (keywordMaxSize < 3) {
			return null; // no significant keyword to search (SKIP search)
		}
		return sb.toString();
	}

	private static final long POI_MAX_VALIDITY_IN_MS = Long.MAX_VALUE;

	private static final long POI_VALIDITY_IN_MS = Long.MAX_VALUE;

	@Override
	public long getPOIMaxValidityInMs() {
		return POI_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getPOIValidityInMs() {
		return POI_VALIDITY_IN_MS;
	}

	@Nullable
	@Override
	public Cursor getPOI(@Nullable Filter poiFilter) {
		if (poiFilter == null) {
			return null;
		}
		if (POIProviderContract.Filter.isAreaFilter(poiFilter)) {
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		} else if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
			return fetchTextSearchResults(poiFilter);
		} else if (POIProviderContract.Filter.isUUIDFilter(poiFilter)) {
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		} else if (POIProviderContract.Filter.isSQLSelection(poiFilter)) {
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		} else {
			MTLog.w(this, "Unexpected POI filter '%s'!", poiFilter);
			return null;
		}
	}

	@Nullable
	private Cursor fetchTextSearchResults(@NonNull Filter poiFilter) {
		final Context context = requireContextCompat();
		Double lat = poiFilter.getExtraDouble("lat", null);
		Double lng = poiFilter.getExtraDouble("lng", null);
		String url = getTextSearchUrlString(getGOOGLE_PLACES_API_KEY(context), lat, lng, null, poiFilter.getSearchKeywords());
		if (url == null) { // no search keyboard => no search
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		}
		return getTextSearchResults(context, url,
				getTextSearchUrlString(StringUtils.EMPTY, lat, lng, null, poiFilter.getSearchKeywords()));
	}

	@Nullable
	private Cursor getTextSearchResults(@NonNull Context context, @NonNull String urlString, @Nullable String publicUrlString) {
		try {
			MTLog.i(this, "Loading from '%s'...", publicUrlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlc;
			switch (httpsUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = UITimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				String lang = LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : Locale.ENGLISH.getLanguage();
				return parseTextSearchJson(jsonString, getAUTHORITY(context), lang, newLastUpdateInMs);
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpsUrlConnection.getResponseCode(),
						httpsUrlConnection.getResponseMessage());
				return null;
			}
		} catch (SSLHandshakeException sslhe) {
			MTLog.w(this, sslhe, "SSL error!");
			return null;
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection!");
			} else {
				MTLog.w(this, "No Internet Connection!");
			}
			return null;
		} catch (SocketException se) {
			MTLog.w(LOG_TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) {
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static final String JSON_CANDIDATES = "candidates";
	private static final String JSON_NAME = "name";
	private static final String JSON_PLACE_ID = "place_id";
	private static final String JSON_GEOMETRY = "geometry";
	private static final String JSON_LOCATION = "location";
	private static final String JSON_LAT = "lat";
	private static final String JSON_LNG = "lng";

	@Nullable
	private Cursor parseTextSearchJson(String jsonString, String authority, String lang, long nowInMs) {
		try {
			ArrayList<Place> result = new ArrayList<>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_CANDIDATES)) {
				int score = 1000;
				JSONArray jCandidates = json.getJSONArray(JSON_CANDIDATES);
				for (int i = 0; i < jCandidates.length(); i++) {
					try {
						JSONObject jResult = jCandidates.getJSONObject(i);
						String name = jResult.getString(JSON_NAME);
						String placeId = jResult.getString(JSON_PLACE_ID);
						JSONObject jGeometry = jResult.getJSONObject(JSON_GEOMETRY);
						JSONObject jLocation = jGeometry.getJSONObject(JSON_LOCATION);
						Place place = new Place(authority, placeId, lang, nowInMs);
						place.setName(name);
						place.setLat(jLocation.getDouble(JSON_LAT));
						place.setLng(jLocation.getDouble(JSON_LNG));
						place.setScore(score--);
						result.add(place);
					} catch (Exception e) {
						MTLog.w(this, e, "Error while parsing JSON result '%s'!", i);
					}
				}
			}
			MTLog.i(this, "Loaded %d places.", result.size());
			return getTextSearchResults(result);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	@NonNull
	private Cursor getTextSearchResults(@Nullable List<Place> places) {
		MatrixCursor cursor = new MatrixCursor(getPOIProjection());
		if (places != null) {
			for (Place place : places) {
				cursor.addRow(new Object[]{ //
						place.getUUID(), place.getDataSourceTypeId(), place.getId(), place.getName(), place.getLat(), place.getLng(), //
						place.getType(), place.getStatusType(), place.getActionsType(), //
						place.getScore(), //
						place.getProviderId(), place.getLang(), place.getReadAtInMs() //
				});
			}
		}
		return cursor;
	}

	@Nullable
	@Override
	public Cursor getPOIFromDB(@Nullable Filter poiFilter) {
		return null;
	}

	@NonNull
	@Override
	public LocationUtils.Area getAgencyArea(@NonNull Context context) {
		return LocationUtils.THE_WORLD;
	}

	@Override
	public int getAgencyMaxValidSec(@NonNull Context context) {
		return 0; // unlimited
	}

	@Override
	public int getAvailableVersionCode(@NonNull Context context, @Nullable String filterS) {
		return 0; // main app in-app update not supported yet
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	@NonNull
	public static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.place_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	@SuppressWarnings("unused")
	@NonNull
	public static Uri getAUTHORITYURI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	@Nullable
	@Override
	public String getAgencyColorString(@NonNull Context context) {
		return null; // default
	}

	@StringRes
	@Override
	public int getAgencyLabelResId() {
		return R.string.place_label;
	}

	@StringRes
	@Override
	public int getAgencyShortNameResId() {
		return R.string.place_short_name;
	}

	/**
	 * Override if multiple {@link PlaceProvider} in same app.
	 */
	@NonNull
	public String getDbName() {
		return PlaceDbHelper.DB_NAME;
	}

	@Override
	public boolean isAgencyDeployed() {
		return SqlUtils.isDbExist(requireContextCompat(), getDbName());
	}

	@Override
	public boolean isAgencySetupRequired() {
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			return true; // live update required => update
		}
		if (!SqlUtils.isDbExist(requireContextCompat(), getDbName())) {
			return true; // not deployed => initialization
		}
		//noinspection RedundantIfStatement
		if (SqlUtils.getCurrentDbVersion(requireContextCompat(), getDbName()) != getCurrentDbVersion()) {
			return true; // update required => update
		}
		return false;
	}

	@Override
	public void ping() {
		// do nothing
	}

	@Nullable
	private static ArrayMap<String, String> poiProjectionMap;

	@NonNull
	@Override
	public ArrayMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			poiProjectionMap = getNewPoiProjectionMap(getAUTHORITY(requireContextCompat()));
		}
		return poiProjectionMap;
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return super.onCreateMT();
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
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while resolving query '%s'!", uri);
			return null;
		}
	}

	@Nullable
	@Override
	public String getSortOrder(@NonNull Uri uri) {
		String sortOrder = POIProvider.getSortOrderS(this, uri);
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
	public static ArrayMap<String, String> getNewPoiProjectionMap(@NonNull String authority) {
		// @formatter:off
		final SqlUtils.ProjectionMapBuilder builder = SqlUtils.ProjectionMapBuilder.getNew() //
				.appendValue(SqlUtils.concatenate( //
						SqlUtils.escapeString(POIUtils.UID_SEPARATOR), //
						SqlUtils.escapeString(authority), //
						SqlUtils.getTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_PROVIDER_ID) //
						), POIProviderContract.Columns.T_POI_K_UUID_META) //
				.appendValue(Place.DST_ID, POIProviderContract.Columns.T_POI_K_DST_ID_META) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_ID, POIProviderContract.Columns.T_POI_K_ID) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_NAME, POIProviderContract.Columns.T_POI_K_NAME) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_LAT, POIProviderContract.Columns.T_POI_K_LAT) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_LNG, POIProviderContract.Columns.T_POI_K_LNG) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_TYPE, POIProviderContract.Columns.T_POI_K_TYPE) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_STATUS_TYPE, POIProviderContract.Columns.T_POI_K_STATUS_TYPE) //
				.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_ACTIONS_TYPE, POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE) //
				//
				.appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_PROVIDER_ID, PlaceColumns.T_PLACE_K_PROVIDER_ID) //
				.appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_LANG, PlaceColumns.T_PLACE_K_LANG) //
				.appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_READ_AT_IN_MS, PlaceColumns.T_PLACE_K_READ_AT_IN_MS) //
				;
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			builder.appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_ACCESSIBLE, POIProviderContract.Columns.T_POI_K_ACCESSIBLE); //
		}
		return builder.build();
		// @formatter:on
	}

	@NonNull
	private SQLiteOpenHelper getDBHelper() {
		return getDBHelper(requireContextCompat());
	}

	@NonNull
	@Override
	public SQLiteDatabase getReadDB() {
		return getDBHelper().getReadableDatabase();
	}

	@NonNull
	@Override
	public SQLiteDatabase getWriteDB() {
		return getDBHelper().getWritableDatabase();
	}

	@Nullable
	private static PlaceDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private PlaceDbHelper getDBHelper(@NonNull Context context) {
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
				MTLog.d(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	@NonNull
	public PlaceDbHelper getNewDbHelper(@NonNull Context context) {
		return new PlaceDbHelper(context.getApplicationContext());
	}

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	private int getCurrentDbVersion() {
		return PlaceDbHelper.getDbVersion();
	}

	@Override
	public int getAgencyVersion() {
		return getCurrentDbVersion();
	}

	private static class PlaceDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = PlaceDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link PlaceDbHelper} in same app.
		 */
		static final String DB_NAME = "place.db";

		/**
		 * Override if multiple {@link PlaceDbHelper} in same app.
		 */
		static final int DB_VERSION = 2;

		static final String T_PLACE = POIProvider.POIDbHelper.T_POI;
		static final String T_PLACE_K_PROVIDER_ID = POIProvider.POIDbHelper.getFkColumnName("provider_id");
		static final String T_PLACE_K_LANG = POIProvider.POIDbHelper.getFkColumnName("lang");
		static final String T_PLACE_K_READ_AT_IN_MS = POIProvider.POIDbHelper.getFkColumnName("read_at_in_ms");
		private static final String T_PLACE_SQL_CREATE = POIProvider.POIDbHelper.getSqlCreateBuilder(T_PLACE) //
				.appendColumn(T_PLACE_K_PROVIDER_ID, SqlUtils.TXT) //
				.appendColumn(T_PLACE_K_LANG, SqlUtils.TXT) //
				.appendColumn(T_PLACE_K_READ_AT_IN_MS, SqlUtils.INT) //
				.build();
		private static final String T_PLACE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_PLACE);

		/**
		 * Override if multiple {@link PlaceDbHelper} in same app.
		 */
		static int getDbVersion() {
			return DB_VERSION;
		}

		PlaceDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion());
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_PLACE_SQL_DROP);
			initAllDbTables(db);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_PLACE_SQL_CREATE);
		}
	}

	public static class PlaceColumns {
		public static final String T_PLACE_K_PROVIDER_ID = POIProviderContract.Columns.getFkColumnName("provider_id");
		public static final String T_PLACE_K_LANG = POIProviderContract.Columns.getFkColumnName("lang");
		public static final String T_PLACE_K_READ_AT_IN_MS = POIProviderContract.Columns.getFkColumnName("read_at_in_ms");
	}
}
