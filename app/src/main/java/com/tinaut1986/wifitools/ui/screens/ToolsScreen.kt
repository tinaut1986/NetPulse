package com.tinaut1986.wifitools.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@Composable
fun ToolsScreen(
    pingResult: String?,
    toolResult: String?,
    publicIp: String,
    isPinging: Boolean,
    onPing: (String) -> Unit,
    onStopPing: () -> Unit,
    onPortCheck: (String, Int) -> Unit,
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
            1 -> DnsPortSection(host, port, toolResult, { host = it }, { port = it }, onDnsLookup, onPortCheck)
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
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onDns: (String) -> Unit,
    onPort: (String, Int) -> Unit
) {
    PremiumCard {
        ToolHeader(Icons.Default.Search, stringResource(R.string.dns_port_title))
        ToolInput(host, stringResource(R.string.host_url_label), onHostChange)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text(stringResource(R.string.port_label)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onPort(host, port.toIntOrNull() ?: 80) },
                modifier = Modifier.align(Alignment.CenterVertically),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.check_btn), fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { onDns(host) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Text(stringResource(R.string.dns_lookup_btn), fontSize = 14.sp)
        }
        
        ResultDisplay(result)
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
