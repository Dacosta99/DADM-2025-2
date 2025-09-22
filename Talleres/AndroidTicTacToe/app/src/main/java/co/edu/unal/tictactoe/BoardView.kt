package co.edu.unal.tictactoe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mGame: TicTacToeGame? = null
    private var mCellWidth: Int = 0
    private var mCellHeight: Int = 0

    private val mGridPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mHumanBitmap: Bitmap? = null // Nullable initially
    private var mComputerBitmap: Bitmap? = null // Nullable initially

    private var mBoardRect: Rect = Rect()

    private var mListener: BoardTouchListener? = null

    interface BoardTouchListener {
        fun onMoveMade()
    }

    init {
        setBackgroundColor(Color.DKGRAY) // Fondo del tablero gris oscuro
        mGridPaint.color = Color.LTGRAY  // Líneas de la cuadrícula gris claro
        mGridPaint.strokeWidth = 8f    // Increased strokeWidth

        // Attempt to load bitmaps, handle potential errors
        try {
            mHumanBitmap = BitmapFactory.decodeResource(resources, R.drawable.human_player)
            mComputerBitmap = BitmapFactory.decodeResource(resources, R.drawable.computer_player)
        } catch (e: Exception) {
            // Log error or handle missing resources, e.g., draw placeholders
            // For now, bitmaps might remain null if resources are missing
            // Consider adding placeholder drawing logic in onDraw if bitmaps are null
        }
    }

    fun setGame(game: TicTacToeGame) {
        mGame = game
    }

    fun setOnBoardTouchListener(listener: BoardTouchListener) {
        mListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Use the view's width and height directly, respecting padding
        val xpad = paddingLeft + paddingRight
        val ypad = paddingTop + paddingBottom

        val usableWidth = w - xpad
        val usableHeight = h - ypad

        // Make the board square
        val boardSize = if (usableWidth < usableHeight) usableWidth else usableHeight

        mCellWidth = boardSize / 3
        mCellHeight = boardSize / 3

        // Center the board within the padded area
        val xOffset = paddingLeft + (usableWidth - boardSize) / 2
        val yOffset = paddingTop + (usableHeight - boardSize) / 2

        mBoardRect.set(xOffset, yOffset, xOffset + boardSize, yOffset + boardSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mGame ?: return // If game is null, don't draw

        // Draw the grid lines
        for (i in 1..2) {
            // Vertical lines
            canvas.drawLine(
                mBoardRect.left + mCellWidth * i.toFloat(), mBoardRect.top.toFloat(),
                mBoardRect.left + mCellWidth * i.toFloat(), mBoardRect.bottom.toFloat(), mGridPaint
            )
            // Horizontal lines
            canvas.drawLine(
                mBoardRect.left.toFloat(), mBoardRect.top + mCellHeight * i.toFloat(),
                mBoardRect.right.toFloat(), mBoardRect.top + mCellHeight * i.toFloat(), mGridPaint
            )
        }

        // Draw the X's and O's
        for (i in 0 until TicTacToeGame.BOARD_SIZE) {
            val row = i / 3
            val col = i % 3

            val piece = mGame!!.getBoardOccupant(i)
            if (piece != TicTacToeGame.OPEN_SPOT) {
                val pieceBitmap = if (piece == TicTacToeGame.HUMAN_PLAYER) mHumanBitmap else mComputerBitmap

                if (pieceBitmap != null) {
                    val pieceX = mBoardRect.left + col * mCellWidth
                    val pieceY = mBoardRect.top + row * mCellHeight
                    val destRect = Rect(pieceX, pieceY, pieceX + mCellWidth, pieceY + mCellHeight)
                    canvas.drawBitmap(pieceBitmap, null, destRect, null)
                } else {
                    // Optional: Draw placeholder text if bitmaps are missing
                    val placeholderPaint = Paint().apply { textSize = mCellHeight * 0.5f; textAlign = Paint.Align.CENTER; color = Color.RED }
                    canvas.drawText(
                        piece.toString(),
                        mBoardRect.left + col * mCellWidth + mCellWidth / 2f,
                        mBoardRect.top + row * mCellHeight + mCellHeight / 2f - (placeholderPaint.descent() + placeholderPaint.ascent()) / 2,
                        placeholderPaint
                    )
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGame ?: return false
        if (mGame!!.isGameOver()) return false // Check if game is over before processing touch

        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x.toInt()
            val y = event.y.toInt()

            // Check if touch is within the board boundaries
            if (mBoardRect.contains(x,y)) {
                val col = (x - mBoardRect.left) / mCellWidth
                val row = (y - mBoardRect.top) / mCellHeight

                // Ensure col and row are within 0-2 range
                if (col in 0..2 && row in 0..2) {
                    val location = row * 3 + col
                    if (mGame!!.getBoardOccupant(location) == TicTacToeGame.OPEN_SPOT) {
                        if (mGame!!.setMove(TicTacToeGame.HUMAN_PLAYER, location)) {
                            invalidate() // Redraw the board
                            mListener?.onMoveMade()
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}
