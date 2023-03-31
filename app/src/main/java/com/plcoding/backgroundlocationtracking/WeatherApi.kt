package com.plcoding.backgroundlocationtracking

import retrofit2.http.GET
import retrofit2.http.Query

interface ObservationApi {

    //@GET("https://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&parameters=t2m,ws_10min,wg_10min,wd_10min,p_sea&bbox=23,60,24,61&maxlocations=1&")
    //@GET("wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&starttime=2023-03-26T10:00:00Z&endtime=2023-03-26T10:40:00Z&timestep=10")
    @GET("wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&timestep=10")
    suspend fun getWeatherObservationData(
        @Query("bbox") bbox: String
    ): String

    @GET("wfs/fin?service=WFS&version=2.0.0&request=GetFeature&storedquery_id=fmi::observations::radiation::multipointcoverage&parameters=GLOB_1MIN&timestep=10&")
    suspend fun getSunRadiationData(): String

    @GET("wfs?service=WFS&version=2.0.0&request=GetFeature&storedquery_id=fmi::observations::lightning::multipointcoverage&")
    suspend fun getLightningStrikesData(
        @Query("bbox") bbox: String,
        @Query("starttime") starttime: String //(esim. &starttime=2013-02-26T20:00:00Z&)
    ): String

}