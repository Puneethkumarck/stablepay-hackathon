# Architecture Standards for StablePay Frontend

> Architecture decisions and patterns for the StablePay Next.js web application.
> Coding agents must follow these patterns exactly.

## 1. System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Next.js Web App (web-app/)                                  │
│                                                              │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐   │
│  │ Server         │  │ Client         │  │ Route        │   │
│  │ Components     │  │ Components     │  │ Handlers     │   │
│  │ (RSC, PPR,     │  │ ("use client") │  │ (app/api/)   │   │
│  │  use cache)    │  │                │  │              │   │
│  └───────┬────────┘  └───────┬────────┘  └──────┬───────┘   │
│          │                   │                   │           │
│          │            ┌──────▼───────┐           │           │
│          │            │ TanStack     │           │           │
│          │            │ Query 5      │           │           │
│          │            └──────┬───────┘           │           │
│          │                   │                   │           │
│          └──────────┬────────┴───────────────────┘           │
│                     │                                        │
│              ┌──────▼────────┐                               │
│              │ API Client    │                               │
│              │ (lib/api-     │                               │
│              │  client.ts)   │                               │
│              └──────┬────────┘                               │
└─────────────────────┼────────────────────────────────────────┘
                      │ HTTP (native fetch)
                      ▼
┌──────────────────────────────────────────────────────────────┐
│  Spring Boot Backend (backend/)                              │
│  REST API on :8080                                           │
│  Auth: JWT (access + refresh tokens)                         │
└──────────────────────────────────────────────────────────────┘
```

### Data Flow Rules

1. **Server Components** fetch data directly from the backend via the API client during SSR
2. **Client Components** use TanStack Query hooks, which call Next.js Route Handlers
3. **Route Handlers** (`app/api/`) proxy requests to the Spring Boot backend, attaching auth from cookies
4. **Server Actions** handle form submissions that mutate backend state
5. **Never** call the Spring Boot backend directly from browser-side JavaScript
6. **Native `fetch`** only — no `ky`, `ofetch`, or `axios` (preserves Next.js caching extensions)

## 2. Rendering Strategy

### Next.js 16 Rendering Model

Next.js 16 defaults to dynamic rendering. Caching is **opt-in** via the `use cache` directive:

```typescript
// Opt-in caching at page level
"use cache";

export default async function ActivityPage() {
  const remittances = await fetchRemittances();
  return <ActivityList remittances={remittances} />;
}

// Opt-in caching at function level
async function getFxRate(corridor: string) {
  "use cache";
  return apiClient.get<FxRateResponse>(`/fx/rate?corridor=${corridor}`);
}
```

### Partial Prerendering (PPR)

PPR serves a static shell from the edge cache with dynamic content streamed via Suspense:

```typescript
// Enable in next.config.ts
const nextConfig = {
  experimental: {
    ppr: true,
  },
};
```

```typescript
// Page with PPR: static shell + dynamic islands
export default async function HomePage() {
  return (
    <div>
      {/* Static: rendered at build time */}
      <Header />

      {/* Dynamic: streamed at request time */}
      <Suspense fallback={<BalanceSkeleton />}>
        <WalletBalance />
      </Suspense>

      <Suspense fallback={<ActivitySkeleton />}>
        <RecentActivity />
      </Suspense>

      {/* Static */}
      <BottomNav />
    </div>
  );
}
```

### Decision Framework

```
Does this component need interactivity?
├── No  → Server Component (default)
│   ├── Does the data change rarely? → Add "use cache"
│   └── Does it load slowly? → Wrap in <Suspense>
├── Yes → Does it need real-time updates?
│   ├── No  → Client Component with TanStack Query
│   └── Yes → Client Component with polling (queryRefetchInterval)
└── Is it a form?
    └── Yes → Client Component with React Hook Form
```

### Page-Level Decisions

| Page | Rendering | Rationale |
|---|---|---|
| Home | Server Component + Client islands | Balance/recent activity streamed via Suspense |
| Send Amount | Client Component | Form with live FX rate preview |
| Send Recipient | Client Component | Form with phone input + contact selection |
| Send Review | Client Component | Reads Zustand store, has submit action |
| Sending | Client Component | Polls remittance status via TanStack Query |
| Activity | Server Component (cached) | Static list, paginated; `use cache` with revalidation |
| Detail | Server Component + Client island | Timeline is static; status polling is client |
| Add Funds | Client Component | Stripe integration |
| Me | Server Component | Profile display, mostly static |
| Login | Client Component | Google OAuth flow |

### Streaming and Suspense

Use `<Suspense>` with `loading.tsx` for route-level loading states:

```typescript
// app/(app)/activity/loading.tsx
export default function ActivityLoading() {
  return <ActivitySkeleton />;
}
```

**Suspense boundary rule:** One boundary per independent data dependency, grouped by visual section. Avoid too many boundaries (causes "popcorn effect" — dozens of skeleton-to-content transitions).

## 3. Authentication Architecture

### Auth Flow

```
User clicks "Sign in with Google"
    │
    ▼
Google OAuth consent screen
    │
    ▼
Redirect to /auth/callback with code
    │
    ▼
Next.js Route Handler exchanges code → calls Spring Boot POST /auth/google
    │
    ▼
Spring Boot verifies Google ID token → returns { accessToken, refreshToken, user, wallet }
    │
    ▼
Next.js sets HTTP-only cookies:
  - accessToken (SameSite=Strict, Secure, HttpOnly, short TTL)
  - refreshToken (SameSite=Strict, Secure, HttpOnly, long TTL)
    │
    ▼
Redirect to /home
```

### Token Refresh

```typescript
// lib/auth.ts
import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export async function getAccessToken(): Promise<string | null> {
  const cookieStore = await cookies();
  return cookieStore.get("accessToken")?.value ?? null;
}

export async function requireAuth(): Promise<string> {
  const token = await getAccessToken();
  if (token) return token;

  const refreshed = await refreshAccessToken();
  if (refreshed) return refreshed;

  redirect("/login");
}

async function refreshAccessToken(): Promise<string | null> {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get("refreshToken")?.value;
  if (!refreshToken) return null;

  const res = await fetch(`${process.env.STABLEPAY_BACKEND_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${refreshToken}`,
    },
  });

  if (!res.ok) return null;

  const data = await res.json();
  cookieStore.set("accessToken", data.accessToken, {
    httpOnly: true,
    secure: true,
    sameSite: "strict",
    path: "/",
    maxAge: 900, // 15 minutes
  });

  return data.accessToken;
}
```

### Auth in Route Handlers

```typescript
// app/api/remittances/route.ts
import { requireAuth } from "@/lib/auth";
import { apiClient } from "@/lib/api-client";

export async function GET() {
  const token = await requireAuth();
  const data = await apiClient.get("/remittances", { token });
  return Response.json(data);
}

export async function POST(request: Request) {
  const token = await requireAuth();
  const body = await request.json();
  const data = await apiClient.post("/remittances", { token, body });
  return Response.json(data, { status: 201 });
}
```

### Middleware for Route Protection

```typescript
// middleware.ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const PUBLIC_PATHS = ["/login", "/auth/callback"];

export function middleware(request: NextRequest) {
  const token = request.cookies.get("accessToken");
  const refreshToken = request.cookies.get("refreshToken");
  const isPublic = PUBLIC_PATHS.some((p) => request.nextUrl.pathname.startsWith(p));

  if (!token && !refreshToken && !isPublic) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  if ((token || refreshToken) && isPublic) {
    return NextResponse.redirect(new URL("/home", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
```

**Security note:** Middleware is a fast-path redirect, not a security boundary. Always verify auth again in the Route Handler or Server Component. This defends against CVE-2025-29927-class middleware bypass vulnerabilities.

## 4. API Client Architecture

### Typed API Client

```typescript
// lib/api-client.ts
const BACKEND_URL = process.env.STABLEPAY_BACKEND_URL ?? "http://localhost:8080";

interface RequestOptions {
  token?: string;
  body?: unknown;
  cache?: RequestCache;
  revalidate?: number;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (options.token) {
    headers["Authorization"] = `Bearer ${options.token}`;
  }

  const fetchOptions: RequestInit & { next?: { revalidate?: number } } = {
    method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
    cache: options.cache,
  };

  if (options.revalidate !== undefined) {
    fetchOptions.next = { revalidate: options.revalidate };
  }

  const response = await fetch(`${BACKEND_URL}${path}`, fetchOptions);

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      code: "SP-9999",
      detail: "Request failed",
    }));
    throw new ApiError(response.status, error.code, error.detail);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>("GET", path, options),
  post: <T>(path: string, options?: RequestOptions) => request<T>("POST", path, options),
  put: <T>(path: string, options?: RequestOptions) => request<T>("PUT", path, options),
  delete: <T>(path: string, options?: RequestOptions) => request<T>("DELETE", path, options),
};
```

### Type Safety with Backend DTOs

Mirror the Spring Boot DTOs as TypeScript interfaces:

```typescript
// types/api.ts
export interface WalletResponse {
  walletId: string;
  solanaAddress: string;
  usdcBalance: string;
}

export interface RemittanceResponse {
  id: number;
  remittanceId: string;
  recipientPhone: string;
  amountUsdc: string;
  amountInr: string;
  fxRate: string;
  status: RemittanceStatus;
  escrowPda: string;
  claimTokenId: string;
  smsNotificationFailed: boolean;
  createdAt: string;
  updatedAt: string;
  expiresAt: string;
}

export type RemittanceStatus =
  | "INITIATED"
  | "FX_LOCKED"
  | "ESCROWED"
  | "NOTIFIED"
  | "CLAIMED"
  | "COMPLETED"
  | "EXPIRED"
  | "FAILED";

export interface FxRateResponse {
  rate: string;
  corridor: string;
  expiresAt: string;
}

export interface CreateRemittanceRequest {
  recipientPhone: string;
  amountUsdc: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
  wallet: WalletResponse;
}

export interface UserResponse {
  id: string;
  email: string;
  createdAt: string;
}
```

## 5. Navigation Architecture

### Bottom Navigation (Mobile-First)

The sender app uses a bottom tab bar with 4 tabs: Home, Activity, Send (center action), Me.

```typescript
// components/shared/bottom-nav.tsx
"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const tabs = [
  { href: "/home", label: "Home", icon: HomeIcon },
  { href: "/activity", label: "Activity", icon: ActivityIcon },
  { href: "/send", label: "Send", icon: SendIcon, primary: true },
  { href: "/me", label: "Me", icon: UserIcon },
] as const;

export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 flex items-center justify-around border-t border-border bg-surface-1 px-4 pb-[env(safe-area-inset-bottom)]">
      {tabs.map((tab) => (
        <Link
          key={tab.href}
          href={tab.href}
          className={cn(
            "flex flex-col items-center gap-1 py-2 text-xs transition-colors",
            pathname.startsWith(tab.href) ? "text-accent" : "text-fg-3",
          )}
        >
          <tab.icon className="h-5 w-5" />
          <span>{tab.label}</span>
        </Link>
      ))}
    </nav>
  );
}
```

### Route Groups

```
app/
├── (auth)/              # No bottom nav, no auth required
│   ├── login/
│   ├── auth/callback/
│   ├── error.tsx
│   └── layout.tsx
├── (app)/               # Bottom nav + auth guard
│   ├── layout.tsx       # Wraps children with BottomNav + padding
│   ├── error.tsx
│   ├── home/
│   ├── activity/
│   ├── send/
│   │   ├── error.tsx    # Send flow error boundary
│   │   ├── page.tsx
│   │   ├── recipient/
│   │   ├── review/
│   │   └── sending/
│   ├── detail/[id]/
│   ├── add-funds/
│   └── me/
├── global-error.tsx     # Catches root layout errors
└── layout.tsx           # Root: fonts, ThemeProvider, Toaster, QueryProvider
```

## 6. Multi-Step Flow Architecture

The send remittance flow spans 4 pages. State persists across navigation via Zustand with `sessionStorage` persistence for crash recovery.

```
/send (Amount) → /send/recipient → /send/review → /send/sending
     │                │                │               │
     ▼                ▼                ▼               ▼
 Store: set       Store: set       Store: read     API mutation +
 amountUsdc       recipientPhone   all fields      poll status
 + fetch FX rate  recipientName    + confirm        + invalidate queries
```

### Flow Guard

Each step validates that previous steps are complete:

```typescript
// features/remittance/hooks/use-send-flow-guard.ts
"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSendFlowStore } from "@/stores/send-flow";

export function useSendFlowGuard(requiredFields: (keyof ReturnType<typeof useSendFlowStore.getState>)[]) {
  const router = useRouter();
  const state = useSendFlowStore();

  useEffect(() => {
    const missing = requiredFields.some((field) => !state[field]);
    if (missing) router.replace("/send");
  }, []);
}

// Usage in /send/recipient/page.tsx:
export default function RecipientPage() {
  useSendFlowGuard(["amountUsdc"]);
  return <RecipientForm />;
}

// Usage in /send/review/page.tsx:
export default function ReviewPage() {
  useSendFlowGuard(["amountUsdc", "recipientPhone"]);
  return <SendReviewCard />;
}
```

## 7. Error Architecture

### Error Boundary Hierarchy

```
app/
├── global-error.tsx       # Root — catches root layout errors (must include <html><body>)
├── error.tsx              # Fallback for uncaught route errors
├── (auth)/
│   └── error.tsx          # Auth-specific (e.g., OAuth callback failure)
├── (app)/
│   ├── error.tsx          # App-wide (e.g., session expired → redirect to login)
│   └── send/
│       └── error.tsx      # Send flow (show retry + return to home)
```

**Important:** `error.tsx` does NOT wrap `layout.tsx` in the same segment. Only `global-error.tsx` catches root layout errors.

### Error Component Pattern

```typescript
// app/global-error.tsx — must include its own <html> and <body>
"use client";

import { useEffect } from "react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Sentry.captureException(error);
    console.error(error);
  }, [error]);

  return (
    <html>
      <body className="flex min-h-screen flex-col items-center justify-center gap-4 bg-surface-1 p-8">
        <h2 className="text-xl font-semibold text-fg-1">Something went wrong</h2>
        <p className="text-fg-3 text-center">{error.message}</p>
        <button onClick={reset} className="rounded-lg bg-accent px-4 py-2 text-white">
          Try again
        </button>
      </body>
    </html>
  );
}
```

```typescript
// app/(app)/error.tsx
"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

export default function AppError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const router = useRouter();

  useEffect(() => {
    // Sentry.captureException(error);
    console.error(error);
  }, [error]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-8">
      <h2 className="text-xl font-semibold text-fg-1">Something went wrong</h2>
      <p className="text-fg-3 text-center max-w-sm">{error.message}</p>
      <div className="flex gap-3">
        <Button variant="outline" onClick={() => router.push("/home")}>Go home</Button>
        <Button onClick={reset}>Try again</Button>
      </div>
    </div>
  );
}
```

### API Error Handling

```typescript
// lib/errors.ts
const ERROR_MESSAGES: Record<string, string> = {
  "SP-0001": "Wallet not found. Please try again.",
  "SP-0002": "Insufficient balance. Add funds to continue.",
  "SP-0003": "FX rate expired. We'll get a fresh quote.",
  "SP-0004": "Remittance not found.",
  "SP-0005": "Phone number is invalid.",
};

export function getErrorMessage(code: string): string {
  return ERROR_MESSAGES[code] ?? "Something went wrong. Please try again.";
}
```

## 8. Design System Integration

### Token Import

```typescript
// app/layout.tsx
import "@/styles/design-tokens.css";
import "@/styles/globals.css";
```

The design tokens CSS file (`styles/design-tokens.css`) is derived from `stablepay-design-system/project/colors_and_type.css`. Contains all `--surface-*`, `--fg-*`, `--border-*`, `--solana-*`, `--font-*`, `--space-*`, `--radius-*` variables.

### Font Loading

```typescript
// app/layout.tsx
import { Space_Grotesk, JetBrains_Mono } from "next/font/google";
import { ThemeProvider } from "@/components/shared/theme-provider";
import { Toaster } from "@/components/ui/sonner";
import { QueryProvider } from "@/components/shared/query-provider";

const spaceGrotesk = Space_Grotesk({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  display: "swap",
});

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${spaceGrotesk.variable} ${jetbrainsMono.variable}`} suppressHydrationWarning>
      <body>
        <ThemeProvider attribute="class" defaultTheme="dark" enableSystem>
          <QueryProvider>
            <div className="mx-auto max-w-[430px] min-h-screen bg-surface-1 lg:my-8 lg:rounded-3xl lg:shadow-3 lg:overflow-hidden">
              {children}
            </div>
            <Toaster position="bottom-center" />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
```

### Dark Mode

Default theme is dark (matching the design system). Uses `next-themes` with class-based toggling:

```typescript
// components/shared/theme-provider.tsx
"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";
import type { ComponentProps } from "react";

export function ThemeProvider({ children, ...props }: ComponentProps<typeof NextThemesProvider>) {
  return <NextThemesProvider {...props}>{children}</NextThemesProvider>;
}
```

In `globals.css`, the dark variant is class-based:
```css
@custom-variant dark (&:is(.dark *));
```

### Responsive Strategy

Mobile-first, single-column layout optimized for phone viewports (matches the sender app design):

| Breakpoint | Usage |
|---|---|
| Default (< 640px) | Phone layout — primary target |
| `sm` (640px+) | Slightly wider phones, no layout change |
| `md` (768px+) | Tablet — centered with max-width |
| `lg` (1024px+) | Desktop — phone frame mockup for demo |

The root layout wraps content in a phone-width container for the hackathon demo:

```tsx
<div className="mx-auto max-w-[430px] min-h-screen bg-surface-1 lg:my-8 lg:rounded-3xl lg:shadow-3 lg:overflow-hidden">
  {children}
</div>
```

## 9. Security

### Cookie Security

- **HTTP-only cookies** for JWT tokens — never store tokens in `localStorage` or `sessionStorage`
- **`SameSite=Strict`** — blocks cookie on all cross-site requests (stronger than Lax for fintech)
- **`Secure` flag** — cookies only sent over HTTPS
- **Short-lived access tokens** (15 min) with refresh token rotation

### Content Security Policy (Nonce-Based)

```typescript
// middleware.ts (CSP generation)
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import crypto from "crypto";

export function middleware(request: NextRequest) {
  const nonce = crypto.randomBytes(16).toString("base64");
  const csp = [
    `default-src 'self'`,
    `script-src 'self' 'nonce-${nonce}'`,
    `style-src 'self' 'unsafe-inline'`,
    `img-src 'self' data: https:`,
    `font-src 'self' https://fonts.gstatic.com`,
    `connect-src 'self' ${process.env.STABLEPAY_BACKEND_URL}`,
    `frame-ancestors 'none'`,
  ].join("; ");

  const response = NextResponse.next();
  response.headers.set("Content-Security-Policy", csp);
  response.headers.set("X-Nonce", nonce);
  return response;
}
```

### Defense in Depth

- **Middleware is NOT a security boundary** — always verify auth again in Route Handlers and Server Components
- **Input sanitization** — Zod schemas validate all user input before submission
- **No secrets in client code** — only `NEXT_PUBLIC_*` vars reach the browser
- **Server Actions** automatically check `Origin` vs `Host` for CSRF protection
- **Route Handlers** that mutate state must verify auth via `requireAuth()`

## 10. Providers Architecture

### Provider Composition

```typescript
// components/shared/query-provider.tsx
"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
```

### Provider Stack (Root Layout)

```
<ThemeProvider>          ← next-themes (class-based dark mode)
  <QueryProvider>        ← TanStack Query
    <div>                ← Phone frame container
      {children}         ← App routes
    </div>
    <Toaster />          ← Sonner toast notifications
  </QueryProvider>
</ThemeProvider>
```

## 11. Performance Budget

| Metric | Target |
|---|---|
| First Contentful Paint | < 1.5s |
| Largest Contentful Paint | < 2.5s |
| Total Blocking Time | < 200ms |
| Cumulative Layout Shift | < 0.1 |
| JS bundle (initial route) | < 100 KB gzipped |

### Performance Rules

- **Server Components by default** — minimize client JS bundle
- **`use cache`** for cacheable data (FX rates, user profile)
- **PPR** for pages with mixed static/dynamic content
- **Dynamic imports** for heavy client components: `dynamic(() => import("./chart"), { ssr: false })`
- **`next/image`** for all images (automatic optimization)
- **`next/font`** for fonts (no layout shift)
- **No barrel exports** (`index.ts` re-exports break tree-shaking)
- **One `<Suspense>` per visual section** — avoid popcorn effect
- **Prefetch critical routes** — `<Link>` prefetches by default

### Monitoring

- `reportWebVitals` in root layout for Core Web Vitals telemetry
- Lighthouse CI in build pipeline for regression detection
