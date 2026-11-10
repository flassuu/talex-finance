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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.flassuu.talex.R
import io.github.flassuu.talex.data.session.LockState
import io.github.flassuu.talex.feature.accounts.AccountsScreen
import io.github.flassuu.talex.feature.app.AppViewModel
import io.github.flassuu.talex.feature.home.OverviewScreen
import io.github.flassuu.talex.feature.home.TransactionsScreen
import io.github.flassuu.talex.feature.setup.SetupScreen
import io.github.flassuu.talex.feature.unlock.UnlockScreen
import io.github.flassuu.talex.ui.component.PlaceholderScreen

@Composable
fun TalexApp(viewModel: AppViewModel = hiltViewModel()) {
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()

    when (lockState) {
        LockState.NOT_INITIALIZED -> SetupScreen()
        LockState.LOCKED -> UnlockScreen()
        LockState.UNLOCKED -> MainShell()
    }
}

@Composable
private fun MainShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var showQuickAdd by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            TalexBottomBar(
                currentRoute = currentRoute,
                onNavigate = navController::navigateTopLevel,
                onQuickAdd = { showQuickAdd = true },
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
            composable(Routes.TRANSACTIONS) { TransactionsScreen() }
            composable(Routes.ACCOUNTS) { AccountsScreen() }
            composable(Routes.BUDGETS) { PlaceholderScreen(R.string.budgets_title) }
            composable(Routes.GOALS) { PlaceholderScreen(R.string.goals_title) }
            composable(Routes.ASSETS) { PlaceholderScreen(R.string.assets_title) }
            composable(Routes.ANALYTICS) { PlaceholderScreen(R.string.analytics_title) }
            composable(Routes.SETTINGS) { PlaceholderScreen(R.string.settings_title) }
        }
    }

    if (showQuickAdd) {
        PlaceholderSheet(
            title = stringResource(R.string.quick_add_title),
            onDismiss = { showQuickAdd = false },
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
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) { saveState = true }
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
        MoreItem(R.string.budgets_title, Icons.Outlined.Savings) { onNavigate(Routes.BUDGETS) }
        MoreItem(R.string.goals_title, Icons.Outlined.Flag) { onNavigate(Routes.GOALS) }
        MoreItem(R.string.assets_title, Icons.Outlined.AccountBalance) { onNavigate(Routes.ASSETS) }
        MoreItem(R.string.analytics_title, Icons.Outlined.Insights) { onNavigate(Routes.ANALYTICS) }
        MoreItem(R.string.settings_title, Icons.Outlined.Settings) { onNavigate(Routes.SETTINGS) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderSheet(title: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.placeholder_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(32.dp))
    }
}
