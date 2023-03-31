package com.plcoding.backgroundlocationtracking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// In class B
class LocationReceiver(private val emitter: LocationService) {
    private var job: Job? = null

    fun startCollecting() {
        job = CoroutineScope(Dispatchers.Main).launch {
            emitter.locationFlow.collect { locationValues ->
                // Do something with the location values
                println(locationValues)
            }
        }
    }

    fun stopCollecting() {
        job?.cancel()
        job = null
    }
}