package io.github.flassuu.talex.ui.navigation

import io.github.flassuu.talex.core.navigation.RouteKeys

object Routes {
    const val OVERVIEW = RouteKeys.OVERVIEW
    const val TRANSACTIONS = RouteKeys.TRANSACTIONS
    const val TRANSACTIONS_FILTERED = "transactions_filtered/{categoryId}"
    const val ACCOUNTS = RouteKeys.ACCOUNTS
    const val ACCOUNT_DETAIL = "accounts/{accountId}"
    const val ENTRY_NEW = "entry_new/{type}"
    const val ENTRY_EDIT = "entry_edit/{entryId}"
    const val TRANSFER_NEW = "transfer_new"
    const val TRANSFER_EDIT = "transfer_edit/{transferId}"
    const val CATEGORIES = RouteKeys.CATEGORIES
    const val RECURRING = RouteKeys.RECURRING
    const val BUDGETS = RouteKeys.BUDGETS
    const val GOALS = RouteKeys.GOALS
    const val GOAL_DETAIL = "goals/{goalId}"
    const val ASSETS = RouteKeys.ASSETS
    const val ANALYTICS = RouteKeys.ANALYTICS
    const val SETTINGS = RouteKeys.SETTINGS
    const val RATES = "rates"
    const val CHANGE_BASE_CURRENCY = "change_base_currency"
    const val TAGS = "tags"

    fun accountDetail(accountId: String): String = "accounts/$accountId"

    fun transactionsFiltered(categoryId: String): String = "transactions_filtered/$categoryId"

    fun entryNew(type: String): String = "entry_new/$type"

    fun entryEdit(entryId: String): String = "entry_edit/$entryId"

    fun transferEdit(transferId: String): String = "transfer_edit/$transferId"

    fun goalDetail(goalId: String): String = "goals/$goalId"
}
