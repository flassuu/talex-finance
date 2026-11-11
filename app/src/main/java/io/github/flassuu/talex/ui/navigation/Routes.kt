package io.github.flassuu.talex.ui.navigation

object Routes {
    const val OVERVIEW = "overview"
    const val TRANSACTIONS = "transactions"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "accounts/{accountId}"
    const val ENTRY_NEW = "entry_new/{type}"
    const val ENTRY_EDIT = "entry_edit/{entryId}"
    const val CATEGORIES = "categories"
    const val BUDGETS = "budgets"
    const val GOALS = "goals"
    const val ASSETS = "assets"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"

    fun accountDetail(accountId: String): String = "accounts/$accountId"

    fun entryNew(type: String): String = "entry_new/$type"

    fun entryEdit(entryId: String): String = "entry_edit/$entryId"
}
