#!/usr/bin/env node
/**
 * prototype.tsx 액션 목적지 감사 스크립트 (2026-07-31 신설).
 *
 * 목적: prototype 의 `go('screen')`, `<a href="X">`, `onClick={()=>...}` 형태의 액션 목적지를 파싱해 목록화.
 *  → 인터랙션 계약 (`e2e/contracts/interactions-*.ts`) 작성 시 참고 자료로 사용.
 *  → 구현과 대조 (수동) 로 라우팅 불일치 조기 감지 (예: 2026-07-31 profile-edit → /find-password 사고).
 *
 * 실행:
 *   node tools/audit-prototype-actions.mjs
 *   → tools/audit-out/prototype-actions.json 산출
 *
 * MVP 스코프: 정규식 기반 단순 파싱. AST 파싱은 미도입 (재발 시 tsx-morph 로 승격).
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO = path.resolve(__dirname, '..');
const PROTOTYPE = path.join(REPO, 'docs/00_assets/prototype.tsx');
const OUT_DIR = path.join(__dirname, 'audit-out');

/**
 * 파일에서 라인 번호와 함께 정규식 매칭 결과 추출.
 * @param {string} src
 * @param {RegExp} re — 반드시 global flag
 * @param {(m: RegExpExecArray, lineNo: number) => object} pick
 */
function scan(src, re, pick) {
    const results = [];
    const lines = src.split('\n');
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        re.lastIndex = 0;
        let m;
        while ((m = re.exec(line)) !== null) {
            results.push({ ...pick(m, i + 1), line: line.trim() });
        }
    }
    return results;
}

function main() {
    if (!fs.existsSync(PROTOTYPE)) {
        console.error(`prototype not found: ${PROTOTYPE}`);
        process.exit(1);
    }
    const src = fs.readFileSync(PROTOTYPE, 'utf-8');

    // go('screen') 호출 — 프로토타입 라우팅 함수
    const goCalls = scan(src, /go\(['"](\w+)['"]\)/g, (m, ln) => ({
        kind: 'go',
        target: m[1],
        location: `L${ln}`,
    }));

    // <a href="..."> — 실 링크
    const hrefs = scan(src, /href=(?:\{)?['"](\S+?)['"]/g, (m, ln) => ({
        kind: 'href',
        target: m[1],
        location: `L${ln}`,
    }));

    // onClick={() => X}, onClick={handleY} 형태의 함수 참조
    const onClicks = scan(
        src,
        /onClick=\{[^}]*(?:go\(['"](\w+)['"]\)|(\w+)\(\))[^}]*\}/g,
        (m, ln) => ({
            kind: 'onClick',
            target: m[1] || m[2] || '(inline)',
            location: `L${ln}`,
        }),
    );

    const summary = {
        source: PROTOTYPE.replace(REPO + path.sep, ''),
        generatedAt: new Date().toISOString(),
        totals: {
            goCalls: goCalls.length,
            hrefs: hrefs.length,
            onClicks: onClicks.length,
        },
        goCalls,
        hrefs,
        onClicks,
    };

    fs.mkdirSync(OUT_DIR, { recursive: true });
    const out = path.join(OUT_DIR, 'prototype-actions.json');
    fs.writeFileSync(out, JSON.stringify(summary, null, 2), 'utf-8');

    // 콘솔 요약
    console.log(`Wrote ${out}`);
    console.log(`  go() calls    : ${goCalls.length}`);
    console.log(`  href links    : ${hrefs.length}`);
    console.log(`  onClick refs  : ${onClicks.length}`);

    // go() 대상별 count top 10
    const goFreq = {};
    for (const g of goCalls) goFreq[g.target] = (goFreq[g.target] || 0) + 1;
    const top = Object.entries(goFreq)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10);
    console.log('\n  top go() targets:');
    for (const [t, n] of top) console.log(`    ${t.padEnd(20)} ${n}`);
}

main();
