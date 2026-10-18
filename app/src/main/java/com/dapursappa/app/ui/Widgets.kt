package com.dapursappa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    placeholder: String = "",
    enabled: Boolean = true,
    password: Boolean = false,
) {
    val c = Sappa.c
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = c.text3) },
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.accentText,
            unfocusedLabelColor = c.text2,
            cursorColor = c.accent,
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            disabledTextColor = c.text2,
            disabledBorderColor = c.border,
            disabledLabelColor = c.text3,
        ),
    )
}

@Composable
fun SubHeader(title: String, sub: String = "", onBack: () -> Unit) {
    val c = Sappa.c
    Row(
        Modifier.fillMaxWidth().background(c.header).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹",
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(horizontal = 16.dp, vertical = 2.dp),
            color = c.headerText, fontSize = 30.sp, fontWeight = FontWeight.Bold,
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = c.headerText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            if (sub.isNotEmpty()) Text(sub, color = c.headerText, fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(top = 6.dp), color = Sappa.c.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Int = 12, color: Color? = null) {
    Text(text, modifier = modifier, color = color ?: Sappa.c.text2, fontSize = size.sp)
}

@Composable
fun Strong(text: String, modifier: Modifier = Modifier, size: Int = 14, color: Color? = null) {
    Text(text, modifier = modifier, color = color ?: Sappa.c.text, fontSize = size.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
fun Banner(small: String, big: String) {
    val c = Sappa.c
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accentSoft).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(small, color = c.accentText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(big, color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ArrowBtn(symbol: String, onClick: () -> Unit) {
    val c = Sappa.c
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).border(1.5.dp, c.border, RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun NavBar(title: String, sub: String, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = Sappa.c
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArrowBtn("‹", onPrev)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(sub, color = c.text2, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        ArrowBtn("›", onNext)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
fun ChipScroll(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int = 0) {
    val c = Sappa.c
    Row(verticalAlignment = Alignment.CenterVertically) {
        ArrowBtn("−") { if (value > min) onChange(value - 1) }
        Text(
            value.toString(),
            modifier = Modifier.width(64.dp),
            color = c.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
        )
        ArrowBtn("+") { onChange(value + 1) }
    }
}

@Composable
fun Rule() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(Sappa.c.border))
}

@Composable
fun ClickCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = Sappa.c
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.border, shape).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                onDismiss()
            }) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    ) { DatePicker(state = state) }
}

fun digits(s: String): String = s.filter { it.isDigit() }.take(12)

fun canEdit(me: com.dapursappa.app.data.Me, dateIso: String): Boolean =
    me.canAdmin || dateIso == LocalDate.now().toString()
