package org.mtransit.android.commons.data

fun makeRDS(
    authority: String = "authority",
    routeId: Long = 1L,
    routeOriginalIdHash: Int? = routeId.toString().hashCode(),
    routeType: Int = 3,
    originalDirectionId: Int? = 1,
    directionId: Long = originalDirectionId?.let { routeId * 100L + it } ?: (routeId * 100L + 9L),
    stopId: Int = 1,
    stopOriginalIdHash: Int? = stopId.toString().hashCode() // stopId, // "$stopId".hashCode()
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
    Stop(
        stopId,
        "#$stopId",
        "Stop #$stopId",
        1.0,
        2.0,
        Accessibility.DEFAULT,
        stopOriginalIdHash,
    ),
    false,
    false,
)
