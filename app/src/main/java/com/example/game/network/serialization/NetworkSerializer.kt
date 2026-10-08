package com.example.game.network.serialization

import com.example.game.engine.PlayDirection
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.PlayerType
import com.example.game.model.UnoCard
import com.example.game.network.protocol.ClientActionType
import com.example.game.network.protocol.MessageType
import com.example.game.network.protocol.NetworkEnvelope
import com.example.game.network.protocol.NetworkGameStatePayload
import com.example.game.network.protocol.NetworkPlayerInfo
import com.example.game.network.protocol.PlayerActionPayload
import com.example.game.network.protocol.PlayerJoinAcceptedPayload
import com.example.game.network.protocol.PlayerJoinRejectedPayload
import com.example.game.network.protocol.PlayerJoinRequestPayload
import com.example.game.network.protocol.RoomStatePayload
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust, zero-overhead JSON serializer for the Offline UNO! LAN protocol.
 * Uses Android platform org.json to eliminate third-party reflection issues.
 */
object NetworkSerializer {

  fun serializeEnvelope(envelope: NetworkEnvelope): String {
    val obj = JSONObject()
    obj.put("v", envelope.protocolVersion)
    obj.put("id", envelope.messageId)
    obj.put("t", envelope.type.name)
    obj.put("s", envelope.senderPlayerId)
    obj.put("p", envelope.payload)
    return obj.toString()
  }

  fun deserializeEnvelope(jsonStr: String): NetworkEnvelope? {
    return try {
      val obj = JSONObject(jsonStr)
      NetworkEnvelope(
        protocolVersion = obj.getInt("v"),
        messageId = obj.getString("id"),
        type = MessageType.valueOf(obj.getString("t")),
        senderPlayerId = obj.getString("s"),
        payload = obj.getString("p")
      )
    } catch (_: Exception) {
      null
    }
  }

  // --- Join Request Serialization ---
  fun serializeJoinRequest(payload: PlayerJoinRequestPayload): String {
    val obj = JSONObject()
    obj.put("name", payload.playerName)
    obj.put("id", payload.requestedPlayerId)
    obj.put("room", payload.roomCode)
    obj.put("pwd", payload.password)
    return obj.toString()
  }

  fun deserializeJoinRequest(jsonStr: String): PlayerJoinRequestPayload? {
    return try {
      val obj = JSONObject(jsonStr)
      PlayerJoinRequestPayload(
        playerName = obj.getString("name"),
        requestedPlayerId = obj.getString("id"),
        roomCode = if (obj.has("room")) obj.getString("room") else "",
        password = if (obj.has("pwd")) obj.getString("pwd") else ""
      )
    } catch (_: Exception) {
      null
    }
  }

  // --- Join Accepted Serialization ---
  fun serializeJoinAccepted(payload: PlayerJoinAcceptedPayload): String {
    val obj = JSONObject()
    obj.put("assignedId", payload.assignedPlayerId)
    obj.put("roomName", payload.roomName)
    obj.put("roomCode", payload.roomCode)
    obj.put("maxPlayers", payload.maxPlayers)
    obj.put("entryFee", payload.entryFee)
    return obj.toString()
  }

  fun deserializeJoinAccepted(jsonStr: String): PlayerJoinAcceptedPayload? {
    return try {
      val obj = JSONObject(jsonStr)
      PlayerJoinAcceptedPayload(
        assignedPlayerId = obj.getString("assignedId"),
        roomName = obj.getString("roomName"),
        roomCode = if (obj.has("roomCode")) obj.getString("roomCode") else "",
        maxPlayers = obj.getInt("maxPlayers"),
        entryFee = obj.getInt("entryFee")
      )
    } catch (_: Exception) {
      null
    }
  }

  // --- Room State Serialization ---
  fun serializeRoomState(payload: RoomStatePayload): String {
    val obj = JSONObject()
    obj.put("hostId", payload.hostPlayerId)
    obj.put("roomCode", payload.roomCode)
    obj.put("max", payload.maxPlayers)
    obj.put("fee", payload.entryFee)
    obj.put("started", payload.isGameStarted)

    val arr = JSONArray()
    for (p in payload.players) {
      val pObj = JSONObject()
      pObj.put("id", p.id)
      pObj.put("name", p.name)
      pObj.put("type", p.type.name)
      pObj.put("count", p.cardCount)
      pObj.put("uno", p.hasDeclaredUno)
      pObj.put("conn", p.isConnected)
      pObj.put("ready", p.isReady)
      arr.put(pObj)
    }
    obj.put("players", arr)
    return obj.toString()
  }

  fun deserializeRoomState(jsonStr: String): RoomStatePayload? {
    return try {
      val obj = JSONObject(jsonStr)
      val arr = obj.getJSONArray("players")
      val playersList = ArrayList<NetworkPlayerInfo>(arr.length())
      for (i in 0 until arr.length()) {
        val pObj = arr.getJSONObject(i)
        playersList.add(
          NetworkPlayerInfo(
            id = pObj.getString("id"),
            name = pObj.getString("name"),
            type = PlayerType.valueOf(pObj.getString("type")),
            cardCount = pObj.getInt("count"),
            hasDeclaredUno = pObj.getBoolean("uno"),
            isConnected = pObj.getBoolean("conn"),
            isReady = if (pObj.has("ready")) pObj.getBoolean("ready") else false
          )
        )
      }
      RoomStatePayload(
        hostPlayerId = obj.getString("hostId"),
        roomCode = if (obj.has("roomCode")) obj.getString("roomCode") else "",
        maxPlayers = obj.getInt("max"),
        entryFee = obj.getInt("fee"),
        players = playersList,
        isGameStarted = obj.getBoolean("started")
      )
    } catch (_: Exception) {
      null
    }
  }

  // --- Player Action Serialization ---
  fun serializePlayerAction(payload: PlayerActionPayload): String {
    val obj = JSONObject()
    obj.put("action", payload.actionType.name)
    if (payload.cardId != null) obj.put("cardId", payload.cardId)
    if (payload.chosenColor != null) obj.put("color", payload.chosenColor.name)
    return obj.toString()
  }

  fun deserializePlayerAction(jsonStr: String): PlayerActionPayload? {
    return try {
      val obj = JSONObject(jsonStr)
      PlayerActionPayload(
        actionType = ClientActionType.valueOf(obj.getString("action")),
        cardId = if (obj.has("cardId")) obj.getString("cardId") else null,
        chosenColor = if (obj.has("color")) CardColor.valueOf(obj.getString("color")) else null
      )
    } catch (_: Exception) {
      null
    }
  }

  // --- Game State Serialization ---
  fun serializeGameState(payload: NetworkGameStatePayload): String {
    val obj = JSONObject()
    if (payload.currentTurnPlayerId != null) obj.put("turnId", payload.currentTurnPlayerId)
    obj.put("dir", payload.direction.name)
    obj.put("activeColor", payload.activeColor.name)
    obj.put("drawCount", payload.drawPileCount)
    obj.put("discardCount", payload.discardPileCount)
    obj.put("ended", payload.isRoundEnded)
    if (payload.winnerPlayerId != null) obj.put("winnerId", payload.winnerPlayerId)
    if (payload.winnerPlayerName != null) obj.put("winnerName", payload.winnerPlayerName)
    obj.put("pot", payload.totalPot)
    obj.put("msg", payload.lastEventMessage)
    obj.put("waitingWild", payload.isWaitingForWildColor)
    if (payload.wildColorChooserPlayerId != null) obj.put("wildChooser", payload.wildColorChooserPlayerId)

    if (payload.topDiscard != null) {
      val cardObj = JSONObject()
      cardObj.put("id", payload.topDiscard.id)
      cardObj.put("color", payload.topDiscard.color.name)
      cardObj.put("val", payload.topDiscard.value.name)
      obj.put("topDiscard", cardObj)
    }

    val playersArr = JSONArray()
    for (p in payload.players) {
      val pObj = JSONObject()
      pObj.put("id", p.id)
      pObj.put("name", p.name)
      pObj.put("type", p.type.name)
      pObj.put("count", p.cardCount)
      pObj.put("uno", p.hasDeclaredUno)
      pObj.put("conn", p.isConnected)
      pObj.put("ready", p.isReady)
      playersArr.put(pObj)
    }
    obj.put("players", playersArr)

    val handArr = JSONArray()
    for (c in payload.clientHand) {
      val cObj = JSONObject()
      cObj.put("id", c.id)
      cObj.put("color", c.color.name)
      cObj.put("val", c.value.name)
      handArr.put(cObj)
    }
    obj.put("hand", handArr)

    return obj.toString()
  }

  fun deserializeGameState(jsonStr: String): NetworkGameStatePayload? {
    return try {
      val obj = JSONObject(jsonStr)
      val topDiscard = if (obj.has("topDiscard")) {
        val cObj = obj.getJSONObject("topDiscard")
        UnoCard(
          id = cObj.getString("id"),
          color = CardColor.valueOf(cObj.getString("color")),
          value = CardValue.valueOf(cObj.getString("val"))
        )
      } else null

      val pArr = obj.getJSONArray("players")
      val playersList = ArrayList<NetworkPlayerInfo>(pArr.length())
      for (i in 0 until pArr.length()) {
        val pObj = pArr.getJSONObject(i)
        playersList.add(
          NetworkPlayerInfo(
            id = pObj.getString("id"),
            name = pObj.getString("name"),
            type = PlayerType.valueOf(pObj.getString("type")),
            cardCount = pObj.getInt("count"),
            hasDeclaredUno = pObj.getBoolean("uno"),
            isConnected = pObj.getBoolean("conn"),
            isReady = if (pObj.has("ready")) pObj.getBoolean("ready") else false
          )
        )
      }

      val hArr = obj.getJSONArray("hand")
      val handList = ArrayList<UnoCard>(hArr.length())
      for (i in 0 until hArr.length()) {
        val cObj = hArr.getJSONObject(i)
        handList.add(
          UnoCard(
            id = cObj.getString("id"),
            color = CardColor.valueOf(cObj.getString("color")),
            value = CardValue.valueOf(cObj.getString("val"))
          )
        )
      }

      NetworkGameStatePayload(
        currentTurnPlayerId = if (obj.has("turnId")) obj.getString("turnId") else null,
        direction = PlayDirection.valueOf(obj.getString("dir")),
        activeColor = CardColor.valueOf(obj.getString("activeColor")),
        topDiscard = topDiscard,
        drawPileCount = obj.getInt("drawCount"),
        discardPileCount = obj.getInt("discardCount"),
        isRoundEnded = obj.getBoolean("ended"),
        winnerPlayerId = if (obj.has("winnerId")) obj.getString("winnerId") else null,
        winnerPlayerName = if (obj.has("winnerName")) obj.getString("winnerName") else null,
        totalPot = obj.getInt("pot"),
        lastEventMessage = obj.getString("msg"),
        players = playersList,
        clientHand = handList,
        isWaitingForWildColor = obj.getBoolean("waitingWild"),
        wildColorChooserPlayerId = if (obj.has("wildChooser")) obj.getString("wildChooser") else null
      )
    } catch (_: Exception) {
      null
    }
  }
}
