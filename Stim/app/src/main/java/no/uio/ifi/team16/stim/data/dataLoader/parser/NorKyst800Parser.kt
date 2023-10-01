package no.uio.ifi.team16.stim.data.dataLoader.parser

import android.util.Log
import no.uio.ifi.team16.stim.util.*
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory


class NorKyst800Parser {
    companion object Parser {
        const val TAG = "NORKYST800PARSER"

        /////////////
        // REGEXES //
        /////////////
        //finds attribute data start, captures dimensions(group 0,1,2,3) and data(group 4)
        //only read the attribute itself, not the mappings included. i.e. starts with attribute.attribute
        /**
         * Androidstudio gives warnings for these regexes, but their fixes ARE NOT CORRECT.
         */
        private val dataRegex: (String) -> Regex = { str ->
            Regex("$str(?:\\[(.*?)\\])(?:\\[(.*?)\\])(?:\\[(.*?)\\])(?:\\[(.*?)\\])\\n((?:.*\\n)+?)\\n")
        }

        //parse 1D data, which has a slightly different format
        private val dataRegex1D: (String) -> Regex = { str ->
            Regex("$str(?:\\[(.*?)\\])?(?:\\[(.*?)\\])?(?:\\[(.*?)\\])?(?:\\[(.*?)\\])?\\n(.*\\n)*?\\n")
        }

        //finds start of a row, captures the row contents
        private val arrayRowRegex = Regex("""(?:\[.*?\]\[.*?\]\[.*?\])?(?:, )?(.+?)\n""")

        //capture a single entry (in a row)
        private val entryRegex = Regex(""" ?(.+?)(?:,|$)""")

        //capture a variable and its list of attributes of das-response
        private val dasVariableRegex = Regex("""    (.*?) \{\n((?:.*\n)*?) *?\}""")
        private val dasAttributeRegex = Regex("""        (.+?) (.+?) (.+);""")
        /////////////
        // PARSING //
        /////////////
        /**
         * Takes a DAS response(from opendap), and parses its variables and their attributes to a map, mapping
         * the variables name to a sequence of that variable attributes. Each attribute is represented
         * as a triple, the first being the attributes type, the second its name, and the third its value.
         *
         * @param das: das response from opendap, as a string
         * @return map from variable name to variables attribute
         */
        private fun parseDas(das: String): Map<String, Sequence<Triple<String, String, String>>> =
            dasVariableRegex.findAll(das).associate { match ->
                match.groupValues[1] to
                        dasAttributeRegex.findAll(match.groupValues[2]).map { attributeMatch ->
                            Triple(
                                attributeMatch.groupValues[1],
                                attributeMatch.groupValues[2],
                                attributeMatch.groupValues[3]
                            )
                        }
            }

        /**
         * takes the attribute declaration of a variable and returns a map from the name of its attributes to
         * pairs of the attributes type and value(as strings)
         *
         * @param variableDas: attributes of a variable in the das response
         * @return map from attribute names to attribute type and value
         */
        private fun parseVariableAttributes(variableDas: Sequence<Triple<String, String, String>>): Map<String, Pair<String, String>> =
            variableDas.associate { (type, name, value) ->
                name to Pair(type, value)
            }

        /**
         * get Fillvalue, Scaling and Offset of a given attribute associated to a given variable
         * Note that not all variables have all three attributes, for example w in the norkyst dataset.
         *
         * @param variablesToAttributes map from variable-names to sequences of their attirbutes
         * an attribute has a type(first), name(second) and value(third)
         * @param variable name of variable to look up attributes for
         * @retrun fillvalue, scale and offset of the given variable
         */
        private fun parseFSO(
            variablesToAttributes:
            Map<String, Sequence<Triple<String, String, String>>>,
            variable: String
        ): Triple<Number?, Number?, Number?> {
            var fillValue: Number? = null
            var scale: Number? = null
            var offset: Number? = null

            //open attributes of the given variable, the nparse out FSO from it
            variablesToAttributes[variable]?.let { attributes ->
                val attributeMap = parseVariableAttributes(attributes)

                fillValue = attributeMap["_FillValue"]?.let { (typeString, valueString) ->
                    parseFSOAttributeAs(typeString, valueString) //parse valuestring to typestring
                } ?: Log.w(
                    TAG,
                    "Failed to load norkyst800data - ${variable}FillValue from das, using default"
                )

                scale = attributeMap["scale_factor"]?.let { (typeString, valueString) ->
                    parseFSOAttributeAs(typeString, valueString) //parse valuestring to typestring
                } ?: Log.w(
                    TAG,
                    "Failed to load norkyst800data - ${variable}Scaling from das, using default"
                )

                offset = attributeMap["add_offset"]?.let { (typeString, valueString) ->
                    parseFSOAttributeAs(typeString, valueString) //parse valuestring to typestring
                } ?: Log.w(
                    TAG,
                    "Failed to load norkyst800data - ${variable}Offset from das, using default"
                )
            }

            return Triple(fillValue, scale, offset)
        }

        /**
         * parse an attribute with given type and value
         *
         * this function is not exhaustive, but sufficient.
         *
         * @param type: type to parse attribute as, as string
         * @param attr: attribute-value as a string
         * @return The attribute, parsed as specified type
         */
        private fun parseFSOAttributeAs(type: String, attr: String): Number? =
            when (type) {
                "Float32" -> attr.toFloatOrNull()
                "Int16" -> attr.toIntOrNull()
                else -> run {
                    Log.w(
                        TAG,
                        "Failed to parse FSO attribute with unknown type $type, returning null"
                    )
                    null
                }
            }

        /**
         * Parse time and depth response-string to 1D arrays
         *
         * @param timeAndDepthString: time and depth response from norkyst800 response
         * @return time, and depth arrays as a pair
         */
        fun parseTimeAndDepth(timeAndDepthString: String): Pair<FloatArray1D, FloatArray1D>? {
            val depth =
                make1DFloatArrayOf("depth", timeAndDepthString)
                    ?: run {
                        Log.e(TAG, "Failed to read <depth> from NorKyst800")
                        return null
                    }

            val time =
                make1DFloatArrayOf("time", timeAndDepthString)
                    ?: run {
                        Log.e(TAG, "Failed to read <time> from NorKyst800")
                        return null
                    }

            return Pair(
                time,
                depth
            )
        }

        /**
         * parse out the interesting attributes from the DAS response.
         *
         * Uses default values if unable to parse
         *
         * @param dasString das response from opendap as a string
         * @return a pair of
         *         A list of the FSO for each(interesting) attribute, and
         *         the projection of the dataset
         */
        fun parseFSOAndProjectionFromDAS(dasString: String): Pair<
                List<Triple<Number, Number, Number>>,
                CoordinateTransform
                > {
            //make a map from variable names to strings of their attributes
            val variablesToAttributes = parseDas(dasString)
            //make FSOs(Fillvalue, Scale, Offset)s
            //SALINITY, make standard, then parse and put any non-null into it
            val salinityFSO = Triple(-32767, 0.001f, 30.0f).let { defaultFSO ->
                val (f, s, o) = parseFSO(variablesToAttributes, "salinity")
                Triple(
                    f ?: defaultFSO.first,
                    s ?: defaultFSO.second,
                    o ?: defaultFSO.third
                )
            }
            //TEMPERATURE, make standard, then parse and put any non-null into it
            val temperatureFSO = Triple(-32767, 0.01f, 0.0f).let { defaultFSO ->
                val (f, s, o) = parseFSO(variablesToAttributes, "temperature")
                Triple(
                    f ?: defaultFSO.first,
                    s ?: defaultFSO.second,
                    o ?: defaultFSO.third
                )
            }
            //U, make standard, then parse and put any non-null into it
            val uFSO = Triple(-32767, 0.001f, 0.0f).let { defaultFSO ->
                val (f, s, o) = parseFSO(variablesToAttributes, "u")
                Triple(
                    f ?: defaultFSO.first,
                    s ?: defaultFSO.second,
                    o ?: defaultFSO.third
                )
            }
            //V, make standard, then parse and put any non-null into it
            val vFSO = Triple(-32767, 0.001f, 0.0f).let { defaultFSO ->
                val (f, s, o) = parseFSO(variablesToAttributes, "v")
                Triple(
                    f ?: defaultFSO.first,
                    s ?: defaultFSO.second,
                    o ?: defaultFSO.third
                )
            }
            //W, make standard, then parse and put any non-null into it
            val wFSO = Triple(1.0E37f, 1.0f, 0.0f).let { defaultFSO ->
                val (f, s, o) = parseFSO(variablesToAttributes, "w")
                Triple(
                    f ?: defaultFSO.first,
                    s ?: defaultFSO.second,
                    o ?: defaultFSO.third
                )
            }
            //PROJECTION
            val proj4String = variablesToAttributes["projection_stere"]
                ?.toList() //evaluate sequence
                ?.find { (_, name, _) -> //find the correct attribute
                    name == "proj4"
                }
                ?.third //take the value of that attribute
                ?.drop(1)?.dropLast(1) //trim off " "-s
                ?: run {
                    Log.w(TAG, "Failed to parse projection from DAS response, using default")
                    Options.defaultProj4String
                }
            //make the projection from string
            val projection: CoordinateTransform =
                CRSFactory().createFromParameters(null, proj4String).let { stereoCRT ->
                    val latLngCRT = stereoCRT.createGeographic()
                    val ctFactory = CoordinateTransformFactory()
                    ctFactory.createTransform(latLngCRT, stereoCRT)
                }

            return Pair(
                listOf(
                    salinityFSO,
                    temperatureFSO,
                    uFSO,
                    vFSO,
                    wFSO
                ),
                projection
            )
        }

        /**
         * given a OPENDAP data-string, parse out its data to an array of nullable values,
         * using scaling, offset and fillvalues
         *
         * @param dataString ascii response of data to parse
         * @param dataFSO, Fillvalue, Scale and Offset, as a triple of Numbers
         * @param name, name of attribue, used for debugging
         * @return 4D array of Nullable Floats
         */
        suspend fun parseNullable4DArrayFrom(
            dataString: String,
            dataFSO: Triple<Number, Number, Number>,
            name: String
        ): NullableFloatArray4D? {
            //PARSE
            val data =
                makeNullable4DFloatArrayOf(
                    name,
                    dataString,
                    dataFSO
                ) ?: run {
                    Log.e(TAG, "Failed to read $name from NorKyst800\nfrom data string $dataString")
                    return null
                }

            return data
        }

        /**
         * given a OPENDAP data-string of velocity, parse out its data to arrays of nullable values,
         * using scaling, offset and fillvalues
         *
         * @param velocityString ascii response of velocitydata (u, v and w)
         * @param uFSO, Fillvalue, Scale and Offset of u
         * @param vFSO, Fillvalue, Scale and Offset of v
         * @param wFSO, Fillvalue, Scale and Offset of w
         * @return a triple of 4D array of Nullable Floats, representing velocity in all 3 direcitons
         */
        suspend fun parseVelocity(
            velocityString: String,
            uFSO: Triple<Number, Number, Number>,
            vFSO: Triple<Number, Number, Number>,
            wFSO: Triple<Number, Number, Number>
        ):
                Triple<NullableFloatArray4D, NullableFloatArray4D, NullableFloatArray4D>? {
            //PARSE VELOCITY
            return Triple(
                makeNullable4DFloatArrayOf(
                    "u",
                    velocityString,
                    uFSO
                ) ?: run {
                    Log.e(TAG, "Failed to read <u> from NorKyst800")
                    return null
                },
                makeNullable4DFloatArrayOf(
                    "v",
                    velocityString,
                    vFSO
                ) ?: run {
                    Log.e(TAG, "Failed to read <v> from NorKyst800")
                    return null
                },
                makeNullable4DFloatArrayOfW(
                    "w",
                    velocityString,
                    wFSO
                ) ?: run {
                    Log.e(TAG, "Failed to read <w> from NorKyst800")
                    return null
                }
            )
        }

        ///////////////////
        // MAKE 1D ARRAY //
        ///////////////////
        /**
         * try to parse out a 1D FloatArray from an ascii opendap response,
         * returns null if any parsing fails.
         *
         * @param attribute name of attribute to parse out
         * @param response entire ascii response from opendap
         * @return array of Floats with the attributes values.
         */
        private fun make1DFloatArrayOf(attribute: String, response: String): FloatArray? =
            dataRegex1D(attribute).find(response, 0)?.let { match ->
                //parse dimensions
                val dT = match.groupValues.getOrNull(1)?.toInt() ?: run {
                    Log.e(TAG, "Failed to read <dimension-size> from 1DArray")
                    return null
                }
                //parse the data
                val dataString = match.groupValues.getOrNull(5) ?: run {
                    Log.e(TAG, "Failed to read <data-section> from 1DArray")
                    return null
                }
                //match all rows, for each one parse out entries
                val dataSequence = arrayRowRegex.findAll(dataString, 0).map { rowMatch ->
                    entryRegex.findAll(rowMatch.groupValues.getOrElse(1) { "" }, 0)
                        .map { entryMatch ->
                            entryMatch.groupValues.getOrNull(1)?.toFloat()
                        }
                }

                FloatArray(dT) { id ->
                    dataSequence.elementAtOrNull(0)?.elementAtOrNull(id) ?: run {
                        Log.e(TAG, "Failed to read an entry in data-section from 1DArray")
                        return null
                    }
                }
            }

        ///////////////////
        // MAKE 4D ARRAY //
        ///////////////////
        /**
         * try to parse out a 4D intarray from an ascii opendap response,
         * returns null if any parsing fails.
         * The array contains null where the data is not available(ie where there are filler values)
         *
         * @param attribute name of attribute to parse out
         * @param response entire ascii response from opendap
         * @param fso, FillValue, Scale and Offset of the attribute (parsed from DAS response)
         * @return 4D array of nullable floats
         */
        private suspend fun makeNullable4DFloatArrayOf(
            attribute: String,
            response: String,
            fso: Triple<Number, Number, Number>
        ): NullableFloatArray4D? =
            dataRegex(attribute).find(response, 0)?.let { match ->
                val (fillValue, scale, offset) = fso
                //parse dimensions
                val dD = match.groupValues.getOrNull(2)?.toInt() ?: run {
                    Log.e(TAG, "Failed to read <depth-dimension-size> from 4DArray")
                    return null
                }
                val dY = match.groupValues.getOrNull(3)?.toInt() ?: run {
                    Log.e(TAG, "Failed to read <y-dimension-size> from 4DArray")
                    return null
                }
                //parse the data
                val dataString = match.groupValues.getOrNull(5) ?: run {
                    Log.e(TAG, "Failed to read <data-section> from 4DArray")
                    return null
                }
                //read the rows of ints, apply scale, offset and fillvalues to get the floats
                readRowsOf4DIntArray(dD, dY, dataString, scale, offset, fillValue)
            }


        /**
         * for some reason the velocity w variable has a double as a fillervalue,
         * so we have to make an entirely separate function for it.
         *
         * try to parse out a 4D intarray from an ascii opendap response,
         * returns null if any parsing fails.
         * The array contains null where the data is not available(ie where there are filler values)
         *
         * @param attribute name of attribute to parse out
         * @param response entire ascii response from opendap
         * @param fso, FillValue, Scale and Offset of the attribute (parsed from DAS response)
         * @return 4D array of nullable floats
         */
        private suspend fun makeNullable4DFloatArrayOfW(
            attribute: String,
            response: String,
            fso: Triple<Number, Number, Number>
        ): NullableFloatArray4D? =
            dataRegex(attribute).find(response, 0)?.let { match ->
                val (fillValue, scale, offset) = fso
                //parse dimensions
                val dD = match.groupValues.getOrNull(2)?.toInt() ?: run {
                    Log.e(TAG, "Failed to read <depth-dimension-size> from 4DArray")
                    return null
                }
                val dY = match.groupValues.getOrNull(3)?.toInt() ?: run {
                    Log.e(TAG, "Failed to read <y-dimension-size> from 4DArray")
                    return null
                }
                //parse the data
                val dataString = match.groupValues.getOrNull(5) ?: run {
                    Log.e(TAG, "Failed to read <data-section> from 4DArray")
                    return null
                }

                //val dataSequence = readRowsOf4DFloatArray(dataString, scale, offset, fillValue)

                readRowsOf4DFloatArray(dD, dY, dataString, scale, offset, fillValue)
            }


        ///////////////////////////// opendap stores data as ints or floats. most are ints which scaling
        // READ INT OR FLOAT ARRAY // is applied to, but w-values are stored as floats
        /////////////////////////////
        /**
         * read a string of 4D data of ints to a 4D array of floats(after applying scale and offset)
         *
         * @param dD depth dimension-size
         * @param dY y dimension-
         * @param str string to parse
         * @param scale scale to apply to values
         * @param offset offset to apply to values
         * @param fillValue values to set to null when parsing
         */
        private suspend fun readRowsOf4DIntArray(
            dD: Int,
            dY: Int,
            str: String,
            scale: Number,
            offset: Number,
            fillValue: Number
        ): NullableFloatArray4D =
            str.split("\n")
                .dropLast(1) //drop empty row
                .chunked(dD * dY) //chunk into time-slices, there should be dT of them
                .mapAsync(16) { timeChunk -> //list of string-rows representing depth, y, x at a given time
                    timeChunk.chunked(dY)
                        .map { depthChunk -> //list of string-rows representing a xy grid at given time, depth
                            //read depthChunk, a XY grid, to a 2D array
                            depthChunk.map { row ->
                                row.split(", ")
                                    .drop(1) //drop indexes
                                    .map { entry ->
                                        val parsed = entry.toInt()
                                        if (parsed == fillValue) {
                                            null
                                        } else {
                                            parsed.times(scale.toFloat()) + offset.toFloat()
                                        }
                                    }
                                    .toTypedArray()
                            }.toTypedArray()
                        }.toTypedArray()
                }.toTypedArray()

        /**
         * read a string of 4D data of floats to a 4D array of floats(after applying scale and offset)
         *
         * @param dD depth dimension-size
         * @param dY y dimension-
         * @param str string to parse
         * @param scale scale to apply to values
         * @param offset offset to apply to values
         * @param fillValue values to set to null when parsing
         */
        private suspend fun readRowsOf4DFloatArray(
            dD: Int,
            dY: Int,
            str: String,
            scale: Number,
            offset: Number,
            fillValue: Number
        ): NullableFloatArray4D =
            str.split("\n")
                .dropLast(1) //drop empty row
                .chunked(dD * dY) //chunk into time-slices, there should be dT of them
                .mapAsync(16) { timeChunk -> //list of string-rows representing depth, y, x at a given time
                    timeChunk.chunked(dY)
                        .map { depthChunk -> //list of string-rows representing a xy grid at given time, depth
                            //read depthChunk, a XY grid, to a 2D array
                            depthChunk.map { row ->
                                row.split(", ")
                                    .drop(1) //drop indexes
                                    .map { entry ->
                                        val parsed = entry.toFloat()
                                        if (parsed == fillValue) {
                                            null
                                        } else {
                                            parsed * scale.toFloat() + offset.toFloat()
                                        }
                                    }.toTypedArray()
                            }.toTypedArray()
                        }.toTypedArray()
                }.toTypedArray()

        ///////////////
        // MAKE URLS //
        ///////////////
        /**
         * make an url to get time and depth
         *
         * @param baseUrl base url of dataset, usually retrieved by loadForecastURL().
         */
        fun makeTimeAndDepthUrl(
            baseUrl: String
        ): String {
            return baseUrl +
                    ".ascii?" +
                    "depth," +
                    "time"
        }

        /**
         * given a baseurl, return an url that gives the DAS of the dataset
         *
         * @param baseUrl baseurl
         * @return url to get das response
         */
        fun makeDasUrl(baseUrl: String): String {
            return "$baseUrl.das?"
        }

        /**
         * make an url to get all data in specified range
         *
         * @param baseUrl base url of dataset, usually retrieved by loadForecastURL().
         * @param xRange range of x-values to get from
         * @param yRange range of y-values to get from
         * @param depthRange depth as a range with format from:stride:to
         * @param timeRange time as a range with format from:stride:to
         * @return url to get response with all the interesting variables
         */
        fun makeFullDataUrl(
            baseUrl: String,
            xRange: IntProgression,
            yRange: IntProgression,
            depthRange: IntProgression,
            timeRange: IntProgression
        ): String {
            val xyString =
                "[${xRange.reformatFSL()}][${yRange.reformatFSL()}]"
            val dString = "[${depthRange.reformatFSL()}]"
            val tString = "[${timeRange.reformatFSL()}]"
            val tdxyString = tString + dString + xyString
            return baseUrl +
                    ".ascii?" +
                    "time," +
                    "depth," +
                    "salinity.salinity$tdxyString," +
                    "temperature.temperature$tdxyString," +
                    "u.u$tdxyString," +
                    "v.v$tdxyString," +
                    "w.w$tdxyString"
        }
    }
}