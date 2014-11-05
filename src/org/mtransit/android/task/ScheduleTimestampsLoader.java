package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ScheduleTimestampsFilter;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.ScheduleProviderProperties;

import android.content.Context;
import android.net.Uri;

public class ScheduleTimestampsLoader extends MTAsyncTaskLoaderV4<List<Schedule.Timestamp>> {

	private static final String TAG = ScheduleTimestampsLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private List<Schedule.Timestamp> timestamps;
	private long startsAtInMs;
	private RouteTripStop rts;

	public ScheduleTimestampsLoader(Context context, RouteTripStop rts, long startsAtInMs) {
		super(context);
		this.rts = rts;
		this.startsAtInMs = startsAtInMs;
	}

	@Override
	public List<Schedule.Timestamp> loadInBackgroundMT() {
		if (this.timestamps != null) {
			return this.timestamps;
		}
		this.timestamps = new ArrayList<Schedule.Timestamp>();
		long endsAtInMs = this.startsAtInMs + TimeUtils.ONE_DAY_IN_MS;
		ScheduleTimestampsFilter scheduleFilter = new ScheduleTimestampsFilter(this.rts, this.startsAtInMs, endsAtInMs);
		Collection<ScheduleProviderProperties> scheduleProviders = DataSourceProvider.get().getTargetAuthorityScheduleProviders(getContext(),
				this.rts.getAuthority());
		if (scheduleProviders != null) {
			for (ScheduleProviderProperties scheduleProvider : scheduleProviders) {
				Uri contentUri = UriUtils.newContentUri(scheduleProvider.getAuthority());
				ScheduleTimestamps scheduleTimestamps = DataSourceProvider.findScheduleTimestamps(getContext(), contentUri, scheduleFilter);
				if (scheduleTimestamps != null && scheduleTimestamps.getTimestampsCount() > 0) {
					this.timestamps = scheduleTimestamps.getTimestamps();
				}
			}
		}
		return this.timestamps;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (this.timestamps != null) {
			deliverResult(this.timestamps);
		}
		if (this.timestamps == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(List<Schedule.Timestamp> data) {
		this.timestamps = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

}
