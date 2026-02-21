package com.tinaut1986.wifitools

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinaut1986.wifitools.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WifiViewModel(application: Application) : AndroidViewModel(application) {
    private val wifiScanner = WifiScanner(application)
    private val networkScanner = NetworkScanner()
    private val pingTool = PingTool()

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

    private var pingJob: kotlinx.coroutines.Job? = null

    init {
        startSignalMonitoring()
    }

    private fun startSignalMonitoring() {
        viewModelScope.launch {
            while (true) {
                wifiScanner.updateWifiInfo()
                val currentRssi = wifiInfo.value.rssi
                if (currentRssi != 0) {
                    signalHistory.add(currentRssi)
                    if (signalHistory.size > 50) signalHistory.removeAt(0)
                }
                delay(2000)
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
}
