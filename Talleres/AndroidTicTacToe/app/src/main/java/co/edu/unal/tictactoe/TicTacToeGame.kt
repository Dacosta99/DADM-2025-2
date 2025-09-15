package co.edu.unal.tictactoe

import kotlin.random.Random

class TicTacToeGame {
    companion object {
        const val BOARD_SIZE = 9
        const val OPEN_SPOT: Char = ' '
        const val HUMAN_PLAYER: Char = 'X'
        const val COMPUTER_PLAYER: Char = 'O'
    }

    private val mBoard = CharArray(BOARD_SIZE) { OPEN_SPOT }
    private val mRand = Random(System.currentTimeMillis())

    fun clearBoard() {
        for (i in 0 until BOARD_SIZE) mBoard[i] = OPEN_SPOT
    }

    fun setMove(player: Char, location: Int) {
        if (location in 0 until BOARD_SIZE && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player
        }
    }

    fun getBoardCopy(): CharArray = mBoard.copyOf()

    // Return best move for computer (0-8)
    fun getComputerMove(): Int {
        // First, check if can win in next move
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = COMPUTER_PLAYER
                if (checkForWinner() == 3) {
                    mBoard[i] = OPEN_SPOT
                    return i
                }
                mBoard[i] = OPEN_SPOT
            }
        }

        // Then check if human can win next and block
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = HUMAN_PLAYER
                if (checkForWinner() == 2) {
                    mBoard[i] = OPEN_SPOT
                    return i
                }
                mBoard[i] = OPEN_SPOT
            }
        }

        // Otherwise, pick random open spot
        val openSpots = mutableListOf<Int>()
        for (i in 0 until BOARD_SIZE) if (mBoard[i] == OPEN_SPOT) openSpots.add(i)
        return if (openSpots.isNotEmpty()) openSpots[mRand.nextInt(openSpots.size)] else -1
    }

    /*
    checkForWinner():
      0 -> no winner yet
      1 -> tie
      2 -> X won (human)
      3 -> O won (computer)
    */
    fun checkForWinner(): Int {
        // Check rows
        for (i in 0..6 step 3) {
            if (mBoard[i] == mBoard[i + 1] && mBoard[i + 1] == mBoard[i + 2] && mBoard[i] != OPEN_SPOT) {
                return if (mBoard[i] == HUMAN_PLAYER) 2 else 3
            }
        }
        // Check cols
        for (i in 0..2) {
            if (mBoard[i] == mBoard[i + 3] && mBoard[i + 3] == mBoard[i + 6] && mBoard[i] != OPEN_SPOT) {
                return if (mBoard[i] == HUMAN_PLAYER) 2 else 3
            }
        }
        // Diagonals
        if (mBoard[0] == mBoard[4] && mBoard[4] == mBoard[8] && mBoard[0] != OPEN_SPOT) {
            return if (mBoard[0] == HUMAN_PLAYER) 2 else 3
        }
        if (mBoard[2] == mBoard[4] && mBoard[4] == mBoard[6] && mBoard[2] != OPEN_SPOT) {
            return if (mBoard[2] == HUMAN_PLAYER) 2 else 3
        }

        // If no winner and no open spots -> tie
        for (i in 0 until BOARD_SIZE) if (mBoard[i] == OPEN_SPOT) return 0
        return 1
    }
}

