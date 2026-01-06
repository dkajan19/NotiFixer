package com.dkajan.notifixer

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

fun checkCurrentPermission(pkg: String): Boolean {
    if (!Shizuku.pingBinder()) return false
    return try {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", "appops get $pkg SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS"), null, null)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readLine() ?: ""
        process.waitFor()
        output.contains("allow")
    } catch (e: Exception) { false }
}

fun runAdbCommand(pkg: String, isEnabled: Boolean) {
    if (!Shizuku.pingBinder()) return
    val mode = if (isEnabled) "allow" else "default"
    val cmd = "appops set $pkg SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS $mode"
    try { Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null).waitFor() } catch (e: Exception) { e.printStackTrace() }
}