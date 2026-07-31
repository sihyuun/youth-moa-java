/**
 * 인터랙션 계약 러너.
 *
 * 시각 계약 러너(`runner.ts`)와 유사한 구조. 각 interaction 을 순회하며 클릭 → 기대 결과 검증.
 */

import { expect, type Page } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import type { AuthState, Interaction, InteractionContract } from './interactions-types';

export interface InteractionResult {
    interaction: Interaction;
    pass: boolean;
    actual: string;
}

async function runOne(page: Page, base: string, ix: Interaction): Promise<InteractionResult> {
    // 1) 시작 페이지로 이동
    await page.goto(base + ix.startPath, { waitUntil: 'domcontentloaded' });

    // 2) 요소 존재 확인
    const locator = page.locator(ix.selector);
    if ((await locator.count()) === 0) {
        return { interaction: ix, pass: false, actual: '(대상 요소 없음)' };
    }

    // 3) 클릭 + 결과 관찰
    const before = page.url();

    if (ix.expected.kind === 'navigate') {
        // waitForURL 은 클릭 유발 후 이동을 기다린다
        try {
            await Promise.all([
                page.waitForURL(ix.expected.toPattern, { timeout: 5_000 }),
                locator.first().click(),
            ]);
            const after = page.url();
            return { interaction: ix, pass: true, actual: after };
        } catch {
            return { interaction: ix, pass: false, actual: page.url() };
        }
    }

    // stay: 클릭 후 URL 이 변하지 않아야 함
    await locator.first().click().catch(() => {});
    await page.waitForTimeout(300);
    const after = page.url();
    const stayed = after === before;
    return { interaction: ix, pass: stayed, actual: stayed ? '(이동 없음)' : after };
}

export async function runInteractionContract(
    page: Page,
    baseUrl: string,
    contract: InteractionContract,
    auth: AuthState = 'anon',
): Promise<InteractionResult[]> {
    const results: InteractionResult[] = [];
    for (const ix of contract.interactions) {
        if (ix.deviation) continue;
        const need = ix.auth ?? 'anon';
        if (need !== auth) continue;
        results.push(await runOne(page, baseUrl, ix));
    }

    for (const r of results) {
        const label = `[${r.interaction.severity}] ${r.interaction.id} — ${r.interaction.desc}`;
        expect.soft(r.pass, label).toBe(true);
    }
    return results;
}

/** 마크다운 셀 안전 이스케이프 */
function cell(v: string | number | boolean): string {
    return `\`${String(v).replace(/\n/g, ' ⏎ ').replace(/\|/g, '\\|')}\``;
}

export function writeInteractionReport(
    contract: InteractionContract,
    byState: Record<string, InteractionResult[]>,
): void {
    const lines: string[] = [];
    lines.push(`# 인터랙션 계약 검사 — ${contract.name}`);
    lines.push('');
    lines.push(`> ${contract.description}`);
    lines.push('');

    let total = 0;
    let failed = 0;
    for (const [state, results] of Object.entries(byState)) {
        if (results.length === 0) continue;
        const fails = results.filter(r => !r.pass);
        total += results.length;
        failed += fails.length;
        lines.push(
            `## ${state === 'anon' ? '비로그인' : '로그인'} — ${results.length - fails.length}/${results.length} 통과`,
        );
        lines.push('');
        if (fails.length === 0) {
            lines.push('갭 없음.');
            lines.push('');
            continue;
        }
        lines.push('| 심각도 | 항목 | 기대 | 실제 | 근거 |');
        lines.push('|---|---|---|---|---|');
        for (const r of fails) {
            const exp =
                r.interaction.expected.kind === 'navigate'
                    ? `이동 → ${String(r.interaction.expected.toPattern)}`
                    : '이동 없음 (stay)';
            lines.push(
                `| ${r.interaction.severity} | \`${r.interaction.id}\` — ${r.interaction.desc} | ${cell(exp)} | ${cell(r.actual)} | ${r.interaction.proto ?? '-'} |`,
            );
        }
        lines.push('');
    }
    lines.push('---');
    lines.push('');
    lines.push(`**합계: ${total - failed}/${total} 통과 · 갭 ${failed}건**`);
    lines.push('');

    const outDir = path.join(__dirname, '..', 'gap-reports');
    fs.mkdirSync(outDir, { recursive: true });
    fs.writeFileSync(path.join(outDir, `interactions-${contract.name}.md`), lines.join('\n'), 'utf-8');
}
