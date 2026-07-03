import { defineConfig, devices } from '@playwright/test';

/**
 * youth-moa-java Playwright 설정.
 * - `webServer` 로 bootRun 자동 기동 지원. 이미 로컬에서 서버가 떠 있으면 재사용.
 * - 회사 PC 의 SSL 프록시 환경에 맞춰 ignoreHTTPSErrors: true.
 */
const isWindows = process.platform === 'win32';
const gradleCmd = isWindows ? 'gradlew.bat bootRun' : './gradlew bootRun';

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
    /**
     * bootRun 자동 기동. reuseExistingServer=true 라 이미 8080 이 떠 있으면 그대로 사용.
     * CI 및 새 개인 PC 셋업에서 "playwright 실행 전에 bootRun 을 별도 터미널로 띄워야 한다" 는 제약을 제거.
     * cwd 는 repo 루트 (e2e 상위) 를 가리키도록 조정.
     */
    webServer: {
        command: gradleCmd,
        cwd: '..',
        // actuator 미도입 상태 → 로그인 페이지 (permitAll, 인증 불필요) 로 readiness 체크
        url: 'http://localhost:8080/login',
        reuseExistingServer: true,
        timeout: 180_000,               // Boot 4 + 시드 초기화 최대 2~3분 (JDK cold start 포함)
        stdout: 'pipe',
        stderr: 'pipe',
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
        },
    ],
});
