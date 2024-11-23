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

    var squareColors = IntArray(BOARD_SIZE)
    var squarePieces = IntArray(BOARD_SIZE)
    var pieceInstances = arrayOfNulls<Piece>(BOARD_SIZE)
    var currentSide = 0
    var opponentSide = 0
    var castleRights = 0
    var enPassantSquare = 0
    var moveList: MutableList<Move> = ArrayList()
    var halfMoveCount = 0
    var currentPly = 0
    private var fiftyMoveRuleCount = 0
    private var undoMove = UndoMove()

    constructor() {
        (0 until BOARD_SIZE).forEach { c -> pieceInstances[c] = Piece() }
    }

    constructor(board: Board) {
        arraycopy(board.squareColors, 0, squareColors, 0, BOARD_SIZE)
        arraycopy(board.squarePieces, 0, squarePieces, 0, BOARD_SIZE)
        currentSide = board.currentSide
        opponentSide = board.opponentSide
        castleRights = board.castleRights
        enPassantSquare = board.enPassantSquare
        fiftyMoveRuleCount = board.fiftyMoveRuleCount
        moveList = ArrayList()
        undoMove = UndoMove()
    }

    fun isSquareAttacked(sqTarget: Int, side: Int): Boolean {
        return (0 until BOARD_SIZE).any { sq ->
            squareColors[sq] == side && pieceAttacksSquare(sq, sqTarget, squarePieces[sq], side)
        }
    }

    fun hasMatchingOffset(offsets: IntRange, checkCondition: (Int) -> Boolean): Boolean =
        offsets.any(checkCondition) // Vérifie si au moins un offset satisfait la condition donnée

    fun pieceAttacksSquare(sq: Int, sqTarget: Int, pieceType: Int, side: Int): Boolean {
        return if (pieceType == PAWN) {
            isPawnAttacked(sq, sqTarget, side)
        } else {
            hasMatchingOffset(0 until offsets[pieceType]) { attackViaOffset(sq, sqTarget, pieceType, it) }
        }
    }

    fun isPawnAttacked(sq: Int, sqTarget: Int, side: Int): Boolean {
        val offset = if (side == LIGHT) UP_OFFSET else DOWN_OFFSET
        return ((sq and RIGHT_DIAGONAL_OFFSET) != 0 && sq + offset - 1 == sqTarget) || // Vérifie la capture à gauche
                ((sq and RIGHT_DIAGONAL_OFFSET) != RIGHT_DIAGONAL_OFFSET && sq + offset + 1 == sqTarget)    // Vérifie la capture à droite
    }

    fun attackViaOffset(sq: Int, sqTarget: Int, pieceType: Int, offsetIndex: Int): Boolean {
        var currentSquare = sq
        while (true) {
            currentSquare = mailbox[mailbox64[currentSquare] + offset[pieceType][offsetIndex]]
            if (currentSquare == INVALID_INDEX) break // Hors des limites du plateau
            if (currentSquare == sqTarget) return true
            if (squareColors[currentSquare] != EMPTY || !slide[pieceType]) break // Obstacle ou pièce non glissante
        }
        return false
    }

    fun generateMoves() {
        range(0, BOARD_SIZE)
            .filter { c: Int -> squareColors[c] == currentSide }
            .forEach { c: Int ->
                if (squarePieces[c] == PAWN) generatePawnMoves(c)
                else generateMoves(c)
            }
        generateCastlingMoves()
        generateEnPassantMoves()
    }

    fun generatePawnMoves(c: Int) {
        val offset = if (currentSide == LIGHT) UP_OFFSET else DOWN_OFFSET
        (currentSide xor 1).also { generatePawnCaptures(c, offset, it) }

        // Génération du mouvement simple du pion vers l'avant
        when {
            EMPTY == squareColors[c + offset] -> {
                addMove(c, c + offset, 16)
                // Génération du double mouvement initial du pion
                when {
                    isPawnOnStartRank(c) -> if (EMPTY == squareColors[c + (offset shl 1)])
                        addMove(c, c + (offset shl 1), 24)
                }
            }
        }
    }

    fun generatePawnCaptures(c: Int, offset: Int, oppositeColor: Int) {
        val leftCapture = c + offset - 1
        val rightCapture = c + offset + 1

        if (c and RIGHT_DIAGONAL_OFFSET != 0 && squareColors[leftCapture] == oppositeColor) addMove(
            c,
            leftCapture,
            PROMOTION_MOVE_FLAG
        )
        if (c and RIGHT_DIAGONAL_OFFSET != RIGHT_DIAGONAL_OFFSET && squareColors[rightCapture] == oppositeColor) addMove(
            c,
            rightCapture,
            PROMOTION_MOVE_FLAG
        )
    }

    fun isPawnOnStartRank(c: Int): Boolean {
        return currentSide == LIGHT && c in 48..55 || currentSide == DARK && c in 8..15
    }

    fun generateEnPassantMoves() {
        when {
            enPassantSquare != INVALID_INDEX -> {
                val offsets = if (currentSide == LIGHT) listOf(RIGHT_DIAGONAL_OFFSET, 9) else listOf(
                    UP_LEFT_OFFSET,
                    LEFT_DIAGONAL_OFFSET
                )
                val targetColor = if (currentSide == LIGHT) LIGHT else DARK
                offsets.forEach { offset ->
                    val newEp = enPassantSquare + offset
                    if (enPassantSquare and RIGHT_DIAGONAL_OFFSET != (if (offset == offsets[0]) 0 else RIGHT_DIAGONAL_OFFSET))
                        if (squareColors[newEp] == targetColor && squarePieces[newEp] == PAWN) addMove(
                            newEp,
                            enPassantSquare,
                            21
                        )
                }
            }
        }

    }

    fun generateCastlingMoves() {

        val (kingStart, kingsideTarget, towersideTarget) = if (currentSide == LIGHT) Triple(E1, G1, C1) else Triple(
            E8,
            G8,
            C8
        )

        if ((castleRights and (if (currentSide == LIGHT) 1 else 4)) != 0) addMove(
            kingStart,
            kingsideTarget,
            CASTLE_KINGSIDE_FLAG
        )
        if ((castleRights and (if (currentSide == LIGHT) CASTLE_KINGSIDE_FLAG else DOWN_OFFSET)) != 0) addMove(
            kingStart,
            towersideTarget,
            CASTLE_KINGSIDE_FLAG
        )

    }

    fun generateMoves(c: Int) {

        val p = squarePieces[c]

        (0 until offsets[p]).forEach { d ->
            var to = c
            var continueDirection = true
            while (continueDirection) {
                to = mailbox[mailbox64[to] + offset[p][d]]
                when {
                    to == INVALID_INDEX -> continueDirection = false
                    squareColors[to] != EMPTY -> {
                        if (squareColors[to] == opponentSide) addMove(c, to, 1)
                        continueDirection = false
                    }

                    else -> {
                        addMove(c, to, 0)
                        if (!slide[p]) continueDirection = false
                    }
                }
            }
        }
    }

    fun addMove(from: Int, to: Int, bits: Int) {
        if (isPromotion(to, bits)) {
            addPromotionMoves(from, to, bits, moveList)
        } else {
            moveList.add(Move(from, to, 0, bits))
        }
    }

    fun isPromotion(to: Int, bits: Int): Boolean {
        return (bits and 16) != 0 && when (currentSide) {
            LIGHT -> to <= H8
            DARK -> to >= A1
            else -> false
        }
    }

    fun addPromotionMoves(from: Int, to: Int, bits: Int, movesList: MutableList<Move>) {
        (KNIGHT..QUEEN).forEach { promotionPiece ->
            movesList.add(Move(from, to, promotionPiece, (bits or CASTLE_QUEENSIDE_FLAG)))
        }
    }

    fun makeMove(m: Move): Boolean {
        // Gérer le roque (si applicable)
        if (m.bits and CASTLE_KINGSIDE_FLAG != 0) {
            val from: Int
            val to: Int

            // Vérifier si le roi est en échec avant le roque
            if (isInCheck(this, currentSide)) return false

            // Déterminer les positions de la tour pour le roque
            when (m.to) {
                G1 -> { // Roque roi côté blanc
                    if (squareColors[F1] != EMPTY || squareColors[G1] != EMPTY ||
                        isSquareAttacked(F1, opponentSide) || isSquareAttacked(G1, opponentSide)
                    ) return false
                    from = H1
                    to = F1
                }

                C1 -> { // Roque dame côté blanc
                    if (squareColors[B1] != EMPTY || squareColors[C1] != EMPTY || squareColors[D1] != EMPTY ||
                        isSquareAttacked(C1, opponentSide) || isSquareAttacked(D1, opponentSide)
                    ) return false
                    from = A1
                    to = D1
                }

                G8 -> { // Roque roi côté noir
                    if (squareColors[F8] != EMPTY || squareColors[G8] != EMPTY ||
                        isSquareAttacked(F8, opponentSide) || isSquareAttacked(G8, opponentSide)
                    ) return false
                    from = H8
                    to = F8
                }

                C8 -> { // Roque dame côté noir
                    if (squareColors[B8] != EMPTY || squareColors[C8] != EMPTY || squareColors[D8] != EMPTY ||
                        isSquareAttacked(C8, opponentSide) || isSquareAttacked(D8, opponentSide)
                    ) return false
                    from = A8
                    to = D8
                }

                else -> {
                    from = INVALID_INDEX
                    to = INVALID_INDEX
                }
            }

            // Déplacer la tour pour le roque
            if (from != INVALID_INDEX && to != INVALID_INDEX) {
                setSquare(squareColors, squarePieces, to, currentSide, ROOK)
                clearSquare(squareColors, squarePieces, from)
            }
        }

        // Sauvegarder l'état pour un éventuel retour en arrière
        undoMove.mov = m
        undoMove.capture = squarePieces[m.to]
        undoMove.castle = castleRights
        undoMove.ep = enPassantSquare
        undoMove.fifty = fiftyMoveRuleCount

        // Mettre à jour les drapeaux de roque
        castleRights = castleRights and (castle_mask[m.from.toInt()] and castle_mask[m.to.toInt()])

        // Mettre à jour le pion passant (si applicable)
        enPassantSquare = if (m.bits and DOWN_OFFSET != 0) { // Bit DOWN_OFFSET : double déplacement de pion
            if (currentSide == LIGHT) m.to + DOWN_OFFSET else m.to - DOWN_OFFSET
        } else {
            INVALID_INDEX
        }

        // Réinitialiser ou incrémenter la règle des FIFTY_MOVE_RULE coups
        fiftyMoveRuleCount = if (m.bits and PROMOTION_MOVE_FLAG != 0) 0 else fiftyMoveRuleCount + 1

        // Déplacer la pièce
        setSquare(
            squareColors,
            squarePieces,
            m.to,
            currentSide,
            if ((m.bits and CASTLE_QUEENSIDE_FLAG) != 0) m.promote else squarePieces[m.from] // Promotion
        )
        clearSquare(squareColors, squarePieces, m.from)

        // Gérer la prise en passant
        if (m.bits and 4 != 0) { // Bit 4 : prise en passant
            val offset = if (currentSide == LIGHT) DOWN_OFFSET else UP_OFFSET
            clearSquare(squareColors, squarePieces, m.to + offset)
        }

        // Passer au camp suivant
        currentSide = currentSide xor 1
        opponentSide = opponentSide xor 1

        // Vérifier si le roi adverse est en échec après le coup
        return if (isInCheck(this, opponentSide)) {
            undoMove() // Annuler le coup si le roi est en échec
            false
        } else {
            true
        }
    }

    fun rookMovePositions(mTo: Int): Pair<Int, Int> {
        return when (mTo) {
            MAX_SQUARE_INDEX -> F1 to H1 // Roque roi côté blanc
            58 -> D1 to A1 // Roque dame côté blanc
            6 -> F8 to H8  // Roque roi côté noir
            CASTLE_KINGSIDE_FLAG -> D8 to A8  // Roque dame côté noir
            else -> INVALID_INDEX to INVALID_INDEX // Aucun roque
        }
    }

    fun undoMove() {
        // Inverser les valeurs de 'side' et 'xside'
        currentSide = currentSide xor 1
        opponentSide = opponentSide xor 1

        // Extraire les données de mouvement
        val m = undoMove.mov
        castleRights = undoMove.castle
        enPassantSquare = undoMove.ep
        fiftyMoveRuleCount = undoMove.fifty

        // Mettre à jour la position de départ
        setSquare(
            squareColors,
            squarePieces,
            m.from,
            currentSide,
            if ((m.bits and CASTLE_QUEENSIDE_FLAG) != 0) PAWN else squarePieces[m.to]
        )

        // Mettre à jour la position de destination en fonction de la capture
        when (undoMove.capture) {
            EMPTY -> clearSquare(squareColors, squarePieces, m.to)
            else -> setSquare(squareColors, squarePieces, m.to, opponentSide, undoMove.capture)
        }

        // Gérer les roques (si le mouvement était un roque)
        when {
            m.bits and CASTLE_KINGSIDE_FLAG != 0 -> {
                val (rookFrom, rookTo) = rookMovePositions(m.to)
                moveRookForCastling(squareColors, squarePieces, rookFrom, rookTo, currentSide)
            }

            m.bits and 4 != 0 -> {
                val offset = if (currentSide == LIGHT) DOWN_OFFSET else UP_OFFSET
                setSquare(squareColors, squarePieces, m.to + offset, opponentSide, PAWN)
            }
        }
    }

    fun isInCheck(board: Board, side: Int): Boolean {
        return (0 until BOARD_SIZE).any { i ->
            board.squarePieces[i] == KING && board.squareColors[i] == side &&
                    board.isSquareAttacked(i, side xor 1)
        }
    }

    fun setSquare(colorArray: IntArray, pieceArray: IntArray, square: Int, newColor: Int, newPiece: Int) {
        // Met à jour la couleur et le type de pièce pour la case donnée
        colorArray[square] = newColor
        pieceArray[square] = newPiece
    }

    fun clearSquare(colorArray: IntArray, pieceArray: IntArray, square: Int) {
        // Réinitialise la case donnée en la marquant comme vide
        colorArray[square] = EMPTY
        pieceArray[square] = EMPTY
    }

    fun moveRookForCastling(colorArray: IntArray, pieceArray: IntArray, from: Int, to: Int, side: Int) {
        // Vérifie si les positions de départ et d'arrivée sont valides
        if (from != INVALID_INDEX && to != INVALID_INDEX) {
            setSquare(colorArray, pieceArray, to, side, ROOK) // Place la tour à la nouvelle position
            clearSquare(colorArray, pieceArray, from)        // Vide la case d'origine
        }
    }

}
