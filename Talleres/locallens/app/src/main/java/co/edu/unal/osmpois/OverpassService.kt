package co.edu.unal.osmpois

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OverpassService {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun query(@Field("data") data: String): OverpassResponse
}

