"use client";

import { useQuery } from "@tanstack/react-query";
import type { FxRateResponse } from "@/types/api";

async function fetchFxRate(): Promise<FxRateResponse> {
  const response = await fetch("/api/fx/USD-INR");

  if (!response.ok) {
    throw new Error("Failed to fetch FX rate");
  }

  return response.json() as Promise<FxRateResponse>;
}

export function useFxRate() {
  return useQuery<FxRateResponse>({
    queryKey: ["fx", "USD-INR"],
    queryFn: fetchFxRate,
    staleTime: 30_000,
    refetchInterval: 30_000,
  });
}
