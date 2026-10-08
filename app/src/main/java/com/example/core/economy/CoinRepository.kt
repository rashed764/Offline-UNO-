package com.example.core.economy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized, thread-safe economy & coin management system for Offline UNO!.
 * Prevents negative coins, duplicates, and maintains strict balance rules.
 * Initial launch starts with exactly 100 coins.
 */
class CoinRepository(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _coinBalance = MutableStateFlow(loadInitialCoins())
  val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

  private fun loadInitialCoins(): Int {
    val saved = prefs.getInt(KEY_COINS, -1)
    return if (saved < 0) {
      // First time launch: initialize with 100 coins
      prefs.edit().putInt(KEY_COINS, INITIAL_COINS).apply()
      INITIAL_COINS
    } else {
      saved
    }
  }

  @Synchronized
  fun deductCoins(amount: Int): Boolean {
    if (amount <= 0) return false
    val current = _coinBalance.value
    if (current < amount) return false
    val updated = current - amount
    prefs.edit().putInt(KEY_COINS, updated).apply()
    _coinBalance.value = updated
    return true
  }

  @Synchronized
  fun awardCoins(amount: Int) {
    if (amount <= 0) return
    val current = _coinBalance.value
    val updated = (current + amount).coerceAtLeast(0)
    prefs.edit().putInt(KEY_COINS, updated).apply()
    _coinBalance.value = updated
  }

  fun canAfford(amount: Int): Boolean {
    return _coinBalance.value >= amount
  }

  /**
   * Safe refill for testing or bonus claiming
   */
  @Synchronized
  fun claimFreeDailyBonus(): Boolean {
    awardCoins(100)
    return true
  }

  companion object {
    private const val PREFS_NAME = "offline_uno_economy"
    private const val KEY_COINS = "user_coin_balance"
    const val INITIAL_COINS = 100
  }
}
