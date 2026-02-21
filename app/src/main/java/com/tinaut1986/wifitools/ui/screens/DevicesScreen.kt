package com.tinaut1986.wifitools.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.wifitools.data.DeviceInfo
import com.tinaut1986.wifitools.ui.components.*
import com.tinaut1986.wifitools.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DevicesScreen(devices: List<DeviceInfo>, isScanning: Boolean, onRefresh: () -> Unit) {
    var selectedIp by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${devices.size} devices found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            IconButton(
                onClick = onRefresh,
                enabled = !isScanning,
                colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = PrimaryBlue)
                }
            }
        }

        if (devices.isEmpty() && !isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No devices found. Start a scan!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Subnet Scan Map", color = PrimaryBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    NetworkMap(devices, selectedIp) { ip ->
                        selectedIp = ip
                        val index = devices.indexOfFirst { it.ip == ip }
                        if (index >= 0) {
                            scope.launch {
                                // +2 to account for the Map and Header items in LazyColumn
                                listState.animateScrollToItem(index + 2)
                            }
                        }
                    }
                }
                
                item {
                    Text("Device List", color = PrimaryBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }

                items(devices) { device ->
                    DeviceItem(device, isSelected = device.ip == selectedIp)
                }
            }
        }
    }
}

@Composable
fun NetworkMap(devices: List<DeviceInfo>, selectedIp: String?, onIpClick: (String) -> Unit) {
    val prefix = remember(devices) { 
        devices.firstOrNull()?.ip?.substringBeforeLast(".")?.let { "$it." } ?: "192.168.1."
    }
    val activeIps = remember(devices) { 
        devices.associateBy { it.ip.substringAfterLast(".").toIntOrNull() ?: -1 }
    }

    PremiumCard {
        Column {
            for (row in 0 until 16) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 16) {
                        val ipLastOctet = row * 16 + col
                        if (ipLastOctet in 1..254) {
                            val device = activeIps[ipLastOctet]
                            val isActive = device != null
                            val fullIp = "$prefix$ipLastOctet"
                            val isSelected = selectedIp == fullIp
                            
                            val color = when {
                                isSelected -> Color.White
                                isActive -> PrimaryBlue
                                else -> Color.White.copy(alpha = 0.05f)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .padding(1.dp)
                                    .size(14.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                                    .let {
                                        if (isActive) it.clickable { onIpClick(fullIp) }
                                        else it
                                    }
                            )
                        } else {
                            Spacer(Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(PrimaryBlue, RoundedCornerShape(2.dp)))
                Text(" Active", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(end = 16.dp))
                Box(modifier = Modifier.size(10.dp).background(Color.White, RoundedCornerShape(2.dp)))
                Text(" Selected", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(end = 16.dp))
                Box(modifier = Modifier.size(10.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp)))
                Text(" Empty", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DeviceItem(device: DeviceInfo, isSelected: Boolean) {
    val backgroundColor = if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else CardBackground
    val borderColor = if (isSelected) PrimaryBlue else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected) BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryPurple.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Computer, contentDescription = null, tint = PrimaryPurple)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(device.hostname.ifEmpty { "Unknown Device" }, color = Color.White, fontWeight = FontWeight.Bold)
                Text(device.ip, color = Color.Gray, fontSize = 12.sp)
            }
            
            if (device.isReachable) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00FF88).copy(alpha = 0.1f),
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}
