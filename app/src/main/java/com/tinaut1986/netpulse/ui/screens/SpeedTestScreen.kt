package com.tinaut1986.netpulse.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.netpulse.R
import com.tinaut1986.netpulse.ui.theme.PrimaryBlue
import com.tinaut1986.netpulse.ui.theme.PrimaryPurple
import java.util.Locale

@Composable
fun SpeedTestScreen(
    isTesting: Boolean,
    progress: Float,
    downloadSpeed: Double?,
    uploadSpeed: Double?,
    latency: Double?,
    jitter: Double?,
    phase: String,
    onStartTest: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Unified Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.speed_test),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Large Gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round,
            )
            
            val barColor = when(phase) {
                "latency" -> PrimaryPurple
                "upload" -> Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.primary
            }
            
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = barColor,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val currentDisplayValue = when(phase) {
                    "latency" -> latency
                    "upload" -> uploadSpeed
                    else -> downloadSpeed
                }
                if (currentDisplayValue != null && isTesting) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", currentDisplayValue),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = barColor
                    )
                    Text(
                        text = if (phase == "latency") stringResource(R.string.ms_unit) else stringResource(R.string.mbps_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isTesting) {
                    Text(
                        text = when(phase) {
                            "latency" -> stringResource(R.string.status_latency)
                            "upload" -> stringResource(R.string.status_uploading)
                            else -> stringResource(R.string.status_downloading)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f%%", progress * 100),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Results Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultItem(
                label = stringResource(R.string.download),
                value = downloadSpeed,
                unit = stringResource(R.string.mbps_label),
                icon = Icons.Default.ArrowDownward,
                color = MaterialTheme.colorScheme.primary,
                active = phase == "download" || phase == "finished",
                modifier = Modifier.weight(1f)
            )
            ResultItem(
                label = stringResource(R.string.upload),
                value = uploadSpeed,
                unit = stringResource(R.string.mbps_label),
                icon = Icons.Default.ArrowUpward,
                color = Color(0xFF4CAF50),
                active = phase == "upload" || phase == "finished",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultItem(
                label = stringResource(R.string.latency_ms),
                value = latency,
                unit = stringResource(R.string.ms_unit),
                icon = Icons.Default.Timer,
                color = PrimaryPurple,
                active = phase == "latency" || phase == "finished",
                modifier = Modifier.weight(1f)
            )
            ResultItem(
                label = stringResource(R.string.jitter_ms),
                value = jitter,
                unit = stringResource(R.string.ms_unit),
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                color = Color(0xFFFFA000),
                active = phase == "latency" || phase == "finished",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val statusText = when(phase) {
                    "latency" -> stringResource(R.string.testing_latency)
                    "download" -> stringResource(R.string.testing_download)
                    "upload" -> stringResource(R.string.testing_upload)
                    "finished" -> stringResource(R.string.test_finished)
                    else -> stringResource(R.string.test_start_desc)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartTest,
            enabled = !isTesting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = when(phase) {
                        "latency" -> stringResource(R.string.testing_latency)
                        "upload" -> stringResource(R.string.testing_phase_upload)
                        else -> stringResource(R.string.testing_phase_download)
                    }
                )
            } else {
                Text(
                    text = if (phase == "finished") stringResource(R.string.repeat_test) else stringResource(R.string.start_test),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ResultItem(
    label: String,
    value: Double?,
    unit: String,
    icon: ImageVector,
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (active) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        ),
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = if (active) color else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (value != null) String.format(Locale.getDefault(), "%.1f", value) else "--",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
