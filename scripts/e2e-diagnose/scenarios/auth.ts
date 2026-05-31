// scripts/e2e-diagnose/scenarios/auth.ts
import { Page } from '@playwright/test';

interface StateSnapshot {
    label    : string;
    url      : string;
    timestamp: string;
}

const snapshots: StateSnapshot[] = [];

async function captureState(page: Page, label: string): Promise<StateSnapshot> {
    const snap: StateSnapshot = {
        label,
        url      : page.url(),
        timestamp: new Date().toISOString(),
    };
    snapshots.push(snap);
    console.log(`  📸 상태 스냅샷 [${label}]: ${snap.url}`);
    return snap;
}

export async function runAuthScenario(
    page: Page,
    targetUrl: string,
): Promise<void> {
    console.log('🔍 시나리오 A: 로그인 직후 상태 수집');

    // 1. 홈 화면 진입
    await page.goto(targetUrl);
    await page.waitForLoadState('networkidle');
    await captureState(page, '로그인_전');

    // 2. 로그인 페이지 이동
    await page.goto(`${targetUrl}/login`);
    await page.waitForLoadState('networkidle');

    // 3. 카카오 로그인 버튼 감지 (외부 소셜 로그인은 클릭까지만 추적)
    const kakaoBtn = page.locator('button:has-text("카카오로 로그인")');
    if (await kakaoBtn.isVisible()) {
        await captureState(page, '카카오버튼_클릭전');

        const [response] = await Promise.all([
            page.waitForResponse(
                res => res.url().includes('/api/v1/auth'),
                { timeout: 5000 },
            ).catch(() => null),
            kakaoBtn.click(),
        ]);

        if (response) {
            console.log(`  카카오 Auth 응답: ${response.status()} ${response.url()}`);
        }

        await page.waitForTimeout(2000);
        await captureState(page, '카카오버튼_클릭후');
    } else {
        console.log('  카카오 로그인 버튼 미발견 — 관리자 로그인 시도');
    }

    // 4. 관리자 로그인 시도 (E2E_ADMIN_CODE 환경 변수 필요)
    await testAdminLogin(page, targetUrl);
}

async function testAdminLogin(page: Page, targetUrl: string): Promise<void> {
    await page.goto(`${targetUrl}/login`);
    await page.waitForLoadState('networkidle');

    const adminBtn = page.locator('button:has-text("관리자 로그인")');
    if (!(await adminBtn.isVisible())) {
        console.log('  관리자 로그인 버튼 미발견 — 시나리오 A 종료');
        return;
    }

    await adminBtn.click();
    await page.waitForTimeout(500);

    const adminCode = process.env.E2E_ADMIN_CODE;
    if (!adminCode) {
        console.log('  E2E_ADMIN_CODE 미설정 — 관리자 로그인 건너뜀');
        return;
    }

    const passwordInput = page.locator('input[type="password"]');
    if (await passwordInput.isVisible()) {
        await passwordInput.fill(adminCode);
        await page.click('button:has-text("로그인")');
        await page.waitForLoadState('networkidle');
        await captureState(page, '관리자_로그인_직후');
    }

    // 스크린샷 저장
    await page.screenshot({
        path    : 'scripts/e2e-diagnose/output/screenshot.png',
        fullPage: false,
    });

    // /api/v1/users/me 직접 호출 (브라우저 컨텍스트에서 쿠키 포함)
    const apiUrl = process.env.E2E_API_URL ?? 'https://api.ssac.io';
    const meResult = await page.evaluate(
        async (url: string) => {
            const res = await fetch(`${url}/api/v1/users/me`, {
                credentials: 'include',
            });
            return {
                status : res.status,
                headers: Object.fromEntries(res.headers.entries()),
                body   : await res.text().then(t => t.slice(0, 500)),
            };
        },
        apiUrl,
    );

    console.log(`\n  GET /api/v1/users/me 결과:`);
    console.log(`    status : ${meResult.status}`);
    console.log(`    body   : ${meResult.body.slice(0, 200)}`);
}
