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
  val isUnoVulnerable: Boolean = false, // Vulnerable to being caught if 1 card left and didn't declare UNO
  val isConnected: Boolean = true
) {
  val cardCount: Int get() = hand.size
  val hasUno: Boolean get() = hand.size == 1
  val isHandEmpty: Boolean get() = hand.isEmpty()

  fun withAddedCards(newCards: List<UnoCard>): UnoPlayer {
    val newTotal = hand.size + newCards.size
    return copy(
      hand = hand + newCards,
      hasDeclaredUno = if (newTotal == 1) hasDeclaredUno else false,
      isUnoVulnerable = if (newTotal == 1) isUnoVulnerable else false
    )
  }

  fun withRemovedCard(cardId: String): UnoPlayer {
    val updatedHand = hand.toMutableList()
    val index = updatedHand.indexOfFirst { it.id == cardId }
    if (index >= 0) {
      updatedHand.removeAt(index)
    }
    val newCount = updatedHand.size
    return copy(
      hand = updatedHand,
      hasDeclaredUno = if (newCount == 1) hasDeclaredUno else false,
      isUnoVulnerable = if (newCount == 1) !hasDeclaredUno else false
    )
  }

  fun withUnoDeclared(declared: Boolean): UnoPlayer {
    return copy(hasDeclaredUno = declared, isUnoVulnerable = if (declared) false else isUnoVulnerable)
  }

  fun withUnoVulnerability(vulnerable: Boolean): UnoPlayer {
    return copy(isUnoVulnerable = vulnerable)
  }
}
