package co.edu.unal.tictactoe

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class AndroidTicTacToeActivity : AppCompatActivity(), BoardView.BoardTouchListener {

    private var mProcessingComputerTurn = false

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardView: BoardView
    private lateinit var mInfoTextView: TextView

    private var mHumanMediaPlayer: MediaPlayer? = null
    private var mComputerMediaPlayer: MediaPlayer? = null

    private var mGameOver = false
    private val mHandler = Handler(Looper.getMainLooper())

    companion object {
        const val DIALOG_DIFFICULTY_ID = 0
        const val DIALOG_QUIT_ID = 1
        const val DIALOG_ABOUT_ID = 2
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mGame = TicTacToeGame() // Assuming TicTacToeGame is in the same package or imported
        mInfoTextView = findViewById(R.id.information)
        mBoardView = findViewById(R.id.board_view)
        mBoardView.setGame(mGame)
        mBoardView.setOnBoardTouchListener(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name) // Set title

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_new_game -> {
                    startNewGame()
                    true
                }
                R.id.action_difficulty -> {
                    // Suppress deprecation for showDialog for this tutorial's scope
                    @Suppress("DEPRECATION")
                    showDialog(DIALOG_DIFFICULTY_ID)
                    true
                }
                R.id.action_quit -> {
                    @Suppress("DEPRECATION")
                    showDialog(DIALOG_QUIT_ID)
                    true
                }
                else -> false
            }
        }
        startNewGame()
    }

    override fun onResume() {
        super.onResume()
        try {
            if (mHumanMediaPlayer == null) {
                mHumanMediaPlayer = MediaPlayer.create(applicationContext, R.raw.soundx)
            }
            if (mComputerMediaPlayer == null) {
                mComputerMediaPlayer = MediaPlayer.create(applicationContext, R.raw.soundo)
            }
        } catch (e: Exception) {
            // Log or handle error if sound files are missing
            Toast.makeText(this, "Error loading sound files", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        mHumanMediaPlayer?.release()
        mComputerMediaPlayer?.release()
        mHumanMediaPlayer = null
        mComputerMediaPlayer = null
    }

    private fun startNewGame() {
        mProcessingComputerTurn = false
        mGame.clearBoard()
        mBoardView.invalidate()
        mGameOver = false
        mInfoTextView.text = getString(R.string.first_human)
    }

    override fun onMoveMade() { // Called from BoardView after a human move
        if (mProcessingComputerTurn || mGameOver) return // Guard against multiple calls or moves when game is over

        mHumanMediaPlayer?.start()
        var winner = mGame.checkForWinner()

        if (winner == 0) { // No winner yet, computer's turn
            mInfoTextView.text = getString(R.string.turn_computer)
            mProcessingComputerTurn = true // Prevent human moves during computer's turn processing

            mHandler.postDelayed({
                if (!mGameOver) { // Check again in case game ended for some other reason (should not happen here)
                    val move = mGame.getComputerMove()
                    if (move != -1) {
                        mGame.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                        mBoardView.invalidate()
                        mComputerMediaPlayer?.start()
                    }
                    val currentWinner = mGame.checkForWinner() // Check winner again after computer's move
                    handleEndOfTurn(currentWinner) // This will set mGameOver if needed
                    // Only allow human to play again if the game is NOT over
                    if (!mGameOver) {
                        mProcessingComputerTurn = false
                    }
                }
            }, 1000L) // 1000ms (1 second) delay
        } else {
            handleEndOfTurn(winner)
        }
    }


    private fun handleEndOfTurn(winner: Int) {
        when (winner) {
            0 -> mInfoTextView.text = getString(R.string.turn_human)
            1 -> {
                mInfoTextView.text = getString(R.string.result_tie)
                mGameOver = true
            }
            2 -> { // Human wins
                mInfoTextView.text = getString(R.string.result_human_wins)
                mGameOver = true
            }
            3 -> { // Computer wins
                mInfoTextView.text = getString(R.string.result_computer_wins)
                mGameOver = true
            }
        }
        if (mGameOver) {
            mBoardView.invalidate() // To ensure board doesn't accept more touches if logic is there
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                @Suppress("DEPRECATION")
                showDialog(DIALOG_ABOUT_ID)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java. Using for consistency with original tutorial structure.")
    override fun onCreateDialog(id: Int): Dialog {
        var dialog: Dialog? = null
        val builder = AlertDialog.Builder(this)

        when (id) {
            DIALOG_DIFFICULTY_ID -> {
                builder.setTitle(R.string.difficulty_choose)
                val levels = TicTacToeGame.DifficultyLevel.values()
                val levelStrings = levels.map { getString(resources.getIdentifier("difficulty_${it.name.lowercase()}", "string", packageName)) }.toTypedArray()
                val selected = mGame.getDifficultyLevel().ordinal

                builder.setSingleChoiceItems(levelStrings, selected) { dialogInterface, which ->
                    mGame.setDifficultyLevel(levels[which])
                    Toast.makeText(applicationContext, levelStrings[which], Toast.LENGTH_SHORT).show()
                    dialogInterface.dismiss()
                    startNewGame()
                }
                dialog = builder.create()
            }
            DIALOG_QUIT_ID -> {
                builder.setMessage(R.string.quit_question)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> this.finish() }
                    .setNegativeButton(R.string.no, null)
                dialog = builder.create()
            }
            DIALOG_ABOUT_ID -> {
                @SuppressLint("InflateParams") // OK for AlertDialogs
                val aboutView: View = LayoutInflater.from(this).inflate(R.layout.about_dialog, null)
                builder.setView(aboutView)
                builder.setTitle(R.string.about_dialog_title)
                builder.setPositiveButton(android.R.string.ok, null)
                dialog = builder.create()
            }
        }
        return dialog ?: super.onCreateDialog(id) // Fallback to super if dialog is null
    }
}
