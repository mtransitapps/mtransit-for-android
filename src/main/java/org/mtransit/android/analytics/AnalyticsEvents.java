package org.mtransit.android.analytics;

public interface AnalyticsEvents {

	String FOUND_DISABLED_MODULE = "mt_found_disabled_module";
	String FOUND_APP_UPDATE = "mt_found_app_update";
	String SHOWED_APP_UPDATE_POI = "mt_showed_app_update_poi";
	String HIDDEN_APP_UPDATE_POI = "mt_hidden_app_update_poi";
	String CLICK_APP_UPDATE_POI = "mt_click_app_update_poi";
	String OPENED_GOOGLE_MAPS_TRIP_PLANNER = "mt_open_google_maps_trip_planner";
	String LOCATION_SETTINGS_RESOLUTION_AVAILABLE = "mt_location_settings_resolution";

	interface Params {
		String CODE = "mt_code";
		String PKG = "mt_pkg";
		String STATE = "mt_state";
	}
}
