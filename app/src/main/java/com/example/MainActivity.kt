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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Router
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.local.CoreLensDatabase
import com.example.data.local.CoreLensPreferences
import com.example.data.local.CoreLensRepository
import com.example.ui.screens.CodecScreen
import com.example.ui.screens.DispatchScreen
import com.example.ui.screens.InspectHubScreen
import com.example.ui.screens.NodeScreen
import com.example.ui.screens.RelaysScreen
import com.example.ui.theme.CoreLensTheme
import com.example.ui.viewmodel.ChainSpecsViewModel
import com.example.ui.viewmodel.CodecViewModel
import com.example.ui.viewmodel.DispatchViewModel
import com.example.ui.viewmodel.NodeViewModel
import com.example.ui.viewmodel.RelaysViewModel
import com.example.ui.viewmodel.TxInspectorViewModel

const val ROUTE_NODE = "node"
const val ROUTE_RELAYS = "relays"
const val ROUTE_DISPATCH = "dispatch"
const val ROUTE_CODEC = "codec"
const val ROUTE_INSPECT = "inspect"

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
        route = ROUTE_RELAYS,
        labelResId = R.string.nav_relays,
        selectedIcon = Icons.Filled.Router,
        unselectedIcon = Icons.Outlined.Router,
        testTag = "nav_tab_relays"
    ),
    TopLevelDestination(
        route = ROUTE_DISPATCH,
        labelResId = R.string.nav_dispatch,
        selectedIcon = Icons.Filled.Inbox,
        unselectedIcon = Icons.Outlined.Inbox,
        testTag = "nav_tab_dispatch"
    ),
    TopLevelDestination(
        route = ROUTE_CODEC,
        labelResId = R.string.nav_codec,
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code,
        testTag = "nav_tab_codec"
    ),
    TopLevelDestination(
        route = ROUTE_INSPECT,
        labelResId = R.string.nav_inspect,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "nav_tab_inspect"
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
    val context = LocalContext.current.applicationContext
    val repository = remember {
        CoreLensRepository(
            db = CoreLensDatabase.getInstance(context),
            preferences = CoreLensPreferences(context)
        )
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_NODE

    val nodeViewModel: NodeViewModel = viewModel(factory = NodeViewModel.Factory(repository.preferences))
    val relaysViewModel: RelaysViewModel = viewModel(factory = RelaysViewModel.Factory(repository))
    val dispatchViewModel: DispatchViewModel = viewModel(factory = DispatchViewModel.Factory(repository))
    val codecViewModel: CodecViewModel = viewModel()
    val txViewModel: TxInspectorViewModel = viewModel()
    val chainSpecsViewModel: ChainSpecsViewModel = viewModel()

    val nodeState by nodeViewModel.uiState.collectAsStateWithLifecycle()
    val relaysState by relaysViewModel.uiState.collectAsStateWithLifecycle()
    val dispatchState by dispatchViewModel.uiState.collectAsStateWithLifecycle()
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
                                label = { Text(label, maxLines = 1) },
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
                    composable(ROUTE_RELAYS) {
                        RelaysScreen(uiState = relaysState, viewModel = relaysViewModel)
                    }
                    composable(ROUTE_DISPATCH) {
                        DispatchScreen(uiState = dispatchState, viewModel = dispatchViewModel)
                    }
                    composable(ROUTE_CODEC) {
                        CodecScreen(uiState = codecState, viewModel = codecViewModel)
                    }
                    composable(ROUTE_INSPECT) {
                        InspectHubScreen(
                            txState = txState,
                            txViewModel = txViewModel,
                            chainSpecsState = chainSpecsState,
                            chainSpecsViewModel = chainSpecsViewModel
                        )
                    }
                }
            }
        }
    }
}
