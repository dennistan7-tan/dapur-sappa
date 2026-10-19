package com.dapursappa.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dapursappa.app.data.AppData
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Prefs
import java.time.LocalDate

enum class Tab(val label: String, val icon: ImageVector) {
    BELANJA("Belanja", Icons.Default.ShoppingCart),
    ORDER("Order", Icons.Default.Create),
    SUMMARY("Summary", Icons.Default.Home),
    LAPORAN("Laporan", Icons.Default.DateRange),
    LAINNYA("Lainnya", Icons.Default.Menu),
}

sealed class Route {
    data class Detail(val date: LocalDate) : Route()
    data class OrderEdit(val id: String?, val date: LocalDate) : Route()
    object MenuEdit : Route()
    object Riwayat : Route()
    object Kelola : Route()
}

@Composable
fun MainScreen(
    prefs: Prefs,
    me: Me,
    data: AppData,
    themeMode: Int,
    onThemeMode: (Int) -> Unit,
    onSwitchUser: () -> Unit,
) {
    val c = Sappa.c
    val tabs = Tab.values().toList()
    val start = Tab.SUMMARY
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    var tab by remember(me.idx, me.isDev) { mutableStateOf(start) }
    var route by remember { mutableStateOf<Route?>(null) }
    val today = LocalDate.now()
    var orderDate by remember { mutableStateOf(today) }
    var weekStart by remember { mutableStateOf(Fmt.mondayOf(today)) }
    var repTab by remember { mutableIntStateOf(2) }
    var repMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var repWeek by remember { mutableStateOf(Fmt.mondayOf(today)) }

    BackHandler(enabled = route != null) { route = null }

    Scaffold(
        containerColor = c.bg,
        topBar = { if (route == null) AppHeader() },
        bottomBar = {
            NavigationBar(containerColor = c.surface) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t; route = null },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.accentText,
                            selectedTextColor = c.accentText,
                            indicatorColor = c.accentSoft,
                            unselectedIconColor = c.text2,
                            unselectedTextColor = c.text2,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val r = route
            if (r != null) {
                when (r) {
                    is Route.Detail -> DetailHari(me, data, r.date, { route = null }, { id -> route = Route.OrderEdit(id, r.date) })
                    is Route.OrderEdit -> OrderForm(me, data, r.id, r.date) { route = null }
                    Route.MenuEdit -> MenuScreen(me, data) { route = null }
                    Route.Riwayat -> RiwayatScreen(data) { route = null }
                    Route.Kelola -> KelolaUserScreen(me, data, prefs) { route = null }
                }
            } else {
                when (tab) {
                    Tab.BELANJA -> BelanjaTab(me, data)
                    Tab.ORDER -> OrderTab(me, data, orderDate, { orderDate = it }, { id, d -> route = Route.OrderEdit(id, d) })
                    Tab.SUMMARY -> SummaryTab(
                        data, weekStart, { weekStart = it },
                        { d -> route = Route.Detail(d) }, dark, { onThemeMode(if (dark) 1 else 2) },
                    )
                    Tab.LAPORAN -> LaporanTab(
                        data, repTab, { repTab = it }, repMonth, { repMonth = it.withDayOfMonth(1) },
                        repWeek, { repWeek = it }, { d -> route = Route.Detail(d) },
                    )
                    Tab.LAINNYA -> MoreTab(
                        me, prefs, onSwitchUser,
                        { route = Route.MenuEdit }, { route = Route.Riwayat }, { route = Route.Kelola },
                    )
                }
            }
        }
    }
}
