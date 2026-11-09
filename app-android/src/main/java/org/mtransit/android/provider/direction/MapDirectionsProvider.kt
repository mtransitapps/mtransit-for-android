package org.mtransit.android.provider.direction

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.core.net.toUri
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.device.DevicePrefManager
import org.mtransit.android.provider.location.CountryProvider
import org.mtransit.android.provider.location.MTLocationProvider
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.WebBrowserFragment
import org.mtransit.android.ui.view.common.navigateF
import org.mtransit.android.ui.view.map.MapConstants
import org.mtransit.android.user.UserPrefManager
import org.mtransit.android.util.KeysManager
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.SystemSettingManager
import org.mtransit.commons.FeatureFlags
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import android.location.Location as AndroidLocation

@Singleton
class MapDirectionsProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val userPrefManager: UserPrefManager,
    private val devicePrefManager: DevicePrefManager,
    private val locationProvider: MTLocationProvider,
    private val countryProvider: CountryProvider,
) {

    companion object {

        // region Maps URL
        // https://developers.google.com/maps/documentation/urls/get-started#directions-action
        // ex: https://maps.google.com/maps?saddr=Montreal,+Quebec&daddr=Toronto,+Ontario&dirflg=r
        // private const val MAPS_URL_DIRECTIONS_PART_1 = "https://maps.google.com/maps"
        private const val MAPS_URL_DIRECTIONS = "https://www.google.com/maps/dir/?api=1"

        private const val MAPS_URL_DIRECTIONS_P_LANG = "hl"

        // private const val MAPS_URL_DIRECTIONS_P_ORIGIN = "saddr"
        private const val MAPS_URL_DIRECTIONS_P_ORIGIN = "origin"

        // private const val MAPS_URL_DIRECTIONS_P_DESTINATION = "daddr"
        private const val MAPS_URL_DIRECTIONS_P_DESTINATION = "destination"

        @Suppress("SpellCheckingInspection")
        // private const val MAPS_URL_DIRECTIONS_P_MODE = "dirflg"
        private const val MAPS_URL_DIRECTIONS_P_MODE = "travelmode"

        // private const val MAPS_URL_DIRECTIONS_P_MODE_V_TRANSIT = "r"
        private const val MAPS_URL_DIRECTIONS_P_MODE_V_TRANSIT = "transit"

        // endregion

        // region Maps Embed API

        // https://developers.google.com/maps/documentation/embed/quickstart
        // https://developers.google.com/maps/documentation/embed/embedding-map
        private const val MAPS_EMBED_API_DIRECTIONS_URL = "https://www.google.com/maps/embed/v1/directions"

        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_ORIGIN = "origin"

        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_DESTINATION = "destination"

        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_KEY = "key"

        @Suppress("SpellCheckingInspection")
        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_TYPE = "maptype"
        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_TYPE_V_SATELLITE = "satellite"

        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_UNITS = "units"
        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_UNITS_V_METRIC = "metric"
        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_UNITS_V_IMPERIAL = "imperial"

        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_LANG = "language" // 2-letters language code like "en"

        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_REGION = "region" // 2-letters country code like "CA"

        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_MODE = "mode"
        private const val MAPS_EMBED_API_DIRECTIONS_URL_P_MODE_V_TRANSIT = "transit"

        // endregion

        private const val GOOGLE_MAPS_PKG = "com.google.android.apps.maps"
        private const val GOOGLE_MAPS_LITE_PKG = "com.google.android.apps.mapslite"
    }

    private val lastDeviceLocation: AndroidLocation? get() = locationProvider.lastLocationOrNull

    private val useEmbedApi: Boolean
        get() = remoteConfigProvider.get(
            RemoteConfigProvider.MAPS_DIRECTION_USE_EMBED_API, RemoteConfigProvider.MAPS_DIRECTION_USE_EMBED_API_DEFAULT
        )

    private val embedApiKey by lazy {
        KeysManager.getKey(appContext, KeysIds.GOOGLE_MAPS_EMBED_API_KEY)?.takeIf { it.isNotBlank() }
    }

    private val usingEmbedApi get() = embedApiKey != null && useEmbedApi

    @SuppressLint("DiscouragedApi")
    private fun getMapTypePrefNow() = devicePrefManager.getMapTypeNow()

    @SuppressLint("DiscouragedApi")
    private fun getDistanceUnitPrefNow() = userPrefManager.getDistanceUnitNow()

    fun getMapsDirectionUrl(
        destinationLat: Double?,
        destinationLng: Double?,
        originLat: Double?,
        originLng: Double?,
        @Suppress("unused") query: String?,
        mapTypePref: Int? = getMapTypePrefNow(),
        distanceUnitsPref: String? = getDistanceUnitPrefNow(),
    ): Uri {
        val origin = if (originLat != null && originLng != null) {
            "$originLat,$originLng"
        } else {
            this.lastDeviceLocation?.let { "${it.latitude},${it.longitude}" }
        }
        val destination = if (destinationLat != null && destinationLng != null) "$destinationLat,$destinationLng" else null
        return if (
            origin != null && destination != null // origin & destination are required for Maps Embed API
            && usingEmbedApi // FF: ON & valid embed API key available
        ) {
            getMapsEmbedApiDirectionUrl(origin, destination, mapTypePref, distanceUnitsPref)
        } else {
            getMapsDirectionUrl(origin, destination)
        }
    }

    private fun getMapsDirectionUrl(origin: String?, destination: String?) = MAPS_URL_DIRECTIONS.toUri().buildUpon().apply {
        appendQueryParameter(MAPS_URL_DIRECTIONS_P_MODE, MAPS_URL_DIRECTIONS_P_MODE_V_TRANSIT)
        origin?.let { appendQueryParameter(MAPS_URL_DIRECTIONS_P_ORIGIN, it) }
        destination?.let { appendQueryParameter(MAPS_URL_DIRECTIONS_P_DESTINATION, it) }
        val appLang = LocaleUtils.getDefaultLocale().language // always override to be consistent with rest of the app UI
        if (LocaleUtils.isFR(appLang)) {
            appendQueryParameter(MAPS_URL_DIRECTIONS_P_LANG, appLang)
        } else { // ELSE default app UI language = English
            appendQueryParameter(MAPS_URL_DIRECTIONS_P_LANG, Locale.ENGLISH.language)
        }
    }.build()

    private fun getMapsEmbedApiDirectionUrl(
        origin: String,
        destination: String,
        mapTypePref: Int?,
        distanceUnitsPref: String?
    ) =
        MAPS_EMBED_API_DIRECTIONS_URL.toUri().buildUpon().apply {
            appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_MODE, MAPS_EMBED_API_DIRECTIONS_URL_P_MODE_V_TRANSIT)
            appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_ORIGIN, origin)
            appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_DESTINATION, destination)
            if (mapTypePref == MapConstants.MAP_TYPE_SATELLITE) {
                appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_TYPE, MAPS_EMBED_API_DIRECTIONS_URL_P_TYPE_V_SATELLITE)
            }
            distanceUnitsPref?.let {
                if (it == DefaultPreferenceRepository.PREFS_DISTANCE_UNITS_METRIC) {
                    appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_UNITS, MAPS_EMBED_API_DIRECTIONS_URL_P_UNITS_V_METRIC)
                } else if (it == DefaultPreferenceRepository.PREFS_DISTANCE_UNITS_IMPERIAL) {
                    appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_UNITS, MAPS_EMBED_API_DIRECTIONS_URL_P_UNITS_V_IMPERIAL)
                }
            }
            val appLang = LocaleUtils.getDefaultLocale().language // always override to be consistent with rest of the app UI
            if (LocaleUtils.isFR(appLang)) {
                appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_LANG, appLang)
            } else { // ELSE default app UI language = English
                appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_LANG, Locale.ENGLISH.language)
            }
            countryProvider.getCurrentCountryCode()?.let { countryCode ->
                appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_REGION, countryCode)
            }
            appendQueryParameter(MAPS_EMBED_API_DIRECTIONS_URL_P_KEY, embedApiKey) // last
        }.build()

    @SuppressLint("DiscouragedApi")
    private fun getUseInternalWebBrowserPrefNow() = userPrefManager.getUseInternalWebBrowserNow()

    fun showDirection(
        binding: ViewBinding? = null,
        activity: Activity,
        destinationLat: Double? = null,
        destinationLng: Double? = null,
        originLat: Double? = null,
        originLng: Double? = null,
        optQuery: String? = null,
        useInternalWebBrowserPref: Boolean? = getUseInternalWebBrowserPrefNow(),
        mapTypePref: Int? = getMapTypePrefNow(),
        distanceUnitsPref: String? = getDistanceUnitPrefNow(),
    ) {
        val useInternalWebBrowser = !SystemSettingManager.isUsingFirebaseTestLab(appContext)
            && (useInternalWebBrowserPref ?: DefaultPreferenceRepository.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT)
        val gmmIntentUri = getMapsDirectionUrl(
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            originLat = originLat,
            originLng = originLng,
            query = optQuery,
            mapTypePref = mapTypePref,
            distanceUnitsPref = distanceUnitsPref,
        )
        val view = binding?.root
        val url = gmmIntentUri.toString()
        val titleStatic = appContext.getString(R.string.trip_planner)
        if (useInternalWebBrowser) {
            if (usingEmbedApi) { // need to use <iframe />
                showEmbedDirections(view, url, titleStatic, activity)
                return
            }
            LinkUtils.open(
                view,
                activity,
                url,
                titleStatic,
                titleStatic,
                true,
                true,
            )
            return
        }
        startMapIntent(activity, gmmIntentUri)
    }

    private fun showEmbedDirections(view: View?, url: String, titleStatic: String, activity: Activity) {
        @Suppress("SimplifyBooleanWithConstants")
        if (FeatureFlags.F_NAVIGATION && view != null) {
            val navController = findNavController(view)
            var extras: FragmentNavigator.Extras? = null
            if (FeatureFlags.F_TRANSITION) {
                extras = FragmentNavigator.Extras.Builder() // TODO ? .addSharedElement(view, view.getTransitionName())
                    .build()
            }
            navController
                .navigateF(
                    R.id.nav_to_web_screen,
                    WebBrowserFragment.newInstanceArgs(url, titleStatic, true),
                    null,
                    extras
                )
        } else {
            if (activity is MainActivity) {
                activity.addFragmentToStack(
                    WebBrowserFragment.newInstance(url, titleStatic, true)
                )
            }
        }
    }

    //  https://developers.google.com/maps/documentation/urls/android-intents
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
}
