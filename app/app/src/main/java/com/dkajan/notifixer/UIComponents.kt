package com.dkajan.notifixer

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AppIcon(packageName: String, pm: PackageManager) {
    val icon = remember(packageName) {
        try { pm.getApplicationIcon(packageName).toBitmap(120, 120).asImageBitmap() }
        catch (e: Exception) { null }
    }
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 12.dp))
    }
}

@Composable
fun WarningBox(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Important Step", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "To apply changes, clear all existing notifications from the target app. The setting usually takes effect when a new notification appears. If it remains dismissible, clear the notifications again and force stop the target app.",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ShizukuErrorScreen(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Shizuku is not running", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "The Shizuku service was disconnected or is not started.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry / Reconnect") }
    }
}