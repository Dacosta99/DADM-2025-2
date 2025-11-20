package co.edu.unal.osmpois

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(val elements: List<Element>?)
@JsonClass(generateAdapter = true)
data class Element(val id: Long, val lat: Double?, val lon: Double?, val tags: Map<String, String>?, val center: Center?)
@JsonClass(generateAdapter = true)
data class Center(val lat: Double, val lon: Double)