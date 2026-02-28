package com.tinaut1986.netpulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.netpulse.R
import com.tinaut1986.netpulse.data.DiagnosticReport
import com.tinaut1986.netpulse.data.PingSample
import com.tinaut1986.netpulse.data.TargetDiagnostic
import com.tinaut1986.netpulse.ui.components.PremiumCard
import com.tinaut1986.netpulse.ui.theme.*

// ------------------------------------------------------------------
// Main Screen
// ------------------------------------------------------------------
@Composable
fun NetworkQualityScreen(
    report: DiagnosticReport?,
    liveLatencySamples: List<PingSample>,
    isDiagnosing: Boolean,
    diagnosisStep: String,
    onStartDiagnosis: () -> Unit,
    onStopDiagnosis: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Unified Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.network_quality),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onHistoryClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.history_title),
                    tint = PrimaryBlue
                )
            }
        }

        // Live latency chart (always visible)
        LiveLatencyChartCard(samples = liveLatencySamples)

        Spacer(Modifier.height(16.dp))

        // Diagnosis button / progress
        DiagnosisControlCard(
            isDiagnosing = isDiagnosing,
            diagnosisStep = diagnosisStep,
            onStart = onStartDiagnosis,
            onStop = onStopDiagnosis
        )

        Spacer(Modifier.height(16.dp))

        if (report != null) {
            // Overall quality score
            QualityScoreCard(report.overallScore)

            Spacer(Modifier.height(16.dp))

            // Problems summary
            if (report.problems.isNotEmpty()) {
                ProblemsCard(report.problems)
                Spacer(Modifier.height(16.dp))
            }

            // Per target breakdown
            report.gatewayDiag?.let {
                TargetCard(it, Icons.Default.Router, PrimaryBlue)
                Spacer(Modifier.height(8.dp))
            }
            report.dns1Diag?.let {
                TargetCard(it, Icons.Default.Dns, PrimaryPurple)
                Spacer(Modifier.height(8.dp))
            }
            report.googleDnsDiag?.let {
                TargetCard(it, Icons.Default.Public, SignalGreen)
                Spacer(Modifier.height(8.dp))
            }
            report.internetDiag?.let {
                TargetCard(it, Icons.Default.Cloud, SignalYellow)
                Spacer(Modifier.height(8.dp))
            }

            // DNS resolution
            DnsResolutionCard(report.dnsResolutionMs)
        } else if (!isDiagnosing) {
            EmptyStateCard()
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ------------------------------------------------------------------
// Live latency chart
// ------------------------------------------------------------------
@Composable
fun LiveLatencyChartCard(samples: List<PingSample>) {
    PremiumCard {
        Text(
            text = stringResource(R.string.live_latency_title),
            color = PrimaryBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = stringResource(R.string.gateway_continuous),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))

        if (samples.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.collecting_data),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        } else {
            LatencySparkline(
                samples = samples,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            val goodSamples = samples.filter { it.latencyMs >= 0 }
            val avg = if (goodSamples.isNotEmpty()) goodSamples.map { it.latencyMs }.average().toLong() else 0L
            val lost = samples.count { it.latencyMs < 0 }
            val lossPercent = if (samples.isEmpty()) 0 else (lost * 100 / samples.size)

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LiveStatChip("${avg}ms", stringResource(R.string.avg))
                LiveStatChip("${samples.filter { it.latencyMs >= 0 }.minByOrNull { it.latencyMs }?.latencyMs ?: 0}ms", stringResource(R.string.min_stat))
                LiveStatChip("${samples.filter { it.latencyMs >= 0 }.maxByOrNull { it.latencyMs }?.latencyMs ?: 0}ms", stringResource(R.string.max_stat))
                LiveStatChip("${lossPercent}%", stringResource(R.string.loss))
            }
        }
    }
}

@Composable
fun LiveStatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}

@Composable
fun LatencySparkline(samples: List<PingSample>, modifier: Modifier = Modifier) {
    val primary = PrimaryBlue
    val red = SignalRed
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val validSamples = samples.filter { it.latencyMs >= 0 }
        if (validSamples.isEmpty()) return@Canvas

        val maxLat = (validSamples.maxOf { it.latencyMs }).coerceAtLeast(100)
        val w = size.width
        val h = size.height
        val step = w / (samples.size - 1).coerceAtLeast(1)

        // Grid lines
        val gridLevels = listOf(0.25f, 0.5f, 0.75f)
        gridLevels.forEach { level ->
            val y = h * level
            drawLine(surfaceVariant, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        // Build path
        val path = Path()
        val fillPath = Path()
        var first = true
        samples.forEachIndexed { i, sample ->
            val x = i * step
            val y = if (sample.latencyMs >= 0) {
                h - (sample.latencyMs.toFloat() / maxLat * h).coerceIn(0f, h)
            } else h // timeout at bottom

            if (first) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo((samples.size - 1) * step, h)
        fillPath.close()

        // Fill gradient
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // Stroke
        drawPath(path, color = primary, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Timeout dots
        samples.forEachIndexed { i, sample ->
            if (sample.latencyMs < 0) {
                drawCircle(color = red.copy(alpha = 0.8f), radius = 5f, center = Offset(i * step, h - 8f))
            }
        }
    }
}

// ------------------------------------------------------------------
// Diagnosis control
// ------------------------------------------------------------------
@Composable
fun DiagnosisControlCard(
    isDiagnosing: Boolean,
    diagnosisStep: String,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.full_diagnosis),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                Text(
                    text = stringResource(R.string.full_diagnosis_desc),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            if (isDiagnosing) {
                OutlinedButton(
                    onClick = onStop,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalRed)
                ) {
                    Text(stringResource(R.string.stop_scan))
                }
            } else {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.run_diagnosis))
                }
            }
        }

        if (isDiagnosing) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stepLabel(diagnosisStep),
                color = PrimaryBlue,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun stepLabel(step: String): String = when (step) {
    "gateway" -> stringResource(R.string.step_gateway)
    "dns" -> stringResource(R.string.step_dns)
    "google_dns" -> stringResource(R.string.step_google_dns)
    "internet" -> stringResource(R.string.step_internet)
    "dns_resolution" -> stringResource(R.string.step_dns_resolution)
    else -> stringResource(R.string.diagnosing)
}

// ------------------------------------------------------------------
// Quality score card with circular gauge
// ------------------------------------------------------------------
@Composable
fun QualityScoreCard(score: Int) {
    val animScore by animateIntAsState(targetValue = score, animationSpec = tween(800), label = "score")
    val scoreColor = when {
        score >= 80 -> SignalGreen
        score >= 50 -> SignalYellow
        else -> SignalRed
    }
    val scoreLabel = when {
        score >= 80 -> stringResource(R.string.quality_good)
        score >= 50 -> stringResource(R.string.quality_fair)
        else -> stringResource(R.string.quality_poor)
    }

    PremiumCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.overall_quality),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center) {
                ScoreGauge(score = animScore, color = scoreColor)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animScore",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor
                    )
                    Text(
                        text = "/100",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = scoreLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = scoreColor
            )
        }
    }
}

@Composable
fun ScoreGauge(score: Int, color: Color) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = Modifier.size(130.dp)
    ) {
        val strokeWidth = 14.dp.toPx()
        val inset = strokeWidth / 2f
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        // Track
        drawArc(
            color = trackColor,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = strokeStyle,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
        )

        // Score arc
        val sweep = (score / 100f * 270f).coerceIn(0f, 270f)
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = sweep,
            useCenter = false,
            style = strokeStyle,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
        )
    }
}

// ------------------------------------------------------------------
// Problems card
// ------------------------------------------------------------------
@Composable
fun ProblemsCard(problems: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SignalRed.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, tint = SignalRed, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.problems_detected),
                    color = SignalRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            problems.forEach { problem ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Text("• ", color = SignalRed, fontSize = 13.sp)
                    Text(
                        text = problemText(problem),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun problemText(code: String): String = when (code) {
    "packet_loss_gateway" -> stringResource(R.string.prob_loss_gateway)
    "high_latency_gateway" -> stringResource(R.string.prob_high_latency)
    "high_jitter" -> stringResource(R.string.prob_high_jitter)
    "packet_loss_internet" -> stringResource(R.string.prob_loss_internet)
    "dns_failure" -> stringResource(R.string.prob_dns_failure)
    "slow_dns" -> stringResource(R.string.prob_slow_dns)
    else -> code
}

// ------------------------------------------------------------------
// Per-target diagnostic card
// ------------------------------------------------------------------
@Composable
fun TargetCard(diag: TargetDiagnostic, icon: ImageVector, accentColor: Color) {
    PremiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = diag.target.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Text(
                    text = diag.target.host,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.weight(1f))
            val lossColor = when {
                diag.packetLoss <= 0f -> SignalGreen
                diag.packetLoss <= 5f -> SignalYellow
                else -> SignalRed
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${diag.packetLoss.toInt()}% " + stringResource(R.string.loss),
                    color = lossColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (diag.avgLatency >= 0) "${diag.avgLatency}ms avg" else stringResource(R.string.no_response),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Mini sparkline
        if (diag.samples.isNotEmpty()) {
            LatencySparkline(
                samples = diag.samples,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // Stat row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TargetStat(
                label = stringResource(R.string.min_stat),
                value = if (diag.minLatency >= 0) "${diag.minLatency}ms" else "—",
                color = accentColor
            )
            TargetStat(
                label = stringResource(R.string.avg),
                value = if (diag.avgLatency >= 0) "${diag.avgLatency}ms" else "—",
                color = accentColor
            )
            TargetStat(
                label = stringResource(R.string.max_stat),
                value = if (diag.maxLatency >= 0) "${diag.maxLatency}ms" else "—",
                color = accentColor
            )
            TargetStat(
                label = stringResource(R.string.jitter_label),
                value = "${diag.jitter}ms",
                color = if (diag.jitter < 20) SignalGreen else if (diag.jitter < 50) SignalYellow else SignalRed
            )
        }
    }
}

@Composable
fun TargetStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}

// ------------------------------------------------------------------
// DNS resolution card
// ------------------------------------------------------------------
@Composable
fun DnsResolutionCard(dnsMs: Long) {
    val color = when {
        dnsMs < 0 -> SignalRed
        dnsMs < 100 -> SignalGreen
        dnsMs < 500 -> SignalYellow
        else -> SignalRed
    }
    val label = when {
        dnsMs < 0 -> stringResource(R.string.dns_failed)
        dnsMs < 100 -> stringResource(R.string.dns_fast)
        dnsMs < 500 -> stringResource(R.string.dns_slow)
        else -> stringResource(R.string.dns_very_slow)
    }

    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Dns, tint = color, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.dns_resolution_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(label, color = color, fontSize = 12.sp)
                }
            }
            Text(
                if (dnsMs >= 0) "${dnsMs}ms" else stringResource(R.string.failed),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

// ------------------------------------------------------------------
// Empty state
// ------------------------------------------------------------------
@Composable
fun EmptyStateCard() {
    PremiumCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.NetworkCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.no_diagnosis_yet),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}
