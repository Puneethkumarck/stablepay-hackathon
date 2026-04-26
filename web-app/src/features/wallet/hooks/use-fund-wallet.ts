"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { FundingOrderResponse } from "@/types/api";

export class FundWalletError extends Error {
  readonly errorCode: string;

  constructor(errorCode: string, message: string) {
    super(message);
    this.name = "FundWalletError";
    this.errorCode = errorCode;
  }
}

interface FundWalletRequest {
  walletId: number;
  amount: string;
}

async function fundWallet(request: FundWalletRequest): Promise<FundingOrderResponse> {
  const response = await fetch(`/api/wallets/${request.walletId}/fund`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount: request.amount }),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({
      errorCode: "SP-9999",
      message: "Request failed",
    }));
    throw new FundWalletError(body.errorCode, body.message);
  }

  return response.json() as Promise<FundingOrderResponse>;
}

export function useFundWallet() {
  const queryClient = useQueryClient();

  return useMutation<FundingOrderResponse, FundWalletError, FundWalletRequest>({
    mutationFn: fundWallet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
    },
  });
}
