package com.dapursappa.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapursappa.app.R
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID = Locale.forLanguageTag("id-ID")
private val DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", ID)
private val TIME_FMT = DateTimeFormatter.ofPattern("HH.mm", ID)

@Composable
fun AppHeader() {
    val c = Sappa.c
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.header)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo Dapur Sappa",
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text("Dapur Sappa", color = c.headerText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(now.format(DATE_FMT), color = c.headerText, fontSize = 11.sp)
            Text("${now.format(TIME_FMT)} WIB", color = c.headerText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun SCard(
    modifier: Modifier = Modifier,
    background: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Sappa.c
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background ?: c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val c = Sappa.c
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) c.accentSoft else c.surface2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, color = if (highlight) c.accentText else c.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = if (highlight) c.accentText else c.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Sappa.c
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
    ) { Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Sappa.c
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, c.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
    ) { Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Sappa.c
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) c.accentSoft else c.surface)
            .border(1.5.dp, if (selected) c.accent else c.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        color = if (selected) c.accentText else c.text2,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
}

@Composable
fun Tag(text: String, background: Color, color: Color) {
    Text(
        text,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(background).padding(horizontal = 10.dp, vertical = 3.dp),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
fun RowSpaced(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
