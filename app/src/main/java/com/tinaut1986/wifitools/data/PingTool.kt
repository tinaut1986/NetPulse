package com.tinaut1986.wifitools.data

import kotlinx.coroutines.Dispatchers
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
