import { NextResponse } from "next/server";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { WalletResponse } from "@/types/api";

export async function GET() {
  const token = await requireAuth();
  const data = await apiClient.get<WalletResponse>("/api/wallets/me", { token });
  return NextResponse.json(data);
}
