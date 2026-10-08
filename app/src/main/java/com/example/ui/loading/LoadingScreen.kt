package com.example.ui.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.UnoBlue
import com.example.ui.theme.UnoGreen
import com.example.ui.theme.UnoRed
import com.example.ui.theme.UnoYellow
import kotlinx.coroutines.delay

/**
 * Landscape-optimized Loading Screen for Offline UNO!.
 * Automatically loads save data, initializes audio/economy, and transitions to Lobby.
 */
@Composable
fun LoadingScreen(
  onLoadingFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "cards_orbit"
  )

  LaunchedEffect(Unit) {
    delay(1400) // Brief startup animation before auto-transitioning
    onLoadingFinished()
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          colors = listOf(Color(0xFF1E1B4B), TableFeltDark),
          radius = 1200f
        )
      )
      .testTag("loading_screen"),
    contentAlignment = Alignment.Center
  ) {
    val screenHeight = maxHeight
    val logoSize = (screenHeight * 0.38f).coerceIn(90.dp, 160.dp)

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(16.dp)
    ) {
      Box(
        modifier = Modifier.size(logoSize + 36.dp),
        contentAlignment = Alignment.Center
      ) {
        // Orbiting color dots representing the 4 UNO card colors
        Box(
          modifier = Modifier
            .size(logoSize + 32.dp)
            .rotate(rotation)
        ) {
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .size(14.dp)
              .clip(CircleShape)
              .background(UnoRed)
          )
          Box(
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .size(14.dp)
              .clip(CircleShape)
              .background(UnoYellow)
          )
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .size(14.dp)
              .clip(CircleShape)
              .background(UnoGreen)
          )
          Box(
            modifier = Modifier
              .align(Alignment.CenterStart)
              .size(14.dp)
              .clip(CircleShape)
              .background(UnoBlue)
          )
        }

        // Center Logo
        Box(
          modifier = Modifier
            .size(logoSize)
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(2.5.dp, GoldAccent, RoundedCornerShape(28.dp))
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_uno_logo),
            contentDescription = "Offline UNO! Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Offline UNO!",
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = Color.White,
        letterSpacing = 2.sp
      )

      Text(
        text = "VS COMPUTER & LAN WI-FI MULTIPLAYER",
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = Color(0xFFFFD100),
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(18.dp))

      Box(
        modifier = Modifier
          .width(200.dp)
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Color.White.copy(alpha = 0.15f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
              Brush.horizontalGradient(
                listOf(UnoRed, UnoYellow, UnoGreen, UnoBlue)
              )
            )
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Preparing 108-card deck & local systems...",
        fontFamily = FredokaFontFamily,
        fontSize = 12.sp,
        color = Color(0xFF94A3B8)
      )
    }
  }
}
