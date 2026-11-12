package io.github.flassuu.talex.core.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** The app language chosen in Settings. [SYSTEM] follows the device language. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    UKRAINIAN("uk"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/**
 * Stores the language choice outside the encrypted database so the setup and
 * unlock screens are localized before the database is opened.
 */
object LanguagePreferences {
    private const val PREFS = "talex.language"
    private const val KEY = "language"

    fun get(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppLanguage.fromTag(prefs.getString(KEY, null))
    }

    fun set(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, language.tag)
            .apply()
    }
}

/**
 * Returns a context configured for [language]. [AppLanguage.SYSTEM] keeps the
 * device configuration but restores the process default locale, so money and date
 * formatting follow the chosen language as well.
 */
fun Context.withAppLanguage(language: AppLanguage): Context {
    if (language == AppLanguage.SYSTEM) {
        Locale.setDefault(resources.configuration.locales[0])
        return this
    }
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(configuration)
}
