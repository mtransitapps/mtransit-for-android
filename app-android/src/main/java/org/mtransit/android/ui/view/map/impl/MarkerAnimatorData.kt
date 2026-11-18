package org.mtransit.android.ui.view.map.impl

import android.view.animation.Interpolator
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.ui.view.map.IMarker

data class MarkerAnimatorData(
    val from: LatLng,
    val to: LatLng,
    val start: Long,
    val duration: Long,
    val interpolator: Interpolator,
    val callback: IMarker.AnimationCallback?
)
