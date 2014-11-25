package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.HashSet;

import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
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

	private DataSourceManager() {
	}

	public static ArrayList<ServiceUpdate> findServiceUpdates(Context context, Uri contentUri, ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter) {
		Cursor cursor = null;
		try {
			String serviceUpdateFilterJSONString = serviceUpdateFilter == null ? null : serviceUpdateFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(contentUri, ServiceUpdateProvider.SERVICE_UPDATE_CONTENT_DIRECTORY);
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

	public static ScheduleTimestamps findScheduleTimestamps(Context context, Uri contentUri, ScheduleTimestampsFilter scheduleTimestampsFilter) {
		Cursor cursor = null;
		try {
			String scheduleTimestampsFilterJSONString = scheduleTimestampsFilter == null ? null : scheduleTimestampsFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(contentUri, ScheduleTimestampsProvider.SCHEDULE_TIMESTAMPS_CONTENT_DIRECTORY);
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

	public static POIStatus findStatus(Context context, Uri contentUri, StatusFilter statusFilter) {
		Cursor cursor = null;
		try {
			String statusFilterJSONString = statusFilter == null ? null : statusFilter.toJSONStringStatic(statusFilter);
			Uri uri = Uri.withAppendedPath(contentUri, StatusProvider.STATUS_CONTENT_DIRECTORY);
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

	public static String findAgencyColor(Context context, Uri contentUri) {
		String result = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(contentUri, "color");
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
			final ArrayList<Trip> rtsTrips = getRTSTrips(cursor, contentUri.getAuthority());
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

	public static ArrayList<Trip> findRTSRouteTrips(Context context, Uri contentUri, int routeId) {
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

	private static ArrayList<Trip> getRTSTrips(Cursor cursor, String authority) {
		ArrayList<Trip> result = new ArrayList<Trip>();
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
			final ArrayList<Route> rtsRoutes = getRTSRoutes(cursor, contentUri.getAuthority());
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

	public static ArrayList<Route> findAllRTSAgencyRoutes(Context context, Uri contentUri) {
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

	private static ArrayList<Route> getRTSRoutes(Cursor cursor, String authority) {
		ArrayList<Route> result = new ArrayList<Route>();
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

	public static POIManager findPOI(Context context, Uri contentUri, POIFilter poiFilter) {
		final ArrayList<POIManager> pois = findPOIs(context, contentUri, poiFilter);
		return pois == null || pois.size() == 0 ? null : pois.get(0);
	}

	public static ArrayList<POIManager> findPOIs(Context context, Uri contentUri, POIFilter poiFilter) {
		Cursor cursor = null;
		try {
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

	public static HashSet<String> findSearchSuggest(Context context, Uri contentUri, String query) {
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

	public static HashSet<String> getSearchSuggest(Cursor cursor) {
		HashSet<String> results = new HashSet<String>();
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
