package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LocationUtils.LocationPOI;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.ServiceUpdateProvider;
import org.mtransit.android.commons.provider.StatusFilter;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.NearbyFragment;
import org.mtransit.android.ui.fragment.POIFragment;
import org.mtransit.android.ui.fragment.RTSRouteFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

public class POIManager implements LocationPOI, MTLog.Loggable {

	private static final String TAG = POIManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		if (this.poi != null) {
			return TAG + "-" + this.poi.getUUID();
		}
		return TAG;
	}

	private static Integer defaultPoiTextColor = null;

	public static int getDefaultPOITextColor(Context context) {
		if (defaultPoiTextColor == null) {
			defaultPoiTextColor = ColorUtils.getTextColorPrimary(context);
		}
		return defaultPoiTextColor;
	}

	private static ForegroundColorSpan defaultPoiTextColorSpan = null;

	public static ForegroundColorSpan getDefaultPOITextColorSpan(Context context) {
		if (defaultPoiTextColorSpan == null) {
			defaultPoiTextColorSpan = SpanUtils.getTextColor(getDefaultPOITextColor(context));
		}
		return defaultPoiTextColorSpan;
	}


	public static final POIAlphaComparator POI_ALPHA_COMPARATOR = new POIAlphaComparator();

	private static int defaultDistanceAndCompassColor = -1;

	public static int getDefaultDistanceAndCompassColor(Context context) {
		if (defaultDistanceAndCompassColor < 0) {
			defaultDistanceAndCompassColor = ColorUtils.getTextColorTertiary(context);
		}
		return defaultDistanceAndCompassColor;
	}

	public POI poi;

	private CharSequence distanceString = null;

	private float distance = -1;

	private POIStatus status;
	private ArrayList<ServiceUpdate> serviceUpdates;
	private boolean inFocus = false;

	private long lastFindStatusTimestampMs = -1;

	private WeakReference<StatusLoader.StatusLoaderListener> statusLoaderListenerWR;

	private int scheduleMaxDataRequests = Schedule.ScheduleStatusFilter.MAX_DATA_REQUESTS_DEFAULT;

	public POIManager(POI poi) {
		this(poi, null);
	}

	public POIManager(POI poi, POIStatus status) {
		this.poi = poi;
		this.status = status;
	}

	@Override
	public String toString() {
		return new StringBuilder(POIManager.class.getSimpleName()).append('[')//
				.append("poi:").append(this.poi) //
				.append(']').toString();
	}

	public void resetLastFindTimestamps() {
		this.lastFindServiceUpdateTimestampMs = -1;
		this.lastFindStatusTimestampMs = -1;
	}

	public void setInFocus(boolean inFocus) {
		this.inFocus = inFocus;
	}

	public boolean isInFocus() {
		return this.inFocus;
	}

	@Override
	public float getDistance() {
		return distance;
	}

	@Override
	public void setDistance(float distance) {
		this.distance = distance;
	}

	@Override
	public CharSequence getDistanceString() {
		return distanceString;
	}

	@Override
	public void setDistanceString(CharSequence distanceString) {
		this.distanceString = distanceString;
	}

	public void setStatusLoaderListener(StatusLoader.StatusLoaderListener statusLoaderListener) {
		this.statusLoaderListenerWR = new WeakReference<StatusLoader.StatusLoaderListener>(statusLoaderListener);
	}

	public String getLocation() {
		if (this.poi != null && this.poi instanceof Module) {
			return ((Module) this.poi).getLocation();
		}
		return null;
	}

	public int getStatusType() {
		return this.poi.getStatusType();
	}

	public boolean hasStatus() {
		return this.status != null;
	}

	public boolean setStatus(POIStatus newStatus) {
		if (newStatus == null || !newStatus.isUseful()) {
			return false; // no change
		}
		switch (getStatusType()) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			if (!(newStatus instanceof Schedule)) {
				MTLog.w(this, "setStatus() > Unexpected schedule status '%s'!", newStatus);
				return false; // no change
			}
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			if (!(newStatus instanceof AvailabilityPercent)) {
				MTLog.w(this, "setStatus() > Unexpected availability percent status '%s'!", newStatus);
				return false; // no change
			}
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			if (!(newStatus instanceof AppStatus)) {
				MTLog.w(this, "setStatus() > Unexpected app status '%s'!", newStatus);
				return false; // no change
			}
			break;
		default:
			MTLog.w(this, "setStatus() > Unexpected status '%s'!", newStatus);
			return false; // no change
		}
		if (this.status != null && this.status.getReadFromSourceAtInMs() > newStatus.getReadFromSourceAtInMs()) {
			return false; // no change
		}
		this.status = newStatus;
		return true; // change
	}

	public POIStatus getStatusOrNull() {
		return this.status;
	}

	public POIStatus getStatus(Context context) {
		if (this.status == null || this.lastFindStatusTimestampMs < 0 || this.inFocus || !this.status.isUseful()) {
			findStatus(context, false);
		}
		return this.status;
	}


	private boolean findStatus(Context context, boolean skipIfBusy) {
		long findStatusTimestampMs = TimeUtils.currentTimeToTheMinuteMillis();
		boolean isNotSkipped = false;
		if (this.lastFindStatusTimestampMs != findStatusTimestampMs) { // IF not same minute as last findStatus() call DO
			StatusFilter filter = getFilter();
			if (filter != null) {
				filter.setInFocus(this.inFocus);
				StatusLoader.StatusLoaderListener listener = this.statusLoaderListenerWR == null ? null : this.statusLoaderListenerWR.get();
				isNotSkipped = StatusLoader.get().findStatus(context, this, filter, listener, skipIfBusy);
				if (isNotSkipped) {
					this.lastFindStatusTimestampMs = findStatusTimestampMs;
				}
			}
		}
		return isNotSkipped;
	}

	public void setScheduleMaxDataRequests(int scheduleMaxDataRequests) {
		this.scheduleMaxDataRequests = scheduleMaxDataRequests;
	}

	private StatusFilter getFilter() {
		switch (getStatusType()) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			if (this.poi instanceof RouteTripStop) {
				RouteTripStop rts = (RouteTripStop) this.poi;
				Schedule.ScheduleStatusFilter filter = new Schedule.ScheduleStatusFilter(this.poi.getUUID(), rts);
				filter.setLookBehindInMs(TimeUtils.RECENT_IN_MILLIS);
				filter.setMaxDataRequests(this.scheduleMaxDataRequests);
				return filter;
			} else {
				MTLog.w(this, "Schedule filter w/o '%s'!", this.poi);
				return null;
			}
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return new AvailabilityPercent.AvailabilityPercentStatusFilter(this.poi.getUUID());
		case POI.ITEM_STATUS_TYPE_APP:
			if (poi instanceof Module) {
				Module module = (Module) this.poi;
				return new AppStatus.AppStatusFilter(this.poi.getUUID(), module.getPkg());
			} else {
				MTLog.w(this, "App status filter w/o '%s'!", this.poi);
				return null;
			}
		default:
			MTLog.w(this, "Unexpected status type '%s´  for filter!", getStatusType());
			return null;
		}
	}

	private WeakReference<ServiceUpdateLoader.ServiceUpdateLoaderListener> serviceUpdateLoaderListenerWR;

	public void setServiceUpdateLoaderListener(ServiceUpdateLoader.ServiceUpdateLoaderListener serviceUpdateLoaderListener) {
		this.serviceUpdateLoaderListenerWR = new WeakReference<ServiceUpdateLoader.ServiceUpdateLoaderListener>(serviceUpdateLoaderListener);
	}

	public boolean hasServiceUpdates() {
		return CollectionUtils.getSize(this.serviceUpdates) != 0;
	}

	public void setServiceUpdates(Collection<ServiceUpdate> newServiceUpdates) {
		if (this.serviceUpdates == null) {
			this.serviceUpdates = new ArrayList<ServiceUpdate>();
		} else {
			this.serviceUpdates.clear();
		}
		if (newServiceUpdates != null) {
			this.serviceUpdates.addAll(newServiceUpdates);
			CollectionUtils.sort(this.serviceUpdates, ServiceUpdate.HIGHER_SEVERITY_FIRST_COMPARATOR);
		}
	}

	public ArrayList<ServiceUpdate> getServiceUpdatesOrNull() {
		return this.serviceUpdates;
	}

	public Boolean isServiceUpdateWarning(Context context) {
		if (this.serviceUpdates == null || this.lastFindServiceUpdateTimestampMs < 0 || this.inFocus || !areServiceUpdatesUseful()) {
			findServiceUpdates(context, false);
		}
		return ServiceUpdate.isSeverityWarning(this.serviceUpdates);
	}

	public ArrayList<ServiceUpdate> getServiceUpdates(Context context) {
		if (this.serviceUpdates == null || this.lastFindServiceUpdateTimestampMs < 0 || this.inFocus || !areServiceUpdatesUseful()) {
			findServiceUpdates(context, false);
		}
		return this.serviceUpdates;
	}

	private boolean areServiceUpdatesUseful() {
		if (this.serviceUpdates != null) {
			for (ServiceUpdate serviceUpdate : this.serviceUpdates) {
				if (serviceUpdate.isUseful()) {
					return true;
				}
			}
		}
		return false;
	}


	private long lastFindServiceUpdateTimestampMs = -1;

	private boolean findServiceUpdates(Context context, boolean skipIfBusy) {
		long findServiceUpdateTimestampMs = TimeUtils.currentTimeToTheMinuteMillis();
		boolean isNotSkipped = false;
		if (this.lastFindServiceUpdateTimestampMs != findServiceUpdateTimestampMs) { // IF not same minute as last findStatus() call DO
			ServiceUpdateProvider.ServiceUpdateFilter filter = new ServiceUpdateProvider.ServiceUpdateFilter(this.poi);
			filter.setInFocus(this.inFocus);
			ServiceUpdateLoader.ServiceUpdateLoaderListener listener = this.serviceUpdateLoaderListenerWR == null ? null : this.serviceUpdateLoaderListenerWR
					.get();
			isNotSkipped = ServiceUpdateLoader.get().findServiceUpdate(context, this, filter, listener, skipIfBusy);
			if (isNotSkipped) {
				this.lastFindServiceUpdateTimestampMs = findServiceUpdateTimestampMs;
			}
		}
		return isNotSkipped;
	}

	public CharSequence[] getActionsItems(Context context, CharSequence defaultAction) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return new CharSequence[] { //
			defaultAction, //
					FavoriteManager.isFavorite(context, poi.getUUID()) ? context.getString(R.string.remove_fav) : context.getString(R.string.add_fav) //
			};
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			RouteTripStop rts = (RouteTripStop) poi;
			return new CharSequence[] { //
					context.getString(R.string.view_stop), //
					TextUtils.isEmpty(rts.route.shortName) ? context.getString(R.string.view_stop_route) : context.getString(
							R.string.view_stop_route_and_route, rts.route.shortName), //
					FavoriteManager.isFavorite(context, poi.getUUID()) ? context.getString(R.string.remove_fav) : context.getString(R.string.add_fav) //
			};
		case POI.ITEM_ACTION_TYPE_APP:
			if (PackageManagerUtils.isAppInstalled(context, ((Module) poi).getPkg())) {
				return new CharSequence[] { //
				context.getString(R.string.join_leave_test_on_store), //
						context.getString(R.string.rate_on_store), //
						context.getString(R.string.uninstall), //
				};
			} else {
				return new CharSequence[] { //
				context.getString(R.string.join_leave_test_on_store), //
						context.getString(R.string.download_on_store), //
				};
			}
		case POI.ITEM_ACTION_TYPE_PLACE:
			return new CharSequence[] { defaultAction };
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return new CharSequence[] { defaultAction };
		}

	}

	public boolean onActionsItemClick(Activity activity, int itemClicked, FavoriteManager.FavoriteUpdateListener listener) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return onActionsItemClickFavoritable(activity, itemClicked, listener);
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return onActionsItemClickRTS(activity, itemClicked, listener);
		case POI.ITEM_ACTION_TYPE_APP:
			return onActionsItemClickApp(activity, itemClicked, listener);
		case POI.ITEM_ACTION_TYPE_PLACE:
			return onActionsItemClickPlace(activity, itemClicked, listener);
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return false; // NOT HANDLED
		}
	}

	private boolean onActionsItemClickApp(Activity activity, int itemClicked, FavoriteManager.FavoriteUpdateListener listener) {
		switch (itemClicked) {
		case 0:
			StoreUtils.viewTestingWebPage(activity, ((Module) poi).getPkg());
			return true; // HANDLED
		case 1:
			StoreUtils.viewAppPage(activity, ((Module) poi).getPkg());
			return true; // HANDLED
		case 2:
			PackageManagerUtils.uninstallApp(activity, ((Module) poi).getPkg());
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickPlace(Activity activity, int itemClicked, FavoriteManager.FavoriteUpdateListener listener) {
		switch (itemClicked) {
		case 0:
			Integer optColor = null;
			if (poi instanceof RouteTripStop) {
				optColor = ((RouteTripStop) poi).route.getColorInt();
			} else {
				AgencyProperties agency = DataSourceProvider.get(activity).getAgency(activity, poi.getAuthority());
				if (agency != null && agency.hasColor()) {
					optColor = agency.getColorInt();
				}
			}
			((MainActivity) activity).addFragmentToStack(NearbyFragment.newFixedOnInstance(null, poi.getLat(), poi.getLng(), poi.getName(), optColor));
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickRTS(Activity activity, int itemClicked, FavoriteManager.FavoriteUpdateListener listener) {
		switch (itemClicked) {
		case 1:
			RouteTripStop rts = (RouteTripStop) poi;
			((MainActivity) activity).addFragmentToStack(RTSRouteFragment.newInstance(rts.getAuthority(), rts.route.id, rts.trip.id, rts.stop.id, rts.route));
			return true; // HANDLED
		case 2:
			return addRemoteFavorite(activity, FavoriteManager.isFavorite(activity, poi.getUUID()), listener);
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickFavoritable(Activity activity, int itemClicked, FavoriteManager.FavoriteUpdateListener listener) {
		switch (itemClicked) {
		case 1:
			return addRemoteFavorite(activity, FavoriteManager.isFavorite(activity, poi.getUUID()), listener);
		}
		return false; // NOT HANDLED
	}

	public boolean addRemoteFavorite(Activity activity, boolean isFavorite, FavoriteManager.FavoriteUpdateListener listener) {
		FavoriteManager.addOrDeleteFavorite(activity, isFavorite, this.poi.getUUID());
		if (listener != null) {
			listener.onFavoriteUpdated();
		}
		return true; // HANDLED
	}

	public boolean isFavoritable() {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return true;
		case POI.ITEM_ACTION_TYPE_APP:
		case POI.ITEM_ACTION_TYPE_PLACE:
			return false;
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return false;
		}
	}

	private boolean showPoiViewerScreen(Activity activity) {
		if (activity == null) {
			return false; // show long-click menu
		}
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_APP:
			StoreUtils.viewAppPage(activity, ((Module) poi).getPkg());
			return true; // handled
		case POI.ITEM_ACTION_TYPE_PLACE:
			((MainActivity) activity).addFragmentToStack(NearbyFragment.newFixedOnInstance(null, poi.getLat(), poi.getLng(), poi.getName(), null));
			return true; // nearby screen shown
		}
		if (activity instanceof MainActivity) {
			((MainActivity) activity).addFragmentToStack(POIFragment.newInstance(this.poi.getUUID(), this.poi.getAuthority(), null, this));
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	public void onActionItemLongClick(Activity activity, FavoriteManager.FavoriteUpdateListener favoriteUpdateListener) {
		if (activity == null) {
			return;
		}
		showPoiMenu(activity, favoriteUpdateListener);
	}

	public void onActionItemClick(Activity activity, FavoriteManager.FavoriteUpdateListener favoriteUpdateListener) {
		if (activity == null) {
			return;
		}
		boolean poiScreenShow = showPoiViewerScreen(activity);
		if (!poiScreenShow) {
			showPoiMenu(activity, favoriteUpdateListener);
		}
	}

	private boolean showPoiMenu(final Activity activity, final FavoriteManager.FavoriteUpdateListener favoriteUpdateListener) {
		if (activity == null) {
			return false;
		}
		switch (this.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
		case POI.ITEM_VIEW_TYPE_MODULE:
			new AlertDialog.Builder(activity).setTitle(this.poi.getName())
					.setItems(getActionsItems(activity, activity.getString(R.string.view_details)), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							if (onActionsItemClick(activity, item, favoriteUpdateListener)) {
								return;
							}
							switch (item) {
							case 0:
								showPoiViewerScreen(activity);
								break;
							default:
								MTLog.w(POIManager.this, "Unexpected action item '%s'!", item);
								break;
							}
						}
					}).create().show();
			return true;
		default:
			MTLog.w(this, "Unknow view type '%s' for poi '%s'!", this.poi.getType(), this);
			return false;
		}
	}

	public static POIManager fromCursorStatic(Cursor cursor, String authority) {
		switch (DefaultPOI.getTypeFromCursor(cursor)) {
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return new POIManager(DefaultPOI.fromCursorStatic(cursor, authority));
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return new POIManager(RouteTripStop.fromCursorStatic(cursor, authority));
		case POI.ITEM_VIEW_TYPE_MODULE:
			return new POIManager(Module.fromCursorStatic(cursor, authority));
		default:
			MTLog.w(TAG, "Unexpected POI type '%s'! (using default)", DefaultPOI.getTypeFromCursor(cursor));
			return new POIManager(DefaultPOI.fromCursorStatic(cursor, authority));
		}
	}

	@Override
	public Double getLat() {
		return this.poi.getLat();
	}

	@Override
	public void setLat(Double lat) {
		this.poi.setLat(lat);
	}

	@Override
	public void setLng(Double lng) {
		this.poi.setLng(lng);
	}

	@Override
	public Double getLng() {
		return this.poi.getLng();
	}

	@Override
	public boolean hasLocation() {
		return this.poi != null && this.poi.hasLocation();
	}

	private static class POIAlphaComparator implements Comparator<POIManager> {
		@Override
		public int compare(POIManager lhs, POIManager rhs) {
			POI lhsPoi = lhs == null ? null : lhs.poi;
			POI rhsPoi = rhs == null ? null : rhs.poi;
			if (lhsPoi == null && rhsPoi == null) {
				return ComparatorUtils.SAME;
			}
			if (lhsPoi == null) {
				return ComparatorUtils.BEFORE;
			} else if (rhsPoi == null) {
				return ComparatorUtils.AFTER;
			}
			return lhsPoi.compareToAlpha(null, rhsPoi);
		}
	}


}
