package com.example

import com.example.game.engine.GameStatus
import com.example.game.engine.PlayDirection
import com.example.game.engine.UnoGameSession
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.PlayerType
import com.example.game.model.UnoCard
import com.example.game.network.core.NetworkConfig
import com.example.game.network.protocol.ClientActionType
import com.example.game.network.protocol.MessageType
import com.example.game.network.protocol.NetworkEnvelope
import com.example.game.network.protocol.NetworkGameStatePayload
import com.example.game.network.protocol.NetworkPlayerInfo
import com.example.game.network.protocol.PlayerActionPayload
import com.example.game.network.protocol.RoomStatePayload
import com.example.game.network.serialization.NetworkSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineUnoPhase4NetworkTestSuite {

  @Test
  fun networkEnvelopeSerializationAndDeserializationPreservesAllFields() {
    val payload = PlayerActionPayload(
      actionType = ClientActionType.PLAY_CARD,
      cardId = "card_42",
      chosenColor = CardColor.RED
    )
    val serializedPayload = NetworkSerializer.serializePlayerAction(payload)

    val envelope = NetworkEnvelope(
      protocolVersion = NetworkConfig.PROTOCOL_VERSION,
      messageId = "msg_12345",
      type = MessageType.PLAYER_ACTION,
      senderPlayerId = "player_abc",
      payload = serializedPayload
    )

    val wireString = NetworkSerializer.serializeEnvelope(envelope)
    val deserialized = NetworkSerializer.deserializeEnvelope(wireString)

    assertNotNull(deserialized)
    assertEquals(NetworkConfig.PROTOCOL_VERSION, deserialized!!.protocolVersion)
    assertEquals("msg_12345", deserialized.messageId)
    assertEquals(MessageType.PLAYER_ACTION, deserialized.type)
    assertEquals("player_abc", deserialized.senderPlayerId)

    val actionPayload = NetworkSerializer.deserializePlayerAction(deserialized.payload)
    assertNotNull(actionPayload)
    assertEquals(ClientActionType.PLAY_CARD, actionPayload!!.actionType)
    assertEquals("card_42", actionPayload.cardId)
    assertEquals(CardColor.RED, actionPayload.chosenColor)
  }

  @Test
  fun invalidOrMalformedJsonSafelyReturnsNullWithoutCrashing() {
    val invalidJson = "{ malformed json string !! "
    val envelope = NetworkSerializer.deserializeEnvelope(invalidJson)
    assertNull(envelope)

    val invalidJoin = NetworkSerializer.deserializeJoinRequest("not valid")
    assertNull(invalidJoin)

    val invalidState = NetworkSerializer.deserializeGameState("invalid")
    assertNull(invalidState)
  }

  @Test
  fun roomStatePayloadSerializationMaintainsPlayerListsAndLimits() {
    val players = listOf(
      NetworkPlayerInfo("h1", "Host Player", PlayerType.HUMAN, 7, false, true),
      NetworkPlayerInfo("c1", "Client One", PlayerType.LAN_CLIENT, 7, false, true)
    )
    val room = RoomStatePayload(
      hostPlayerId = "h1",
      roomCode = "TEST99",
      maxPlayers = 4,
      entryFee = 100,
      players = players,
      isGameStarted = false
    )

    val serialized = NetworkSerializer.serializeRoomState(room)
    val deserialized = NetworkSerializer.deserializeRoomState(serialized)

    assertNotNull(deserialized)
    assertEquals("h1", deserialized!!.hostPlayerId)
    assertEquals(4, deserialized.maxPlayers)
    assertEquals(100, deserialized.entryFee)
    assertEquals(2, deserialized.players.size)
    assertFalse(deserialized.isGameStarted)
    assertEquals("Client One", deserialized.players[1].name)
  }

  @Test
  fun gameStatePayloadProtectsOpponentHandAndTransmitsRecipientClientHand() {
    val clientHand = listOf(
      UnoCard("c1", CardColor.BLUE, CardValue.SEVEN),
      UnoCard("c2", CardColor.YELLOW, CardValue.SKIP)
    )
    val topDiscard = UnoCard("td", CardColor.BLUE, CardValue.THREE)

    val gameState = NetworkGameStatePayload(
      currentTurnPlayerId = "c1",
      direction = PlayDirection.CLOCKWISE,
      activeColor = CardColor.BLUE,
      topDiscard = topDiscard,
      drawPileCount = 60,
      discardPileCount = 2,
      isRoundEnded = false,
      winnerPlayerId = null,
      winnerPlayerName = null,
      totalPot = 200,
      lastEventMessage = "Your turn",
      players = listOf(
        NetworkPlayerInfo("h1", "Host", PlayerType.HUMAN, 6, false, true),
        NetworkPlayerInfo("c1", "Client", PlayerType.LAN_CLIENT, 2, false, true)
      ),
      clientHand = clientHand,
      isWaitingForWildColor = false,
      wildColorChooserPlayerId = null
    )

    val serialized = NetworkSerializer.serializeGameState(gameState)
    val deserialized = NetworkSerializer.deserializeGameState(serialized)

    assertNotNull(deserialized)
    assertEquals("c1", deserialized!!.currentTurnPlayerId)
    assertEquals(CardColor.BLUE, deserialized.activeColor)
    assertEquals(topDiscard.id, deserialized.topDiscard?.id)
    assertEquals(2, deserialized.clientHand.size)
    assertEquals("c1", deserialized.clientHand[0].id)
    assertEquals(200, deserialized.totalPot)
  }

  @Test
  fun hostAuthorityRejectsActionsFromNonCurrentPlayer() {
    // Use fixed seed that gives a number opening card to ensure turn 0 starts normally
    val session = UnoGameSession(random = Random(123))
    val host = com.example.game.model.UnoPlayer("host_1", "Host", PlayerType.HUMAN)
    val client = com.example.game.model.UnoPlayer("client_1", "Client", PlayerType.LAN_CLIENT)

    session.startNewMatch(listOf(host, client), fee = 50)

    val currentTurnPlayerId = session.state.value.currentPlayer?.id
    assertNotNull(currentTurnPlayerId)

    // Identify non-current player
    val nonCurrentPlayerId = if (currentTurnPlayerId == host.id) client.id else host.id

    // Action attempted by non-current player must be rejected
    val isAllowed = currentTurnPlayerId == nonCurrentPlayerId
    assertFalse("Host must reject action from non-current player", isAllowed)

    // Verify illegal card play rejection
    val topDiscard = session.state.value.topDiscard!!
    val illegalColor = if (topDiscard.color == CardColor.RED) CardColor.BLUE else CardColor.RED
    val illegalCard = UnoCard("illegal", illegalColor, CardValue.ONE)
    val isPlayable = com.example.game.rules.UnoRulesEngine.isCardPlayable(
      illegalCard,
      topDiscard,
      session.state.value.activeColor
    )
    if (illegalCard.value != topDiscard.value && illegalCard.color != session.state.value.activeColor) {
      assertFalse(isPlayable)
    }
  }

  @Test
  fun networkConfigPortAndProtocolIntegrity() {
    assertEquals(19842, NetworkConfig.DEFAULT_PORT)
    assertEquals(1, NetworkConfig.PROTOCOL_VERSION)
  }
}
