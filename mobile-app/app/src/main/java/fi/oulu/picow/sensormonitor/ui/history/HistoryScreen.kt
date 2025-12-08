package fi.oulu.picow.sensormonitor.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.oulu.picow.sensormonitor.data.HistoryPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onBack: (() -> Unit)? = null
) {
    val currentRange = viewModel.selectedRange
    val periodLabel = viewModel.getCurrentPeriodLabel()
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "History") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RangeSelectorRow(
                selected = currentRange,
                onSelect = { range -> viewModel.selectRange(range) }
            )

            PeriodNavigationRow(
                label = periodLabel,
                canGoNext = viewModel.periodOffset < 0,
                onPrev = { viewModel.goToPreviousPeriod() },
                onNext = { viewModel.goToNextPeriod() }
            )

            when (uiState) {
                is HistoryUiState.Loading -> {
                    LoadingHistoryCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                is HistoryUiState.Error -> {
                    ErrorHistoryCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        message = uiState.message
                    )
                }

                is HistoryUiState.Success -> {
                    HistoryChartCard(
                        temperaturePoints = uiState.temperature,
                        pressurePoints = uiState.pressure,
                        selectedRange = currentRange,
                        periodLabel = periodLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryChartCard(
    temperaturePoints: List<HistoryPoint>,
    pressurePoints: List<HistoryPoint>,
    selectedRange: HistoryRange,
    periodLabel: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Temperature & pressure history",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${selectedRange.label} · $periodLabel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (temperaturePoints.isEmpty() && pressurePoints.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data in this period")
                    }
                } else {
                    // Chart area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        DualHistoryChart(
                            temperaturePoints = temperaturePoints,
                            pressurePoints = pressurePoints
                        )
                    }

                    // Legend + min / max summary under the chart
                    val tempValues = temperaturePoints.map { it.value }
                    val presValues = pressurePoints.map { it.value }

                    val tempColor = MaterialTheme.colorScheme.primary
                    val presColor = MaterialTheme.colorScheme.tertiary

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (tempValues.isNotEmpty()) {
                            Text(
                                text = "Temperature: " +
                                        "${"%.1f".format(tempValues.minOrNull()!!)} – " +
                                        "${"%.1f".format(tempValues.maxOrNull()!!)} °C",
                                color = tempColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (presValues.isNotEmpty()) {
                            Text(
                                text = "Pressure: " +
                                        "${"%.1f".format(presValues.minOrNull()!!)} – " +
                                        "${"%.1f".format(presValues.maxOrNull()!!)} hPa",
                                color = presColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualHistoryChart(
    temperaturePoints: List<HistoryPoint>,
    pressurePoints: List<HistoryPoint>
) {
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val tempColor = MaterialTheme.colorScheme.primary
    val presColor = MaterialTheme.colorScheme.tertiary

    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        // Padding inside the canvas for axes
        val leftPadding = 40f
        val rightPadding = 40f
        val topPadding = 16f
        val bottomPadding = 24f

        val width = size.width
        val height = size.height

        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val chartRight = width - rightPadding
        val chartBottom = height - bottomPadding

        // --- Draw axes ---
        drawLine(
            color = axisColor,
            start = androidx.compose.ui.geometry.Offset(leftPadding, topPadding),
            end = androidx.compose.ui.geometry.Offset(leftPadding, chartBottom),
            strokeWidth = 2f
        )
        drawLine(
            color = axisColor,
            start = androidx.compose.ui.geometry.Offset(leftPadding, chartBottom),
            end = androidx.compose.ui.geometry.Offset(chartRight, chartBottom),
            strokeWidth = 2f
        )

        // --- Horizontal grid lines (4) ---
        val gridLines = 4
        for (i in 1 until gridLines) {
            val y = topPadding + chartHeight * (i.toFloat() / gridLines.toFloat())
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(leftPadding, y),
                end = androidx.compose.ui.geometry.Offset(chartRight, y),
                strokeWidth = 1f
            )
        }

        // Helper to compute x coordinate based on index
        fun xForIndex(index: Int, total: Int): Float {
            if (total <= 1) return leftPadding
            val fraction = index.toFloat() / (total - 1).toFloat()
            return leftPadding + fraction * chartWidth
        }

        // --- Temperature series (left Y scale) ---
        val tempValues = temperaturePoints.map { it.value }
        if (tempValues.isNotEmpty()) {
            var tMin = tempValues.minOrNull()!!
            var tMax = tempValues.maxOrNull()!!

            if (tMin == tMax) {
                tMin -= 1.0
                tMax += 1.0
            }

            fun yForTemp(value: Double): Float {
                val fraction = (value - tMin) / (tMax - tMin)
                val clamped = fraction.coerceIn(0.0, 1.0)
                return (chartBottom - (clamped * chartHeight).toFloat())
            }

            val path = Path()
            temperaturePoints.forEachIndexed { index, point ->
                val x = xForIndex(index, temperaturePoints.size)
                val y = yForTemp(point.value)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = tempColor,
                style = Stroke(width = 3f)
            )
        }

        // --- Pressure series (right Y scale) ---
        val presValues = pressurePoints.map { it.value }
        if (presValues.isNotEmpty()) {
            var pMin = presValues.minOrNull()!!
            var pMax = presValues.maxOrNull()!!

            if (pMin == pMax) {
                pMin -= 1.0
                pMax += 1.0
            }

            fun yForPres(value: Double): Float {
                val fraction = (value - pMin) / (pMax - pMin)
                val clamped = fraction.coerceIn(0.0, 1.0)
                return (chartBottom - (clamped * chartHeight).toFloat())
            }

            val path = Path()
            pressurePoints.forEachIndexed { index, point ->
                val x = xForIndex(index, pressurePoints.size)
                val y = yForPres(point.value)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = presColor,
                style = Stroke(width = 2f)
            )

            // Optional: small right-side axis to hint second scale
            drawLine(
                color = axisColor,
                start = androidx.compose.ui.geometry.Offset(chartRight, topPadding),
                end = androidx.compose.ui.geometry.Offset(chartRight, chartBottom),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
private fun LoadingHistoryCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorHistoryCard(
    modifier: Modifier = Modifier,
    message: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Error loading history:\n$message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun RangeSelectorRow(
    selected: HistoryRange,
    onSelect: (HistoryRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryRange.entries.forEach { range ->
            val isSelected = range == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(range) },
                label = { Text(text = range.label) }
            )
        }
    }
}

@Composable
private fun PeriodNavigationRow(
    label: String,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous period"
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        IconButton(
            onClick = onNext,
            enabled = canGoNext
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next period"
            )
        }
    }
}
