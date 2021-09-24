package org.mtransit.android.data

fun Iterable<POIManager>.toStringUUID(): String {
    return this.joinToString {
        it.poi.uuid
    }
}