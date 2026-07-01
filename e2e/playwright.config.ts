import { defineConfig, devices } from '@playwright/test';

/**
 * youth-moa-java Playwright 설정.
 * - bootRun (port 8080) 이 동작 중이라고 가정. webServer 자동 기동은 후속에서 검토.
 * - 회사 PC 의 SSL 프록시 환경에 맞춰 ignoreHTTPSErrors: true.
 */
export default defineConfig({
    testDir: './tests',
    timeout: 60_000,                    // 30s → 60s (회사 PC SSL 프록시 + 시드 부담 회피)
    expect: { timeout: 5_000 },
    fullyParallel: false,               // 5 worker 동시 진입 시 DB 부담 → 순차 실행
    workers: 1,
    reporter: [['list'], ['html', { open: 'never' }]],
    use: {
        baseURL: 'http://localhost:8080',
        ignoreHTTPSErrors: true,
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
        },
    ],
});
