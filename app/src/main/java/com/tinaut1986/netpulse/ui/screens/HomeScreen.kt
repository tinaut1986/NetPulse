package com.tinaut1986.netpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.netpulse.R
import com.tinaut1986.netpulse.data.WifiInfo
import com.tinaut1986.netpulse.ui.components.*
import com.tinaut1986.netpulse.ui.theme.*

@Composable
fun HomeScreen(wifiInfo: WifiInfo, signalHistory: List<Int>, publicIp: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Unified Screen Header
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.dashboard),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        // Main Signal Card
        PremiumCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wifiInfo.ssid,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (wifiInfo.isWifi) wifiInfo.bssid else stringResource(R.string.cellular_network),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    val signalColor = when {
                        !wifiInfo.isWifi -> PrimaryPurple
                        wifiInfo.rssi > -60 -> SignalGreen
                        wifiInfo.rssi > -80 -> SignalYellow
                        else -> SignalRed
                    }
                    Icon(
                        imageVector = if (wifiInfo.isWifi) Icons.Default.SignalWifi4Bar else Icons.Default.SignalCellular4Bar,
                        contentDescription = null,
                        tint = signalColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (wifiInfo.isWifi) {
                    Text("${wifiInfo.rssi} dBm", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.signal_strength), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    SignalGraph(history = signalHistory)
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.cellular_mode_active), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = stringResource(R.string.speed),
                value = if (wifiInfo.isWifi) "${wifiInfo.linkSpeed} Mbps" else "LTE/5G",
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            InfoCard(
                title = stringResource(R.string.latency),
                value = "${wifiInfo.avgLatency} ms",
                icon = Icons.Default.History,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            if (wifiInfo.isWifi) {
                InfoCard(
                    title = stringResource(R.string.channel),
                    value = "${wifiInfo.getChannel()}",
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            InfoCard(
                title = stringResource(R.string.loss),
                value = "${wifiInfo.packetLoss.toInt()}%",
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Network Config Card
        PremiumCard {
            Text(stringResource(R.string.network_details), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(stringResource(R.string.ip_address), wifiInfo.ipAddress)
            DetailRow(stringResource(R.string.public_ip), publicIp)
            
            if (wifiInfo.isWifi) {
                DetailRow(stringResource(R.string.frequency), "${wifiInfo.frequency} MHz")
                DetailRow(stringResource(R.string.gateway), wifiInfo.gateway)
                DetailRow(stringResource(R.string.dns_1), wifiInfo.dns1)
            } else {
                DetailRow("Status", "Mobile Connected")
            }
        }
    }
}
