package com.tinaut1986.wifitools.data

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
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateWifiInfo()
            }

            override fun onLost(network: Network) {
                _wifiState.value = WifiInfo()
            }
        })
    }

    fun updateWifiInfo() {
        val info = wifiManager.connectionInfo
        val dhcp = wifiManager.dhcpInfo

        val ip = Formatter.formatIpAddress(info.ipAddress)
        val gateway = Formatter.formatIpAddress(dhcp.gateway)
        val mask = Formatter.formatIpAddress(dhcp.netmask)
        val dns1 = Formatter.formatIpAddress(dhcp.dns1)
        val dns2 = Formatter.formatIpAddress(dhcp.dns2)

        _wifiState.value = WifiInfo(
            ssid = info.ssid.replace("\"", ""),
            bssid = info.bssid ?: "Unknown",
            rssi = info.rssi,
            linkSpeed = info.linkSpeed,
            frequency = info.frequency,
            ipAddress = ip,
            gateway = gateway,
            mask = mask,
            dns1 = dns1,
            dns2 = dns2
        )
    }
}
