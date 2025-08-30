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
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.Area;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.AgencyProviderContract;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.android.commons.provider.NewsProviderContract;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.provider.ProviderContract;
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract;
import org.mtransit.android.commons.provider.ServiceUpdateProviderContract;
import org.mtransit.android.commons.provider.StatusProviderContract;
import org.mtransit.android.commons.provider.config.news.NewsProviderConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
public static NewsProviderConfig findNewsProviderConfig(@NonNull Context context, @NonNull String authority) {
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(getUri(authority), NewsProviderContract.CONFIG_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, null, null, null);
			return NewsProviderConfig.fromCursor(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
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
					break;
				}
			}
		}
		return result;
	}

	public static void ping(@NonNull Context context, @NonNull String authority) {
		Cursor cursor = null;
		try {
			final Uri uri = Uri.withAppendedPath(getUri(authority), ProviderContract.PING_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, null, null, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@Nullable
	public static Integer findAgencyAvailableVersionCode(@NonNull Context context, @NonNull String authority, boolean forceAppUpdateRefresh, boolean inFocus) {
		Integer availableVersionCode = null;
		Cursor cursor = null;
		try {
			final String appUpdateFilterString = new AppUpdateUtils.AppUpdateFilter(forceAppUpdateRefresh, inFocus).toJSONString();
			final Uri uri = Uri.withAppendedPath(getUri(authority), AgencyProviderContract.AVAILABLE_VERSION_CODE);
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
														boolean isRDS,
														@Nullable JPaths logo,
														@NonNull String pkg,
														long longVersionCode,
														boolean enabled,
														int trigger) {
		AgencyProperties result = null;
		Cursor cursor = null;
		try {
			final Uri uri = Uri.withAppendedPath(getUri(authority), AgencyProviderContract.ALL_PATH);
			cursor = queryContentResolver(context.getContentResolver(), uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					final String shortName = CursorExtKt.getString(cursor, AgencyProviderContract.SHORT_NAME_PATH);
					final String longName = CursorExtKt.getString(cursor, AgencyProviderContract.LABEL_PATH);
					final String color = CursorExtKt.optString(cursor, AgencyProviderContract.COLOR_PATH, null);
					final Area area = Area.fromCursorNN(cursor);
					final int maxValidInSec = CursorExtKt.optIntNN(cursor, AgencyProviderContract.MAX_VALID_SEC, -1);
					final int availableVersionCode = CursorExtKt.optIntNN(cursor, AgencyProviderContract.AVAILABLE_VERSION_CODE, -1);
					final String contactUsWeb = CursorExtKt.optString(cursor, AgencyProviderContract.CONTACT_US_WEB, null);
					final String contactUsWebFr = CursorExtKt.optString(cursor, AgencyProviderContract.CONTACT_US_WEB_FR, null);
					final String faresWeb = CursorExtKt.optString(cursor, AgencyProviderContract.FARES_WEB, null);
					final String faresWebFr = CursorExtKt.optString(cursor, AgencyProviderContract.FARES_WEB_FR, null);
					final DataSourceType exType = DataSourceType.parseId(
							CursorExtKt.optIntNN(cursor, AgencyProviderContract.EXTENDED_TYPE_ID, DataSourceTypeId.INVALID)
					);
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
							isRDS,
							logo,
							maxValidInSec,
							trigger,
							contactUsWeb,
							contactUsWebFr,
							faresWeb,
							faresWebFr,
							exType
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
	public static JPaths findAgencyRDSRouteLogo(@NonNull Context context, @NonNull String authority) {
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
	public static Direction findRDSDirection(@NonNull Context context, @NonNull String authority, int directionId) {
		Cursor cursor = null;
		try {
			Uri uri = getRDSDirectionsUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.DirectionColumns.T_DIRECTION_K_ID, directionId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_DIRECTION, selection, null, null);
			ArrayList<Direction> rdsDirections = getRDSDirections(cursor);
			return rdsDirections.isEmpty() ? null : rdsDirections.get(0);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@Nullable
	public static ArrayList<Direction> findRDSRouteDirections(@NonNull Context context, @NonNull String authority, long routeId) {
		Cursor cursor = null;
		try {
			Uri uri = getRDSDirectionsUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.DirectionColumns.T_DIRECTION_K_ROUTE_ID, routeId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_DIRECTION, selection, null, null);
			return getRDSDirections(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	private static ArrayList<Direction> getRDSDirections(@Nullable Cursor cursor) {
		ArrayList<Direction> result = new ArrayList<>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				do {
					Direction fromCursor = Direction.fromCursor(cursor);
					result.add(fromCursor);
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	@Nullable
	public static Route findRDSRoute(@NonNull Context context, @NonNull String authority, long routeId) {
		Cursor cursor = null;
		try {
			final Uri uri = getRDSRoutesUri(authority);
			String selection = SqlUtils.getWhereEquals(GTFSProviderContract.RouteColumns.T_ROUTE_K_ID, routeId);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_ROUTE, selection, null, null);
			final List<Route> rdsRoutes = getRDSRoutes(cursor);
			return rdsRoutes.isEmpty() ? null : rdsRoutes.get(0);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

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
	public static List<Route> findAllRDSAgencyRoutes(@NonNull Context context, @NonNull String authority) {
		Cursor cursor = null;
		try {
			Uri uri = getRDSRoutesUri(authority);
			cursor = queryContentResolver(context.getContentResolver(), uri, GTFSProviderContract.PROJECTION_ROUTE, null, null, null);
			return getRDSRoutes(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	@NonNull
	private static List<Route> getRDSRoutes(@Nullable Cursor cursor) {
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
		return pois == null || pois.isEmpty() ? null : pois.get(0);
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
		final ArrayList<POIManager> result = new ArrayList<>();
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
	private static Uri getRDSRoutesUri(@NonNull String authority) {
		return Uri.withAppendedPath(getUri(authority), GTFSProviderContract.ROUTE_PATH);
	}

	@NonNull
	private static Uri getRDSDirectionsUri(@NonNull String authority) {
		return Uri.withAppendedPath(getUri(authority), GTFSProviderContract.DIRECTION_PATH);
	}
}
