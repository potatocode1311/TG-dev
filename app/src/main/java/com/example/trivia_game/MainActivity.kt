package com.example.trivia_game

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.trivia_game.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var roundNumber = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //main activity initialization
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the round number display
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

    private fun showTeamNameInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Team Name")

        // Set up the input field
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Team Name"

        // Add some padding around the EditText
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

        // Set up the buttons
        builder.setPositiveButton("OK") { _, _ ->
            val teamName = input.text.toString()
            if (teamName.isNotBlank()) {
                addTeam(teamName)
                Toast.makeText(this, "Team '$teamName' added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Team name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()


        dialog.show()
        // Show keyboard automatically when dialog appears
        input.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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
            // User clicked Yes button
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
                //"Action 2" -> { /* Code for action 2 */ }
                //"Action 3" -> { /* Code for action 3 */ }

        builder.setNegativeButton("No") { dialog: DialogInterface, id: Int ->
            //user cancelled the dialog
            Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
    //data class for team that initializes it as a string and defaults score to 0
    data class Team(
        val name: String,
        var score: Int = 0
    )
    private val teams = mutableListOf<Team>()
    //add teams function that adds teams and updates ui
    private fun addTeam(name: String) {
        val newTeam = Team(name)
        teams.add(newTeam)
        displayTeams()
    }

    //function to display teams in the scrollview
    private fun displayTeams() {
        binding.teamsContainer.removeAllViews()

        // Layout for teams to be displayed
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

                // Sets unscored teams to a gray color
                setBackgroundResource(R.drawable.team_card_background_unscored)
            }

            // Team name and score in a vertical layout (left side)
            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Team name styling
            val nameView = TextView(this).apply {
                text = team.name
                textSize = 18f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
            }
            infoLayout.addView(nameView)

            // Score styling
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