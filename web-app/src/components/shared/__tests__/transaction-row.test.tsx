import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { RemittanceResponse } from "@/types/api";
import { TransactionRow } from "../transaction-row";

vi.mock("next/link", () => ({
  default: ({
    children,
    href,
    ...props
  }: {
    children: React.ReactNode;
    href: string;
    [key: string]: unknown;
  }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

const baseRemittance: RemittanceResponse = {
  id: 1,
  remittanceId: "8ce3f4a1-1234-5678-9abc-dc2a12345678",
  recipientPhone: "+919876543210",
  recipientName: "Raj Patel",
  amountUsdc: "100.00",
  amountInr: "8450.00",
  fxRate: "84.50",
  status: "ESCROWED",
  escrowPda: "7C2zABC123DEF456",
  claimTokenId: "claim-token-1",
  smsNotificationFailed: false,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  expiresAt: new Date(Date.now() + 48 * 60 * 60 * 1000).toISOString(),
};

describe("TransactionRow", () => {
  it("should display recipient name and initials", () => {
    // given
    render(<TransactionRow remittance={baseRemittance} />);

    // then
    expect(screen.getByText("Raj Patel")).toBeInTheDocument();
    expect(screen.getByText("RP")).toBeInTheDocument();
  });

  it("should display amount with dollar sign", () => {
    // given
    render(<TransactionRow remittance={baseRemittance} />);

    // then
    expect(screen.getByText("-$100.00")).toBeInTheDocument();
  });

  it("should display status badge", () => {
    // given
    render(<TransactionRow remittance={baseRemittance} />);

    // then
    expect(screen.getByText("Escrowed")).toBeInTheDocument();
  });

  it("should link to detail page using remittanceId UUID", () => {
    // given
    render(<TransactionRow remittance={baseRemittance} />);

    // then
    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("href", "/detail/8ce3f4a1-1234-5678-9abc-dc2a12345678");
  });

  it("should show phone number as primary when name is null", () => {
    // given
    const remittanceNoName: RemittanceResponse = {
      ...baseRemittance,
      recipientName: null,
    };
    render(<TransactionRow remittance={remittanceNoName} />);

    // then
    expect(screen.getByText("+919876543210")).toBeInTheDocument();
    expect(screen.queryByText("Raj Patel")).not.toBeInTheDocument();
  });

  it("should show phone number in subtitle when name is present", () => {
    // given
    render(<TransactionRow remittance={baseRemittance} />);

    // then
    const subtitle = screen.getByText(/\+919876543210/);
    expect(subtitle).toBeInTheDocument();
  });

  it("should display relative time", () => {
    // given
    render(<TransactionRow remittance={baseRemittance} />);

    // then
    expect(screen.getByText(/ago|just now/)).toBeInTheDocument();
  });
});
