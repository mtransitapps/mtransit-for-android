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
import org.mtransit.android.provider.location.MTLocationProvider
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.WebBrowserFragment
import org.mtransit.android.ui.view.common.navigateF
import org.mtransit.android.user.UserPrefManager
import org.mtransit.android.util.KeysManager
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.SystemSettingManager
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject
import javax.inject.Singleton
import android.location.Location as AndroidLocation

@Singleton
class MapDirectionsProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val userPrefManager: UserPrefManager,
    private val locationProvider: MTLocationProvider,
) {

    companion object {
        // https://developers.google.com/maps/documentation/urls/get-started#directions-action
        // https://developers.google.com/maps/documentation/urls/android-intents
        // ex: https://maps.google.com/maps?saddr=Montreal,+Quebec&daddr=Toronto,+Ontario&dirflg=r
        // private const val MAP_DIRECTION_URL_PART_1 = "https://maps.google.com/maps"
        private const val MAP_DIRECTION_URL_PART_1 = "https://www.google.com/maps/dir/?api=1"

        // https://developers.google.com/maps/documentation/embed/quickstart
        private const val EMBED_API_MAP_DIRECTION_URL_PART_1 = "https://www.google.com/maps/embed/v1/directions"

        private const val EMBED_API_MAP_DIRECTION_URL_KEY_PARAM = "key"

        @Suppress("SpellCheckingInspection")
        // private const val MAP_DIRECTION_URL_SOURCE_ADDRESS_PARAM = "saddr"
        private const val MAP_DIRECTION_URL_ORIGIN_ADDRESS_PARAM = "origin"

        @Suppress("SpellCheckingInspection")
        // private const val MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM = "daddr"
        private const val MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM = "destination"

        @Suppress("SpellCheckingInspection")
        // private const val MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM = "dirflg"
        private const val MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM = "travelmode"
        private const val EMBED_API_MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM = "mode"

        // private const val MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE = "r"
        private const val MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE = "transit"

        private const val GOOGLE_MAPS_PKG = "com.google.android.apps.maps"

        @Suppress("SpellCheckingInspection")
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

    fun getMapsDirectionUrl(
        destinationLat: Double?,
        destinationLng: Double?,
        originLat: Double?,
        originLng: Double?,
        @Suppress("unused") query: String?
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
            EMBED_API_MAP_DIRECTION_URL_PART_1.toUri().buildUpon().apply {
                appendQueryParameter(EMBED_API_MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM, MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE)
                appendQueryParameter(MAP_DIRECTION_URL_ORIGIN_ADDRESS_PARAM, origin)
                appendQueryParameter(MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM, destination)
                appendQueryParameter(EMBED_API_MAP_DIRECTION_URL_KEY_PARAM, embedApiKey) // last
            }.build()
        } else {
            MAP_DIRECTION_URL_PART_1.toUri().buildUpon().apply {
                appendQueryParameter(MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM, MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE)
                origin?.let { appendQueryParameter(MAP_DIRECTION_URL_ORIGIN_ADDRESS_PARAM, it) }
                destination?.let { appendQueryParameter(MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM, it) }
            }.build()
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun getUseInternalWebBrowserPrefNow(): Boolean = userPrefManager.getUseInternalWebBrowserNow()

    fun showDirection(
        binding: ViewBinding? = null,
        activity: Activity,
        destinationLat: Double? = null,
        destinationLng: Double? = null,
        originLat: Double? = null,
        originLng: Double? = null,
        optQuery: String? = null,
        useInternalWebBrowserPref: Boolean? = getUseInternalWebBrowserPrefNow(),
    ) {
        val useInternalWebBrowser = !SystemSettingManager.isUsingFirebaseTestLab(appContext)
            && (useInternalWebBrowserPref ?: DefaultPreferenceRepository.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT)
        val gmmIntentUri = getMapsDirectionUrl(destinationLat, destinationLng, originLat, originLng, optQuery)
        val view = binding?.root
        val url = gmmIntentUri.toString()
        val titleStatic = activity.getString(R.string.google_maps)
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
