import { NextResponse } from "next/server";
import { ApiError, apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { FundingOrderResponse } from "@/types/api";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ fundingId: string }> },
) {
  try {
    const token = await requireAuth();
    const { fundingId } = await params;
    const data = await apiClient.get<FundingOrderResponse>(`/api/funding-orders/${fundingId}`, {
      token,
    });
    return NextResponse.json(data);
  } catch (error) {
    if (error instanceof ApiError) {
      return NextResponse.json(
        { errorCode: error.errorCode, message: error.message },
        { status: error.status },
      );
    }
    throw error;
  }
}
