package com.tinaut1986.netpulse.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedDiagnostic(
    val id: String,           // filename without extension
    val timestampMs: Long,
    val label: String,        // human-readable date+time
    val ssid: String,
    val overallScore: Int,
    val problems: List<String>
)

class DiagnosticHistoryManager(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "diagnostics").also { it.mkdirs() }

    // ---------------------------------------------------------------
    // Save
    // ---------------------------------------------------------------
    fun save(report: DiagnosticReport, ssid: String): String {
        val ts = System.currentTimeMillis()
        val id = "diag_$ts"
        val file = File(dir, "$id.json")
        file.writeText(reportToJson(report, ssid, ts, id))
        pruneOldEntries()
        return id
    }

    // ---------------------------------------------------------------
    // List (sorted newest first)
    // ---------------------------------------------------------------
    fun list(): List<SavedDiagnostic> {
        return dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { readSummary(it) }
            ?.sortedByDescending { it.timestampMs }
            ?: emptyList()
    }

    // ---------------------------------------------------------------
    // Load full report
    // ---------------------------------------------------------------
    fun load(id: String): Pair<DiagnosticReport, String>? {
        val file = File(dir, "$id.json")
        if (!file.exists()) return null
        return try {
            parseReport(JSONObject(file.readText()))
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------
    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun deleteMultiple(ids: List<String>) {
        ids.forEach { delete(it) }
    }

    // ---------------------------------------------------------------
    // Export multiple as a single combined text file
    // ---------------------------------------------------------------
    fun exportMultipleAsText(ids: List<String>): String {
        val separator = "\n\n${"═".repeat(48)}\n\n"
        return ids.mapNotNull { exportAsText(it) }.joinToString(separator)
    }

    fun writeMultipleExportFile(ids: List<String>): File? {
        val text = exportMultipleAsText(ids)
        if (text.isBlank()) return null
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val ts = fmt.format(Date())
        val count = ids.size
        val file = File(context.cacheDir, "NetPulse_${count}diag_$ts.txt")
        file.writeText(text)
        return file
    }

    // ---------------------------------------------------------------
    // Export as human-readable text
    // ---------------------------------------------------------------
    fun exportAsText(id: String): String? {
        val (report, ssid) = load(id) ?: return null
        val sb = StringBuilder()
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val ts = id.removePrefix("diag_").toLongOrNull()?.let { fmt.format(Date(it)) } ?: "?"

        sb.appendLine("════════════════════════════════")
        sb.appendLine("  NetPulse — Diagnóstico de Red")
        sb.appendLine("════════════════════════════════")
        sb.appendLine("Fecha:   $ts")
        sb.appendLine("Red:     $ssid")
        sb.appendLine("Puntuación: ${report.overallScore}/100")
        sb.appendLine()

        if (report.problems.isEmpty()) {
            sb.appendLine("✓ Sin problemas detectados")
        } else {
            sb.appendLine("⚠ Problemas detectados:")
            report.problems.forEach { sb.appendLine("  • $it") }
        }
        sb.appendLine()

        fun appendTarget(label: String, diag: TargetDiagnostic?) {
            if (diag == null) return
            sb.appendLine("── $label (${diag.target.host}) ──")
            sb.appendLine("  Pérdida:  ${diag.packetLoss.toInt()}%")
            sb.appendLine("  Min:      ${if (diag.minLatency >= 0) "${diag.minLatency}ms" else "—"}")
            sb.appendLine("  Media:    ${if (diag.avgLatency >= 0) "${diag.avgLatency}ms" else "—"}")
            sb.appendLine("  Máx:      ${if (diag.maxLatency >= 0) "${diag.maxLatency}ms" else "—"}")
            sb.appendLine("  Jitter:   ${diag.jitter}ms")
            sb.appendLine("  Muestras: ${diag.samples.map { if (it.latencyMs >= 0) "${it.latencyMs}ms" else "timeout" }.joinToString(", ")}")
            sb.appendLine()
        }

        appendTarget("Router / Gateway", report.gatewayDiag)
        appendTarget("DNS Local", report.dns1Diag)
        appendTarget("Google DNS (8.8.8.8)", report.googleDnsDiag)
        appendTarget("Cloudflare (1.1.1.1)", report.internetDiag)

        sb.appendLine("── Resolución DNS ──")
        sb.appendLine("  Tiempo: ${if (report.dnsResolutionMs >= 0) "${report.dnsResolutionMs}ms" else "Fallo"}")
        sb.appendLine()
        sb.appendLine("Generado por NetPulse · https://github.com/tinaut1986/NetPulse")
        return sb.toString()
    }

    // ---------------------------------------------------------------
    // Write shareable file to cache dir (for share intent)
    // ---------------------------------------------------------------
    fun writeExportFile(id: String): File? {
        val text = exportAsText(id) ?: return null
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val ts = id.removePrefix("diag_").toLongOrNull()?.let { fmt.format(Date(it)) } ?: id
        val file = File(context.cacheDir, "NetPulse_diag_$ts.txt")
        file.writeText(text)
        return file
    }

    // ---------------------------------------------------------------
    // Keep at most 50 entries
    // ---------------------------------------------------------------
    private fun pruneOldEntries(keep: Int = 50) {
        val files = dir.listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.nameWithoutExtension.removePrefix("diag_").toLongOrNull() ?: 0L }
            ?: return
        files.drop(keep).forEach { it.delete() }
    }

    // ---------------------------------------------------------------
    // JSON serialisation
    // ---------------------------------------------------------------
    private fun reportToJson(report: DiagnosticReport, ssid: String, ts: Long, id: String): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("ts", ts)
        root.put("ssid", ssid)
        root.put("score", report.overallScore)
        root.put("problems", JSONArray(report.problems))
        root.put("dnsResMs", report.dnsResolutionMs)

        fun targetToJson(diag: TargetDiagnostic?): JSONObject? {
            if (diag == null) return null
            val o = JSONObject()
            o.put("name", diag.target.name)
            o.put("host", diag.target.host)
            o.put("category", diag.target.category)
            o.put("avg", diag.avgLatency)
            o.put("min", diag.minLatency)
            o.put("max", diag.maxLatency)
            o.put("jitter", diag.jitter)
            o.put("loss", diag.packetLoss.toDouble())
            val samples = JSONArray()
            diag.samples.forEach { s ->
                val so = JSONObject()
                so.put("ts", s.timestampMs)
                so.put("lat", s.latencyMs)
                samples.put(so)
            }
            o.put("samples", samples)
            return o
        }

        report.gatewayDiag?.let { root.put("gateway", targetToJson(it)) }
        report.dns1Diag?.let { root.put("dns1", targetToJson(it)) }
        report.googleDnsDiag?.let { root.put("google", targetToJson(it)) }
        report.internetDiag?.let { root.put("internet", targetToJson(it)) }
        return root.toString(2)
    }

    private fun jsonToTarget(o: JSONObject): TargetDiagnostic {
        val target = PingTarget(
            name = o.getString("name"),
            host = o.getString("host"),
            category = o.getString("category")
        )
        val samplesArr = o.getJSONArray("samples")
        val samples = (0 until samplesArr.length()).map { i ->
            val s = samplesArr.getJSONObject(i)
            PingSample(s.getLong("ts"), s.getLong("lat"))
        }
        return TargetDiagnostic(
            target = target,
            samples = samples,
            avgLatency = o.getLong("avg"),
            minLatency = o.getLong("min"),
            maxLatency = o.getLong("max"),
            jitter = o.getLong("jitter"),
            packetLoss = o.getDouble("loss").toFloat()
        )
    }

    private fun parseReport(root: JSONObject): Pair<DiagnosticReport, String> {
        val ssid = root.optString("ssid", "")
        val problems = (0 until root.getJSONArray("problems").length())
            .map { root.getJSONArray("problems").getString(it) }

        val report = DiagnosticReport(
            gatewayDiag = root.optJSONObject("gateway")?.let { jsonToTarget(it) },
            dns1Diag = root.optJSONObject("dns1")?.let { jsonToTarget(it) },
            googleDnsDiag = root.optJSONObject("google")?.let { jsonToTarget(it) },
            internetDiag = root.optJSONObject("internet")?.let { jsonToTarget(it) },
            dnsResolutionMs = root.getLong("dnsResMs"),
            overallScore = root.getInt("score"),
            problems = problems
        )
        return Pair(report, ssid)
    }

    private fun readSummary(file: File): SavedDiagnostic? {
        return try {
            val root = JSONObject(file.readText())
            val ts = root.getLong("ts")
            val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            SavedDiagnostic(
                id = root.getString("id"),
                timestampMs = ts,
                label = fmt.format(Date(ts)),
                ssid = root.optString("ssid", "?"),
                overallScore = root.getInt("score"),
                problems = (0 until root.getJSONArray("problems").length())
                    .map { root.getJSONArray("problems").getString(it) }
            )
        } catch (e: Exception) { null }
    }
}
