package co.edu.unal.service

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val data by viewModel.data
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    var selectedItem by remember { mutableStateOf<ViveDigitalData?>(null) }

    val establecimientos by viewModel.establecimientos
    val sexos by viewModel.sexos
    val actividades by viewModel.actividades
    val nivelesEstudio by viewModel.nivelesEstudio

    Column(modifier = Modifier.fillMaxSize()) {
        FilterSection(
            establecimientos = establecimientos,
            sexos = sexos,
            actividades = actividades,
            nivelesEstudio = nivelesEstudio,
            onFilter = { filters ->
                viewModel.fetchData(filters)
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: $error")
            }
        } else if (selectedItem != null) {
            DetailView(selectedItem!!) { 
                selectedItem = null 
            }
        } else {
            DataList(data) { item ->
                selectedItem = item
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    establecimientos: List<String>,
    sexos: List<String>,
    actividades: List<String>,
    nivelesEstudio: List<String>,
    onFilter: (Map<String, String>) -> Unit
) {
    var establecimiento by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var actividad by remember { mutableStateOf("") }
    var nivelEstudio by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        // Dropdown for Establecimiento
        DropdownFilter(label = "Establecimiento", options = establecimientos, selectedOption = establecimiento, onOptionSelected = { establecimiento = it })
        
        // Dropdown for Sexo
        DropdownFilter(label = "Sexo", options = sexos, selectedOption = sexo, onOptionSelected = { sexo = it })

        // Dropdown for Actividad
        DropdownFilter(label = "Actividad", options = actividades, selectedOption = actividad, onOptionSelected = { actividad = it })

        // Dropdown for Nivel de Estudio
        DropdownFilter(label = "Nivel de Estudio", options = nivelesEstudio, selectedOption = nivelEstudio, onOptionSelected = { nivelEstudio = it })

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = {
                val filters = mutableMapOf<String, String>()
                if (establecimiento.isNotBlank()) filters["establecimiento"] = establecimiento
                if (sexo.isNotBlank()) filters["sexo"] = sexo
                if (actividad.isNotBlank()) filters["actividad"] = actividad
                if (nivelEstudio.isNotBlank()) filters["nivel_estudio"] = nivelEstudio
                onFilter(filters)
            }) {
                Text("Filter")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                establecimiento = ""
                sexo = ""
                actividad = ""
                nivelEstudio = ""
                onFilter(emptyMap())
            }) {
                Text("Clear")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        TextField(
            value = selectedOption,
            onValueChange = {}, // Input is read-only for dropdowns
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun DataList(data: List<ViveDigitalData>, onItemSelected: (ViveDigitalData) -> Unit) {
    LazyColumn {
        items(data) { item ->
            Card(modifier = Modifier.padding(8.dp).fillMaxWidth().clickable { onItemSelected(item) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Establecimiento: ${item.establecimiento}")
                    Text("Actividad: ${item.actividad}")
                }
            }
        }
    }
}

@Composable
fun DetailView(item: ViveDigitalData, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Area: ${item.area}")
        Text("Actividad: ${item.actividad}")
        Text("Grupo Poblacional: ${item.grupo_poblacional}")
        Text("Establecimiento: ${item.establecimiento}")
        Text("Tipo de Documento: ${item.tipo_documento}")
        Text("Sexo: ${item.sexo}")
        Text("Edad: ${item.edad}")
        Text("Etnia: ${item.etnia}")
        Text("Nivel de Estudio: ${item.nivel_estudio}")
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}