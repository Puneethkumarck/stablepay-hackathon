import { NextResponse } from "next/server";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { RemittanceTimelineResponse } from "@/types/api";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const token = await requireAuth();
  const { id } = await params;
  const data = await apiClient.get<RemittanceTimelineResponse>(`/api/remittances/${id}/timeline`, {
    token,
  });
  return NextResponse.json(data);
}
