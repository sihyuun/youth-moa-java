import { test } from '@playwright/test';
import { mypageProfileEditContract } from '../contracts/mypage-profile-edit';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, login, seedEmail, seedPassword } from '../helpers';

/**
 * 개인정보 수정 Step2 (`/mypage/profile/edit`) 디자인 계약 실행.
 *
 * ─────────────────────────────────────────────────────────
 * 세션 flag 세팅 (진입 조건)
 * ─────────────────────────────────────────────────────────
 * Step2 는 세션에 `mypageProfileVerifiedAt` (TTL 10 분) 이 있어야 진입 허용.
 * 없으면 GET /mypage/profile/edit → redirect /mypage?tab=profile 로 되돌아감.
 *
 * 계약 실행 순서:
 *   1) helpers.login → 인증 쿠키 획득
 *   2) POST /mypage/profile/verify (seed 계정 비밀번호) → 세션 flag 부여
 *      - 실패 시 redirect 되어 Step2 진입 불가 → 계약 검사 실패로 노출
 *   3) GET /mypage/profile/edit → 폼 렌더
 *   4) runContract 로 검사 실행 + 리포트 저장
 *
 * side-effect: verify POST 는 세션 flag 만 세팅하고 사용자 필드는 변경하지 않으므로
 *   seed rotation 불필요. 저장(POST /mypage/profile) · 탈퇴(POST /mypage/withdraw) 는
 *   호출하지 않음.
 *
 * helpers.seedPassword: seedEmail 과 페어링된 시드 계정 비밀번호 반환.
 *   helpers.ts 에 export 되어 있지 않으면 impl 단계에서 도입 필요 → 임시로 로그인 후
 *   form submit 로 트리거하는 fallback 유지.
 */
test('개인정보 수정 Step2 디자인 계약 — 세션 verify 통과 후 편집 폼', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: mypageProfileEditContract.viewport.width,
        height: mypageProfileEditContract.viewport.height,
    });
    // seed1 사용 — 성별 시드가 있어야 gender.pill.active.svg 검사가 매칭 (is-on 상태 발생).
    const email = seedEmail(1);
    await login(page, email);

    // Step1 폼으로 이동 → 폼 submit 으로 세션 flag 부여.
    // seedPassword 가 helpers 에 없으면 impl 단계에서 도입. 임시 상수 사용.
    await page.goto('/mypage?tab=profile', { waitUntil: 'domcontentloaded' });
    const password =
        (typeof seedPassword === 'function' ? seedPassword(30) : undefined) ?? 'Passw0rd!';
    await page.fill('#verifyPassword', password);
    await Promise.all([
        page.waitForURL(/\/mypage\/profile\/edit/, { waitUntil: 'domcontentloaded' }),
        page.click('.mypage-verify-form button[type="submit"]'),
    ]);

    const auth = await runContract(page, mypageProfileEditContract, 'auth');
    writeGapReport(mypageProfileEditContract, { auth });
});
