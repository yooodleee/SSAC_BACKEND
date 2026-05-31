// scripts/e2e-diagnose/collectors/index.ts
import { Page, BrowserContext } from '@playwright/test';
import { execSync } from 'child_process';
import * as path from 'path';

interface NetworkEntry {
    type     : 'REQUEST' | 'RESPONSE';
    method?  : string;
    status?  : number;
    url      : string;
    headers  : Record<string, string>;
    timestamp: string;
}

interface CookieInfo {
    value   : string;
    domain  : string;
    path    : string;
    secure  : boolean;
    httpOnly: boolean;
    sameSite: string | undefined;
    expires : number;
}

interface DiagnosisResult {
    timestamp      : string;
    url            : string;
    cookies        : Record<string, CookieInfo>;
    localStorage   : Record<string, string>;
    sessionStorage : Record<string, string>;
    networkTrace   : NetworkEntry[];
    consoleLogs    : string[];
    railwayLog     : string[];
    screenshotPath : string;
}

export async function collectAll(
    page: Page,
    context: BrowserContext,
    consoleLogs: string[],
    networkTrace: NetworkEntry[],
): Promise<DiagnosisResult> {

    // 1. Cookie 수집 (민감 값은 [존재] / [없음]으로만 표시)
    const cookies = await context.cookies();
    const cookieMap: Record<string, CookieInfo> = {};
    for (const c of cookies) {
        cookieMap[c.name] = {
            value   : c.value ? '[존재]' : '[없음]',
            domain  : c.domain,
            path    : c.path,
            secure  : c.secure,
            httpOnly: c.httpOnly,
            sameSite: c.sameSite,
            expires : c.expires,
        };
    }

    // 2. LocalStorage 수집 (토큰 키는 [MASKED])
    const localStorage = await page.evaluate((): Record<string, string> => {
        const items: Record<string, string> = {};
        for (let i = 0; i < window.localStorage.length; i++) {
            const key = window.localStorage.key(i) ?? '';
            const value = window.localStorage.getItem(key) ?? '';
            items[key] = key.toLowerCase().includes('token')
                ? '[MASKED]'
                : value.slice(0, 100);
        }
        return items;
    });

    // 3. SessionStorage 수집 (토큰 키는 [MASKED])
    const sessionStorage = await page.evaluate((): Record<string, string> => {
        const items: Record<string, string> = {};
        for (let i = 0; i < window.sessionStorage.length; i++) {
            const key = window.sessionStorage.key(i) ?? '';
            const value = window.sessionStorage.getItem(key) ?? '';
            items[key] = key.toLowerCase().includes('token')
                ? '[MASKED]'
                : value.slice(0, 100);
        }
        return items;
    });

    // 4. 인증 관련 Network Trace 필터링
    const authNetworkTrace = networkTrace.filter(entry =>
        entry.url.includes('/api/v1/auth') ||
        entry.url.includes('/api/v1/users') ||
        entry.status === 401 ||
        entry.status === 403 ||
        (entry.headers && entry.headers['set-cookie'] !== undefined),
    );

    // 5. Screenshot 저장
    const screenshotPath = path.join('scripts', 'e2e-diagnose', 'output', 'screenshot.png');
    await page.screenshot({
        path    : screenshotPath,
        fullPage: false,
    }).catch(() => {
        // 이미 저장된 경우 무시
    });

    // 6. Railway Log 수집
    const railwayLog = collectRailwayLog();

    return {
        timestamp     : new Date().toISOString(),
        url           : page.url(),
        cookies       : cookieMap,
        localStorage,
        sessionStorage,
        networkTrace  : authNetworkTrace,
        consoleLogs,
        railwayLog,
        screenshotPath: 'output/screenshot.png',
    };
}

function collectRailwayLog(): string[] {
    try {
        const logs = execSync(
            'railway logs --service ssac-backend --tail 50',
            { encoding: 'utf-8', timeout: 10000 },
        );
        return logs.split('\n').filter(Boolean);
    } catch {
        return ['Railway CLI 미연결 또는 오류 — railway login 후 재시도'];
    }
}
