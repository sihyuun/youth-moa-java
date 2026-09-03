# 관리자 shell (다크 헤더) — 서술 계약

- 추출 기준: `docs/00_assets/admin/prototype.html` L336~537 · 2026-09-03 A1 신설
- 관련 기계 계약: `e2e/contracts/admin-shell.ts` (~30 assertions)

## 정보 구조

- `<header class="admin-header">` — 56px sticky · `#111827` · z-index 500
    - 좌 (flex 1)
        - `logo_white.png` 34px + ADMIN 뱃지 (10px, `#1E293B` bg, `#334155` border, `#A6A3B3` text)
        - 세로 divider 1px x 18px `#334155`
        - 센터 스코프 셀렉터 (표시만, A7 실동작)
            - SYSTEM_ADMIN: "전체" + 아래 화살표 (드롭다운 자리)
            - CENTER_ADMIN: `.center.name` 고정 (화살표 없음)
    - 중앙 (absolute · translateX(-50%)): GNB
        - **Qn-6 B**: 통계/프로그램 관리/사용자 관리 미구현 → 렌더 안 함
        - 대시보드 활성 링크만 남김 (`admin-nav-link active`)
    - 우 (flex 1 · justify-end)
        - 검색 자리 (disabled 아이콘)
        - 알림 벨 자리 (disabled)
        - 유저 드롭다운 (avatar `.` chevron)
            - 프로필 요약 (이름 · 역할 · 이메일)
            - 사용자 페이지 링크 (`/`)
            - 로그아웃 form (`POST /admin/logout`)

## 이탈 · 이월

| 항목 | 종류 | 사유 |
|---|---|---|
| 통계·프로그램·사용자 GNB | **deviation: A2/A5/A6 순차 도입** | Qn-6 B — 미구현 링크 미노출 |
| 검색 실동작 | **deferred: A7** | 자리 disabled |
| 알림 벨 드롭다운·미읽음 뱃지 | **deferred: A7** | 자리 disabled |
| 센터 스코프 셀렉터 실동작 (SYSTEM 드롭다운) | **deferred: A7** | 표시만 |
| 데모 권한 전환 pill | **deviation: production 제거** | HANDOFF 정책 |
| 개인 정보 수정 / 프로그램 현황 / 설정 | **deferred: A7** | 유저 드롭다운 상단 항목 |

## 왕복 링크

- 사용자 헤더 `fragments/header.html` L107~ 인증 드롭다운에 `sec:authorize` 조건부 "관리자 페이지" 링크 삽입 완료
