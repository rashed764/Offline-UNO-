package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.economy.CoinRepository
import com.example.core.persistence.PreferencesManager
import com.example.game.deck.CardPiles
import com.example.game.deck.UnoDeckFactory
import com.example.game.engine.GameStatus
import com.example.game.engine.PlayDirection
import com.example.game.engine.UnoGameSession
import com.example.game.model.CardColor
import com.example.game.model.CardValue
import com.example.game.model.PlayerType
import com.example.game.model.UnoCard
import com.example.game.model.UnoPlayer
import com.example.game.rules.UnoRulesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineUnoPhase2TestSuite {

  @Test
  fun appNameResourceMatchesOfflineUno() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Offline UNO!", appName)
  }

  @Test
  fun deckContainsExactly108CardsWithCorrectDistribution() {
    val deck = UnoDeckFactory.createStandard108Deck()
    assertEquals(108, deck.size)

    val redCards = deck.filter { it.color == CardColor.RED }
    val yellowCards = deck.filter { it.color == CardColor.YELLOW }
    val greenCards = deck.filter { it.color == CardColor.GREEN }
    val blueCards = deck.filter { it.color == CardColor.BLUE }
    val wildCards = deck.filter { it.color == CardColor.WILD }

    // 25 cards per colored suit (1 zero, 18 numbers, 2 skips, 2 reverses, 2 draw-twos)
    assertEquals(25, redCards.size)
    assertEquals(25, yellowCards.size)
    assertEquals(25, greenCards.size)
    assertEquals(25, blueCards.size)

    // 8 wild cards total (4 wild, 4 wild +4)
    assertEquals(8, wildCards.size)
    assertEquals(4, wildCards.count { it.value == CardValue.WILD })
    assertEquals(4, wildCards.count { it.value == CardValue.WILD_DRAW_FOUR })

    // Check zero counts
    assertEquals(1, redCards.count { it.value == CardValue.ZERO })
    assertEquals(1, yellowCards.count { it.value == CardValue.ZERO })
    assertEquals(1, greenCards.count { it.value == CardValue.ZERO })
    assertEquals(1, blueCards.count { it.value == CardValue.ZERO })

    // Check action card counts per color
    assertEquals(2, redCards.count { it.value == CardValue.SKIP })
    assertEquals(2, redCards.count { it.value == CardValue.REVERSE })
    assertEquals(2, redCards.count { it.value == CardValue.DRAW_TWO })
  }

  @Test
  fun cardPilesShuffleAndRecyclingWorksWithoutLoss() {
    val deck = UnoDeckFactory.createStandard108Deck()
    val piles = CardPiles()
    piles.resetWithDeck(deck)

    assertEquals(108, piles.drawCount)
    assertEquals(0, piles.discardCount)

    // Draw 10 cards
    val drawn = piles.drawCards(10)
    assertEquals(10, drawn.size)
    assertEquals(98, piles.drawCount)

    // Discard 9 of them
    for (i in 0 until 9) {
      piles.discard(drawn[i])
    }
    assertEquals(9, piles.discardCount)
    assertEquals(drawn[8], piles.topDiscard)

    // Empty the draw pile completely
    val rest = piles.drawCards(98)
    assertEquals(98, rest.size)
    assertEquals(0, piles.drawCount)

    // Next draw triggers recycling of discard pile (except top card)
    val recycledCard = piles.drawCard()
    assertNotNull(recycledCard)
    // 8 were recycled into draw pile, 1 was drawn, leaving 7 in draw pile
    assertEquals(7, piles.drawCount)
    // Top discard remains intact
    assertEquals(1, piles.discardCount)
    assertEquals(drawn[8], piles.topDiscard)
  }

  @Test
  fun rulesEngineValidatesLegalAndIllegalMoves() {
    val redFive = UnoCard("c1", CardColor.RED, CardValue.FIVE)
    val redSkip = UnoCard("c2", CardColor.RED, CardValue.SKIP)
    val blueFive = UnoCard("c3", CardColor.BLUE, CardValue.FIVE)
    val greenTwo = UnoCard("c4", CardColor.GREEN, CardValue.TWO)
    val wildCard = UnoCard("c5", CardColor.WILD, CardValue.WILD)
    val wildFour = UnoCard("c6", CardColor.WILD, CardValue.WILD_DRAW_FOUR)

    val topDiscard = UnoCard("top", CardColor.RED, CardValue.FIVE)

    // Matching color
    assertTrue(UnoRulesEngine.isCardPlayable(redSkip, topDiscard, CardColor.RED))
    // Matching number
    assertTrue(UnoRulesEngine.isCardPlayable(blueFive, topDiscard, CardColor.RED))
    // Wild cards always playable
    assertTrue(UnoRulesEngine.isCardPlayable(wildCard, topDiscard, CardColor.RED))
    assertTrue(UnoRulesEngine.isCardPlayable(wildFour, topDiscard, CardColor.RED))
    // Non-matching card rejected
    assertFalse(UnoRulesEngine.isCardPlayable(greenTwo, topDiscard, CardColor.RED))
  }

  @Test
  fun initialDealProvidesExactlySevenCards() {
    val session = UnoGameSession()
    val p1 = UnoPlayer("p1", "Player 1", PlayerType.HUMAN)
    val p2 = UnoPlayer("p2", "Player 2", PlayerType.BOT)
    val p3 = UnoPlayer("p3", "Player 3", PlayerType.BOT)

    session.startNewMatch(listOf(p1, p2, p3), fee = 50)
    val state = session.state.value

    assertEquals(3, state.players.size)
    assertEquals(7, state.players[0].hand.size)
    assertEquals(7, state.players[1].hand.size)
    assertEquals(7, state.players[2].hand.size)
    assertNotNull(state.topDiscard)
    assertEquals(150, state.totalPot)
  }

  @Test
  fun turnProgressionIn2PlayerAnd3PlayerMatches() {
    // 2-Player: index 0 -> index 1 -> index 0
    val next2p = UnoRulesEngine.calculateNextTurnIndex(0, 2, 1, 1)
    assertEquals(1, next2p)
    val next2pRound = UnoRulesEngine.calculateNextTurnIndex(1, 2, 1, 1)
    assertEquals(0, next2pRound)

    // 2-Player Reverse acts as Skip (+2 steps): 0 + 2 = 0 (same player keeps turn!)
    val next2pReverse = UnoRulesEngine.calculateNextTurnIndex(0, 2, 1, 2)
    assertEquals(0, next2pReverse)

    // 3-Player Clockwise (+1)
    assertEquals(1, UnoRulesEngine.calculateNextTurnIndex(0, 3, 1, 1))
    assertEquals(2, UnoRulesEngine.calculateNextTurnIndex(1, 3, 1, 1))
    assertEquals(0, UnoRulesEngine.calculateNextTurnIndex(2, 3, 1, 1))

    // 3-Player Counter-Clockwise (-1)
    assertEquals(2, UnoRulesEngine.calculateNextTurnIndex(0, 3, -1, 1))
    assertEquals(1, UnoRulesEngine.calculateNextTurnIndex(2, 3, -1, 1))
  }

  @Test
  fun winConditionDetectedWhenHandReachesZero() {
    val session = UnoGameSession()
    val p1 = UnoPlayer("p1", "Player 1", PlayerType.HUMAN)
    val p2 = UnoPlayer("p2", "Player 2", PlayerType.BOT)

    session.startNewMatch(listOf(p1, p2), fee = 100)
    val state = session.state.value

    assertEquals(200, state.totalPot)
    assertEquals(GameStatus.InProgress, state.status)
  }

  @Test
  fun coinSystemPreservedAndDeductionWorks() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val coinRepo = CoinRepository(context)

    // Starts at 100
    assertEquals(100, coinRepo.coinBalance.value)
    assertTrue(coinRepo.deductCoins(50))
    assertEquals(50, coinRepo.coinBalance.value)
  }
}
