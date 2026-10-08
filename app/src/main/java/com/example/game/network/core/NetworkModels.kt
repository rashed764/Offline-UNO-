package com.example.game.network.core

/**
 * Role of this local Android device within the LAN session.
 */
enum class NetworkRole {
  HOST,
  CLIENT
}

/**
 * High-level connection state of the network node.
 */
enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  FAILED
}

/**
 * Standard configuration constants for Offline UNO! LAN networking.
 */
object NetworkConfig {
  const val DEFAULT_PORT = 19842
  const val PROTOCOL_VERSION = 1
  const val SOCKET_TIMEOUT_MS = 6000
  const val PING_INTERVAL_MS = 3000L
}

/**
 * Structured network errors with friendly human-readable descriptions.
 */
sealed class NetworkError(val message: String) {
  data object HostUnreachable : NetworkError("Host device could not be reached on local network.")
  data object RoomFull : NetworkError("LAN room has already reached maximum player limit.")
  data object InvalidProtocol : NetworkError("Incompatible network game protocol version.")
  data object GameAlreadyStarted : NetworkError("Match in this room has already started.")
  data object DuplicatePlayerId : NetworkError("Player identity conflict detected.")
  data object DisconnectedByHost : NetworkError("Host closed the room or ended the connection.")
  data class InvalidAction(val reason: String) : NetworkError("Illegal action rejected: $reason")
  data class General(val errorMsg: String) : NetworkError(errorMsg)
}
