package com.plcoding.backgroundlocationtracking

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.InputStream
import kotlin.math.roundToInt

class LocationService: Service() {



    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private val _locationFlow = MutableSharedFlow<String>()
    val locationFlow: Flow<String> = _locationFlow.asSharedFlow()

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Reading observations...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //var textToSpeech =
        val textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Text-to-speech engine initialized successfully
            } else {
                // Failed to initialize text-to-speech engine
            }
        }


        locationClient
            .getLocationUpdates(600000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = String.format("%.2f", location.latitude)
                val long = String.format("%.2f", location.longitude)
                val bearing = String.format("%.1f", location.bearing)
                val speed = String.format("%.1f", location.speed)
                val altitude = String.format("%.1f", location.altitude)
                val locationValues = "Location: ($lat, $long, $bearing, $speed, $altitude)"

                //val testLocation = location
                //testLocation.latitude = 64.96
                //testLocation.longitude = 25.07
                //25.07 - 0.4)},${(64.96 - 0.4

                val repository = ObservationRepository2()
                // when (val result = repository.getObservationData(location.latitude, location.longitude)) {
                //when (val result = repository.getObservationData(64.96, 25.07)) {
                when (val result = repository.getObservationData(location.latitude, location.longitude)) {
                    is Resource.Success -> {
                        val readThisOut = constructLanguageString(result.data?.currentObservationData, location)
                        if (readThisOut != null) {
                            speakOut(textToSpeech, readThisOut)
                        } else {
                            println("Error, got answer but answer empty")
                        }
                    }
                    is Resource.Error -> {
                        println("Error, got Error from web.")
                    }
                }
                /*

                when( val result = ObservationRepository2.getObservationData(location.latitude, location.longitude)) {
                    is Resource.Success -> {
                        val readThisOut = constructLanguageString(result.data?.currentObservationData, location)
                        if (readThisOut != null) {
                            speakOut(textToSpeech, readThisOut)
                        } else {
                            println("Error, got answer but answer empty")
                        }
                    }
                    is Resource.Error -> {
                        println("Error, got Error from web.")

                    }
                } ?: kotlin.run {
                    error("Couldn't retrieve location. Make sure to grant permission and enable GPS.")
                }
                */


                sendLocation(locationValues)
                val updatedNotification = notification.setContentText(
                    "Location: ($lat, $long, $bearing, $speed, $altitude)"
                )
                /*var textToSpeech = TextToSpeech(applicationContext) { it ->
                    if (it == TextToSpeech.SUCCESS) {
                        textToSpeech?.let {
                            speakOut(it, "Location, latitude $lat, longitude $long)")
                        }
                    }
                }

                 */
                notificationManager.notify(1, updatedNotification.build())
            }
            .launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun speakOut(tts: TextToSpeech, text: String) {
        tts.apply {
            setSpeechRate(0.5f) // set the speech rate to 0.5x
            speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun sendLocation(locationValues: String) {
        _locationFlow.tryEmit(locationValues)
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    private fun constructLanguageString(data: Observation?, location: Location): String? {
        if (data == null) {
            return null
        }
        val dist = distance(data.lat, data.long, location.latitude, location.longitude)
        val bear = bearing(location.latitude, location.longitude, data.lat, data.long)
        return "Uusi havainto " +
                dist.let {if (dist < 4.0 ) "" else "etäisyys ${dist.roundToInt()} kilometriä ${bearingToDirection(bear)}." } +
                (data.ri10min?.let { if (it == 0.0) "" else " sadetta $it millimetriä tunnissa" } ?: "") +
                (data.ws10min?.let { " tuuli ${it.roundToInt()}" } ?: "") +
                (data.wg10min?.let { " kautta ${it.roundToInt()} metriä sekunnissa" } ?: "") +
                (data.wd10min?.let { " suunta ${it.roundToNearestFive().toString()} astetta." } ?: "") +
                if (data.t2m != null && data.td != null) " pilvenpohjat ${calculateCloudBaseHeight(data.t2m,data.td,0.0).roundToNearestHundred()} metriä." else ""
    }

    private fun Double?.roundToNearestFive(): Int? {
        val remainder = this?.rem(5)
        val roundedDegrees = if (remainder!! < 2.5) {
            this?.minus(remainder)
        } else {
            this?.plus((5 - remainder))
        }
        if (roundedDegrees != null) {
            return roundedDegrees.roundToInt()
        }
        return null
    }

    private fun Double?.roundToNearestHundred(): Int? {
        val remainder = this?.rem(100)
        val roundedDegrees = if (remainder!! < 50) {
            this?.minus(remainder)
        } else {
            this?.plus((100 - remainder))
        }
        if (roundedDegrees != null) {
            return roundedDegrees.roundToInt()
        }
        return null
    }

/*
    interface ObservationRepository2 {
        suspend fun getObservationData(lat: Double, long: Double): Resource<ObservationInfo>
    }


 */

    interface ObservationApi2 {
        @GET("wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&timestep=10")
        suspend fun getWeatherObservationData(
            @Query("bbox") bbox: String
        ): String
    }


    class ObservationRepository2 {

        private var callApi: ObservationApi2

        private fun provideFMIApi(): ObservationApi2 {
            val loggingInterceptor = Interceptor { chain ->
                val request = chain.request()
                Log.d("OkHttp", "Request URL: ${request.url}")
                chain.proceed(request)
            }
            return Retrofit.Builder()
                .baseUrl("https://opendata.fmi.fi/")
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create()
        }

        init {
            callApi = provideFMIApi()
        }


        suspend fun getObservationData(lat: Double, long: Double): Resource<ObservationInfo> {
            return try {
                Resource.Success(
                    data = callApi.getWeatherObservationData(
                    //    bbox = "${(25.07 - 0.4)},${(64.96 - 0.4)},${(25.07 + 0.4)},${(64.96 + 0.4)}"
                      bbox = "${(long - 0.4)},${(lat - 0.4)},${(long + 0.4)},${(lat + 0.4)}"
                    ).toObservationInfo(long, lat)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Resource.Error(e.message ?: "An unknown error occurred.")
            }
        }


        /*
        private fun toObservationInfo(long: Double, lat: Double): ObservationInfo {
            val inputStream: InputStream = this.byteInputStream()
            val parser = FMIXmlParser()
            val parsed = parser.parse(inputStream)

            val observationDataMap = parsed.toObservationDataMap()

            val highestTimeStamp = observationDataMap.keys.maxByOrNull { it }
            val highestTimeStampList = observationDataMap[highestTimeStamp]

            val closestLocation = highestTimeStampList?.findClosestLocation(lat, long)
            return ObservationInfo(
                observationDataPerDay = observationDataMap,//weatherDataMap,
                currentObservationData = closestLocation//currentWeatherData
            )
        }

         */
        private fun String.toObservationInfo(long: Double, lat: Double): ObservationInfo {
            val inputStream: InputStream = this.byteInputStream()
            val parser = FMIXmlParser()
            val parsed = parser.parse(inputStream)

            val observationDataMap = parsed.toObservationDataMap()

            val highestTimeStamp = observationDataMap.keys.maxByOrNull { it }
            val highestTimeStampList = observationDataMap[highestTimeStamp]

            val closestLocation = highestTimeStampList?.findClosestLocation(lat, long)
            return ObservationInfo(
                observationDataPerDay = observationDataMap,//weatherDataMap,
                currentObservationData = closestLocation//currentWeatherData
            )
        }


        /*
        suspend fun getObservationData(lat: Double, long: Double, apiResponse: String): Resource<ObservationInfo> {
            // Implementation of getObservationData function
        }

         */
        }
}

/*
private fun Unit.toObservationInfo(long: Double, lat: Double): ObservationInfo {
    val inputStream: InputStream = this.byteInputStream()
    val parser = FMIXmlParser()
    val parsed = parser.parse(inputStream)

    val observationDataMap = parsed.toObservationDataMap()

    val highestTimeStamp = observationDataMap.keys.maxByOrNull { it }
    val highestTimeStampList = observationDataMap[highestTimeStamp]

    val closestLocation = highestTimeStampList?.findClosestLocation(lat, long)
    return ObservationInfo(
        observationDataPerDay = observationDataMap,//weatherDataMap,
        currentObservationData = closestLocation//currentWeatherData
    )
}

 */

fun calculateCloudBaseHeight(temperatureC: Double, dewPointC: Double, heightStationM: Double): Double {
    return ((temperatureC - dewPointC) / 10) * 1247 + heightStationM
}