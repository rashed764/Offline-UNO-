package com.example.ui.lan

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AudioManager
import com.example.core.economy.CoinRepository
import com.example.core.persistence.PreferencesManager
import com.example.game.network.core.LanNetworkUtils
import com.example.game.network.host.LanHostServer
import com.example.game.network.qr.QrCodeGenerator
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
 * Polished, landscape-native Host Waiting Room screen.
 * Displays:
 * - Room code (e.g. "AB7K9P")
 * - Password protection status
 * - High-contrast QR code
 * - Connected players list with ready indicators
 * - Start Game button (enabled only when >= 2 players and ready)
 */
@Composable
fun HostWaitingRoomScreen(
  entryFee: Int,
  maxPlayers: Int,
  coinRepository: CoinRepository,
  preferencesManager: PreferencesManager,
  audioManager: AudioManager,
  onBackClick: () -> Unit,
  onStartMatch: (server: LanHostServer) -> Unit,
  modifier: Modifier = Modifier
) {
  val coins by coinRepository.coinBalance.collectAsState()
  val scope = rememberCoroutineScope()

  var passwordInput by remember { mutableStateOf("") }
  var isServerCreated by remember { mutableStateOf(false) }

  val localIp = remember { LanNetworkUtils.getLocalIpv4Address() ?: "192.168.1.100" }

  var hostServer by remember {
    mutableStateOf<LanHostServer?>(null)
  }

  var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      hostServer?.stopServer()
    }
  }

  val players = hostServer?.connectedPlayers?.collectAsState()?.value ?: emptyList()
  val canStart = players.size >= 2 && players.all { it.isReady }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(TableFeltMid, TableFeltDark)))
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag("host_waiting_room_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. TOP HEADER: Back, Title, Entry stakes, Coin Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              audioManager.playButtonClick()
              hostServer?.stopServer()
              onBackClick()
            },
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
              .testTag("host_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Leave Room",
              tint = Color.White
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "HOST THE GAME • WAITING ROOM",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = "Stakes: $entryFee Coins • Pot: ${entryFee * maxPlayers} Coins",
              fontFamily = FredokaFontFamily,
              fontSize = 12.sp,
              color = Color(0xFFFFD100)
            )
          }
        }

        CoinBadge(coins = coins)
      }

      // 2. MAIN SPLIT CONTENT:
      if (!isServerCreated) {
        // Initial Room Configuration before opening listener
        Card(
          modifier = Modifier
            .fillMaxWidth(0.7f)
            .align(Alignment.CenterHorizontally)
            .padding(vertical = 12.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(GoldAccent, UnoGreen))
          )
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = "SET UP YOUR LAN ROOM",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )

            Text(
              text = "Optionally set a password so only invited friends can join over Wi-Fi:",
              fontFamily = FredokaFontFamily,
              fontSize = 13.sp,
              color = Color(0xFF94A3B8),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedTextField(
              value = passwordInput,
              onValueChange = { passwordInput = it.take(12) },
              label = { Text("Room Password (Optional)") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldAccent) },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth(0.85f)
                .testTag("input_host_password"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              )
            )

            GameButton(
              text = "CREATE & OPEN ROOM",
              onClick = {
                audioManager.playButtonClick()
                val server = LanHostServer(
                  hostPlayerName = preferencesManager.playerName,
                  password = passwordInput.trim(),
                  maxPlayers = maxPlayers,
                  entryFee = entryFee,
                  scope = scope
                )
                server.startServer()
                hostServer = server

                val qrPayload = server.getQrPayload(localIp)
                qrBitmap = QrCodeGenerator.generateQrBitmap(qrPayload.toUriString(), sizePixels = 240)
                isServerCreated = true
              },
              gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
              testTag = "btn_create_open_room"
            )
          }
        }
      } else {
        // Active Host Waiting Room (QR Code on Left, Player list on Right)
        val server = hostServer!!

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          // Left: QR Code Card & Room Credentials
          Card(
            modifier = Modifier
              .weight(1f)
              .fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(UnoBlue, GoldAccent))
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              if (qrBitmap != null) {
                Box(
                  modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "LAN Join QR Code",
                    modifier = Modifier.fillMaxSize()
                  )
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                  Text("ROOM CODE", fontFamily = FredokaFontFamily, fontSize = 11.sp, color = Color(0xFF94A3B8))
                  Text(
                    text = server.roomCode,
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = GoldAccent,
                    letterSpacing = 2.sp
                  )
                }

                if (passwordInput.isNotEmpty()) {
                  Column {
                    Text("PASSWORD", fontFamily = FredokaFontFamily, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                      text = passwordInput,
                      fontFamily = FredokaFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                      color = Color.White
                    )
                  }
                }

                Column {
                  Text("LOCAL IP", fontFamily = FredokaFontFamily, fontSize = 10.sp, color = Color(0xFF94A3B8))
                  Text(text = localIp, fontFamily = FredokaFontFamily, fontSize = 12.sp, color = Color(0xFFE2E8F0))
                }
              }
            }
          }

          // Right: Connected Players List & Status
          Card(
            modifier = Modifier
              .weight(1.1f)
              .fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(UnoGreen, Color(0xFF047857)))
            )
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "CONNECTED PLAYERS (${players.size}/$maxPlayers)",
                  fontFamily = FredokaFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Color.White
                )

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (canStart) UnoGreen else Color(0xFF475569))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = if (canStart) "READY TO START" else "WAITING FOR OPPONENT",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (canStart) Color.Black else Color.White
                  )
                }
              }

              // Player slots list
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until maxPlayers) {
                  val player = players.getOrNull(i)
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (player != null) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.4f))
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(if (player != null && player.isConnected) UnoGreen else Color.Gray)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text = player?.let { "${it.name} ${if (it.id == server.hostPlayerId) "(Host)" else ""}" } ?: "Empty Slot ${i + 1}",
                        fontFamily = FredokaFontFamily,
                        fontWeight = if (player != null) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (player != null) Color.White else Color.Gray
                      )
                    }

                    if (player != null) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = if (player.isReady) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                          contentDescription = null,
                          tint = if (player.isReady) UnoGreen else GoldAccent,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = if (player.isReady) "READY" else "WAITING",
                          fontFamily = FredokaFontFamily,
                          fontSize = 11.sp,
                          color = if (player.isReady) UnoGreen else GoldAccent
                        )
                      }
                    }
                  }
                }
              }

              // Start Game action
              GameButton(
                text = "START LAN MATCH",
                enabled = canStart,
                icon = Icons.Default.PlayArrow,
                onClick = {
                  audioManager.playButtonClick()
                  server.startGame()
                  onStartMatch(server)
                },
                gradientColors = listOf(UnoGreen, Color(0xFF059669)),
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_start_lan_match"
              )
            }
          }
        }
      }

      // 3. BOTTOM FOOTER BAR
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Local Wi-Fi Only • Friends scan QR or type room code • Zero Internet Required",
          fontFamily = FredokaFontFamily,
          fontSize = 11.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }
  }
}
