package no.uio.ifi.team16.stim.util

import ucar.ma2.ArrayFloat
import kotlin.ranges.IntProgression.Companion.fromClosedRange
///////////////////////////////
// typealiases for ND arrays //
///////////////////////////////
typealias FloatArray1D = FloatArray
typealias FloatArray2D = Array<FloatArray1D>
typealias FloatArray3D = Array<FloatArray2D>
typealias FloatArray4D = Array<FloatArray3D>

typealias IntArray1D = IntArray
typealias IntArray2D = Array<IntArray1D>
typealias IntArray3D = Array<IntArray2D>
typealias IntArray4D = Array<IntArray3D>

////////////////////////////////////
// classes for ND nullable arrays //
////////////////////////////////////
/*
When working with thredds-datasets, some grids have filler values, like intarrays having -32767
at entries where there is no data.
 */
typealias NullableFloatArray1D = Array<Float?>
typealias NullableFloatArray2D = Array<NullableFloatArray1D>
typealias NullableFloatArray3D = Array<NullableFloatArray2D>
typealias NullableFloatArray4D = Array<NullableFloatArray3D>

//there is no use for nullable int arrays - yet.

///////////////////////////
// getters for ND arrays // Note that these have explicit return-values
///////////////////////////
fun FloatArray2D.get(row: Int, column: Int): Float = this[row][column]
fun FloatArray3D.get(depth: Int, row: Int, column: Int): Float = this[depth][row][column]
fun FloatArray4D.get(depth: Int, time: Int, row: Int, column: Int): Float =
    this[depth][time][row][column]

fun IntArray2D.get(row: Int, column: Int): Int = this[row][column]
fun IntArray3D.get(depth: Int, row: Int, column: Int): Int = this[depth][row][column]
fun IntArray4D.get(depth: Int, time: Int, row: Int, column: Int): Int =
    this[depth][time][row][column]

////////////////////////////////////
// getters for ND nullable arrays // Note that these have nullable return-values
////////////////////////////////////
fun NullableFloatArray2D.get(row: Int, column: Int): Float? = this[row][column]
fun NullableFloatArray3D.get(depth: Int, row: Int, column: Int): Float? = this[depth][row][column]
fun NullableFloatArray4D.get(depth: Int, time: Int, row: Int, column: Int): Float? =
    this[depth][time][row][column]

/////////////
// SLICING //
/////////////
fun NullableFloatArray1D.get(indexes: IntProgression): NullableFloatArray1D =
    indexes.map { i -> this[i] }.toTypedArray()

fun FloatArray1D.get(indexes: IntProgression): FloatArray1D =
    indexes.map { i -> this[i] }.toFloatArray()

fun NullableFloatArray2D.get(rows: IntProgression, columns: IntProgression): NullableFloatArray2D =
    rows.map { r -> this[r].get(columns) }.toTypedArray()

fun FloatArray2D.get(rows: IntProgression, columns: IntProgression): FloatArray2D =
    rows.map { r -> this[r].get(columns) }.toTypedArray()

fun NullableFloatArray3D.get(
    depths: IntProgression,
    rows: IntProgression,
    columns: IntProgression
): NullableFloatArray3D =
    depths.map { d -> this[d].get(rows, columns) }.toTypedArray()

fun NullableFloatArray4D.get(
    times: IntProgression,
    depths: IntProgression,
    rows: IntProgression,
    columns: IntProgression
): NullableFloatArray4D =
    times.map { t -> this[t].get(depths, rows, columns) }.toTypedArray()

//////////////////////
// GET SORROUNNDING //
//////////////////////
fun NullableFloatArray2D.getSorrounding(row: Int, col: Int, radius: Int): NullableFloatArray2D {
    val (rows, cols) = shape()
    val maxRow = kotlin.math.min(rows - 1, row + radius)
    val minRow = kotlin.math.max(0, row - radius)
    val maxCol = kotlin.math.min(cols - 1, col + radius)
    val minCol = kotlin.math.max(0, col - radius)
    return get(
        fromClosedRange(minRow, maxRow, 1),
        fromClosedRange(minCol, maxCol, 1)
    )
}

///////////////
// GET SHAPE //
///////////////
fun NullableFloatArray2D.shape(): Pair<Int, Int> = Pair(this.size, this.firstOrNull()?.size ?: 0)


//////////////////////////////////
//"pretty" prints for ND arrays //
//////////////////////////////////
fun FloatArray1D.prettyPrint(): String = fold("[") { acc, arr ->
    "$acc, " + "%10.7f".format(arr)
} + "]"

fun FloatArray2D.prettyPrint(): String =
    foldIndexed("") { row, acc, arr -> "$acc[$row]${arr.prettyPrint()}" }

/////////////////////////////////////////////////
//"pretty" prints for nullable ND Float arrays //
/////////////////////////////////////////////////
fun NullableFloatArray4D.prettyPrint(): String =
    foldIndexed("[") { _, acc4, arr4 ->
        "$acc4\n" +
                arr4.foldIndexed("[") { _, acc3, arr3 ->
                    "$acc3\n" +
                            arr3.foldIndexed("") { row, acc2, arr2 ->
                                "$acc2[$row]" +
                                        arr2.fold("[") { acc, arr ->
                                            "$acc, " +
                                                    (if (arr == null) "N/A  " else "%5.2f".format(
                                                        arr
                                                    ))
                                        } + "]"
                            }
                } + "],"
    } + "]"



/////////////////////////////////
// ARRAYFLOAT TO ND FLOATARRAY //
/////////////////////////////////

/** cast netcdfs version of an array to the kind of ND arrays we use */
fun ArrayFloat.to2DFloatArray(): FloatArray2D {
    val asFloatArray =
        this.copyToNDJavaArray() as Array<FloatArray> //unchecked cast, but guaranteed to be Array<FloatArray>
    return Array(asFloatArray.size) { row ->
        asFloatArray[row]
    }
}