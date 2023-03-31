package com.plcoding.backgroundlocationtracking

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

//val url = "https://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&bbox=23,63,23.5,64&&starttime=2023-03-23T22:50:00Z&timestep=10&"
// val urlFile =

/*
fun readXMLFile(context: Context): String? {
    val stringBuilder = StringBuilder()
    try {
        val inputStream: InputStream = context.assets.open("FMI.xml")
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
        bufferedReader.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return stringBuilder.toString()
}

 */


data class Observation(
    val lat: Double,
    val long: Double,
    val timeStamp: Int,
    val t2m: Double? = null,
    val ws10min: Double? = null,
    val wg10min: Double? = null,
    val wd10min: Double? = null,
    val rh: Double? = null,
    val td: Double? = null,
    val r1h: Double? = null,
    val ri10min: Double? = null,
    val snowAws: Double? = null,
    val pSea: Double? = null,
    val vis: Double? = null,
    val nMan: Double? = null,
    val wawa: Double? = null
)

// We don't use namespaces.
private val ns: String? = null

class FMIXmlParser {

    /*
    @Throws(IOException::class)
    fun downloadUrl(urlString: String): InputStream? {
        val url = URL("https://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&bbox=23,63,23.5,64&&starttime=2023-03-23T23:50:00Z&timestep=10&")
        return (url.openConnection() as? HttpURLConnection)?.run {
            readTimeout = 10000
            connectTimeout = 15000
            requestMethod = "GET"
            doInput = true
            // Starts the query.
            connect()
            inputStream
        }
    }

     */

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): List<Observation> {
        inputStream.use {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(it, null)
            parser.nextTag()
            return readObservation(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readObservation(parser: XmlPullParser): List<Observation> {
        var observations: List<Observation> = emptyList()
        var observationsPart: List<Observation> = emptyList()

        parser.require(XmlPullParser.START_TAG, ns, "wfs:FeatureCollection")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "wfs:member" -> {
                    parser.require(XmlPullParser.START_TAG, ns, "wfs:member")
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.eventType != XmlPullParser.START_TAG) {
                            continue
                        }
                        when (parser.name) {
                            "omso:GridSeriesObservation" -> {
                                parser.require(XmlPullParser.START_TAG, ns, "omso:GridSeriesObservation")
                                while (parser.next() != XmlPullParser.END_TAG) {
                                    if (parser.eventType != XmlPullParser.START_TAG) {
                                        continue
                                    }
                                    when (parser.name) {
                                        "om:result" -> {
                                            parser.require(XmlPullParser.START_TAG, ns, "om:result")
                                            while (parser.next() != XmlPullParser.END_TAG) {
                                                if (parser.eventType != XmlPullParser.START_TAG) {
                                                    continue
                                                }
                                                when (parser.name) {
                                                    "gmlcov:MultiPointCoverage" -> {
                                                        parser.require(XmlPullParser.START_TAG, ns, "gmlcov:MultiPointCoverage")
                                                        while (parser.next() != XmlPullParser.END_TAG) {
                                                            if (parser.eventType != XmlPullParser.START_TAG) {
                                                                continue
                                                            }
                                                            when (parser.name) {
                                                                "gml:domainSet" -> {
                                                                    parser.require(XmlPullParser.START_TAG, ns, "gml:domainSet")
                                                                    while (parser.next() != XmlPullParser.END_TAG) {
                                                                        if (parser.eventType != XmlPullParser.START_TAG) {
                                                                            continue
                                                                        }
                                                                        when (parser.name) {
                                                                            "gmlcov:SimpleMultiPoint" -> {
                                                                                parser.require(XmlPullParser.START_TAG, ns, "gmlcov:SimpleMultiPoint")
                                                                                while (parser.next() != XmlPullParser.END_TAG) {
                                                                                    if (parser.eventType != XmlPullParser.START_TAG) {
                                                                                        continue
                                                                                    }
                                                                                    when (parser.name) {
                                                                                        "gmlcov:positions" -> { // found gmlcov:positions element
                                                                                            //println("PRINTING PARSER XML")
                                                                                            //val text = parser.bufferedReader().use { it.readText() }
                                                                                            //println(parser.name)
                                                                                            //println("DONE: PRINTING PARSER XML")
                                                                                            observationsPart = readLocationTime(parser)
                                                                                        }
                                                                                        else -> skip(parser)
                                                                                    }
                                                                                }
                                                                            }
                                                                            else -> skip(parser)
                                                                        }
                                                                    }
                                                                }
                                                                "gml:rangeSet" -> { // found gml:rangeSet element
                                                                    parser.require(XmlPullParser.START_TAG, ns, "gml:rangeSet")
                                                                    while (parser.next() != XmlPullParser.END_TAG) {
                                                                        if (parser.eventType != XmlPullParser.START_TAG) {
                                                                            continue
                                                                        }
                                                                        when (parser.name) {
                                                                            "gml:DataBlock" -> { // found gml:DataBlock element
                                                                                parser.require(XmlPullParser.START_TAG, ns, "gml:DataBlock")
                                                                                while (parser.next() != XmlPullParser.END_TAG) {
                                                                                    if (parser.eventType != XmlPullParser.START_TAG) {
                                                                                        continue
                                                                                    }
                                                                                    when (parser.name) {
                                                                                        "gml:doubleOrNilReasonTupleList" -> { // found gml:doubleOrNilReasonTupleList element
                                                                                            observations = readObservations(parser, observationsPart)
                                                                                        }
                                                                                        else -> skip(parser)
                                                                                    }
                                                                                }
                                                                            }
                                                                            else -> skip(parser)
                                                                        }
                                                                    }
                                                                }
                                                                else -> skip(parser)
                                                            }
                                                        }
                                                    }
                                                    else -> skip(parser)
                                                }
                                            }
                                        }
                                        else -> skip(parser)
                                    }
                                }
                            }
                            else -> skip(parser)
                        }
                    }
                }
                else -> skip(parser)
            }
        }
        return observations
    }

    /*
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readObservation_old(parser: XmlPullParser): List<Observation> {
        var observations: List<Observation> = emptyList()
        var observationsPart: List<Observation> = emptyList()

        parser.require(XmlPullParser.START_TAG, ns, "wfs:FeatureCollection")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "gmlcov:positions" -> {
                    println("HERE GOING TO gmlcov:positions")
                    observationsPart = readLocationTime(parser)
                }
                "gml:doubleOrNilReasonTupleList" -> {
                    println("HERE GOING TO gml:doubleOrNilReasonTupleList")
                    observations = readObservations(parser, observationsPart)
                }
                else -> {
                    println("SKIPPING PARSE")
                    skip(parser)
                }
            }
        }
        return observations
    }

     */

    /*


<wfs:FeatureCollection>
    <wfs:member>
        <omso:GridSeriesObservation>
            <om:result>
                <gmlcov:MultiPointCoverage>
                    <gml:domainSet>
                        <gmlcov:SimpleMultiPoint>
                            <gmlcov:positions>
                            </gmlcov:positions>
                        </gmlcov:SimpleMultiPoint>
                    </gml:domainSet>
                    <gml:rangeSet>
                        <gml:DataBlock>
                            <gml:doubleOrNilReasonTupleList>
                            </gml:doubleOrNilReasonTupleList>
                        </gml:DataBlock>
                    </gml:rangeSet>
                </gmlcov:MultiPointCoverage>
            </om:result>
        </omso:GridSeriesObservation>
    </wfs:member>
</wfs:FeatureCollection>


     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readLocationTime(parser: XmlPullParser): List<Observation> {

        //println("PRINTING PARSER XML inside readLocationTime")
        //println(parser.name)
        //println("DONE: PRINTING PARSER XML")

        parser.require(XmlPullParser.START_TAG, ns, "gmlcov:positions")
        //println("PRINTING PARSER XML inside require")
        //println(parser.name)
        //parser.isWhitespace
        val readParser = readText(parser)
        //val readParser = parser.text

        //val readParser = parser.text

        val output = readParser.replace("\\s+".toRegex(), " ").replace("\n", " ")

        //println(output)
        //println("PRINTING PARSER XML before trim")
        //println(output.trim().split(" ").chunked(3))
        //println("DONE: PRINTING PARSER XML")
        //val data = readText(parser).split(" ").chunked(3) { values ->
        val data = output.trim().split(" ").chunked(3) { values ->
            //println(values[0])
            //println(values[1])
            //println(values[2])
            Observation(
                lat = values[0].toDouble(),
                long = values[1].toDouble(),
                timeStamp = values[2].toInt()
            )
        }
        parser.require(XmlPullParser.END_TAG, ns, "gmlcov:positions")
        return data
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readObservations(
        parser: XmlPullParser,
        observations: List<Observation>
    ): List<Observation> {
        parser.require(XmlPullParser.START_TAG, ns, "gml:doubleOrNilReasonTupleList")
        var i = 0
        val readParser = readText(parser)
        //println(readParser)
        val output = readParser.replace("\\s+".toRegex(), " ").replace("\n", " ")

        //println("SIZE OF THE obs so far ${observations.size}")
        //println(output.trim())
        val data = output.trim().split(" ").chunked(13) { values ->
            //println("here is obs for i:<$i>: ${observations[i].lat}, ${observations[i].long}, ${observations[i].timeStamp}, ${values[0].toDoubleOrNullWithNan()}, ${values[1].toDoubleOrNullWithNan()}, ${values[2].toDoubleOrNullWithNan()}, ${values[3].toDoubleOrNullWithNan()}, ${values[4].toDoubleOrNullWithNan()}, ${values[5].toDoubleOrNullWithNan()}, ${values[6].toDoubleOrNullWithNan()}, ${values[7].toDoubleOrNullWithNan()}, ${values[8].toDoubleOrNullWithNan()}, ${values[9].toDoubleOrNullWithNan()}, ${values[10].toDoubleOrNullWithNan()}, ${values[11].toDoubleOrNullWithNan()}, ${values[12].toDoubleOrNullWithNan()}")
            //println(values[0])
            //println(values[1])
            //println(values[2])
            //println(values[3])
            Observation(
                lat = observations[i].lat,
                long = observations[i].long,
                timeStamp = observations[i++].timeStamp,
                t2m = values[0].toDoubleOrNullWithNan(),
                ws10min = values[1].toDoubleOrNullWithNan(),
                wg10min = values[2].toDoubleOrNullWithNan(),
                wd10min = values[3].toDoubleOrNullWithNan(),
                rh = values[4].toDoubleOrNullWithNan(),
                td = values[5].toDoubleOrNullWithNan(),
                r1h = values[6].toDoubleOrNullWithNan(),
                ri10min = values[7].toDoubleOrNullWithNan(),
                snowAws = values[8].toDoubleOrNullWithNan(),
                pSea = values[9].toDoubleOrNullWithNan(),
                vis = values[10].toDoubleOrNullWithNan(),
                nMan = values[11].toDoubleOrNullWithNan(),
                wawa = values[12].toDoubleOrNullWithNan()
            )
        }
        //println("SIZE OF THE obs after merge ${data.size}")
        parser.require(XmlPullParser.END_TAG, ns, "gml:doubleOrNilReasonTupleList")
        return data
    }



    /*
                    -2.5 4.3 6.7 53.0 88.0 -4.2 0.0 0.0 7.0 999.0 39990.0 8.0 0.0
                    -2.4 3.9 6.8 51.0 89.0 -4.1 NaN 0.0 7.0 999.1 42380.0 8.0 0.0
                    -2.4 4.0 5.8 35.0 89.0 -4.1 NaN 0.0 7.0 999.2 42880.0 8.0 0.0
                    -2.4 4.1 6.3 37.0 88.0 -4.2 NaN 0.0 7.0 999.3 49470.0 8.0 0.0
                    -2.5 4.4 7.0 37.0 88.0 -4.1 NaN 0.0 7.0 999.2 43440.0 8.0 0.0
                    -0.6 5.0 8.2 59.0 88.0 -2.4 NaN NaN NaN 997.8 50000.0 8.0 0.0
                    -0.4 4.9 7.9 57.0 89.0 -2.0 NaN NaN NaN 997.9 50000.0 8.0 0.0
                    -0.4 4.5 7.1 59.0 88.0 -2.2 NaN NaN NaN 997.9 50000.0 8.0 0.0
                    -0.4 5.3 7.7 56.0 88.0 -2.2 NaN NaN NaN 998.0 50000.0 8.0 0.0
                    -0.4 5.1 7.7 57.0 88.0 -2.1 NaN NaN NaN 998.1 50000.0 8.0 0.0
                    -1.5 NaN NaN NaN 90.0 -2.9 0.0 0.0 20.0 NaN NaN NaN NaN
                    -1.5 NaN NaN NaN 90.0 -2.8 NaN 0.0 22.0 NaN NaN NaN NaN
                    -1.4 NaN NaN NaN 91.0 -2.6 NaN 0.0 23.0 NaN NaN NaN NaN
                    -1.4 NaN NaN NaN 90.0 -2.8 NaN 0.0 23.0 NaN NaN NaN NaN
                    -1.1 NaN NaN NaN 93.0 -2.1 NaN 0.0 23.0 NaN NaN NaN NaN
                    -2.9 2.7 5.9 98.0 90.0 -4.3 0.0 0.0 34.0 NaN NaN NaN NaN
                    -2.8 2.5 5.5 98.0 91.0 -4.1 NaN 0.0 34.0 NaN NaN NaN NaN
                    -2.9 3.2 7.3 65.0 90.0 -4.4 NaN 0.0 34.0 NaN NaN NaN NaN
                    -2.9 2.3 7.0 93.0 90.0 -4.3 NaN 0.0 34.0 NaN NaN NaN NaN
                    -3.0 3.7 6.7 62.0 89.0 -4.5 NaN 0.0 34.0 NaN NaN NaN NaN


     */

    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

}

private fun String.toDoubleOrNullWithNan(): Double? {
    if (this == "NaN")
        return null
    return this.toDoubleOrNull()
}
