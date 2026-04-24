import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import type { ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { server } from "@/test-utils/msw-server";
import { useWallet } from "../use-wallet";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("useWallet", () => {
  it("should fetch wallet data successfully", async () => {
    // given
    server.use(
      http.get("/api/wallet", () =>
        HttpResponse.json({
          id: 1,
          solanaAddress: "CrsMdGHJ4wFKqnDE5nXkDAd18xyz",
          availableBalance: "248.50",
          totalBalance: "248.50",
          createdAt: "2026-04-20T10:00:00Z",
          updatedAt: "2026-04-24T10:00:00Z",
        }),
      ),
    );

    // when
    const { result } = renderHook(() => useWallet(), {
      wrapper: createWrapper(),
    });

    // then
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(result.current.data?.id).toBe(1);
    expect(result.current.data?.availableBalance).toBe("248.50");
  });

  it("should handle fetch error", async () => {
    // given
    server.use(
      http.get("/api/wallet", () => HttpResponse.json({ message: "Not found" }, { status: 404 })),
    );

    // when
    const { result } = renderHook(() => useWallet(), {
      wrapper: createWrapper(),
    });

    // then
    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});
