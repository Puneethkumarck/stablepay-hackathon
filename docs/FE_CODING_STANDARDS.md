# Coding Standards for StablePay Frontend

Instructions for coding agents developing the StablePay Next.js web application.

## 1. Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Next.js | 16.2.x | Framework (App Router, RSC, Turbopack, PPR) |
| React | 19.2.x | UI library (Server Components, Actions, `use()`) |
| TypeScript | 6.0.x | Type safety (strict mode) |
| Tailwind CSS | 4.2.x | Utility-first styling (CSS-first config, OKLCH) |
| shadcn/ui | CLI v4 (Radix base) | Component primitives (copy-paste, not dependency) |
| TanStack Query | 5.100.x | Server state management |
| Zustand | 5.0.x | Client state management |
| React Hook Form + Zod | 7.73.x + 4.3.x | Form handling + validation |
| Motion | 12.38.x | Animation (`motion/react`) |
| Sonner | latest | Toast notifications |
| Bun | latest | Package manager + runtime |
| Biome | 2.4.x | Linter + formatter (replaces ESLint + Prettier) |

## 2. Architecture: Vertical Slices + BFF

### Folder Structure

```
web-app/
├── src/
│   ├── app/                        # Next.js App Router
│   │   ├── (auth)/                 # Route group: login, callback
│   │   │   ├── login/page.tsx
│   │   │   ├── auth/callback/route.ts
│   │   │   ├── error.tsx
│   │   │   └── layout.tsx
│   │   ├── (app)/                  # Route group: authenticated app
│   │   │   ├── home/page.tsx
│   │   │   ├── send/
│   │   │   │   ├── page.tsx        # Amount entry
│   │   │   │   ├── recipient/page.tsx
│   │   │   │   ├── review/page.tsx
│   │   │   │   ├── sending/page.tsx
│   │   │   │   └── error.tsx
│   │   │   ├── activity/
│   │   │   │   ├── page.tsx
│   │   │   │   └── loading.tsx
│   │   │   ├── detail/[id]/page.tsx
│   │   │   ├── add-funds/page.tsx
│   │   │   ├── me/page.tsx
│   │   │   ├── error.tsx
│   │   │   └── layout.tsx          # BottomNav + auth guard
│   │   ├── api/                    # Route Handlers (BFF proxy)
│   │   │   ├── remittances/route.ts
│   │   │   ├── wallet/route.ts
│   │   │   └── fx/rate/route.ts
│   │   ├── layout.tsx              # Root layout (fonts, providers)
│   │   ├── global-error.tsx        # Root layout error boundary
│   │   ├── not-found.tsx
│   │   └── error.tsx
│   ├── components/
│   │   ├── ui/                     # shadcn/ui primitives (generated, do not hand-edit)
│   │   └── shared/                 # App-level shared components
│   │       ├── bottom-nav.tsx
│   │       ├── status-badge.tsx
│   │       └── amount-display.tsx
│   ├── features/                   # Domain-specific feature modules
│   │   ├── auth/
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   └── types.ts
│   │   ├── wallet/
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   └── types.ts
│   │   ├── remittance/
│   │   │   ├── components/
│   │   │   │   └── __tests__/
│   │   │   ├── hooks/
│   │   │   │   └── __tests__/
│   │   │   └── types.ts
│   │   └── fx/
│   │       ├── components/
│   │       ├── hooks/
│   │       └── types.ts
│   ├── lib/                        # Shared utilities
│   │   ├── api-client.ts           # Typed fetch wrapper for Spring Boot backend
│   │   ├── auth.ts                 # Server-side auth helpers (cookies, token refresh)
│   │   ├── constants.ts
│   │   ├── errors.ts               # API error code → user message mapping
│   │   ├── utils.ts                # cn() and general utils
│   │   └── format.ts               # Currency, date, phone formatters
│   ├── hooks/                      # App-wide custom hooks
│   ├── stores/                     # Zustand stores
│   │   └── send-flow.ts            # Multi-step send wizard state
│   ├── styles/
│   │   ├── design-tokens.css       # StablePay design tokens (surfaces, colors, type)
│   │   └── globals.css             # Tailwind import + @theme inline + base layer
│   ├── test-utils/                 # MSW handlers, test wrappers
│   │   ├── msw-server.ts
│   │   └── handlers.ts
│   ├── test-setup.ts
│   └── types/                      # Shared type definitions
│       ├── api.ts                  # Backend API response types
│       └── common.ts
├── e2e/                            # Playwright E2E tests
├── public/
├── biome.json                      # Linter + formatter config
├── tsconfig.json
├── next.config.ts
├── vitest.config.ts
└── package.json
```

### Dependency Rules

- `app/` pages import from `features/`, `components/`, `lib/`, `hooks/`, `stores/`
- `features/` import from `components/ui/`, `lib/`, `types/`
- `components/ui/` imports nothing from the app (self-contained primitives)
- `lib/` imports nothing from `features/` or `components/`
- Never import from `app/` — pages are leaves, not dependencies

### Server vs Client Components

Server Components are the default. Add `"use client"` only when the component needs:
- Event handlers (`onClick`, `onChange`, `onSubmit`)
- React hooks (`useState`, `useEffect`, `useRef`)
- Browser APIs (`window`, `localStorage`, `navigator`)
- Third-party client-only libraries

**Rule:** If a component can be a Server Component, it must be a Server Component. Split interactive parts into small Client Components and compose them inside Server Component layouts.

### BFF Pattern

The Next.js app talks to the Spring Boot backend via:
1. **Server Components** — `fetch()` in `async` components (direct backend calls during SSR)
2. **Route Handlers** (`app/api/`) — proxy endpoints for Client Component data fetching
3. **Server Actions** — form mutations that call the backend

Never call the Spring Boot backend directly from Client Components. Use TanStack Query hooks that call Next.js Route Handlers.

## 3. TypeScript Rules

### Strict Mode

```jsonc
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "isolatedDeclarations": true
  }
}
```

### Type Rules

- **No `any`** — use `unknown` and narrow with type guards
- **No `as` casts** — except for `as const` and proven-safe DOM element casts
- **No `enum`** — use `as const` objects or union types
- **No `namespace`** — use ES modules
- **Prefer `interface` over `type`** for object shapes (better error messages, declaration merging)
- **Export types explicitly** — `export type { Foo }` when re-exporting types only
- **Discriminated unions** for state variants (loading, error, success)
- **`ref` as a prop** — React 19 removed `forwardRef`; pass `ref` directly

```typescript
// FORBIDDEN
const status: any = response.data;
const el = document.getElementById("root") as HTMLDivElement;
enum Status { Pending, Active }

// REQUIRED
interface RemittanceResponse {
  id: string;
  status: "PENDING" | "ESCROWED" | "CLAIMED" | "COMPLETED";
  amountUsdc: string;
  amountInr: string;
  fxRate: string;
}

type AsyncState<T> =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "error"; error: Error }
  | { status: "success"; data: T };
```

### Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Components | PascalCase | `SendReviewCard` |
| Hooks | camelCase, `use` prefix | `useRemittance` |
| Utilities | camelCase | `formatCurrency` |
| Types/Interfaces | PascalCase | `RemittanceResponse` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_AMOUNT_USDC` |
| All files | kebab-case | `send-review-card.tsx`, `api-client.ts` |
| Route folders | kebab-case | `add-funds/` |
| CSS classes | Tailwind utilities | — |

## 4. Component Rules

### Component Structure

```typescript
// 1. Imports (external, then internal, then types)
import { useState } from "react";
import { Button } from "@/components/ui/button";
import type { RemittanceResponse } from "@/types/api";

// 2. Types (if component-local)
interface SendReviewCardProps {
  remittance: RemittanceResponse;
  onConfirm: () => void;
}

// 3. Component (named export, never default export)
export function SendReviewCard({ remittance, onConfirm }: SendReviewCardProps) {
  // hooks first
  const [isSubmitting, setIsSubmitting] = useState(false);

  // derived state
  const formattedAmount = formatCurrency(remittance.amountUsdc);

  // handlers
  function handleConfirm() {
    setIsSubmitting(true);
    onConfirm();
  }

  // render
  return (
    <div className="rounded-lg border border-border bg-surface-2 p-4">
      {/* ... */}
    </div>
  );
}
```

### Component Rules

- **Named exports only** — never `export default` (exception: Next.js pages, layouts, error boundaries require default exports)
- **One component per file** — exception: small helper components used only by the parent
- **Props as interface** — defined directly above the component
- **No prop spreading** — `{...props}` hides the API; pass props explicitly
- **No inline styles** — use Tailwind classes
- **Destructure props** in the function signature
- **Return early** for loading/error states before the main render
- **No `forwardRef`** — React 19 passes `ref` as a regular prop

### Import Aliases

Use `@/` path alias for all imports from `src/`:

```typescript
// FORBIDDEN
import { Button } from "../../../components/ui/button";

// REQUIRED
import { Button } from "@/components/ui/button";
```

## 5. Styling Rules

### Tailwind v4 CSS-First Configuration

Tailwind v4 uses CSS-based configuration. **No `tailwind.config.ts` file.** All tokens are defined in CSS:

```css
/* styles/globals.css */
@import "tailwindcss";
@import "tw-animate-css";

@custom-variant dark (&:is(.dark *));

/* Register StablePay design tokens into Tailwind */
@theme inline {
  /* Surface colors */
  --color-surface-0: var(--surface-0);
  --color-surface-1: var(--surface-1);
  --color-surface-2: var(--surface-2);
  --color-surface-3: var(--surface-3);
  --color-surface-4: var(--surface-4);

  /* Foreground colors */
  --color-fg-1: var(--fg-1);
  --color-fg-2: var(--fg-2);
  --color-fg-3: var(--fg-3);
  --color-fg-4: var(--fg-4);

  /* Border colors */
  --color-border-1: var(--border-1);
  --color-border-2: var(--border-2);
  --color-border-3: var(--border-3);

  /* Solana brand */
  --color-solana-teal: var(--solana-teal);
  --color-solana-purple: var(--solana-purple);
  --color-solana-magenta: var(--solana-magenta);

  /* Semantic */
  --color-usdc: var(--usdc-blue);
  --color-success: var(--success);
  --color-warning: var(--warning);
  --color-danger: var(--danger);
  --color-info: var(--info);
  --color-accent: var(--accent);

  /* shadcn/ui semantic mappings */
  --color-background: var(--surface-1);
  --color-foreground: var(--fg-1);
  --color-primary: var(--accent);
  --color-primary-foreground: #FFFFFF;
  --color-muted: var(--surface-3);
  --color-muted-foreground: var(--fg-3);
  --color-destructive: var(--danger);
  --color-destructive-foreground: #FFFFFF;
  --color-border: var(--border-1);
  --color-input: var(--surface-4);
  --color-ring: var(--accent);

  /* Typography */
  --font-sans: var(--font-sans);
  --font-mono: var(--font-mono);

  /* Radii */
  --radius-sm: var(--radius-sm);
  --radius-md: var(--radius-md);
  --radius-lg: var(--radius-lg);
  --radius-xl: var(--radius-xl);
  --radius-full: var(--radius-full);
}

@layer base {
  body {
    @apply bg-background text-foreground font-sans antialiased;
  }
}
```

### Design Token Source

The design tokens CSS file (`styles/design-tokens.css`) is derived from `stablepay-design-system/project/colors_and_type.css`. It defines the raw CSS variables (`:root` for dark theme default, `[data-theme="light"]` for light theme).

### Class Ordering

Follow the Tailwind recommended order: layout → sizing → spacing → typography → visual → interactive → responsive.

```tsx
<div className="flex items-center gap-3 p-4 text-sm text-fg-2 bg-surface-2 rounded-lg hover:bg-surface-3 transition-colors" />
```

### Conditional Classes

Use `cn()` from shadcn/ui `lib/utils.ts`:

```typescript
import { cn } from "@/lib/utils";

<div className={cn(
  "rounded-lg border p-4",
  isActive ? "border-accent bg-accent/10" : "border-border-1 bg-surface-2",
)} />
```

## 6. Data Fetching

### TanStack Query for Client Components

```typescript
// features/remittance/hooks/use-remittances.ts
import { useQuery } from "@tanstack/react-query";
import type { RemittanceResponse } from "@/types/api";

export function useRemittances() {
  return useQuery({
    queryKey: ["remittances"],
    queryFn: async () => {
      const res = await fetch("/api/remittances");
      if (!res.ok) throw new Error("Failed to fetch remittances");
      return res.json() as Promise<RemittanceResponse[]>;
    },
  });
}
```

### Query Key Convention

```typescript
["remittances"]                          // Entity list
["remittances", remittanceId]            // Entity detail
["remittances", { status: "PENDING" }]   // Filtered list
["wallet"]                               // Singleton
["fx", "rate", { corridor: "USD_INR" }]  // Parameterized
```

### Server Components (Direct Backend Fetch)

```typescript
// app/(app)/activity/page.tsx
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";

export default async function ActivityPage() {
  const token = await requireAuth();
  const remittances = await apiClient.get<RemittanceResponse[]>("/remittances", { token });
  return <ActivityList remittances={remittances} />;
}
```

### Mutations with TanStack Query

```typescript
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

export function useCreateRemittance() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateRemittanceRequest) => {
      const res = await fetch("/api/remittances", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      if (!res.ok) throw new Error("Failed to create remittance");
      return res.json() as Promise<RemittanceResponse>;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["remittances"] });
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
    },
    onError: (error) => {
      toast.error(error.message);
    },
  });
}
```

## 7. State Management

### When to Use What

| State type | Tool | Example |
|---|---|---|
| Server state | TanStack Query | Remittances, wallet balance, FX rates |
| Form state | React Hook Form + Zod | Send flow inputs, recipient entry |
| Multi-step wizard | Zustand (persisted) | Send flow progress (amount → recipient → review) |
| UI state (local) | `useState` | Modal open, dropdown expanded |
| URL state | `useSearchParams` / `nuqs` | Filters, pagination |
| Toast notifications | Sonner | Success/error messages |

### Zustand Store Pattern (with Crash Recovery)

```typescript
// stores/send-flow.ts
import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

interface SendFlowState {
  amountUsdc: string;
  recipientPhone: string;
  recipientName: string;
  fxRate: string | null;
  setAmount: (amount: string) => void;
  setRecipient: (phone: string, name: string) => void;
  setFxRate: (rate: string) => void;
  reset: () => void;
}

const initialState = {
  amountUsdc: "",
  recipientPhone: "",
  recipientName: "",
  fxRate: null,
};

export const useSendFlowStore = create<SendFlowState>()(
  persist(
    (set) => ({
      ...initialState,
      setAmount: (amountUsdc) => set({ amountUsdc }),
      setRecipient: (recipientPhone, recipientName) => set({ recipientPhone, recipientName }),
      setFxRate: (fxRate) => set({ fxRate }),
      reset: () => set(initialState),
    }),
    {
      name: "send-flow",
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);
```

## 8. Form Handling (shadcn/ui v4 Pattern)

shadcn/ui v4 uses the new `<Field>` component system with React Hook Form's `Controller`:

```typescript
"use client";

import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { useSendFlowStore } from "@/stores/send-flow";

const sendAmountSchema = z.object({
  amountUsdc: z
    .string()
    .min(1, "Amount is required")
    .refine((v) => parseFloat(v) > 0, "Amount must be positive")
    .refine((v) => parseFloat(v) <= 10000, "Maximum $10,000 per transfer"),
});

type SendAmountForm = z.infer<typeof sendAmountSchema>;

export function SendAmountInput() {
  const form = useForm<SendAmountForm>({
    resolver: zodResolver(sendAmountSchema),
    defaultValues: { amountUsdc: "" },
  });

  function onSubmit(data: SendAmountForm) {
    useSendFlowStore.getState().setAmount(data.amountUsdc);
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <FieldGroup>
        <Controller
          name="amountUsdc"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid || undefined}>
              <FieldLabel htmlFor="amountUsdc">Amount (USDC)</FieldLabel>
              <Input {...field} id="amountUsdc" inputMode="decimal" aria-invalid={fieldState.invalid} />
              <FieldDescription>Enter the amount in US dollars.</FieldDescription>
              {fieldState.error && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
      </FieldGroup>
      <Button type="submit" className="mt-4 w-full">Continue</Button>
    </form>
  );
}
```

## 9. Error Handling

### API Error Mapping

```typescript
// lib/errors.ts
const ERROR_MESSAGES: Record<string, string> = {
  "SP-0001": "Wallet not found. Please try again.",
  "SP-0002": "Insufficient balance. Add funds to continue.",
  "SP-0003": "FX rate expired. We'll get a fresh quote.",
  "SP-0004": "Remittance not found.",
};

export function getErrorMessage(code: string): string {
  return ERROR_MESSAGES[code] ?? "Something went wrong. Please try again.";
}
```

### Toast Notifications (Sonner)

```typescript
import { toast } from "sonner";

// Success
toast.success("Remittance sent successfully");

// Error with API code
toast.error(getErrorMessage(error.code));

// In root layout:
import { Toaster } from "@/components/ui/sonner";
// <Toaster position="bottom-center" />
```

### Error Boundaries

- **`error.tsx`** at route group level — `(auth)/error.tsx`, `(app)/error.tsx`, `send/error.tsx`
- **`global-error.tsx`** at root — catches root layout errors (the only boundary that wraps layout.tsx)
- **TanStack Query errors** handled inline with `isError` / `error` from the hook
- **Form validation errors** from Zod schemas, displayed inline via `<FieldError>`
- **Never use `try/catch` for control flow** — let error boundaries handle unexpected errors

## 10. Accessibility

- **Semantic HTML** — `<button>` not `<div onClick>`, `<nav>` not `<div className="nav">`
- **All interactive elements focusable** — keyboard navigation works
- **ARIA labels** on icon-only buttons: `<Button size="icon" aria-label="Send money">`
- **Color contrast** — 4.5:1 minimum (design system tokens satisfy this)
- **Focus indicators** — visible focus rings (design system provides `:focus-visible` styles)
- **Motion** — respect `prefers-reduced-motion` via Tailwind `motion-safe:` / `motion-reduce:`
- **Form accessibility** — every input has a `<FieldLabel>`, errors linked via `aria-describedby`

## 11. Performance

- **Server Components by default** — minimize client JS
- **`use cache`** directive for cacheable Server Components and functions (Next.js 16)
- **PPR (Partial Prerendering)** — static shell from edge + dynamic content via Suspense
- **Dynamic imports** for heavy client components: `const Chart = dynamic(() => import("./chart"), { ssr: false })`
- **Image optimization** — use `next/image` for all images
- **Font optimization** — use `next/font/google` for Space Grotesk and JetBrains Mono
- **No barrel exports** (`index.ts` re-exports) — they break tree-shaking
- **Prefetch links** — `<Link>` prefetches by default; don't disable without reason
- **Suspense boundaries** — one per independent data dependency, grouped by visual section

## 12. Linting and Formatting

Use **Biome 2.x** (replaces ESLint + Prettier — single tool, faster, CSS + GraphQL support):

```jsonc
// biome.json
{
  "$schema": "https://biomejs.dev/schemas/2.4.0/schema.json",
  "formatter": {
    "indentStyle": "space",
    "indentWidth": 2,
    "lineWidth": 100
  },
  "linter": {
    "rules": {
      "recommended": true,
      "suspicious": {
        "noExplicitAny": "error"
      },
      "style": {
        "noDefaultExport": "warn",
        "useImportType": "error"
      }
    }
  },
  "css": {
    "linter": { "enabled": true },
    "formatter": { "enabled": true }
  }
}
```

Run before committing:

```bash
bunx biome check --write .
```

## 13. Environment Variables

- **Public vars** prefixed with `NEXT_PUBLIC_` — exposed to the browser
- **Server-only vars** — no prefix, only accessible in Server Components / Route Handlers
- **`.env.local`** for local development (git-ignored)
- **Never commit secrets** — API keys, auth secrets go in `.env.local` only

```bash
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:3000
STABLEPAY_BACKEND_URL=http://localhost:8080
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx
```
