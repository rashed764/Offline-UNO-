package com.example.ui.game

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.UnoCard
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.UnoBlue
import com.example.ui.theme.UnoGreen
import com.example.ui.theme.UnoRed
import com.example.ui.theme.UnoYellow

/**
 * Polished, scalable, tactile UNO Playing Card component.
 * Features curved border, centered badge, dual corner indices, and touch elevation.
 */
@Composable
fun CardView(
  card: UnoCard,
  modifier: Modifier = Modifier,
  isPlayable: Boolean = true,
  isFaceDown: Boolean = false,
  elevationOffset: Dp = 0.dp,
  onClick: (() -> Unit)? = null
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "card_scale")
  val yOffset by animateDpAsState(targetValue = if (isPlayable && !isFaceDown) -elevationOffset else 0.dp, label = "card_y_offset")

  val shape = RoundedCornerShape(12.dp)

  val cardBg = if (isFaceDown) {
    Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
  } else {
    when (card.color) {
      CardColor.RED -> Brush.verticalGradient(listOf(Color(0xFFFF334B), UnoRed))
      CardColor.YELLOW -> Brush.verticalGradient(listOf(Color(0xFFFFE066), UnoYellow))
      CardColor.GREEN -> Brush.verticalGradient(listOf(Color(0xFF34D399), UnoGreen))
      CardColor.BLUE -> Brush.verticalGradient(listOf(Color(0xFF38BDF8), UnoBlue))
      CardColor.WILD -> Brush.sweepGradient(
        listOf(UnoRed, UnoYellow, UnoGreen, UnoBlue, UnoRed)
      )
    }
  }

  val textColor = if (card.color == CardColor.YELLOW) Color(0xFF1E293B) else Color.White

  Box(
    modifier = modifier
      .offset(y = yOffset)
      .scale(scale)
      .aspectRatio(0.68f) // Classic card ratio (2.5 x 3.5 inches approx)
      .shadow(elevation = if (isPlayable) 8.dp else 3.dp, shape = shape)
      .clip(shape)
      .background(cardBg)
      .border(
        width = if (isPlayable && !isFaceDown) 2.5.dp else 1.5.dp,
        color = if (isPlayable && !isFaceDown) Color.White else Color.White.copy(alpha = 0.35f),
        shape = shape
      )
      .then(
        if (onClick != null) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
          )
        } else Modifier
      )
      .testTag("card_${card.id}"),
    contentAlignment = Alignment.Center
  ) {
    if (isFaceDown) {
      // Card Back design
      Box(
        modifier = Modifier
          .fillMaxSize(0.85f)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.35f))
          .border(1.5.dp, Color(0xFFFFD100).copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "UNO",
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = Color(0xFFFFD100),
          modifier = Modifier.rotate(-25f)
        )
      }
    } else {
      // Center White Ellipse Oval
      Box(
        modifier = Modifier
          .fillMaxSize(0.82f)
          .rotate(-22f)
          .clip(RoundedCornerShape(32.dp))
          .background(Color.White.copy(alpha = if (card.color == CardColor.WILD) 0.88f else 0.95f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = card.value.symbol,
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = if (card.value.symbol.length > 1) 22.sp else 30.sp,
          color = when (card.color) {
            CardColor.RED -> UnoRed
            CardColor.YELLOW -> Color(0xFFD97706)
            CardColor.GREEN -> UnoGreen
            CardColor.BLUE -> UnoBlue
            CardColor.WILD -> Color.Black
          },
          modifier = Modifier.rotate(22f)
        )
      }

      // Top-Left corner small indicator
      Text(
        text = card.value.symbol,
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = textColor,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 6.dp, top = 4.dp)
      )

      // Bottom-Right corner small indicator
      Text(
        text = card.value.symbol,
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = textColor,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 6.dp, bottom = 4.dp)
          .rotate(180f)
      )
    }
  }
}
