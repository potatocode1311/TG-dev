package com.example.trivia_game

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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
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
    private lateinit var binding : ActivityMainBinding
    private var roundNumber: Int = 1
    private val teams = mutableListOf<Team>()
    //data class for team that initializes it as a string and defaults score to 0
    @Parcelize
    data class Team(
        val name: String,
        var score: Int = 0
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //main activity initialization
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
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
            roundNumber = savedInstanceState.getInt("roundNumber", 1)

            //update display
            displayTeams()
            //update round number
            updateRoundDisplay()
        }
        //initialize the round number display
        binding.roundNumber.text = "Round: $roundNumber"
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //button initialization
        val button1 = findViewById<Button>(R.id.newGame)
        val button2 = findViewById<Button>(R.id.addTeam)
        val button3 = findViewById<Button>(R.id.nextRound)

        //show confirmation dialog boxes when buttons are pushed
        button1.setOnClickListener {
            showConfirmationDialog("New Game")
        }
        button2.setOnClickListener {
            showConfirmationDialog("Add Team")
        }
        button3.setOnClickListener {
            showConfirmationDialog("Next Round")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("teams", ArrayList(teams))
        outState.putBoolean("isInputVisible", binding.teamsContainer.visibility == View.VISIBLE)

        //save the current round
        outState.putInt("roundNumber", roundNumber)
    }

    private fun updateRoundDisplay() {
        //update round number to current view on save state
        binding.roundNumber.text = "Round $roundNumber"

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

                addTeam(teamName)
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

            "Next Round" -> {
                builder.setMessage("Are you sure you want to proceed to the next round?")
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

                "Next Round" -> {
                    //increment round number
                    roundNumber++
                    //update round container with new round number
                    binding.roundNumber.text = "Round: $roundNumber"

                }
                "New Game" -> {

                    //clear the teamsContainer
                    binding.teamsContainer.removeAllViews()
                    //reset round number
                    roundNumber = 1
                    teams.clear()
                    binding.roundNumber.text = "Round: $roundNumber"

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

    //function to display teams in the scrollview
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
                setBackgroundResource(R.drawable.team_card_background_unscored)
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
                text = "Score: ${team.score}"
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
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(40)
                ).apply {
                    marginEnd = dpToPx(8)  // Margin between buttons
                }
                //when + button is pushed, update team score
                setOnClickListener {
                    updateTeamScore(index, 1)
                    //TODO:change background color and update view when button is pushed
                }
            }
            buttonContainer.addView(addButton)

            //subtract points button using buttonContainer and functionality to subtract points
            val subtractButton = Button(this).apply {
                text = "-"
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(40),  // Width in dp
                    dpToPx(40)   // Height in dp
                )
                setOnClickListener {
                    updateTeamScore(index, -1)
                }
            }
            buttonContainer.addView(subtractButton)

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
        if (teamIndex in teams.indices) {
            teams[teamIndex].score += points
            displayTeams()
            Toast.makeText(
                this,
                "${teams[teamIndex].name}: ${if (points > 0) "+" else ""}$points points",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}