package com.example.game.model

/**
 * Domain card color according to classic UNO specifications.
 */
enum class CardColor(val displayName: String) {
  RED("Red"),
  YELLOW("Yellow"),
  GREEN("Green"),
  BLUE("Blue"),
  WILD("Wild")
}

/**
 * Domain card type and face value according to classic UNO specifications.
 */
enum class CardValue(val symbol: String, val isAction: Boolean, val isWild: Boolean) {
  ZERO("0", false, false),
  ONE("1", false, false),
  TWO("2", false, false),
  THREE("3", false, false),
  FOUR("4", false, false),
  FIVE("5", false, false),
  SIX("6", false, false),
  SEVEN("7", false, false),
  EIGHT("8", false, false),
  NINE("9", false, false),
  SKIP("⊘", true, false),
  REVERSE("⇄", true, false),
  DRAW_TWO("+2", true, false),
  WILD("★", true, true),
  WILD_DRAW_FOUR("+4", true, true)
}

/**
 * Immutable card instance with a unique ID for animation tracking & stable rendering.
 */
data class UnoCard(
  val id: String,
  val color: CardColor,
  val value: CardValue
) {
  val isWild: Boolean get() = color == CardColor.WILD || value.isWild

  override fun toString(): String = "${color.name}_${value.name}"
}
