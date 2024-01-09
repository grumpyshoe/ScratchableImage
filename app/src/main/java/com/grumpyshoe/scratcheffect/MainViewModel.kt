package com.grumpyshoe.scratcheffect

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GameState {
    object RUNNING : GameState()
    object WIN : GameState()
    object LOOSE : GameState()
}

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow<GameState>(GameState.RUNNING)
    val state = _state.asStateFlow()

    private val _answerList = MutableStateFlow<List<Boolean>>(emptyList())
    val answerList = _answerList.asStateFlow()

    private var successCounter = 0
    private var scratchCounter = 0
    private var scratchFieldState = MutableList(9) { false }

    init {

        _answerList.tryEmit(generateAnswers())

    }

    private fun generateAnswers(): List<Boolean> {
        val answers = MutableList(9) { false }.apply {
            val updatedIndexes = MutableList(9) { false }
            while (updatedIndexes.filter { it }.size < 3) {
                (0..8).random().let { successIndex ->
                    this[successIndex] = true
                    updatedIndexes[successIndex] = true
                }
            }
        }
        Log.d("AnswerList", "rrr answers:$answers")
        return answers
    }

    fun onScratch(index: Int, success: Boolean) {

        if (scratchFieldState[index]) return

        scratchCounter++
        scratchFieldState[index] = true

        if (success) {
            successCounter++
        }

        if (scratchCounter >= 3 && successCounter == 3) {
            _state.tryEmit(GameState.WIN)
        } else if (scratchCounter >= 3 && successCounter < 3) {
            _state.tryEmit(GameState.LOOSE)
        }
    }

    fun reset() {
        viewModelScope.launch {
            scratchFieldState = MutableList(9) { false }
            _answerList.tryEmit(generateAnswers())
            scratchCounter = 0
            successCounter = 0
            //delay(1000L)
            _state.tryEmit(GameState.RUNNING)
        }
    }

}
