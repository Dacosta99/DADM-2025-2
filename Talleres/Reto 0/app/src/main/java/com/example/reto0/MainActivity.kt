package com.example.reto0

// Importaciones necesarias para el funcionamiento de Jetpack Compose y la actividad principal
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp // Import para usar tamaños de fuente
import com.example.reto0.ui.theme.Reto0Theme

// Clase principal de la aplicación, que hereda de ComponentActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita que la app se dibuje detrás de las barras del sistema (status bar, navigation bar)
        enableEdgeToEdge()

        // Define el contenido de la actividad utilizando Jetpack Compose
        setContent {
            // Aplica el tema definido en la app (colores, tipografía, estilos, etc.)
            Reto0Theme {
                // Scaffold es un componente de Compose que provee una estructura básica
                // para pantallas (barra superior, barra inferior, floating action button, etc.)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Se llama a la función composable Greeting para mostrar un texto en pantalla
                    Greeting(
                        name = "World",
                        // Se pasa el padding interno que genera el Scaffold para respetar
                        // los espacios de elementos como la barra de estado o navegación
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Función composable que muestra un saludo en pantalla
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Hello $name!", // Texto dinámico que incluye el nombre pasado como parámetro
            fontSize = 32.sp
        )
    }
}

// Función de vista previa que permite ver cómo se vería el composable en el editor de Android Studio
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    // Se aplica el tema para que la vista previa tenga el mismo estilo que la app real
    Reto0Theme {
        Greeting("Android") // Llama al composable Greeting con el nombre "Android"
    }
}
