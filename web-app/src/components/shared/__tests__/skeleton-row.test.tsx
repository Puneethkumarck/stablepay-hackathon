import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SkeletonRow } from "@/components/shared/skeleton-row";

describe("SkeletonRow", () => {
  it("should render with status role and busy state", () => {
    // given
    render(<SkeletonRow />);

    // when
    const status = screen.getByRole("status");

    // then
    expect(status).toHaveAttribute("aria-busy", "true");
    expect(status).toHaveAttribute("aria-label", "Loading");
  });

  it("should apply custom className", () => {
    // given
    render(<SkeletonRow className="border-b" />);

    // when
    const status = screen.getByRole("status");

    // then
    expect(status.className).toContain("border-b");
  });
});
