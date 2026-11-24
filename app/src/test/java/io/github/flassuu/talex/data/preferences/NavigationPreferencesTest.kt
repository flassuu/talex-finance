package io.github.flassuu.talex.data.preferences

import io.github.flassuu.talex.core.navigation.RouteKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPreferencesTest {

    @Test
    fun emptyInputFallsBackToTheDefaults() {
        assertEquals(NavigationPreferences.DEFAULT, NavigationPreferences.sanitize(emptyList()))
    }

    @Test
    fun keepsValidOrderAndCapsAtThree() {
        val routes = listOf(
            RouteKeys.SETTINGS,
            RouteKeys.GOALS,
            RouteKeys.ANALYTICS,
            RouteKeys.BUDGETS,
        )
        assertEquals(
            listOf(RouteKeys.SETTINGS, RouteKeys.GOALS, RouteKeys.ANALYTICS),
            NavigationPreferences.sanitize(routes),
        )
    }

    @Test
    fun dropsInvalidAndDuplicateKeys() {
        val routes = listOf(
            RouteKeys.BUDGETS,
            "nope",
            RouteKeys.BUDGETS,
            RouteKeys.ASSETS,
        )
        assertEquals(
            listOf(RouteKeys.BUDGETS, RouteKeys.ASSETS, RouteKeys.OVERVIEW),
            NavigationPreferences.sanitize(routes),
        )
    }

    @Test
    fun fillsMissingSlotsFromTheDefaults() {
        assertEquals(
            listOf(RouteKeys.SETTINGS, RouteKeys.OVERVIEW, RouteKeys.TRANSACTIONS),
            NavigationPreferences.sanitize(listOf(RouteKeys.SETTINGS)),
        )
    }

    @Test
    fun everyDefaultIsAnAllowedDestination() {
        assertTrue(NavigationPreferences.DEFAULT.all { it in NavigationPreferences.ALLOWED })
    }

    @Test
    fun allMainDestinationsAreAllowed() {
        val expected = setOf(
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
        assertEquals(expected, NavigationPreferences.ALLOWED.toSet())
    }
}
