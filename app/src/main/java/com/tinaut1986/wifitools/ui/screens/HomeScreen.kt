package com.tinaut1986.wifitools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.wifitools.data.WifiInfo
import com.tinaut1986.wifitools.ui.components.*
import com.tinaut1986.wifitools.ui.theme.*

@Composable
fun HomeScreen(wifiInfo: WifiInfo, signalHistory: List<Int>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "WiFi Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Main Signal Card
        PremiumCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(wifiInfo.ssid, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(wifiInfo.bssid, color = Color.Gray, fontSize = 12.sp)
                    }
                    val signalColor = when {
                        wifiInfo.rssi > -60 -> SignalGreen
                        wifiInfo.rssi > -80 -> SignalYellow
                        else -> SignalRed
                    }
                    Icon(Icons.Default.SignalWifi4Bar, contentDescription = null, tint = signalColor, modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("${wifiInfo.rssi} dBm", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text("Signal Strength", color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SignalGraph(history = signalHistory)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Speed",
                value = "${wifiInfo.linkSpeed} Mbps",
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            InfoCard(
                title = "Frequency",
                value = "${wifiInfo.frequency} MHz",
                icon = Icons.Default.Info,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Network Config Card
        PremiumCard {
            Text("Network Details", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow("IP Address", wifiInfo.ipAddress)
            DetailRow("Gateway", wifiInfo.gateway)
            DetailRow("Subnet Mask", wifiInfo.mask)
            DetailRow("DNS 1", wifiInfo.dns1)
        }
    }
}
