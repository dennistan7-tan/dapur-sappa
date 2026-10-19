package com.dapursappa.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapursappa.app.Config
import com.dapursappa.app.R
import com.dapursappa.app.data.Prefs
import com.dapursappa.app.data.UserCfg
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun CodeScreen(onOk: () -> Unit) {
    val c = Sappa.c
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit() {
        if (Config.isCodeValid(code)) onOk() else error = true
    }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo Dapur Sappa",
            modifier = Modifier.size(200.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("Selamat datang", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Masukkan kode toko untuk memakai aplikasi", color = c.text2, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { v ->
                code = v.filter { it.isDigit() }.take(5)
                error = false
            },
            modifier = Modifier.width(220.dp),
            singleLine = true,
            isError = error,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center, letterSpacing = 8.sp, color = c.text),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.border,
                errorBorderColor = c.accentText,
                cursorColor = c.accent,
            ),
        )
        if (error) {
            Spacer(Modifier.height(6.dp))
            Text("Kode salah. Coba lagi.", color = c.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Lanjut") { submit() }
        Spacer(Modifier.height(10.dp))
        Text("Kode hanya diminta sekali di HP ini.", color = c.text3, fontSize = 12.sp)
    }
}

@Composable
fun PickUserScreen(prefs: Prefs, users: List<UserCfg>, onPicked: (Int) -> Unit, onDev: () -> Unit) {
    val c = Sappa.c
    var selected by remember { mutableIntStateOf(0) }
    var showDev by remember { mutableStateOf(false) }
    if (selected >= users.size) selected = 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo Dapur Sappa",
            modifier = Modifier.size(130.dp),
        )
        Text("Kamu siapa?", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("Pilih nama supaya riwayat tahu siapa yang mencatat.", color = c.text2, fontSize = 13.sp, textAlign = TextAlign.Center)

        for (i in users.indices) {
            val on = i == selected
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (on) c.accentSoft else c.surface)
                    .border(1.5.dp, if (on) c.accent else c.border, shape)
                    .clickable { selected = i }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(users[i].name, color = if (on) c.accentText else c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("Belanja, order, menu dan laporan", color = c.text2, fontSize = 12.sp)
            }
        }

        PrimaryButton("Masuk sebagai ${users[selected].name}") { onPicked(selected) }
        OutlineButton("Masuk sebagai developer") { showDev = true }
    }

    if (showDev) DevLoginDialog(prefs, onDismiss = { showDev = false }, onOk = { showDev = false; onDev() })
}

@Composable
fun DevLoginDialog(prefs: Prefs, onDismiss: () -> Unit, onOk: () -> Unit) {
    val c = Sappa.c
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Login developer", color = c.text, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Password", pass, { pass = it; error = false }, password = true)
                if (error) Text("Password salah.", color = c.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (Config.sha256(pass) == prefs.devHash) onOk() else error = true
            }) { Text("Masuk", color = c.accentText, fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = c.text2) } },
    )
}
