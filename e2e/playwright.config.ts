import { defineConfig, devices } from '@playwright/test';

/**
 * youth-moa-java Playwright 설정.
 * - `webServer` 로 bootRun 자동 기동 지원. 이미 로컬에서 서버가 떠 있으면 재사용.
 * - 회사 PC 의 SSL 프록시 환경에 맞춰 ignoreHTTPSErrors: true.
 */
const isWindows = process.platform === 'win32';
const gradleCmd = isWindows ? 'gradlew.bat bootRun' : './gradlew bootRun';

/**
 * BASE_URL 환경변수 지원 (기본 8080).
 * - CI (e2e-playwright.yml) 는 이미 BASE_URL=http://localhost:8080 을 넘기고 있음
 * - 회사 PC 로컬 재현: bootrun-e2e.cmd 가 8090 에 기동 → BASE_URL=http://localhost:8090
 */
const baseURL = process.env.BASE_URL ?? 'http://localhost:8080';

export default defineConfig({
    testDir: './tests',
    timeout: 60_000,                    // 30s → 60s (회사 PC SSL 프록시 + 시드 부담 회피)
    expect: { timeout: 5_000 },
    fullyParallel: false,               // 5 worker 동시 진입 시 DB 부담 → 순차 실행
    workers: 1,
    reporter: [['list'], ['html', { open: 'never' }]],
    use: {
        baseURL,
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
        url: `${baseURL}/login`,
        reuseExistingServer: true,
        timeout: 180_000,               // Boot 4 + 시드 초기화 최대 2~3분 (JDK cold start 포함)
        stdout: 'pipe',
        stderr: 'pipe',
    },
    /**
     * 프로젝트 2분할 (2026-07-28).
     *
     * - `chromium`  : 기능 E2E. **green 유지가 원칙** — 여기가 red 면 회귀가 가려진다
     *                 (2026-07-13 E2E red 방치가 신규 회귀 5건을 마스킹한 사고).
     * - `contracts` : 디자인 계약 검사 (visual-*.spec.ts). prototype 대비 정량 갭을 리포트하는 게
     *                 목적이라 **갭이 남아 있는 동안은 의도적으로 red** 다. 기능 E2E 와 섞으면
     *                 위 원칙이 무너지므로 분리하고, CI 에서는 non-blocking 으로 돌린다.
     *
     * 실행:
     *   npx playwright test --project=chromium    # 기능 E2E (블로킹)
     *   npx playwright test --project=contracts   # 디자인 계약 (논블로킹)
     */
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
            testIgnore: /visual-.*\.spec\.ts$/,
        },
        {
            name: 'contracts',
            use: { ...devices['Desktop Chrome'] },
            testMatch: /visual-.*\.spec\.ts$/,
        },
    ],
});
