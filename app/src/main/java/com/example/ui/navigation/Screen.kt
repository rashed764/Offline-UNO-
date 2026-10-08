package com.example.ui.navigation

sealed class Screen {
  data object Loading : Screen()
  data object Lobby : Screen()
  data object Settings : Screen()

  data class MatchEntrySelection(val isVsFriends: Boolean) : Screen()
  data class MatchPlayerCountSelection(val isVsFriends: Boolean, val entryFee: Int) : Screen()
  data class MatchConfirmation(val isVsFriends: Boolean, val entryFee: Int, val playerCount: Int) : Screen()

  // VS FRIENDS Menu & Waiting Rooms
  data class VsFriendsModeSelection(val entryFee: Int, val playerCount: Int) : Screen()
  data class HostWaitingRoom(val entryFee: Int, val playerCount: Int) : Screen()
  data object JoinWaitingRoom : Screen()

  // Active Game table
  data class GameTable(val isVsFriends: Boolean, val entryFee: Int, val playerCount: Int) : Screen()
}
