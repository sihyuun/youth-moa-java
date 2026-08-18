/**
 * 디자인 계약 러너.
 *
 * 계약 파일의 모든 항목을 실제 렌더 결과와 대조하고, **soft assertion** 으로 보고한다.
 * soft 를 쓰는 이유: 첫 실패에서 멈추면 "갭 1건" 만 보이지만, 우리에게 필요한 건
 * 한 번 실행에 나오는 **전체 갭 목록** 이기 때문. 이 출력이 곧 갭 리포트다.
 *
 * 배경·사용법: docs/design-contracts/README.md
 */

import { expect, type Page } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import type { AuthState, Check, ScreenContract } from './types';

export interface CheckResult {
    check: Check;
    pass: boolean;
    actual: string;
}

/** 텍스트 비교용 정규화 — 줄바꿈은 보존하되 좌우 공백·중복 공백 정리 */
function normalizeText(s: string): string {
    return s
        .split('\n')
        .map(line => line.replace(/\s+/g, ' ').trim())
        .filter(line => line.length > 0)
        .join('\n');
}

async function measure(page: Page, check: Check): Promise<string> {
    const locator = page.locator(check.selector);

    if (check.kind === 'count') {
        return String(await locator.count());
    }

    if (check.kind === 'exists') {
        return String((await locator.count()) > 0);
    }

    if ((await locator.count()) === 0) {
        return '(요소 없음)';
    }

    const first = locator.first();

    if (check.kind === 'box') {
        const box = await first.boundingBox();
        if (!box) return '(보이지 않음)';
        const value = check.prop === 'height' ? box.height : box.width;
        return String(Math.round(value * 100) / 100);
    }

    if (check.kind === 'text') {
        return normalizeText(await first.innerText());
    }

    // kind === 'css'
    const prop = check.prop!;
    return await first.evaluate(
        (el, p) => window.getComputedStyle(el).getPropertyValue(p).trim(),
        prop,
    );
}

function isPass(check: Check, actual: string): boolean {
    if (check.kind === 'box') {
        const expected = Number(check.expected);
        const value = Number(actual);
        if (Number.isNaN(value)) return false;
        return Math.abs(value - expected) <= (check.tolerance ?? 1);
    }
    return actual === String(check.expected);
}

/**
 * 계약의 한 상태(anon/auth) 분을 측정만 수행 (assert 없음).
 * 여러 페이지/탭을 순회하며 결과를 병합해야 하는 경우 사용.
 */
export async function collectContract(
    page: Page,
    contract: ScreenContract,
    state: AuthState,
): Promise<CheckResult[]> {
    const targets = contract.checks.filter(
        c => (c.states ?? ['anon']).includes(state) && !c.deviation && !c.deferred,
    );
    const results: CheckResult[] = [];
    for (const check of targets) {
        const actual = await measure(page, check);
        results.push({ check, pass: isPass(check, actual), actual });
    }
    return results;
}

/** 결과 배열에 대해 soft assertion 만 수행. collectContract 병합 후 최종 관문에서 사용. */
export function assertResults(results: CheckResult[]): void {
    for (const r of results) {
        expect
            .soft(
                r.actual,
                `[${r.check.severity}] ${r.check.id} — ${r.check.desc} (출처 ${r.check.proto})`,
            )
            .toBe(
                r.check.kind === 'box'
                    ? r.pass
                        ? r.actual
                        : String(r.check.expected)
                    : String(r.check.expected),
            );
    }
}

/**
 * 계약의 한 상태(anon/auth) 분을 실행한다.
 * 페이지는 호출자가 미리 목표 경로로 이동시켜 둔 상태여야 한다.
 */
export async function runContract(
    page: Page,
    contract: ScreenContract,
    state: AuthState,
): Promise<CheckResult[]> {
    const results = await collectContract(page, contract, state);
    assertResults(results);
    return results;
}

/** 마크다운 표 셀로 안전하게 만든다 (줄바꿈·파이프가 표를 깨뜨리지 않도록) */
function cell(value: string | number | boolean): string {
    const text = String(value).replace(/\n/g, ' ⏎ ').replace(/\|/g, '\\|');
    return `\`${text}\``;
}

/** 결과를 사람이 읽는 마크다운 갭 리포트로 저장 */
export function writeGapReport(
    contract: ScreenContract,
    byState: Record<string, CheckResult[]>,
): string {
    const lines: string[] = [];
    lines.push(`# 디자인 계약 검사 — ${contract.screen} (\`${contract.path}\`)`);
    lines.push('');
    lines.push(`> 계약 출처: ${contract.source}`);
    lines.push(`> 뷰포트: ${contract.viewport.width}x${contract.viewport.height}`);
    lines.push('');

    let total = 0;
    let failed = 0;

    for (const [state, results] of Object.entries(byState)) {
        if (results.length === 0) continue;
        const fails = results.filter(r => !r.pass);
        total += results.length;
        failed += fails.length;

        lines.push(`## ${state === 'anon' ? '비로그인' : '로그인'} — ${results.length - fails.length}/${results.length} 통과`);
        lines.push('');

        if (fails.length === 0) {
            lines.push('갭 없음.');
            lines.push('');
            continue;
        }

        lines.push('| 심각도 | 항목 | 기대 (prototype) | 실제 | 출처 |');
        lines.push('|---|---|---|---|---|');
        for (const r of fails.sort((a, b) => a.check.severity.localeCompare(b.check.severity))) {
            lines.push(
                `| ${r.check.severity} | \`${r.check.id}\` — ${r.check.desc} | ${cell(r.check.expected)} | ${cell(r.actual)} | ${r.check.proto} |`,
            );
        }
        lines.push('');
    }

    // 의도적 이탈 — 검사에서 뺐다는 사실 자체가 보이도록 항상 표기한다
    const deviations = contract.checks.filter(c => c.deviation);
    if (deviations.length > 0) {
        lines.push(`## 의도적 이탈 (검사 제외) — ${deviations.length}건`);
        lines.push('');
        lines.push('| 항목 | prototype 기대값 | 이탈 사유 |');
        lines.push('|---|---|---|');
        for (const c of deviations) {
            lines.push(`| \`${c.id}\` — ${c.desc} | ${cell(c.expected)} | ${c.deviation} |`);
        }
        lines.push('');
    }

    // 이월 — 맞출 예정이지만 이번 범위가 아닌 항목. 담당 티켓과 함께 항상 표기한다
    const deferred = contract.checks.filter(c => c.deferred && !c.deviation);
    if (deferred.length > 0) {
        lines.push(`## 이월 (검사 제외 · 맞출 예정) — ${deferred.length}건`);
        lines.push('');
        lines.push('| 항목 | prototype 기대값 | 담당 |');
        lines.push('|---|---|---|');
        for (const c of deferred) {
            lines.push(`| \`${c.id}\` — ${c.desc} | ${cell(c.expected)} | ${c.deferred} |`);
        }
        lines.push('');
    }

    lines.push('---');
    lines.push('');
    lines.push(
        `**합계: ${total - failed}/${total} 통과 · 갭 ${failed}건**` +
            (deviations.length > 0 ? ` · 의도적 이탈 ${deviations.length}건` : '') +
            (deferred.length > 0 ? ` · 이월 ${deferred.length}건` : '') +
            (deviations.length + deferred.length > 0 ? ' (검사 제외)' : ''),
    );
    lines.push('');

    const body = lines.join('\n');
    // `test-results/` 에 쓰면 안 된다 — Playwright 가 매 실행 시작 시 그 디렉토리를 통째로 지우므로
    // 스펙을 하나씩 돌릴 때마다 다른 화면의 리포트가 사라진다 (2026-07-28 실측).
    const outDir = path.join(__dirname, '..', 'gap-reports');
    fs.mkdirSync(outDir, { recursive: true });
    fs.writeFileSync(path.join(outDir, `gap-${contract.screen}.md`), body, 'utf-8');
    return body;
}
