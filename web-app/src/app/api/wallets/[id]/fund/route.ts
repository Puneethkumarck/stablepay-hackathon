import { type NextRequest, NextResponse } from "next/server";
import { ApiError, apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { FundingOrderResponse } from "@/types/api";

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const token = await requireAuth();
    const { id } = await params;
    const body = await request.json();
    const data = await apiClient.post<FundingOrderResponse>(`/api/wallets/${id}/fund`, {
      token,
      body,
    });
    return NextResponse.json(data, { status: 201 });
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
