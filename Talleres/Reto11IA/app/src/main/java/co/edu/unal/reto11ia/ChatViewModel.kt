package co.edu.unal.reto11ia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<ChatUiState> = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private lateinit var generativeModel: GenerativeModel

    init {
        // TODO: Add your API key
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "AIzaSyB85km8xivprGbSuQ2AflJ18xzg0dWPYaw"
        )
    }

    fun sendMessage(userMessage: String) {
        // Add user message to the UI
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + ChatMessage(message = userMessage, isFromUser = true)
            )
        }

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(userMessage)

                response.text?.let { modelMessage ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages + ChatMessage(message = modelMessage, isFromUser = false)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(message = e.message ?: "Unknown error", isFromUser = false)
                    )
                }
            }
        }
    }

    data class ChatUiState(
        val messages: List<ChatMessage> = listOf()
    )

    data class ChatMessage(
        val message: String,
        val isFromUser: Boolean
    )
}