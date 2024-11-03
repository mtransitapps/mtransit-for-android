package org.mtransit.android.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils.EMPTY
import org.mtransit.android.data.ITargetedProviderProperties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeysManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    companion object {

        private val LOG_TAG: String = KeysManager::class.java.simpleName

        private val debugS = if (BuildConfig.DEBUG) ".debug" else ""

        @JvmStatic
        fun getKey(context: Context, key: String) = when (key) {
            KeysIds.TWITTER_BEARER_TOKEN -> context.resources.getStringArray(R.array.twitter_bearer_token).join()
            KeysIds.GOOGLE_PLACES_API_KEY -> context.resources.getStringArray(R.array.google_places_api_key).join()
            KeysIds.YOUTUBE_API_KEY -> context.resources.getStringArray(R.array.youtube_api_key).join()
            KeysIds.CA_WINNIPEG_TRANSIT_API -> context.resources.getStringArray(R.array.ca_winnipeg_transit_api_key).join()
            else -> {
                MTLog.w(LOG_TAG, "Unexpected '$key'!")
                null
            }
        }?.takeIf { it.isNotBlank() }

        fun getKeyEntry(context: Context, key: String) = getKey(context, key)?.let { key to it }

        @JvmStatic
        fun getKeysMap(context: Context, authority: String): Map<String, String>? = when {
            // MAIN
            authority.endsWith("$debugS.provider.place") -> getKeyEntry(context, KeysIds.GOOGLE_PLACES_API_KEY)?.let { mapOf(it) }
            // NEWS
            authority.endsWith("$debugS.news.twitter") -> getKeyEntry(context, KeysIds.TWITTER_BEARER_TOKEN)?.let { mapOf(it) }
            authority.endsWith("$debugS.news.youtube") -> getKeyEntry(context, KeysIds.YOUTUBE_API_KEY)?.let { mapOf(it) }
            // GTFS
            authority.endsWith("$debugS.gtfs.realtime") -> {
                when (authority) {
                    "org.mtransit.android.ca_gatineau_sto_bus$debugS.gtfs.realtime" -> {
                        mapOf(
                            KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_sto_ca).join(),
                            KeysIds.GTFS_REAL_TIME_URL_SECRET to context.resources.getStringArray(R.array.gtfs_real_time_url_secret_sto_ca).join()
                        )
                    }

                    "org.mtransit.android.ca_gtha_go_transit_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_gtha_go_transit_train$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_gta_up_express_train$debugS.gtfs.realtime" -> {
                        mapOf(KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_openmetrolinx_com).join())
                    }

                    "org.mtransit.android.ca_vancouver_translink_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_vancouver_translink_ferry2$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_vancouver_translink_train$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_west_coast_express_train$debugS.gtfs.realtime" -> {
                        mapOf(KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_translink_ca).join())
                    }

                    "org.mtransit.android.ca_chambly_richelieu_carignan_citcrc_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_l_assomption_mrclasso_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_la_presqu_ile_citpi_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_laurentides_citla_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_le_richelain_citlr_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_les_moulins_mrclm_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_montreal_amt_train$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_richelieu_citvr_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_sorel_varennes_citsv_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_ste_julie_omitsju_bus$debugS.gtfs.realtime",
                    "org.mtransit.android.ca_sud_ouest_citso_bus$debugS.gtfs.realtime"
                        -> {
                        mapOf(KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_exo_quebec).join())
                    }

                    else -> {
                        MTLog.d(LOG_TAG, "Unexpected '$authority'!")
                        null
                    }
                }
            }
            // CUSTOM
            authority.endsWith("$debugS.winnipeg_transit") -> getKeyEntry(context, KeysIds.CA_WINNIPEG_TRANSIT_API)?.let { mapOf(it) }
            else -> {
                MTLog.d(LOG_TAG, "Unexpected '$authority'!")
                null
            }
        }
    }

    fun getKeysMap(targetedProviders: Iterable<ITargetedProviderProperties>?): Map<String, String>? {
        targetedProviders ?: return null
        val map = mutableMapOf<String, String>()
        targetedProviders.forEach { targetedProvider ->
            getKeysMap(targetedProvider)?.let { map.putAll(it) }
        }
        return map
    }

    fun getKeysMap(targetedProvider: ITargetedProviderProperties) = getKeysMap(targetedProvider.authority)

    fun getKeysMap(authority: String) = getKeysMap(appContext, authority)
}

private fun <T> Array<out T>.join() = this.joinToString(separator = EMPTY)