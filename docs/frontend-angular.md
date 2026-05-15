---
description: Angular frontend conventions — NgModules pattern, module structure, component inventory, services, mock JWT auth
globs: cosmolab-frontend/src/**/*.ts,cosmolab-frontend/src/**/*.html
alwaysApply: false
---

# Angular Frontend Conventions

## Module Pattern — NgModules (not Standalone)

CosmoLab uses NgModules deliberately — this is the enterprise pattern and closer to
what legacy COSMIC runs. Do not use standalone components or the new `bootstrapApplication` API.

Three module types:
- **`CoreModule`** — singleton services and interceptors. Imported **once** in `AppModule` only.
  Throws an error if imported a second time (guard pattern in constructor).
- **`SharedModule`** — reusable components, pipes, directives. Imported in every feature module.
  Re-exports `CommonModule` and `ReactiveFormsModule` so feature modules don't need to.
- **Feature modules** — lazy-loaded via router. Each is a separate JS chunk in the production build.

## Module Structure

```
src/app/
├── app.module.ts              # imports CoreModule once; declares AppComponent
├── app-routing.module.ts      # loadChildren for all feature modules
├── core/
│   ├── core.module.ts
│   ├── guards/auth.guard.ts
│   ├── interceptors/api.interceptor.ts
│   └── services/
│       ├── auth.service.ts
│       └── notification.service.ts
├── shared/
│   ├── shared.module.ts
│   └── components/
│       ├── page-header/
│       ├── loading-spinner/
│       ├── severity-badge/      # MILD/MODERATE/SEVERE + ROUTINE/URGENT/CRITICAL chips
│       └── vital-signs-chart/   # sparkline; cells amber/red outside normal range
└── features/
    ├── ward/                    # /ward/:wardId  — WardOverviewComponent
    ├── patients/                # /patients, /patients/:id
    │   ├── patient-list/        # table, search, ward/status filter, paginator
    │   └── patient-detail/      # mat-tab-group: Overview | Vitals | Problems | Compositions
    └── admin/                   # /admin — system links
```

## Routing

```typescript
// app-routing.module.ts
const routes: Routes = [
  { path: '', redirectTo: 'ward/ICU', pathMatch: 'full' },
  { path: 'ward/:wardId', loadChildren: () => import('./features/ward/ward.module') },
  { path: 'patients',     loadChildren: () => import('./features/patients/patients.module') },
  { path: 'admin',        loadChildren: () => import('./features/admin/admin.module') },
];
```

Use `HashLocationStrategy` — avoids Nginx `try_files` complexity for SPA deep-link refresh.

## Auth — Mock JWT Pattern

**Decision 2 is deferred.** The pattern is scaffolded correctly; backend does not validate.

```typescript
// core/services/auth.service.ts
login(): void {
  const mockToken = 'eyJhbGciOiJIUzI1NiJ9.mock.signature';
  sessionStorage.setItem('token', mockToken);
}

// core/interceptors/api.interceptor.ts
intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
  const token = sessionStorage.getItem('token');
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;
  return next.handle(authReq).pipe(
    catchError(err => {
      this.notificationService.error(`API error: ${err.status}`);
      return throwError(() => err);
    })
  );
}

// core/guards/auth.guard.ts
canActivate(): boolean {
  if (!sessionStorage.getItem('token')) {
    this.router.navigate(['/login']);
    return false;
  }
  return true;
}
```

## Angular Services — HTTP Patterns

All services are in `core/services/` (singletons) or co-located in feature modules (scoped).
Services wrap HTTP calls and return `Observable<T>`. No raw `HttpClient` calls in components.

```typescript
// Example: vital-signs.service.ts
getLatestVitals(ehrId: string): Observable<VitalSignsResponse> {
  return this.http.get<VitalSignsResponse>(`/api/v1/ehr/${ehrId}/vitals/latest`);
}
```

Use `debounceTime(300)` on search inputs. Use `switchMap` for search + pagination.

## Component Conventions

- All interactive elements have `data-testid` attributes — Playwright selects by these, not CSS classes
- Use `MatSnackBar` via `NotificationService` for all user feedback (no inline alert divs)
- `VitalSignsChartComponent` — inputs: `readings: VitalSigns[]`; highlights cells outside normal range
- `SeverityBadgeComponent` — input: `severity: string`; outputs a colour-coded `mat-chip`
- `PatientDetailComponent` uses `mat-tab-group` — tabs: Overview, Vitals, Problems, Compositions

## Key data-testid Attributes (for Playwright)

```
data-testid="ward-overview-table"
data-testid="patient-row"
data-testid="search-input"
data-testid="ward-filter"
data-testid="status-chip-{STATUS}"
data-testid="patient-detail-header"
data-testid="tab-vitals" / "tab-problems" / "tab-compositions"
data-testid="vitals-cell-{measurement}"
data-testid="vital-flag-badge"
data-testid="problem-row"
data-testid="add-problem-btn"
data-testid="icd10-input"
data-testid="create-vitals-btn"
data-testid="submit-btn"
```
