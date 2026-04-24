"use client";

import { useQuery } from "@tanstack/react-query";
import type { WalletResponse } from "@/types/api";

async function fetchWallet(): Promise<WalletResponse> {
  const response = await fetch("/api/wallet");

  if (!response.ok) {
    throw new Error("Failed to fetch wallet");
  }

  return response.json() as Promise<WalletResponse>;
}

export function useWallet() {
  return useQuery<WalletResponse>({
    queryKey: ["wallet"],
    queryFn: fetchWallet,
    staleTime: 30_000,
  });
}
