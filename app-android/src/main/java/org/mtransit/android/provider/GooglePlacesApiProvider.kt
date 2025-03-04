package org.mtransit.android.provider

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.util.KeysManager

object GooglePlacesApiProvider {

    private fun makePlacesClient(context: Context): PlacesClient? {
        val appContext = context.applicationContext
        val apiKey = KeysManager.getKey(appContext, KeysIds.GOOGLE_PLACES_NEW_API_KEY)
        if (apiKey.isNullOrBlank()) return null

        Places.initializeWithNewPlacesApiEnabled(appContext, apiKey)

        return Places.createClient(appContext)
    }

    private var _placesClient: PlacesClient? = null

    @JvmStatic
    fun getPlacesClient(context: Context): PlacesClient? {
        return _placesClient ?: makePlacesClient(context)?.also { _placesClient = it }
    }
}