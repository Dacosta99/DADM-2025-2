package co.edu.unal.reto11ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.edu.unal.reto11ia.ui.theme.Reto11IATheme

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Reto11IATheme {
                val uiState by chatViewModel.uiState.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        uiState = uiState,
                        onSendMessage = { message -> chatViewModel.sendMessage(message) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    uiState: ChatViewModel.ChatUiState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var userMessage by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(uiState.messages.reversed()) { chatMessage ->
                ChatMessageView(chatMessage)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    onSendMessage(userMessage)
                    userMessage = ""
                },
                enabled = userMessage.isNotBlank()
            ) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun ChatMessageView(chatMessage: ChatViewModel.ChatMessage) {
    val isUserMessage = chatMessage.isFromUser
    val horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isUserMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = bubbleColor
        ) {
            Text(
                text = chatMessage.message,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    Reto11IATheme {
        val messages = listOf(
            ChatViewModel.ChatMessage("Hello!", isFromUser = false),
            ChatViewModel.ChatMessage("Hi! How are you?", isFromUser = true),
            ChatViewModel.ChatMessage(
                "I'm doing great, thanks for asking! How can I help you today?",
                isFromUser = false
            )
        )
        val uiState = ChatViewModel.ChatUiState(messages)
        ChatScreen(uiState = uiState, onSendMessage = {})
    }
}
