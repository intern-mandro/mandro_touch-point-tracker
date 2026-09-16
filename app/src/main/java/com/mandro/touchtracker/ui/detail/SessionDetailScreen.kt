package com.mandro.touchtracker.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.mandro.touchtracker.ui.components.HintText
import com.mandro.touchtracker.ui.components.SectionCard
import com.mandro.touchtracker.ui.theme.NumericSmallTextStyle
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 형식마다 런처를 따로 둔다. CreateDocument 의 MIME 타입은 런처를 만들 때 굳으므로,
    // 하나를 돌려 쓰면 방금 바꾼 형식이 아니라 직전 형식으로 파일이 만들어진다.
    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType),
    ) { uri -> uri?.let { viewModel.export(ExportFormat.CSV, it) } }

    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.JSON.mimeType),
    ) { uri -> uri?.let { viewModel.export(ExportFormat.JSON, it) } }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    fun startExport(format: ExportFormat) {
        scope.launch {
            val fileName = viewModel.suggestFileName(format)
            when (format) {
                ExportFormat.CSV -> createCsv.launch(fileName)
                ExportFormat.JSON -> createJson.launch(fileName)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DetailLine("기기", "${session.device.manufacturer} ${session.device.model}")
                    DetailLine("화면", "${session.metrics.widthPx} × ${session.metrics.heightPx} px")
                    DetailLine(
                        label = "DPI",
                        value = if (session.metrics.hasPhysicalDpi) {
                            "x ${session.metrics.xDpi} / y ${session.metrics.yDpi}"
                        } else {
                            "신뢰 불가 — mm 환산 사용 금지"
                        },
                    )
                    DetailLine("기록된 점", "${state.points.size}")
                }
            }

            SectionCard(title = "내보내기", modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { startExport(ExportFormat.CSV) },
                        enabled = !state.isExporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.export_csv)) }

                    Button(
                        onClick = { startExport(ExportFormat.JSON) },
                        enabled = !state.isExporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.export_json)) }

                    HintText("CSV 는 분석용(px·mm·정규화 좌표 모두 포함), JSON 은 기기 메타까지 담습니다")
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = NumericSmallTextStyle, color = MaterialTheme.colorScheme.onSurface)
    }
}
