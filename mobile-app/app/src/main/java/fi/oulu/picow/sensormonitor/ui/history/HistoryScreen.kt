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

    //  👇 NEW: read history loading state
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
            // Range: 24h / Week / Month / Year
            RangeSelectorRow(
                selected = currentRange,
                onSelect = { range -> viewModel.selectRange(range) }
            )

            // Period navigation: arrows + label
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
                            .weight(1f),
                        selectedRange = currentRange,
                        periodLabel = periodLabel
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
                    SuccessHistoryCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        selectedRange = currentRange,
                        periodLabel = periodLabel,
                        points = uiState.points
                    )
                }
            }

        }
    }
}

@Composable
private fun LoadingHistoryCard(
    modifier: Modifier = Modifier,
    selectedRange: HistoryRange,
    periodLabel: String
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

@Composable
private fun SuccessHistoryCard(
    modifier: Modifier = Modifier,
    selectedRange: HistoryRange,
    periodLabel: String,
    points: List<HistoryPoint>
) {
    Card(
        modifier = modifier,    // <-- no weight here
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Temperature history",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${selectedRange.label} · $periodLabel",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            //  👇 TEMPORARY visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Points: ${points.size}\n(first value = ${points.firstOrNull()?.value ?: "-"} )",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Real chart coming next…",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
