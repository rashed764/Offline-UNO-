package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.economy.CoinRepository
import com.example.core.persistence.PreferencesManager
import com.example.game.bot.BotAction
import com.example.game.bot.BotDecisionEngine
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
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineUnoPhase3TestSuite {

  @Test
  fun appNameResourceMatchesOfflineUno() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Offline UNO!", appName)
  }

  @Test
  fun botNeverChoosesAnIllegalCard() {
    val botHand = listOf(
      UnoCard("c1", CardColor.RED, CardValue.TWO),
      UnoCard("c2", CardColor.BLUE, CardValue.SEVEN),
      UnoCard("c3", CardColor.YELLOW, CardValue.SKIP)
    )
    val bot = UnoPlayer("bot_1", "Bot Alpha", PlayerType.BOT, hand = botHand)
    val human = UnoPlayer("human", "You", PlayerType.HUMAN, hand = emptyList())

    val topDiscard = UnoCard("top", CardColor.GREEN, CardValue.TWO)
    val state = com.example.game.engine.UnoGameState(
      players = listOf(bot, human),
      currentTurnIndex = 0,
      direction = PlayDirection.CLOCKWISE,
      activeColor = CardColor.GREEN,
      topDiscard = topDiscard,
      drawPileCount = 40,
      discardPileCount = 5,
      status = GameStatus.InProgress,
      lastEventMessage = "Game ongoing"
    )

    val action = BotDecisionEngine.evaluateTurn(bot, state)
    assertTrue("Bot action should be PlayCard", action is BotAction.PlayCard)
    val play = action as BotAction.PlayCard
    // Only Red Two is legal because of value match (2 on 2)
    assertEquals(CardValue.TWO, play.card.value)
    assertTrue(UnoRulesEngine.isCardPlayable(play.card, topDiscard, state.activeColor))
  }

  @Test
  fun botDrawsWhenNoLegalCardExists() {
    val botHand = listOf(
      UnoCard("c1", CardColor.RED, CardValue.SEVEN),
      UnoCard("c2", CardColor.BLUE, CardValue.FOUR)
    )
    val bot = UnoPlayer("bot_1", "Bot Alpha", PlayerType.BOT, hand = botHand)
    val topDiscard = UnoCard("top", CardColor.YELLOW, CardValue.NINE)

    val state = com.example.game.engine.UnoGameState(
      players = listOf(bot),
      currentTurnIndex = 0,
      direction = PlayDirection.CLOCKWISE,
      activeColor = CardColor.YELLOW,
      topDiscard = topDiscard,
      drawPileCount = 40,
      discardPileCount = 5,
      status = GameStatus.InProgress,
      lastEventMessage = "Game ongoing"
    )

    val action = BotDecisionEngine.evaluateTurn(bot, state)
    assertEquals(BotAction.DrawCard, action)
  }

  @Test
  fun botChoosesDominantColorAccuratelyWithDeterministicTieBreak() {
    val hand = listOf(
      UnoCard("c1", CardColor.RED, CardValue.ONE),
      UnoCard("c2", CardColor.RED, CardValue.TWO),
      UnoCard("c3", CardColor.BLUE, CardValue.FIVE),
      UnoCard("c4", CardColor.WILD, CardValue.WILD)
    )

    // Red has 2 cards, Blue has 1 card -> Red dominant
    val chosen1 = BotDecisionEngine.chooseDominantColor(hand, excludingCardId = "c4")
    assertEquals(CardColor.RED, chosen1)

    // Tie between Red (1) and Blue (1) -> Red prioritized deterministically
    val tieHand = listOf(
      UnoCard("c1", CardColor.RED, CardValue.ONE),
      UnoCard("c2", CardColor.BLUE, CardValue.FIVE)
    )
    val chosenTie = BotDecisionEngine.chooseDominantColor(tieHand)
    assertEquals(CardColor.RED, chosenTie)
  }

  @Test
  fun botPrioritizesDisruptiveCardsWhenNextOpponentHasFewCards() {
    val botHand = listOf(
      UnoCard("c1", CardColor.RED, CardValue.THREE),
      UnoCard("c2", CardColor.RED, CardValue.SKIP)
    )
    val bot = UnoPlayer("bot_1", "Bot", PlayerType.BOT, hand = botHand)
    // Opponent has 1 card (threatening UNO)
    val opponent = UnoPlayer("opp", "Opponent", PlayerType.HUMAN, hand = listOf(UnoCard("x", CardColor.RED, CardValue.NINE)))

    val topDiscard = UnoCard("top", CardColor.RED, CardValue.FIVE)
    val state = com.example.game.engine.UnoGameState(
      players = listOf(bot, opponent),
      currentTurnIndex = 0,
      direction = PlayDirection.CLOCKWISE,
      activeColor = CardColor.RED,
      topDiscard = topDiscard,
      drawPileCount = 30,
      discardPileCount = 5,
      status = GameStatus.InProgress,
      lastEventMessage = "Opponent on UNO"
    )

    val action = BotDecisionEngine.evaluateTurn(bot, state)
    assertTrue(action is BotAction.PlayCard)
    val play = action as BotAction.PlayCard
    assertEquals(CardValue.SKIP, play.card.value) // Bot should choose Skip over normal 3
  }

  @Test
  fun fullAutonomousSimulationMatchReachesWinnerWithProperPotSettlement() {
    val session = UnoGameSession(random = Random(42))
    val human = UnoPlayer("p_human", "You", PlayerType.HUMAN)
    val bot = UnoPlayer("p_bot", "Bot Alpha", PlayerType.BOT)

    session.startNewMatch(listOf(human, bot), fee = 100)
    assertEquals(200, session.state.value.totalPot)

    // Simulate turn loops through domain rules until match completion or safety max turns
    var turnsCount = 0
    val maxTurns = 300

    while (session.state.value.status is GameStatus.InProgress && turnsCount < maxTurns) {
      turnsCount++
      val state = session.state.value
      val current = state.currentPlayer ?: break

      val action = BotDecisionEngine.evaluateTurn(current, state)
      when (action) {
        is BotAction.PlayCard -> {
          session.playCard(current.id, action.card.id)
          if (action.chosenWildColor != null && session.state.value.status is GameStatus.WaitingForWildColor) {
            session.selectWildColor(action.chosenWildColor)
          }
        }
        is BotAction.DrawCard -> {
          val drawn = session.drawCard(current.id)
          // Evaluate if drawn card can be played immediately
          if (drawn != null && session.state.value.status is GameStatus.InProgress) {
            val (canPlay, color) = BotDecisionEngine.shouldPlayDrawnCard(drawn, current.hand, session.state.value)
            if (canPlay) {
              session.playCard(current.id, drawn.id)
              if (color != null && session.state.value.status is GameStatus.WaitingForWildColor) {
                session.selectWildColor(color)
              }
            }
          }
        }
      }
    }

    val finalState = session.state.value
    assertTrue("Match should reach RoundEnded within $maxTurns turns", finalState.status is GameStatus.RoundEnded)
    val result = finalState.status as GameStatus.RoundEnded
    assertNotNull(result.winner)
    assertEquals(200, result.totalPot)
  }

  @Test
  fun multiBotPlayerMatchSupportIn3And4PlayerMatches() {
    val session = UnoGameSession(random = Random(123))
    val p1 = UnoPlayer("p1", "Human", PlayerType.HUMAN)
    val p2 = UnoPlayer("p2", "Bot Alpha", PlayerType.BOT)
    val p3 = UnoPlayer("p3", "Bot Bravo", PlayerType.BOT)
    val p4 = UnoPlayer("p4", "Bot Charlie", PlayerType.BOT)

    session.startNewMatch(listOf(p1, p2, p3, p4), fee = 50)
    val state = session.state.value

    assertEquals(4, state.players.size)
    assertEquals(200, state.totalPot)
    assertEquals(7, state.players[0].hand.size)
    assertEquals(7, state.players[1].hand.size)
    assertEquals(7, state.players[2].hand.size)
    assertEquals(7, state.players[3].hand.size)
  }
}
