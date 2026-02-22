package com.tinaut1986.netpulse.data

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
    val dns2: String = "0.0.0.0",
    val packetLoss: Float = 0f,
    val avgLatency: Long = 0,
    val isWifi: Boolean = true
) {
    fun getChannel(): Int {
        return if (frequency >= 2412 && frequency <= 2484) {
            (frequency - 2412) / 5 + 1
        } else if (frequency >= 5170 && frequency <= 5825) {
            (frequency - 5170) / 5 + 34
        } else {
            0
        }
    }
}

enum class DeviceType { ROUTER, PC, MOBILE, PRINTER, TV, SERVER, IOT, UNKNOWN }

data class DeviceInfo(
    val ip: String,
    val mac: String = "Unknown",
    val vendor: String = "Unknown",
    val hostname: String = "Unknown",
    val isReachable: Boolean = false,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val openPorts: List<Int> = emptyList()
)
