import { NextResponse } from "next/server";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { FxRateResponse } from "@/types/api";

export async function GET(_request: Request, { params }: { params: Promise<{ pair: string }> }) {
  const token = await requireAuth();
  const { pair } = await params;
  const data = await apiClient.get<FxRateResponse>(`/api/fx/${pair}`, { token });
  return NextResponse.json(data);
}
