package org.mtransit.android.analytics;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.ui.view.common.IActivity;

import java.util.Map;

public interface IAnalyticsManager {

	void trackUserProperty(@NonNull String name, @NonNull String value);

	void trackEvent(@NonNull String name);

	void trackEvent(@NonNull String name, @Nullable Map<String, Object> params);

	@MainThread
	void trackScreenView(@NonNull IActivity activity, @NonNull Trackable page);

	interface Trackable {
		@NonNull
		String getScreenName();
	}
}
