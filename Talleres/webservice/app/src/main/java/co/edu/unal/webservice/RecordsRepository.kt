package co.edu.unal.webservice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RecordsRepository {
    suspend fun getRecords(): List<Record> = withContext(Dispatchers.IO) {
        val url = "https://www.datos.gov.co/resource/gt2j-8ykr.json"
        val response = NetworkClient.fetchData(url)
        val list = mutableListOf<Record>()
        if (response != null) {
            val jsonArray = org.json.JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Record(
                        title = obj.optString("ciudad_municipio_nom", "Sin título"),
                        description = obj.optString("departamento_nom", "Sin descripción")
                    )
                )
            }
        }
        return@withContext list
    }
}
