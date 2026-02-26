package com.tinaut1986.netpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

data class PingTarget(
    val name: String,
    val host: String,
    val category: String // "gateway", "dns", "internet"
)

data class PingSample(
    val timestampMs: Long,
    val latencyMs: Long, // -1 = timeout
)

data class TargetDiagnostic(
    val target: PingTarget,
    val samples: List<PingSample>,
    val avgLatency: Long,
    val minLatency: Long,
    val maxLatency: Long,
    val jitter: Long,       // average deviation
    val packetLoss: Float   // 0..100
)

data class DiagnosticReport(
    val gatewayDiag: TargetDiagnostic?,
    val dns1Diag: TargetDiagnostic?,
    val googleDnsDiag: TargetDiagnostic?,
    val internetDiag: TargetDiagnostic?,
    val dnsResolutionMs: Long,  // -1 = failed
    val overallScore: Int,      // 0..100
    val problems: List<String>
)

class NetworkDiagnosticTool {

    /**
     * Ping a single host [count] times and return ordered samples.
     */
    suspend fun pingMultiple(host: String, count: Int, timeoutMs: Int = 2000): List<PingSample> =
        withContext(Dispatchers.IO) {
            val samples = mutableListOf<PingSample>()
            repeat(count) {
                val start = System.currentTimeMillis()
                val latency = try {
                    val address = InetAddress.getByName(host)
                    if (address.isReachable(timeoutMs)) {
                        System.currentTimeMillis() - start
                    } else -1L
                } catch (e: Exception) { -1L }
                samples.add(PingSample(System.currentTimeMillis(), latency))
            }
            samples
        }

    /**
     * Compute diagnostic stats from a list of samples.
     */
    fun computeDiagnostic(target: PingTarget, samples: List<PingSample>): TargetDiagnostic {
        val successful = samples.filter { it.latencyMs >= 0 }.map { it.latencyMs }
        val failed = samples.size - successful.size
        val loss = if (samples.isEmpty()) 100f else (failed.toFloat() / samples.size * 100f)

        val avg = if (successful.isNotEmpty()) successful.average().toLong() else -1L
        val min = successful.minOrNull() ?: -1L
        val max = successful.maxOrNull() ?: -1L

        // Jitter = mean absolute deviation from average
        val jitter = if (successful.size >= 2 && avg >= 0) {
            successful.map { Math.abs(it - avg) }.average().toLong()
        } else 0L

        return TargetDiagnostic(target, samples, avg, min, max, jitter, loss)
    }

    /**
     * Measure DNS resolution time for a hostname.
     * Returns time in ms, or -1 if failed.
     */
    suspend fun measureDnsResolution(hostname: String = "www.google.com"): Long =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                InetAddress.getByName(hostname)
                System.currentTimeMillis() - start
            } catch (e: Exception) { -1L }
        }

    /**
     * Full diagnostic: pings gateway, DNS servers, internet, measures DNS resolution.
     * Returns a complete DiagnosticReport.
     */
    suspend fun runFullDiagnostic(
        gatewayIp: String,
        dns1Ip: String,
        pingCount: Int = 10,
        onProgress: (String) -> Unit = {}
    ): DiagnosticReport {
        val problems = mutableListOf<String>()

        onProgress("gateway")
        val gwTarget = PingTarget("Router", gatewayIp, "gateway")
        val gwSamples = pingMultiple(gatewayIp, pingCount, 2000)
        val gwDiag = computeDiagnostic(gwTarget, gwSamples)

        onProgress("dns")
        val dns1Target = PingTarget("DNS Local", dns1Ip, "dns")
        val dns1Samples = if (dns1Ip.isNotBlank() && dns1Ip != "0.0.0.0")
            pingMultiple(dns1Ip, pingCount, 2000) else emptyList()
        val dns1Diag = if (dns1Samples.isNotEmpty()) computeDiagnostic(dns1Target, dns1Samples) else null

        onProgress("google_dns")
        val googleTarget = PingTarget("Google DNS", "8.8.8.8", "internet")
        val googleSamples = pingMultiple("8.8.8.8", pingCount, 3000)
        val googleDiag = computeDiagnostic(googleTarget, googleSamples)

        onProgress("internet")
        val cf1Target = PingTarget("Cloudflare", "1.1.1.1", "internet")
        val cfSamples = pingMultiple("1.1.1.1", pingCount, 3000)
        val cfDiag = computeDiagnostic(cf1Target, cfSamples)

        onProgress("dns_resolution")
        val dnsResMs = measureDnsResolution()

        // Detect problems
        if (gwDiag.packetLoss > 5f) problems.add("packet_loss_gateway")
        if (gwDiag.avgLatency > 50) problems.add("high_latency_gateway")
        if (gwDiag.jitter > 30) problems.add("high_jitter")
        if (googleDiag.packetLoss > 10f) problems.add("packet_loss_internet")
        if (dnsResMs < 0) problems.add("dns_failure")
        else if (dnsResMs > 500) problems.add("slow_dns")

        // Score calculation
        var score = 100
        score -= (gwDiag.packetLoss * 1.5f).toInt()
        if (gwDiag.avgLatency in 1..49) score -= 0
        else if (gwDiag.avgLatency in 50..100) score -= 10
        else if (gwDiag.avgLatency > 100) score -= 20
        if (gwDiag.jitter > 20) score -= 10
        if (gwDiag.jitter > 50) score -= 10
        if (googleDiag.packetLoss > 5) score -= 10
        if (dnsResMs < 0) score -= 20
        else if (dnsResMs > 500) score -= 5
        score = score.coerceIn(0, 100)

        return DiagnosticReport(gwDiag, dns1Diag, googleDiag, cfDiag, dnsResMs, score, problems)
    }
}
