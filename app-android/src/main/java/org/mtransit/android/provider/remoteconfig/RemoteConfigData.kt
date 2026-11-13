package org.mtransit.android.provider.remoteconfig

data class RemoteConfigData(
    private val adBannerLoadOnScreenResumeMinDurationSec: Long? = null,
    private val adBannerLarge: Boolean? = null,
    private val adAppOpenEnabled: Boolean? = null,
    private val adRewardedAmountInDays: Long? = null,
    @Deprecated("use allowTwitterNewsCached instead")
    private val allowTwitterNewsForFree: Boolean? = null,
    private val allowTwitterNewsCached: Boolean? = null,
    private val vehicleLocationDataRefreshMinMs: Long? = null,
    private val newUserAdsFree: Boolean? = null,
    private val mapsDirectionsEmbedApiEnabled: Boolean? = null,
) {

    companion object {

        // internal const val AD_BANNER_LOAD_ON_SCREEN_RESUME = "mt_ad_banner_load_on_screen_resume" // -> true is outperforming Baseline by 95% after 2 weeks
        // private const val AD_BANNER_LOAD_ON_SCREEN_RESUME_DEFAULT = false

        // should be using different AD unit IDs with different custom Automatic refresh (30-150 secs) or Google Optimized (-1)
        internal const val AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC = "mt_ad_banner_load_on_screen_resume_min_duration_sec"
        private const val AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC_DEFAULT = 1L // -> 1 sec is outperforming Baseline by 114% (& 2,3,5,7,10)

        internal const val AD_BANNER_LARGE = "mt_ad_banner_large"
        private const val AD_BANNER_LARGE_DEFAULT = false
        // private const val AD_BANNER_LARGE_DEFAULT = true // DEBUG

        internal const val AD_APP_OPEN_ENABLED = "mt_ad_app_open"
        private const val AD_APP_OPEN_ENABLED_DEFAULT = false
        // private const val AD_APP_OPEN_ENABLED_DEFAULT = true // DEBUG

        internal const val AD_REWARDED_AMOUNT_IN_DAYS = "mt_ad_rewarded_amount_days"
        const val AD_REWARDED_AMOUNT_IN_DAYS_DEFAULT_INT = 7
        private const val AD_REWARDED_AMOUNT_IN_DAYS_DEFAULT = AD_REWARDED_AMOUNT_IN_DAYS_DEFAULT_INT.toLong()

        @Deprecated("use ALLOW_TWITTER_NEWS_CACHED instead")
        internal const val ALLOW_TWITTER_NEWS_FOR_FREE = "mt_twitter_news_free"
        private const val ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT = false
        // private const val ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT = true // DEBUG

        internal const val ALLOW_TWITTER_NEWS_CACHED = "mt_twitter_news_cached"
        private const val ALLOW_TWITTER_NEWS_CACHED_DEFAULT = false
        // private const val ALLOW_TWITTER_NEWS_CACHED_DEFAULT = true // DEBUG

        internal const val VEHICLE_LOCATION_DATA_REFRESH_MIN_MS = "mt_vehicle_location_refresh_min_ms"
        private const val VEHICLE_LOCATION_DATA_REFRESH_MIN_MS_DEFAULT = 30_000L // 30 seconds
        // private const val VEHICLE_LOCATION_DATA_REFRESH_MIN_MS_DEFAULT = 10_000L // DEBUG

        internal const val NEW_USER_ADS_FREE = "mt_new_user_ads_free"
        private const val NEW_USER_ADS_FREE_DEFAULT = false
        // private const val NEW_USER_ADS_FREE_DEFAULT = true // DEBUG

        internal const val MAPS_DIRECTION_EMBED_API_ENABLED = "mt_maps_embed_api"
        private const val MAPS_DIRECTION_EMBED_API_ENABLED_DEFAULT = false
        // private const val MAPS_DIRECTION_EMBED_API_ENABLED_DEFAULT = true // DEBUG
    }

    val adBannerLoadOnScreenResumeMinDurationSecOrDefault =
        this.adBannerLoadOnScreenResumeMinDurationSec ?: AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC_DEFAULT

    val adBannerLargeOrDefault =
        this.adBannerLarge ?: AD_BANNER_LARGE_DEFAULT

    val adAppOpenEnabledOrDefault =
        this.adAppOpenEnabled ?: AD_APP_OPEN_ENABLED_DEFAULT

    val adRewardedAmountInDaysOrDefault =
        this.adRewardedAmountInDays ?: AD_REWARDED_AMOUNT_IN_DAYS_DEFAULT

    @Suppress("DEPRECATION")
    @Deprecated("use allowTwitterNewsCachedOrDefault instead")
    val allowTwitterNewsForFreeOrDefault =
        this.allowTwitterNewsForFree ?: ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT

    val allowTwitterNewsCachedOrDefault =
        this.allowTwitterNewsCached ?: ALLOW_TWITTER_NEWS_CACHED_DEFAULT

    val vehicleLocationDataRefreshMinMsOrDefault =
        this.vehicleLocationDataRefreshMinMs ?: VEHICLE_LOCATION_DATA_REFRESH_MIN_MS_DEFAULT

    val newUserAdsFreeOrDefault =
        this.newUserAdsFree ?: NEW_USER_ADS_FREE_DEFAULT

    val mapsDirectionsEmbedApiEnabledOrDefault =
        this.mapsDirectionsEmbedApiEnabled ?: MAPS_DIRECTION_EMBED_API_ENABLED_DEFAULT
}
