package com.dkajan.notifixer

import rikka.shizuku.Shizuku
import java.io.BufferedReader

private fun runShizukuShellCommand(command: String): Pair<Int, String> {
    if (!Shizuku.pingBinder()) return -1 to "Shizuku is not running"

    return try {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
        val exitCode = process.waitFor()
        exitCode to (if (error.isNotBlank()) error else output)
    } catch (e: Exception) {
        -1 to (e.message ?: "Unknown shell error")
    }
}

fun checkCurrentPermission(pkg: String): Boolean {
    val (_, output) = runShizukuShellCommand("appops get $pkg SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS")
    return output.contains("allow")
}

fun runAdbCommand(pkg: String, isEnabled: Boolean) {
    val mode = if (isEnabled) "allow" else "default"
    val cmd = "appops set $pkg SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS $mode"
    runShizukuShellCommand(cmd)
}

fun forceCloseAndRelaunchInBackground(pkg: String): Pair<Boolean, String> {
    val (stopCode, stopOutput) = runShizukuShellCommand("am force-stop $pkg")
    if (stopCode != 0) {
        return false to "Force-stop failed: $stopOutput"
    }

    val launchCommand = "monkey -p $pkg -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1"
    val (launchCode, launchOutput) = runShizukuShellCommand(launchCommand)

    if (launchCode != 0) {
        return false to "Relaunch failed: $launchOutput"
    }

    return true to "App restarted in background"
}