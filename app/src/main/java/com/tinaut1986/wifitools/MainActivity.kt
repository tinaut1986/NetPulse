package com.tinaut1986.wifitools

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tinaut1986.wifitools.ui.screens.DevicesScreen
import com.tinaut1986.wifitools.ui.screens.HomeScreen
import com.tinaut1986.wifitools.ui.screens.ToolsScreen
import com.tinaut1986.wifitools.ui.theme.WifiToolsTheme
import com.tinaut1986.wifitools.ui.theme.BackgroundDark
import com.tinaut1986.wifitools.ui.theme.CardBackground
import com.tinaut1986.wifitools.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        setContent {
            WifiToolsTheme {
                val viewModel: WifiViewModel = viewModel()
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentRoute by remember { mutableStateOf("home") }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(300.dp),
                            drawerContainerColor = CardBackground
                        ) {
                            Spacer(Modifier.height(48.dp))
                            Text(
                                "WiFi Tools",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.headlineMedium,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Dashboard") },
                                selected = currentRoute == "home",
                                onClick = {
                                    currentRoute = "home"
                                    navController.navigate("home")
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Devices, contentDescription = null) },
                                label = { Text("Network Map") },
                                selected = currentRoute == "devices",
                                onClick = {
                                    currentRoute = "devices"
                                    navController.navigate("devices")
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                                label = { Text("Tools") },
                                selected = currentRoute == "tools",
                                onClick = {
                                    currentRoute = "tools"
                                    navController.navigate("tools")
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("WiFi Tools", color = Color.White, fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = BackgroundDark
                                )
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                val wifiInfo by viewModel.wifiInfo.collectAsState()
                                HomeScreen(wifiInfo, viewModel.signalHistory)
                            }
                            composable("devices") {
                                val devices by viewModel.devices.collectAsState()
                                val isScanning by viewModel.isScanning.collectAsState()
                                DevicesScreen(devices, isScanning, onRefresh = { viewModel.scanDevices() })
                            }
                            composable("tools") {
                                 val pingResult by viewModel.pingResult.collectAsState()
                                 val isPinging by viewModel.isPinging.collectAsState()
                                 ToolsScreen(
                                     onPing = { viewModel.runPing(it) },
                                     onStopPing = { viewModel.stopPing() },
                                     pingResult = pingResult,
                                     isPinging = isPinging
                                 )
                            }
                        }
                    }
                }
            }
        }
    }
}