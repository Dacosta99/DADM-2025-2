package co.edu.unal.tictactoe

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
// import android.view.Menu // Removed
// import android.view.MenuInflater // Removed
// import android.view.MenuItem // Removed
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AndroidTicTacToeActivity : AppCompatActivity() {

    private lateinit var mBoardButtons: Array<Button>
    private lateinit var mInfoTextView: TextView
    private lateinit var mGame: TicTacToeGame
    private var mGameOver = false

    // Dialog identifiers
    companion object {
        const val DIALOG_DIFFICULTY_ID = 0
        const val DIALOG_QUIT_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBoardButtons = Array(TicTacToeGame.BOARD_SIZE) { i ->
            val id = resources.getIdentifier("button_${i + 1}", "id", packageName)
            findViewById(id)
        }

        mInfoTextView = findViewById(R.id.information)
        mGame = TicTacToeGame()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_new_game -> {
                    startNewGame()
                    true
                }
                R.id.action_difficulty -> {
                    showDialog(DIALOG_DIFFICULTY_ID)
                    true
                }
                R.id.action_quit -> {
                    showDialog(DIALOG_QUIT_ID)
                    true
                }
                else -> false
            }
        }

        startNewGame()
    }

    private fun startNewGame() {
        mGame.clearBoard()
        mGameOver = false

        for (i in mBoardButtons.indices) {
            mBoardButtons[i].text = ""
            mBoardButtons[i].isEnabled = true
            mBoardButtons[i].setOnClickListener(ButtonClickListener(i))
        }

        mInfoTextView.text = getString(R.string.first_human)
    }

    private inner class ButtonClickListener(private val location: Int) : View.OnClickListener {
        override fun onClick(view: View?) {
            if (!mGameOver && mBoardButtons[location].isEnabled) {
                setMove(TicTacToeGame.HUMAN_PLAYER, location)

                var winner = mGame.checkForWinner()
                if (winner == 0) {
                    mInfoTextView.text = getString(R.string.turn_computer)
                    val move = mGame.getComputerMove()
                    if (move >= 0) setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                    winner = mGame.checkForWinner()
                }

                when (winner) {
                    0 -> mInfoTextView.text = getString(R.string.turn_human)
                    1 -> {
                        mInfoTextView.text = getString(R.string.result_tie)
                        mGameOver = true
                    }
                    2 -> {
                        mInfoTextView.text = getString(R.string.result_human_wins)
                        mGameOver = true
                    }
                    3 -> {
                        mInfoTextView.text = getString(R.string.result_computer_wins)
                        mGameOver = true
                    }
                }
            }
        }
    }

    private fun setMove(player: Char, location: Int) {
        mGame.setMove(player, location)
        val btn = mBoardButtons[location]
        btn.isEnabled = false
        btn.text = player.toString()
        if (player == TicTacToeGame.HUMAN_PLAYER) btn.setTextColor(android.graphics.Color.rgb(0, 200, 0))
        else btn.setTextColor(android.graphics.Color.rgb(200, 0, 0))
    }

    // Dialog creation (aligned with tutorial; showDialog calls this method)
    override fun onCreateDialog(id: Int): Dialog {
        var dialog: Dialog? = null
        val builder = AlertDialog.Builder(this)

        when (id) {
            DIALOG_DIFFICULTY_ID -> {
                builder.setTitle(R.string.difficulty_choose)

                val levels = arrayOf(
                    getString(R.string.difficulty_easy),
                    getString(R.string.difficulty_harder),
                    getString(R.string.difficulty_expert)
                )

                // selected index depends on current game difficulty
                val selected = when (mGame.getDifficultyLevel()) {
                    TicTacToeGame.DifficultyLevel.Easy -> 0
                    TicTacToeGame.DifficultyLevel.Harder -> 1
                    TicTacToeGame.DifficultyLevel.Expert -> 2
                }

                builder.setSingleChoiceItems(levels, selected) { dialogInterface, which ->
                    // Set the difficulty on selection
                    val chosen = when (which) {
                        0 -> TicTacToeGame.DifficultyLevel.Easy
                        1 -> TicTacToeGame.DifficultyLevel.Harder
                        else -> TicTacToeGame.DifficultyLevel.Expert
                    }
                    mGame.setDifficultyLevel(chosen)
                    Toast.makeText(applicationContext, levels[which], Toast.LENGTH_SHORT).show()
                    dialogInterface.dismiss()
                }

                dialog = builder.create()
            }

            DIALOG_QUIT_ID -> {
                builder.setMessage(R.string.quit_question)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        this.finish()
                    }
                    .setNegativeButton(R.string.no, null)
                dialog = builder.create()
            }
        }

        return dialog ?: super.onCreateDialog(id)
    }

    // Optional About dialog using a custom layout
    private fun showAboutDialog() {
        val inflater = LayoutInflater.from(this)
        val aboutView: View = inflater.inflate(R.layout.about_dialog, null)
        val aboutBuilder = AlertDialog.Builder(this)
            .setView(aboutView)
            .setPositiveButton("OK", null)
        aboutBuilder.create().show()
    }
}