package com.mandro.touchtracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mandro.touchtracker.R
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.ui.components.HintText
import com.mandro.touchtracker.ui.components.SectionCard
import com.mandro.touchtracker.ui.theme.NumericSmallTextStyle

/**
 * 캡처·표시 설정.
 *
 * @param isRecording 기록 중이면 측정 조건을 바꾸는 항목을 잠근다. 세션 중간에
 *        기준이 바뀌면 그 세션의 앞부분과 뒷부분을 비교할 수 없게 된다.
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isRecording) {
                HintText(
                    text = "기록 중에는 측정 기준을 바꿀 수 없습니다",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SectionCard(title = "표시", modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CoordinateUnitPicker(
                        selected = settings.coordinateUnit,
                        onSelect = viewModel::setCoordinateUnit,
                    )
                    GridSpacingSlider(
                        spacingMm = settings.gridSpacingMm,
                        onChange = viewModel::setGridSpacing,
                    )
                    SettingSwitch(
                        label = stringResource(R.string.settings_show_trail),
                        hint = "DOWN → UP 사이 이동 궤적을 모눈종이에 선으로 그립니다",
                        checked = settings.showTrail,
                        onCheckedChange = viewModel::setShowTrail,
                    )
                }
            }

            SectionCard(title = "기록", modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingSwitch(
                        label = stringResource(R.string.settings_record_move),
                        hint = "끄면 DOWN/UP 만 남깁니다. 조준 정확도만 볼 때 권장",
                        checked = settings.recordMoveEvents,
                        enabled = !isRecording,
                        onCheckedChange = viewModel::setRecordMoveEvents,
                    )
                    SettingSwitch(
                        label = stringResource(R.string.settings_ignore_synthetic),
                        hint = "adb 주입·접근성 이벤트를 버려 실측에 섞이지 않게 합니다",
                        checked = settings.ignoreSyntheticInput,
                        enabled = !isRecording,
                        onCheckedChange = viewModel::setIgnoreSyntheticInput,
                    )
                    SettingSwitch(
                        label = stringResource(R.string.settings_keep_screen_on),
                        hint = "로봇 팔 정렬 중 화면이 꺼져 측정이 끊기는 것을 막습니다",
                        checked = settings.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn,
                    )
                }
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
    Column {
        Text(
            text = stringResource(R.string.settings_coordinate_unit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (unit in CoordinateUnit.entries) {
                FilterChip(
                    selected = unit == selected,
                    onClick = { onSelect(unit) },
                    label = { Text(unit.displayLabel()) },
                )
            }
        }
        // 저장되는 원본은 항상 px 다. 이 설정은 화면 표시만 바꾼다.
        HintText("저장·내보내기는 언제나 px 원본을 기준으로 합니다")
    }
}

@Composable
private fun GridSpacingSlider(spacingMm: Float, onChange: (Float) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_grid_spacing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "${spacingMm.toInt()} mm", style = NumericSmallTextStyle)
        }
        Slider(
            value = spacingMm,
            onValueChange = onChange,
            valueRange = CaptureSettings.MIN_GRID_SPACING_MM..CaptureSettings.MAX_GRID_SPACING_MM,
            // 1mm 단위로 끊는다. 눈금이 정수 mm 여야 화면을 보고 거리를 셀 수 있다.
            steps = GRID_SLIDER_STEPS,
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            HintText(hint)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private fun CoordinateUnit.displayLabel(): String = when (this) {
    CoordinateUnit.PX -> "px"
    CoordinateUnit.MM -> "mm"
    CoordinateUnit.NORMALIZED -> "0–1"
}

/** 1mm 간격으로 멈추게 하는 중간 눈금 수 (양 끝 제외). */
private val GRID_SLIDER_STEPS =
    (CaptureSettings.MAX_GRID_SPACING_MM - CaptureSettings.MIN_GRID_SPACING_MM).toInt() - 1
