package io.github.flassuu.talex.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.flassuu.talex.core.navigation.RouteKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The three configurable bottom-bar slots (ADR-0034). The choice lives in
 * plaintext SharedPreferences so the shell is available before unlock; it holds
 * no financial data.
 */
@Singleton
class NavigationPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _slots = MutableStateFlow(load())
    val slots: StateFlow<List<String>> = _slots.asStateFlow()

    /** Replaces one slot; choosing a destination already in another slot swaps them. */
    fun setSlot(index: Int, route: String) {
        if (index !in 0 until SLOT_COUNT || route !in ALLOWED) return
        val current = _slots.value.toMutableList()
        if (current.getOrNull(index) == route) return
        val existing = current.indexOf(route)
        if (existing >= 0) {
            current[existing] = current[index]
        }
        current[index] = route
        persist(current)
    }

    fun setSlots(routes: List<String>) = persist(routes)

    private fun load(): List<String> =
        sanitize(prefs.getString(KEY, null)?.split(",") ?: emptyList())

    private fun persist(routes: List<String>) {
        val sanitized = sanitize(routes)
        prefs.edit().putString(KEY, sanitized.joinToString(",")).apply()
        _slots.value = sanitized
    }

    companion object {
        private const val PREFS = "talex.navigation"
        private const val KEY = "slots"
        const val SLOT_COUNT = 3

        val DEFAULT: List<String> = listOf(
            RouteKeys.OVERVIEW,
            RouteKeys.TRANSACTIONS,
            RouteKeys.ACCOUNTS,
        )

        /** Every destination that may fill a slot (ADR-0034). */
        val ALLOWED: List<String> = listOf(
            RouteKeys.OVERVIEW,
            RouteKeys.TRANSACTIONS,
            RouteKeys.ACCOUNTS,
            RouteKeys.BUDGETS,
            RouteKeys.GOALS,
            RouteKeys.ASSETS,
            RouteKeys.ANALYTICS,
            RouteKeys.RECURRING,
            RouteKeys.CATEGORIES,
            RouteKeys.SETTINGS,
        )

        /**
         * Keeps the first three distinct allowed routes in order and fills any
         * remaining slot from the defaults. Stale or duplicate keys never reach
         * the bar.
         */
        fun sanitize(routes: List<String>): List<String> {
            val result = mutableListOf<String>()
            routes.forEach { route ->
                if (route in ALLOWED && route !in result && result.size < SLOT_COUNT) {
                    result.add(route)
                }
            }
            DEFAULT.forEach { route ->
                if (result.size < SLOT_COUNT && route !in result) result.add(route)
            }
            return result
        }
    }
}
