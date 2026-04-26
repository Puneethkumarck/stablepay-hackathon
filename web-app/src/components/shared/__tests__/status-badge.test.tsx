import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { RemittanceStatus } from "@/types/api";
import { StatusBadge } from "../status-badge";

describe("StatusBadge", () => {
  it("should display the status label for DELIVERED", () => {
    // given
    render(<StatusBadge status="DELIVERED" />);

    // then
    expect(screen.getByText("Delivered")).toBeInTheDocument();
  });

  it("should display the status label for ESCROWED", () => {
    // given
    render(<StatusBadge status="ESCROWED" />);

    // then
    expect(screen.getByText("Escrowed")).toBeInTheDocument();
  });

  it("should show pulsing dot for ESCROWED status", () => {
    // given
    render(<StatusBadge status="ESCROWED" />);

    // then
    expect(screen.getByTestId("pulse-indicator")).toBeInTheDocument();
  });

  it("should show pulsing dot for CLAIMED status", () => {
    // given
    render(<StatusBadge status="CLAIMED" />);

    // then
    expect(screen.getByTestId("pulse-indicator")).toBeInTheDocument();
  });

  it("should not show pulsing dot for DELIVERED status", () => {
    // given
    render(<StatusBadge status="DELIVERED" />);

    // then
    expect(screen.queryByTestId("pulse-indicator")).not.toBeInTheDocument();
  });

  it("should render all 10 status variants without errors", () => {
    // given
    const statuses: RemittanceStatus[] = [
      "INITIATED",
      "ESCROWED",
      "CLAIMED",
      "DELIVERED",
      "DISBURSEMENT_FAILED",
      "DEPOSIT_FAILED",
      "CLAIM_FAILED",
      "REFUND_FAILED",
      "REFUNDED",
      "CANCELLED",
    ];

    // when
    for (const status of statuses) {
      const { unmount } = render(<StatusBadge status={status} />);
      unmount();
    }
  });

  it("should display correct labels for failure statuses", () => {
    // given
    const failureLabels: [RemittanceStatus, string][] = [
      ["DISBURSEMENT_FAILED", "Disbursement Failed"],
      ["DEPOSIT_FAILED", "Deposit Failed"],
      ["CLAIM_FAILED", "Claim Failed"],
      ["REFUND_FAILED", "Refund Failed"],
    ];

    // then
    for (const [status, label] of failureLabels) {
      const { unmount } = render(<StatusBadge status={status} />);
      expect(screen.getByText(label)).toBeInTheDocument();
      unmount();
    }
  });

  it("should not show pulsing dot for failure statuses", () => {
    // given
    const failureStatuses: RemittanceStatus[] = [
      "DISBURSEMENT_FAILED",
      "DEPOSIT_FAILED",
      "CLAIM_FAILED",
      "REFUND_FAILED",
    ];

    // then
    for (const status of failureStatuses) {
      const { unmount } = render(<StatusBadge status={status} />);
      expect(screen.queryByTestId("pulse-indicator")).not.toBeInTheDocument();
      unmount();
    }
  });
});
