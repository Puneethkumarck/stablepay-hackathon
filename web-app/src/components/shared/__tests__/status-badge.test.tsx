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
    const { container } = render(<StatusBadge status="ESCROWED" />);

    // then
    expect(container.querySelector(".animate-ping")).toBeInTheDocument();
  });

  it("should show pulsing dot for CLAIMED status", () => {
    // given
    const { container } = render(<StatusBadge status="CLAIMED" />);

    // then
    expect(container.querySelector(".animate-ping")).toBeInTheDocument();
  });

  it("should not show pulsing dot for DELIVERED status", () => {
    // given
    const { container } = render(<StatusBadge status="DELIVERED" />);

    // then
    expect(container.querySelector(".animate-ping")).not.toBeInTheDocument();
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

  it("should display failure statuses with danger styling", () => {
    // given
    const failureStatuses: RemittanceStatus[] = [
      "DISBURSEMENT_FAILED",
      "DEPOSIT_FAILED",
      "CLAIM_FAILED",
      "REFUND_FAILED",
    ];

    // then
    for (const status of failureStatuses) {
      const { container, unmount } = render(<StatusBadge status={status} />);
      const badge = container.firstElementChild;
      expect(badge?.className).toContain("text-danger");
      unmount();
    }
  });
});
