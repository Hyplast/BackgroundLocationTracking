package com.plcoding.backgroundlocationtracking

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.Clock

@RequiresApi(Build.VERSION_CODES.O)
fun fileMain() = runBlocking {

    println("Inside fileMain !!")
    val sendDispatcher1 = Dispatchers.Default
    val sendDispatcher2 = Dispatchers.IO
    val combineDispatcher = Dispatchers.IO

    val flow1 = flow {
        var count1 = 0
        while (true) {
            emit(count1)
            count1++
            delay(100L)
        }
    }.flowOn(sendDispatcher1)

    val flow2 = flow {
        var count2 = 0
        while (true) {
            emit(count2)
            count2++
            delay(1000L)
        }
    }.flowOn(sendDispatcher2)

    val combinedFlow = flow1.combine(flow2) { value1, value2 ->
        Pair(value1, value2)
    }.buffer(Channel.CONFLATED)
        .flowOn(combineDispatcher)


    println("Before outputstream")

    var isLogging = true
    val clock = Clock.systemUTC()
    val start = System.currentTimeMillis()
    val resolver = LocationApp.ContextProvider.get().contentResolver
    var contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "uri_${clock.instant()}.txt")
        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
    }
    var uri: Uri? = resolver.insert(
        MediaStore.Files.getContentUri("external"),
        contentValues
    )



       // var parcelFileDesc = uri?.let { resolver.openFileDescriptor(it, "w") }
//        var fileOutputStream2 = FileOutputStream(parcelFileDesc?.fileDescriptor)


    /*
        resolver.openFileDescriptor(uri, "w")?.use { parcelFileDescriptor ->
            FileOutputStream(parcelFileDescriptor.fileDescriptor).use {
                println("LOGGING...")

                combinedFlow.collect { (value1, value2) ->
                    it.write("Value1: $value1, Value2: $value2  after ${(System.currentTimeMillis() - start)/1000F}s\n".toByteArray())
                }
                println("LOGGING STOPPED")
                it.close()
            }
        }
    }

    var outputStream: OutputStream?
    withContext(Dispatchers.IO) {
        outputStream = FileOutputStream(System.currentTimeMillis().toString())
    }
    */

    var writeToFile = false

    var parcelFileDesc = uri?.let { resolver.openFileDescriptor(it, "w") }

    val job = launch {
        try {


                //var fileOutputStream2 = FileOutputStream(parcelFileDesc?.fileDescriptor)


            if (parcelFileDesc != null) {
                FileOutputStream(parcelFileDesc!!.fileDescriptor).use {
                    var prevFlow1Value: Int? = null
                    combinedFlow.collect { (value1, value2) ->
                        if (writeToFile) {
                            if (prevFlow1Value != value1) {
                                withContext(Dispatchers.IO) {
                                    it.write("${clock.instant()} - Value1: $value1, Value2: $value2\n".toByteArray())
                                }
                                //outputStream?.write("Value1: $value1, Value2: $value2\n".toByteArray())
                                prevFlow1Value = value1
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error collecting flow: ${e.message}")
        }
    }

    delay(2000L) // delay for 5 seconds before writing to file

    writeToFile = true
    delay(5000L)
    writeToFile = false

    /*
    contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "uri_${clock.instant()}.txt")
        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
    }
    uri = resolver.insert(
        MediaStore.Files.getContentUri("external"),
        contentValues
    )

    parcelFileDesc = uri?.let { resolver.openFileDescriptor(it, "w") }
*/
//    outputStream = withContext(Dispatchers.IO) {
//        FileOutputStream(System.currentTimeMillis().toString())
//    }
    delay(1000L)
    writeToFile = true
    delay(5000L)
    job.cancel()

}