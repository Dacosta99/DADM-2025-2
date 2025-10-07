package co.edu.unal.tictactoe

import kotlin.random.Random

class TicTacToeGame {
    companion object {
        const val BOARD_SIZE_SIDE = 3
        const val BOARD_SIZE = BOARD_SIZE_SIDE * BOARD_SIZE_SIDE
        const val OPEN_SPOT: Char = ' '
        const val HUMAN_PLAYER: Char = 'X'
        const val COMPUTER_PLAYER: Char = 'O'
    }

    // The computer's difficulty levels
    enum class DifficultyLevel { Easy, Harder, Expert }

    // Current difficulty level (default Expert)
    private var mDifficultyLevel: DifficultyLevel = DifficultyLevel.Expert

    fun getDifficultyLevel(): DifficultyLevel = mDifficultyLevel
    fun setDifficultyLevel(difficultyLevel: DifficultyLevel) {
        mDifficultyLevel = difficultyLevel
    }

    private var mBoard = CharArray(BOARD_SIZE) { OPEN_SPOT } // mBoard ahora es var
    private val mRand = Random(System.currentTimeMillis())

    fun clearBoard() {
        for (i in 0 until BOARD_SIZE) mBoard[i] = OPEN_SPOT
    }

    fun setMove(player: Char, location: Int): Boolean {
        if (location in 0 until BOARD_SIZE && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player
            return true
        }
        return false
    }

    fun getBoardOccupant(location: Int): Char {
        return if (location in 0 until BOARD_SIZE) {
            mBoard[location]
        } else {
            OPEN_SPOT
        }
    }

    // Renombrado de getBoardCopy a getBoardState para consistencia con los requisitos
    fun getBoardState(): CharArray = mBoard.copyOf()

    fun setBoardState(state: CharArray) {
        if (state.size == BOARD_SIZE) {
            mBoard = state.copyOf() // Asegurar que se copia el estado
        }
        // Podrías añadir un log o manejo de error si state.size != BOARD_SIZE
    }

    fun getComputerMove(): Int {
        var move = -1
        when (mDifficultyLevel) {
            DifficultyLevel.Easy -> move = getRandomMove()
            DifficultyLevel.Harder -> {
                move = getWinningMove()
                if (move == -1) move = getRandomMove()
            }
            DifficultyLevel.Expert -> {
                move = getWinningMove()
                if (move == -1) move = getBlockingMove()
                if (move == -1) move = getRandomMove()
            }
        }
        return move
    }

    private fun getRandomMove(): Int {
        val openSpots = mutableListOf<Int>()
        for (i in 0 until BOARD_SIZE) if (mBoard[i] == OPEN_SPOT) openSpots.add(i)
        return if (openSpots.isNotEmpty()) openSpots[mRand.nextInt(openSpots.size)] else -1
    }

    private fun getWinningMove(): Int {
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = COMPUTER_PLAYER
                val winner = checkForWinner()
                mBoard[i] = OPEN_SPOT
                if (winner == 3) return i
            }
        }
        return -1
    }

    private fun getBlockingMove(): Int {
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = HUMAN_PLAYER
                val winner = checkForWinner()
                mBoard[i] = OPEN_SPOT
                if (winner == 2) return i
            }
        }
        return -1
    }

    fun checkForWinner(): Int {
        for (i in 0..6 step 3) {
            if (mBoard[i] == mBoard[i + 1] && mBoard[i + 1] == mBoard[i + 2] && mBoard[i] != OPEN_SPOT) {
                return if (mBoard[i] == HUMAN_PLAYER) 2 else 3
            }
        }
        for (i in 0..2) {
            if (mBoard[i] == mBoard[i + 3] && mBoard[i + 3] == mBoard[i + 6] && mBoard[i] != OPEN_SPOT) {
                return if (mBoard[i] == HUMAN_PLAYER) 2 else 3
            }
        }
        if (mBoard[0] == mBoard[4] && mBoard[4] == mBoard[8] && mBoard[0] != OPEN_SPOT) {
            return if (mBoard[0] == HUMAN_PLAYER) 2 else 3
        }
        if (mBoard[2] == mBoard[4] && mBoard[4] == mBoard[6] && mBoard[2] != OPEN_SPOT) {
            return if (mBoard[2] == HUMAN_PLAYER) 2 else 3
        }
        for (i in 0 until BOARD_SIZE) if (mBoard[i] == OPEN_SPOT) return 0
        return 1
    }

    fun isGameOver(): Boolean {
        return checkForWinner() != 0
    }
}