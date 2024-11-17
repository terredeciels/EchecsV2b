package perft

import board.Board
import board.Constantes
import board.Move
import java.io.BufferedReader
import java.io.FileReader


fun main() {
    val perftCompare = PerftCompare()
    perftCompare.main()
}

class PerftCompare : Constantes() {

    fun main() {
        val maxDepth = 4
        val fileReader = FileReader("/Users/gilles/IdeaProjects/EchecsV2b/src/main/java/perft/perftsuite.epd")
        val reader = BufferedReader(fileReader)
        var line: String
        var passes = 0
        var fails = 0
        while ((reader.readLine().also { line = it }) != null) {
            val parts = line.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size >= 3) {
                val fen = parts[0].trim { it <= ' ' }
                for (i in 1 until parts.size) {
                    if (i > maxDepth) {
                        break
                    }
                    val entry = parts[i].trim { it <= ' ' }
                    val entryParts = entry.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val perftResult = entryParts[1].toInt()

                    val board = FenToBoard().toBoard(fen)

                    val result = Perft.perft(board!!, i)
                    if (perftResult.toLong() == result.moveCount) {
                        passes++
                        println("PASS: " + fen + ". Moves " + result.moveCount + ", depth " + i)
                    } else {
                        fails++
                        println("FAIL: " + fen + ". Moves " + result.moveCount + ", depth " + i)
                        break
                    }
                }
            }
        }

        println("Passed: $passes")
        println("Failed: $fails")
    }

    internal class PerftResult {

        @JvmField
        var moveCount: Long = 0
    }

    private object Perft {
        fun perft(board: Board, depth: Int): PerftResult {
            val result = PerftResult()
            if (depth == 0) {
                result.moveCount++
                return result
            }

            board.generateMoves()
            val moves: List<Move> = board.pseudomoves
            for (move in moves) {
                if (board.makeMove(move)) {
                    val subPerft = perft(Board(board), depth - 1)
                    board.takeback()
                    result.moveCount += subPerft.moveCount
                }
            }
            return result
        }
    }
}

