package co.edu.unal.osmpois

object OverpassQueryBuilder {
    fun build(radiusMeters: Int, lat: Double, lon: Double): String {
        return """
            [out:json][timeout:25];
            (
              node["amenity"="hospital"](around:$radiusMeters,$lat,$lon);
              node["amenity"="clinic"](around:$radiusMeters,$lat,$lon);
              node["amenity"="pharmacy"](around:$radiusMeters,$lat,$lon);
              node["tourism"="attraction"](around:$radiusMeters,$lat,$lon);
              node["tourism"="museum"](around:$radiusMeters,$lat,$lon);
              node["amenity"="doctors"](around:$radiusMeters,$lat,$lon);
            );
            out center;
        """.trimIndent()
    }
}
