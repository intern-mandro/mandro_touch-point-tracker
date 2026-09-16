package com.mandro.touchtracker.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mandro.touchtracker.R
import com.mandro.touchtracker.model.ExportFormat
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.ui.components.HintText
import com.mandro.touchtracker.ui.components.SectionCard
import com.mandro.touchtracker.ui.components.TrackerSnackbarHost
import com.mandro.touchtracker.ui.theme.NumericSmallTextStyle
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 세션 하나의 요약과 내보내기.
 *
 * 내보내기는 SAF 의 CreateDocument 로 사용자가 위치를 고르게 한다 — 저장소 권한이
 * 필요 없고, 실험 데이터를 바로 공유 폴더/USB 로 떨굴 수 있다.
 *
 * TODO 이 화면에도 모눈종이 뷰(전체 점) 붙이기. 지금은 요약 수치만 보여준다.
 * TODO 조준 정확도 통계(평균 중심, 표준편차, CEP) — 목표 좌표 입력 UI 가 생긴 뒤에.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onResumeSession: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType),
    ) { uri -> uri?.let { viewModel.export(ExportFormat.CSV, it) } }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    fun startExport() {
        scope.launch {
            val fileName = viewModel.suggestFileName(ExportFormat.CSV)
            createCsv.launch(fileName)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { TrackerSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.session?.name ?: stringResource(R.string.session_detail_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val session = state.session
            if (session == null) {
                HintText("세션을 불러오는 중입니다")
                return@Column
            }

            SectionCard(title = "측정 조건", modifier = Modifier.fillMaxWidth()) {
                MeasurementConditions(session = session, pointCount = state.points.size)
            }
            // 동작은 버튼 두 개가 전부다. 설명 문구와 카드 껍데기를 걷어내
            // 화면에서 읽을 것은 위의 측정 조건만 남긴다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.resumeSession(onResumeSession) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.session_resume_short))
                }

                Button(
                    onClick = { startExport() },
                    enabled = !state.isExporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.export_csv))
                }
            }
        }
    }
}

@Composable
private fun MeasurementConditions(session: TouchSession, pointCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConditionSummary("기록된 점", "$pointCount", "points", Modifier.weight(1f))
            ConditionSummary(
                "화면 해상도",
                "${session.metrics.widthPx} × ${session.metrics.heightPx}",
                "px",
                Modifier.weight(1f),
            )
        }

        if (session.note.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("실험 조건 메모", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        text = session.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }

        Column {
            ConditionRow("기기", "${session.device.manufacturer} ${session.device.model}")
            ConditionDivider()
            ConditionRow("Android", "API ${session.device.androidSdk}")
            ConditionDivider()
            ConditionRow(
                "물리 DPI",
                if (session.metrics.hasPhysicalDpi) {
                    "X ${formatDpi(session.metrics.xDpi)}  ·  Y ${formatDpi(session.metrics.yDpi)}"
                } else {
                    "확인되지 않음"
                },
            )
        }

        if (!session.metrics.hasPhysicalDpi) {
            Text(
                text = "물리 DPI를 확인할 수 없어 mm 단위 환산을 사용할 수 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ConditionSummary(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConditionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, style = NumericSmallTextStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun ConditionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
}

private fun formatDpi(value: Float): String = String.format(Locale.US, "%.1f", value)
