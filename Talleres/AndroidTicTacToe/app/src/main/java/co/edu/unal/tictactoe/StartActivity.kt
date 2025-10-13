package co.edu.unal.tictactoe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val offlineButton: Button = findViewById(R.id.button_offline)
        offlineButton.setOnClickListener {
            val intent = Intent(this, AndroidTicTacToeActivity::class.java)
            // Add a flag to ensure the game activity runs in offline mode
            intent.putExtra("isOnline", false)
            startActivity(intent)
        }

        val onlineButton: Button = findViewById(R.id.button_online)
        onlineButton.setOnClickListener {
            val intent = Intent(this, LobbyActivity::class.java)
            startActivity(intent)
        }
    }
}
