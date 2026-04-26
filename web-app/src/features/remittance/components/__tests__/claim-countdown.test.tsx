import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ClaimCountdown } from "../claim-countdown";

describe("ClaimCountdown", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date("2026-04-24T10:00:00Z"));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("should display remaining hours and minutes", () => {
    // given
    const expiresAt = "2026-04-26T10:00:00Z";

    // when
    render(<ClaimCountdown expiresAt={expiresAt} />);

    // then
    expect(screen.getByText("Expires in 48h 0m")).toBeInTheDocument();
  });

  it("should display Expired when past expiry", () => {
    // given
    const expiresAt = "2026-04-23T10:00:00Z";

    // when
    render(<ClaimCountdown expiresAt={expiresAt} />);

    // then
    expect(screen.getByText("Expired")).toBeInTheDocument();
  });

  it("should display partial hours and minutes", () => {
    // given
    const expiresAt = "2026-04-25T23:58:00Z";

    // when
    render(<ClaimCountdown expiresAt={expiresAt} />);

    // then
    expect(screen.getByText("Expires in 37h 58m")).toBeInTheDocument();
  });
});
