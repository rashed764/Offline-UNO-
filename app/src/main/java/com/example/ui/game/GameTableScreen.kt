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
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Report
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
import com.example.game.network.client.LanClientConnection
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
  lanClient: LanClientConnection? = null,
  modifier: Modifier = Modifier
) {
  val hostState by session.state.collectAsState()
  val clientState by lanClient?.gameState?.collectAsState() ?: remember { mutableStateOf(null) }
  val coins by coinRepository.coinBalance.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  // Track if coin settlement has already occurred for this round
  var hasSettledCoins by remember { mutableStateOf(false) }

  // Autonomous bot coordinator with lifecycle-safe lifecycle attachment (only runs if local host session has bots)
  DisposableEffect(session, lanClient) {
    var coordinator: BotCoordinator? = null
    if (lanClient == null) {
      coordinator = BotCoordinator(session = session, scope = coroutineScope)
      coordinator.start()
    }
    onDispose {
      coordinator?.stop()
    }
  }

  // Derive active game view properties whether acting as Local Host or LAN Client
  val isLanClientMode = lanClient != null
  val activeColor = if (isLanClientMode && clientState != null) clientState!!.activeColor else hostState.activeColor
  val topDiscard = if (isLanClientMode && clientState != null) clientState!!.topDiscard else hostState.topDiscard
  val drawPileCount = if (isLanClientMode && clientState != null) clientState!!.drawPileCount else hostState.drawPileCount
  val discardPileCount = if (isLanClientMode && clientState != null) clientState!!.discardPileCount else hostState.discardPileCount
  val direction = if (isLanClientMode && clientState != null) clientState!!.direction else hostState.direction
  val lastEventMessage = if (isLanClientMode && clientState != null) clientState!!.lastEventMessage else hostState.lastEventMessage
  val totalPot = if (isLanClientMode && clientState != null) clientState!!.totalPot else hostState.totalPot
  val pendingDrawnCardId = if (isLanClientMode && clientState != null) clientState!!.drawnCardPlayableId else hostState.drawnCardPlayableId

  val localPlayerId = if (isLanClientMode) (lanClient?.assignedPlayerId ?: "") else (hostState.localPlayer?.id ?: "player_human")
  val isLocalPlayerTurn = if (isLanClientMode && clientState != null) {
    clientState!!.currentTurnPlayerId == localPlayerId
  } else {
    hostState.isLocalPlayerTurn
  }

  val localHand = if (isLanClientMode && clientState != null) {
    clientState!!.clientHand
  } else {
    hostState.localPlayer?.hand ?: emptyList()
  }

  val localPlayerHasDeclaredUno = if (isLanClientMode && clientState != null) {
    clientState!!.players.find { it.id == localPlayerId }?.hasDeclaredUno ?: false
  } else {
    hostState.localPlayer?.hasDeclaredUno ?: false
  }

  val isRoundEnded = if (isLanClientMode && clientState != null) {
    clientState!!.isRoundEnded
  } else {
    hostState.status is GameStatus.RoundEnded
  }

  val winnerId = if (isLanClientMode && clientState != null) {
    clientState!!.winnerPlayerId
  } else {
    (hostState.status as? GameStatus.RoundEnded)?.winner?.id
  }

  val winnerName = if (isLanClientMode && clientState != null) {
    clientState!!.winnerPlayerName ?: "Winner"
  } else {
    (hostState.status as? GameStatus.RoundEnded)?.winner?.name ?: "Winner"
  }

  val isWaitingForWildColor = if (isLanClientMode && clientState != null) {
    clientState!!.isWaitingForWildColor && clientState!!.wildColorChooserPlayerId == localPlayerId
  } else {
    hostState.status is GameStatus.WaitingForWildColor && (hostState.status as GameStatus.WaitingForWildColor).player.id == localPlayerId
  }

  // Audio & Haptic cues on state transitions
  LaunchedEffect(topDiscard?.id) {
    if (topDiscard != null && !isRoundEnded) {
      audioManager.playCardPlay()
    }
  }

  LaunchedEffect(isLocalPlayerTurn) {
    if (isLocalPlayerTurn) {
      audioManager.playTurnAlert()
    }
  }

  // Handle victory sound & exact one-time coin settlement
  LaunchedEffect(isRoundEnded) {
    if (isRoundEnded && !hasSettledCoins) {
      hasSettledCoins = true
      audioManager.playWin()
      if (winnerId == localPlayerId) {
        coinRepository.awardCoins(totalPot)
      }
    }
  }

  // Opponents list
  val opponents: List<UnoPlayer> = if (isLanClientMode && clientState != null) {
    clientState!!.players.filter { it.id != localPlayerId }.map { p ->
      UnoPlayer(
        id = p.id,
        name = p.name,
        type = p.type,
        hand = List(p.cardCount) { UnoCard("dummy_${p.id}_$it", CardColor.RED, CardValue.ZERO) },
        hasDeclaredUno = p.hasDeclaredUno,
        isUnoVulnerable = p.isUnoVulnerable,
        isConnected = p.isConnected
      )
    }
  } else {
    hostState.players.filter { it.id != localPlayerId }
  }

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
              text = "POT: $totalPot COINS",
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
            val isTheirTurn = if (isLanClientMode && clientState != null) {
              clientState!!.currentTurnPlayerId == opp.id
            } else {
              hostState.currentPlayer?.id == opp.id
            }
            OpponentHudBadge(
              opponent = opp,
              isTurn = isTheirTurn,
              onCatchUno = {
                audioManager.playUnoPenalty()
                if (isLanClientMode) {
                  lanClient?.catchUnoPenalty(opp.id)
                } else {
                  session.catchUnoPenalty(localPlayerId, opp.id)
                }
              }
            )
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
              isPlayable = isLocalPlayerTurn && pendingDrawnCardId == null,
              modifier = Modifier
                .width(66.dp)
                .testTag("draw_pile"),
              onClick = {
                if (isLocalPlayerTurn && pendingDrawnCardId == null && !isRoundEnded) {
                  audioManager.playCardDraw()
                  if (isLanClientMode) {
                    lanClient?.drawCard()
                  } else {
                    session.drawCard(localPlayerId)
                  }
                }
              }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "DRAW ($drawPileCount)",
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              color = if (isLocalPlayerTurn && pendingDrawnCardId == null) GoldAccent else Color(0xFF94A3B8)
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
                  when (activeColor) {
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
                tint = if (activeColor == CardColor.YELLOW) Color.Black else Color.White,
                modifier = Modifier
                  .size(24.dp)
                  .rotate(if (direction == PlayDirection.CLOCKWISE) 0f else 180f)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = activeColor.displayName.uppercase(),
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White
            )
          }

          // Discard Pile (Top card)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val top = topDiscard
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
              text = "DISCARD ($discardPileCount)",
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
            if (!isLanClientMode && hostState.isBotTurn) {
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
              text = lastEventMessage,
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
        // Hand cards row
        Box(modifier = Modifier.weight(1f)) {
          PlayerHandRow(
            hand = localHand,
            topDiscard = topDiscard,
            activeColor = activeColor,
            isPlayerTurn = isLocalPlayerTurn,
            pendingDrawnCardId = pendingDrawnCardId,
            onCardClick = { card ->
              if (isLocalPlayerTurn) {
                audioManager.playCardPlay()
                if (isLanClientMode) {
                  lanClient?.playCard(card.id)
                } else {
                  session.playCard(localPlayerId, card.id)
                }
              }
            }
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Actions Column: PASS button (if drawn card held) + UNO Button & Turn Status
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom,
          modifier = Modifier.padding(bottom = 4.dp)
        ) {
          // If a drawn playable card is held, show PASS TURN button
          if (isLocalPlayerTurn && pendingDrawnCardId != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFEAB308), Color(0xFFCA8A04))))
                .clickable {
                  audioManager.playButtonClick()
                  if (isLanClientMode) {
                    lanClient?.passDrawnTurn()
                  } else {
                    session.passDrawnCard(localPlayerId)
                  }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("btn_pass_drawn_card"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "PASS TURN",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.Black
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
          }

          val canShoutUno = localHand.size <= 2 && !localPlayerHasDeclaredUno
          Box(
            modifier = Modifier
              .size(54.dp)
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
                audioManager.playUnoShout()
                if (isLanClientMode) {
                  lanClient?.declareUno()
                } else {
                  session.declareUno(localPlayerId)
                }
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
              isLocalPlayerTurn -> if (pendingDrawnCardId != null) "PLAY DRAWN OR PASS" else "YOUR TURN"
              !isLanClientMode && hostState.isBotTurn -> "${hostState.currentPlayer?.name?.uppercase()} THINKING..."
              else -> "WAITING..."
            },
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (isLocalPlayerTurn) UnoGreen else Color(0xFFFFD100)
          )
        }
      }
    }

    // Wild Color Picker Modal when player plays Wild
    if (isWaitingForWildColor) {
      WildColorPickerModal(
        onColorSelected = { selectedColor ->
          audioManager.playCardPlay()
          if (isLanClientMode) {
            lanClient?.chooseWildColor(selectedColor)
          } else {
            session.selectWildColor(selectedColor)
          }
        }
      )
    }

    // Win Modal Dialog
    if (isRoundEnded) {
      val isWinner = winnerId == localPlayerId

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
                "$winnerName finished first!"
              },
              fontFamily = FredokaFontFamily,
              fontSize = 15.sp,
              color = Color(0xFFE2E8F0)
            )

            Text(
              text = if (isWinner) "PRIZE: +$totalPot COINS" else "POT: $totalPot COINS",
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
  isTurn: Boolean,
  onCatchUno: () -> Unit = {}
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

    // UNO Penalty Catch Button: Visible whenever opponent holds 1 card and has NOT declared UNO
    if (opponent.cardCount == 1 && opponent.isUnoVulnerable) {
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(Brush.horizontalGradient(listOf(UnoRed, Color(0xFF991B1B))))
          .border(1.dp, GoldAccent, RoundedCornerShape(8.dp))
          .clickable { onCatchUno() }
          .padding(horizontal = 6.dp, vertical = 4.dp)
          .testTag("catch_uno_${opponent.id}"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = "Catch UNO Penalty",
            tint = Color(0xFFFFD100),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = "CATCH!",
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            color = Color.White
          )
        }
      }
    }
  }
}
