package com.dapursappa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Prefs
import com.dapursappa.app.data.Store
import com.dapursappa.app.data.Sync
import com.dapursappa.app.data.Updater
import com.dapursappa.app.ui.UpdateDialog
import androidx.compose.runtime.LaunchedEffect
import com.dapursappa.app.ui.CodeScreen
import com.dapursappa.app.ui.DapurSappaTheme
import com.dapursappa.app.ui.MainScreen
import com.dapursappa.app.ui.PickUserScreen
import com.dapursappa.app.ui.Sappa

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        Store.init(applicationContext)
        Sync.start(prefs)
        setContent {
            val data by Store.data.collectAsState()
            var mode by remember { mutableIntStateOf(prefs.themeMode) }
            var codeOk by remember { mutableStateOf(prefs.codeOk) }
            var userIdx by remember { mutableIntStateOf(prefs.userIndex) }
            var isDev by remember { mutableStateOf(prefs.isDev) }
            var update by remember { mutableStateOf<Updater.Info?>(null) }
            LaunchedEffect(Unit) {
                Updater.check { info, _ -> update = info }
            }

            DapurSappaTheme(mode) {
                val upd = update
                if (upd != null) UpdateDialog(upd) { update = null }
                Surface(Modifier.fillMaxSize(), color = Sappa.c.bg) {
                    val validUser = userIdx >= 0 && userIdx < data.users.size
                    when {
                        !codeOk -> CodeScreen(onOk = {
                            prefs.codeOk = true
                            codeOk = true
                        })
                        !isDev && !validUser -> PickUserScreen(
                            prefs, data.users,
                            onPicked = {
                                prefs.isDev = false
                                prefs.userIndex = it
                                isDev = false
                                userIdx = it
                            },
                            onDev = {
                                prefs.isDev = true
                                isDev = true
                            },
                        )
                        else -> {
                            val me = if (isDev) Me(-1, true, "Developer", "Developer")
                            else Me(userIdx, false, data.users[userIdx].name, data.users[userIdx].role)
                            MainScreen(
                                prefs = prefs,
                                me = me,
                                data = data,
                                themeMode = mode,
                                onThemeMode = {
                                    prefs.themeMode = it
                                    mode = it
                                },
                                onSwitchUser = {
                                    prefs.userIndex = -1
                                    prefs.isDev = false
                                    userIdx = -1
                                    isDev = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
