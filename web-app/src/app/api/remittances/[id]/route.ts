import { NextResponse } from "next/server";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { RemittanceResponse } from "@/types/api";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const token = await requireAuth();
  const { id } = await params;
  const data = await apiClient.get<RemittanceResponse>(`/api/remittances/${id}`, { token });
  return NextResponse.json(data);
}
