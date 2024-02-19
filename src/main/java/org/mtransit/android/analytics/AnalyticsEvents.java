package org.mtransit.android.analytics;

public interface AnalyticsEvents {

	String FOUND_DISABLED_MODULE = "mt_found_disabled_module";
	String SHOWED_APP_WAS_DISABLED_POI = "mt_showed_app_was_disabled_poi";
	String HIDDEN_APP_WAS_DISABLED_POI = "mt_hidden_app_was_disabled_poi";
	String CLICK_APP_WAS_DISABLED_POI = "mt_click_app_was_disabled_poi";

	String FOUND_APP_UPDATE = "mt_found_app_update";
	String SHOWED_APP_UPDATE_POI = "mt_showed_app_update_poi";
	String HIDDEN_APP_UPDATE_POI = "mt_hidden_app_update_poi";
	String CLICK_APP_UPDATE_POI = "mt_click_app_update_poi";

	String APP_RATINGS_REQUEST_CAN_DISPLAY = "mt_app_ratings_can_display";
	String APP_RATINGS_REQUEST_DISPLAYED = "mt_app_ratings_displayed";
	String APP_RATINGS_REQUEST_PLAY_ERROR = "mt_app_ratings_play_error";

	String OPENED_GOOGLE_MAPS_TRIP_PLANNER = "mt_open_google_maps_trip_planner";

	String LOCATION_SETTINGS_RESOLUTION_AVAILABLE = "mt_location_settings_resolution";

	interface Params {
		String CODE = "mt_code";
		String PKG = "mt_pkg";
		String STATE = "mt_state";
		String SCREEN = "mt_screen";
		String COUNT = "mt_count";
	}
}
