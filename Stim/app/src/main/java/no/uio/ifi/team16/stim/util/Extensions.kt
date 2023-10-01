package no.uio.ifi.team16.stim.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.time.DayOfWeek
import java.time.ZonedDateTime

///////////
// ASYNC //
///////////
/**
 * A chunked version of List.mapAsync. Spawns fewer coroutines.
 *
 * This is not correct. There seems to be a problem in the scoping of the chunk value,
 * which is in the scope of all coroutines and thus shared. This leads to all
 * coroutines working on the same chunk rather than their own.
 */
suspend fun <T, U> List<T>.mapAsync(chunks: Int, f: (T) -> U): List<U> =
    chunked(chunks).map { chunk -> //list of rows
        CoroutineScope(Dispatchers.IO).async(Dispatchers.IO) {
            chunk.map { t -> //for each row apply
                f(t)
            }
        }
    }.map { deferred -> //list of rows deferred
        val v =
            deferred.await() //await the result of each mapping, in total blocks til all are finished
        v
    }.filter {
        isNotEmpty()
    }.flatten()


/////////////////////////////////
// REFORMATTING INTPROGRESSION //
/////////////////////////////////
/**
 * return an intprogression as a string with format "first:last:step"
 * indexing used in infectiouspressure, or more specifically ucar.edu.netcdf
 */
fun IntProgression.reformatFLS(): String {
    return "${this.first}:${this.last}:${this.step}"
}

/**
 * return an intprogression as a string with format "first:step:last"
 * indexing used in norkyst800, or more specifically slicing in opendap
 */
fun IntProgression.reformatFSL(): String {
    return "${this.first}:${this.step}:${this.last}"
}

fun DayOfWeek.toShortString(context: Context): String {
    return when (this) {
        DayOfWeek.MONDAY -> context.getString(no.uio.ifi.team16.stim.R.string.shortMonday)
        DayOfWeek.TUESDAY -> context.getString(no.uio.ifi.team16.stim.R.string.shortTuesday)
        DayOfWeek.WEDNESDAY -> context.getString(no.uio.ifi.team16.stim.R.string.shortWednesday)
        DayOfWeek.THURSDAY -> context.getString(no.uio.ifi.team16.stim.R.string.shortThursday)
        DayOfWeek.FRIDAY -> context.getString(no.uio.ifi.team16.stim.R.string.shortFriday)
        DayOfWeek.SATURDAY -> context.getString(no.uio.ifi.team16.stim.R.string.shortSaturday)
        DayOfWeek.SUNDAY -> context.getString(no.uio.ifi.team16.stim.R.string.shortSunday)
    }
}

//LIST EXTENSIONS
/**
 * from a sequence take every (stride) eleemnt
 */
fun <T> List<T>.takeEvery(stride: Int): List<T> =
    this.filterIndexed { i, _ -> (i % stride == 0) }

/**
 * for the given sequence, take values in the given intProgression(range with stride)
 */
fun <T> List<T>.takeRange(range: IntProgression): List<T> =
    drop(range.first).take(range.last - range.first).takeEvery(range.step)

/**
 * same as Map.getOrPut, but if the put value resuls in null, don't put
 *
 * if the key exists, return its value
 * if not, evaluate default,
 *      if default succeeds(not null) put it in the cache, and return the value
 *      if default fails (null), dont put anything in cache(get or put would put null in cache) and return null
 * @see MutableMap.getOrPut
 */
inline fun <K, V> MutableMap<K, V>.getOrPutOrPass(key: K, default: () -> V?): V? =
        getOrElse(key) {
            default()?.let { value ->
                this[key] = value
                value
            }
        }

fun ZonedDateTime.closestHour(): Int {

    val minutes = this.minute
    val hour = this.hour
    Log.d("CL", minutes.toString())
    return if (minutes < 30) hour else (hour + 1).mod(24)
}