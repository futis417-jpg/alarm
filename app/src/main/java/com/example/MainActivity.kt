package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.screens.AddAlarmScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RingingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.RingtonePlayer

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val ringingAlarmId by viewModel.ringingAlarmId.collectAsState()
                val ringingAlarmUri by viewModel.ringingAlarmUri.collectAsState()

                val navController = rememberNavController()

                LaunchedEffect(ringingAlarmId) {
                    if (ringingAlarmId != null) {
                        RingtonePlayer.play(this@MainActivity, ringingAlarmUri)
                    } else {
                        RingtonePlayer.stop()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (ringingAlarmId != null) {
                        RingingScreen(
                            onDismiss = {
                                viewModel.dismissRinging()
                            }
                        )
                    } else {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddAlarm = {
                                        navController.navigate("add_alarm")
                                    }
                                )
                            }
                            composable("add_alarm") {
                                AddAlarmScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onSaveAlarm = { hour, minute, uri ->
                                        viewModel.addAlarm(hour, minute, uri)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "ACTION_ALARM_RINGING") {
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            val uri = intent.getStringExtra("RINGTONE_URI")
            if (alarmId != -1) {
                viewModel.triggerAlarmRinging(alarmId, uri)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RingtonePlayer.stop()
    }
}
