package com.example

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.game.engine.UnoGameSession
import com.example.game.model.PlayerType
import com.example.game.model.UnoPlayer
import com.example.ui.game.GameTableScreen
import com.example.ui.lan.HostWaitingRoomScreen
import com.example.ui.lan.JoinWaitingRoomScreen
import com.example.ui.lan.VsFriendsModeSelectionScreen
import com.example.ui.loading.LoadingScreen
import com.example.ui.lobby.LobbyScreen
import com.example.ui.matchsetup.MatchConfirmationScreen
import com.example.ui.matchsetup.MatchEntrySelectionScreen
import com.example.ui.matchsetup.PlayerCountSelectionScreen
import com.example.ui.navigation.Screen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.ColorClashTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    hideSystemBars()

    val app = application as ColorClashApp

    setContent {
      ColorClashTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          GameNavigationRoot(app = app)
        }
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      hideSystemBars()
    }
  }

  private fun hideSystemBars() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.insetsController?.let { controller ->
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
          WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      }
    } else {
      @Suppress("DEPRECATION")
      window.decorView.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          or View.SYSTEM_UI_FLAG_FULLSCREEN
          or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      )
    }
  }
}

@Composable
fun GameNavigationRoot(app: ColorClashApp) {
  var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
  var activeGameSession by remember { mutableStateOf(UnoGameSession()) }

  BackHandler(enabled = currentScreen !is Screen.Lobby && currentScreen !is Screen.Loading) {
    currentScreen = when (val s = currentScreen) {
      is Screen.Settings -> Screen.Lobby
      is Screen.MatchEntrySelection -> Screen.Lobby
      is Screen.MatchPlayerCountSelection -> Screen.MatchEntrySelection(s.isVsFriends)
      is Screen.MatchConfirmation -> Screen.MatchPlayerCountSelection(s.isVsFriends, s.entryFee)
      is Screen.VsFriendsModeSelection -> Screen.MatchConfirmation(isVsFriends = true, s.entryFee, s.playerCount)
      is Screen.HostWaitingRoom -> Screen.VsFriendsModeSelection(s.entryFee, s.playerCount)
      is Screen.JoinWaitingRoom -> Screen.Lobby
      is Screen.GameTable -> Screen.Lobby
      else -> Screen.Lobby
    }
  }

  Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
    when (screen) {
      is Screen.Loading -> {
        LoadingScreen(
          onLoadingFinished = {
            currentScreen = Screen.Lobby
          }
        )
      }

      is Screen.Lobby -> {
        LobbyScreen(
          coinRepository = app.coinRepository,
          audioManager = app.audioManager,
          onVsComputerClick = {
            currentScreen = Screen.MatchEntrySelection(isVsFriends = false)
          },
          onVsFriendsClick = {
            currentScreen = Screen.MatchEntrySelection(isVsFriends = true)
          },
          onSettingsClick = {
            currentScreen = Screen.Settings
          }
        )
      }

      is Screen.Settings -> {
        SettingsScreen(
          preferencesManager = app.preferencesManager,
          onBackClick = {
            currentScreen = Screen.Lobby
          }
        )
      }

      is Screen.MatchEntrySelection -> {
        MatchEntrySelectionScreen(
          isVsFriends = screen.isVsFriends,
          coinRepository = app.coinRepository,
          audioManager = app.audioManager,
          onBackClick = {
            currentScreen = Screen.Lobby
          },
          onProceedToPlayerCount = { entryFee ->
            currentScreen = Screen.MatchPlayerCountSelection(
              isVsFriends = screen.isVsFriends,
              entryFee = entryFee
            )
          }
        )
      }

      is Screen.MatchPlayerCountSelection -> {
        PlayerCountSelectionScreen(
          isVsFriends = screen.isVsFriends,
          entryFee = screen.entryFee,
          audioManager = app.audioManager,
          onBackClick = {
            currentScreen = Screen.MatchEntrySelection(screen.isVsFriends)
          },
          onProceedToConfirmation = { playerCount ->
            currentScreen = Screen.MatchConfirmation(
              isVsFriends = screen.isVsFriends,
              entryFee = screen.entryFee,
              playerCount = playerCount
            )
          }
        )
      }

      is Screen.MatchConfirmation -> {
        MatchConfirmationScreen(
          isVsFriends = screen.isVsFriends,
          entryFee = screen.entryFee,
          playerCount = screen.playerCount,
          audioManager = app.audioManager,
          onBackClick = {
            currentScreen = Screen.MatchPlayerCountSelection(screen.isVsFriends, screen.entryFee)
          },
          onStartGameClick = {
            if (screen.isVsFriends) {
              // Directs to VS FRIENDS Mode Selection (Host vs Join)
              currentScreen = Screen.VsFriendsModeSelection(screen.entryFee, screen.playerCount)
            } else {
              // VS COMPUTER autonomous offline flow
              if (app.coinRepository.deductCoins(screen.entryFee)) {
                val newSession = UnoGameSession()
                activeGameSession = newSession
                val players = mutableListOf<UnoPlayer>()
                players.add(UnoPlayer(id = "player_human", name = "You", type = PlayerType.HUMAN))
                for (i in 1 until screen.playerCount) {
                  players.add(UnoPlayer(id = "player_bot_$i", name = "Bot $i", type = PlayerType.BOT))
                }
                newSession.startNewMatch(players, screen.entryFee)
                currentScreen = Screen.GameTable(screen.isVsFriends, screen.entryFee, screen.playerCount)
              }
            }
          }
        )
      }

      is Screen.VsFriendsModeSelection -> {
        VsFriendsModeSelectionScreen(
          entryFee = screen.entryFee,
          playerCount = screen.playerCount,
          coinRepository = app.coinRepository,
          audioManager = app.audioManager,
          onBackClick = {
            currentScreen = Screen.MatchConfirmation(isVsFriends = true, screen.entryFee, screen.playerCount)
          },
          onHostClick = {
            currentScreen = Screen.HostWaitingRoom(screen.entryFee, screen.playerCount)
          },
          onJoinClick = {
            currentScreen = Screen.JoinWaitingRoom
          }
        )
      }

      is Screen.HostWaitingRoom -> {
        HostWaitingRoomScreen(
          entryFee = screen.entryFee,
          maxPlayers = screen.playerCount,
          coinRepository = app.coinRepository,
          preferencesManager = app.preferencesManager,
          audioManager = app.audioManager,
          onBackClick = {
            currentScreen = Screen.VsFriendsModeSelection(screen.entryFee, screen.playerCount)
          },
          onStartMatch = { server ->
            if (app.coinRepository.deductCoins(screen.entryFee)) {
              activeGameSession = server.gameSession
              currentScreen = Screen.GameTable(isVsFriends = true, screen.entryFee, screen.playerCount)
            }
          }
        )
      }

      is Screen.JoinWaitingRoom -> {
        JoinWaitingRoomScreen(
          coinRepository = app.coinRepository,
          preferencesManager = app.preferencesManager,
          audioManager = app.audioManager,
          onBackClick = {
            currentScreen = Screen.Lobby
          },
          onGameStarted = { client ->
            currentScreen = Screen.GameTable(isVsFriends = true, entryFee = 50, playerCount = 2)
          }
        )
      }

      is Screen.GameTable -> {
        GameTableScreen(
          session = activeGameSession,
          coinRepository = app.coinRepository,
          audioManager = app.audioManager,
          onLeaveGameClick = {
            currentScreen = Screen.Lobby
          }
        )
      }
    }
  }
}
