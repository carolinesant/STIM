package no.uio.ifi.team16.stim.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import no.uio.ifi.team16.stim.R
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * Weather forecast for the next 4 days
 */
data class WeatherForecast(
    val first: Weather,
    val second: Weather,
    val third: Weather,
    val fourth: Weather
) {
    var storm: Storm? = null
}

/**
 * The weather at a location at a given day
 */
data class Weather(
    val temperature: Double,
    val icon: WeatherIcon,
    val day: Weekday
)

/**
 * Represents a storm (wind higher than 20 m/s)
 */
data class Storm(
    val day: Weekday,
    val strength: Double
)

enum class WeatherIcon(@DrawableRes private val drawable: Int) {

    SUNNY(R.drawable.sunny),
    CLOUDY(R.drawable.cloudy),
    RAINY(R.drawable.rainy),
    SNOWY(R.drawable.snowy);

    /**
     * Get this icon as an Android drawable
     */
    fun asDrawable(context: Context): Drawable? {
        return AppCompatResources.getDrawable(context, drawable)
    }

    companion object {

        /**
         * Get icon from the response of the weather API
         */
        fun fromMetName(metName: String): WeatherIcon {
            if (metName.contains("rain") || metName.contains("sleet")) {
                return RAINY
            }

            if (metName.contains("snow")) {
                return SNOWY
            }

            var name = metName
            if (name.contains("_")) {
                name = metName.split("_")[0]
            }

            return when (name) {
                "clearsky" -> SUNNY
                "fair" -> SUNNY
                "fog" -> CLOUDY
                "partlycloudy" -> CLOUDY
                "cloudy" -> CLOUDY
                else -> {
                    Log.e("Weather", "Ukjent værikon: $name")
                    return CLOUDY
                }
            }
        }
    }
}

enum class Weekday(private val num: Int) {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    /**
     * Returns true if this day is today
     */
    fun isToday(): Boolean {
        return LocalDateTime.now().dayOfWeek.value == num
    }

    fun getTranslation(context: Context): String {
        if (isToday()) {
            return context.getString(R.string.today)
        }

        return when (this) {
            MONDAY -> context.getString(R.string.monday)
            TUESDAY -> context.getString(R.string.tuesday)
            WEDNESDAY -> context.getString(R.string.wednesday)
            THURSDAY -> context.getString(R.string.thursday)
            FRIDAY -> context.getString(R.string.friday)
            SATURDAY -> context.getString(R.string.saturday)
            SUNDAY -> context.getString(R.string.sunday)
        }
    }



    companion object {

        /**
         * Construct a weekday object from an ISO string
         */
        fun fromISOString(isoString: String): Weekday {
            return values()[ZonedDateTime.parse(isoString).dayOfWeek.ordinal]
        }
    }
}