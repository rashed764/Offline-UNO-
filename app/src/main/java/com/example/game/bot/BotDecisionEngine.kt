package com.example.game.bot

import com.example.game.engine.PlayDirection
import com.example.game.engine.UnoGameState
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.UnoCard
import com.example.game.model.UnoPlayer
import com.example.game.rules.UnoRulesEngine

/**
 * Result of a bot decision step.
 */
sealed class BotAction {
  data class PlayCard(val card: UnoCard, val chosenWildColor: CardColor? = null) : BotAction()
  data object DrawCard : BotAction()
}

/**
 * Lightweight deterministic heuristic decision engine for offline bots.
 * Analyzes legal moves, opponent states (UNO alerts), hand composition, and dominant colors.
 */
object BotDecisionEngine {

  /**
   * Evaluates the best action for the specified bot given the authoritative game state.
   */
  fun evaluateTurn(
    bot: UnoPlayer,
    state: UnoGameState
  ): BotAction {
    val topDiscard = state.topDiscard ?: return BotAction.DrawCard
    val playableCards = UnoRulesEngine.getPlayableCards(
      hand = bot.hand,
      topDiscard = topDiscard,
      activeColor = state.activeColor
    )

    if (playableCards.isEmpty()) {
      return BotAction.DrawCard
    }

    // Inspect public opponent awareness (next player in turn order)
    val nextPlayerIndex = UnoRulesEngine.calculateNextTurnIndex(
      currentIndex = state.currentTurnIndex,
      totalPlayers = state.players.size,
      direction = state.direction.multiplier,
      steps = 1
    )
    val nextPlayer = state.players.getOrNull(nextPlayerIndex)
    val isNextPlayerThreat = nextPlayer != null && nextPlayer.cardCount <= 2

    // Score and rank all playable candidates
    val scoredCards = playableCards.map { card ->
      val score = calculateCardScore(card, bot.hand, isNextPlayerThreat)
      Pair(card, score)
    }

    val bestCard = scoredCards.maxByOrNull { it.second }?.first ?: playableCards.first()

    val chosenWildColor = if (bestCard.isWild) {
      chooseDominantColor(bot.hand, excludingCardId = bestCard.id)
    } else null

    return BotAction.PlayCard(bestCard, chosenWildColor)
  }

  /**
   * If a bot draws a card that is immediately playable, decides whether to play it.
   */
  fun shouldPlayDrawnCard(
    drawnCard: UnoCard,
    botHand: List<UnoCard>,
    state: UnoGameState
  ): Pair<Boolean, CardColor?> {
    val topDiscard = state.topDiscard ?: return Pair(false, null)
    val isPlayable = UnoRulesEngine.isCardPlayable(drawnCard, topDiscard, state.activeColor)
    if (!isPlayable) return Pair(false, null)

    val chosenWildColor = if (drawnCard.isWild) {
      chooseDominantColor(botHand, excludingCardId = drawnCard.id)
    } else null

    return Pair(true, chosenWildColor)
  }

  /**
   * Chooses the dominant non-wild color from the remaining hand.
   * Deterministic tie-breaker: RED > BLUE > GREEN > YELLOW.
   */
  fun chooseDominantColor(
    hand: List<UnoCard>,
    excludingCardId: String? = null
  ): CardColor {
    val remaining = if (excludingCardId != null) {
      hand.filter { it.id != excludingCardId }
    } else hand

    val counts = mapOf(
      CardColor.RED to remaining.count { it.color == CardColor.RED },
      CardColor.BLUE to remaining.count { it.color == CardColor.BLUE },
      CardColor.GREEN to remaining.count { it.color == CardColor.GREEN },
      CardColor.YELLOW to remaining.count { it.color == CardColor.YELLOW }
    )

    // Find color with maximum count
    val maxCount = counts.values.maxOrNull() ?: 0
    if (maxCount == 0) {
      // If no colored cards left, deterministic default RED
      return CardColor.RED
    }

    // Deterministic tie-breaking order
    val priority = listOf(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW)
    return priority.firstOrNull { counts[it] == maxCount } ?: CardColor.RED
  }

  /**
   * Heuristic scoring for candidate cards.
   */
  private fun calculateCardScore(
    card: UnoCard,
    hand: List<UnoCard>,
    isNextPlayerThreat: Boolean
  ): Int {
    var score = 0

    // Count how many cards in hand share this card's color
    val colorMatches = if (card.color != CardColor.WILD) {
      hand.count { it.color == card.color }
    } else 0

    when (card.value) {
      CardValue.WILD_DRAW_FOUR -> {
        // High priority if next opponent has UNO, otherwise save for emergency
        score = if (isNextPlayerThreat) 95 else 30
      }
      CardValue.DRAW_TWO -> {
        // Highly disruptive
        score = if (isNextPlayerThreat) 90 else 50 + colorMatches * 5
      }
      CardValue.SKIP -> {
        // Highly disruptive against next player
        score = if (isNextPlayerThreat) 85 else 45 + colorMatches * 5
      }
      CardValue.REVERSE -> {
        // Diverts turn away from threatening opponent
        score = if (isNextPlayerThreat) 80 else 40 + colorMatches * 5
      }
      CardValue.WILD -> {
        // Versatile, save unless hand is getting small or no good color cards
        score = if (hand.size <= 3) 70 else 25
      }
      else -> {
        // Normal number cards: prefer playing from dominant color to cycle hand
        score = 20 + colorMatches * 8 + card.value.ordinal
      }
    }

    return score
  }
}
