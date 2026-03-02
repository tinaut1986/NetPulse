package com.tinaut1986.netpulse

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tinaut1986.netpulse.ui.screens.DevicesScreen
import com.tinaut1986.netpulse.ui.screens.DiagnosticDetailScreen
import com.tinaut1986.netpulse.ui.screens.DiagnosticHistoryScreen
import com.tinaut1986.netpulse.ui.screens.HomeScreen
import com.tinaut1986.netpulse.ui.screens.NetworkQualityScreen
import com.tinaut1986.netpulse.ui.screens.SettingsScreen
import com.tinaut1986.netpulse.ui.screens.SpeedTestScreen
import com.tinaut1986.netpulse.ui.theme.NetPulseTheme
import com.tinaut1986.netpulse.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch
import java.util.Locale

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
                NetPulseTheme(themeMode = currentTheme) {
                    val viewModel: NetPulseViewModel = viewModel()
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
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_logo),
                                        contentDescription = "NetPulse Logo",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                    // CATEGORY: MAIN
                                    DrawerCategoryHeader(stringResource(R.string.nav_main))
                                    DrawerItem(stringResource(R.string.dashboard), Icons.Default.Home, "home", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.network_map), Icons.Default.Devices, "devices", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.network_quality), Icons.Default.NetworkCheck, "network_quality", currentRoute, navController, scope, drawerState)

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                    // CATEGORY: NETWORK AUDIT
                                    DrawerCategoryHeader(stringResource(R.string.nav_network_tools))
                                    DrawerItem(stringResource(R.string.ping_tool), Icons.Default.Terminal, "tool_ping", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.dns_tool), Icons.Default.Search, "tool_dns", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.port_tool),
                                        Icons.AutoMirrored.Filled.ManageSearch, "tool_port", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.trace_tool), Icons.Default.Map, "tool_trace", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.wifi_explorer_tool), Icons.Default.Wifi, "tool_wifi", currentRoute, navController, scope, drawerState)

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                    // CATEGORY: UTILITIES
                                    DrawerCategoryHeader(stringResource(R.string.nav_utilities))
                                    DrawerItem(stringResource(R.string.speed_test), Icons.Default.Speed, "speed_test", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.wol_tool), Icons.Default.FlashOn, "tool_wol", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.subnet_calc_tool), Icons.Default.Calculate, "tool_subnet", currentRoute, navController, scope, drawerState)
                                    DrawerItem(stringResource(R.string.whois_tool), Icons.Default.Info, "tool_whois", currentRoute, navController, scope, drawerState)

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                DrawerItem(stringResource(R.string.settings), Icons.Default.Settings, "settings", currentRoute, navController, scope, drawerState)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                val (titleRes, iconVector, isSubmenu) = when (currentRoute) {
                                    "home" -> Triple(R.string.dashboard, Icons.Default.Home, false)
                                    "devices" -> Triple(R.string.network_map, Icons.Default.Devices, false)
                                    "network_quality" -> Triple(R.string.network_quality, Icons.Default.NetworkCheck, false)
                                    "tool_ping" -> Triple(R.string.ping_tool, Icons.Default.Terminal, false)
                                    "tool_dns" -> Triple(R.string.dns_tool, Icons.Default.Search, false)
                                    "tool_port" -> Triple(R.string.port_tool, Icons.AutoMirrored.Filled.ManageSearch, false)
                                    "tool_trace" -> Triple(R.string.trace_tool, Icons.Default.Map, false)
                                    "tool_wifi" -> Triple(R.string.wifi_explorer_tool, Icons.Default.Wifi, false)
                                    "speed_test" -> Triple(R.string.speed_test, Icons.Default.Speed, false)
                                    "tool_wol" -> Triple(R.string.wol_tool, Icons.Default.FlashOn, false)
                                    "tool_subnet" -> Triple(R.string.subnet_calc_tool, Icons.Default.Calculate, false)
                                    "tool_whois" -> Triple(R.string.whois_tool, Icons.Default.Info, false)
                                    "settings" -> Triple(R.string.settings, Icons.Default.Settings, false)
                                    "history", "history_detail" -> Triple(R.string.history_title, Icons.Default.History, true)
                                    else -> Triple(R.string.app_name, Icons.Default.Home, false)
                                }

                                CenterAlignedTopAppBar(
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = stringResource(titleRes),
                                                tint = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                stringResource(titleRes),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        if (isSubmenu) {
                                            IconButton(onClick = { navController.popBackStack() }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        } else {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                                            }
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
                                    val scanProgress by viewModel.scanProgress.collectAsState()
                                    val wifiInfo by viewModel.wifiInfo.collectAsState()
                                    DevicesScreen(
                                        devices = devices,
                                        isScanning = isScanning,
                                        scanProgress = scanProgress,
                                        currentIp = wifiInfo.ipAddress,
                                        onRefresh = { viewModel.scanDevices() }
                                    )
                                }
                                composable("tool_ping") {
                                    val pingResult by viewModel.pingResult.collectAsState()
                                    val isPinging by viewModel.isPinging.collectAsState()
                                    val toolHost by viewModel.toolHost.collectAsState()
                                    com.tinaut1986.netpulse.ui.screens.PingScreen(
                                        host = toolHost,
                                        isPinging = isPinging,
                                        result = pingResult,
                                         onHostChange = { viewModel.updateToolHost(it) },
                                         onStart = { viewModel.runPing(it) },
                                         onStop = { viewModel.stopPing() },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("tool_dns") {
                                    val dnsResult by viewModel.dnsResult.collectAsState()
                                    val toolHost by viewModel.toolHost.collectAsState()

                                    com.tinaut1986.netpulse.ui.screens.DnsLookupScreen(
                                        host = toolHost,
                                        dnsResult = dnsResult,
                                         onHostChange = { viewModel.updateToolHost(it) },
                                         onDns = { viewModel.runDnsLookup(it) },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("tool_port") {
                                    val portResult by viewModel.portResult.collectAsState()
                                    val isPortScanning by viewModel.isPortScanning.collectAsState()
                                    val portScanProgress by viewModel.portScanProgress.collectAsState()
                                    val portScanResults by viewModel.portScanResults.collectAsState()
                                    val toolHost by viewModel.toolHost.collectAsState()
                                    val toolPort by viewModel.toolPort.collectAsState()

                                    com.tinaut1986.netpulse.ui.screens.PortScannerScreen(
                                        host = toolHost,
                                        port = toolPort,
                                        portResult = portResult,
                                        isPortScanning = isPortScanning,
                                        portScanProgress = portScanProgress,
                                        portScanResults = portScanResults,
                                        onHostChange = { viewModel.updateToolHost(it) },
                                         onPortChange = { viewModel.updateToolPort(it) },
                                         onPort = { host, port -> viewModel.runPortCheck(host, port) },
                                         onFullPortScan = { viewModel.runFullPortScan(it) },
                                         onStopPortScan = { viewModel.stopPortScan() },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("tool_trace") {
                                    val traceResult by viewModel.traceResult.collectAsState()
                                    val isPinging by viewModel.isPinging.collectAsState()
                                    val toolHost by viewModel.toolHost.collectAsState()
                                    com.tinaut1986.netpulse.ui.screens.TraceScreen(
                                        host = toolHost,
                                        isPinging = isPinging,
                                        result = traceResult,
                                         onHostChange = { viewModel.updateToolHost(it) },
                                         onTrace = { viewModel.runTraceroute(it) },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("tool_wol") {
                                    val wolResult by viewModel.wolResult.collectAsState()
                                     com.tinaut1986.netpulse.ui.screens.WolScreen(
                                         result = wolResult,
                                         onWol = { viewModel.wakeOnLan(it) },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("tool_subnet") {
                                    val subnetInfo by viewModel.subnetInfo.collectAsState()
                                     com.tinaut1986.netpulse.ui.screens.SubnetCalcScreen(
                                         subnetInfo = subnetInfo,
                                         onCalculate = { ip, mask -> viewModel.calculateSubnet(ip, mask) },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("tool_wifi") {
                                    val nearbyWifi by viewModel.nearbyWifi.collectAsState()
                                     com.tinaut1986.netpulse.ui.screens.WifiExplorerScreen(
                                         nearbyWifi = nearbyWifi,
                                         onScan = { viewModel.scanNearbyWifi() },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("tool_whois") {
                                    val whoisResult by viewModel.whoisResult.collectAsState()
                                    val toolHost by viewModel.toolHost.collectAsState()
                                    com.tinaut1986.netpulse.ui.screens.WhoisScreen(
                                        host = toolHost,
                                         result = whoisResult,
                                         onHostChange = { viewModel.updateToolHost(it) },
                                         onWhois = { viewModel.runWhois(it) },
                                         onBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("speed_test") {
                                    val isTesting by viewModel.isTestingSpeed.collectAsState()
                                    val progress by viewModel.speedTestProgress.collectAsState()
                                    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
                                    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
                                    val latency by viewModel.speedTestLatency.collectAsState()
                                    val jitter by viewModel.speedTestJitter.collectAsState()
                                    val phase by viewModel.speedTestPhase.collectAsState()

                                    SpeedTestScreen(
                                        isTesting = isTesting,
                                        progress = progress,
                                        downloadSpeed = downloadSpeed,
                                        uploadSpeed = uploadSpeed,
                                        latency = latency,
                                        jitter = jitter,
                                        phase = phase,
                                        onStartTest = { viewModel.runSpeedTest() },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                 composable("settings") {
                                    SettingsScreen(
                                        currentTheme = currentTheme,
                                        onThemeChange = { settingsManager.setTheme(it) },
                                        currentLanguage = currentLanguage,
                                        onLanguageChange = { settingsManager.setLanguage(it) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("network_quality") {
                                    val report by viewModel.diagnosticReport.collectAsState()
                                    val isDiagnosing by viewModel.isDiagnosing.collectAsState()
                                    val diagnosisStep by viewModel.diagnosisStep.collectAsState()
                                    NetworkQualityScreen(
                                        report = report,
                                        liveLatencySamples = viewModel.liveLatencySamples,
                                        isDiagnosing = isDiagnosing,
                                        diagnosisStep = diagnosisStep,
                                        onStartDiagnosis = { viewModel.runDiagnosis() },
                                        onStopDiagnosis = { viewModel.stopDiagnosis() },
                                        onHistoryClick = {
                                            viewModel.refreshHistory()
                                            navController.navigate("history")
                                        }
                                    )
                                }
                                 composable("history") {
                                    val history by viewModel.historyList.collectAsState()
                                    DiagnosticHistoryScreen(
                                        entries = history,
                                        onOpen = { id ->
                                            viewModel.loadDetail(id)
                                            navController.navigate("history_detail")
                                        },
                                        onDelete = { id -> viewModel.deleteHistoryEntry(id) },
                                        onDeleteAll = { viewModel.deleteAllHistory() },
                                        onExport = { id -> viewModel.exportDiagnostic(id) },
                                        onDeleteMultiple = { ids -> viewModel.deleteMultipleDiagnostics(ids) },
                                        onExportMultiple = { ids -> viewModel.exportMultipleDiagnostics(ids) },
                                        onExportAll = { viewModel.exportAllDiagnostics() },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("history_detail") {
                                    val entry by viewModel.detailEntry.collectAsState()
                                    val report by viewModel.detailReport.collectAsState()
                                    if (entry != null && report != null) {
                                        DiagnosticDetailScreen(
                                            entry = entry!!,
                                            report = report!!,
                                            onExport = { viewModel.exportDiagnostic(entry!!.id) },
                                            onBack = {
                                                viewModel.clearDetail()
                                                navController.popBackStack()
                                            }
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
}

@Composable
fun DrawerCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlue,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    route: String,
    currentRoute: String,
    navController: androidx.navigation.NavController,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: DrawerState
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = currentRoute == route,
        onClick = {
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo("home") { saveState = true }
                restoreState = true
            }
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
