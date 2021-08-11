package org.mtransit.android.data;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.SimpleArrayMap;

import org.json.JSONObject;
import org.mtransit.android.commons.AppUpdateUtils;
import org.mtransit.android.commons.Constants;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mtransit.commons.FeatureFlags.F_APP_UPDATE;

@WorkerThread
public final class DataSourceManager implements MTLog.Loggable {

	private static final String LOG_TAG = DataSourceManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private static final SimpleArrayMap<String, Uri> uriMap = new SimpleArrayMap<>();

	@NonNull
	private static Uri getUri(@NonNull String authority) {
		Uri uri = uriMap.get(authority);
		if (uri == null) {
			synchronized (uriMap) {
				uri = uriMap.get(authority);
				if (uri == null) {
					uri = UriUtils.newContentUri(authority);
					uriMap.put(authority, uri);
				}
			}
		}
		return uri;
	}

	private DataSourceManager() {
	}

	@Nullable
	public static ArrayList<ServiceUpdate> findServiceUpdates(@NonNull Context context, @NonNull String authority,
															  @Nullable ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		Cursor cursor = null;
		try {
			String serviceUpdateFilterJSONString = serviceUpdateFilter == null ? null : serviceUpdateFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), ServiceUpdateProviderContract.SERVICE_UPDATE_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, serviceUpdateFilterJSONString, null, null);
			return getServiceUpdates(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	private static ArrayList<ServiceUpdate> getServiceUpdates(@Nullable Cursor cursor) {
		ArrayList<ServiceUpdate> result = new ArrayList<>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					result.add(ServiceUpdate.fromCursor(cursor));
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	@Nullable
	public static News findANews(@NonNull Context context, @NonNull String authority, @Nullable NewsProviderContract.Filter newsFilter) {
		ArrayList<News> news = findNews(context, authority, newsFilter);
		return news == null || news.size() == 0 ? null : news.get(0);
	}

	@Nullable
	public static ArrayList<News> findNews(@NonNull Context context, @NonNull String authority, @Nullable NewsProviderContract.Filter newsFilter) {
		Cursor cursor = null;
		try {
			String newsFilterJSONString = newsFilter == null ? null : newsFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), NewsProviderContract.NEWS_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, newsFilterJSONString, null, null);
			return getNews(cursor, authority);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	private static ArrayList<News> getNews(@Nullable Cursor cursor, @NonNull String authority) {
		ArrayList<News> result = new ArrayList<>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					result.add(News.fromCursorStatic(cursor, authority));
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	@Nullable
	public static ScheduleTimestamps findScheduleTimestamps(@NonNull Context context,
															@NonNull String authority,
															@Nullable ScheduleTimestampsProviderContract.Filter scheduleTimestampsFilter) {
		Cursor cursor = null;
		try {
			String scheduleTimestampsFilterJSONString = scheduleTimestampsFilter == null ? null : scheduleTimestampsFilter.toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), ScheduleTimestampsProviderContract.SCHEDULE_TIMESTAMPS_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, scheduleTimestampsFilterJSONString, null, null);
			return getScheduleTimestamp(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@Nullable
	private static ScheduleTimestamps getScheduleTimestamp(@Nullable Cursor cursor) {
		ScheduleTimestamps result = null;
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				result = ScheduleTimestamps.fromCursor(cursor);
			}
		}
		return result;
	}

	@Nullable
	public static POIStatus findStatus(@NonNull Context context, @NonNull String authority, @NonNull StatusProviderContract.Filter statusFilter) {
		Cursor cursor = null;
		try {
			String statusFilterJSONString = statusFilter.toJSONStringStatic(statusFilter);
			Uri uri = Uri.withAppendedPath(getUri(authority), StatusProviderContract.STATUS_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, statusFilterJSONString, null, null);
			return getPOIStatus(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@Nullable
	private static POIStatus getPOIStatus(@Nullable Cursor cursor) {
		POIStatus result = null;
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				int status = POIStatus.getTypeFromCursor(cursor);
				switch (status) {
				case POI.ITEM_STATUS_TYPE_NONE:
					result = null;
					break;
				case POI.ITEM_STATUS_TYPE_SCHEDULE:
					result = UISchedule.fromCursorWithExtra(cursor);
					break;
				case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
					result = AvailabilityPercent.fromCursorWithExtra(cursor);
					break;
				case POI.ITEM_STATUS_TYPE_APP:
					result = AppStatus.fromCursorWithExtra(cursor);
					break;
				default:
					MTLog.w(LOG_TAG, "getPOIStatus() > Unexpected status '%s'!", status);
					result = null;
					break;
				}
			}
		}
		return result;
	}

	public static void ping(@NonNull Context context, @NonNull String authority) {
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), ProviderContract.PING_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, null, null, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	public static int findAgencyAvailableVersionCode(@NonNull Context context, @NonNull String authority, boolean forceAppUpdateRefresh, boolean inFocus) {
		int availableVersionCode = -1;
		if (!F_APP_UPDATE) {
			MTLog.d(LOG_TAG, "findAgencyAvailableVersionCode() > SKIP (feature disabled)");
			return availableVersionCode;
		}
		Cursor cursor = null;
		try {
			String appUpdateFilterString = new AppUpdateUtils.AppUpdateFilter(forceAppUpdateRefresh, inFocus).toJSONString();
			Uri uri = Uri.withAppendedPath(getUri(authority), AgencyProviderContract.AVAILABLE_VERSION_CODE);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, appUpdateFilterString, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					final int availableVersionCodeIdx = cursor.getColumnIndex(AgencyProviderContract.AVAILABLE_VERSION_CODE);
					if (availableVersionCodeIdx >= 0) {
						availableVersionCode = cursor.getInt(availableVersionCodeIdx);
					}
				}
			}
		} catch (IllegalArgumentException iae) {
			MTLog.d(LOG_TAG, iae, "IAE: feature not supported yet?");
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return availableVersionCode;
	}

	@Nullable
	public static AgencyProperties findAgencyProperties(@NonNull Context context,
														@NonNull String authority,
														@NonNull DataSourceType dst,
														boolean isRTS,
														@Nullable JPaths logo,
														@NonNull String pkg,
														long longVersionCode,
														boolean enabled,
														int trigger) {
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
					Area area = Area.fromCursorNN(cursor);
					int maxValidInSec = -1; // unknown
					final int maxValidSecIdx = cursor.getColumnIndex(AgencyProviderContract.MAX_VALID_SEC);
					if (maxValidSecIdx >= 0) {
						maxValidInSec = cursor.getInt(maxValidSecIdx);
					}
					int availableVersionCode = -1;
					if (F_APP_UPDATE) {
						final int availableVersionCodeIdx = cursor.getColumnIndex(AgencyProviderContract.AVAILABLE_VERSION_CODE);
						if (availableVersionCodeIdx >= 0) {
							availableVersionCode = cursor.getInt(availableVersionCodeIdx);
						}
					}
					result = new AgencyProperties(
							authority,
							dst,
							shortName,
							longName,
							color,
							area,
							pkg,
							longVersionCode,
							availableVersionCode,
							true,
							enabled,
							isRTS,
							logo,
							maxValidInSec,
							trigger
					);
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	@Nullable
	public static JPaths findAgencyRTSRouteLogo(@NonNull Context context, @NonNull String authority) {
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
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	@SuppressWarnings("unused")
	@Nullable
	public static Trip findRTSTrip(@NonNull Context context, @NonNull String authority, int tripId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSTripsUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.TripColumns.T_TRIP_K_ID, tripId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_TRIP, selection, null, null);
			ArrayList<Trip> rtsTrips = getRTSTrips(cursor);
			return rtsTrips.size() == 0 ? null : rtsTrips.get(0);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@Nullable
	public static ArrayList<Trip> findRTSRouteTrips(@NonNull Context context, @NonNull String authority, long routeId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSTripsUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID, routeId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_TRIP, selection, null, null);
			return getRTSTrips(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	private static ArrayList<Trip> getRTSTrips(@Nullable Cursor cursor) {
		ArrayList<Trip> result = new ArrayList<>();
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

	@Nullable
	public static Route findRTSRoute(@NonNull Context context, @NonNull String authority, long routeId) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSRoutesUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.RouteColumns.T_ROUTE_K_ID, routeId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_ROUTE, selection, null, null);
			List<Route> rtsRoutes = getRTSRoutes(cursor);
			return rtsRoutes.size() == 0 ? null : rtsRoutes.get(0);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@WorkerThread
	@Nullable
	public static Cursor queryContentResolver(@NonNull ContentResolver contentResolver,
											  @NonNull Uri uri,
											  @Nullable String[] projection, @Nullable String selection,
											  @Nullable String[] selectionArgs,
											  @Nullable String sortOrder) {
		if (Constants.LOG_MT_QUERY) {
			MTLog.d(LOG_TAG,
					"QUERY['%s':'%s'(%s:%s)'%s'] ...",
					uri,
					(projection == null ? null : Arrays.asList(projection)),
					selection,
					(selectionArgs == null ? null : Arrays.asList(selectionArgs)),
					sortOrder);
		}
		final Cursor result = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
		if (Constants.LOG_MT_QUERY) {
			MTLog.d(LOG_TAG,
					"QUERY['%s':'%s'(%s:%s)'%s'] > DONE",
					uri,
					(projection == null ? null : Arrays.asList(projection)),
					selection,
					(selectionArgs == null ? null : Arrays.asList(selectionArgs)),
					sortOrder);
		}
		return result;
	}

	@Nullable
	public static List<Route> findAllRTSAgencyRoutes(@NonNull Context context, @NonNull String authority) {
		Cursor cursor = null;
		try {
			Uri uri = getRTSRoutesUri(authority);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_ROUTE, null, null, null);
			return getRTSRoutes(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	private static List<Route> getRTSRoutes(@Nullable Cursor cursor) {
		final List<Route> result = new ArrayList<>();
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

	@Nullable
	public static POIManager findPOI(@NonNull Context context, @NonNull String authority, @Nullable POIProviderContract.Filter poiFilter) {
		final List<POIManager> pois = findPOIs(context, authority, poiFilter);
		return pois == null || pois.size() == 0 ? null : pois.get(0);
	}

	@Nullable
	public static List<POIManager> findPOIs(@NonNull Context context, @NonNull String authority, @Nullable POIProviderContract.Filter poiFilter) {
		Cursor cursor = null;
		try {
			JSONObject filterJSON = POIProviderContract.Filter.toJSON(poiFilter);
			if (filterJSON == null) {
				MTLog.w(LOG_TAG, "Invalid POI filter!");
				return null;
			}
			String filterJsonString = filterJSON.toString();
			Uri uri = getPOIUri(authority);
			cursor = queryContentResolver(context.getContentResolver(), uri, POIProvider.PROJECTION_POI_ALL_COLUMNS, filterJsonString, null, null);
			return getPOIs(cursor, authority);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	private static List<POIManager> getPOIs(@Nullable Cursor cursor, @NonNull String authority) {
		ArrayList<POIManager> result = new ArrayList<>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					result.add(POIManager.fromCursorStatic(cursor, authority));
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	@Nullable
	public static HashSet<String> findSearchSuggest(@NonNull Context context, @NonNull String authority, @Nullable String query) {
		Cursor cursor = null;
		try {
			Uri searchSuggestUri = Uri.withAppendedPath(getUri(authority), SearchManager.SUGGEST_URI_PATH_QUERY);
			if (!TextUtils.isEmpty(query)) {
				searchSuggestUri = Uri.withAppendedPath(searchSuggestUri, Uri.encode(query));
			}
			cursor = queryContentResolver(context.getContentResolver(), searchSuggestUri, null, null, null, null);
			return getSearchSuggest(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	public static HashSet<String> getSearchSuggest(@Nullable Cursor cursor) {
		HashSet<String> results = new HashSet<>();
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

	@NonNull
	private static Uri getPOIUri(@NonNull String authority) {
		return Uri.withAppendedPath(getUri(authority), POIProviderContract.POI_PATH);
	}

	@NonNull
	private static Uri getRTSRoutesUri(@NonNull String authority) {
		return Uri.withAppendedPath(getUri(authority), GTFSProviderContract.ROUTE_PATH);
	}

	@NonNull
	private static Uri getRTSTripsUri(@NonNull String authority) {
		return Uri.withAppendedPath(getUri(authority), GTFSProviderContract.TRIP_PATH);
	}
}
