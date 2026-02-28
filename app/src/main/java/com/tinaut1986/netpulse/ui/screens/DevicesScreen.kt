package com.tinaut1986.netpulse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import com.tinaut1986.netpulse.data.DeviceType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.tinaut1986.netpulse.R
import com.tinaut1986.netpulse.data.DeviceInfo
import androidx.compose.ui.graphics.vector.ImageVector
import com.tinaut1986.netpulse.ui.components.*
import com.tinaut1986.netpulse.ui.theme.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Port → Service name mapping (common well-known ports)
// ─────────────────────────────────────────────────────────────────────────────
private val PORT_SERVICES = mapOf(
    21 to "FTP",
    22 to "SSH",
    23 to "Telnet",
    25 to "SMTP",
    53 to "DNS",
    80 to "HTTP",
    110 to "POP3",
    135 to "RPC",
    139 to "NetBIOS",
    143 to "IMAP",
    443 to "HTTPS",
    445 to "SMB",
    554 to "RTSP",
    631 to "IPP",
    3306 to "MySQL",
    3389 to "RDP",
    5357 to "WSD",
    5900 to "VNC",
    7000 to "AirPlay",
    8008 to "Chromecast",
    8009 to "Chromecast",
    8060 to "Roku",
    8080 to "HTTP-Alt",
    8443 to "HTTPS-Alt",
    9100 to "JetDirect"
)

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    devices: List<DeviceInfo>,
    isScanning: Boolean,
    scanProgress: Float,
    currentIp: String,
    onRefresh: () -> Unit
) {
    var selectedIp by remember { mutableStateOf<String?>(null) }
    var detailDevice by remember { mutableStateOf<DeviceInfo?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Bottom-sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show the detail sheet whenever detailDevice is set
    detailDevice?.let { device ->
        DeviceDetailSheet(
            device = device,
            isCurrent = device.ip == currentIp,
            sheetState = sheetState,
            onDismiss = { }
        )
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.network_map),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.devices_found, devices.size),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
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
                    // Scanning progress bar
                    AnimatedVisibility(
                        visible = isScanning,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = PrimaryBlue,
                                trackColor = PrimaryBlue.copy(alpha = 0.1f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${(scanProgress * 100).toInt()}%",
                                color = PrimaryBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    Text(stringResource(R.string.subnet_scan_map), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                    NetworkMap(
                        devices = devices,
                        selectedIp = selectedIp,
                        currentIp = currentIp,
                        onIpClick = { ip -> selectedIp = ip },
                        onViewDetails = { ip ->
                            devices.find { it.ip == ip }
                        }
                    )
                }

                item {
                    Text(stringResource(R.string.device_list), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                }

                items(devices) { device ->
                    DeviceItem(
                        device,
                        isCurrent = device.ip == currentIp,
                        isSelected = device.ip == selectedIp,
                        onClick = {
                            // Select in map + open detail
                            selectedIp = device.ip
                            scope.launch {
                                val idx = devices.indexOf(device)
                                if (idx >= 0) listState.animateScrollToItem(idx + 2)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Network Map
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NetworkMap(
    devices: List<DeviceInfo>,
    selectedIp: String?,
    currentIp: String,
    onIpClick: (String) -> Unit,
    onViewDetails: (String) -> Unit
) {
    val prefix = remember(devices) {
        devices.firstOrNull()?.ip?.substringBeforeLast(".")?.let { "$it." } ?: "192.168.1."
    }
    val activeIps = remember(devices) {
        devices.associateBy { it.ip.substringAfterLast(".").toIntOrNull() ?: -1 }
    }

    val labelW = 42.dp
    val gridGap = 2.dp

    PremiumCard {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalAvailableWidth = maxWidth - labelW - 16.dp

            val columns = when {
                totalAvailableWidth >= 16.dp * 40 -> 40
                totalAvailableWidth >= 16.dp * 30 -> 30
                else -> 20
            }

            val cellSize = (totalAvailableWidth - (gridGap * (columns - 1))) / columns

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gridGap)
            ) {
                // Header row
                Row(
                    modifier = Modifier.padding(start = labelW),
                    horizontalArrangement = Arrangement.spacedBy(gridGap)
                ) {
                    for (col in 0 until columns) {
                        Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                            Text(
                                text = "$col",
                                fontSize = if (columns <= 20) 7.sp else 6.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Grid rows
                val maxIp = 254
                val rowCount = (maxIp / columns) + 1

                for (row in 0 until rowCount) {
                    val rowStart = row * columns

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(gridGap)
                    ) {
                        Box(
                            modifier = Modifier.width(labelW).height(cellSize),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${rowStart}x",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        for (col in 0 until columns) {
                            val ipLastOctet = rowStart + col

                            if (ipLastOctet in 1..maxIp) {
                                val device = activeIps[ipLastOctet]
                                val isActive = device != null
                                val fullIp = "$prefix$ipLastOctet"
                                val isSelected = selectedIp == fullIp
                                val isLocal = fullIp == currentIp

                                val color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isLocal    -> PrimaryPurple
                                    isActive   -> PrimaryBlue
                                    else       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .background(color, RoundedCornerShape(2.dp))
                                        .let {
                                            if (isSelected) it.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(2.dp))
                                            else it
                                        }
                                        .let {
                                            if (isActive) it.clickable { onIpClick(fullIp) }
                                            else it
                                        }
                                )
                            } else {
                                Box(modifier = Modifier.size(cellSize))
                            }
                        }
                    }
                }

                // Bubble info for selected device
                selectedIp?.let { ip ->
                    val device = devices.find { it.ip == ip }
                    if (device != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = deviceTypeIcon(device.deviceType),
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        device.hostname.ifEmpty { stringResource(R.string.unknown_device) },
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(device.ip, color = PrimaryBlue, fontSize = 10.sp)
                                }
                                // "View Details" button
                                TextButton(
                                    onClick = { onViewDetails(ip) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.view_details),
                                        color = PrimaryBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(10.dp).padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendDot(color = PrimaryPurple, label = stringResource(R.string.you))
                    LegendDot(color = PrimaryBlue, label = stringResource(R.string.active))
                    LegendDot(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.selected))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Device Detail Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailSheet(
    device: DeviceInfo,
    isCurrent: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            // Custom drag handle with title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag pill
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isCurrent) PrimaryPurple.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = deviceTypeIcon(device.deviceType),
                            contentDescription = null,
                            tint = if (isCurrent) PrimaryPurple else PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.hostname.ifEmpty { stringResource(R.string.unknown_device) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device.ip,
                                color = PrimaryBlue,
                                fontSize = 13.sp
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = PrimaryPurple,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "YOU",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── General information ──────────────────────────────────
            item {
                DetailSectionTitle(stringResource(R.string.general_info))
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailRow(
                            icon = Icons.Default.Devices,
                            label = stringResource(R.string.device_type),
                            value = deviceTypeName(device.deviceType)
                        )
                        DetailDivider()
                        DetailRow(
                            icon = Icons.Default.Wifi,
                            label = stringResource(R.string.ip_address),
                            value = device.ip
                        )
                        DetailDivider()
                        DetailRow(
                            icon = Icons.Default.Tag,
                            label = stringResource(R.string.mac_address),
                            value = device.mac.ifBlank { "—" }.takeIf { it != "Unknown" } ?: "—"
                        )
                        DetailDivider()
                        DetailRow(
                            icon = Icons.Default.Business,
                            label = stringResource(R.string.manufacturer),
                            value = device.vendor.takeIf { it != "Unknown" } ?: "—"
                        )
                        DetailDivider()
                        DetailRow(
                            icon = Icons.Default.AlternateEmail,
                            label = stringResource(R.string.hostname_label),
                            value = device.hostname.ifEmpty { "—" }
                        )
                        DetailDivider()
                        // Online / offline status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (device.isReachable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (device.isReachable) Color(0xFF00DD77) else Color(0xFFFF5566),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.status_label),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = if (device.isReachable) Color(0xFF00DD77).copy(alpha = 0.15f)
                                        else Color(0xFFFF5566).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = if (device.isReachable) stringResource(R.string.status_reachable)
                                           else stringResource(R.string.status_unreachable),
                                    color = if (device.isReachable) Color(0xFF00DD77) else Color(0xFFFF5566),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Open Ports ───────────────────────────────────────────
            item {
                DetailSectionTitle(stringResource(R.string.open_ports))
                Spacer(modifier = Modifier.height(8.dp))

                if (device.openPorts.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                stringResource(R.string.no_open_ports),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Group ports into rows of chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(device.openPorts.sorted()) { port ->
                            val service = PORT_SERVICES[port] ?: stringResource(R.string.port_service_unknown)
                            PortChip(port = port, service = service)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small composable helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        color = PrimaryBlue,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        thickness = 0.5.dp
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PortChip(port: Int, service: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PrimaryBlue.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$port",
                color = PrimaryBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = service,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            " $label",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Device list item (now clickable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DeviceItem(device: DeviceInfo, isCurrent: Boolean, isSelected: Boolean, onClick: () -> Unit = {}) {
    val backgroundColor = when {
        isSelected -> PrimaryBlue.copy(alpha = 0.2f)
        isCurrent -> PrimaryPurple.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) PrimaryBlue else if (isCurrent) PrimaryPurple else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    imageVector = deviceTypeIcon(device.deviceType),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(device.ip, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    if (device.vendor != "Unknown") {
                        Text(" · ${device.vendor}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                }
                if (device.mac != "Unknown") {
                    Text(device.mac, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 10.sp)
                }
            }

            // Right-side: open ports count badge + chevron
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (device.openPorts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${device.openPorts.size}p",
                            color = PrimaryBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (device.isReachable) {
                    Surface(shape = CircleShape, color = Color(0xFF00FF88).copy(alpha = 0.15f), modifier = Modifier.size(8.dp)) {}
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

fun deviceTypeIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.ROUTER  -> Icons.Default.Router
    DeviceType.MOBILE  -> Icons.Default.Smartphone
    DeviceType.PC      -> Icons.Default.Computer
    DeviceType.PRINTER -> Icons.Default.Print
    DeviceType.TV      -> Icons.Default.Tv
    DeviceType.SERVER  -> Icons.Default.Storage
    DeviceType.IOT     -> Icons.Default.Sensors
    DeviceType.UNKNOWN -> Icons.Default.Devices
}

@Composable
private fun deviceTypeName(type: DeviceType): String = when (type) {
    DeviceType.ROUTER  -> stringResource(R.string.dtype_router)
    DeviceType.MOBILE  -> stringResource(R.string.dtype_mobile)
    DeviceType.PC      -> stringResource(R.string.dtype_pc)
    DeviceType.PRINTER -> stringResource(R.string.dtype_printer)
    DeviceType.TV      -> stringResource(R.string.dtype_tv)
    DeviceType.SERVER  -> stringResource(R.string.dtype_server)
    DeviceType.IOT     -> stringResource(R.string.dtype_iot)
    DeviceType.UNKNOWN -> stringResource(R.string.dtype_unknown)
}
