package io.github.flassuu.talex.core.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun mapsStoredTagsAndFallsBackToSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.UKRAINIAN, AppLanguage.fromTag("uk"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("de"))
    }
}
