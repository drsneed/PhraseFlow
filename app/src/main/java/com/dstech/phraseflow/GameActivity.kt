package com.dstech.phraseflow
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.random.Random
import android.widget.Toast
import com.dstech.phraseflow.utils.ToastUtil

class GameActivity : AppCompatActivity() {
    private lateinit var currentGame: Game
    private lateinit var letterBoxes: List<EditText>
    private lateinit var wordList: List<String>
    private lateinit var secretWord: String
    private lateinit var guessContainer: LinearLayout
    private lateinit var guessRow: LinearLayout
    private var guessAttempts: Int = 0
    private lateinit var keyboardButtons: Map<Char, Button>
    private var currentLetterBoxIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        currentGame = MainActivity.currentGame ?: Game()
        letterBoxes = listOf(
            findViewById(R.id.letter1),
            findViewById(R.id.letter2),
            findViewById(R.id.letter3),
            findViewById(R.id.letter4),
            findViewById(R.id.letter5)
        )
        letterBoxes.forEach { editText ->
            editText.apply {
                isFocusable = false
                isFocusableInTouchMode = false
                isClickable = false
                isLongClickable = false
                inputType = InputType.TYPE_NULL
                keyListener = null
            }
        }
        guessContainer = findViewById(R.id.guessContainer)
        guessRow = findViewById(R.id.guessRow)
        //keyboard = findViewById(R.id.keyboard)

        setupKeyboard()
        loadWordList()
        selectSecretWord()

        findViewById<Button>(R.id.debugButton).setOnClickListener {
            showSecretWordDialog()
        }

        creditNewRoundBonus()

        // Update the UI to show current round
        updateRoundDisplay()
        updateScoreDisplay()
    }

    private fun creditNewRoundBonus() {
        // Give to all players
        Player.credit(ScoreCalculator.getNewRoundBonus())
    }

    private fun deductIncorrectGuessPenalty() {
        Player.deduct(ScoreCalculator.getIncorrectGuessPenalty())
    }

    private fun updateScoreDisplay() {
        findViewById<TextView>(R.id.scoreTextView).text = "Score: ${Player.score}"
    }

    private fun updateRoundDisplay() {
        // Add a TextView in your layout to display the current round
        findViewById<TextView>(R.id.roundTextView).text = "Round ${currentGame.currentRound}/${currentGame.maxRounds}"
    }


    private fun setupKeyboard() {
        findViewById<Button>(R.id.keyBackspace).setOnClickListener {onBackspaceKeyClick()}
        findViewById<Button>(R.id.keyReturn).setOnClickListener {onReturnKeyClick()}
        keyboardButtons = mapOf(
            'Q' to findViewById(R.id.keyQ),
            'W' to findViewById(R.id.keyW),
            'E' to findViewById(R.id.keyE),
            'R' to findViewById(R.id.keyR),
            'T' to findViewById(R.id.keyT),
            'Y' to findViewById(R.id.keyY),
            'U' to findViewById(R.id.keyU),
            'I' to findViewById(R.id.keyI),
            'O' to findViewById(R.id.keyO),
            'P' to findViewById(R.id.keyP),
            'A' to findViewById(R.id.keyA),
            'S' to findViewById(R.id.keyS),
            'D' to findViewById(R.id.keyD),
            'F' to findViewById(R.id.keyF),
            'G' to findViewById(R.id.keyG),
            'H' to findViewById(R.id.keyH),
            'J' to findViewById(R.id.keyJ),
            'K' to findViewById(R.id.keyK),
            'L' to findViewById(R.id.keyL),
            'Z' to findViewById(R.id.keyZ),
            'X' to findViewById(R.id.keyX),
            'C' to findViewById(R.id.keyC),
            'V' to findViewById(R.id.keyV),
            'B' to findViewById(R.id.keyB),
            'N' to findViewById(R.id.keyN),
            'M' to findViewById(R.id.keyM)
        )

        keyboardButtons.forEach { entry ->
            entry.value.setOnClickListener{ onKeyClick(entry.key) }
        }

    }

    private fun onBackspaceKeyClick() {
        currentLetterBoxIndex--
        if (currentLetterBoxIndex < 0) {
            currentLetterBoxIndex++
        }
        val currentBox = letterBoxes[currentLetterBoxIndex]
        currentBox.setText("")
        vibrate(40)
    }
    private fun onReturnKeyClick() {
        vibrate(40)
        checkWord()
    }

    // Helper function to convert dp to pixels
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }


    private fun onKeyClick(letter: Char) {
        if (currentLetterBoxIndex < letterBoxes.size) {
            val currentBox = letterBoxes[currentLetterBoxIndex]
            currentBox.setText(letter.toString())
            vibrate(40)
            currentLetterBoxIndex++
        }
    }

    private fun checkWord() {
        guessAttempts++
        val guess = collectGuess()
        Log.i("GUESS", guess)
        if (!isValidWord(guess)) {
            handleInvalidWord()
            return
        }
        animateGuessResult(guess)
    }

    private fun disableKeyboard() {
        keyboardButtons.values.forEach { it.isEnabled = false }
    }

    private fun enableKeyboard() {
        keyboardButtons.values.forEach { it.isEnabled = true }
    }

    private fun handleCorrectGuess() {
        // Show a congratulatory message
        ToastUtil.showCustomToast(this, "Correct! You guessed the word!")

        // Move to the next round
        currentGame.nextRound()

        if (currentGame.isOver) {
            // Game is over
            ToastUtil.showCustomToast(this, "Game Over!")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        } else {
            if(guessContainer.childCount > 0) {
                val intent = Intent(this, GameBoardActivity::class.java)
                startActivity(intent)
            }
            else {
                // Prepare for the next round
                resetForNextRound()
            }

        }
    }

    private fun animateGuessResult(guess: String) {
        val handler = Handler(Looper.getMainLooper())
        val animationDuration = 500L // Increased duration for each letter animation
        val delayBetweenLetters = 100L // Added delay between letter animations

        disableKeyboard()

        guess.forEachIndexed { index, guessedLetter ->
            handler.postDelayed({
                val letterBox = letterBoxes[index]
                val backgroundColor = when {
                    guessedLetter.lowercaseChar() == secretWord[index].lowercaseChar() ->
                        ContextCompat.getColor(this, android.R.color.holo_green_light)
                    secretWord.contains(guessedLetter, ignoreCase = true) ->
                        ContextCompat.getColor(this, android.R.color.holo_blue_light)
                    else ->
                        ContextCompat.getColor(this, android.R.color.darker_gray)
                }

                // Animate background color change
                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE, backgroundColor)
                colorAnimation.duration = animationDuration
                colorAnimation.addUpdateListener { animator ->
                    letterBox.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Animate letter flip
                letterBox.animate()
                    .rotationX(90f)
                    .setDuration(animationDuration / 2)
                    .withEndAction {
                        letterBox.animate()
                            .rotationX(0f)
                            .setDuration(animationDuration / 2)
                            .start()
                    }
                    .start()

            }, index * (animationDuration + delayBetweenLetters))
        }

        // After all animations, proceed with game logic
        handler.postDelayed({
            proceedAfterAnimation(guess)
        }, guess.length * (animationDuration + delayBetweenLetters) + 100)
    }

    private fun proceedAfterAnimation(guess: String) {
        enableKeyboard()

        if (guess.equals(secretWord, ignoreCase = true)) {
            handleCorrectGuess()
        } else {
            Log.i("RESULT", "Incorrect. Try again!")
            addIncorrectGuess(guess)
            clearLetterBoxes()
            deductIncorrectGuessPenalty()
            updateScoreDisplay()
        }

        // Reset letter box backgrounds
        resetLetterBoxBackgrounds()
    }

    private fun resetLetterBoxBackgrounds() {
        letterBoxes.forEach { letterBox ->
            letterBox.setBackgroundResource(R.drawable.square_border)
        }
    }

    private fun resetForNextRound() {
        // Clear the word list and incorrect guesses
        Player.wordList.clear()
        clearIncorrectGuesses()
        clearLetterBoxes()
        resetKeyboardState()

        // Select a new secret word for the next round
        selectSecretWord()

        // Reset guess attempts
        guessAttempts = 0

        // Give player bonus
        creditNewRoundBonus()

        // Update UI for the new round
        updateRoundDisplay()
        updateScoreDisplay()
    }

    override fun onResume() {
        super.onResume()
        if (currentGame.isOver) {
            // Game is over, return to MainActivity
            finish()
        } else {
            // Start a new round
            selectSecretWord()
            clearIncorrectGuesses()
            clearLetterBoxes()
            resetKeyboardState()
            updateRoundDisplay()
            updateScoreDisplay()
        }
    }

    private fun resetKeyboardState() {
        keyboardButtons.forEach { entry ->
            entry.value.setBackgroundResource(R.drawable.keyboard_button_drawable)
        }
    }

    private fun isValidWord(word: String): Boolean {
        return wordList.contains(word.lowercase())
    }

    private fun vibrate(ms: Long) {
        @Suppress("DEPRECATION")
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    private fun shakeLetterBoxes() {
        val shakeAnimationLeft = AnimationUtils.loadAnimation(this, R.anim.shake_animation_left)
        val shakeAnimationRight = AnimationUtils.loadAnimation(this, R.anim.shake_animation_right)
        guessRow.startAnimation(shakeAnimationLeft)
        Handler().postDelayed({
            guessRow.startAnimation(shakeAnimationRight)
        }, 100)
    }

    private fun handleInvalidWord() {
        vibrate(200)
        shakeLetterBoxes()

        // Show custom centered message
        ToastUtil.showCustomToast(this, "Not in word list")
    }


    private fun clearLetterBoxes() {
        letterBoxes.forEach { editText -> editText.setText("") }
        currentLetterBoxIndex = 0
    }

    private fun clearIncorrectGuesses() {
        guessContainer.removeAllViews()
    }

    private fun addIncorrectGuess(guess: String) {
        val guessLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        guess.forEachIndexed { index, char ->
            val secretContainsLetter = secretWord.contains(char, ignoreCase = true)
            if(!secretContainsLetter) {
                keyboardButtons[char]?.let { button ->
                    button.setBackgroundResource(R.drawable.keyboard_button_guessed)
                }
            }
            val charView = TextView(this).apply {
                text = char.toString()
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(4, 4, 4, 4)
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(48), // width in pixels
                    dpToPx(48)  // height in pixels
                ).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
                background = when {
                    char.lowercaseChar() == secretWord[index] -> getColoredBackground(android.R.color.holo_green_light)
                    secretContainsLetter -> getColoredBackground(android.R.color.holo_blue_light)
                    else -> getColoredBackground(android.R.color.darker_gray)
                }
                setTypeface(typeface, Typeface.BOLD) // Make the text bold
            }
            guessLayout.addView(charView)
        }

        // Add the guess layout after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            guessContainer.addView(guessLayout)
        }, 100)
        Player.wordList.add(guess)
    }

    private fun getColoredBackground(colorRes: Int): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(this@GameActivity, colorRes))
            setStroke(2, Color.BLACK)
        }
    }

    private fun loadWordList() {
        wordList = resources.openRawResource(R.raw.word_list)
            .bufferedReader()
            .useLines { lines -> lines.map { it.trim().lowercase() }.toList() }
        Log.i("WORD_LIST_COUNT", wordList.size.toString())
    }

    private fun selectSecretWord() {
        secretWord = wordList[Random.nextInt(wordList.size)]
        Log.i("SECRET_WORD", secretWord) // For debugging purposes
    }


    private fun showSecretWordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Debug: Secret Word")
            .setMessage("The secret word is: $secretWord")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun collectGuess(): String {
        return letterBoxes.joinToString(separator = "") { it.text.toString() }
    }
}
