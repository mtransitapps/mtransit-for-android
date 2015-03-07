package org.mtransit.android.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.R;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI.POIUtils;
import org.mtransit.android.commons.provider.AgencyProvider;
import org.mtransit.android.commons.provider.ContentProviderConstants;
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.Place;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

public class PlaceProvider extends AgencyProvider implements POIProviderContract {

	private static final String TAG = PlaceProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = AgencyProvider.getNewUriMatcher(authority);
		POIProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public UriMatcher getAgencyUriMatcher() {
		return getURIMATCHER(getContext());
	}

	@Override
	public Cursor getSearchSuggest(String query) {
		return null; // TODO implement Place/Query auto-complete
	}

	@Override
	public HashMap<String, String> getSearchSuggestProjectionMap() {
		return null; // TODO implement Place/Query auto-complete
	}

	@Override
	public String getSearchSuggestTable() {
		return null; // TODO implement Place/Query auto-complete
	}

	@Override
	public String getPOITable() {
		return PlaceDbHelper.T_PLACE;
	}

	private static final String[] PROJECTION_PLACE = new String[] { POIProviderContract.Columns.T_POI_K_SCORE_META_OPT, //
			PlaceColumns.T_PLACE_K_PROVIDER_ID, PlaceColumns.T_PLACE_K_LANG, PlaceColumns.T_PLACE_K_READ_AT_IN_MS };

	public static final String[] PROJECTION_PLACE_POI = ArrayUtils.addAll(POIProvider.PROJECTION_POI, PROJECTION_PLACE);

	@Override
	public String[] getPOIProjection() {
		return PROJECTION_PLACE_POI;
	}

	private static String googlePlacesApiKey = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	public static String getGOOGLE_PLACES_API_KEY(Context context) {
		if (googlePlacesApiKey == null) {
			googlePlacesApiKey = context.getResources().getString(R.string.google_places_api_key);
		}
		return googlePlacesApiKey;
	}

	private static final String TEXT_SEARCH_URL_PART_1_BEFORE_KEY = "https://maps.googleapis.com/maps/api/place/textsearch/json?key=";
	private static final String TEXT_SEARCH_URL_PART_2_BEFORE_LANG = "&language=";
	private static final String TEXT_SEARCH_URL_PART_3_BEFORE_LOCATION = "&location=";
	private static final String TEXT_SEARCH_URL_PART_4_BEFORE_RADIUS = "&radius=";
	private static final String TEXT_SEARCH_URL_PART_5_BEFORE_QUERY = "&query=";
	private static final String TEXT_SEARCH_URL_LANG_DEFAULT = "en";
	private static final String TEXT_SEARCH_URL_LANG_FRENCH = "fr";
	private static final int TEXT_SEARCH_URL_RADIUS_IN_METERS_DEFAULT = 50000; // max = 50000

	private static String getTextSearchUrlString(Context context, Double optLat, Double optLng, Integer optRadiusInMeters, String[] searchKeywords) {
		StringBuilder sb = new StringBuilder();
		sb.append(TEXT_SEARCH_URL_PART_1_BEFORE_KEY).append(getGOOGLE_PLACES_API_KEY(context));
		sb.append(TEXT_SEARCH_URL_PART_2_BEFORE_LANG).append(LocaleUtils.isFR() ? TEXT_SEARCH_URL_LANG_FRENCH : TEXT_SEARCH_URL_LANG_DEFAULT);
		if (optLat != null && optLng != null) {
			sb.append(TEXT_SEARCH_URL_PART_3_BEFORE_LOCATION).append(optLat).append(',').append(optLng);
			sb.append(TEXT_SEARCH_URL_PART_4_BEFORE_RADIUS).append(optRadiusInMeters == null ? TEXT_SEARCH_URL_RADIUS_IN_METERS_DEFAULT : optRadiusInMeters);
		}
		if (ArrayUtils.getSize(searchKeywords) != 0 && !TextUtils.isEmpty(searchKeywords[0])) {
			sb.append(TEXT_SEARCH_URL_PART_5_BEFORE_QUERY);
			boolean isFirstKeyword = true;
			for (String searchKeyword : searchKeywords) {
				if (TextUtils.isEmpty(searchKeyword)) {
					continue;
				}
				String[] keywords = searchKeyword.toLowerCase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON);
				for (String keyword : keywords) {
					if (TextUtils.isEmpty(searchKeyword)) {
						continue;
					}
					if (!isFirstKeyword) {
						sb.append('+');
					}
					sb.append(keyword);
					isFirstKeyword = false;
				}
			}
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

	@Override
	public Cursor getPOI(POIProviderContract.Filter poiFilter) {
		if (poiFilter == null) {
			return null;
		}
		String url;
		if (POIProviderContract.Filter.isAreaFilter(poiFilter)) {
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		} else if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
			Double lat = poiFilter.getExtraDouble("lat", null);
			Double lng = poiFilter.getExtraDouble("lng", null);
			url = getTextSearchUrlString(getContext(), lat, lng, null, poiFilter.getSearchKeywords());
			return getTextSearchResults(url);
		} else if (POIProviderContract.Filter.isUUIDFilter(poiFilter)) {
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		} else if (POIProviderContract.Filter.isSQLSelection(poiFilter)) {
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		} else {
			MTLog.w(this, "Unexpected POI filter '%s'!", poiFilter);
			return null;
		}
	}

	private Cursor getTextSearchResults(String urlString) {
		try {
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlc;
			switch (httpsUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				String lang = LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : Locale.ENGLISH.getLanguage();
				return parseTextSearchJson(jsonString, getAUTHORITY(getContext()), lang, newLastUpdateInMs);
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
			MTLog.w(TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static final String JSON_RESULTS = "results";
	private static final String JSON_NAME = "name";
	private static final String JSON_PLACE_ID = "place_id";
	private static final String JSON_GEOMETRY = "geometry";
	private static final String JSON_LOCATION = "location";
	private static final String JSON_LAT = "lat";
	private static final String JSON_LNG = "lng";

	private Cursor parseTextSearchJson(String jsonString, String authority, String lang, long nowInMs) {
		try {
			ArrayList<Place> result = new ArrayList<Place>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_RESULTS)) {
				int score = 1000;
				JSONArray jResults = json.getJSONArray(JSON_RESULTS);
				for (int i = 0; i < jResults.length(); i++) {
					try {
						JSONObject jResult = jResults.getJSONObject(i);
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
			return getTextSearchResults(result);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private Cursor getTextSearchResults(ArrayList<Place> places) {
		MatrixCursor cursor = new MatrixCursor(getPOIProjection());
		if (places != null) {
			for (Place place : places) {
				cursor.addRow(new Object[] { //
				place.getUUID(), place.getDataSourceTypeId(), place.getId(), place.getName(), place.getLat(), place.getLng(), //
						place.getType(), place.getStatusType(), place.getActionsType(), //
						place.getScore(), //
						place.getProviderId(), place.getLang(), place.getReadAtInMs() //
				});
			}
		}
		return cursor;
	}

	@Override
	public Cursor getPOIFromDB(POIProviderContract.Filter poiFilter) {
		return null;
	}

	@Override
	public LocationUtils.Area getAgencyArea(Context context) {
		return LocationUtils.THE_WORLD;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.place_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	public static Uri getAUTHORITYURI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	/**
	 * Override if multiple {@link PlaceProvider} implementations in same app.
	 */
	@Override
	public String getAgencyColorString(Context context) {
		return null; // default
	}

	@Override
	public int getAgencyLabelResId() {
		return R.string.place_label;
	}

	@Override
	public int getAgencyShortNameResId() {
		return R.string.place_short_name;
	}

	/**
	 * Override if multiple {@link PlaceProvider} in same app.
	 */
	public String getDbName() {
		return PlaceDbHelper.DB_NAME;
	}

	@Override
	public boolean isAgencyDeployed() {
		return SqlUtils.isDbExist(getContext(), getDbName());
	}

	@Override
	public boolean isAgencySetupRequired() {
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			setupRequired = true; // live update required => update
		} else if (!SqlUtils.isDbExist(getContext(), getDbName())) {
			setupRequired = true; // not deployed => initialization
		} else if (SqlUtils.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
			setupRequired = true; // update required => update
		}
		return setupRequired;
	}

	@Override
	public void ping() {
		// do nothing
	}

	private static HashMap<String, String> poiProjectionMap;

	@Override
	public HashMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			poiProjectionMap = getNewPoiProjectionMap(getAUTHORITY(getContext()));
		}
		return poiProjectionMap;
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
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
		return super.getSortOrder(uri);
	}

	@Override
	public String getTypeMT(Uri uri) {
		String type = POIProvider.getTypeS(this, uri);
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

	public static HashMap<String, String> getNewPoiProjectionMap(String authority) {
		// @formatter:off
		return SqlUtils.ProjectionMapBuilder.getNew() //
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
				.build();
		// @formatter:on
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	private static PlaceDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private PlaceDbHelper getDBHelper(Context context) {
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
	public PlaceDbHelper getNewDbHelper(Context context) {
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

		private static final String TAG = PlaceDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link PlaceDbHelper} in same app.
		 */
		public static final String DB_NAME = "place.db";

		/**
		 * Override if multiple {@link PlaceDbHelper} in same app.
		 */
		public static final int DB_VERSION = 2;

		public static final String T_PLACE = POIProvider.POIDbHelper.T_POI;
		public static final String T_PLACE_K_PROVIDER_ID = POIProvider.POIDbHelper.getFkColumnName("provider_id");
		public static final String T_PLACE_K_LANG = POIProvider.POIDbHelper.getFkColumnName("lang");
		public static final String T_PLACE_K_READ_AT_IN_MS = POIProvider.POIDbHelper.getFkColumnName("read_at_in_ms");
		private static final String T_PLACE_SQL_CREATE = POIProvider.POIDbHelper.getSqlCreateBuilder(T_PLACE) //
				.appendColumn(T_PLACE_K_PROVIDER_ID, SqlUtils.TXT) //
				.appendColumn(T_PLACE_K_LANG, SqlUtils.TXT) //
				.appendColumn(T_PLACE_K_READ_AT_IN_MS, SqlUtils.INT) //
				.build();
		private static final String T_PLACE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_PLACE);

		/**
		 * Override if multiple {@link PlaceDbHelper} in same app.
		 */
		public static int getDbVersion() {
			return DB_VERSION;
		}

		public PlaceDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion());
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_PLACE_SQL_DROP);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_PLACE_SQL_CREATE);
		}
	}

	public static class PlaceColumns {
		public static final String T_PLACE_K_PROVIDER_ID = POIProviderContract.Columns.getFkColumnName("provider_id");
		public static final String T_PLACE_K_LANG = POIProviderContract.Columns.getFkColumnName("lang");
		public static final String T_PLACE_K_READ_AT_IN_MS = POIProviderContract.Columns.getFkColumnName("read_at_in_ms");
	}
}
