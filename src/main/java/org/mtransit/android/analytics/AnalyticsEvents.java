package org.mtransit.android.analytics;

public interface AnalyticsEvents {

	String FOUND_DISABLED_MODULE = "mt_found_disabled_module";
	String FOUND_APP_UPDATE = "mt_found_app_update";

	interface Params {
		String PKG = "mt_pkg";
		String STATE = "mt_state";
	}
}
