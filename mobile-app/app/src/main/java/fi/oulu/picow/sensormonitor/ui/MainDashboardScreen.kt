package fi.oulu.picow.sensormonitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.oulu.picow.sensormonitor.model.Measurement
import kotlinx.coroutines.delay

/**
 * Main dashboard screen showing the latest sensor measurement.
 *
 * Responsibilities:
 * - Observe [MeasurementViewModel] UI state
 * - Periodically refresh the current measurement while visible
 * - Provide navigation entry point to the history screen
 *
 * The screen itself remains stateless; all data and refresh logic
 * is delegated to the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: MeasurementViewModel,
    onOpenHistory: () -> Unit
) {
    // Collect UI state from the ViewModel as Compose state
    val uiState by viewModel.uiState.collectAsState()

    /**
     * Auto-refresh loop:
     * - Runs while this composable is in the composition
     * - Triggers a refresh every 10 seconds
     * - Automatically cancelled when the screen leaves composition
     */
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PicoW Sensor Monitor") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            // Render content based on the current UI state
            when (uiState) {
                is MeasurementUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is MeasurementUiState.Error -> {
                    // Simple error placeholder; can be expanded with retry UI later
                    Text("Failed to load data")
                }

                is MeasurementUiState.Success -> {
                    val m = (uiState as MeasurementUiState.Success).measurement
                    CurrentMeasurementCard(
                        measurement = m,
                        onOpenHistory = onOpenHistory,
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}

/**
 * Card displaying the most recent sensor measurement.
 *
 * The entire card is clickable to provide a natural navigation
 * affordance to the history screen.
 */
@Composable
fun CurrentMeasurementCard(
    measurement: Measurement,
    onOpenHistory: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        /**
         * Primary measurement card.
         *
         * Clicking the card navigates directly to the history view,
         * reducing the need for a separate "History" button.
         */
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable { onOpenHistory() },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Prominent temperature display
                Text(
                    text = "${measurement.temperatureC} °C",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                )

                // Secondary measurement information
                Text(text = "Pressure: ${measurement.pressureHpa} hPa")
                Text(text = "Device: ${measurement.deviceId}")

                // Timestamp of the latest update
                Text(
                    text = "Last updated: ${measurement.timestamp}",
                    style = MaterialTheme.typography.bodySmall
                )

                // Subtle affordance hint for navigation
                Text(
                    text = "Tap to view history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        /**
         * Manual refresh action.
         *
         * Useful when the user wants immediate feedback instead of
         * waiting for the automatic refresh interval.
         */
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh")
        }
    }
}
