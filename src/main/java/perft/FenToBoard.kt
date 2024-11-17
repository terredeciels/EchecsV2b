package perft

import board.Board
import board.Constantes

class FenToBoard : Constantes() {
    private var board: Board? = null


    fun toBoard(fen: String): Board? {
        board = Board()
        initFromFEN(fen, true)
        return board
    }


    private fun initFromFEN(fen: String, strict: Boolean) {
        // pos.clear(); // TODO ?
        var index = 0
        var ch: Char
        /*========== 1st field : pieces ==========*/
        var row = 7
        var col = 0
        while (index < fen.length && fen[index] != ' ') {
            ch = fen[index]
            if (ch == '/') {
                require(col == 8) { "Malformatted fen string: unexpected '/' found at index $index" }
                row--
                col = 0
            } else if (ch >= '1' && ch <= '8') {
                val num = ch.code - '0'.code
                require(col + num <= 8) { "Malformatted fen string: too many pieces in rank at index $index: $ch" }
                (0 until num).forEach { j ->
                    // int _case = coorToSqi(col, row);
                    setStone(col, row, NO_STONE.toInt())

                    col++
                }
            } else {
                val stone = fenCharToStone(ch)
                require(stone != NO_STONE.toInt()) { "Malformatted fen string: illegal piece char: $ch" }
                //  int _case = coorToSqi(col, row);
                setStone(col, row, stone)
                col++
            }
            index++
        }
        require(!(row != 0 || col != 8)) { "Malformatted fen string: missing pieces at index: $index" }
        /*========== 2nd field : to play ==========*/
        if (index + 1 < fen.length && fen[index] == ' ') {
            ch = fen[index + 1]
            when (ch) {
                'w' -> setToPlay(LIGHT)
                'b' -> setToPlay(DARK)
                else -> throw IllegalArgumentException("Malformatted fen string: expected 'to play' as second field but found $ch")
            }
            index += 2
        }
        /*========== 3rd field : castles ==========*/
        if (index + 1 < fen.length && fen[index] == ' ') {
            index++
            var castles: Int = NO_CASTLES
            if (fen[index] == '-') {
                index++
            } else {
                var last = -1
                while (index < fen.length && fen[index] != ' ') {
                    ch = fen[index]
                    if (ch == 'K') {
                        castles = castles or WHITE_SHORT_CASTLE
                        last = 0
                    } else if (ch == 'Q' && (!strict || last < 1)) {
                        castles = castles or WHITE_LONG_CASTLE
                        last = 1
                    } else if (ch == 'k' && (!strict || last < 2)) {
                        castles = castles or BLACK_SHORT_CASTLE
                        last = 2
                    } else if (ch == 'q' && (!strict || last < 3)) {
                        castles = castles or BLACK_LONG_CASTLE
                        last = 3
                    } else {
                        throw IllegalArgumentException("Malformatted fen string: illegal castles identifier or sequence $ch")
                    }
                    index++
                }
            }
            board!!.castleRights = if ((castles and 1) == 1) 2 else 0
            board!!.castleRights += if ((castles and 2) == 2) 1 else 0
            board!!.castleRights += if ((castles and 4) == 4) 8 else 0
            board!!.castleRights += if ((castles and 8) == 8) 4 else 0
        } else {
            throw IllegalArgumentException("Malformatted fen string: expected castles at index $index")
        }
        /*========== 4th field : ep square ==========*/
        if (index + 1 < fen.length && fen[index] == ' ') {
            index++
            var sqiEP: Int = NO_SQUARE
            if (fen[index] == '-') {
                index++
            } else if (index + 2 < fen.length) {
                sqiEP = strToSqi(fen.substring(index, index + 2))
                index += 2
            }
            board!!.enPassantSquare = sqiEP
        } else {
            throw IllegalArgumentException("Malformatted fen string: expected ep square at index $index")
        }
        /*========== 5th field : half move clock ==========*/
        if (index + 1 < fen.length && fen[index] == ' ') {
            index++
            val start = index
            while (index < fen.length && fen[index] != ' ') {
                index++
            }
            board!!.halfMoveCount = fen.substring(start, index).toInt()
        } else {
            throw IllegalArgumentException("Malformatted fen string: expected half move clock at index $index")
        }
        /*========== 6th field : full move number ==========*/
        if (index + 1 < fen.length && fen[index] == ' ') {
            val i = 2 * (fen.substring(index + 1).toInt() - 1)
            if (board!!.currentSide == LIGHT) {
                setPlyNumber(i)
            } else {
                setPlyNumber(i + 1)
            }
        } else {
            throw IllegalArgumentException("Malformatted fen string: expected ply number at index $index")
        }

        /*========== now check the produced position ==========*/
        // @TODO 
    }

    fun setPlyNumber(plyNumber: Int) {
        board!!.currentPly = plyNumber
    }

    fun setToPlay(side: Int) {
        board!!.currentSide = side
        board!!.opponentSide =
            if (board!!.currentSide == LIGHT) DARK else LIGHT
    }

    //    int PAWN = 0, KNIGHT = 1, BISHOP = 2, ROOK = 3, QUEEN = 4, KING = 5;
    //    int EMPTY = 6;
    fun setStone(j: Int, i: Int, stone: Int) {
        val _case = 56 - 8 * i + j
        board!!.squarePieces[_case] =
            if (abs(stone) == 0)
                6
            else
                if (abs(stone) == 6)
                    5
                else
                    if (abs(stone) == 5) 0 else abs(stone)
        board!!.squareColors[_case] =
            if (stone < 0) LIGHT else if (stone > 0) DARK else EMPTY


        //        board.pieces[_case].code
//                = abs(stone) == 0 ? 6
//                : abs(stone) == 6 ? 5
//                : abs(stone) == 5 ? 0 : abs(stone);
//        board.pieces[_case].couleur
//                = stone < 0 ? LIGHT : stone > 0 ? DARK : EMPTY;
    }

    fun abs(x: Int): Int {
        return if (x < 0) -x else x
    }

    fun fenCharToStone(ch: Char): Int {
        for (stone in MIN_STONE..MAX_STONE) {
            if (fenChars[stone - MIN_STONE] == ch) {
                return stone
            }
        }
        return NO_STONE.toInt()
    }

    fun strToSqi(s: String?): Int {
        if (s == null || s.length != 2) {
            return NO_SQUARE
        }
        val col = charToCol(s[0])
        if (col == NO_COL) {
            return NO_SQUARE
        }
        val row = charToRow(s[1])
        if (row == NO_ROW) {
            return NO_SQUARE
        }
        return row * 8 + col
    }

    fun charToCol(ch: Char): Int {
        return if ((ch >= 'a') && (ch <= 'h')) {
            ch.code - 'a'.code
        } else {
            NO_COL
        }
    }

    fun charToRow(ch: Char): Int {
        return if ((ch >= '1') && (ch <= '8')) {
            ch.code - '1'.code
        } else {
            NO_ROW
        }
    }


}
