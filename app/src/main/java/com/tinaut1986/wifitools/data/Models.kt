package com.tinaut1986.wifitools.data

data class WifiInfo(
    val ssid: String = "Unknown",
    val bssid: String = "Unknown",
    val rssi: Int = 0,
    val linkSpeed: Int = 0,
    val frequency: Int = 0,
    val ipAddress: String = "0.0.0.0",
    val gateway: String = "0.0.0.0",
    val mask: String = "255.255.255.0",
    val dns1: String = "0.0.0.0",
    val dns2: String = "0.0.0.0"
)

data class DeviceInfo(
    val ip: String,
    val mac: String = "Unknown",
    val vendor: String = "Unknown",
    val hostname: String = "Unknown",
    val isReachable: Boolean = false
)
