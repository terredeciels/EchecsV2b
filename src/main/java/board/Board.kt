package board

import java.lang.System.arraycopy
import java.util.stream.IntStream.range

class Piece

class Move(var from: Int = 0, var to: Int = 0, var promote: Int = 0, var bits: Int = 0)

class UndoMove {
    var mov = Move()
    var capture = 0
    var castle = 0
    var ep = 0
    var fifty = 0 //public int hash;
}

class Board : Constantes {

    var color = IntArray(BOARD_SIZE)
    var piece = IntArray(BOARD_SIZE)
    var pieces = arrayOfNulls<Piece>(BOARD_SIZE)
    var side = 0
    var xside = 0
    var castle = 0
    var ep = 0
    var pseudomoves: MutableList<Move> = ArrayList()
    var halfMoveClock = 0
    var plyNumber = 0
    private var fifty = 0
    private var um = UndoMove()

    constructor() {
        (0 until BOARD_SIZE).forEach { c -> pieces[c] = Piece() }
    }

    constructor(board: Board) {
        arraycopy(board.color, 0, color, 0, BOARD_SIZE)
        arraycopy(board.piece, 0, piece, 0, BOARD_SIZE)
        side = board.side
        xside = board.xside
        castle = board.castle
        ep = board.ep
        fifty = board.fifty
        pseudomoves = ArrayList()
        um = UndoMove()
    }

    fun isAttacked(sqTarget: Int, side: Int): Boolean {
        return range(0, BOARD_SIZE)
            .filter { sq: Int -> color[sq] == side }
            .anyMatch { sq: Int -> isAttackedByPiece(sq, sqTarget, piece[sq], side) }
    }

    fun anyMatchingOffsets(offsets: IntRange, checkCondition: (Int) -> Boolean) = offsets.any(checkCondition)

    fun isAttackedByPiece(sq: Int, sqTarget: Int, pieceType: Int, side: Int): Boolean {
        return when (pieceType) {
            PAWN -> isPawnAttacked(sq, sqTarget, side)
            else -> anyMatchingOffsets(0 until offsets[pieceType]) { isAttackedByOffset(sq, sqTarget, pieceType, it) }
        }
    }

    fun isPawnAttacked(sq: Int, sqTarget: Int, side: Int): Boolean {
        val offset = if (side == LIGHT) -8 else 8
        return (sq and 7) != 0 && sq + offset - 1 == sqTarget ||
                (sq and 7) != 7 && sq + offset + 1 == sqTarget
    }

    fun isAttackedByOffset(sq: Int, sqTarget: Int, pieceType: Int, offsetIndex: Int): Boolean {
        var sqIndex = sq
        while (mailbox[mailbox64[sqIndex] + offset[pieceType][offsetIndex]].also { sqIndex = it } != -1) {
            if (sqIndex == sqTarget) return true
            if (color[sqIndex] != EMPTY || !slide[pieceType]) break
        }
        return false
    }

    fun gen() {
        range(0, BOARD_SIZE)
            .filter { c: Int -> color[c] == side }
            .forEach { c: Int ->
                if (piece[c] == PAWN) genPawn(c)
                else gen(c)
            }
        genCastles()
        genEnpassant()
    }

    fun genPawn(c: Int) {
        val offset = if (side == LIGHT) -8 else 8
        (side xor 1).also { generatePawnCaptures(c, offset, it) }

        // Génération du mouvement simple du pion vers l'avant
        when {
            EMPTY == color[c + offset] -> {
                genPush(c, c + offset, 16)
                // Génération du double mouvement initial du pion
                when {
                    isPawnStartingRank(c) -> if (EMPTY == color[c + (offset shl 1)])
                        genPush(c, c + (offset shl 1), 24)
                }
            }
        }
    }

    fun generatePawnCaptures(c: Int, offset: Int, oppositeColor: Int) {
        val leftCapture = c + offset - 1
        val rightCapture = c + offset + 1

        if (c and 7 != 0 && color[leftCapture] == oppositeColor) genPush(c, leftCapture, 17)
        if (c and 7 != 7 && color[rightCapture] == oppositeColor) genPush(c, rightCapture, 17)
    }

    fun isPawnStartingRank(c: Int): Boolean {
        return side == LIGHT && c in 48..55 || side == DARK && c in 8..15
    }

    fun genEnpassant() {
        when {
            ep != -1 -> {
                val offsets = if (side == LIGHT) listOf(7, 9) else listOf(-9, -7)
                val targetColor = if (side == LIGHT) LIGHT else DARK
                offsets.forEach { offset ->
                    val newEp = ep + offset
                    if (ep and 7 != (if (offset == offsets[0]) 0 else 7))
                        if (color[newEp] == targetColor && piece[newEp] == PAWN) genPush(newEp, ep, 21)
                }
            }
        }

    }

    fun genCastles() {

        val (kingStart, kingsideTarget, towersideTarget) = if (side == LIGHT) Triple(E1, G1, C1) else Triple(E8, G8, C8)

        if ((castle and (if (side == LIGHT) 1 else 4)) != 0) genPush(kingStart, kingsideTarget, 2)
        if ((castle and (if (side == LIGHT) 2 else 8)) != 0) genPush(kingStart, towersideTarget, 2)

    }

    fun gen(c: Int) {

        val p = piece[c]

        (0 until offsets[p]).forEach { d ->
            var to = c
            var continueDirection = true
            while (continueDirection) {
                to = mailbox[mailbox64[to] + offset[p][d]]
                when {
                    to == -1 -> continueDirection = false
                    color[to] != EMPTY -> {
                        if (color[to] == xside) genPush(c, to, 1)
                        continueDirection = false
                    }

                    else -> {
                        genPush(c, to, 0)
                        if (!slide[p]) continueDirection = false
                    }
                }
            }
        }
    }

    fun genPush(from: Int, to: Int, bits: Int) {
        if (isPromotionMove(to, bits)) {
            generatePromotions(from, to, bits, pseudomoves)
        } else {
            pseudomoves.add(Move(from, to, 0, bits))
        }
    }

    private fun isPromotionMove(to: Int, bits: Int): Boolean {
        return (bits and 16) != 0 && when (side) {
            LIGHT -> to <= H8
            DARK -> to >= A1
            else -> false
        }
    }

    fun generatePromotions(from: Int, to: Int, bits: Int, movesList: MutableList<Move>) {
        (KNIGHT..QUEEN).forEach { promotionPiece ->
            movesList.add(Move(from, to, promotionPiece, (bits or 32)))
        }
    }

    fun makemove(m: Move): Boolean {
        // Gérer le roque (si applicable)
        if (m.bits and 2 != 0) {
            val from: Int
            val to: Int

            if (inCheck(this, side)) return false

            when (m.to) {
                G1 -> {
                    if (color[F1] != EMPTY || color[G1] != EMPTY || isAttacked(F1, xside) || isAttacked(
                            G1,
                            xside
                        )
                    ) return false
                    from = H1
                    to = F1
                }

                C1 -> {
                    if (color[B1] != EMPTY || color[C1] != EMPTY || color[D1] != EMPTY || isAttacked(
                            C1,
                            xside
                        ) || isAttacked(D1, xside)
                    ) return false
                    from = A1
                    to = D1
                }

                G8 -> {
                    if (color[F8] != EMPTY || color[G8] != EMPTY || isAttacked(F8, xside) || isAttacked(
                            G8,
                            xside
                        )
                    ) return false
                    from = H8
                    to = F8
                }

                C8 -> {
                    if (color[B8] != EMPTY || color[C8] != EMPTY || color[D8] != EMPTY || isAttacked(
                            C8,
                            xside
                        ) || isAttacked(D8, xside)
                    ) return false
                    from = A8
                    to = D8
                }

                else -> {
                    from = -1
                    to = -1
                }
            }

            when {
                from != -1 && to != -1 -> {
                    updateSquare(color, piece, to, side, ROOK)
                    clearSquare(color, piece, from)
                }
            }
        }

        // Sauvegarder les informations pour un éventuel retour en arrière
        um.mov = m
        um.capture = piece[m.to]
        um.castle = castle
        um.ep = ep
        um.fifty = fifty

        // Mettre à jour les drapeaux de roque
        castle = castle and (castle_mask[m.from.toInt()] and castle_mask[m.to.toInt()])

        // Mettre à jour le pion passant (si applicable)
        ep = if (m.bits and 8 != 0) {
            if (side == LIGHT) m.to + 8 else m.to - 8
        } else {
            -1
        }

        // Réinitialiser la règle des 50 coups ou l'incrémenter
        fifty = if (m.bits and 17 != 0) 0 else fifty + 1

        // Déplacer la pièce
        updateSquare(color, piece, m.to, side, if ((m.bits and 32) != 0) m.promote else piece[m.from])
        clearSquare(color, piece, m.from)

        // Gérer la prise en passant (si applicable)
        if (m.bits and 4 != 0) {
            val offset = if (side == LIGHT) 8 else -8
            clearSquare(color, piece, m.to + offset)
        }

        // Passer au camp suivant
        side = side xor 1
        xside = xside xor 1

        // Vérifier si le roi adverse est en échec après le coup
        when {
            inCheck(this, xside) -> {
                takeback()
                return false
            }

            else -> return true
        }
    }

    fun getRookMovePositions(mTo: Int): Pair<Int, Int> {
        return when (mTo) {
            62 -> F1 to H1
            58 -> D1 to A1
            6 -> F8 to H8
            2 -> D8 to A8
            else -> -1 to -1
        }
    }

    fun takeback() {
        // Inverser les valeurs de 'side' et 'xside'
        side = side xor 1
        xside = xside xor 1

        // Extraire les données de mouvement
        val m = um.mov
        castle = um.castle
        ep = um.ep
        fifty = um.fifty

        // Mettre à jour la position de départ
        updateSquare(color, piece, m.from, side, if ((m.bits and 32) != 0) PAWN else piece[m.to])

        // Mettre à jour la position de destination en fonction de la capture
        when (um.capture) {
            EMPTY -> clearSquare(color, piece, m.to)
            else -> updateSquare(color, piece, m.to, xside, um.capture)
        }

        // Gérer les roques (si le mouvement était un roque)
        when {
            m.bits and 2 != 0 -> {
                val (rookFrom, rookTo) = getRookMovePositions(m.to)
                updateRookForCastling(color, piece, rookFrom, rookTo, side)
            }

            m.bits and 4 != 0 -> {
                val offset = if (side == LIGHT) 8 else -8
                updateSquare(color, piece, m.to + offset, xside, PAWN)
            }
        }
    }

    fun inCheck(board: Board, s: Int): Boolean {
        return range(0, BOARD_SIZE)
            .filter { i: Int -> board.piece[i] == KING && board.color[i] == s }
            .anyMatch { i: Int -> with(board) { isAttacked(i, s xor 1) } }
    }

    fun updateSquare(colorArray: IntArray, pieceArray: IntArray, square: Int, newColor: Int, newPiece: Int) {
        colorArray[square] = newColor
        pieceArray[square] = newPiece
    }

    fun clearSquare(colorArray: IntArray, pieceArray: IntArray, square: Int) {
        colorArray[square] = EMPTY
        pieceArray[square] = EMPTY
    }

    fun updateRookForCastling(colorArray: IntArray, pieceArray: IntArray, from: Int, to: Int, side: Int) {
        when {
            from != -1 && to != -1 -> {
                updateSquare(colorArray, pieceArray, to, side, ROOK)
                clearSquare(colorArray, pieceArray, from)
            }
        }
    }

}
