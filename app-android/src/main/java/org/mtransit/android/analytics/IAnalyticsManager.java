package org.mtransit.android.analytics;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import org.mtransit.android.ui.view.common.IActivity;

@SuppressWarnings("unused")
public interface IAnalyticsManager {

	void setUserProperty(@NonNull @Size(min = 1L, max = 24L) String name, int value);

	void setUserProperty(@NonNull @Size(min = 1L, max = 24L) String name, @NonNull String value);

	void logEvent(@NonNull @Size(min = 1L, max = 40L) String name);

	void logEvent(@NonNull @Size(min = 1L, max = 40L) String name, @Nullable AnalyticsEventsParamsProvider params);

	@MainThread
	void trackScreenView(@NonNull IActivity activity, @NonNull Trackable page);

	interface Trackable {
		@NonNull
		String getScreenName();
	}
}
