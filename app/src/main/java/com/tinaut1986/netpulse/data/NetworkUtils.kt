package com.tinaut1986.netpulse.data

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.pow

data class SubnetInfo(
    val networkAddress: String,
    val broadcastAddress: String,
    val firstHost: String,
    val lastHost: String,
    val totalHosts: Long,
    val mask: String,
    val cidr: Int
)

object NetworkUtils {

    /**
     * Sends a Wake-on-LAN magic packet to the specified MAC address.
     */
    fun sendWakeOnLan(macAddress: String, broadcastIp: String = "255.255.255.255"): Result<Unit> {
        return try {
            val macBytes = getMacBytes(macAddress)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0..5) {
                bytes[i] = 0xff.toByte()
            }
            for (i in 6 until bytes.size step macBytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
            }

            val address = InetAddress.getByName(broadcastIp)
            val packet = DatagramPacket(bytes, bytes.size, address, 9)
            DatagramSocket().use { it.send(packet) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getMacBytes(macString: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macString.split(":", "-")
        if (hex.size != 6) {
            throw IllegalArgumentException("Invalid MAC address.")
        }
        try {
            for (i in 0..5) {
                bytes[i] = hex[i].toInt(16).toByte()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex digit in MAC address.")
        }
        return bytes
    }

    /**
     * Calculates subnet details from an IP and mask/CIDR.
     */
    fun calculateSubnet(ip: String, maskOrCidr: String): SubnetInfo? {
        return try {
            val cidr = if (maskOrCidr.contains(".")) {
                maskToCidr(maskOrCidr)
            } else {
                maskOrCidr.toIntOrNull() ?: 24
            }

            val ipInt = ipToInt(ip)
            val maskInt = -0x1 shl (32 - cidr)
            
            val networkInt = ipInt and maskInt
            val broadcastInt = networkInt or maskInt.inv()
            
            val totalHosts = if (cidr >= 31) 0 else (2.0.pow(32.0 - cidr).toLong() - 2)
            
            SubnetInfo(
                networkAddress = intToIp(networkInt),
                broadcastAddress = intToIp(broadcastInt),
                firstHost = if (cidr >= 31) "N/A" else intToIp(networkInt + 1),
                lastHost = if (cidr >= 31) "N/A" else intToIp(broadcastInt - 1),
                totalHosts = totalHosts,
                mask = intToIp(maskInt),
                cidr = cidr
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun ipToInt(ip: String): Int {
        val parts = ip.split(".")
        return (parts[0].toInt() shl 24) or (parts[1].toInt() shl 16) or (parts[2].toInt() shl 8) or parts[3].toInt()
    }

    private fun intToIp(ip: Int): String {
        return "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"
    }

    private fun maskToCidr(mask: String): Int {
        val maskInt = ipToInt(mask)
        var count = 0
        var m = maskInt
        while (m != 0) {
            m = m shl 1
            count++
        }
        return count
    }
    
    /**
     * Performs a WHOIS lookup for a domain or IP.
     * Note: Simplistic version, real WHOIS often requires connecting to specific servers based on TLD.
     */
    fun whoisLookup(target: String): String {
        val cleanTarget = target.trim().lowercase()
        // WHOIS servers for domains usually don't handle www. or subdomains directly
        if (cleanTarget.startsWith("www.")) {
            return "Error: Please use the base domain (e.g., 'google.com' instead of 'www.google.com') for WHOIS lookups."
        }
        
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("whois.iana.org", 43), 5000)
            socket.getOutputStream().write((cleanTarget + "\r\n").toByteArray())
            val response = socket.getInputStream().bufferedReader().readText()
            socket.close()
            response.ifBlank { "No data returned from WHOIS server for $cleanTarget." }
        } catch (e: Exception) {
            "Error performing WHOIS: ${e.message ?: e.toString()}"
        }
    }
}
