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
    AMOUNT_REQUIRED,
    DESCRIPTION_REQUIRED,
    DESCRIPTION_TOO_LONG,
    ACCOUNT_NOT_FOUND,
    ACCOUNT_ARCHIVED,
    ACCOUNT_NOT_EMPTY,
    CURRENCY_MISMATCH,
    CATEGORY_REQUIRED,
    CATEGORY_NOT_FOUND,
    CATEGORY_ARCHIVED,
    CATEGORY_KIND_MISMATCH,
    CATEGORY_IN_USE,
    CATEGORY_HAS_CHILDREN,
    CATEGORY_PARENT_INVALID,
    ENTRY_NOT_FOUND,
    SAME_ACCOUNT_TRANSFER,
    TRANSFER_AMOUNT_MISMATCH,
    SPLIT_INVALID,
    SPLIT_SUM_MISMATCH,
    SPLIT_SAME_PARENT,
    ACCOUNT_TYPE_INVALID,
    BUDGET_NOT_FOUND,
    GOAL_NOT_FOUND,
    VALUATION_NOT_FOUND,
    UNKNOWN,
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}
