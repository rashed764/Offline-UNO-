package com.example.game.deck

import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.UnoCard
import java.util.UUID

/**
 * Standard 108-Card UNO Deck factory and generator.
 *
 * Exact breakdown:
 * - 4 Colors (RED, YELLOW, GREEN, BLUE)
 * - 1 Zero card per color (4 cards)
 * - 2 copies of numbers 1-9 per color (2 * 9 * 4 = 72 cards)
 * - 2 Skip cards per color (2 * 4 = 8 cards)
 * - 2 Reverse cards per color (2 * 4 = 8 cards)
 * - 2 Draw Two (+2) cards per color (2 * 4 = 8 cards)
 * - 4 Wild cards (4 cards)
 * - 4 Wild Draw Four (+4) cards (4 cards)
 * Total: 4 + 72 + 8 + 8 + 8 + 4 + 4 = 108 cards.
 */
object UnoDeckFactory {

  fun createStandard108Deck(): List<UnoCard> {
    val cards = ArrayList<UnoCard>(108)
    val standardColors = listOf(CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE)

    var cardCounter = 1

    for (color in standardColors) {
      // Exactly one 0 card
      cards.add(UnoCard(id = "card_${cardCounter++}", color = color, value = CardValue.ZERO))

      // Exactly two copies of numbers 1..9
      val numberValues = listOf(
        CardValue.ONE, CardValue.TWO, CardValue.THREE, CardValue.FOUR,
        CardValue.FIVE, CardValue.SIX, CardValue.SEVEN, CardValue.EIGHT, CardValue.NINE
      )
      for (number in numberValues) {
        cards.add(UnoCard(id = "card_${cardCounter++}", color = color, value = number))
        cards.add(UnoCard(id = "card_${cardCounter++}", color = color, value = number))
      }

      // Exactly two copies of action cards: Skip, Reverse, Draw Two
      val actionValues = listOf(CardValue.SKIP, CardValue.REVERSE, CardValue.DRAW_TWO)
      for (action in actionValues) {
        cards.add(UnoCard(id = "card_${cardCounter++}", color = color, value = action))
        cards.add(UnoCard(id = "card_${cardCounter++}", color = color, value = action))
      }
    }

    // Exactly 4 Wild cards
    repeat(4) {
      cards.add(UnoCard(id = "card_${cardCounter++}", color = CardColor.WILD, value = CardValue.WILD))
    }

    // Exactly 4 Wild Draw Four cards
    repeat(4) {
      cards.add(UnoCard(id = "card_${cardCounter++}", color = CardColor.WILD, value = CardValue.WILD_DRAW_FOUR))
    }

    return cards
  }
}
