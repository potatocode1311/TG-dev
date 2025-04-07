package com.example.gamestate

import android.content.SharedPreferences
import com.example.trivia_game.MainActivity
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
        private const val MAX_SAVED_GAMES = 10
    }

    init {
        loadSavedGames()
    }


    fun saveAutoSaveGame(
        questionNumber: Int,
        teams: List<SerializableTeam>,
        timerSeconds: Int
    ) {
        val autoSaveGame = SavedGameState(
            //fixed ID for autosave slot
            id = -1L,
            questionNumber = questionNumber,
            teams = teams,
            timerSeconds = timerSeconds,
            gameName = "Autosave"
        )

        //check if an autosave already exists
        val existingAutoSaveIndex = savedGames.indexOfFirst { it.id == -1L }
        if (existingAutoSaveIndex != -1) {
            //overwrite the existing autosave
            savedGames[existingAutoSaveIndex] = autoSaveGame
        } else {
            //add a new autosave
            savedGames.add(0, autoSaveGame)
        }

        //persist the updated list of saved games
        persistGames()
    }

    //saves the current game state for question number, current list of teams, and current timer value
    fun saveCurrentGame(
        questionNumber: Int,
        teams: List<SerializableTeam>,
        timerSeconds: Int
    ) {
        val currentGame = SavedGameState(
            questionNumber = questionNumber,
            teams = teams,
            timerSeconds = timerSeconds
        )

        savedGames.add(0, currentGame)
        if (savedGames.size > MAX_SAVED_GAMES) {
            savedGames.removeAt(MAX_SAVED_GAMES)
        }

        persistGames()
    }

    //persists the saved games list to SharedPreferences as JSON and handles serialization errors
    private fun persistGames() {
        try {
            val jsonString = json.encodeToString(savedGames)
            sharedPreferences.edit().putString(KEY_SAVED_GAMES, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetAutoSave() {
        savedGames.removeAll { it.id == -1L } // Remove the autosave entry
        persistGames()
    }

    fun clearSavedGames() {
        savedGames.clear() // Clear the in-memory list
        persistGames() // Persist the empty list to SharedPreferences
        println("All saved games have been cleared.")
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

    //retrieves a specific saved game by its ID
    fun loadGame(id: Long): SavedGameState? {
        return savedGames.find { it.id == id }
    }

}

