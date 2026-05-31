// scripts/e2e-diagnose/runner.ts
import { chromium, Browser, Page, BrowserContext } from '@playwright/test';
import { collectAll } from './collectors';
import { runScenario } from './scenarios';
import * as fs from 'fs/promises';
import * as path from 'path';

interface NetworkEntry {
    type: 'REQUEST' | 'RESPONSE';
    method?: string;
    status?: number;
    url: string;
    headers: Record<string, string>;
    timestamp: string;
}

const TARGET_URL = process.env.E2E_TARGET_URL ?? 'https://ssac.io';
const API_URL    = process.env.E2E_API_URL    ?? 'https://api.ssac.io';
const OUTPUT_DIR = path.join(__dirname, 'output');

async function diagnose(): Promise<void> {
    const browser: Browser = await chromium.launch({ headless: true });

    const context: BrowserContext = await browser.newContext({
        recordHar: {
            path: path.join(OUTPUT_DIR, 'network.har'),
            mode: 'full',
        },
    });

    const page: Page = await context.newPage();

    // Console Log 수집
    const consoleLogs: string[] = [];
    page.on('console', msg => {
        consoleLogs.push(`[${msg.type().toUpperCase()}] ${msg.text()}`);
    });

    // Network 요청/응답 수집
    const networkTrace: NetworkEntry[] = [];
    page.on('request', req => {
        networkTrace.push({
            type      : 'REQUEST',
            method    : req.method(),
            url       : req.url(),
            headers   : req.headers(),
            timestamp : new Date().toISOString(),
        });
    });
    page.on('response', res => {
        networkTrace.push({
            type      : 'RESPONSE',
            status    : res.status(),
            url       : res.url(),
            headers   : res.headers(),
            timestamp : new Date().toISOString(),
        });
    });

    try {
        const scenario = process.env.E2E_SCENARIO ?? 'auth';
        console.log(`\n🚀 E2E 진단 시작 — 시나리오: ${scenario}`);
        console.log(`   대상: ${TARGET_URL}`);

        await runScenario(page, scenario, TARGET_URL);

        const result = await collectAll(page, context, consoleLogs, networkTrace);

        // 결과 JSON 저장
        await fs.writeFile(
            path.join(OUTPUT_DIR, 'result.json'),
            JSON.stringify(result, null, 2),
            'utf-8',
        );

        // AI 추론용 출력
        printForAI(result);

    } finally {
        await context.close();
        await browser.close();
    }
}

function printForAI(result: Record<string, unknown>): void {
    const cookies   = result.cookies as Record<string, Record<string, unknown>>;
    const network   = result.networkTrace as Array<Record<string, unknown>>;
    const consoleLogs = result.consoleLogs as string[];
    const railwayLog  = result.railwayLog  as string[];

    const refreshToken = cookies['refresh_token'];
    const statusEntry  = network.find(e =>
        typeof e.url === 'string' && e.url.includes('/auth/status') && e.type === 'RESPONSE',
    );
    const meEntry = network.find(e =>
        typeof e.url === 'string' && e.url.includes('/users/me') && e.type === 'RESPONSE',
    );
    const setCookieEntries = network.filter(e =>
        e.type === 'RESPONSE' &&
        e.headers && (e.headers as Record<string, string>)['set-cookie'],
    );

    console.log('\n=== E2E 진단 결과 ===');
    console.log(`시각: ${result.timestamp}`);
    console.log(`대상: ${result.url}`);

    console.log('\n--- Cookie ---');
    if (refreshToken) {
        console.log(`refresh_token : 존재`);
        console.log(`  domain  : ${refreshToken.domain}`);
        console.log(`  secure  : ${refreshToken.secure}`);
        console.log(`  httpOnly: ${refreshToken.httpOnly}`);
        console.log(`  sameSite: ${refreshToken.sameSite ?? '없음'}`);
    } else {
        console.log('refresh_token : 없음');
    }
    console.log('access_token  : 없음 (메모리 저장이므로 Cookie에 없어야 정상)');

    console.log('\n--- API 호출 결과 ---');
    console.log(`GET /api/v1/auth/status : ${statusEntry?.status ?? '호출 없음'}`);
    console.log(`GET /api/v1/users/me   : ${meEntry?.status ?? '호출 없음'}`);

    console.log('\n--- Network Set-Cookie ---');
    if (setCookieEntries.length > 0) {
        setCookieEntries.forEach(e => {
            const h = e.headers as Record<string, string>;
            console.log(`  ${e.url} → ${h['set-cookie']?.slice(0, 120)}...`);
        });
    } else {
        console.log('  없음');
    }

    console.log('\n--- Console Log (오류만) ---');
    const errorLogs = consoleLogs.filter(l => l.startsWith('[ERROR]') || l.startsWith('[WARN]'));
    if (errorLogs.length > 0) {
        errorLogs.forEach(l => console.log(`  ${l}`));
    } else {
        console.log('  오류 없음');
    }

    console.log('\n--- Railway Log (오류만) ---');
    const railwayErrors = railwayLog.filter(l =>
        l.toLowerCase().includes('error') || l.toLowerCase().includes('exception'),
    );
    if (railwayErrors.length > 0) {
        railwayErrors.slice(0, 10).forEach(l => console.log(`  ${l}`));
    } else {
        console.log('  오류 없음');
    }

    console.log('\n결과 저장: scripts/e2e-diagnose/output/result.json');
    console.log('스크린샷: scripts/e2e-diagnose/output/screenshot.png\n');
}

diagnose().catch(err => {
    console.error('E2E 진단 실패:', err);
    process.exit(1);
});
