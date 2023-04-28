package org.example.student.battleshipgame

import uk.ac.bournemouth.ap.battleshiplib.*
import uk.ac.bournemouth.ap.lib.matrix.MutableMatrix
import uk.ac.bournemouth.ap.lib.matrix.ext.Coordinate
import kotlin.math.max
import kotlin.random.Random

class StudentBattleshipOpponent(
    override val columns: Int,
    override val rows: Int,
    ships : List<Ship>
) : BattleshipOpponent {

    companion object {
        fun createRandomShips(
            columns: Int,
            rows: Int,
            shipSizes: IntArray,
            random: Random
        ): List<StudentShip> {
            val placedShips = mutableListOf<StudentShip>()
            for (size in shipSizes) {
                var left: Int
                var top: Int
                var overlaps: Boolean
                var isHorizontal: Boolean
                do {
                    isHorizontal = random.nextBoolean()
                    if (size>columns && isHorizontal){
                        isHorizontal = false
                    }
                    if (size>rows && !isHorizontal){
                        isHorizontal = true
                    }
                    left = random.nextInt(if (isHorizontal) max(columns - size + 1, 1).coerceAtLeast(1) else columns)
                    top = random.nextInt(if (isHorizontal) rows else rows - size + 1)
                    if (isHorizontal && left + size > columns) {
                        left = columns - size
                    }
                    if (!isHorizontal && top + size > rows) {
                        top = rows - size
                    }
                    overlaps = placedShips.any {
                        it.overlapsWith(left, top, left + if (isHorizontal) size - 1 else 0, top + if (isHorizontal) 0 else size - 1)
                    }
                } while (overlaps)

                val right = left + if (isHorizontal) size - 1 else 0
                val bottom = top + if (isHorizontal) 0 else size - 1

                val candidateShip = StudentShip(top, left, bottom, right)
                placedShips.add(candidateShip)
            }
            return placedShips
        }
    }

    init {
        val occupiedCells = HashSet<Coordinate>()
        ships.forEach { ship ->
            if (ship.topLeft.x < 0 || ship.topLeft.y < 0 || ship.bottomRight.x >= columns || ship.bottomRight.y >= rows) {
                throw IllegalArgumentException("Ship $ship is out of bounds")
            }
            for (x in ship.topLeft.x..ship.bottomRight.x) {
                for (y in ship.topLeft.y..ship.bottomRight.y) {
                    val cell = Coordinate(x, y)
                    if (occupiedCells.contains(cell)) {
                        throw IllegalArgumentException("Ship $ship overlaps with another ship")
                    }
                    occupiedCells.add(cell)
                }
            }
        }
    }

    override val ships: List<Ship> = ships

    override fun shipAt(column: Int, row: Int): BattleshipOpponent.ShipInfo<Ship>? {
        val ship =
            ships.firstOrNull { it.columnIndices.contains(column) && it.rowIndices.contains(row) }
        return ship?.let { BattleshipOpponent.ShipInfo(ships.indexOf(it), it) }
    }
    constructor(columns: Int, rows: Int, shipSizes: IntArray, random: Random)
            : this(columns, rows, createRandomShips(columns, rows, shipSizes, random))
}

data class StudentShip(
    override val top: Int,
    override val left: Int,
    override val bottom: Int,
    override val right: Int
) : Ship {

    init {
        require(top <= bottom) { "Invalid ship coordinates: top=$top, bottom=$bottom" }
        require(left <= right) { "Invalid ship coordinates: left=$left, right=$right" }
        require(width==1 || height ==1 ) {" Invalid ship"}
    }

    var selected: Boolean = false

    fun overlapsWith(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        return !(right < this.left || left > this.right || bottom < this.top || top > this.bottom)
    }

    override val topLeft: Coordinate get() = Coordinate(x = left, y = top)
    override val bottomRight: Coordinate get() = Coordinate(x = right, y = bottom)
}

class StudentBattleshipGrid(
    override val columns: Int,
    override val rows: Int,
    override val opponent: BattleshipOpponent,
    override val shipsSunk: BooleanArray
) : BattleshipGrid {

    constructor(columns: Int, rows: Int, opponent: BattleshipOpponent) : this(
        columns,
        rows,
        opponent,
        BooleanArray(opponent.ships.size)
    )

    private val guesses = MutableMatrix<GuessCell>(columns, rows, GuessCell.UNSET)
    private val hits = IntArray(opponent.ships.size)
    private val listeners = mutableListOf<BattleshipGrid.BattleshipGridListener>()

    override val isFinished: Boolean
        get() = shipsSunk.all { it }

    override fun get(column: Int, row: Int): GuessCell {
        return guesses[column, row]
    }

    override fun shootAt(x: Int, y: Int): GuessResult {
        if (x !in 0 until columns || y !in 0 until rows) {
            throw IllegalArgumentException("Coordinates ($x, $y) are not within the grid.")
        }
        if (guesses[x, y] != GuessCell.UNSET) {
            throw Exception("Duplicate move")
        }
        val shipInfo = opponent.shipAt(x, y)
        return if (shipInfo != null) {
            guesses[x, y] = GuessCell.HIT(shipInfo.index)

            hits[shipInfo.index]++

            if (hits[shipInfo.index] == shipInfo.ship.size) {
                shipsSunk[shipInfo.index] = true

                shipInfo.ship.forEachIndex { x, y -> guesses[x, y] = GuessCell.SUNK(shipInfo.index) }

                GuessResult.SUNK(shipInfo.index)
            } else {
                GuessResult.HIT(shipInfo.index)
            }
        } else {
            guesses[x, y] = GuessCell.MISS
            GuessResult.MISS
        }.also {
            listeners.forEach { it.onGridChanged(this, x, y) }
        }
    }

    override fun addOnGridChangeListener(listener: BattleshipGrid.BattleshipGridListener) {
        listeners.add(listener)
    }

    override fun removeOnGridChangeListener(listener: BattleshipGrid.BattleshipGridListener) {
        listeners.remove(listener)
    }
}