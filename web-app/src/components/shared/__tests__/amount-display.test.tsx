import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AmountDisplay } from "../amount-display";

describe("AmountDisplay", () => {
  it("should render USD amount with dollar sign", () => {
    // given
    render(<AmountDisplay amount="248.50" currency="USD" />);

    // then
    expect(screen.getByText("$")).toBeInTheDocument();
    expect(screen.getByText("248.50")).toBeInTheDocument();
  });

  it("should render INR amount with rupee sign and Indian grouping", () => {
    // given
    render(<AmountDisplay amount="8450" currency="INR" />);

    // then
    expect(screen.getByText("₹")).toBeInTheDocument();
    expect(screen.getByText("8,450.00")).toBeInTheDocument();
  });

  it("should render with mono font", () => {
    // given
    const { container } = render(<AmountDisplay amount="100" currency="USD" />);

    // then
    expect(container.firstElementChild?.className).toContain("font-mono");
  });

  it("should render xl size with larger text", () => {
    // given
    const { container } = render(<AmountDisplay amount="100" currency="USD" size="xl" />);

    // then
    expect(container.firstElementChild?.className).toContain("text-[56px]");
  });

  it("should render lg size by default", () => {
    // given
    const { container } = render(<AmountDisplay amount="100" currency="USD" />);

    // then
    expect(container.firstElementChild?.className).toContain("text-[40px]");
  });

  it("should handle zero amount", () => {
    // given
    render(<AmountDisplay amount="0" currency="USD" />);

    // then
    expect(screen.getByText("$")).toBeInTheDocument();
    expect(screen.getByText("0.00")).toBeInTheDocument();
  });
});
