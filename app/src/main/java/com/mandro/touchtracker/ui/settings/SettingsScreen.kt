package com.mandro.touchtracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mandro.touchtracker.R
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.ui.components.SectionCard

/**
 * 캡처·표시 설정 화면.
 *
 * 사용자의 요구에 따라 '좌표 단위' 설정만 제공하며, 모눈 간격은 12mm로 고정된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isRecording: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(
                title = stringResource(R.string.settings_coordinate_unit),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CoordinateUnitPicker(
                    selected = settings.coordinateUnit,
                    onSelect = viewModel::setCoordinateUnit,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoordinateUnitPicker(
    selected: CoordinateUnit,
    onSelect: (CoordinateUnit) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (unit in CoordinateUnit.entries) {
            FilterChip(
                selected = unit == selected,
                onClick = { onSelect(unit) },
                label = { Text(unit.displayLabel()) },
            )
        }
    }
}

private fun CoordinateUnit.displayLabel(): String = when (this) {
    CoordinateUnit.PX -> "px"
    CoordinateUnit.MM -> "mm"
    CoordinateUnit.NORMALIZED -> "0–1"
}
