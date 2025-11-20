package co.edu.unal.osmpois

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimService {
    @GET("search")
    suspend fun search(@Query("q") q: String, @Query("format") format: String = "json", @Query("limit") limit: Int = 1): List<NominatimResult>
}

data class NominatimResult(val lat: String, val lon: String, val display_name: String)