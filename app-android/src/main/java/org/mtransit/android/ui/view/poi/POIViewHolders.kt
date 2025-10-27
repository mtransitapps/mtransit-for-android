package org.mtransit.android.ui.view.poi

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.mtransit.android.R
import org.mtransit.android.ui.view.MTCompassView
import org.mtransit.android.ui.view.MTJPathsView
import org.mtransit.android.ui.view.poi.serviceupdate.POIServiceUpdateViewHolder
import org.mtransit.android.ui.view.poi.status.POICommonStatusViewHolder

interface CommonViewHolder {
    val view: View
    val nameTv: TextView
    val favImg: ImageView
    val distanceTv: TextView?
    val compassV: MTCompassView?
    val locationTv: TextView?
    var uuid: String?
    var statusViewHolder: POICommonStatusViewHolder<*, *>?
    var serviceUpdateViewHolder: POIServiceUpdateViewHolder?
}

data class TextMessageViewHolder(
    override val view: View,
    override val nameTv: TextView = view.findViewById(R.id.name),
    override val favImg: ImageView = view.findViewById(R.id.fav),
    override val distanceTv: TextView? = view.findViewById(R.id.distance),
    override val compassV: MTCompassView? = view.findViewById(R.id.compass),
    override val locationTv: TextView? = view.findViewById(R.id.location),
) : CommonViewHolder {
    override var uuid: String? = null
    override var statusViewHolder: POICommonStatusViewHolder<*, *>? = null
    override var serviceUpdateViewHolder: POIServiceUpdateViewHolder? = null
}

data class PlaceViewHolder(
    override val view: View,
    override val nameTv: TextView = view.findViewById(R.id.name),
    override val favImg: ImageView = view.findViewById(R.id.fav),
    override val distanceTv: TextView? = view.findViewById(R.id.distance),
    override val compassV: MTCompassView? = view.findViewById(R.id.compass),
    override val locationTv: TextView? = view.findViewById(R.id.location),
    val placeIconImg: ImageView = view.findViewById(R.id.extra),
) : CommonViewHolder {
    override var uuid: String? = null
    override var statusViewHolder: POICommonStatusViewHolder<*, *>? = null
    override var serviceUpdateViewHolder: POIServiceUpdateViewHolder? = null
}

data class ModuleViewHolder(
    override val view: View,
    override val nameTv: TextView = view.findViewById(R.id.name),
    override val favImg: ImageView = view.findViewById(R.id.fav),
    override val distanceTv: TextView? = view.findViewById(R.id.distance),
    override val compassV: MTCompassView? = view.findViewById(R.id.compass),
    override val locationTv: TextView? = view.findViewById(R.id.location),
    val moduleExtraTypeImg: ImageView = view.findViewById(R.id.extra),
) : CommonViewHolder {
    override var uuid: String? = null
    override var statusViewHolder: POICommonStatusViewHolder<*, *>? = null
    override var serviceUpdateViewHolder: POIServiceUpdateViewHolder? = null
}

class RouteDirectionStopViewHolder(
    override val view: View,
    override val nameTv: TextView = view.findViewById(R.id.name),
    override val favImg: ImageView = view.findViewById(R.id.fav),
    override val distanceTv: TextView? = view.findViewById(R.id.distance),
    override val compassV: MTCompassView? = view.findViewById(R.id.compass),
    override val locationTv: TextView? = view.findViewById(R.id.location),
    val noExtra: View = view.findViewById(R.id.no_extra),
    val rdsExtraV: View = view.findViewById(R.id.extra),
    val routeFL: View = view.findViewById(R.id.route),
    val routeShortNameTv: TextView = view.findViewById(R.id.route_short_name),
    val routeTypeImg: MTJPathsView = view.findViewById(R.id.route_type_img),
    val directionHeadingTv: TextView = view.findViewById(R.id.direction_heading),
    val directionHeadingBg: View = view.findViewById(R.id.direction_heading_bg),
) : CommonViewHolder {
    override var uuid: String? = null
    override var statusViewHolder: POICommonStatusViewHolder<*, *>? = null
    override var serviceUpdateViewHolder: POIServiceUpdateViewHolder? = null
}

data class BasicPOIViewHolder(
    override val view: View,
    override val nameTv: TextView = view.findViewById(R.id.name),
    override val favImg: ImageView = view.findViewById(R.id.fav),
    override val distanceTv: TextView? = view.findViewById(R.id.distance),
    override val compassV: MTCompassView? = view.findViewById(R.id.compass),
    override val locationTv: TextView? = view.findViewById(R.id.location),
) : CommonViewHolder {
    override var uuid: String? = null
    override var statusViewHolder: POICommonStatusViewHolder<*, *>? = null
    override var serviceUpdateViewHolder: POIServiceUpdateViewHolder? = null
}
