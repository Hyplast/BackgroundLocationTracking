package com.plcoding.backgroundlocationtracking

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NewScreen() {
    val applicationContext = LocationApp.ContextProvider.get()
    println("Inside NewScreen")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Button(onClick = {
            Intent(applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_START
                applicationContext.startService(this)
            }
            //GlobalScope.launch {
            //    fileMain()
            //}
        }) {
            Text(text = "Start")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            Intent(applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_STOP
                applicationContext.startService(this)
            }
        }) {
            //Text(text = LocationService.)
            Text(text = "Stop")
        }
    }

}