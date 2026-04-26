import { type NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.STABLEPAY_BACKEND_URL ?? "http://localhost:8080";

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ token: string }> },
) {
  const { token } = await params;
  const body = await request.json();

  const res = await fetch(`${BACKEND_URL}/api/claims/${token}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}
