/**
 * 인터랙션 계약 검사 스펙.
 *
 * `--project=contracts` 로 실행된다. 시각 계약 (visual-*.spec.ts) 과 동일 프로젝트에 배치해 함께 관리.
 */

import { test } from '@playwright/test';
import { userInteractions } from '../contracts/interactions-user';
import { runInteractionContract, writeInteractionReport } from '../contracts/interactions-runner';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8080';

test('사용자 화면 인터랙션 계약 — 비로그인', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    const anon = await runInteractionContract(page, BASE_URL, userInteractions, 'anon');
    writeInteractionReport(userInteractions, { anon });
});
