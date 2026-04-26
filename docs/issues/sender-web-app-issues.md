# Sender Web App — GitHub Issues

**Specs:**
- `docs/FE_CODING_STANDARDS.md` — coding conventions
- `docs/FE_TESTING_STANDARDS.md` — testing rules
- `docs/FE_ARCHITECTURE_STANDARDS.md` — architecture patterns
- `docs/specs/2026-04-23-001-sender-app-api-gaps-spec.md` — API gaps
- `stablepay-design-system/project/ui_kits/sender_app/` — design reference

**Total:** 18 issues
**Dependency chain:** STA-116, STA-119, STA-120 are foundational (scaffold, auth, API client). STA-121–STA-131 are screens (can be parallelized after foundation). STA-117, STA-118, STA-132 are backend API gaps.

---

## STA-116: Scaffold Next.js project with design system tokens

**Labels:** `frontend`, `setup`, `design-system`
**Size:** L
**Depends on:** —

### Business context

The sender web app is the primary demo artifact for the Colosseum Frontier Hackathon. We're building a mobile-first Next.js PWA (not React Native) because judges need to interact with a URL — no App Store install. The visual identity must match the StablePay design system exactly: dark theme, Solana gradient, Space Grotesk + JetBrains Mono fonts, 4px spacing grid.

### Description

Initialize the Next.js 16.2.x project in `web-app/` with the full tech stack and design token integration.

**Project initialization:**
```bash
cd web-app
bunx create-next-app@latest . --typescript --tailwind --app --turbopack --no-src-dir
```
Then restructure to use `src/` directory per our folder structure standard.

**Dependencies to install:**
```bash
bun add @tanstack/react-query@^5.100 zustand@^5.0 react-hook-form@^7.73 @hookform/resolvers zod@^4.3 motion sonner next-themes
bun add -d vitest @vitejs/plugin-react vite-tsconfig-paths @testing-library/react @testing-library/jest-dom @testing-library/user-event happy-dom msw @playwright/test @biomejs/biome @axe-core/playwright
```

**shadcn/ui setup:**
```bash
bunx shadcn@latest init --base radix
bunx shadcn@latest add button input field card sheet dialog dropdown-menu sonner command
```

**Design token integration:**
1. Copy `stablepay-design-system/project/colors_and_type.css` to `src/styles/design-tokens.css`
2. Create `src/styles/globals.css` with:
   - `@import "tailwindcss"` and `@import "tw-animate-css"`
   - `@custom-variant dark (&:is(.dark *))` for class-based dark mode
   - `@theme inline` block registering all StablePay tokens into Tailwind:
     - Surface colors: `--color-surface-0` through `--color-surface-4` → `bg-surface-0`, etc.
     - Foreground: `--color-fg-1` through `--color-fg-4` → `text-fg-1`, etc.
     - Borders: `--color-border-1` through `--color-border-3`
     - Solana brand: `--color-solana-teal`, `--color-solana-purple`, `--color-solana-magenta`
     - Semantic: `--color-usdc`, `--color-success`, `--color-warning`, `--color-danger`, `--color-info`, `--color-accent`
     - shadcn mappings: `--color-background: var(--surface-1)`, `--color-foreground: var(--fg-1)`, `--color-primary: var(--accent)`, etc.
     - Radii: `--radius-sm` through `--radius-full`
     - Fonts: `--font-sans`, `--font-mono`

**Root layout (`src/app/layout.tsx`):**
- Load Space Grotesk + JetBrains Mono via `next/font/google` with CSS variable binding
- ThemeProvider (next-themes, `defaultTheme="dark"`)
- QueryProvider (TanStack Query)
- Toaster (Sonner, `position="bottom-center"`)
- Phone frame wrapper: `<div className="mx-auto max-w-[430px] min-h-screen bg-surface-1 lg:my-8 lg:rounded-3xl lg:shadow-3 lg:overflow-hidden">`

**Config files:**
- `tsconfig.json` — strict mode, `@/` path alias, `isolatedDeclarations`
- `biome.json` — Biome 2.4.x config (no `any`, `useImportType`, CSS linting)
- `vitest.config.ts` — happy-dom, setupFiles, coverage exclusions
- `playwright.config.ts` — E2E config with webServer
- `next.config.ts` — PPR experimental flag
- `.env.local.example` — document required env vars

**Folder scaffolding (empty directories with .gitkeep):**
```
src/app/(auth)/, src/app/(app)/, src/app/api/
src/components/ui/, src/components/shared/
src/features/auth/, src/features/wallet/, src/features/remittance/, src/features/fx/
src/lib/, src/hooks/, src/stores/, src/styles/, src/types/, src/test-utils/
e2e/
```

### Acceptance criteria

- [ ] `bun run dev` starts on localhost:3000 with dark theme, Space Grotesk font, Solana-gradient accent
- [ ] `bun run build` succeeds with zero errors
- [ ] `bunx biome check .` passes with zero violations
- [ ] `bun run test` runs (even if zero test files) without config errors
- [ ] Tailwind utility classes work: `bg-surface-2`, `text-fg-1`, `text-solana-teal`, `border-border-1`, `rounded-lg`, `font-mono`
- [ ] shadcn/ui `<Button>` renders with StablePay accent color
- [ ] Phone frame wrapper visible on desktop viewport (430px centered with rounded corners)
- [ ] Dark theme active by default; design tokens from `colors_and_type.css` applied
- [ ] `web-app/CLAUDE.md` is in place and correct

---

## STA-119: Auth flow — Google OAuth login + token management

**Labels:** `frontend`, `auth`
**Size:** L
**Depends on:** STA-116

### Business context

Users sign in with Google Sign-In (one-tap or redirect). The client obtains a Google ID token (not an authorization code) and sends it to the Spring Boot backend, which verifies the token against Google's JWKS and issues StablePay JWTs. The frontend's job is to get the Google credential, exchange it via `POST /api/auth/social`, store the returned tokens in HTTP-only cookies, and protect authenticated routes. For the hackathon demo, this is the first screen judges see — it must work flawlessly.

### Description

Implement the full auth flow: login page, OAuth callback, token storage, token refresh, route protection, and logout.

**Files to create:**

1. **`src/app/(auth)/layout.tsx`** — minimal layout (no bottom nav, no auth guard), centered content
2. **`src/app/(auth)/login/page.tsx`** — Auth screen matching design:
   - StablePay logo mark (gradient "S" in circle)
   - Headline: "Send money home. Under a minute." with "Under a minute." in Solana gradient text
   - Subtext: "Real-time remittances on Solana. No app needed for your family."
   - Live transfer feed (5 items, hardcoded for hackathon — GAP-1 is nice-to-have)
   - "Continue with Google" button with Google icon
   - Terms text at bottom
3. **`src/app/(auth)/auth/callback/route.ts`** — Route Handler:
   - Receives Google OAuth credential from redirect
   - Calls `POST ${BACKEND_URL}/api/auth/social` with `{ provider: "google", idToken: "..." }`
   - Sets `accessToken` and `refreshToken` as HTTP-only, Secure, SameSite=Strict cookies
   - Redirects to `/home`
4. **`src/lib/auth.ts`** — server-side helpers:
   - `getAccessToken()` — reads from cookies
   - `requireAuth()` — returns token or attempts refresh, redirects to `/login` if both fail
   - `refreshAccessToken()` — calls `POST ${BACKEND_URL}/api/auth/refresh` with body `{ refreshToken }`, updates cookies with new token pair
5. **`src/middleware.ts`** — route protection:
   - Public paths: `/login`, `/auth/callback`
   - Redirect unauthenticated users to `/login`
   - Redirect authenticated users away from `/login`
   - **Not a security boundary** — just UX. Auth verified again in Route Handlers.
6. **`src/app/api/auth/logout/route.ts`** — calls `POST ${BACKEND_URL}/api/auth/logout` with Bearer token, clears cookies, redirects to `/login`

**Backend endpoints used:**
- `POST /api/auth/social` — body: `{ "provider": "google", "idToken": "..." }` → response: `AuthResponse { accessToken, refreshToken, tokenType: "Bearer", expiresIn: int, user: { id: UUID, email, createdAt }, wallet: { id: Long, solanaAddress, availableBalance, totalBalance, createdAt, updatedAt } }` — returns 201 for new users, 200 for returning
- `POST /api/auth/refresh` — body: `{ "refreshToken": "..." }` → response: `AuthResponse { accessToken, refreshToken, tokenType, expiresIn }` (user and wallet are null on refresh — use cached values from initial login)
- `POST /api/auth/logout` — header: `Authorization: Bearer {accessToken}` → 204 No Content

**Design reference:** `Components.jsx:54-109` — `AuthScreen` component with live feed, branding, Google CTA.

### Acceptance criteria

- [ ] Login page matches design: dark theme, gradient "S" mark, headline with gradient text, live feed, Google button
- [ ] Clicking "Continue with Google" redirects to Google OAuth consent
- [ ] Successful OAuth sets HTTP-only cookies and redirects to `/home`
- [ ] Failed OAuth shows error toast and stays on `/login`
- [ ] Unauthenticated access to `/home` redirects to `/login`
- [ ] Authenticated access to `/login` redirects to `/home`
- [ ] Token refresh works transparently when access token expires
- [ ] Logout clears cookies and redirects to `/login`
- [ ] Middleware does NOT use `--no-verify` or skip auth checks

---

## STA-120: API client + shared types + error handling

**Labels:** `frontend`, `infrastructure`
**Size:** M
**Depends on:** STA-116

### Business context

Every screen in the app calls the Spring Boot backend. A typed API client with error handling prevents bugs (wrong URL, missing auth header, unhandled error responses) and ensures all backend endpoints are callable from a single consistent interface. The backend returns `ErrorResponse` records with `SP-XXXX` error codes (fields: `errorCode`, `message`, `timestamp`, `path`).

### Description

Create the typed API client, TypeScript types mirroring backend DTOs, and error handling utilities.

**Files to create:**

1. **`src/lib/api-client.ts`** — typed fetch wrapper:
   - `BACKEND_URL` from `process.env.STABLEPAY_BACKEND_URL`
   - `ApiError` class with `status: number`, `errorCode: string` (matches backend `ErrorResponse.errorCode`), `message: string`
   - `request<T>(method, path, options)` — attaches auth header, parses JSON, throws `ApiError` on non-2xx
   - Handles 204 No Content (returns `undefined`)
   - Supports `cache` and `revalidate` options for Next.js fetch extensions
   - Exports `apiClient.get()`, `.post()`, `.put()`, `.delete()`

2. **`src/types/api.ts`** — TypeScript interfaces mirroring backend DTOs:
   - `AuthResponse` — `{ accessToken: string, refreshToken: string, tokenType: string, expiresIn: number, user: UserResponse | null, wallet: WalletResponse | null }`
   - `UserResponse` — `{ id: string, email: string, createdAt: string }`
   - `WalletResponse` — `{ id: number, solanaAddress: string, availableBalance: string, totalBalance: string, createdAt: string, updatedAt: string }`
   - `RemittanceResponse` — `{ id: number, remittanceId: string, recipientPhone: string, amountUsdc: string, amountInr: string, fxRate: string, status: RemittanceStatus, escrowPda: string, claimTokenId: string, smsNotificationFailed: boolean, createdAt: string, updatedAt: string, expiresAt: string }`
   - `RemittanceStatus` — union type: `"INITIATED" | "ESCROWED" | "CLAIMED" | "DELIVERED" | "DISBURSEMENT_FAILED" | "DEPOSIT_FAILED" | "CLAIM_FAILED" | "REFUND_FAILED" | "REFUNDED" | "CANCELLED"`
   - `CreateRemittanceRequest` — `{ recipientPhone: string, amountUsdc: string }`
   - `FxRateResponse` — `{ rate: string, source: string, timestamp: string, expiresAt: string }`
   - `RemittanceTimelineResponse` — `{ steps: TimelineStep[], failed: boolean }`
   - `TimelineStep` — `{ step: RemittanceStatus, status: TimelineStepStatus, message: string, completedAt: string | null }`
   - `TimelineStepStatus` — union type: `"COMPLETED" | "CURRENT" | "PENDING" | "FAILED"`
   - `FundingOrderResponse` — `{ fundingId: string, walletId: number, amountUsdc: string, status: FundingStatus, stripePaymentIntentId: string, stripeClientSecret?: string, createdAt: string }` (stripeClientSecret only present on creation, omitted on GET poll via `@JsonInclude(NON_NULL)`)
   - `FundingStatus` — union type: `"PAYMENT_CONFIRMED" | "FUNDED" | "FAILED" | "REFUND_INITIATED" | "REFUNDED" | "REFUND_FAILED"`
   - `ErrorResponse` — `{ errorCode: string, message: string, timestamp: string, path: string }`

3. **`src/lib/errors.ts`** — error code → user message mapping:
   - Maps `SP-XXXX` codes from backend `ErrorResponse.errorCode` to friendly messages
   - `getErrorMessage(code: string): string`

4. **`src/lib/format.ts`** — formatting utilities:
   - `formatCurrency(amount: string, currency: "USD" | "INR"): string` — `$100.00` or `₹8,450.00` (Indian grouping)
   - `formatPhone(phone: string): string` — `+91 98765 43210`
   - `formatRelativeTime(isoDate: string): string` — `2m ago`, `yesterday`, `3 days ago`
   - `truncateAddress(address: string): string` — `CrsMd…DAd18`

5. **`src/lib/utils.ts`** — `cn()` utility (shadcn standard) + general helpers

6. **`src/lib/constants.ts`** — app constants:
   - `CORRIDOR = "USD_INR"`
   - `NETWORK_FEE = "0.002"` (hardcoded per GAP-3)
   - `SETTLEMENT_TIME = "~30 sec"` (hardcoded per GAP-4)
   - `CLAIM_EXPIRY_HOURS = 48`
   - `MAX_AMOUNT_USDC = 10000`
   - `MIN_AMOUNT_USDC = 1`

7. **Route Handlers (BFF proxy):**
   - `src/app/api/wallet/route.ts` — `GET` → proxies to `GET /api/wallets/me`
   - `src/app/api/remittances/route.ts` — `GET` (list, passes `page`/`size`/`sort` query params) → proxies to `GET /api/remittances`, `POST` (create) → proxies to `POST /api/remittances`
   - `src/app/api/remittances/[id]/route.ts` — `GET` (detail) → proxies to `GET /api/remittances/{remittanceId}`
   - `src/app/api/remittances/[id]/timeline/route.ts` — `GET` (timeline) → proxies to `GET /api/remittances/{remittanceId}/timeline`
   - `src/app/api/fx/[pair]/route.ts` — `GET` → proxies to `GET /api/fx/{from}-{to}` (e.g., `/api/fx/USD-INR`)
   - `src/app/api/wallets/[id]/fund/route.ts` — `POST` (create funding order) → proxies to `POST /api/wallets/{id}/fund`
   - `src/app/api/funding-orders/[fundingId]/route.ts` — `GET` (poll status) → proxies to `GET /api/funding-orders/{fundingId}`
   - `src/app/api/funding-orders/[fundingId]/refund/route.ts` — `POST` (refund) → proxies to `POST /api/funding-orders/{fundingId}/refund`

   Each Route Handler: calls `requireAuth()`, attaches token, proxies to backend, returns JSON response.

   **Note:** There is no cancel remittance or confirm funding endpoint. Funding confirmation is handled server-side by Stripe webhook (`POST /webhooks/stripe`).

### Acceptance criteria

- [ ] `apiClient.get<WalletResponse>("/api/wallets/me", { token })` returns typed response
- [ ] `ApiError` thrown on 4xx/5xx with `status`, `errorCode`, `message` fields (matching backend `ErrorResponse`)
- [ ] 204 responses return `undefined` without JSON parse error
- [ ] All backend DTO types in `types/api.ts` match actual backend response shapes (verified: `WalletResponse` has `id/solanaAddress/availableBalance/totalBalance/createdAt/updatedAt`, `RemittanceStatus` has 10 values, `FundingOrderResponse` has `fundingId/walletId/amountUsdc/status/stripePaymentIntentId/stripeClientSecret/createdAt`)
- [ ] `formatCurrency("8450", "INR")` returns `"₹8,450.00"` (Indian grouping)
- [ ] `formatPhone("+919876543210")` returns `"+91 98765 43210"`
- [ ] `formatRelativeTime()` handles seconds, minutes, hours, days, weeks
- [ ] `truncateAddress("CrsMdGHJ...DAd18xyz")` returns `"CrsMd…DAd18"`
- [ ] All Route Handlers verify auth before proxying
- [ ] No cancel or funding confirm Route Handlers (these endpoints don't exist)
- [ ] Unit tests exist for `format.ts`, `errors.ts`, `constants.ts`

---

## STA-121: Shared components — BottomNav, StatusBadge, AmountDisplay

**Labels:** `frontend`, `ui-components`
**Size:** M
**Depends on:** STA-116

### Business context

Three UI components appear across multiple screens: the bottom tab bar (Home, Send, Activity, Me), status badges on remittance rows ("Escrowed", "Delivered", "Failed"), and the large currency amount display used on home, send, and detail screens. Building these first prevents duplication across screen implementations.

### Description

**Files to create:**

1. **`src/components/shared/bottom-nav.tsx`** — Client Component:
   - 4 tabs: Home (`/home`), Send (`/send`), Activity (`/activity`), Me (`/me`)
   - Active tab highlighted with accent color
   - Fixed to bottom, safe area padding (`pb-[env(safe-area-inset-bottom)]`)
   - Solana gradient on active tab icon (matches design's `GradDefs` + `sp-tab-grad`)
   - **Design reference:** `Components.jsx:125-139` — `TabBar` component

2. **`src/components/shared/status-badge.tsx`** — displays remittance status:
   - Maps all `RemittanceStatus` values to visual variants:
     - `INITIATED` (gray), `ESCROWED` (purple/accent), `CLAIMED` (blue), `DELIVERED` (green/success)
     - `DISBURSEMENT_FAILED` | `DEPOSIT_FAILED` | `CLAIM_FAILED` | `REFUND_FAILED` (red/danger)
     - `REFUNDED` (orange/warning), `CANCELLED` (gray/muted)
   - Pulsing dot animation for active states (`ESCROWED`, `CLAIMED`)
   - **Design reference:** `Components.jsx:361` — `<span className="sp-badge esc">`

3. **`src/components/shared/amount-display.tsx`** — large currency display:
   - Props: `amount`, `currency` ("USD" | "INR"), `size` ("lg" | "xl")
   - Mono font, tabular numerals, currency symbol styled smaller
   - **Design reference:** `Components.jsx:155-156` — `<div className="amt"><small>$</small>248.50</div>`

4. **`src/components/shared/top-bar.tsx`** — screen header:
   - Props: `title`, `onBack?`, `right?`
   - Back arrow or user icon on left
   - Title centered
   - Optional right content (step indicator, bell icon)
   - **Design reference:** `Components.jsx:113-122` — `TopBar` component

5. **`src/components/shared/transaction-row.tsx`** — reusable remittance list item:
   - Avatar circle with initials (derived from name or phone)
   - Name, phone + relative time, amount, status badge
   - Clickable (navigates to detail)
   - Used on Home (recent), Activity (all), Detail (related)
   - **Design reference:** `Components.jsx:169-181` — transaction row in HomeScreen

### Acceptance criteria

- [ ] BottomNav highlights active tab based on current pathname
- [ ] BottomNav uses Solana gradient on active tab icon
- [ ] StatusBadge renders all 10 `RemittanceStatus` variants with correct colors (4 failure states share red/danger)
- [ ] StatusBadge shows pulsing dot for active states (`ESCROWED`, `CLAIMED`)
- [ ] AmountDisplay renders `$248.50` with small `$` and mono font
- [ ] AmountDisplay renders `₹8,450.00` with Indian grouping
- [ ] TopBar shows back arrow when `onBack` is provided, user icon otherwise
- [ ] TransactionRow shows avatar initials, name, phone, relative time, amount, status
- [ ] Component tests exist for each component with at least 2 test cases each

---

## STA-123: Home screen — balance card + recent transactions

**Labels:** `frontend`, `screen`
**Size:** M
**Depends on:** STA-116, STA-119, STA-120, STA-121

### Business context

The home screen is the first thing users see after login. It shows their USDC balance, wallet address, and recent remittances. This is the "dashboard" that proves StablePay works — judges need to see a balance, see transaction history, and navigate to Send or Add Funds.

### Description

**File:** `src/app/(app)/home/page.tsx`

**Data fetching:**
- Server Component that calls `GET /api/wallets/me` for balance + address
- Server Component that calls `GET /api/remittances?size=3&sort=createdAt,desc` for recent transactions (user derived from JWT, paginated via Spring `Pageable` — returns `Page<RemittanceResponse>` wrapper)
- Uses `requireAuth()` to get the token

**Layout (top to bottom, from design):**
1. **TopBar** — title "stablepay", user icon left, bell icon right
2. **Balance card** — gradient glow background:
   - Eyebrow: "USDC balance · Solana"
   - Amount: `$248.50` (large, mono font)
   - Sub: `CrsMd…DAd18 · Available to send` (truncated wallet address)
3. **Action buttons** — two cards side by side:
   - "Send" (primary, gradient border) → navigates to `/send`
   - "Add funds" (secondary) → navigates to `/add-funds`
4. **Recent section header** — "Recent" with "See all" link (→ `/activity`)
5. **Transaction rows** (3 items) — using `TransactionRow` component, each links to `/detail/[remittanceId]` (use `remittance.remittanceId` UUID field, NOT `remittance.id` Long — the backend path variable expects UUID)

**Design reference:** `Components.jsx:142-185` — `HomeScreen` component. Key data:
- Balance: `$248.50`
- Address: `CrsMd…DAd18`
- 3 transactions with names (Raj Patel, Meera Iyer, Vikram Shah), phones, amounts, statuses

**Note:** Recipient names in transaction rows depend on GAP-2 being resolved on the backend. Until then, display the phone number as the primary identifier and hide the name field.

### Acceptance criteria

- [ ] Balance card shows USDC balance from `GET /api/wallets/me` (`availableBalance` field) with gradient glow
- [ ] Wallet address truncated as `CrsMd…DAd18`
- [ ] Empty wallet state handled ($0 balance for brand-new user)
- [ ] "Send" button navigates to `/send`
- [ ] "Add funds" button navigates to `/add-funds`
- [ ] Recent transactions list shows up to 3 remittances from API (extract `content` array from `Page<RemittanceResponse>` wrapper)
- [ ] Each transaction row shows phone number, relative time, amount, and status badge
- [ ] "See all" navigates to `/activity`
- [ ] Clicking a transaction row navigates to `/detail/{remittanceId}` (uses `remittanceId` UUID, not `id` Long)
- [ ] Loading state shows skeleton placeholders during SSR data fetch
- [ ] Error state handled gracefully (error boundary catches API failures)

---

## STA-124: Send flow — Step 1/3 Amount entry with live FX rate

**Labels:** `frontend`, `screen`, `send-flow`
**Size:** M
**Depends on:** STA-116, STA-119, STA-120, STA-121

### Business context

The send flow is StablePay's core user journey: enter amount → pick recipient → review → send. Step 1 collects the USDC amount with a live FX rate preview showing the INR equivalent. This is where users decide "how much to send" — the FX rate must feel real-time and the INR conversion must update as they type.

### Description

**File:** `src/app/(app)/send/page.tsx` (Client Component — form with live state)

**Zustand store:** `src/stores/send-flow.ts` — persisted to `sessionStorage`:
- `amountUsdc: string`
- `recipientPhone: string`
- `recipientName: string`
- `fxRate: string | null`
- Actions: `setAmount`, `setRecipient`, `setFxRate`, `reset`

**FX rate hook:** `src/features/fx/hooks/use-fx-rate.ts`:
- TanStack Query: `GET /api/fx/USD-INR` (path-based, not query param)
- `staleTime: 30_000` (refresh every 30s)
- Returns `FxRateResponse { rate, source, timestamp, expiresAt }`

**Layout (from design):**
1. **TopBar** — "Send", back arrow, "STEP 1 OF 3" right
2. **Amount input** — large centered mono font input:
   - Eyebrow: "You send"
   - Dollar sign prefix (styled smaller, gray)
   - Auto-width input that grows with content
   - Below: "They receive ₹{computed}" with live INR conversion
   - Rate line: "Rate locked · 84.50 INR / USD · from open.er-api.com"
3. **Info rows** (key-value):
   - Network fee: `$0.002` (green, hardcoded per GAP-3)
   - Settlement: `~30 sec` (hardcoded per GAP-4)
   - Corridor: `USD → INR`
4. **Continue button** — disabled when amount is empty or ≤ 0

**On continue:** Store amount + FX rate in Zustand, navigate to `/send/recipient`.

**Design reference:** `Components.jsx:188-216` — `SendAmount` component.

### Acceptance criteria

- [ ] Amount input accepts only numbers and decimal point
- [ ] INR conversion updates live as user types (amount × FX rate)
- [ ] FX rate fetched from `GET /api/fx/USD-INR` and displayed as "84.50 INR / USD"
- [ ] Network fee shows `$0.002` (hardcoded)
- [ ] Settlement shows `~30 sec` (hardcoded)
- [ ] Continue button disabled when amount is empty, zero, or negative
- [ ] Continue stores amount + FX rate in Zustand and navigates to `/send/recipient`
- [ ] Back button navigates to `/home`
- [ ] Zustand store persisted to `sessionStorage` (crash recovery)
- [ ] Frontend validation: min $1.00, max $10,000.00 (matches `FundWalletRequest` limits — user can't send more than they can fund). Note: backend `CreateRemittanceRequest` only enforces `@Positive` — frontend provides the UX-friendly bounds.
- [ ] FX rate fetch failure handled gracefully: show error toast, disable Continue button
- [ ] FX rate expiry handled: if `expiresAt` has passed, refetch before allowing Continue

---

## STA-125: Send flow — Step 2/3 Recipient phone + recent contacts

**Labels:** `frontend`, `screen`, `send-flow`
**Size:** M
**Depends on:** STA-116, STA-120, STA-124

### Business context

Step 2 collects the recipient's phone number. The design shows a "Recent" contacts list so returning senders can tap a previous recipient instead of retyping. This reduces friction for repeat transfers — a key demo scenario showing "I send money to my family regularly."

### Description

**File:** `src/app/(app)/send/recipient/page.tsx` (Client Component)

**Flow guard:** If `amountUsdc` is not set in Zustand store, redirect to `/send`.

**Recent recipients hook:** `src/features/remittance/hooks/use-recent-recipients.ts`:
- TanStack Query: `GET /api/recipients/recent?limit=10`
- **Note:** This endpoint requires GAP-5 on the backend. Until implemented, show an empty state or hardcode demo contacts.
- Returns `Array<{ name: string, phone: string, lastSentAt: string }>`

**Layout (from design):**
1. **TopBar** — "Recipient", back arrow, "STEP 2 OF 3"
2. **Phone input** — full width, mono font:
   - Phone icon left
   - Placeholder: "+91 98765 43210"
3. **Recent contacts list** — section with "RECENT" eyebrow:
   - Each row: avatar initials, name, phone number
   - Tapping a contact fills the phone input AND navigates to review
4. **Continue button** — at bottom, disabled when phone is empty

**Design reference:** `Components.jsx:219-257` — `SendRecipient`. Contacts:
- Raj Patel (+91 98765 43210), Meera Iyer (+91 99887 66554), Vikram Shah (+91 99001 23456), Ananya Rao (+91 90000 12345)

**On continue:** Store `recipientPhone` (and `recipientName` if from recent contacts) in Zustand, navigate to `/send/review`.

### Acceptance criteria

- [ ] Redirects to `/send` if amount not set in Zustand
- [ ] Phone input with phone icon and placeholder
- [ ] Recent contacts displayed (from API when GAP-5 is available, hardcoded fallback otherwise)
- [ ] Tapping a contact fills the phone and navigates to `/send/review`
- [ ] Continue button disabled when phone is empty
- [ ] Continue stores phone in Zustand and navigates to `/send/review`
- [ ] Back button navigates to `/send` (amount step)

---

## STA-126: Send flow — Step 3/3 Review + confirm

**Labels:** `frontend`, `screen`, `send-flow`
**Size:** M
**Depends on:** STA-116, STA-120, STA-125

### Business context

The review screen is the last checkpoint before the user commits money. It shows all details (amount, recipient, FX rate, fees, delivery method, claim expiry) and the escrow safety explanation. This screen must build trust — the user is about to send real money. The escrow banner explains that funds are safe until claimed.

### Description

**File:** `src/app/(app)/send/review/page.tsx` (Client Component)

**Flow guard:** If `amountUsdc` or `recipientPhone` not set in Zustand, redirect to `/send`.

**Create remittance mutation:** `src/features/remittance/hooks/use-create-remittance.ts`:
- TanStack Query mutation: `POST /api/remittances` with `{ recipientPhone, amountUsdc }`
- On success: invalidate `["remittances"]` and `["wallet"]` queries, navigate to `/send/sending`
- On error: `toast.error(getErrorMessage(error.errorCode))`

**Layout (from design):**
1. **TopBar** — "Review", back arrow, "STEP 3 OF 3"
2. **Amount display** — "They receive" eyebrow, large `₹{inr}`, "to {phone}"
3. **Detail rows** (key-value):
   - You send: `${amount} USDC`
   - FX rate: `84.50 INR / USD`
   - Network fee: `$0.002` (green, hardcoded)
   - Delivery: `Instant on-chain + UPI`
   - Claim expires: `48 hours`
4. **Escrow info banner** — purple accent background:
   - Shield icon
   - Text: "Funds are held in a Solana escrow until the recipient enters their UPI ID. If unclaimed within 48h, you get an automatic refund."
5. **Confirm button** — "Confirm & send ${amount}", shows "Signing…" during mutation

**Design reference:** `Components.jsx:260-292` — `SendReview` component.

### Acceptance criteria

- [ ] Redirects to `/send` if amount or phone not set
- [ ] Shows all review details from Zustand store (amount, phone, FX rate)
- [ ] INR amount calculated as `amount × fxRate`
- [ ] Escrow banner with shield icon and safety explanation
- [ ] Confirm button calls `POST /api/remittances` with body `{ recipientPhone, amountUsdc }`
- [ ] Button shows "Signing…" and is disabled during API call
- [ ] On success: navigates to `/send/sending`, clears Zustand store
- [ ] On error: shows toast with user-friendly error message (maps `SP-XXXX` codes from `ErrorResponse`)
- [ ] Handles `SP-0002` (insufficient balance) with specific message
- [ ] If FX rate `expiresAt` has passed since step 1, refetch rate before showing review or display warning
- [ ] Back button navigates to `/send/recipient`

---

## STA-127: Send flow — Sending screen with animated progress

**Labels:** `frontend`, `screen`, `send-flow`
**Size:** M
**Depends on:** STA-126

### Business context

After confirming, users see a 3-step animated progress tracker: Authorising → Locking funds → Notifying recipient. This is the "wow moment" for judges — it shows the Solana transaction happening in real-time. In reality, the backend processes asynchronously (Temporal workflow), but the client animates through steps to give visual feedback.

### Description

**File:** `src/app/(app)/send/sending/page.tsx` (Client Component)

**Flow guard:** If no remittance ID is available (neither in Zustand nor URL param), redirect to `/home`.

**Behavior:**
- Receives the remittance ID from the mutation (via Zustand or URL param)
- Animates through 3 steps with `setTimeout` (900ms, 1800ms, 2700ms) — matches design
- After animation completes, shows "Sent — awaiting claim" state
- Optionally polls `GET /api/remittances/{remittanceId}` to confirm real status (GAP-6 nice-to-have)

**Layout (from design):**
1. **TopBar** — "Sending…" (changes to "Transfer sent" on completion)
2. **Amount display** — "Sending" eyebrow (changes to "Sent"), amount, "to {phone} · ₹{inr}"
3. **Timeline** (3 steps with animation):
   - Step 1: "Authorising transfer" / "Securely signing your transaction"
   - Step 2: "Locking funds" / "Held safely until recipient claims"
   - Step 3: "Notifying recipient" / "Claim link sent to {phone}"
   - Each step: pending → live (pulsing dot) → done (check icon, green)
4. **Completion state** — "Sent — awaiting claim" + "48h claim window"
5. **Done button** — navigates to `/home`
6. **Processing button** — disabled "Processing…" during animation

**Animation:** Use Motion (`motion/react`) for step transitions — fade in, scale the checkmark.

**Design reference:** `Components.jsx:295-349` — `SendingScreen` with `setTimeout` step progression.

### Acceptance criteria

- [ ] Redirects to `/home` if no remittance ID available (direct navigation guard)
- [ ] 3-step timeline animates through pending → live → done states
- [ ] Each step transition takes ~900ms (matching design)
- [ ] Pulsing dot on the active step
- [ ] Green checkmark appears on completed steps
- [ ] "Sending…" title changes to "Transfer sent" on completion
- [ ] "Sent — awaiting claim" message appears after all steps complete
- [ ] Done button navigates to `/home`
- [ ] Processing button is disabled during animation
- [ ] Motion animations for step transitions (opacity, scale)

---

## STA-128: Remittance detail screen with timeline

**Labels:** `frontend`, `screen`
**Size:** M
**Depends on:** STA-116, STA-119, STA-120, STA-121

### Business context

The detail screen shows the full lifecycle of a remittance: when it was initiated, when funds were escrowed on Solana, when the SMS was sent, whether the recipient has claimed, and final delivery. This screen builds transparency — users can verify exactly where their money is, see the escrow PDA (on-chain proof), and track the claim countdown.

### Description

**File:** `src/app/(app)/detail/[remittanceId]/page.tsx` (Server Component with Client island for countdown)

**Route parameter:** `[remittanceId]` is the `remittanceId` UUID from `RemittanceResponse`, NOT the `id` Long. The backend endpoint `GET /api/remittances/{remittanceId}` expects UUID.

**Data fetching:**
- `GET /api/remittances/{remittanceId}` — remittance details (Server Component)
- `GET /api/remittances/{remittanceId}/timeline` — timeline steps (Server Component), returns `RemittanceTimelineResponse { steps: TimelineStep[], failed: boolean }`
- Countdown timer for `expiresAt` — Client Component (`use client`)

**Layout (from design):**
1. **TopBar** — "Remittance", back arrow
2. **Amount display** — "You sent" eyebrow, `$100.00`, "to Raj Patel · ₹8,450.00"
3. **Status badge** — e.g., "Escrowed · awaiting claim" (purple with pulsing dot)
4. **Timeline** (5 visual steps, mapped from 4 API steps):
   - Initiated (done) — "2m ago"
   - Escrowed on Solana (done) — "5KWq…9xZ2 · 1m 52s ago"
   - Claim SMS delivered (done) — "+91 98765 43210"
   - Awaiting recipient claim (live) — "Expires in 47h 58m"
   - Delivery via UPI (pending)
   - **Note:** Split CLAIMED step into "SMS delivered" + "Awaiting claim" using `smsNotificationFailed` flag (see spec section 8.1)
5. **Detail rows**:
   - Remittance ID: `8ce3…dc2a` with copy button
   - Escrow PDA: `7C2z…zWij`
   - On-chain fee: `$0.002` (green, hardcoded per GAP-3)
   - FX rate: `84.50`

**Design reference:** `Components.jsx:352-381` — `DetailScreen` with 5-step timeline.

### Acceptance criteria

- [ ] Fetches remittance detail and timeline from backend
- [ ] Shows amount, recipient (phone, name when available), INR equivalent
- [ ] Status badge with correct variant color
- [ ] 5-step timeline rendered from API timeline data
- [ ] `smsNotificationFailed` flag handled: show "SMS delivery failed" if true
- [ ] Live countdown for claim expiry (Client Component)
- [ ] Copy button on remittance ID copies to clipboard
- [ ] Escrow PDA displayed (truncated)
- [ ] FX rate and network fee shown
- [ ] Back button navigates to previous page (home or activity)
- [ ] Loading skeleton during data fetch

---

## STA-129: Activity screen — paginated remittance list

**Labels:** `frontend`, `screen`
**Size:** S
**Depends on:** STA-116, STA-119, STA-120, STA-121

### Business context

The activity screen shows all remittances, not just the 3 most recent. This is the transaction history judges scroll through to verify that multiple transfers work. It uses the same `TransactionRow` component as the home screen.

### Description

**File:** `src/app/(app)/activity/page.tsx` (Server Component, cached)

**Data fetching:**
- `GET /api/remittances?sort=createdAt,desc` — paginated list (Server Component, user from JWT, returns `Page<RemittanceResponse>` — extract `content` array)
- Consider `use cache` with short revalidation for faster repeat visits

**Layout (from design):**
1. **TopBar** — "Activity"
2. **Section header** — "All transfers"
3. **Transaction rows** — full list using `TransactionRow` component
4. **Empty state** — if no remittances: "No transfers yet. Send your first remittance!"

**Design reference:** `Components.jsx:464-493` — `ActivityScreen` with 5 transactions.

### Acceptance criteria

- [ ] Shows remittances from API (`Page<RemittanceResponse>.content` array), sorted by most recent first
- [ ] Each row shows avatar, name/phone, relative time, amount, status badge
- [ ] Clicking a row navigates to `/detail/{remittanceId}` (uses `remittanceId` UUID, not `id` Long)
- [ ] Empty state displayed when no remittances exist
- [ ] For hackathon: single page of results (default `size=20`). Pagination controls (load more / infinite scroll) are nice-to-have.
- [ ] Loading skeleton via `loading.tsx`
- [ ] Bottom nav visible with "Activity" tab highlighted

---

## STA-130: Add Funds screen — Stripe payment flow

**Labels:** `frontend`, `screen`, `payments`
**Size:** L
**Depends on:** STA-116, STA-119, STA-120

### Business context

Users fund their USDC wallet via Stripe (credit/debit card). This is critical for the demo — judges need to add funds before they can send a remittance. The flow: enter amount → pay with Stripe → see "Added to wallet" confirmation. Preset buttons ($25, $50, $100, $250) reduce friction.

### Description

**File:** `src/app/(app)/add-funds/page.tsx` (Client Component)

**Behavior (3 states, matching design):**
1. **Amount entry** — input with presets, validation, "Pay with Stripe" button
2. **Processing** — animated spinner with "Processing payment… Stripe · $50.00 USD"
3. **Done** — green checkmark, "$50.00 USDC Added to your wallet", Done button

**Backend flow:**
- **Pre-requisite:** Fetch `walletId` from `GET /api/wallets/me` → `WalletResponse.id` (Long). This is the wallet ID needed for the funding endpoint.
1. `POST /api/wallets/{walletId}/fund` with `{ amount }` → returns `FundingOrderResponse { fundingId, walletId, amountUsdc, status: "PAYMENT_CONFIRMED", stripePaymentIntentId, stripeClientSecret, createdAt }` (201)
2. Client uses Stripe Elements with `stripeClientSecret` to complete payment (or for hackathon: auto-confirm)
3. Stripe sends webhook to `POST /webhooks/stripe` → backend confirms payment and mints USDC (no client-facing confirm endpoint)
4. Poll `GET /api/funding-orders/{fundingId}` until `status === "FUNDED"` (note: `stripeClientSecret` is null on poll responses — `@JsonInclude(NON_NULL)`)

**Error states during funding:**
- `FAILED` — Stripe payment failed (show retry option)
- `REFUND_INITIATED` / `REFUNDED` / `REFUND_FAILED` — if refund was triggered

**Layout (amount state, from design):**
1. **TopBar** — "Add Funds", back arrow
2. **Amount input** — same style as send amount:
   - Eyebrow: "Amount (USD)"
   - Large centered mono input
   - Validation text: "Min $1.00 · Max $10,000.00" (red if exceeded)
3. **Preset buttons** — 4 grid: $25, $50, $100, $250 (accent border when selected)
4. **Info rows**:
   - Payment method: "Credit / Debit card"
   - Powered by: "Stripe"
5. **Pay button** — "Pay $50.00 with Stripe" (disabled if invalid)
6. **Footer text** — "Secured by Stripe · Funds appear instantly"

**Design reference:** `Components.jsx:384-461` — `AddFundsScreen` with 3 states.

### Acceptance criteria

- [ ] Amount input with preset buttons ($25, $50, $100, $250)
- [ ] Preset selection updates the input and highlights the button
- [ ] Validation: min $1.00, max $10,000.00 (matches `FundWalletRequest` `@DecimalMin`/`@DecimalMax`), error text turns red
- [ ] Pay button calls `POST /api/wallets/{walletId}/fund` with `{ amount }` — disabled when amount is invalid
- [ ] Processing state: poll `GET /api/funding-orders/{fundingId}` until terminal status
- [ ] Success state (`FUNDED`): green checkmark, amount added, Done button
- [ ] Failure state (`FAILED`): show error message with retry option
- [ ] Conflict handling: `409` response (`SP-0022`) means funding already in progress — show message, don't allow duplicate
- [ ] Done button navigates to `/home`
- [ ] Back button navigates to `/home`
- [ ] Wallet balance query (`GET /api/wallets/me`) invalidated on successful funding

---

## STA-131: Me screen — profile, wallet, settings

**Labels:** `frontend`, `screen`
**Size:** S
**Depends on:** STA-116, STA-119, STA-120

### Business context

The Me screen shows the user's profile, wallet details, and settings. It's where judges see account info, wallet address, and the sign-out button. KYC status and notification preferences are hardcoded for the hackathon (GAP-8, GAP-9).

### Description

**File:** `src/app/(app)/me/page.tsx` (Server Component)

**Data fetching:**
- User data from auth session (cached from initial `POST /api/auth/social` response — `AuthResponse.user`)
- Wallet data from `GET /api/wallets/me` (returns `WalletResponse { id, solanaAddress, availableBalance, totalBalance, createdAt, updatedAt }`)

**Layout (from design):**
1. **TopBar** — "Account"
2. **Profile section** (centered):
   - Avatar circle with gradient background, initial derived from name (or email first letter if name unavailable per GAP-7)
   - Display name (or email if GAP-7 not resolved)
   - Email in mono font
3. **Account info rows**:
   - USD Balance: `$248.50`
   - Member since: formatted `createdAt`
4. **Wallet rows**:
   - Wallet: `CrsMd…DAd18` (truncated)
   - Network: `Solana Mainnet` (hardcoded)
   - KYC status: `Verified` (green, hardcoded per GAP-8)
5. **Settings rows**:
   - Notifications: `On` (hardcoded per GAP-9)
   - Support: external link arrow
   - Sign out: red text, calls logout

**Design reference:** `Components.jsx:496-521` — `MeScreen`.

### Acceptance criteria

- [ ] Avatar shows initial from user name (or email initial as fallback)
- [ ] Gradient background on avatar circle (Solana gradient)
- [ ] Display name shown (email prefix if GAP-7 not resolved)
- [ ] Email displayed in mono font
- [ ] Balance from wallet API
- [ ] Member since formatted from `createdAt`
- [ ] Wallet address truncated
- [ ] KYC status hardcoded as "Verified" (green)
- [ ] Notifications hardcoded as "On"
- [ ] Sign out calls logout API, clears cookies, redirects to `/login`
- [ ] Bottom nav visible with "Me" tab highlighted

---

## STA-122: App layout — route groups, error boundaries, loading states

**Labels:** `frontend`, `layout`
**Size:** M
**Depends on:** STA-116, STA-119, STA-121

### Business context

The app needs two layout groups: `(auth)` for login (no nav, no auth) and `(app)` for authenticated screens (bottom nav, auth guard). Error boundaries at each level catch failures gracefully — a failed API call shouldn't crash the whole app. Loading states (skeletons) prevent blank screens during data fetches.

### Description

**Files to create:**

1. **`src/app/(auth)/layout.tsx`** — centered layout, no bottom nav
2. **`src/app/(app)/layout.tsx`** — authenticated layout:
   - Auth guard (verify token exists, redirect if not)
   - Bottom nav at bottom
   - Content area with bottom padding for nav
3. **`src/app/global-error.tsx`** — root error boundary (must include `<html><body>`)
4. **`src/app/error.tsx`** — root fallback
5. **`src/app/(auth)/error.tsx`** — auth error boundary (OAuth failures)
6. **`src/app/(app)/error.tsx`** — app error boundary ("Go home" + "Try again")
7. **`src/app/(app)/send/error.tsx`** — send flow error boundary
8. **`src/app/not-found.tsx`** — 404 page
9. **`src/app/(app)/activity/loading.tsx`** — activity skeleton
10. **`src/app/(app)/home/loading.tsx`** — home skeleton (balance card + transaction row placeholders)

**Skeleton components:** `src/components/shared/skeleton-*.tsx` — reusable skeleton shapes (card, row, text line).

### Acceptance criteria

- [ ] `(auth)` layout has no bottom nav
- [ ] `(app)` layout has bottom nav and auth check
- [ ] `global-error.tsx` renders with its own `<html><body>`
- [ ] `(app)/error.tsx` shows "Go home" and "Try again" buttons
- [ ] `send/error.tsx` shows send-specific error with retry
- [ ] `not-found.tsx` shows friendly 404
- [ ] Loading skeletons appear during page transitions
- [ ] Error boundaries catch and display API errors gracefully

---

## STA-117: Backend — Add recipient name to remittance model (GAP-2)

**Labels:** `backend`, `api-gap`, `critical`
**Size:** M
**Depends on:** —

### Business context

The sender app design shows recipient names ("Raj Patel", "Meera Iyer") in transaction rows on Home, Activity, and Detail screens. The current backend has no `recipientName` field anywhere in the remittance pipeline. Without this, every transaction row shows only a phone number — which is a poor user experience for the hackathon demo.

### Description

Add `recipientName` as an optional field through the full remittance pipeline: request DTO → controller → handler → domain model → entity → response DTO.

**Files to modify (7 total):**

1. **`CreateRemittanceRequest.java`** — add `@Size(max = 100) String recipientName` (optional, nullable)
2. **`Remittance.java`** (domain model) — add `String recipientName` field to the record
3. **`CreateRemittanceHandler.java`** — add `recipientName` parameter to `handle()` method
4. **`RemittanceController.java`** — pass `request.recipientName()` to handler
5. **`RemittanceEntity.java`** — add `@Column(name = "recipient_name", length = 100)` field
6. **`RemittanceResponse.java`** — add `String recipientName` field
7. **New migration `V{N}__add_recipient_name_to_remittances.sql`:**
   ```sql
   ALTER TABLE remittances ADD COLUMN recipient_name VARCHAR(100);
   ```

**MapStruct auto-maps** `recipientName` between entity/domain/response — no mapper changes needed.

**Source code evidence:**
- `CreateRemittanceRequest.java` — currently only has `recipientPhone` and `amountUsdc`
- `Remittance.java` — no `recipientName` field
- `RemittanceEntity.java` — no `recipient_name` column
- `V1__initial_schema.sql` — `remittances` table has no `recipient_name`

### Acceptance criteria

- [ ] `POST /api/remittances` accepts optional `recipientName` in request body
- [ ] `recipientName` persisted to `remittances.recipient_name` column
- [ ] `GET /api/remittances` and `GET /api/remittances/{id}` include `recipientName` in response
- [ ] Existing remittances without a name return `recipientName: null`
- [ ] Flyway migration runs without error
- [ ] Unit tests for `CreateRemittanceHandler` updated with `recipientName` parameter
- [ ] Controller test verifies `recipientName` round-trips through create → get

---

## STA-118: Backend — Add user display name from Google OAuth (GAP-7)

**Labels:** `backend`, `api-gap`, `critical`
**Size:** M
**Depends on:** —

### Business context

The Me screen shows the user's full name ("Raj Sharma") and derives an avatar initial ("R"). Currently, `UserResponse` only has `id`, `email`, `createdAt`. Google's OAuth ID token includes a `name` claim that we're discarding. Fixing this also improves the claim page — `GetClaimQueryHandler` currently shows the email prefix ("raj") instead of the real name ("Raj Sharma") as the sender.

### Description

Extract the `name` claim from Google's JWT and store it through the full user pipeline.

**Files to modify (7 total):**

1. **`GoogleIdTokenVerifierAdapter.java`** — extract `jwt.getClaimAsString("name")`
2. **`SocialIdentity.java`** — add `String name` field to the record
3. **`SocialLoginHandler.java`** — pass `name` to `AppUser` creation
4. **`AppUser.java`** — add `String name` field to the record
5. **`UserEntity.java`** — add `@Column(name = "name", length = 200)` field
6. **`UserResponse.java`** — add `String name` field
7. **New migration `V{N}__add_name_to_users.sql`:**
   ```sql
   ALTER TABLE users ADD COLUMN name VARCHAR(200);
   ```

**Cascade fix:** Update `GetClaimQueryHandler.java` to use `user.name()` instead of `user.email().split("@")[0]` for `senderDisplayName`.

**Source code evidence:**
- `GoogleIdTokenVerifierAdapter.java:48` — only extracts `sub`, `email`, `email_verified`
- `AppUser.java` — no `name` field
- `V8__users_and_auth.sql` — `users` table has no `name` column
- `GetClaimQueryHandler.java:31-32` — `user.email().split("@")[0]`

### Acceptance criteria

- [ ] Google OAuth `name` claim extracted during login
- [ ] `name` persisted to `users.name` column
- [ ] `UserResponse` includes `name` field
- [ ] `AuthResponse.user.name` populated on login
- [ ] Existing users without a name return `name: null`
- [ ] `GetClaimQueryHandler` uses real name instead of email prefix
- [ ] Flyway migration runs without error
- [ ] Unit tests updated for all modified handlers

---

## STA-132: Backend — Recent recipients endpoint (GAP-5)

**Labels:** `backend`, `api-gap`, `critical`
**Size:** M
**Depends on:** STA-117 (GAP-2 — recipient name must exist on remittances)

### Business context

The Send Recipient screen shows a "Recent" contacts list so returning senders can tap a previous recipient instead of retyping a phone number. This is a key UX optimization for the demo — it shows that StablePay remembers who you send to. Without this, every transfer requires manually entering a phone number.

### Description

Add `GET /api/recipients/recent` endpoint that returns distinct past recipients for the authenticated user.

**Files to create:**

1. **`RecentRecipientResponse.java`** (DTO) — `record RecentRecipientResponse(String name, String phone, Instant lastSentAt) {}`
2. **`GetRecentRecipientsHandler.java`** (domain handler) — queries repository for distinct recipients
3. **`RecipientController.java`** (or add to `RemittanceController`) — `GET /api/recipients/recent?limit=10`
4. **`RemittanceRepository.java`** (port) — add `findRecentRecipients(UUID senderId, int limit)` method
5. **`RemittanceRepositoryAdapter.java`** (infrastructure) — implement with JPQL or native query:
   ```sql
   SELECT DISTINCT ON (recipient_phone) recipient_name, recipient_phone, created_at
   FROM remittances
   WHERE sender_id = :senderId AND recipient_name IS NOT NULL
   ORDER BY recipient_phone, created_at DESC
   LIMIT :limit
   ```

**Source code evidence:**
- No controller handles recipients or contacts
- No handler provides this query
- `RemittanceRepository` port has no `findDistinctRecipients` method

### Acceptance criteria

- [ ] `GET /api/recipients/recent?limit=10` returns distinct recipients
- [ ] Response: `[{ "name": "Raj Patel", "phone": "+919876543210", "lastSentAt": "..." }]`
- [ ] Results sorted by most recent `lastSentAt` first
- [ ] Only returns recipients with non-null `recipientName`
- [ ] Respects `limit` parameter (default 10, max 50)
- [ ] Requires authentication (Bearer JWT)
- [ ] Returns empty array for users with no past remittances
- [ ] Unit test for handler with mocked repository
- [ ] Controller test with MockMvc

---

## Dependency Graph

```
STA-116: Scaffold
├── STA-119: Auth flow
├── STA-120: API client + types
├── STA-121: Shared components
│
├── STA-122: Layouts + error boundaries (depends on 116, 119, 121)
│
├── STA-123: Home screen (depends on 116, 119, 120, 121)
├── STA-124: Send Amount (depends on 116, 119, 120, 121)
│   └── STA-125: Send Recipient (depends on 116, 120, 124)
│       └── STA-126: Send Review (depends on 116, 120, 125)
│           └── STA-127: Sending screen (depends on 126)
├── STA-128: Detail screen [/detail/[remittanceId]] (depends on 116, 119, 120, 121)
├── STA-129: Activity screen (depends on 116, 119, 120, 121)
├── STA-130: Add Funds screen (depends on 116, 119, 120)
├── STA-131: Me screen (depends on 116, 119, 120)

Backend (independent):
STA-117: GAP-2 recipient name
STA-118: GAP-7 user display name
STA-132: GAP-5 recent recipients (depends on STA-117)
STA-133: Persist escrow PDA on deposit confirmation (independent)
```

## Recommended Implementation Order

**Phase 1 — Foundation (STA-116, STA-117, STA-118) — parallel:**
Scaffold + backend gaps (GAP-2, GAP-7). Backend issues are independent and can start immediately.

**Phase 2 — Core infra (STA-119, STA-120, STA-121) — parallel, after STA-116:**
Auth, API client, shared components. Everything else depends on these.

**Phase 3 — Layouts + core screens (STA-122, STA-123, STA-124) — after Phase 2:**
App layout + home screen + send flow step 1.

**Phase 4 — Send flow chain (STA-125 → STA-126 → STA-127) — sequential:**
Recipient → review → sending. Each step depends on the previous.

**Phase 5 — Supporting screens (STA-128, STA-129, STA-130, STA-131) — parallel, after Phase 2:**
Detail, activity, add funds, me — can all be parallelized.

**Phase 6 — Backend gap (STA-132) — after STA-117:**
Recent recipients endpoint. Depends on GAP-2 (recipient name).

**Phase 7 — Backend fix (STA-133) — independent:**
Persist escrow PDA on deposit confirmation.

---

## STA-133: Backend — Persist escrow PDA on deposit confirmation

**Labels:** `backend`, `bug`, `solana`
**Size:** M
**Depends on:** —

### Business context

When a sender initiates a remittance, the Temporal workflow deposits USDC into a Solana escrow PDA on devnet and transitions the remittance to ESCROWED. The escrow PDA address is derived on-chain and the deposit transaction succeeds, but the PDA address is **never persisted** to the `remittances.escrow_pda` column. As a result:

- The **remittance detail screen** shows "—" for the Escrow PDA instead of the actual on-chain address (frontend guard prevents crash, but data is missing).
- Users and support **cannot verify the on-chain escrow** via Solana Explorer.
- The **Solana Explorer link** cannot be generated for audit/transparency — a key hackathon demo requirement ("prove funds are on-chain").

This affects all remittances on devnet. The `Remittance` domain model has an `escrowPda` field (`Remittance.java:21`) that is never populated.

### Root cause

The workflow calls `depositEscrow()` which returns the **transaction signature**, confirms it on-chain, then calls `updateRemittanceStatus(ESCROWED)`. The `UpdateRemittanceStatusHandler` only writes the `status` column — there is no mechanism to write the escrow PDA back to the database.

Additionally, `RemittanceWorkflowResult.escrowPda` is a misnomer — it stores the **deposit transaction signature**, not the PDA address. These are different values.

**Evidence (current flow):**

```
RemittanceLifecycleWorkflowImpl.execute()
  → depositEscrow() returns tx signature              ✓ on-chain tx succeeds
  → awaitTransactionConfirmation() confirms tx         ✓ tx confirmed
  → updateRemittanceStatus(ESCROWED)                   ✗ only writes status, not escrowPda
  → escrowTxSignature stored in workflow memory         ✗ never persisted to DB
```

**Evidence (naming confusion):**

```java
// RemittanceLifecycleWorkflowImpl.java:106
.escrowPda(escrowTxSignature)  // ← stores tx signature, NOT the PDA address
```

### Description

Extend the existing `updateRemittanceStatus` flow to persist the escrow PDA atomically with the ESCROWED status transition — one transaction, one DB round-trip, no inconsistent intermediate state. The PDA computation happens in a dedicated activity (Temporal determinism requires crypto/hashing to run inside activities, not workflows), but persistence piggybacks on the existing status update.

**Key insight:** The PDA is deterministic — `EscrowInstructionBuilder.deriveEscrowPda()` derives it from `remittanceId` + program seeds. The PDA cannot be computed inside the workflow (Temporal workflows must be deterministic — no SHA-256 hashing). It must be computed inside an activity.

**Design decision: single atomic update, not separate handler.** Both `escrowPda` and `status` live on the same `remittances` row. A separate `UpdateEscrowPdaHandler` would mean two DB round-trips (load → save → load → save) and a window where `escrowPda` is set but status is still INITIATED — an inconsistent state. Instead, the activity computes the PDA, then passes it to `updateRemittanceStatus` which sets both fields in one transaction.

**Files to modify:**

1. **`infrastructure/solana/EscrowInstructionBuilder.java`** — add public convenience method that accepts a `UUID` directly (the existing `uuidToPublicKey` is package-private, so cross-package callers cannot use it):
   ```java
   public PublicKey deriveEscrowPda(UUID remittanceId) {
       var remittanceIdPubkey = uuidToPublicKey(remittanceId);
       return deriveEscrowPda(remittanceIdPubkey.bytes());
   }
   ```

2. **`domain/remittance/handler/UpdateRemittanceStatusHandler.java`** — add overloaded `handle` method accepting optional escrow PDA:
   ```java
   public void handle(UUID remittanceId, RemittanceStatus targetStatus, String escrowPda) {
       var remittance = remittanceRepository.findByRemittanceId(remittanceId)
               .orElseThrow(() -> RemittanceNotFoundException.byId(remittanceId));

       if (!remittance.status().canTransitionTo(targetStatus)) {
           throw InvalidRemittanceStateException.forTransition(remittance.status(), targetStatus);
       }

       var builder = remittance.toBuilder().status(targetStatus);
       if (escrowPda != null) {
           builder.escrowPda(escrowPda);
       }
       remittanceRepository.save(builder.build());

       remittanceStatusEventRepository.save(RemittanceStatusEvent.builder()
               .remittanceId(remittanceId)
               .status(targetStatus)
               .message(STATUS_MESSAGES.getOrDefault(targetStatus, "Status updated"))
               .createdAt(Instant.now())
               .build());
   }

   public void handle(UUID remittanceId, RemittanceStatus targetStatus) {
       handle(remittanceId, targetStatus, null);
   }
   ```

3. **`infrastructure/temporal/RemittanceLifecycleActivities.java`** — add activity methods:
   ```java
   String deriveEscrowPda(String remittanceId);

   void updateRemittanceStatusWithEscrowPda(
           String remittanceId, RemittanceStatus status, String escrowPda);
   ```

4. **`infrastructure/temporal/RemittanceLifecycleActivitiesImpl.java`** — implement:
   ```java
   @Override
   public String deriveEscrowPda(String remittanceId) {
       requireNonNull(remittanceId, "remittanceId must not be null");
       var uuid = UUID.fromString(remittanceId);
       var pda = escrowInstructionBuilder.deriveEscrowPda(uuid);
       log.info("Derived escrow PDA {} for remittance {}", pda.toBase58(), remittanceId);
       return pda.toBase58();
   }

   @Override
   public void updateRemittanceStatusWithEscrowPda(
           String remittanceId, RemittanceStatus status, String escrowPda) {
       requireNonNull(remittanceId, "remittanceId must not be null");
       requireNonNull(status, "status must not be null");
       requireNonNull(escrowPda, "escrowPda must not be null");
       var uuid = UUID.fromString(remittanceId);
       updateRemittanceStatusHandler.handle(uuid, status, escrowPda);
   }
   ```
   Inject `EscrowInstructionBuilder` into the constructor.

5. **`infrastructure/temporal/RemittanceLifecycleWorkflowImpl.java`** — compute PDA in activity, pass to status update:
   ```java
   awaitTransactionConfirmation(depositSignature);

   var escrowPdaAddress = statusActivities.deriveEscrowPda(remittanceId.toString());
   statusActivities.updateRemittanceStatusWithEscrowPda(
           remittanceId.toString(), RemittanceStatus.ESCROWED, escrowPdaAddress);
   currentStatus = RemittanceStatus.ESCROWED;
   ```
   Remove the old `statusActivities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.ESCROWED)` call at this point.

6. **`infrastructure/temporal/RemittanceWorkflowResult.java`** — rename misleading field:
   ```java
   public record RemittanceWorkflowResult(
       UUID remittanceId,
       String finalStatus,
       String depositTxSignature,  // was: escrowPda (misnomer — stored tx sig, not PDA)
       String txSignature
   ) {}
   ```

7. **`infrastructure/temporal/RemittanceWorkflowStatus.java`** — rename corresponding field to match.

8. **`RemittanceLifecycleWorkflowImpl.java`** — update all result builder references from `.escrowPda(escrowTxSignature)` to `.depositTxSignature(escrowTxSignature)`.

### Files changed summary

| File | Change |
|---|---|
| `infrastructure/solana/EscrowInstructionBuilder.java` | Add public `deriveEscrowPda(UUID)` convenience overload |
| `domain/remittance/handler/UpdateRemittanceStatusHandler.java` | Add overloaded `handle(id, status, escrowPda)` method |
| `infrastructure/temporal/RemittanceLifecycleActivities.java` | Add `deriveEscrowPda` and `updateRemittanceStatusWithEscrowPda` |
| `infrastructure/temporal/RemittanceLifecycleActivitiesImpl.java` | Implement both; inject `EscrowInstructionBuilder` |
| `infrastructure/temporal/RemittanceLifecycleWorkflowImpl.java` | Compute PDA via activity, pass to status update; rename field references |
| `infrastructure/temporal/RemittanceWorkflowResult.java` | Rename `escrowPda` → `depositTxSignature` |
| `infrastructure/temporal/RemittanceWorkflowStatus.java` | Rename field |

### Test plan

| Test | Type | Validates |
|---|---|---|
| `UpdateRemittanceStatusHandler.handle(id, ESCROWED, pda)` unit test | Unit | Status + escrowPda set atomically in one save |
| `UpdateRemittanceStatusHandler.handle(id, DELIVERED, null)` unit test | Unit | Non-escrow transitions still work (escrowPda unchanged) |
| `RemittanceLifecycleActivitiesImpl.deriveEscrowPda` unit test | Unit | PDA derived correctly from remittance ID |
| `RemittanceLifecycleActivitiesImpl.updateRemittanceStatusWithEscrowPda` unit test | Unit | Delegates to handler with all 3 params |
| Verify `GET /api/remittances/{id}` returns non-null `escrowPda` after ESCROWED | Manual/E2E | API returns PDA for display |
| Detail screen shows truncated PDA (e.g., `7Ksw2…xR4pN`) instead of "—" | Manual | Frontend renders correctly |

### Acceptance criteria

- [ ] After a remittance transitions to ESCROWED, `remittances.escrow_pda` contains the base58-encoded Solana PDA address
- [ ] The PDA and status are set in a **single atomic transaction** (one `save()` call, no inconsistent intermediate state)
- [ ] The PDA is derived deterministically from `remittanceId` using `EscrowInstructionBuilder.deriveEscrowPda()` inside a Temporal activity
- [ ] PDA computation runs in an activity (not the workflow) to preserve Temporal determinism
- [ ] Non-escrow status transitions (`CLAIMED`, `DELIVERED`, `REFUNDED`, etc.) are unaffected — `handle(id, status)` still works
- [ ] `GET /api/remittances/{remittanceId}` returns a non-null `escrowPda` for ESCROWED/CLAIMED/DELIVERED remittances
- [ ] The remittance detail screen displays the truncated escrow PDA (e.g., `7Ksw2…xR4pN`) instead of "—"
- [ ] `RemittanceWorkflowResult.escrowPda` field is renamed to `depositTxSignature` to eliminate naming confusion
- [ ] Unit tests cover the overloaded handler and both new activities
- [ ] Existing tests continue to pass

### Out of scope

- Adding a clickable Solana Explorer link for the escrow PDA (separate enhancement)
- Backfilling `escrow_pda` for existing remittances (PDA is deterministic — can be derived from `remittance_id` if needed)
- Persisting deposit/release/refund tx signatures to the remittance (separate tracking concern)
