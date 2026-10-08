package com.example.game.deck

import com.example.game.model.UnoCard
import java.util.Collections
import kotlin.random.Random

/**
 * Manages the Draw Pile and Discard Pile.
 * Handles card draws, recycling of the discard pile when the draw pile runs out,
 * ensuring the active top discard card is never recycled.
 */
class CardPiles(
  initialCards: List<UnoCard> = emptyList(),
  private val random: Random = Random.Default
) {
  private val drawPile = ArrayDeque<UnoCard>()
  private val discardPile = ArrayDeque<UnoCard>()

  init {
    if (initialCards.isNotEmpty()) {
      val shuffled = initialCards.shuffled(random)
      drawPile.addAll(shuffled)
    }
  }

  val drawCount: Int get() = drawPile.size
  val discardCount: Int get() = discardPile.size

  val topDiscard: UnoCard? get() = discardPile.lastOrNull()

  /**
   * Resets with a full newly shuffled deck.
   */
  fun resetWithDeck(cards: List<UnoCard>) {
    drawPile.clear()
    discardPile.clear()
    drawPile.addAll(cards.shuffled(random))
  }

  /**
   * Pushes a card onto the discard pile.
   */
  fun discard(card: UnoCard) {
    discardPile.addLast(card)
  }

  /**
   * Draws a single card. If the draw pile is empty, recycles all discard cards
   * except the active top card into the draw pile.
   */
  fun drawCard(): UnoCard? {
    if (drawPile.isEmpty()) {
      recycleDiscardIntoDrawPile()
    }
    return if (drawPile.isNotEmpty()) drawPile.removeFirst() else null
  }

  /**
   * Draws multiple cards up to the requested count.
   */
  fun drawCards(count: Int): List<UnoCard> {
    val drawn = ArrayList<UnoCard>(count)
    repeat(count) {
      val card = drawCard()
      if (card != null) {
        drawn.add(card)
      }
    }
    return drawn
  }

  /**
   * Recycles the discard pile (excluding the top card) into the draw pile and shuffles it.
   */
  fun recycleDiscardIntoDrawPile(): Boolean {
    if (discardPile.size <= 1) return false // Nothing to recycle

    val top = discardPile.removeLast()
    val toRecycle = ArrayList(discardPile)
    discardPile.clear()
    discardPile.addLast(top) // Keep top card on discard pile

    toRecycle.shuffle(random)
    drawPile.addAll(toRecycle)
    return true
  }
}
