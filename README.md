# Tourch Tracker (mandro touch-point-tracker)

로봇 손이 스마트폰 화면을 터치했을 때 **그 좌표를 실시간으로 보여주고 기록하는 사내 실험용 앱**.

실험 대상 폰에 이 앱을 띄워 두고 로봇 손으로 화면을 누르면, 앱이 그 접촉을
`MotionEvent` 로 직접 받아 좌표·압력·접촉 면적을 남긴다. 외부 통신은 쓰지 않는다.

> `mandro-Mark7-Android_BLE_Controller` 와는 **완전히 다른 앱**이다. 빌드 구성과
> 코드 규약(Compose · Hilt · 버전 카탈로그 · 한국어 주석)만 그쪽을 따랐다.

---

## 화면

메인 화면은 탭 2개다. 둘은 **같은 데이터를 다르게 보여줄 뿐**이며, 탭을 옮겨도
기록은 끊기지 않는다.

| 탭 | 내용 |
|---|---|
| **데이터** | 방금 찍힌 좌표를 크게 + 최근 **9개**를 등폭 숫자 표로 |
| **모눈종이** | 화면 전체를 비율 그대로 축소한 격자 위에 점·궤적·십자선으로 |

세션 목록과 설정은 탭이 아니라 상단 액션에 있다. 측정 중 화면을 탭 두 개로
유지하는 게 이 앱의 핵심 제약이기 때문이다.

---

## 아키텍처: 단일 모듈 + 얇은 레이어드

참고 앱(Mark7)은 BLE 프로토콜 코덱·프레임 재조립·영속화가 얽혀 있어 Clean
Architecture 가 값을 했다. 이 앱의 도메인은 "세션을 열고 점을 쌓고 닫는다" 가
전부라서, **UseCase 레이어를 두지 않았다.** 한 줄짜리 UseCase 클래스를 늘리는 건
배선만 늘린다. ViewModel 이 Repository 인터페이스를 직접 쓴다.

```
app/src/main/java/com/mandro/touchtracker/
├── MainActivity.kt              ★ 터치 캡처 지점 (dispatchTouchEvent)
├── TouchTrackerApplication.kt   @HiltAndroidApp
│
├── core/                        안드로이드 의존이 얇은 값 타입·유틸
│   ├── geometry/ScreenMetrics   px ↔ mm ↔ 정규화 좌표 변환
│   ├── time/Clock               epoch(절대) · uptime(단조) 두 시계
│   └── AppResult                사용자에게 보여줄 성공/실패
│
├── domain/
│   ├── model/                   TouchPoint · TouchSession · CaptureSettings · ExportFormat
│   └── repository/              TouchSessionRepository · SettingsRepository · SessionExporter
│
├── data/
│   ├── touch/
│   │   ├── TouchEventMapper     ★ MotionEvent → 샘플 (묶음 전달·멀티포인터 해석)
│   │   └── TouchCaptureController  ★ @Singleton 수집기, 링버퍼 + 묶음 저장
│   ├── db/                      Room: sessions 1:N touch_points (CASCADE)
│   ├── local/                   SettingsStore(DataStore) · DeviceProfileProvider
│   ├── repository/              TouchSessionRepositoryImpl
│   └── export/                  CsvSessionWriter · JsonSessionWriter · SessionExporterImpl
│
├── di/                          Coroutines · Database · DataStore · Repository 모듈
│
└── ui/
    ├── theme/                   제도 용지(라이트) / 청사진(다크)
    ├── navigation/              Screen · CaptureTab
    ├── components/              SectionCard · StatusDot · HintText
    ├── capture/                 ★ 탭 2개 + ScreenProjection + CoordinateFormat
    ├── sessions/                세션 목록
    ├── detail/                  세션 상세 + 내보내기(SAF)
    └── settings/                캡처·표시 설정
```

### 설계상 중요한 결정 3가지

**1. 캡처는 Composable 이 아니라 `MainActivity.dispatchTouchEvent` 에서 한다.**
특정 화면의 `pointerInput` 에 매달면 탭을 옮기는 순간 측정이 끊긴다. Activity 에서
**관찰만 하고 이벤트를 소비하지 않으므로**, 같은 터치가 그대로 Compose 로 흘러가
탭 전환·버튼도 평소처럼 동작한다.

**2. 좌표계를 세션에 박제한다.**
px 값은 기기·해상도가 바뀌면 비교할 수 없다. 세션을 열 때의 `ScreenMetrics`
(화면 크기·xdpi·ydpi·density)를 통째로 저장하고, 내보낸 파일에도 같이 싣는다.
그래서 `MainActivity` 는 **방향 고정·멀티윈도우 금지**다 — 세션 중간에 좌표계가
바뀌면 앞뒤 데이터를 비교할 수 없다.

**3. 쓰기는 묶음으로, 화면은 즉시.**
MOVE 는 초당 수백 건이라 건건이 insert 하면 터치 디스패치 스레드가 막힌다.
점은 채널에 던지고 백그라운드에서 묶어 저장하며, UI 는 DB 를 기다리지 않고
메모리 링버퍼(`livePoints`)를 본다.

---

## 스택

| | |
|---|---|
| UI | Jetpack Compose + Material 3, Navigation-Compose |
| DI | Hilt |
| 비동기 | Coroutines / Flow (MVVM: 단일 `UiState` + `StateFlow`) |
| 저장 | Room (세션·터치포인트) + DataStore(Preferences) (설정) |
| 내보내기 | CSV · JSON(kotlinx.serialization), SAF 로 사용자가 위치 선택 |
| 빌드 | AGP 8.7 / Kotlin 2.1 / Hilt 2.56 / JDK 21 / Gradle 9.4 (wrapper) |

`minSdk 24 · targetSdk 35 · applicationId com.mandro.touchtracker`

권한은 비어 있다. 자기 화면의 터치는 권한이 필요 없고, 내보내기는 SAF 가
처리한다. `WAKE_LOCK` 만 선언되어 있다(정렬 중 화면 꺼짐 방지).

---

## 런처 아이콘

십자선 3쌍 + 교차점 마커 3개 — 이 앱이 하는 일(터치 한 점을 좌표로 집어내기)을
그대로 그린 것이다. 초기 참고 이미지(`docs/icon_ex.png`)의 또렷한 원형감과
터치 클릭 발광(Glow)을 충실히 계승하되, 둔탁한 검은 테두리를 단정하고 세련된
잉크 외곽선으로 가다듬고 색상을 모던하게 개선했다.

초기 참고 이미지는 `docs/icon_ex.png`.

| 파일 | 대상 |
|---|---|
| `drawable/ic_launcher_{background,foreground}.xml` | API 26+ adaptive icon (+ monochrome 테마 아이콘) |
| `mipmap-*dpi/ic_launcher{,_round}.webp` | API 24~25 폴백. **손으로 만들지 않는다** |

폴백 비트맵은 벡터와 **같은 좌표·색**을 스크립트가 다시 그린 것이다.
벡터를 고쳤으면 반드시 같이 돌린다:

```bash
python scripts/gen_legacy_icons.py app/src/main/res
```

> 참고 앱(Mark7)의 만드로 로고 아이콘을 쓰지 않은 이유: 실험용 폰에 두 앱을 같이
> 깔아 두면 런처에서 구분이 안 된다.

---

## 데이터 모델

```
TouchSession(id, name, note, startedAt, endedAt, DeviceProfile, pointCount)
  └─ TouchPoint(sequence, pointerId, phase, xPx, yPx,
                pressure, touchMajorPx, touchMinorPx, orientationRad,
                elapsedMs, epochMs)
```

- **저장은 언제나 px 원본.** mm·정규화 좌표는 중복 저장하지 않고 세션의
  `ScreenMetrics` 로 그때그때 환산한다 — 중복은 둘이 어긋날 여지만 만든다.
- `phase` 는 `DOWN / MOVE / UP / CANCEL`. 로봇 손의 "조준 결과"는 보통 `DOWN` 이고,
  `DOWN`→`UP` 사이 거리가 접촉 중 미끄러짐(slip)이다.
- `pressure` 는 기기마다 스케일이 달라 **절대값 비교 금지**. 같은 기기 안 상대 비교만.
- 시간은 두 가지를 다 남긴다: `elapsedMs`(단조 시계 기준, 간격 계산용),
  `epochMs`(절대 시각, 로봇 컨트롤러 로그와 맞출 때).

### 내보내기

| 형식 | 용도 |
|---|---|
| CSV | 엑셀·pandas. `#` 주석 줄에 기기·좌표계, 그 아래 헤더 + 한 줄 = 한 점. px·mm·정규화 좌표 모두 포함 |
| JSON | 재현·재생. 세션 메타(`ScreenMetrics` 포함)까지 한 파일에 |

```python
import pandas as pd
df = pd.read_csv("touch_20260916_143012_grip-A.csv", comment="#")
```

---

## 지금 상태 (스캐폴드)

| 영역 | 상태 |
|---|---|
| 좌표 변환 (`ScreenMetrics`) | **구현 + 단위테스트** |
| MotionEvent 해석 (`TouchEventMapper`) | **구현 + 단위테스트** (묶음 전달·멀티포인터·합성 입력 필터) |
| 모눈종이 사영 (`ScreenProjection`) | **구현 + 단위테스트** |
| CSV / JSON 내보내기 | **구현 + 단위테스트** (형식 계약 고정) |
| 수집기 / Room / DI / Navigation / Theme | 배선 완료 (실기기 검증 전) |
| 화면 5종 | 동작하는 뼈대 — 상세 편집·통계 UI 는 `// TODO` |

### 다음 작업

1. **실기기 검증** — 로봇 손으로 실제 탭 → `pressure`/`touchMajor` 가 기기에서
   의미 있는 값으로 오는지 확인. `ScreenMetrics.xDpi` 신뢰 여부도 같이.
2. **목표 좌표 입력 UI** — 모눈종이에 목표점을 찍고 실측점과의 오차 벡터를 표시.
   이게 있어야 "정확도"를 수치로 말할 수 있다.
3. **조준 통계** — 평균 중심, 표준편차, CEP(원형 확률 오차). 세션 상세 화면에.
4. **세션 상세에 모눈종이 뷰** — 지금은 요약 수치만.
5. **세션 이름·메모 편집**, 삭제 전 확인 다이얼로그.
6. 기록 중 UI 크롬(탭 바·버튼) 영역 터치를 측정에서 제외할지 결정.
