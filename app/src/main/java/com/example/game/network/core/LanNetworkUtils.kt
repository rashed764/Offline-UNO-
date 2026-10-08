package com.example.game.network.core

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utility to identify the local device's IPv4 address on the Wi-Fi/LAN interface.
 */
object LanNetworkUtils {

  fun getLocalIpv4Address(): String? {
    try {
      val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
      while (interfaces.hasMoreElements()) {
        val intf = interfaces.nextElement()
        // Skip loopback and down interfaces
        if (intf.isLoopback || !intf.isUp) continue

        val addresses = intf.inetAddresses
        while (addresses.hasMoreElements()) {
          val addr = addresses.nextElement()
          if (!addr.isLoopbackAddress && addr is Inet4Address) {
            val ip = addr.hostAddress
            // Prefer typical private Wi-Fi ranges (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
            if (ip != null && (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))) {
              return ip
            }
          }
        }
      }
    } catch (_: Exception) {
      // Fallback
    }
    return null
  }
}
