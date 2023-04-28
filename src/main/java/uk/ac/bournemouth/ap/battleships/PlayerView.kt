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
import org.example.student.battleshipgame.StudentShip
import uk.ac.bournemouth.ap.battleshiplib.BattleshipGrid
import uk.ac.bournemouth.ap.battleshiplib.GuessResult

class PlayerView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val colCount: Int get() = studentGrid.columns
    private val rowCount: Int get() = studentGrid.rows
    private val squareSpacingRatio = 0.1f
    private val opponentGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }
    private val shipGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN
    }
    var shipsPlaced: Boolean = false
    private val playerShips: List<StudentShip> = listOf(StudentShip(0,0, 0, 1), StudentShip(0,0, 0, 2), StudentShip(0,0, 0, 2), StudentShip(0,0, 0, 3), StudentShip(0,0, 0, 4))
    var userPlacedShips: MutableList<StudentShip> = mutableListOf<StudentShip>()
    var studentGrid: StudentBattleshipGrid = StudentBattleshipGrid(
        BattleshipGrid.DEFAULT_COLUMNS, BattleshipGrid.DEFAULT_ROWS, StudentBattleshipOpponent(
            BattleshipGrid.DEFAULT_COLUMNS,
            BattleshipGrid.DEFAULT_ROWS,
            userPlacedShips
        )
    )

    private var squareSize = 0f
    private var squareSpacing = 0f

    fun opponentAttack() {
        var validMove = false
        var randomX: Int
        var randomY: Int
        var opponentGuessResult: GuessResult

        if (shipsPlaced){

            while (!validMove) {
                randomX = (0 until studentGrid.columns).random()
                randomY = (0 until studentGrid.rows).random()

                try {
                    opponentGuessResult = studentGrid.shootAt(randomY, randomX)
                    updateUIForGuessResult(randomY, randomX, opponentGuessResult)
                    validMove = true

                    if (studentGrid.isFinished) {
                        // This is where I have attempted to handle the game over situation
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {

            MotionEvent.ACTION_DOWN -> {
                var overlaps: Boolean
                try{
                    if (!shipsPlaced) {
                        val selectedShip = playerShips.lastOrNull { !it.selected }
                        selectedShip?.let {
                            val x = event.x
                            val y = event.y
                            val size = selectedShip.size

                            val horizontalOrVert = true

                            var top = (y / (squareSize + squareSpacing)).toInt()
                            var left = (x / (squareSize + squareSpacing)).toInt()

                            if (horizontalOrVert && left + size > colCount) {
                                left = colCount - size
                            }
                            if (!horizontalOrVert && top + size > rowCount) {
                                top = rowCount - size
                            }

                            overlaps = userPlacedShips.any {
                                it.overlapsWith(
                                    left,
                                    top,
                                    left + if (horizontalOrVert) size - 1 else 0,
                                    top + if (horizontalOrVert) 0 else size - 1
                                )
                            }

                            val right = left + if (horizontalOrVert) size - 1 else 0
                            val bottom = top + if (horizontalOrVert) 0 else size - 1

                            val candidateShip = StudentShip(top, left, bottom, right)
                            if (overlaps) {
                                throw Exception("Overlapping ships")
                            }

                            selectedShip.selected = true
                            userPlacedShips.add(candidateShip)
                            updateUIForShipPlacement(candidateShip)

                            invalidate()
                        }
                        if (selectedShip == null) {
                            shipsPlaced = true
                            studentGrid = StudentBattleshipGrid(
                                BattleshipGrid.DEFAULT_COLUMNS, BattleshipGrid.DEFAULT_ROWS, StudentBattleshipOpponent(
                                    BattleshipGrid.DEFAULT_COLUMNS,
                                    BattleshipGrid.DEFAULT_ROWS,
                                    userPlacedShips
                                )
                            )
                        }
                    }
                }
                catch (e: Exception) {
                    Toast.makeText(context, "Duplicate move", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    private val cellColors = Array(rowCount) { Array(colCount) { Color.RED } }

    private fun updateUIForGuessResult(row: Int, col: Int, guessResult: GuessResult) {
        val color = when (guessResult) {
            is GuessResult.HIT -> Color.BLACK
            GuessResult.MISS -> Color.BLUE
            is GuessResult.SUNK -> Color.YELLOW
            else -> Color.RED
        }
        cellColors[col][row] = color
        invalidate()
    }

    private fun updateUIForShipPlacement(ship:StudentShip) {
        for (col in ship.columnIndices){
            for (row in ship.rowIndices) {
                val color = shipGridPaint.color
                cellColors[row][col] = color
            }
        }
    }
}

