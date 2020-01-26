package org.mtransit.android.analytics;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class AnalyticsEventsParamsProvider {

	@NonNull
	private final Map<String, Object> params;

	public AnalyticsEventsParamsProvider() {
		params = new HashMap<>();
	}

	@NonNull
	public AnalyticsEventsParamsProvider put(@NonNull String name, @NonNull String value) {
		params.put(name, value);
		return this;
	}

	@NonNull
	public AnalyticsEventsParamsProvider put(@NonNull String name, @NonNull Long value) {
		params.put(name, value);
		return this;
	}

	@NonNull
	public AnalyticsEventsParamsProvider put(@NonNull String name, @NonNull Double value) {
		params.put(name, value);
		return this;
	}

	@NonNull
	public Map<String, Object> to() {
		return params;
	}
}
