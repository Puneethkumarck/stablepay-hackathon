import { type NextRequest, NextResponse } from "next/server";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { PageResponse, RemittanceResponse } from "@/types/api";

export async function GET(request: NextRequest) {
  const token = await requireAuth();
  const { searchParams } = request.nextUrl;
  const query = searchParams.toString();
  const path = query ? `/api/remittances?${query}` : "/api/remittances";
  const data = await apiClient.get<PageResponse<RemittanceResponse>>(path, { token });
  return NextResponse.json(data);
}

export async function POST(request: NextRequest) {
  const token = await requireAuth();
  const body = await request.json();
  const data = await apiClient.post<RemittanceResponse>("/api/remittances", { token, body });
  return NextResponse.json(data, { status: 201 });
}
