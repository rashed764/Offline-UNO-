package com.example.game.rules

import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.UnoCard

/**
 * Pure Domain Rules Engine for standard UNO card play.
 * Decoupled from UI and networking.
 */
object UnoRulesEngine {

  /**
   * Checks if a card is legally playable on top of the active discard state.
   *
   * @param card The candidate card to play
   * @param topDiscard The current card on top of the discard pile
   * @param activeColor The current active color (which may differ from topDiscard.color after a Wild)
   */
  fun isCardPlayable(
    card: UnoCard,
    topDiscard: UnoCard,
    activeColor: CardColor
  ): Boolean {
    // 1. Wild cards (WILD and WILD DRAW FOUR) can always be played
    if (card.isWild) {
      return true
    }

    // 2. Playable if it matches the current active color
    if (card.color == activeColor) {
      return true
    }

    // 3. Playable if it matches the face value/symbol of the top discard card
    // (Note: only valid if top discard wasn't a wild card, or if it matches the value)
    if (card.value == topDiscard.value) {
      return true
    }

    return false
  }

  /**
   * Returns list of cards in hand that can be legally played.
   */
  fun getPlayableCards(
    hand: List<UnoCard>,
    topDiscard: UnoCard,
    activeColor: CardColor
  ): List<UnoCard> {
    return hand.filter { isCardPlayable(it, topDiscard, activeColor) }
  }

  /**
   * Determines the turn step effect of playing a specific card.
   */
  sealed class CardEffect {
    data object None : CardEffect()
    data object SkipNext : CardEffect()
    data object ReverseDirection : CardEffect()
    data class DrawTwo(val targetPlayerDrawCount: Int = 2) : CardEffect()
    data class Wild(val requiresColorSelection: Boolean = true) : CardEffect()
    data class WildDrawFour(val requiresColorSelection: Boolean = true, val targetPlayerDrawCount: Int = 4) : CardEffect()
  }

  fun evaluateCardEffect(card: UnoCard): CardEffect {
    return when (card.value) {
      CardValue.SKIP -> CardEffect.SkipNext
      CardValue.REVERSE -> CardEffect.ReverseDirection
      CardValue.DRAW_TWO -> CardEffect.DrawTwo(2)
      CardValue.WILD -> CardEffect.Wild(true)
      CardValue.WILD_DRAW_FOUR -> CardEffect.WildDrawFour(true, 4)
      else -> CardEffect.None
    }
  }

  /**
   * Calculates the next turn index given the current index, total player count, and direction.
   * Direction: +1 for clockwise, -1 for counter-clockwise.
   * Steps: 1 for standard turn transition, 2 for Skip / 2-player Reverse.
   */
  fun calculateNextTurnIndex(
    currentIndex: Int,
    totalPlayers: Int,
    direction: Int,
    steps: Int = 1
  ): Int {
    require(totalPlayers > 0)
    var next = (currentIndex + (direction * steps)) % totalPlayers
    if (next < 0) {
      next += totalPlayers
    }
    return next
  }
}
