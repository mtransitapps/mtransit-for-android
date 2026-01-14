package org.mtransit.android.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.ResourceUtils

@Suppress("unused")
object MapUtils : Loggable {

    private val LOG_TAG: String = MapUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    const val MAP_ZOOM_LEVEL_FARTHEST = 0f
    const val MAP_ZOOM_LEVEL_WORLD = 1f
    const val MAP_ZOOM_LEVEL_CONTINENT = 5f
    const val MAP_ZOOM_LEVEL_CITY = 10f
    const val MAP_ZOOM_LEVEL_STREETS = 15f
    const val MAP_ZOOM_LEVEL_STREETS_BUSY = 16f
    const val MAP_ZOOM_LEVEL_STREETS_BUSY_BUSY = 17f
    const val MAP_ZOOM_LEVEL_CLOSEST = Float.MAX_VALUE // 20 or 21?

    const val MAP_MARKER_ALPHA_DEFAULT = 1.0f
    const val MAP_MARKER_ROTATION_DEFAULT = 0.0f

    const val MAP_MARKER_Z_INDEX_HIGHEST = 1.0f // drawn on top
    const val MAP_MARKER_Z_INDEX_PRIMARY = MAP_MARKER_Z_INDEX_HIGHEST
    const val MAP_MARKER_Z_INDEX_SECONDARY = 0.9f
    const val MAP_MARKER_Z_INDEX_TERTIARY = 0.8f
    const val MAP_MARKER_Z_INDEX_QUATERNARY = 0.7f
    const val MAP_MARKER_Z_INDEX_QUINARY = 0.6f
    const val MAP_MARKER_Z_INDEX_LOWEST = 0.0f
    const val MAP_MARKER_Z_INDEX_DEFAULT = MAP_MARKER_Z_INDEX_LOWEST

    const val MAP_TYPE_NORMAL: Int = GoogleMap.MAP_TYPE_NORMAL
    const val MAP_TYPE_SATELLITE: Int = GoogleMap.MAP_TYPE_HYBRID

    const val PREFS_LCL_MAP_TYPE: String = "pMapType"
    const val PREFS_LCL_MAP_TYPE_DEFAULT: Int = MAP_TYPE_NORMAL

    const val DEFAULT_MARKET_COLOR: Int = Color.WHITE

    // https://developers.google.com/maps/documentation/urls/get-started#directions-action
    // https://developers.google.com/maps/documentation/urls/android-intents
    // ex: https://maps.google.com/maps?saddr=Montreal,+Quebec&daddr=Toronto,+Ontario&dirflg=r
    private const val MAP_DIRECTION_URL_PART_1 = "https://maps.google.com/maps"
    private const val MAP_DIRECTION_URL_SOURCE_ADDRESS_PARAM = "saddr"
    private const val MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM = "daddr"
    private const val MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM = "dirflg"
    private const val MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE = "r"

    @JvmStatic
    fun showDirection(
        view: View? = null,
        activity: Activity,
        optDestLat: Double?, optDestLng: Double?,
        optSrcLat: Double?, optSrcLng: Double?,
        optQuery: String?,
    ) {
        val useInternalWebBrowser = !SystemSettingManager.isUsingFirebaseTestLab(activity) && PreferenceUtils.getPrefDefault(
            activity,
            PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER,
            PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT
        )
        val gmmIntentUri = getMapsDirectionUrl(optDestLat, optDestLng, optSrcLat, optSrcLng, optQuery)
        if (useInternalWebBrowser) {
            LinkUtils.open(
                view,
                activity,
                gmmIntentUri.toString(),
                activity.getString(R.string.google_maps),
                true
            )
            return
        }
        startMapIntent(activity, gmmIntentUri)
    }

    @JvmStatic
    fun getMapsDirectionUrl(
        optDestLat: Double?, optDestLng: Double?,
        optSrcLat: Double?, optSrcLng: Double?,
        @Suppress("unused") optQuery: String?
    ): Uri = MAP_DIRECTION_URL_PART_1.toUri().buildUpon().apply {
        if (optSrcLat != null && optSrcLng != null) {
            appendQueryParameter(MAP_DIRECTION_URL_SOURCE_ADDRESS_PARAM, "$optSrcLat,$optSrcLng")
        }
        if (optDestLat != null && optDestLng != null) {
            appendQueryParameter(MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM, "$optDestLat,$optDestLng")
        }
        appendQueryParameter(MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM, MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE)
    }.build()

    private const val GOOGLE_MAPS_PKG = "com.google.android.apps.maps"
    private const val GOOGLE_MAPS_LITE_PKG = "com.google.android.apps.mapslite"

    private fun startMapIntent(activity: Activity, gmmIntentUri: Uri?) {
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage(GOOGLE_MAPS_PKG)
        var label = activity.getString(R.string.google_maps)
        if (mapIntent.resolveActivity(activity.packageManager) == null) { // works API Level 30+ because added to AndroidManifest.xml
            mapIntent.setPackage(GOOGLE_MAPS_LITE_PKG) // try with Maps Lite
        }
        if (mapIntent.resolveActivity(activity.packageManager) == null) { // works API Level 30+ because added to AndroidManifest.xml
            mapIntent.setPackage(null) // clear Google Maps targeting
            label = activity.getString(R.string.map)
        }
        LinkUtils.open(activity, mapIntent, label, false)
    }

    private const val MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP = 64

    private var mapWithButtonsCameraPaddingInPx: Int? = null

    @JvmStatic
    fun getMapWithButtonsCameraPaddingInPx(context: Context?): Int {
        return mapWithButtonsCameraPaddingInPx
            ?: ResourceUtils.convertSPtoPX(context, MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP).toInt()
                .also { mapWithButtonsCameraPaddingInPx = it }
    }

    private const val MAP_WITHOUT_BUTTONS_CAMERA_PADDING_IN_SP = 32

    private var mapWithoutButtonsCameraPaddingInPx: Int? = null

    @JvmStatic
    fun getMapWithoutButtonsCameraPaddingInPx(context: Context?): Int {
        return mapWithoutButtonsCameraPaddingInPx
            ?: ResourceUtils.convertSPtoPX(context, MAP_WITHOUT_BUTTONS_CAMERA_PADDING_IN_SP).toInt()
                .also { mapWithoutButtonsCameraPaddingInPx = it }
    }

    fun fixScreenFlickering(viewGroup: ViewGroup?) {
        if (true) return // DISABLED (testing...)
        // https://issuetracker.google.com/issues/35822212
        viewGroup?.requestTransparentRegion(viewGroup)
    }

    @JvmStatic
    fun resetColorCache() {
        cache.evictAll()
    }

    private val cache = LruCache<Pair<Int, Int>?, BitmapDescriptor?>(128)

    @JvmStatic
    fun getIcon(context: Context?, @DrawableRes iconResId: Int, @ColorInt color: Int, replaceColor: Boolean): BitmapDescriptor? {
        var color = color
        val key = iconResId to color
        color = ColorUtils.adaptColorToTheme(context, color)
        val cachedBitmap: BitmapDescriptor? = cache.get(key)
        if (cachedBitmap != null) {
            return cachedBitmap
        }
        val newBase = ColorUtils.colorizeBitmapResource(context, color, iconResId, replaceColor) ?: return null
        val newBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(newBase)
        cache.put(key, newBitmapDescriptor)
        return newBitmapDescriptor
    }
}
