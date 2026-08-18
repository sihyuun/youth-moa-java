import { test } from '@playwright/test';
import { mypageContract } from '../contracts/mypage';
import { assertResults, collectContract, writeGapReport, type CheckResult } from '../contracts/runner';
import { abortExternal, login, seedEmail } from '../helpers';

/**
 * 마이페이지 4탭 디자인 계약 실행 (인증 필요).
 *
 * 4탭을 순회 로드하며 매 탭에서 계약을 실행한 뒤, 항목별 최적 결과를 병합해서
 * writeGapReport 한 번으로 최종 리포트를 남긴다. (탭별 셀렉터가 분리돼 있어
 * 한 탭에서 (요소 없음) 이어도 다른 탭에서 통과할 수 있다.)
 *
 * seed 전제:
 *  - seed1 은 program[0] APPROVED + program[1] PENDING + 관심 지역/분야 시드 보유.
 *  - noti/profile 탭의 필드는 모든 seed 계정에 렌더된다.
 */
test('마이페이지 4탭 디자인 계약 — 로그인 상태 순회', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: mypageContract.viewport.width,
        height: mypageContract.viewport.height,
    });
    // seed1 사용 근거: 관심 지역·분야 시드가 있고 (interest chip 검사) + program[0] APPROVED /
    // program[1] PENDING 신청 이력이 있음 (history 카드 · 상태 뱃지). seed30 은 신청·관심 모두 없음.
    await login(page, seedEmail(1));

    const tabs = ['history', 'favorites', 'noti', 'profile'] as const;
    // id → 병합 결과 (pass 인 결과가 있으면 pass 우선 채택)
    const merged = new Map<string, CheckResult>();

    for (const tab of tabs) {
        const url = tab === 'history' ? mypageContract.path : `${mypageContract.path}?tab=${tab}`;
        await page.goto(url, { waitUntil: 'domcontentloaded' });
        const results = await collectContract(page, mypageContract, 'auth');
        for (const r of results) {
            const prev = merged.get(r.check.id);
            // 규칙: 한 탭이라도 pass → pass. 모든 탭이 fail 이면 첫 fail 결과 유지.
            if (!prev || (!prev.pass && r.pass)) merged.set(r.check.id, r);
        }
    }

    const finalResults = Array.from(merged.values());
    writeGapReport(mypageContract, { auth: finalResults });
    assertResults(finalResults);
});
