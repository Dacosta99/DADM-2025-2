package co.edu.unal.reto10

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.unal.reto10.ui.theme.Reto10Theme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed interface UiState {
    data class Success(val puntos: List<PuntoAtencion>) : UiState
    data class Error(val message: String) : UiState
    object Loading : UiState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Reto10Theme {
                PuntoApp()
            }
        }
    }
}

@Composable
fun PuntoApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreenWithFilters(navController)
        }
        composable("detail/{puntoJson}") { backStackEntry ->
            val puntoJson = backStackEntry.arguments?.getString("puntoJson")
            val punto = Gson().fromJson(puntoJson, PuntoAtencion::class.java)
            DetailScreen(punto) { navController.popBackStack() }
        }
    }
}

@Composable
fun MainScreenWithFilters(navController: NavController) {
    var establecimiento by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var actividad by remember { mutableStateOf("") }
    var nivelEstudio by remember { mutableStateOf("") }

    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    val scope = rememberCoroutineScope()

    fun fetchDataForFilters() {
        uiState = UiState.Loading
        scope.launch(Dispatchers.IO) {
            val filters = mutableMapOf<String, String>()
            if (establecimiento.isNotBlank()) filters["establecimiento"] = establecimiento
            if (sexo.isNotBlank()) filters["sexo"] = sexo
            if (actividad.isNotBlank()) filters["actividad"] = actividad
            if (nivelEstudio.isNotBlank()) filters["nivel_estudio"] = nivelEstudio
            try {
                val puntos = fetchData(filters)
                withContext(Dispatchers.Main) {
                    uiState = UiState.Success(puntos)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = UiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchDataForFilters()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        FilterSection(
            establecimiento = establecimiento, onEstablecimientoChange = { establecimiento = it },
            sexo = sexo, onSexoChange = { sexo = it },
            actividad = actividad, onActividadChange = { actividad = it },
            nivelEstudio = nivelEstudio, onNivelEstudioChange = { nivelEstudio = it },
            onFilterClick = { fetchDataForFilters() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is UiState.Success -> MainScreen(
                puntos = state.puntos,
                onPuntoClick = { punto ->
                    val puntoJson = Gson().toJson(punto)
                    navController.navigate("detail/$puntoJson")
                }
            )
            is UiState.Error -> ErrorScreen(state.message)
            UiState.Loading -> LoadingScreen()
        }
    }
}

private fun fetchData(filters: Map<String, String>): List<PuntoAtencion> {
    val baseUrl = "https://www.datos.gov.co/resource/e4mc-qr8v.json"
    val filterString = if (filters.isNotEmpty()) {
        filters.entries.joinToString(" AND ") { entry ->
            "${entry.key}='${entry.value}'"
        }
    } else {
        ""
    }
    val urlString = when {
        filterString.isNotEmpty() -> "$baseUrl?\$where=$filterString"
        else -> "$baseUrl?\$limit=50"
    }
    val url = URL(urlString.replace(" ", "%20"))
    val connection = url.openConnection() as HttpsURLConnection
    val jsonString = connection.inputStream.use { it.reader().readText() }
    return parsePuntos(jsonString)
}

@Composable
fun FilterSection(
    establecimiento: String, onEstablecimientoChange: (String) -> Unit,
    sexo: String, onSexoChange: (String) -> Unit,
    actividad: String, onActividadChange: (String) -> Unit,
    nivelEstudio: String, onNivelEstudioChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Column {
        TextField(
            value = establecimiento,
            onValueChange = onEstablecimientoChange,
            label = { Text("Establecimiento") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
            DropdownFilter("Sexo", listOf("","Masculino", "Femenino"), sexo, onSexoChange)
            DropdownFilter("Actividad", listOf("","Secundaria", "Capacitación", "Acceso a Internet"), actividad, onActividadChange)
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()){
            DropdownFilter("Nivel de Estudio", listOf("","Bachillerato", "Primaria"), nivelEstudio, onNivelEstudioChange)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onFilterClick, modifier = Modifier.fillMaxWidth()) {
            Text("Filtrar")
        }
    }
}

@Composable
fun DropdownFilter(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) { Text(selected.ifBlank { label }) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = { onSelected(option); expanded = false }, text = { Text(option.ifBlank { "Todos" }) })
            }
        }
    }
}

private fun parsePuntos(json: String): List<PuntoAtencion> {
    val puntos = mutableListOf<PuntoAtencion>()
    val jsonArray = JSONArray(json)
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        if (jsonObject.has("establecimiento")) {
            puntos.add(
                PuntoAtencion(
                    area = jsonObject.optString("area", "N/A"),
                    actividad = jsonObject.optString("actividad", "N/A"),
                    grupo_poblacional = jsonObject.optString("grupo_poblacional", "N/A"),
                    establecimiento = jsonObject.getString("establecimiento"),
                    tipo_documento = jsonObject.optString("tipo_documento", "N/A"),
                    sexo = jsonObject.optString("sexo", "N/A"),
                    edad = jsonObject.optString("edad", "N/A"),
                    etnia = jsonObject.optString("etnia", "N/A"),
                    nivel_estudio = jsonObject.optString("nivel_estudio", "N/A")
                )
            )
        }
    }
    return puntos
}

@Composable
fun MainScreen(puntos: List<PuntoAtencion>, onPuntoClick: (PuntoAtencion) -> Unit, modifier: Modifier = Modifier) {
    if (puntos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No se encontraron resultados.")
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(puntos) { punto ->
                PuntoItem(punto, modifier = Modifier.clickable { onPuntoClick(punto) })
            }
        }
    }
}

@Composable
fun DetailScreen(punto: PuntoAtencion, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Establecimiento: ${punto.establecimiento}")
        Text(text = "Actividad: ${punto.actividad}")
        Text(text = "Grupo Poblacional: ${punto.grupo_poblacional}")
        Text(text = "Sexo: ${punto.sexo}")
        Text(text = "Edad: ${punto.edad}")
        Text(text = "Etnia: ${punto.etnia}")
        Text(text = "Nivel de Estudio: ${punto.nivel_estudio}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Volver")
        }
    }
}

@Composable
fun PuntoItem(punto: PuntoAtencion, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Establecimiento: ${punto.establecimiento}")
        Text(text = "Actividad: ${punto.actividad}")
        Text(text = "Sexo: ${punto.sexo}")
        Text(text = "Nivel de Estudio: ${punto.nivel_estudio}")
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Error: $message")
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Reto10Theme {
        val samplePuntos = listOf(
            PuntoAtencion("URBANA", "Capacitación", "Adulto", "VIVE DIGITAL PEREIRA CENTRO", "CC", "Masculino", "33", "Mestizo", "Bachillerato"),
            PuntoAtencion("RURAL", "Acceso a Internet", "Niño", "PUNTO VIVE DIGITAL COMBIA", "TI", "Femenino", "12", "Indigena", "Primaria")
        )
        MainScreen(samplePuntos, {})
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    Reto10Theme {
        val samplePunto = PuntoAtencion("URBANA", "Capacitación", "Adulto", "VIVE DIGITAL PEREIRA CENTRO", "CC", "Masculino", "33", "Mestizo", "Bachillerato")
        DetailScreen(samplePunto, {})
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    Reto10Theme {
        ErrorScreen("Failed to load data.")
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    Reto10Theme {
        LoadingScreen()
    }
}
