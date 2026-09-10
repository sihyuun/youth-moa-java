# 관리자 프로그램 등록·편집 폼 (`/admin/programs/new`, `/admin/programs/{id}`) — 디자인 계약

- 상태: 신설 (A3-1 admin-program-form · 2026-09-10)
- 원본: `docs/00_assets/admin/prototype.html` L2422~2618 (Program Form 3탭)
- 스코프 결정: `docs/specs/A3-admin-program-form.md` (Qn-A/B/C/1~8/Δ1~6 모두 A)
- 계약 파일: `e2e/contracts/admin-program-form.ts`

## 아키텍처

- SSR + PRG (Post-Redirect-Get). form-urlencoded 만 사용 (A3-1 은 multipart 없음, 이미지는 URL 입력만)
- 3탭 (프로그램 정보 / 신청 정보 / 약관 정보) — 클라이언트 사이드 탭 전환 (`data-tab-target`)
- 저장 시 전체 폼 검증 (탭별 partial 저장 없음 · Qn-Δ5 A)
- 400 매핑: `AdminExceptionHandler` (admin-notice/term 승계). 검증 실패 → `IllegalArgumentException` → 400
- 삭제: FK 참조 (Application) 있으면 400 (Qn-3 A). 없을 때만 물리 삭제. 이때는 `active=false` 전환을 안내

## 상태머신

| 상태 | 진입 | 다음 |
|---|---|---|
| 신규 폼 (`mode=new`) | `GET /admin/programs/new` | 저장 → `POST /admin/programs` → 302 `/admin/programs/{newId}` |
| 편집 폼 (`mode=edit`) | `GET /admin/programs/{id}` (A2 상세 대체 · Qn-A A) | 저장 → `POST /admin/programs/{id}` → 302 (자기 자신) |
| 삭제 confirm | 편집 폼 삭제 버튼 클릭 → 모달 | 확인 → `POST /admin/programs/{id}/delete` → 302 `/admin/programs` (성공) or 400 (FK) |
| 탭 전환 | 탭 헤더 클릭 | 클라이언트 사이드 (`.admin-program-form-tab-panel--active` 클래스 토글) |

## RBAC

- 목록·조회: SYSTEM_ADMIN + CENTER_ADMIN (A2 승계)
- 등록·편집·삭제: **SYSTEM_ADMIN only** (Qn-1 A). Program-Center FK 미도입 → CENTER 격리 fragile → A9 이후 확장

## CTA 라우팅

| 트리거 | 경로 | 응답 |
|---|---|---|
| A2 목록 "+ 프로그램 등록" | `/admin/programs/new` | 200 (신규 폼) |
| A2 목록 행 클릭 / 편집 버튼 | `/admin/programs/{id}` | 200 (편집 폼) |
| 편집 폼 취소 | `/admin/programs` | 302 목록 |
| 편집 폼 저장 | `POST /admin/programs/{id}` | 302 자기 자신 |
| 편집 폼 삭제 → 모달 확인 | `POST /admin/programs/{id}/delete` | 302 목록 or 400 FK |
| 편집 폼 "자격요건 편집" | `/admin/programs/{id}/eligibility` | F4 페이지 (별도) |
| 편집 폼 "동적 필드 관리" | `/admin/programs/{id}/dynamic-fields` | F0c 페이지 (별도) |

## 폼 필드 (탭별)

**탭 1 — 프로그램 정보**
- `title` * (255), `description` (500), `imageUrl` (500 · URL 입력만), `category` (50), `region` (50), `organization` * (100), `startDate` (date), `endDate` (date), `content` * (TEXT)

**탭 2 — 신청 정보**
- `applyStartDate` * (date), `applyEndDate` * (date), `venue` (200), `contact` (100), `capacity` (number), `approvalMode` * (radio · AUTO/MANUAL), `active` (checkbox · Qn-Δ6 A)

**탭 3 — 약관 정보**
- `termsService` (TEXT), `termsPrivacy` (TEXT), `termsMarketing` (TEXT)

## 검증 규칙 (서비스 계층)

| 규칙 | 메시지 |
|---|---|
| title 필수 | 프로그램 제목을 입력해주세요. |
| title ≤255자 | 프로그램 제목은 255자 이하여야 합니다. |
| organization 필수 | 청년센터(운영기관) 를 입력해주세요. |
| content 필수 | 상세 내용을 입력해주세요. |
| applyStart/End 필수 | 신청 기간을 입력해주세요. |
| applyStart ≤ applyEnd | 신청 시작일이 신청 마감일보다 이후일 수 없어요. |
| startDate ≤ endDate | 진행 시작일이 진행 종료일보다 이후일 수 없어요. |
| capacity > 0 | 모집 인원은 1명 이상이어야 합니다. |
| FK 삭제 차단 | 이 프로그램은 신청 이력이 있어 삭제할 수 없어요. 대신 비활성화(운영중단)로 전환해주세요. |

## 이월 (A3-2 이관 · 절대 손대지 말 것)

- F4/F0c 인라인 통합 (본 티켓은 별도 페이지 링크만)
- Course 엔티티 (강좌 제공 radio + 다중 row)
- ProgramAttachment 엔티티 (첨부 다중 업로드)
- 이미지 파일 업로드 (URL 입력만 유지)
- draft 임시저장

## deviation

- prototype 청년센터 select 는 하드코딩 9종 → 본 티켓은 text input (Program-Center FK 미도입 A9 승계)
- prototype "강좌 제공 · 신청 질문 관리 · 약관명 · 약관 내용 1000자" 라디오 UI → A3-2 로 이월. 본 티켓 약관 탭은 3분류 textarea (`termsService/termsPrivacy/termsMarketing`) 로 단순화
- prototype 툴바 B/I/U + 첨부 대시드 버튼 → A3-2 (rich editor · attachment)
- 이미지 업로드 UI 190×190 카드 → URL text input 단독
