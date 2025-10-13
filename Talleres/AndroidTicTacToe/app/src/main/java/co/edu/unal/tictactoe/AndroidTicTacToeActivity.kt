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
import com.google.firebase.database.*

class AndroidTicTacToeActivity : AppCompatActivity(), BoardView.BoardTouchListener {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardView: BoardView
    private lateinit var mInfoTextView: TextView

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
    private var mComputerTurnRunnable: Runnable? = null
    private var mCurrentTurn = TicTacToeGame.HUMAN_PLAYER

    private lateinit var mPrefs: SharedPreferences

    private var database: FirebaseDatabase? = null
    private var gameRef: DatabaseReference? = null
    private var gameId: String? = null
    private var playerRole: Char? = null
    private var isOnline: Boolean = false
    private var gameValueEventListener: ValueEventListener? = null

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
        mHumanScoreTextView = findViewById(R.id.player1_score)
        mComputerScoreTextView = findViewById(R.id.player2_score)
        mTiesScoreTextView = findViewById(R.id.ties_score)

        mBoardView.setGame(mGame)
        mBoardView.setOnBoardTouchListener(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        // --- Restauración de estado en rotación ---
        if (savedInstanceState != null) {
            gameId = savedInstanceState.getString("GAME_ID")
            isOnline = savedInstanceState.getBoolean("IS_ONLINE", false)
            playerRole = savedInstanceState.getString("PLAYER_ROLE")?.firstOrNull()
            mHumanWins = savedInstanceState.getInt("mHumanWins", 0)
            mComputerWins = savedInstanceState.getInt("mComputerWins", 0)
            mTies = savedInstanceState.getInt("mTies", 0)
            mGameOver = savedInstanceState.getBoolean("mGameOver", false)
            mCurrentTurn = savedInstanceState.getString("currentTurn")?.firstOrNull() ?: TicTacToeGame.HUMAN_PLAYER

            val savedBoard = savedInstanceState.getCharArray("board")
            if (savedBoard != null && savedBoard.size == TicTacToeGame.BOARD_SIZE) {
                mGame.setBoardState(savedBoard)
            }
        } else {
            // Primer arranque: leer del intent
            gameId = intent.getStringExtra("GAME_ID")
            isOnline = intent.getBooleanExtra("IS_ONLINE", false)
            playerRole = intent.getCharExtra("PLAYER_ROLE", ' ')
        }

        // Dependiendo del modo
        if (isOnline) setupOnlineMode(savedInstanceState) else setupOfflineMode(savedInstanceState)
    }

    private fun setupOnlineMode(savedInstanceState: Bundle?) {
        // Etiquetas
        findViewById<TextView>(R.id.player1_label).text = "Player X"
        findViewById<TextView>(R.id.player2_label).text = "Player O"

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.menu.findItem(R.id.action_difficulty)?.isVisible = false
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_new_game -> {
                    startNewOnlineGame()
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

        // Restaurar scores (si vienen en savedInstanceState)
        displayScores()

        // Si tenemos gameId, inicializamos Firebase y el listener
        if (!gameId.isNullOrBlank()) {
            // Usa tu URL de RTDB correcta
            database = FirebaseDatabase.getInstance("https://tic-tac-toe-14c96-default-rtdb.firebaseio.com/")
            gameRef = database!!.getReference("games").child(gameId!!)
            // Mostrar código del juego
            findViewById<TextView>(R.id.game_code_text)?.text = "Código del juego: $gameId"
            // Desactivar tablero hasta que estado sea IN_PROGRESS
            mBoardView.isEnabled = false
            addGameEventListener()
        } else {
            // Si por alguna razón no hay gameId, mostramos mensaje claro
            mInfoTextView.text = "No se encontró ID de partida."
            mBoardView.isEnabled = false
        }

        // Si teníamos tablero guardado (rotación), dibujarlo
        mBoardView.invalidate()
    }

    private fun setupOfflineMode(savedInstanceState: Bundle?) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_new_game -> { startNewGame(); true }
                R.id.action_difficulty -> { @Suppress("DEPRECATION") showDialog(DIALOG_DIFFICULTY_ID); true }
                R.id.action_quit -> { @Suppress("DEPRECATION") showDialog(DIALOG_QUIT_ID); true }
                else -> false
            }
        }

        mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (savedInstanceState == null) {
            mHumanWins = mPrefs.getInt("mHumanWins", 0)
            mComputerWins = mPrefs.getInt("mComputerWins", 0)
            mTies = mPrefs.getInt("mTies", 0)
            val difficultyOrdinal = mPrefs.getInt("difficulty", TicTacToeGame.DifficultyLevel.Expert.ordinal)
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[difficultyOrdinal])
            startNewGame()
        } else {
            // Si hay savedInstanceState, ya restauramos valores en onCreate
        }
        displayScores()
    }

    private fun addGameEventListener() {
        // Remove previous listener (defensivo)
        gameValueEventListener?.let { gameRef?.removeEventListener(it) }

        gameValueEventListener = object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val state = snapshot.child("state").getValue(String::class.java) ?: "WAITING"

                if (state == "WAITING") {
                    // Mostrar código y esperar
                    mInfoTextView.text = "Esperando oponente... comparte: $gameId"
                    mBoardView.isEnabled = false
                    return
                }

                // Estado IN_PROGRESS -> sincronizar tablero y turno
                val boardList = snapshot.child("board").children.mapNotNull { it.getValue(String::class.java)?.firstOrNull() }
                if (boardList.size == TicTacToeGame.BOARD_SIZE) {
                    mGame.setBoardState(boardList.toCharArray())
                    mBoardView.invalidate()
                }

                val turn = snapshot.child("turn").getValue(String::class.java)?.firstOrNull()
                mCurrentTurn = turn ?: TicTacToeGame.HUMAN_PLAYER

                val winner = mGame.checkForWinner()
                if (winner != 0) {
                    handleEndOfTurn(winner)
                    return
                }

                if (mCurrentTurn == playerRole) {
                    mInfoTextView.text = getString(R.string.turn_human)
                    mBoardView.isEnabled = true
                } else {
                    mInfoTextView.text = "Turno del oponente..."
                    mBoardView.isEnabled = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AndroidTicTacToeActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        gameRef?.addValueEventListener(gameValueEventListener!!)
    }

    override fun onBoardTouched(location: Int) {
        if (mGameOver) return

        if (isOnline) {
            if (mCurrentTurn == playerRole && mGame.getBoardOccupant(location) == TicTacToeGame.OPEN_SPOT) {
                mHumanMediaPlayer?.start()
                gameRef?.child("board")?.child(location.toString())?.setValue(playerRole.toString())
                val nextTurn = if (playerRole == TicTacToeGame.HUMAN_PLAYER) TicTacToeGame.COMPUTER_PLAYER else TicTacToeGame.HUMAN_PLAYER
                gameRef?.child("turn")?.setValue(nextTurn.toString())
            }
        } else {
            if (mCurrentTurn == TicTacToeGame.HUMAN_PLAYER && mGame.setMove(TicTacToeGame.HUMAN_PLAYER, location)) {
                mCurrentTurn = TicTacToeGame.COMPUTER_PLAYER
                mBoardView.invalidate()
                mHumanMediaPlayer?.start()

                val winner = mGame.checkForWinner()
                if (winner == 0) {
                    mInfoTextView.text = getString(R.string.turn_computer)
                    mBoardView.isEnabled = false
                    scheduleComputerMove()
                } else handleEndOfTurn(winner)
            }
        }
    }

    private fun scheduleComputerMove() {
        mComputerTurnRunnable = Runnable {
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
        mHandler.postDelayed(mComputerTurnRunnable!!, 1000L)
    }

    private fun handleEndOfTurn(winner: Int) {
        if (mGameOver || winner == 0) return
        mGameOver = true
        mBoardView.isEnabled = false
        val message = when (winner) {
            1 -> getString(R.string.result_tie)
            2 -> if (isOnline) (if (playerRole == 'X') "You win!" else "You lose!") else getString(R.string.result_human_wins)
            3 -> if (isOnline) (if (playerRole == 'O') "You win!" else "You lose!") else getString(R.string.result_computer_wins)
            else -> ""
        }
        mInfoTextView.text = message
        when (winner) {
            1 -> mTies++
            2 -> mHumanWins++
            3 -> mComputerWins++
        }
        displayScores()
        if (!isOnline) mHandler.removeCallbacksAndMessages(null)
    }

    private fun displayScores() {
        mHumanScoreTextView.text = mHumanWins.toString()
        mComputerScoreTextView.text = mComputerWins.toString()
        mTiesScoreTextView.text = mTies.toString()
    }

    private fun startNewGame() {
        mHandler.removeCallbacksAndMessages(null)
        mGame.clearBoard()
        mBoardView.invalidate()
        mBoardView.isEnabled = true
        mGameOver = false
        mCurrentTurn = TicTacToeGame.HUMAN_PLAYER
        mInfoTextView.text = getString(R.string.first_human)
    }

    private fun startNewOnlineGame() {
        gameRef?.let { ref ->
            val newBoard = CharArray(TicTacToeGame.BOARD_SIZE) { TicTacToeGame.OPEN_SPOT }
            ref.child("board").setValue(newBoard.map { it.toString() })
            ref.child("turn").setValue(TicTacToeGame.HUMAN_PLAYER.toString())
            ref.child("state").setValue("IN_PROGRESS")
        }
        mGameOver = false
        mBoardView.isEnabled = true
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

    override fun onPause() {
        super.onPause()
        mHumanMediaPlayer?.release()
        mComputerMediaPlayer?.release()
        if (!isOnline) {
            mComputerTurnRunnable?.let { mHandler.removeCallbacks(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHumanMediaPlayer?.release()
        mComputerMediaPlayer?.release()
        // Remover listener si existe
        gameValueEventListener?.let { gameRef?.removeEventListener(it) }
        mHandler.removeCallbacksAndMessages(null)
    }

    // --- Guardar estado para rotación ---
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("mHumanWins", mHumanWins)
        outState.putInt("mComputerWins", mComputerWins)
        outState.putInt("mTies", mTies)
        outState.putCharArray("board", mGame.getBoardState())
        outState.putBoolean("mGameOver", mGameOver)
        outState.putString("info", mInfoTextView.text.toString())
        outState.putString("currentTurn", mCurrentTurn.toString())
        outState.putString("GAME_ID", gameId)
        outState.putString("PLAYER_ROLE", playerRole?.toString())
        outState.putBoolean("IS_ONLINE", isOnline)
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
                if(!isOnline) {
                    val editor = mPrefs.edit()
                    editor.putInt("mHumanWins", mHumanWins)
                    editor.putInt("mComputerWins", mComputerWins)
                    editor.putInt("mTies", mTies)
                    editor.apply()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int): Dialog {
        val dialog: Dialog?
        val builder = AlertDialog.Builder(this)

        when (id) {
            DIALOG_DIFFICULTY_ID -> {
                builder.setTitle(R.string.difficulty_choose)
                val levels = resources.getStringArray(R.array.difficulty_levels)
                val selected = mGame.getDifficultyLevel().ordinal

                builder.setSingleChoiceItems(levels, selected) { dialogInterface, i ->
                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[i])
                    dialogInterface.dismiss()
                    Toast.makeText(
                        applicationContext,
                        "Difficulty set to ${levels[i]}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog = builder.create()
            }

            DIALOG_QUIT_ID -> {
                builder.setMessage(R.string.quit_question)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> finish() }
                    .setNegativeButton(R.string.no, null)
                dialog = builder.create()
            }

            DIALOG_ABOUT_ID -> {
                val inflater = LayoutInflater.from(this)
                @SuppressLint("InflateParams")
                val view = inflater.inflate(R.layout.about_dialog, null)
                builder.setView(view)
                builder.setPositiveButton("OK", null)
                dialog = builder.create()
            }
            else -> dialog = null
        }

        return dialog ?: super.onCreateDialog(id)
    }
}
