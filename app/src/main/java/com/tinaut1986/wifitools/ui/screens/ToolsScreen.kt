package com.tinaut1986.wifitools.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.wifitools.R
import com.tinaut1986.wifitools.ui.components.PremiumCard
import com.tinaut1986.wifitools.ui.theme.*

// Known service names for the port chip labels
private val PORT_SERVICES_TOOLS = mapOf(
    21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP",
    53 to "DNS", 80 to "HTTP", 110 to "POP3", 135 to "RPC",
    139 to "NetBIOS", 143 to "IMAP", 443 to "HTTPS", 445 to "SMB",
    554 to "RTSP", 631 to "IPP", 3306 to "MySQL", 3389 to "RDP",
    5357 to "WSD", 5900 to "VNC", 7000 to "AirPlay", 8008 to "Cast",
    8009 to "Cast", 8060 to "Roku", 8080 to "HTTP-Alt", 8443 to "HTTPS-Alt",
    9100 to "JetDirect"
)

@Composable
fun ToolsScreen(
    pingResult: String?,
    toolResult: String?,
    publicIp: String,
    isPinging: Boolean,
    isPortScanning: Boolean = false,
    portScanProgress: Float = 0f,
    portScanResults: List<Int> = emptyList(),
    onPing: (String) -> Unit,
    onStopPing: () -> Unit,
    onPortCheck: (String, Int) -> Unit,
    onFullPortScan: (String) -> Unit = {},
    onStopPortScan: () -> Unit = {},
    onDnsLookup: (String) -> Unit,
    onTraceroute: (String) -> Unit
) {
    var host by remember { mutableStateOf("google.com") }
    var port by remember { mutableStateOf("80") }
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.ping_tool),
        stringResource(R.string.dns_port_tool),
        stringResource(R.string.trace_tool)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Public IP Info
        PremiumCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.internet_status), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text("${stringResource(R.string.public_ip)}: $publicIp", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryBlue,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PrimaryBlue
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> PingSection(host, isPinging, pingResult, { host = it }, onPing, onStopPing)
            1 -> DnsPortSection(
                host, port, toolResult,
                isPortScanning, portScanProgress, portScanResults,
                { host = it }, { port = it },
                onDnsLookup, onPortCheck, onFullPortScan, onStopPortScan
            )
            2 -> TraceSection(host, isPinging, toolResult, { host = it }, onTraceroute)
        }
    }
}

@Composable
fun PingSection(
    host: String,
    isPinging: Boolean,
    result: String?,
    onHostChange: (String) -> Unit,
    onStart: (String) -> Unit,
    onStop: () -> Unit
) {
    PremiumCard {
        ToolHeader(Icons.Default.Terminal, stringResource(R.string.ping_title))
        ToolInput(host, stringResource(R.string.host_ip_label), onHostChange, !isPinging)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { if (isPinging) onStop() else onStart(host) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (isPinging) SignalRed else PrimaryBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(if (isPinging) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPinging) stringResource(R.string.stop_ping) else stringResource(R.string.start_ping), fontSize = 14.sp)
        }

        ResultDisplay(result)
    }
}

@Composable
fun DnsPortSection(
    host: String,
    port: String,
    result: String?,
    isPortScanning: Boolean,
    portScanProgress: Float,
    portScanResults: List<Int>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onDns: (String) -> Unit,
    onPort: (String, Int) -> Unit,
    onFullPortScan: (String) -> Unit,
    onStopPortScan: () -> Unit
) {
    PremiumCard {
        ToolHeader(Icons.Default.Search, stringResource(R.string.dns_port_title))
        ToolInput(host, stringResource(R.string.host_url_label), onHostChange, !isPortScanning)

        Spacer(modifier = Modifier.height(12.dp))

        // ── Port check / Full port scan ────────────────────────────────
        val isFullScanMode = port.trim().isEmpty()

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text(stringResource(R.string.port_label_optional)) },
                modifier = Modifier.weight(1f),
                enabled = !isPortScanning,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (isPortScanning) {
                        onStopPortScan()
                    } else if (isFullScanMode) {
                        onFullPortScan(host)
                    } else {
                        onPort(host, port.toIntOrNull() ?: 80)
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isPortScanning -> SignalRed
                        isFullScanMode -> Color(0xFF1A7F5A)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                if (isFullScanMode || isPortScanning) {
                    Icon(
                        if (isPortScanning) Icons.Default.Stop else Icons.Default.ManageSearch,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = when {
                        isPortScanning -> stringResource(R.string.stop_scan)
                        isFullScanMode -> stringResource(R.string.scan_all)
                        else -> stringResource(R.string.check_btn)
                    },
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── DNS Lookup ────────────────────────────────────────────────
        Button(
            onClick = { onDns(host) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isPortScanning,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Text(stringResource(R.string.dns_lookup_btn), fontSize = 14.sp)
        }

        // Progress bar (visible while scanning)
        if (isPortScanning || portScanResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            if (isPortScanning) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${stringResource(R.string.scanning_ports)}…",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Text(
                            "${(portScanProgress * 100).toInt()}%",
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { portScanProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryBlue,
                        trackColor = PrimaryBlue.copy(alpha = 0.15f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Live results chips
            if (portScanResults.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.ports_found, portScanResults.size),
                        color = Color(0xFF00DD77),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(portScanResults) { p ->
                        val service = PORT_SERVICES_TOOLS[p] ?: "?"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00DD77).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF00DD77).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$p", color = Color(0xFF00DD77), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(service, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        ResultDisplay(if (isPortScanning) null else result)
    }
}


@Composable
fun TraceSection(
    host: String,
    isPinging: Boolean,
    result: String?,
    onHostChange: (String) -> Unit,
    onTrace: (String) -> Unit
) {
    PremiumCard {
        ToolHeader(Icons.Default.Map, stringResource(R.string.traceroute_title))
        ToolInput(host, stringResource(R.string.target_host_label), onHostChange, !isPinging)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onTrace(host) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isPinging,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.run_traceroute_btn), fontSize = 14.sp)
        }
        
        ResultDisplay(result)
    }
}

@Composable
fun ToolHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun ToolInput(value: String, label: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun ResultDisplay(result: String?) {
    if (result != null) {
        val scrollState = rememberScrollState()
        
        LaunchedEffect(result) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp),
            color = Color(0xFF0F0F1A), // Fixed dark background for terminal look
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier
                .padding(12.dp)
                .verticalScroll(scrollState)
            ) {
                Text(
                    result,
                    color = PrimaryBlue,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}
