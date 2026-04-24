import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { WalletResponse } from "@/types/api";
import { BalanceCard } from "../balance-card";

const wallet: WalletResponse = {
  id: 1,
  solanaAddress: "CrsMdGHJ1234567890DAd18",
  availableBalance: "248.50",
  totalBalance: "248.50",
  createdAt: "2026-04-20T10:00:00Z",
  updatedAt: "2026-04-24T10:00:00Z",
};

describe("BalanceCard", () => {
  it("should display balance amount", () => {
    // given
    render(<BalanceCard wallet={wallet} />);

    // then
    expect(screen.getByText("$")).toBeInTheDocument();
    expect(screen.getByText("248.50")).toBeInTheDocument();
  });

  it("should display truncated wallet address", () => {
    // given
    render(<BalanceCard wallet={wallet} />);

    // then
    expect(screen.getByText(/CrsMd…DAd18/)).toBeInTheDocument();
  });

  it("should display USDC balance label", () => {
    // given
    render(<BalanceCard wallet={wallet} />);

    // then
    expect(screen.getByText("USDC balance · Solana")).toBeInTheDocument();
  });

  it("should display available to send text", () => {
    // given
    render(<BalanceCard wallet={wallet} />);

    // then
    expect(screen.getByText(/Available to send/)).toBeInTheDocument();
  });

  it("should handle zero balance", () => {
    // given
    const emptyWallet: WalletResponse = {
      ...wallet,
      availableBalance: "0",
      totalBalance: "0",
    };
    render(<BalanceCard wallet={emptyWallet} />);

    // then
    expect(screen.getByText("$")).toBeInTheDocument();
    expect(screen.getByText("0.00")).toBeInTheDocument();
  });
});
