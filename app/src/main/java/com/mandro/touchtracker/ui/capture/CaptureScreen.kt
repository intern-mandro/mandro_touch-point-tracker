package com.mandro.touchtracker.ui.capture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mandro.touchtracker.R
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.ui.components.StatusDot
import com.mandro.touchtracker.ui.navigation.CaptureTab
import com.mandro.touchtracker.ui.theme.LocalTrackerColors
import kotlinx.coroutines.launch

/**
 * 메인 화면. 탭 2개가 **같은 [CaptureUiState]** 를 다르게 보여준다.
 *
 * 탭을 옮겨도 기록은 끊기지 않는다 — 터치 수집은 Activity 의 dispatchTouchEvent 에
 * 달려 있고 이 화면은 결과만 그린다. 세션 목록·설정은 탭이 아니라 상단 액션으로 뺐다.
 * 측정 중 화면을 탭 두 개로 유지하는 게 이 앱의 핵심 제약이기 때문이다.
 */
@Composable
fun CaptureScreen(
    deviceProfile: () -> DeviceProfile,
    onOpenSessions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabs = remember { CaptureTab.entries }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CaptureTopBar(
                state = state,
                onToggleRecording = { viewModel.toggleRecording(deviceProfile) },
                onClear = viewModel::clearLivePoints,
                onOpenSessions = onOpenSessions,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (tabs[page]) {
                    CaptureTab.DATA -> TouchDataTab(state)
                    CaptureTab.GRID -> GraphPaperTab(state)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CaptureTopBar(
    state: CaptureUiState,
    onToggleRecording: () -> Unit,
    onClear: () -> Unit,
    onOpenSessions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalTrackerColors.current

    TopAppBar(
        title = {
            StatusDot(
                active = state.isRecording,
                label = if (state.isRecording) {
                    "${stringResource(R.string.capture_recording)} · ${state.sessionName}"
                } else {
                    stringResource(R.string.capture_idle)
                },
            )
        },
        actions = {
            IconButton(onClick = onToggleRecording) {
                Icon(
                    imageVector = if (state.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = stringResource(
                        if (state.isRecording) R.string.capture_stop else R.string.capture_start,
                    ),
                    tint = if (state.isRecording) colors.alert else colors.live,
                )
            }
            IconButton(onClick = onClear, enabled = state.hasPoints) {
                Icon(Icons.Default.DeleteSweep, stringResource(R.string.capture_clear))
            }
            IconButton(onClick = onOpenSessions) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, stringResource(R.string.sessions_title))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
