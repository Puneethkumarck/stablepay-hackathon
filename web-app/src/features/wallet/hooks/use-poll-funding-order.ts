"use client";

import { useQuery } from "@tanstack/react-query";
import type { FundingOrderResponse, FundingStatus } from "@/types/api";

const TERMINAL_STATUSES: ReadonlySet<FundingStatus> = new Set([
  "FUNDED",
  "FAILED",
  "REFUNDED",
  "REFUND_FAILED",
]);

async function fetchFundingOrder(fundingId: string): Promise<FundingOrderResponse> {
  const response = await fetch(`/api/funding-orders/${fundingId}`);

  if (!response.ok) {
    throw new Error("Failed to fetch funding order");
  }

  return response.json() as Promise<FundingOrderResponse>;
}

export function usePollFundingOrder(fundingId: string | null) {
  return useQuery<FundingOrderResponse>({
    queryKey: ["funding-order", fundingId],
    queryFn: () => {
      if (fundingId == null) throw new Error("fundingId is required");
      return fetchFundingOrder(fundingId);
    },
    enabled: fundingId != null,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status && TERMINAL_STATUSES.has(status)) return false;
      return 2000;
    },
  });
}
