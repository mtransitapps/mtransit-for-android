package org.mtransit.android.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.allowTwitterNews
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils.EMPTY
import org.mtransit.android.data.ITargetedProviderProperties
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeysManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val billingManager: IBillingManager,
    private val remoteConfigProvider: RemoteConfigProvider,
) {

    companion object {

        private val LOG_TAG: String = KeysManager::class.java.simpleName

        private const val DEBUGGING_KEYS = false
        // private const val DEBUGGING_KEYS = true // DEBUG

        private const val PKG_START = "org.mtransit.android"
        private const val GTFS_RT_END = "gtfs.realtime"
        private const val OBA_END = "oba"

        private val debugS = if (BuildConfig.DEBUG) ".debug" else ""

        @JvmStatic
        fun getKey(context: Context, key: String) = when (key) {
            KeysIds.GOOGLE_PLACES_NEW_API_KEY -> context.resources.getStringArray(R.array.google_places_new_api_key).join()
            KeysIds.TWITTER_CACHED_API_URL -> context.resources.getStringArray(R.array.twitter_cached_api_url).join()
            KeysIds.TWITTER_BEARER_TOKEN -> context.resources.getStringArray(R.array.twitter_bearer_token).join()
            KeysIds.YOUTUBE_API_KEY -> context.resources.getStringArray(R.array.youtube_api_key).join()
            KeysIds.CA_SUDBURY_TRANSIT_AUTH_TOKEN -> context.resources.getStringArray(R.array.greater_sudbury_auth_token).join()
            KeysIds.CA_WINNIPEG_TRANSIT_API_KEY -> context.resources.getStringArray(R.array.ca_winnipeg_transit_api_key).join()
            else -> {
                if (DEBUGGING_KEYS) MTLog.w(LOG_TAG, "Unexpected '$key'!")
                null
            }
        }?.takeIf { it.isNotBlank() }

        private fun getKeyEntry(context: Context, key: String) = getKey(context, key)?.let { key to it }

        private fun getKeysMap(
            context: Context,
            billingManager: IBillingManager,
            remoteConfigProvider: RemoteConfigProvider,
            authority: String
        ): Map<String, String>? = when {
            // MAIN
            authority.endsWith("$debugS.provider.place") -> getKeyEntry(context, KeysIds.GOOGLE_PLACES_NEW_API_KEY)?.let { mapOf(it) }
            // NEWS
            authority.endsWith("$debugS.news.twitter") -> {
                buildMap {
                    getKeyEntry(context, KeysIds.TWITTER_BEARER_TOKEN)
                        ?.takeIf { billingManager.showingPaidFeatures() } // main app token $$ for paid users only
                        ?.let { (key, value) ->
                            put(key, value)
                        }
                    getKeyEntry(context, KeysIds.TWITTER_CACHED_API_URL)
                        ?.takeIf { remoteConfigProvider.allowTwitterNews() } // remote cached API for free users as well (with feature flag)
                        ?.let { (key, value) ->
                            put(key, value)
                        }
                }.takeIf { it.isNotEmpty() }
            }

            authority.endsWith("$debugS.news.youtube") -> getKeyEntry(context, KeysIds.YOUTUBE_API_KEY)?.let { mapOf(it) }
            // GTFS
            authority.endsWith("$debugS.gtfs") -> null // no keys (static)
            // GTFS-RT
            authority.endsWith("$debugS.$GTFS_RT_END") -> {
                when (authority) {
                    "$PKG_START.ca_gatineau_sto_bus$debugS.$GTFS_RT_END" -> {
                        mapOf(
                            KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_sto_ca).join(),
                            KeysIds.GTFS_REAL_TIME_URL_SECRET to context.resources.getStringArray(R.array.gtfs_real_time_url_secret_sto_ca).join()
                        )
                    }

                    "$PKG_START.ca_gtha_go_transit_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_gtha_go_transit_train$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_gta_up_express_train$debugS.$GTFS_RT_END" -> {
                        mapOf(KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_openmetrolinx_com).join())
                    }

                    "$PKG_START.ca_vancouver_translink_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_vancouver_translink_ferry2$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_vancouver_translink_train$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_west_coast_express_train$debugS.$GTFS_RT_END" -> {
                        mapOf(KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_translink_ca).join())
                    }

                    "$PKG_START.ca_chambly_richelieu_carignan_citcrc_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_l_assomption_mrclasso_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_la_presqu_ile_citpi_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_laurentides_citla_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_le_richelain_citlr_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_les_moulins_mrclm_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_montreal_amt_train$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_richelieu_citvr_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_sorel_varennes_citsv_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_ste_julie_omitsju_bus$debugS.$GTFS_RT_END",
                    "$PKG_START.ca_sud_ouest_citso_bus$debugS.$GTFS_RT_END" -> {
                        mapOf(KeysIds.GTFS_REAL_TIME_URL_TOKEN to context.resources.getStringArray(R.array.gtfs_real_time_url_token_exo_quebec).join())
                    }

                    else -> {
                        if (DEBUGGING_KEYS) MTLog.d(LOG_TAG, "Unexpected '$authority'!")
                        null
                    }
                }
            }
            // ONE BUS AWAY
            authority.endsWith("$debugS.$OBA_END") -> {
                when (authority) {
                    "$PKG_START.ca_york_region_yrt_viva_bus$debugS.$OBA_END" -> {
                        mapOf(KeysIds.ONE_BUS_AWAY_API_KEY to context.resources.getStringArray(R.array.one_bus_away_api_key_yrt_ca).join())
                    }

                    else -> {
                        if (DEBUGGING_KEYS) MTLog.d(LOG_TAG, "Unexpected '$authority'!")
                        null
                    }
                }
            }
            // CUSTOM
            authority.endsWith("$debugS.winnipeg_transit") -> getKeyEntry(context, KeysIds.CA_WINNIPEG_TRANSIT_API_KEY)?.let { mapOf(it) }
            else -> {
                if (DEBUGGING_KEYS) MTLog.d(LOG_TAG, "No key for '$authority'.")
                null
            }
        }
    }

    @Suppress("unused")
    fun getKeysMap(targetedProviders: Iterable<ITargetedProviderProperties>?): Map<String, String>? {
        targetedProviders ?: return null
        val map = mutableMapOf<String, String>()
        targetedProviders.forEach { targetedProvider ->
            getKeysMap(targetedProvider)?.let { map.putAll(it) }
        }
        return map
    }

    private fun getKeysMap(targetedProvider: ITargetedProviderProperties) = getKeysMap(targetedProvider.authority)

    fun getKeysMap(authority: String) = getKeysMap(appContext, billingManager, remoteConfigProvider, authority)
}

/**
 * join to string without separator
 */
private fun <T> Array<out T>.join() = this.joinToString(separator = EMPTY)