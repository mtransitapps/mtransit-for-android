package org.mtransit.android.commons.data

fun makeDefaultPOI(
    authority: String = "authority",
    id: Int = 1,
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int = DataSourceTypeId.INVALID,
    @POI.ItemViewType type: Int = POI.ITEM_VIEW_TYPE_BASIC_POI,
    @POI.ItemStatusType statusType: Int = POI.ITEM_STATUS_TYPE_NONE,
    @POI.ItemActionType actionsType: Int = POI.ITEM_ACTION_TYPE_NONE,
    lat: Double = 1.0,
    lng: Double = 2.0,
) = DefaultPOI(
    authority,
    id,
    dataSourceTypeId,
    type,
    statusType,
    actionsType,
    lat,
    lng,
)

fun makeBikeStation(
    authority: String = "authority",
    id: Int = 1,
    lat: Double = 1.0,
    lng: Double = 2.0,
) = makeDefaultPOI(
    authority = authority,
    id = id,
    dataSourceTypeId = DataSourceTypeId.BIKE,
    type = POI.ITEM_VIEW_TYPE_BASIC_POI,
    statusType = POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
    actionsType = POI.ITEM_ACTION_TYPE_FAVORITABLE,
    lat = lat,
    lng = lng,
)

fun makeRDS(
    authority: String = "authority",
    routeId: Long = 1L,
    routeOriginalIdHash: Int? = routeId.toString().hashCode(),
    routeType: Int = 3,
    originalDirectionId: Int? = 1,
    directionId: Long = originalDirectionId?.let { routeId * 100L + it } ?: (routeId * 100L + 9L),
    stopId: Int = 1,
    stopOriginalIdHash: Int? = stopId.toString().hashCode(), // stopId, // "$stopId".hashCode()
    stopLat: Double = 1.0,
    stopLng: Double = 2.0,
    stopTimeZoneId: String? = "UTC",
    isNoPickup: Boolean = false,
    alwaysLastTripStop: Boolean = false,
) = RouteDirectionStop(
    1,
    Route(
        authority,
        routeId,
        "#$routeId",
        "route $routeId",
        "color",
        routeOriginalIdHash,
        routeType,
    ),
    Direction(
        authority,
        directionId,
        Direction.HEADSIGN_TYPE_STRING,
        "Head-Sign $originalDirectionId",
        routeId,
    ),
    makeStop(
        stopId = stopId,
        stopLat = stopLat,
        stopLng = stopLng,
        stopOriginalIdHash = stopOriginalIdHash,
        stopTimeZoneId = stopTimeZoneId,
    ),
    isNoPickup,
    alwaysLastTripStop,
)

fun makeStop(
    stopId: Int = 1,
    stopCode: String = "#$stopId",
    stopName: String = "Stop #$stopId",
    stopLat: Double = 1.0,
    stopLng: Double = 2.0,
    stopAccessibility: Int = Accessibility.DEFAULT,
    stopOriginalIdHash: Int? = stopId.toString().hashCode(), // stopId, // "$stopId".hashCode()
    stopTimeZoneId: String? = "UTC",
) = Stop(
    stopId,
    stopCode,
    stopName,
    stopLat,
    stopLng,
    stopAccessibility,
    stopOriginalIdHash,
    stopTimeZoneId,
)
