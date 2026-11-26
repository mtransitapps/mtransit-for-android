package org.mtransit.android.commons.data

import org.mtransit.android.data.IAgencyProperties

fun mkDefaultPOI(
    agency: IAgencyProperties? = null,
    authority: String = agency?.authority ?: "authority",
    id: Int = 1,
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int = agency?.type?.id ?: DataSourceTypeId.INVALID,
    @POI.ItemViewType type: Int = POI.ITEM_VIEW_TYPE_BASIC_POI,
    @POI.ItemStatusType statusType: Int = POI.ITEM_STATUS_TYPE_NONE,
    @POI.ItemActionType actionsType: Int = POI.ITEM_ACTION_TYPE_NONE,
    name: String = "name #$id",
    lat: Double = 1.0,
    lng: Double = 2.0,
) = makeDefaultPOI(
    authority = authority,
    id = id,
    dataSourceTypeId = dataSourceTypeId,
    type = type,
    statusType = statusType,
    actionsType = actionsType,
    lat = lat,
    lng = lng,
    name = name,
)

fun makeBikeStation(
    agency: IAgencyProperties? = null,
    authority: String = agency?.authority ?: "authority",
    dataSourceTypeId: Int = agency?.type?.id ?: DataSourceTypeId.BIKE,
    id: Int = 1,
    name: String = "name #$id",
    lat: Double = 1.0,
    lng: Double = 2.0,
) = mkDefaultPOI(
    authority = authority,
    id = id,
    dataSourceTypeId = dataSourceTypeId,
    statusType = POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
    actionsType = POI.ITEM_ACTION_TYPE_FAVORITABLE,
    name = name,
    lat = lat,
    lng = lng,
)

fun makeRDS(
    agency: IAgencyProperties? = null,
    authority: String = agency?.authority ?: "authority",
    routeId: Long = 1L,
    routeOriginalIdHash: Int? = routeId.toString().hashCode(),
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int = agency?.type?.id ?: DataSourceTypeId.INVALID,
    routeType: Int? = null, // custom route type != agency type
    originalDirectionId: Int? = 1,
    directionId: Long = originalDirectionId?.let { routeId * 100L + it } ?: (routeId * 100L + 9L),
    stopId: Int = 1,
    stopOriginalIdHash: Int? = stopId.toString().hashCode(), // stopId, // "$stopId".hashCode()
    stopName: String = "stop #$stopId",
    stopLat: Double = 1.0,
    stopLng: Double = 2.0,
    stopTimeZoneId: String? = "UTC",
    isNoPickup: Boolean = false,
    alwaysLastTripStop: Boolean = false,
) = makeRouteDirectionStop(
    dataSourceTypeId = dataSourceTypeId,
    route = Route(
        authority,
        routeId,
        "#$routeId",
        "route $routeId",
        "color",
        routeOriginalIdHash,
        routeType,
    ),
    direction = Direction(
        authority,
        directionId,
        Direction.HEADSIGN_TYPE_STRING,
        "Head-Sign $originalDirectionId",
        routeId,
    ),
    stop = makeStop(
        stopId = stopId,
        stopName = stopName,
        stopLat = stopLat,
        stopLng = stopLng,
        stopOriginalIdHash = stopOriginalIdHash,
        stopTimeZoneId = stopTimeZoneId,
    ),
    noPickup = isNoPickup,
    alwaysLastTripStop = alwaysLastTripStop,
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
