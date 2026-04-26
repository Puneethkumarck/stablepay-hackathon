import { type NextRequest, NextResponse } from "next/server";
import { ApiError, apiClient } from "@/lib/api-client";
import { requireAuthWithRefresh } from "@/lib/auth";
import type { PageResponse, RemittanceResponse } from "@/types/api";

export async function GET(request: NextRequest) {
  try {
    const token = await requireAuthWithRefresh();
    const { searchParams } = request.nextUrl;
    const query = searchParams.toString();
    const path = query ? `/api/remittances?${query}` : "/api/remittances";
    const data = await apiClient.get<PageResponse<RemittanceResponse>>(path, { token });
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

export async function POST(request: NextRequest) {
  try {
    const token = await requireAuthWithRefresh();
    const body = await request.json();
    const data = await apiClient.post<RemittanceResponse>("/api/remittances", { token, body });
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
