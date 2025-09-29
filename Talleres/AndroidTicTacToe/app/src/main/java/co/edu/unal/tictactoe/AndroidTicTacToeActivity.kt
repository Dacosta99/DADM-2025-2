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
    private var mProcessingComputerTurn = false // Para evitar movimientos mientras la CPU piensa

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

        // Inicializar TextViews de puntajes (pueden no existir en layout portrait)
        // Es mejor verificar si existen antes de usarlos para evitar NullPointerExceptions
        // si el layout actual no los tiene (ej. portrait vs landscape)
        mHumanScoreTextView = findViewById(R.id.human_score) ?: TextView(this) // Fallback si no existe
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
                    startNewGame(true) // true para indicar que es un nuevo juego iniciado por el usuario
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
        // Recuperar puntajes y dificultad persistidos
        mHumanWins = mPrefs.getInt("mHumanWins", 0)
        mComputerWins = mPrefs.getInt("mComputerWins", 0)
        mTies = mPrefs.getInt("mTies", 0)
        val difficultyOrdinal = mPrefs.getInt("difficulty", TicTacToeGame.DifficultyLevel.Expert.ordinal)
        mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[difficultyOrdinal])

        if (savedInstanceState != null) {
            // Restaurar estado de la instancia (rotación)
            mGame.setBoardState(savedInstanceState.getCharArray("board") ?: CharArray(TicTacToeGame.BOARD_SIZE))
            mGameOver = savedInstanceState.getBoolean("mGameOver")
            mInfoTextView.text = savedInstanceState.getString("info")
            // Los puntajes se restauran directamente del Bundle, no de SharedPreferences aquí,
            // para reflejar el estado exacto *antes* de la rotación, no el último guardado en onStop.
            mHumanWins = savedInstanceState.getInt("mHumanWins")
            mComputerWins = savedInstanceState.getInt("mComputerWins")
            mTies = savedInstanceState.getInt("mTies")
            mCurrentTurn = savedInstanceState.getChar("currentTurn", TicTacToeGame.HUMAN_PLAYER)
            // La dificultad ya fue restaurada de mPrefs, pero el Bundle tiene prioridad si existe
            val savedDifficultyOrdinal = savedInstanceState.getInt("difficulty", mGame.getDifficultyLevel().ordinal)
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[savedDifficultyOrdinal])

            mBoardView.invalidate() // Actualizar el tablero visualmente
            if (!mGameOver && mCurrentTurn == TicTacToeGame.COMPUTER_PLAYER && !mProcessingComputerTurn) {
                // Si era turno de la CPU y no se estaba procesando, forzar jugada
                // El mensaje de "Turno de Android" ya debería estar si fue guardado
                mProcessingComputerTurn = true
                scheduleComputerMove()
            }
        } else {
            // Si no hay estado guardado (primera ejecución o después de cerrar), iniciar nuevo juego
            startNewGame(false) // false para no resetear puntajes de SharedPreferences
        }
        displayScores()
    }

    override fun onResume() {
        super.onResume()
        Log.d("TicTacToeActivity", "onResume: Intentando inicializar MediaPlayers.")
        try {
            // Usando los nombres de archivo correctos que especificaste
            mHumanMediaPlayer = MediaPlayer.create(applicationContext, R.raw.soundx)
            mComputerMediaPlayer = MediaPlayer.create(applicationContext, R.raw.soundo)

            if (mHumanMediaPlayer == null) {
                Log.e("TicTacToeActivity", "onResume: Falló la creación de mHumanMediaPlayer (R.raw.soundx). Revisa que el archivo exista en res/raw y no esté corrupto.")
            } else {
                Log.d("TicTacToeActivity", "onResume: mHumanMediaPlayer (R.raw.soundx) creado exitosamente.")
                mHumanMediaPlayer?.setOnErrorListener { mp, what, extra ->
                    Log.e("TicTacToeActivity", "Error en mHumanMediaPlayer: what=$what, extra=$extra")
                    true // true si el error fue manejado
                }
            }

            if (mComputerMediaPlayer == null) {
                Log.e("TicTacToeActivity", "onResume: Falló la creación de mComputerMediaPlayer (R.raw.soundo). Revisa que el archivo exista en res/raw y no esté corrupto.")
            } else {
                Log.d("TicTacToeActivity", "onResume: mComputerMediaPlayer (R.raw.soundo) creado exitosamente.")
                mComputerMediaPlayer?.setOnErrorListener { mp, what, extra ->
                    Log.e("TicTacToeActivity", "Error en mComputerMediaPlayer: what=$what, extra=$extra")
                    true // true si el error fue manejado
                }
            }
        } catch (e: Exception) {
            Log.e("TicTacToeActivity", "onResume: Excepción al crear MediaPlayers: ${e.message}", e)
            // mHumanMediaPlayer y mComputerMediaPlayer permanecerán null si hay una excepción
        }
    }

    private fun startNewGame(resetScoresFromMenuOrNewGameButton: Boolean) {
        mHandler.removeCallbacksAndMessages(null) // Cancelar cualquier postDelayed pendiente
        mProcessingComputerTurn = false
        mGame.clearBoard()
        mBoardView.invalidate()
        mGameOver = false
        mCurrentTurn = TicTacToeGame.HUMAN_PLAYER
        mInfoTextView.text = getString(R.string.first_human)
        if (resetScoresFromMenuOrNewGameButton) {
             // No reseteamos los contadores mHumanWins, etc. aquí, ya que se leen de SharedPreferences
             // o se manejan con el botón Reset Scores.
        }
        displayScores() // Asegurar que los puntajes (posiblemente de SharedPreferences) se muestren
    }

    override fun onMoveMade() {
        if (mProcessingComputerTurn || mGameOver || mCurrentTurn == TicTacToeGame.COMPUTER_PLAYER) return

        mHumanMediaPlayer?.start()
        var winner = mGame.checkForWinner()
        mCurrentTurn = TicTacToeGame.COMPUTER_PLAYER // Turno de la CPU después del humano

        if (winner == 0) {
            mInfoTextView.text = getString(R.string.turn_computer)
            mProcessingComputerTurn = true
            scheduleComputerMove()
        } else {
            handleEndOfTurn(winner)
            mCurrentTurn = TicTacToeGame.HUMAN_PLAYER // El juego terminó, el turno vuelve a ser del humano para el próximo juego
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
                }
            }
            mProcessingComputerTurn = false
            mComputerTurnRunnable = null // Limpiar runnable
        }
        mHandler.postDelayed(mComputerTurnRunnable!!, 1000L)
    }

    private fun handleEndOfTurn(winner: Int) {
        when (winner) {
            0 -> { /* No hacer nada aquí, el turno se gestiona en onMoveMade/scheduleComputerMove */ }
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
            mBoardView.invalidate()
            mCurrentTurn = TicTacToeGame.HUMAN_PLAYER // Listo para el próximo juego
            mProcessingComputerTurn = false // Detener cualquier procesamiento de la CPU
            mHandler.removeCallbacksAndMessages(null) // Cancelar postDelayed si el juego termina
        }
    }

    private fun displayScores() {
        // Verificar si los TextViews de puntaje están disponibles (p.ej. en layout landscape)
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
        // Guardar puntajes y dificultad en SharedPreferences
        val editor = mPrefs.edit()
        editor.putInt("mHumanWins", mHumanWins)
        editor.putInt("mComputerWins", mComputerWins)
        editor.putInt("mTies", mTies)
        editor.putInt("difficulty", mGame.getDifficultyLevel().ordinal)
        editor.apply()
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("TicTacToeActivity", "onPause: Liberando MediaPlayers.")
        try {
            mHumanMediaPlayer?.release()
            mComputerMediaPlayer?.release()
        } catch (e: IllegalStateException) {
            Log.e("TicTacToeActivity", "onPause: IllegalStateException al liberar MediaPlayers: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("TicTacToeActivity", "onPause: Excepción general al liberar MediaPlayers: ${e.message}", e)
        } finally {
            mHumanMediaPlayer = null
            mComputerMediaPlayer = null
        }

        // Cancelar el runnable de la CPU si está pendiente para evitar que se ejecute mientras la actividad está pausada
        mComputerTurnRunnable?.let {
            mHandler.removeCallbacks(it)
            Log.d("TicTacToeActivity", "onPause: Callbacks para mComputerTurnRunnable eliminados.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TicTacToeActivity", "onDestroy: Liberación final de MediaPlayers y limpieza del Handler.")
        try {
            // Aseguramos la liberación por si onPause no se ejecutó o falló
            mHumanMediaPlayer?.release()
            mComputerMediaPlayer?.release()
        } catch (e: IllegalStateException) {
            Log.e("TicTacToeActivity", "onDestroy: IllegalStateException en liberación final de MediaPlayers: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("TicTacToeActivity", "onDestroy: Excepción general en liberación final de MediaPlayers: ${e.message}", e)
        } finally {
            mHumanMediaPlayer = null
            mComputerMediaPlayer = null
        }
        
        // Esta parte es importante para los Handlers, la mantenemos
        mHandler.removeCallbacksAndMessages(null)
        Log.d("TicTacToeActivity", "onDestroy: Todos los callbacks y mensajes del Handler eliminados.")
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
                // Actualizar SharedPreferences también
                val editor = mPrefs.edit()
                editor.putInt("mHumanWins", mHumanWins)
                editor.putInt("mComputerWins", mComputerWins)
                editor.putInt("mTies", mTies)
                editor.apply()
                // Opcional: Iniciar un nuevo juego después de resetear puntajes
                // startNewGame(false)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
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
                    // Guardar nueva dificultad inmediatamente en SharedPreferences
                    mPrefs.edit().putInt("difficulty", mGame.getDifficultyLevel().ordinal).apply()
                    Toast.makeText(applicationContext, levelStrings[which], Toast.LENGTH_SHORT).show()
                    dialogInterface.dismiss()
                    startNewGame(false) // No resetear puntajes al cambiar dificultad
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
                builder.setPositiveButton(android.R.string.ok, null)
                dialog = builder.create()
            }
        }
        return dialog ?: super.onCreateDialog(id)
    }
}
