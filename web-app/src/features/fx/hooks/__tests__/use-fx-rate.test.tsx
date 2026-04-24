import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import type { ReactNode } from "react";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import type { FxRateResponse } from "@/types/api";
import { useFxRate } from "../use-fx-rate";

const FX_RESPONSE: FxRateResponse = {
  rate: "84.50",
  source: "open.er-api.com",
  timestamp: "2026-04-24T10:00:00Z",
  expiresAt: "2026-04-24T10:05:00Z",
};

const server = setupServer(
  http.get("/api/fx/USD-INR", () => {
    return HttpResponse.json(FX_RESPONSE);
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("useFxRate", () => {
  it("should fetch and return FX rate data", async () => {
    // when
    const { result } = renderHook(() => useFxRate(), {
      wrapper: createWrapper(),
    });

    // then
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(FX_RESPONSE);
  });

  it("should return error state when API fails", async () => {
    // given
    server.use(
      http.get("/api/fx/USD-INR", () => {
        return HttpResponse.json(
          { errorCode: "SP-0014", message: "Unsupported corridor" },
          { status: 400 },
        );
      }),
    );

    // when
    const { result } = renderHook(() => useFxRate(), {
      wrapper: createWrapper(),
    });

    // then
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
