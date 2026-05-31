// scripts/e2e-diagnose/scenarios/index.ts
import { Page } from '@playwright/test';
import { runAuthScenario } from './auth';

export async function runScenario(
    page: Page,
    scenario: string,
    targetUrl: string,
): Promise<void> {
    switch (scenario) {
        case 'auth':
            await runAuthScenario(page, targetUrl);
            break;
        default:
            console.warn(`알 수 없는 시나리오: ${scenario}. auth 시나리오로 대체 실행합니다.`);
            await runAuthScenario(page, targetUrl);
    }
}
