package com.dstech.phraseflow
object ScoreCalculator {
    private const val NEW_ROUND_BONUS = 100
    private const val UNUSED_LETTER_PENALTY = 1
    private const val INCORRECT_GUESS_PENALTY = 10
    fun getUnusedLettersPenalty(unusedLettersCount: Int): Int {
        return unusedLettersCount * UNUSED_LETTER_PENALTY
    }
    fun getIncorrectGuessPenalty(): Int {
        return INCORRECT_GUESS_PENALTY
    }
    fun getNewRoundBonus(): Int {
        return NEW_ROUND_BONUS
    }
}