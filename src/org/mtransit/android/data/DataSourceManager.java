package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ScheduleTimestampsFilter;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.ScheduleTimestampsProvider;
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

	private DataSourceManager() {
	}

	public static ScheduleTimestamps findScheduleTimestamps(Context context, Uri contentUri, ScheduleTimestampsFilter scheduleTimestampsFilter) {
		ScheduleTimestamps result = null;
		Cursor cursor = null;
		try {
			String scheduleTimestampsFilterFilterJSONString = scheduleTimestampsFilter == null ? null : scheduleTimestampsFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(contentUri, ScheduleTimestampsProvider.SCHEDULE_TIMESTAMPS_CONTENT_DIRECTORY);
			cursor = context.getContentResolver().query(uri, null, scheduleTimestampsFilterFilterJSONString, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = ScheduleTimestamps.fromCursor(cursor);
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

	public static POIStatus findStatus(Context context, Uri contentUri, StatusFilter statusFilter) {
		POIStatus result = null;
		Cursor cursor = null;
		try {
			String statusFilterJSONString = statusFilter == null ? null : statusFilter.toJSONStringStatic(statusFilter);
			Uri uri = Uri.withAppendedPath(contentUri, StatusProvider.STATUS_CONTENT_DIRECTORY);
			cursor = context.getContentResolver().query(uri, null, statusFilterJSONString, null, null);
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
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static int findTypeId(Context context, Uri contentUri) {
		int result = -1;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(contentUri, "type");
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

	public static void ping(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(contentUri, "ping");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static String findAgencyLabel(Context context, Uri contentUri) {
		String result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(contentUri, "label");
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

	public static String findAgencyShortName(Context context, Uri contentUri) {
		String result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(contentUri, "shortName");
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

	public static Area findAgencyArea(Context context, Uri contentUri) {
		Area result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(contentUri, "area");
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

	public static JPaths findAgencyRTSRouteLogo(Context context, Uri contentUri) {
		JPaths result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(contentUri, "route"), "logo");
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

	public static Trip findRTSTrip(Context context, Uri contentUri, int tripId) {
		Cursor cursor = null;
		try {
			final Uri uri = getRTSTripsUri(contentUri);
			final String selection = GTFSRouteTripStopProvider.TripColumns.T_TRIP_K_ID + "=" + tripId;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_TRIP, selection, null, null);
			final List<Trip> rtsTrips = getRTSTrips(cursor, contentUri.getAuthority());
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

	public static List<Trip> findRTSRouteTrips(Context context, Uri contentUri, int routeId) {
		Cursor cursor = null;
		try {
			final Uri uri = getRTSTripsUri(contentUri);
			final String selection = GTFSRouteTripStopProvider.TripColumns.T_TRIP_K_ROUTE_ID + "=" + routeId;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_TRIP, selection, null, null);
			return getRTSTrips(cursor, contentUri.getAuthority());
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static List<Trip> getRTSTrips(Cursor cursor, String authority) {
		List<Trip> result = new ArrayList<Trip>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					final Trip fromCursor = Trip.fromCursor(cursor);
					result.add(fromCursor);
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static Route findRTSRoute(Context context, Uri contentUri, int routeId) {
		Cursor cursor = null;
		try {
			final Uri uri = getRTSRoutesUri(contentUri);
			final String selection = GTFSRouteTripStopProvider.RouteColumns.T_ROUTE_K_ID + "=" + routeId;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_ROUTE, selection, null, null);
			final List<Route> rtsRoutes = getRTSRoutes(cursor, contentUri.getAuthority());
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

	public static List<Route> findAllRTSAgencyRoutes(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			final Uri uri = getRTSRoutesUri(contentUri);
			final String selection = null;
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_ROUTE, selection, null, null);
			return getRTSRoutes(cursor, contentUri.getAuthority());
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static List<Route> getRTSRoutes(Cursor cursor, String authority) {
		List<Route> result = new ArrayList<Route>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					final Route fromCursor = Route.fromCursor(cursor);
					result.add(fromCursor);
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static List<POIManager> findRTSTripPOIs(Context context, Uri contentUri, int tripId) {
		Cursor cursor = null;
		try {
			final String sortOrder = GTFSRouteTripStopProvider.RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC";
			final Uri uri = getPOIUri(contentUri);
			final String selection = GTFSRouteTripStopProvider.RouteTripStopColumns.T_TRIP_K_ID + "=" + tripId;
			POIFilter poiFilter = new POIFilter(selection);
			String filterJsonString = POIFilter.toJSON(poiFilter).toString();
			cursor = context.getContentResolver().query(uri, GTFSRouteTripStopProvider.PROJECTION_RTS_POI, filterJsonString, null, sortOrder);
			return getRTSPOIs(cursor, contentUri.getAuthority());
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static List<POIManager> getRTSPOIs(Cursor cursor, String authority) {
		List<POIManager> result = new ArrayList<POIManager>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					final RouteTripStop rts = RouteTripStop.fromCursorStatic(cursor, authority);
					final POIManager fromCursor = new POIManager(rts);
					result.add(fromCursor);
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static POIManager findPOIWithUUID(Context context, Uri contentUri, String uuid) {
		Cursor cursor = null;
		try {
			POIFilter poiFilter = new POIFilter(POIProvider.POIColumns.T_POI_K_UUID_META + " = '" + uuid + "'");
			String filterJsonString = POIFilter.toJSON(poiFilter).toString();
			final String sortOrder = null;
			final Uri uri = getPOIUri(contentUri);
			cursor = context.getContentResolver().query(uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, sortOrder);
			final List<POIManager> pois = getPOIs(cursor, contentUri.getAuthority());
			if (pois != null && pois.size() > 0) {
				return pois.get(0);
			}
			return null;
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static List<POIManager> findPOIsWithUUIDs(Context context, Uri contentUri, Set<String> uuids) {
		Cursor cursor = null;
		try {
			POIFilter poiFilter = new POIFilter(uuids);
			String filterJsonString = POIFilter.toJSON(poiFilter).toString();
			final String sortOrder = null;
			final Uri uri = getPOIUri(contentUri);
			cursor = context.getContentResolver().query(uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, sortOrder);
			return getPOIs(cursor, contentUri.getAuthority());
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static List<POIManager> findPOIWithSearchList(Context context, Uri contentUri, String query, boolean hideDecentOnly) {
		Cursor cursor = null;
		try {
			POIFilter poiFilter = new POIFilter(new String[] { query });
			if (hideDecentOnly) {
				poiFilter.addExtra("decentOnly", true);
			}
			String filterJsonString = POIFilter.toJSON(poiFilter).toString();
			final String sortOrder = null;
			final Uri uri = getPOIUri(contentUri);
			cursor = context.getContentResolver().query(uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, sortOrder);
			return getPOIs(cursor, contentUri.getAuthority());
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static List<POIManager> findAllAgencyPOIs(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			POIFilter poiFilter = new POIFilter(StringUtils.EMPTY);
			String filterJsonString = POIFilter.toJSON(poiFilter).toString();
			final String sortOrder = null;
			final Uri uri = getPOIUri(contentUri);
			cursor = context.getContentResolver().query(uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, sortOrder);
			return getPOIs(cursor, contentUri.getAuthority());
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static List<POIManager> findPOIsWithLatLngList(Context context, Uri contentUri, double lat, double lng, double aroundDiff, boolean hideDecentOnly) {
		Cursor cursor = null;
		try {
			POIFilter poiFilter = new POIFilter(lat, lng, aroundDiff);
			if (hideDecentOnly) {
				poiFilter.addExtra("decentOnly", true);
			}
			String filterJsonString = POIFilter.toJSON(poiFilter).toString();
			final String sortOrder = null;
			final Uri uri = getPOIUri(contentUri);
			cursor = context.getContentResolver().query(uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, sortOrder);
			return getPOIs(cursor, contentUri.getAuthority());
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static List<POIManager> getPOIs(Cursor cursor, String authority) {
		List<POIManager> result = new ArrayList<POIManager>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					POIManager fromCursor = POIManager.fromCursorStatic(cursor, authority);
					result.add(fromCursor);
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static Set<String> findSearchSuggest(Context context, Uri contentUri, String query) {
		Cursor cursor = null;
		try {
			Uri searchSuggestUri = Uri.withAppendedPath(contentUri, SearchManager.SUGGEST_URI_PATH_QUERY);
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

	public static Set<String> getSearchSuggest(Cursor cursor) {
		Set<String> results = new HashSet<String>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				final int text1ColumnIdx = cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1);
				do {
					final String suggest = cursor.getString(text1ColumnIdx);
					results.add(suggest);
				} while (cursor.moveToNext());
			}
		}
		return results;
	}

	private static Uri getPOIUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, POIProvider.POI_CONTENT_DIRECTORY);
	}

	private static Uri getRTSRoutesUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, "route");
	}

	private static Uri getRTSTripsUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, "trip");
	}

}
