import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SkeletonRow } from "@/components/shared/skeleton-row";

describe("SkeletonRow", () => {
  it("should render skeleton elements with pulse animation", () => {
    // given
    render(<SkeletonRow />);

    // when
    const skeletons = screen.getAllByRole("generic").filter((el) => el.dataset.slot === "skeleton");

    // then
    expect(skeletons.length).toBe(4);
  });

  it("should apply custom className", () => {
    // given
    const { container } = render(<SkeletonRow className="border-b" />);

    // when
    const wrapper = container.firstChild as HTMLElement;

    // then
    expect(wrapper.className).toContain("border-b");
  });
});
