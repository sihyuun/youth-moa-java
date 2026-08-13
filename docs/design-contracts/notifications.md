# `/notifications` 알림 목록 화면 계약

> 마지막 갱신: 2026-08-12 · 신설 (사용자 트랙 계약 시리즈 5번째, login #138 · signup #139 · notices #140 · apply #141 이후).
>
> prototype 기준: `docs/00_assets/prototype.tsx` `NotificationsScreen` L1252~1314.
> 참고: html 라인 = tsx 라인 + 35 (같은 소스).

## 1. 목적

로그인한 사용자가 자신에게 발행된 모든 알림을 한 화면에서 확인·필터·읽음 처리하는 화면. 헤더 종모양 드롭다운(`NotifPanel`, 최대 4건) 의 "전체 보기" 대상.

## 2. 진입 · 인증

- Path: `GET /notifications`
- Auth: **로그인 필수**. `SecurityConfig.java:84` 에서 `/notifications/**` authenticated.
- 비로그인 접근 → `/login?returnUrl=/notifications` 리다이렉트.
- Controller: `NotificationController#list` (`notification/NotificationController.java`).

## 3. 컴포넌트 아키텍처 (prototype tsx)

```
NotificationsScreen (L1252)
├── 뒤로가기 버튼 (L1265, arrowL)              — history.back() or /
├── 헤더 (L1268)
│   ├── 좌측: h2 "알림" + unread 카운트 뱃지 (L1270~1272)
│   └── 우측: "모두 읽음" 버튼 (L1273, check SVG + 텍스트, unread>0 조건)
├── 필터 pill 바 (L1276)
│   ├── 전체 (count = items.length)
│   └── 안 읽음 (count = unread)
└── 그룹별 리스트 (L1288, groupOf 함수로 3분류)
    ├── "오늘"       (time 이 방금/분/시간 매칭)
    ├── "지난 7일"    (^[1-6]일 매칭)
    └── "이전"       (기타)
        └── 아이템 (L1293)
            ├── 아이콘 원형 36×36 (L1294)
            │   └── SVG (L1295, item.icon: check/calendar/bell/close)
            ├── 본문 (L1297)
            │   ├── unread dot (6×6, primary)
            │   ├── 제목 (14.5, unread 시 700 / else 500)
            │   ├── 메시지 (13.5, textSec)
            │   └── 시각 (12, textTri)
            └── 닫기(X) 삭제 버튼 (L1305, item 별 remove)
```

## 4. 상태 머신

| 상태 | 트리거 | 결과 |
|---|---|---|
| `list-all` | GET `/notifications` (default, `unread=false`) | 그룹 3개 전부 렌더, 필터 pill "전체" active |
| `list-unread` | GET `/notifications?unread=true` | unread=true 항목만 그룹핑, "안 읽음" pill active |
| `read-one` | POST `/notifications/{id}/read` (아이템 body 클릭) | 해당 항목 unread=false, link 있으면 redirect / 없으면 `/notifications` |
| `read-all` | POST `/notifications/read-all` (mark-all 버튼) | 전체 unread=false, `/notifications` redirect (non-HX) or panel fragment (HX) |
| `delete-one` | POST `/notifications/{id}/delete` (item 우측 close(X) 버튼) | 해당 항목 hard delete. HX 요청 시 204 + `hx-swap="delete"` 로 li 제거, non-HX 는 302 `/notifications` (Q-3 반영) |
| `empty` | grouped 이 비어 있음 | notif-page-empty 렌더, unread 필터 여부에 따라 문구 분기 |

## 5. CTA 라우팅

| 요소 | 이동 |
|---|---|
| 뒤로가기 버튼 | `history.back()` 있으면 그쪽, 없으면 `/` |
| 아이템 body (`.notif-page-item-body`) | POST `/notifications/{id}/read` → link 있으면 해당 리소스 (예: 프로그램 상세), 없으면 `/notifications` |
| "모두 읽음" | POST `/notifications/read-all` → `/notifications` |
| 필터 pill "전체" | GET `/notifications` |
| 필터 pill "안 읽음" | GET `/notifications?unread=true` |

## 6. POLICY 매핑

| 정책 | 이 화면에서 |
|---|---|
| **P-1** 카피 유지 | "읽지 않은 알림이 없어요" (구현) vs "안 읽은 알림이 없어요" (proto) — 현행 유지. `deviation` 처리하지 않고 정책 P-1 로 흡수 |
| **P-2** 그림자 브랜드 토큰 | 이 화면은 shadow 없음 (border-light only). 해당 없음 |
| **P-3** SVG 아이콘 강제 | **⚠️ 위반 중** — `list.html` L79~81 `<div class="notif-icon" aria-hidden>` 빈 컨테이너. impl 에서 icons.html fragment (`check`·`calendar`·`bell`·`close`) 이식 필수. `NotificationType#getIconName()` 이 이미 매핑 반환 준비 완료 |
| **P-4** 폭 토큰 전역 적용 금지 | prototype maxWidth:680 (L1264) vs 구현 720 (css L4208). notif-page 별도 값 유지 대상 |
| **P-5** prototype 외 개선 별도 기록 | 필터 active 배경(구현: primary 진배경) 은 prototype (primary-bg 옅은 배경) 을 벗어남 — Q-2 로 결정 예정 |

## 7. 강제 수단 (정책 축)

| 항목 | 강제 |
|---|---|
| 로그인 필수 | `SecurityConfig.java:84` + `HeaderNotificationAdviceTest` 통합 테스트 |
| unread 뱃지 노출 조건 (`unreadCount > 0`) | `NotificationControllerTest` 렌더 테스트 (기존) |
| 모두 읽음 처리 후 unread=0 | `NotificationServiceTest` write→read 왕복 검증 (기존) |
| 아이콘 tone 매핑 | `NotificationType#getToneColor` 단위 테스트 (기존) |

## 8. 결정 확정 (2026-08-12)

### ✅ Q-1. 알림 아이콘 SVG 이식 — (A) fragment 이식
`NotificationType.getIconName()` + `icons.html` fragment 를 `notification/list.html` 에 삽입. P-3 준수.

### ✅ Q-2. 필터 pill active 스타일 — 각 화면 prototype 값 그대로 반영 (impl 재판단)
- **초기 진단 (ym-spec)**: "notices/apply 도 같은 이슈 → 5화면 일괄 조정"
- **impl 실측 재판단**: 두 화면 prototype 이 **애초부터 다른 스타일**을 의도
  - `notifications` (tsx L1278): `T.primaryBg` (옅은 tint) + `T.primary` 글자 — **구현 이탈 → 조정**
  - `notices` (tsx L2046): `T.primary` (진 solid) + `#fff` 글자 — **구현 정합 → 유지**
  - `apply`: 필터 pill 없음 (스텝퍼)
  - `programs`: 별개 chip 계열
- **처리 결과**: `.notif-filter-pill--active` 만 옅은 primary-bg 로 조정. `.notice-tab.active` 는 solid primary 유지
- **원칙 재확인**: 계약은 각 화면의 prototype 실제 값을 기준으로 삼는다. "일괄 정책" 진단은 실측 없이 넓게 잡은 초기 진단으로, 실측 후 화면별 정합이 우선

### ✅ Q-3. 개별 알림 X 삭제 — 이번 세션 구현
prototype L1305 정합. impl 스코프 편입. 계약에서 `item.close-btn` deferred 제거 · 활성 check 로 승격.

### ✅ Q-4. P2 미세차 일괄 정정 — (A) 이번 impl 세션 포함
필터 gap · pill font-size · group-header font-weight 등 이번 impl 에서 갭 0 달성.

## 9. impl 결과 (2026-08-12 청산 완료)

- **P0 청산**: `icon.svg` fragment 이식, `filter.count` · `item.exists` seed 정합
- **P1 청산**: `back-btn.svg`, `mark-all.svg`, `title.text/font-size`, `unread-badge.exists`, `item.unread.border-color`, `item.unread.bg`, `icon.width/height` 전부 정합
- **P2 청산**: `container.max-width` 720→680, `filter.bar.gap` 8→6, `filter.pill.padding` 6/14→7/15, `filter.pill.font-size` 13→13.5, `group.header.font-weight` 600→700, item padding/gap 등
- **Q-3 승격**: `item.close-btn` deferred → 활성 check (P2)

최종: **notifications 37/37 갭 0**. deviation 1건 (P-1 카피 "읽지 않은 알림이 없어요" 유지).

## 10. 참고 자료

- `e2e/contracts/notifications.ts` (계약 소스)
- `e2e/tests/visual-notifications.spec.ts` (실행 스펙)
- `docs/specs/F2c-notification-channels.md` (관련 스펙)
- `src/main/resources/templates/notification/list.html` (현행 마크업)
- `src/main/resources/static/css/main.css` L4128~4350 (현행 CSS)
- `src/main/java/io/github/sihyuuun/youthmoa/notification/NotificationController.java`
- `src/main/java/io/github/sihyuuun/youthmoa/notification/NotificationType.java` — icon/tone 매핑 이미 완료
- prototype: `docs/00_assets/prototype.tsx` L1252~1314 (본체), L313~321 (NOTIF_ITEMS seed)
