---
description: Enterprise UI specification for the CosmoLab Angular frontend — design system, layout, page-by-page wireframes, component contracts, data bindings, interaction patterns, and data-testid inventory.
globs: frontend/src/**/*.ts,frontend/src/**/*.html,frontend/src/**/*.scss
alwaysApply: false
---

# CosmoLab — Frontend UI Specification

> This document is the implementation source of truth for Sprint 3.
> Every page, component, form, state, and `data-testid` is defined here.
> The Angular conventions and module structure live in [`frontend-angular.md`](frontend-angular.md).
> The clinical data model the UI renders is in [`clinical-domain.md`](clinical-domain.md).
>
> **Design reference** — pixel-accurate HTML mockups and the design system token file live in:
> `docs/stitch_sleek_nordic_enterprise_ui/`
>
> | Screen | HTML mockup | Screenshot |
> |---|---|---|
> | Design tokens | `nordic_clinical/DESIGN.md` | — |
> | Ward Overview | `ward_overview_icu/code.html` | `screen.png` |
> | Patient List | `patient_list/code.html` | `screen.png` |
> | Patient Detail — Overview tab | `patient_detail_overview/code.html` | `screen.png` |
> | Patient Detail — Vitals tab | `patient_detail_vitals/code.html` | `screen.png` |
> | Add Vital Signs form | `add_vital_signs_form/code.html` | `screen.png` |
>
> When any detail in this spec conflicts with those HTML files, **the HTML files win**.

---

## 0. Guiding Principles

These rules apply to every file in the frontend. Deviate only when the framework explicitly requires it.

### 0.1 Separation of Concerns

| Concern | Where it lives | Must NOT |
|---|---|---|
| HTTP and data fetching | Service (`*.service.ts`) | Render anything; import components |
| Display and interaction | Component (`.ts` + `.html`) | Call `HttpClient` directly; contain business logic |
| Types / interfaces | `core/models/*.model.ts` | Import Angular; contain logic |
| State derivation | `computed()` in the component | Trigger side effects; call services |
| Side effects (routing, snackbar) | Method bodies or `effect()` | Go inside `computed()` |
| Styles | `*.component.scss` | Reach outside the component (no global selectors) |
| Threshold / range logic | `shared/utils/*.util.ts` (pure functions) | Live inline in components or templates |

### 0.2 File Structure Rules

- Every component lives in its own folder: `component-name/component-name.component.ts|.html|.scss`
- No inline templates or inline styles except for trivially single-element components
- No logic in templates beyond signal reads, method calls, and simple ternary display expressions
- Interfaces are never declared inside component or service files — import from `core/models/`
- Services used by only one feature live in that feature's folder, not in `core/`

### 0.3 Angular-Specific Best Practices

- **`inject()`** everywhere — no constructor injection
- **`ChangeDetectionStrategy.OnPush`** on every component — no exceptions
- **Signal inputs** (`input()`, `input.required()`) and **signal outputs** (`output()`) for all component I/O
- **`@if` / `@for` / `@switch` / `@defer`** — never `*ngIf`, `*ngFor`, `NgIf`, `NgFor`
- **`toSignal()`** to convert HTTP Observables at the component boundary — never subscribe manually unless unavoidable
- **`takeUntilDestroyed()`** for any manual subscription that cannot use `toSignal()`
- No `BehaviorSubject` or `Subject` in components — use `signal()` instead
- No `async` pipe — signals are synchronous and require no pipe
- `protected` visibility for template-accessed members; `private` for everything else

### 0.4 Smart vs. Presentational Components

- **Smart (container) components** — own the data: call services, hold signals, handle routing. One per route.
- **Presentational (dumb) components** — receive data via `input()`, emit events via `output()`. No service injection.
- All shared components (`severity-badge`, `loading-spinner`, `vital-signs-chart`) are always presentational.

---

## 1. Design System

### 1.1 Colour Palette — Nordic Clinical

CosmoLab uses the **Nordic Clinical** design system — a restricted, purposeful palette rooted in Nordic Minimalism. Deep forest green primary on arctic white surfaces, with semantic colours tuned for clinical safety. All CSS custom properties are declared in `styles.scss` under `:root` prefixed `--nc-*`. The authoritative token file is `docs/stitch_sleek_nordic_enterprise_ui/nordic_clinical/DESIGN.md`.

**Core M3 tokens — primary, secondary, tertiary:**

| Token | CSS var | Hex | Usage |
|---|---|---|---|
| `primary` | `--nc-primary` | `#00342b` | Brand, primary buttons, ACTIVE ward chip bg |
| `on-primary` | `--nc-on-primary` | `#ffffff` | Text/icons on primary; ACTIVE ward chip text |
| `primary-container` | `--nc-primary-container` | `#004d40` | Sidenav background, secondary button hover |
| `on-primary-container` | `--nc-on-primary-container` | `#7ebdac` | Text on primary-container |
| `inverse-primary` | `--nc-inverse-primary` | `#94d3c1` | Active nav item text (on dark sidenav) |
| `primary-fixed` | `--nc-primary-fixed` | `#afefdd` | Active nav item tonal highlight |
| `primary-fixed-dim` | `--nc-primary-fixed-dim` | `#94d3c1` | Dimmer variant of primary-fixed |
| `on-primary-fixed` | `--nc-on-primary-fixed` | `#00201a` | Text on primary-fixed |
| `on-primary-fixed-variant` | `--nc-on-primary-fixed-variant` | `#065043` | Muted text on primary-fixed |
| `secondary` | `--nc-secondary` | `#5e5e5e` | Secondary text, inactive nav icons |
| `on-secondary` | `--nc-on-secondary` | `#ffffff` | Text on secondary bg |
| `secondary-container` | `--nc-secondary-container` | `#e1dfdf` | Gender chip, patient initials avatar bg |
| `on-secondary-container` | `--nc-on-secondary-container` | `#626262` | Text on secondary-container |
| `secondary-fixed` | `--nc-secondary-fixed` | `#e4e2e2` | Fixed secondary surface |
| `secondary-fixed-dim` | `--nc-secondary-fixed-dim` | `#c7c6c6` | Dimmer secondary-fixed |
| `on-secondary-fixed` | `--nc-on-secondary-fixed` | `#1b1c1c` | Text on secondary-fixed |
| `on-secondary-fixed-variant` | `--nc-on-secondary-fixed-variant` | `#464747` | Muted text on secondary-fixed |
| `tertiary` | `--nc-tertiary` | `#262f32` | Deep neutral accent |
| `on-tertiary` | `--nc-on-tertiary` | `#ffffff` | Text on tertiary bg |
| `tertiary-container` | `--nc-tertiary-container` | `#3c4548` | Tertiary surface |
| `on-tertiary-container` | `--nc-on-tertiary-container` | `#a9b2b6` | Text on tertiary-container |

**Surface and background tokens:**

| Token | CSS var | Hex | Usage |
|---|---|---|---|
| `surface` | `--nc-surface` | `#f9f9f9` | Page background |
| `background` | `--nc-background` | `#f9f9f9` | M3 alias for surface |
| `on-background` | `--nc-on-background` | `#1a1c1c` | Primary text on background |
| `surface-dim` | `--nc-surface-dim` | `#dadada` | Skeleton loader, disabled surface |
| `surface-container-lowest` | `--nc-surface-lowest` | `#ffffff` | Card and table backgrounds |
| `surface-container-low` | `--nc-surface-low` | `#f3f3f3` | Row hover state |
| `surface-container` | `--nc-surface-container` | `#eeeeee` | Table header background |
| `surface-container-high` | `--nc-surface-high` | `#e8e8e8` | Selected row background |
| `surface-container-highest` | `--nc-surface-highest` | `#e2e2e2` | Dividers, skeleton highlight |
| `surface-variant` | `--nc-surface-variant` | `#e2e2e2` | Alias for surface-container-highest |
| `surface-tint` | `--nc-surface-tint` | `#29695b` | Tonal overlay on primary surfaces |
| `on-surface` | `--nc-on-surface` | `#1a1c1c` | Primary text |
| `on-surface-variant` | `--nc-on-surface-variant` | `#3f4945` | Secondary text (labels, metadata) |
| `inverse-surface` | `--nc-inverse-surface` | `#2f3131` | Snackbar, tooltip background |
| `inverse-on-surface` | `--nc-inverse-on-surface` | `#f1f1f1` | Text on inverse-surface |
| `outline` | `--nc-outline` | `#707975` | Muted text, disabled |
| `outline-variant` | `--nc-outline-variant` | `#bfc9c4` | Table row separators, card borders, toolbar border |

**Error tokens:**

| Token | CSS var | Hex | Usage |
|---|---|---|---|
| `error` | `--nc-error` | `#ba1a1a` | Error states, validation messages, SEVERE badge, critical vitals |
| `on-error` | `--nc-on-error` | `#ffffff` | Text on error bg |
| `error-container` | `--nc-error-container` | `#ffdad6` | Flagged vital highlight, critical row tint |
| `on-error-container` | `--nc-on-error-container` | `#93000a` | Text on error-container |

**Semantic clinical tokens (project-defined in `styles.scss`):**

| Token | CSS var | Value | Usage |
|---|---|---|---|
| `status-active` | `--nc-status-active` | bg `#00342b` text `#ffffff` | ACTIVE ward chip — solid primary, white text |
| `status-discharged` | `--nc-status-discharged` | bg `#e8eaf6` text `#3949ab` | DISCHARGED chip |
| `status-transferred` | `--nc-status-transferred` | bg `#fff3e0` text `#e65100` | TRANSFERRED chip |
| `severity-mild` | `--nc-severity-mild` | `#388e3c` | MILD severity badge |
| `severity-moderate` | `--nc-severity-moderate` | `#f9a825` | MODERATE badge; amber vital flag |
| `severity-severe` | `--nc-severity-severe` | `#ba1a1a` | SEVERE badge; red vital flag (= `error`) |

> **ACTIVE chip distinction**: The ward overview uses `bg-primary text-on-primary` (solid `#00342b`, white text) for ACTIVE patient status — a strong visual anchor, not a soft fill. Patient list clinical acuity chips (Stable / Observation / Critical) use soft semantic fills with dark text.

**Depth / elevation**: No drop shadows. Depth is communicated through tonal layering and `1px solid outline-variant` borders on cards/tables.

### 1.2 Typography — Inter

All text uses **Inter** (loaded from Google Fonts: `wght@400;500;600;700`). Inter's tabular numeral feature is essential for comparing clinical values in tables.

| Role | Size / Weight | Line height | Letter spacing | Usage |
|---|---|---|---|---|
| `headline-lg` | 28px / 600 | 36px | −0.02em | Page titles |
| `headline-md` | 20px / 600 | 28px | −0.01em | Section headings |
| `headline-sm` | 16px / 600 | 24px | — | Card titles, tab labels |
| `body-md` | 14px / 400 | 20px | — | Table cell text, form labels (default) |
| `body-sm` | 13px / 400 | 18px | — | Secondary descriptions |
| `label-md` | 12px / 600 | 16px | 0.05em | Column headers, button labels, status chips |
| `label-sm` | 11px / 500 | 14px | — | Timestamps, metadata captions |
| `data-mono` | 13px / 500 | 18px | — | Vital values, IDs, personnummer (tabular nums) |

> Set `font-size: 14px` as the body root. Dense clinical tables use `mat-table` with `[class.mat-table-dense]` styling (`row height: 36px`, `padding: 6px 12px`).

### 1.3 Spacing Scale

8px base grid. Clinical density: internal padding is reduced by 4px vs. standard enterprise patterns.

| Token | Value | Usage |
|---|---|---|
| `element-gap-tight` | 4px | Chip padding, icon gap |
| `element-gap-md` | 8px | Between related items |
| `gutter` | 16px | Card padding, form field gap |
| `container-padding` | 24px | Between card sections, page padding |
| `section-gap` | 32px | Between major page sections |

### 1.4 Angular Material Theme

Configure a custom theme in `styles.scss` using the Nordic primary palette:

```scss
@use '@angular/material' as mat;

// Nordic Clinical primary palette (forest green anchored on #00342b)
$nordic-primary-map: (
  50: #e0f2ee, 100: #b2dfda, 200: #80cab8, 300: #4db596,
  400: #26a67f, 500: #00976a, 600: #008060, 700: #006a4e,
  800: #004d40, 900: #00342b,
  A100: #94d3c1, A200: #7ebdac, A400: #29695b, A700: #065043,
  contrast: ( ... 50–400: rgba(0,0,0,0.87), 500–900: #ffffff )
);

$cosmolab-primary: mat.define-palette($nordic-primary-map, 900, 100, 700);
$cosmolab-accent:  mat.define-palette(mat.$amber-palette, 700, 200, 900);
$cosmolab-warn:    mat.define-palette(mat.$red-palette, 800);

$cosmolab-theme: mat.define-light-theme((
  color: (primary: $cosmolab-primary, accent: $cosmolab-accent, warn: $cosmolab-warn),
  typography: mat.define-typography-config(
    $font-family: "'Inter', sans-serif",
    $body-1: mat.define-typography-level(14px, 20px, 400),
    $subtitle-1: mat.define-typography-level(16px, 24px, 600),
    $headline-5: mat.define-typography-level(28px, 36px, 600, $letter-spacing: -0.02em),
  ),
  density: -1
));

@include mat.all-component-themes($cosmolab-theme);
```

`density: -1` reduces component padding one step — standard for data-dense enterprise UIs.

### 1.5 Vital Signs — Normal Range Reference

Used by `VitalSignsChartComponent` and `WardOverviewComponent` to colour-code cells.

| Measurement | Normal range | Flag colour |
|---|---|---|
| Systolic BP | 90–140 mmHg | Amber if outside; red if >180 or <80 |
| Diastolic BP | 60–90 mmHg | Amber if outside |
| Heart rate | 60–100 bpm | Amber if outside; red if >140 or <40 |
| Respiratory rate | 12–20 /min | Amber if outside |
| Temperature | 36.1–37.2 °C | Amber if outside; red if >39.0 |
| SpO₂ | ≥ 95 % | Amber if 93–94; red if < 93 |
| Weight | No range | Never flagged |

Flag icons use `Material Symbols Outlined` with `warning` (amber) and `error_outline` (red) icon names at 16px — see §1.6 for icon setup.

### 1.6 Icon Library, Border Radius, and Component Details

**Icon library — Material Symbols Outlined (variable font)**

Load in `index.html`:
```html
<link rel="stylesheet"
  href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" />
```

Default variation settings in `styles.scss`:
```scss
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
```

Use `<span class="material-symbols-outlined">icon_name</span>` in templates. Do **not** use `<mat-icon>` — it uses the older fixed-weight font family.

**Border radius** — from DESIGN.md `rounded` scale:

| Token | Value | Applied to |
|---|---|---|
| `rounded-sm` (sm) | `0.125rem` = 2px | Status chips, badge chips |
| `rounded` (DEFAULT) | `0.25rem` = 4px | Inputs, buttons, cards |
| `rounded-md` | `0.375rem` = 6px | Larger containers |
| `rounded-lg` | `0.5rem` = 8px | Modal dialogs |
| `rounded-full` | `9999px` | Circular avatars, FAB buttons |

Override Material's defaults via `--mdc-*` CSS tokens in `styles.scss`:
```scss
:root {
  --mdc-shape-small:  4px;   // inputs, chips
  --mdc-shape-medium: 4px;   // cards, menus
  --mdc-shape-large:  8px;   // dialogs
}
```

**Input height**: 32px (density: -1 applied globally via `mat.define-light-theme`). Use `mat-form-field` with `appearance="outline"` and `subscriptSizing="dynamic"`.

**Flagged rows** (abnormal vitals in ward overview / vitals table): Apply `background-color` as `color-mix(in srgb, var(--nc-error-container) 10%, transparent)` on the `<tr>`. Do **not** use a left border accent.

**Patient initials avatar**: 32×32px circle, `background-color: var(--nc-secondary-container)`, 2-letter initials in `var(--nc-on-secondary-container)`. Class: `w-8 h-8 rounded-full bg-secondary-container`.

**Ward overview bento summary cards**: Icon container is 40×40px with a tinted background (e.g. `bg-primary-container` for the primary metric). Card is 128px height, `1px solid outline-variant` border, no shadow.

**FAB button** (ward overview — add vital signs): Fixed position, `border-radius: 50%`, 56×56px, `background: var(--nc-primary)`, `color: var(--nc-on-primary)`. Positioned `bottom: 32px; right: 32px` on desktop; `bottom: 80px` on mobile (above bottom nav).

---

## 2. Application Shell

### 2.1 Layout Structure

The app uses **two distinct layout patterns** depending on route — this is a deliberate design choice visible in the HTML mockups.

**Dashboard layout** — Ward Overview (`/ward/:wardId`):
```
┌─────────────────────────────────────────────────────────┐
│  [Header] CosmoLab ⚕  [Refresh ↺]  [Ward: ICU ▾] [●]  │  56px fixed top bar
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [main — max-w-7xl mx-auto, pt-20, full width]          │
│                                                         │
└─────────────────────────────────────────────────────────┘
         [Mobile bottom nav — hidden md:hidden]
```

- No sidebar. Full-width content with `max-w-7xl mx-auto` centring.
- Header: `surface` background, `1px solid outline-variant` bottom border, `position: fixed; top: 0; z-index: 50`.
- On mobile: a bottom navigation bar replaces the sidebar (4 icon+label items).
- Page background: `background` (`#f9f9f9`).

**Record layout** — Patient List / Patient Detail (`/patients`, `/patients/:id`):
```
┌──────────────────────────────────────────────────────────┐
│ CosmoLab ⚕ │  [top app bar with breadcrumb / actions]   │
│  Ward      │                                             │
│  Patients  │  [Router outlet — page content]             │
│  Admin     │                                             │
│  240px     │                                             │
│  fixed     │                                             │
│  ─────     │                                             │
│  Dr. ...   │                                             │
└──────────────────────────────────────────────────────────┘
```

- 240px left sidebar (`position: fixed; inset-y: 0; left: 0`), `surface` background, `1px solid outline-variant` right border.
- Sidebar hidden on mobile (`hidden md:flex`); main content `margin-left: 240px` on `≥ md`.
- Sidebar contains: logo/wordmark at top, nav links (Ward, Patients, Admin), user info at bottom.
- Active nav link: `on-surface` text with `surface-container-low` background; no left-border accent.
- Main content area has its own top bar (breadcrumb + page actions).
- Page background: `background` (`#f9f9f9`).

### 2.2 Ward Header (`WardHeaderComponent`) — dashboard layout only

Used on `/ward/:wardId`. Full-width fixed top bar, no hamburger.

```
┌────────────────────────────────────────────────────────────┐
│  ⚕ CosmoLab   [↺ Refresh]   [Ward: ICU ▾]   [user avatar] │  56px
└────────────────────────────────────────────────────────────┘
```

| Element | Binding | Notes |
|---|---|---|
| `clinical_notes` icon + "CosmoLab" wordmark | routes to `/ward/ICU` | `data-testid="nav-logo"` |
| Refresh button | calls `wardOverviewService.reload()` | `data-testid="refresh-btn"` |
| Ward selector | `mat-select`, hardcoded: ICU, CARDIOLOGY, ONCOLOGY, NEUROLOGY | Routes to `/ward/:wardId`; `data-testid="ward-selector"` |
| User avatar | circular 32px image or initials | Non-interactive; `data-testid="user-indicator"` |

Background: `surface` (`#f9f9f9`), `1px solid outline-variant` bottom border. Brand wordmark in `primary` (`#00342b`).

### 2.3 Record Sidebar (`RecordSidenavComponent`) — record layout only

Used on `/patients` and `/patients/:id`. 240px fixed left sidebar, hidden on mobile.

```
┌──────────────────────────┐
│ ⚕ CosmoLab               │  56px logo row; matches header height
├──────────────────────────┤
│  grid_view  Ward         │  routerLink="/ward/ICU"
│  group      Patients     │  routerLink="/patients" ← active
│  settings   Admin        │  routerLink="/admin"
│                          │
│  (flex spacer)           │
├──────────────────────────┤
│  [avatar] Dr. Andersson  │  user info, bottom-aligned
│           Chief Resident │
└──────────────────────────┘
```

Background: `surface` (`#f9f9f9`), `1px solid outline-variant` right border.

Active nav link: full-width row, `surface-container-low` background, `primary` text + icon, `font-weight: 600`. Inactive: `secondary` text + icon.

Icons: `Material Symbols Outlined` at 24px (grid_view, group, settings).

`data-testid` values: `"nav-ward"`, `"nav-patients"`, `"nav-admin"`, `"nav-logo"`.

---

## 3. Shared Components

### 3.1 `PageHeaderComponent`

Selector: `app-page-header`

```
┌─────────────────────────────────────────────────────────┐
│  [icon]  [title]            [actions slot — ng-content] │
│          [subtitle]                                      │
└─────────────────────────────────────────────────────────┘
```

```typescript
readonly icon     = input.required<string>();   // Material icon name e.g. 'local_hospital'
readonly title    = input.required<string>();
readonly subtitle = input<string>();
```

Content projection slot: `[actions]` — right-aligned action buttons.

Styling: white card, 16px padding, bottom border `1px solid --nc-outline-variant`, `mat-icon` in primary.

`data-testid="page-header"` on root element.

### 3.2 `LoadingSpinnerComponent`

Selector: `app-loading-spinner`

```typescript
readonly diameter = input<number>(40);
readonly overlay  = input<boolean>(false);
```

When `overlay()` is true: covers parent element with a translucent white mask and centres the spinner.

`data-testid="loading-spinner"`.

### 3.3 `SeverityBadgeComponent`

Selector: `app-severity-badge`

```typescript
readonly severity = input.required<'MILD' | 'MODERATE' | 'SEVERE' | 'ROUTINE' | 'URGENT' | 'CRITICAL'>();
```

Renders a `mat-chip` (non-interactive, `[selectable]="false"`):

| Value | Background | Text | Icon |
|---|---|---|---|
| MILD | `#E8F5E9` | `#2E7D32` | `check_circle` |
| MODERATE | `#FFF8E1` | `#F9A825` | `warning` |
| SEVERE | `#FFEBEE` | `#C62828` | `error` |

`data-testid="severity-badge-{severity}"` (e.g. `severity-badge-SEVERE`).

### 3.4 `VitalSignsChartComponent`

Selector: `app-vital-signs-chart`

```typescript
readonly readings = input<VitalSignsResponse[]>([]);  // newest first
readonly compact  = input<boolean>(false);
```

Normal-range thresholds are imported from `shared/utils/vital-flags.util.ts` — not hardcoded in this component.

**Compact mode** (ward table, 1 row per patient):

```
BP     HR     RR     Temp   SpO₂
120/80 ⚠72   16     37.1   98%
```

Each value is a plain `<span>` with inline colour styling based on normal ranges:
- Normal: `on-surface` (black)
- Amber flag: `accent-600` + amber `warning` icon
- Red flag: `warn-700` + red `error` icon

`data-testid="vitals-cell-{measurement}"` e.g. `vitals-cell-systolicBp`, `vitals-cell-heartRate`.
`data-testid="vital-flag-badge"` on each icon.

**Full mode** (patient detail Vitals tab):

Renders an `ngx-charts` line chart (or a hand-rolled SVG sparkline using `@angular/cdk/overlay`). Each vital sign is a separate series on a shared time axis. The chart height is 240px. Below the chart: a dense table of the last 10 readings, same colour-coding as compact mode.

If `readings` is empty: renders an empty-state placeholder (see §4.4).

### 3.5 Status Chip (inline, not a separate component)

Used in tables for `PatientStatus`. Rendered as a `mat-chip` with `[class]` binding:

| Status | Background | Text |
|---|---|---|
| ACTIVE | `#E8F5E9` | `#2E7D32` |
| DISCHARGED | `#EDE7F6` | `#5C6BC0` |
| TRANSFERRED | `#FFF3E0` | `#E65100` |

`data-testid="status-chip-{STATUS}"` e.g. `status-chip-ACTIVE`.

---

## 4. General Patterns

### 4.1 Loading State

All pages and data-fetching components start in a loading state. Use `signal<boolean>` — no `BehaviorSubject`, no `async` pipe.

```typescript
protected readonly loading = signal(false);

protected load(): void {
  this.loading.set(true);
  this.service.getAll().pipe(
    finalize(() => this.loading.set(false)),
  ).subscribe(data => this.items.set(data));
}
```

```html
@if (loading()) {
  <app-loading-spinner [overlay]="true" />
} @else {
  <!-- content -->
}
```

Prefer `toSignal()` when there is no need to imperatively trigger reloads:

```typescript
protected readonly patients = toSignal(this.patientService.getAll(), { initialValue: [] });
```

### 4.2 Error State

```
┌─────────────────────────────┐
│  ⚠  Failed to load data     │
│     [Retry]                 │
└─────────────────────────────┘
```

`mat-icon` (`error_outline`) in `warn-700`. Retry button calls the same data-fetch method. `data-testid="error-state"`. `data-testid="retry-btn"`.

### 4.3 Empty State

When a query returns zero results (not an error):

```
┌─────────────────────────────┐
│  [icon]                     │
│  No {items} found           │
│  {contextual hint}          │
└─────────────────────────────┘
```

Contextual hints: "No patients match the current filter." / "No vital signs recorded yet. Use the button above to add a reading."

`data-testid="empty-state"`.

### 4.4 Notification Pattern

All user feedback goes through `NotificationService` → `MatSnackBar`. Never render inline success/error text.

- Success: `duration: 3000`, no action, default background
- Error: `duration: 6000`, "Dismiss" action, `panelClass: ['snackbar-error']` (red accent)

### 4.5 Form Submission Pattern

1. Disable the submit button while `loading()` is true
2. Show `LoadingSpinnerComponent` overlay on the form
3. On success: close dialog, show snackbar, parent refreshes
4. On 4xx: apply `ProblemDetail.properties.fieldErrors` to form controls; generic snackbar for non-field errors
5. On 5xx: generic snackbar

```typescript
protected readonly loading = signal(false);

protected submit(): void {
  if (this.form.invalid) return;
  this.loading.set(true);
  this.service.create(this.form.getRawValue()).pipe(
    finalize(() => this.loading.set(false)),
  ).subscribe({
    next: result => { this.notifications.success('Saved'); this.dialogRef.close(result); },
    error: err => this.handleFieldErrors(err),
  });
}
```

### 4.6 Confirmation Dialogs

Used for destructive actions only (discharge patient / delete composition). Use `MatDialog` with a simple inline template — no separate component needed for two-line dialogs.

```
┌─────────────────────────────┐
│  Discharge patient?         │
│  This cannot be undone.     │
│                             │
│           [Cancel] [Confirm]│
└─────────────────────────────┘
```

---

## 5. Ward Overview Page

Route: `/ward/:wardId`
Component: `WardOverviewComponent`
API: `GET /api/v1/ward/{wardId}/overview`

### 5.1 Layout

```
┌───────────────────────────────────────────────────────────────────┐
│  🏥  Ward Overview — ICU               [20 patients]  [⟳ Refresh] │  PageHeader
├───────────────────────────────────────────────────────────────────┤
│  [Patients with flags: 3]  [No vitals: 1]  [SEVERE problems: 2]   │  Summary chips
├──────┬──────────────┬──────────┬───────┬───────┬───────┬──────────┤
│  #   │  Patient     │  Status  │  BP   │  HR   │  Temp │ SpO₂  Prb│  Table header
├──────┼──────────────┼──────────┼───────┼───────┼───────┼──────────┤
│  1   │  Lindström,  │  ACTIVE  │120/78 │  72   │  37.0 │  98%   2 │
│      │  Anna        │          │       │       │       │          │
├──────┼──────────────┼──────────┼───────┼───────┼───────┼──────────┤
│  2   │  Berg, Erik  │  ACTIVE  │188/94⚠│  110⚠│  38.9⚠│  91%🔴  4│  flagged row
├──────┴──────────────┴──────────┴───────┴───────┴───────┴──────────┤
│  ...                                                               │
└───────────────────────────────────────────────────────────────────┘
```

### 5.2 Summary Chips

Three `mat-chip` counters derived with `computed()` from the overview signal — not calculated in the template:

```typescript
protected readonly flaggedCount   = computed(() => this.overview()?.patients.filter(p => p.flags.length > 0).length ?? 0);
protected readonly noVitalsCount  = computed(() => this.overview()?.patients.filter(p => !p.latestVitals).length ?? 0);
protected readonly activeProblems = computed(() => this.overview()?.patients.reduce((s, p) => s + p.activeProblemCount, 0) ?? 0);
```

Informational only (not filterable). `data-testid="summary-chip-flagged"`, `"summary-chip-no-vitals"`, `"summary-chip-problems"`.

### 5.3 Table Specification

Component: `mat-table` with `mat-sort` on name and BP columns. No pagination — ward overview loads all active patients in one request (bounded by ward, typically < 50).

| Column | Source field | `data-testid` | Notes |
|---|---|---|---|
| # | Row index + 1 | — | Not sortable |
| Patient | `lastName + ', ' + firstName` | `patient-row` | Clickable → `/patients/{patientId}` |
| Status | `status` | `status-chip-{STATUS}` | Status chip component |
| BP | `latestVitals.systolicBp / diastolicBp` | `vitals-cell-bp` | "—" if null |
| HR | `latestVitals.heartRate` | `vitals-cell-heartRate` | "—" if null |
| Temp | `latestVitals.temperature` | `vitals-cell-temperature` | "—" if null |
| SpO₂ | `latestVitals.oxygenSaturation` | `vitals-cell-spo2` | "—" if null |
| Prb | `activeProblemCount` | `active-problem-count` | Right-aligned integer |

Flagged rows: if `flags.length > 0`, the row gets `class="row-flagged"` → left border `3px solid accent-600` and background `#FFF8E1`.
Critical rows: if any red-level flag (e.g. `SpO₂ < 93`, `systolicBp > 180`), left border `3px solid warn-700` and background `#FFEBEE`.

The full table div: `data-testid="ward-overview-table"`.

### 5.4 Refresh

Refresh button in the page header actions slot triggers a re-fetch. Shows loading spinner overlay on the table only (not full page). `data-testid="refresh-btn"`.

### 5.5 Ward Selector Interaction

When the toolbar ward selector changes, the router navigates to `/ward/{newWardId}` and the component reloads via `ActivatedRoute.paramMap` subscription.

### 5.6 States

| State | Trigger | UI |
|---|---|---|
| Loading | Initial + refresh | Spinner overlay on table |
| Populated | API returns patients | Full table |
| Empty | API returns `patientCount: 0` | Empty state: "No active patients in {wardId}" |
| Error | HTTP error | Error state with retry |

---

## 6. Patient List Page

Route: `/patients`
Component: `PatientListComponent`
API: `GET /api/v1/patients?ward=&status=&search=&page=&size=20`

### 6.1 Layout

```
┌───────────────────────────────────────────────────────────────────┐
│  👤  Patients                               [+ New Patient (stub)] │  PageHeader
├───────────────────────────────────────────────────────────────────┤
│  [🔍 Search by name or personnummer]  [Ward ▾]  [Status ▾]         │  Filter bar
├──────────────────┬──────────────┬────────┬──────────┬─────────────┤
│  Patient         │  Personnummer│  Ward  │  Status  │  DoB        │
├──────────────────┼──────────────┼────────┼──────────┼─────────────┤
│  Lindström, Anna │ 19850315-1234│  ICU   │  ACTIVE  │  1985-03-15 │
│  Berg, Erik      │ 19601022-5678│  CARD  │  ACTIVE  │  1960-10-22 │
├──────────────────┴──────────────┴────────┴──────────┴─────────────┤
│  ← 1–20 of 47                                            [1][2][3]►│  Paginator
└───────────────────────────────────────────────────────────────────┘
```

### 6.2 Filter Bar

Three controls in a single row:

**Search input** (`mat-form-field` with `mat-icon` prefix `search`):
- Placeholder: "Search patients…"
- Applies `debounceTime(300)` + `distinctUntilChanged()` + `switchMap` to call the API
- Clears with ✕ icon (appears when input is non-empty)
- `data-testid="search-input"`

**Ward filter** (`mat-select`):
- Options: All Wards, ICU, CARDIOLOGY, ONCOLOGY, NEUROLOGY
- Default: "All Wards" (no filter parameter sent)
- `data-testid="ward-filter"`

**Status filter** (`mat-select`):
- Options: All Statuses, ACTIVE, DISCHARGED, TRANSFERRED
- Default: "All Statuses"
- `data-testid="status-filter"`

All three filters are `signal()`s. A `computed()` derives the query params object and triggers re-fetch via `toSignal()`.

### 6.3 Table Specification

`mat-table` with `mat-sort`. Default sort: `lastName ASC`.

| Column | Source field | Sortable | `data-testid` |
|---|---|---|---|
| Patient | `lastName + ', ' + firstName` | Yes | `patient-row` |
| Personnummer | `personalNumber` | No | — |
| Ward | `ward` | Yes | — |
| Status | `status` | No | `status-chip-{STATUS}` |
| Date of Birth | `dateOfBirth` (formatted `YYYY-MM-DD`) | Yes | — |

Clicking any row navigates to `/patients/{id}`.

### 6.4 Pagination

`mat-paginator` with `pageSize: 20`, `pageSizeOptions: [10, 20, 50]`. Binds to `PagedResponse.totalElements` from the API. `data-testid="patient-paginator"`.

### 6.5 "New Patient" Button

This is a stub in Sprint 3 (navigates to a blank form that is not yet implemented). Renders `mat-raised-button` with `color="primary"` in the page header actions slot. `data-testid="new-patient-btn"`. Opens a `MatDialog` containing a minimal form — out of scope for Sprint 3; show "Coming soon" dialog body.

### 6.6 States

Same loading / error / empty pattern as §4. Empty state hint: "No patients match your search. Try adjusting the filters."

---

## 7. Patient Detail Page

Route: `/patients/:id`
Component: `PatientDetailComponent`
APIs:
- `GET /api/v1/patients/{id}` — demographics
- `GET /api/v1/ehr/subject/{id}` — EHR root
- `GET /api/v1/ehr/{ehrId}/compositions` — compositions list
- `GET /api/v1/ehr/{ehrId}/vitals` — all vital signs
- `GET /api/v1/ehr/{ehrId}/problems?status=ACTIVE` — problem list

### 7.1 Layout

```
┌──────────────────────────────────────────────────────────────────┐
│  ← Back    Anna Lindström      ACTIVE  F  1985-03-15  19850315-1234│  Header card
│            ICU  •  EHR: 3fa85f64-...                              │
├──────────────────────────────────────────────────────────────────┤
│  [Overview] [Vitals] [Problems] [Compositions]                    │  mat-tab-group
├──────────────────────────────────────────────────────────────────┤
│  [Tab content — see §7.2–7.5]                                     │
└──────────────────────────────────────────────────────────────────┘
```

**Header card** — `mat-card` without shadow (`elevation: 0`), horizontal layout:

Left section:
- Name (headline-5): `{firstName} {lastName}`
- Subtitle row: `{ward}  •  EHR: {ehrId}`
- Personal number and date of birth in caption

Right section (chips row):
- Status chip (see §3.5)
- Gender chip: `mat-chip` with `M` / `F` / `OTHER`

`data-testid="patient-detail-header"`.

Back button: `mat-icon-button` with `arrow_back` icon, navigates to `/patients`.
`data-testid="back-btn"`.

**Tab group** — `mat-tab-group` with `[selectedIndex]` bound to query param `?tab=0|1|2|3` so that deep-link and browser back work correctly.

`data-testid`: `"tab-overview"`, `"tab-vitals"`, `"tab-problems"`, `"tab-compositions"`.

### 7.2 Overview Tab

Two-column card layout (side by side on ≥ 768px; stacked on mobile):

**Left card — Demographics**

```
┌──────────────────────────────┐
│  Demographics                │
│  First name   Anna           │
│  Last name    Lindström      │
│  Personnummer 19850315-1234  │
│  Date of birth 1985-03-15    │
│  Gender       Female         │
│  Ward         ICU            │
│  Status       ● ACTIVE       │
│  Record created 2024-01-15   │
└──────────────────────────────┘
```

**Right card — Latest Vitals Snapshot**

Reuses `VitalSignsChartComponent` in compact mode but as a 2×3 grid of value cards:

```
┌──────────┬──────────┬──────────┐
│  BP      │  HR      │  RR      │
│  120/78  │  72 bpm  │  16/min  │
├──────────┼──────────┼──────────┤
│  Temp    │  SpO₂    │  Weight  │
│  37.0°C  │  98%     │  72 kg   │
└──────────┴──────────┴──────────┘
recorded 2024-01-15 08:30 by ...
```

Each value card has colour-coding as per normal ranges. Shows "No vitals recorded" if none exist.

`data-testid="overview-vitals-grid"`.

### 7.3 Vitals Tab

`data-testid="tab-content-vitals"`.

**Action bar**:
```
[+ Add Vital Signs]                          [Show: Last 10 ▾]
```
`data-testid="create-vitals-btn"` — opens `MatDialog` with `VitalsFormComponent` (see §8.1).
Range selector (`mat-select`): Last 10 / Last 30 / All. Controls the `?from=&to=` params.

**Chart area**: `VitalSignsChartComponent` in full mode — line chart of last 10 readings (or selected range) with the dense table beneath.

**Dense readings table**:

| Recorded | BP | HR | RR | Temp | SpO₂ | Weight |
|---|---|---|---|---|---|---|
| 2024-01-15 08:30 | 120/78 | 72 | 16 | 37.0°C | 98% | 72 kg |

No pagination — cap at 50 rows. Sort: newest first.

### 7.4 Problems Tab

`data-testid="tab-content-problems"`.

**Action bar**:
```
[+ Add Problem]          [Status filter: ACTIVE ▾]
```
`data-testid="add-problem-btn"` — opens `MatDialog` with `ProblemFormComponent` (see §8.2).

**Problem list table**:

| ICD-10 | Diagnosis | Severity | Status | Onset | Recorded |
|---|---|---|---|---|---|
| I10 | Essential Hypertension | MODERATE | ACTIVE | 2020-05-01 | 2024-01-10 |
| E11 | Type 2 Diabetes | MILD | ACTIVE | 2018-03-15 | 2024-01-10 |

`data-testid="problem-row"` on each `<tr>`.

Severity column: `SeverityBadgeComponent`.
Status column: `mat-chip` (same pattern as patient status but problem-specific).

No pagination — problem lists are typically short (< 20 entries).

When `status` filter is "ALL", resolved/refuted entries show with reduced opacity (`opacity: 0.6`).

### 7.5 Compositions Tab

`data-testid="tab-content-compositions"`.

**Action bar**:
```
[Type filter: All ▾]
```

**Compositions table**:

| Type | Facility | Start time | Commit time | Status |
|---|---|---|---|---|
| ENCOUNTER_NOTE | Central Hospital | 2024-01-15 06:00 | 2024-01-15 08:30 | COMPLETE |

`data-testid="composition-row"` on each `<tr>`.

Clicking a row expands an inline panel (`mat-expansion-panel` inside the row) showing:
- Composition ID (UUID)
- Author ID
- startTime vs commitTime explained (tooltip on `(i)` icon: "startTime is when the encounter occurred; commitTime is when it was saved to the system")

No add/edit form for compositions in Sprint 3 — read-only.

Pagination: `mat-paginator` with `pageSize: 10`. `data-testid="composition-paginator"`.

---

## 8. Form Dialogs

### 8.1 Add Vital Signs Dialog

Component: `VitalsFormComponent` inside `MatDialog`
API: `POST /api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals`

> **Pre-condition**: requires an existing Composition. The dialog will first create a new `ENCOUNTER_NOTE` composition (POST to `/api/v1/ehr/{ehrId}/compositions`), then create the vital signs inside it. This is transparent to the user.

**Dialog dimensions**: 520px wide, `mat-dialog-content` scrollable.

```
┌──────────────────────────────────────────────┐
│  Add Vital Signs                          ✕  │  mat-dialog-title
├──────────────────────────────────────────────┤
│  Recorded at*  [2024-01-15 08:30]            │
│                                              │
│  Blood Pressure                              │
│  Systolic (mmHg)*  [____]  Diastolic (mmHg) [____]│
│                                              │
│  Heart Rate (bpm)  [____]                    │
│  Respiratory Rate (/min) [____]              │
│  Temperature (°C)  [____]                    │
│  SpO₂ (%)          [____]                    │
│  Weight (kg)       [____]                    │
│                                              │
│  * At least one measurement must be provided │
├──────────────────────────────────────────────┤
│                     [Cancel] [Save Reading]  │  mat-dialog-actions
└──────────────────────────────────────────────┘
```

**Field spec**:

| Field | Control | Validators | `data-testid` |
|---|---|---|---|
| Recorded at | `mat-datepicker` + time input | Required | `vitals-recorded-at` |
| Systolic BP | `input[type=number]` | min 0, max 300 | `vitals-systolic` |
| Diastolic BP | `input[type=number]` | min 0, max 200 | `vitals-diastolic` |
| Heart Rate | `input[type=number]` | min 0, max 300 | `vitals-heart-rate` |
| Respiratory Rate | `input[type=number]` | min 0, max 60 | `vitals-resp-rate` |
| Temperature | `input[type=number]` | min 30, max 45, step 0.1 | `vitals-temperature` |
| SpO₂ | `input[type=number]` | min 0, max 100, step 0.1 | `vitals-spo2` |
| Weight | `input[type=number]` | min 0, max 500, step 0.1 | `vitals-weight` |

Cross-field validator: at least one measurement field must be non-null. Show error at form level if none filled.

Submit button: `data-testid="submit-btn"`. Disabled when form invalid or loading.

On success: close dialog, emit `{ refreshVitals: true }` from `afterClosed()`, parent component refreshes vitals data and shows sparkline update.

### 8.2 Add Problem Dialog

Component: `ProblemFormComponent` inside `MatDialog`
API: `POST /api/v1/ehr/{ehrId}/problems`

> **Pre-condition**: same as 8.1 — creates a composition first, then the problem entry.

```
┌──────────────────────────────────────────────┐
│  Add Problem                              ✕  │
├──────────────────────────────────────────────┤
│  ICD-10 Code*      [I10        ]             │
│  Diagnosis name*   [Essential Hypertension ] │
│  Severity*         [MODERATE ▾]              │
│  Status            [ACTIVE ▾]                │
│  Onset date        [2024-01-10]              │
│                                              │
├──────────────────────────────────────────────┤
│                        [Cancel] [Add Problem]│
└──────────────────────────────────────────────┘
```

**Field spec**:

| Field | Control | Validators | `data-testid` |
|---|---|---|---|
| ICD-10 Code | `input[type=text]` | Required, pattern `/^[A-Z]\d{2}(\.\d{1,2})?$/` | `icd10-input` |
| Display Name | `input[type=text]` | Required, maxLength 200 | `display-name-input` |
| Severity | `mat-select` | Required | `severity-select` |
| Status | `mat-select` | Required, default ACTIVE | `status-select` |
| Onset date | `mat-datepicker` | Optional, no future dates | `onset-date-input` |

Severity options: MILD, MODERATE, SEVERE (displayed as `SeverityBadgeComponent` in the options panel).

ICD-10 validation error text: "Invalid ICD-10 format. Examples: I10, E11, J45.9"

Submit button: `data-testid="submit-btn"`. On success: close dialog, parent problem list refreshes.

---

## 9. Admin Page

Route: `/admin`
Component: `AdminComponent`

### 9.1 Layout

```
┌──────────────────────────────────────────────────────────────────┐
│  ⚙  System Administration                                        │
├──────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │  Grafana        │  │  RabbitMQ UI    │  │  Actuator       │  │
│  │  Monitoring     │  │  Message Broker │  │  Health & Info  │  │
│  │  [Open →]       │  │  [Open →]       │  │  [Open →]       │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  API Documentation                                          │  │
│  │  Swagger UI (OpenAPI 3)                [Open Swagger UI →] │  │
│  └────────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│  System Health                                                    │
│  Status: ● UP           DB: ● UP      Broker: ● UP               │
└──────────────────────────────────────────────────────────────────┘
```

### 9.2 External Link Cards

Three `mat-card` elements in a responsive `grid` (3 columns ≥ 960px; 1 column on mobile):

| Card | Link | `data-testid` |
|---|---|---|
| Grafana | `http://localhost:3000` | `admin-grafana-link` |
| RabbitMQ Management | `http://localhost:15672` | `admin-rabbitmq-link` |
| Spring Actuator | `http://localhost:8080/actuator` | `admin-actuator-link` |

All open in a new tab (`target="_blank" rel="noopener"`).

### 9.3 API Documentation Card

Full-width `mat-card`. Link to `http://localhost:8080/swagger-ui.html`. `data-testid="admin-swagger-link"`.

### 9.4 Health Status

Polls `GET /api/actuator/health` on component init. Displays a `mat-chip` per component:

| Indicator | Colour |
|---|---|
| UP | `status-active` green |
| DOWN | `warn-700` red |
| Unknown / loading | grey |

`data-testid="health-status"`.

---

## 10. Angular Services — API Contract

All services return `Observable<T>`. HTTP errors are caught in `ApiInterceptor` and surfaced as `MatSnackBar` notifications. Services do not handle errors — they propagate so the component can update its error signal.

Services are co-located with their feature (§14). Only services used across multiple features live in `core/`.

### 10.1 `WardOverviewService`

```typescript
getOverview(wardId: string): Observable<WardOverviewResponse>
// GET /api/v1/ward/{wardId}/overview
```

### 10.2 `PatientService`

```typescript
getPatients(params: PatientQueryParams): Observable<PagedResponse<PatientResponse>>
// GET /api/v1/patients?search=&ward=&status=&page=&size=

getPatient(id: string): Observable<PatientResponse>
// GET /api/v1/patients/{id}
```

### 10.3 `EhrService`

```typescript
getEhrBySubject(patientId: string): Observable<EhrResponse>
// GET /api/v1/ehr/subject/{patientId}
```

### 10.4 `VitalSignsService`

```typescript
getVitals(ehrId: string, params?: VitalsQueryParams): Observable<VitalSignsResponse[]>
// GET /api/v1/ehr/{ehrId}/vitals?from=&to=

getLatestVitals(ehrId: string): Observable<VitalSignsResponse>
// GET /api/v1/ehr/{ehrId}/vitals/latest

createVitals(ehrId: string, compositionId: string, body: VitalSignsRequest): Observable<VitalSignsResponse>
// POST /api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals
```

### 10.5 `CompositionService`

```typescript
getCompositions(ehrId: string, params?: CompositionQueryParams): Observable<PagedResponse<CompositionResponse>>
// GET /api/v1/ehr/{ehrId}/compositions?type=&page=&size=

createComposition(ehrId: string, body: CompositionRequest): Observable<CompositionResponse>
// POST /api/v1/ehr/{ehrId}/compositions
```

### 10.6 `ProblemListService`

```typescript
getProblems(ehrId: string, status?: string): Observable<ProblemResponse[]>
// GET /api/v1/ehr/{ehrId}/problems?status=

createProblem(ehrId: string, body: ProblemRequest): Observable<ProblemResponse>
// POST /api/v1/ehr/{ehrId}/problems
```

---

## 11. TypeScript Interface Definitions

All interfaces live in `src/app/core/models/`. Never declare interfaces inside component or service files.

```typescript
// models/patient.model.ts
export interface PatientResponse {
  id: string;
  firstName: string;
  lastName: string;
  personalNumber: string;
  dateOfBirth: string;          // ISO date string
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  ward: string;
  status: 'ACTIVE' | 'DISCHARGED' | 'TRANSFERRED';
  createdAt: string;
  updatedAt: string;
}

// models/ehr.model.ts
export interface EhrResponse {
  ehrId: string;
  subjectId: string;
  systemId: string;
  createdAt: string;
  status: 'ACTIVE' | 'INACTIVE';
}

// models/vital-signs.model.ts
export interface VitalSignsResponse {
  id: string;
  compositionId: string;
  recordedAt: string;
  recordedBy: string;
  systolicBp: number | null;
  diastolicBp: number | null;
  heartRate: number | null;
  respiratoryRate: number | null;
  temperature: number | null;
  oxygenSaturation: number | null;
  weight: number | null;
}

export interface VitalSignsRequest {
  recordedAt: string;
  systolicBp?: number;
  diastolicBp?: number;
  heartRate?: number;
  respiratoryRate?: number;
  temperature?: number;
  oxygenSaturation?: number;
  weight?: number;
}

// models/problem.model.ts
export interface ProblemResponse {
  id: string;
  ehrId: string;
  compositionId: string;
  icd10Code: string;
  displayName: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  status: 'ACTIVE' | 'INACTIVE' | 'RESOLVED' | 'REFUTED';
  onsetDate: string | null;
  resolvedDate: string | null;
  recordedAt: string;
}

export interface ProblemRequest {
  compositionId: string;
  icd10Code: string;
  displayName: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  status: 'ACTIVE' | 'INACTIVE' | 'RESOLVED' | 'REFUTED';
  onsetDate?: string;
}

// models/composition.model.ts
export type CompositionType = 'ENCOUNTER_NOTE' | 'ADMISSION' | 'PROGRESS_NOTE' | 'DISCHARGE_SUMMARY';
export type CompositionStatus = 'COMPLETE' | 'INCOMPLETE' | 'AMENDED';

export interface CompositionResponse {
  id: string;
  ehrId: string;
  type: CompositionType;
  authorId: string;
  startTime: string;
  commitTime: string;
  facilityName: string;
  status: CompositionStatus;
}

export interface CompositionRequest {
  type: CompositionType;
  facilityName: string;
  startTime: string;
}

// models/ward-overview.model.ts
export interface WardOverviewResponse {
  wardId: string;
  patientCount: number;
  patients: PatientSummary[];
}

export interface PatientSummary {
  patientId: string;
  ehrId: string;
  firstName: string;
  lastName: string;
  status: 'ACTIVE' | 'DISCHARGED' | 'TRANSFERRED';
  latestVitals: LatestVitals | null;
  activeProblemCount: number;
  flags: string[];
}

export interface LatestVitals {
  recordedAt: string;
  systolicBp: number | null;
  diastolicBp: number | null;
  heartRate: number | null;
  temperature: number | null;
  oxygenSaturation: number | null;
}

// models/paged-response.model.ts
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

---

## 12. Consolidated `data-testid` Inventory

All selectors used by Playwright tests. This is the authoritative list — update both here and in [`testing-e2e.md`](testing-e2e.md) if changed.

### Shell
| Attribute | Element | Page |
|---|---|---|
| `nav-toggle` | Hamburger button | All |
| `ward-selector` | Toolbar ward dropdown | All |
| `admin-link` | Toolbar admin icon button | All |
| `user-indicator` | User label | All |
| `nav-ward` | Sidenav ward link | All |
| `nav-patients` | Sidenav patients link | All |
| `nav-admin` | Sidenav admin link | All |

### Shared Components
| Attribute | Element |
|---|---|
| `page-header` | PageHeaderComponent root |
| `loading-spinner` | LoadingSpinnerComponent root |
| `error-state` | Error state container |
| `retry-btn` | Retry button in error state |
| `empty-state` | Empty state container |
| `severity-badge-MILD` / `MODERATE` / `SEVERE` | SeverityBadgeComponent |
| `status-chip-ACTIVE` / `DISCHARGED` / `TRANSFERRED` | Status chip |

### Ward Overview
| Attribute | Element |
|---|---|
| `ward-overview-table` | Table root |
| `patient-row` | Each `<tr>` |
| `summary-chip-flagged` | Flagged patients chip |
| `summary-chip-no-vitals` | No vitals chip |
| `summary-chip-problems` | Problems chip |
| `vitals-cell-bp` | BP cell |
| `vitals-cell-heartRate` | HR cell |
| `vitals-cell-temperature` | Temp cell |
| `vitals-cell-spo2` | SpO₂ cell |
| `vital-flag-badge` | Flag icon in any vitals cell |
| `active-problem-count` | Problem count cell |
| `refresh-btn` | Refresh button |

### Patient List
| Attribute | Element |
|---|---|
| `search-input` | Search text field |
| `ward-filter` | Ward dropdown |
| `status-filter` | Status dropdown |
| `patient-row` | Each table row |
| `patient-paginator` | Paginator |
| `new-patient-btn` | New Patient button |

### Patient Detail
| Attribute | Element |
|---|---|
| `patient-detail-header` | Header card |
| `back-btn` | Back navigation button |
| `tab-overview` | Overview tab |
| `tab-vitals` | Vitals tab |
| `tab-problems` | Problems tab |
| `tab-compositions` | Compositions tab |
| `tab-content-vitals` | Vitals tab content panel |
| `tab-content-problems` | Problems tab content panel |
| `tab-content-compositions` | Compositions tab content panel |
| `overview-vitals-grid` | Overview vitals snapshot grid |
| `create-vitals-btn` | Add Vital Signs button |
| `add-problem-btn` | Add Problem button |
| `problem-row` | Each problem list row |
| `composition-row` | Each composition list row |
| `composition-paginator` | Compositions paginator |

### Forms (dialogs)
| Attribute | Element |
|---|---|
| `vitals-recorded-at` | Recorded at field |
| `vitals-systolic` | Systolic BP input |
| `vitals-diastolic` | Diastolic BP input |
| `vitals-heart-rate` | Heart rate input |
| `vitals-resp-rate` | Respiratory rate input |
| `vitals-temperature` | Temperature input |
| `vitals-spo2` | SpO₂ input |
| `vitals-weight` | Weight input |
| `icd10-input` | ICD-10 code input |
| `display-name-input` | Diagnosis name input |
| `severity-select` | Severity dropdown |
| `status-select` | Status dropdown |
| `onset-date-input` | Onset date picker |
| `submit-btn` | Submit button in any dialog |

### Admin
| Attribute | Element |
|---|---|
| `admin-grafana-link` | Grafana card link |
| `admin-rabbitmq-link` | RabbitMQ card link |
| `admin-actuator-link` | Actuator card link |
| `admin-swagger-link` | Swagger UI card link |
| `health-status` | Health status container |

---

## 13. Responsive Breakpoints

| Breakpoint | Width | Layout changes |
|---|---|---|
| Mobile | < 600px | Sidenav collapses to overlay; tables collapse to card list |
| Tablet | 600–959px | Sidenav overlay; 2-column card grids |
| Desktop | ≥ 960px | Sidenav fixed; 3-column admin cards; full tables |
| Wide | ≥ 1280px | No additional changes; max-content-width: 1440px centred |

Use Angular CDK `BreakpointObserver` — not CSS-only media queries — for sidenav collapse logic so Angular can manage the `MatSidenav` state.

---

## 14. File Structure Checklist

Sprint 3 deliverables. No NgModule files — everything is standalone.

```
frontend/
├── package.json              # @angular/core@19, @angular/material@19, rxjs, ngx-charts
├── angular.json
├── tsconfig.json
├── proxy.conf.json           # /api → http://localhost:8080
├── .gitignore
└── src/
    ├── styles.scss           # Material theme, CSS custom properties (--nc-*), global resets
    ├── index.html
    ├── main.ts               # bootstrapApplication(AppComponent, appConfig)
    └── app/
        ├── app.config.ts     # provideRouter, provideHttpClient, provideAnimationsAsync
        ├── app.routes.ts     # loadComponent() lazy routes; canActivate: [authGuard]
        ├── app.component.ts  # mat-sidenav-container shell
        ├── app.component.html
        ├── app.component.scss
        │
        ├── core/
        │   ├── auth/
        │   │   ├── auth.service.ts
        │   │   └── auth.guard.ts             # CanActivateFn
        │   ├── interceptors/
        │   │   └── api.interceptor.ts        # HttpInterceptorFn
        │   ├── notifications/
        │   │   └── notification.service.ts
        │   └── models/                       # All interfaces from §11
        │       ├── patient.model.ts
        │       ├── ehr.model.ts
        │       ├── vital-signs.model.ts
        │       ├── problem.model.ts
        │       ├── composition.model.ts
        │       ├── ward-overview.model.ts
        │       └── paged-response.model.ts
        │
        ├── shared/
        │   ├── utils/
        │   │   └── vital-flags.util.ts       # Pure functions for normal-range logic
        │   ├── page-header/
        │   │   ├── page-header.component.ts
        │   │   ├── page-header.component.html
        │   │   └── page-header.component.scss
        │   ├── loading-spinner/
        │   │   ├── loading-spinner.component.ts
        │   │   └── loading-spinner.component.scss
        │   ├── severity-badge/
        │   │   ├── severity-badge.component.ts
        │   │   └── severity-badge.component.scss
        │   └── vital-signs-chart/
        │       ├── vital-signs-chart.component.ts
        │       ├── vital-signs-chart.component.html
        │       └── vital-signs-chart.component.scss
        │
        └── features/
            ├── ward/
            │   ├── ward-overview.service.ts
            │   └── ward-overview/
            │       ├── ward-overview.component.ts
            │       ├── ward-overview.component.html
            │       └── ward-overview.component.scss
            │
            ├── patients/
            │   ├── patient.service.ts
            │   ├── patient-list/
            │   │   ├── patient-list.component.ts
            │   │   ├── patient-list.component.html
            │   │   └── patient-list.component.scss
            │   └── patient-detail/
            │       ├── ehr.service.ts
            │       ├── vital-signs.service.ts
            │       ├── composition.service.ts
            │       ├── problem-list.service.ts
            │       ├── patient-detail.component.ts
            │       ├── patient-detail.component.html
            │       ├── patient-detail.component.scss
            │       ├── vitals-form/
            │       │   ├── vitals-form.component.ts
            │       │   └── vitals-form.component.html
            │       └── problem-form/
            │           ├── problem-form.component.ts
            │           └── problem-form.component.html
            │
            └── admin/
                ├── admin.component.ts
                ├── admin.component.html
                └── admin.component.scss
```

---

*Last updated: Sprint 3 — 2026-05-16. Update §12 inventory whenever a new `data-testid` is added.*
