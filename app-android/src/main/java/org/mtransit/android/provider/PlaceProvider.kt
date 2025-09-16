package org.mtransit.android.provider

import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.SearchByTextRequest
import org.mtransit.android.R
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.SqlUtils.ProjectionMapBuilder
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.POI.POIUtils
import org.mtransit.android.commons.provider.AgencyProvider
import org.mtransit.android.commons.provider.ContentProviderConstants
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper
import org.mtransit.android.commons.provider.POIProvider
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.provider.common.requiredContext
import org.mtransit.android.data.Place
import org.mtransit.android.util.UITimeUtils
import org.mtransit.android.util.toLatLngBounds
import org.mtransit.commons.FeatureFlags
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLHandshakeException
import kotlin.math.max

class PlaceProvider : AgencyProvider(), POIProviderContract {

    override fun getLogTag() = LOG_TAG

    private val _uriMatcher: UriMatcher by lazy { getNewUriMatcher(_authority) }

    override fun getURI_MATCHER() = _uriMatcher

    override fun getAgencyUriMatcher() = getURI_MATCHER()

    override fun getSearchSuggest(query: String?): Cursor? = null // TODO implement Place/Query auto-complete

    override fun getSearchSuggestProjectionMap(): ArrayMap<String?, String?>? = null // TODO implement Place/Query auto-complete

    override fun getSearchSuggestTable(): String? = null // TODO implement Place/Query auto-complete

    override fun getPOITable() = PlaceDbHelper.T_PLACE

    override fun getPOIProjection() = PROJECTION_PLACE_POI

    override fun getPOIMaxValidityInMs() = POI_MAX_VALIDITY_IN_MS

    override fun getPOIValidityInMs() = POI_VALIDITY_IN_MS

    override fun getPOI(poiFilter: POIProviderContract.Filter?): Cursor? {
        if (poiFilter == null) {
            return null
        }
        if (POIProviderContract.Filter.isAreaFilter(poiFilter)) {
            return ContentProviderConstants.EMPTY_CURSOR // empty cursor = processed
        } else if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
            return fetchTextSearchResults(poiFilter)
        } else if (POIProviderContract.Filter.isUUIDFilter(poiFilter)) {
            return ContentProviderConstants.EMPTY_CURSOR // empty cursor = processed
        } else if (POIProviderContract.Filter.isSQLSelection(poiFilter)) {
            return ContentProviderConstants.EMPTY_CURSOR // empty cursor = processed
        } else {
            MTLog.w(this, "Unexpected POI filter '$poiFilter'!")
            return null
        }
    }

    private fun fetchTextSearchResults(poiFilter: POIProviderContract.Filter): Cursor? {
        val context = requireContextCompat()
        val lat = poiFilter.getExtraDouble("lat", null)
        val lng = poiFilter.getExtraDouble("lng", null)
        val searchKeywords = poiFilter.searchKeywords
        val searchByTextRequest = getTextSearchRequest(lat, lng, null, searchKeywords)
        if (searchByTextRequest == null) { // no search keyboard => no search
            return ContentProviderConstants.EMPTY_CURSOR // empty cursor = processed
        }
        return getTextSearchResults(context, searchByTextRequest)
    }

    private fun getTextSearchResults(context: Context, searchByTextRequest: SearchByTextRequest): Cursor? {
        try {
            val placesClient = GooglePlacesApiProvider.getPlacesClient(context)
            if (placesClient == null) {
                MTLog.d(this, "SKIP > Places API client not available.")
                return null
            }
            MTLog.i(this, "Loading from Places API...")
            val searchByTextResponse = Tasks.await(placesClient.searchByText(searchByTextRequest))
            val newLastUpdateInMs = UITimeUtils.currentTimeMillis()
            val authority = _authority
            val lang = if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language
            var score = 1000
            val places: List<Place>? = searchByTextResponse?.places?.mapNotNull { place ->
                try {
                    val providerId = place.id ?: return@mapNotNull null
                    Place(
                        authority,
                        providerId,
                        lang,
                        newLastUpdateInMs
                    ).apply {
                        this.name = place.name ?: return@mapNotNull null
                        this.lat = place.latLng?.latitude ?: return@mapNotNull null
                        this.lng = place.latLng?.longitude ?: return@mapNotNull null
                        this.score = score--
                        this.iconUrl = place.iconUrl
                        this.iconBgColor = place.iconBackgroundColor
                    }
                } catch (e: Exception) {
                    MTLog.w(this, e, "Error while parsing result '$place'!")
                    null
                }
            }
            MTLog.i(this, "Loaded ${places?.size} places.")
            if (Constants.DEBUG) {
                places?.forEach { place ->
                    MTLog.d(this, "- place: $place")
                }
            }
            return convertTextSearchResults(places)
        } catch (sslhe: SSLHandshakeException) {
            MTLog.w(this, sslhe, "SSL error!")
            return null
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(Log.DEBUG)) {
                MTLog.w(this, uhe, "No Internet Connection!")
            } else {
                MTLog.w(this, "No Internet Connection!")
            }
            return null
        } catch (se: SocketException) {
            MTLog.w(LOG_TAG, se, "No Internet Connection!")
            return null
        } catch (e: Exception) {
            MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    private fun convertTextSearchResults(places: List<Place>?): Cursor =
        MatrixCursor(poiProjection).apply {
            places?.forEach { place ->
                addRow(place.getCursorRow())
            }
        }

    override fun getPOIFromDB(poiFilter: POIProviderContract.Filter?): Cursor? = null

    override fun getAgencyArea(context: Context) = Area.THE_WORLD

    override fun getAgencyMaxValidSec(context: Context) = 0 // unlimited

    override fun getAvailableVersionCode(context: Context, filterS: String?) = 0 // main app in-app update not supported yet

    override fun getContactUsWeb(context: Context) = StringUtils.EMPTY
    override fun getContactUsWebFr(context: Context) = StringUtils.EMPTY

    override fun getFaresWeb(context: Context) = StringUtils.EMPTY
    override fun getFaresWebFr(context: Context) = StringUtils.EMPTY

    override fun getExtendedTypeId(context: Context) = DataSourceTypeId.INVALID // not supported

    private val _authority: String by lazy { requiredContext.getString(R.string.place_authority) }

    @Suppress("unused")
    private val _authorityUri: Uri by lazy { UriUtils.newContentUri(_authority) }

    /**
     * Override if multiple [PlaceProvider] implementations in same app.
     */
    override fun getAgencyColorString(context: Context): String? = null // default

    @StringRes
    override fun getAgencyLabelResId() = R.string.place_label

    @StringRes
    override fun getAgencyShortNameResId() = R.string.place_short_name

    /**
     * Override if multiple [PlaceProvider] in same app.
     */
    private fun getDbName() = PlaceDbHelper.DB_NAME

    override fun isAgencyDeployed() = SqlUtils.isDbExist(requireContextCompat(), getDbName())

    override fun isAgencySetupRequired(): Boolean {
        if (_currentDbVersion > 0 && _currentDbVersion != getCurrentDbVersion()) {
            return true // live update required => update
        }
        if (!SqlUtils.isDbExist(requireContextCompat(), getDbName())) {
            return true // not deployed => initialization
        }
        if (SqlUtils.getCurrentDbVersion(requireContextCompat(), getDbName()) != getCurrentDbVersion()) {
            return true // update required => update
        }
        return false
    }

    private var _poiProjectionMap: ArrayMap<String, String>? = null

    override fun getPOIProjectionMap() = _poiProjectionMap ?: getNewPoiProjectionMap(_authority).also { _poiProjectionMap = it }

    override fun queryMT(uri: Uri, projection: Array<String?>?, selection: String?, selectionArgs: Array<String?>?, sortOrder: String?): Cursor? {
        try {
            var cursor = super.queryMT(uri, projection, selection, selectionArgs, sortOrder)
            if (cursor != null) {
                return cursor
            }
            cursor = POIProvider.queryS(this, uri, selection)
            if (cursor != null) {
                return cursor
            }
            throw IllegalArgumentException(String.format("Unknown URI (query): '$uri'"))
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while resolving query '$uri'!")
            return null
        }
    }

    override fun getSortOrder(uri: Uri): String? {
        val sortOrder = POIProvider.getSortOrderS(this, uri)
        if (sortOrder != null) {
            return sortOrder
        }
        return super.getSortOrder(uri)
    }

    override fun getTypeMT(uri: Uri): String? {
        val type = POIProvider.getTypeS(this, uri)
        if (type != null) {
            return type
        }
        return super.getTypeMT(uri)
    }

    override fun updateMT(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String?>?): Int {
        MTLog.w(this, "The update method is not available.")
        return 0
    }

    override fun insertMT(uri: Uri, values: ContentValues?): Uri? {
        MTLog.w(this, "The insert method is not available.")
        return null
    }

    override fun deleteMT(uri: Uri, selection: String?, selectionArgs: Array<String?>?): Int {
        MTLog.w(this, "The delete method is not available.")
        return 0
    }

    private fun getDBHelper(): SQLiteOpenHelper = getDBHelper(requireContextCompat())

    @WorkerThread
    override fun getReadDB(): SQLiteDatabase = getDBHelper().readableDatabase

    @WorkerThread
    override fun getWriteDB(): SQLiteDatabase = getDBHelper().writableDatabase

    private var _dbHelper: PlaceDbHelper? = null
    private var _currentDbVersion: Int = -1

    private fun getDBHelper(context: Context): PlaceDbHelper {
        when (val currentDbHelper: PlaceDbHelper? = _dbHelper) {
            null -> {  // initialize
                val newDbHelper = getNewDbHelper(context)
                _dbHelper = newDbHelper
                _currentDbVersion = getCurrentDbVersion()
                return newDbHelper
            }

            else -> {
                return try {
                    if (_currentDbVersion == getCurrentDbVersion()) {
                        _dbHelper?.close()
                        _dbHelper = null
                        getDBHelper(context)
                    } else {
                        currentDbHelper
                    }
                } catch (e: Exception) { // fail if locked, will try again later
                    MTLog.w(this, e, "Can't check DB version!")
                    currentDbHelper
                }
            }
        }
    }

    /**
     * Override if multiple [PlaceProvider] implementations in same app.
     */
    private fun getNewDbHelper(context: Context) = PlaceDbHelper(context.applicationContext)

    /**
     * Override if multiple [PlaceProvider] implementations in same app.
     */
    private fun getCurrentDbVersion() = PlaceDbHelper.getDbVersion()

    override fun getAgencyVersion() = getCurrentDbVersion()

    private class PlaceDbHelper(context: Context) : MTSQLiteOpenHelper(context, DB_NAME, null, getDbVersion()) {

        override fun getLogTag(): String = LOG_TAG

        override fun onCreateMT(db: SQLiteDatabase) {
            initAllDbTables(db)
        }

        override fun onUpgradeMT(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL(T_PLACE_SQL_DROP)
            initAllDbTables(db)
        }

        fun initAllDbTables(db: SQLiteDatabase) {
            db.execSQL(T_PLACE_SQL_CREATE)
        }

        companion object {
            private val LOG_TAG: String = PlaceDbHelper::class.java.getSimpleName()

            /**
             * Override if multiple [PlaceDbHelper] in same app.
             */
            const val DB_NAME: String = "place.db"

            /**
             * Override if multiple [PlaceDbHelper] in same app.
             */
            const val DB_VERSION: Int = 4

            const val T_PLACE: String = POIProvider.POIDbHelper.T_POI
            val T_PLACE_K_PROVIDER_ID: String = POIProvider.POIDbHelper.getFkColumnName("provider_id")
            val T_PLACE_K_LANG: String = POIProvider.POIDbHelper.getFkColumnName("lang")
            val T_PLACE_K_READ_AT_IN_MS: String = POIProvider.POIDbHelper.getFkColumnName("read_at_in_ms")
            val T_PLACE_K_ICON_URL: String = POIProvider.POIDbHelper.getFkColumnName("icon_url")
            val T_PLACE_K_ICON_BG_COLOR: String = POIProvider.POIDbHelper.getFkColumnName("icon_bg_url")
            private val T_PLACE_SQL_CREATE = POIProvider.POIDbHelper.getSqlCreateBuilder(T_PLACE)
                .appendColumn(T_PLACE_K_PROVIDER_ID, SqlUtils.TXT)
                .appendColumn(T_PLACE_K_LANG, SqlUtils.TXT)
                .appendColumn(T_PLACE_K_READ_AT_IN_MS, SqlUtils.INT)
                .appendColumn(T_PLACE_K_ICON_URL, SqlUtils.TXT)
                .appendColumn(T_PLACE_K_ICON_BG_COLOR, SqlUtils.INT) // @ColorInt
                .build()
            private val T_PLACE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_PLACE)

            /**
             * Override if multiple [PlaceDbHelper] in same app.
             */
            fun getDbVersion() = DB_VERSION
        }
    }

    object PlaceColumns {
        @JvmField
        val T_PLACE_K_PROVIDER_ID: String = POIProviderContract.Columns.getFkColumnName("provider_id")

        @JvmField
        val T_PLACE_K_LANG: String = POIProviderContract.Columns.getFkColumnName("lang")

        @JvmField
        val T_PLACE_K_READ_AT_IN_MS: String = POIProviderContract.Columns.getFkColumnName("read_at_in_ms")

        @JvmField
        val T_PLACE_K_ICON_URL: String = POIProviderContract.Columns.getFkColumnName("icon_url")

        @JvmField
        val T_PLACE_K_ICON_BG_COLOR: String = POIProviderContract.Columns.getFkColumnName("icon_bg_color")
    }

    companion object {

        private val LOG_TAG: String = PlaceProvider::class.java.getSimpleName()

        fun getNewUriMatcher(authority: String): UriMatcher =
            AgencyProvider.getNewUriMatcher(authority).apply {
                POIProvider.append(this, authority)
            }

        private val PROJECTION_PLACE = arrayOf<String>(
            POIProviderContract.Columns.T_POI_K_SCORE_META_OPT,
            PlaceColumns.T_PLACE_K_PROVIDER_ID,
            PlaceColumns.T_PLACE_K_LANG,
            PlaceColumns.T_PLACE_K_READ_AT_IN_MS,
            PlaceColumns.T_PLACE_K_ICON_URL,
            PlaceColumns.T_PLACE_K_ICON_BG_COLOR,
        )

        private val PROJECTION_PLACE_POI: Array<String> = POIProvider.PROJECTION_POI + PROJECTION_PLACE

        // https://developers.google.com/maps/documentation/places/android-sdk/text-search
        // https://developers.google.com/maps/documentation/places/android-sdk/usage-and-billing#text-search-id-only-ess-sku
        // https://developers.google.com/maps/documentation/places/android-sdk/usage-and-billing#text-search-pro-sku

        private val GOOGLE_PLACE_TEXT_SEARCH_FIELDS = listOf(
            Field.ID,
            Field.NAME, // DISPLAY_NAME
            Field.LAT_LNG, // LOCATION
            Field.ICON_URL,
            Field.ICON_BACKGROUND_COLOR,
        )

        @VisibleForTesting
        @JvmStatic
        fun getTextSearchRequest(
            optLat: Double?,
            optLng: Double?,
            optRadiusInMeters: Int?,
            searchKeywords: Array<String?>?,
        ): SearchByTextRequest? {
            var keywordMaxLength = -1
            val textQuery = searchKeywords
                ?.filterNotNull()
                ?.filter { it.isNotBlank() }
                ?.joinToString(separator = " ") { keyword ->
                    var keywordLength = keyword.length
                    if (keywordLength < 5 && StringUtils.isDigitsOnly(keyword, false)) {
                        keywordLength = -1 // ignore 4 digits
                    }
                    keywordMaxLength = max(keywordMaxLength, keywordLength)
                    keyword
                }
                .takeIf { keywordMaxLength >= 3 } ?: return null
            MTLog.d(this, "getTextSearchRequest() > textQuery: '$textQuery'")
            return SearchByTextRequest.builder(textQuery, GOOGLE_PLACE_TEXT_SEARCH_FIELDS)
                // .setMaxResultCount(1) // max 20 // cost is per / request
                .apply {
                    if (optLat != null && optLng != null) {
                        @Suppress("KotlinConstantConditions") // doesn't work? maybe with SDK 4.0.0+ (minSDK 23)
                        setLocationRestriction(
                            CircularBounds.newInstance(LatLng(optLat, optLng), (optRadiusInMeters ?: TEXT_SEARCH_URL_RADIUS_IN_METERS_DEFAULT).toDouble())
                                .takeIf { false } // doesn't work? maybe with SDK 4.0.0+ (minSDK 23)
                                ?: RectangularBounds.newInstance(
                                    Area.getArea(optLat, optLng, LocationUtils.MIN_AROUND_DIFF + LocationUtils.INC_AROUND_DIFF).toLatLngBounds()
                                )
                        )
                    }
                }.build()
        }

        private const val TEXT_SEARCH_URL_RADIUS_IN_METERS_DEFAULT = 50_000 // max = 50_000 // 50 km

        private const val POI_MAX_VALIDITY_IN_MS = Long.MAX_VALUE

        private const val POI_VALIDITY_IN_MS = Long.MAX_VALUE

        private fun getNewPoiProjectionMap(authority: String) = ProjectionMapBuilder.getNew()
            .appendValue(
                SqlUtils.concatenate(
                    SqlUtils.escapeString(POIUtils.UID_SEPARATOR),
                    SqlUtils.escapeString(authority),
                    SqlUtils.getTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_PROVIDER_ID)
                ), POIProviderContract.Columns.T_POI_K_UUID_META
            )
            .appendValue(DataSourceTypeId.PLACE, POIProviderContract.Columns.T_POI_K_DST_ID_META)
            .appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_ID, POIProviderContract.Columns.T_POI_K_ID)
            .appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_NAME, POIProviderContract.Columns.T_POI_K_NAME)
            .appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_LAT, POIProviderContract.Columns.T_POI_K_LAT)
            .appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_LNG, POIProviderContract.Columns.T_POI_K_LNG)
            .appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_TYPE, POIProviderContract.Columns.T_POI_K_TYPE)
            .appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_STATUS_TYPE, POIProviderContract.Columns.T_POI_K_STATUS_TYPE)
            .appendTableColumn(POIProvider.POIDbHelper.T_POI, POIProvider.POIDbHelper.T_POI_K_ACTIONS_TYPE, POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE)
            .appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_PROVIDER_ID, PlaceColumns.T_PLACE_K_PROVIDER_ID)
            .appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_LANG, PlaceColumns.T_PLACE_K_LANG)
            .appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_READ_AT_IN_MS, PlaceColumns.T_PLACE_K_READ_AT_IN_MS)
            .appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_ICON_URL, PlaceColumns.T_PLACE_K_ICON_URL)
            .appendTableColumn(PlaceDbHelper.T_PLACE, PlaceDbHelper.T_PLACE_K_ICON_BG_COLOR, PlaceColumns.T_PLACE_K_ICON_BG_COLOR)
            .apply {
                if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
                    appendTableColumn(
                        POIProvider.POIDbHelper.T_POI,
                        POIProvider.POIDbHelper.T_POI_K_ACCESSIBLE,
                        POIProviderContract.Columns.T_POI_K_ACCESSIBLE
                    )
                }
            }.build()
    }
}
