package org.mtransit.android.data;

import java.lang.ref.WeakReference;

import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.ScheduleStatusFilter;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.task.StatusLoader.StatusLoaderListener;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class RouteTripStopWithData implements POI, MTLog.Loggable {

	private static final String TAG = RouteTripStopWithData.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private Schedule status;
	private long lastFindStatusTimestampMs = -1;
	public RouteTripStop rts;
	private WeakReference<StatusLoaderListener> statusLoaderListenerWR;

	public RouteTripStopWithData(RouteTripStop rts) {
		this.rts = rts;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	public void setStatusLoaderListener(StatusLoader.StatusLoaderListener statusLoaderListener) {
		this.statusLoaderListenerWR = new WeakReference<StatusLoader.StatusLoaderListener>(statusLoaderListener);
	}

	@Override
	public boolean hasStatus() {
		return this.status != null;
	}

	@Override
	public void setStatus(POIStatus status) {
		if (status == null) {
			this.status = null;
			return;
		}
		if (status instanceof Schedule) {
			this.status = (Schedule) status;
		} else {
			MTLog.w(this, "Unexpected status '%s'!", status);
		}
	}

	@Override
	public POIStatus getStatusOrNull() {
		return this.status;
	}

	public void setSchedule(Schedule schedule) {
		this.status = schedule;
	}

	@Override
	public POIStatus getStatus(Context context) {
		if (this.status == null || !status.isUseful()) {
			findStatus(context, false);
		}
		return this.status;
	}

	@Override
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
			StatusLoader.StatusLoaderListener listener = this.statusLoaderListenerWR == null ? null : this.statusLoaderListenerWR.get();
			ScheduleStatusFilter filter = new ScheduleStatusFilter(getUUID(), this.rts);
			filter.setTimestamp(findStatusTimestampMs);
			isNotSkipped = StatusLoader.get().findStatus(context, this, /* after, */filter, listener, skipIfBusy);
			if (isNotSkipped) {
				this.lastFindStatusTimestampMs = findStatusTimestampMs;
			}
		}
		return isNotSkipped;
	}

	@Override
	public int getActionsType() {
		return POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP;
	}

	@Override
	public CharSequence[] getActionsItems(Context context, CharSequence defaultAction, boolean isFavorite) {
		return new CharSequence[] {//
		context.getString(R.string.view_stop), //
				context.getString(R.string.view_stop_route), //
				isFavorite ? context.getString(R.string.remove_fav) : context.getString(R.string.add_fav) //
		};
	}

	@Override
	public boolean onActionsItemClick(Activity activity, int itemClicked, boolean isFavorite, POIUpdateListener listener) {
		switch (itemClicked) {
		case 1:
			return true; // HANDLED
		case 2:
			FavoriteManager.addOrDeleteFavorite(activity, isFavorite, getUUID()/* , getFavoriteType() */);
			if (listener != null) {
				listener.onPOIUpdated();
			}
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	@Override
	public boolean onActionItemClick(Activity activity) {
		return false; // NOT HANDLED
	}

	private CharSequence distanceString = null;
	private float distance = -1;

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

	// FORWARD POI CALLS TO INNER ROUTE TRIP STOP OBJECT

	@Override
	public String getAuthority() {
		return this.rts.getAuthority();
	}

	@Override
	public void setAuthority(String authority) {
		this.rts.setAuthority(authority);
	}

	@Override
	public int getId() {
		return this.rts.getId();
	}

	@Override
	public void setId(int id) {
		this.rts.setId(id);
	}

	@Override
	public String getName() {
		return this.rts.getName();
	}

	@Override
	public void setName(String name) {
		this.rts.setName(name);
	}

	@Override
	public Double getLat() {
		return this.rts.getLat();
	}

	@Override
	public void setLat(Double lat) {
		this.rts.setLat(lat);
	}

	@Override
	public Double getLng() {
		return this.rts.getLng();
	}

	@Override
	public void setLng(Double lng) {
		this.rts.setLng(lng);
	}

	@Override
	public boolean hasLocation() {
		return this.rts.hasLocation();
	}

	@Override
	public String getUUID() {
		return this.rts.getUUID();
	}

	@Override
	public int getType() {
		return this.rts.getType();
	}

	@Override
	public void setType(int type) {
		this.rts.setType(type);
	}

	@Override
	public JSONObject toJSON() {
		return this.rts.toJSON();
	}

	@Override
	public POI fromJSON(JSONObject json) {
		return this.rts.fromJSON(json);
	}

	@Override
	public ContentValues toContentValues() {
		return this.rts.toContentValues();
	}

	@Override
	public POI fromCursor(Cursor cursor, String authority) {
		return this.fromCursor(cursor, authority);
	}

}
