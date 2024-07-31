package com.dstech.phraseflow

object Player {
    var name: String = ""
    var wordList: ArrayDeque<String> = ArrayDeque()
    var score: Int = 0

    fun reset() {
        name = ""
        wordList.clear()
        score = 0
    }

    fun deduct(points: Int) {
        score -= points
    }

    fun credit(points: Int) {
        score += points
    }
}