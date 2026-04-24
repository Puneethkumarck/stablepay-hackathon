import { type NextRequest, NextResponse } from "next/server";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { FundingOrderResponse } from "@/types/api";

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const token = await requireAuth();
  const { id } = await params;
  const body = await request.json();
  const data = await apiClient.post<FundingOrderResponse>(`/api/wallets/${id}/fund`, {
    token,
    body,
  });
  return NextResponse.json(data, { status: 201 });
}
