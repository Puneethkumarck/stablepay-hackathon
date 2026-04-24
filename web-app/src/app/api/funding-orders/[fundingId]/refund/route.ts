import { NextResponse } from "next/server";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { FundingOrderResponse } from "@/types/api";

export async function POST(
  _request: Request,
  { params }: { params: Promise<{ fundingId: string }> },
) {
  const token = await requireAuth();
  const { fundingId } = await params;
  const data = await apiClient.post<FundingOrderResponse>(
    `/api/funding-orders/${fundingId}/refund`,
    { token },
  );
  return NextResponse.json(data);
}
