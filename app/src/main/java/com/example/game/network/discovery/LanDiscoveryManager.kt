package com.example.game.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredRoom(
  val serviceName: String,
  val hostIp: String,
  val port: Int,
  val entryFee: Int = 50,
  val maxPlayers: Int = 2,
  val currentPlayers: Int = 1
)

/**
 * Android NSD (Network Service Discovery / mDNS) Manager.
 * Advertises active host rooms on local Wi-Fi without internet or external servers,
 * and allows joining clients to automatically discover nearby active games.
 */
class LanDiscoveryManager(context: Context) {
  private val TAG = "LAN-NSD"
  private val SERVICE_TYPE = "_offlineuno._tcp."

  private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

  private var registrationListener: NsdManager.RegistrationListener? = null
  private var discoveryListener: NsdManager.DiscoveryListener? = null

  private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
  val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

  fun registerHostService(
    roomName: String,
    port: Int,
    entryFee: Int,
    maxPlayers: Int
  ) {
    unregisterHostService()
    val nsd = nsdManager ?: return

    val serviceInfo = NsdServiceInfo().apply {
      serviceName = roomName.take(16)
      serviceType = SERVICE_TYPE
      setPort(port)
      setAttribute("fee", entryFee.toString())
      setAttribute("max", maxPlayers.toString())
    }

    registrationListener = object : NsdManager.RegistrationListener {
      override fun onServiceRegistered(info: NsdServiceInfo?) {
        Log.i(TAG, "NSD host service registered: ${info?.serviceName}")
      }

      override fun onRegistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
        Log.w(TAG, "NSD registration failed: $errorCode")
      }

      override fun onServiceUnregistered(info: NsdServiceInfo?) {
        Log.i(TAG, "NSD host service unregistered")
      }

      override fun onUnregistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
        Log.w(TAG, "NSD unregistration failed: $errorCode")
      }
    }

    try {
      nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    } catch (e: Exception) {
      Log.w(TAG, "Exception registering NSD service: ${e.message}")
    }
  }

  fun unregisterHostService() {
    registrationListener?.let { listener ->
      try {
        nsdManager?.unregisterService(listener)
      } catch (_: Exception) {}
    }
    registrationListener = null
  }

  fun startDiscovery() {
    stopDiscovery()
    val nsd = nsdManager ?: return
    _discoveredRooms.value = emptyList()
    _isSearching.value = true

    discoveryListener = object : NsdManager.DiscoveryListener {
      override fun onDiscoveryStarted(regType: String?) {
        Log.i(TAG, "NSD discovery started: $regType")
      }

      override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo == null || serviceInfo.serviceType != SERVICE_TYPE) return
        resolveService(serviceInfo)
      }

      override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo == null) return
        _discoveredRooms.value = _discoveredRooms.value.filter { it.serviceName != serviceInfo.serviceName }
      }

      override fun onDiscoveryStopped(serviceType: String?) {
        _isSearching.value = false
        Log.i(TAG, "NSD discovery stopped")
      }

      override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
        _isSearching.value = false
        Log.w(TAG, "NSD start discovery failed: $errorCode")
      }

      override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
        Log.w(TAG, "NSD stop discovery failed: $errorCode")
      }
    }

    try {
      nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    } catch (e: Exception) {
      _isSearching.value = false
      Log.w(TAG, "Exception starting NSD discovery: ${e.message}")
    }
  }

  private fun resolveService(info: NsdServiceInfo) {
    try {
      nsdManager?.resolveService(info, object : NsdManager.ResolveListener {
        override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
          val host = resolvedInfo?.host?.hostAddress ?: return
          val port = resolvedInfo.port
          val name = resolvedInfo.serviceName ?: "Offline UNO Room"

          val fee = try {
            resolvedInfo.attributes?.get("fee")?.let { String(it) }?.toIntOrNull() ?: 50
          } catch (_: Exception) { 50 }

          val max = try {
            resolvedInfo.attributes?.get("max")?.let { String(it) }?.toIntOrNull() ?: 2
          } catch (_: Exception) { 2 }

          val room = DiscoveredRoom(
            serviceName = name,
            hostIp = host,
            port = port,
            entryFee = fee,
            maxPlayers = max
          )

          val current = _discoveredRooms.value.toMutableList()
          current.removeAll { it.serviceName == room.serviceName }
          current.add(room)
          _discoveredRooms.value = current
          Log.i(TAG, "Discovered room: $name at $host:$port")
        }

        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
          Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
        }
      })
    } catch (e: Exception) {
      Log.w(TAG, "Exception resolving service: ${e.message}")
    }
  }

  fun stopDiscovery() {
    discoveryListener?.let { listener ->
      try {
        nsdManager?.stopServiceDiscovery(listener)
      } catch (_: Exception) {}
    }
    discoveryListener = null
    _isSearching.value = false
  }

  fun cleanup() {
    unregisterHostService()
    stopDiscovery()
  }
}
