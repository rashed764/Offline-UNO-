package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FredokaFontFamily

/**
 * Tactile, responsive game button with gradient sheen, 3D border, and press feedback.
 */
@Composable
fun GameButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  gradientColors: List<Color> = listOf(Color(0xFFFFB703), Color(0xFFFB8500)),
  textColor: Color = Color.White,
  testTag: String = ""
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "button_scale")

  val shape = RoundedCornerShape(20.dp)

  val activeBrush = if (enabled) {
    Brush.verticalGradient(gradientColors)
  } else {
    Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF334155)))
  }

  Box(
    modifier = modifier
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
      .scale(scale)
      .shadow(elevation = if (enabled) 6.dp else 2.dp, shape = shape)
      .clip(shape)
      .background(activeBrush)
      .border(
        width = 2.dp,
        color = if (enabled) Color.White.copy(alpha = 0.45f) else Color.Gray.copy(alpha = 0.2f),
        shape = shape
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick
      )
      .defaultMinSize(minHeight = 52.dp)
      .padding(horizontal = 24.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) textColor else Color.LightGray,
          modifier = Modifier
            .padding(end = 8.dp)
            .size(22.dp)
        )
      }
      Text(
        text = text,
        color = if (enabled) textColor else Color.LightGray,
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.5.sp
      )
    }
  }
}
