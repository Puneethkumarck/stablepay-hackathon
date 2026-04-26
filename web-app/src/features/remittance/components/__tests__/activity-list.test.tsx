import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { RemittanceResponse } from "@/types/api";
import { ActivityList } from "../activity-list";

const REMITTANCE_A: RemittanceResponse = {
  id: 1,
  remittanceId: "aaa-111",
  recipientPhone: "+919876543210",
  amountUsdc: "100.00",
  amountInr: "8450.00",
  fxRate: "84.50",
  status: "ESCROWED",
  escrowPda: "7C2zAbCdEfGh",
  claimTokenId: "tok-1",
  smsNotificationFailed: false,
  createdAt: "2026-04-24T10:00:00Z",
  updatedAt: "2026-04-24T10:02:00Z",
  expiresAt: "2026-04-26T10:00:00Z",
  recipientName: "Raj Patel",
};

const REMITTANCE_B: RemittanceResponse = {
  id: 2,
  remittanceId: "bbb-222",
  recipientPhone: "+919000000001",
  amountUsdc: "50.00",
  amountInr: "4225.00",
  fxRate: "84.50",
  status: "DELIVERED",
  escrowPda: "9X4yBcDeFgHi",
  claimTokenId: "tok-2",
  smsNotificationFailed: false,
  createdAt: "2026-04-23T08:00:00Z",
  updatedAt: "2026-04-23T09:00:00Z",
  expiresAt: "2026-04-25T08:00:00Z",
  recipientName: null,
};

describe("ActivityList", () => {
  it("should show empty state when no remittances exist", () => {
    // given
    // when
    render(<ActivityList remittances={[]} />);

    // then
    expect(screen.getByText("No transfers yet.")).toBeInTheDocument();
    expect(screen.getByText("Send your first remittance!")).toBeInTheDocument();
  });

  it("should not show section header in empty state", () => {
    // given
    // when
    render(<ActivityList remittances={[]} />);

    // then
    expect(screen.queryByText("All transfers")).not.toBeInTheDocument();
  });

  it("should show section header when remittances exist", () => {
    // given
    // when
    render(<ActivityList remittances={[REMITTANCE_A]} />);

    // then
    expect(screen.getByText("All transfers")).toBeInTheDocument();
  });

  it("should render a row for each remittance", () => {
    // given
    // when
    render(<ActivityList remittances={[REMITTANCE_A, REMITTANCE_B]} />);

    // then
    expect(screen.getByText("Raj Patel")).toBeInTheDocument();
    expect(screen.getByText("+919000000001")).toBeInTheDocument();
  });

  it("should link rows to detail pages using remittanceId", () => {
    // given
    // when
    render(<ActivityList remittances={[REMITTANCE_A]} />);

    // then
    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("href", "/detail/aaa-111");
  });
});
