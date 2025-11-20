package co.edu.unal.service

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val openDataService = OpenDataService()

    // Data state
    val data = mutableStateOf<List<ViveDigitalData>>(emptyList())
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    // Filter options state
    val establecimientos = mutableStateOf<List<String>>(emptyList())
    val sexos = mutableStateOf<List<String>>(emptyList())
    val actividades = mutableStateOf<List<String>>(emptyList())
    val nivelesEstudio = mutableStateOf<List<String>>(emptyList())

    init {
        // Fetch initial data and filter options when ViewModel is created
        fetchData()
        loadFilterOptions()
    }

    fun fetchData(filters: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                data.value = openDataService.fetchData(filters = filters)
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                // Fetch distinct values for each filter concurrently for efficiency
                // Using async would be even better, but for simplicity, sequential calls are fine.
                establecimientos.value = openDataService.fetchDistinctValues("establecimiento")
                sexos.value = openDataService.fetchDistinctValues("sexo")
                actividades.value = openDataService.fetchDistinctValues("actividad")
                nivelesEstudio.value = openDataService.fetchDistinctValues("nivel_estudio")
            } catch (e: Exception) {
                // Optionally handle errors for filter loading separately
                error.value = "Failed to load filter options: ${e.message}"
            }
        }
    }
}