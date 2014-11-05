package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.UriUtils;
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
import org.mtransit.android.commons.task.MTAsyncTask;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

public class DataSourceProvider implements MTLog.Loggable {

	private static final String TAG = DataSourceProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static DataSourceProvider instance = null;

	private List<AgencyProperties> allAgencies = null;

	private List<StatusProviderProperties> allStatusProviders = null;

	private List<ScheduleProviderProperties> allScheduleProviders = null;

	private List<DataSourceType> allAgencyTypes = null;

	private WeakHashMap<DataSourceType, List<AgencyProperties>> allAgenciesByType = null;

	private WeakHashMap<String, AgencyProperties> allAgenciesByAuthority = null;

	private WeakHashMap<String, Set<StatusProviderProperties>> statusProvidersByTargetAuthority = null;

	private WeakHashMap<String, Set<ScheduleProviderProperties>> scheduleProvidersByTargetAuthority = null;

	private WeakHashMap<String, JPaths> rtsRouteLogoByAuthority = null;

	private HashMap<String, Uri> uriMap = new HashMap<String, Uri>();

	public static DataSourceProvider get() {
		if (instance == null) {
			instance = new DataSourceProvider();
		}
		return instance;
	}

	public static boolean isSet() {
		return instance != null;
	}

	public static void destroy() {
		if (instance != null) {
			instance.onDestroy();
			instance = null;
		}
	}

	public static void reset(Context context) {
		if (instance != null) {
			if (context == null) { // cannot compare w/o context
				destroy();
				triggerModulesUpdated();
			} else {
				final DataSourceProvider newInstance = new DataSourceProvider();
				newInstance.init(context);
				if (CollectionUtils.getSize(newInstance.allAgencies) != CollectionUtils.getSize(instance.allAgencies)
						|| CollectionUtils.getSize(newInstance.allStatusProviders) != CollectionUtils.getSize(instance.allStatusProviders)
						|| CollectionUtils.getSize(newInstance.allScheduleProviders) != CollectionUtils.getSize(instance.allScheduleProviders)) {
					instance = newInstance;
					triggerModulesUpdated();
				}
			}
		}
	}

	private DataSourceProvider() {
	}

	public Uri getUri(String authority) {
		Uri uri = uriMap.get(authority);
		if (uri == null) {
			uri = UriUtils.newContentUri(authority);
			uriMap.put(authority, uri);
		}
		return uri;
	}

	public List<DataSourceType> getAvailableAgencyTypes(Context context) {
		if (this.allAgencyTypes == null) {
			init(context);
		}
		return this.allAgencyTypes;
	}

	public List<AgencyProperties> getAllDataSources(Context context) {
		if (this.allAgencies == null) {
			init(context);
		}
		return this.allAgencies;
	}

	public List<AgencyProperties> getTypeDataSources(Context context, DataSourceType type) {
		if (this.allAgenciesByType == null) {
			init(context);
		}
		return this.allAgenciesByType.get(type);
	}

	public AgencyProperties getAgency(Context context, String authority) {
		if (this.allAgenciesByAuthority == null) {
			init(context);
		}
		return this.allAgenciesByAuthority.get(authority);
	}

	public Collection<StatusProviderProperties> getTargetAuthorityStatusProviders(Context context, String targetAuthority) {
		if (this.statusProvidersByTargetAuthority == null) {
			init(context);
		}
		return this.statusProvidersByTargetAuthority.get(targetAuthority);
	}

	public Collection<ScheduleProviderProperties> getTargetAuthorityScheduleProviders(Context context, String targetAuthority) {
		if (this.scheduleProvidersByTargetAuthority == null) {
			init(context);
		}
		return this.scheduleProvidersByTargetAuthority.get(targetAuthority);
	}

	public JPaths getRTSRouteLogo(Context context, String authority) {
		if (this.rtsRouteLogoByAuthority == null) {
			init(context);
		}
		return this.rtsRouteLogoByAuthority.get(authority);
	}

	public void onDestroy() {
		if (this.allAgencies != null) {
			this.allAgencies.clear();
			this.allAgencies = null;
		}
		if (this.allAgencyTypes != null) {
			this.allAgencyTypes.clear();
			this.allAgencyTypes = null;
		}
		if (this.allAgenciesByType != null) {
			this.allAgenciesByType.clear();
			this.allAgenciesByType = null;
		}
		if (this.allAgenciesByAuthority != null) {
			this.allAgenciesByAuthority.clear();
			this.allAgenciesByAuthority = null;
		}
		if (this.allStatusProviders != null) {
			this.allStatusProviders.clear();
			this.allStatusProviders = null;
		}
		if (this.statusProvidersByTargetAuthority != null) {
			this.statusProvidersByTargetAuthority.clear();
			this.statusProvidersByTargetAuthority = null;
		}
		if (this.rtsRouteLogoByAuthority != null) {
			this.rtsRouteLogoByAuthority.clear();
			this.rtsRouteLogoByAuthority = null;
		}
		if (this.allScheduleProviders != null) {
			this.allScheduleProviders.clear();
			this.allScheduleProviders = null;
		}
		if (this.scheduleProvidersByTargetAuthority != null) {
			this.scheduleProvidersByTargetAuthority.clear();
			this.scheduleProvidersByTargetAuthority = null;
		}
	}

	private synchronized void init(Context context) {
		this.allAgencies = new ArrayList<AgencyProperties>();
		this.allAgencyTypes = new ArrayList<DataSourceType>();
		this.allAgenciesByType = new WeakHashMap<DataSourceType, List<AgencyProperties>>();
		this.allAgenciesByAuthority = new WeakHashMap<String, AgencyProperties>();
		this.allStatusProviders = new ArrayList<StatusProviderProperties>();
		this.statusProvidersByTargetAuthority = new WeakHashMap<String, Set<StatusProviderProperties>>();
		this.rtsRouteLogoByAuthority = new WeakHashMap<String, JPaths>();
		this.allScheduleProviders = new ArrayList<ScheduleProviderProperties>();
		this.scheduleProvidersByTargetAuthority = new WeakHashMap<String, Set<ScheduleProviderProperties>>();
		String agencyProviderMetaData = context.getString(R.string.agency_provider);
		String rtsProviderMetaData = context.getString(R.string.rts_provider);
		String scheduleProviderMetaData = context.getString(R.string.schedule_provider);
		String statusProviderMetaData = context.getString(R.string.status_provider);
		String statusProviderTargetMetaData = context.getString(R.string.status_provider_target);
		String scheduleProviderTargetMetaData = context.getString(R.string.schedule_provider_target);
		final PackageManager pm = context.getPackageManager();
		for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
			ProviderInfo[] providers = packageInfo.providers;
			if (providers != null) {
				for (ProviderInfo provider : providers) {
					if (provider.metaData != null) {
						if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
							final Uri contentUri = getUri(provider.authority);
							final String label = findAgencyLabel(context, contentUri);
							final String shortName = findAgencyShortName(context, contentUri);
							final int typeId = findTypeId(context, contentUri);
							final DataSourceType type = DataSourceType.parseId(typeId);
							final Area area = findAgencyArea(context, contentUri);
							final boolean isRTS = rtsProviderMetaData.equals(provider.metaData.getString(rtsProviderMetaData));
							final JPaths jPath = isRTS ? findAgencyRTSRouteLogo(context, contentUri) : null;
							if (type != null && typeId >= 0) {
								final AgencyProperties newAgency = new AgencyProperties(provider.authority, type, shortName, label, area, isRTS);
								addNewAgency(newAgency);
								if (jPath != null) {
									this.rtsRouteLogoByAuthority.put(newAgency.getAuthority(), jPath);
								}
							} else {
								MTLog.d(this, "Invalid type, skipping agency provider.");
							}
						}
						if (statusProviderMetaData.equals(provider.metaData.getString(statusProviderMetaData))) {
							String targetAuthority = provider.metaData.getString(statusProviderTargetMetaData);
							StatusProviderProperties newStatusProvider = new StatusProviderProperties(provider.authority, targetAuthority);
							addNewStatusProvider(newStatusProvider);
						}
						if (scheduleProviderMetaData.equals(provider.metaData.getString(scheduleProviderMetaData))) {
							String targetAuthority = provider.metaData.getString(scheduleProviderTargetMetaData);
							ScheduleProviderProperties newScheduleProvider = new ScheduleProviderProperties(provider.authority, targetAuthority);
							addNewScheduleProvider(newScheduleProvider);
						}
					}
				}
			}
		}
		CollectionUtils.sort(this.allAgencyTypes, new DataSourceType.DataSourceTypeShortNameComparator(context));
		CollectionUtils.sort(this.allAgencies, AgencyProperties.SHORT_NAME_COMPARATOR);
		if (this.allAgenciesByType != null) {
			for (DataSourceType type : this.allAgenciesByType.keySet()) {
				CollectionUtils.sort(this.allAgenciesByType.get(type), AgencyProperties.SHORT_NAME_COMPARATOR);
			}
		}
	}

	private void addNewStatusProvider(StatusProviderProperties newStatusProvider) {
		this.allStatusProviders.add(newStatusProvider);
		String newScheduleProviderTargetAuthority = newStatusProvider.getTargetAuthority();
		if (!this.statusProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
			this.statusProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<StatusProviderProperties>());
		}
		this.statusProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newStatusProvider);
	}

	private void addNewScheduleProvider(ScheduleProviderProperties newScheduleProvider) {
		this.allScheduleProviders.add(newScheduleProvider);
		String newScheduleProviderTargetAuthority = newScheduleProvider.getTargetAuthority();
		if (!this.scheduleProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
			this.scheduleProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<ScheduleProviderProperties>());
		}
		this.scheduleProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newScheduleProvider);
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

	private void addNewAgency(AgencyProperties newAgency) {
		this.allAgencies.add(newAgency);
		this.allAgenciesByAuthority.put(newAgency.getAuthority(), newAgency);
		DataSourceType newAgencyType = newAgency.getType();
		if (!this.allAgencyTypes.contains(newAgencyType)) {
			this.allAgencyTypes.add(newAgencyType);
		}
		if (!this.allAgenciesByType.containsKey(newAgencyType)) {
			this.allAgenciesByType.put(newAgencyType, new ArrayList<AgencyProperties>());
		}
		this.allAgenciesByType.get(newAgencyType).add(newAgency);
	}

	private int findTypeId(Context context, Uri contentUri) {
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
			MTLog.w(this, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	private String findAgencyLabel(Context context, Uri contentUri) {
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
			MTLog.w(this, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	private String findAgencyShortName(Context context, Uri contentUri) {
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
			MTLog.w(this, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	private Area findAgencyArea(Context context, Uri contentUri) {
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
			MTLog.w(this, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	private JPaths findAgencyRTSRouteLogo(Context context, Uri contentUri) {
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
			MTLog.w(this, t, "Error!");
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

	private static Uri getPOIUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, POIProvider.POI_CONTENT_DIRECTORY);
	}

	private static Uri getRTSRoutesUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, "route");
	}

	private static Uri getRTSTripsUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, "trip");
	}

	private static WeakHashMap<ModulesUpdateListener, Object> modulesUpdateListeners = new WeakHashMap<ModulesUpdateListener, Object>();

	public static void addModulesUpdateListerner(ModulesUpdateListener listener) {
		try {
			if (!modulesUpdateListeners.containsKey(listener)) {
				modulesUpdateListeners.put(listener, null);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "addModulesUpdateListerner() > error while adding listerner '%s'!", listener);
		}
	}

	public static void removeModulesUpdateListerner(ModulesUpdateListener listener) {
		try {
			if (modulesUpdateListeners.containsKey(listener)) {
				modulesUpdateListeners.remove(listener);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "removeModulesUpdateListerner() > error while removing listerner '%s'!", listener);
		}
	}

	private static TriggerModulesUpdatedTask triggerModulesUpdatedTask = null;

	private static void triggerModulesUpdated() {
		if (modulesUpdateListeners != null) {
			if (triggerModulesUpdatedTask != null && triggerModulesUpdatedTask.getStatus() != AsyncTask.Status.RUNNING) {
				triggerModulesUpdatedTask.cancel(true);
			}
			final WeakHashMap<ModulesUpdateListener, Object> listenersCopy = new WeakHashMap<ModulesUpdateListener, Object>(modulesUpdateListeners);
			triggerModulesUpdatedTask = new TriggerModulesUpdatedTask(listenersCopy);
			triggerModulesUpdatedTask.execute();
		}
	}

	public static interface ModulesUpdateListener {
		public void onModulesUpdated();
	}

	private static class TriggerModulesUpdatedTask extends MTAsyncTask<Void, ModulesUpdateListener, Void> {

		private static final String TAG = TriggerModulesUpdatedTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakHashMap<ModulesUpdateListener, Object> listerners;

		public TriggerModulesUpdatedTask(WeakHashMap<ModulesUpdateListener, Object> listeners) {
			this.listerners = listeners;
		}

		@Override
		protected Void doInBackgroundMT(Void... params) {
			if (this.listerners != null) {
				Iterator<ModulesUpdateListener> it = this.listerners.keySet().iterator();
				while (it.hasNext()) {
					ModulesUpdateListener listener = it.next();
					if (listener != null) {
						publishProgress(listener);
					}
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(ModulesUpdateListener... values) {
			if (values != null && values.length > 0) {
				values[0].onModulesUpdated();
			}
		}
	}
}
