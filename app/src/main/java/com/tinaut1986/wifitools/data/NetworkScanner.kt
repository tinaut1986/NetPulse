package com.tinaut1986.wifitools.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NetworkScanner {

    suspend fun scanSubnet(gateway: String): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val prefix = gateway.substringBeforeLast(".") + "."

        val jobs = (1..254).map { i ->
            async {
                val ip = prefix + i
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(500)) {
                        // 1. Try NetBIOS (gives real name + MAC for Windows/NAS)
                        val (nbName, nbMac) = getNetbiosInfo(ip)

                        // 2. Fallback: try ARP cache (works on older Android)
                        val mac = nbMac ?: getMacFromArpCache(ip) ?: "Unknown"
                        val vendor = if (mac != "Unknown") getVendorFromMac(mac) else "Unknown"

                        // 3. Detect type: hostname hints → port scan
                        val deviceType = detectType(ip, nbName ?: "", vendor)

                        DeviceInfo(
                            ip = ip,
                            mac = mac,
                            vendor = vendor,
                            hostname = nbName ?: "",
                            isReachable = true,
                            deviceType = deviceType
                        )
                    } else null
                } catch (e: Exception) { null }
            }
        }
        jobs.awaitAll().filterNotNull()
    }

    // ─────────────────────────────────────────────────────────────────
    // NetBIOS Node Status Query (RFC 1002)
    // Gives us: real hostname + MAC address for Windows PCs, NAS, etc.
    // ─────────────────────────────────────────────────────────────────
    private fun getNetbiosInfo(ip: String): Pair<String?, String?> {
        try {
            // NBSTAT query packet: ask for all names registered at the target
            val query = byteArrayOf(
                0xAB.toByte(), 0xCD.toByte(), // Transaction ID
                0x00, 0x00,                   // Flags: standard query
                0x00, 0x01,                   // Questions: 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Answer/Auth/Additional: 0
                0x20,                         // Name length = 32
                // Encoded wildcard '*' (0x2A): high=2→'C', low=A→'K'
                0x43, 0x4B,
                // 15 × null byte (0x00), each → 'A','A'
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x00,                         // End of name
                0x00, 0x21,                   // Type: NBSTAT (33)
                0x00, 0x01                    // Class: IN (1)
            )

            val socket = DatagramSocket()
            socket.soTimeout = 1500
            val addr = InetAddress.getByName(ip)
            socket.send(DatagramPacket(query, query.size, addr, 137))

            val buf = ByteArray(1024)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            socket.close()

            // Response layout:
            //   12 bytes: header
            //   38 bytes: echo of our question  (12+38=50)
            //    2 bytes: answer name ptr (0xC0, 0x0C)
            //    2 bytes: type
            //    2 bytes: class
            //    4 bytes: TTL
            //    2 bytes: rdlength     (50+12=62)
            //    1 byte : num_names    ← offset 62
            //   18 bytes × N: name entries (15 name + 1 type + 2 flags)
            //    + STATISTICS section (first 6 bytes = MAC)

            if (resp.length < 63) return Pair(null, null)

            val numNames = buf[62].toInt() and 0xFF
            if (numNames <= 0 || numNames > 50) return Pair(null, null)

            var foundName: String? = null
            for (i in 0 until numNames) {
                val base = 63 + i * 18
                if (base + 17 >= resp.length) break

                val nameBytes = buf.copyOfRange(base, base + 15)
                val nameType  = buf[base + 15].toInt() and 0xFF
                val flagHi    = buf[base + 16].toInt() and 0xFF
                val isGroup   = (flagHi and 0x80) != 0

                // Unique names with type 0x00 (Workstation) or 0x20 (File Server)
                if (!isGroup && (nameType == 0x00 || nameType == 0x20) && foundName == null) {
                    val n = String(nameBytes, Charsets.US_ASCII).trim()
                    if (n.isNotBlank() && n != "*") foundName = n
                }
            }

            // MAC is at the start of the STATISTICS section
            val macOffset = 63 + numNames * 18
            var mac: String? = null
            if (macOffset + 5 < resp.length) {
                val candidate = (0..5).joinToString(":") {
                    buf[macOffset + it].toInt().and(0xFF).toString(16).padStart(2, '0').uppercase()
                }
                if (candidate != "00:00:00:00:00:00") mac = candidate
            }

            return Pair(foundName, mac)
        } catch (e: Exception) {
            return Pair(null, null)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ARP cache fallback (only works reliably on Android < 10)
    // ─────────────────────────────────────────────────────────────────
    private fun getMacFromArpCache(ip: String): String? {
        return try {
            BufferedReader(FileReader("/proc/net/arp")).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val parts = line!!.trim().split("\\s+".toRegex())
                    if (parts.size >= 4 && parts[0] == ip) {
                        val mac = parts[3]
                        if (mac.length == 17 && mac != "00:00:00:00:00:00") return mac.uppercase()
                    }
                }
                null
            }
        } catch (e: Exception) { null }
    }

    // ─────────────────────────────────────────────────────────────────
    // Port-based device type detection (most reliable fallback)
    // ─────────────────────────────────────────────────────────────────
    private fun detectType(ip: String, hostname: String, vendor: String): DeviceType {
        val h = hostname.lowercase()
        val v = vendor.lowercase()

        // Fast hints (no network needed)
        if (v.contains("raspberry pi")) return DeviceType.IOT
        if (h.contains("router") || h.contains("gateway") || v.contains("cisco") || v.contains("ubiquiti")) return DeviceType.ROUTER
        if (h.contains("iphone") || h.contains("ipad") || h.contains("android") || h.contains("phone") || h.contains("pixel") || h.contains("galaxy")) return DeviceType.MOBILE
        if (h.contains("printer") || h.contains("epson") || h.contains("canon") || (v.contains("hp") && !h.contains("desktop"))) return DeviceType.PRINTER
        if (h.contains("bravia") || h.contains("chromecast") || h.contains("appletv") || h.contains("roku")) return DeviceType.TV
        if (h.contains("nas") || h.contains("synology") || h.contains("server")) return DeviceType.SERVER

        // Port checks (ordered fastest/most-specific first)
        if (isPortOpen(ip, 9100) || isPortOpen(ip, 631)) return DeviceType.PRINTER
        if (isPortOpen(ip, 8008) || isPortOpen(ip, 8009)) return DeviceType.TV   // Chromecast
        if (isPortOpen(ip, 8060)) return DeviceType.TV                           // Roku
        if (isPortOpen(ip, 7000)) return DeviceType.TV                           // AirPlay / Apple TV
        if (isPortOpen(ip, 5357) || isPortOpen(ip, 135)) return DeviceType.PC    // Windows
        if (isPortOpen(ip, 554)) return DeviceType.IOT                           // RTSP camera
        if (isPortOpen(ip, 22)) return DeviceType.SERVER                         // SSH → likely server
        if (isPortOpen(ip, 80) || isPortOpen(ip, 443)) {
            val lastOctet = ip.substringAfterLast(".").toIntOrNull() ?: 0
            return if (lastOctet == 1 || lastOctet == 254) DeviceType.ROUTER else DeviceType.SERVER
        }

        return DeviceType.UNKNOWN
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int = 350): Boolean {
        return try {
            Socket().use { it.connect(InetSocketAddress(ip, port), timeout); true }
        } catch (e: Exception) { false }
    }

    // ─────────────────────────────────────────────────────────────────
    // MAC Vendor OUI lookup
    // ─────────────────────────────────────────────────────────────────
    private fun getVendorFromMac(mac: String): String {
        val oui = mac.split(":").take(3).joinToString("").uppercase()
        return when (oui) {
            "00000C" -> "Cisco"
            "0001E3" -> "Siemens"
            "0005CD" -> "Asus"
            "000C29" -> "VMware"
            "001132" -> "Synology"
            "001422" -> "Dell"
            "00155D" -> "Microsoft"
            "001A11" -> "Google"
            "001C42" -> "Parallels"
            "00212F" -> "Cisco"
            "0418D6" -> "Ubiquiti"
            "04D4C4" -> "HP"
            "080027" -> "VirtualBox"
            "18B430" -> "Nest"
            "2C91AB" -> "Sony"
            "3C15C2", "50C7BF", "8416F9" -> "TP-Link"
            "40B4CD" -> "Amazon"
            "48D6D5", "B0C559" -> "Xiaomi"
            "705A0F" -> "HP"
            "7483C2", "8C6422", "A47733", "AC84C6", "F4F5E8" -> "Apple"
            "7811DC" -> "Huawei"
            "B827EB", "DCA632", "E45F01" -> "Raspberry Pi"
            "C05627" -> "Samsung"
            "D850E6" -> "Google"
            "E4E749" -> "Sonos"
            else -> "Unknown"
        }
    }
}
