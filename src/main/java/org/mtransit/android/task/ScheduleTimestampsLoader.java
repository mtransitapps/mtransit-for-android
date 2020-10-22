package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule.Timestamp;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.ScheduleProviderProperties;

import android.content.Context;

public class ScheduleTimestampsLoader extends MTAsyncTaskLoaderV4<ArrayList<Timestamp>> {

	private static final String TAG = ScheduleTimestampsLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<Timestamp> timestamps;
	private long startsAtInMs;
	private RouteTripStop rts;

	public ScheduleTimestampsLoader(Context context, RouteTripStop rts, long startsAtInMs) {
		super(context);
		this.rts = rts;
		this.startsAtInMs = startsAtInMs;
	}

	@Override
	public ArrayList<Timestamp> loadInBackgroundMT() {
		if (this.timestamps != null) {
			return this.timestamps;
		}
		this.timestamps = new ArrayList<>();
		long endsAtInMs = this.startsAtInMs + TimeUnit.DAYS.toMillis(1);
		ScheduleTimestampsProviderContract.Filter scheduleFilter = new ScheduleTimestampsProviderContract.Filter(this.rts, this.startsAtInMs, endsAtInMs);
		Collection<ScheduleProviderProperties> scheduleProviders = DataSourceProvider.get(getContext()).getTargetAuthorityScheduleProviders(
				this.rts.getAuthority());
		if (scheduleProviders != null) {
			for (ScheduleProviderProperties scheduleProvider : scheduleProviders) {
				ScheduleTimestamps scheduleTimestamps = DataSourceManager.findScheduleTimestamps(getContext(), scheduleProvider.getAuthority(), scheduleFilter);
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
	public void deliverResult(ArrayList<Timestamp> data) {
		this.timestamps = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

}
