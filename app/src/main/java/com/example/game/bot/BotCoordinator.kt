package com.example.game.bot

import com.example.game.engine.GameStatus
import com.example.game.engine.UnoGameSession
import com.example.game.model.PlayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Autonomous lifecycle-safe Bot Coordinator.
 * Listens to the authoritative UnoGameSession state flow and triggers
 * bot moves with realistic pacing (~700ms–1100ms) whenever a bot's turn is active.
 */
class BotCoordinator(
  private val session: UnoGameSession,
  private val scope: CoroutineScope,
  private val thinkingDelayMs: Long = 850L
) {
  private var loopJob: Job? = null
  private var isExecutingAction = false

  fun start() {
    stop() // Ensure no duplicate jobs
    loopJob = scope.launch {
      session.state.collect { state ->
        if (!isActive) return@collect

        // 1. If game ended or not in progress, do nothing
        if (state.status !is GameStatus.InProgress && state.status !is GameStatus.WaitingForWildColor) {
          return@collect
        }

        // 2. Handle Bot Wild Color Selection
        if (state.status is GameStatus.WaitingForWildColor) {
          val waiting = state.status as GameStatus.WaitingForWildColor
          if (waiting.player.type == PlayerType.BOT && !isExecutingAction) {
            isExecutingAction = true
            launch {
              delay(450L) // Short delay for color selection
              if (isActive && session.state.value.status is GameStatus.WaitingForWildColor) {
                val dominantColor = BotDecisionEngine.chooseDominantColor(waiting.player.hand)
                session.selectWildColor(dominantColor)
              }
              isExecutingAction = false
            }
          }
          return@collect
        }

        // 3. Handle Bot Normal Turn
        val current = state.currentPlayer
        if (current != null && current.type == PlayerType.BOT && !isExecutingAction) {
          isExecutingAction = true
          launch {
            // Human-like thinking pause
            delay(thinkingDelayMs)

            if (!isActive) {
              isExecutingAction = false
              return@launch
            }

            val currentState = session.state.value
            val activePlayer = currentState.currentPlayer

            if (activePlayer != null && activePlayer.id == current.id && currentState.status is GameStatus.InProgress) {
              when (val action = BotDecisionEngine.evaluateTurn(activePlayer, currentState)) {
                is BotAction.PlayCard -> {
                  session.playCard(activePlayer.id, action.card.id)

                  // If Wild card, resolve color choice if pending
                  if (action.chosenWildColor != null) {
                    delay(350L)
                    if (session.state.value.status is GameStatus.WaitingForWildColor) {
                      session.selectWildColor(action.chosenWildColor)
                    }
                  }
                }
                is BotAction.DrawCard -> {
                  session.drawCard(activePlayer.id)
                }
              }
            }
            isExecutingAction = false
          }
        }
      }
    }
  }

  fun stop() {
    loopJob?.cancel()
    loopJob = null
    isExecutingAction = false
  }
}
