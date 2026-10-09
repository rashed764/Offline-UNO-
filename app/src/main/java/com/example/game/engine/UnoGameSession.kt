package com.example.game.engine

import com.example.game.deck.CardPiles
import com.example.game.deck.UnoDeckFactory
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.PlayerType
import com.example.game.model.UnoCard
import com.example.game.model.UnoPlayer
import com.example.game.rules.UnoRulesEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Direction of play around the table.
 */
enum class PlayDirection(val multiplier: Int) {
  CLOCKWISE(1),
  COUNTER_CLOCKWISE(-1);

  fun toggle(): PlayDirection = if (this == CLOCKWISE) COUNTER_CLOCKWISE else CLOCKWISE
}

/**
 * Status of the UNO game match.
 */
sealed class GameStatus {
  data object NotStarted : GameStatus()
  data object InProgress : GameStatus()
  data class WaitingForWildColor(val cardPlayed: UnoCard, val isDrawFour: Boolean, val player: UnoPlayer) : GameStatus()
  data class RoundEnded(val winner: UnoPlayer, val totalPot: Int) : GameStatus()
}

/**
 * Immutable Game State snapshot suitable for Compose UI observation and LAN synchronization.
 */
data class UnoGameState(
  val players: List<UnoPlayer>,
  val currentTurnIndex: Int,
  val direction: PlayDirection,
  val activeColor: CardColor,
  val topDiscard: UnoCard?,
  val drawPileCount: Int,
  val discardPileCount: Int,
  val status: GameStatus,
  val lastEventMessage: String,
  val entryFee: Int = 50,
  val totalPot: Int = 100,
  val drawnCardPlayableId: String? = null // Non-null when player just drew a card that is legal to play
) {
  val currentPlayer: UnoPlayer? get() = players.getOrNull(currentTurnIndex)
  val localPlayer: UnoPlayer? get() = players.firstOrNull { it.type == PlayerType.HUMAN }
  val isLocalPlayerTurn: Boolean get() = currentPlayer?.type == PlayerType.HUMAN
  val isBotTurn: Boolean get() = currentPlayer?.type == PlayerType.BOT && status is GameStatus.InProgress
}

/**
 * Authoritative, decoupled UNO Game Session Controller.
 * Handles game initialization, legal moves, wild card resolution, card draws,
 * immediate play of drawn cards, UNO declaration, catch penalty, and turn transitions.
 */
class UnoGameSession(
  private val random: Random = Random.Default
) {
  private val piles = CardPiles(random = random)
  private var playersList = mutableListOf<UnoPlayer>()
  private var turnIndex = 0
  private var direction = PlayDirection.CLOCKWISE
  private var activeColor = CardColor.RED
  private var status: GameStatus = GameStatus.NotStarted
  private var lastMessage = "Waiting for game to begin"
  private var entryFee = 50
  private var pendingDrawnCardId: String? = null

  private val _state = MutableStateFlow(buildState())
  val state: StateFlow<UnoGameState> = _state.asStateFlow()

  fun startNewMatch(players: List<UnoPlayer>, fee: Int) {
    require(players.size in 2..4) { "UNO supports 2 to 4 players" }
    entryFee = fee
    playersList = players.map { it.copy(hand = emptyList(), hasDeclaredUno = false, isUnoVulnerable = false) }.toMutableList()
    direction = PlayDirection.CLOCKWISE
    turnIndex = 0
    pendingDrawnCardId = null

    val fullDeck = UnoDeckFactory.createStandard108Deck()
    piles.resetWithDeck(fullDeck)

    for (i in playersList.indices) {
      val dealtHand = piles.drawCards(7)
      playersList[i] = playersList[i].copy(hand = dealtHand)
    }

    var initialTop = piles.drawCard() ?: UnoCard("start_fallback", CardColor.RED, CardValue.ZERO)
    while (initialTop.value == CardValue.WILD_DRAW_FOUR) {
      piles.discard(initialTop)
      piles.recycleDiscardIntoDrawPile()
      initialTop = piles.drawCard() ?: UnoCard("start_fallback", CardColor.RED, CardValue.ZERO)
    }

    piles.discard(initialTop)

    activeColor = if (initialTop.color == CardColor.WILD) {
      CardColor.RED
    } else {
      initialTop.color
    }

    status = GameStatus.InProgress
    lastMessage = "Match started! First card is ${initialTop.color.name} ${initialTop.value.name}"

    when (initialTop.value) {
      CardValue.SKIP -> {
        lastMessage = "${playersList[turnIndex].name} is skipped on opening!"
        advanceTurn(steps = 1)
      }
      CardValue.REVERSE -> {
        if (playersList.size == 2) {
          lastMessage = "Reverse on 2 players acts like Skip!"
          advanceTurn(steps = 1)
        } else {
          direction = direction.toggle()
          lastMessage = "Play direction reversed on opening!"
        }
      }
      CardValue.DRAW_TWO -> {
        val target = playersList[turnIndex]
        val drawn = piles.drawCards(2)
        playersList[turnIndex] = target.withAddedCards(drawn)
        lastMessage = "${target.name} draws 2 and misses turn on opening!"
        advanceTurn(steps = 1)
      }
      else -> { /* Normal start */ }
    }

    publishState()
  }

  /**
   * Attempts to play a card from a player's hand.
   */
  fun playCard(playerId: String, cardId: String): Boolean {
    val current = playersList.getOrNull(turnIndex) ?: return false
    if (current.id != playerId) return false
    if (status !is GameStatus.InProgress) return false

    // If a card was drawn and waiting decision, player can only play that drawn card or pass
    if (pendingDrawnCardId != null && pendingDrawnCardId != cardId) {
      return false
    }

    val top = piles.topDiscard ?: return false
    val card = current.hand.find { it.id == cardId } ?: return false

    if (!UnoRulesEngine.isCardPlayable(card, top, activeColor)) {
      return false
    }

    pendingDrawnCardId = null

    // Remove from hand and add to discard
    playersList[turnIndex] = current.withRemovedCard(cardId)
    piles.discard(card)

    // Check if player won immediately
    if (playersList[turnIndex].isHandEmpty) {
      val winner = playersList[turnIndex]
      val totalPot = entryFee * playersList.size
      status = GameStatus.RoundEnded(winner, totalPot)
      lastMessage = "${winner.name} played all cards and won the match!"
      publishState()
      return true
    }

    // Check UNO status: Bot auto declares UNO; Human becomes vulnerable if didn't declare
    val newCardCount = playersList[turnIndex].cardCount
    if (newCardCount == 1) {
      if (current.type == PlayerType.BOT) {
        playersList[turnIndex] = playersList[turnIndex].withUnoDeclared(true)
        lastMessage = "${current.name} shouted UNO!"
      } else {
        if (playersList[turnIndex].hasDeclaredUno) {
          lastMessage = "${current.name} shouted UNO!"
        } else {
          playersList[turnIndex] = playersList[turnIndex].withUnoVulnerability(true)
          lastMessage = "${current.name} has ONE card left!"
        }
      }
    }

    // Handle card effect
    when (val effect = UnoRulesEngine.evaluateCardEffect(card)) {
      is UnoRulesEngine.CardEffect.Wild -> {
        status = GameStatus.WaitingForWildColor(card, isDrawFour = false, player = current)
        lastMessage = "${current.name} played Wild! Choose next color."
        publishState()
        return true
      }
      is UnoRulesEngine.CardEffect.WildDrawFour -> {
        status = GameStatus.WaitingForWildColor(card, isDrawFour = true, player = current)
        lastMessage = "${current.name} played Wild +4! Choose next color."
        publishState()
        return true
      }
      is UnoRulesEngine.CardEffect.SkipNext -> {
        activeColor = card.color
        val skippedIndex = UnoRulesEngine.calculateNextTurnIndex(turnIndex, playersList.size, direction.multiplier, 1)
        val skippedPlayer = playersList[skippedIndex]
        lastMessage = "${current.name} played Skip! ${skippedPlayer.name} was skipped."
        advanceTurn(steps = 2)
      }
      is UnoRulesEngine.CardEffect.ReverseDirection -> {
        activeColor = card.color
        if (playersList.size == 2) {
          lastMessage = "${current.name} played Reverse (Acts as Skip in 1v1)!"
          advanceTurn(steps = 2)
        } else {
          direction = direction.toggle()
          lastMessage = "${current.name} reversed the turn direction!"
          advanceTurn(steps = 1)
        }
      }
      is UnoRulesEngine.CardEffect.DrawTwo -> {
        activeColor = card.color
        val targetIndex = UnoRulesEngine.calculateNextTurnIndex(turnIndex, playersList.size, direction.multiplier, 1)
        val target = playersList[targetIndex]
        val drawn = piles.drawCards(2)
        playersList[targetIndex] = target.withAddedCards(drawn)
        lastMessage = "${current.name} played +2! ${target.name} drew 2 cards and forfeited turn."
        advanceTurn(steps = 2)
      }
      is UnoRulesEngine.CardEffect.None -> {
        activeColor = card.color
        lastMessage = "${current.name} played ${card.color.name} ${card.value.name}"
        advanceTurn(steps = 1)
      }
    }

    publishState()
    return true
  }

  /**
   * Current player draws a card from the draw pile according to official UNO rules:
   * - Draws 1 card
   * - If the drawn card is legally playable, the player REMAINS the current player and can play it immediately!
   * - If the drawn card is not playable, turn advances automatically.
   */
  fun drawCard(playerId: String): UnoCard? {
    val current = playersList.getOrNull(turnIndex) ?: return null
    if (current.id != playerId) return null
    if (status !is GameStatus.InProgress) return null

    val drawn = piles.drawCard() ?: return null
    playersList[turnIndex] = current.withAddedCards(listOf(drawn))

    val top = piles.topDiscard
    val isPlayable = top != null && UnoRulesEngine.isCardPlayable(drawn, top, activeColor)

    if (isPlayable) {
      // Drawn card can be played immediately! Hold turn for player decision.
      pendingDrawnCardId = drawn.id
      lastMessage = "${current.name} drew ${drawn.color.name} ${drawn.value.name} (Playable!)"
    } else {
      // Not playable: turn advances to next player
      pendingDrawnCardId = null
      lastMessage = "${current.name} drew a card."
      advanceTurn(steps = 1)
    }

    publishState()
    return drawn
  }

  /**
   * If a player drew a playable card but decides to keep it and pass.
   */
  fun passDrawnCard(playerId: String): Boolean {
    val current = playersList.getOrNull(turnIndex) ?: return false
    if (current.id != playerId) return false
    if (pendingDrawnCardId == null) return false

    pendingDrawnCardId = null
    lastMessage = "${current.name} kept the drawn card and passed."
    advanceTurn(steps = 1)
    publishState()
    return true
  }

  fun selectWildColor(selectedColor: CardColor): Boolean {
    val waiting = status as? GameStatus.WaitingForWildColor ?: return false
    require(selectedColor != CardColor.WILD)

    activeColor = selectedColor
    val current = playersList[turnIndex]

    if (waiting.isDrawFour) {
      val targetIndex = UnoRulesEngine.calculateNextTurnIndex(turnIndex, playersList.size, direction.multiplier, 1)
      val target = playersList[targetIndex]
      val drawn = piles.drawCards(4)
      playersList[targetIndex] = target.withAddedCards(drawn)
      lastMessage = "${current.name} picked ${selectedColor.displayName}. ${target.name} drew 4 cards and forfeited turn!"
      status = GameStatus.InProgress
      advanceTurn(steps = 2)
    } else {
      lastMessage = "${current.name} picked ${selectedColor.displayName} as the active color."
      status = GameStatus.InProgress
      advanceTurn(steps = 1)
    }

    publishState()
    return true
  }

  /**
   * Declares UNO for a player who has 1 or 2 cards.
   */
  fun declareUno(playerId: String): Boolean {
    val index = playersList.indexOfFirst { it.id == playerId }
    if (index >= 0) {
      val p = playersList[index]
      if (p.cardCount <= 2) {
        playersList[index] = p.withUnoDeclared(true)
        lastMessage = "${p.name} shouted UNO!"
        publishState()
        return true
      }
    }
    return false
  }

  /**
   * UNO Penalty Catch:
   * If a player has 1 card, did not declare UNO, and another player catches them,
   * the vulnerable player must draw 2 penalty cards. Applied exactly once.
   */
  fun catchUnoPenalty(catcherId: String, targetPlayerId: String): Boolean {
    val targetIndex = playersList.indexOfFirst { it.id == targetPlayerId }
    if (targetIndex < 0) return false
    val target = playersList[targetIndex]

    if (target.cardCount == 1 && target.isUnoVulnerable) {
      val penaltyCards = piles.drawCards(2)
      playersList[targetIndex] = target.withAddedCards(penaltyCards).withUnoVulnerability(false)
      val catcher = playersList.find { it.id == catcherId }
      lastMessage = "${catcher?.name ?: "Opponent"} caught ${target.name} not saying UNO! +2 Cards penalty!"
      publishState()
      return true
    }
    return false
  }

  fun updateLastMessage(msg: String) {
    lastMessage = msg
    publishState()
  }

  private fun advanceTurn(steps: Int) {
    turnIndex = UnoRulesEngine.calculateNextTurnIndex(
      currentIndex = turnIndex,
      totalPlayers = playersList.size,
      direction = direction.multiplier,
      steps = steps
    )
  }

  private fun buildState(): UnoGameState {
    return UnoGameState(
      players = playersList.toList(),
      currentTurnIndex = turnIndex,
      direction = direction,
      activeColor = activeColor,
      topDiscard = piles.topDiscard,
      drawPileCount = piles.drawCount,
      discardPileCount = piles.discardCount,
      status = status,
      lastEventMessage = lastMessage,
      entryFee = entryFee,
      totalPot = entryFee * playersList.size.coerceAtLeast(1),
      drawnCardPlayableId = pendingDrawnCardId
    )
  }

  private fun publishState() {
    _state.value = buildState()
  }
}
