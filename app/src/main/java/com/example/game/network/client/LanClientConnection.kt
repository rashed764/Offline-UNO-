package com.example.game.network.client

import android.util.Log
import com.example.game.model.CardColor
import com.example.game.model.UnoCard
import com.example.game.network.core.ConnectionState
import com.example.game.network.core.NetworkConfig
import com.example.game.network.protocol.ClientActionType
import com.example.game.network.protocol.MessageType
import com.example.game.network.protocol.NetworkEnvelope
import com.example.game.network.protocol.NetworkGameStatePayload
import com.example.game.network.protocol.PlayerActionPayload
import com.example.game.network.protocol.PlayerJoinRequestPayload
import com.example.game.network.protocol.RoomStatePayload
import com.example.game.network.serialization.NetworkSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/**
 * LAN Client Connection manager.
 * Connects to a host IP and port over local Wi-Fi, passes roomCode and password during handshake,
 * receives room and game updates, toggles ready state, and submits player actions.
 */
class LanClientConnection(
  private val playerName: String = "Player",
  private val scope: CoroutineScope
) {
  private val TAG = "LAN-CLIENT"

  val localPlayerId = "client_req_" + UUID.randomUUID().toString().take(6)
  var assignedPlayerId: String? = null
    private set

  private var socket: Socket? = null
  private var reader: BufferedReader? = null
  private var writer: BufferedWriter? = null
  private var receiveJob: Job? = null

  private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _isGameStarted = MutableStateFlow(false)
  val isGameStarted: StateFlow<Boolean> = _isGameStarted.asStateFlow()

  private val _roomState = MutableStateFlow<RoomStatePayload?>(null)
  val roomState: StateFlow<RoomStatePayload?> = _roomState.asStateFlow()

  private val _gameState = MutableStateFlow<NetworkGameStatePayload?>(null)
  val gameState: StateFlow<NetworkGameStatePayload?> = _gameState.asStateFlow()

  private val _lastErrorMessage = MutableStateFlow<String?>(null)
  val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

  fun connectToHost(
    hostIp: String,
    port: Int = NetworkConfig.DEFAULT_PORT,
    roomCode: String = "",
    password: String = ""
  ) {
    if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
      return
    }

    _connectionState.value = ConnectionState.CONNECTING
    _lastErrorMessage.value = null

    scope.launch(Dispatchers.IO) {
      try {
        val s = Socket()
        s.connect(InetSocketAddress(hostIp, port), NetworkConfig.SOCKET_TIMEOUT_MS)
        socket = s

        reader = BufferedReader(InputStreamReader(s.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(s.getOutputStream()))

        val joinReq = PlayerJoinRequestPayload(
          playerName = playerName,
          requestedPlayerId = localPlayerId,
          roomCode = roomCode.trim().uppercase(),
          password = password
        )
        val envelope = NetworkEnvelope(
          messageId = UUID.randomUUID().toString(),
          type = MessageType.PLAYER_JOIN_REQUEST,
          senderPlayerId = localPlayerId,
          payload = NetworkSerializer.serializeJoinRequest(joinReq)
        )
        sendEnvelope(envelope)

        _connectionState.value = ConnectionState.CONNECTED
        Log.i(TAG, "Connected to host at $hostIp:$port")

        startReceiveLoop()
      } catch (e: Exception) {
        Log.w(TAG, "Failed to connect to host: ${e.message}")
        _connectionState.value = ConnectionState.FAILED
        _lastErrorMessage.value = "Could not reach host at $hostIp:$port"
        disconnect()
      }
    }
  }

  private fun startReceiveLoop() {
    receiveJob = scope.launch(Dispatchers.IO) {
      try {
        val r = reader ?: return@launch
        while (isActive && socket != null && !socket!!.isClosed) {
          val line = r.readLine() ?: break
          handleIncomingMessage(line)
        }
      } catch (e: Exception) {
        Log.i(TAG, "Client socket disconnected: ${e.message}")
      } finally {
        if (_connectionState.value == ConnectionState.CONNECTED) {
          _connectionState.value = ConnectionState.DISCONNECTED
          _lastErrorMessage.value = "Connection to host lost."
        }
      }
    }
  }

  private fun handleIncomingMessage(rawLine: String) {
    val envelope = NetworkSerializer.deserializeEnvelope(rawLine) ?: return

    when (envelope.type) {
      MessageType.PLAYER_JOIN_ACCEPTED -> {
        val payload = NetworkSerializer.deserializeJoinAccepted(envelope.payload)
        if (payload != null) {
          assignedPlayerId = payload.assignedPlayerId
          Log.i(TAG, "Join accepted! Assigned ID: $assignedPlayerId, Room: ${payload.roomName}")
        }
      }

      MessageType.GAME_STARTED -> {
        Log.i(TAG, "Received GAME_STARTED message from host.")
        _isGameStarted.value = true
      }

      MessageType.PLAYER_JOIN_REJECTED -> {
        val reasonMsg = when (envelope.payload) {
          "WRONG_PASSWORD" -> "Incorrect room password."
          "ROOM_FULL" -> "Room has reached maximum capacity."
          "GAME_ALREADY_STARTED" -> "Match has already started in this room."
          "INVALID_ROOM_CODE" -> "Invalid room code."
          else -> envelope.payload
        }
        _lastErrorMessage.value = reasonMsg
        disconnect()
      }

      MessageType.HOST_DISCONNECTED -> {
        _lastErrorMessage.value = "Host ended the room session."
        disconnect()
      }

      MessageType.ROOM_STATE_UPDATE -> {
        val payload = NetworkSerializer.deserializeRoomState(envelope.payload)
        if (payload != null) {
          _roomState.value = payload
        }
      }

      MessageType.GAME_STATE_UPDATE -> {
        val payload = NetworkSerializer.deserializeGameState(envelope.payload)
        if (payload != null) {
          Log.d(TAG, "Received game state update. Active color: ${payload.activeColor}, Turn: ${payload.currentTurnPlayerId}, Hand size: ${payload.clientHand.size}")
          _gameState.value = payload
        } else {
          Log.w(TAG, "Failed to deserialize game state update payload.")
          _lastErrorMessage.value = "Received malformed game state from host."
        }
      }

      MessageType.ACTION_REJECTED -> {
        _lastErrorMessage.value = "Action rejected: ${envelope.payload}"
        Log.w(TAG, "Action rejected: ${envelope.payload}")
      }

      MessageType.PONG -> {
        // Keep-alive OK
      }

      else -> {
        Log.d(TAG, "Received message: ${envelope.type}")
      }
    }
  }

  fun toggleReady() {
    val pid = assignedPlayerId ?: return
    val envelope = NetworkEnvelope(
      messageId = UUID.randomUUID().toString(),
      type = MessageType.PLAYER_READY,
      senderPlayerId = pid,
      payload = ""
    )
    sendEnvelope(envelope)
  }

  fun playCard(cardId: String) {
    val pid = assignedPlayerId ?: return
    val payload = PlayerActionPayload(
      actionType = ClientActionType.PLAY_CARD,
      cardId = cardId
    )
    sendAction(pid, payload)
  }

  fun drawCard() {
    val pid = assignedPlayerId ?: return
    val payload = PlayerActionPayload(actionType = ClientActionType.DRAW_CARD)
    sendAction(pid, payload)
  }

  fun chooseWildColor(color: CardColor) {
    val pid = assignedPlayerId ?: return
    val payload = PlayerActionPayload(
      actionType = ClientActionType.CHOOSE_WILD_COLOR,
      chosenColor = color
    )
    sendAction(pid, payload)
  }

  fun declareUno() {
    val pid = assignedPlayerId ?: return
    val payload = PlayerActionPayload(actionType = ClientActionType.DECLARE_UNO)
    sendAction(pid, payload)
  }

  fun passDrawnTurn() {
    val pid = assignedPlayerId ?: return
    val payload = PlayerActionPayload(actionType = ClientActionType.PASS_DRAWN_TURN)
    sendAction(pid, payload)
  }

  fun catchUnoPenalty(targetPlayerId: String) {
    val pid = assignedPlayerId ?: return
    val payload = PlayerActionPayload(
      actionType = ClientActionType.CATCH_UNO,
      targetPlayerId = targetPlayerId
    )
    sendAction(pid, payload)
  }

  private fun sendAction(senderId: String, payload: PlayerActionPayload) {
    val envelope = NetworkEnvelope(
      messageId = UUID.randomUUID().toString(),
      type = MessageType.PLAYER_ACTION,
      senderPlayerId = senderId,
      payload = NetworkSerializer.serializePlayerAction(payload)
    )
    sendEnvelope(envelope)
  }

  private fun sendEnvelope(envelope: NetworkEnvelope) {
    scope.launch(Dispatchers.IO) {
      try {
        val w = writer ?: return@launch
        val serialized = NetworkSerializer.serializeEnvelope(envelope)
        synchronized(w) {
          w.write(serialized)
          w.newLine()
          w.flush()
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error sending envelope: ${e.message}")
      }
    }
  }

  fun disconnect() {
    receiveJob?.cancel()
    receiveJob = null
    try {
      socket?.close()
    } catch (_: Exception) {}
    socket = null
    reader = null
    writer = null
    _connectionState.value = ConnectionState.DISCONNECTED
  }
}
