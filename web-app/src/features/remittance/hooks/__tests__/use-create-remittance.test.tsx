import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import type { ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { server } from "@/test-utils/msw-server";
import { CreateRemittanceError, useCreateRemittance } from "../use-create-remittance";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("useCreateRemittance", () => {
  it("should return remittance data on success", async () => {
    // given
    const { result } = renderHook(() => useCreateRemittance(), {
      wrapper: createWrapper(),
    });

    // when
    result.current.mutate({ recipientPhone: "+919876543210", amountUsdc: "100" });

    // then
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(result.current.data?.remittanceId).toBe("rem-uuid-123");
  });

  it("should throw CreateRemittanceError on API failure", async () => {
    // given
    server.use(
      http.post("/api/remittances", () =>
        HttpResponse.json(
          { errorCode: "SP-0002", message: "Insufficient balance" },
          { status: 400 },
        ),
      ),
    );
    const { result } = renderHook(() => useCreateRemittance(), {
      wrapper: createWrapper(),
    });

    // when
    result.current.mutate({ recipientPhone: "+919876543210", amountUsdc: "100" });

    // then
    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
    expect(result.current.error).toBeInstanceOf(CreateRemittanceError);
    expect(result.current.error?.errorCode).toBe("SP-0002");
  });

  it("should throw CreateRemittanceError with fallback on unparseable response", async () => {
    // given
    server.use(
      http.post("/api/remittances", () => new HttpResponse("Server Error", { status: 500 })),
    );
    const { result } = renderHook(() => useCreateRemittance(), {
      wrapper: createWrapper(),
    });

    // when
    result.current.mutate({ recipientPhone: "+919876543210", amountUsdc: "100" });

    // then
    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
    expect(result.current.error).toBeInstanceOf(CreateRemittanceError);
    expect(result.current.error?.errorCode).toBe("SP-9999");
  });
});
