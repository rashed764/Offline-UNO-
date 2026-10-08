package com.example.game.network.qr

import android.net.Uri

/**
 * Structured, versioned payload for LAN Wi-Fi QR code sharing.
 * Format:
 * OFFLINEUNO://join?v=1&room=AB7K9P&host=192.168.1.100&port=19842&pwd=secret123&fee=100&max=4
 */
data class LanQrPayload(
  val version: Int = 1,
  val roomCode: String,
  val hostIp: String,
  val port: Int,
  val password: String = "",
  val entryFee: Int = 50,
  val maxPlayers: Int = 2
) {

  fun toUriString(): String {
    val builder = Uri.Builder()
      .scheme(SCHEME)
      .authority("join")
      .appendQueryParameter("v", version.toString())
      .appendQueryParameter("room", roomCode)
      .appendQueryParameter("host", hostIp)
      .appendQueryParameter("port", port.toString())
      .appendQueryParameter("fee", entryFee.toString())
      .appendQueryParameter("max", maxPlayers.toString())

    if (password.isNotEmpty()) {
      builder.appendQueryParameter("pwd", password)
    }

    return builder.build().toString()
  }

  companion object {
    const val SCHEME = "OFFLINEUNO"

    fun parse(uriString: String): LanQrPayload? {
      return try {
        val uri = Uri.parse(uriString.trim())
        if (uri.scheme?.uppercase() != SCHEME) return null
        if (uri.authority?.lowercase() != "join") return null

        val v = uri.getQueryParameter("v")?.toIntOrNull() ?: 1
        val room = uri.getQueryParameter("room")?.uppercase()?.trim() ?: return null
        val host = uri.getQueryParameter("host")?.trim() ?: return null
        val port = uri.getQueryParameter("port")?.toIntOrNull() ?: return null
        val pwd = uri.getQueryParameter("pwd") ?: ""
        val fee = uri.getQueryParameter("fee")?.toIntOrNull() ?: 50
        val max = uri.getQueryParameter("max")?.toIntOrNull() ?: 2

        if (room.isEmpty() || host.isEmpty() || port <= 0 || port > 65535) {
          return null
        }

        LanQrPayload(
          version = v,
          roomCode = room,
          hostIp = host,
          port = port,
          password = pwd,
          entryFee = fee,
          maxPlayers = max
        )
      } catch (_: Exception) {
        null
      }
    }
  }
}
