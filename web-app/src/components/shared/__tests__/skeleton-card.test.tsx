import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SkeletonCard } from "@/components/shared/skeleton-card";

describe("SkeletonCard", () => {
  it("should render with status role and busy state", () => {
    // given
    render(<SkeletonCard />);

    // when
    const status = screen.getByRole("status");

    // then
    expect(status).toHaveAttribute("aria-busy", "true");
    expect(status).toHaveAttribute("aria-label", "Loading");
  });

  it("should apply custom className", () => {
    // given
    render(<SkeletonCard className="mt-4" />);

    // when
    const status = screen.getByRole("status");

    // then
    expect(status.className).toContain("mt-4");
  });
});
