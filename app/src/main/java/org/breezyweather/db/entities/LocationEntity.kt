package org.breezyweather.db.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.breezyweather.common.basic.models.options.provider.WeatherSource
import org.breezyweather.db.converters.TimeZoneConverter
import org.breezyweather.db.converters.WeatherSourceConverter
import java.util.*

/**
 * Location entity.
 *
 * [Location].
 */
@Entity
data class LocationEntity(
    @field:Id var id: Long = 0,

    var formattedId: String,
    var cityId: String,
    var latitude: Float,
    var longitude: Float,
    @field:Convert(
        converter = TimeZoneConverter::class,
        dbType = String::class
    ) var timeZone: TimeZone,
    var country: String,
    var countryCode: String? = null,
    var province: String? = null,
    var provinceCode: String? = null,
    var city: String,
    var district: String? = null,
    @field:Convert(
        converter = WeatherSourceConverter::class,
        dbType = String::class
    ) var weatherSource: WeatherSource,
    var currentPosition: Boolean = false,
    var residentPosition: Boolean = false,
    var china: Boolean = false
)