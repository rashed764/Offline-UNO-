package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight

/**
 * Top HUD coin display showing current balance with gold badge and optional bonus action.
 */
@Composable
fun CoinBadge(
  coins: Int,
  modifier: Modifier = Modifier,
  onAddClick: (() -> Unit)? = null
) {
  val scaleAnim = remember { Animatable(1f) }

  LaunchedEffect(coins) {
    scaleAnim.animateTo(1.15f, tween(120, easing = FastOutSlowInEasing))
    scaleAnim.animateTo(1.0f, tween(120, easing = FastOutSlowInEasing))
  }

  val shape = RoundedCornerShape(24.dp)

  Box(
    modifier = modifier
      .scale(scaleAnim.value)
      .testTag("coin_badge")
      .shadow(4.dp, shape)
      .clip(shape)
      .background(
        Brush.horizontalGradient(
          listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        )
      )
      .border(1.5.dp, GoldAccent.copy(alpha = 0.6f), shape)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              listOf(GoldAccentLight, GoldAccent, Color(0xFFB45309))
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.MonetizationOn,
          contentDescription = "Coin",
          tint = Color.White,
          modifier = Modifier.size(22.dp)
        )
      }

      Text(
        text = String.format("%,d", coins),
        color = Color(0xFFFFE082),
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        modifier = Modifier.testTag("coin_balance_text")
      )

      if (onAddClick != null) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(GoldAccent)
            .clickable { onAddClick() }
            .testTag("claim_coins_bonus_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Claim Coins Bonus",
            tint = Color.Black,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
