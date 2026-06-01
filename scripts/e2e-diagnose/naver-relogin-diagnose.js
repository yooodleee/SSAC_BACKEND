// scripts/e2e-diagnose/naver-relogin-diagnose.js
// 네이버 로그아웃 후 재로그인 상태 유지 불가 E2E 진단

const { chromium } = require('playwright');
const https = require('https');
const fs = require('fs/promises');
const path = require('path');

const FE_URL  = process.env.E2E_TARGET_URL || 'https://ssac.io';
const API_URL = process.env.E2E_API_URL    || 'https://api.ssac.io';
const OUTPUT  = path.join(__dirname, 'output');

// ── HTTP 헬퍼 ─────────────────────────────────────────────────────────────────
function httpGet(url, headers = {}) {
    return new Promise((resolve, reject) => {
        const req = https.get(url, { headers }, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => resolve({
                status   : res.statusCode,
                headers  : res.headers,
                body,
                location : res.headers.location,
            }));
        });
        req.on('error', reject);
        req.setTimeout(10000, () => { req.destroy(); reject(new Error('timeout')); });
    });
}

function httpPost(url, bodyStr = '{}', extraHeaders = {}) {
    return new Promise((resolve, reject) => {
        const u = new URL(url);
        const opts = {
            hostname: u.hostname,
            path    : u.pathname + u.search,
            method  : 'POST',
            headers : {
                'Content-Type'  : 'application/json',
                'Content-Length': Buffer.byteLength(bodyStr),
                ...extraHeaders,
            },
        };
        const req = https.request(opts, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body }));
        });
        req.on('error', reject);
        req.setTimeout(10000, () => { req.destroy(); reject(new Error('timeout')); });
        req.write(bodyStr);
        req.end();
    });
}

// ── 메인 ──────────────────────────────────────────────────────────────────────
async function diagnose() {
    const result = { timestamp: new Date().toISOString(), feUrl: FE_URL, apiUrl: API_URL };
    console.log('\n🔍 네이버 재로그인 E2E 진단');
    console.log(`   FE : ${FE_URL}\n   API: ${API_URL}`);

    // ── STEP 1: 헬스체크 ──────────────────────────────────────────────────────
    console.log('\n[STEP 1] BE 헬스 확인');
    try {
        const h = await httpGet(`${API_URL}/actuator/health`);
        console.log(`  /actuator/health → ${h.status} ${h.body.slice(0, 100)}`);
        result.health = { status: h.status, body: h.body.slice(0, 100) };
    } catch (e) { result.health = { error: String(e) }; console.log(`  오류: ${e}`); }

    // ── STEP 2: auth/status (쿠키 없음) ───────────────────────────────────────
    console.log('\n[STEP 2] /api/v1/auth/status');
    try {
        const s = await httpGet(`${API_URL}/api/v1/auth/status`, { Origin: FE_URL });
        console.log(`  status → ${s.status}`);
        console.log(`  body   → ${s.body.slice(0, 200)}`);
        console.log(`  CORS   → ${s.headers['access-control-allow-origin']}`);
        result.authStatus = { status: s.status, body: s.body.slice(0, 200), cors: s.headers['access-control-allow-origin'] };
    } catch (e) { result.authStatus = { error: String(e) }; }

    // ── STEP 3: naver/login 리다이렉트 URL 확인 ───────────────────────────────
    console.log('\n[STEP 3] /api/v1/auth/naver/login');
    try {
        const n = await httpGet(`${API_URL}/api/v1/auth/naver/login`, { Origin: FE_URL });
        const loc = n.location || '';
        console.log(`  status   → ${n.status}`);
        console.log(`  Location → ${loc.slice(0, 150)}`);
        result.naverLogin = {
            status    : n.status,
            hasState  : loc.includes('state='),
            hasClientId: loc.includes('client_id='),
            locationPrefix: loc.slice(0, 150),
        };
    } catch (e) { result.naverLogin = { error: String(e) }; }

    // ── STEP 4: reissue — 쿠키 없음 ──────────────────────────────────────────
    console.log('\n[STEP 4] /api/v1/auth/reissue (refreshToken 쿠키 없음)');
    try {
        const r = await httpPost(`${API_URL}/api/v1/auth/reissue`, '{}', { Origin: FE_URL });
        console.log(`  status → ${r.status}`);
        console.log(`  body   → ${r.body.slice(0, 200)}`);
        result.reissueNoCookie = { status: r.status, body: r.body.slice(0, 200) };
    } catch (e) { result.reissueNoCookie = { error: String(e) }; }

    // ── STEP 5: logout (쿠키 없음) ────────────────────────────────────────────
    console.log('\n[STEP 5] /api/v1/auth/logout (쿠키 없음)');
    try {
        const lo = await httpPost(`${API_URL}/api/v1/auth/logout`, '{}', { Origin: FE_URL });
        console.log(`  status     → ${lo.status}`);
        console.log(`  Set-Cookie → ${lo.headers['set-cookie']}`);
        console.log(`  body       → ${lo.body.slice(0, 200)}`);
        result.logoutNoCookie = {
            status   : lo.status,
            setCookie: lo.headers['set-cookie'],
            body     : lo.body.slice(0, 200),
        };
    } catch (e) { result.logoutNoCookie = { error: String(e) }; }

    // ── STEP 6: BFF 레이어 확인 (ssac.io/api/v1/auth/...) ────────────────────
    console.log('\n[STEP 6] FE BFF 엔드포인트 직접 확인');
    for (const bffPath of ['/api/v1/auth/status', '/api/v1/auth/reissue']) {
        try {
            const bff = bffPath.includes('reissue')
                ? await httpPost(`${FE_URL}${bffPath}`, '{}', { Origin: FE_URL })
                : await httpGet(`${FE_URL}${bffPath}`, { Origin: FE_URL });
            console.log(`  ${FE_URL}${bffPath}`);
            console.log(`    status     → ${bff.status}`);
            console.log(`    Set-Cookie → ${bff.headers['set-cookie'] || '없음'}`);
            console.log(`    body       → ${bff.body.slice(0, 150)}`);
            if (!result.bff) result.bff = {};
            result.bff[bffPath] = { status: bff.status, setCookie: bff.headers['set-cookie'], body: bff.body.slice(0, 200) };
        } catch (e) {
            console.log(`  ${bffPath} 오류: ${e}`);
            if (!result.bff) result.bff = {};
            result.bff[bffPath] = { error: String(e) };
        }
    }

    // ── STEP 7: Playwright — 홈/로그인 페이지 네트워크 수집 ──────────────────
    console.log('\n[STEP 7] Playwright 브라우저 진단');
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({
        recordHar: { path: path.join(OUTPUT, 'naver-relogin.har'), mode: 'full' },
    });
    const page = await context.newPage();

    const consoleLogs = [];
    page.on('console', msg => {
        if (msg.type() === 'error' || msg.type() === 'warning') {
            consoleLogs.push(`[${msg.type().toUpperCase()}] ${msg.text()}`);
        }
    });

    const networkEntries = [];
    page.on('request', req => {
        const u = req.url();
        if (u.includes('api.ssac.io') || u.includes('ssac.io/api')) {
            networkEntries.push({
                type  : 'REQ',
                method: req.method(),
                url   : u,
                cookie: req.headers()['cookie'] ? '[있음]' : '[없음]',
            });
        }
    });
    page.on('response', async res => {
        const u = res.url();
        if (u.includes('api.ssac.io') || u.includes('ssac.io/api')) {
            const h = res.headers();
            networkEntries.push({
                type     : 'RES',
                url      : u,
                status   : res.status(),
                setCookie: h['set-cookie'] || null,
            });
        }
    });

    try {
        // 7-1. 홈
        await page.goto(FE_URL, { waitUntil: 'networkidle', timeout: 30000 });
        await page.screenshot({ path: path.join(OUTPUT, 'naver-01-home.png') });
        const cookiesHome = await context.cookies();
        console.log(`  홈 로드: ${page.url()}`);
        console.log(`  쿠키: ${cookiesHome.map(c => `${c.name}@${c.domain}`).join(', ') || '없음'}`);

        // 7-2. 로그인 페이지
        await page.goto(`${FE_URL}/login`, { waitUntil: 'networkidle', timeout: 20000 });
        await page.screenshot({ path: path.join(OUTPUT, 'naver-02-login.png') });
        console.log(`  로그인 페이지: ${page.url()}`);

        const allButtons = await page.locator('button, a[href*="naver"], a[href*="kakao"]').allTextContents();
        console.log(`  버튼/링크: ${allButtons.slice(0, 8).join(' | ')}`);

        // 네이버 버튼/링크 href 확인
        const naverHref = await page.locator('a[href*="naver"]').first()
            .getAttribute('href').catch(() => null);
        console.log(`  네이버 href: ${naverHref || '없음'}`);

        // 7-3. 네이버 로그인 클릭 → 리다이렉트 URL 캡처
        const naverBtn = page.locator('button:has-text("네이버"), a:has-text("네이버")').first();
        const naverVisible = await naverBtn.isVisible().catch(() => false);
        if (naverVisible) {
            const [navResp] = await Promise.all([
                page.waitForResponse(r => r.url().includes('/auth/naver') || r.url().includes('nid.naver.com'), { timeout: 8000 }).catch(() => null),
                naverBtn.click(),
            ]);
            await page.waitForTimeout(2000);
            await page.screenshot({ path: path.join(OUTPUT, 'naver-03-after-click.png') });
            console.log(`  클릭 후 URL: ${page.url().slice(0, 120)}`);
            if (navResp) console.log(`  응답 URL: ${navResp.url().slice(0, 120)} → ${navResp.status()}`);
        }

        // 7-4. 네이버 인증 직접 시작 (Playwright로 naver/login 호출)
        await page.goto(`${API_URL}/api/v1/auth/naver/login`, { waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => {});
        await page.waitForTimeout(2000);
        const afterNaverLogin = page.url();
        await page.screenshot({ path: path.join(OUTPUT, 'naver-04-naver-login.png') });
        console.log(`  /naver/login 후 URL: ${afterNaverLogin.slice(0, 150)}`);
        const isNaverOAuth = afterNaverLogin.includes('nid.naver.com') || afterNaverLogin.includes('naver.com/oauth');
        console.log(`  네이버 OAuth 페이지 도달: ${isNaverOAuth}`);

        const cookiesFinal = await context.cookies();
        result.playwright = {
            cookiesHome: cookiesHome.map(c => ({
                name: c.name, domain: c.domain, path: c.path,
                secure: c.secure, httpOnly: c.httpOnly, sameSite: c.sameSite,
            })),
            cookiesFinal: cookiesFinal.map(c => ({
                name: c.name, domain: c.domain, path: c.path,
                secure: c.secure, httpOnly: c.httpOnly, sameSite: c.sameSite,
            })),
            naverHref,
            afterNaverLoginUrl: afterNaverLogin.slice(0, 200),
            isNaverOAuth,
            consoleLogs,
            networkEntries: networkEntries.slice(0, 60),
        };

    } finally {
        await context.close();
        await browser.close();
    }

    // ── STEP 8: 요약 출력 ─────────────────────────────────────────────────────
    console.log('\n=== E2E 진단 결과 ===');
    console.log(`시각           : ${result.timestamp}`);
    console.log(`BE 헬스        : ${result.health?.status}`);
    console.log(`auth/status    : ${result.authStatus?.status} / CORS=${result.authStatus?.cors}`);
    console.log(`naver/login    : state=${result.naverLogin?.hasState}, clientId=${result.naverLogin?.hasClientId}`);
    console.log(`reissue(no RT) : ${result.reissueNoCookie?.status} → ${result.reissueNoCookie?.body?.slice(0, 80)}`);
    console.log(`logout(no RT)  : ${result.logoutNoCookie?.status}, Set-Cookie=${JSON.stringify(result.logoutNoCookie?.setCookie)?.slice(0, 100)}`);

    console.log('\n--- BFF 엔드포인트 ---');
    if (result.bff) {
        for (const [k, v] of Object.entries(result.bff)) {
            console.log(`  ${k}: status=${v.status}, setCookie=${JSON.stringify(v.setCookie)?.slice(0, 80)}`);
            console.log(`         body=${v.body?.slice(0, 100)}`);
        }
    }

    console.log('\n--- 쿠키 (홈 방문 후) ---');
    if (result.playwright?.cookiesHome?.length > 0) {
        result.playwright.cookiesHome.forEach(c =>
            console.log(`  ${c.name}: domain=${c.domain}, path=${c.path}, secure=${c.secure}, sameSite=${c.sameSite}`));
    } else {
        console.log('  없음');
    }

    console.log('\n--- 네트워크 (인증 관련 RES) ---');
    result.playwright?.networkEntries?.filter(e => e.type === 'RES').forEach(e => {
        console.log(`  [${e.status}] ${e.url.slice(0, 100)}`);
        if (e.setCookie) console.log(`      Set-Cookie: ${String(e.setCookie).slice(0, 200)}`);
    });

    console.log('\n--- Console 오류 ---');
    if (result.playwright?.consoleLogs?.length > 0) {
        result.playwright.consoleLogs.forEach(l => console.log(`  ${l}`));
    } else {
        console.log('  없음');
    }

    console.log(`\n네이버 OAuth 페이지 도달: ${result.playwright?.isNaverOAuth}`);

    await fs.writeFile(
        path.join(OUTPUT, 'naver-relogin-result.json'),
        JSON.stringify(result, null, 2),
        'utf-8',
    );
    console.log('\n💾 결과: scripts/e2e-diagnose/output/naver-relogin-result.json');
}

diagnose().catch(err => {
    console.error('진단 실패:', err);
    process.exit(1);
});
