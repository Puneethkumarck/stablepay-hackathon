import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { RemittanceResponse } from "@/types/api";
import { RecentTransactions } from "../recent-transactions";

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

const remittances: RemittanceResponse[] = [
  {
    id: 1,
    remittanceId: "rem-uuid-001",
    recipientPhone: "+919876543210",
    recipientName: "Raj Patel",
    amountUsdc: "100.00",
    amountInr: "8450.00",
    fxRate: "84.50",
    status: "ESCROWED",
    escrowPda: "7C2zABC123DEF456",
    claimTokenId: "claim-1",
    smsNotificationFailed: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    expiresAt: new Date(Date.now() + 48 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 2,
    remittanceId: "rem-uuid-002",
    recipientPhone: "+919988766554",
    recipientName: "Meera Iyer",
    amountUsdc: "250.00",
    amountInr: "21125.00",
    fxRate: "84.50",
    status: "DELIVERED",
    escrowPda: "5KWqXYZ789",
    claimTokenId: "claim-2",
    smsNotificationFailed: false,
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
    expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  },
];

describe("RecentTransactions", () => {
  it("should render transaction rows when remittances exist", () => {
    // given
    render(<RecentTransactions remittances={remittances} />);

    // then
    expect(screen.getByText("Raj Patel")).toBeInTheDocument();
    expect(screen.getByText("Meera Iyer")).toBeInTheDocument();
  });

  it("should render Recent header and See all link", () => {
    // given
    render(<RecentTransactions remittances={remittances} />);

    // then
    expect(screen.getByText("Recent")).toBeInTheDocument();
    const seeAllLink = screen.getByRole("link", { name: /see all/i });
    expect(seeAllLink).toHaveAttribute("href", "/activity");
  });

  it("should render empty state when no remittances", () => {
    // given
    render(<RecentTransactions remittances={[]} />);

    // then
    expect(screen.getByText("No transfers yet.")).toBeInTheDocument();
    expect(screen.getByText("Send your first remittance!")).toBeInTheDocument();
  });

  it("should not render See all link when empty", () => {
    // given
    render(<RecentTransactions remittances={[]} />);

    // then
    expect(screen.queryByRole("link", { name: /see all/i })).not.toBeInTheDocument();
  });

  it("should link each transaction to detail page using remittanceId", () => {
    // given
    render(<RecentTransactions remittances={remittances} />);

    // then
    const links = screen
      .getAllByRole("link")
      .filter((l) => l.getAttribute("href")?.startsWith("/detail/"));
    expect(links[0]).toHaveAttribute("href", "/detail/rem-uuid-001");
    expect(links[1]).toHaveAttribute("href", "/detail/rem-uuid-002");
  });
});
