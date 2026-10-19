package com.example.candy.game

import com.example.candy.model.*
import kotlin.random.Random

data class MatchEvaluation(
    val matchedPositions: Set<Position>,
    val specialCreations: Map<Position, SpecialCandyType>,
    val scoreEarned: Int,
    val ingredientsCollected: Map<CandyColor, Int>,
    val jelliesCleared: Int,
    val specialsTriggered: Set<SpecialCandyType>,
    val stripedRowsCleared: List<Int> = emptyList(),
    val stripedColsCleared: List<Int> = emptyList(),
    val wrappedExplosionCenters: List<Position> = emptyList(),
    val colorBombPositions: List<Position> = emptyList()
)

object GameEngine {
    const val ROWS = 8
    const val COLS = 8
    private var nextTileId = 1000L

    fun getNextTileId(): Long = nextTileId++

    fun createInitialBoard(level: LevelDef): List<List<CandyTile>> {
        val colors = CandyColor.values()
        val board = MutableList(ROWS) { r ->
            MutableList(COLS) { c ->
                val hasJelly = level.jellyPositions.contains(Position(r, c))
                CandyTile(
                    id = getNextTileId(),
                    row = r,
                    col = c,
                    color = colors[0],
                    hasJelly = hasJelly
                )
            }
        }

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val forbiddenColors = mutableSetOf<CandyColor>()
                // Avoid horizontal 3 in a row
                if (c >= 2) {
                    val c1 = board[r][c - 1].color
                    val c2 = board[r][c - 2].color
                    if (c1 == c2) forbiddenColors.add(c1)
                }
                // Avoid vertical 3 in a row
                if (r >= 2) {
                    val r1 = board[r - 1][c].color
                    val r2 = board[r - 2][c].color
                    if (r1 == r2) forbiddenColors.add(r1)
                }

                val availableColors = colors.filter { it !in forbiddenColors }
                val chosenColor = availableColors.random()
                val hasJelly = level.jellyPositions.contains(Position(r, c))
                board[r][c] = CandyTile(
                    id = getNextTileId(),
                    row = r,
                    col = c,
                    color = chosenColor,
                    hasJelly = hasJelly
                )
            }
        }

        // Verify there is at least one possible move; if not, re-roll
        if (!hasPossibleMoves(board)) {
            return createInitialBoard(level)
        }

        return board
    }

    fun canSwap(pos1: Position, pos2: Position, board: List<List<CandyTile>>): Boolean {
        if (!pos1.isAdjacent(pos2)) return false
        val tile1 = board[pos1.row][pos1.col]
        val tile2 = board[pos2.row][pos2.col]

        // Color Bomb swap with anything is always a valid move
        if (tile1.special == SpecialCandyType.COLOR_BOMB || tile2.special == SpecialCandyType.COLOR_BOMB) {
            return true
        }

        // Simulate swap
        val simulated = simulateSwap(pos1, pos2, board)
        val evaluation = evaluateBoardMatches(simulated, swappedAt = pos2)
        return evaluation.matchedPositions.isNotEmpty()
    }

    fun simulateSwap(pos1: Position, pos2: Position, board: List<List<CandyTile>>): List<List<CandyTile>> {
        val newBoard = board.map { it.toMutableList() }
        val t1 = newBoard[pos1.row][pos1.col]
        val t2 = newBoard[pos2.row][pos2.col]
        newBoard[pos1.row][pos1.col] = t2.copy(row = pos1.row, col = pos1.col)
        newBoard[pos2.row][pos2.col] = t1.copy(row = pos2.row, col = pos2.col)
        return newBoard
    }

    fun evaluateBoardMatches(
        board: List<List<CandyTile>>,
        swappedAt: Position? = null
    ): MatchEvaluation {
        val horizontalMatches = mutableListOf<List<Position>>()
        val verticalMatches = mutableListOf<List<Position>>()

        // 1. Scan horizontal matches
        for (r in 0 until ROWS) {
            var c = 0
            while (c < COLS) {
                val startC = c
                val color = board[r][c].color
                val isBomb = board[r][c].special == SpecialCandyType.COLOR_BOMB
                if (isBomb) {
                    c++
                    continue
                }
                while (c < COLS && board[r][c].color == color && board[r][c].special != SpecialCandyType.COLOR_BOMB) {
                    c++
                }
                val length = c - startC
                if (length >= 3) {
                    val line = (startC until c).map { Position(r, it) }
                    horizontalMatches.add(line)
                }
            }
        }

        // 2. Scan vertical matches
        for (c in 0 until COLS) {
            var r = 0
            while (r < ROWS) {
                val startR = r
                val color = board[r][c].color
                val isBomb = board[r][c].special == SpecialCandyType.COLOR_BOMB
                if (isBomb) {
                    r++
                    continue
                }
                while (r < ROWS && board[r][c].color == color && board[r][c].special != SpecialCandyType.COLOR_BOMB) {
                    r++
                }
                val length = r - startR
                if (length >= 3) {
                    val line = (startR until r).map { Position(it, c) }
                    verticalMatches.add(line)
                }
            }
        }

        val allBasePositions = mutableSetOf<Position>()
        horizontalMatches.forEach { allBasePositions.addAll(it) }
        verticalMatches.forEach { allBasePositions.addAll(it) }

        if (allBasePositions.isEmpty()) {
            return MatchEvaluation(
                matchedPositions = emptySet(),
                specialCreations = emptyMap(),
                scoreEarned = 0,
                ingredientsCollected = emptyMap(),
                jelliesCleared = 0,
                specialsTriggered = emptySet()
            )
        }

        // 3. Determine Special Candy creations
        val specialCreations = mutableMapOf<Position, SpecialCandyType>()

        // Check for 5-in-a-line (Color Bomb)
        val fiveLines = (horizontalMatches + verticalMatches).filter { it.size >= 5 }
        for (line in fiveLines) {
            val targetPos = line.firstOrNull { it == swappedAt } ?: line[line.size / 2]
            specialCreations[targetPos] = SpecialCandyType.COLOR_BOMB
        }

        // Check for T or L shapes (intersection of horizontal and vertical match of same color -> Wrapped Candy)
        for (hLine in horizontalMatches) {
            for (vLine in verticalMatches) {
                val intersection = hLine.intersect(vLine.toSet()).firstOrNull()
                if (intersection != null && !specialCreations.containsKey(intersection)) {
                    val hColor = board[intersection.row][intersection.col].color
                    val vColor = board[intersection.row][intersection.col].color
                    if (hColor == vColor) {
                        specialCreations[intersection] = SpecialCandyType.WRAPPED
                    }
                }
            }
        }

        // Check for 4-in-a-line (Striped Candy)
        val fourLinesH = horizontalMatches.filter { it.size == 4 }
        for (line in fourLinesH) {
            val targetPos = line.firstOrNull { it == swappedAt } ?: line[1]
            if (!specialCreations.containsKey(targetPos)) {
                // Horizontal match creates Vertical Striped candy (clears column)
                specialCreations[targetPos] = SpecialCandyType.STRIPED_VERTICAL
            }
        }
        val fourLinesV = verticalMatches.filter { it.size == 4 }
        for (line in fourLinesV) {
            val targetPos = line.firstOrNull { it == swappedAt } ?: line[1]
            if (!specialCreations.containsKey(targetPos)) {
                // Vertical match creates Horizontal Striped candy (clears row)
                specialCreations[targetPos] = SpecialCandyType.STRIPED_HORIZONTAL
            }
        }

        // 4. Expand cascade with special candies contained in allBasePositions
        val expandedClears = mutableSetOf<Position>()
        val queue = ArrayDeque<Position>()
        queue.addAll(allBasePositions)
        expandedClears.addAll(allBasePositions)

        val specialsTriggered = mutableSetOf<SpecialCandyType>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            // If this position was chosen to CREATE a new special, it shouldn't detonate as an old special
            if (specialCreations.containsKey(current)) continue

            val tile = board[current.row][current.col]
            when (tile.special) {
                SpecialCandyType.STRIPED_HORIZONTAL -> {
                    specialsTriggered.add(SpecialCandyType.STRIPED_HORIZONTAL)
                    for (c in 0 until COLS) {
                        val p = Position(current.row, c)
                        if (expandedClears.add(p)) queue.add(p)
                    }
                }
                SpecialCandyType.STRIPED_VERTICAL -> {
                    specialsTriggered.add(SpecialCandyType.STRIPED_VERTICAL)
                    for (r in 0 until ROWS) {
                        val p = Position(r, current.col)
                        if (expandedClears.add(p)) queue.add(p)
                    }
                }
                SpecialCandyType.WRAPPED -> {
                    specialsTriggered.add(SpecialCandyType.WRAPPED)
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val r = current.row + dr
                            val c = current.col + dc
                            if (r in 0 until ROWS && c in 0 until COLS) {
                                val p = Position(r, c)
                                if (expandedClears.add(p)) queue.add(p)
                            }
                        }
                    }
                }
                SpecialCandyType.COLOR_BOMB -> {
                    specialsTriggered.add(SpecialCandyType.COLOR_BOMB)
                    val targetColor = tile.color
                    for (r in 0 until ROWS) {
                        for (c in 0 until COLS) {
                            if (board[r][c].color == targetColor) {
                                val p = Position(r, c)
                                if (expandedClears.add(p)) queue.add(p)
                            }
                        }
                    }
                }
                SpecialCandyType.NONE -> { /* standard clear */ }
            }
        }

        // 5. Calculate collected ingredients, jellies, and scores
        val collectedIngredients = mutableMapOf<CandyColor, Int>()
        var jelliesClearedCount = 0

        for (p in expandedClears) {
            val tile = board[p.row][p.col]
            if (tile.special != SpecialCandyType.COLOR_BOMB) {
                val currentCount = collectedIngredients[tile.color] ?: 0
                collectedIngredients[tile.color] = currentCount + 1
            }
            if (tile.hasJelly) {
                jelliesClearedCount++
            }
        }

        val baseScore = expandedClears.size * 60
        val specialsScore = specialCreations.size * 200 + specialsTriggered.size * 300
        val totalScore = baseScore + specialsScore

        return MatchEvaluation(
            matchedPositions = expandedClears,
            specialCreations = specialCreations,
            scoreEarned = totalScore,
            ingredientsCollected = collectedIngredients,
            jelliesCleared = jelliesClearedCount,
            specialsTriggered = specialsTriggered
        )
    }

    fun handleColorBombSwap(
        bombPos: Position,
        targetPos: Position,
        board: List<List<CandyTile>>
    ): MatchEvaluation {
        val bombTile = board[bombPos.row][bombPos.col]
        val targetTile = board[targetPos.row][targetPos.col]

        val clearedPositions = mutableSetOf<Position>()
        clearedPositions.add(bombPos)
        clearedPositions.add(targetPos)

        val specialsTriggered = mutableSetOf<SpecialCandyType>()
        specialsTriggered.add(SpecialCandyType.COLOR_BOMB)

        // Case 1: Bomb + Bomb -> Clear ALL tiles on the entire board!
        if (targetTile.special == SpecialCandyType.COLOR_BOMB) {
            for (r in 0 until ROWS) {
                for (c in 0 until COLS) {
                    clearedPositions.add(Position(r, c))
                }
            }
        } else {
            // Case 2: Bomb + Regular or Special Candy -> destroy all of that color!
            val targetColor = targetTile.color
            for (r in 0 until ROWS) {
                for (c in 0 until COLS) {
                    if (board[r][c].color == targetColor) {
                        clearedPositions.add(Position(r, c))
                    }
                }
            }
            // If target was striped or wrapped, trigger secondary board blast
            if (targetTile.special != SpecialCandyType.NONE) {
                specialsTriggered.add(targetTile.special)
                // Detonate all matching positions as striped/wrapped
                for (r in 0 until ROWS) {
                    for (c in 0 until COLS) {
                        if (board[r][c].color == targetColor) {
                            if (targetTile.special == SpecialCandyType.STRIPED_HORIZONTAL || targetTile.special == SpecialCandyType.STRIPED_VERTICAL) {
                                for (colIdx in 0 until COLS) clearedPositions.add(Position(r, colIdx))
                                for (rowIdx in 0 until ROWS) clearedPositions.add(Position(rowIdx, c))
                            }
                        }
                    }
                }
            }
        }

        var jelliesCount = 0
        val ingredients = mutableMapOf<CandyColor, Int>()
        for (p in clearedPositions) {
            val tile = board[p.row][p.col]
            if (tile.hasJelly) jelliesCount++
            val cnt = ingredients[tile.color] ?: 0
            ingredients[tile.color] = cnt + 1
        }

        val totalScore = clearedPositions.size * 100 + 1000

        return MatchEvaluation(
            matchedPositions = clearedPositions,
            specialCreations = emptyMap(),
            scoreEarned = totalScore,
            ingredientsCollected = ingredients,
            jelliesCleared = jelliesCount,
            specialsTriggered = specialsTriggered
        )
    }

    /**
     * Drops candies down and generates new ones at the top to fill empty gaps.
     */
    fun dropAndRefill(
        board: List<List<CandyTile>>,
        clearedPositions: Set<Position>,
        specialCreations: Map<Position, SpecialCandyType> = emptyMap()
    ): List<List<CandyTile>> {
        val colors = CandyColor.values()
        val newBoard = board.map { it.toMutableList() }

        // 1. Process positions that create new special candies: keep them with updated special type!
        for ((pos, specialType) in specialCreations) {
            val currentTile = newBoard[pos.row][pos.col]
            val keepColor = if (specialType == SpecialCandyType.COLOR_BOMB) CandyColor.YELLOW else currentTile.color
            newBoard[pos.row][pos.col] = currentTile.copy(
                special = specialType,
                color = keepColor,
                hasJelly = false // Jelly beneath is cleared
            )
        }

        // 2. Clear positions that were destroyed (excluding newly created specials)
        for (pos in clearedPositions) {
            if (!specialCreations.containsKey(pos)) {
                val current = newBoard[pos.row][pos.col]
                newBoard[pos.row][pos.col] = current.copy(
                    isClearing = true,
                    hasJelly = false
                )
            }
        }

        // 3. Gravity: shift remaining tiles down per column
        for (c in 0 until COLS) {
            val survivingTiles = mutableListOf<CandyTile>()
            for (r in ROWS - 1 downTo 0) {
                val tile = newBoard[r][c]
                if (!tile.isClearing) {
                    survivingTiles.add(tile)
                }
            }

            // Fill from bottom up
            var fillRow = ROWS - 1
            for (tile in survivingTiles) {
                newBoard[fillRow][c] = tile.copy(row = fillRow, col = c)
                fillRow--
            }

            // Fill remaining top spaces with new random candies
            while (fillRow >= 0) {
                newBoard[fillRow][c] = CandyTile(
                    id = getNextTileId(),
                    row = fillRow,
                    col = c,
                    color = colors.random(),
                    special = SpecialCandyType.NONE,
                    hasJelly = false
                )
                fillRow--
            }
        }

        return newBoard
    }

    fun hasPossibleMoves(board: List<List<CandyTile>>): Boolean {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val p = Position(r, c)
                // Check right
                if (c + 1 < COLS) {
                    val pRight = Position(r, c + 1)
                    if (canSwap(p, pRight, board)) return true
                }
                // Check down
                if (r + 1 < ROWS) {
                    val pDown = Position(r + 1, c)
                    if (canSwap(p, pDown, board)) return true
                }
            }
        }
        return false
    }

    fun findHint(board: List<List<CandyTile>>): Pair<Position, Position>? {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val p = Position(r, c)
                if (c + 1 < COLS) {
                    val pRight = Position(r, c + 1)
                    if (canSwap(p, pRight, board)) return Pair(p, pRight)
                }
                if (r + 1 < ROWS) {
                    val pDown = Position(r + 1, c)
                    if (canSwap(p, pDown, board)) return Pair(p, pDown)
                }
            }
        }
        return null
    }

    fun reshuffleBoard(board: List<List<CandyTile>>, level: LevelDef): List<List<CandyTile>> {
        val allTiles = board.flatten().map { it.color to it.special }.shuffled()
        var idx = 0
        val newBoard = MutableList(ROWS) { r ->
            MutableList(COLS) { c ->
                val prev = board[r][c]
                val (color, special) = allTiles[idx++]
                CandyTile(
                    id = getNextTileId(),
                    row = r,
                    col = c,
                    color = color,
                    special = special,
                    hasJelly = prev.hasJelly
                )
            }
        }
        // If still no moves, regenerate freshly
        return if (hasPossibleMoves(newBoard)) newBoard else createInitialBoard(level)
    }
}
