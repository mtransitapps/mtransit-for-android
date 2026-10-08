package org.mtransit.android.ui.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.location.Address as AndroidAddress
import android.location.Geocoder as AndroidGeocoder
import android.location.Location as AndroidLocation

class GeocoderManager(
    @ApplicationContext private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : MTLog.Loggable {

    @Inject
    constructor(
        @ApplicationContext appContext: Context,
    ) : this(
        appContext = appContext,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG: String = GeocoderManager::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val geocoder: AndroidGeocoder by lazy {
        AndroidGeocoder(appContext, Locale.getDefault())
    }

    suspend fun getAddressesFromLocation(
        latitude: Double,
        longitude: Double,
        maxResults: Int = 1
    ): List<AndroidAddress> = withContext(ioDispatcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    maxResults,
                    object : AndroidGeocoder.GeocodeListener {
                        override fun onGeocode(addresses: List<AndroidAddress>) {
                            continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resumeWithException(IOException(errorMessage ?: "Unknown Geocoder error"))
                        }
                    }
                )
            }
        } else {
            @Suppress("DEPRECATION")
            @SuppressLint("DeprecatedCall")
            geocoder.getFromLocation(latitude, longitude, maxResults).orEmpty()
        }
    }

    @Suppress("unused")
    suspend fun getLocationFromAddressesName(
        locationName: String,
        maxResults: Int = 1
    ): List<AndroidAddress> = withContext(ioDispatcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    locationName,
                    maxResults,
                    object : AndroidGeocoder.GeocodeListener {
                        override fun onGeocode(addresses: List<AndroidAddress>) {
                            continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resumeWithException(IOException(errorMessage ?: "Unknown Geocoder error"))
                        }
                    }
                )
            }
        } else {
            @Suppress("DEPRECATION")
            @SuppressLint("DeprecatedCall")
            geocoder.getFromLocationName(locationName, maxResults).orEmpty()
        }
    }
}

suspend fun AndroidLocation.toAddressOrNull(geocoderManager: GeocoderManager): AndroidAddress? {
    return try {
        geocoderManager.getAddressesFromLocation(this.latitude, this.longitude, maxResults = 1).firstOrNull()
    } catch (ioe: IOException) {
        if (MTLog.isLoggable(android.util.Log.DEBUG)) {
            MTLog.w(GeocoderManager, ioe, "getLocationAddress() > Can't find the address of location $latitude, $longitude !")
        } else {
            MTLog.w(GeocoderManager, "getLocationAddress() > Can't find the address of location $latitude, $longitude !")
        }
        null
    }
}
