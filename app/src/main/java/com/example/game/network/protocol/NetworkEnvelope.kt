package com.example.game.network.protocol

/**
 * Explicit message types for the Offline UNO! LAN protocol.
 */
enum class MessageType {
  // Discovery & Room Negotiation
  HOST_ROOM_CREATED,
  PLAYER_JOIN_REQUEST,
  PLAYER_JOIN_ACCEPTED,
  PLAYER_JOIN_REJECTED,
  ROOM_STATE_UPDATE,
  PLAYER_READY,

  // Match Lifecycle
  GAME_START_REQUEST,
  GAME_STARTED,
  GAME_STATE_UPDATE,

  // Gameplay Actions (Client -> Host)
  PLAYER_ACTION,
  ACTION_REJECTED,

  // UNO Declarations & Catch Penalty
  DECLARE_UNO,
  CATCH_UNO_PENALTY,

  // Heartbeat & Presence
  PLAYER_DISCONNECTED,
  HOST_DISCONNECTED,
  PING,
  PONG,
  ERROR
}

/**
 * Standard Network Envelope transmitted over TCP streams.
 */
data class NetworkEnvelope(
  val protocolVersion: Int = 1,
  val messageId: String,
  val type: MessageType,
  val senderPlayerId: String,
  val payload: String
)
