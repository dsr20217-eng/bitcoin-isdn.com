package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChainSpecsScreen
import com.example.ui.screens.CodecScreen
import com.example.ui.screens.NodeScreen
import com.example.ui.screens.TxInspectorScreen
import com.example.ui.theme.CoreLensTheme
import com.example.ui.viewmodel.ChainSpecsViewModel
import com.example.ui.viewmodel.CodecViewModel
import com.example.ui.viewmodel.NodeViewModel
import com.example.ui.viewmodel.TxInspectorViewModel

const val ROUTE_NODE = "node"
const val ROUTE_CODEC = "codec"
const val ROUTE_TX = "tx"
const val ROUTE_CHAINS = "chains"

private data class TopLevelDestination(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = ROUTE_NODE,
        labelResId = R.string.nav_node,
        selectedIcon = Icons.Filled.Hub,
        unselectedIcon = Icons.Outlined.Hub,
        testTag = "nav_tab_node"
    ),
    TopLevelDestination(
        route = ROUTE_CODEC,
        labelResId = R.string.nav_codec,
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code,
        testTag = "nav_tab_codec"
    ),
    TopLevelDestination(
        route = ROUTE_TX,
        labelResId = R.string.nav_tx,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_tab_tx"
    ),
    TopLevelDestination(
        route = ROUTE_CHAINS,
        labelResId = R.string.nav_chains,
        selectedIcon = Icons.Filled.Dns,
        unselectedIcon = Icons.Outlined.Dns,
        testTag = "nav_tab_chains"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoreLensTheme {
                CoreLensApp()
            }
        }
    }
}

@Composable
fun CoreLensApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_NODE

    val nodeViewModel: NodeViewModel = viewModel()
    val codecViewModel: CodecViewModel = viewModel()
    val txViewModel: TxInspectorViewModel = viewModel()
    val chainSpecsViewModel: ChainSpecsViewModel = viewModel()

    val nodeState by nodeViewModel.uiState.collectAsStateWithLifecycle()
    val codecState by codecViewModel.uiState.collectAsStateWithLifecycle()
    val txState by txViewModel.uiState.collectAsStateWithLifecycle()
    val chainSpecsState by chainSpecsViewModel.uiState.collectAsStateWithLifecycle()

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(id = dest.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(id = dest.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_NODE,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_NODE) {
                        NodeScreen(uiState = nodeState, viewModel = nodeViewModel)
                    }
                    composable(ROUTE_CODEC) {
                        CodecScreen(uiState = codecState, viewModel = codecViewModel)
                    }
                    composable(ROUTE_TX) {
                        TxInspectorScreen(uiState = txState, viewModel = txViewModel)
                    }
                    composable(ROUTE_CHAINS) {
                        ChainSpecsScreen(uiState = chainSpecsState, viewModel = chainSpecsViewModel)
                    }
                }
            }
        }
    }
}
