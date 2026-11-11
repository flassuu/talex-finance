package io.github.flassuu.talex.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.data.session.LockState
import io.github.flassuu.talex.domain.usecase.RecurringGeneration
import io.github.flassuu.talex.feature.accounts.AccountDetailScreen
import io.github.flassuu.talex.feature.accounts.AccountsScreen
import io.github.flassuu.talex.feature.app.AppViewModel
import io.github.flassuu.talex.feature.categories.CategoriesScreen
import io.github.flassuu.talex.feature.entries.EntryEditScreen
import io.github.flassuu.talex.feature.entries.TransactionsScreen
import io.github.flassuu.talex.feature.overview.OverviewScreen
import io.github.flassuu.talex.feature.recurring.RecurringScreen
import io.github.flassuu.talex.feature.transfers.TransferEditScreen
import io.github.flassuu.talex.feature.setup.SetupScreen
import io.github.flassuu.talex.feature.unlock.UnlockScreen
import io.github.flassuu.talex.ui.component.PlaceholderScreen

@Composable
fun TalexApp(viewModel: AppViewModel = hiltViewModel()) {
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()
    val summary by viewModel.recurringSummary.collectAsStateWithLifecycle()

    when (lockState) {
        LockState.NOT_INITIALIZED -> SetupScreen()
        LockState.LOCKED -> UnlockScreen()
        LockState.UNLOCKED -> MainShell()
    }

    summary?.let {
        RecurringSummaryDialog(summary = it, onDismiss = viewModel::clearRecurringSummary)
    }
}

@Composable
private fun RecurringSummaryDialog(summary: RecurringGeneration, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurring_summary_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (summary.created.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.recurring_summary_created),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    summary.created.forEach { entry ->
                        Text(
                            text = entry.date.toString() + " · " + entry.description + " · " +
                                MoneyFormat.format(entry.amount),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (summary.skipped.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.recurring_summary_skipped),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    summary.skipped.forEach { rule ->
                        Text(text = rule.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok_action))
            }
        },
    )
}

@Composable
private fun MainShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var showMore by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            TalexBottomBar(
                currentRoute = currentRoute,
                onNavigate = navController::navigateTopLevel,
                onQuickAdd = { showAddMenu = true },
                onMore = { showMore = true },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OVERVIEW,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.OVERVIEW) { OverviewScreen() }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    onOpenEntry = { entryId ->
                        navController.navigate(Routes.entryEdit(entryId))
                    },
                    onNewTransfer = { navController.navigate(Routes.TRANSFER_NEW) },
                    onOpenTransfer = { transferId ->
                        navController.navigate(Routes.transferEdit(transferId))
                    },
                )
            }
            composable(
                route = Routes.ENTRY_NEW,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) {
                EntryEditScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.ENTRY_EDIT,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) {
                EntryEditScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.TRANSFER_NEW) {
                TransferEditScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.TRANSFER_EDIT,
                arguments = listOf(navArgument("transferId") { type = NavType.StringType }),
            ) {
                TransferEditScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.TRANSACTIONS_FILTERED,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) {
                TransactionsScreen(
                    onOpenEntry = { entryId ->
                        navController.navigate(Routes.entryEdit(entryId))
                    },
                    onNewTransfer = { navController.navigate(Routes.TRANSFER_NEW) },
                    onOpenTransfer = { transferId ->
                        navController.navigate(Routes.transferEdit(transferId))
                    },
                )
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onViewTransactions = { categoryId ->
                        navController.navigate(Routes.transactionsFiltered(categoryId))
                    },
                )
            }
            composable(Routes.RECURRING) { RecurringScreen() }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    onOpenAccount = { accountId ->
                        navController.navigate(Routes.accountDetail(accountId))
                    },
                )
            }
            composable(
                route = Routes.ACCOUNT_DETAIL,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
            ) {
                AccountDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BUDGETS) { PlaceholderScreen(R.string.budgets_title) }
            composable(Routes.GOALS) { PlaceholderScreen(R.string.goals_title) }
            composable(Routes.ASSETS) { PlaceholderScreen(R.string.assets_title) }
            composable(Routes.ANALYTICS) { PlaceholderScreen(R.string.analytics_title) }
            composable(Routes.SETTINGS) { PlaceholderScreen(R.string.settings_title) }
        }
    }

    if (showAddMenu) {
        AddMenuSheet(
            onDismiss = { showAddMenu = false },
            onExpense = {
                showAddMenu = false
                navController.navigate(Routes.entryNew("EXPENSE"))
            },
            onIncome = {
                showAddMenu = false
                navController.navigate(Routes.entryNew("INCOME"))
            },
            onTransfer = {
                showAddMenu = false
                navController.navigate(Routes.TRANSFER_NEW)
            },
        )
    }

    if (showMore) {
        MoreSheet(
            onDismiss = { showMore = false },
            onNavigate = { route ->
                showMore = false
                navController.navigateTopLevel(route)
            },
        )
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    // Do not save or restore tab state: a pushed entry form must never reappear
    // when the user switches tabs. The stack is kept flat at [start, target].
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.findStartDestination().id) { inclusive = false }
    }
}

@Composable
private fun TalexBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onQuickAdd: () -> Unit,
    onMore: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomBarItem(
                selected = currentRoute == Routes.OVERVIEW,
                icon = Icons.Outlined.Home,
                labelRes = R.string.overview_title,
                onClick = { onNavigate(Routes.OVERVIEW) },
            )
            BottomBarItem(
                selected = currentRoute == Routes.TRANSACTIONS,
                icon = Icons.Outlined.SwapVert,
                labelRes = R.string.transactions_title,
                onClick = { onNavigate(Routes.TRANSACTIONS) },
            )
            AddSlot(onClick = onQuickAdd)
            BottomBarItem(
                selected = currentRoute == Routes.ACCOUNTS,
                icon = Icons.Outlined.AccountBalanceWallet,
                labelRes = R.string.accounts_title,
                onClick = { onNavigate(Routes.ACCOUNTS) },
            )
            BottomBarItem(
                selected = false,
                icon = Icons.Outlined.MoreHoriz,
                labelRes = R.string.more_title,
                onClick = onMore,
            )
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    selected: Boolean,
    icon: ImageVector,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = stringResource(labelRes), tint = color)
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.AddSlot(onClick: () -> Unit) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_add),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.more_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        MoreItem(R.string.categories_title, Icons.Outlined.Category) { onNavigate(Routes.CATEGORIES) }
        MoreItem(R.string.recurring_title, Icons.Outlined.Repeat) { onNavigate(Routes.RECURRING) }
        MoreItem(R.string.budgets_title, Icons.Outlined.Savings) { onNavigate(Routes.BUDGETS) }
        MoreItem(R.string.goals_title, Icons.Outlined.Flag) { onNavigate(Routes.GOALS) }
        MoreItem(R.string.assets_title, Icons.Outlined.AccountBalance) { onNavigate(Routes.ASSETS) }
        MoreItem(R.string.analytics_title, Icons.Outlined.Insights) { onNavigate(Routes.ANALYTICS) }
        MoreItem(R.string.settings_title, Icons.Outlined.Settings) { onNavigate(Routes.SETTINGS) }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMenuSheet(
    onDismiss: () -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.add_action_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        MoreItem(R.string.new_expense_title, Icons.Outlined.TrendingDown) { onExpense() }
        MoreItem(R.string.new_income_title, Icons.Outlined.TrendingUp) { onIncome() }
        MoreItem(R.string.new_transfer_title, Icons.Outlined.SwapHoriz) { onTransfer() }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MoreItem(@StringRes titleRes: Int, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}


