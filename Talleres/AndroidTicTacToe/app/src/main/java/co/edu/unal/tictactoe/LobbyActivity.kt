package co.edu.unal.tictactoe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class LobbyActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        database = FirebaseDatabase.getInstance("https://tic-tac-toe-14c96-default-rtdb.firebaseio.com")

        val createGameButton: Button = findViewById(R.id.create_game_button)
        val joinGameButton: Button = findViewById(R.id.join_game_button)
        val gameIdInput: EditText = findViewById(R.id.game_id_input)

        createGameButton.setOnClickListener {
            createNewGame()
        }

        joinGameButton.setOnClickListener {
            val gameId = gameIdInput.text.toString()
            if (gameId.isNotBlank()) {
                joinGame(gameId)
            } else {
                Toast.makeText(this, "Please enter a Game ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNewGame() {
        val gameId = (10000..99999).random().toString()
        val gameRef = database.getReference("games").child(gameId)

        // Create a basic game object - USE STRINGS for Firebase
        val game = mapOf(
            "board" to List(TicTacToeGame.BOARD_SIZE) { TicTacToeGame.OPEN_SPOT.toString() },
            "turn" to TicTacToeGame.HUMAN_PLAYER.toString(),
            "state" to "WAITING" // Player 1 is waiting for Player 2
        )

        gameRef.setValue(game).addOnSuccessListener {
            val intent = Intent(this, AndroidTicTacToeActivity::class.java).apply {
                putExtra("GAME_ID", gameId)
                putExtra("IS_ONLINE", true)
                putExtra("PLAYER_ROLE", TicTacToeGame.HUMAN_PLAYER) // Player 1 is 'X'
            }
            startActivity(intent)
        }.addOnFailureListener { e ->
            Log.e("LobbyActivity", "Failed to create game.", e)
            Toast.makeText(this, "Failed to create game: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun joinGame(gameId: String) {
        val gameRef = database.getReference("games").child(gameId)
        gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Game exists, check state
                    val gameState = snapshot.child("state").getValue(String::class.java)
                    if (gameState == "WAITING"){
                        gameRef.child("state").setValue("IN_PROGRESS")
                        val intent = Intent(this@LobbyActivity, AndroidTicTacToeActivity::class.java).apply {
                            putExtra("GAME_ID", gameId)
                            putExtra("IS_ONLINE", true)
                            putExtra("PLAYER_ROLE", TicTacToeGame.COMPUTER_PLAYER) // Player 2 is 'O'
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@LobbyActivity, "Game is already in progress.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LobbyActivity, "Game ID not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LobbyActivity, "Failed to check game ID: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
