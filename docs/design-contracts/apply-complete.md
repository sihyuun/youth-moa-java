# 디자인 계약 — 신청 완료 `/apply/complete?applicationId={id}`

> **추출 기준**: `docs/00_assets/prototype.tsx` `ApplyComplete` L1217~1249 · 2026-08-11
> **검증 상태**: 계약 초안 (`spec_draft`) — Q-1~Q-3 사용자 결정 대기
> **기계 계약**: `e2e/contracts/apply-complete.ts` — 총 28 check
> **auth**: 필요 + 완료 페이지는 **직접 URL 접근 불가 아님** (권한 검증 통과 시 GET 가능)

## 1. 화면 아키텍처

**단일 완료 화면** — 헤더 없이 중앙 정렬 · Footer 하단 밀림.

```
┌───────────────── viewport (bg=#F7F7F8) ──────────────────┐
│                                                           │
│              (flex center, min-h 100vh)                   │
│                                                           │
│          ┌────── apply-complete-inner ──────┐             │
│          │  padding 56 80 · align-center     │            │
│          │                                    │           │
│          │        ⬤ (88 · success-light bg)  │           │
│          │         ✓ (46 SVG · #10B981)      │           │
│          │                                    │           │
│          │  프로그램 신청이 완료되었습니다 (h1 26/700)  │
│          │  <부제> 결과는 카카오톡·문자·이메일로 안내… │
│          │                                    │           │
│          │  ┌── 요약 카드 (width 520) ──┐    │           │
│          │  │ [80 썸네일][승인대기 뱃지]│    │           │
│          │  │             [프로그램 제목]│   │           │
│          │  │             [#A123 신청번호]│  │           │
│          │  │             [기관 · 지역]  │   │           │
│          │  │             [기간]         │   │           │
│          │  │             [신청일시 …]   │   │           │
│          │  └───────────────────────────┘    │           │
│          │                                    │           │
│          │  [ 홈으로 (ghost 160) ][ 내 신청 현황 보기 (primary 200) ] │
│          │                                    │           │
│          │  잘못 신청하셨나요? 신청 현황에서 취소할 수 있어요 (mini) │
│          └────────────────────────────────────┘           │
│                                                           │
├────────────────────── Footer ────────────────────────────┤
```

- 컴포넌트 계층: `body > main.apply-complete-screen(flex column) > .apply-complete-inner(flex 1 center) > Footer`
- 헤더 slot 없음 — prototype L1219~1247 에 Header 컴포넌트 자체가 렌더되지 않음 (**F2c Q4 로 향후 헤더 추가 논의** — apply.md Q-1 과 짝)

## 2. 상태 머신

완료 페이지는 클라이언트 상태가 없다. 서버 렌더 후 정적.

| 서버 상태 (Thymeleaf) | 데이터 | 화면 반영 |
|---|---|---|
| `myApplication.id` | Long | `#A{id}` (신청번호) |
| `myApplication.appliedAt` | LocalDateTime | `신청일시 YYYY-MM-DD HH:mm` |
| `program.title/organization/region/startDate/endDate/imageUrl` | Program | 요약 카드 표시 |
| `channelSubtitle` | String (서버 조립) | 부제 문구 (0/1/2/3 활성 채널) |

- **h1 `autofocus`** — 완료 페이지 진입 시 스크린리더/키보드 포커스가 h1 로. 접근성 대응 (prototype 없음, 구현 추가)

## 3. CTA·링크 라우팅

| 요소 | prototype 목적지 | 구현 목적지 | 판정 |
|---|---|---|---|
| 홈으로 (ghost) | `go('home')` | GET `/` | 정합 |
| 신청 현황 보기 (primary) | `go('mypage')` | GET `/mypage?tab=history` | 정합 (구현이 탭 파라미터 추가) |
| 하단 mini-link | (없음) | GET `/mypage?tab=history` | prototype 추가 (P-5) |

## 4. POLICY 준수

| 정책 | 상태 | 비고 |
|---|---|---|
| P-1 카피 | ⚠️ 정정 | primary CTA "신청 현황 보기" → "내 신청 현황 보기" 로 정정. deviation |
| P-2 그림자 | ✅ 준수 | 요약 카드 shadow 없음 (proto 정합, border 만) |
| P-3 SVG 아이콘 | ✅ 준수 | 성공 체크 아이콘 SVG 인라인 (문자 대체 없음) |
| P-4 폭 토큰 | ✅ 준수 | 카드 520 · ghost 160 · primary 200 를 그대로 명시 |
| P-5 prototype 없는 추가 | ✅ 기록 | `#A{id}` · `신청일시` · h1 autofocus · mini-link · fallback 부제 문구 (§5) |

## 5. prototype 에 없는 구현 추가 요소

- **신청번호** `.apply-complete-appno` (`#A{id}`) — 사용자가 상담 시 번호 인용 가능
- **신청일시** `.apply-complete-applied-at` — 신청 접수 시각 명시
- **h1 `autofocus` + `tabindex="-1"`** — 접근성 · 키보드 포커스 앵커
- **mini-link** (`.apply-complete-mini-link`) — "잘못 신청하셨나요?" 취소 안내
- **부제 fallback 문구** — 활성 채널 0 시 "결과는 마이페이지 > 신청 현황에서 확인해주세요"
- **정보 대비**: prototype `pg.date` 는 단일 문자열, 구현은 `기간` 을 `startDate~endDate` 로 분해

## 6. 계약이 커버하지 않는 항목

- 부제 문구 4가지 조립 결과 (`buildChannelSubtitle`) — 단위 테스트가 커버
- 존재하지 않는 applicationId → 404 (`apply-complete.spec.ts` 가 커버)
- 다른 유저의 applicationId 접근 → 404 (`ApplicationCompleteControllerTest` @WebMvcTest 가 커버)
- 성공 아이콘 색상 (`#10B981` 인라인 fill) — SVG 내부 색은 계약 대상 아님

## 7. 이월 (deferred) · 영구 이탈 (deviation) 요약

| id | 필드 | 사유 |
|---|---|---|
| `header.absent` | `deferred` | F2c Q4 (`fix/apply-complete-header`) 결정 확정 시 계약 갱신 |
| `card.border-radius` | `deviation` | 구현 8px(md), proto 12px(lg) 이탈 — Q-2 대기 |
| `action.ghost.height` | `deviation` | 구현 48, proto 50 이탈 — Q-3 대기 |
| `action.primary.height` | `deviation` | 위와 동일 사유 |
| `action.primary.text` | `deviation` | POLICY P-1 카피 정정 "내 신청 현황 보기" |

## 8. 결정 확정 (2026-08-11)

### ✅ Q-1. 공용 헤더 노출 — **(A) 헤더 없음 (prototype 정합)**
- apply.md Q-1 과 짝 결정. 두 화면 모두 헤더 제거
- 심리적 성취감·몰입 완료 흐름 우선. 국내 관용 정합
- **F2c Q4 파생 큐(`fix/apply-complete-header`) 는 이 결정으로 해소** — 헤더 추가가 아니라 유지 없음이 정답

### ✅ Q-2. 요약 카드 radius — **(A) 12 로 통일**
- `--radius-lg`. apply Summary 카드와 동일

### ✅ Q-3. CTA 버튼 height — **(A) 50 으로 통일**
- `Btn size:l`. apply nav 버튼과 동일

### ✅ Q-4. complete.html 주석 오기 정정 — **(A) 정정**
- `prototype.tsx ApplyComplete L1217~1249` 로 갱신

## 9. 다음 단계

1. ~~Q-1~Q-4 사용자 결정~~ ✅ 2026-08-11 완료
2. `apply-complete.ts` 갱신 (필요 시 deviation 정리)
3. ym-impl 인계 — 헤더 제거 · 카드 radius 12 · CTA height 50 · 주석 정정 · 뒤로가기 SVG(필요 시)
4. bootRun 후 `npx playwright test --project=contracts visual-apply-complete` → 갭 0 확인
5. `docs/specs/README.md` 파생 큐에서 `fix/apply-complete-header` 항목 제거 (Q-1 로 해소)

## 관련

- 신청 폼 계약: [apply.md](apply.md) · `e2e/contracts/apply.ts`
- 프로그램 상세: [program-detail.md](program-detail.md) (신청 진입점)
- 파생 큐: `docs/specs/README.md §파생 큐` — `fix/apply-complete-header` (F2c Q4)
- 원본 명세: [../specs/F0c-remainder.md](../specs/F0c-remainder.md)
- 전 화면 공통 정책: [POLICY.md](POLICY.md)
