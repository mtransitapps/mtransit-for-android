package org.mtransit.android.analytics;

public interface AnalyticsEvents {

	String FOUND_DISABLED_MODULE = "mt_found_disabled_module";
	String FOUND_APP_UPDATE = "mt_found_app_update";
	String SHOWED_APP_UPDATE_POI = "mt_showed_app_update_poi";
	String HIDDEN_APP_UPDATE_POI = "mt_hidden_app_update_poi";
	String CLICK_APP_UPDATE_POI = "mt_click_app_update_poi";

	interface Params {
		String PKG = "mt_pkg";
		String STATE = "mt_state";
	}
}
