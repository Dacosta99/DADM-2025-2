package co.edu.unal.webservice

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val records = MutableLiveData<List<Record>>()
    private val repository = RecordsRepository()

    fun loadRecords() {
        viewModelScope.launch {
            val data = repository.getRecords()
            records.postValue(data)
        }
    }
}
