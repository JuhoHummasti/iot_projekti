package fi.oulu.picow.sensormonitor.ui.history

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is HistoryUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is HistoryUiState.Success -> {
                    HistoryChartDataCard(
                        points = uiState.points,
                        selectedRange = currentRange,
                        periodLabel = periodLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}
@Composable
private fun HistoryLineChart(
    points: List<HistoryPoint>,
    modifier: Modifier = Modifier
) {
    val values = points.map { it.value.toFloat() }
    if (values.isEmpty()) return

    val yMinRaw = values.minOrNull()!!
    val yMaxRaw = values.maxOrNull()!!
    val sameValue = yMaxRaw == yMinRaw

    val (yMin, yMax) = if (sameValue) {
        val v = yMinRaw
        v - 0.5f to v + 0.5f
    } else {
        yMinRaw to yMaxRaw
    }
    val yRange = yMax - yMin

    val density = LocalDensity.current

    // ✅ All MaterialTheme usage happens here (in composable scope)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val axisColor = MaterialTheme.colorScheme.outline
    val strokeColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // X labels: first, middle, last
    val xLabels = remember(points) {
        val size = points.size
        if (size == 1) {
            listOf(points[0].time, "", "")
        } else {
            listOf(points.first().time, points[size / 2].time, points.last().time)
        }
    }

    fun formatTime(raw: String): String {
        return try {
            val instant = java.time.Instant.parse(raw)
            val ldt = java.time.LocalDateTime.ofInstant(
                instant,
                java.time.ZoneId.systemDefault()
            )
            java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(ldt)
        } catch (_: Exception) {
            if (raw.length >= 5) raw.takeLast(5) else raw
        }
    }

    // Y-axis tick labels (4 ticks)
    val yTicks = remember(yMin, yMax) {
        val tickCount = 4
        val step = yRange / (tickCount - 1)
        (0 until tickCount).map { i ->
            val v = yMin + step * i
            String.format(java.util.Locale.getDefault(), "%.1f", v)
        }
    }

    Canvas(
        modifier = modifier
            .background(
                color = surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val leftPadding = with(density) { 40.dp.toPx() }
        val rightPadding = with(density) { 16.dp.toPx() }
        val topPadding = with(density) { 16.dp.toPx() }
        val bottomPadding = with(density) { 32.dp.toPx() }

        val chartLeft = leftPadding
        val chartRight = canvasWidth - rightPadding
        val chartTop = topPadding
        val chartBottom = canvasHeight - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        // ----- Y axis + grid -----
        val yStepPx = chartHeight / (yTicks.size - 1)

        yTicks.forEachIndexed { index, label ->
            val y = chartBottom - yStepPx * index

            // grid line
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx()
            )

            // label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = textColor.toArgb()
                    textSize = with(density) { 12.sp.toPx() }
                }
                val textWidth = paint.measureText(label)
                drawText(
                    label,
                    chartLeft - 8.dp.toPx() - textWidth,
                    y + (paint.textSize / 3),
                    paint
                )
            }
        }

        // Y axis line
        drawLine(
            color = axisColor,
            start = Offset(chartLeft, chartTop),
            end = Offset(chartLeft, chartBottom),
            strokeWidth = 1.5.dp.toPx()
        )

        // ----- X axis -----
        drawLine(
            color = axisColor,
            start = Offset(chartLeft, chartBottom),
            end = Offset(chartRight, chartBottom),
            strokeWidth = 1.5.dp.toPx()
        )

        val xLabelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = textColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
        }

        val formattedX = xLabels.map(::formatTime)
        val positions = listOf(chartLeft, chartLeft + chartWidth / 2f, chartRight)

        formattedX.forEachIndexed { i, label ->
            if (label.isBlank()) return@forEachIndexed
            val x = positions[i]
            val textWidth = xLabelPaint.measureText(label)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x - textWidth / 2f,
                canvasHeight - 8.dp.toPx(),
                xLabelPaint
            )
        }

        // ----- Data line + points -----
        if (points.size == 1) {
            val v = values.first()
            val ratio = (v - yMin) / yRange
            val y = chartBottom - ratio * chartHeight
            val x = chartLeft + chartWidth / 2f

            drawCircle(
                color = strokeColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        } else {
            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

            val linePath = Path()
            points.forEachIndexed { index, p ->
                val v = p.value.toFloat()
                val ratio = (v - yMin) / yRange
                val x = chartLeft + stepX * index
                val y = chartBottom - ratio * chartHeight

                if (index == 0) {
                    linePath.moveTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                }
            }

            drawPath(
                path = linePath,
                color = strokeColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { index, p ->
                val v = p.value.toFloat()
                val ratio = (v - yMin) / yRange
                val x = chartLeft + stepX * index
                val y = chartBottom - ratio * chartHeight
                drawCircle(
                    color = strokeColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun HistoryChartDataCard(
    points: List<HistoryPoint>,
    selectedRange: HistoryRange,
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Temperature history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${selectedRange.label} · $periodLabel",
                style = MaterialTheme.typography.bodyMedium
            )

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data in this period")
                }
            } else {
                HistoryLineChart(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
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
