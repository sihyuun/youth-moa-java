import { test } from '@playwright/test';
import { adminShellContract } from '../contracts/admin-shell';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 shell 다크 헤더 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminShellContract.viewport.width,
        height: adminShellContract.viewport.height,
    });
    await loginAdmin(page);
    // 계약 checks 는 default state='anon' 이지만 페이지는 이미 인증 상태로 진입해 있어 무관.
    // state 라벨은 필터링용이지 인증 컨텍스트 자체가 아니다.
    const anon = await runContract(page, adminShellContract, 'anon');
    writeGapReport(adminShellContract, { anon });
});
