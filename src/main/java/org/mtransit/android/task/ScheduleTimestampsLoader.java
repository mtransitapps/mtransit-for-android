package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule.Timestamp;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.ScheduleProviderProperties;
import org.mtransit.android.datasource.DataSourcesRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class ScheduleTimestampsLoader extends MTAsyncTaskLoaderX<ArrayList<Timestamp>> {

	private static final String TAG = ScheduleTimestampsLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;
	private final long startsAtInMs;
	@NonNull
	private final RouteTripStop rts;

	@Nullable
	private ArrayList<Timestamp> timestamps;

	public ScheduleTimestampsLoader(@NonNull Context context,
									@NonNull DataSourcesRepository dataSourcesRepository,
									@NonNull RouteTripStop rts,
									long startsAtInMs) {
		super(context);
		this.dataSourcesRepository = dataSourcesRepository;
		this.rts = rts;
		this.startsAtInMs = startsAtInMs;
	}

	@Nullable
	@Override
	public ArrayList<Timestamp> loadInBackgroundMT() {
		if (this.timestamps != null) {
			return this.timestamps;
		}
		this.timestamps = new ArrayList<>();
		long endsAtInMs = this.startsAtInMs + TimeUnit.DAYS.toMillis(1);
		ScheduleTimestampsProviderContract.Filter scheduleFilter = new ScheduleTimestampsProviderContract.Filter(this.rts, this.startsAtInMs, endsAtInMs);
		Collection<ScheduleProviderProperties> scheduleProviders = this.dataSourcesRepository.getScheduleProviders(this.rts.getAuthority());
		for (ScheduleProviderProperties scheduleProvider : scheduleProviders) {
			ScheduleTimestamps scheduleTimestamps = DataSourceManager.findScheduleTimestamps(getContext(), scheduleProvider.getAuthority(), scheduleFilter);
			if (scheduleTimestamps != null && scheduleTimestamps.getTimestampsCount() > 0) {
				this.timestamps = scheduleTimestamps.getTimestamps();
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
	public void deliverResult(@Nullable ArrayList<Timestamp> data) {
		this.timestamps = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}
}
