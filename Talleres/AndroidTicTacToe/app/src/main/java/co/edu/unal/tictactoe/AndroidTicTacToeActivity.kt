package co.edu.unal.tictactoe

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardView: BoardView
    private lateinit var mInfoTextView: TextView

    // Variables para los puntajes
    private var mHumanWins = 0
    private var mComputerWins = 0
    private var mTies = 0
    private lateinit var mHumanScoreTextView: TextView
    private lateinit var mComputerScoreTextView: TextView
    private lateinit var mTiesScoreTextView: TextView

    private var mHumanMediaPlayer: MediaPlayer? = null
    private var mComputerMediaPlayer: MediaPlayer? = null

    private var mGameOver = false
    private val mHandler = Handler(Looper.getMainLooper())
    private var mComputerTurnRunnable: Runnable? = null // Para cancelar el postDelayed

    private var mCurrentTurn = TicTacToeGame.HUMAN_PLAYER // H para Humano, C para Computadora

    private lateinit var mPrefs: SharedPreferences

    companion object {
        const val DIALOG_DIFFICULTY_ID = 0
        const val DIALOG_QUIT_ID = 1
        const val DIALOG_ABOUT_ID = 2
        const val PREFS_NAME = "ttt_prefs"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mGame = TicTacToeGame()
        mBoardView = findViewById(R.id.board_view)
        mInfoTextView = findViewById(R.id.information)

        mHumanScoreTextView = findViewById(R.id.human_score) ?: TextView(this)
        mComputerScoreTextView = findViewById(R.id.computer_score) ?: TextView(this)
        mTiesScoreTextView = findViewById(R.id.ties_score) ?: TextView(this)

        mBoardView.setGame(mGame)
        mBoardView.setOnBoardTouchListener(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_new_game -> {
                    startNewGame()
                    true
                }
                R.id.action_difficulty -> {
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

        mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        mHumanWins = mPrefs.getInt("mHumanWins", 0)
        mComputerWins = mPrefs.getInt("mComputerWins", 0)
        mTies = mPrefs.getInt("mTies", 0)
        val difficultyOrdinal = mPrefs.getInt("difficulty", TicTacToeGame.DifficultyLevel.Expert.ordinal)
        mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[difficultyOrdinal])

        if (savedInstanceState != null) {
            mGame.setBoardState(savedInstanceState.getCharArray("board") ?: CharArray(TicTacToeGame.BOARD_SIZE))
            mGameOver = savedInstanceState.getBoolean("mGameOver")
            mInfoTextView.text = savedInstanceState.getString("info")
            mHumanWins = savedInstanceState.getInt("mHumanWins")
            mComputerWins = savedInstanceState.getInt("mComputerWins")
            mTies = savedInstanceState.getInt("mTies")
            mCurrentTurn = savedInstanceState.getChar("currentTurn", TicTacToeGame.HUMAN_PLAYER)
            val savedDifficultyOrdinal = savedInstanceState.getInt("difficulty", mGame.getDifficultyLevel().ordinal)
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[savedDifficultyOrdinal])

            mBoardView.invalidate()
            if (!mGameOver && mCurrentTurn == TicTacToeGame.COMPUTER_PLAYER) {
                mBoardView.isEnabled = false
                scheduleComputerMove()
            }
        } else {
            startNewGame()
        }
        displayScores()
    }

    override fun onResume() {
        super.onResume()
        try {
            mHumanMediaPlayer = MediaPlayer.create(applicationContext, R.raw.soundx)
            mComputerMediaPlayer = MediaPlayer.create(applicationContext, R.raw.soundo)
        } catch (e: Exception) {
            Log.e("TicTacToeActivity", "Error creating MediaPlayers", e)
        }
    }

    private fun startNewGame() {
        mHandler.removeCallbacksAndMessages(null)
        mGame.clearBoard()
        mBoardView.invalidate()
        mBoardView.isEnabled = true
        mGameOver = false
        mCurrentTurn = TicTacToeGame.HUMAN_PLAYER
        mInfoTextView.text = getString(R.string.first_human)
        displayScores()
    }

    override fun onBoardTouched(location: Int) {
        if (mCurrentTurn != TicTacToeGame.HUMAN_PLAYER || mGameOver) {
            return
        }

        if (mGame.setMove(TicTacToeGame.HUMAN_PLAYER, location)) {
            mCurrentTurn = TicTacToeGame.COMPUTER_PLAYER
            mBoardView.invalidate()
            mHumanMediaPlayer?.start()

            val winner = mGame.checkForWinner()
            if (winner == 0) {
                mInfoTextView.text = getString(R.string.turn_computer)
                mBoardView.isEnabled = false
                scheduleComputerMove()
            } else {
                handleEndOfTurn(winner)
            }
        }
    }

    private fun scheduleComputerMove() {
        mComputerTurnRunnable = Runnable {
            if (!mGameOver) {
                val move = mGame.getComputerMove()
                if (move != -1) {
                    mGame.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                    mBoardView.invalidate()
                    mComputerMediaPlayer?.start()
                }
                val currentWinner = mGame.checkForWinner()
                handleEndOfTurn(currentWinner)
                if (!mGameOver) {
                    mCurrentTurn = TicTacToeGame.HUMAN_PLAYER
                    mInfoTextView.text = getString(R.string.turn_human)
                    mBoardView.isEnabled = true
                }
            }
            mComputerTurnRunnable = null
        }
        mHandler.postDelayed(mComputerTurnRunnable!!, 1000L)
    }

    private fun handleEndOfTurn(winner: Int) {
        when (winner) {
            0 -> return
            1 -> {
                mInfoTextView.text = getString(R.string.result_tie)
                mTies++
                mGameOver = true
            }
            2 -> {
                mInfoTextView.text = getString(R.string.result_human_wins)
                mHumanWins++
                mGameOver = true
            }
            3 -> {
                mInfoTextView.text = getString(R.string.result_computer_wins)
                mComputerWins++
                mGameOver = true
            }
        }

        displayScores()

        if (mGameOver) {
            mBoardView.isEnabled = false
            mBoardView.invalidate()
            mHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun displayScores() {
        if (::mHumanScoreTextView.isInitialized && mHumanScoreTextView.parent != null) {
            mHumanScoreTextView.text = mHumanWins.toString()
        }
        if (::mComputerScoreTextView.isInitialized && mComputerScoreTextView.parent != null) {
            mComputerScoreTextView.text = mComputerWins.toString()
        }
        if (::mTiesScoreTextView.isInitialized && mTiesScoreTextView.parent != null) {
            mTiesScoreTextView.text = mTies.toString()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharArray("board", mGame.getBoardState())
        outState.putBoolean("mGameOver", mGameOver)
        outState.putString("info", mInfoTextView.text.toString())
        outState.putInt("mHumanWins", mHumanWins)
        outState.putInt("mComputerWins", mComputerWins)
        outState.putInt("mTies", mTies)
        outState.putChar("currentTurn", mCurrentTurn)
        outState.putInt("difficulty", mGame.getDifficultyLevel().ordinal)
    }

    override fun onStop() {
        super.onStop()
        val editor = mPrefs.edit()
        editor.putInt("mHumanWins", mHumanWins)
        editor.putInt("mComputerWins", mComputerWins)
        editor.putInt("mTies", mTies)
        editor.putInt("difficulty", mGame.getDifficultyLevel().ordinal)
        editor.apply()
    }

    override fun onPause() {
        super.onPause()
        mHumanMediaPlayer?.release()
        mComputerMediaPlayer?.release()
        mComputerTurnRunnable?.let { mHandler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHumanMediaPlayer?.release()
        mComputerMediaPlayer?.release()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                @Suppress("DEPRECATION")
                showDialog(DIALOG_ABOUT_ID)
                return true
            }
            R.id.action_reset_scores -> {
                mHumanWins = 0
                mComputerWins = 0
                mTies = 0
                displayScores()
                val editor = mPrefs.edit()
                editor.putInt("mHumanWins", mHumanWins)
                editor.putInt("mComputerWins", mComputerWins)
                editor.putInt("mTies", mTies)
                editor.apply()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
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
                    mPrefs.edit().putInt("difficulty", mGame.getDifficultyLevel().ordinal).apply()
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
                @SuppressLint("InflateParams")
                val aboutView: View = LayoutInflater.from(this).inflate(R.layout.about_dialog, null)
                builder.setView(aboutView)
                builder.setTitle(R.string.about_dialog_title)
                builder.setPositiveButton("OK", null)
                dialog = builder.create()
            }
        }
        return dialog ?: super.onCreateDialog(id)
    }
}
