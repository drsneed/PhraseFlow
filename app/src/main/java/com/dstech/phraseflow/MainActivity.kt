package com.dstech.phraseflow

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.dstech.phraseflow.databinding.ActivityMainBinding
import android.content.Intent
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    companion object {
        var currentGame: Game? = null
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        findViewById<Button>(R.id.playNowButton).setOnClickListener {
            currentGame = Game()
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        updateScoreDisplay()

    }

    private fun updateScoreDisplay() {
        //findViewById<TextView>(R.id.scoreTextView).text = "Score: ${Player.score}"
    }

    override fun onResume() {
        super.onResume()
        resetGameState()
    }

    private fun resetGameState() {
        currentGame = null
        Player.reset()
        updateScoreDisplay()
    }
}