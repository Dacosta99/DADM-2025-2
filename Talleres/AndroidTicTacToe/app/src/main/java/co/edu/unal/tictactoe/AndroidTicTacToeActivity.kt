package co.edu.unal.tictactoe

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AndroidTicTacToeActivity : AppCompatActivity() {

    private lateinit var mBoardButtons: Array<Button>
    private lateinit var mInfoTextView: TextView
    private lateinit var mGame: TicTacToeGame
    private var mGameOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBoardButtons = Array(TicTacToeGame.BOARD_SIZE) { i ->
            val id = resources.getIdentifier("button_${i + 1}", "id", packageName)
            findViewById(id)
        }

        mInfoTextView = findViewById(R.id.information)
        mGame = TicTacToeGame()

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
        if (player == TicTacToeGame.HUMAN_PLAYER) btn.setTextColor(Color.rgb(0, 200, 0))
        else btn.setTextColor(Color.rgb(200, 0, 0))
    }

    // Options menu to start a new game
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add("New Game")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startNewGame()
        return true
    }
}


