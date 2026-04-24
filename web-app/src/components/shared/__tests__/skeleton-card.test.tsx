import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SkeletonCard } from "@/components/shared/skeleton-card";

describe("SkeletonCard", () => {
  it("should render skeleton elements with pulse animation", () => {
    // given
    render(<SkeletonCard />);

    // when
    const skeletons = screen.getAllByRole("generic").filter((el) => el.dataset.slot === "skeleton");

    // then
    expect(skeletons.length).toBe(3);
  });

  it("should apply custom className", () => {
    // given
    const { container } = render(<SkeletonCard className="mt-4" />);

    // when
    const wrapper = container.firstChild as HTMLElement;

    // then
    expect(wrapper.className).toContain("mt-4");
  });
});
