package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONObject;
import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.AgencyProviderContract;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.android.commons.provider.NewsProviderContract;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.provider.ProviderContract;
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract;
import org.mtransit.android.commons.provider.ServiceUpdateProviderContract;
import org.mtransit.android.commons.provider.StatusProviderContract;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import android.text.TextUtils;

public final class DataSourceManager implements MTLog.Loggable {

	private static final String TAG = DataSourceManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static ArrayMap<String, Uri> uriMap = new ArrayMap<String, Uri>();

	private static Uri getUri(String authority) {
		Uri uri = uriMap.get(authority);
		if (uri == null) {
			uri = UriUtils.newContentUri(authority);
			uriMap.put(authority, uri);
		}
		return uri;
	}

	private DataSourceManager() {
	}

	public static ArrayList<ServiceUpdate> findServiceUpdates(Context context, String authority, ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		Cursor cursor = null;
		try {
			String serviceUpdateFilterJSONString = serviceUpdateFilter == null ? null : serviceUpdateFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), ServiceUpdateProviderContract.SERVICE_UPDATE_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, serviceUpdateFilterJSONString, null, null);
			return getServiceUpdates(cursor);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	private static ArrayList<ServiceUpdate> getServiceUpdates(Cursor cursor) {
		ArrayList<ServiceUpdate> result = new ArrayList<ServiceUpdate>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					result.add(ServiceUpdate.fromCursor(cursor));
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static News findANews(Context context, String authority, NewsProviderContract.Filter newsFilter) {
		ArrayList<News> news = findNews(context, authority, newsFilter);
		return news == null || news.size() == 0 ? null : news.get(0);
	}

	public static ArrayList<News> findNews(Context context, String authority, NewsProviderContract.Filter newsFilter) {
		Cursor cursor = null;
		try {
			String newsFilterJSONString = newsFilter == null ? null : newsFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), NewsProviderContract.NEWS_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, newsFilterJSONString, null, null);
			return getNews(cursor, authority);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	private static ArrayList<News> getNews(Cursor cursor, String authority) {
		ArrayList<News> result = new ArrayList<News>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					result.add(News.fromCursorStatic(cursor, authority));
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static ScheduleTimestamps findScheduleTimestamps(Context context, String authority,
			ScheduleTimestampsProviderContract.Filter scheduleTimestampsFilter) {
		Cursor cursor = null;
		try {
			String scheduleTimestampsFilterJSONString = scheduleTimestampsFilter == null ? null : scheduleTimestampsFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), ScheduleTimestampsProviderContract.SCHEDULE_TIMESTAMPS_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, scheduleTimestampsFilterJSONString, null, null);
			return getScheduleTimestamp(cursor);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	private static ScheduleTimestamps getScheduleTimestamp(Cursor cursor) {
		ScheduleTimestamps result = null;
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				result = ScheduleTimestamps.fromCursor(cursor);
			}
		}
		return result;
	}

	public static POIStatus findStatus(Context context, String authority, StatusProviderContract.Filter statusFilter) {
		Cursor cursor = null;
		try {
			String statusFilterJSONString = statusFilter == null ? null : statusFilter.toJSONStringStatic(statusFilter);
			Uri uri = Uri.withAppendedPath(getUri(authority), StatusProviderContract.STATUS_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, statusFilterJSONString, null, null);
			return getPOIStatus(cursor);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@Nullable
	private static POIStatus getPOIStatus(Cursor cursor) {
		POIStatus result = null;
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				int status = POIStatus.getTypeFromCursor(cursor);
				switch (status) {
				case POI.ITEM_STATUS_TYPE_NONE:
					result = null;
					break;
				case POI.ITEM_STATUS_TYPE_SCHEDULE:
					result = Schedule.fromCursor(cursor);
					break;
				case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
					result = AvailabilityPercent.fromCursor(cursor);
					break;
				case POI.ITEM_STATUS_TYPE_APP:
					result = AppStatus.fromCursor(cursor);
					break;
				default:
					MTLog.w(TAG, "findStatus() > Unexpected status '%s'!", status);
					result = null;
					break;
				}
			}
		}
		return result;
	}

	public static void ping(@NonNull Context context, String authority) {
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), ProviderContract.PING_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, null, null, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	public static AgencyProperties findAgencyProperties(Context context, String authority, DataSourceType dst, boolean isRTS) {
		AgencyProperties result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), AgencyProviderContract.ALL_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					String shortName = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.SHORT_NAME_PATH));
					String longName = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.LABEL_PATH));
					String color = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.COLOR_PATH));
					Area area = Area.fromCursor(cursor);
					result = new AgencyProperties(authority, dst, shortName, longName, color, area, isRTS);
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	public static JPaths findAgencyRTSRouteLogo(Context context, String authority) {
		JPaths result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), GTFSProviderContract.ROUTE_LOGO_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = JPaths.fromJSONString(cursor.getString(0));
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	public static Trip findRTSTrip(Context context, String authority, int tripId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSTripsUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.TripColumns.T_TRIP_K_ID, tripId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_TRIP, selection, null, null);
			ArrayList<Trip> rtsTrips = getRTSTrips(cursor);
			return rtsTrips == null || rtsTrips.size() == 0 ? null : rtsTrips.get(0);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	public static ArrayList<Trip> findRTSRouteTrips(Context context, String authority, long routeId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSTripsUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID, routeId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_TRIP, selection, null, null);
			return getRTSTrips(cursor);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	private static ArrayList<Trip> getRTSTrips(Cursor cursor) {
		ArrayList<Trip> result = new ArrayList<Trip>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					Trip fromCursor = Trip.fromCursor(cursor);
					result.add(fromCursor);
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static Route findRTSRoute(Context context, String authority, long routeId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSRoutesUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.RouteColumns.T_ROUTE_K_ID, routeId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_ROUTE, selection, null, null);
			ArrayList<Route> rtsRoutes = getRTSRoutes(cursor);
			return rtsRoutes == null || rtsRoutes.size() == 0 ? null : rtsRoutes.get(0);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@Nullable
	public static Cursor queryContentResolver(@NonNull ContentResolver contentResolver, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs,
			@Nullable String sortOrder) {
		return contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
	}

	public static ArrayList<Route> findAllRTSAgencyRoutes(Context context, String authority) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSRoutesUri(authority);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_ROUTE, null, null, null);
			return getRTSRoutes(cursor);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	private static ArrayList<Route> getRTSRoutes(Cursor cursor) {
		ArrayList<Route> result = new ArrayList<Route>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					Route fromCursor = Route.fromCursor(cursor);
					result.add(fromCursor);
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static POIManager findPOI(Context context, String authority, POIProviderContract.Filter poiFilter) {
		ArrayList<POIManager> pois = findPOIs(context, authority, poiFilter);
		return pois == null || pois.size() == 0 ? null : pois.get(0);
	}

	public static ArrayList<POIManager> findPOIs(Context context, String authority, POIProviderContract.Filter poiFilter) {
		Cursor cursor = null;
		try {
			JSONObject filterJSON = POIProviderContract.Filter.toJSON(poiFilter);
			if (filterJSON == null) {
				MTLog.w(TAG, "Invalid POI filter!");
				return null;
			}
			String filterJsonString = filterJSON.toString();
			Uri uri = getPOIUri(authority);
			cursor = queryContentResolver(context.getContentResolver(), uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, null);
			return getPOIs(cursor, authority);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	private static ArrayList<POIManager> getPOIs(Cursor cursor, String authority) {
		ArrayList<POIManager> result = new ArrayList<POIManager>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					result.add(POIManager.fromCursorStatic(cursor, authority));
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static HashSet<String> findSearchSuggest(Context context, String authority, String query) {
		Cursor cursor = null;
		try {
			Uri searchSuggestUri = Uri.withAppendedPath(getUri(authority), SearchManager.SUGGEST_URI_PATH_QUERY);
			if (!TextUtils.isEmpty(query)) {
				searchSuggestUri = Uri.withAppendedPath(searchSuggestUri, Uri.encode(query));
			}
			cursor = queryContentResolver(context.getContentResolver(), searchSuggestUri, null, null, null, null);
			return getSearchSuggest(cursor);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	public static HashSet<String> getSearchSuggest(Cursor cursor) {
		HashSet<String> results = new HashSet<String>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				int text1ColumnIdx = cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1);
				do {
					String suggest = cursor.getString(text1ColumnIdx);
					results.add(suggest);
				} while (cursor.moveToNext());
			}
		}
		return results;
	}

	private static Uri getPOIUri(String authority) {
		return Uri.withAppendedPath(getUri(authority), POIProviderContract.POI_PATH);
	}

	private static Uri getRTSRoutesUri(String authority) {
		return Uri.withAppendedPath(getUri(authority), GTFSProviderContract.ROUTE_PATH);
	}

	private static Uri getRTSTripsUri(String authority) {
		return Uri.withAppendedPath(getUri(authority), GTFSProviderContract.TRIP_PATH);
	}
}
