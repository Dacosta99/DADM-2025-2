package co.edu.unal.osmpois

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit

// ----------------------
// DataStore (delegate + key) - imports above
// ----------------------
private const val DATASTORE_NAME = "settings"
// create the DataStore delegate on Context (explicit type)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)
// preference key for radius in kilometers (float)
private val RADIUS_KM_KEY = floatPreferencesKey("pref_radius_km")

// ----------------------
// Retrofit services & models (local to this file for convenience)
// ----------------------
interface OverpassService {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun query(@Field("data") data: String): OverpassResponse
}

interface NominatimService {
    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1
    ): List<NominatimResult>
}

@JsonClass(generateAdapter = true)
data class OverpassResponse(val elements: List<Element>?)

@JsonClass(generateAdapter = true)
data class Element(
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String>? = null,
    val center: Center? = null
)

@JsonClass(generateAdapter = true)
data class Center(val lat: Double, val lon: Double)

@JsonClass(generateAdapter = true)
data class NominatimResult(val lat: String, val lon: String, val display_name: String)

// ----------------------
// Overpass query builder
// ----------------------
object OverpassQueryBuilder {
    fun build(radiusMeters: Int, lat: Double, lon: Double): String {
        return """
            [out:json][timeout:25];
            (
              node["amenity"="hospital"](around:$radiusMeters,$lat,$lon);
              node["amenity"="clinic"](around:$radiusMeters,$lat,$lon);
              node["amenity"="pharmacy"](around:$radiusMeters,$lat,$lon);
              node["amenity"="doctors"](around:$radiusMeters,$lat,$lon);
              node["tourism"="attraction"](around:$radiusMeters,$lat,$lon);
              node["tourism"="museum"](around:$radiusMeters,$lat,$lon);
              node["leisure"="park"](around:$radiusMeters,$lat,$lon);
            );
            out center;
        """.trimIndent()
    }
}

// ----------------------
// MainActivity
// ----------------------
class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var edtAddress: EditText

    // OkHttp client with User-Agent and logging
    private val okHttpClient: OkHttpClient by lazy {
        val uaInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "UNAL-OSM-POIS-App/1.0 (your-email@example.com)")
                .header("Accept-Language", "en")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(uaInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy { Moshi.Builder().build() }

    private val retrofitOverpass: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    private val overpass: OverpassService by lazy { retrofitOverpass.create(OverpassService::class.java) }

    private val retrofitNominatim: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    private val nominatim: NominatimService by lazy { retrofitNominatim.create(NominatimService::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid configuration - use a private SharedPreferences instance for osmdroid
        val prefs = getSharedPreferences("osm_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(applicationContext, prefs)

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        edtAddress = findViewById(R.id.edtAddress)
        val btnSearch: ImageButton = findViewById(R.id.btnSearch)
        val fabSettings: FloatingActionButton = findViewById(R.id.fabSettings)

        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        btnSearch.setOnClickListener { onSearchClicked() }
        fabSettings.setOnClickListener { showRadiusDialog() }

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            centerOnLocationAndLoadPOIs()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) centerOnLocationAndLoadPOIs()
            else Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }

    private fun centerOnLocationAndLoadPOIs() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        client.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                val gp = GeoPoint(loc.latitude, loc.longitude)
                map.controller.setCenter(gp)
                addMyLocationMarker(gp)
                lifecycleScope.launch {
                    loadPOIs(loc.latitude, loc.longitude)
                }
            } else {
                Toast.makeText(this, "No se pudo obtener ubicación. Ingresa dirección.", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error obteniendo ubicación: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addMyLocationMarker(gp: GeoPoint) {
        val m = Marker(map)
        m.position = gp
        m.title = "Tu posición"
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        runOnUiThread {
            map.overlays.removeIf { it is Marker && (it as Marker).title == "Tu posición" }
            map.overlays.add(m)
            map.invalidate()
        }
    }

    private fun onSearchClicked() {
        val address = edtAddress.text.toString().trim()
        if (address.isEmpty()) {
            centerOnLocationAndLoadPOIs()
            return
        }
        lifecycleScope.launch {
            try {
                val results = nominatim.search(address, "json", 1)
                if (results.isNotEmpty()) {
                    val first = results[0]
                    val lat = first.lat.toDouble()
                    val lon = first.lon.toDouble()
                    val gp = GeoPoint(lat, lon)
                    map.controller.setCenter(gp)
                    addMyLocationMarker(gp)
                    loadPOIs(lat, lon)
                } else {
                    Toast.makeText(this@MainActivity, "Dirección no encontrada", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error geocoding: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Suspend function to load POIs; reads radius from DataStore
    private suspend fun loadPOIs(lat: Double, lon: Double) {
        // read radiusKm from DataStore (flow -> first)
        val radiusKm: Float = try {
            applicationContext.dataStore.data.map { it[RADIUS_KM_KEY] ?: 2f }.first()
        } catch (e: Exception) {
            2f
        }
        val radiusMeters = (radiusKm * 1000).toInt()

        val query = OverpassQueryBuilder.build(radiusMeters, lat, lon)

        withContext(Dispatchers.Main) {
            map.overlays.removeIf { it is Marker && (it as Marker).title != "Tu posición" }
            map.invalidate()
        }

        try {
            val response = overpass.query(query)
            val elements = response.elements ?: emptyList()
            withContext(Dispatchers.Main) {
                elements.forEach { el ->
                    val (plat, plon) = if (el.lat != null && el.lon != null) {
                        el.lat to el.lon
                    } else {
                        el.center?.lat to el.center?.lon
                    }
                    if (plat != null && plon != null) {
                        val name = el.tags?.get("name") ?: el.tags?.get("official_name") ?: "Sin nombre"
                        val type = el.tags?.get("amenity") ?: el.tags?.get("tourism") ?: "POI"
                        addPoiMarker(GeoPoint(plat, plon), name, type, lat, lon)
                    }
                }
                map.invalidate()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Error buscando POIs: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addPoiMarker(pos: GeoPoint, name: String, type: String, originLat: Double, originLon: Double) {
        val m = Marker(map)
        m.position = pos
        m.title = name
        m.subDescription = type
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        m.setOnMarkerClickListener { _, _ ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(originLat, originLon, pos.latitude, pos.longitude, results)
            val km = results[0] / 1000.0
            AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage("$type\nDistancia: ${"%.2f".format(km)} km")
                .setPositiveButton("Cerrar", null)
                .show()
            true
        }
        runOnUiThread {
            map.overlays.add(m)
            map.invalidate()
        }
    }

    private fun showRadiusDialog() {
        val items = arrayOf("0.5", "1", "2", "5", "10")
        lifecycleScope.launch {
            val currentKm: Float = try {
                applicationContext.dataStore.data.map { it[RADIUS_KM_KEY] ?: 2f }.first()
            } catch (e: Exception) {
                2f
            }
            val checked = items.indexOfFirst { it.toFloatOrNull() == currentKm }
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Radio (km)")
                    .setSingleChoiceItems(items, if (checked >= 0) checked else 2) { dialog, which ->
                        // write new value to DataStore (suspend)
                        lifecycleScope.launch {
                            applicationContext.dataStore.edit { prefs ->
                                prefs[RADIUS_KM_KEY] = items[which].toFloat()
                            }
                        }
                        dialog.dismiss()
                        val center = map.mapCenter as GeoPoint
                        lifecycleScope.launch {
                            loadPOIs(center.latitude, center.longitude)
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }
}
