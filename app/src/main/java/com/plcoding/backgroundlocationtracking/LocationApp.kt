package com.plcoding.backgroundlocationtracking

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class LocationApp: Application() {

    override fun onCreate() {
        super.onCreate()
        ContextProvider.initialize(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

        object ContextProvider {
            private lateinit var applicationContext: Context

            fun initialize(application: Application) {
                applicationContext = application.applicationContext
            }

            fun get(): Context {
                return applicationContext
            }

        }

}