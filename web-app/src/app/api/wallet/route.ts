import { NextResponse } from "next/server";
import { ApiError, apiClient } from "@/lib/api-client";
import { requireAuthWithRefresh } from "@/lib/auth";
import type { WalletResponse } from "@/types/api";

export async function GET() {
  try {
    const token = await requireAuthWithRefresh();
    const data = await apiClient.get<WalletResponse>("/api/wallets/me", { token });
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
