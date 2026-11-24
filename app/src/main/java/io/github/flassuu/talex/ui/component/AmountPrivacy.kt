package io.github.flassuu.talex.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.money.MoneyFormat

/** Shown instead of a formatted amount while the privacy switch is on. */
const val HIDDEN_AMOUNT = "••••"

/**
 * Carries the privacy switch to every screen without threading the flag through
 * each composable. Provided once in the unlocked shell.
 */
val LocalAmountsHidden = staticCompositionLocalOf { false }

/** The single way screens format money; masks the value when privacy is on. */
@Composable
fun formatMoney(money: Money): String =
    if (LocalAmountsHidden.current) HIDDEN_AMOUNT else MoneyFormat.format(money)
