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

/**
 * Main entry point of the application.
 *
 * Hosts the Compose UI and provides simple in-memory navigation between
 * the main dashboard and the history screen.
 *
 * Navigation is intentionally lightweight (enum-based) to avoid introducing
 * the Navigation component for a small app.
 */
class MainActivity : ComponentActivity() {

    /**
     * ViewModel scoped to the Activity lifecycle.
     *
     * Shared across composables so the latest measurement survives
     * configuration changes and screen navigation.
     */
    private val measurementViewModel: MeasurementViewModel by viewModels()

    /**
     * Top-level navigation destinations within the app.
     */
    enum class AppScreen {
        MAIN,
        HISTORY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            /**
             * Simple navigation state.
             *
             * Stored in Compose memory and reset only when the Activity
             * is destroyed.
             */
            var currentScreen by remember {
                mutableStateOf(AppScreen.MAIN)
            }

            /**
             * History ViewModel created inside the Compose hierarchy.
             *
             * Scoped to the navigation graph (here: the Activity),
             * but owned by the history screen.
             */
            val historyViewModel: HistoryViewModel = viewModel()

            when (currentScreen) {

                AppScreen.MAIN -> {
                    MainDashboardScreen(
                        viewModel = measurementViewModel,
                        onOpenHistory = {
                            // Navigate to history screen
                            currentScreen = AppScreen.HISTORY
                        }
                    )
                }

                AppScreen.HISTORY -> {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onBack = {
                            // Navigate back to main dashboard
                            currentScreen = AppScreen.MAIN
                        }
                    )
                }
            }
        }
    }
}
