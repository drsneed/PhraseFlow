package com.dstech.phraseflow

class Game {
    var currentRound: Int = 1
    val maxRounds: Int = 4
    var isOver: Boolean = false

    fun nextRound() {
        if (currentRound < maxRounds) {
            currentRound++
        } else {
            isOver = true
        }
    }
}