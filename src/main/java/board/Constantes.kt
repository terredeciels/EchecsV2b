package board


open class Constantes {
    val BOARD_SIZE: Int = 64
    val fenChars: CharArray = charArrayOf('K', 'P', 'Q', 'R', 'B', 'N', '-', 'n', 'b', 'r', 'q', 'p', 'k')
    val NO_CASTLES: Int = 0
    val WHITE_LONG_CASTLE: Int = 1
    val WHITE_SHORT_CASTLE: Int = 2
    val BLACK_LONG_CASTLE: Int = 4
    val BLACK_SHORT_CASTLE: Int = 8
    val MIN_STONE: Short = -6
    val MAX_STONE: Short = 6
    val NO_STONE: Short = 0
    val NO_COL: Int = -1
    val NO_ROW: Int = -1
    val NO_SQUARE: Int = -1
    val LIGHT: Int = 0
    val DARK: Int = 1


    val A1: Int = 56
    val B1: Int = 57
    val C1: Int = 58
    val D1: Int = 59
    val E1: Int = 60
    val F1: Int = 61
    val G1: Int = 62
    val H1: Int = 63
    val A8: Int = 0
    val B8: Int = 1
    val C8: Int = 2
    val D8: Int = 3
    val E8: Int = 4
    val F8: Int = 5
    val G8: Int = 6
    val H8: Int = 7

    open val mailbox: IntArray = intArrayOf(
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        -1,
        -1,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        -1,
        -1,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23,
        -1,
        -1,
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        -1,
        -1,
        32,
        33,
        34,
        35,
        36,
        37,
        38,
        39,
        -1,
        -1,
        40,
        41,
        42,
        43,
        44,
        45,
        46,
        47,
        -1,
        -1,
        48,
        49,
        50,
        51,
        52,
        53,
        54,
        55,
        -1,
        -1,
        56,
        57,
        58,
        59,
        60,
        61,
        62,
        63,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1
    )

    open val mailbox64: IntArray = intArrayOf(
        21,
        22,
        23,
        24,
        25,
        26,
        27,
        28,
        31,
        32,
        33,
        34,
        35,
        36,
        37,
        38,
        41,
        42,
        43,
        44,
        45,
        46,
        47,
        48,
        51,
        52,
        53,
        54,
        55,
        56,
        57,
        58,
        61,
        62,
        63,
        64,
        65,
        66,
        67,
        68,
        71,
        72,
        73,
        74,
        75,
        76,
        77,
        78,
        81,
        82,
        83,
        84,
        85,
        86,
        87,
        88,
        91,
        92,
        93,
        94,
        95,
        96,
        97,
        98
    )

    val PAWN: Int = 0
    val KNIGHT: Int = 1
    val ROOK: Int = 3
    val QUEEN: Int = 4
    val KING: Int = 5
    val EMPTY: Int = 6
    open val slide: BooleanArray = booleanArrayOf(false, false, true, true, true, false)
    val offsets: IntArray = intArrayOf(0, 8, 4, 4, 8, 8)
    private val P: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    private val C: IntArray = intArrayOf(-21, -19, -12, -8, 8, 12, 19, 21)
    private val F: IntArray = intArrayOf(-11, -9, 9, 11, 0, 0, 0, 0)
    private val T: IntArray = intArrayOf(-10, -1, 1, 10, 0, 0, 0, 0)
    private val D: IntArray = intArrayOf(-11, -10, -9, -1, 1, 9, 10, 11)
    private val R: IntArray = intArrayOf(-11, -10, -9, -1, 1, 9, 10, 11)

    open val offset: Array<IntArray> = arrayOf(P, C, F, T, D, R)


    val castle_mask: IntArray = intArrayOf(
        7,
        15,
        15,
        15,
        3,
        15,
        15,
        11,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        15,
        13,
        15,
        15,
        15,
        12,
        15,
        15,
        14
    )
}
