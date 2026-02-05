package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLng

val IMarker?.uuid: String? get() = this?.getData<MTPOIMarkerIds>()?.entrySet()?.firstOrNull()?.key

fun IMarker.updateAlpha(alpha: Float) {
    if (alpha != getAlpha()) {
        setAlpha(alpha)
    }
}

fun IMarker.updateRotation(rotation: Float) {
    if (rotation != getRotation()) {
        setRotation(rotation)
    }
}

fun IMarker.updatePosition(position: LatLng, animate: Boolean = false) {
    if (position != getPosition()) {
        if (animate) animatePosition(position) else setPosition(position)
    }
}

fun IMarker.updateTitle(title: String?) {
    if (title != getTitle()) {
        setTitle(title)
    }
}

fun IMarker.updateSnippet(snippet: String?) {
    if (snippet != getSnippet()) {
        setSnippet(snippet)
    }
}

fun IMarker.updateData(data: Any?) {
    if (data != getData()) {
        setData(data)
    }
}

fun IMarker.updateZIndex(zIndex: Float) {
    if (zIndex != getZIndex()) {
        setZIndex(zIndex)
    }
}

fun IMarker.updateFlat(flat: Boolean) {
    if (flat != isFlat()) {
        setFlat(flat)
    }
}
