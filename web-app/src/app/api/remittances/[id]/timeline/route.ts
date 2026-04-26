import { NextResponse } from "next/server";
import { ApiError, apiClient } from "@/lib/api-client";
import { requireAuthWithRefresh } from "@/lib/auth";
import type { RemittanceTimelineResponse } from "@/types/api";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  try {
    const token = await requireAuthWithRefresh();
    const { id } = await params;
    const data = await apiClient.get<RemittanceTimelineResponse>(
      `/api/remittances/${id}/timeline`,
      { token },
    );
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
