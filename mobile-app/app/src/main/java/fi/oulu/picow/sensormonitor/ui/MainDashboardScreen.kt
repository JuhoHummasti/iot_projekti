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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: MeasurementViewModel,
    onOpenHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    // Auto-refresh current measurement every 10 seconds while this screen is visible
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)   // 10 seconds – adjust if needed
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
            when (uiState) {
                is MeasurementUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is MeasurementUiState.Error -> {
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
        // The "sensor card" – now clickable
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable { onOpenHistory() },   // 👈 go to history when tapped
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${measurement.temperatureC} °C",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                )
                Text(text = "Pressure: ${measurement.pressureHpa} hPa")
                Text(text = "Device: ${measurement.deviceId}")
                Text(
                    text = "Last updated: ${measurement.timestamp}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Tap to view history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh")
        }
    }
}
