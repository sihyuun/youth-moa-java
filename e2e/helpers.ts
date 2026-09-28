/**
 * E2E 공용 헬퍼.
 *
 * 목적: spec 마다 반복되던 login·외부 요청 차단·HTMX 로드 대기 로직을 한 곳에 모아
 * spec 파일이 시나리오 자체에 집중하도록 한다.
 *
 * seed 유저 규약 (DataInitializer 참고):
 *  - seed1~seed30@youth-moa.test 모두 비밀번호 'Test1234!'
 *  - seed1~seed28: programs[0] APPROVED
 *  - seed1~seed19: programs[1] PENDING
 *  - seed1~seed6:  programs[2] PENDING
 *  - seed29, seed30: 어떤 프로그램에도 신청 없음 (fresh)
 */

import { expect, type Page } from '@playwright/test';

export const SEED_PASS = 'Test1234!';

/** 시드 유저 이메일 조립 (seed1@youth-moa.test 등) */
export function seedEmail(n: number): string {
    return `seed${n}@youth-moa.test`;
}

/**
 * 시드 유저 비밀번호. DataInitializer 시드는 seed1~30 이 모두 SEED_PASS 를 사용.
 * mypage-profile-edit 계약처럼 재확인 폼 통과가 필요한 시나리오용.
 */
export function seedPassword(_n: number): string {
    return SEED_PASS;
}

/**
 * 외부 도메인 (CDN/폰트 등) 호출을 모두 차단한다.
 * 회사 PC SSL 프록시 환경에서 hang 회피 목적.
 * test.beforeEach 안에서 호출.
 */
export async function abortExternal(page: Page): Promise<void> {
    await page.route(/^https?:\/\/(?!localhost)/, route => route.abort());
}

/**
 * 로그인 폼 채우고 제출 → "/" 로 이동 완료까지 대기.
 * SecurityConfig 는 seed 유저 email 을 username 필드로 받도록 구성됨.
 */
export async function login(
    page: Page,
    email: string,
    password: string = SEED_PASS,
): Promise<void> {
    await page.goto('/login', { waitUntil: 'commit' });
    await page.locator('input[name="username"]').fill(email);
    await page.locator('input[name="password"]').fill(password);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
}

/**
 * A1 관리자 시드 계정. DataInitializer.seedAdmins() 로 부팅 시 idempotent 시드.
 * email 은 고정 (Qn-4 B), 비밀번호는 env override 가능하지만 e2e 기본값을 사용.
 */
export const ADMIN_SYSTEM_EMAIL = 'sysadmin@youth-moa.test';
export const ADMIN_CENTER1_EMAIL = 'center1@youth-moa.test';
// A7-e2e-suite 사전 시드 (2026-09-28): fan-out 3축 검증용 확장 계정.
//   - ADMIN_CENTER2_EMAIL — centers[2] 소속 CENTER_ADMIN. organization 축과 createdBy/watcher 축 분리 검증용.
//   - ADMIN_INACTIVE_EMAIL — centers[3] 소속 CENTER_ADMIN, isActive=false. 비활성 admin skip 검증용.
export const ADMIN_CENTER2_EMAIL = 'admin_center2@youth-moa.test';
export const ADMIN_INACTIVE_EMAIL = 'admin_inactive@youth-moa.test';
export const ADMIN_SEED_PASS = 'Admin!234';

/**
 * 관리자 계정으로 로그인 후 `/admin` 도달까지 대기.
 * SecurityConfig 는 별도 admin filter chain 을 사용하므로 /admin/login form 을 통과해야 한다.
 */
export async function loginAdmin(
    page: Page,
    email: string = ADMIN_SYSTEM_EMAIL,
    password: string = ADMIN_SEED_PASS,
): Promise<void> {
    await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(email);
    await page.locator('input[name="password"]').fill(password);
    await page.locator('#adminLoginForm button[type="submit"]').click();
    await page.waitForURL('**/admin');
}

/**
 * 신청 폼 3단계 위저드 (PR #75 F0c) 의 "다음" 버튼으로 스텝을 전환한다.
 * 비활성 스텝 카드는 display:none (.apply-step-card.is-active 만 노출) 이므로
 * 입력 전에 대상 스텝 카드가 실제로 표시될 때까지 명시적으로 대기한다.
 *
 * step 1: 신청자 정보 (readonly) / step 2: 지원 동기 #applyReason /
 * step 3: 개인정보 동의 + 제출 버튼 #applyNavSubmit
 */
export async function applyNextStep(page: Page, expectStep: 2 | 3): Promise<void> {
    // A형 flaky 방지: apply.html 인라인 IIFE 가 nextBtn.addEventListener('click', ...) 등록을
    // 완료했음을 보장. IIFE 마지막에 window.__applyReady = true 세팅.
    // (waitUntil:'domcontentloaded' 만으로는 CI 환경 script 실행 지연 시 부족)
    await page.locator('#applyNavNext').waitFor({ state: 'attached' });
    await page.waitForFunction(() => (window as unknown as { __applyReady?: boolean }).__applyReady === true);
    await page.locator('#applyNavNext').click();
    await expect(page.locator(`[data-step-card="${expectStep}"]`)).toBeVisible();
}

/**
 * HTMX 스크립트 로드 완료를 대기한다 (window.htmx 존재 확인).
 * HTMX 를 쓰는 페이지 (프로그램 목록 필터 등) 진입 직후 호출.
 */
export async function waitForHtmx(page: Page, timeoutMs: number = 10_000): Promise<void> {
    await page.waitForFunction(() => typeof (window as any).htmx !== 'undefined', {
        timeout: timeoutMs,
    });
}

/**
 * 페이지 타이틀이 정규식과 매칭될 때까지 assert.
 * beforeEach 안에서 페이지 이동 후 즉시 확인용.
 */
export async function expectTitle(page: Page, pattern: RegExp): Promise<void> {
    await expect(page).toHaveTitle(pattern);
}

/**
 * fix-e2e-seed-pollution: 테스트 전용 신청 정리 endpoint 호출.
 *
 * 배경: `--repeat-each=N` 반복 실행 또는 `webServer.reuseExistingServer=true` 로 상태가 남는 로컬 환경에서
 * 같은 seed 유저가 같은 프로그램을 재신청하면 `ApplicationService` 가 `IllegalStateException("이미 신청한 프로그램입니다.")`
 * 로 실패 → B형 flaky.
 *
 * 호출: e2e 프로파일 (bootrun-e2e.cmd 로 기동) 에서만 활성화된 `TestFixtureController` 로
 * POST /__test__/reset-applications. programId 를 지정하면 해당 프로그램 신청만, 생략하면 유저 전체 신청 삭제.
 *
 * @param page Playwright Page (request context 재사용)
 * @param opts.userEmail 필수 (seed유저 이메일)
 * @param opts.programId optional (지정 시 해당 프로그램 신청만 삭제)
 */
export async function resetApplications(
    page: Page,
    opts: { userEmail: string; programId?: number },
): Promise<void> {
    const response = await page.request.post('/__test__/reset-applications', {
        data: { userEmail: opts.userEmail, programId: opts.programId ?? null },
    });
    // 204 No Content 기대. 실패 시 스펙 실패로 즉시 노출 (조용히 continue 하지 않음).
    if (response.status() !== 204) {
        throw new Error(
            `resetApplications failed: status=${response.status()} body=${await response.text()}`,
        );
    }
}

/**
 * A-admin-notice-attachment seed-pollution 해소: 관리자 계정으로 생성된 임시 공지를 정리한다.
 *
 * 배경: admin-notice-form / admin-notice-upload / admin-notice-rbac spec 이 POST /admin/notices 로
 * 임시 공지를 생성하고 정리 없이 종료 → notices.spec.ts:78 페이지네이션 테스트가 오염된 12+N 상태로 실행되어
 * page 2 에 2건이 아니라 N+2건이 나오는 회귀 발생.
 *
 * 정책: TestFixtureController.resetNotices() 가 createdBy != sysadmin 인 공지만 삭제. 시드 12건은 보존.
 *
 * @param page Playwright Page (request context 재사용)
 */
export async function resetNotices(page: Page): Promise<void> {
    const response = await page.request.post('/__test__/reset-notices');
    if (response.status() !== 204) {
        throw new Error(
            `resetNotices failed: status=${response.status()} body=${await response.text()}`,
        );
    }
}

/**
 * A-admin-terms-crud (Qn-8 A, 2026-09-04): admin-term E2E 가 신규 약관을 생성 후 정리하지 않으면
 * 다음 실행에서 signup 회귀 및 목록 count 기대가 오염된다.
 * `TestFixtureController.resetTerms()` = id > SEED_TERM_COUNT 인 term + 관련 agreement 삭제.
 */
export async function resetTerms(page: Page): Promise<void> {
    const response = await page.request.post('/__test__/reset-terms');
    if (response.status() !== 204) {
        throw new Error(
            `resetTerms failed: status=${response.status()} body=${await response.text()}`,
        );
    }
}

/**
 * F0c-dynamic-fields (Qn-7 A, 2026-09-08): admin-dynamic-field E2E 가 신규 필드를 만든 후 정리하지 않으면
 * apply flow 렌더와 목록 count 기대가 오염된다.
 * `TestFixtureController.resetApplyQuestions()` = id > SEED_APPLY_QUESTION_COUNT (3) 인 필드 + 관련 응답 삭제.
 */
export async function resetApplyQuestions(page: Page): Promise<void> {
    const response = await page.request.post('/__test__/reset-apply-questions');
    if (response.status() !== 204) {
        throw new Error(
            `resetApplyQuestions failed: status=${response.status()} body=${await response.text()}`,
        );
    }
}

/**
 * A7-e2e-suite (2026-09-28): 3단계 apply 위저드를 한 번에 완주하는 헬퍼.
 * fan-out spec 이 여러 프로그램에 반복 신청할 때 반복 코드를 줄이기 위해.
 *
 * 전제: 이미 로그인 상태. 프로그램은 신청 가능(모집 중 등) 상태.
 * 완료 후 /apply/complete 로 리다이렉트.
 */
export async function applyProgram(
    page: Page,
    programId: number,
    reason: string = 'A7 e2e fan-out 시나리오 신청 사유입니다.',
): Promise<void> {
    await page.goto(`/programs/${programId}/apply`, { waitUntil: 'domcontentloaded' });
    await applyNextStep(page, 2);
    await page.locator('#applyReason').fill(reason);
    await applyNextStep(page, 3);
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await Promise.all([
        page.waitForURL(/\/apply\/complete/),
        page.locator('#applyNavSubmit').click(),
    ]);
}

/**
 * A7-e2e-suite (2026-09-28): admin 계정 unread 알림 초기화 (fan-out spec seed pollution 방지).
 *
 * fan-out spec 은 "신청 전 배지 값" → "신청 후 배지 값 = +1" 을 검증하므로 관측 기준선이 필요.
 * 이 헬퍼는 로그인 페이지 세션을 훔치지 않고 request context 만 사용해 mark-all-read POST 를 보낸다.
 *
 * 절차: admin login (별도 페이지) → POST /admin/notifications/mark-all-read → logout.
 * spec 내부 페이지는 사용자 세션이라 이 헬퍼 호출 이후에도 오염되지 않는다.
 */
export async function resetAdminNotifications(
    browser: import('@playwright/test').Browser,
    adminEmail: string,
    adminPassword: string = ADMIN_SEED_PASS,
): Promise<void> {
    const ctx = await browser.newContext();
    try {
        const page = await ctx.newPage();
        await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
        await page.locator('input[name="username"]').fill(adminEmail);
        await page.locator('input[name="password"]').fill(adminPassword);
        await page.locator('#adminLoginForm button[type="submit"]').click();
        await page.waitForURL('**/admin');
        // 헤더에 mark-all POST — HTMX 헤더 없이 폼-스타일 POST 시 302 redirect
        await page.request.post('/admin/notifications/mark-all-read');
    } finally {
        await ctx.close();
    }
}

/**
 * A7-e2e-suite (2026-09-28): 특정 admin 계정으로 프로그램 watch 를 등록한다 (fan-out watcher 축 검증용).
 * `POST /admin/programs/{programId}/watch/toggle` 는 상태 토글이라 이미 등록된 상태면 해제하므로,
 * 최종 상태를 등록으로 보장하기 위해 응답 fragment 의 클래스명으로 확인 후 필요 시 재호출한다.
 */
export async function ensureAdminWatches(
    browser: import('@playwright/test').Browser,
    adminEmail: string,
    programId: number,
    adminPassword: string = ADMIN_SEED_PASS,
): Promise<void> {
    const ctx = await browser.newContext();
    try {
        const page = await ctx.newPage();
        await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
        await page.locator('input[name="username"]').fill(adminEmail);
        await page.locator('input[name="password"]').fill(adminPassword);
        await page.locator('#adminLoginForm button[type="submit"]').click();
        await page.waitForURL('**/admin');

        // toggle 1회 → 응답 fragment 에 is-watched 여부 확인
        const first = await page.request.post(
            `/admin/programs/${programId}/watch/toggle`,
            { form: { styleClass: 'list-watch-btn' } },
        );
        const firstBody = await first.text();
        if (!firstBody.includes('is-watched')) {
            // 첫 호출이 해제였으므로 다시 눌러 등록 상태로 복귀
            await page.request.post(
                `/admin/programs/${programId}/watch/toggle`,
                { form: { styleClass: 'list-watch-btn' } },
            );
        }
    } finally {
        await ctx.close();
    }
}

/**
 * A3-1 admin-program-form (Qn-6 A, 2026-09-10): admin-program-form E2E 가 신규 프로그램을 생성 후
 * 정리하지 않으면 admin-programs-list 개수 기대 · apply flow 시드가 오염된다.
 * `TestFixtureController.resetPrograms()` = id > SEED_PROGRAM_COUNT (23) 인 프로그램 + FK cascade 삭제.
 */
export async function resetPrograms(page: Page): Promise<void> {
    const response = await page.request.post('/__test__/reset-programs');
    if (response.status() !== 204) {
        throw new Error(
            `resetPrograms failed: status=${response.status()} body=${await response.text()}`,
        );
    }
}
