import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it, vi } from "vitest";
import { useSendFlowStore } from "@/stores/send-flow";
import { server } from "@/test-utils/msw-server";
import { renderWithProviders } from "@/test-utils/render";
import type { FxRateResponse } from "@/types/api";
import SendAmountPage from "../page";

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

const FX_RATE: FxRateResponse = {
  rate: "84.50",
  source: "open.er-api.com",
  timestamp: "2026-04-24T10:00:00Z",
  expiresAt: "2099-12-31T23:59:59Z",
};

describe("SendAmountPage", () => {
  beforeEach(() => {
    mockPush.mockClear();
    useSendFlowStore.getState().reset();
    server.use(http.get("/api/fx/USD-INR", () => HttpResponse.json(FX_RATE)));
  });

  it("should render amount input with FX rate", async () => {
    // given
    renderWithProviders(<SendAmountPage />);

    // then
    expect(screen.getByLabelText("Amount in USD")).toBeInTheDocument();
    expect(screen.getByText("You send")).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText(/84\.50 INR \/ USD/)).toBeInTheDocument();
    });
  });

  it("should show live INR conversion as user types", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);
    await waitFor(() => {
      expect(screen.getByText(/84\.50 INR \/ USD/)).toBeInTheDocument();
    });

    // when
    await user.type(screen.getByLabelText("Amount in USD"), "100");

    // then
    expect(screen.getByText("₹8,450.00")).toBeInTheDocument();
  });

  it("should reject non-numeric input", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);

    // when
    await user.type(screen.getByLabelText("Amount in USD"), "abc");

    // then
    expect(screen.getByLabelText("Amount in USD")).toHaveValue("");
  });

  it("should show validation error for amount below minimum", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);

    // when
    await user.type(screen.getByLabelText("Amount in USD"), "0.5");

    // then
    expect(screen.getByText("Min $1.00")).toBeInTheDocument();
  });

  it("should show validation error for amount above maximum", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);

    // when
    await user.type(screen.getByLabelText("Amount in USD"), "20000");

    // then
    expect(screen.getByText("Max $10,000.00")).toBeInTheDocument();
  });

  it("should disable continue button when amount is empty", async () => {
    // given
    renderWithProviders(<SendAmountPage />);

    // then
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
    });
  });

  it("should enable continue button for valid amount with FX rate loaded", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);
    await waitFor(() => {
      expect(screen.getByText(/84\.50 INR \/ USD/)).toBeInTheDocument();
    });

    // when
    await user.type(screen.getByLabelText("Amount in USD"), "50");

    // then
    expect(screen.getByRole("button", { name: "Continue" })).toBeEnabled();
  });

  it("should store amount and navigate on continue", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);
    await waitFor(() => {
      expect(screen.getByText(/84\.50 INR \/ USD/)).toBeInTheDocument();
    });

    // when
    await user.type(screen.getByLabelText("Amount in USD"), "100");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    // then
    expect(useSendFlowStore.getState().amountUsdc).toBe("100");
    expect(useSendFlowStore.getState().fxRate).toBe("84.50");
    expect(mockPush).toHaveBeenCalledWith("/send/recipient");
  });

  it("should navigate to home on back", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Go back" }));

    // then
    expect(mockPush).toHaveBeenCalledWith("/home");
  });

  it("should show error state when FX rate fetch fails", async () => {
    // given
    server.use(
      http.get("/api/fx/USD-INR", () =>
        HttpResponse.json(
          { errorCode: "SP-0014", message: "Unsupported corridor" },
          { status: 400 },
        ),
      ),
    );
    renderWithProviders(<SendAmountPage />);

    // then
    await waitFor(() => {
      expect(screen.getByText(/Failed to load rate/)).toBeInTheDocument();
    });
  });

  it("should display hardcoded info rows", async () => {
    // given
    renderWithProviders(<SendAmountPage />);

    // then
    expect(screen.getByText("Network fee")).toBeInTheDocument();
    expect(screen.getByText("$0.002")).toBeInTheDocument();
    expect(screen.getByText("Settlement")).toBeInTheDocument();
    expect(screen.getByText("~30 sec")).toBeInTheDocument();
    expect(screen.getByText("Corridor")).toBeInTheDocument();
    expect(screen.getByText("USD → INR")).toBeInTheDocument();
  });

  it("should show 0.00 INR for dot-only input", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(<SendAmountPage />);
    await waitFor(() => {
      expect(screen.getByText(/84\.50 INR \/ USD/)).toBeInTheDocument();
    });

    // when
    await user.type(screen.getByLabelText("Amount in USD"), ".");

    // then
    expect(screen.getByText("₹0.00")).toBeInTheDocument();
  });
});
