package org.mtransit.android.analytics;

import androidx.annotation.NonNull;

import org.mtransit.android.ui.view.common.IActivity;

public interface IAnalyticsManager {

	void trackUserProperty(@NonNull String name, @NonNull String value);

	void trackScreenView(@NonNull IActivity activity, @NonNull Trackable page);

	interface Trackable {
		@NonNull
		String getScreenName();
	}
}
