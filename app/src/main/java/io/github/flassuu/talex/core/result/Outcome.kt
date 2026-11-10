package io.github.flassuu.talex.core.result

/**
 * Failure codes shared by domain and UI. The UI turns these into localized messages;
 * domain code never contains user-facing text.
 */
enum class AppError {
    DATABASE_LOCKED,
    WRONG_PASSPHRASE,
    CRYPTO_FAILED,
    BIOMETRIC_FAILED,
    PASSPHRASE_TOO_SHORT,
    PASSPHRASE_MISMATCH,
    NAME_REQUIRED,
    NAME_TOO_LONG,
    AMOUNT_INVALID,
    UNKNOWN,
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}
