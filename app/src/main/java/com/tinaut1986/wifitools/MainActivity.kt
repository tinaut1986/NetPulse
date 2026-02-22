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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.currentBackStackEntryAsState
import android.content.res.Configuration
import java.util.Locale
import com.tinaut1986.wifitools.ui.screens.DevicesScreen
import com.tinaut1986.wifitools.ui.screens.HomeScreen
import com.tinaut1986.wifitools.ui.screens.ToolsScreen
import com.tinaut1986.wifitools.ui.screens.SettingsScreen
import com.tinaut1986.wifitools.ui.screens.SpeedTestScreen
import com.tinaut1986.wifitools.ui.theme.WifiToolsTheme
import com.tinaut1986.wifitools.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
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
            val context = LocalContext.current
            val currentConfiguration = LocalConfiguration.current
            val settingsManager = remember { SettingsManager(context) }
            val currentTheme by settingsManager.theme
            val currentLanguage by settingsManager.language

            val locale = remember(currentLanguage) { Locale.forLanguageTag(currentLanguage) }
            val localizedContext = remember(currentLanguage, currentConfiguration) {
                val config = Configuration(currentConfiguration)
                config.setLocale(locale)
                context.createConfigurationContext(config)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                WifiToolsTheme(themeMode = currentTheme) {
                    val viewModel: WifiViewModel = viewModel()
                    val navController = rememberNavController()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.width(300.dp),
                                drawerContainerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Spacer(Modifier.height(48.dp))
                                Text(
                                    stringResource(R.string.app_name),
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(stringResource(R.string.dashboard)) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home")
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Devices, contentDescription = null) },
                                    label = { Text(stringResource(R.string.network_map)) },
                                    selected = currentRoute == "devices",
                                    onClick = {
                                        navController.navigate("devices")
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                                    label = { Text(stringResource(R.string.tools)) },
                                    selected = currentRoute == "tools",
                                    onClick = {
                                        navController.navigate("tools")
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                    label = { Text(stringResource(R.string.speed_test)) },
                                    selected = currentRoute == "speed_test",
                                    onClick = {
                                        navController.navigate("speed_test")
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(stringResource(R.string.settings)) },
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        navController.navigate("settings")
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { Text(stringResource(R.string.app_name), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
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
                                    val publicIp by viewModel.publicIp.collectAsState()
                                    HomeScreen(wifiInfo, viewModel.signalHistory, publicIp)
                                }
                                composable("devices") {
                                    val devices by viewModel.devices.collectAsState()
                                    val isScanning by viewModel.isScanning.collectAsState()
                                    val wifiInfo by viewModel.wifiInfo.collectAsState()
                                    DevicesScreen(
                                        devices = devices,
                                        isScanning = isScanning,
                                        currentIp = wifiInfo.ipAddress,
                                        onRefresh = { viewModel.scanDevices() }
                                    )
                                }
                                composable("tools") {
                                    val pingResult by viewModel.pingResult.collectAsState()
                                    val toolResult by viewModel.toolResult.collectAsState()
                                    val publicIp by viewModel.publicIp.collectAsState()
                                    val isPinging by viewModel.isPinging.collectAsState()
                                    val isPortScanning by viewModel.isPortScanning.collectAsState()
                                    val portScanProgress by viewModel.portScanProgress.collectAsState()
                                    val portScanResults by viewModel.portScanResults.collectAsState()

                                    ToolsScreen(
                                        pingResult = pingResult,
                                        toolResult = toolResult,
                                        publicIp = publicIp,
                                        isPinging = isPinging,
                                        isPortScanning = isPortScanning,
                                        portScanProgress = portScanProgress,
                                        portScanResults = portScanResults,
                                        onPing = { viewModel.runPing(it) },
                                        onStopPing = { viewModel.stopPing() },
                                        onPortCheck = { host, port -> viewModel.runPortCheck(host, port) },
                                        onFullPortScan = { viewModel.runFullPortScan(it) },
                                        onStopPortScan = { viewModel.stopPortScan() },
                                        onDnsLookup = { viewModel.runDnsLookup(it) },
                                        onTraceroute = { viewModel.runTraceroute(it) }
                                    )
                                }
                                composable("speed_test") {
                                    val isTesting by viewModel.isTestingSpeed.collectAsState()
                                    val progress by viewModel.speedTestProgress.collectAsState()
                                    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
                                    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
                                    val phase by viewModel.speedTestPhase.collectAsState()
                                    SpeedTestScreen(
                                        isTesting = isTesting,
                                        progress = progress,
                                        downloadSpeed = downloadSpeed,
                                        uploadSpeed = uploadSpeed,
                                        phase = phase,
                                        onStartTest = { viewModel.runSpeedTest() }
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        currentTheme = currentTheme,
                                        onThemeChange = { settingsManager.setTheme(it) },
                                        currentLanguage = currentLanguage,
                                        onLanguageChange = { settingsManager.setLanguage(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}