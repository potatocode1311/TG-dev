package com.example.gamestate

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.example.trivia_game.MainActivity
import com.example.trivia_game.utils.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.Serializable

//serializer to convert LocalDateTime objects to and from JSON format
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

//represents a serializable version of the team class for JSON storage
@Serializable
data class SerializableTeam(
    val name: String,
    val score: Int,
    val questionScore: Int,
    val currentRank: Int,
    val isLocked: Boolean,
    val buttonStates: MainActivity.ButtonStates
)

//data class for saved game variables that is serialized to JSON
@Serializable
data class SavedGameState(
    val id: Long = System.currentTimeMillis(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val savedAt: LocalDateTime = LocalDateTime.now(),
    val questionNumber: Int,
    val teams: List<SerializableTeam>,
    val timerSeconds: Int,
    val gameName: String = "Game ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm:ss"))}"
)

//manages saving and loading game states using SharedPreferences
//handles JSON serialization and maintains a list of saved games
//uses Android SharedPreferences for persistent storage
class GameStateManager(private val sharedPreferences: SharedPreferences) {
    private var savedGames = mutableListOf<SavedGameState>()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        prettyPrint = true
    }

    //constants for game state management
    companion object {
        private const val KEY_SAVED_GAMES = "saved_games"
        private const val MAX_HISTORY = 10
        const val AUTOSAVE_ID = -1L

    }

    init {
        loadSavedGames()
    }

    // Add function to get non-autosave games
    fun getGameHistory(): List<SavedGameState> {
        return savedGames
            .filter { it.id != AUTOSAVE_ID }
            .take(MAX_HISTORY)
            .sortedByDescending { it.savedAt }
    }


    fun saveGameToHistory(
        questionNumber: Int,
        teams: List<SerializableTeam>,
        timerSeconds: Int,
        gameName: String
    ) {
        val historyGame = SavedGameState(
            id = System.currentTimeMillis(),  // Unique timestamp ID
            savedAt = LocalDateTime.now(),
            questionNumber = questionNumber,
            teams = teams.map { it.copy() },  // Create deep copy of teams
            timerSeconds = timerSeconds,
            gameName = gameName
        )

        // Remove existing game with same name if exists (to avoid duplicates)
        savedGames.removeAll { it.gameName == gameName && it.id != AUTOSAVE_ID }

        // Add new game to history
        savedGames.add(0, historyGame)

        // Maintain history limit
        val nonAutoSaves = savedGames.filter { it.id != AUTOSAVE_ID }
        if (nonAutoSaves.size > MAX_HISTORY) {
            savedGames.removeAll { game ->
                game.id != AUTOSAVE_ID && game.savedAt <= nonAutoSaves[MAX_HISTORY - 1].savedAt
            }
        }

        persistGames()
    }



    fun saveAutoSaveGame(
        questionNumber: Int,
        teams: List<SerializableTeam>,
        timerSeconds: Int,
        gameName: String
    ) {
        val autoSaveGame = SavedGameState(
            //fixed ID for autosave slot
            id = AUTOSAVE_ID,
            questionNumber = questionNumber,
            teams = teams,
            timerSeconds = timerSeconds,
            gameName = "$gameName (Autosave)"
        )

        // Replace existing autosave or add new one
        val existingAutoSaveIndex = savedGames.indexOfFirst { it.id == AUTOSAVE_ID }
        if (existingAutoSaveIndex != -1) {
            savedGames[existingAutoSaveIndex] = autoSaveGame
        } else {
            savedGames.add(0, autoSaveGame)
        }
        persistGames()
    }



    //persists the saved games list to SharedPreferences as JSON and handles serialization errors
    private fun persistGames() {
        try {
            Logger.log("Persisting ${savedGames.size} games")
            val jsonString = json.encodeToString(savedGames)
            sharedPreferences.edit().putString(KEY_SAVED_GAMES, jsonString).apply()
        } catch (e: Exception) {
            Logger.log("Error persisting games", e)
            e.printStackTrace()
        }
    }



    private fun resetAutoSave() {
        //remove autosave history
        savedGames.removeAll { it.id == -1L }
        persistGames()
    }

    fun clearAllSavedGames() {
        // Clear both in-memory list and SharedPreferences
        savedGames.clear()
        sharedPreferences.edit().clear().apply()
        persistGames()
        println("All saved games and autosaves have been cleared.")
    }

    //loads saved games from SharedPreferences
    //creates empty list if no saves exist or if loading fails
    private fun loadSavedGames() {
        try {
            val jsonString = sharedPreferences.getString(KEY_SAVED_GAMES, "[]")
            savedGames = if (jsonString.isNullOrEmpty()) {
                mutableListOf()
            } else {
                json.decodeFromString<List<SavedGameState>>(jsonString).toMutableList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            savedGames = mutableListOf()
        }
    }

    //returns a copy of the saved games list
    fun getSavedGames(): List<SavedGameState> = savedGames.toList()


    //function to load a game from either autosave or history
    fun loadGame(id: Long): SavedGameState? {
        val game = savedGames.find { it.id == id }

        // Create deep copy of found game to prevent shared references
        return game?.copy(
            teams = game.teams.map { it.copy() }
        )
    }

}

