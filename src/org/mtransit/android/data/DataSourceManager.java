package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ScheduleTimestampsFilter;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.ScheduleTimestampsProvider;
import org.mtransit.android.commons.provider.ServiceUpdateProvider;
import org.mtransit.android.commons.provider.StatusFilter;
import org.mtransit.android.commons.provider.StatusProvider;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public final class DataSourceManager implements MTLog.Loggable {

	private static final String TAG = DataSourceManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static HashMap<String, Uri> uriMap = new HashMap<String, Uri>();

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

	public static ArrayList<ServiceUpdate> findServiceUpdates(Context context, String authority, ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter) {
		Cursor cursor = null;
		try {
			String serviceUpdateFilterJSONString = serviceUpdateFilter == null ? null : serviceUpdateFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), ServiceUpdateProvider.SERVICE_UPDATE_CONTENT_DIRECTORY);
			cursor = context.getContentResolver().query(uri, null, serviceUpdateFilterJSONString, null, null);
			return getServiceUpdates(cursor);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
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

	public static ScheduleTimestamps findScheduleTimestamps(Context context, String authority, ScheduleTimestampsFilter scheduleTimestampsFilter) {
		Cursor cursor = null;
		try {
			String scheduleTimestampsFilterJSONString = scheduleTimestampsFilter == null ? null : scheduleTimestampsFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), ScheduleTimestampsProvider.SCHEDULE_TIMESTAMPS_CONTENT_DIRECTORY);
			cursor = context.getContentResolver().query(uri, null, scheduleTimestampsFilterJSONString, null, null);
			return getScheduleTimestamp(cursor);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
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

	public static POIStatus findStatus(Context context, String authority, StatusFilter statusFilter) {
		Cursor cursor = null;
		try {
			String statusFilterJSONString = statusFilter == null ? null : statusFilter.toJSONStringStatic(statusFilter);
			Uri uri = Uri.withAppendedPath(getUri(authority), StatusProvider.STATUS_CONTENT_DIRECTORY);
			cursor = context.getContentResolver().query(uri, null, statusFilterJSONString, null, null);
			return getPOIStatus(cursor);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static POIStatus getPOIStatus(Cursor cursor) {
		POIStatus result = null;
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				int status = POIStatus.getTypeFromCursor(cursor);
				switch (status) {
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

	public static int findTypeId(Context context, String authority) {
		int result = -1;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), "type");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getInt(0);
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static void ping(Context context, String authority) {
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), "ping");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static String findAgencyLabel(Context context, String authority) {
		String result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), "label");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getString(0);
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static String findAgencyColor(Context context, String authority) {
		String result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), "color");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getString(0);
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static String findAgencyShortName(Context context, String authority) {
		String result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), "shortName");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getString(0);
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static Area findAgencyArea(Context context, String authority) {
		Area result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), "area");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = Area.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static JPaths findAgencyRTSRouteLogo(Context context, String authority) {
		JPaths result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(getUri(authority), "route"), "logo");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = JPaths.fromJSONString(cursor.getString(0));
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static Trip findRTSTrip(Context context, String authority, int tripId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSTripsUri(authority);
			String selection = GTFSRouteTripStopProvider.TripColumns.T_TRIP_K_ID + "=" + tripId;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_TRIP, selection, null, null);
			ArrayList<Trip> rtsTrips = getRTSTrips(cursor, authority);
			return rtsTrips == null || rtsTrips.size() == 0 ? null : rtsTrips.get(0);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static ArrayList<Trip> findRTSRouteTrips(Context context, String authority, int routeId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSTripsUri(authority);
			String selection = GTFSRouteTripStopProvider.TripColumns.T_TRIP_K_ROUTE_ID + "=" + routeId;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_TRIP, selection, null, null);
			return getRTSTrips(cursor, authority);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static ArrayList<Trip> getRTSTrips(Cursor cursor, String authority) {
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

	public static Route findRTSRoute(Context context, String authority, int routeId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSRoutesUri(authority);
			String selection = GTFSRouteTripStopProvider.RouteColumns.T_ROUTE_K_ID + "=" + routeId;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_ROUTE, selection, null, null);
			ArrayList<Route> rtsRoutes = getRTSRoutes(cursor, authority);
			return rtsRoutes == null || rtsRoutes.size() == 0 ? null : rtsRoutes.get(0);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static ArrayList<Route> findAllRTSAgencyRoutes(Context context, String authority) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSRoutesUri(authority);
			String selection = null;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_ROUTE, selection, null, null);
			return getRTSRoutes(cursor, authority);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static ArrayList<Route> getRTSRoutes(Cursor cursor, String authority) {
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

	public static POIManager findPOI(Context context, String authority, POIFilter poiFilter) {
		ArrayList<POIManager> pois = findPOIs(context, authority, poiFilter);
		return pois == null || pois.size() == 0 ? null : pois.get(0);
	}

	public static ArrayList<POIManager> findPOIs(Context context, String authority, POIFilter poiFilter) {
		Cursor cursor = null;
		try {
			String filterJsonString = POIFilter.toJSON(poiFilter).toString();
			String sortOrder = null;
			Uri uri = getPOIUri(authority);
			cursor = context.getContentResolver().query(uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, sortOrder);
			return getPOIs(cursor, authority);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
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
			cursor = context.getContentResolver().query(searchSuggestUri, null, null, null, null);
			return getSearchSuggest(cursor);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
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
		return Uri.withAppendedPath(getUri(authority), POIProvider.POI_CONTENT_DIRECTORY);
	}

	private static Uri getRTSRoutesUri(String authority) {
		return Uri.withAppendedPath(getUri(authority), "route");
	}

	private static Uri getRTSTripsUri(String authority) {
		return Uri.withAppendedPath(getUri(authority), "trip");
	}

}
