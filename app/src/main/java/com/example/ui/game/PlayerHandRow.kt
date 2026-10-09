package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.game.model.CardColor
import com.example.game.model.UnoCard
import com.example.game.rules.UnoRulesEngine

/**
 * Responsive, overlap-capable player card hand.
 * Adapts dynamically to hand size so cards never leave the screen.
 */
@Composable
fun PlayerHandRow(
  hand: List<UnoCard>,
  topDiscard: UnoCard?,
  activeColor: CardColor,
  isPlayerTurn: Boolean,
  pendingDrawnCardId: String? = null,
  onCardClick: (UnoCard) -> Unit,
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .height(115.dp),
    contentAlignment = Alignment.Center
  ) {
    val totalCards = hand.size
    val availableWidth = maxWidth

    // Dynamic width per card based on count:
    // If cards fit easily, use comfortable 70.dp; if many cards, overlap intelligently
    val cardWidth = when {
      totalCards <= 7 -> 70.dp
      totalCards <= 12 -> 62.dp
      totalCards <= 18 -> 54.dp
      else -> 48.dp
    }

    val spacing = when {
      totalCards <= 7 -> 8.dp
      totalCards <= 10 -> (-10).dp
      totalCards <= 15 -> (-22).dp
      else -> (-30).dp
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(spacing),
      verticalAlignment = Alignment.Bottom,
      modifier = Modifier.fillMaxSize()
    ) {
      itemsIndexed(hand, key = { _, card -> card.id }) { index, card ->
        val isLegal = if (isPlayerTurn && topDiscard != null) {
          if (pendingDrawnCardId != null) {
            card.id == pendingDrawnCardId && UnoRulesEngine.isCardPlayable(card, topDiscard, activeColor)
          } else {
            UnoRulesEngine.isCardPlayable(card, topDiscard, activeColor)
          }
        } else false

        CardView(
          card = card,
          isPlayable = isLegal,
          isFaceDown = false,
          elevationOffset = if (isLegal) 14.dp else 0.dp,
          modifier = Modifier.width(cardWidth),
          onClick = {
            if (isPlayerTurn) {
              onCardClick(card)
            }
          }
        )
      }
    }
  }
}
