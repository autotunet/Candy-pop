package com.example.candy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.candy.audio.SoundManager
import com.example.candy.data.LevelsData
import com.example.candy.data.local.CandyDatabase
import com.example.candy.data.local.LevelProgressEntity
import com.example.candy.data.local.PlayerStatsEntity
import com.example.candy.game.GameEngine
import com.example.candy.game.MatchEvaluation
import com.example.candy.model.*
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.GoldStar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

enum class GameScreen {
    MAP,
    PLAYING,
    HOW_TO_PLAY
}

data class GameUiState(
    val currentScreen: GameScreen = GameScreen.MAP,
    val currentLevel: LevelDef = LevelsData.getLevel(1),
    val board: List<List<CandyTile>> = emptyList(),
    val movesLeft: Int = 20,
    val currentScore: Int = 0,
    val jelliesRemaining: Int = 0,
    val collectedIngredients: Map<CandyColor, Int> = emptyMap(),
    val createdSpecials: Map<SpecialCandyType, Int> = emptyMap(),
    val isGameOver: Boolean = false,
    val isGameWon: Boolean = false,
    val starsEarned: Int = 0,
    val isProcessing: Boolean = false,
    val selectedPosition: Position? = null,
    val activeBooster: BoosterType? = null,
    val hintPair: Pair<Position, Position>? = null,
    val comboText: String? = null,
    val comboStreak: Int = 0,
    val comboMultiplier: Float = 1.0f,
    val maxComboStreak: Int = 0,
    val comboMeterFill: Float = 0f,
    val isComboActive: Boolean = false,
    val particles: List<Particle> = emptyList(),
    val floatingScores: List<FloatingScore> = emptyList(),
    val shockwaves: List<Shockwave> = emptyList(),
    val lasers: List<LaserBeam> = emptyList(),
    val rainbowTentacles: List<RainbowTentacle> = emptyList(),
    val screenShakeIntensity: Float = 0f,
    val timeLeftSeconds: Int? = null,
    val isTimedOut: Boolean = false,
    val elapsedTimeSeconds: Int = 0,
    val isTimerMode: Boolean = false,
    val isPaused: Boolean = false,
    val language: com.example.candy.util.AppLanguage = com.example.candy.util.AppLanguage.ES,
    val isDarkMode: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = CandyDatabase.getDatabase(application)
    val soundManager = SoundManager(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val levelProgressList: StateFlow<List<LevelProgressEntity>> =
        db.levelProgressDao().getAllProgress().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    val playerStats: StateFlow<PlayerStatsEntity?> =
        db.playerStatsDao().getPlayerStats().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PlayerStatsEntity()
        )

    init {
        // Initialize default level 1 unlocked if empty
        viewModelScope.launch {
            val existing = db.levelProgressDao().getProgressForLevel(1)
            if (existing == null) {
                db.levelProgressDao().insertOrUpdate(
                    LevelProgressEntity(
                        levelNumber = 1,
                        stars = 0,
                        highScore = 0,
                        isUnlocked = true
                    )
                )
            }
            val stats = db.playerStatsDao().getPlayerStatsSync()
            if (stats == null) {
                db.playerStatsDao().updateStats(PlayerStatsEntity())
            } else {
                soundManager.isSoundEnabled = stats.soundEnabled
                soundManager.isHapticsEnabled = stats.hapticsEnabled
                val lang = if (stats.language == "en") com.example.candy.util.AppLanguage.EN else com.example.candy.util.AppLanguage.ES
                _uiState.value = _uiState.value.copy(
                    language = lang,
                    isDarkMode = stats.darkModeEnabled
                )
            }
        }
    }

    fun attachView(view: android.view.View) {
        soundManager.attachView(view)
    }

    fun detachView() {
        soundManager.detachView()
    }

    fun toggleDarkMode() {
        val enabled = !_uiState.value.isDarkMode
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
        viewModelScope.launch {
            val stats = db.playerStatsDao().getPlayerStatsSync() ?: PlayerStatsEntity()
            db.playerStatsDao().updateStats(stats.copy(darkModeEnabled = enabled))
        }
    }

    fun setLanguage(lang: com.example.candy.util.AppLanguage) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(language = lang)
        viewModelScope.launch {
            val stats = db.playerStatsDao().getPlayerStatsSync() ?: PlayerStatsEntity()
            db.playerStatsDao().updateStats(stats.copy(language = lang.code))
        }
    }

    fun navigateTo(screen: GameScreen) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(currentScreen = screen, isPaused = false)
    }

    private var timerJob: kotlinx.coroutines.Job? = null
    private var comboDecayJob: kotlinx.coroutines.Job? = null
    private var currentStreak: Int = 0

    fun getMultiplierForStreak(streak: Int): Float {
        return when {
            streak <= 1 -> 1.0f
            streak == 2 -> 1.5f
            streak == 3 -> 2.0f
            streak == 4 -> 2.5f
            streak == 5 -> 3.0f
            streak == 6 -> 3.5f
            streak == 7 -> 4.0f
            else -> 5.0f
        }
    }

    private fun startComboDecayLoop() {
        comboDecayJob?.cancel()
        if (currentStreak <= 0) return
        comboDecayJob = viewModelScope.launch {
            val totalDurationMs = 4500L
            val stepIntervalMs = 50L
            val steps = (totalDurationMs / stepIntervalMs).toInt()
            for (i in steps downTo 0) {
                delay(stepIntervalMs)
                val state = _uiState.value
                if (state.currentScreen != GameScreen.PLAYING || state.isGameOver || state.isGameWon) {
                    break
                }
                if (state.isProcessing || state.isPaused) {
                    continue
                }
                val fill = i.toFloat() / steps
                _uiState.value = _uiState.value.copy(comboMeterFill = fill)
            }
            currentStreak = 0
            _uiState.value = _uiState.value.copy(
                comboStreak = 0,
                comboMultiplier = 1.0f,
                comboMeterFill = 0f,
                isComboActive = false,
                comboText = null
            )
        }
    }

    fun startLevel(levelNumber: Int, forceTimerMode: Boolean = false) {
        val level = LevelsData.getLevel(levelNumber)
        val initialBoard = GameEngine.createInitialBoard(level)
        val totalJellies = level.jellyPositions.size
        val timeLimit = if (forceTimerMode) 60 else level.timeLimitSeconds

        timerJob?.cancel()
        comboDecayJob?.cancel()
        currentStreak = 0
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(
            currentScreen = GameScreen.PLAYING,
            currentLevel = level,
            board = initialBoard,
            movesLeft = level.maxMoves,
            currentScore = 0,
            jelliesRemaining = totalJellies,
            collectedIngredients = level.targetIngredients.mapValues { 0 },
            createdSpecials = level.targetSpecials.mapValues { 0 },
            isGameOver = false,
            isGameWon = false,
            starsEarned = 0,
            isProcessing = false,
            selectedPosition = null,
            activeBooster = null,
            hintPair = null,
            comboStreak = 0,
            comboMultiplier = 1.0f,
            maxComboStreak = 0,
            comboMeterFill = 0f,
            isComboActive = false,
            comboText = null,
            timeLeftSeconds = timeLimit,
            isTimedOut = false,
            elapsedTimeSeconds = 0,
            isTimerMode = (timeLimit != null),
            shockwaves = emptyList(),
            lasers = emptyList(),
            rainbowTentacles = emptyList(),
            floatingScores = emptyList(),
            screenShakeIntensity = 0f
        )

        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                if (state.currentScreen != GameScreen.PLAYING) break
                if (state.isPaused || state.isGameOver || state.isGameWon) continue

                val newElapsed = state.elapsedTimeSeconds + 1
                val remaining = state.timeLeftSeconds

                if (remaining != null) {
                    if (remaining > 1) {
                        val next = remaining - 1
                        _uiState.value = state.copy(
                            timeLeftSeconds = next,
                            elapsedTimeSeconds = newElapsed
                        )
                        if (next <= 5) {
                            soundManager.playTick()
                        }
                    } else {
                        // Time's up!
                        soundManager.playLose()
                        _uiState.value = state.copy(
                            timeLeftSeconds = 0,
                            elapsedTimeSeconds = newElapsed,
                            isGameOver = true,
                            isTimedOut = true,
                            isProcessing = false
                        )
                        break
                    }
                } else {
                    _uiState.value = state.copy(elapsedTimeSeconds = newElapsed)
                }
            }
        }
    }

    fun addExtraTime(seconds: Int = 15) {
        val state = _uiState.value
        val cur = (state.timeLeftSeconds ?: 0) + seconds
        soundManager.playSpecialCreated()
        _uiState.value = state.copy(
            timeLeftSeconds = cur,
            isGameOver = false,
            isTimedOut = false
        )
        startTimerLoop()
    }

    fun rewardForWatchingAd() {
        viewModelScope.launch {
            val stats = db.playerStatsDao().getPlayerStatsSync() ?: PlayerStatsEntity()
            db.playerStatsDao().updateStats(stats.copy(coins = stats.coins + 100))
            soundManager.playWin()
        }
    }

    fun togglePause() {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isPaused = !_uiState.value.isPaused)
    }

    fun onTileClick(pos: Position) {
        val state = _uiState.value
        if (state.isProcessing || state.isGameOver || state.isPaused) return

        // 1. If a booster is armed, handle target click
        if (state.activeBooster != null) {
            handleBoosterTarget(pos, state.activeBooster)
            return
        }

        // 2. Normal selection / swap logic
        val selected = state.selectedPosition
        if (selected == null) {
            soundManager.playClick()
            _uiState.value = state.copy(selectedPosition = pos, hintPair = null)
        } else if (selected == pos) {
            // Deselect
            soundManager.playClick()
            _uiState.value = state.copy(selectedPosition = null)
        } else if (selected.isAdjacent(pos)) {
            // Try Swap
            attemptSwap(selected, pos)
        } else {
            // Select new tile
            soundManager.playClick()
            _uiState.value = state.copy(selectedPosition = pos)
        }
    }

    fun onTileSwipe(from: Position, dr: Int, dc: Int) {
        val state = _uiState.value
        if (state.isProcessing || state.isGameOver || state.isPaused) return
        val targetR = from.row + dr
        val targetC = from.col + dc
        if (targetR in 0 until GameEngine.ROWS && targetC in 0 until GameEngine.COLS) {
            val to = Position(targetR, targetC)
            attemptSwap(from, to)
        }
    }

    private fun attemptSwap(pos1: Position, pos2: Position) {
        val state = _uiState.value
        val tile1 = state.board[pos1.row][pos1.col]
        val tile2 = state.board[pos2.row][pos2.col]

        // Color Bomb swap case
        if (tile1.special == SpecialCandyType.COLOR_BOMB || tile2.special == SpecialCandyType.COLOR_BOMB) {
            executeColorBombSwap(pos1, pos2)
            return
        }

        // Check if swap produces matches
        if (GameEngine.canSwap(pos1, pos2, state.board)) {
            executeValidSwap(pos1, pos2)
        } else {
            // Invalid swap: tactile rejection + sound + deselect
            soundManager.playSwap(isValid = false)
            _uiState.value = state.copy(selectedPosition = null)
        }
    }

    private fun executeValidSwap(pos1: Position, pos2: Position) {
        viewModelScope.launch {
            comboDecayJob?.cancel()
            soundManager.playSwap(isValid = true)
            val state = _uiState.value
            val swappedBoard = GameEngine.simulateSwap(pos1, pos2, state.board)
            val newMoves = (state.movesLeft - 1).coerceAtLeast(0)

            _uiState.value = state.copy(
                board = swappedBoard,
                selectedPosition = null,
                movesLeft = newMoves,
                isProcessing = true
            )

            delay(150)
            processCascades(swappedBoard, swapTarget = pos2)
        }
    }

    private fun executeColorBombSwap(pos1: Position, pos2: Position) {
        viewModelScope.launch {
            comboDecayJob?.cancel()
            currentStreak++
            val mult = getMultiplierForStreak(currentStreak)
            val state = _uiState.value
            val bombPos = if (state.board[pos1.row][pos1.col].special == SpecialCandyType.COLOR_BOMB) pos1 else pos2
            val targetPos = if (bombPos == pos1) pos2 else pos1
            val newMoves = (state.movesLeft - 1).coerceAtLeast(0)

            soundManager.playColorBomb()
            _uiState.value = state.copy(
                selectedPosition = null,
                movesLeft = newMoves,
                isProcessing = true,
                comboStreak = currentStreak,
                comboMultiplier = mult,
                maxComboStreak = maxOf(state.maxComboStreak, currentStreak),
                comboMeterFill = 1f,
                isComboActive = true
            )

            val evaluation = GameEngine.handleColorBombSwap(bombPos, targetPos, state.board)
            val tentacles = evaluation.matchedPositions.map { target ->
                RainbowTentacle(
                    id = Random.nextLong(),
                    fromPos = bombPos,
                    toPos = target,
                    color = Color(0xFFFF007F)
                )
            }
            _uiState.value = _uiState.value.copy(
                rainbowTentacles = tentacles,
                screenShakeIntensity = 14f
            )

            spawnParticlesForPositions(evaluation.matchedPositions)

            // Animate Candy Removal for all cleared bomb targets
            val clearingBoard = state.board.map { rowList ->
                rowList.map { tile ->
                    if (evaluation.matchedPositions.contains(Position(tile.row, tile.col))) {
                        tile.copy(isClearing = true)
                    } else {
                        tile
                    }
                }
            }
            _uiState.value = _uiState.value.copy(
                board = clearingBoard,
                selectedPosition = null,
                movesLeft = newMoves,
                isProcessing = true
            )

            delay(320)
            applyMatchEvaluation(evaluation, streakCount = currentStreak)

            val refilledBoard = GameEngine.dropAndRefill(state.board, evaluation.matchedPositions, evaluation.specialCreations)
            _uiState.value = _uiState.value.copy(
                board = refilledBoard,
                rainbowTentacles = emptyList(),
                screenShakeIntensity = 0f
            )
            delay(340)
            processCascades(refilledBoard)
        }
    }

    private suspend fun processCascades(currentBoard: List<List<CandyTile>>, swapTarget: Position? = null) {
        var board = currentBoard
        var combo = 1
        var activeTarget = swapTarget

        while (true) {
            val evaluation = GameEngine.evaluateBoardMatches(board, swappedAt = activeTarget)
            activeTarget = null // only applies to first match

            if (evaluation.matchedPositions.isEmpty()) {
                break
            }

            comboDecayJob?.cancel()
            currentStreak++
            val currentMult = getMultiplierForStreak(currentStreak)
            val newMax = maxOf(_uiState.value.maxComboStreak, currentStreak)
            val fill = (currentStreak / 6f).coerceIn(0.25f, 1.0f)

            // Audio & Haptic feedback
            if (evaluation.specialsTriggered.isNotEmpty()) {
                soundManager.playSpecialExplosion()
            } else {
                soundManager.playMatch(currentStreak)
            }
            if (evaluation.specialCreations.isNotEmpty()) {
                soundManager.playSpecialCreated()
            }

            // Show combo announcement with localized praise
            if (currentStreak >= 2) {
                val strings = com.example.candy.util.Localization.getStrings(_uiState.value.language)
                val praise = when (currentStreak) {
                    2 -> strings.sweet
                    3 -> strings.tasty
                    4 -> strings.delicious
                    else -> strings.sugarCrush
                }
                soundManager.playComboPraise()
                _uiState.value = _uiState.value.copy(comboText = praise)
            }

            // Modern Special Effects
            val newShockwaves = evaluation.matchedPositions.take(4).map { pos ->
                val tileColor = try { board[pos.row][pos.col].color.primaryColor } catch (_: Exception) { CandyPink }
                Shockwave(
                    id = Random.nextLong(),
                    col = pos.col,
                    row = pos.row,
                    color = tileColor
                )
            }

            val newLasers = mutableListOf<LaserBeam>()
            if (evaluation.specialsTriggered.contains(SpecialCandyType.STRIPED_HORIZONTAL)) {
                val rowIdx = evaluation.matchedPositions.firstOrNull()?.row ?: 3
                newLasers.add(LaserBeam(id = Random.nextLong(), isHorizontal = true, lineIndex = rowIdx, color = CandyYellow))
                soundManager.playLaser()
            }
            if (evaluation.specialsTriggered.contains(SpecialCandyType.STRIPED_VERTICAL)) {
                val colIdx = evaluation.matchedPositions.firstOrNull()?.col ?: 3
                newLasers.add(LaserBeam(id = Random.nextLong(), isHorizontal = false, lineIndex = colIdx, color = CandyBlue))
                soundManager.playLaser()
            }

            val firstPos = evaluation.matchedPositions.firstOrNull()
            val newScores = if (firstPos != null) {
                val earned = (evaluation.scoreEarned * currentMult).toInt()
                val multTag = if (currentMult > 1.0f) " (x${String.format(Locale.US, "%.1f", currentMult)})" else ""
                listOf(
                    FloatingScore(
                        id = Random.nextLong(),
                        text = "+$earned$multTag",
                        col = firstPos.col,
                        row = firstPos.row,
                        color = if (currentMult >= 2.0f) GoldStar else CandyYellow
                    )
                )
            } else emptyList()

            val shake = if (evaluation.specialsTriggered.isNotEmpty() || currentStreak >= 2) (6f * currentStreak.coerceAtMost(3)) else 0f

            _uiState.value = _uiState.value.copy(
                shockwaves = newShockwaves,
                lasers = newLasers,
                floatingScores = newScores,
                screenShakeIntensity = shake,
                comboStreak = currentStreak,
                comboMultiplier = currentMult,
                maxComboStreak = newMax,
                comboMeterFill = fill,
                isComboActive = true
            )

            spawnParticlesForPositions(evaluation.matchedPositions)
            applyMatchEvaluation(evaluation, streakCount = currentStreak)

            // Step 1: Animate Candy Removal (Popping, flash, scale down & shockwave ring)
            val clearingBoard = board.map { rowList ->
                rowList.map { tile ->
                    if (evaluation.matchedPositions.contains(Position(tile.row, tile.col)) &&
                        !evaluation.specialCreations.containsKey(Position(tile.row, tile.col))) {
                        tile.copy(isClearing = true)
                    } else {
                        tile
                    }
                }
            }
            _uiState.value = _uiState.value.copy(board = clearingBoard)

            // Wait for custom removal popping animation
            delay(280)

            // Clear transient effects
            _uiState.value = _uiState.value.copy(
                shockwaves = emptyList(),
                lasers = emptyList(),
                screenShakeIntensity = 0f
            )

            // Step 2: Animate Candy Replacement (Drop candies down with spring bounce physics)
            board = GameEngine.dropAndRefill(
                board,
                evaluation.matchedPositions,
                evaluation.specialCreations
            )
            _uiState.value = _uiState.value.copy(board = board)

            // Wait for bouncy drop-in replacement animation to settle
            delay(340)
            combo++
        }

        // Clear combo toast after delay
        if (_uiState.value.comboText != null) {
            delay(400)
            _uiState.value = _uiState.value.copy(comboText = null)
        }

        // Verify if board is deadlocked; if so, reshuffle!
        if (!GameEngine.hasPossibleMoves(board)) {
            val strings = com.example.candy.util.Localization.getStrings(_uiState.value.language)
            _uiState.value = _uiState.value.copy(comboText = strings.reshuffle)
            delay(600)
            board = GameEngine.reshuffleBoard(board, _uiState.value.currentLevel)
            _uiState.value = _uiState.value.copy(board = board, comboText = null)
        }

        // Check Win/Loss conditions
        checkGameCompletion()
    }

    private fun applyMatchEvaluation(evaluation: MatchEvaluation, streakCount: Int) {
        val state = _uiState.value
        val comboMultiplier = getMultiplierForStreak(streakCount)
        val earnedScore = (evaluation.scoreEarned * comboMultiplier).toInt()
        val newScore = state.currentScore + earnedScore

        // Update collected ingredients
        val updatedIngredients = state.collectedIngredients.toMutableMap()
        for ((color, count) in evaluation.ingredientsCollected) {
            if (updatedIngredients.containsKey(color)) {
                updatedIngredients[color] = (updatedIngredients[color] ?: 0) + count
            }
        }

        // Update created specials
        val updatedSpecials = state.createdSpecials.toMutableMap()
        for (specialType in evaluation.specialCreations.values) {
            if (updatedSpecials.containsKey(specialType)) {
                updatedSpecials[specialType] = (updatedSpecials[specialType] ?: 0) + 1
            }
        }

        val newJellies = (state.jelliesRemaining - evaluation.jelliesCleared).coerceAtLeast(0)

        // Calculate stars
        val level = state.currentLevel
        val stars = when {
            newScore >= level.starScores.third -> 3
            newScore >= level.starScores.second -> 2
            newScore >= level.starScores.first -> 1
            else -> 0
        }

        _uiState.value = state.copy(
            currentScore = newScore,
            jelliesRemaining = newJellies,
            collectedIngredients = updatedIngredients,
            createdSpecials = updatedSpecials,
            starsEarned = stars
        )
    }

    private fun checkGameCompletion() {
        val state = _uiState.value
        val level = state.currentLevel

        // Check if all goals are fulfilled
        val scoreGoalMet = state.currentScore >= level.targetScore
        val jelliesGoalMet = state.jelliesRemaining <= 0
        val ingredientsGoalMet = level.targetIngredients.all { (color, target) ->
            (state.collectedIngredients[color] ?: 0) >= target
        }
        val specialsGoalMet = level.targetSpecials.all { (specType, target) ->
            (state.createdSpecials[specType] ?: 0) >= target
        }

        val allGoalsMet = scoreGoalMet && jelliesGoalMet && ingredientsGoalMet && specialsGoalMet

        if (allGoalsMet) {
            // Victory!
            comboDecayJob?.cancel()
            val finalStars = state.starsEarned.coerceAtLeast(1)
            soundManager.playWin()
            _uiState.value = state.copy(
                isGameOver = true,
                isGameWon = true,
                starsEarned = finalStars,
                isProcessing = false
            )
            saveLevelProgress(level.levelNumber, finalStars, state.currentScore)
        } else if (state.movesLeft <= 0) {
            // Defeat!
            comboDecayJob?.cancel()
            soundManager.playLose()
            _uiState.value = state.copy(
                isGameOver = true,
                isGameWon = false,
                isProcessing = false
            )
        } else {
            // Still in play
            _uiState.value = state.copy(isProcessing = false)
            startComboDecayLoop()
        }
    }

    private fun saveLevelProgress(levelNum: Int, stars: Int, score: Int) {
        viewModelScope.launch {
            val existing = db.levelProgressDao().getProgressForLevel(levelNum)
            val bestStars = maxOf(existing?.stars ?: 0, stars)
            val bestScore = maxOf(existing?.highScore ?: 0, score)

            db.levelProgressDao().insertOrUpdate(
                LevelProgressEntity(
                    levelNumber = levelNum,
                    stars = bestStars,
                    highScore = bestScore,
                    isUnlocked = true
                )
            )

            // Unlock next level if available
            val nextLevel = levelNum + 1
            if (nextLevel <= LevelsData.TOTAL_LEVELS) {
                val nextProg = db.levelProgressDao().getProgressForLevel(nextLevel)
                if (nextProg == null || !nextProg.isUnlocked) {
                    db.levelProgressDao().insertOrUpdate(
                        LevelProgressEntity(
                            levelNumber = nextLevel,
                            stars = nextProg?.stars ?: 0,
                            highScore = nextProg?.highScore ?: 0,
                            isUnlocked = true
                        )
                    )
                }
            }

            // Reward coins for winning
            val stats = db.playerStatsDao().getPlayerStatsSync() ?: PlayerStatsEntity()
            val rewardCoins = 50 * stars
            db.playerStatsDao().updateStats(stats.copy(coins = stats.coins + rewardCoins))
        }
    }

    fun requestHint() {
        val state = _uiState.value
        if (state.isProcessing || state.isGameOver) return
        val hint = GameEngine.findHint(state.board)
        if (hint != null) {
            soundManager.playClick()
            _uiState.value = state.copy(hintPair = hint)
        }
    }

    fun selectBooster(booster: BoosterType) {
        val stats = playerStats.value ?: return
        val count = when (booster) {
            BoosterType.LOLLIPOP_HAMMER -> stats.hammers
            BoosterType.FREE_SWITCH -> stats.freeSwitches
            BoosterType.COLOR_BOMB -> stats.colorBombs
            BoosterType.EXTRA_MOVES -> stats.extraMoves
        }
        if (count <= 0 && stats.coins < 100) return

        soundManager.playClick()
        if (booster == BoosterType.EXTRA_MOVES) {
            useExtraMovesBooster()
        } else {
            _uiState.value = _uiState.value.copy(
                activeBooster = if (_uiState.value.activeBooster == booster) null else booster,
                selectedPosition = null
            )
        }
    }

    private fun handleBoosterTarget(pos: Position, booster: BoosterType) {
        val state = _uiState.value
        when (booster) {
            BoosterType.LOLLIPOP_HAMMER -> {
                // Smash tile!
                soundManager.playSpecialExplosion()
                val cleared = setOf(pos)
                spawnParticlesForPositions(cleared)
                decrementBooster(BoosterType.LOLLIPOP_HAMMER)

                viewModelScope.launch {
                    val updatedBoard = GameEngine.dropAndRefill(state.board, cleared)
                    _uiState.value = state.copy(
                        board = updatedBoard,
                        activeBooster = null,
                        currentScore = state.currentScore + 100,
                        jelliesRemaining = if (state.board[pos.row][pos.col].hasJelly) (state.jelliesRemaining - 1).coerceAtLeast(0) else state.jelliesRemaining,
                        isProcessing = true
                    )
                    delay(200)
                    processCascades(updatedBoard)
                }
            }
            BoosterType.FREE_SWITCH -> {
                // Select first, then second, swap without spending moves!
                val selected = state.selectedPosition
                if (selected == null) {
                    soundManager.playClick()
                    _uiState.value = state.copy(selectedPosition = pos)
                } else if (selected.isAdjacent(pos)) {
                    soundManager.playSwap()
                    decrementBooster(BoosterType.FREE_SWITCH)
                    viewModelScope.launch {
                        val swappedBoard = GameEngine.simulateSwap(selected, pos, state.board)
                        _uiState.value = state.copy(
                            board = swappedBoard,
                            selectedPosition = null,
                            activeBooster = null,
                            isProcessing = true
                        )
                        delay(200)
                        processCascades(swappedBoard)
                    }
                } else {
                    _uiState.value = state.copy(selectedPosition = pos)
                }
            }
            BoosterType.COLOR_BOMB -> {
                // Turn target tile into a COLOR_BOMB!
                soundManager.playSpecialCreated()
                decrementBooster(BoosterType.COLOR_BOMB)
                val newBoard = state.board.map { it.toMutableList() }
                newBoard[pos.row][pos.col] = newBoard[pos.row][pos.col].copy(special = SpecialCandyType.COLOR_BOMB)
                _uiState.value = state.copy(
                    board = newBoard,
                    activeBooster = null
                )
            }
            BoosterType.EXTRA_MOVES -> {}
        }
    }

    fun useExtraMovesBooster() {
        val state = _uiState.value
        decrementBooster(BoosterType.EXTRA_MOVES)
        soundManager.playSpecialCreated()
        _uiState.value = state.copy(
            movesLeft = state.movesLeft + 5,
            isGameOver = false,
            activeBooster = null
        )
    }

    private fun decrementBooster(booster: BoosterType) {
        viewModelScope.launch {
            val stats = db.playerStatsDao().getPlayerStatsSync() ?: PlayerStatsEntity()
            val newStats = when (booster) {
                BoosterType.LOLLIPOP_HAMMER -> if (stats.hammers > 0) stats.copy(hammers = stats.hammers - 1) else stats.copy(coins = stats.coins - 80)
                BoosterType.FREE_SWITCH -> if (stats.freeSwitches > 0) stats.copy(freeSwitches = stats.freeSwitches - 1) else stats.copy(coins = stats.coins - 80)
                BoosterType.COLOR_BOMB -> if (stats.colorBombs > 0) stats.copy(colorBombs = stats.colorBombs - 1) else stats.copy(coins = stats.coins - 120)
                BoosterType.EXTRA_MOVES -> if (stats.extraMoves > 0) stats.copy(extraMoves = stats.extraMoves - 1) else stats.copy(coins = stats.coins - 100)
            }
            db.playerStatsDao().updateStats(newStats)
        }
    }

    private fun spawnParticlesForPositions(positions: Set<Position>) {
        val newParticles = mutableListOf<Particle>()
        val rnd = Random
        for (pos in positions) {
            val tileColor = try { _uiState.value.board[pos.row][pos.col].color.primaryColor } catch (_: Exception) { CandyPink }
            // generate 8-12 particles per candy
            for (i in 0 until 10) {
                val speed = rnd.nextFloat() * 15f + 5f
                val angle = rnd.nextFloat() * 2f * Math.PI.toFloat()
                newParticles.add(
                    Particle(
                        id = rnd.nextLong(),
                        x = pos.col * 100f + 50f,
                        y = pos.row * 100f + 50f,
                        vx = (kotlin.math.cos(angle) * speed),
                        vy = (kotlin.math.sin(angle) * speed),
                        color = if (i % 3 == 0) GoldStar else tileColor,
                        size = rnd.nextFloat() * 10f + 6f,
                        shape = if (i % 2 == 0) ParticleShape.STAR else ParticleShape.CIRCLE
                    )
                )
            }
        }
        _uiState.value = _uiState.value.copy(particles = _uiState.value.particles + newParticles)

        // Clear particles after animation
        viewModelScope.launch {
            delay(400)
            _uiState.value = _uiState.value.copy(particles = emptyList())
        }
    }

    fun toggleSound() {
        val enabled = !soundManager.isSoundEnabled
        soundManager.isSoundEnabled = enabled
        viewModelScope.launch {
            val stats = db.playerStatsDao().getPlayerStatsSync() ?: PlayerStatsEntity()
            db.playerStatsDao().updateStats(stats.copy(soundEnabled = enabled))
        }
    }

    fun toggleHaptics() {
        val enabled = !soundManager.isHapticsEnabled
        soundManager.isHapticsEnabled = enabled
        viewModelScope.launch {
            val stats = db.playerStatsDao().getPlayerStatsSync() ?: PlayerStatsEntity()
            db.playerStatsDao().updateStats(stats.copy(hapticsEnabled = enabled))
        }
    }
}
