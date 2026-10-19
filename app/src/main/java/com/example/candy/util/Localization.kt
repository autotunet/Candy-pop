package com.example.candy.util

import androidx.compose.runtime.compositionLocalOf
import com.example.candy.model.CandyColor
import com.example.candy.model.SpecialCandyType

enum class AppLanguage(val label: String, val flag: String, val code: String) {
    ES("Español", "🇪🇸", "es"),
    EN("English", "🇺🇸", "en")
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ES }

data class AppStrings(
    val appName: String,
    val stars: String,
    val coins: String,
    val moves: String,
    val score: String,
    val targetObjectives: String,
    val targetScore: String,
    val clearJellies: String,
    val collectCandies: String,
    val createSpecials: String,
    val movesAllowed: String,
    val highScore: String,
    val play: String,
    val back: String,
    val cancel: String,
    val nextLevel: String,
    val replay: String,
    val map: String,
    val retry: String,
    val resume: String,
    val gamePaused: String,
    val restartLevel: String,
    val backToMap: String,
    val outOfMoves: String,
    val outOfMovesDesc: String,
    val addMovesOffer: String,
    val time: String,
    val timeUp: String,
    val addTimeOffer: String,
    val timedLevel: String,
    val levelCompleted: String,
    val sweetVictory: String,
    val coinsEarned: String,
    val sound: String,
    val vibration: String,
    val language: String,
    val howToPlay: String,
    val finalScore: String,
    // Combo praises & Streak
    val sweet: String,
    val tasty: String,
    val delicious: String,
    val sugarCrush: String,
    val reshuffle: String,
    val combo: String,
    val streak: String,
    val multiplier: String,
    val maxStreak: String,
    val streakReady: String,
    // Power-Up Announcements
    val powerUpStriped: String,
    val powerUpWrapped: String,
    val powerUpColorBomb: String,
    val powerUpComboCross: String,
    val powerUpMegaExplosion: String,
    val powerUpSupernova: String,
    // Map Progress
    val progress: String,
    val worlds: String,
    val jumpToCurrent: String,
    val currentLevelTag: String,
    val completedTag: String,
    // Boosters
    val hammerTitle: String,
    val hammerDesc: String,
    val switchTitle: String,
    val switchDesc: String,
    val bombTitle: String,
    val bombDesc: String,
    val hintTitle: String,
    val extraMovesTitle: String,
    val extraMovesDesc: String,
    // Tutorial
    val tutBasicTitle: String,
    val tutBasicBadge: String,
    val tutBasicDesc: String,
    val tutStripedTitle: String,
    val tutStripedBadge: String,
    val tutStripedDesc: String,
    val tutWrappedTitle: String,
    val tutWrappedBadge: String,
    val tutWrappedDesc: String,
    val tutBombTitle: String,
    val tutBombBadge: String,
    val tutBombDesc: String,
    val tutCombosTitle: String,
    val tutCombosBadge: String,
    val tutCombosDesc: String,
    val tutBoostersTitle: String,
    val tutBoostersBadge: String,
    val tutBoostersDesc: String
)

object Localization {
    val Spanish = AppStrings(
        appName = "Candy Pop",
        stars = "Estrellas",
        coins = "Monedas",
        moves = "MOVIMIENTOS",
        score = "Puntos",
        targetObjectives = "Objetivos:",
        targetScore = "Puntuación objetivo",
        clearJellies = "Eliminar gelatinas",
        collectCandies = "Recolectar",
        createSpecials = "Crear",
        movesAllowed = "Movimientos permitidos",
        highScore = "Puntuación récord:",
        play = "JUGAR",
        back = "Atrás",
        cancel = "Cancelar",
        nextLevel = "SIGUIENTE NIVEL",
        replay = "Repetir",
        map = "Mapa",
        retry = "Reintentar",
        resume = "CONTINUAR",
        gamePaused = "JUEGO PAUSADO",
        restartLevel = "Reiniciar nivel",
        backToMap = "Volver al mapa",
        outOfMoves = "¡SIN MOVIMIENTOS!",
        outOfMovesDesc = "¡No te rindas! Continúa la fiebre del azúcar.",
        addMovesOffer = "+5 Movimientos extra (🪙 100)",
        time = "Tiempo",
        timeUp = "¡TIEMPO AGOTADO!",
        addTimeOffer = "+15 Segundos extra (🪙 100)",
        timedLevel = "⏱️ Nivel con Temporizador",
        levelCompleted = "¡Nivel completado!",
        sweetVictory = "🎉 ¡DULCE VICTORIA! 🎉",
        coinsEarned = "Monedas ganadas",
        sound = "Sonido",
        vibration = "Vibración",
        language = "Idioma",
        howToPlay = "Cómo Jugar",
        finalScore = "Puntuación Final",
        sweet = "¡DULCE!",
        tasty = "¡SABROSO!",
        delicious = "¡DELICIOSO!",
        sugarCrush = "¡SUGAR CRUSH!",
        reshuffle = "¡REORGANIZANDO!",
        combo = "COMBO",
        streak = "RACHA",
        multiplier = "MULTIPLICADOR",
        maxStreak = "Racha Máxima",
        streakReady = "¡Haz combinaciones para activar la racha!",
        powerUpStriped = "¡CARAMELO RAYADO!",
        powerUpWrapped = "¡CARAMELO ENVUELTO!",
        powerUpColorBomb = "¡BOMBA DE COLOR!",
        powerUpComboCross = "¡LÁSER CRUZADO!",
        powerUpMegaExplosion = "¡MEGA EXPLOSIÓN!",
        powerUpSupernova = "¡SUPERNOVA!",
        progress = "Progreso",
        worlds = "Mundos",
        jumpToCurrent = "Nivel Actual",
        currentLevelTag = "¡AQUÍ!",
        completedTag = "Completado",
        hammerTitle = "Martillo",
        hammerDesc = "Rompe cualquier caramelo o gelatina",
        switchTitle = "Cambio",
        switchDesc = "Intercambia 2 caramelos sin gastar turno",
        bombTitle = "Bomba",
        bombDesc = "Coloca una bomba de color arcoíris",
        hintTitle = "Pista",
        extraMovesTitle = "+5 Turnos",
        extraMovesDesc = "Agrega 5 movimientos para seguir jugando",
        tutBasicTitle = "Coincidencia Básica de 3",
        tutBasicBadge = "LÍNEA DE 3",
        tutBasicDesc = "Intercambia caramelos adyacentes horizontal o verticalmente para alinear 3 del mismo color y hacerlos estallar por puntos.",
        tutStripedTitle = "Caramelo Rayado",
        tutStripedBadge = "LÍNEA DE 4",
        tutStripedDesc = "Alinea 4 caramelos en fila o columna para crear un Caramelo Rayado. Al explotar, dispara un rayo que limpia toda la fila o columna.",
        tutWrappedTitle = "Caramelo Envuelto",
        tutWrappedBadge = "FORMA T o L",
        tutWrappedDesc = "Alinea 5 caramelos en forma de T o L para forjar un Caramelo Envuelto. ¡Explota en un radio gigante de 3x3 destruyendo todo alrededor!",
        tutBombTitle = "Bomba de Color Arcoíris",
        tutBombBadge = "5 EN LÍNEA",
        tutBombDesc = "Combina 5 caramelos en línea recta para crear la legendaria Bomba de Color. ¡Intercámbiala con cualquier color para destruir todos los caramelos de ese color en el tablero!",
        tutCombosTitle = "Cascadas y Combos",
        tutCombosBadge = "CADENAS",
        tutCombosDesc = "Cuando los caramelos caen y forman nuevas combinaciones automáticamente, obtienes multiplicadores enormes y elogios: '¡Dulce!', '¡Sabroso!', '¡Delicioso!' y '¡Sugar Crush!'.",
        tutBoostersTitle = "Potenciadores Mágicos",
        tutBoostersBadge = "AYUDAS",
        tutBoostersDesc = "Usa el Martillo de Piruleta para romper cualquier casilla difícil o el Cambio Libre para intercambiar sin gastar movimientos."
    )

    val English = AppStrings(
        appName = "Candy Pop",
        stars = "Stars",
        coins = "Coins",
        moves = "MOVES",
        score = "Score",
        targetObjectives = "Target Objectives:",
        targetScore = "Target Score",
        clearJellies = "Clear jelly tiles",
        collectCandies = "Collect",
        createSpecials = "Create",
        movesAllowed = "Moves Allowed",
        highScore = "High Score:",
        play = "PLAY",
        back = "Back",
        cancel = "Cancel",
        nextLevel = "NEXT LEVEL",
        replay = "Replay",
        map = "Map",
        retry = "Retry",
        resume = "RESUME",
        gamePaused = "GAME PAUSED",
        restartLevel = "Restart Level",
        backToMap = "Back to Level Map",
        outOfMoves = "OUT OF MOVES!",
        outOfMovesDesc = "Don't give up! Keep the sugar rush going.",
        addMovesOffer = "+5 Extra Moves! (🪙 100)",
        time = "Time",
        timeUp = "TIME'S UP!",
        addTimeOffer = "+15 Extra Seconds! (🪙 100)",
        timedLevel = "⏱️ Timed Level",
        levelCompleted = "Level Completed!",
        sweetVictory = "🎉 SWEET VICTORY! 🎉",
        coinsEarned = "Coins Earned",
        sound = "Sound",
        vibration = "Vibration",
        language = "Language",
        howToPlay = "How to Play",
        finalScore = "Final Score",
        sweet = "SWEET!",
        tasty = "TASTY!",
        delicious = "DELICIOUS!",
        sugarCrush = "SUGAR CRUSH!",
        reshuffle = "RESHUFFLE!",
        combo = "COMBO",
        streak = "STREAK",
        multiplier = "MULTIPLIER",
        maxStreak = "Max Streak",
        streakReady = "Make matches to build your streak!",
        powerUpStriped = "STRIPED CANDY!",
        powerUpWrapped = "WRAPPED CANDY!",
        powerUpColorBomb = "COLOR BOMB!",
        powerUpComboCross = "CROSS LASER BLAST!",
        powerUpMegaExplosion = "MEGA EXPLOSION!",
        powerUpSupernova = "CANDY SUPERNOVA!",
        progress = "Progress",
        worlds = "Worlds",
        jumpToCurrent = "Current Level",
        currentLevelTag = "PLAY!",
        completedTag = "Completed",
        hammerTitle = "Hammer",
        hammerDesc = "Smash any single candy or jelly",
        switchTitle = "Switch",
        switchDesc = "Swap any 2 candies without spending a move",
        bombTitle = "Bomb",
        bombDesc = "Place a rainbow color bomb anywhere",
        hintTitle = "Hint",
        extraMovesTitle = "+5 Moves",
        extraMovesDesc = "Add 5 extra moves to keep playing",
        tutBasicTitle = "Basic Match-Three",
        tutBasicBadge = "MATCH 3",
        tutBasicDesc = "Swap adjacent candies horizontally or vertically to line up 3 of the same color and pop them for sweet points!",
        tutStripedTitle = "Striped Candy",
        tutStripedBadge = "MATCH 4",
        tutStripedDesc = "Match 4 candies in a line to create a Striped Candy. When matched, it fires a dazzling sugar beam clearing an entire row or column!",
        tutWrappedTitle = "Wrapped Candy",
        tutWrappedBadge = "T or L SHAPE",
        tutWrappedDesc = "Match 5 candies in a T or L formation to create a Wrapped Candy! When popped, it explodes in a massive 3x3 blast, clearing all nearby treats!",
        tutBombTitle = "Rainbow Color Bomb",
        tutBombBadge = "5 IN A LINE",
        tutBombDesc = "Match 5 candies in a straight line to create the legendary Color Bomb! Swap it with any candy color to instantly zap and destroy ALL candies of that color!",
        tutCombosTitle = "Cascades & Sugar Crush",
        tutCombosBadge = "COMBOS",
        tutCombosDesc = "When candies fall and trigger more matches automatically, you earn massive combo multipliers and hear praise: 'Sweet!', 'Tasty!', 'Delicious!', and 'Sugar Crush'!",
        tutBoostersTitle = "Lollipop Hammer & Boosters",
        tutBoostersBadge = "BOOSTERS",
        tutBoostersDesc = "Use the Lollipop Hammer to smash any stubborn candy or jelly! Use the Free Switch to swap any two candies without spending a turn."
    )

    fun getStrings(lang: AppLanguage): AppStrings = when (lang) {
        AppLanguage.ES -> Spanish
        AppLanguage.EN -> English
    }

    fun getCandyName(color: CandyColor, lang: AppLanguage): String = when (lang) {
        AppLanguage.ES -> when (color) {
            CandyColor.RED -> "Fresa"
            CandyColor.YELLOW -> "Limón"
            CandyColor.ORANGE -> "Naranja"
            CandyColor.GREEN -> "Menta"
            CandyColor.BLUE -> "Arándano"
            CandyColor.PURPLE -> "Uva"
        }
        AppLanguage.EN -> color.displayName
    }

    fun getSpecialName(special: SpecialCandyType, lang: AppLanguage): String = when (lang) {
        AppLanguage.ES -> when (special) {
            SpecialCandyType.STRIPED_HORIZONTAL -> "Caramelo Rayado"
            SpecialCandyType.STRIPED_VERTICAL -> "Caramelo Rayado"
            SpecialCandyType.WRAPPED -> "Caramelo Envuelto"
            SpecialCandyType.COLOR_BOMB -> "Bomba de Color"
            SpecialCandyType.NONE -> "Caramelo"
        }
        AppLanguage.EN -> when (special) {
            SpecialCandyType.STRIPED_HORIZONTAL, SpecialCandyType.STRIPED_VERTICAL -> "Striped Candy"
            SpecialCandyType.WRAPPED -> "Wrapped Candy"
            SpecialCandyType.COLOR_BOMB -> "Color Bomb"
            SpecialCandyType.NONE -> "Candy"
        }
    }

    fun getLevelName(levelNumber: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ES -> when (levelNumber) {
            1 -> "Pradera de Azúcar"
            2 -> "Arboleda de Fresas"
            3 -> "Lago de Gelatina"
            4 -> "Torbellino Rayado"
            5 -> "Cruz de Gomita"
            6 -> "Delicia Envuelta"
            7 -> "Carnaval Arcoíris"
            8 -> "Esquinas de Gelatina"
            9 -> "Explosión Cítrica"
            10 -> "Ciudadela Dulce"
            11 -> "Cañón de Caramelo"
            12 -> "Picos de Algodón"
            13 -> "Palacio de Menta"
            14 -> "Ventisca de Bayas"
            15 -> "Corona del Rey Dulce"
            else -> "Nivel $levelNumber"
        }
        AppLanguage.EN -> when (levelNumber) {
            1 -> "Sugar Meadow"
            2 -> "Strawberry Grove"
            3 -> "Jelly Lake"
            4 -> "Striped Whirlwind"
            5 -> "Gummy Cross"
            6 -> "Wrapped Delight"
            7 -> "Rainbow Carnival"
            8 -> "Corner Craze"
            9 -> "Citrus Blast"
            10 -> "Sweet Citadel"
            11 -> "Caramel Canyon"
            12 -> "Cotton Candy Peaks"
            13 -> "Peppermint Palace"
            14 -> "Berry Blizzard"
            15 -> "Crown of Sugar King"
            else -> "Level $levelNumber"
        }
    }

    fun getLevelQuote(levelNumber: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ES -> when (levelNumber) {
            1 -> "¡Bienvenido a la pradera dulce! Alinea 3 caramelos en línea."
            2 -> "¡Recolecta fresas jugosas y gotas dulces de limón!"
            3 -> "¡Limpia todas las gelatinas brillantes debajo de los dulces!"
            4 -> "¡Alinea 4 caramelos seguidos para crear caramelos rayados!"
            5 -> "Una cruz brillante de gelatina espera tus dulces combinaciones."
            6 -> "¡Crea caramelos envueltos haciendo combinaciones en T o L!"
            7 -> "¡Alinea 5 caramelos en línea para forjar una Bomba de Color!"
            8 -> "¡Elimina las difíciles gelatinas de las esquinas!"
            9 -> "¡Montones de delicias cítricas para recolectar con movimientos limitados!"
            10 -> "¡Las puertas del castillo están cubiertas de gelatina! Desata tus mejores cascadas."
            11 -> "¡Combina una Bomba de Color con un Rayado para limpiar el tablero!"
            12 -> "Las crestas diagonales de gelatina exigen máxima precisión."
            13 -> "¡Forja un arsenal de caramelos rayados y envueltos!"
            14 -> "¡Una ola gigante de bayas! Desencadena grandes reacciones en cadena."
            15 -> "¡El triunfo supremo de la confitería! ¡Sé el Rey de los Dulces!"
            else -> "¡El dulce sabor de la victoria!"
        }
        AppLanguage.EN -> when (levelNumber) {
            1 -> "Welcome to the sweet candy meadow! Match 3 treats in a line."
            2 -> "Pick juicy strawberries and sweet lemon drops!"
            3 -> "Clear all the shimmering jelly tiles under the candy!"
            4 -> "Match 4 candies in a row to create dazzling striped candies!"
            5 -> "A sparkling cross of jelly awaits your sweet combos."
            6 -> "Match 5 candies in a T or L shape to create explosive wrapped treats!"
            7 -> "Match 5 candies in a straight line to craft a legendary Color Bomb!"
            8 -> "Clear the tricky corner jellies before your moves run out!"
            9 -> "Tons of citrus goodness to harvest in limited moves!"
            10 -> "The castle gates are coated in thick jelly! Unleash your best cascades."
            11 -> "Combine Color Bomb with a Striped candy for board-clearing spectacle!"
            12 -> "The sweet diagonal jelly crests demand master precision."
            13 -> "Craft an arsenal of striped and wrapped sweets to shatter the goals!"
            14 -> "A massive berry wave! Trigger huge chain reactions."
            15 -> "The ultimate confectionery triumph! Become the King of Candy!"
            else -> "Sweet taste of victory!"
        }
    }
}
