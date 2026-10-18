package com.dapursappa.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.dapursappa.app.Config
import com.dapursappa.app.data.Updater

fun openUrl(ctx: android.content.Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // abaikan jika tidak ada browser
    }
}

@Composable
fun UpdateDialog(info: Updater.Info, onDismiss: () -> Unit) {
    val c = Sappa.c
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Update tersedia", color = c.text, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Versi ${info.version} sudah ada (di HP ini: ${Config.APP_VERSION}).", color = c.text)
                if (info.notes.isNotBlank()) {
                    Text(info.notes.take(600), color = c.text2, modifier = Modifier.padding(top = 8.dp))
                }
                Text(
                    "Tekan Update, unduh file APK, lalu buka dan pilih Pasang. Data tidak hilang.",
                    color = c.text2, modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { openUrl(ctx, info.url); onDismiss() }) {
                Text("Update sekarang", color = c.accentText, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Nanti", color = c.text2) } },
    )
}
