package co.edu.unal.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class OpenDataService {

    private val BASE_URL = "https://www.datos.gov.co/resource/e4mc-qr8v.json"

    suspend fun fetchData(limit: Int = 50, filters: Map<String, String> = emptyMap()): List<ViveDigitalData> {
        return withContext(Dispatchers.IO) {
            val queryParams = mutableListOf<String>()
            queryParams.add("\$limit=$limit")

            val filterClauses = filters.filter { it.value.isNotBlank() }.map { (key, value) ->
                "$key = '${value.replace("'", "''")}'" // Escape single quotes in value
            }

            if (filterClauses.isNotEmpty()) {
                val whereClause = filterClauses.joinToString(" AND ")
                // SODA API requires the $where clause to be URL encoded.
                queryParams.add("\$where=${URLEncoder.encode(whereClause, "UTF-8")}")
            }

            val fullUrl = "$BASE_URL?${queryParams.joinToString("&")}"
            val url = URL(fullUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Failed to connect: ${connection.responseCode} ${connection.responseMessage}")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()

            parseJson(response)
        }
    }

    suspend fun fetchDistinctValues(column: String): List<String> {
        return withContext(Dispatchers.IO) {
            // Use SoQL's $select and $group to get distinct values
            val query = "$BASE_URL?\$select=$column&\$group=$column"
            val url = URL(query)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Failed to fetch distinct values for $column: ${connection.responseCode} ${connection.responseMessage}")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()

            val values = mutableListOf<String>()
            val jsonArray = JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                if (jsonObject.has(column) && !jsonObject.isNull(column)) {
                    values.add(jsonObject.getString(column))
                }
            }
            // Clean up, unify casing, remove duplicates and sort
            values.map { it.trim().replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }
                .distinct()
                .sorted()
        }
    }


    private fun parseJson(jsonString: String): List<ViveDigitalData> {
        val dataList = mutableListOf<ViveDigitalData>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            dataList.add(ViveDigitalData.fromJson(jsonObject))
        }
        return dataList
    }
}