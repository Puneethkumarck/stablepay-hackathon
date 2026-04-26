import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { toast } from "sonner";
import { describe, expect, it, vi } from "vitest";
import { useSendFlowStore } from "@/stores/send-flow";
import { server } from "@/test-utils/msw-server";
import { renderWithProviders } from "@/test-utils/render";
import SendReviewPage from "../page";

const mockPush = vi.fn();
const mockReplace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn() },
}));

function setupStore(overrides?: {
  amountUsdc?: string;
  recipientPhone?: string;
  recipientName?: string;
  fxRate?: string;
  fxRateExpiresAt?: string;
}) {
  const store = useSendFlowStore.getState();
  store.setAmount(overrides?.amountUsdc ?? "100");
  store.setRecipient(overrides?.recipientPhone ?? "+919876543210", overrides?.recipientName);
  store.setFxRate(
    overrides?.fxRate ?? "84.50",
    overrides?.fxRateExpiresAt ?? "2099-12-31T23:59:59Z",
  );
}

describe("SendReviewPage", () => {
  beforeEach(() => {
    mockPush.mockClear();
    mockReplace.mockClear();
    vi.mocked(toast.error).mockClear();
    useSendFlowStore.getState().reset();
  });

  it("should redirect to /send when amount is not set", () => {
    // given
    useSendFlowStore.getState().setRecipient("+919876543210");

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(mockReplace).toHaveBeenCalledWith("/send");
  });

  it("should redirect to /send when phone is not set", () => {
    // given
    useSendFlowStore.getState().setAmount("100");

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(mockReplace).toHaveBeenCalledWith("/send");
  });

  it("should redirect to /send when fxRate is not set", () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    useSendFlowStore.getState().setRecipient("+919876543210");

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(mockReplace).toHaveBeenCalledWith("/send");
  });

  it("should show step indicator", () => {
    // given
    setupStore();

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(screen.getByText("Step 3 of 3")).toBeInTheDocument();
  });

  it("should display INR amount and recipient phone", () => {
    // given
    setupStore();

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(screen.getByText("They receive")).toBeInTheDocument();
    expect(screen.getByText("to +919876543210")).toBeInTheDocument();
    expect(screen.getByText(/8,450\.00/)).toBeInTheDocument();
  });

  it("should display all detail rows", () => {
    // given
    setupStore();

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(screen.getByText("You send")).toBeInTheDocument();
    expect(screen.getByText("$100.00 USDC")).toBeInTheDocument();
    expect(screen.getByText("FX rate")).toBeInTheDocument();
    expect(screen.getByText("84.50 INR / USD")).toBeInTheDocument();
    expect(screen.getByText("Network fee")).toBeInTheDocument();
    expect(screen.getByText("$0.002")).toBeInTheDocument();
    expect(screen.getByText("Delivery")).toBeInTheDocument();
    expect(screen.getByText("Instant on-chain + UPI")).toBeInTheDocument();
    expect(screen.getByText("Claim expires")).toBeInTheDocument();
    expect(screen.getByText("48 hours")).toBeInTheDocument();
  });

  it("should display escrow info banner", () => {
    // given
    setupStore();

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(screen.getByText(/Funds are held in a Solana escrow/)).toBeInTheDocument();
  });

  it("should show confirm button with amount", () => {
    // given
    setupStore();

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(screen.getByRole("button", { name: "Confirm & send $100.00" })).toBeEnabled();
  });

  it("should navigate to /send/sending and reset store on successful confirm", async () => {
    // given
    setupStore();
    const user = userEvent.setup();
    renderWithProviders(<SendReviewPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Confirm & send $100.00" }));

    // then
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith(expect.stringContaining("/send/sending?"));
      const url = String(mockPush.mock.calls[0]?.[0]);
      const params = new URLSearchParams(url.split("?")[1]);
      expect(params.get("remittanceId")).toBe("rem-uuid-123");
      expect(params.get("amount")).toBe("100");
      expect(params.get("phone")).toBe("+919876543210");
      expect(params.get("fxRate")).toBe("84.50");
    });
    expect(useSendFlowStore.getState().amountUsdc).toBe("");
  });

  it("should show Signing… and disable button during API call", async () => {
    // given
    let resolveRequest: (() => void) | undefined;
    server.use(
      http.post(
        "/api/remittances",
        () =>
          new Promise<Response>((resolve) => {
            resolveRequest = () =>
              resolve(
                HttpResponse.json(
                  { id: 1, remittanceId: "rem-uuid-123", status: "INITIATED" },
                  { status: 201 },
                ),
              );
          }),
      ),
    );
    setupStore();
    const user = userEvent.setup();
    renderWithProviders(<SendReviewPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Confirm & send $100.00" }));

    // then
    expect(screen.getByRole("button", { name: "Signing…" })).toBeDisabled();

    // cleanup
    resolveRequest?.();
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalled();
    });
  });

  it("should show error toast on API failure with SP-0002", async () => {
    // given
    server.use(
      http.post("/api/remittances", () =>
        HttpResponse.json(
          { errorCode: "SP-0002", message: "Insufficient balance" },
          { status: 400 },
        ),
      ),
    );
    setupStore();
    const user = userEvent.setup();
    renderWithProviders(<SendReviewPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Confirm & send $100.00" }));

    // then
    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Insufficient balance. Add funds to continue.");
    });
  });

  it("should show expired rate warning and disable confirm", () => {
    // given
    setupStore({ fxRateExpiresAt: "2020-01-01T00:00:00Z" });

    // when
    renderWithProviders(<SendReviewPage />);

    // then
    expect(screen.getByText(/FX rate has expired/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Confirm/ })).toBeDisabled();
  });

  it("should navigate to /send/recipient on back", async () => {
    // given
    setupStore();
    const user = userEvent.setup();
    renderWithProviders(<SendReviewPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Go back" }));

    // then
    expect(mockPush).toHaveBeenCalledWith("/send/recipient");
  });

  it("should include recipientName in API request when available", async () => {
    // given
    let capturedBody: Record<string, unknown> | undefined;
    server.use(
      http.post("/api/remittances", async ({ request }) => {
        capturedBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          { id: 1, remittanceId: "rem-uuid-123", status: "INITIATED" },
          { status: 201 },
        );
      }),
    );
    setupStore({ recipientName: "Raj Patel" });
    const user = userEvent.setup();
    renderWithProviders(<SendReviewPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Confirm & send $100.00" }));

    // then
    await waitFor(() => {
      expect(capturedBody).toBeDefined();
    });
    expect(capturedBody?.recipientName).toBe("Raj Patel");
    expect(capturedBody?.recipientPhone).toBe("+919876543210");
    expect(capturedBody?.amountUsdc).toBe("100");
  });
});
