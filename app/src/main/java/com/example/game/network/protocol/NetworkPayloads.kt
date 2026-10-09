package com.example.game.network.protocol

import com.example.game.engine.GameStatus
import com.example.game.engine.PlayDirection
import com.example.game.engine.UnoGameState
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.PlayerType
import com.example.game.model.UnoCard
import com.example.game.model.UnoPlayer

/**
 * Client-submitted gameplay action types.
 */
enum class ClientActionType {
  PLAY_CARD,
  DRAW_CARD,
  PASS_DRAWN_TURN, // When player draws a card but chooses not to play it
  CHOOSE_WILD_COLOR,
  DECLARE_UNO,
  CATCH_UNO // Catch another player who reached 1 card without declaring UNO
}

/**
 * Payload for PLAYER_ACTION message.
 */
data class PlayerActionPayload(
  val actionType: ClientActionType,
  val cardId: String? = null,
  val chosenColor: CardColor? = null,
  val targetPlayerId: String? = null
)

/**
 * Payload for PLAYER_JOIN_REQUEST message.
 */
data class PlayerJoinRequestPayload(
  val playerName: String,
  val requestedPlayerId: String,
  val roomCode: String = "",
  val password: String = ""
)

/**
 * Payload for PLAYER_JOIN_ACCEPTED message.
 */
data class PlayerJoinAcceptedPayload(
  val assignedPlayerId: String,
  val roomName: String,
  val roomCode: String,
  val maxPlayers: Int,
  val entryFee: Int
)

/**
 * Payload for PLAYER_JOIN_REJECTED message.
 */
data class PlayerJoinRejectedPayload(
  val reason: String
)

/**
 * Summarized public player representation for LAN clients.
 * Protects opponent card identity (hides opponent hand cards).
 */
data class NetworkPlayerInfo(
  val id: String,
  val name: String,
  val type: PlayerType,
  val cardCount: Int,
  val hasDeclaredUno: Boolean,
  val isUnoVulnerable: Boolean = false,
  val isConnected: Boolean = true,
  val isReady: Boolean = false
)

/**
 * Payload for ROOM_STATE_UPDATE message.
 */
data class RoomStatePayload(
  val hostPlayerId: String,
  val roomCode: String,
  val maxPlayers: Int,
  val entryFee: Int,
  val players: List<NetworkPlayerInfo>,
  val isGameStarted: Boolean
)

/**
 * Network-safe authoritative snapshot sent to clients.
 */
data class NetworkGameStatePayload(
  val currentTurnPlayerId: String?,
  val direction: PlayDirection,
  val activeColor: CardColor,
  val topDiscard: UnoCard?,
  val drawPileCount: Int,
  val discardPileCount: Int,
  val isRoundEnded: Boolean,
  val winnerPlayerId: String?,
  val winnerPlayerName: String?,
  val totalPot: Int,
  val lastEventMessage: String,
  val players: List<NetworkPlayerInfo>,
  val clientHand: List<UnoCard>,
  val isWaitingForWildColor: Boolean,
  val wildColorChooserPlayerId: String?,
  val drawnCardPlayableId: String? = null // When drawn card is playable during same turn
)
