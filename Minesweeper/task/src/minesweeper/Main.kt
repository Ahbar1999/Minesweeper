package minesweeper
import kotlin.random.Random

class MineField(private val numberOfMines: Int, seedRandom: Int = 23) {

    // nested classes for encapsulation
    enum class CellState(var stateRepr: Char) {
        Explored('/'),
        Unexplored('.'),
        Marked('*');
    }

    class Cell(mine: Boolean = false, _showMine: Boolean = true,
               _cellStatus: CellState = CellState.Unexplored, val row_num: Int, val col_num: Int) {

        val neighbors = mutableListOf<Cell>()

        var cellStatus: CellState = _cellStatus
            set(value) {
                field = value
                repr = value.stateRepr
            }

        var repr: Char = cellStatus.stateRepr

        var showMine: Boolean = _showMine
            set(value) {
                field = value
                // if it is a mine then change its representation
                repr = if (field && isMine) 'X' else repr
            }
        var isMine: Boolean = mine
            set(value) {
                field = value
                // if it is a mine then change its representation
                repr = if (field && showMine) 'X' else repr
            }

        // Represents the mine count around it
        // 0 if no mine or itself a mine
        var mineCount: Int = 0
            set(value) {
                field = value
                // Change the cell representation appropriately
                repr = when (cellStatus) {
                    CellState.Explored -> if (field == 0) repr else field.digitToChar()
                    else -> repr
                }

//                repr = if (field != 0 && cellStatus == CellState.Explored) field.digitToChar() else if repr
            }
    }

    private val MINEFIELDSIZE = 9
    private var minefieldRows: MutableList<MutableList<Cell>> = mutableListOf()
    private val randomGen = Random(seedRandom)
    private val showMines = false
    private var gameEnd = false

    init {
        buildMinefield()
        updateMineCount()
    }

    fun play() {
        this.print()
        var startFlag = true
        while (!checkGameEnd() || this.gameEnd) {
            print("Set/unset mine marks or claim a cell as free: ")

            val (x, y, cmd) = readln().split(" ")

            if (processCellInput(x.toInt() - 1, y.toInt() - 1, cmd, startFlag)) this.print()

            // set startFlag to false after first run
            if (startFlag && cmd == "free") startFlag = false
        }
        // if the game finished
        if (!this.gameEnd) println("Congratulations! You found all the mines!")
    }

    private fun enableMines() {
        for (row in 0 until MINEFIELDSIZE) {
            for (col in 0 until MINEFIELDSIZE)  {
                if (minefieldRows[row][col].isMine) {
                    minefieldRows[row][col].showMine = true
                }
            }
        }
    }

    private fun processCellInput(x: Int, y: Int, cmd: String, flag: Boolean): Boolean {
        val currentCell = minefieldRows[y][x]

        if (cmd == "free") {
            if (currentCell.isMine) {
                if (flag) {
                    currentCell.isMine = false
                    // place this mine somewhere else
                    do {
                        this.placeMine()
//                        println(currentCell.isMine)
                    } while (currentCell.isMine)
                    // update the mine count for each cell
                    this.updateMineCount()
                } else {
                    println("You stepped on a mine and failed!")
                    this.gameEnd = true
                    this.enableMines()
                    return true
                }
            }

            processFreeCommand(x, y)
        } else if (cmd == "mine") {    // process mine
            currentCell.cellStatus = if (currentCell.cellStatus == CellState.Marked) CellState.Unexplored else CellState.Marked
        }

        return true
    }

    private fun processFreeCommand(x: Int, y: Int) {
        val queue = mutableListOf(minefieldRows[y][x])
        val visited = mutableSetOf<Cell>()

        while (queue.isNotEmpty()) {
//            println(queue.size)
            val cell = queue.removeFirst()
            cell.cellStatus = CellState.Explored
            visited.add(cell)

            if (cell.mineCount == 0) {
                var validNeighbors = true
                for (neighbor in cell.neighbors) {
                    if (neighbor.isMine) {
                        validNeighbors = false
                        break
                    }
                }

                if (validNeighbors) cell.neighbors.filter { !visited.contains(it) }.forEach {
                    run {
                        queue.add(it)
                        visited.add(it)
                    }
                }
            }
        }

        this.updateMineCount()
    }

    private fun checkGameEnd(): Boolean {
        var allMinesFoundFlag = true
        // check if all mines flagged
        for (row in 0 until MINEFIELDSIZE) {
            for (col in 0 until MINEFIELDSIZE) {
                if (minefieldRows[row][col].isMine && minefieldRows[row][col].cellStatus != CellState.Marked) {
                    allMinesFoundFlag = false
                }
            }
        }

        var allValidCellsExplored = true
        // check if all valid cells explored
        for (row in 0 until MINEFIELDSIZE) {
            for (col in 0 until MINEFIELDSIZE) {
                if (!minefieldRows[row][col].isMine && minefieldRows[row][col].cellStatus != CellState.Explored) {
                    allValidCellsExplored = false
                }
            }
        }

        return allMinesFoundFlag || allValidCellsExplored
    }

    private fun buildMinefield() {
        // Set the random generator seed
        // Build minefield without mines
        for (row in 0 until MINEFIELDSIZE) {
            val currentRow = mutableListOf<Cell>()
            for (col in 0 until MINEFIELDSIZE) {
                val newCell = Cell(_showMine = showMines, row_num = row, col_num = col)
                currentRow.add(newCell)
            }
            minefieldRows.add(currentRow)
        }

        // Place mines
        for (mine in 0 until numberOfMines) {
            placeMine()
        }
    }

    private fun placeMine(row: Int = -1, col: Int = -1) {

        // if the cell is specified
        if (row != -1 && col != -1) {
            minefieldRows[row][col].isMine = true
            return
        }

        // if the cell is not specified
        var randomRow: Int
        var randomCol: Int
        do {
            randomRow = randomGen.nextInt(0, MINEFIELDSIZE)
            randomCol = randomGen.nextInt(0, MINEFIELDSIZE)
        } while (minefieldRows[randomRow][randomCol].isMine)

        minefieldRows[randomRow][randomCol].isMine = true
    }

    private fun updateMineCount() {
        // reset mine counts
        for (row in 0 until MINEFIELDSIZE) {
            for (col in 0 until MINEFIELDSIZE)  {
                minefieldRows[row][col].mineCount = 0
            }
        }

        // Place the mine count
        for (row in 0 until MINEFIELDSIZE) {
            for (col in 0 until MINEFIELDSIZE)  {
                val currCell = minefieldRows[row][col]
                if (!currCell.isMine) {
                    // Upper Row
                    if (row - 1 >= 0) {
                        // Top Left
                        if (col - 1 >= 0) {
                            currCell.neighbors.add(minefieldRows[row-1][col-1])
                            if (minefieldRows[row-1][col-1].isMine) {
                                currCell.mineCount += 1
                            }
                        }
                        // Top
                        currCell.neighbors.add(minefieldRows[row-1][col])
                        if (minefieldRows[row-1][col].isMine) {
                            currCell.mineCount += 1
                        }
                        // Top Right
                        if (col + 1 < MINEFIELDSIZE) {
                            currCell.neighbors.add(minefieldRows[row-1][col+1])
                            if (minefieldRows[row-1][col+1].isMine) {
                                currCell.mineCount += 1
                            }
                        }
                    }
                    // Lower Row
                    if (row + 1 < MINEFIELDSIZE) {
                        // Bottom Left
                        if (col - 1 >= 0) {
                            currCell.neighbors.add(minefieldRows[row+1][col-1])
                            if (minefieldRows[row+1][col-1].isMine) {
                                currCell.mineCount += 1
                            }
                        }
                        // Bottom
                        currCell.neighbors.add(minefieldRows[row+1][col])
                        if (minefieldRows[row+1][col].isMine) {
                            currCell.mineCount += 1
                        }
                        // Bottom Right
                        if (col + 1 < MINEFIELDSIZE) {
                            currCell.neighbors.add(minefieldRows[row+1][col+1])
                            if (minefieldRows[row+1][col+1].isMine) {
                                currCell.mineCount += 1
                            }
                        }
                    }
                    // Left Side
                    if (col - 1 >= 0) {
                        currCell.neighbors.add(minefieldRows[row][col-1])
                        if (minefieldRows[row][col-1].isMine) {
                            currCell.mineCount += 1
                        }
                    }
                    // Right Side
                    if (col + 1 < MINEFIELDSIZE) {
                        currCell.neighbors.add(minefieldRows[row][col+1])
                        if (minefieldRows[row][col+1].isMine) {
                            currCell.mineCount += 1
                        }
                    }
                }
            }
        }
    }

    // Show the minefield
    private fun print() {
        println(" |123456789|")
        println("-|---------|")
        var rowNumber = 1
        for (row in minefieldRows) {
            print("${rowNumber++}|")
            for (cell in row) {
                print(cell.repr)
            }
            print("|\n")
        }
        println("-|---------|")
    }
}


fun main() {
    print("How many mines do you want on the field? ")
    val numberOfMines = readln().toInt()

    // Create a Minefield object
    val minefield = MineField(numberOfMines)
    minefield.play()
}
