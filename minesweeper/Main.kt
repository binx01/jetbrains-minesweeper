package minesweeper

open class Game(val name: String)

class Minesweeper(private val grid: Grid) : Game("Minesweeper") {

    val status: Status
        get() {
            return when {
                grid.allMinesFound || grid.allCellsExplored -> Status.WIN
                grid.mineExplored -> Status.LOSE
                else -> Status.UNFINISHED
            }
        }

    fun showGrid(): String {
        return grid.show()
    }

    fun makeMove(x: Int, y: Int, command: String) {
        grid.move(Move(x, y, command))
    }

}

class Grid(private val height: Int, private val width: Int, private val numOfMines: Int) {

    private var _cells = emptyArray<Array<Cell>>()
    private var _safeCells = mutableListOf<Cell>()
    private var _mineCells = mutableListOf<Cell>()
    private var _markedCells = mutableListOf<Cell>()
    private var _exploredCells = mutableListOf<Cell>()
    private var _moves = mutableListOf<Move>()
    private var _mineExplored = false

    val allMinesFound: Boolean
        get() = _mineCells.size == _markedCells.size && _mineCells.containsAll(_markedCells)

    val allCellsExplored: Boolean
        get() = _exploredCells.size == _safeCells.size && _exploredCells.containsAll(_safeCells)

    val mineExplored: Boolean
        get() = _mineExplored

    init {
        var i = 0

        for (y in 0 until height) {

            var array = emptyArray<Cell>()

            for (x in 0 until width) {
                val cell = Cell(x, y, ++i)
                array += arrayOf(cell)
            }
            _cells += array
        }
    }

    fun show(): String {
        val sb = StringBuilder()
        sb.append(" |")
        repeat(width) { sb.append(it + 1) }
        sb.append("|\n")
        sb.append("-|")
        repeat(width) { sb.append("-") }
        sb.append("|\n")
        for (cells in _cells) {
            var str = "${_cells.indexOf(cells) + 1}|"
            for (cell in cells) {
                str += cell.symbol
                // str += if (cell.minesAround > 0) cell.minesAround else cell.symbol
            }
            str += "|"
            sb.append("$str\n")
        }
        sb.append("-|")
        repeat(width) { sb.append("-") }
        sb.append("|\n")
        return sb.toString()
    }

    private fun calculateMines() {
        for (cell in _safeCells) {
            val (x, y) = cell.coordinates
            for (yy in y - 1..y + 1) {
                for (xx in x - 1..x + 1) {
                    val neighbor = _cells.elementAtOrNull(yy)?.elementAtOrNull(xx)
                    if (neighbor != null && cell !== neighbor) {
                        cell.neighbors.add(neighbor)
                    }
                    if (neighbor?.safe == false) {
                        // _cells[cell.coordinates.second][cell.coordinates.first].minesAround += 1
                        cell.minesAround++
                    }
                }
            }
        }
    }

    private fun generateMines(excludeNo: Int): List<Int> {

        var numbers = emptyArray<Int>()
        var number: Int
        return IntArray(height * width) {
            do {
                number = (1..height * width).random()
            } while (numbers.indexOf(number) != -1 && number != excludeNo)
            numbers += number
            number
        }.take(numOfMines)

    }

    private fun placeMines(mines: List<Int>) {

        _cells.forEach { cells ->
            cells.forEach { cell ->
                if (mines.indexOf(cell.no) != -1) {
                    cell.safe = false
                    _mineCells.add(cell)
                } else {
                    _safeCells.add(cell)
                }
            }
        }
    }

    private fun explore(cell: Cell) {
        cell.explored = true
        cell.marked = false
        _exploredCells.add(cell)
        if (_markedCells.contains(cell)) {
            _markedCells.remove(cell)
        }
        if (cell.minesAround == 0) {
            cell.neighbors.forEach { neighbor -> if (!neighbor.explored) explore(neighbor) }
        }
    }

    fun move(move: Move) {
        val cell = _cells[move.y - 1][move.x - 1]
        if (_moves.size == 0) {
            placeMines(generateMines(cell.no))
            calculateMines()
        }
        if (move.command == "free") {
            if (cell.safe) {
                explore(cell)
            } else {
                _mineExplored = true
                _mineCells.forEach { it.explored = true }

            }
        }
        if (move.command == "mine") {
            if (_markedCells.contains(cell)) {
                cell.marked = false
                _markedCells.remove(cell)
            } else {
                cell.marked = true
                _markedCells.add(cell)
            }
        }
        _moves.add(move)
    }
}

class Move(val x: Int, val y: Int, val command: String)

class Cell(private val x: Int, private val y: Int, val no: Int) {

    val coordinates: Pair<Int, Int>
        get() = Pair(x, y)

    var minesAround = 0
    var safe = true
    var explored = false
    var marked = false

    val neighbors = mutableListOf<Cell>()

    val symbol: String
        get() {
            return when {
                safe && explored && !marked && minesAround == 0 -> Symbol.SLASH.value
                safe && explored && !marked && minesAround > 0 -> minesAround.toString()
                !safe && explored -> Symbol.X.value
                marked && !explored -> Symbol.STAR.value
                else -> Symbol.DOT.value
            }

        }
}

enum class Symbol(val value: String) {
    SLASH("/"),
    X("X"),
    STAR("*"),
    DOT(".")
}

enum class Status(val result: String) {
    WIN("Congratulations! You found all the mines!"),
    LOSE("You stepped on a mine and failed!"),
    UNFINISHED("Unfinished")
}

fun main() {

    println("How many mines do you want on the field?")
    val numberOfMines = readLine()!!.toInt()

    val game = Minesweeper(Grid(9, 9, numberOfMines))
    println(game.showGrid())

    do {

        println("Set/unset mines marks or claim a cell as free:")
        val (x, y, command) = readLine()!!.split(" ")

        try {
            game.makeMove(x.toInt(), y.toInt(), command)
            println(game.showGrid())
        } catch (e: Error) {
            println(if (e.message == "Cell with number!") "There is a number here!" else e.message)
        }


    } while (game.status == Status.UNFINISHED)

    println(game.status.result)

}