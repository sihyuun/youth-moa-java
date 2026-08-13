import { test } from '@playwright/test';
import { notificationsContract } from '../contracts/notifications';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, login, seedEmail } from '../helpers';

/**
 * 알림 목록 화면 디자인 계약 실행 (인증 필요).
 *
 * seed 알림: DataInitializer L182~203 에서 사용자당 4건 시드 (일부 unread=true).
 *   → unread-badge · mark-all-btn · notif-item--unread 클래스가 노출된다.
 *   seed 이메일은 apply.spec.ts 와 동일하게 seed30 사용. 알림은 사용자 종속이 아닌 공통 시드라
 *   어떤 계정으로 로그인해도 unread 상태가 잔존하는지 사전 확인 필요 — 만약 unread=0 계정만
 *   존재하면 unread-badge/mark-all 계약이 지속 실패하므로 rotation pool 도입 검토.
 *
 * side-effect: GET 렌더만 수행. read-all/read-one POST 는 트리거하지 않음 → seed 오염 없음.
 *
 * 진입 순서:
 *   1) helpers.login → "/" 이동 완료 대기
 *   2) /notifications 로 이동
 *   3) 전체 필터 상태 (default) 로 계약 검사
 */
test('알림 목록 화면 디자인 계약 — 로그인 상태 · 전체 필터', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: notificationsContract.viewport.width,
        height: notificationsContract.viewport.height,
    });
    await login(page, seedEmail(30));
    await page.goto(notificationsContract.path, { waitUntil: 'domcontentloaded' });
    const auth = await runContract(page, notificationsContract, 'auth');
    writeGapReport(notificationsContract, { auth });
});
