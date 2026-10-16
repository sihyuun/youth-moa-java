/**
 * A5-1-e2e Part 1 Q3 (2026-10-16) — 관리자 사용자 상세 **발급 직후 상태** 디자인 계약.
 *
 * 왜 별도 계약인가 (Q3 B안 — 명세 §6):
 *  - 기존 `admin-user-detail.ts` 는 "임의 시점의 상세 페이지" 전제 (seed1 상세로 이동 후 검사).
 *  - A5-1 발급 flash 섹션 (초기 password 카드 · 메일 성공 배너 · 메일 실패 배너) 은 **flashInitialPassword / flashMailSent /
 *    flashMailFailed** redirect flash 가 심어진 1회성 상태에서만 렌더된다.
 *  - optional 로 기존 계약에 넣으면 "기대값이 뭔지" 모호해지고, 발급 성공·실패 경로 간 상충이 생긴다.
 *  - 따라서 러너 spec 이 **발급 → redirect → 셀렉터 검사** 를 수행하는 전용 계약을 신설한다.
 *
 * 2 variant:
 *  - **mail-sent** (기본 mock, forceFail=false): 성공 배너 노출 + password 카드 비노출 + reset CTA 노출.
 *  - **mail-fallback** (forceFail=true): 메일 실패 배너 + fallback password 카드 (value 노출) + reset CTA 노출.
 *
 * prototype 매핑:
 *  - admin/prototype.html L1774~1943 (사용자 상세 레이아웃 기본). 발급 flash 섹션 자체는 prototype 미명시 →
 *    **deviation 전제 계약** (A5-1 보안 정책에서 신설). source 필드에 명시한다.
 *
 * 사용 셀렉터 (`templates/admin/user/detail.html`):
 *  - L50  `data-testid="admin-user-mail-sent"`
 *  - L62  `data-testid="admin-user-mail-failed"`
 *  - L81  `data-testid="admin-user-initial-password"` (fallback password 카드 wrapper)
 *  - L92  `data-testid="admin-user-initial-password-value"` (password value <strong>)
 *  - L198 `data-testid="admin-user-reset-password-btn"` (재발급 CTA)
 *  - L43  `.admin-flash.admin-flash--error` (A5-1 flashError 공용)
 */

import type { ScreenContract } from './types';

/**
 * mail 발송 **성공** 직후 상세 상태.
 *
 * 러너 spec 이 반드시 아래 순서로 세팅한다:
 *  1. force-fail OFF 확인 (`/__test__/mail/force-fail?enabled=false`)
 *  2. SYSTEM_ADMIN 로그인 → `/admin/users/new` 발급 폼 입력 → submit
 *  3. 302 → `/admin/users/{id}` 리다이렉트 완료 대기 후 러너 호출
 */
export const adminUserDetailAfterIssueMailSentContract: ScreenContract = {
    screen: 'admin-user-detail-after-issue-mail-sent',
    path: '/admin/users/{id}', // 실제 이동은 러너 spec 안에서 발급 flow 로 수행
    source:
        'templates/admin/user/detail.html L40~98 · docs/specs/A5-1-e2e-admin-staff-management.md §5 ' +
        '· prototype 미명시 flash 섹션 (deviation — A5-1 보안 정책 신설)',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'mail.sent.banner.exists',
            desc: '메일 발송 성공 배너 노출',
            selector: '[data-testid="admin-user-mail-sent"]',
            kind: 'exists',
            expected: true,
            proto: 'templates/admin/user/detail.html L47~58 (A5-1-e2e deviation)',
            severity: 'P0',
        },
        {
            id: 'mail.sent.banner.text',
            desc: '배너 안내 문구 — "초대 메일이 발송"',
            selector: '[data-testid="admin-user-mail-sent"] p:first-of-type',
            kind: 'text',
            expected: '초대 메일이 발송되었어요',
            proto: 'templates/admin/user/detail.html L52~54',
            severity: 'P1',
        },
        {
            id: 'initial.password.card.absent',
            desc: '메일 성공 시 fallback password 카드는 렌더되지 않음',
            selector: '[data-testid="admin-user-initial-password"]',
            kind: 'count',
            expected: 0,
            proto:
                'templates/admin/user/detail.html L78 flashInitialPassword 가 flashMailSent 와 상호배타 ' +
                '(AdminUserController Q3 보안 분기)',
            severity: 'P0',
        },
        {
            id: 'mail.failed.banner.absent',
            desc: '메일 성공 경로에서는 실패 배너 비노출',
            selector: '[data-testid="admin-user-mail-failed"]',
            kind: 'count',
            expected: 0,
            proto: 'templates/admin/user/detail.html L59~73',
            severity: 'P1',
        },
        {
            id: 'reset.password.cta.exists',
            desc: '임시 비밀번호 재발급 CTA 노출',
            selector: 'button[data-testid="admin-user-reset-password-btn"]',
            kind: 'exists',
            expected: true,
            proto: 'templates/admin/user/detail.html L198',
            severity: 'P0',
        },
        {
            id: 'flash.error.absent',
            desc: '발급 에러 flash 비노출 (성공 경로)',
            selector: '.admin-flash.admin-flash--error',
            kind: 'count',
            expected: 0,
            proto: 'templates/admin/user/detail.html L43~44 (A5-1 flashError 공용)',
            severity: 'P1',
        },
    ],
};

/**
 * mail 발송 **실패 fallback** 직후 상세 상태.
 *
 * 러너 spec 이 반드시 아래 순서로 세팅한다:
 *  1. `POST /__test__/mail/force-fail?enabled=true` (204)
 *  2. SYSTEM_ADMIN 로그인 → `/admin/users/new` 발급 폼 입력 → submit
 *  3. 302 → `/admin/users/{id}` 리다이렉트 완료 대기 후 러너 호출
 *  4. finally 로 반드시 force-fail OFF 복구 (`?enabled=false`)
 */
export const adminUserDetailAfterIssueMailFallbackContract: ScreenContract = {
    screen: 'admin-user-detail-after-issue-mail-fallback',
    path: '/admin/users/{id}',
    source:
        'templates/admin/user/detail.html L59~98 · docs/specs/A5-1-e2e-admin-staff-management.md §5 ' +
        '· A7 followup force-fail flag',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'mail.failed.banner.exists',
            desc: '메일 발송 실패 배너 노출',
            selector: '[data-testid="admin-user-mail-failed"]',
            kind: 'exists',
            expected: true,
            proto: 'templates/admin/user/detail.html L59~73 (deviation — A5-1 fallback)',
            severity: 'P0',
        },
        {
            id: 'mail.sent.banner.absent',
            desc: '실패 경로에서는 성공 배너 비노출',
            selector: '[data-testid="admin-user-mail-sent"]',
            kind: 'count',
            expected: 0,
            proto: 'templates/admin/user/detail.html L47~58',
            severity: 'P1',
        },
        {
            id: 'initial.password.card.exists',
            desc: 'fallback 초기 password 카드 노출',
            selector: '[data-testid="admin-user-initial-password"]',
            kind: 'exists',
            expected: true,
            proto: 'templates/admin/user/detail.html L78~98 (Q3 A fallback)',
            severity: 'P0',
        },
        {
            id: 'initial.password.value.not.empty',
            desc: 'password value 가 비어있지 않음 (운영자가 직접 전달해야 하므로)',
            selector: '[data-testid="admin-user-initial-password-value"]',
            kind: 'attr-not-empty',
            attr: 'data-testid',
            expected: true,
            proto: 'templates/admin/user/detail.html L91~93 (SecureRandomPasswordGenerator 결과)',
            severity: 'P0',
        },
        {
            id: 'reset.password.cta.exists',
            desc: '재발급 CTA 는 fallback 경로에서도 노출',
            selector: 'button[data-testid="admin-user-reset-password-btn"]',
            kind: 'exists',
            expected: true,
            proto: 'templates/admin/user/detail.html L198',
            severity: 'P0',
        },
    ],
};
