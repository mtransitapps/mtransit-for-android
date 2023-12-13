package org.mtransit.android.provider.location.network

import retrofit2.Response
import retrofit2.http.GET

interface IPWhoIsApiService {

    @GET("/?output=json&fields=latitude,longitude")
    suspend fun getCurrentIPLocation(): Response<IPLocation>
}