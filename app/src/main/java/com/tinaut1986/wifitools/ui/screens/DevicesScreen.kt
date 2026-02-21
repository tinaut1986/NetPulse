package com.tinaut1986.wifitools.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.tinaut1986.wifitools.R
import com.tinaut1986.wifitools.data.DeviceInfo
import com.tinaut1986.wifitools.ui.components.*
import com.tinaut1986.wifitools.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DevicesScreen(
    devices: List<DeviceInfo>,
    isScanning: Boolean,
    currentIp: String,
    onRefresh: () -> Unit
) {
    var selectedIp by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.connected_devices),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.devices_found, devices.size),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp
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
                Text(stringResource(R.string.no_devices), color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(stringResource(R.string.subnet_scan_map), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                    NetworkMap(devices, selectedIp, currentIp) { ip ->
                        selectedIp = ip
                    }
                }
                
                item {
                    Text(stringResource(R.string.device_list), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                }

                items(devices) { device ->
                    DeviceItem(
                        device, 
                        isCurrent = device.ip == currentIp,
                        isSelected = device.ip == selectedIp
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkMap(
    devices: List<DeviceInfo>,
    selectedIp: String?,
    currentIp: String,
    onIpClick: (String) -> Unit
) {
    val prefix = remember(devices) { 
        devices.firstOrNull()?.ip?.substringBeforeLast(".")?.let { "$it." } ?: "192.168.1."
    }
    val activeIps = remember(devices) { 
        devices.associateBy { it.ip.substringAfterLast(".").toIntOrNull() ?: -1 }
    }

    PremiumCard {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                                    val isLocal = fullIp == currentIp
                                    
                                    val color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isLocal -> PrimaryPurple
                                        isActive -> PrimaryBlue
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .padding(1.dp)
                                            .size(14.dp)
                                            .background(color, RoundedCornerShape(2.dp))
                                            .let {
                                                if (isSelected) it.border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(2.dp))
                                                else it
                                            }
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
                }

                // Bubble Info
                selectedIp?.let { ip ->
                    val device = devices.find { it.ip == ip }
                    if (device != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(device.hostname.ifEmpty { stringResource(R.string.unknown_device) }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(device.ip, color = PrimaryBlue, fontSize = 10.sp)
                                if (device.mac != "Unknown") {
                                    Text(device.mac, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(PrimaryPurple, RoundedCornerShape(2.dp)))
                    Text(" " + stringResource(R.string.you), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(end = 12.dp))
                    Box(modifier = Modifier.size(10.dp).background(PrimaryBlue, RoundedCornerShape(2.dp)))
                    Text(" " + stringResource(R.string.active), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(end = 12.dp))
                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    Text(" " + stringResource(R.string.selected), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
    }
}

@Composable
fun DeviceItem(device: DeviceInfo, isCurrent: Boolean, isSelected: Boolean) {
    val backgroundColor = when {
        isSelected -> PrimaryBlue.copy(alpha = 0.2f)
        isCurrent -> PrimaryPurple.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) PrimaryBlue else if (isCurrent) PrimaryPurple else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected || isCurrent) BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isCurrent) PrimaryPurple.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCurrent) Icons.Default.Computer else Icons.Default.Computer, 
                    contentDescription = null, 
                    tint = if (isCurrent) Color.White else PrimaryPurple
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        device.hostname.ifEmpty { stringResource(R.string.unknown_device) }, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (isCurrent) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            color = PrimaryPurple,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("YOU", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(device.ip, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                if (device.mac != "Unknown") {
                    Text(device.mac, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                }
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
