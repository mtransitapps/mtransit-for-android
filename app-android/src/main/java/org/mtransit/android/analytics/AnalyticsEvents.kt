package org.mtransit.android.analytics

/**
 * Adds to [com.google.firebase.analytics.FirebaseAnalytics.Event]
 */
interface AnalyticsEvents {

    companion object {
        const val FOUND_DISABLED_MODULE = "mt_found_disabled_module"
        const val SHOWED_APP_WAS_DISABLED_POI = "mt_showed_app_was_disabled_poi"
        const val HIDDEN_APP_WAS_DISABLED_POI = "mt_hidden_app_was_disabled_poi"
        const val CLICK_APP_WAS_DISABLED_POI = "mt_click_app_was_disabled_poi"

        const val FOUND_APP_UPDATE = "mt_found_app_update"
        const val SHOWED_APP_UPDATE_POI = "mt_showed_app_update_poi"
        const val HIDDEN_APP_UPDATE_POI = "mt_hidden_app_update_poi"
        const val CLICK_APP_UPDATE_POI = "mt_click_app_update_poi"

        const val APP_RATINGS_REQUEST_CAN_DISPLAY = "mt_app_ratings_can_display"
        const val APP_RATINGS_REQUEST_DISPLAYED = "mt_app_ratings_displayed"
        const val APP_RATINGS_REQUEST_PLAY_ERROR = "mt_app_ratings_play_error"

        const val OPENED_GOOGLE_MAPS_TRIP_PLANNER = "mt_open_google_maps_trip_planner"

        const val LOCATION_SETTINGS_RESOLUTION_AVAILABLE = "mt_location_settings_resolution"

        const val BUTTON_CLICK = "mt_button_click"
    }

    /**
     * Adds to [com.google.firebase.analytics.FirebaseAnalytics.Param]
     */
    interface Params {
        companion object {
            const val CODE = "mt_code"
            const val PKG = "mt_pkg"
            const val STATE = "mt_state"
            const val SCREEN = "mt_screen"
            const val COUNT = "mt_count"
            const val BUTTON_NAME = "mt_button_name"
        }
    }
}
