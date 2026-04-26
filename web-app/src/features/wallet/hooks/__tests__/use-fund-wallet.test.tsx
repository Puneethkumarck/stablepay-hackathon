import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import type { ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { server } from "@/test-utils/msw-server";
import { FundWalletError, useFundWallet } from "../use-fund-wallet";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("useFundWallet", () => {
  it("should create a funding order successfully", async () => {
    // given
    server.use(
      http.post("/api/wallets/1/fund", () =>
        HttpResponse.json(
          {
            fundingId: "fund-uuid-123",
            walletId: 1,
            amountUsdc: "50.00",
            status: "PAYMENT_CONFIRMED",
            stripePaymentIntentId: "pi_123",
            stripeClientSecret: "pi_123_secret",
            createdAt: "2026-04-24T10:00:00Z",
          },
          { status: 201 },
        ),
      ),
    );

    // when
    const { result } = renderHook(() => useFundWallet(), {
      wrapper: createWrapper(),
    });
    result.current.mutate({ walletId: 1, amount: "50" });

    // then
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(result.current.data?.fundingId).toBe("fund-uuid-123");
    expect(result.current.data?.status).toBe("PAYMENT_CONFIRMED");
  });

  it("should throw FundWalletError on conflict", async () => {
    // given
    server.use(
      http.post("/api/wallets/1/fund", () =>
        HttpResponse.json(
          { errorCode: "SP-0022", message: "Funding already in progress" },
          { status: 409 },
        ),
      ),
    );

    // when
    const { result } = renderHook(() => useFundWallet(), {
      wrapper: createWrapper(),
    });
    result.current.mutate({ walletId: 1, amount: "50" });

    // then
    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
    expect(result.current.error).toBeInstanceOf(FundWalletError);
    expect(result.current.error?.errorCode).toBe("SP-0022");
  });
});
