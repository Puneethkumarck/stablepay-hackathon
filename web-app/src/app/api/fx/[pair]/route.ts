import { NextResponse } from "next/server";
import { ApiError, apiClient } from "@/lib/api-client";
import { requireAuthWithRefresh } from "@/lib/auth";
import type { FxRateResponse } from "@/types/api";

export async function GET(_request: Request, { params }: { params: Promise<{ pair: string }> }) {
  try {
    const token = await requireAuthWithRefresh();
    const { pair } = await params;
    const data = await apiClient.get<FxRateResponse>(`/api/fx/${pair}`, { token });
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
