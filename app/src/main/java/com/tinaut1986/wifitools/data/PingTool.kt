package com.tinaut1986.wifitools.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class PingTool {
    suspend fun ping(host: String): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val address = InetAddress.getByName(host)
            if (address.isReachable(2000)) {
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
}
