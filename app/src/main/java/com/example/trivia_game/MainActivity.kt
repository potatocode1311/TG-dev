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
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.trivia_game.databinding.ActivityMainBinding
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize



class MainActivity : AppCompatActivity() {
    private var questionNumber: Int = 1
    private val teams = mutableListOf<Team>()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private var scoresLocked = false
    //data class for team that initializes it as a string and defaults score/question score to 0
    //and initializes the switch for each team
    @Parcelize
    data class Team(
        val name: String,
        var score: Int = 0,
        var questionScore: Int = 0,
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

    //button states class to track button states
    data class ButtonStates(
        var addEnabled: Boolean = true,
        var subtractEnabled: Boolean = true
    )

    private fun resetTeamSwitches() {
        teams.forEach { team ->
            team.isLocked = false
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //main activity initialization
        WindowCompat.setDecorFitsSystemWindows(window, false)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //initialize teams list if not already initialized
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
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("teams", ArrayList(teams))
        outState.putBoolean("isInputVisible", binding.teamsContainer.visibility == View.VISIBLE)

        //save the current question
        outState.putInt("questionNumber", questionNumber)
    }

    private fun updateQuestionDisplay() {
        //update question number to current view on save state
        binding.questionNumber.text = "Question $questionNumber"

    }

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
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
                displayTeams()
                Toast.makeText(this, "Team '$teamName' added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Team name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            //hide keyboard when canceled
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            dialog.cancel()
        }

        val dialog = builder.create()

        //set dialog dismissal listener to ensure keyboard is hidden
        dialog.setOnDismissListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }
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
                    //increment question number
                    questionNumber++

                    //update question container with new question number
                    binding.questionNumber.text = "Question: $questionNumber"

                    //update total scores and reset question scores
                    finalizeQuestionScores()

                    //reset switches for next question
                    resetTeamSwitches()

                    //sort teams by highest to lowest score
                    teams.sortByDescending { it.score }

                    //update switch and lock state
                    displayTeams()
                }
                "New Game" -> {

                    //clear the teamsContainer
                    binding.teamsContainer.removeAllViews()
                    //reset question number
                    questionNumber = 1
                    teams.clear()
                    binding.questionNumber.text = "Question: $questionNumber"

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
    }
    //function that adds score and total questions this score for new total
    private fun finalizeQuestionScores() {
        teams.forEach { team ->
            team.score += team.questionScore
            //reset score for next question
            team.questionScore = 0
        }
    }

    //function to display teams in the scrollview
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun displayTeams() {
        binding.teamsContainer.removeAllViews()
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

            }

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


            //add points button using buttonContainer and functionality to add points
            val addButton = Button(this).apply {
                text = "+"
                isEnabled = team.buttonStates.addEnabled && !team.isLocked
                alpha = if (isEnabled) 1.0f else 0.5f
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(40)
                ).apply {
                    marginEnd = dpToPx(8)  // Margin between buttons
                }
                //when + button is pushed, update team score
                setOnClickListener {
                    updateTeamScore(index, 1)
                }
            }


            //subtract points button using buttonContainer and functionality to subtract points
            val subtractButton = Button(this).apply {
                text = "-"
                isEnabled = team.buttonStates.subtractEnabled && !team.isLocked
                alpha = if (isEnabled) 1.0f else 0.5f
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(40),  // Width in dp
                    dpToPx(40)   // Height in dp
                ).apply {
                    marginEnd = dpToPx(8)
                }
                    setOnClickListener {
                    updateTeamScore(index, -1)
                }
            }

            //add switch for each team
            val teamSwitch = Switch(this).apply {
                isChecked = team.isLocked
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(8)
                }
                setOnCheckedChangeListener { _, isChecked ->
                    team.isLocked = isChecked
                    addButton.isEnabled = !isChecked
                    subtractButton.isEnabled = !isChecked
                    addButton.alpha = if (isChecked) 0.5f else 1.0f
                    subtractButton.alpha = if (isChecked) 0.5f else 1.0f
                }
            }

            //place switch, +/- from left to right
            buttonContainer.addView(teamSwitch)
            buttonContainer.addView(subtractButton)
            buttonContainer.addView(addButton)

            teamLayout.addView(buttonContainer)
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