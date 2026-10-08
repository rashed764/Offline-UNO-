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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.game.network.core.NetworkConfig
import com.example.game.network.qr.LanQrPayload
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
 * Polished, landscape Join Waiting Room screen.
 * Supports:
 * - Method A: Manual Room Code, Host IP, and Password entry
 * - Method B: QR Code Join input / scanner parser
 * - Live Room Waiting Room observing host room updates and ready status
 */
@Composable
fun JoinWaitingRoomScreen(
  coinRepository: CoinRepository,
  preferencesManager: PreferencesManager,
  audioManager: AudioManager,
  onBackClick: () -> Unit,
  onGameStarted: (client: LanClientConnection) -> Unit,
  modifier: Modifier = Modifier
) {
  val coins by coinRepository.coinBalance.collectAsState()
  val scope = rememberCoroutineScope()

  val localIp = remember { LanNetworkUtils.getLocalIpv4Address() ?: "192.168.1.100" }

  var hostIpInput by remember { mutableStateOf(localIp) }
  var roomCodeInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var qrCodePayloadInput by remember { mutableStateOf("") }

  var showQrInputBox by remember { mutableStateOf(false) }

  val clientConnection = remember {
    LanClientConnection(
      playerName = preferencesManager.playerName,
      scope = scope
    )
  }

  val connectionState by clientConnection.connectionState.collectAsState()
  val roomState by clientConnection.roomState.collectAsState()
  val errorMessage by clientConnection.lastErrorMessage.collectAsState()

  DisposableEffect(Unit) {
    onDispose {
      clientConnection.disconnect()
    }
  }

  // Observe match start signal from host
  LaunchedEffect(roomState?.isGameStarted) {
    if (roomState?.isGameStarted == true) {
      audioManager.playTurnAlert()
      onGameStarted(clientConnection)
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(TableFeltMid, TableFeltDark)))
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag("join_waiting_room_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. TOP HEADER: Back, Title, Coin Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              audioManager.playButtonClick()
              clientConnection.disconnect()
              onBackClick()
            },
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
              .testTag("join_back_button")
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
              text = "JOIN THE GAME • LAN WI-FI",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = if (connectionState == ConnectionState.CONNECTED) "Connected to Host • Waiting for Start" else "Enter room credentials or scan QR",
              fontFamily = FredokaFontFamily,
              fontSize = 12.sp,
              color = if (connectionState == ConnectionState.CONNECTED) UnoGreen else Color(0xFFFFD100)
            )
          }
        }

        CoinBadge(coins = coins)
      }

      // 2. MAIN CONTENT: Connection Form vs. Live Room Waiting Panel
      if (connectionState != ConnectionState.CONNECTED) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          // Left: Manual Join Card
          Card(
            modifier = Modifier
              .weight(1.1f)
              .fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(UnoBlue, Color(0xFF0284C7)))
            )
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "MANUAL ROOM ENTRY",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = hostIpInput,
                  onValueChange = { hostIpInput = it.trim() },
                  label = { Text("Host Wi-Fi IP") },
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_client_host_ip"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UnoBlue,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedTextField(
                    value = roomCodeInput,
                    onValueChange = { roomCodeInput = it.take(6).uppercase() },
                    label = { Text("Room Code") },
                    singleLine = true,
                    modifier = Modifier
                      .weight(1f)
                      .testTag("input_client_room_code"),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = GoldAccent,
                      unfocusedBorderColor = Color.Gray,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    )
                  )

                  OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it.take(12) },
                    label = { Text("Password (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                      .weight(1.2f)
                      .testTag("input_client_password"),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = GoldAccent,
                      unfocusedBorderColor = Color.Gray,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    )
                  )
                }
              }

              if (errorMessage != null) {
                Text(
                  text = errorMessage ?: "",
                  color = UnoRed,
                  fontFamily = FredokaFontFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              GameButton(
                text = if (connectionState == ConnectionState.CONNECTING) "CONNECTING..." else "JOIN ROOM",
                enabled = connectionState != ConnectionState.CONNECTING && hostIpInput.isNotEmpty(),
                onClick = {
                  audioManager.playButtonClick()
                  clientConnection.connectToHost(
                    hostIp = hostIpInput.trim(),
                    roomCode = roomCodeInput.trim().uppercase(),
                    password = passwordInput.trim()
                  )
                },
                gradientColors = listOf(UnoBlue, Color(0xFF0284C7)),
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_submit_join"
              )
            }
          }

          // Right: QR Scan Option Card
          Card(
            modifier = Modifier
              .weight(0.9f)
              .fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(GoldAccent, UnoGreen))
            )
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.QrCodeScanner,
                  contentDescription = null,
                  tint = GoldAccent,
                  modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "SCAN HOST QR CODE",
                  fontFamily = FredokaFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = Color.White
                )
                Text(
                  text = "Join in seconds without typing IP addresses",
                  fontFamily = FredokaFontFamily,
                  fontSize = 12.sp,
                  color = Color(0xFF94A3B8),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }

              if (showQrInputBox) {
                OutlinedTextField(
                  value = qrCodePayloadInput,
                  onValueChange = { qrCodePayloadInput = it },
                  label = { Text("Paste/Enter QR URL payload", fontSize = 11.sp) },
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_qr_payload"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UnoGreen,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  )
                )
              }

              GameButton(
                text = if (showQrInputBox) "CONNECT VIA QR DATA" else "SCAN / PARSE QR",
                onClick = {
                  audioManager.playButtonClick()
                  if (!showQrInputBox) {
                    showQrInputBox = true
                  } else {
                    val parsed = LanQrPayload.parse(qrCodePayloadInput)
                    if (parsed != null) {
                      hostIpInput = parsed.hostIp
                      roomCodeInput = parsed.roomCode
                      passwordInput = parsed.password
                      clientConnection.connectToHost(
                        hostIp = parsed.hostIp,
                        port = parsed.port,
                        roomCode = parsed.roomCode,
                        password = parsed.password
                      )
                    }
                  }
                },
                gradientColors = listOf(UnoGreen, Color(0xFF047857)),
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_parse_qr"
              )
            }
          }
        }
      } else {
        // Connected: Live Join Waiting Room
        val room = roomState
        Card(
          modifier = Modifier
            .fillMaxWidth(0.85f)
            .align(Alignment.CenterHorizontally)
            .padding(vertical = 8.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(UnoGreen, GoldAccent))
          )
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "CONNECTED TO ROOM: ${room?.roomCode ?: "LAN"}",
                  fontFamily = FredokaFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = GoldAccent
                )
                Text(
                  text = "Entry: ${room?.entryFee ?: 50} Coins • Max Players: ${room?.maxPlayers ?: 2}",
                  fontFamily = FredokaFontFamily,
                  fontSize = 12.sp,
                  color = Color(0xFFE2E8F0)
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(UnoGreen)
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "CONNECTED",
                  fontFamily = FredokaFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = Color.Black
                )
              }
            }

            // Connected players list in waiting room
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Players in Room (${room?.players?.size ?: 1}/${room?.maxPlayers ?: 2}):",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
              )

              room?.players?.forEach { p ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF334155).copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = p.name, fontFamily = FredokaFontFamily, color = Color.White, fontSize = 13.sp)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = if (p.isReady) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                      contentDescription = null,
                      tint = if (p.isReady) UnoGreen else GoldAccent,
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (p.isReady) "READY" else "WAITING",
                      fontFamily = FredokaFontFamily,
                      fontSize = 11.sp,
                      color = if (p.isReady) UnoGreen else GoldAccent
                    )
                  }
                }
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              GameButton(
                text = "TOGGLE READY",
                onClick = {
                  audioManager.playButtonClick()
                  clientConnection.toggleReady()
                },
                gradientColors = listOf(GoldAccent, Color(0xFFD97706)),
                modifier = Modifier.weight(1f),
                testTag = "btn_client_toggle_ready"
              )

              GameButton(
                text = "LEAVE ROOM",
                onClick = {
                  audioManager.playButtonClick()
                  clientConnection.disconnect()
                  onBackClick()
                },
                gradientColors = listOf(UnoRed, Color(0xFF991B1B)),
                modifier = Modifier.weight(1f),
                testTag = "btn_client_leave_room"
              )
            }
          }
        }
      }

      // 3. BOTTOM FOOTER
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Local Wi-Fi Only • Host controls match start • Completely Offline",
          fontFamily = FredokaFontFamily,
          fontSize = 11.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }
  }
}
