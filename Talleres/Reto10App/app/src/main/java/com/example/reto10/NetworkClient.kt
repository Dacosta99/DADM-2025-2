package com.example.reto10

import okhttp3.OkHttpClient
import okhttp3.Request

object NetworkClient {
    private val client = OkHttpClient()

    fun fetchData(url: String): String? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) response.body?.string() else null
        }
    }
}
