package com.example.game.model

/**
 * Type of player in the match.
 */
enum class PlayerType {
  HUMAN,
  BOT,
  LAN_CLIENT
}

/**
 * Domain representation of a game participant.
 */
data class UnoPlayer(
  val id: String,
  val name: String,
  val type: PlayerType,
  val hand: List<UnoCard> = emptyList(),
  val hasDeclaredUno: Boolean = false,
  val isConnected: Boolean = true
) {
  val cardCount: Int get() = hand.size
  val hasUno: Boolean get() = hand.size == 1
  val isHandEmpty: Boolean get() = hand.isEmpty()

  fun withAddedCards(newCards: List<UnoCard>): UnoPlayer {
    return copy(hand = hand + newCards, hasDeclaredUno = if (hand.size + newCards.size == 1) hasDeclaredUno else false)
  }

  fun withRemovedCard(cardId: String): UnoPlayer {
    val updatedHand = hand.toMutableList()
    val index = updatedHand.indexOfFirst { it.id == cardId }
    if (index >= 0) {
      updatedHand.removeAt(index)
    }
    return copy(hand = updatedHand)
  }

  fun withUnoDeclared(declared: Boolean): UnoPlayer {
    return copy(hasDeclaredUno = declared)
  }
}
