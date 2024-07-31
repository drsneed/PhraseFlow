package com.dstech.phraseflow
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.GridLayout
import android.util.DisplayMetrics
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import com.dstech.phraseflow.utils.ToastUtil
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton

class GameBoardActivity : AppCompatActivity() {

    companion object {
        private const val GRID_SIZE = 10
        private val boardState = Array(GRID_SIZE) { Array<String?>(GRID_SIZE) { null } }
    }
    private lateinit var currentGame: Game
    private lateinit var gridLayout: GridLayout
    private lateinit var shelfLayout: LinearLayout
    private lateinit var wordsLayout: FlexboxLayout
    private lateinit var playButton: Button
    private lateinit var unusedLettersLayout: FlexboxLayout
    private lateinit var currentWord: String
    private lateinit var recallButton: Button
    private lateinit var shuffleButton: Button
    private var guessAttempts: Int = 0
    private val unusedLetters = mutableListOf<LetterTileView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_board)
        currentGame = MainActivity.currentGame ?: Game()
        guessAttempts = intent.getIntExtra("guessAttempts", 0)
        gridLayout = findViewById(R.id.gridLayout)
        shelfLayout = findViewById(R.id.shelfLayout)
        wordsLayout = findViewById(R.id.wordsLayout)
        playButton = findViewById(R.id.playButton)
        unusedLettersLayout = findViewById(R.id.unusedLettersLayout)
        setupGrid()
        currentWord = if (Player.wordList.any()) Player.wordList.removeFirst() else "ABCDE"
        setupShelf()
        playButton.setOnClickListener { onPlayButtonClicked() }
        recallButton = findViewById(R.id.recallButton)
        recallButton.setOnClickListener { recallTilesFromBoard() }
        shuffleButton = findViewById(R.id.shuffleButton)
        shuffleButton.setOnClickListener { onShuffleButtonClicked() }
        updateWordsLayout()
        updateButtonStates()
        loadDictionary()
        updateScoreDisplay()
    }

    private fun updateScoreDisplay() {
        findViewById<TextView>(R.id.scoreTextView).text = "Score: ${Player.score}"
    }

    private fun onShuffleButtonClicked() {
        val tiles = mutableListOf<LetterTileView>()

        // Remove all tiles from the shelf and add them to a list
        for (i in shelfLayout.childCount - 1 downTo 0) {
            val tile = shelfLayout.getChildAt(i) as? LetterTileView
            if (tile != null) {
                shelfLayout.removeView(tile)
                tiles.add(tile)
            }
        }

        // Shuffle the list of tiles
        tiles.shuffle()

        // Add the shuffled tiles back to the shelf
        for (tile in tiles) {
            shelfLayout.addView(tile)
        }

        // Animate the shuffle
        animateShuffleTiles(tiles)
    }

    private fun animateShuffleTiles(tiles: List<LetterTileView>) {
        val duration = 300L // Animation duration in milliseconds

        tiles.forEachIndexed { index, tile ->
            tile.animate()
                .translationY(-50f) // Move up
                .rotationY(360f) // Rotate around Y-axis
                .setDuration(duration)
                .setStartDelay(index * 50L) // Stagger the animations
                .withEndAction {
                    tile.animate()
                        .translationY(0f) // Move back down
                        .setDuration(duration / 2)
                        .start()
                }
                .start()
        }
    }

    private fun updateWordsLayout() {
        wordsLayout.removeAllViews()
        for (word in Player.wordList) {
            val wordButton = MaterialButton(this).apply {
                text = word
                setOnClickListener { onWordButtonClicked(word) }
                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(4, 4, 4, 4)
                }
            }
            wordsLayout.addView(wordButton)
        }
    }

    private fun onWordButtonClicked(word: String) {
        // Recall tiles from the board
        recallTilesFromBoard()

        // Swap the current word with the clicked word
        val clickedWordIndex = Player.wordList.indexOf(word)
        val clickedWord = Player.wordList.removeAt(clickedWordIndex)
        Player.wordList.add(currentWord)
        currentWord = clickedWord

        // Update the shelf with the new word
        setupShelf()

        // Update the words layout
        updateWordsLayout()

        // Update play button state
        updateButtonStates()
    }

    private fun recallTilesFromBoard() {
        for (i in 0 until gridLayout.childCount) {
            val cell = gridLayout.getChildAt(i) as? FrameLayout
            val tile = cell?.getChildAt(0) as? LetterTileView
            if (tile != null && tile.isShelfTile) {
                cell.removeView(tile)
                tile.layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.tile_width),
                    resources.getDimensionPixelSize(R.dimen.tile_height)
                )
                tile.setOnTouchListener(TileTouchListener())
                shelfLayout.addView(tile)
            }
        }

        updateButtonStates()
    }


    private fun updateRecallButtonState() {
        recallButton.isEnabled = shelfLayout.childCount < 5 && hasShelfTilesOnBoard()
    }

    private fun hasShelfTilesOnBoard(): Boolean {
        for (i in 0 until gridLayout.childCount) {
            val cell = gridLayout.getChildAt(i) as? FrameLayout
            val tile = cell?.getChildAt(0) as? LetterTileView
            if (tile != null && tile.isShelfTile) {
                return true
            }
        }
        return false
    }

    private fun setupGrid() {
        gridLayout.rowCount = GRID_SIZE
        gridLayout.columnCount = GRID_SIZE

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val cellSize = (screenWidth - 32 - 16) / GRID_SIZE

        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val cell = FrameLayout(this)
                val params = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    setMargins(2, 2, 2, 2)
                    rowSpec = GridLayout.spec(row)
                    columnSpec = GridLayout.spec(col)
                }
                cell.layoutParams = params
                cell.setBackgroundResource(R.drawable.grid_cell)
                cell.setOnDragListener(CellDragListener())
                gridLayout.addView(cell)

                // Restore tile if it exists in the boardState
                boardState[row][col]?.let { letter ->
                    val tile = LetterTileView(this).apply {
                        text = letter
                        isShelfTile = false
                    }
                    tile.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    cell.addView(tile)
                }
            }
        }
    }

    private fun clearBoardState() {
        // Clear the board state for the next round
        for (row in boardState.indices) {
            for (col in boardState[row].indices) {
                boardState[row][col] = null
            }
        }
    }

    private fun claimBoardTiles() {
        for (i in 0 until gridLayout.childCount) {
            val cell = gridLayout.getChildAt(i) as? FrameLayout
            val tile = cell?.getChildAt(0) as? LetterTileView
            if (tile != null) {
                tile.isShelfTile = false;
                // Save the board state
                val row = i / GRID_SIZE
                val col = i % GRID_SIZE
                boardState[row][col] = tile.text.toString()
            }
        }
    }

    private fun setupShelf() {
        shelfLayout.removeAllViews() // Clear existing tiles
        unusedLetters.clear()
        for (letter in currentWord) {
            val tile = LetterTileView(this).apply {
                text = letter.toString()
                isShelfTile = true
                setOnTouchListener(TileTouchListener())
            }
            shelfLayout.addView(tile)
        }
        shelfLayout.setOnDragListener(ShelfDragListener())
    }

    private lateinit var dictionary: Set<String>

    private fun loadDictionary() {
        dictionary = resources.openRawResource(R.raw.us_english_dict)
            .bufferedReader().useLines { lines ->
                lines.map { it.uppercase() }.toSet()
            }
    }

    private fun validateBoard(): Boolean {
        val horizontalWords = mutableSetOf<String>()
        val verticalWords = mutableSetOf<String>()
        val allWords = mutableSetOf<String>()

        // Check horizontal words
        for (row in 0 until GRID_SIZE) {
            var word = StringBuilder()
            for (col in 0 until GRID_SIZE) {
                val cell = gridLayout.getChildAt(row * GRID_SIZE + col) as? FrameLayout
                val letter = (cell?.getChildAt(0) as? TextView)?.text?.toString() ?: ""
                if (letter.isNotEmpty()) {
                    word.append(letter)
                } else if (word.isNotEmpty()) {
                    val currentWord = word.toString()
                    if (currentWord.length > 1) {
                        horizontalWords.add(currentWord)
                    }
                    word = StringBuilder()
                }
            }
            if (word.isNotEmpty()) {
                val currentWord = word.toString()
                if (currentWord.length > 1) {
                    horizontalWords.add(currentWord)
                }
            }
        }

        // Check vertical words
        for (col in 0 until GRID_SIZE) {
            var word = StringBuilder()
            for (row in 0 until GRID_SIZE) {
                val cell = gridLayout.getChildAt(row * GRID_SIZE + col) as? FrameLayout
                val letter = (cell?.getChildAt(0) as? TextView)?.text?.toString() ?: ""
                if (letter.isNotEmpty()) {
                    word.append(letter)
                } else if (word.isNotEmpty()) {
                    val currentWord = word.toString()
                    if (currentWord.length > 1) {
                        verticalWords.add(currentWord)
                    }
                    word = StringBuilder()
                }
            }
            if (word.isNotEmpty()) {
                val currentWord = word.toString()
                if (currentWord.length > 1) {
                    verticalWords.add(currentWord)
                }
            }
        }

        // Combine horizontal and vertical words
        allWords.addAll(horizontalWords)
        allWords.addAll(verticalWords)

        // Collect invalid words
        val invalidWords = allWords.filter { it !in dictionary }
        var isValid = invalidWords.isEmpty()
        val invalidWordsCount = invalidWords.count()

        ToastUtil.showCustomToast(this, "isValid = $isValid, invalid words: $invalidWordsCount")
        if(isValid) {
            val isSingleTileIsl = isSingleTileIsland()
            ToastUtil.showCustomToast(this, "isSingleTileIsland = $isSingleTileIsl")
            isValid = isSingleTileIsl
        }
        return isValid
    }

    private fun isSingleTileIsland(): Boolean {
        val visited = mutableSetOf<Pair<Int, Int>>()
        var foundTile = false

        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val cell = gridLayout.getChildAt(row * GRID_SIZE + col) as? FrameLayout
                val tile = cell?.getChildAt(0) as? LetterTileView

                if (tile != null && !visited.contains(Pair(row, col))) {
                    if (foundTile) {
                        return false // Found more than one island
                    }
                    foundTile = true
                    bfs(row, col, visited)
                }
            }
        }
        return foundTile // Return true if exactly one island was found
    }

    private fun bfs(startRow: Int, startCol: Int, visited: MutableSet<Pair<Int, Int>>) {
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startRow, startCol))
        visited.add(Pair(startRow, startCol))

        val directions = listOf(
            Pair(0, 1),  // Right
            Pair(1, 0),  // Down
            Pair(0, -1), // Left
            Pair(-1, 0)  // Up
        )

        while (queue.isNotEmpty()) {
            val (row, col) = queue.removeFirst()

            for ((dRow, dCol) in directions) {
                val newRow = row + dRow
                val newCol = col + dCol

                if (newRow in 0 until GRID_SIZE && newCol in 0 until GRID_SIZE) {
                    val cell = gridLayout.getChildAt(newRow * GRID_SIZE + newCol) as? FrameLayout
                    val tile = cell?.getChildAt(0) as? LetterTileView

                    if (tile != null && !visited.contains(Pair(newRow, newCol))) {
                        visited.add(Pair(newRow, newCol))
                        queue.add(Pair(newRow, newCol))
                    }
                }
            }
        }
    }


    private fun onPlayButtonClicked() {
        if (validateBoard()) {
            // Convert all tiles on the board to board tiles
            val unusedLetters = mutableListOf<LetterTileView>()

            for (i in shelfLayout.childCount - 1 downTo 0) {
                val tile = shelfLayout.getChildAt(i) as? LetterTileView
                if (tile != null) {
                    // Move unused tiles to a list
                    unusedLetters.add(tile)
                    shelfLayout.removeView(tile)
                }
            }

            for (tile in unusedLetters) {
                // Inflate the layout and get the TextView
                val unusedTileView = layoutInflater.inflate(R.layout.unused_letter_tile, unusedLettersLayout, false) as TextView
                unusedTileView.text = tile.text
                unusedLettersLayout.addView(unusedTileView)
            }

            claimBoardTiles()
            updateWordsLayout()
            updateButtonStates()

            // Load next word if available
            if (Player.wordList.isNotEmpty()) {
                currentWord = Player.wordList.removeFirst()
                setupShelf()
                updateWordsLayout()
            } else {
                // Calculate and score this round
                val unusedLettersPenalty = ScoreCalculator.getUnusedLettersPenalty(unusedLettersLayout.childCount)
                Player.deduct(unusedLettersPenalty)
                updateScoreDisplay()

                currentGame.nextRound()
                if(currentGame.isOver) {
                    clearBoardState()
                    ToastUtil.showCustomToast(this, "Game Over!")
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } else {
                    Player.credit(ScoreCalculator.getNewRoundBonus())
                }
                finish()
            }
        } else {
            ToastUtil.showCustomToast(this, "Invalid board layout!")
        }
    }

    private fun updatePlayButtonState() {
        val tileCount = shelfLayout.childCount
        playButton.isEnabled = tileCount < 5

    }

    private fun updateButtonStates() {
        updatePlayButtonState()
        updateRecallButtonState()
        updateShuffleButtonState()
    }

    private fun updateShuffleButtonState() {
        val tileCount = shelfLayout.childCount
        shuffleButton.isEnabled = tileCount > 0
    }


    private inner class TileTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_DOWN && view is LetterTileView && view.isShelfTile) {
                val clipData = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(view)
                view.startDragAndDrop(clipData, shadowBuilder, view, 0)
                view.visibility = View.INVISIBLE
                return true
            }
            return false
        }
    }

    private inner class ShelfDragListener : View.OnDragListener {
        override fun onDrag(view: View, dragEvent: DragEvent): Boolean {
            when (dragEvent.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d("ShelfDragListener", "Drag started")
                    return true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.setBackgroundColor(Color.LTGRAY)
                    return true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    view.setBackgroundColor(Color.TRANSPARENT)
                    return true
                }
                DragEvent.ACTION_DROP -> {
                    view.setBackgroundColor(Color.TRANSPARENT)
                    Log.d("ShelfDragListener", "Drop action")
                    val droppedView = dragEvent.localState as? LetterTileView ?: return false
                    val target = view as? LinearLayout ?: return false
                    if (!droppedView.isShelfTile) return false // Only accept shelf tiles

                    // Get the coordinates of the drop event
                    val x = dragEvent.x
                    val y = dragEvent.y

                    // Determine the index of the view where the tile should be inserted
                    val dropIndex = getDropIndex(target, x, y)

                    // Remove the view from its current parent
                    val sourceParent = droppedView.parent as? ViewGroup
                    sourceParent?.removeView(droppedView)

                    // Apply the style from letter_tile.xml to the dropped view
                    droppedView.setBackgroundResource(R.drawable.shelf_tile_background) // Set background
                    droppedView.layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.tile_width),
                        resources.getDimensionPixelSize(R.dimen.tile_height)
                    )
                    droppedView.textSize = 28f
                    droppedView.visibility = View.VISIBLE
                    droppedView.setOnTouchListener(TileTouchListener())
                    // Insert the view at the dropIndex in the target LinearLayout
                    if (dropIndex >= 0) {
                        target.addView(droppedView, dropIndex)
                    } else {
                        target.addView(droppedView) // Add to end if index is invalid
                    }

                    updateButtonStates()
                    return true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("ShelfDragListener", "Drag ended")
                    if (!dragEvent.result) {
                        val view = dragEvent.localState as? View
                        view?.visibility = View.VISIBLE
                    }
                    return true
                }
            }
            return false
        }

        // Helper function to determine the index at which the view should be inserted
        private fun getDropIndex(container: LinearLayout, x: Float, y: Float): Int {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                val rect = android.graphics.Rect()
                child.getHitRect(rect)
                if (x < rect.right && y < rect.bottom) {
                    return i
                }
            }
            return -1 // If no suitable position is found, return -1 to append the view at the end
        }
    }

    private inner class CellDragListener : View.OnDragListener {
        override fun onDrag(view: View, dragEvent: DragEvent): Boolean {
            when (dragEvent.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    return true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    return true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    return true
                }
                DragEvent.ACTION_DROP -> {
                    Log.d("CellDragListener", "Drop action")
                    val cell = view as? FrameLayout ?: return false
                    val droppedView = dragEvent.localState as? LetterTileView ?: return false

                    if (cell.childCount > 0) return false

                    val sourceParent = droppedView.parent as? ViewGroup
                    sourceParent?.removeView(droppedView)

                    droppedView.isShelfTile = true // Keep as shelf tile
                    cell.addView(droppedView)
                    droppedView.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    droppedView.visibility = View.VISIBLE
                    updateButtonStates()
                    return true
                }
                // ... other cases
            }
            return false
        }
    }
}

class LetterTileView(context: Context, attrs: AttributeSet? = null) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    var isShelfTile: Boolean = true
        set(value) {
            field = value
            updateAppearance()
        }

    private fun updateAppearance() {
        if (isShelfTile) {
            setBackgroundResource(R.drawable.shelf_tile_background)
            isLongClickable = true
        } else {
            setBackgroundResource(R.drawable.board_tile_background)
            isLongClickable = false
        }
    }

    init {
        layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.tile_width),
            resources.getDimensionPixelSize(R.dimen.tile_height)
        )
        textSize = 28f
        gravity = Gravity.CENTER
        updateAppearance()
    }
}
