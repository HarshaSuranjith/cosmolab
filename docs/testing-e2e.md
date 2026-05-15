---
description: Playwright E2E testing conventions — config, Page Object Model rules, test scenarios, data-testid conventions, CI integration
globs: testing/e2e/**
alwaysApply: false
---

# Playwright E2E Conventions

## Config (`testing/e2e/playwright.config.ts`)

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  reporter: [['html', { outputFolder: 'playwright-report' }]],
  use: {
    baseURL: 'http://localhost:80',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
  ],
});
```

## Page Object Model — Rules

**All selectors live in page objects. Never in test files.**
Selectors use `data-testid` attributes — never CSS classes, never text content (fragile).

```typescript
// testing/e2e/pages/ward-overview.page.ts
export class WardOverviewPage {
  constructor(private page: Page) {}

  async navigate(wardId = 'ICU') {
    await this.page.goto(`/ward/${wardId}`);
  }

  patientRows() {
    return this.page.getByTestId('patient-row');
  }

  vitalCell(patientId: string, measurement: string) {
    return this.page.getByTestId(`vitals-cell-${measurement}-${patientId}`);
  }

  async clickPatient(index = 0) {
    await this.patientRows().nth(index).click();
  }
}
```

Test files import page objects and read as user workflows:
```typescript
test('ward overview shows colour-coded vitals', async ({ page }) => {
  const ward = new WardOverviewPage(page);
  await ward.navigate('ICU');
  await expect(ward.patientRows()).toHaveCountGreaterThan(0);
  await expect(page.getByTestId('vital-flag-badge').first()).toBeVisible();
});
```

## Page Objects Inventory

```
testing/e2e/pages/
├── ward-overview.page.ts     navigate(), patientRows(), vitalCell(), clickPatient()
├── patient-list.page.ts      navigate(), searchInput(), wardFilter(), paginator(), clickRow()
├── patient-detail.page.ts    getTab(), problemRows(), vitalSparklines(), compositionRows()
├── create-vitals.page.ts     fillForm(vitals), submit(), expectNewReading()
└── add-problem.page.ts       icd10Input(), severitySelect(), submit(), expectInList()
```

## Test Files and Scenarios

**`ward-overview.spec.ts`**
- Table renders with at least 1 patient row
- Abnormal vital sign cells show flag badge
- Clicking a row navigates to `/patients/:id`

**`patient-list.spec.ts`**
- Default load shows 20 rows
- Search by last name filters correctly (debounced — wait for network idle)
- Ward dropdown reduces to matching ward patients
- Paginator loads a different set on next page
- Status filter chip toggles correctly

**`patient-detail.spec.ts`**
- All 4 tabs (Overview, Vitals, Problems, Compositions) are present and clickable
- Vitals tab: sparkline components visible, cells colour-coded
- Problems tab: ICD-10 codes displayed, severity badges shown
- Compositions tab: type filter works

**`create-vitals.spec.ts`**
- Form opens on button click
- Filling all fields and submitting sends POST (intercept request to verify)
- New reading appears in vitals list without full page reload
- Abnormal value (e.g. systolicBP=180) shows flag badge on new row

**`add-problem.spec.ts`**
- Form opens on "Add Problem" click
- Submitting with ICD-10 + displayName + severity creates entry
- New problem appears in problem list
- Submitting with empty ICD-10 shows validation error and does not submit

**`navigation.spec.ts`**
- Direct URL entry `/patients` renders correctly
- Direct URL entry `/ward/ICU` renders correctly
- Direct URL entry `/patients/invalid-uuid` shows error state (not blank page)
- `/admin` page loads with links to Grafana, RabbitMQ UI, Actuator
- Nav active state updates on route change

## Test Fixtures

```typescript
// testing/e2e/fixtures/test-data.ts
// References seed data from V5__seed_patients.sql + V6__seed_clinical_data.sql
export const SEED = {
  WARD_ICU: 'ICU',
  PATIENT_1: { id: '...', name: 'Eriksson, Lars', personalNumber: '19540312-1234' },
  EHR_1: { ehrId: '...' },
};
```

Use fixed seed IDs — don't create test data at runtime; it complicates cleanup and parallelism.

## CI Integration (Jenkins Stage 6)

```groovy
stage('E2E tests') {
  steps {
    sh 'docker-compose -f devops/docker-compose.yml up -d'
    sh './devops/scripts/wait-for-health.sh http://localhost/api/actuator/health 60'
    sh 'npx playwright test --config testing/e2e/playwright.config.ts'
  }
  post {
    always {
      sh 'docker-compose -f devops/docker-compose.yml down'
      publishHTML(target: [
        reportDir: 'testing/e2e/playwright-report',
        reportFiles: 'index.html',
        reportName: 'Playwright E2E Report'
      ])
    }
  }
}
```

`wait-for-health.sh` polls `/api/actuator/health` until HTTP 200 or 60s timeout.
Without this, Playwright starts before WildFly finishes deploying and every test fails.
