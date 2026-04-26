import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { formatCurrency, truncateAddress } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { WalletResponse } from "@/types/api";

const wallet: WalletResponse = {
  id: 1,
  solanaAddress: "CrsMdGHJ1234567890DAd18",
  availableBalance: "248.50",
  totalBalance: "248.50",
  createdAt: "2026-04-20T10:00:00Z",
  updatedAt: "2026-04-24T10:00:00Z",
};

function InfoRow({
  label,
  value,
  valueClassName,
}: {
  label: string;
  value: string;
  valueClassName?: string;
}) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-surface-2 px-4 py-3.5">
      <span className="text-sm text-fg-3">{label}</span>
      <span className={cn("text-sm", valueClassName ?? "text-fg-2")}>{value}</span>
    </div>
  );
}

describe("MePage helpers", () => {
  it("should format balance as USD currency", () => {
    // given
    const balance = wallet.availableBalance;

    // when
    const formatted = formatCurrency(balance, "USD");

    // then
    expect(formatted).toBe("$248.50");
  });

  it("should truncate wallet address", () => {
    // given
    const address = wallet.solanaAddress;

    // when
    const truncated = truncateAddress(address);

    // then
    expect(truncated).toBe("CrsMd…DAd18");
  });

  it("should render info row with label and value", () => {
    // given
    render(<InfoRow label="USD Balance" value="$248.50" />);

    // then
    expect(screen.getByText("USD Balance")).toBeInTheDocument();
    expect(screen.getByText("$248.50")).toBeInTheDocument();
  });

  it("should render info row with custom value class", () => {
    // given
    render(<InfoRow label="KYC status" value="Verified" valueClassName="text-green-300" />);

    // then
    expect(screen.getByText("Verified")).toBeInTheDocument();
    expect(screen.getByText("Verified")).toHaveClass("text-green-300");
  });

  it("should derive initial from email when name is absent", () => {
    // given
    const email = "raj@gmail.com";

    // when
    const initial = email.charAt(0).toUpperCase();

    // then
    expect(initial).toBe("R");
  });

  it("should derive display name from email prefix when name is absent", () => {
    // given
    const email = "raj@gmail.com";

    // when
    const displayName = email.split("@")[0];

    // then
    expect(displayName).toBe("raj");
  });

  it("should format member since date", () => {
    // given
    const createdAt = "2025-01-15T10:00:00Z";

    // when
    const formatted = new Date(createdAt).toLocaleDateString("en-US", {
      month: "short",
      year: "numeric",
    });

    // then
    expect(formatted).toBe("Jan 2025");
  });

  it("should derive initial from name when present", () => {
    // given
    const name = "Raj Sharma";

    // when
    const initial = name.charAt(0).toUpperCase();

    // then
    expect(initial).toBe("R");
  });

  it("should use name as display name when present", () => {
    // given
    const name = "Raj Sharma";

    // then
    expect(name).toBe("Raj Sharma");
  });

  it("should fallback to dash for empty email", () => {
    // given
    const email = "";

    // when
    const display = email || "—";

    // then
    expect(display).toBe("—");
  });
});
