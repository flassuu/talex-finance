package io.github.flassuu.talex.core.security

/**
 * The user-selectable re-lock policy (ADR-0030). [IMMEDIATELY] locks as soon as
 * the app leaves the foreground; the others compare elapsed background time.
 */
enum class LockTimeout(val millis: Long) {
    IMMEDIATELY(0L),
    THIRTY_SECONDS(30_000L),
    ONE_MINUTE(60_000L),
    FIVE_MINUTES(300_000L),
    FIFTEEN_MINUTES(900_000L),
    ;

    companion object {
        val DEFAULT = ONE_MINUTE

        fun fromName(name: String?): LockTimeout =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
