import { NextResponse } from "next/server";
import { clearAuthCookies, getAccessToken } from "@/lib/auth";

const BACKEND_URL = process.env.STABLEPAY_BACKEND_URL ?? "http://localhost:8080";

export async function POST() {
  const token = await getAccessToken();

  if (token) {
    try {
      await fetch(`${BACKEND_URL}/api/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch {
      // Best-effort — clear cookies regardless
    }
  }

  await clearAuthCookies();
  return NextResponse.json({ redirectTo: "/login" });
}
