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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.oulu.picow.sensormonitor.data.HistoryPoint
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onBack: (() -> Unit)? = null
) {
    val currentRange = viewModel.selectedRange
    val periodLabel = viewModel.getCurrentPeriodLabel()
    val uiState = viewModel.uiState
    // Auto-refresh only when looking at the current period (offset = 0)
    LaunchedEffect(currentRange, viewModel.periodOffset) {
        if (viewModel.periodOffset == 0) {
            while (true) {
                delay(60_000L)   // 60 seconds – adjust as needed
                viewModel.refreshHistory()
            }
        }
        // When offset != 0 (yesterday/last week/etc.), this effect does nothing
        // and any previous loop is cancelled when LaunchedEffect keys change.
    }
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
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = androidx.compose.ui.platform.LocalDensity.current

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- Layout constants (in px) ---
        val leftPadding = 48f
        val rightPadding = 48f
        val topPadding = 16f
        val bottomPadding = 32f

        val width = size.width
        val height = size.height

        val chartRight = width - rightPadding
        val chartBottom = height - bottomPadding

        val chartWidth = chartRight - leftPadding
        val chartHeight = chartBottom - topPadding

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        // --- Time handling: map timestamps -> X coordinate ---
        fun parseTimeMillis(p: HistoryPoint): Long? =
            kotlin.runCatching { java.time.Instant.parse(p.time).toEpochMilli() }.getOrNull()

        val tempTimes = temperaturePoints.mapNotNull(::parseTimeMillis)
        val presTimes = pressurePoints.mapNotNull(::parseTimeMillis)
        val allTimes = tempTimes + presTimes

        if (allTimes.isEmpty()) return@Canvas

        var minTime = allTimes.minOrNull()!!
        var maxTime = allTimes.maxOrNull()!!

        if (minTime == maxTime) {
            // Expand 1 minute around single point so we get a visible line
            minTime -= 60_000L
            maxTime += 60_000L
        }

        fun xForTime(tMillis: Long): Float {
            val fraction = (tMillis - minTime).toFloat() /
                    (maxTime - minTime).toFloat()
            return leftPadding + fraction * chartWidth
        }

        // --- Draw axes ---
        drawLine(
            color = axisColor,
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, chartBottom),
            strokeWidth = 2f
        )
        drawLine(
            color = axisColor,
            start = Offset(leftPadding, chartBottom),
            end = Offset(chartRight, chartBottom),
            strokeWidth = 2f
        )

        // --- Horizontal grid lines (4) ---
        val gridLines = 4
        for (i in 1 until gridLines) {
            val y = topPadding + chartHeight * (i.toFloat() / gridLines.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(chartRight, y),
                strokeWidth = 1f
            )
        }

        // --- Temperature Y scale (left) ---
        val tempValues = temperaturePoints.map { it.value }
        var tMin = 0.0
        var tMax = 0.0
        if (tempValues.isNotEmpty()) {
            tMin = tempValues.minOrNull()!!
            tMax = tempValues.maxOrNull()!!

            if (tMin == tMax) {
                tMin -= 1.0
                tMax += 1.0
            }
        }

        fun yForTemp(value: Double): Float {
            val fraction = if (tMax == tMin) 0.5
            else (value - tMin) / (tMax - tMin)
            val clamped = fraction.coerceIn(0.0, 1.0)
            return (chartBottom - (clamped * chartHeight).toFloat())
        }

        // --- Pressure Y scale (right) ---
        val presValues = pressurePoints.map { it.value }
        var pMin = 0.0
        var pMax = 0.0
        if (presValues.isNotEmpty()) {
            pMin = presValues.minOrNull()!!
            pMax = presValues.maxOrNull()!!

            if (pMin == pMax) {
                pMin -= 1.0
                pMax += 1.0
            }
        }

        fun yForPres(value: Double): Float {
            val fraction = if (pMax == pMin) 0.5
            else (value - pMin) / (pMax - pMin)
            val clamped = fraction.coerceIn(0.0, 1.0)
            return (chartBottom - (clamped * chartHeight).toFloat())
        }

        // --- Draw temperature series (left scale) ---
        if (tempValues.isNotEmpty()) {
            val path = Path()
            var started = false
            temperaturePoints.forEach { point ->
                val tMillis = parseTimeMillis(point) ?: return@forEach
                val x = xForTime(tMillis)
                val y = yForTemp(point.value)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
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

        // --- Draw pressure series (right scale) ---
        if (presValues.isNotEmpty()) {
            val path = Path()
            var started = false
            pressurePoints.forEach { point ->
                val tMillis = parseTimeMillis(point) ?: return@forEach
                val x = xForTime(tMillis)
                val y = yForPres(point.value)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = presColor,
                style = Stroke(width = 2f)
            )

            // Right-side axis line for pressure
            drawLine(
                color = axisColor,
                start = Offset(chartRight, topPadding),
                end = Offset(chartRight, chartBottom),
                strokeWidth = 1.5f
            )
        }

        // --- Axis labels (temp left, pressure right, time bottom) ---
        val stepsY = 4

        val textSizePx = with(density) { 10.sp.toPx() }
        val labelPadding = with(density) { 4.dp.toPx() }

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = textColor.toArgb()
            textSize = textSizePx
        }

        // Left Y labels (temperature)
        if (tempValues.isNotEmpty()) {
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            for (i in 0..stepsY) {
                val fraction = i.toFloat() / stepsY.toFloat()
                val value = tMax - (tMax - tMin) * fraction
                val y = topPadding + chartHeight * fraction
                val label = String.format(Locale.US, "%.1f", value)
                val x = leftPadding - labelPadding
                drawContext.canvas.nativeCanvas.drawText(label, x, y + textSizePx / 2f, paint)
            }
        }

        // Right Y labels (pressure)
        if (presValues.isNotEmpty()) {
            paint.textAlign = android.graphics.Paint.Align.LEFT
            for (i in 0..stepsY) {
                val fraction = i.toFloat() / stepsY.toFloat()
                val value = pMax - (pMax - pMin) * fraction
                val y = topPadding + chartHeight * fraction
                val label = String.format(Locale.US, "%.1f", value)
                val x = chartRight + labelPadding
                drawContext.canvas.nativeCanvas.drawText(label, x, y + textSizePx / 2f, paint)
            }
        }

        // Bottom X labels (time)
        val timeTicks = 3 // min, middle, max
        paint.textAlign = android.graphics.Paint.Align.CENTER

        fun formatTime(millis: Long): String {
            val instant = java.time.Instant.ofEpochMilli(millis)
            val localTime = instant
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()
            return String.format(
                Locale.US,
                "%02d:%02d",
                localTime.hour,
                localTime.minute
            )
        }

        for (i in 0..timeTicks) {
            val fraction = i.toFloat() / timeTicks.toFloat()
            val t = (minTime + ((maxTime - minTime) * fraction)).toLong()
            val x = xForTime(t)
            val y = chartBottom + textSizePx + labelPadding / 2f
            val label = formatTime(t)
            drawContext.canvas.nativeCanvas.drawText(label, x, y, paint)
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
