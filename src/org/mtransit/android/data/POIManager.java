package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.Comparator;

import org.mtransit.android.commons.LocationUtils.LocationPOI;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.AvailabilityPercentStatusFilter;
import org.mtransit.android.commons.provider.ScheduleStatusFilter;
import org.mtransit.android.commons.provider.StatusFilter;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.provider.FavoriteManager.FavoriteUpdateListener;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.task.StatusLoader.StatusLoaderListener;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class POIManager implements LocationPOI, MTLog.Loggable {

	private static final String TAG = POIManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final POIDistanceComparator POI_DISTANCE_COMPARATOR = new POIDistanceComparator();

	public static final POIAlphaComparator POI_ALPHA_COMPATOR = new POIAlphaComparator();

	public POI poi;

	private CharSequence distanceString = null;

	private float distance = -1;

	private POIStatus status;

	private long lastFindStatusTimestampMs = -1;

	private WeakReference<StatusLoaderListener> statusLoaderListenerWR;

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

	public int getStatusType() {
		return this.poi.getStatusType();
	}

	public boolean hasStatus() {
		return this.status != null;
	}

	public void setStatus(POIStatus status) {
		if (status == null) {
			this.status = null;
			return;
		}
		switch (getStatusType()) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			if (status instanceof Schedule) {
				this.status = (Schedule) status;
			} else {
				MTLog.w(this, "Unexpected schedule status '%s'!", status);
			}
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			if (status instanceof AvailabilityPercent) {
				this.status = (AvailabilityPercent) status;
			} else {
				MTLog.w(this, "Unexpected availability percent status '%s'!", status);
			}
			break;
		}
	}

	public POIStatus getStatusOrNull() {
		return this.status;
	}

	public POIStatus getStatus(Context context) {
		if (this.status == null || !status.isUseful()) {
			findStatus(context, false);
		}
		return this.status;
	}

	public boolean pingStatus(Context context) {
		if (this.status == null) {
			return findStatus(context, true);
		} else {
			return false;
		}
	}

	private boolean findStatus(Context context, boolean skipIfBusy) {
		long findStatusTimestampMs = TimeUtils.currentTimeToTheMinuteMillis();
		boolean isNotSkipped = false;
		if (this.lastFindStatusTimestampMs != findStatusTimestampMs) { // IF not same minute as last findStatus() call DO
			final StatusFilter filter = getFilter(findStatusTimestampMs);
			if (filter != null) {
				StatusLoader.StatusLoaderListener listener = this.statusLoaderListenerWR == null ? null : this.statusLoaderListenerWR.get();
				isNotSkipped = StatusLoader.get().findStatus(context, this, filter, listener, skipIfBusy);
				if (isNotSkipped) {
					this.lastFindStatusTimestampMs = findStatusTimestampMs;
				}
			}
		}
		return isNotSkipped;
	}

	private StatusFilter getFilter(long findStatusTimestampMs) {
		switch (getStatusType()) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			if (this.poi instanceof RouteTripStop) {
				RouteTripStop rts = (RouteTripStop) this.poi;
				ScheduleStatusFilter filter = new ScheduleStatusFilter(this.poi.getUUID(), rts);
				filter.setTimestamp(findStatusTimestampMs);
				return filter;
			} else {
				MTLog.w(this, "Schedule fiter w/o '%s'!", this.poi);
				return null;
			}
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return new AvailabilityPercentStatusFilter(this.poi.getUUID());
		default:
			MTLog.w(this, "Unexpected status type '%s´  for filter!", getStatusType());
			return null;
		}
	}

	public CharSequence[] getActionsItems(Context context, CharSequence defaultAction, boolean isFavorite) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return new CharSequence[] {//
			defaultAction, //
					isFavorite ? context.getString(R.string.remove_fav) : context.getString(R.string.add_fav) //
			};
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return new CharSequence[] {//
			context.getString(R.string.view_stop), //
					context.getString(R.string.view_stop_route), //
					isFavorite ? context.getString(R.string.remove_fav) : context.getString(R.string.add_fav) //
			};
		default:
			return new CharSequence[] { defaultAction };
		}

	}

	public boolean onActionsItemClick(Activity activity, int itemClicked, boolean isFavorite, FavoriteUpdateListener listener) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return onActionsItemClickFavoritable(activity, itemClicked, isFavorite, listener);
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return onActionsItemClickRTS(activity, itemClicked, isFavorite, listener);
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return false; // NOT HANDLED
		}
	}

	private boolean onActionsItemClickRTS(Activity activity, int itemClicked, boolean isFavorite, FavoriteUpdateListener listener) {
		switch (itemClicked) {
		case 1:
			return true; // HANDLED
		case 2:
			return addRemoteFavorite(activity, isFavorite, listener);
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickFavoritable(Activity activity, int itemClicked, boolean isFavorite, FavoriteUpdateListener listener) {
		switch (itemClicked) {
		case 1:
			return addRemoteFavorite(activity, isFavorite, listener);
		}
		return false; // NOT HANDLED
	}

	private boolean addRemoteFavorite(Activity activity, boolean isFavorite, FavoriteUpdateListener listener) {
		FavoriteManager.addOrDeleteFavorite(activity, isFavorite, this.poi.getUUID()/* , getFavoriteType() */);
		if (listener != null) {
			listener.onFavoriteUpdated();
		}
		return true; // HANDLED
	}

	public boolean onActionItemClick(Activity activity) {
		return false; // NOT HANDLED
	}

	public static POIManager fromCursorStatic(Cursor cursor, String authority) {
		switch (DefaultPOI.getTypeFromCursor(cursor)) {
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return new POIManager(DefaultPOI.fromCursorStatic(cursor, authority));
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return new POIManager(RouteTripStop.fromCursorStatic(cursor, authority));
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
		return this.poi.hasLocation();
	}

	private static class POIAlphaComparator implements Comparator<POIManager> {
		@Override
		public int compare(POIManager lhs, POIManager rhs) {
			final POI lhsPoi = lhs == null ? null : lhs.poi;
			final POI rhsPoi = rhs == null ? null : rhs.poi;
			if (lhsPoi == null && rhsPoi == null) {
				return 0;
			}
			if (lhsPoi == null) {
				return -1;
			} else if (rhsPoi == null) {
				return +1;
			}
			return lhsPoi.compareToAlpha(null, rhsPoi);
		}
	}

	private static class POIDistanceComparator implements Comparator<POIManager> {
		@Override
		public int compare(POIManager lhs, POIManager rhs) {
			if (lhs.poi instanceof RouteTripStop && rhs.poi instanceof RouteTripStop) {
				RouteTripStop alhs = (RouteTripStop) lhs.poi;
				RouteTripStop arhs = (RouteTripStop) rhs.poi;
				if (alhs.stop.id == arhs.stop.id) {
					if (!TextUtils.isEmpty(alhs.route.shortName) || !TextUtils.isEmpty(arhs.route.shortName)) {
						try {
							return Integer.valueOf(alhs.route.shortName) - Integer.valueOf(arhs.route.shortName);
						} catch (NumberFormatException nfe) {
							return alhs.route.shortName.compareTo(arhs.route.shortName);
						}
					}
				}
			}
			float d1 = lhs.getDistance();
			float d2 = rhs.getDistance();
			if (d1 > d2) {
				return +1;
			} else if (d1 < d2) {
				return -1;
			} else {
				return 0;
			}
		}
	}

}
