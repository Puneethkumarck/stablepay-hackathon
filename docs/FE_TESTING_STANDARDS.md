# Testing Standards for StablePay Frontend

> Mandatory testing rules for the StablePay Next.js web application.
> Coding agents must follow these rules exactly.

## 1. Test Stack

| Tool | Version | Purpose |
|---|---|---|
| Vitest | 4.1.x | Test runner + assertions |
| React Testing Library | 16.3.x | Component testing (user-centric) |
| happy-dom | 20.x | DOM environment (5-10x faster than jsdom) |
| Playwright | 1.59.x | End-to-end browser tests |
| MSW | 2.13.x | API mocking (network-level interception) |
| @axe-core/playwright | latest | Automated accessibility testing in E2E |

## 2. Test Strategy (Testing Trophy)

Priority order — integration tests deliver the most value per test:

| Priority | Layer | Tool | Scope | Speed |
|---|---|---|---|---|
| 1 | Integration | Vitest + RTL + MSW | Feature flows with mocked API | < 200ms each |
| 2 | Component | Vitest + RTL | Individual components with props | < 50ms each |
| 3 | Unit | Vitest | Pure functions, utilities, hooks, stores | < 5ms each |
| 4 | E2E | Playwright | Critical user journeys only | < 10s each |
| 5 | Accessibility | Playwright + axe-core | WCAG compliance on critical pages | < 5s each |

### What to Test at Each Layer

**Unit tests** — formatting functions (`formatCurrency`, `formatPhone`), Zod schemas, Zustand store logic, pure utility functions.

**Component tests** — rendering with props, user interactions (click, type, submit), conditional rendering, error states, loading states.

**Integration tests** — multi-step flows (send wizard), API call → UI update cycles, form submission → API call → success/error display. Most of your tests should be at this level.

**E2E tests** — critical user journeys only: login → home, send remittance end-to-end, view activity, add funds. Not every edge case.

**Accessibility tests** — axe-core scans on every page route, keyboard navigation for critical flows.

## 3. File Organization

```
src/
├── features/
│   └── remittance/
│       ├── components/
│       │   ├── send-review-card.tsx
│       │   └── __tests__/
│       │       └── send-review-card.test.tsx
│       ├── hooks/
│       │   ├── use-remittances.ts
│       │   └── __tests__/
│       │       └── use-remittances.test.ts
│       └── types.ts
├── lib/
│   ├── format.ts
│   └── __tests__/
│       └── format.test.ts
├── stores/
│   ├── send-flow.ts
│   └── __tests__/
│       └── send-flow.test.ts
├── test-utils/
│   ├── msw-server.ts               # MSW server setup
│   ├── handlers.ts                  # Default API mock handlers
│   └── render.tsx                   # Custom render with providers
└── test-setup.ts                    # Global setup (RTL matchers + MSW lifecycle)

e2e/                                 # Playwright tests (project root)
├── send-flow.spec.ts
├── activity.spec.ts
├── accessibility.spec.ts
└── fixtures/
    └── auth.ts                      # Shared auth fixture
```

**Rule:** Test files live in `__tests__/` directories adjacent to the code they test. E2E tests live in a top-level `e2e/` directory.

## 4. Vitest Configuration

```typescript
// vitest.config.ts
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig({
  plugins: [react(), tsconfigPaths()],
  test: {
    environment: "happy-dom",
    globals: true,
    setupFiles: ["./src/test-setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      exclude: [
        "src/components/ui/**",
        "src/**/*.test.*",
        "src/types/**",
        "src/test-utils/**",
      ],
    },
  },
});
```

### Test Setup

```typescript
// src/test-setup.ts
import "@testing-library/jest-dom/vitest";
import { server } from "./test-utils/msw-server";
import { afterAll, afterEach, beforeAll } from "vitest";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

### Custom Render with Providers

```typescript
// src/test-utils/render.tsx
import { render } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";

export function renderWithProviders(ui: ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  );
}
```

## 5. Component Test Pattern

### The Pattern: Render → Act → Assert

```typescript
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { renderWithProviders } from "@/test-utils/render";
import { SendReviewCard } from "../send-review-card";

const remittance = {
  id: "abc-123",
  recipientPhone: "+919876543210",
  amountUsdc: "100.00",
  amountInr: "8350.00",
  fxRate: "83.50",
  status: "PENDING" as const,
};

describe("SendReviewCard", () => {
  it("should display remittance details", () => {
    // given
    renderWithProviders(<SendReviewCard remittance={remittance} onConfirm={vi.fn()} />);

    // then
    expect(screen.getByText("$100.00")).toBeInTheDocument();
    expect(screen.getByText("₹8,350.00")).toBeInTheDocument();
    expect(screen.getByText("83.50")).toBeInTheDocument();
  });

  it("should call onConfirm when send button is clicked", async () => {
    // given
    const onConfirm = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<SendReviewCard remittance={remittance} onConfirm={onConfirm} />);

    // when
    await user.click(screen.getByRole("button", { name: /send/i }));

    // then
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it("should disable button while submitting", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendReviewCard remittance={remittance} onConfirm={vi.fn()} />);

    // when
    await user.click(screen.getByRole("button", { name: /send/i }));

    // then
    expect(screen.getByRole("button", { name: /send/i })).toBeDisabled();
  });
});
```

### Test Structure Rules

- **`// given`, `// when`, `// then` comments** in every test — bare markers, no trailing rationale
- **`describe` block** per component or module
- **`it("should...")` naming** — describes the expected behavior
- **`userEvent` over `fireEvent`** — `userEvent` simulates real user interactions
- **Query priority**: `getByRole` > `getByLabelText` > `getByText` > `getByTestId`
- **Never query by CSS class, element tag, or data attribute** (unless `data-testid` as last resort)
- **Use `renderWithProviders`** for components that need TanStack Query or other context

### FORBIDDEN Patterns

```typescript
// FORBIDDEN: implementation-detail queries
screen.getByClassName("send-btn");
container.querySelector(".amount-input");

// FORBIDDEN: testing implementation details
expect(component.state.isLoading).toBe(true);
expect(setState).toHaveBeenCalledWith({ loading: true });

// FORBIDDEN: snapshot tests (brittle, no intent)
expect(tree).toMatchSnapshot();

// FORBIDDEN: testing library internals
expect(result.current._internal.cache).toBeDefined();
```

## 6. Hook Test Pattern

```typescript
import { renderHook, waitFor } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { server } from "@/test-utils/msw-server";
import { useRemittances } from "../use-remittances";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("useRemittances", () => {
  it("should fetch remittances", async () => {
    // given
    const mockData = [{ id: "abc-123", status: "PENDING", amountUsdc: "100.00" }];
    server.use(
      http.get("/api/remittances", () => HttpResponse.json(mockData)),
    );

    // when
    const { result } = renderHook(() => useRemittances(), { wrapper: createWrapper() });

    // then
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(mockData);
  });

  it("should handle fetch error", async () => {
    // given
    server.use(
      http.get("/api/remittances", () => HttpResponse.error()),
    );

    // when
    const { result } = renderHook(() => useRemittances(), { wrapper: createWrapper() });

    // then
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
```

## 7. MSW Setup

```typescript
// src/test-utils/msw-server.ts
import { setupServer } from "msw/node";
import { handlers } from "./handlers";

export const server = setupServer(...handlers);
```

```typescript
// src/test-utils/handlers.ts
import { http, HttpResponse } from "msw";

export const handlers = [
  http.get("/api/wallet", () =>
    HttpResponse.json({
      walletId: "wallet-1",
      solanaAddress: "ABC123def456...",
      usdcBalance: "250.00",
    }),
  ),

  http.get("/api/remittances", () =>
    HttpResponse.json([]),
  ),

  http.get("/api/fx/rate", () =>
    HttpResponse.json({
      rate: "83.50",
      corridor: "USD_INR",
      expiresAt: "2026-04-24T12:00:00Z",
    }),
  ),

  http.post("/api/remittances", async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      id: 1,
      remittanceId: "rem-uuid-123",
      ...body,
      status: "INITIATED",
      createdAt: new Date().toISOString(),
    }, { status: 201 });
  }),
];
```

### Per-Test Handler Overrides

```typescript
it("should show error when wallet fetch fails", async () => {
  // given
  server.use(
    http.get("/api/wallet", () =>
      HttpResponse.json(
        { code: "SP-0001", detail: "Wallet not found" },
        { status: 404 },
      ),
    ),
  );

  // when
  renderWithProviders(<WalletBalance />);

  // then
  await waitFor(() => {
    expect(screen.getByText(/wallet not found/i)).toBeInTheDocument();
  });
});
```

## 8. Unit Test Pattern

```typescript
// lib/__tests__/format.test.ts
import { describe, it, expect } from "vitest";
import { formatCurrency, formatPhone } from "../format";

describe("formatCurrency", () => {
  it("should format USD amounts with two decimals", () => {
    expect(formatCurrency("100", "USD")).toBe("$100.00");
  });

  it("should format INR amounts with Indian grouping", () => {
    expect(formatCurrency("835000", "INR")).toBe("₹8,35,000.00");
  });

  it("should handle zero", () => {
    expect(formatCurrency("0", "USD")).toBe("$0.00");
  });

  it("should handle empty string", () => {
    expect(formatCurrency("", "USD")).toBe("$0.00");
  });
});

describe("formatPhone", () => {
  it("should format Indian phone number with country code", () => {
    expect(formatPhone("+919876543210")).toBe("+91 98765 43210");
  });
});
```

## 9. Zustand Store Test Pattern

```typescript
// stores/__tests__/send-flow.test.ts
import { describe, it, expect, beforeEach } from "vitest";
import { useSendFlowStore } from "../send-flow";

describe("useSendFlowStore", () => {
  beforeEach(() => {
    useSendFlowStore.getState().reset();
  });

  it("should set amount", () => {
    // when
    useSendFlowStore.getState().setAmount("100.00");

    // then
    expect(useSendFlowStore.getState().amountUsdc).toBe("100.00");
  });

  it("should set recipient with name", () => {
    // when
    useSendFlowStore.getState().setRecipient("+919876543210", "Ravi Kumar");

    // then
    expect(useSendFlowStore.getState().recipientPhone).toBe("+919876543210");
    expect(useSendFlowStore.getState().recipientName).toBe("Ravi Kumar");
  });

  it("should reset all fields to initial state", () => {
    // given
    useSendFlowStore.getState().setAmount("100.00");
    useSendFlowStore.getState().setRecipient("+919876543210", "Ravi");

    // when
    useSendFlowStore.getState().reset();

    // then
    const state = useSendFlowStore.getState();
    expect(state.amountUsdc).toBe("");
    expect(state.recipientPhone).toBe("");
    expect(state.recipientName).toBe("");
    expect(state.fxRate).toBeNull();
  });
});
```

## 10. Playwright E2E Pattern

```typescript
// e2e/send-flow.spec.ts
import { test, expect } from "@playwright/test";

test.describe("Send Remittance Flow", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/home");
  });

  test("should complete send flow from amount to confirmation", async ({ page }) => {
    // given — navigate to send
    await page.getByRole("link", { name: /send/i }).click();

    // when — enter amount
    await page.getByRole("textbox", { name: /amount/i }).fill("100");
    await page.getByRole("button", { name: /continue/i }).click();

    // when — enter recipient
    await page.getByRole("textbox", { name: /phone/i }).fill("+919876543210");
    await page.getByRole("button", { name: /continue/i }).click();

    // then — review screen shows details
    await expect(page.getByText("$100.00")).toBeVisible();
    await expect(page.getByText("+91 98765 43210")).toBeVisible();

    // when — confirm
    await page.getByRole("button", { name: /send/i }).click();

    // then — sending screen
    await expect(page.getByText(/sending/i)).toBeVisible();
  });
});
```

### Accessibility E2E Tests

```typescript
// e2e/accessibility.spec.ts
import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const routes = ["/home", "/send", "/activity", "/me"];

for (const route of routes) {
  test(`should have no accessibility violations on ${route}`, async ({ page }) => {
    await page.goto(route);

    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();

    expect(results.violations).toEqual([]);
  });
}
```

### Playwright Configuration

```typescript
// playwright.config.ts
import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [["html"], ...(process.env.CI ? [["github" as const]] : [])],
  use: {
    baseURL: "http://localhost:3000",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  webServer: {
    command: "bun run dev",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
  },
});
```

## 11. Coverage Requirements

| Layer | Target | Enforced |
|---|---|---|
| Utility functions | 90% | Yes |
| Zustand stores | 90% | Yes |
| Custom hooks | 80% | Yes |
| Components | 70% | No (focus on behavior, not coverage) |
| E2E | Critical paths covered | No numeric target |

**Do not chase coverage numbers.** A test that asserts behavior is worth ten tests that touch lines. Untested code that works is better than tested code that tests implementation details.

## 12. What NOT to Test

- shadcn/ui component internals (tested upstream)
- CSS / visual styling (use Playwright screenshots for visual regression if needed)
- Third-party library behavior (TanStack Query caching, Zustand internals)
- Implementation details (internal state shape, private functions, re-render counts)
- Next.js framework behavior (routing, SSR hydration, middleware)
- `components/ui/` generated files

## 13. Test Commands

```bash
bun run test                # run all unit + component + integration tests
bun run test:watch          # watch mode during development
bun run test:coverage       # with coverage report
bun run test:e2e            # run Playwright E2E tests (requires dev server)
bun run test:e2e:ui         # Playwright with UI mode (interactive debugging)
```
