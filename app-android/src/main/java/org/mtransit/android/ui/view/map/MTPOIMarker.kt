package org.mtransit.android.ui.view.map

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.data.POIManager
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.ui.view.map.MTMapIconsProvider.defaultIconDef
import org.mtransit.android.util.MapUtils
import org.mtransit.commons.CollectionUtils
import java.util.Locale

data class MTPOIMarker(
    var position: LatLng,
    val names: MutableList<String>,
    val agencies: MutableList<String>,
    val extras: MutableList<String>,
    var iconDef: MTMapIconDef,
    @param:ColorInt var color: Int?,
    @param:ColorInt var secondaryColor: Int?,
    var alpha: Float?,
    var rotation: Float?,
    var zIndex: Float?,
    val uuidsAndAuthority: MTPOIMarkerIds,
) : MTLog.Loggable {

    fun getIdentifier(): String {
        return position.toString()
    }

    companion object {
        private val LOG_TAG: String = MTPOIMarker::class.java.simpleName

        private const val SLASH = " / "

        private const val P1 = "("
        private const val P2 = ")"

        private const val AROUND_TRUNC = "%.6g"

        private fun truncAround(loc: Double) = String.format(Locale.US, AROUND_TRUNC, loc).toDouble()

        @JvmStatic
        fun getLatLngTrunc(poim: POIManager) = getLatLngTrunc(poim.poi.getLat(), poim.poi.getLng())

        fun getLatLngTrunc(lat: Double, lng: Double) = LatLng(truncAround(lat), truncAround(lng))

        @JvmStatic
        fun MTPOIMarker.toExtendedMarkerOptions(
            context: Context,
            markerLabelShowExtra: Boolean,
            currentZoomGroup: MTMapIconZoomGroup?,
        ) = ExtendedMarkerOptions().apply {
            position(this@toExtendedMarkerOptions.position)
            title(this@toExtendedMarkerOptions.title)
            snippet(if (markerLabelShowExtra) this@toExtendedMarkerOptions.snippet else null)
            val iconDef = this@toExtendedMarkerOptions.iconDef
            anchor(iconDef.anchorU, iconDef.anchorV)
            infoWindowAnchor(iconDef.infoWindowAnchorU, iconDef.infoWindowAnchorV)
            flat(iconDef.flat)
            icon(
                context,
                iconDef.getZoomResId(currentZoomGroup),
                iconDef.getZoomSize(currentZoomGroup),
                iconDef.replaceColor,
                this@toExtendedMarkerOptions.color,
                this@toExtendedMarkerOptions.secondaryColor,
                Color.BLACK
            )
            alpha(this@toExtendedMarkerOptions.alphaOrDefault)
            rotation(this@toExtendedMarkerOptions.rotationOrDefault)
            zIndex(this@toExtendedMarkerOptions.zIndexOrDefault)
            data(this@toExtendedMarkerOptions.uuidsAndAuthority)
        }

        @JvmStatic
        fun MTPOIMarker.updateMarker(
            marker: IMarker,
            context: Context,
            markerLabelShowExtra: Boolean,
            currentZoomGroup: MTMapIconZoomGroup?,
        ) = marker.apply {
            updatePosition(this@updateMarker.position, animate = true)
            updateRotation(this@updateMarker.rotationOrDefault)
            setIcon(context, iconDef.getZoomResId(currentZoomGroup), iconDef.getZoomSize(currentZoomGroup), iconDef.replaceColor, color, secondaryColor, Color.BLACK)
            setAnchor(iconDef.anchorU, iconDef.anchorV)
            setInfoWindowAnchor(iconDef.infoWindowAnchorU, iconDef.infoWindowAnchorV)
            updateFlat(iconDef.flat)
            updateTitle(this@updateMarker.title)
            updateSnippet(if (markerLabelShowExtra) this@updateMarker.snippet else null)
            updateAlpha(this@updateMarker.alphaOrDefault)
            updateZIndex(this@updateMarker.zIndexOrDefault)
            updateData(this@updateMarker.uuidsAndAuthority)
        }

    }

    override fun getLogTag() = LOG_TAG

    constructor(
        position: LatLng,
        name: String,
        agency: String?,
        extra: String?,
        iconDef: MTMapIconDef,
        @ColorInt color: Int?,
        @ColorInt secondaryColor: Int?,
        alpha: Float?,
        rotation: Float?,
        zIndex: Float?,
        uuid: String,
        authority: String,
    ) : this(
        position,
        name.takeIf { it.isNotBlank() }?.let { mutableListOf(name) } ?: mutableListOf(),
        agency?.takeIf { it.isNotBlank() }?.let { mutableListOf(agency) } ?: mutableListOf(),
        extra?.takeIf { it.isNotBlank() }?.let { mutableListOf(extra) } ?: mutableListOf(),
        iconDef,
        color,
        secondaryColor,
        alpha,
        rotation,
        zIndex,
        MTPOIMarkerIds.from(uuid, authority),
    )

    val title: String
        get() = buildString {
            CollectionUtils.sort(this@MTPOIMarker.names, MapViewController.MARKER_NAME_COMPARATOR)
            val addedNames = mutableSetOf<String>()
            this@MTPOIMarker.names.forEach { name ->
                if (addedNames.contains(name)) return@forEach
                if (isNotEmpty()) append(SLASH)
                append(name)
                addedNames.add(name)
            }
        }

    val snippet: String
        get() = buildString {
            var hasExtras = false
            CollectionUtils.sort(this@MTPOIMarker.extras, MapViewController.MARKER_NAME_COMPARATOR)
            val addedExtras = mutableSetOf<String>()
            this@MTPOIMarker.extras.forEach { extra ->
                if (addedExtras.contains(extra)) return@forEach
                if (hasExtras) {
                    append(SLASH)
                }
                append(extra)
                hasExtras = true
                addedExtras.add(extra)
            }
            var hasAgencies = false
            CollectionUtils.sort(this@MTPOIMarker.agencies, MapViewController.MARKER_NAME_COMPARATOR)
            val addedAgencies = mutableSetOf<String>()
            this@MTPOIMarker.agencies.forEach { agency ->
                if (addedAgencies.contains(agency)) return@forEach
                if (hasAgencies) {
                    append(SLASH)
                } else if (hasExtras) {
                    append(StringUtils.SPACE_CAR).append(P1)
                }
                append(agency)
                hasAgencies = true
                addedAgencies.add(agency)
            }
            if (hasExtras && hasAgencies) {
                append(P2)
            }
        }

    val alphaOrDefault get() = alpha ?: MapUtils.MAP_MARKER_ALPHA_DEFAULT

    val rotationOrDefault get() = rotation ?: MapUtils.MAP_MARKER_ROTATION_DEFAULT

    val zIndexOrDefault get() = zIndex ?: MapUtils.MAP_MARKER_Z_INDEX_DEFAULT

    fun hasUUID(uuid: String?) = this.uuidsAndAuthority.hasUUID(uuid)

    fun merge(poiMarker: MTPOIMarker) {
        merge(
            position = poiMarker.position,
            names = poiMarker.names,
            agencies = poiMarker.agencies,
            extras = poiMarker.extras,
            iconDef = poiMarker.iconDef,
            color = poiMarker.color,
            secondaryColor = poiMarker.secondaryColor,
            alpha = poiMarker.alpha,
            rotation = poiMarker.rotation,
            zIndex = poiMarker.zIndex,
            uuidsAndAuthority = poiMarker.uuidsAndAuthority
        )
    }

    fun merge(
        position: LatLng,
        name: String?,
        agency: String?,
        extra: String?,
        iconDef: MTMapIconDef,
        @ColorInt color: Int?,
        @ColorInt secondaryColor: Int?,
        alpha: Float?,
        rotation: Float?,
        zIndex: Float?,
        uuid: String,
        authority: String
    ) {
        merge(
            position = position,
            names = mutableListOf(name),
            agencies = mutableListOf(agency),
            extras = mutableListOf(extra),
            iconDef = iconDef,
            color = color,
            secondaryColor = secondaryColor,
            alpha = alpha,
            rotation = rotation,
            zIndex = zIndex,
            uuidsAndAuthority = MTPOIMarkerIds.from(uuid, authority)
        )
    }

    fun merge(
        position: LatLng,
        names: Collection<String?>,
        agencies: Collection<String?>,
        extras: Collection<String?>,
        iconDef: MTMapIconDef,
        @ColorInt color: Int?,
        @ColorInt secondaryColor: Int?,
        alpha: Float?,
        rotation: Float?,
        zIndex: Float?,
        uuidsAndAuthority: MTPOIMarkerIds
    ) {
        addPosition(position)
        names.forEach { name ->
            name?.takeIf { it.isNotBlank() } ?: return@forEach
            if (this.names.contains(name)) return@forEach
            this.names.add(name)
        }
        agencies.forEach { agency ->
            agency?.takeIf { it.isNotBlank() } ?: return@forEach
            if (this.agencies.contains(agency)) return@forEach
            this.agencies.add(agency)
        }
        extras.forEach { extra ->
            extra?.takeIf { it.isNotBlank() } ?: return@forEach
            if (this.extras.contains(extra)) return@forEach
            this.extras.add(extra)
        }
        if (this.iconDef != iconDef) {
            this.iconDef = defaultIconDef
        }
        this.color?.let {
            if (this.color != color) {
                this.color = null
            }
        }
        this.secondaryColor?.let {
            if (this.secondaryColor != secondaryColor) {
                this.secondaryColor = null
            }
        }
        val thisAlpha = this.alpha ?: MapUtils.MAP_MARKER_ALPHA_DEFAULT
        val otherAlpha = alpha ?: MapUtils.MAP_MARKER_ALPHA_DEFAULT
        if (thisAlpha != otherAlpha) {
            this.alpha = (thisAlpha + otherAlpha) / 2.0f
        }
        val thisRotation = this.rotation ?: MapUtils.MAP_MARKER_ROTATION_DEFAULT
        val otherRotation = rotation ?: MapUtils.MAP_MARKER_ROTATION_DEFAULT
        if (thisRotation != otherRotation) {
            this.rotation = (thisRotation + otherRotation) / 2.0f
        }
        val thisIndex = this.zIndex ?: MapUtils.MAP_MARKER_Z_INDEX_DEFAULT
        val otherIndex = zIndex ?: MapUtils.MAP_MARKER_Z_INDEX_DEFAULT
        if (thisIndex != otherIndex) {
            this.zIndex = (thisIndex + otherIndex) / 2.0f
        }
        this.uuidsAndAuthority.merge(uuidsAndAuthority)
    }

    private fun addPosition(position: LatLng) {
        this.position = LatLng(
            (this.position.latitude + position.latitude) / 2.0,
            (this.position.longitude + position.longitude) / 2.0
        )
    }
}
