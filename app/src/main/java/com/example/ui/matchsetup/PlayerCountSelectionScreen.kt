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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.GameButton
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid

/**
 * Player count selection screen:
 * 1v1 (2 players), 1v1v1 (3 players), 1v1v1v1 (4 players).
 */
@Composable
fun PlayerCountSelectionScreen(
  isVsFriends: Boolean,
  entryFee: Int,
  audioManager: AudioManager,
  onBackClick: () -> Unit,
  onProceedToConfirmation: (playerCount: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedCount by remember { mutableIntStateOf(2) }

  val options = listOf(
    Triple(2, "1 VS 1", "2 Players (You vs 1 Opponent)"),
    Triple(3, "1 VS 1 VS 1", "3 Players (You vs 2 Opponents)"),
    Triple(4, "1 VS 1 VS 1 VS 1", "4 Players (You vs 3 Opponents)")
  )

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(TableFeltMid, TableFeltDark)
        )
      )
      .padding(horizontal = 24.dp, vertical = 14.dp)
      .testTag("player_count_screen")
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
              .testTag("player_count_back_button")
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
              text = if (isVsFriends) "VS FRIENDS • PLAYER COUNT" else "VS COMPUTER • PLAYER COUNT",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = "Entry Stake: $entryFee Coins",
              fontFamily = FredokaFontFamily,
              fontSize = 12.sp,
              color = Color(0xFFFFD100)
            )
          }
        }
      }

      // Three big choice cards in row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        options.forEach { (count, title, desc) ->
          val isSelected = selectedCount == count
          val shape = RoundedCornerShape(18.dp)

          Card(
            shape = shape,
            modifier = Modifier
              .weight(1f)
              .height(130.dp)
              .shadow(if (isSelected) 8.dp else 2.dp, shape)
              .background(
                if (isSelected) {
                  Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF172554)))
                } else {
                  Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                }
              )
              .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.2f),
                shape = shape
              )
              .clickable {
                audioManager.playButtonClick()
                selectedCount = count
              }
              .testTag("player_count_option_$count"),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceEvenly
            ) {
              Icon(
                imageVector = if (count == 2) Icons.Default.Person else Icons.Default.Group,
                contentDescription = null,
                tint = if (isSelected) GoldAccent else Color.LightGray,
                modifier = Modifier.size(28.dp)
              )

              Text(
                text = title,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White
              )

              Text(
                text = desc,
                fontFamily = FredokaFontFamily,
                fontSize = 11.sp,
                color = if (isSelected) Color(0xFFFFE082) else Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      }

      // Bottom bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Total Reward Pot: ${entryFee * selectedCount} Coins",
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFFFFD100)
          )
          Text(
            text = if (isVsFriends) "Host will wait for $selectedCount players" else "Bots fill remaining slots",
            fontFamily = FredokaFontFamily,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
          )
        }

        GameButton(
          text = "REVIEW SETUP",
          onClick = {
            audioManager.playButtonClick()
            onProceedToConfirmation(selectedCount)
          },
          testTag = "btn_proceed_confirmation"
        )
      }
    }
  }
}
