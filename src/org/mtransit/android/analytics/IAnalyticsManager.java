package org.mtransit.android.analytics;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.ui.view.common.IActivity;

@SuppressWarnings("unused")
public interface IAnalyticsManager {

	void setUserProperty(@NonNull String name, int value);

	void setUserProperty(@NonNull String name, @NonNull String value);

	void logEvent(@NonNull String name);

	void logEvent(@NonNull String name, @Nullable AnalyticsEventsParamsProvider params);

	@MainThread
	void trackScreenView(@NonNull IActivity activity, @NonNull Trackable page);

	interface Trackable {
		@NonNull
		String getScreenName();
	}
}
