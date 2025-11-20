package co.edu.unal.service

import org.json.JSONObject

data class ViveDigitalData(
    val area: String,
    val actividad: String,
    val grupo_poblacional: String,
    val establecimiento: String,
    val tipo_documento: String,
    val sexo: String,
    val edad: String,
    val etnia: String,
    val nivel_estudio: String
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): ViveDigitalData {
            return ViveDigitalData(
                area = jsonObject.optString("area"),
                actividad = jsonObject.optString("actividad"),
                grupo_poblacional = jsonObject.optString("grupo_poblacional"),
                establecimiento = jsonObject.optString("establecimiento"),
                tipo_documento = jsonObject.optString("tipo_documento"),
                sexo = jsonObject.optString("sexo"),
                edad = jsonObject.optString("edad"),
                etnia = jsonObject.optString("etnia"),
                nivel_estudio = jsonObject.optString("nivel_estudio")
            )
        }
    }
}