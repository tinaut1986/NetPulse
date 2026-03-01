package com.tinaut1986.netpulse.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.net.wifi.ScanResult

class WifiScanner(private val context: Context) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _wifiState = MutableStateFlow(WifiInfo())
    val wifiState: StateFlow<WifiInfo> = _wifiState.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
 
        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateNetworkInfo()
            }
 
            override fun onLost(network: Network) {
                _wifiState.value = WifiInfo(ssid = "No Connection", isWifi = false)
            }
        })
    }
 
    fun updateNetworkInfo() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        if (capabilities == null) {
            _wifiState.value = WifiInfo(ssid = "Disconnected", isWifi = false)
            return
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val info = wifiManager.connectionInfo
            val dhcp = wifiManager.dhcpInfo
 
            val ip = Formatter.formatIpAddress(info.ipAddress ?: 0)
            val gateway = Formatter.formatIpAddress(dhcp?.gateway ?: 0)
            val mask = Formatter.formatIpAddress(dhcp?.netmask ?: 0)
            val dns1 = Formatter.formatIpAddress(dhcp?.dns1 ?: 0)
            val dns2 = Formatter.formatIpAddress(dhcp?.dns2 ?: 0)
 
            _wifiState.value = _wifiState.value.copy(
                ssid = (info?.ssid ?: "WiFi").replace("\"", "").let { if (it == "<unknown ssid>" || it.isEmpty()) "WiFi" else it },
                bssid = info?.bssid ?: "Unknown",
                rssi = info?.rssi ?: 0,
                linkSpeed = info?.linkSpeed ?: 0,
                frequency = info?.frequency ?: 0,
                ipAddress = ip,
                gateway = gateway,
                mask = mask,
                dns1 = dns1,
                dns2 = dns2,
                isWifi = true
            )
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // Basic mobile info
            _wifiState.value = WifiInfo(
                ssid = "Mobile Data (5G/LTE)",
                bssid = "Cellular Tower",
                isWifi = false,
                ipAddress = getLocalIpAddress() ?: "0.0.0.0"
            )
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val en = java.net.NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is java.net.Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    fun updateWifiInfo() = updateNetworkInfo()

    fun updateNetworkQuality(packetLoss: Float, avgLatency: Long) {
        _wifiState.value = _wifiState.value.copy(
            packetLoss = packetLoss,
            avgLatency = avgLatency
        )
    }

    /**
     * Retrieves nearby WiFi access points.
     */
    fun getNearbyWifi(): List<NearbyWifi> {
        return try {
            wifiManager.scanResults.map { result ->
                NearbyWifi(
                    ssid = if (result.SSID.isNullOrEmpty()) "Hidden Network" else result.SSID,
                    bssid = result.BSSID ?: "Unknown",
                    rssi = result.level,
                    frequency = result.frequency,
                    capabilities = result.capabilities ?: "",
                    level = WifiManager.calculateSignalLevel(result.level, 5)
                )
            }.sortedByDescending { it.rssi }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
