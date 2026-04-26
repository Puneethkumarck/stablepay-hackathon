import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { RemittanceResponse, RemittanceTimelineResponse } from "@/types/api";
import { DetailTimeline } from "../detail-timeline";

vi.mock("../claim-countdown", () => ({
  ClaimCountdown: ({ expiresAt }: { expiresAt: string }) => (
    <span data-testid="claim-countdown">{expiresAt}</span>
  ),
}));

const BASE_REMITTANCE: RemittanceResponse = {
  id: 1,
  remittanceId: "8ce3abcd-1234-5678-9012-dc2a3456bcde",
  recipientPhone: "+919876543210",
  amountUsdc: "100.00",
  amountInr: "8450.00",
  fxRate: "84.50",
  status: "ESCROWED",
  escrowPda: "7C2zAbCdEfGhIjKlMnOpQrStUvWxYzWij",
  claimTokenId: "claim-token-1",
  smsNotificationFailed: false,
  createdAt: "2026-04-24T10:00:00Z",
  updatedAt: "2026-04-24T10:02:00Z",
  expiresAt: "2026-04-26T10:00:00Z",
  recipientName: "Raj Patel",
};

const ESCROWED_TIMELINE: RemittanceTimelineResponse = {
  steps: [
    {
      step: "INITIATED",
      status: "COMPLETED",
      message: "Initiated",
      completedAt: "2026-04-24T10:00:00Z",
    },
    {
      step: "ESCROWED",
      status: "COMPLETED",
      message: "Escrowed",
      completedAt: "2026-04-24T10:00:08Z",
    },
    { step: "CLAIMED", status: "CURRENT", message: "Awaiting claim", completedAt: null },
    { step: "DELIVERED", status: "PENDING", message: "Delivery", completedAt: null },
  ],
  failed: false,
};

describe("DetailTimeline", () => {
  it("should render all five visual steps", () => {
    // given
    // when
    render(<DetailTimeline timeline={ESCROWED_TIMELINE} remittance={BASE_REMITTANCE} />);

    // then
    expect(screen.getByText("Initiated")).toBeInTheDocument();
    expect(screen.getByText("Escrowed on Solana")).toBeInTheDocument();
    expect(screen.getByText("Claim SMS delivered")).toBeInTheDocument();
    expect(screen.getByText("Awaiting recipient claim")).toBeInTheDocument();
    expect(screen.getByText("Delivery via UPI")).toBeInTheDocument();
  });

  it("should show SMS delivery failed when smsNotificationFailed is true", () => {
    // given
    const remittance = { ...BASE_REMITTANCE, smsNotificationFailed: true };

    // when
    render(<DetailTimeline timeline={ESCROWED_TIMELINE} remittance={remittance} />);

    // then
    expect(screen.getByText("SMS delivery failed")).toBeInTheDocument();
    expect(screen.queryByText("Claim SMS delivered")).not.toBeInTheDocument();
  });

  it("should show recipient phone in SMS step subtitle when delivered", () => {
    // given
    // when
    render(<DetailTimeline timeline={ESCROWED_TIMELINE} remittance={BASE_REMITTANCE} />);

    // then
    expect(screen.getByText("+919876543210")).toBeInTheDocument();
  });

  it("should show truncated escrow PDA in escrow step subtitle", () => {
    // given
    // when
    render(<DetailTimeline timeline={ESCROWED_TIMELINE} remittance={BASE_REMITTANCE} />);

    // then
    expect(screen.getByText(/7C2zA…YzWij/)).toBeInTheDocument();
  });

  it("should show claim countdown when claim step is live", () => {
    // given
    // when
    render(<DetailTimeline timeline={ESCROWED_TIMELINE} remittance={BASE_REMITTANCE} />);

    // then
    expect(screen.getByTestId("claim-countdown")).toBeInTheDocument();
  });

  it("should not show claim countdown when claim step is pending", () => {
    // given
    const timeline: RemittanceTimelineResponse = {
      steps: [
        {
          step: "INITIATED",
          status: "COMPLETED",
          message: "Initiated",
          completedAt: "2026-04-24T10:00:00Z",
        },
        { step: "ESCROWED", status: "CURRENT", message: "Escrowing", completedAt: null },
        { step: "CLAIMED", status: "PENDING", message: "Awaiting claim", completedAt: null },
        { step: "DELIVERED", status: "PENDING", message: "Delivery", completedAt: null },
      ],
      failed: false,
    };

    // when
    render(<DetailTimeline timeline={timeline} remittance={BASE_REMITTANCE} />);

    // then
    expect(screen.queryByTestId("claim-countdown")).not.toBeInTheDocument();
  });

  it("should render all steps as done for delivered remittance", () => {
    // given
    const timeline: RemittanceTimelineResponse = {
      steps: [
        {
          step: "INITIATED",
          status: "COMPLETED",
          message: "Initiated",
          completedAt: "2026-04-24T10:00:00Z",
        },
        {
          step: "ESCROWED",
          status: "COMPLETED",
          message: "Escrowed",
          completedAt: "2026-04-24T10:00:08Z",
        },
        {
          step: "CLAIMED",
          status: "COMPLETED",
          message: "Claimed",
          completedAt: "2026-04-24T11:00:00Z",
        },
        {
          step: "DELIVERED",
          status: "COMPLETED",
          message: "Delivered",
          completedAt: "2026-04-24T11:00:05Z",
        },
      ],
      failed: false,
    };
    const remittance = { ...BASE_REMITTANCE, status: "DELIVERED" as const };

    // when
    render(<DetailTimeline timeline={timeline} remittance={remittance} />);

    // then
    expect(screen.getByText("Initiated")).toBeInTheDocument();
    expect(screen.getByText("Delivery via UPI")).toBeInTheDocument();
    expect(screen.queryByTestId("claim-countdown")).not.toBeInTheDocument();
  });
});
