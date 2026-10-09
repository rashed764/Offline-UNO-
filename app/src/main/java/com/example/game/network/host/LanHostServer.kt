package com.example.game.network.host

import android.util.Log
import com.example.game.engine.GameStatus
import com.example.game.engine.UnoGameSession
import com.example.game.model.CardColor
import com.example.game.model.PlayerType
import com.example.game.model.UnoPlayer
import com.example.game.network.core.LanNetworkUtils
import com.example.game.network.core.NetworkConfig
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
import com.example.game.network.qr.LanQrPayload
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
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Authoritative LAN Host Server.
 * Supports automatic client game start broadcast, UNO catch validation, and drawn card turn passing.
 */
class LanHostServer(
  private val hostPlayerName: String = "Host",
  val roomCode: String = generateRoomCode(),
  private val password: String = "",
  val maxPlayers: Int = 2,
  val entryFee: Int = 50,
  val port: Int = NetworkConfig.DEFAULT_PORT,
  private val session: UnoGameSession = UnoGameSession(),
  private val scope: CoroutineScope
) {
  private val TAG = "LAN-HOST"

  val hostPlayerId = "host_" + UUID.randomUUID().toString().take(8)

  private var serverSocket: ServerSocket? = null
  private var acceptJob: Job? = null
  private var sessionObserverJob: Job? = null

  private val clientConnections = ConcurrentHashMap<String, ConnectedClient>()

  private val _connectedPlayers = MutableStateFlow<List<NetworkPlayerInfo>>(
    listOf(
      NetworkPlayerInfo(
        id = hostPlayerId,
        name = hostPlayerName,
        type = PlayerType.HUMAN,
        cardCount = 0,
        hasDeclaredUno = false,
        isUnoVulnerable = false,
        isConnected = true,
        isReady = true
      )
    )
  )
  val connectedPlayers: StateFlow<List<NetworkPlayerInfo>> = _connectedPlayers.asStateFlow()

  private val _isListening = MutableStateFlow(false)
  val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

  private val _isGameStarted = MutableStateFlow(false)
  val isGameStarted: StateFlow<Boolean> = _isGameStarted.asStateFlow()

  val gameSession: UnoGameSession get() = session

  fun getQrPayload(hostIp: String = LanNetworkUtils.getLocalIpv4Address() ?: "127.0.0.1"): LanQrPayload {
    return LanQrPayload(
      version = NetworkConfig.PROTOCOL_VERSION,
      roomCode = roomCode,
      hostIp = hostIp,
      port = port,
      password = password,
      entryFee = entryFee,
      maxPlayers = maxPlayers
    )
  }

  inner class ConnectedClient(
    val playerId: String,
    val playerName: String,
    val socket: Socket,
    private val writer: BufferedWriter,
    private val reader: BufferedReader
  ) {
    var listenJob: Job? = null

    fun send(envelope: NetworkEnvelope) {
      try {
        val serialized = NetworkSerializer.serializeEnvelope(envelope)
        synchronized(writer) {
          writer.write(serialized)
          writer.newLine()
          writer.flush()
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error sending to client $playerId: ${e.message}")
        handleClientDisconnect(playerId)
      }
    }

    fun startListening() {
      listenJob = scope.launch(Dispatchers.IO) {
        try {
          while (isActive && !socket.isClosed) {
            val line = reader.readLine() ?: break
            handleIncomingClientMessage(playerId, line)
          }
        } catch (e: Exception) {
          Log.i(TAG, "Client socket closed: $playerId (${e.message})")
        } finally {
          handleClientDisconnect(playerId)
        }
      }
    }

    fun close() {
      listenJob?.cancel()
      try {
        socket.close()
      } catch (_: Exception) {}
    }
  }

  fun startServer() {
    if (_isListening.value) return

    scope.launch(Dispatchers.IO) {
      try {
        serverSocket = ServerSocket(port)
        _isListening.value = true
        Log.i(TAG, "Host server started on port $port for room $roomCode. Host ID: $hostPlayerId")

        sessionObserverJob = scope.launch {
          session.state.collect { gameState ->
            if (_isGameStarted.value) {
              broadcastGameState(gameState)
            }
          }
        }

        while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
          val clientSocket = serverSocket!!.accept()
          handleNewSocketConnection(clientSocket)
        }
      } catch (e: Exception) {
        Log.i(TAG, "Server socket stopped: ${e.message}")
      } finally {
        _isListening.value = false
      }
    }
  }

  private fun handleNewSocketConnection(socket: Socket) {
    scope.launch(Dispatchers.IO) {
      try {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

        val initialLine = reader.readLine() ?: return@launch
        val envelope = NetworkSerializer.deserializeEnvelope(initialLine)

        if (envelope == null || envelope.type != MessageType.PLAYER_JOIN_REQUEST) {
          sendRejection(writer, "Invalid handshake message.")
          socket.close()
          return@launch
        }

        val joinReq = NetworkSerializer.deserializeJoinRequest(envelope.payload)
        if (joinReq == null) {
          sendRejection(writer, "Malformed join request.")
          socket.close()
          return@launch
        }

        // 1. Password Verification
        if (password.isNotEmpty()) {
          if (!constantTimeEquals(password, joinReq.password)) {
            sendRejection(writer, "WRONG_PASSWORD")
            socket.close()
            Log.w(TAG, "Client ${joinReq.playerName} rejected: Incorrect password.")
            return@launch
          }
        }

        // 2. Room Code Verification (if supplied)
        if (joinReq.roomCode.isNotEmpty() && joinReq.roomCode.uppercase() != roomCode) {
          sendRejection(writer, "INVALID_ROOM_CODE")
          socket.close()
          return@launch
        }

        // 3. Room full check
        val currentCount = _connectedPlayers.value.size
        if (currentCount >= maxPlayers) {
          sendRejection(writer, "ROOM_FULL")
          socket.close()
          return@launch
        }

        // 4. Game already started check
        if (_isGameStarted.value) {
          sendRejection(writer, "GAME_ALREADY_STARTED")
          socket.close()
          return@launch
        }

        val assignedId = "client_" + UUID.randomUUID().toString().take(8)
        val client = ConnectedClient(
          playerId = assignedId,
          playerName = joinReq.playerName.take(16).ifEmpty { "Player $currentCount" },
          socket = socket,
          writer = writer,
          reader = reader
        )

        clientConnections[assignedId] = client

        val acceptedPayload = PlayerJoinAcceptedPayload(
          assignedPlayerId = assignedId,
          roomName = "$hostPlayerName's Room",
          roomCode = roomCode,
          maxPlayers = maxPlayers,
          entryFee = entryFee
        )
        val acceptEnvelope = NetworkEnvelope(
          messageId = UUID.randomUUID().toString(),
          type = MessageType.PLAYER_JOIN_ACCEPTED,
          senderPlayerId = hostPlayerId,
          payload = NetworkSerializer.serializeJoinAccepted(acceptedPayload)
        )
        client.send(acceptEnvelope)

        val updated = _connectedPlayers.value.toMutableList()
        updated.add(
          NetworkPlayerInfo(
            id = assignedId,
            name = client.playerName,
            type = PlayerType.LAN_CLIENT,
            cardCount = 0,
            hasDeclaredUno = false,
            isUnoVulnerable = false,
            isConnected = true,
            isReady = true
          )
        )
        _connectedPlayers.value = updated

        client.startListening()
        broadcastRoomState()

        Log.i(TAG, "Client joined: ${client.playerName} ($assignedId). Room size: ${updated.size}/$maxPlayers")
      } catch (e: Exception) {
        Log.w(TAG, "Error handling client handshake: ${e.message}")
      }
    }
  }

  private fun sendRejection(writer: BufferedWriter, reason: String) {
    try {
      val payload = PlayerJoinRejectedPayload(reason = reason)
      val envelope = NetworkEnvelope(
        messageId = UUID.randomUUID().toString(),
        type = MessageType.PLAYER_JOIN_REJECTED,
        senderPlayerId = hostPlayerId,
        payload = reason
      )
      writer.write(NetworkSerializer.serializeEnvelope(envelope))
      writer.newLine()
      writer.flush()
    } catch (_: Exception) {}
  }

  fun handleIncomingClientMessage(playerId: String, rawLine: String) {
    val envelope = NetworkSerializer.deserializeEnvelope(rawLine) ?: return

    when (envelope.type) {
      MessageType.PLAYER_ACTION -> {
        val action = NetworkSerializer.deserializePlayerAction(envelope.payload) ?: return
        executeClientAction(playerId, action)
      }
      MessageType.PLAYER_READY -> {
        togglePlayerReady(playerId)
      }
      MessageType.DECLARE_UNO -> {
        session.declareUno(playerId)
      }
      MessageType.CATCH_UNO_PENALTY -> {
        val action = NetworkSerializer.deserializePlayerAction(envelope.payload)
        val target = action?.targetPlayerId ?: return
        session.catchUnoPenalty(catcherId = playerId, targetPlayerId = target)
      }
      MessageType.PING -> {
        val client = clientConnections[playerId]
        val pong = NetworkEnvelope(
          messageId = UUID.randomUUID().toString(),
          type = MessageType.PONG,
          senderPlayerId = hostPlayerId,
          payload = ""
        )
        client?.send(pong)
      }
      else -> {
        Log.d(TAG, "Unhandled message from $playerId: ${envelope.type}")
      }
    }
  }

  private fun togglePlayerReady(playerId: String) {
    val updated = _connectedPlayers.value.map { p ->
      if (p.id == playerId) p.copy(isReady = !p.isReady) else p
    }
    _connectedPlayers.value = updated
    broadcastRoomState()
  }

  @Synchronized
  fun executeClientAction(playerId: String, action: PlayerActionPayload): Boolean {
    val currentState = session.state.value

    // Catch UNO does not require it to be the catcher's turn
    if (action.actionType == ClientActionType.CATCH_UNO) {
      val target = action.targetPlayerId ?: return false
      return session.catchUnoPenalty(catcherId = playerId, targetPlayerId = target)
    }

    if (currentState.currentPlayer?.id != playerId) {
      sendActionRejection(playerId, "NOT_YOUR_TURN")
      return false
    }

    return when (action.actionType) {
      ClientActionType.PLAY_CARD -> {
        val cardId = action.cardId ?: return false
        val success = session.playCard(playerId, cardId)
        if (!success) {
          sendActionRejection(playerId, "ILLEGAL_MOVE")
        }
        success
      }
      ClientActionType.DRAW_CARD -> {
        val drawn = session.drawCard(playerId)
        drawn != null
      }
      ClientActionType.PASS_DRAWN_TURN -> {
        session.passDrawnCard(playerId)
      }
      ClientActionType.CHOOSE_WILD_COLOR -> {
        val color = action.chosenColor ?: return false
        session.selectWildColor(color)
      }
      ClientActionType.DECLARE_UNO -> {
        session.declareUno(playerId)
      }
      ClientActionType.CATCH_UNO -> {
        val target = action.targetPlayerId ?: return false
        session.catchUnoPenalty(playerId, target)
      }
    }
  }

  private fun sendActionRejection(playerId: String, reason: String) {
    val client = clientConnections[playerId] ?: return
    val env = NetworkEnvelope(
      messageId = UUID.randomUUID().toString(),
      type = MessageType.ACTION_REJECTED,
      senderPlayerId = hostPlayerId,
      payload = reason
    )
    client.send(env)
  }

  fun startGame(): Boolean {
    val currentPlayers = _connectedPlayers.value
    if (currentPlayers.size < 2) return false
    val allReady = currentPlayers.all { it.isReady }
    if (!allReady) return false

    val unoPlayers = currentPlayers.map { p ->
      UnoPlayer(
        id = p.id,
        name = p.name,
        type = if (p.id == hostPlayerId) PlayerType.HUMAN else PlayerType.LAN_CLIENT
      )
    }

    _isGameStarted.value = true
    session.startNewMatch(unoPlayers, entryFee)

    // Critical fix: Broadcast GAME_STARTED and immediately follow with full authoritative initial state
    val startEnvelope = NetworkEnvelope(
      messageId = UUID.randomUUID().toString(),
      type = MessageType.GAME_STARTED,
      senderPlayerId = hostPlayerId,
      payload = ""
    )
    broadcastToClients(startEnvelope)

    // Broadcast room update marked as started as well for redundancy
    broadcastRoomState()
    broadcastGameState(session.state.value)
    return true
  }

  fun broadcastRoomState() {
    val payload = RoomStatePayload(
      hostPlayerId = hostPlayerId,
      roomCode = roomCode,
      maxPlayers = maxPlayers,
      entryFee = entryFee,
      players = _connectedPlayers.value,
      isGameStarted = _isGameStarted.value
    )
    val envelope = NetworkEnvelope(
      messageId = UUID.randomUUID().toString(),
      type = MessageType.ROOM_STATE_UPDATE,
      senderPlayerId = hostPlayerId,
      payload = NetworkSerializer.serializeRoomState(payload)
    )
    broadcastToClients(envelope)
  }

  fun broadcastGameState(gameState: com.example.game.engine.UnoGameState) {
    val playerInfos = gameState.players.map { p ->
      NetworkPlayerInfo(
        id = p.id,
        name = p.name,
        type = p.type,
        cardCount = p.cardCount,
        hasDeclaredUno = p.hasDeclaredUno,
        isUnoVulnerable = p.isUnoVulnerable,
        isConnected = p.isConnected,
        isReady = true
      )
    }

    _connectedPlayers.value = playerInfos

    clientConnections.forEach { (clientId, client) ->
      val clientPlayer = gameState.players.find { it.id == clientId }
      val clientHand = clientPlayer?.hand ?: emptyList()

      val waitingForWild = gameState.status is GameStatus.WaitingForWildColor
      val wildChooserId = (gameState.status as? GameStatus.WaitingForWildColor)?.player?.id

      val payload = NetworkGameStatePayload(
        currentTurnPlayerId = gameState.currentPlayer?.id,
        direction = gameState.direction,
        activeColor = gameState.activeColor,
        topDiscard = gameState.topDiscard,
        drawPileCount = gameState.drawPileCount,
        discardPileCount = gameState.discardPileCount,
        isRoundEnded = gameState.status is GameStatus.RoundEnded,
        winnerPlayerId = (gameState.status as? GameStatus.RoundEnded)?.winner?.id,
        winnerPlayerName = (gameState.status as? GameStatus.RoundEnded)?.winner?.name,
        totalPot = gameState.totalPot,
        lastEventMessage = gameState.lastEventMessage,
        players = playerInfos,
        clientHand = clientHand,
        isWaitingForWildColor = waitingForWild,
        wildColorChooserPlayerId = wildChooserId,
        drawnCardPlayableId = gameState.drawnCardPlayableId
      )

      val envelope = NetworkEnvelope(
        messageId = UUID.randomUUID().toString(),
        type = MessageType.GAME_STATE_UPDATE,
        senderPlayerId = hostPlayerId,
        payload = NetworkSerializer.serializeGameState(payload)
      )
      client.send(envelope)
    }
  }

  private fun broadcastToClients(envelope: NetworkEnvelope) {
    clientConnections.values.forEach { it.send(envelope) }
  }

  private fun handleClientDisconnect(playerId: String) {
    val removed = clientConnections.remove(playerId)
    if (removed != null) {
      removed.close()
      Log.i(TAG, "Client disconnected: $playerId")

      val updated = _connectedPlayers.value.map {
        if (it.id == playerId) it.copy(isConnected = false) else it
      }
      _connectedPlayers.value = updated
      session.updateLastMessage("${removed.playerName} disconnected.")
      broadcastRoomState()
    }
  }

  fun stopServer() {
    sessionObserverJob?.cancel()
    acceptJob?.cancel()

    val hostDisconnectEnv = NetworkEnvelope(
      messageId = UUID.randomUUID().toString(),
      type = MessageType.HOST_DISCONNECTED,
      senderPlayerId = hostPlayerId,
      payload = "Host closed the room."
    )
    broadcastToClients(hostDisconnectEnv)

    clientConnections.values.forEach { it.close() }
    clientConnections.clear()

    try {
      serverSocket?.close()
    } catch (_: Exception) {}
    serverSocket = null
    _isListening.value = false
    _isGameStarted.value = false
    Log.i(TAG, "Host server stopped.")
  }

  private fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code xor b[i].code)
    }
    return result == 0
  }

  companion object {
    fun generateRoomCode(): String {
      val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
      return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
  }
}
