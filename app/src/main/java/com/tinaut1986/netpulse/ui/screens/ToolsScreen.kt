package com.tinaut1986.netpulse.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.netpulse.R
import com.tinaut1986.netpulse.data.NearbyWifi
import com.tinaut1986.netpulse.data.SubnetInfo
import com.tinaut1986.netpulse.ui.components.PremiumCard
import com.tinaut1986.netpulse.ui.theme.*

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

/**
 * A container for a single tool screen to maintain consistent UI.
 */
@Composable
fun ToolScreenContainer(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Screen Title
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        content()
    }
}

@Composable
fun PingScreen(
    host: String,
    isPinging: Boolean,
    result: String?,
    onHostChange: (String) -> Unit,
    onStart: (String) -> Unit,
    onStop: () -> Unit
) {
    ToolScreenContainer(stringResource(R.string.ping_title), Icons.Default.Terminal) {
        PremiumCard {
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
}

@Composable
fun DnsPortScreen(
    host: String,
    port: String,
    dnsResult: String?,
    portResult: String?,
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
    ToolScreenContainer(stringResource(R.string.dns_port_title), Icons.Default.Search) {
        PremiumCard {
            ToolInput(host, stringResource(R.string.host_url_label), onHostChange, !isPortScanning)

            Spacer(modifier = Modifier.height(12.dp))

            // Port check / Full port scan
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
                            if (isPortScanning) Icons.Default.Stop else Icons.AutoMirrored.Filled.ManageSearch,
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

            // DNS Lookup
            Button(
                onClick = { onDns(host) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPortScanning,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text(stringResource(R.string.dns_lookup_btn), fontSize = 14.sp)
            }

            if (isPortScanning || portScanResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                if (isPortScanning) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${stringResource(R.string.scanning_ports)}…", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text("${(portScanProgress * 100).toInt()}%", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(progress = { portScanProgress }, modifier = Modifier.fillMaxWidth(), color = PrimaryBlue, trackColor = PrimaryBlue.copy(alpha = 0.15f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (portScanResults.isNotEmpty()) {
                    Text(stringResource(R.string.ports_found, portScanResults.size), color = Color(0xFF00DD77), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(portScanResults) { p ->
                            val service = PORT_SERVICES_TOOLS[p] ?: "?"
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF00DD77).copy(alpha = 0.12f), border = BorderStroke(1.dp, Color(0xFF00DD77).copy(alpha = 0.4f))) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$p", color = Color(0xFF00DD77), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(service, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
            ResultDisplay(if (isPortScanning) null else portResult)
            ResultDisplay(dnsResult)
        }
    }
}

@Composable
fun TraceScreen(host: String, isPinging: Boolean, result: String?, onHostChange: (String) -> Unit, onTrace: (String) -> Unit) {
    ToolScreenContainer(stringResource(R.string.traceroute_title), Icons.Default.Map) {
        PremiumCard {
            ToolInput(host, stringResource(R.string.target_host_label), onHostChange, !isPinging)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onTrace(host) }, modifier = Modifier.fillMaxWidth(), enabled = !isPinging, shape = RoundedCornerShape(8.dp)) {
                Text(stringResource(R.string.run_traceroute_btn), fontSize = 14.sp)
            }
            ResultDisplay(result)
        }
    }
}

@Composable
fun WolScreen(onWol: (String) -> Unit) {
    var mac by remember { mutableStateOf("") }
    ToolScreenContainer(stringResource(R.string.wol_title), Icons.Default.FlashOn) {
        PremiumCard {
            ToolInput(mac, stringResource(R.string.mac_label), { mac = it })
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onWol(mac) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.send_wol), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SubnetCalcScreen(subnetInfo: SubnetInfo?, onCalculate: (String, String) -> Unit) {
    var ip by remember { mutableStateOf("192.168.1.1") }
    var mask by remember { mutableStateOf("24") }
    ToolScreenContainer(stringResource(R.string.subnet_calc_title), Icons.Default.Calculate) {
        PremiumCard {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP") }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(8.dp))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(value = mask, onValueChange = { mask = it }, label = { Text("Mask/CIDR") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onCalculate(ip, mask) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                Text(stringResource(R.string.calculate))
            }
            if (subnetInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    SubnetRow(stringResource(R.string.network_address), subnetInfo.networkAddress)
                    SubnetRow(stringResource(R.string.broadcast_address), subnetInfo.broadcastAddress)
                    SubnetRow(stringResource(R.string.first_host), subnetInfo.firstHost)
                    SubnetRow(stringResource(R.string.last_host), subnetInfo.lastHost)
                    SubnetRow(stringResource(R.string.total_hosts), subnetInfo.totalHosts.toString())
                }
            }
        }
    }
}

@Composable
fun SubnetRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WhoisScreen(host: String, result: String?, onHostChange: (String) -> Unit, onWhois: (String) -> Unit) {
    ToolScreenContainer(stringResource(R.string.whois_title), Icons.Default.Info) {
        PremiumCard {
            ToolInput(host, stringResource(R.string.host_url_label), onHostChange)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onWhois(host) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                Text(stringResource(R.string.whois_lookup_btn))
            }
            ResultDisplay(result)
        }
    }
}

@Composable
fun WifiExplorerScreen(nearbyWifi: List<NearbyWifi>, onScan: () -> Unit) {
    ToolScreenContainer(stringResource(R.string.wifi_explorer_title), Icons.Default.Wifi) {
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.wifi_explorer_title))
        }

        Column(modifier = Modifier.weight(1f)) {
            nearbyWifi.forEach { wifi ->
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wifi.ssid, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(wifi.bssid, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(wifi.capabilities, fontSize = 10.sp, color = PrimaryPurple, maxLines = 1)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${wifi.rssi} dBm", fontWeight = FontWeight.Bold, color = if (wifi.rssi > -60) SignalGreen else if (wifi.rssi > -80) SignalYellow else SignalRed)
                            Text("${wifi.frequency} MHz", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
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
        shape = RoundedCornerShape(8.dp),
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
        LaunchedEffect(result) { scrollState.animateScrollTo(scrollState.maxValue) }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp), color = Color(0xFF0F0F1A), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Box(modifier = Modifier.padding(12.dp).verticalScroll(scrollState)) {
                Text(result, color = PrimaryBlue, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}
