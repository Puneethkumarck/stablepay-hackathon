import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { server } from "@/test-utils/msw-server";
import { ClaimForm } from "../claim-form";

describe("ClaimForm", () => {
  it("should render UPI input and disabled submit button", () => {
    // given
    render(<ClaimForm token="tok-abc-123" amountInr="1234.56" />);

    // then
    expect(screen.getByLabelText("UPI ID")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("yourname@upi")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Claim/ })).toBeDisabled();
  });

  it("should display formatted INR amount in helper text and button", () => {
    // given
    render(<ClaimForm token="tok-abc-123" amountInr="1234.56" />);

    // then
    expect(screen.getByText(/you'd like to receive/)).toHaveTextContent("1,234.56");
    expect(screen.getByRole("button", { name: /Claim/ })).toHaveTextContent("1,234.56");
  });

  it("should enable submit button when valid UPI is entered", async () => {
    // given
    const user = userEvent.setup();
    render(<ClaimForm token="tok-abc-123" amountInr="100.00" />);

    // when
    await user.type(screen.getByLabelText("UPI ID"), "user@upi");

    // then
    expect(screen.getByRole("button", { name: /Claim/ })).toBeEnabled();
  });

  it("should keep submit disabled for invalid UPI without @", async () => {
    // given
    const user = userEvent.setup();
    render(<ClaimForm token="tok-abc-123" amountInr="100.00" />);

    // when
    await user.type(screen.getByLabelText("UPI ID"), "invalidupi");

    // then
    expect(screen.getByRole("button", { name: /Claim/ })).toBeDisabled();
  });

  it("should keep submit disabled for UPI shorter than 5 chars", async () => {
    // given
    const user = userEvent.setup();
    render(<ClaimForm token="tok-abc-123" amountInr="100.00" />);

    // when
    await user.type(screen.getByLabelText("UPI ID"), "a@b");

    // then
    expect(screen.getByRole("button", { name: /Claim/ })).toBeDisabled();
  });

  it("should show success state after successful claim submission", async () => {
    // given
    server.use(
      http.post("/api/claims/tok-abc-123", () =>
        HttpResponse.json({
          remittanceId: "rem-123",
          senderDisplayName: "Alice",
          amountUsdc: "1.00",
          amountInr: "94.26",
          fxRate: "94.26",
          status: "CLAIMED",
          claimed: true,
          expiresAt: "2026-04-27T12:00:00Z",
        }),
      ),
    );
    const user = userEvent.setup();
    render(<ClaimForm token="tok-abc-123" amountInr="94.26" />);

    // when
    await user.type(screen.getByLabelText("UPI ID"), "alice@upi");
    await user.click(screen.getByRole("button", { name: /Claim/ }));

    // then
    await waitFor(() => {
      expect(screen.getByText("Claim submitted")).toBeInTheDocument();
    });
    expect(screen.getByText("alice@upi")).toBeInTheDocument();
    expect(screen.getByText("Escrow released")).toBeInTheDocument();
    expect(screen.getByText("Confirmed")).toBeInTheDocument();
    expect(screen.getByText("Processing...")).toBeInTheDocument();
    expect(screen.getByText("Under 60s")).toBeInTheDocument();
  });

  it("should display error message on failed claim", async () => {
    // given
    server.use(
      http.post("/api/claims/tok-abc-123", () =>
        HttpResponse.json({ message: "Claim already submitted" }, { status: 409 }),
      ),
    );
    const user = userEvent.setup();
    render(<ClaimForm token="tok-abc-123" amountInr="94.26" />);

    // when
    await user.type(screen.getByLabelText("UPI ID"), "alice@upi");
    await user.click(screen.getByRole("button", { name: /Claim/ }));

    // then
    await waitFor(() => {
      expect(screen.getByText("Claim already submitted")).toBeInTheDocument();
    });
  });

  it("should display network error on fetch failure", async () => {
    // given
    server.use(http.post("/api/claims/tok-abc-123", () => HttpResponse.error()));
    const user = userEvent.setup();
    render(<ClaimForm token="tok-abc-123" amountInr="94.26" />);

    // when
    await user.type(screen.getByLabelText("UPI ID"), "alice@upi");
    await user.click(screen.getByRole("button", { name: /Claim/ }));

    // then
    await waitFor(() => {
      expect(screen.getByText("Network error. Please try again.")).toBeInTheDocument();
    });
  });

  it("should show Claiming... text while request is pending", async () => {
    // given
    server.use(
      http.post("/api/claims/tok-abc-123", async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
        return HttpResponse.json({
          remittanceId: "rem-123",
          senderDisplayName: "Alice",
          amountUsdc: "1.00",
          amountInr: "94.26",
          fxRate: "94.26",
          status: "CLAIMED",
          claimed: true,
          expiresAt: "2026-04-27T12:00:00Z",
        });
      }),
    );
    const user = userEvent.setup();
    render(<ClaimForm token="tok-abc-123" amountInr="94.26" />);

    // when
    await user.type(screen.getByLabelText("UPI ID"), "alice@upi");
    await user.click(screen.getByRole("button", { name: /Claim/ }));

    // then
    expect(screen.getByText("Claiming...")).toBeInTheDocument();
  });

  it("should display trust badge about Solana escrow", () => {
    // given
    render(<ClaimForm token="tok-abc-123" amountInr="100.00" />);

    // then
    expect(screen.getByText(/Funds are held in a Solana escrow/)).toBeInTheDocument();
  });
});
