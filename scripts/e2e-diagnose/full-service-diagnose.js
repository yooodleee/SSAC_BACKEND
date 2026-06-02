// scripts/e2e-diagnose/full-service-diagnose.js
// ssac.io 전체 서비스 종합 진단 — 비로그인 / API / 페이지 오류 전수 점검

const { chromium } = require('playwright');
const https = require('https');
const fs   = require('fs/promises');
const path = require('path');

const FE_URL  = process.env.E2E_TARGET_URL || 'https://ssac.io';
const API_URL = process.env.E2E_API_URL    || 'https://api.ssac.io';
const OUTPUT  = path.join(__dirname, 'output');

// ── HTTP 헬퍼 ─────────────────────────────────────────────────────────────────
function httpGet(url, headers = {}) {
    return new Promise((resolve, reject) => {
        const req = https.get(url, { headers }, res => {
            let body = '';
            res.on('data', c => body += c);
            res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body }));
        });
        req.on('error', reject);
        req.setTimeout(12000, () => { req.destroy(); reject(new Error('timeout')); });
    });
}

function httpPost(url, bodyStr = '{}', extraHeaders = {}) {
    return new Promise((resolve, reject) => {
        const u = new URL(url);
        const opts = {
            hostname: u.hostname,
            path    : u.pathname + u.search,
            method  : 'POST',
            headers : { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(bodyStr), ...extraHeaders },
        };
        const req = https.request(opts, res => {
            let body = '';
            res.on('data', c => body += c);
            res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body }));
        });
        req.on('error', reject);
        req.setTimeout(12000, () => { req.destroy(); reject(new Error('timeout')); });
        req.write(bodyStr); req.end();
    });
}

// ── 페이지 탐색 헬퍼 ──────────────────────────────────────────────────────────
async function visitPage(page, context, url, label, screenshotName) {
    const errors = [], warnings = [], networkFails = [];
    const apiCalls = [];

    const onConsole = msg => {
        if (msg.type() === 'error')   errors.push(msg.text());
        if (msg.type() === 'warning') warnings.push(msg.text());
    };
    const onReq = req => {
        const u = req.url();
        if (u.includes('api.ssac.io') || u.includes('ssac.io/api')) {
            apiCalls.push({ type: 'REQ', method: req.method(), url: u.slice(0, 120) });
        }
    };
    const onRes = async res => {
        const u = res.url();
        if (u.includes('api.ssac.io') || u.includes('ssac.io/api')) {
            apiCalls.push({ type: 'RES', status: res.status(), url: u.slice(0, 120) });
            if (res.status() >= 400) networkFails.push({ status: res.status(), url: u.slice(0, 120) });
        }
    };

    page.on('console', onConsole);
    page.on('request', onReq);
    page.on('response', onRes);

    let finalUrl = url, loadOk = false;
    try {
        await page.goto(url, { waitUntil: 'networkidle', timeout: 25000 });
        finalUrl = page.url();
        loadOk = true;
        await page.waitForTimeout(1500);
    } catch (e) {
        errors.push(`페이지 로드 실패: ${e.message}`);
    }

    const title = await page.title().catch(() => '(제목 없음)');
    await page.screenshot({ path: path.join(OUTPUT, `${screenshotName}.png`), fullPage: false }).catch(() => {});

    page.off('console', onConsole);
    page.off('request', onReq);
    page.off('response', onRes);

    return { label, url, finalUrl, title, loadOk, errors, warnings, networkFails, apiCalls };
}

// ── 메인 ──────────────────────────────────────────────────────────────────────
async function diagnose() {
    const report = { timestamp: new Date().toISOString(), pages: [], api: {}, issues: [] };
    await fs.mkdir(OUTPUT, { recursive: true });

    console.log('\n🔍 ssac.io 전체 서비스 종합 진단');
    console.log(`   FE : ${FE_URL}\n   API: ${API_URL}\n`);

    // ── SECTION A: BE API 상태 점검 ──────────────────────────────────────────
    console.log('━━━ [A] BE API 상태 점검 ━━━');

    const apiChecks = [
        { label: 'health',       method: 'GET',  url: `${API_URL}/actuator/health` },
        { label: 'auth/status',  method: 'GET',  url: `${API_URL}/api/v1/auth/status` },
        { label: 'naver/login',  method: 'GET',  url: `${API_URL}/api/v1/auth/naver/login` },
        { label: 'reissue(no cookie)', method: 'POST', url: `${API_URL}/api/v1/auth/reissue` },
        { label: 'logout(no cookie)',  method: 'POST', url: `${API_URL}/api/v1/auth/logout` },
        { label: 'guest token',  method: 'POST', url: `${API_URL}/api/v1/auth/guest` },
        // 공개 API
        { label: 'contents',     method: 'GET',  url: `${API_URL}/api/v1/contents` },
        { label: 'swagger-ui',   method: 'GET',  url: `${API_URL}/swagger-ui/index.html` },
    ];

    for (const chk of apiChecks) {
        try {
            const res = chk.method === 'GET'
                ? await httpGet(chk.url, { Origin: FE_URL })
                : await httpPost(chk.url, '{}', { Origin: FE_URL });
            const ok = res.status < 400 || (chk.label.includes('no cookie') && res.status === 400);
            console.log(`  [${ok ? '✅' : '❌'}] ${chk.label.padEnd(22)} → ${res.status}`);
            if (!ok) report.issues.push({ section: 'A', severity: 'HIGH', label: chk.label, detail: `HTTP ${res.status}: ${res.body.slice(0, 80)}` });
            report.api[chk.label] = { status: res.status, body: res.body.slice(0, 120) };
        } catch (e) {
            console.log(`  [❌] ${chk.label.padEnd(22)} → 오류: ${e.message}`);
            report.issues.push({ section: 'A', severity: 'CRITICAL', label: chk.label, detail: String(e) });
            report.api[chk.label] = { error: String(e) };
        }
    }

    // ── SECTION B: FE BFF 엔드포인트 ─────────────────────────────────────────
    console.log('\n━━━ [B] FE BFF 엔드포인트 ━━━');
    const bffChecks = [
        { label: 'BFF auth/status', method: 'GET',  url: `${FE_URL}/api/v1/auth/status` },
        { label: 'BFF reissue',     method: 'POST', url: `${FE_URL}/api/v1/auth/reissue` },
        { label: 'BFF logout',      method: 'POST', url: `${FE_URL}/api/v1/auth/logout` },
    ];
    for (const chk of bffChecks) {
        try {
            const res = chk.method === 'GET'
                ? await httpGet(chk.url, { Origin: FE_URL })
                : await httpPost(chk.url, '{}', { Origin: FE_URL });
            const ok = res.status < 500;
            console.log(`  [${ok ? '✅' : '❌'}] ${chk.label.padEnd(22)} → ${res.status}`);
            const setCookie = res.headers['set-cookie'];
            if (setCookie) console.log(`       Set-Cookie: ${String(setCookie).slice(0, 80)}`);
            if (!ok) report.issues.push({ section: 'B', severity: 'HIGH', label: chk.label, detail: `HTTP ${res.status}` });
            report.api[chk.label] = { status: res.status, setCookie, body: res.body.slice(0, 120) };
        } catch (e) {
            console.log(`  [❌] ${chk.label.padEnd(22)} → 오류: ${e.message}`);
            report.issues.push({ section: 'B', severity: 'HIGH', label: chk.label, detail: String(e) });
        }
    }

    // ── SECTION C: Playwright 브라우저 탐색 ──────────────────────────────────
    console.log('\n━━━ [C] 브라우저 페이지 탐색 ━━━');
    const browser = await chromium.launch({ headless: true });

    const pages = [
        { url: FE_URL,                    label: '홈',          shot: 'pg-01-home' },
        { url: `${FE_URL}/login`,         label: '로그인',      shot: 'pg-02-login' },
        { url: `${FE_URL}/signup`,        label: '회원가입',    shot: 'pg-03-signup' },
        { url: `${FE_URL}/onboarding`,    label: '온보딩',      shot: 'pg-04-onboarding' },
        { url: `${FE_URL}/learn`,         label: '학습',        shot: 'pg-05-learn' },
        { url: `${FE_URL}/community`,     label: '커뮤니티',    shot: 'pg-06-community' },
        { url: `${FE_URL}/mypage`,        label: '마이페이지',  shot: 'pg-07-mypage' },
        { url: `${FE_URL}/404-test-xyz`,  label: '존재하지 않는 페이지', shot: 'pg-08-404' },
    ];

    for (const p of pages) {
        // 페이지마다 새 context (쿠키 격리)
        const ctx  = await browser.newContext();
        const page = await ctx.newPage();
        const res  = await visitPage(page, ctx, p.url, p.label, p.shot);
        report.pages.push(res);

        const redirected = res.finalUrl !== p.url;
        const hasErrors  = res.errors.length > 0;
        const hasNetFail = res.networkFails.length > 0;
        const icon = (!hasErrors && !hasNetFail) ? '✅' : (hasNetFail ? '⚠️' : 'ℹ️');

        console.log(`  [${icon}] ${p.label.padEnd(14)} ${p.url}`);
        if (redirected) console.log(`         → 리다이렉트: ${res.finalUrl.slice(0, 80)}`);
        if (hasNetFail) {
            res.networkFails.forEach(f => console.log(`         ❌ API ${f.status}: ${f.url}`));
            report.issues.push({
                section: 'C', severity: hasErrors ? 'HIGH' : 'MEDIUM',
                label: p.label,
                detail: res.networkFails.map(f => `${f.status} ${f.url}`).join(' | '),
            });
        }
        if (hasErrors && res.errors.some(e => !e.includes('preloaded') && !e.includes('WebGL') && !e.includes('GPU'))) {
            const realErrors = res.errors.filter(e => !e.includes('preloaded') && !e.includes('WebGL') && !e.includes('GPU'));
            realErrors.slice(0, 3).forEach(e => console.log(`         🔴 콘솔: ${e.slice(0, 100)}`));
        }

        await ctx.close();
    }

    // ── SECTION D: 비로그인 사용자 API 응답 점검 ─────────────────────────────
    console.log('\n━━━ [D] 비로그인 API 접근 응답 점검 ━━━');
    const protectedApis = [
        { label: 'users/me',            url: `${API_URL}/api/v1/users/me` },
        { label: 'onboarding/questions',url: `${API_URL}/api/v1/onboarding/questions` },
        { label: 'ab-test/menu',        url: `${API_URL}/api/ab-test/menu` },
    ];
    for (const chk of protectedApis) {
        try {
            const res = await httpGet(chk.url, { Origin: FE_URL });
            const expected = res.status === 401 || res.status === 403;
            console.log(`  [${expected ? '✅' : '⚠️'}] ${chk.label.padEnd(26)} → ${res.status} ${expected ? '(인증 필요 — 정상)' : '(예상 외 응답)'}`);
            if (!expected) report.issues.push({ section: 'D', severity: 'MEDIUM', label: chk.label, detail: `비로그인 접근에 ${res.status} 응답` });
        } catch (e) {
            console.log(`  [❌] ${chk.label.padEnd(26)} → 오류: ${e.message}`);
        }
    }

    // ── SECTION E: 성능 / 응답속도 간이 측정 ─────────────────────────────────
    console.log('\n━━━ [E] 응답속도 간이 측정 ━━━');
    const perfUrls = [
        { label: '홈(FE)',         url: FE_URL },
        { label: 'BE health',      url: `${API_URL}/actuator/health` },
        { label: 'BE contents',    url: `${API_URL}/api/v1/contents` },
    ];
    for (const p of perfUrls) {
        const t0 = Date.now();
        try {
            await httpGet(p.url);
            const ms = Date.now() - t0;
            const icon = ms < 1000 ? '✅' : ms < 3000 ? '⚠️' : '❌';
            console.log(`  [${icon}] ${p.label.padEnd(16)} → ${ms}ms`);
            if (ms >= 3000) report.issues.push({ section: 'E', severity: 'MEDIUM', label: p.label, detail: `응답 ${ms}ms (3초 초과)` });
        } catch (e) {
            console.log(`  [❌] ${p.label.padEnd(16)} → 오류: ${e.message}`);
        }
    }

    await browser.close();

    // ── 최종 요약 ─────────────────────────────────────────────────────────────
    console.log('\n' + '═'.repeat(60));
    console.log('📋 종합 진단 결과');
    console.log('═'.repeat(60));

    const criticals = report.issues.filter(i => i.severity === 'CRITICAL');
    const highs     = report.issues.filter(i => i.severity === 'HIGH');
    const mediums   = report.issues.filter(i => i.severity === 'MEDIUM');

    console.log(`\n🔴 CRITICAL (${criticals.length}건):`);
    criticals.forEach(i => console.log(`   [${i.section}] ${i.label}: ${i.detail}`));

    console.log(`\n🟠 HIGH (${highs.length}건):`);
    highs.forEach(i => console.log(`   [${i.section}] ${i.label}: ${i.detail}`));

    console.log(`\n🟡 MEDIUM (${mediums.length}건):`);
    mediums.forEach(i => console.log(`   [${i.section}] ${i.label}: ${i.detail}`));

    if (report.issues.length === 0) {
        console.log('\n✅ 감지된 문제 없음');
    }

    // 페이지별 상세 콘솔 오류
    console.log('\n── 페이지별 실제 콘솔 오류 ──');
    report.pages.forEach(pg => {
        const realErrors = pg.errors.filter(e =>
            !e.includes('preloaded') && !e.includes('WebGL') &&
            !e.includes('GPU') && !e.includes('setPolicyInfo')
        );
        if (realErrors.length > 0) {
            console.log(`\n  [${pg.label}] (${pg.finalUrl.slice(0, 60)})`);
            realErrors.forEach(e => console.log(`    🔴 ${e.slice(0, 120)}`));
        }
    });

    // 페이지별 API 실패
    console.log('\n── 페이지별 API 실패 ──');
    report.pages.forEach(pg => {
        if (pg.networkFails.length > 0) {
            console.log(`\n  [${pg.label}]`);
            pg.networkFails.forEach(f => console.log(`    ❌ ${f.status} ${f.url}`));
        }
    });

    await fs.writeFile(
        path.join(OUTPUT, 'full-service-result.json'),
        JSON.stringify(report, null, 2),
        'utf-8'
    );
    console.log('\n💾 결과: scripts/e2e-diagnose/output/full-service-result.json');
    console.log('📸 스크린샷: scripts/e2e-diagnose/output/pg-*.png');
}

diagnose().catch(err => {
    console.error('진단 실패:', err);
    process.exit(1);
});
