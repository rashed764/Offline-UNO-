package com.example.ui.matchsetup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.CoinBadge
import com.example.ui.components.GameButton
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid
import com.example.ui.theme.UnoGreen

/**
 * Match Entry Selection screen allowing player to pick coin stakes.
 * Disables higher tiers if the player doesn't have sufficient coins.
 */
@Composable
fun MatchEntrySelectionScreen(
  isVsFriends: Boolean,
  coinRepository: CoinRepository,
  audioManager: AudioManager,
  onBackClick: () -> Unit,
  onProceedToPlayerCount: (entryFee: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val coins by coinRepository.coinBalance.collectAsState()
  val entryTiers = listOf(50, 100, 250, 500, 1000, 2500, 5000, 10000)
  var selectedEntry by remember { mutableIntStateOf(50) }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(TableFeltMid, TableFeltDark)
        )
      )
      .padding(horizontal = 24.dp, vertical = 14.dp)
      .testTag("match_entry_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
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
              .testTag("entry_back_button")
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
              text = if (isVsFriends) "VS FRIENDS • SELECT ENTRY" else "VS COMPUTER • SELECT ENTRY",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = "Higher stakes yield bigger reward pots!",
              fontFamily = FredokaFontFamily,
              fontSize = 12.sp,
              color = Color(0xFF94A3B8)
            )
          }
        }

        CoinBadge(coins = coins)
      }

      // Horizontal scrollable card selection for stakes
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Select Match Stakes:",
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          color = Color(0xFFFFD100)
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(entryTiers) { tier ->
            val canAfford = coins >= tier
            val isSelected = selectedEntry == tier

            EntryTierCard(
              tier = tier,
              canAfford = canAfford,
              isSelected = isSelected,
              onClick = {
                if (canAfford) {
                  audioManager.playButtonClick()
                  selectedEntry = tier
                }
              }
            )
          }
        }
      }

      // Bottom confirmation bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Selected Entry: $selectedEntry Coins",
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
          )
          Text(
            text = if (coins >= selectedEntry) "Ready to configure players" else "Not Enough Coins!",
            fontFamily = FredokaFontFamily,
            fontSize = 12.sp,
            color = if (coins >= selectedEntry) UnoGreen else Color(0xFFEF4444)
          )
        }

        GameButton(
          text = "NEXT: PLAYER COUNT",
          enabled = coins >= selectedEntry,
          onClick = {
            audioManager.playButtonClick()
            onProceedToPlayerCount(selectedEntry)
          },
          testTag = "btn_confirm_entry"
        )
      }
    }
  }
}

@Composable
private fun EntryTierCard(
  tier: Int,
  canAfford: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(16.dp)

  val bgModifier = when {
    !canAfford -> Modifier.background(Color(0xFF334155).copy(alpha = 0.5f))
    isSelected -> Modifier.background(
      Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F)))
    )
    else -> Modifier.background(Color(0xFF1E293B))
  }

  val borderModifier = when {
    isSelected -> Modifier.border(2.dp, GoldAccent, shape)
    !canAfford -> Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), shape)
    else -> Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), shape)
  }

  Card(
    shape = shape,
    modifier = Modifier
      .size(width = 110.dp, height = 120.dp)
      .shadow(if (isSelected) 8.dp else 2.dp, shape)
      .then(bgModifier)
      .then(borderModifier)
      .clickable(enabled = canAfford, onClick = onClick)
      .testTag("tier_card_$tier"),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      if (!canAfford) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Locked",
          tint = Color(0xFFEF4444),
          modifier = Modifier.size(20.dp)
        )
      } else {
        Icon(
          imageVector = Icons.Default.MonetizationOn,
          contentDescription = null,
          tint = GoldAccent,
          modifier = Modifier.size(22.dp)
        )
      }

      Text(
        text = "$tier",
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = if (canAfford) Color.White else Color.Gray
      )

      Text(
        text = if (canAfford) "COINS" else "NEED MORE",
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        color = if (canAfford) Color(0xFFFFD100) else Color(0xFFEF4444)
      )
    }
  }
}
