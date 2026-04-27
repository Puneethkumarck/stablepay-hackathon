import { HttpResponse, http } from "msw";

export const handlers = [
  http.get("/api/wallet", () =>
    HttpResponse.json({
      id: 1,
      solanaAddress: "CrsMdGHJ4wFKqnDE5nXkDAd18xyz",
      availableBalance: "248.50",
      totalBalance: "248.50",
      createdAt: "2026-04-20T10:00:00Z",
      updatedAt: "2026-04-24T10:00:00Z",
    }),
  ),

  http.get("/api/remittances", () => HttpResponse.json({ content: [], totalElements: 0 })),

  http.get("/api/fx/USD-INR", () =>
    HttpResponse.json({
      rate: "84.50",
      source: "open.er-api.com",
      timestamp: "2026-04-24T10:00:00Z",
      expiresAt: "2099-12-31T23:59:59Z",
    }),
  ),

  http.get("/api/recipients/recent", () => HttpResponse.json([])),

  http.post("/api/remittances", async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json(
      {
        id: 1,
        remittanceId: "rem-uuid-123",
        ...body,
        status: "INITIATED",
        createdAt: new Date().toISOString(),
      },
      { status: 201 },
    );
  }),

  http.post("/api/wallets/:id/fund", () =>
    HttpResponse.json(
      {
        fundingId: "fund-uuid-123",
        walletId: 1,
        amountUsdc: "50.00",
        status: "PAYMENT_CONFIRMED",
        stripePaymentIntentId: "pi_123",
        stripeClientSecret: "pi_123_secret",
        createdAt: "2026-04-24T10:00:00Z",
      },
      { status: 201 },
    ),
  ),

  http.get("/api/funding-orders/:fundingId", () =>
    HttpResponse.json({
      fundingId: "fund-uuid-123",
      walletId: 1,
      amountUsdc: "50.00",
      status: "FUNDED",
      stripePaymentIntentId: "pi_123",
      createdAt: "2026-04-24T10:00:00Z",
    }),
  ),

  http.post("/api/claims/:token", () =>
    HttpResponse.json({
      remittanceId: "rem-uuid-123",
      senderDisplayName: "Test Sender",
      amountUsdc: "1.00",
      amountInr: "94.26",
      fxRate: "94.26",
      status: "CLAIMED",
      claimed: true,
      expiresAt: "2026-12-31T23:59:59Z",
    }),
  ),
];
