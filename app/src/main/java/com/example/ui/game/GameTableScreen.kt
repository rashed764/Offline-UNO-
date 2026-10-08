package com.example.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AudioManager
import com.example.core.economy.CoinRepository
import com.example.game.bot.BotCoordinator
import com.example.game.engine.GameStatus
import com.example.game.engine.PlayDirection
import com.example.game.engine.UnoGameSession
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.PlayerType
import com.example.game.model.UnoCard
import com.example.game.model.UnoPlayer
import com.example.ui.components.CoinBadge
import com.example.ui.components.GameButton
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMid
import com.example.ui.theme.UnoBlue
import com.example.ui.theme.UnoGreen
import com.example.ui.theme.UnoRed
import com.example.ui.theme.UnoYellow

/**
 * Landscape Fullscreen Responsive Game Table for VS COMPUTER & multiplayer matches.
 * Automatically manages lifecycle-safe autonomous bot turns and coin settlement.
 */
@Composable
fun GameTableScreen(
  session: UnoGameSession,
  coinRepository: CoinRepository,
  audioManager: AudioManager,
  onLeaveGameClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val state by session.state.collectAsState()
  val coins by coinRepository.coinBalance.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  // Track if coin settlement has already occurred for this round
  var hasSettledCoins by remember { mutableStateOf(false) }

  // Autonomous bot coordinator with lifecycle-safe lifecycle attachment
  DisposableEffect(session) {
    val coordinator = BotCoordinator(session = session, scope = coroutineScope)
    coordinator.start()
    onDispose {
      coordinator.stop()
    }
  }

  // Audio & Haptic cues on state transitions
  LaunchedEffect(state.topDiscard?.id) {
    if (state.topDiscard != null && state.status is GameStatus.InProgress) {
      audioManager.playCardPlay()
    }
  }

  LaunchedEffect(state.currentTurnIndex) {
    if (state.isLocalPlayerTurn) {
      audioManager.playTurnAlert()
    }
  }

  // Handle victory sound & exact one-time coin settlement
  LaunchedEffect(state.status) {
    val status = state.status
    if (status is GameStatus.RoundEnded && !hasSettledCoins) {
      hasSettledCoins = true
      audioManager.playWin()
      val localPlayer = state.localPlayer
      if (status.winner.id == localPlayer?.id) {
        coinRepository.awardCoins(status.totalPot)
      }
    }
  }

  val localPlayer = state.localPlayer ?: UnoPlayer("p1", "You", PlayerType.HUMAN)
  val opponents = state.players.filter { it.id != localPlayer.id }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          listOf(Color(0xFF1E3A8A).copy(alpha = 0.5f), TableFeltMid, TableFeltDark),
          radius = 1200f
        )
      )
      .testTag("game_table_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. TOP BAR: Back button, Opponents Header, Match pot, Coin badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              audioManager.playButtonClick()
              onLeaveGameClick()
            },
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
              .testTag("table_leave_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Leave Match",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black.copy(alpha = 0.4f))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "POT: ${state.totalPot} COINS",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = GoldAccent
            )
          }
        }

        // Active Opponents Badges
        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          opponents.forEach { opp ->
            val isTheirTurn = state.currentPlayer?.id == opp.id
            OpponentHudBadge(opponent = opp, isTurn = isTheirTurn)
          }
        }

        CoinBadge(coins = coins)
      }

      // 2. CENTER ARENA: Draw pile, Active Color Orb, Discard pile, Direction
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(28.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Draw Pile
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CardView(
              card = UnoCard("draw_top", CardColor.WILD, CardValue.WILD),
              isFaceDown = true,
              isPlayable = state.isLocalPlayerTurn,
              modifier = Modifier
                .width(66.dp)
                .testTag("draw_pile"),
              onClick = {
                if (state.isLocalPlayerTurn && state.status is GameStatus.InProgress) {
                  audioManager.playCardDraw()
                  session.drawCard(localPlayer.id)
                }
              }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "DRAW (${state.drawPileCount})",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              color = if (state.isLocalPlayerTurn) GoldAccent else Color(0xFF94A3B8)
            )
          }

          // Active Color & Direction Indicator
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                  when (state.activeColor) {
                    CardColor.RED -> UnoRed
                    CardColor.YELLOW -> UnoYellow
                    CardColor.GREEN -> UnoGreen
                    CardColor.BLUE -> UnoBlue
                    CardColor.WILD -> Color.White
                  }
                )
                .border(2.5.dp, Color.White, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Cached,
                contentDescription = "Direction",
                tint = if (state.activeColor == CardColor.YELLOW) Color.Black else Color.White,
                modifier = Modifier
                  .size(24.dp)
                  .rotate(if (state.direction == PlayDirection.CLOCKWISE) 0f else 180f)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = state.activeColor.displayName.uppercase(),
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White
            )
          }

          // Discard Pile (Top card)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val top = state.topDiscard
            if (top != null) {
              CardView(
                card = top,
                isFaceDown = false,
                isPlayable = false,
                modifier = Modifier
                  .width(66.dp)
                  .testTag("top_discard_card")
              )
            } else {
              Box(
                modifier = Modifier
                  .width(66.dp)
                  .height(98.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color.White.copy(alpha = 0.1f))
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "DISCARD (${state.discardPileCount})",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
        }

        // Live Action ticker banner
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("action_ticker_banner")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.isBotTurn) {
              val infiniteTransition = rememberInfiniteTransition(label = "pulse")
              val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600), repeatMode = RepeatMode.Reverse),
                label = "bot_pulse"
              )
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(GoldAccent.copy(alpha = alpha))
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = state.lastEventMessage,
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp,
              color = Color.White
            )
          }
        }
      }

      // 3. BOTTOM REGION: Local Player Hand + UNO declaration button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Hand cards row (Disabled when it's bot's turn)
        Box(modifier = Modifier.weight(1f)) {
          PlayerHandRow(
            hand = localPlayer.hand,
            topDiscard = state.topDiscard,
            activeColor = state.activeColor,
            isPlayerTurn = state.isLocalPlayerTurn,
            onCardClick = { card ->
              if (state.isLocalPlayerTurn) {
                audioManager.playCardPlay()
                session.playCard(localPlayer.id, card.id)
              }
            }
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // UNO Button & Turn Status
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom
        ) {
          val canShoutUno = localPlayer.cardCount <= 2 && !localPlayer.hasDeclaredUno
          Box(
            modifier = Modifier
              .size(56.dp)
              .shadow(if (canShoutUno) 10.dp else 2.dp, CircleShape)
              .clip(CircleShape)
              .background(
                if (canShoutUno) {
                  Brush.radialGradient(listOf(UnoRed, Color(0xFF991B1B)))
                } else {
                  Brush.radialGradient(listOf(Color(0xFF475569), Color(0xFF334155)))
                }
              )
              .border(
                width = 2.dp,
                color = if (canShoutUno) GoldAccent else Color.White.copy(alpha = 0.2f),
                shape = CircleShape
              )
              .clickable(enabled = canShoutUno) {
                audioManager.playTurnAlert()
                session.declareUno(localPlayer.id)
              }
              .testTag("btn_declare_uno"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "UNO!",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              color = if (canShoutUno) Color(0xFFFFD100) else Color.LightGray
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = when {
              state.isLocalPlayerTurn -> "YOUR TURN"
              state.isBotTurn -> "${state.currentPlayer?.name?.uppercase()} THINKING..."
              else -> "WAITING..."
            },
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (state.isLocalPlayerTurn) UnoGreen else Color(0xFFFFD100)
          )
        }
      }
    }

    // Wild Color Picker Modal when human plays Wild
    if (state.status is GameStatus.WaitingForWildColor) {
      val waiting = state.status as GameStatus.WaitingForWildColor
      if (waiting.player.type == PlayerType.HUMAN) {
        WildColorPickerModal(
          onColorSelected = { selectedColor ->
            audioManager.playCardPlay()
            session.selectWildColor(selectedColor)
          }
        )
      }
    }

    // Win Modal Dialog
    if (state.status is GameStatus.RoundEnded) {
      val result = state.status as GameStatus.RoundEnded
      val isWinner = result.winner.id == localPlayer.id

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.75f))
          .testTag("game_result_dialog"),
        contentAlignment = Alignment.Center
      ) {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              listOf(GoldAccent, UnoGreen)
            )
          ),
          modifier = Modifier
            .fillMaxWidth(0.6f)
            .shadow(20.dp, RoundedCornerShape(24.dp))
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = if (isWinner) "🏆 VICTORY!" else "MATCH ENDED",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 28.sp,
              color = if (isWinner) GoldAccent else Color.White
            )

            Text(
              text = if (isWinner) {
                "You played all cards and won the match pot!"
              } else {
                "${result.winner.name} finished first!"
              },
              fontFamily = FredokaFontFamily,
              fontSize = 15.sp,
              color = Color(0xFFE2E8F0)
            )

            Text(
              text = if (isWinner) "PRIZE: +${result.totalPot} COINS" else "POT: ${result.totalPot} COINS",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = if (isWinner) Color(0xFFFFD100) else Color(0xFF94A3B8)
            )

            GameButton(
              text = "BACK TO LOBBY",
              onClick = {
                audioManager.playButtonClick()
                onLeaveGameClick()
              },
              testTag = "btn_victory_leave"
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OpponentHudBadge(
  opponent: UnoPlayer,
  isTurn: Boolean
) {
  val shape = RoundedCornerShape(12.dp)

  val borderModifier = if (isTurn) {
    Modifier.border(2.dp, GoldAccent, shape)
  } else {
    Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), shape)
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .shadow(if (isTurn) 8.dp else 2.dp, shape)
      .clip(shape)
      .background(
        if (isTurn) {
          Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8)))
        } else {
          Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
        }
      )
      .then(borderModifier)
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag("opponent_badge_${opponent.id}")
  ) {
    Icon(
      imageVector = Icons.Default.SmartToy,
      contentDescription = null,
      tint = if (isTurn) GoldAccent else Color.LightGray,
      modifier = Modifier.size(20.dp)
    )

    Spacer(modifier = Modifier.width(6.dp))

    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = opponent.name,
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = Color.White
        )
        if (isTurn) {
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "• PLAYING",
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = GoldAccent
          )
        }
      }
      Text(
        text = "${opponent.cardCount} cards ${if (opponent.cardCount == 1) "• UNO!" else ""}",
        fontFamily = FredokaFontFamily,
        fontSize = 11.sp,
        color = if (opponent.cardCount == 1) UnoRed else Color(0xFFFFD100)
      )
    }
  }
}
