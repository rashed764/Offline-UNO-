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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.CoinBadge
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid
import com.example.ui.theme.UnoBlue
import com.example.ui.theme.UnoGreen

/**
 * Menu presented when player selects VS FRIENDS:
 * Gives choice between "HOST THE GAME" and "JOIN THE GAME".
 */
@Composable
fun VsFriendsModeSelectionScreen(
  entryFee: Int,
  playerCount: Int,
  coinRepository: CoinRepository,
  audioManager: AudioManager,
  onBackClick: () -> Unit,
  onHostClick: () -> Unit,
  onJoinClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val coins by coinRepository.coinBalance.collectAsState()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(TableFeltMid, TableFeltDark)))
      .padding(horizontal = 24.dp, vertical = 14.dp)
      .testTag("vs_friends_mode_selection_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              audioManager.playButtonClick()
              onBackClick()
            },
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
              .testTag("vs_friends_back_button")
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
              text = "VS FRIENDS • LOCAL WI-FI",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = "Stakes: $entryFee Coins • Room Size: $playerCount Players",
              fontFamily = FredokaFontFamily,
              fontSize = 12.sp,
              color = Color(0xFFFFD100)
            )
          }
        }

        CoinBadge(coins = coins)
      }

      // Middle: Two Big Cards (Host vs Join)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Option 1: HOST THE GAME
        Card(
          onClick = {
            audioManager.playButtonClick()
            onHostClick()
          },
          modifier = Modifier
            .weight(1f)
            .height(160.dp)
            .testTag("btn_select_host_game"),
          shape = RoundedCornerShape(22.dp),
          elevation = CardDefaults.cardElevation(8.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF172554)))
              )
              .border(2.dp, GoldAccent, RoundedCornerShape(22.dp))
              .padding(18.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceEvenly
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Cast, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(28.dp))
              }

              Text(
                text = "HOST THE GAME",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
              )

              Text(
                text = "Create room, generate QR code, and invite friends on the same Wi-Fi network",
                fontFamily = FredokaFontFamily,
                fontSize = 11.sp,
                color = Color(0xFFE2E8F0),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp
              )
            }
          }
        }

        // Option 2: JOIN THE GAME
        Card(
          onClick = {
            audioManager.playButtonClick()
            onJoinClick()
          },
          modifier = Modifier
            .weight(1f)
            .height(160.dp)
            .testTag("btn_select_join_game"),
          shape = RoundedCornerShape(22.dp),
          elevation = CardDefaults.cardElevation(8.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(listOf(Color(0xFF0F766E), Color(0xFF115E59)))
              )
              .border(2.dp, UnoGreen, RoundedCornerShape(22.dp))
              .padding(18.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceEvenly
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(28.dp))
              }

              Text(
                text = "JOIN THE GAME",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
              )

              Text(
                text = "Join using a 6-character room code, password, or by scanning host QR code",
                fontFamily = FredokaFontFamily,
                fontSize = 11.sp,
                color = Color(0xFFE2E8F0),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp
              )
            }
          }
        }
      }

      // Footer reassurance
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Zero Internet Required • Fast Local Peer-to-Peer LAN Connection",
          fontFamily = FredokaFontFamily,
          fontSize = 11.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }
  }
}
