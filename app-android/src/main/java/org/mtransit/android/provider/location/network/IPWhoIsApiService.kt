package org.mtransit.android.provider.location.network

import retrofit2.http.GET

interface IPWhoIsApiService {

    @GET("/?output=json&fields=latitude,longitude,country_code")
    suspend fun getCurrentIPLocation(): IPLocation
}
