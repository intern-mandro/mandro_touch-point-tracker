package com.mandro.touchtracker.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.ui.components.HintText
import com.mandro.touchtracker.ui.theme.NumericSmallTextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 저장된 세션 목록. 캡처 화면에서 상단 액션으로 들어온다.
 *
 * TODO 세션 이름·메모 편집, 날짜 필터. 지금은 목록·열기·삭제만 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sessions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoaded && state.sessions.isEmpty()) {
            // 다른 탭의 빈 상태와 같은 위치·같은 서체로 둔다. 목록이 비었다는 사실은
            // 화면 가운데에서 읽히는 게 맞다.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                HintText(text = stringResource(R.string.session_empty))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.sessions, key = { it.id }) { session ->
                SessionRow(
                    session = session,
                    onOpen = { onOpenSession(session.id) },
                    onDelete = { viewModel.deleteSession(session.id) },
                )
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: TouchSession,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = session.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = buildString {
                    append(formatTimestamp(session.startedAtEpochMs))
                    append(" · ")
                    append("${session.pointCount}점")
                    append(" · ")
                    append("${session.metrics.widthPx}×${session.metrics.heightPx}")
                    if (session.isRecording) append(" · 기록 중")
                },
                style = NumericSmallTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            // TODO 삭제 전 확인 다이얼로그. 실험 데이터는 되돌릴 수 없다.
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.session_delete))
        }
    }
}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).format(Date(epochMs))

private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm"
