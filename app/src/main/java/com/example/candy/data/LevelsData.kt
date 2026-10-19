package com.example.candy.data

import com.example.candy.model.CandyColor
import com.example.candy.model.LevelDef
import com.example.candy.model.Position
import com.example.candy.model.SpecialCandyType
import kotlin.random.Random

object LevelsData {
    const val TOTAL_LEVELS = 1000

    private val levelCache = mutableMapOf<Int, LevelDef>()

    val levels: List<LevelDef> by lazy {
        (1..TOTAL_LEVELS).map { getLevel(it) }
    }

    fun getLevel(number: Int): LevelDef {
        val validNum = number.coerceIn(1, TOTAL_LEVELS)
        return levelCache.getOrPut(validNum) {
            generateLevel(validNum)
        }
    }

    fun getWorldNumber(levelNumber: Int): Int = ((levelNumber - 1) / 20) + 1

    fun getWorldName(worldNum: Int, isSpanish: Boolean): String {
        return if (isSpanish) {
            when (worldNum % 10) {
                1 -> "Mundo $worldNum: Pradera de Azúcar"
                2 -> "Mundo $worldNum: Arboleda de Fresas"
                3 -> "Mundo $worldNum: Laguna de Gelatina"
                4 -> "Mundo $worldNum: Cumbres Rayadas"
                5 -> "Mundo $worldNum: Carnaval Arcoíris"
                6 -> "Mundo $worldNum: Valle de Chocolate"
                7 -> "Mundo $worldNum: Bosque de Gomitas"
                8 -> "Mundo $worldNum: Dunas de Caramelo"
                9 -> "Mundo $worldNum: Glaciar de Menta"
                else -> "Mundo $worldNum: Ciudadela del Rey Dulce"
            }
        } else {
            when (worldNum % 10) {
                1 -> "World $worldNum: Sugar Meadow"
                2 -> "World $worldNum: Strawberry Grove"
                3 -> "World $worldNum: Jelly Lagoon"
                4 -> "World $worldNum: Striped Peaks"
                5 -> "World $worldNum: Rainbow Carnival"
                6 -> "World $worldNum: Chocolate Valley"
                7 -> "World $worldNum: Gummy Forest"
                8 -> "World $worldNum: Caramel Dunes"
                9 -> "World $worldNum: Peppermint Glacier"
                else -> "World $worldNum: Sugar King's Citadel"
            }
        }
    }

    private fun generateLevel(levelNum: Int): LevelDef {
        // Deterministic pseudo-random seed per level
        val rnd = Random(levelNum * 31337L)
        val isBossLevel = (levelNum % 10 == 0)

        // Archetypes: 0=Score, 1=Jellies, 2=Ingredients, 3=Specials, 4=Jellies+Ingredients, 5=Specials+Score
        val archetype = if (isBossLevel) 4 else ((levelNum - 1) % 6)

        // Target Score formula: scales progressively with level number
        val baseScore = 1200 + (levelNum * 65) + rnd.nextInt(200)
        val targetScore = if (isBossLevel) (baseScore * 1.35f).toInt() else baseScore

        val star1 = (targetScore * 0.85f).toInt()
        val star2 = (targetScore * 1.45f).toInt()
        val star3 = (targetScore * 2.2f).toInt()

        // Moves allowed: balanced between 20 and 28 moves
        val maxMoves = when {
            isBossLevel -> (24 + (rnd.nextInt(5))).coerceAtMost(30)
            levelNum <= 10 -> (22 + (10 - levelNum) / 2).coerceAtLeast(20)
            else -> 20 + rnd.nextInt(6)
        }

        var jelliesCount = 0
        var jellySet: Set<Position> = emptySet()
        val ingredientsMap = mutableMapOf<CandyColor, Int>()
        val specialsMap = mutableMapOf<SpecialCandyType, Int>()

        when (archetype) {
            0 -> {
                // Pure Score Attack
            }
            1, 4 -> {
                // Jelly Hunt / Boss Jelly Challenge
                jellySet = generateJellyPositions(levelNum, rnd, isBoss = isBossLevel)
                jelliesCount = jellySet.size
                if (isBossLevel) {
                    val color = CandyColor.values()[rnd.nextInt(CandyColor.values().size)]
                    ingredientsMap[color] = 15 + (levelNum / 50)
                }
            }
            2 -> {
                // Ingredient Harvest
                val colors = CandyColor.values().toList().shuffled(rnd)
                val count1 = 12 + (levelNum / 40).coerceAtMost(20)
                val count2 = 12 + (levelNum / 40).coerceAtMost(20)
                ingredientsMap[colors[0]] = count1
                ingredientsMap[colors[1]] = count2
            }
            3 -> {
                // Specials Master (Create Striped, Wrapped, or Color Bomb)
                if (levelNum >= 25 && rnd.nextBoolean()) {
                    specialsMap[SpecialCandyType.COLOR_BOMB] = 1
                } else if (rnd.nextBoolean()) {
                    specialsMap[SpecialCandyType.WRAPPED] = 2
                } else {
                    specialsMap[SpecialCandyType.STRIPED_HORIZONTAL] = 2
                }
            }
            5 -> {
                // Specials + Ingredient Combo
                val color = CandyColor.values()[rnd.nextInt(CandyColor.values().size)]
                ingredientsMap[color] = 15 + (levelNum / 60)
                specialsMap[SpecialCandyType.STRIPED_HORIZONTAL] = 2
            }
        }

        val worldNum = getWorldNumber(levelNum)
        val name = if (isBossLevel) "👑 Boss Citadel $levelNum" else "Level $levelNum"

        val flavorQuote = when {
            isBossLevel -> "A grand fortress of candy and thick jelly! Prove your mastery."
            archetype == 1 -> "Clear all shimmering jelly tiles beneath the candy grid!"
            archetype == 2 -> "Harvest the required colorful candies before your moves run out."
            archetype == 3 -> "Craft explosive special treats to pass this test."
            else -> "Match sweet candies and cascade your way to victory!"
        }

        val isTimedLevel = (levelNum == 2 || levelNum % 3 == 0)
        val timeLimit = if (isTimedLevel) 60 + (rnd.nextInt(3) * 15) else null

        return LevelDef(
            levelNumber = levelNum,
            name = name,
            maxMoves = maxMoves,
            starScores = Triple(star1, star2, star3),
            targetScore = targetScore,
            targetJellies = jelliesCount,
            jellyPositions = jellySet,
            targetIngredients = ingredientsMap,
            targetSpecials = specialsMap,
            flavorQuote = flavorQuote,
            timeLimitSeconds = timeLimit
        )
    }

    private fun generateJellyPositions(levelNum: Int, rnd: Random, isBoss: Boolean): Set<Position> {
        val patternType = if (isBoss) 7 else (levelNum % 7)
        return buildSet {
            when (patternType) {
                0 -> {
                    // Center 4x4 (16 tiles)
                    for (r in 2..5) {
                        for (c in 2..5) add(Position(r, c))
                    }
                }
                1 -> {
                    // Cross / Plus Sign (16 tiles)
                    for (c in 1..6) { add(Position(3, c)); add(Position(4, c)) }
                    for (r in 1..6) { add(Position(r, 3)); add(Position(r, 4)) }
                }
                2 -> {
                    // Diagonal X pattern (16 tiles)
                    for (i in 0..7) {
                        add(Position(i, i))
                        add(Position(i, 7 - i))
                    }
                }
                3 -> {
                    // Four Corners (16 tiles)
                    val corners = listOf(
                        Position(0, 0), Position(0, 1), Position(1, 0), Position(1, 1),
                        Position(0, 6), Position(0, 7), Position(1, 6), Position(1, 7),
                        Position(6, 0), Position(6, 1), Position(7, 0), Position(7, 1),
                        Position(6, 6), Position(6, 7), Position(7, 6), Position(7, 7)
                    )
                    addAll(corners)
                }
                4 -> {
                    // Diamond ring (16 tiles)
                    for (r in 1..6) {
                        for (c in 1..6) {
                            val dist = kotlin.math.abs(r - 3.5) + kotlin.math.abs(c - 3.5)
                            if (dist in 1.8..3.2) add(Position(r, c))
                        }
                    }
                }
                5 -> {
                    // Twin Columns (16 tiles)
                    for (r in 0..7) {
                        add(Position(r, 2))
                        add(Position(r, 5))
                    }
                }
                6 -> {
                    // Checkered pattern in center (18 tiles)
                    for (r in 1..6) {
                        for (c in 1..6) {
                            if ((r + c) % 2 == 0) add(Position(r, c))
                        }
                    }
                }
                else -> {
                    // Boss: Massive fortress border & center (24-28 tiles)
                    for (r in 1..6) {
                        for (c in 1..6) {
                            if (r == 1 || r == 6 || c == 1 || c == 6 || (r in 3..4 && c in 3..4)) {
                                add(Position(r, c))
                            }
                        }
                    }
                }
            }
        }
    }
}
