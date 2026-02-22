package com.tinaut1986.netpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class SpeedTestTool {
    
    // Cloudflare speed test endpoints
    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes="
    private val uploadUrl = "https://speed.cloudflare.com/__up"

    suspend fun runDownloadTest(onProgress: (Float) -> Unit): Double = withContext(Dispatchers.IO) {
        val chunkSizeBytes = 5 * 1024 * 1024L // 5MB per chunk
        val iterations = 3
        val speeds = mutableListOf<Double>()
        
        try {
            for (i in 0 until iterations) {
                val url = URL("$downloadUrl$chunkSizeBytes&_=${System.currentTimeMillis()}")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 15000
                
                var bytesRead = 0L
                val startTime = System.currentTimeMillis()
                
                connection.inputStream.use { input ->
                    val buffer = ByteArray(16384)
                    var bytes = input.read(buffer)
                    while (bytes != -1) {
                        bytesRead += bytes
                        // Calculate overall progress across iterations
                        val totalProgress = (i.toFloat() + (bytesRead.toFloat() / chunkSizeBytes)) / iterations
                        onProgress(totalProgress)
                        bytes = input.read(buffer)
                    }
                }
                
                val durationMs = System.currentTimeMillis() - startTime
                if (durationMs > 0) {
                    val bits = bytesRead * 8
                    val mbps = (bits.toDouble() / 1_000_000.0) / (durationMs.toDouble() / 1000.0)
                    speeds.add(mbps)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext -1.0
        }
        
        return@withContext if (speeds.isNotEmpty()) speeds.average() else -1.0
    }

    suspend fun runUploadTest(onProgress: (Float) -> Unit): Double = withContext(Dispatchers.IO) {
        val uploadSize = 2 * 1024 * 1024 // 2MB for upload
        val iterations = 2
        val speeds = mutableListOf<Double>()
        val data = ByteArray(uploadSize)
        Random.nextBytes(data)

        try {
            for (i in 0 until iterations) {
                val url = URL(uploadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/octet-stream")
                connection.setFixedLengthStreamingMode(uploadSize)
                connection.connectTimeout = 5000
                connection.readTimeout = 15000
                
                val startTime = System.currentTimeMillis()
                
                connection.outputStream.use { output ->
                    val bufferSize = 16384
                    var bytesWritten = 0
                    while (bytesWritten < uploadSize) {
                        val count = minOf(bufferSize, uploadSize - bytesWritten)
                        output.write(data, bytesWritten, count)
                        bytesWritten += count
                        val totalProgress = (i.toFloat() + (bytesWritten.toFloat() / uploadSize)) / iterations
                        onProgress(totalProgress)
                    }
                }
                
                // Read response to complete request
                connection.inputStream.use { it.readBytes() }
                
                val durationMs = System.currentTimeMillis() - startTime
                if (durationMs > 0) {
                    val bits = uploadSize.toLong() * 8
                    val mbps = (bits.toDouble() / 1_000_000.0) / (durationMs.toDouble() / 1000.0)
                    speeds.add(mbps)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext -1.0
        }
        
        return@withContext if (speeds.isNotEmpty()) speeds.average() else -1.0
    }
}
