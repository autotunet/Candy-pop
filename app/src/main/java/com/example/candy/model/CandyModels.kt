package com.example.candy.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

data class Position(val row: Int, val col: Int) {
    fun isAdjacent(other: Position): Boolean {
        val rowDiff = kotlin.math.abs(row - other.row)
        val colDiff = kotlin.math.abs(col - other.col)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }
}

enum class CandyColor(
    val displayName: String,
    val primaryColor: Color,
    val lightColor: Color,
    val darkColor: Color
) {
    RED("Strawberry", CandyRed, Color(0xFFFF8A80), Color(0xFFC62828)),
    YELLOW("Lemon Drop", CandyYellow, Color(0xFFFFFF8D), Color(0xFFF57F17)),
    ORANGE("Orange Jelly", CandyOrange, Color(0xFFFFD180), Color(0xFFE65100)),
    GREEN("Apple Mint", CandyGreen, Color(0xFFB9F6CA), Color(0xFF2E7D32)),
    BLUE("Blueberry Pop", CandyBlue, Color(0xFF80D8FF), Color(0xFF01579B)),
    PURPLE("Grape Star", CandyPurple, Color(0xFFEA80FC), Color(0xFF6A1B9A))
}

enum class SpecialCandyType {
    NONE,
    STRIPED_HORIZONTAL,
    STRIPED_VERTICAL,
    WRAPPED,
    COLOR_BOMB
}

data class CandyTile(
    val id: Long,
    val row: Int,
    val col: Int,
    val color: CandyColor,
    val special: SpecialCandyType = SpecialCandyType.NONE,
    val hasJelly: Boolean = false,
    val isMatched: Boolean = false,
    val isClearing: Boolean = false
)

enum class BoosterType(
    val title: String,
    val iconName: String,
    val description: String
) {
    LOLLIPOP_HAMMER("Lollipop Hammer", "🔨", "Smash any single candy or jelly"),
    FREE_SWITCH("Free Switch", "⇄", "Swap any two candies without spending a move"),
    COLOR_BOMB("Color Bomb", "💣", "Place a rainbow color bomb anywhere on the board"),
    EXTRA_MOVES("+5 Moves", "+5", "Add 5 extra moves to keep playing")
}

sealed interface LevelGoal {
    val current: Int
    val target: Int
    val isCompleted: Boolean get() = current >= target

    data class ScoreGoal(
        override val current: Int,
        override val target: Int
    ) : LevelGoal

    data class JellyGoal(
        override val current: Int,
        override val target: Int
    ) : LevelGoal

    data class CollectCandyGoal(
        val candyColor: CandyColor,
        override val current: Int,
        override val target: Int
    ) : LevelGoal

    data class CreateSpecialGoal(
        val specialType: SpecialCandyType,
        override val current: Int,
        override val target: Int
    ) : LevelGoal
}

data class LevelDef(
    val levelNumber: Int,
    val name: String,
    val maxMoves: Int,
    val starScores: Triple<Int, Int, Int>,
    val targetScore: Int,
    val targetJellies: Int = 0,
    val jellyPositions: Set<Position> = emptySet(),
    val targetIngredients: Map<CandyColor, Int> = emptyMap(),
    val targetSpecials: Map<SpecialCandyType, Int> = emptyMap(),
    val flavorQuote: String = "Sweet taste of victory!",
    val timeLimitSeconds: Int? = null
)

data class Particle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float = 1f,
    val shape: ParticleShape = ParticleShape.CIRCLE
)

enum class ParticleShape {
    CIRCLE, STAR, SPARKLE
}

data class FloatingScore(
    val id: Long,
    val text: String,
    val col: Int,
    val row: Int,
    val color: Color
)

data class Shockwave(
    val id: Long,
    val col: Int,
    val row: Int,
    val color: Color,
    val durationMs: Int = 450
)

data class LaserBeam(
    val id: Long,
    val isHorizontal: Boolean,
    val lineIndex: Int,
    val color: Color,
    val durationMs: Int = 380
)

data class RainbowTentacle(
    val id: Long,
    val fromPos: Position,
    val toPos: Position,
    val color: Color,
    val durationMs: Int = 500
)

data class PowerUpCreationEffect(
    val id: Long,
    val col: Int,
    val row: Int,
    val specialType: SpecialCandyType,
    val color: Color
)
