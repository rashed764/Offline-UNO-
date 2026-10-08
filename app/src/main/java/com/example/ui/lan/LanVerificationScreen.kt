package com.example.ui.lan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AudioManager
import com.example.core.economy.CoinRepository
import com.example.core.persistence.PreferencesManager
import com.example.game.network.client.LanClientConnection
import com.example.game.network.core.ConnectionState
import com.example.game.network.core.LanNetworkUtils
import com.example.game.network.host.LanHostServer
import com.example.ui.components.CoinBadge
import com.example.ui.components.GameButton
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid
import com.example.ui.theme.UnoBlue
import com.example.ui.theme.UnoGreen
import com.example.ui.theme.UnoRed

/**
 * Phase 4 LAN Testing & Verification Screen.
 * Provides a clean landscape test console allowing verification of:
 * - Local Wi-Fi IPv4 address discovery
 * - Host Room Creation & Listening on local port
 * - Client connection over local IP
 * - Real-time room player count & start eligibility
 */
@Composable
fun LanVerificationScreen(
  entryFee: Int,
  maxPlayers: Int,
  coinRepository: CoinRepository,
  preferencesManager: PreferencesManager,
  audioManager: AudioManager,
  onBackClick: () -> Unit,
  onStartMatch: (isHost: Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val coins by coinRepository.coinBalance.collectAsState()
  val scope = rememberCoroutineScope()

  var isHostMode by remember { mutableStateOf(true) }
  val localIp = remember { LanNetworkUtils.getLocalIpv4Address() ?: "192.168.1.100" }

  // Host instance
  val hostServer = remember {
    LanHostServer(
      hostPlayerName = preferencesManager.playerName,
      maxPlayers = maxPlayers,
      entryFee = entryFee,
      scope = scope
    )
  }

  // Client instance
  val clientConnection = remember {
    LanClientConnection(
      playerName = preferencesManager.playerName + " (Client)",
      scope = scope
    )
  }

  var targetHostIpInput by remember { mutableStateOf(localIp) }

  val hostPlayers by hostServer.connectedPlayers.collectAsState()
  val isHostListening by hostServer.isListening.collectAsState()

  val clientConnState by clientConnection.connectionState.collectAsState()
  val clientRoomState by clientConnection.roomState.collectAsState()
  val clientError by clientConnection.lastErrorMessage.collectAsState()

  DisposableEffect(Unit) {
    onDispose {
      hostServer.stopServer()
      clientConnection.disconnect()
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(listOf(TableFeltMid, TableFeltDark))
      )
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag("lan_verification_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              audioManager.playButtonClick()
              hostServer.stopServer()
              clientConnection.disconnect()
              onBackClick()
            },
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
              .testTag("btn_lan_back")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "VS FRIENDS • LAN WI-FI SETUP",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = "Local IP: $localIp • Port: 19842 • No Internet Needed",
              fontFamily = FredokaFontFamily,
              fontSize = 12.sp,
              color = Color(0xFFFFD100)
            )
          }
        }

        CoinBadge(coins = coins)
      }

      // Middle: Split Host vs Join Panels
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Left Panel: HOST ROOM
        Card(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isHostMode) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.6f)
          ),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              if (isHostMode) listOf(GoldAccent, UnoGreen) else listOf(Color.Gray, Color.DarkGray)
            )
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cast, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("HOST A GAME", fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
              }

              if (isHostListening) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(UnoGreen)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("LISTENING", fontFamily = FredokaFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Players in room (${hostPlayers.size}/$maxPlayers):",
                fontFamily = FredokaFontFamily,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
              )
              hostPlayers.forEach { p ->
                Text(
                  text = "• ${p.name} ${if (p.id == hostServer.hostPlayerId) "(Host)" else ""}",
                  fontFamily = FredokaFontFamily,
                  fontSize = 13.sp,
                  color = if (p.isConnected) Color.White else Color.Gray
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              GameButton(
                text = if (isHostListening) "RESTART HOST" else "CREATE HOST ROOM",
                onClick = {
                  isHostMode = true
                  audioManager.playButtonClick()
                  hostServer.startServer()
                },
                gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                modifier = Modifier.weight(1f),
                testTag = "btn_start_host_server"
              )

              GameButton(
                text = "START MATCH",
                enabled = isHostListening && hostPlayers.size >= 2,
                onClick = {
                  audioManager.playButtonClick()
                  hostServer.startGame()
                  onStartMatch(true)
                },
                gradientColors = listOf(UnoGreen, Color(0xFF047857)),
                modifier = Modifier.weight(1f),
                testTag = "btn_host_start_game"
              )
            }
          }
        }

        // Right Panel: JOIN ROOM
        Card(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (!isHostMode) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.6f)
          ),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              if (!isHostMode) listOf(UnoBlue, Color.Cyan) else listOf(Color.Gray, Color.DarkGray)
            )
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Wifi, contentDescription = null, tint = UnoBlue, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("JOIN HOST ROOM", fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = targetHostIpInput,
                onValueChange = { targetHostIpInput = it },
                label = { Text("Host Wi-Fi IPv4 Address", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_target_host_ip"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = UnoBlue,
                  unfocusedBorderColor = Color.Gray,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                )
              )

              Text(
                text = "Status: ${clientConnState.name} ${if (clientRoomState != null) "(${clientRoomState?.players?.size} players)" else ""}",
                fontFamily = FredokaFontFamily,
                fontSize = 12.sp,
                color = when (clientConnState) {
                  ConnectionState.CONNECTED -> UnoGreen
                  ConnectionState.CONNECTING -> GoldAccent
                  else -> Color(0xFF94A3B8)
                }
              )

              if (clientError != null) {
                Text(text = clientError ?: "", color = UnoRed, fontSize = 11.sp, fontFamily = FredokaFontFamily)
              }
            }

            GameButton(
              text = if (clientConnState == ConnectionState.CONNECTED) "CONNECTED TO HOST" else "CONNECT TO HOST",
              enabled = clientConnState != ConnectionState.CONNECTING,
              onClick = {
                isHostMode = false
                audioManager.playButtonClick()
                clientConnection.connectToHost(targetHostIpInput.trim())
              },
              gradientColors = listOf(UnoBlue, Color(0xFF0284C7)),
              testTag = "btn_client_connect"
            )
          }
        }
      }

      // Bottom Status info
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Local Wi-Fi / Hotspot Only • Host validates all card rules & turns • Zero Cloud",
          fontFamily = FredokaFontFamily,
          fontSize = 12.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }
  }
}
