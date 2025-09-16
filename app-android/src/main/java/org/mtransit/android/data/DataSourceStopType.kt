package org.mtransit.android.data

import androidx.annotation.StringRes
import org.mtransit.android.R

@Suppress("unused")
enum class DataSourceStopType(
    val id: Int,
    @param:StringRes val stopStringResId: Int,
    @param:StringRes val stopsStringResId: Int,
) {

    STOP(1, R.string.agency_stop_type_stop, R.string.agency_stop_type_stops),
    STATION(2, R.string.agency_stop_type_station, R.string.agency_stop_type_stations),
    TRAIN_STATION(3, R.string.agency_stop_type_train_station, R.string.agency_stop_type_train_stations),

    TERMINAL(4, R.string.agency_stop_type_terminal, R.string.agency_stop_type_terminals),
    PORT(5, R.string.agency_stop_type_port, R.string.agency_stop_type_ports),

    // other
    PLACE(666, R.string.agency_stop_type_place, R.string.agency_stop_type_places),
    MODULE(999, R.string.agency_stop_type_module, R.string.agency_stop_type_modules), // agencies
    FAVORITE(777, R.string.agency_stop_type_favorite, R.string.agency_stop_type_favorites),
    NEWS_ARTICLE(888, R.string.agency_stop_type_news_article, R.string.agency_stop_type_news_articles),

}