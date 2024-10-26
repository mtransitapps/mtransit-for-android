package org.mtransit.android.util

import android.content.Context
import org.mtransit.android.R
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.MTLog

object KeysUtils {

    private val LOG_TAG: String = KeysUtils::class.java.simpleName

    @JvmStatic
    fun getKey(context: Context, key: String) = when (key) {
        KeysIds.TWITTER_BEARER_TOKEN -> context.resources.getStringArray(R.array.twitter_bearer_token).joinToString()
        KeysIds.GOOGLE_PLACES_API_KEY -> context.resources.getStringArray(R.array.google_places_api_key).joinToString()
        KeysIds.YOUTUBE_API_KEY -> context.resources.getStringArray(R.array.youtube_api_key).joinToString()
        KeysIds.CA_WINNIPEG_TRANSIT_API -> context.resources.getStringArray(R.array.ca_winnipeg_transit_api_key).joinToString()
        else -> {
            MTLog.w(LOG_TAG, "Unexpected '$key'!")
            null
        }
    }?.takeIf { it.isNotBlank() }
}