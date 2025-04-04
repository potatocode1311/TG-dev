package com.example.trivia_game

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.example.gamestate.GameStateManager
import com.example.gamestate.SerializableTeam
import com.example.trivia_game.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable



class MainActivity : AppCompatActivity() {
    private var questionNumber: Int = 0
    private val teams = mutableListOf<Team>()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private var scoresLocked = false
    private var timerJob: Job? = null
    private var timerSeconds = 0
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var gameStateManager: GameStateManager

    //data class for team that initializes it as a string and defaults score/question score to 0
    //and initializes the switch for each team
    @Parcelize
    @Serializable
    data class Team(
        var name: String,
        var score: Int = 0,
        var questionScore: Int = 0,
        var currentRank: Int = 0,
        var isLocked: Boolean = false,
        var buttonStates: ButtonStates = ButtonStates()
    ) : Parcelable {
        //constructors and functions to save team name and score
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readInt()
        )

        companion object : Parceler<Team> {

            override fun Team.write(parcel: Parcel, flags: Int) {
                parcel.writeString(name)
                parcel.writeInt(score)
            }

            override fun create(parcel: Parcel): Team {
                return Team(parcel)
            }
        }

    }

    //companion keys to save time and game state
    companion object {
        //timer keys to track time
        private const val KEY_TIMER_SECONDS = "timer_seconds"
        private const val KEY_TIMER_RUNNING = "timer_running"
    }

    //ranked team class to establish a ranking system between teams based on score
    private data class RankedTeam(
        val team: Team,
        val originalIndex: Int,
        var rank: Int = 0
    )

    //button states class to track button states
    @Serializable
    data class ButtonStates(
        var addEnabled: Boolean = true,
        var subtractEnabled: Boolean = true
    )

    //function that goes through and resets team switches
    private fun resetTeamSwitches() {
        teams.forEach { team ->
            team.isLocked = false
        }
    }

    //handles timer display
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    //starts timer
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                timerSeconds++
                binding.timerText.text = formatTime(timerSeconds)
            }
        }
    }

    //resets timer
    private fun resetTimer() {
        timerJob?.cancel()
        timerSeconds = 0
        binding.timerText.text = formatTime(timerSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        scope.cancel()
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //main activity initialization
        gameStateManager = GameStateManager(getSharedPreferences("GamePrefs", Context.MODE_PRIVATE))
        startAutoSave()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //restore timer state if it exists
        savedInstanceState?.let { bundle ->
            timerSeconds = bundle.getInt(KEY_TIMER_SECONDS, 0)
            if (bundle.getBoolean(KEY_TIMER_RUNNING, false)) {
                startTimer()
            }
            binding.timerText.text = formatTime(timerSeconds)
        }

        //restore teams list if it exists
        if (savedInstanceState != null) {

            //restore teams
            val savedTeams = savedInstanceState.getParcelableArrayList<Team>("teams")
            teams.clear()
            if (savedTeams != null) {
                teams.addAll(savedTeams)
            }

            //restore input visibility
            val isInputVisible = savedInstanceState.getBoolean("isInputVisible", false)
            binding.teamsContainer.visibility = if (isInputVisible) View.VISIBLE else View.GONE

            //restore current round
            questionNumber = savedInstanceState.getInt("questionNumber", 1)

            //update display
            displayTeams()

            //update round number
            updateQuestionDisplay()
        }

        //initialize the round number display
        binding.questionNumber.text = "Question: $questionNumber"
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //button initialization
        val button1 = findViewById<Button>(R.id.newGame)
        val button2 = findViewById<Button>(R.id.addTeam)
        val button3 = findViewById<Button>(R.id.nextQuestion)

        //show confirmation dialog boxes when buttons are pushed
        button1.setOnClickListener {
            showConfirmationDialog("New Game")
        }
        button2.setOnClickListener {
            showConfirmationDialog("Add Team")
        }
        button3.setOnClickListener {
            showConfirmationDialog("Next Question")
        }
        binding.loadGameButton.setOnClickListener {
            showLoadGameDialog()
        }
    }


    //function that autosaves every 10 seconds - 10000 milliseconds = 10 seconds
    private fun startAutoSave() {
        scope.launch {
            while (isActive) {
                delay(10000) // Save every minute
                saveCurrentGameState()
            }
        }
    }

    //function that saves all current game variables, switch/button states, and ranks
    private fun saveCurrentGameState() {
        val serializableTeams = teams.map { team ->
            SerializableTeam(
                name = team.name,
                score = team.score,
                questionScore = team.questionScore,
                currentRank = team.currentRank,
                isLocked = team.isLocked,
                buttonStates = team.buttonStates
            )
        }

        gameStateManager.saveCurrentGame(
            questionNumber = questionNumber,
            teams = serializableTeams,
            timerSeconds = timerSeconds
        )
    }

    private fun showLoadGameDialog() {
        val savedGames = gameStateManager.getSavedGames()
        if (savedGames.isEmpty()) {
            Toast.makeText(this, "No saved games found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Load Game")
            .setItems(savedGames.map { it.gameName }.toTypedArray()) { _, index ->
                loadGame(savedGames[index].id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadGame(gameId: Long) {
        gameStateManager.loadGame(gameId)?.let { savedGame ->
            //convert SerializableTeam back to Team
            teams.clear()
            teams.addAll(savedGame.teams.map { serializableTeam ->
                Team(serializableTeam.name).apply {
                    score = serializableTeam.score
                    questionScore = serializableTeam.questionScore
                    currentRank = serializableTeam.currentRank
                    isLocked = serializableTeam.isLocked
                    buttonStates = serializableTeam.buttonStates
                }
            })
            //restore game state
            questionNumber = savedGame.questionNumber
            timerSeconds = savedGame.timerSeconds

            //update UI
            binding.questionNumber.text = "Question: $questionNumber"
            binding.timerText.text = formatTime(timerSeconds)
            updateTeamCount()
            calculateAndApplyRankings()
            displayTeams()

            //restart timer
            timerJob?.cancel()
            startTimer()

            Toast.makeText(this, "Game loaded successfully", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("teams", ArrayList(teams))
        outState.putBoolean("isInputVisible", binding.teamsContainer.visibility == View.VISIBLE)

        //save the current question
        outState.putInt("questionNumber", questionNumber)

        //save current time
        outState.putInt(KEY_TIMER_SECONDS, timerSeconds)
        outState.putBoolean(KEY_TIMER_RUNNING, timerJob?.isActive == true)
    }

    //function that updates question display counter
    private fun updateQuestionDisplay() {
        //update question number to current view on save state
        binding.questionNumber.text = "Question $questionNumber"

    }
    //rank teams based on score
    private fun calculateAndApplyRankings() {
        val rankedTeams = teams.mapIndexed { index, team ->
            RankedTeam(team, index)
        }.groupBy {
            it.team.score
        }.entries.sortedByDescending {
            it.key
        }.flatMapIndexed { groupIndex, group ->
            val rank = groupIndex + 1
            group.value.map { rankedTeam ->
                rankedTeam.apply { this.rank = rank }
            }
        }

        //store ranks in sorted order (highest rank at top)
        rankedTeams.forEach { rankedTeam ->
            teams[rankedTeam.originalIndex].currentRank = rankedTeam.rank
        }

        //sort teams array by rank (lowest number at top)
        teams.sortBy { it.currentRank }
    }

    //function that brings up team name dialog box to enter team name
    private fun showTeamNameInputDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Team Name")

        //set up the input field
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.hint = "Team Name"

        //set up IME action listener
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //hide keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(input.windowToken, 0)
                return@setOnEditorActionListener true
            }
            false
        }

        //add some padding around the EditText
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        //set up the buttons
        builder.setPositiveButton("OK") { dialog, _ ->
            val teamName = input.text.toString()
            if (teamName.isNotBlank()) {
                //hide keyboard first
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(input.windowToken, 0)
                //get current button states for other teams that might be added
                val currentStates = teams.map { team ->
                    Triple(
                        team.isLocked,
                        team.buttonStates.addEnabled,
                        team.buttonStates.subtractEnabled
                    )
                }

                addTeam(teamName)

                //restore states of other teams buttons
                teams.take(currentStates.size).forEachIndexed { index, team ->
                    val (isLocked, addEnabled, subtractEnabled) = currentStates[index]
                    team.isLocked = isLocked
                    team.buttonStates.addEnabled = addEnabled
                    team.buttonStates.subtractEnabled = subtractEnabled
                }
                calculateAndApplyRankings()
                displayTeams()
                Toast.makeText(this, "Team '$teamName' added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Team name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            //hide keyboard when canceled
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            dialog.cancel()
        }

        val dialog = builder.create()

        //set dialog dismissal listener to ensure keyboard is hidden
        dialog.setOnDismissListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
        }

        //force dialog to use a specific configuration
        dialog.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        dialog.show()

        //show keyboard with a slight delay
        input.post {
            input.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    //function that shows dialog boxes based on which button is pressed
    private fun showConfirmationDialog(actionName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Action")
        when (actionName) {
            "New Game" -> {
                builder.setMessage("Are you sure you want to make a $actionName?")
            }

            "Add Team" -> {
                builder.setMessage("Are you sure you want to make a new team?")
            }

            "Next Question" -> {
                builder.setMessage("Are you sure you want to proceed to the next question?")
            }
        }

        builder.setPositiveButton("Yes") { dialog: DialogInterface, id: Int ->
            //user clicked Yes button
            Toast.makeText(this, "Action confirmed!", Toast.LENGTH_SHORT).show()
            when (actionName) {
                "Add Team" -> {
                    //create team name dialog box to enter a team name and display it
                    showTeamNameInputDialog()
                }

                "Next Question" -> {

                    if (teams.isEmpty()) {
                        //show dialog if no teams exist
                        AlertDialog.Builder(this)
                            .setTitle("No Teams")
                            .setMessage("Please add at least one team before proceeding to the next question.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@setPositiveButton
                    }
                    val unlockedTeams = checkTeamStates()

                    if (unlockedTeams.isNotEmpty()) {
                        showUnlockedTeamsDialog(unlockedTeams)
                    }
                    else {
                        //increment question number
                        questionNumber++

                        //update question container with new question number
                        binding.questionNumber.text = "Question: $questionNumber"

                        //update total scores and reset question scores
                        finalizeQuestionScores()

                        //reset timer and restart timer
                        timerSeconds = 0
                        startTimer()

                        //reset switches for next question
                        resetTeamSwitches()

                        //calculate rankings
                        calculateAndApplyRankings()

                        //update teams with new info
                        displayTeams()
                    }
                }
                "New Game" -> {

                    //reset timer
                    resetTimer()

                    //clear the teamsContainer
                    binding.teamsContainer.removeAllViews()

                    //reset team counter
                    binding.teamCountText.text = "Teams: 0"

                    //reset question number
                    questionNumber = 0
                    binding.questionNumber.text = "Question: $questionNumber"

                    //clear current teams
                    teams.clear()
                }
            }
        }

        builder.setNegativeButton("No") { dialog: DialogInterface, id: Int ->
            //user cancelled the dialog
            Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    //add teams function that adds teams and updates ui
    private fun addTeam(name: String) {
        val newTeam = Team(name)
        teams.add(newTeam)
        displayTeams()
        updateTeamCount()
    }

    //function that adds score and total questions this score for new total
    private fun finalizeQuestionScores() {
        teams.forEach { team ->
            team.score += team.questionScore
            //reset score for next question
            team.questionScore = 0
        }
    }

    //function that allows pop up menu for teams names to be edited or deleted
    private fun showTeamOptionsMenu(view: View, team: Team, teamIndex: Int) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.team_options_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit_team_name -> {
                    showEditTeamNameDialog(team, teamIndex)
                    true
                }
                R.id.delete_team -> {
                    showDeleteTeamConfirmation(teamIndex)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    //function that allows teams counter to increment
    private fun updateTeamCount() {
        binding.teamCountText.text = "Teams: ${teams.size}"
    }

    //function that allows teams to be edited when selected
    private fun showEditTeamNameDialog(team: Team, teamIndex: Int) {
        val editText = EditText(this).apply {
            setText(team.name)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Edit Team Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    team.name = newName
                    displayTeams()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //function that allows teams to be deleted when selected
    private fun showDeleteTeamConfirmation(teamIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Team")
            .setMessage("Are you sure you want to delete this team?")
            .setPositiveButton("Delete") { _, _ ->
                teams.removeAt(teamIndex)
                displayTeams()
                updateTeamCount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //function that checks to see if teams are locked
    private fun checkTeamStates(): List<String> {
        val unlockedTeams = mutableListOf<String>()
        teams.forEach { team ->
            if (!team.isLocked) {
                unlockedTeams.add(team.name)
            }
        }
        return unlockedTeams
    }

    //function that checks to see if teams have been scored
    private fun showUnlockedTeamsDialog(unlockedTeams: List<String>) {
        val message = buildString {
            append("The following teams may not have been scored for this question:\n\n")
            unlockedTeams.forEach { teamName ->
                append("• $teamName\n")
            }
            append("\nPlease lock/score all teams before proceeding.")
        }

        AlertDialog.Builder(this)
            .setTitle("Teams not scored/locked")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    //function to display teams in the scrollview
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun displayTeams() {
        binding.teamsContainer.removeAllViews()
        updateTeamCount()
        //layout for teams to be displayed
        teams.forEachIndexed { index, team ->
            val teamLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                setPadding(16, 16, 16, 16)

                //sets background color for teams
                setBackgroundResource(R.drawable.team_card_background)
                //add long press listener to team layout to edit/delete team
                setOnLongClickListener { view ->
                    showTeamOptionsMenu(view, team, index)
                    true
                }
            }

            //add rank display
            val rankView = TextView(this).apply {
                text = "#${team.currentRank}"
                textSize = 18f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(16)
                    gravity = Gravity.CENTER_VERTICAL
                }
            }
            teamLayout.addView(rankView)

            //team name and score in a vertical layout (left side)
            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            //team name styling
            val nameView = TextView(this).apply {
                text = team.name
                textSize = 18f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
            }
            infoLayout.addView(nameView)

            //score styling
            val scoreView = TextView(this).apply {
                text = "Total Score: ${team.score}\nThis Question: ${team.questionScore}"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    //add margin between name and score
                    topMargin = dpToPx(8)
                    //add margin below score
                    bottomMargin = dpToPx(8)
                }
            }
            infoLayout.addView(scoreView)

            teamLayout.addView(infoLayout)

            //button container for +/- buttons
            val buttonContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            //define point values in matching pairs
            val buttonPairs = listOf(
                Pair(1, arrayOf(-1, "+1")),
                Pair(5, arrayOf(-5, "+5")),
                Pair(10, arrayOf(-10, "+10"))
            )

            //create columns container
            val buttonColumnsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            //create left column for negative buttons
            val negativeButtonColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(8)
                }
            }

            //create right column for positive buttons
            val positiveButtonColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(8)
                }
            }

            //create matching pairs of buttons
            buttonPairs.forEach { (value, labels) ->
                //create negative button
                val negativeButton = Button(this).apply {
                    text = labels[0].toString()
                    isEnabled = team.buttonStates.addEnabled && !team.isLocked
                    alpha = if (isEnabled) 1.0f else 0.5f
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(50),
                        dpToPx(40)
                    ).apply {
                        bottomMargin = dpToPx(4)
                    }
                    setOnClickListener {
                        updateTeamScore(index, -value)
                    }
                }
                negativeButtonColumn.addView(negativeButton)

                //create positive button
                val positiveButton = Button(this).apply {
                    text = labels[1].toString()
                    isEnabled = team.buttonStates.addEnabled && !team.isLocked
                    alpha = if (isEnabled) 1.0f else 0.5f
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(50),
                        dpToPx(40)
                    ).apply {
                        bottomMargin = dpToPx(4)
                    }
                    setOnClickListener {
                        updateTeamScore(index, value)
                    }
                }
                positiveButtonColumn.addView(positiveButton)
            }

            //create switch for each team
            val teamSwitch = Switch(this).apply {
                isChecked = team.isLocked
                isEnabled = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                //set initial button states
                buttonColumnsContainer.children.forEach { column ->
                    if (column is LinearLayout) {
                        column.children.forEach { button ->
                            if (button is Button) {
                                button.isEnabled = !team.isLocked &&
                                        (team.buttonStates.addEnabled || team.buttonStates.subtractEnabled)
                                button.alpha = if (button.isEnabled) 1.0f else 0.5f
                            }
                        }
                    }
                }

                //disable/enable buttons in both columns if switch is pressed
                setOnCheckedChangeListener { _, isChecked ->
                    team.isLocked = isChecked
                    buttonColumnsContainer.children.forEach { column ->
                        if (column is LinearLayout) {
                            column.children.forEach { button ->
                                if (button is Button) {
                                    button.isEnabled = !isChecked &&
                                            (team.buttonStates.addEnabled || team.buttonStates.subtractEnabled)
                                    button.alpha = if (button.isEnabled) 1.0f else 0.5f
                                }
                            }
                        }
                    }
                }
            }

            //create a separate container for the switch
            val switchContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            //add switch to its own container
            switchContainer.addView(teamSwitch)

            //add switch containers to team layout
            teamLayout.addView(switchContainer)

            //add +/- columns to container
            buttonColumnsContainer.addView(negativeButtonColumn)
            buttonColumnsContainer.addView(positiveButtonColumn)

            //add columns container to main button container
            buttonContainer.addView(buttonColumnsContainer)

            //add buttonContainer to teamLayout
            teamLayout.addView(buttonContainer)

            //add teamLayout to entire teams container with all buttons/team name/switches
            binding.teamsContainer.addView(teamLayout)
        }
    }

    //helper function for dp to pixel conversion
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    //function to update score when +/- buttons are used
    private fun updateTeamScore(teamIndex: Int, points: Int) {
        if (scoresLocked) return
        if (teamIndex in teams.indices) {
            teams[teamIndex].questionScore += points
            displayTeams()
            Toast.makeText(
                this,
                "${teams[teamIndex].name}: ${if (points > 0) "+" else ""}$points points",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}