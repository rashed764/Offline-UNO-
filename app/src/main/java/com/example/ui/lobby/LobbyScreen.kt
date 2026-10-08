package com.example.ui.lobby

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.audio.AudioManager
import com.example.core.economy.CoinRepository
import com.example.ui.components.CoinBadge
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid
import com.example.ui.theme.UnoBlue
import com.example.ui.theme.UnoRed
import kotlinx.coroutines.launch

/**
 * Landscape-first, responsive Main Lobby for Offline UNO!.
 * Displays coin balance, brand new UNO hero artwork banner, VS Computer, VS Friends, and Settings.
 */
@Composable
fun LobbyScreen(
  coinRepository: CoinRepository,
  audioManager: AudioManager,
  onVsComputerClick: () -> Unit,
  onVsFriendsClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val coins by coinRepository.coinBalance.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(TableFeltMid, TableFeltDark)
        )
      )
      .testTag("main_lobby_screen")
  ) {
    // Subtle background card table texture/vignette
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF2E1065).copy(alpha = 0.35f), Color.Transparent),
            radius = 1000f
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. TOP BAR: Branding, Coin Badge, and Settings
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .shadow(6.dp, RoundedCornerShape(12.dp))
              .clip(RoundedCornerShape(12.dp))
              .border(1.5.dp, GoldAccent, RoundedCornerShape(12.dp))
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_uno_logo),
              contentDescription = "Offline UNO! Logo",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "Offline UNO!",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White,
              letterSpacing = 1.sp
            )
            Text(
              text = "CLASSIC CARD GAME",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
              color = Color(0xFFFFD100)
            )
          }
        }

        // Action controls: Coin balance with claim button + Settings icon
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          CoinBadge(
            coins = coins,
            onAddClick = {
              audioManager.playButtonClick()
              coinRepository.claimFreeDailyBonus()
              scope.launch {
                snackbarHostState.showSnackbar("Claimed +100 Coins Bonus!")
              }
            }
          )

          IconButton(
            onClick = {
              audioManager.playButtonClick()
              onSettingsClick()
            },
            modifier = Modifier
              .size(44.dp)
              .shadow(4.dp, RoundedCornerShape(14.dp))
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
              .testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = Color.White
            )
          }
        }
      }

      // 2. MAIN CENTER CONTENT: Adaptive 2-Column Split (UNO Hero Banner & Game Modes)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Column: Artwork Hero Card with brand new UNO hero graphic
        Card(
          modifier = Modifier
            .weight(1.1f)
            .fillMaxHeight(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          elevation = CardDefaults.cardElevation(8.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              listOf(Color.White.copy(alpha = 0.25f), GoldAccent.copy(alpha = 0.4f))
            )
          )
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              painter = painterResource(id = R.drawable.img_uno_hero),
              contentDescription = "Offline UNO Game Arena Art",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )

            // Gradient vignette for text readability
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.88f)),
                    startY = 100f
                  )
                )
            )

            Column(
              modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
            ) {
              Text(
                text = "Classic UNO Card Battles",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
              )
              Text(
                text = "Match colors and numbers, unleash Skip, Reverse & +4 power cards! Play solo against smart bots or host a local Wi-Fi room.",
                fontFamily = FredokaFontFamily,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 15.sp
              )
            }
          }
        }

        // Right Column: Big Action Buttons for Game Modes
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.SpaceEvenly,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Mode 1: VS COMPUTER
          ModeCardButton(
            title = "VS COMPUTER",
            subtitle = "Play offline against smart bots",
            badge = "100% OFFLINE",
            icon = Icons.Default.Computer,
            gradientColors = listOf(UnoRed, Color(0xFFC026D3)),
            testTag = "btn_vs_computer",
            onClick = {
              audioManager.playButtonClick()
              onVsComputerClick()
            }
          )

          // Mode 2: VS FRIENDS (LAN)
          ModeCardButton(
            title = "VS FRIENDS",
            subtitle = "Host or Join over Wi-Fi / Hotspot",
            badge = "LOCAL LAN / NO INTERNET",
            icon = Icons.Default.Wifi,
            gradientColors = listOf(UnoBlue, Color(0xFF0D9488)),
            testTag = "btn_vs_friends",
            onClick = {
              audioManager.playButtonClick()
              onVsFriendsClick()
            }
          )
        }
      }

      // 3. BOTTOM FOOTER BAR: Offline status reassurance
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.People,
          contentDescription = null,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Zero Internet Required • Fully Local & Private • Instant Wi-Fi Matchmaking",
          fontFamily = FredokaFontFamily,
          fontSize = 12.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }

    // Snackbar for user feedback
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 24.dp)
    )
  }
}

@Composable
private fun ModeCardButton(
  title: String,
  subtitle: String,
  badge: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  gradientColors: List<Color>,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .height(84.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(6.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.horizontalGradient(gradientColors))
        .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
        .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(26.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = title,
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 19.sp,
              color = Color.White
            )
            Text(
              text = subtitle,
              fontFamily = FredokaFontFamily,
              fontSize = 12.sp,
              color = Color.White.copy(alpha = 0.85f)
            )
          }
        }

        // Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = badge,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = Color(0xFFFFD100)
          )
        }
      }
    }
  }
}
