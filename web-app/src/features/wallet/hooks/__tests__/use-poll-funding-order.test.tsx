import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import type { ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { server } from "@/test-utils/msw-server";
import { usePollFundingOrder } from "../use-poll-funding-order";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("usePollFundingOrder", () => {
  it("should not fetch when fundingId is null", () => {
    // given // when
    const { result } = renderHook(() => usePollFundingOrder(null), {
      wrapper: createWrapper(),
    });

    // then
    expect(result.current.isFetching).toBe(false);
  });

  it("should fetch funding order when fundingId is provided", async () => {
    // given
    server.use(
      http.get("/api/funding-orders/fund-uuid-123", () =>
        HttpResponse.json({
          fundingId: "fund-uuid-123",
          walletId: 1,
          amountUsdc: "50.00",
          status: "FUNDED",
          stripePaymentIntentId: "pi_123",
          createdAt: "2026-04-24T10:00:00Z",
        }),
      ),
    );

    // when
    const { result } = renderHook(() => usePollFundingOrder("fund-uuid-123"), {
      wrapper: createWrapper(),
    });

    // then
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(result.current.data?.status).toBe("FUNDED");
  });

  it("should handle fetch error gracefully", async () => {
    // given
    server.use(
      http.get("/api/funding-orders/fund-bad", () =>
        HttpResponse.json({ message: "Not found" }, { status: 404 }),
      ),
    );

    // when
    const { result } = renderHook(() => usePollFundingOrder("fund-bad"), {
      wrapper: createWrapper(),
    });

    // then
    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});
