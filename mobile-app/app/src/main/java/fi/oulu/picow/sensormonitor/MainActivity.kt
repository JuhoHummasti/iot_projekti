package fi.oulu.picow.sensormonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.oulu.picow.sensormonitor.ui.MainDashboardScreen
import fi.oulu.picow.sensormonitor.ui.MeasurementViewModel
import fi.oulu.picow.sensormonitor.ui.history.HistoryScreen
import fi.oulu.picow.sensormonitor.ui.history.HistoryViewModel

class MainActivity : ComponentActivity() {

    private val measurementViewModel: MeasurementViewModel by viewModels()
    enum class AppScreen { MAIN, HISTORY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Simple navigation state: MAIN or HISTORY

            var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

            // History VM created in composition
            val historyViewModel: HistoryViewModel = viewModel()

            when (currentScreen) {
                AppScreen.MAIN -> {
                    MainDashboardScreen(
                        viewModel = measurementViewModel,
                        onOpenHistory = {
                            currentScreen = AppScreen.HISTORY
                        }
                    )
                }

                AppScreen.HISTORY -> {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onBack = {
                            currentScreen = AppScreen.MAIN
                        }
                    )
                }
            }
        }
    }
}
