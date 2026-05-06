package com.dkajan.notifixer

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.BufferedReader

object BackupUtils {

    fun exportToUri(context: Context, uri: Uri, packages: List<String>): Boolean {
        return try {
            val jsonArray = JSONArray()
            packages.forEach { jsonArray.put(it) }
            val jsonString = jsonArray.toString(4)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importFromUri(context: Context, uri: Uri): List<String> {
        val packages = mutableListOf<String>()
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { reader -> reader.readText() }
            } ?: ""

            if (jsonString.isNotBlank()) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    packages.add(jsonArray.getString(i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return packages
    }
}