package fi.oulu.picow.sensormonitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.oulu.picow.sensormonitor.model.Measurement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorMonitorApp(
    viewModel: MeasurementViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PicoW Sensor Monitor") }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                when (uiState) {
                    is MeasurementUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MeasurementUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Failed to load data")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Try again")
                            }
                        }
                    }
                    is MeasurementUiState.Success -> {
                        val m = (uiState as MeasurementUiState.Success).measurement
                        SensorMonitorHome(
                            measurement = m,
                            onRefresh = { viewModel.refresh() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorMonitorHome(
    measurement: Measurement,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Main card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
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
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status text
        Text(
            text = "Status: Connected (mock data)",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        // Refresh button at bottom
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
        ) {
            Text("Refresh")
        }
    }
}
