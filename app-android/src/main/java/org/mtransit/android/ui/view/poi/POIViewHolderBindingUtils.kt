package org.mtransit.android.ui.view.poi

import android.view.View

@JvmOverloads
fun initModuleViewHolder(view: View, uuid: String? = null) =
    ModuleViewHolder(view).apply {
        this.uuid = uuid
    }

@JvmOverloads
fun initTextMessageViewHolder(view: View, uuid: String? = null) =
    TextMessageViewHolder(view).apply {
        this.uuid = uuid
    }

@JvmOverloads
fun initPlaceViewHolder(view: View, uuid: String? = null) =
    PlaceViewHolder(view).apply {
        this.uuid = uuid
    }

@JvmOverloads
fun initBasicViewHolder(view: View, uuid: String? = null) =
    BasicPOIViewHolder(view).apply {
        this.uuid = uuid
    }

@JvmOverloads
fun initRDSViewHolder(view: View, uuid: String? = null) =
    RouteDirectionStopViewHolder(view).apply {
        this.uuid = uuid
    }
