package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.game.model.CardColor
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.UnoBlue
import com.example.ui.theme.UnoGreen
import com.example.ui.theme.UnoRed
import com.example.ui.theme.UnoYellow

/**
 * Landscape Modal Dialog for selecting color after playing Wild or Wild +4.
 */
@Composable
fun WildColorPickerModal(
  onColorSelected: (CardColor) -> Unit
) {
  val colors = listOf(
    Pair(CardColor.RED, UnoRed),
    Pair(CardColor.YELLOW, UnoYellow),
    Pair(CardColor.GREEN, UnoGreen),
    Pair(CardColor.BLUE, UnoBlue)
  )

  Dialog(onDismissRequest = { /* Must select a color */ }) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      border = CardDefaults.outlinedCardBorder().copy(
        brush = Brush.horizontalGradient(
          listOf(UnoRed, UnoYellow, UnoGreen, UnoBlue)
        )
      ),
      modifier = Modifier
        .shadow(16.dp, RoundedCornerShape(24.dp))
        .testTag("wild_color_picker_modal")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "CHOOSE COLOR",
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp,
          color = Color.White
        )
        Text(
          text = "Select active color for the next player:",
          fontFamily = FredokaFontFamily,
          fontSize = 13.sp,
          color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          colors.forEach { (color, uiColor) ->
            Box(
              modifier = Modifier
                .size(68.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(uiColor)
                .border(2.5.dp, Color.White, CircleShape)
                .clickable { onColorSelected(color) }
                .testTag("color_choice_${color.name.lowercase()}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = color.displayName.take(1),
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (color == CardColor.YELLOW) Color(0xFF1E293B) else Color.White
              )
            }
          }
        }
      }
    }
  }
}
