# admin/user-detail — 관리자 사용자 상세 계약

| 항목 | 값 |
|---|---|
| 화면 | `/admin/users/{id}` |
| 계약 파일 | [e2e/contracts/admin-user-detail.ts](../../../e2e/contracts/admin-user-detail.ts) |
| 원본 | `docs/00_assets/admin/prototype.html` L1774~1943 |
| 착수 티켓 | A5-admin-users (2026-09-15) |
| spec | `docs/specs/A5-admin-users.md` |

## 공통 정책

- Qn-B 결정: **페이지 단위** (모달 아님) — A4 는 모달이지만 사용자 상세는 정보량이 커서 페이지 채택
- Qn-C 결정: 차단 시 기존 신청 이력 유지 (cascade 없음)
- **prototype 이탈 (deviation)**: 회원 차단/재활성화 CTA — prototype 은 status 표시만 · UX 관점 신설 (danger zone)

## prototype 인용 요약

| 라인 | 내용 |
|---|---|
| L1776 | 2컬럼 grid `1fr 1fr` gap 16px |
| L1780~1868 (좌) | 회원 정보 카드 — 이메일(disabled) · 이름 · 성별 · 권한 radio 3종 · 생년월일 · 핸드폰 · 주소 · 취소/저장 |
| L1822~1839 | 권한 radio — 사용자 · 관리자 · 시스템 관리자 3옵션 |
| L1869~1941 (우) | 프로그램 신청 현황 카드 — 탭 4종 (전체·승인·반려·취소) + 카드 리스트 |
| L1889~1894 | 탭 4종 배경 `#F0EFF3` |
| L1898~1919 | 신청 카드 — 프로그램 아이콘 · 이름 링크 · 상태 dropdown/badge · 접수 일시 · "신청 상세 →" |

## 이월/이탈 요약

| 항목 | prototype | 채택 | 사유 |
|---|---|---|---|
| 회원 정보 편집 (name·phone·birth·address 저장) | 있음 | **읽기 전용** (이번 스코프) | Qn-11 이월 = 관리자가 사용자 프로필 편집은 후속 티켓. 이번 스코프는 role/차단/note 만 |
| 새 비밀번호 입력 | 있음 | **미포함** | 관리자 비밀번호 재설정 flow (Qn-11 B) 는 A7 이월 |
| 주소 검색 버튼 | 있음 | **미포함** | 편집 기능 자체가 이월 |
| 신청 상세 dropdown | 있음 | **badge only** (읽기 전용) | 상태 변경은 A4 신청 관리 화면에서 수행 |
| 회원 차단/재활성화 danger zone | **없음** | **신설 (deviation)** | 사용자 요구 반영 — 사유 필수 (`deactivation_reason`) |
| 관리자 메모 (admin-note) | 없음 | **신설** | Qn-2 A · V16 신설 컬럼 |

## RBAC

Qn-A 이월 결정에 따라 `/admin/users/*` 는 **SYSTEM_ADMIN 만 접근**. CENTER_ADMIN 은 403.

## Safeguard 3종 (spec §2)

1. 자기 자신 role/차단 변경 금지
2. 마지막 활성 SYSTEM_ADMIN 강등·차단 금지 (`UserRepository.countByRoleAndIsActiveTrue`)
3. SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 (컨트롤러 `@PreAuthorize` 로 확보)

## 구현 매핑

| 계약 행 | 구현 위치 |
|---|---|
| 2컬럼 grid | `templates/admin/user/detail.html:48` |
| 좌 프로필 카드 | `templates/admin/user/detail.html:51-89` |
| 권한 radio form | `templates/admin/user/detail.html:92-112` |
| admin-note form | `templates/admin/user/detail.html:115-128` |
| Danger zone | `templates/admin/user/detail.html:131-166` |
| 차단 확인 모달 | `templates/admin/user/detail.html:226-255` (사유 필수) |
| 우 신청 이력 카드 | `templates/admin/user/detail.html:170-219` |
| 탭 4종 | `templates/admin/user/detail.html:177-184` |
| 신청 카드 렌더 | `templates/admin/user/detail.html:193-217` |
| 뒤로가기 링크 | `templates/admin/user/detail.html:27` |
| Self-check disable | `templates/admin/user/detail.html:104,110,141` (`isSelf`) |

## 검증 방식

- 계약 검사: `npx playwright test --project=contracts admin-user-detail`
- 기능 E2E: `admin-user-detail.spec.ts` · `admin-users-actions.spec.ts`
- 정적: `AdminUserDetailRenderTest` · `AdminUserServiceTest` (safeguard 3종)
