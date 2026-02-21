package com.tinaut1986.wifitools.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress

class NetworkScanner {
    suspend fun scanSubnet(gateway: String): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val prefix = gateway.substringBeforeLast(".") + "."
        val devices = mutableListOf<DeviceInfo>()
        
        // Parallel scan for speed
        val jobs = (1..254).map { i ->
            async {
                val ip = prefix + i
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(500)) {
                        DeviceInfo(
                            ip = ip,
                            hostname = address.canonicalHostName,
                            isReachable = true
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        jobs.awaitAll().filterNotNull()
    }
}
