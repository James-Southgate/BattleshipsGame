package uk.ac.bournemouth.ap.battleships

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import org.example.student.battleshipgame.StudentBattleshipGrid
import org.example.student.battleshipgame.StudentBattleshipOpponent
import uk.ac.bournemouth.ap.battleshiplib.BattleshipGrid
import kotlin.random.Random
import uk.ac.bournemouth.ap.battleshiplib.GuessResult


class GridView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val colCount: Int get() = opponentGrid.columns
    private val rowCount: Int get() = opponentGrid.rows
    private val squareSpacingRatio = 0.1f
    private val opponentGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    var opponentGrid: StudentBattleshipGrid = StudentBattleshipGrid(
        BattleshipGrid.DEFAULT_COLUMNS, BattleshipGrid.DEFAULT_ROWS, StudentBattleshipOpponent(
            BattleshipGrid.DEFAULT_COLUMNS,
            BattleshipGrid.DEFAULT_ROWS,
            BattleshipGrid.DEFAULT_SHIP_SIZES,
            Random

        )
    )
        set(value) {
            field = value
            recalculateDimensions()
            invalidate()
        }

    private var squareSize = 0f
    private var squareSpacing = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateDimensions(w, h)
    }

    private fun recalculateDimensions(w: Int = width, h: Int = height) {
        val sizeX = w / (colCount + (colCount + 1) * squareSpacingRatio)
        val sizeY = h / (rowCount + (rowCount + 1) * squareSpacingRatio)
        squareSize = minOf(sizeX, sizeY)
        squareSpacing = squareSize * squareSpacingRatio
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        for (row in 0 until rowCount) {
            for (col in 0 until colCount) {
                val left = squareSpacing + ((squareSize + squareSpacing) * col)
                val top = squareSpacing + ((squareSize + squareSpacing) * row)
                val right = left + squareSize
                val bottom = top + squareSize
                opponentGridPaint.color = cellColors[row][col]
                canvas!!.drawRect(left, top, right, bottom, opponentGridPaint)
            }
        }
    }

    interface OnPlayerAttackListener {
        fun onPlayerAttack()
    }

    var onPlayerAttackListener: GridView.OnPlayerAttackListener? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                val row = (y / (squareSize + squareSpacing)).toInt()
                val col = (x / (squareSize + squareSpacing)).toInt()

                if (cellColors[row][col] != Color.BLACK) {
                    Toast.makeText(context, "You've already attacked this square", Toast.LENGTH_SHORT).show()
                    return true
                }
                try {
                    val guessResult = opponentGrid.shootAt(row, col)
                    updateUIForGuessResult(row, col, guessResult)
                    if (opponentGrid.isFinished) {
                        // Here is where i would attempt to handle the game over situation
                        Toast.makeText(context, "Game Over", Toast.LENGTH_SHORT).show()
                    } else {
                        onPlayerAttackListener?.onPlayerAttack()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Duplicate move", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    private val cellColors = Array(rowCount) { Array(colCount) { Color.BLACK } }

    private fun updateUIForGuessResult(row: Int, col: Int, guessResult: GuessResult) {
        val color = when (guessResult) {
            is GuessResult.HIT -> Color.RED
            GuessResult.MISS -> Color.BLUE
            is GuessResult.SUNK -> Color.YELLOW
            else -> Color.BLACK
        }

        cellColors[row][col] = color
        invalidate()
    }
}
