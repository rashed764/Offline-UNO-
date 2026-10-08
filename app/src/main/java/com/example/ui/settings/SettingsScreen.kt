package com.example.ui.settings

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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.persistence.PreferencesManager
import com.example.ui.components.GameButton
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid

/**
 * Landscape Settings Screen for persistent audio & vibration toggles in Offline UNO!.
 */
@Composable
fun SettingsScreen(
  preferencesManager: PreferencesManager,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isMusicEnabled by remember { mutableStateOf(preferencesManager.isMusicEnabled) }
  var isSfxEnabled by remember { mutableStateOf(preferencesManager.isSfxEnabled) }
  var isVibrationEnabled by remember { mutableStateOf(preferencesManager.isVibrationEnabled) }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(TableFeltMid, TableFeltDark)
        )
      )
      .padding(horizontal = 24.dp, vertical = 16.dp)
      .testTag("settings_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .testTag("settings_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Lobby",
            tint = Color.White
          )
        }

        Text(
          text = "GAME SETTINGS",
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp,
          color = Color.White
        )

        // Balanced spacer for alignment
        Spacer(modifier = Modifier.size(44.dp))
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Settings Card in landscape split layout
      Card(
        modifier = Modifier
          .fillMaxWidth(0.82f)
          .weight(1f),
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            listOf(GoldAccent.copy(alpha = 0.5f), Color.White.copy(alpha = 0.2f))
          )
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left Column: Toggles
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            SettingToggleItem(
              icon = Icons.Default.MusicNote,
              title = "Background Music",
              subtitle = "Calm lobby atmosphere",
              checked = isMusicEnabled,
              testTag = "toggle_music",
              onCheckedChange = {
                isMusicEnabled = it
                preferencesManager.isMusicEnabled = it
              }
            )

            SettingToggleItem(
              icon = Icons.Default.GraphicEq,
              title = "Sound Effects",
              subtitle = "Card flip, draw & alert sounds",
              checked = isSfxEnabled,
              testTag = "toggle_sfx",
              onCheckedChange = {
                isSfxEnabled = it
                preferencesManager.isSfxEnabled = it
              }
            )

            SettingToggleItem(
              icon = Icons.Default.Vibration,
              title = "Haptic Vibration",
              subtitle = "Tactile feedback on card play",
              checked = isVibrationEnabled,
              testTag = "toggle_vibration",
              onCheckedChange = {
                isVibrationEnabled = it
                preferencesManager.isVibrationEnabled = it
              }
            )
          }

          Spacer(modifier = Modifier.width(32.dp))

          // Right Column: Info & reset/bonus
          Column(
            modifier = Modifier.weight(0.7f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "Offline UNO! v1.0",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Classic Offline & LAN Card Game\nNo Internet Required",
              fontFamily = FredokaFontFamily,
              fontSize = 13.sp,
              color = Color(0xFF94A3B8),
              lineHeight = 16.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            GameButton(
              text = "SAVE & CLOSE",
              onClick = onBackClick,
              gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
              testTag = "save_settings_button"
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SettingToggleItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  testTag: String,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF334155)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (checked) GoldAccent else Color.LightGray,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = title,
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = Color.White
        )
        Text(
          text = subtitle,
          fontFamily = FredokaFontFamily,
          fontSize = 12.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = GoldAccent,
        uncheckedThumbColor = Color.LightGray,
        uncheckedTrackColor = Color(0xFF475569)
      )
    )
  }
}
