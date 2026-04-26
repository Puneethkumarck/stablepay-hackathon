# CLAUDE.md — StablePay Sender Web App

## What This Is

Next.js sender app for StablePay cross-border remittance (USD → INR via USDC on Solana). Mobile-first PWA, dark theme, talks to a Spring Boot backend via BFF pattern.

## Deep-Dive Docs

Read the relevant doc before writing code:

| Topic | Doc |
|---|---|
| Coding conventions | [docs/FE_CODING_STANDARDS.md](../docs/FE_CODING_STANDARDS.md) |
| Testing rules | [docs/FE_TESTING_STANDARDS.md](../docs/FE_TESTING_STANDARDS.md) |
| Architecture patterns | [docs/FE_ARCHITECTURE_STANDARDS.md](../docs/FE_ARCHITECTURE_STANDARDS.md) |
| Design reference (mockups) | [stablepay-design-system/project/ui_kits/sender_app/](../stablepay-design-system/project/ui_kits/sender_app/) |
| Design tokens (CSS vars) | [stablepay-design-system/project/colors_and_type.css](../stablepay-design-system/project/colors_and_type.css) |
| API gaps vs design | [docs/specs/2026-04-23-001-sender-app-api-gaps-spec.md](../docs/specs/2026-04-23-001-sender-app-api-gaps-spec.md) |
| Backend API surface | [docs/openapi.json](../docs/openapi.json) |

## Build Commands

```bash
bun install              # install dependencies
bun run dev              # start dev server (Turbopack, localhost:3000)
bun run build            # production build
bun run test             # unit + component + integration tests (Vitest)
bun run test:watch       # watch mode
bun run test:coverage    # with coverage report
bun run test:e2e         # E2E tests (Playwright, requires dev server)
bun run test:e2e:ui      # Playwright interactive UI mode
bunx biome check --write . # lint + format (run before committing)
```

## Tech Stack

| Tech | Version | Notes |
|---|---|---|
| Next.js | 16.2.x | App Router, RSC, PPR, `use cache`, Turbopack |
| React | 19.2.x | Server Components, Actions, `use()`, ref-as-prop |
| TypeScript | 6.0.x | Strict mode, `isolatedDeclarations` |
| Tailwind CSS | 4.2.x | CSS-first config (`@theme inline`), no tailwind.config.ts |
| shadcn/ui | CLI v4 | Radix base, `<Field>` form pattern, Sonner toasts |
| TanStack Query | 5.100.x | Server state (queries + mutations) |
| Zustand | 5.0.x | Client state (send flow wizard, persisted to sessionStorage) |
| React Hook Form | 7.73.x | Uncontrolled forms with shadcn `<Field>` + `Controller` |
| Zod | 4.3.x | Schema validation |
| Motion | 12.38.x | Animation (`import from "motion/react"`) |
| Sonner | latest | Toast notifications |
| Biome | 2.4.x | Linter + formatter (replaces ESLint + Prettier) |
| Bun | latest | Package manager + runtime |

## Folder Structure

```
src/
├── app/                          # Next.js App Router
│   ├── (auth)/                   # Route group: no auth, no bottom nav
│   │   ├── login/page.tsx
│   │   ├── auth/callback/route.ts
│   │   ├── error.tsx
│   │   └── layout.tsx
│   ├── (app)/                    # Route group: auth required, bottom nav
│   │   ├── home/page.tsx
│   │   ├── send/                 # Multi-step: amount → recipient → review → sending
│   │   ├── activity/page.tsx
│   │   ├── detail/[id]/page.tsx
│   │   ├── add-funds/page.tsx
│   │   ├── me/page.tsx
│   │   ├── error.tsx
│   │   └── layout.tsx
│   ├── api/                      # Route Handlers (BFF proxy to Spring Boot)
│   ├── layout.tsx                # Root: fonts, ThemeProvider, QueryProvider, Toaster
│   ├── global-error.tsx          # Root layout error boundary
│   └── not-found.tsx
├── components/
│   ├── ui/                       # shadcn/ui (generated — do not hand-edit)
│   └── shared/                   # App-wide: bottom-nav, status-badge, amount-display
├── features/                     # Domain modules (vertical slices)
│   ├── auth/                     # components/, hooks/, types.ts
│   ├── wallet/
│   ├── remittance/
│   └── fx/
├── lib/                          # api-client.ts, auth.ts, errors.ts, format.ts, utils.ts
├── hooks/                        # App-wide custom hooks
├── stores/                       # Zustand stores (send-flow.ts)
├── styles/                       # design-tokens.css, globals.css
├── test-utils/                   # MSW server, handlers, renderWithProviders
├── test-setup.ts
└── types/                        # api.ts, common.ts
e2e/                              # Playwright E2E tests
```

## Critical Rules

### TypeScript

- **`strict: true`** with `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `isolatedDeclarations`
- **No `any`** — use `unknown` + type guards
- **No `as` casts** — except `as const`
- **No `enum`** — use `as const` objects or union types
- **Named exports only** — never `export default` (exception: Next.js pages/layouts/error boundaries)
- **No barrel exports** (`index.ts` re-exports) — breaks tree-shaking

### Components

- **Server Components by default** — `"use client"` only for interactivity, hooks, or browser APIs
- **Props as `interface`** — defined above the component
- **No prop spreading** — pass props explicitly
- **No inline styles** — Tailwind only
- **No `forwardRef`** — React 19 passes `ref` as a regular prop
- **Import with `@/`** — never relative paths with `../../../`

### Styling (Tailwind v4)

- **No `tailwind.config.ts`** — all config in CSS via `@theme inline`
- **Design tokens** registered in `styles/globals.css` as `@theme inline` block
- **`cn()`** for conditional classes (from `lib/utils.ts`)
- **Class order**: layout → sizing → spacing → typography → visual → interactive

### Data Flow

- **Server Components** fetch directly from Spring Boot backend (SSR)
- **Client Components** use TanStack Query → Next.js Route Handlers → Spring Boot
- **Never** call Spring Boot directly from browser JS
- **Native `fetch` only** — no axios, ky, ofetch (preserves Next.js cache extensions)
- **`use cache`** for cacheable data (opt-in caching, Next.js 16 model)

### Auth

- **JWT in HTTP-only cookies** — `SameSite=Strict`, `Secure`, `HttpOnly`
- **Token refresh** in `lib/auth.ts` — transparent refresh when access token expires
- **Middleware** for fast-path redirects (not a security boundary)
- **Always re-verify auth** in Route Handlers and Server Components

### State Management

| State | Tool |
|---|---|
| Server state (API data) | TanStack Query |
| Form input | React Hook Form + Zod |
| Multi-step wizard | Zustand (persisted to sessionStorage) |
| Local UI state | `useState` |
| URL state | `useSearchParams` |
| Toast notifications | Sonner |

### Forms (shadcn/ui v4 Pattern)

Use `Controller` from React Hook Form with shadcn `<Field>` components:

```tsx
<Controller
  name="amountUsdc"
  control={form.control}
  render={({ field, fieldState }) => (
    <Field data-invalid={fieldState.invalid || undefined}>
      <FieldLabel htmlFor="amountUsdc">Amount</FieldLabel>
      <Input {...field} id="amountUsdc" />
      {fieldState.error && <FieldError errors={[fieldState.error]} />}
    </Field>
  )}
/>
```

### Error Handling

- **`error.tsx`** at each route group — `(auth)`, `(app)`, `send/`
- **`global-error.tsx`** at root — must include `<html><body>` tags
- **Sonner `toast.error()`** for mutation failures
- **`lib/errors.ts`** maps Spring Boot `SP-XXXX` codes to user messages

### Testing

- **`// given`, `// when`, `// then`** — bare markers, no trailing rationale
- **`it("should...")`** naming
- **`userEvent`** over `fireEvent`
- **Query priority**: `getByRole` > `getByLabelText` > `getByText` > `getByTestId`
- **No snapshot tests**
- **No implementation-detail testing**
- **MSW `onUnhandledRequest: "error"`** — strict mode
- **`renderWithProviders()`** for components needing QueryClient

### Performance

- **Server Components** by default (minimize client JS)
- **PPR** for mixed static/dynamic pages (static shell + Suspense streaming)
- **`use cache`** for cacheable Server Components and functions
- **`next/image`** for images, **`next/font`** for fonts
- **One `<Suspense>` per visual section** — avoid popcorn effect
- **JS budget**: < 100 KB gzipped initial route

## Backend API

The Spring Boot backend runs on `:8080`. Key endpoints:

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/social` | Social login (`{provider, idToken}`) |
| POST | `/api/auth/refresh` | Refresh tokens (`{refreshToken}`) |
| POST | `/api/auth/logout` | Revoke refresh tokens (Bearer auth) |
| GET | `/api/wallets/me` | Get authenticated user's wallet + balance |
| POST | `/api/wallets` | Create MPC wallet (`{userId}`, dev-gated) |
| GET | `/api/remittances` | List user's remittances (Pageable, user from JWT) |
| GET | `/api/remittances/{remittanceId}` | Get remittance detail |
| POST | `/api/remittances` | Create remittance (`{recipientPhone, amountUsdc}`) |
| GET | `/api/remittances/{remittanceId}/timeline` | Get remittance timeline |
| GET | `/api/fx/{from}-{to}` | Get FX rate (path-based, e.g., `/api/fx/USD-INR`) |
| POST | `/api/wallets/{id}/fund` | Create Stripe funding order (`{amount}`) |
| GET | `/api/funding-orders/{fundingId}` | Poll funding order status |
| POST | `/api/funding-orders/{fundingId}/refund` | Refund funded wallet |

Funding confirmation happens via Stripe webhook (`POST /webhooks/stripe`), not a client-facing endpoint.

Request/response types are mirrored in `src/types/api.ts`.

## Design System

- **Dark theme default** — surfaces `#070B1A` to `#242C4F`, white foreground
- **Fonts**: Space Grotesk (sans), JetBrains Mono (mono)
- **Solana gradient**: `#00FFA3` → `#9945FF` → `#DC1FFF`
- **4px spacing grid** — `--space-1` through `--space-24`
- **Radii**: 8/12/20/28/999px
- **Mobile-first** — 430px max-width, phone frame wrapper on desktop for demo
- **Design tokens** imported from `styles/design-tokens.css`, registered via `@theme inline`
