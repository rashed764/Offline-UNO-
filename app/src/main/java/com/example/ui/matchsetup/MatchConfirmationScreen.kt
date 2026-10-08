package com.example.ui.matchsetup

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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.GameButton
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid
import com.example.ui.theme.UnoGreen

/**
 * Match Setup Confirmation Screen.
 * Summarizes the chosen parameters and launches the match!
 */
@Composable
fun MatchConfirmationScreen(
  isVsFriends: Boolean,
  entryFee: Int,
  playerCount: Int,
  audioManager: AudioManager,
  onBackClick: () -> Unit,
  onStartGameClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val totalPot = entryFee * playerCount

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(TableFeltMid, TableFeltDark)
        )
      )
      .padding(horizontal = 24.dp, vertical = 14.dp)
      .testTag("match_confirmation_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top bar
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
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF1E293B))
              .testTag("confirm_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Text(
            text = "MATCH SETUP SUMMARY",
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
          )
        }
      }

      // Summary Card
      Card(
        modifier = Modifier
          .fillMaxWidth(0.75f)
          .weight(1f)
          .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            listOf(GoldAccent.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f))
          )
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
          verticalArrangement = Arrangement.SpaceEvenly
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Game Mode:", fontFamily = FredokaFontFamily, color = Color(0xFF94A3B8), fontSize = 15.sp)
            Text(
              if (isVsFriends) "VS FRIENDS (LAN Room)" else "VS COMPUTER (Offline)",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 15.sp
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Entry Stake:", fontFamily = FredokaFontFamily, color = Color(0xFF94A3B8), fontSize = 15.sp)
            Text("$entryFee Coins", fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 15.sp)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Total Players:", fontFamily = FredokaFontFamily, color = Color(0xFF94A3B8), fontSize = 15.sp)
            Text("$playerCount Players", fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Winner Pot:", fontFamily = FredokaFontFamily, color = Color(0xFF94A3B8), fontSize = 15.sp)
            Text("$totalPot Coins", fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, color = UnoGreen, fontSize = 16.sp)
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF334155).copy(alpha = 0.6f))
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Official UNO rules: 7 cards dealt, match colors & numbers, play Reverse, Skip & Wild +4!",
              fontFamily = FredokaFontFamily,
              fontSize = 11.sp,
              color = Color(0xFFE2E8F0)
            )
          }
        }
      }

      // Bottom action: START MATCH!
      GameButton(
        text = "START MATCH",
        icon = Icons.Default.Casino,
        onClick = {
          audioManager.playButtonClick()
          onStartGameClick()
        },
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
        testTag = "btn_start_match"
      )
    }
  }
}
