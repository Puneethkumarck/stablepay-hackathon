import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { toast } from "sonner";
import { describe, expect, it, vi } from "vitest";
import { server } from "@/test-utils/msw-server";
import { renderWithProviders } from "@/test-utils/render";
import AddFundsPage from "../page";

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn() },
}));

function setupWalletHandler() {
  server.use(
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
  );
}

function setupFundHandler(response?: object, status?: number) {
  server.use(
    http.post("/api/wallets/1/fund", () =>
      HttpResponse.json(
        response ?? {
          fundingId: "fund-uuid-123",
          walletId: 1,
          amountUsdc: "50.00",
          status: "PAYMENT_CONFIRMED",
          stripePaymentIntentId: "pi_123",
          stripeClientSecret: "pi_123_secret",
          createdAt: "2026-04-24T10:00:00Z",
        },
        { status: status ?? 201 },
      ),
    ),
  );
}

function setupPollHandler(status: string) {
  server.use(
    http.get("/api/funding-orders/fund-uuid-123", () =>
      HttpResponse.json({
        fundingId: "fund-uuid-123",
        walletId: 1,
        amountUsdc: "50.00",
        status,
        stripePaymentIntentId: "pi_123",
        createdAt: "2026-04-24T10:00:00Z",
      }),
    ),
  );
}

describe("AddFundsPage", () => {
  beforeEach(() => {
    mockPush.mockClear();
    vi.mocked(toast.error).mockClear();
  });

  it("should render amount entry with default $50", () => {
    // given
    setupWalletHandler();

    // when
    renderWithProviders(<AddFundsPage />);

    // then
    expect(screen.getByRole("textbox", { name: "Amount in USD" })).toHaveValue("50");
  });

  it("should show preset buttons", () => {
    // given
    setupWalletHandler();

    // when
    renderWithProviders(<AddFundsPage />);

    // then
    expect(screen.getByRole("button", { name: "$25" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "$50" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "$100" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "$250" })).toBeInTheDocument();
  });

  it("should update amount when preset is clicked", async () => {
    // given
    setupWalletHandler();
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await user.click(screen.getByRole("button", { name: "$100" }));

    // then
    expect(screen.getByRole("textbox", { name: "Amount in USD" })).toHaveValue("100");
  });

  it("should show pay button with amount", async () => {
    // given
    setupWalletHandler();

    // when
    renderWithProviders(<AddFundsPage />);

    // then
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
  });

  it("should show validation info row", () => {
    // given
    setupWalletHandler();

    // when
    renderWithProviders(<AddFundsPage />);

    // then
    expect(screen.getByText(/Min \$1\.00 · Max \$10,000\.00/)).toBeInTheDocument();
  });

  it("should show payment info rows", () => {
    // given
    setupWalletHandler();

    // when
    renderWithProviders(<AddFundsPage />);

    // then
    expect(screen.getByText("Payment method")).toBeInTheDocument();
    expect(screen.getByText("Credit / Debit card")).toBeInTheDocument();
    expect(screen.getByText("Powered by")).toBeInTheDocument();
    expect(screen.getByText("Stripe")).toBeInTheDocument();
  });

  it("should show footer text", () => {
    // given
    setupWalletHandler();

    // when
    renderWithProviders(<AddFundsPage />);

    // then
    expect(screen.getByText("Secured by Stripe · Funds appear instantly")).toBeInTheDocument();
  });

  it("should disable pay button when amount is invalid", async () => {
    // given
    setupWalletHandler();
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    const input = screen.getByRole("textbox", { name: "Amount in USD" });
    await user.clear(input);
    await user.type(input, "0");

    // then
    expect(screen.getByRole("button", { name: "Pay $— with Stripe" })).toBeDisabled();
  });

  it("should transition to processing state on successful fund", async () => {
    // given
    setupWalletHandler();
    setupFundHandler();
    setupPollHandler("PAYMENT_CONFIRMED");
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
    await user.click(screen.getByRole("button", { name: "Pay $50.00 with Stripe" }));

    // then
    await waitFor(() => {
      expect(screen.getByText("Processing payment…")).toBeInTheDocument();
    });
    expect(screen.getByText("Stripe · $50.00 USD")).toBeInTheDocument();
  });

  it("should transition to done state when funding is complete", async () => {
    // given
    setupWalletHandler();
    setupFundHandler();
    setupPollHandler("FUNDED");
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
    await user.click(screen.getByRole("button", { name: "Pay $50.00 with Stripe" }));

    // then
    await waitFor(() => {
      expect(screen.getByText("$50.00 USDC")).toBeInTheDocument();
    });
    expect(screen.getByText("Added to your wallet")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Done" })).toBeInTheDocument();
  });

  it("should navigate to /home when Done is clicked", async () => {
    // given
    setupWalletHandler();
    setupFundHandler();
    setupPollHandler("FUNDED");
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
    await user.click(screen.getByRole("button", { name: "Pay $50.00 with Stripe" }));
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Done" })).toBeInTheDocument();
    });
    await user.click(screen.getByRole("button", { name: "Done" }));

    // then
    expect(mockPush).toHaveBeenCalledWith("/home");
  });

  it("should transition to failed state when payment fails", async () => {
    // given
    setupWalletHandler();
    setupFundHandler();
    setupPollHandler("FAILED");
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
    await user.click(screen.getByRole("button", { name: "Pay $50.00 with Stripe" }));

    // then
    await waitFor(() => {
      expect(screen.getByText("Payment failed")).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Try again" })).toBeInTheDocument();
  });

  it("should transition to failed state when payment is refunded", async () => {
    // given
    setupWalletHandler();
    setupFundHandler();
    setupPollHandler("REFUNDED");
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
    await user.click(screen.getByRole("button", { name: "Pay $50.00 with Stripe" }));

    // then
    await waitFor(() => {
      expect(screen.getByText("Payment failed")).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Try again" })).toBeInTheDocument();
  });

  it("should reset to amount entry when Try again is clicked", async () => {
    // given
    setupWalletHandler();
    setupFundHandler();
    setupPollHandler("FAILED");
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
    await user.click(screen.getByRole("button", { name: "Pay $50.00 with Stripe" }));
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Try again" })).toBeInTheDocument();
    });
    await user.click(screen.getByRole("button", { name: "Try again" }));

    // then
    expect(screen.getByRole("textbox", { name: "Amount in USD" })).toHaveValue("50");
    expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeInTheDocument();
  });

  it("should show error toast on 409 conflict (SP-0022)", async () => {
    // given
    setupWalletHandler();
    setupFundHandler({ errorCode: "SP-0022", message: "Funding already in progress" }, 409);
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Pay $50.00 with Stripe" })).toBeEnabled();
    });
    await user.click(screen.getByRole("button", { name: "Pay $50.00 with Stripe" }));

    // then
    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("A funding order is already in progress.");
    });
  });

  it("should navigate to /home on back button", async () => {
    // given
    setupWalletHandler();
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Go back" }));

    // then
    expect(mockPush).toHaveBeenCalledWith("/home");
  });

  it("should only allow numeric and decimal input", async () => {
    // given
    setupWalletHandler();
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    const input = screen.getByRole("textbox", { name: "Amount in USD" });
    await user.clear(input);
    await user.type(input, "abc123.45");

    // then
    expect(input).toHaveValue("123.45");
  });

  it("should strip leading zeros from amount", async () => {
    // given
    setupWalletHandler();
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    const input = screen.getByRole("textbox", { name: "Amount in USD" });
    await user.clear(input);
    await user.type(input, "0050");

    // then
    expect(input).toHaveValue("50");
  });

  it("should reject a lone decimal point", async () => {
    // given
    setupWalletHandler();
    const user = userEvent.setup();
    renderWithProviders(<AddFundsPage />);

    // when
    const input = screen.getByRole("textbox", { name: "Amount in USD" });
    await user.clear(input);
    await user.type(input, ".");

    // then
    expect(input).toHaveValue("");
  });
});
