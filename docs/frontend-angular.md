---
description: Angular frontend conventions — standalone components, signals, functional guards/interceptors, feature-based structure, mock JWT auth
globs: frontend/src/**/*.ts,frontend/src/**/*.html
alwaysApply: false
---

# Angular Frontend Conventions

Angular 19. All components are **standalone** — no NgModules. Bootstrap via `bootstrapApplication()`. Zone.js is still included but `provideZonelessChangeDetection()` is available as a drop-in replacement.

## Application Bootstrap

```typescript
// main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig).catch(console.error);
```

```typescript
// app/app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withInterceptors([apiInterceptor])),
    provideAnimationsAsync(),
    // optional: drop Zone.js entirely — requires OnPush everywhere
    // provideZonelessChangeDetection(),
  ],
};
```

Use `withHashLocation()` — avoids Nginx `try_files` complexity for SPA deep-link refresh.

## Feature-Based Folder Structure

No `CoreModule` or `SharedModule`. Services are `providedIn: 'root'` (singletons). Shared components are standalone and imported directly where used.

```
src/app/
├── main.ts
├── app.config.ts              # providers: router, httpClient, animations
├── app.routes.ts              # top-level route definitions
├── app.component.ts           # root component; <router-outlet>
│
├── core/                      # singleton services and interceptors (no module)
│   ├── auth/
│   │   ├── auth.service.ts    # providedIn: 'root'
│   │   └── auth.guard.ts      # CanActivateFn (functional)
│   ├── interceptors/
│   │   └── api.interceptor.ts # HttpInterceptorFn (functional)
│   └── notifications/
│       └── notification.service.ts
│
├── shared/                    # standalone components imported per-use
│   ├── page-header/
│   ├── loading-spinner/
│   ├── severity-badge/        # MILD/MODERATE/SEVERE + ROUTINE/URGENT/CRITICAL chips
│   └── vital-signs-chart/     # sparkline; cells amber/red outside normal range
│
└── features/
    ├── ward/                  # /ward/:wardId — WardOverviewComponent
    ├── patients/              # /patients, /patients/:id
    │   ├── patient-list/
    │   └── patient-detail/    # tabs: Overview | Vitals | Problems | Compositions
    └── admin/                 # /admin — system links
```

## Routing

```typescript
// app/app.routes.ts
export const routes: Routes = [
  { path: '', redirectTo: 'ward/ICU', pathMatch: 'full' },
  {
    path: 'ward/:wardId',
    loadComponent: () => import('./features/ward/ward-overview.component')
      .then(m => m.WardOverviewComponent),
    canActivate: [authGuard],
  },
  {
    path: 'patients',
    loadComponent: () => import('./features/patients/patient-list/patient-list.component')
      .then(m => m.PatientListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'patients/:id',
    loadComponent: () => import('./features/patients/patient-detail/patient-detail.component')
      .then(m => m.PatientDetailComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin.component')
      .then(m => m.AdminComponent),
    canActivate: [authGuard],
  },
];
```

Use `loadComponent()` for lazy loading — each component is a separate JS chunk.

## Auth — Functional Guard + Mock JWT

```typescript
// core/auth/auth.guard.ts
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  if (!sessionStorage.getItem('token')) {
    return router.createUrlTree(['/login']);
  }
  return true;
};
```

```typescript
// core/auth/auth.service.ts
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'token';

  login(): void {
    sessionStorage.setItem(this.tokenKey, 'eyJhbGciOiJIUzI1NiJ9.mock.signature');
  }

  logout(): void {
    sessionStorage.removeItem(this.tokenKey);
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.tokenKey);
  }
}
```

## HTTP Interceptor — Functional

```typescript
// core/interceptors/api.interceptor.ts
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const notifications = inject(NotificationService);

  const token = auth.getToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError(err => {
      notifications.error(`API error: ${err.status}`);
      return throwError(() => err);
    }),
  );
};
```

Register in `app.config.ts` via `provideHttpClient(withInterceptors([apiInterceptor]))`.

## Standalone Components

All components set `standalone: true` and import their own dependencies.

```typescript
@Component({
  selector: 'app-patient-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatTableModule,
    MatPaginatorModule,
    MatInputModule,
    MatSelectModule,
    SeverityBadgeComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './patient-list.component.html',
})
export class PatientListComponent {
  private readonly patientService = inject(PatientService);
  private readonly router = inject(Router);

  protected readonly patients = signal<PatientSummary[]>([]);
  protected readonly loading = signal(false);
  protected readonly search = signal('');

  protected readonly filteredPatients = computed(() =>
    this.patients().filter(p =>
      p.lastName.toLowerCase().includes(this.search().toLowerCase())
    )
  );
}
```

Rules:
- **`inject()`** instead of constructor injection
- **`protected`** members for template access — avoid `public`
- **`ChangeDetectionStrategy.OnPush`** on all components
- **`signal()`** for mutable state, **`computed()`** for derived state

## Signals

```typescript
// Writable signal
readonly count = signal(0);

// Computed — derived, memoised, read-only
readonly doubled = computed(() => this.count() * 2);

// linkedSignal — writable computed that resets when source changes (Angular 19)
readonly selectedWard = linkedSignal(() => this.wards()[0] ?? null);

// Effect — side effects; use sparingly
constructor() {
  effect(() => console.log('count changed:', this.count()));
}

// Update
increment() { this.count.update(v => v + 1); }
reset()     { this.count.set(0); }
```

Convert Observables to signals at the component boundary with `toSignal()`:

```typescript
protected readonly patients = toSignal(
  this.patientService.getAll(),
  { initialValue: [] as PatientSummary[] },
);
```

## Template Control Flow (`@if`, `@for`, `@switch`, `@defer`, `@let`)

Use built-in control flow — do **not** use `*ngIf`, `*ngFor`, or `NgIf`/`NgFor` imports.

```html
@if (loading()) {
  <app-loading-spinner />
} @else if (patients().length === 0) {
  <p>No patients found.</p>
} @else {
  <table mat-table [dataSource]="patients()" data-testid="ward-overview-table">
    @for (patient of patients(); track patient.id) {
      <tr mat-row data-testid="patient-row" (click)="navigate(patient.id)"></tr>
    } @empty {
      <tr><td colspan="6">No results</td></tr>
    }
  </table>
}
```

```html
@switch (severity()) {
  @case ('SEVERE')   { <mat-chip color="warn">SEVERE</mat-chip> }
  @case ('MODERATE') { <mat-chip color="accent">MODERATE</mat-chip> }
  @default           { <mat-chip>MILD</mat-chip> }
}
```

**`@let`** — declare template-local variables (Angular 19):

```html
@let vitals = patient().latestVitals;
@let flagged = vitals.systolicBP > 140 || vitals.heartRate > 100;

<span [class.warn]="flagged">{{ vitals.systolicBP }} / {{ vitals.diastolicBP }}</span>
```

**`@defer`** — lazy-load heavy components after interaction or on idle:

```html
@defer (on interaction) {
  <app-vital-signs-chart [readings]="readings()" />
} @placeholder {
  <button>Load chart</button>
} @loading (minimum 300ms) {
  <app-loading-spinner />
}
```

## Input / Output / Model

Use the signal-based I/O functions (Angular 17.1+, stable in Angular 19):

```typescript
// Signal inputs — type-safe, read-only inside component
readonly severity = input.required<string>();
readonly readings = input<VitalSigns[]>([]);

// Signal output
readonly selected = output<string>();

// model() — two-way binding (replaces [(ngModel)] for component I/O)
readonly value = model<string>('');
```

Two-way binding in parent template:

```html
<app-icd10-input [(value)]="icd10Code" />
```

## Signal-Based Queries

Replace `@ViewChild` / `@ContentChild` decorators with functional equivalents (stable in Angular 19):

```typescript
// Single element
readonly tableRef = viewChild.required<MatTable<PatientSummary>>('table');
readonly spinnerRef = viewChild(LoadingSpinnerComponent);

// Multiple elements
readonly rows = viewChildren('patientRow', { read: ElementRef });

// Content projection
readonly tab = contentChild.required(MatTab);
```

## Subscription Cleanup

Use `takeUntilDestroyed()` from `@angular/core/rxjs-interop` — no manual `ngOnDestroy` needed:

```typescript
export class PatientListComponent {
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => this.patientService.search(term ?? '')),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(results => this.patients.set(results));
  }
}
```

Or inject `DestroyRef` directly when not in a constructor:

```typescript
readonly results = toSignal(
  this.searchControl.valueChanges.pipe(
    debounceTime(300),
    switchMap(term => this.patientService.search(term ?? '')),
  ),
  { initialValue: [] },
);
```

`toSignal()` automatically unsubscribes on destroy — prefer it over manual subscription.

## Angular Services — HTTP Patterns

Services are `providedIn: 'root'`. They return `Observable<T>`. No raw `HttpClient` calls in components.

```typescript
@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/patients';

  getAll(page = 0, size = 20): Observable<PagedResponse<PatientResponse>> {
    return this.http.get<PagedResponse<PatientResponse>>(this.base, {
      params: { page, size },
    });
  }

  getById(id: string): Observable<PatientResponse> {
    return this.http.get<PatientResponse>(`${this.base}/${id}`);
  }
}
```

Use `debounceTime(300)` + `switchMap` on search inputs — see the `toSignal()` pattern above.

## Class and Style Bindings

Prefer native `[class]` and `[style]` over `NgClass`/`NgStyle`:

```html
<div [class.active]="isActive()" [class.flagged]="isFlagged()">

<div [class]="{ active: isActive(), 'mat-elevation-z4': elevated() }">

<span [style.color]="severity() === 'SEVERE' ? 'red' : null">
```

## Component Conventions

- All interactive elements have `data-testid` attributes — Playwright selects by these, not CSS classes
- Use `MatSnackBar` via `NotificationService` for all user feedback (no inline alert divs)
- `VitalSignsChartComponent` — `readings = input<VitalSigns[]>([])`; highlights cells outside normal range
- `SeverityBadgeComponent` — `severity = input.required<string>()`; outputs a colour-coded `mat-chip`
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
