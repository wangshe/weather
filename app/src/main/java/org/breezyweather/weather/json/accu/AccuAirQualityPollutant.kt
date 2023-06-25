package org.breezyweather.weather.json.accu

import kotlinx.serialization.Serializable

@Serializable
data class AccuAirQualityPollutant(
    val type: String,
    val concentration: AccuAirQualityConcentration
)