package com.mandro.touchtracker.ui.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mandro.touchtracker.R
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.ui.components.TrackerSnackbarHost
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
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabs = remember { CaptureTab.entries }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val canvasPageIndex = remember(tabs) { tabs.indexOf(CaptureTab.GRID) }
    val scope = rememberCoroutineScope()
    // 확인창은 한 번에 하나만. 불린을 따로 두면 두 창이 겹쳐 뜰 수 있고,
    // 그때 누른 버튼이 어느 창 것인지 알 수 없어 "시작" 을 눌렀는데 세션이
    // 폐기되는 일이 실제로 일어났다.
    var dialog by remember { mutableStateOf<CaptureDialog?>(null) }



    /**
     * 캡처 **화면 자체**를 벗어나는 길목(세션 목록·설정)은 이걸 통과한다.
     * 기록 중이 아니면 그냥 보내고, 기록 중이면 저장 여부를 먼저 묻는다.
     *
     * 탭 전환은 여기 해당하지 않는다 — 같은 화면 안에서 보는 방식만 바꾸는 것이다.
     */
    fun leaveCapture(action: () -> Unit) {
        if (state.isRecording) dialog = CaptureDialog.LeaveWhileRecording(action) else action()
    }

    LaunchedEffect(Unit) {
        viewModel.setDeviceProfile(deviceProfile())
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    // 뒤로가기로 앱을 벗어나려 할 때. 기록 중이면 먼저 물어본다 — 그냥 나가면
    // 세션이 endedAt 없이 열린 채로 남는다.
    BackHandler(enabled = state.isRecording) { dialog = CaptureDialog.StopRecording }

    // 홈 버튼·최근 앱은 OS 가 가로채므로 그 순간에는 창을 띄울 수 없다(안드로이드 제약).
    // 대신 기록 중에 앱을 벗어났다는 사실을 기억해 뒀다가, 돌아왔을 때 물어본다.
    var leftWhileRecording by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (state.isRecording) leftWhileRecording = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (leftWhileRecording && state.isRecording) dialog = CaptureDialog.StopRecording
        leftWhileRecording = false
    }

    val isGridTab = tabs[pagerState.currentPage] == CaptureTab.GRID
    DisposableEffect(isGridTab) {
        viewModel.setCaptureActive(isGridTab)
        onDispose {
            viewModel.setCaptureActive(false)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { TrackerSnackbarHost(snackbarHostState) },
        topBar = {
            CaptureTopBar(
                state = state,
                onToggleRecording = {
                    // 시작도 정지도 확인을 거친다. 정지는 곧 세션 확정이라
                    // 실수로 눌렀을 때 되돌릴 방법이 없다.
                    if (state.isRecording) {
                        dialog = CaptureDialog.StopRecording
                    } else {
                        dialog = CaptureDialog.StartRecording
                    }
                },
                onClearPoints = { dialog = CaptureDialog.ClearPoints },
                onSelectUnit = viewModel::setCoordinateUnit,
                onOpenSessions = { leaveCapture(onOpenSessions) },
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            val trackerColors = LocalTrackerColors.current
            // 구분선을 TabRow 의 자식으로 넣으면 안 된다 — TabRow 는 자식들에게 폭을
            // 똑같이 나눠 주므로 선이 탭 하나만큼 자리를 차지해 버린다. 위에 겹쳐 그린다.
            Box {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = trackerColors.tabBar,
                contentColor = trackerColors.onTabBar,
                // 밑줄 표시선은 두지 않는다. 선택 여부는 글자 밝기와 굵기로 읽고,
                // 두 탭의 경계는 아래의 세로 구분선이 알려 준다.
                indicator = {},
                divider = {},
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        // 선택된 탭만 또렷하게. 색 대비 + 굵기 두 가지로 표시해서
                        // 밝은 조명 아래에서도 어느 탭인지 헷갈리지 않게 한다.
                        selectedContentColor = trackerColors.onTabBar,
                        unselectedContentColor = trackerColors.onTabBar.copy(alpha = TAB_INACTIVE_ALPHA),
                        // 탭 전환은 묻지 않는다. 두 탭은 같은 세션을 다르게 볼 뿐이고,
                        // 데이터 탭에서 값을 확인하고 모눈종이로 돌아와 계속 재는 게
                        // 정상 흐름이라 매번 확인창이 뜨면 방해만 된다.
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
            // 탭이 둘이라 가운데가 곧 경계다. 탭이 늘어나면 위치 계산이 필요하다.
            VerticalDivider(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(TAB_DIVIDER_HEIGHT_DP.dp),
                color = trackerColors.onTabBar.copy(alpha = TAB_DIVIDER_ALPHA),
            )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
            ) { page ->
                when (tabs[page]) {
                    CaptureTab.DATA -> TouchDataTab(
                        state = state,
                        onGoToCanvas = {
                            scope.launch { pagerState.animateScrollToPage(canvasPageIndex) }
                        },
                    )
                    CaptureTab.GRID -> GraphPaperTab(
                        state = state,
                        onCanvasBoundsChanged = { bounds -> viewModel.setCaptureBounds(bounds) },
                    )
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit

        is CaptureDialog.LeaveWhileRecording -> ConfirmDialog(
            title = stringResource(R.string.leave_dialog_title),
            message = stringResource(R.string.leave_dialog_message),
            confirmLabel = stringResource(R.string.leave_dialog_save),
            onConfirm = {
                dialog = null
                viewModel.stopRecordingThen(current.onLeave)
            },
            onDismiss = { dialog = null },
            discardLabel = stringResource(R.string.leave_dialog_discard),
            onDiscard = {
                dialog = null
                viewModel.discardRecordingThen(current.onLeave)
            },
        )

        CaptureDialog.StartRecording -> ConfirmDialog(
            title = stringResource(R.string.record_dialog_title),
            message = stringResource(R.string.record_dialog_message),
            confirmLabel = stringResource(R.string.record_dialog_confirm),
            onConfirm = {
                dialog = null
                viewModel.toggleRecording()
                // 터치는 모눈종이 탭에서만 잡힌다. 데이터 탭에 머문 채로 녹화를
                // 켜면 아무것도 안 찍히므로 캔버스로 데려간다.
                scope.launch { pagerState.animateScrollToPage(canvasPageIndex) }
            },
            cancelLabel = stringResource(R.string.common_cancel),
            onDismiss = { dialog = null },
        )

        CaptureDialog.StopRecording -> ConfirmDialog(
            title = stringResource(R.string.stop_dialog_title),
            message = stringResource(R.string.stop_dialog_message),
            confirmLabel = stringResource(R.string.stop_dialog_save),
            onConfirm = {
                dialog = null
                viewModel.toggleRecording()
            },
            onDismiss = { dialog = null },
            discardLabel = stringResource(R.string.stop_dialog_discard),
            onDiscard = {
                dialog = null
                viewModel.discardCurrentSession()
            },
        )

        CaptureDialog.ClearPoints -> ConfirmDialog(
            title = stringResource(R.string.clear_dialog_title),
            message = stringResource(R.string.clear_dialog_message),
            confirmLabel = stringResource(R.string.clear_dialog_save),
            onConfirm = {
                dialog = null
                viewModel.saveAndClearLivePoints()
            },
            onDismiss = { dialog = null },
            discardLabel = stringResource(R.string.clear_dialog_clear_only),
            onDiscard = {
                dialog = null
                viewModel.discardCurrentSession()
            },
        )
    }
}

/** 캡처 화면이 띄우는 확인창. 한 번에 하나만 뜬다. */
private sealed interface CaptureDialog {
    data object StartRecording : CaptureDialog
    data object StopRecording : CaptureDialog
    data object ClearPoints : CaptureDialog

    /** 기록 중에 화면을 떠나려 할 때. 고르고 나면 [onLeave] 가 실행된다. */
    data class LeaveWhileRecording(val onLeave: () -> Unit) : CaptureDialog
}

/**
 * 확인창 한 틀. 앱 안의 모든 확인창이 이걸 쓴다 — 버튼 순서가 창마다 다르면
 * 손이 기억하는 위치가 어긋나 실수로 데이터를 날리게 된다.
 *
 * 배치는 언제나 **왼쪽이 버리기 · 오른쪽이 저장**이다. Material 의 confirm/dismiss
 * 슬롯에 나눠 담으면 테마에 따라 좌우가 뒤집힐 수 있어 한 슬롯에 Row 로 세운다.
 */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    discardLabel: String? = null,
    onDiscard: (() -> Unit)? = null,
    cancelLabel: String? = null,
) {
    AlertDialog(
        // 바깥을 누르거나 뒤로 가면 아무 일도 없이 닫힌다 — 버튼을 누르기 직전
        // 상태로 돌아간다. 저장도 폐기도 하지 않는다.
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Row {
                if (discardLabel != null && onDiscard != null) {
                    TextButton(onClick = onDiscard) {
                        Text(text = discardLabel, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (cancelLabel != null) {
                    TextButton(onClick = onDismiss) { Text(text = cancelLabel) }
                }
                TextButton(onClick = onConfirm) {
                    Text(text = confirmLabel, fontWeight = FontWeight.Bold)
                }
            }
        },
    )
}

/**
 * 녹화 중 표시등 겸 정지 버튼.
 *
 * 천천히 깜빡인다. 로봇 팔을 맞추느라 화면에서 눈을 뗐다 돌아왔을 때, 지금 기록이
 * 돌고 있는지 한눈에 들어와야 하기 때문이다. 누르면 정지 확인창으로 간다.
 */
@Composable
private fun RecordingIndicatorButton(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "recording-pulse")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = RECORD_PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(RECORD_PULSE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recording-alpha",
    )

    IconButton(onClick = onClick) {
        // 메뉴의 "녹화 시작" 과 같은 캠코더를 쓴다. 돌아가는 중이라는 뜻은 깜빡임이
        // 맡고, 아이콘은 "지금 녹화 기능이 켜져 있다" 를 말한다.
        // 색은 본문과 같은 먹색. 빨강은 상단바에서 혼자 튀어서, 측정 화면을 보는
        // 내내 시선을 가져간다 — 상태는 깜빡임만으로 충분히 읽힌다.
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = stringResource(R.string.capture_stop),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
    }
}

private const val RECORD_PULSE_MS = 900
private const val RECORD_PULSE_MIN_ALPHA = 0.3f

/**
 * 상단바의 목록 아이콘과, 눌렀을 때 캔버스 위로 내려오는 메뉴.
 *
 * 세션 목록·설정 아이콘을 따로 늘어놓는 대신 하나로 모았다. 측정 중에는 화면이
 * 곧 측정면이라 상단에 아이콘이 많을수록 잘못 눌릴 여지가 커진다.
 */
@Composable
private fun CaptureMenuButton(
    isRecording: Boolean,
    coordinateUnit: CoordinateUnit,
    onToggleRecording: () -> Unit,
    onSelectUnit: (CoordinateUnit) -> Unit,
    onOpenSessions: () -> Unit,
) {
    val colors = LocalTrackerColors.current
    var expanded by remember { mutableStateOf(false) }

    // 메뉴를 먼저 닫고 나서 동작을 실행한다. 화면 이동이 섞이면 닫히는 애니메이션이
    // 끊겨 잔상이 남는다.
    fun choose(action: () -> Unit) {
        expanded = false
        action()
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                // 글머리 점 없는 줄 세 개(햄버거). FormatListBulleted 는 줄마다 점이 붙는다.
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu_open),
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sessions_title)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                onClick = { choose(onOpenSessions) },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isRecording) R.string.capture_stop else R.string.capture_start,
                        ),
                    )
                },
                leadingIcon = {
                    // 캠코더 = 찍기 시작, 정지 = 끝내기. 상단바의 점+REC 는 상태 표시등이고
                    // 여기 아이콘은 "누르면 무슨 일이 일어나는가" 를 말한다.
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = { choose(onToggleRecording) },
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.menu_unit_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
            )
            for (unit in CoordinateUnit.entries) {
                DropdownMenuItem(
                    text = { Text(unit.menuLabel()) },
                    leadingIcon = {
                        // 고르지 않은 항목도 같은 폭을 차지해야 글자가 좌우로 안 흔들린다.
                        if (unit == coordinateUnit) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        } else {
                            Spacer(Modifier.size(MENU_ICON_SLOT_DP.dp))
                        }
                    },
                    onClick = { choose { onSelectUnit(unit) } },
                )
            }

        }
    }
}

/** 설정 화면의 칩 라벨과 같은 표기. 두 화면이 다른 말을 하면 안 된다. */
private fun CoordinateUnit.menuLabel(): String = when (this) {
    CoordinateUnit.PX -> "px"
    CoordinateUnit.MM -> "mm"
    CoordinateUnit.NORMALIZED -> "0–1 정규화"
}

private const val MENU_ICON_SLOT_DP = 24


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CaptureTopBar(
    state: CaptureUiState,
    onToggleRecording: () -> Unit,
    onClearPoints: () -> Unit,
    onSelectUnit: (CoordinateUnit) -> Unit,
    onOpenSessions: () -> Unit,
) {
    val colors = LocalTrackerColors.current

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            // 같은 자리를 상태에 따라 나눠 쓴다.
            //
            // 녹화 버튼으로 시작했을 때만 표시등이자 정지 버튼이 된다. 그냥 화면을
            // 눌러 자동으로 세션이 열린 경우에는 초기화 버튼을 그대로 둔다 —
            // 사람이 녹화를 켠 적이 없는데 "녹화 중" 이라고 하면 앞뒤가 안 맞는다.
            if (state.isExplicitRecording) {
                RecordingIndicatorButton(onClick = onToggleRecording)
            } else {
                IconButton(
                    onClick = onClearPoints,
                    // 지울 게 없으면 비활성. 빈 화면에서 눌러 봐야 아무 일도 안 일어난다.
                    enabled = state.hasPoints,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.capture_clear),
                    )
                }
            }
            CaptureMenuButton(
                isRecording = state.isRecording,
                coordinateUnit = state.settings.coordinateUnit,
                onToggleRecording = onToggleRecording,
                onSelectUnit = onSelectUnit,
                onOpenSessions = onOpenSessions,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

private const val TAB_INACTIVE_ALPHA = 0.45f
private const val TAB_DIVIDER_HEIGHT_DP = 22
private const val TAB_DIVIDER_ALPHA = 0.3f
