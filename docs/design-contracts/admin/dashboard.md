# 관리자 대시보드 (`/admin`) 콘텐츠 — 서술 계약

- 추출 기준: `docs/00_assets/admin/prototype.html` L602~788 · 2026-09-03 A1 신설
- 관련 기계 계약: `e2e/contracts/admin-dashboard.ts` (~40 assertions)

## 정보 구조

MAIN (`#FAFAFB` bg · padding 24/28)

1. Welcome (mb 22)
    - h2 "반가워요, {이름}님 👋" (21/700 `#2B2A3D`)
    - p "오늘도 청년모아 프로그램 운영을 함께합니다." (13/regular `#6E6B82`)

2. Stat cards (grid `repeat(4, 1fr)` · gap 14 · mb 22)
    - 진행중 프로그램 (녹색 아이콘 `#10B981` on `#D1FAE5`) — count(status=OPEN)
    - 마감 프로그램 (회색) — count(status=ENDED)
    - 진행 예정 (오렌지 `#EA580C` on `#FFF7ED`) — count(status=UPCOMING)
    - 전체 회원 (보라 `#7C3AED` on `#EDE9FE`) — count(User.role=USER)
    - 각 카드: 라벨 · 34px 아이콘 · 32/700 Inter 수치 · 증감 문구 (하드코딩, deferred:A6)

3. Body row (grid `1fr 268px` · gap 16)
    - 최근 프로그램 테이블 (5행)
        - 컬럼: 프로그램명 · 청년센터 · 신청현황 · 상태
    - 우측 컬럼 (flex column · gap 14)
        - 빠른 메뉴 카드 (프로그램 등록 / 통계 보기 / 사용자 관리) — 3개 disabled placeholder
        - 승인 대기 카드 (border-left 4px `#3F30E9` · 32px count + "바로 처리하기 →")

4. 마감 임박 프로그램 (mt 16 · D-7 이내 배지)
    - 컬럼: 프로그램명(+마감일) · 청년센터 · 신청현황 · D-day · 상태
    - `Program.applyEndDate` 미도입 (deferred: A3) → `endDate` 임시 파생

## 센터 격리 (Qn-5 A)

- SYSTEM_ADMIN: 전체 (`scopeCenter = null`)
- CENTER_ADMIN: 자기 센터 (`scopeCenter = user.center.name` · Program.organization 문자열 매칭 근사, A9 에서 FK 로 승격)

## 이탈 · 이월

| 항목 | 종류 | 사유 |
|---|---|---|
| Stat card 증감 문구 | **deferred: A6** | "지난달보다 증가" 계산 미구현. 하드코딩 카피 유지 |
| 마감 임박 D-day (`applyEndDate`) | **deferred: A3** | A3 에서 `applyEndDate` 도입 후 교체 |
| 빠른 메뉴 3개 | **deferred: A2/A5/A6** | 목적지 미구현 · disabled placeholder |
| 승인 대기 "바로 처리하기 →" 링크 | **deferred: A5** | 승인 처리 페이지 A5 |
| 전체보기 → 링크 (최근·마감임박) | **deferred: A2** | 프로그램 목록 A2 |
| 반응형 정규화 | **deferred: A8** | |
