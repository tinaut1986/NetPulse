package com.tinaut1986.wifitools

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinaut1986.wifitools.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WifiViewModel(application: Application) : AndroidViewModel(application) {
    private val wifiScanner = WifiScanner(application)
    private val networkScanner = NetworkScanner()
    private val pingTool = PingTool()
    private val speedTestTool = SpeedTestTool()

    val wifiInfo = wifiScanner.wifiState
    val signalHistory = mutableStateListOf<Int>()
    
    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    private val _pingResult = MutableStateFlow<String?>(null)
    val pingResult: StateFlow<String?> = _pingResult

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging

    private val _toolResult = MutableStateFlow<String?>(null)
    val toolResult: StateFlow<String?> = _toolResult

    private val _publicIp = MutableStateFlow<String>("Loading...")
    val publicIp: StateFlow<String> = _publicIp

    private val _isTestingSpeed = MutableStateFlow(false)
    val isTestingSpeed: StateFlow<Boolean> = _isTestingSpeed

    private val _speedTestProgress = MutableStateFlow(0f)
    val speedTestProgress: StateFlow<Float> = _speedTestProgress

    private val _downloadSpeed = MutableStateFlow<Double?>(null)
    val downloadSpeed: StateFlow<Double?> = _downloadSpeed

    private val _uploadSpeed = MutableStateFlow<Double?>(null)
    val uploadSpeed: StateFlow<Double?> = _uploadSpeed

    private val _speedTestPhase = MutableStateFlow("") // "download", "upload", ""
    val speedTestPhase: StateFlow<String> = _speedTestPhase

    private var pingJob: kotlinx.coroutines.Job? = null

    init {
        startSignalMonitoring()
        fetchPublicIp()
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            var lastConn = ""
            wifiInfo.collect { info ->
                val currentConn = "${info.ssid}_${info.ipAddress}"
                if (currentConn != lastConn && info.ipAddress != "0.0.0.0") {
                    lastConn = currentConn
                    fetchPublicIp()
                    if (info.isWifi) scanDevices() // Auto scan on new WiFi
                }
            }
        }
    }

    private fun startSignalMonitoring() {
        viewModelScope.launch {
            while (true) {
                wifiScanner.updateWifiInfo()
                val info = wifiInfo.value
                val currentRssi = info.rssi
                if (currentRssi != 0) {
                    signalHistory.add(currentRssi)
                    if (signalHistory.size > 50) signalHistory.removeAt(0)
                }

                // Continuous packet loss check
                val gateway = info.gateway
                if (gateway != "0.0.0.0") {
                    var successCount = 0
                    var totalLatency = 0L
                    val testCount = 2 // Keeping it low for continuous check
                    for (i in 1..testCount) {
                        val latency = pingTool.ping(gateway, 1000)
                        if (latency >= 0) {
                            successCount++
                            totalLatency += latency
                        }
                    }
                    val packetLoss = ((testCount - successCount).toFloat() / testCount) * 100
                    val avgLatency = if (successCount > 0) totalLatency / successCount else 0L
                    wifiScanner.updateNetworkQuality(packetLoss, avgLatency)
                }
                
                delay(3000) // Increased delay slightly to accommodate pings
            }
        }
    }

    fun scanDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            val gateway = wifiInfo.value.gateway
            if (gateway != "0.0.0.0") {
                _devices.value = networkScanner.scanSubnet(gateway)
            }
            _isScanning.value = false
        }
    }

    fun runPing(host: String) {
        if (_isPinging.value) return
        
        pingJob = viewModelScope.launch {
            _isPinging.value = true
            _pingResult.value = "Pinging $host..."
            val results = mutableListOf<String>()
            
            while (_isPinging.value) {
                val time = pingTool.ping(host)
                val line = if (time >= 0) "Reply from $host: time=${time}ms" else "Request timed out"
                results.add(line)
                if (results.size > 10) results.removeAt(0)
                _pingResult.value = results.joinToString("\n")
                delay(1000)
            }
        }
    }

    fun stopPing() {
        _isPinging.value = false
        pingJob?.cancel()
    }

    fun fetchPublicIp() {
        viewModelScope.launch {
            _publicIp.value = pingTool.getPublicIp() ?: "Failed to detect"
        }
    }

    fun runPortCheck(host: String, port: Int) {
        viewModelScope.launch {
            _toolResult.value = "Checking $host:$port..."
            val open = pingTool.checkPort(host, port)
            _toolResult.value = if (open) "Port $port is OPEN on $host" else "Port $port is CLOSED on $host"
        }
    }

    fun runDnsLookup(host: String) {
        viewModelScope.launch {
            _toolResult.value = "Resolving $host..."
            val ips = pingTool.dnsLookup(host)
            _toolResult.value = if (ips.isNotEmpty()) "Resolved IPs:\n" + ips.joinToString("\n") else "Could not resolve $host"
        }
    }

    fun runTraceroute(host: String) {
        if (_isPinging.value) return
        pingJob = viewModelScope.launch {
            _isPinging.value = true
            val results = mutableListOf<String>()
            _toolResult.value = "Traceroute to $host..."
            pingTool.traceroute(host) { hop, ip, time ->
                results.add("$hop: $ip (${time}ms)")
                _toolResult.value = results.joinToString("\n")
            }
            _isPinging.value = false
        }
    }

    fun runSpeedTest() {
        if (_isTestingSpeed.value) return
        viewModelScope.launch {
            _isTestingSpeed.value = true
            _speedTestProgress.value = 0f
            _downloadSpeed.value = null
            _uploadSpeed.value = null
            
            // Phase 1: Download
            _speedTestPhase.value = "download"
            val dSpeed = speedTestTool.runDownloadTest { progress ->
                _speedTestProgress.value = progress
            }
            _downloadSpeed.value = if (dSpeed >= 0) dSpeed else 0.0
            
            // Phase 2: Upload
            _speedTestPhase.value = "upload"
            _speedTestProgress.value = 0f
            val uSpeed = speedTestTool.runUploadTest { progress ->
                _speedTestProgress.value = progress
            }
            _uploadSpeed.value = if (uSpeed >= 0) uSpeed else 0.0
            
            _speedTestPhase.value = "finished"
            _isTestingSpeed.value = false
        }
    }
}
