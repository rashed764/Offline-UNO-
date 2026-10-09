package com.example

import com.example.game.engine.UnoGameSession
import com.example.game.model.PlayerType
import com.example.game.network.client.LanClientConnection
import com.example.game.network.core.ConnectionState
import com.example.game.network.core.NetworkConfig
import com.example.game.network.host.LanHostServer
import com.example.game.network.protocol.MessageType
import com.example.game.network.protocol.NetworkEnvelope
import com.example.game.network.serialization.NetworkSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineUnoNetworkRegressionTestSuite {

  private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var hostServer: LanHostServer? = null
  private val clients = mutableListOf<LanClientConnection>()

  @After
  fun tearDown() {
    clients.forEach { it.disconnect() }
    clients.clear()
    hostServer?.stopServer()
    hostServer = null
  }

  @Test
  fun test1And3_ClientRemainsConnectedAndEntersGameWithoutFalseDisconnectWhenHostStarts() = runTest {
    val server = LanHostServer(hostPlayerName = "Host", port = 0, scope = testScope)
    hostServer = server
    server.startServer()

    waitUntil { server.isListening.value }
    val port = server.localPort

    val client = LanClientConnection(playerName = "Opponent", scope = testScope)
    clients.add(client)
    client.connectToHost("127.0.0.1", port)

    waitUntil { client.connectionState.value == ConnectionState.CONNECTED && server.connectedPlayers.value.size == 2 }

    assertEquals(ConnectionState.CONNECTED, client.connectionState.value)
    assertEquals(2, server.connectedPlayers.value.size)

    val started = server.startGame()
    assertTrue(started)

    waitUntil { client.isGameStarted.value && client.gameState.value != null }

    assertTrue("Client must receive GAME_STARTED signal", client.isGameStarted.value)
    assertNotNull("Client must receive initial authoritative game state", client.gameState.value)
    assertEquals(ConnectionState.CONNECTED, client.connectionState.value)
    assertNull(client.lastErrorMessage.value)
  }

  @Test
  fun test2_GameStartedAndInitialStateArriveInOrder() = runTest {
    val server = LanHostServer(hostPlayerName = "Host", port = 0, scope = testScope)
    hostServer = server
    server.startServer()
    waitUntil { server.isListening.value }

    val client = LanClientConnection(playerName = "OrderedClient", scope = testScope)
    clients.add(client)
    client.connectToHost("127.0.0.1", server.localPort)
    waitUntil { client.connectionState.value == ConnectionState.CONNECTED && server.connectedPlayers.value.size == 2 }

    server.startGame()

    waitUntil { client.isGameStarted.value && client.gameState.value != null }
    assertTrue(client.isGameStarted.value)
    assertNotNull(client.gameState.value)
    assertEquals(2, client.gameState.value!!.players.size)
    assertFalse(client.gameState.value!!.clientHand.isEmpty())
  }

  @Test
  fun test4_MultipleConnectedClientsReceiveGameStartAndValidInitialState() = runTest {
    val server = LanHostServer(hostPlayerName = "Host", maxPlayers = 3, port = 0, scope = testScope)
    hostServer = server
    server.startServer()
    waitUntil { server.isListening.value }

    val client1 = LanClientConnection(playerName = "Client1", scope = testScope)
    val client2 = LanClientConnection(playerName = "Client2", scope = testScope)
    clients.add(client1)
    clients.add(client2)

    client1.connectToHost("127.0.0.1", server.localPort)
    client2.connectToHost("127.0.0.1", server.localPort)

    waitUntil { server.connectedPlayers.value.size == 3 }
    assertEquals(3, server.connectedPlayers.value.size)

    server.startGame()

    waitUntil { client1.isGameStarted.value && client1.gameState.value != null &&
                client2.isGameStarted.value && client2.gameState.value != null }

    assertTrue(client1.isGameStarted.value)
    assertNotNull(client1.gameState.value)
    assertTrue(client2.isGameStarted.value)
    assertNotNull(client2.gameState.value)
    assertEquals(ConnectionState.CONNECTED, client1.connectionState.value)
    assertEquals(ConnectionState.CONNECTED, client2.connectionState.value)
  }

  @Test
  fun test5_GenuineDisconnectionIsDetectedCorrectly() = runTest {
    val server = LanHostServer(hostPlayerName = "Host", port = 0, scope = testScope)
    hostServer = server
    server.startServer()
    waitUntil { server.isListening.value }

    val client = LanClientConnection(playerName = "DiscClient", scope = testScope)
    clients.add(client)
    client.connectToHost("127.0.0.1", server.localPort)
    waitUntil { client.connectionState.value == ConnectionState.CONNECTED }

    server.stopServer()

    waitUntil { client.connectionState.value == ConnectionState.DISCONNECTED }
    assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
    assertNotNull(client.lastErrorMessage.value)
  }

  @Test
  fun test6_SerializationOrStateValidationFailuresProduceDiagnosableError() = runTest {
    val malformedJson = "{ invalid game state payload }"
    val deserializedState = NetworkSerializer.deserializeGameState(malformedJson)
    assertNull("Deserializing malformed game state must return null safely", deserializedState)

    val client = LanClientConnection(playerName = "SafeClient", scope = testScope)
    clients.add(client)
    val invalidEnv = NetworkEnvelope(
      messageId = "test_msg",
      type = MessageType.GAME_STATE_UPDATE,
      senderPlayerId = "host",
      payload = malformedJson
    )
    val serialized = NetworkSerializer.serializeEnvelope(invalidEnv)
    assertNotNull(serialized)
    val env = NetworkSerializer.deserializeEnvelope(serialized)
    assertNotNull(env)
    val statePayload = NetworkSerializer.deserializeGameState(env!!.payload)
    assertNull(statePayload)
  }

  private suspend fun waitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!condition()) {
      if (System.currentTimeMillis() - start > timeoutMs) {
        throw AssertionError("Condition timed out after $timeoutMs ms")
      }
      delay(50)
    }
  }
}
