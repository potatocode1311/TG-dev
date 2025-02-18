package com.example.trivia_game

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.trivia_game.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputLayout


class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val button1 = findViewById<Button>(R.id.newGame)
        val button2 = findViewById<Button>(R.id.addTeam)
        val button3 = findViewById<Button>(R.id.addPoints)
        val button4 = findViewById<Button>(R.id.subtractPoints)
        val button5 = findViewById<Button>(R.id.nextRound)


        button1.setOnClickListener {
            showConfirmationDialog("New Game") // Pass a message specific to button 1
        }
        button2.setOnClickListener {
            showConfirmationDialog("Add Team") // Pass a message specific to button 2
        }
        button3.setOnClickListener {
            showConfirmationDialog("Add Points") // Pass a message specific to button 3
        }
        button4.setOnClickListener {
            showConfirmationDialog("Subtract Points") // Pass a message specific to button 3
        }
        button5.setOnClickListener {
            showConfirmationDialog("Next Round") // Pass a message specific to button 3
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

            "Add Points" -> {
                builder.setMessage("Are you sure you want to add points to a team?")
            }

            "Subtract Points" -> {
                builder.setMessage("Are you sure you want to subtract points to a team?")
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
                    showTeamNameInputDialog()
                }
            }
        }
                //"Action 2" -> { /* Code for action 2 */ }
                //"Action 3" -> { /* Code for action 3 */ }

        builder.setNegativeButton("No") { dialog: DialogInterface, id: Int ->
            // User cancelled the dialog
            Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
    data class Team(
        val name: String,
        var score: Int = 0  // Starting score of 0
    )
    private val teams = mutableListOf<Team>()
    private fun addTeam(name: String) {
        val newTeam = Team(name)
        teams.add(newTeam)
        displayTeams()  // Update the UI
    }
    private fun displayTeams() {
        binding.teamsContainer.removeAllViews()  // Clear existing views

        teams.forEach { team ->
            // Create a horizontal layout for each team
            val teamLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)  // Add some spacing between teams
                }
            }
            // Add team name
            val nameView = TextView(this).apply {
                text = team.name
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f  // Take up available space
                )
                textSize = 18f
            }
            teamLayout.addView(nameView)
            // Add score
            val scoreView = TextView(this).apply {
                text = "Score: ${team.score}"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 16
                }
                textSize = 18f
            }
            teamLayout.addView(scoreView)

            binding.teamsContainer.addView(teamLayout)
        }

        }
    private fun updateTeamScore(teamIndex: Int, points: Int) {
        if (teamIndex in teams.indices) {
            teams[teamIndex].score += points
            displayTeams()  // Refresh the display
        }
    }
    /*private fun addNewTeam() {
        val teamName = binding.teamNameEditText.text.toString()
        if (teamName.isBlank()) {
            Toast.makeText(this, "Please enter a team name", Toast.LENGTH_SHORT).show()
            return
        }

        val newTeam = Team(teamName)
        teams.add(newTeam)

        // Create the team's view dynamically
        val teamLayout = LinearLayout(this)
        teamLayout.orientation = LinearLayout.HORIZONTAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        teamLayout.layoutParams = params

        val teamNameTextView = TextView(this)
        teamNameTextView.text = teamName
        teamNameTextView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) // Weight to take up space
        teamLayout.addView(teamNameTextView)

        val scoreTextView = TextView(this)
        scoreTextView.text = "0"
        scoreTextView.id = newTeam.id // Store the ID for easy access later
        scoreTextView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        teamLayout.addView(scoreTextView)

        val incrementButton = Button(this)
        incrementButton.text = "+"
        incrementButton.setOnClickListener {
            incrementScore(newTeam)
        }
        teamLayout.addView(incrementButton)

        binding.teamsContainer.addView(teamLayout) // Add to the main layout
        binding.teamNameEditText.text.clear() // Clear the EditText
    }*/

}