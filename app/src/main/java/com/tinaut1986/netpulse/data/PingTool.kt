package com.tinaut1986.netpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

import java.net.HttpURLConnection
import java.net.URL

class PingTool {
    suspend fun ping(host: String, timeout: Int = 2000): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val address = InetAddress.getByName(host)
            if (address.isReachable(timeout)) {
                return@withContext System.currentTimeMillis() - startTime
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext -1L
    }

    suspend fun checkPort(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1000)
                return@withContext true
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Scans all ports in [portRange] on [host] using parallel batches.
     * [onProgress] is called with (progressFraction 0..1, openPortsFoundSoFar).
     * Returns the full list of open ports when complete.
     */
    suspend fun scanAllPorts(
        host: String,
        portRange: IntRange = 1..65535,
        batchSize: Int = 500,
        timeoutMs: Int = 400,
        onProgress: suspend (Float, List<Int>) -> Unit
    ): List<Int> = withContext(Dispatchers.IO) {
        val openPorts = mutableListOf<Int>()
        val ports = portRange.toList()
        val total = ports.size

        ports.chunked(batchSize).forEachIndexed { batchIdx, batch ->
            val results = batch.map { port ->
                async {
                    val open = try {
                        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs); true }
                    } catch (e: Exception) { false }
                    if (open) port else null
                }
            }.awaitAll().filterNotNull()

            synchronized(openPorts) { openPorts.addAll(results) }

            val scanned = (batchIdx + 1) * batchSize
            val progress = (scanned.coerceAtMost(total)).toFloat() / total
            onProgress(progress, openPorts.sorted())
        }
        openPorts.sorted()
    }

    suspend fun getPublicIp(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun dnsLookup(host: String): List<String> = withContext(Dispatchers.IO) {
        try {
            InetAddress.getAllByName(host).map { it.hostAddress }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun traceroute(host: String, onHop: (Int, String, Long) -> Unit) = withContext(Dispatchers.IO) {
        for (ttl in 1..30) {
            val startTime = System.currentTimeMillis()
            try {
                // Using ping command with TTL is the most portable way in Android
                val process = Runtime.getRuntime().exec("ping -c 1 -t $ttl $host")
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val elapsedTime = System.currentTimeMillis() - startTime

                // Parse IP from output (Format: From 192.168.1.1 ...)
                val ipMatch = "from ([0-9.]+)".toRegex(RegexOption.IGNORE_CASE).find(output)
                val ip = ipMatch?.groupValues?.get(1) ?: "*"

                onHop(ttl, ip, elapsedTime)

                if (output.contains("bytes from $host", ignoreCase = true) || output.contains("1 packets transmitted, 1 received")) {
                    break
                }
            } catch (e: Exception) {
                onHop(ttl, "*", -1)
            }
        }
    }
}
