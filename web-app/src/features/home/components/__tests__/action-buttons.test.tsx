import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ActionButtons } from "../action-buttons";

vi.mock("next/link", () => ({
  default: ({
    children,
    href,
    ...props
  }: {
    children: React.ReactNode;
    href: string;
    [key: string]: unknown;
  }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

describe("ActionButtons", () => {
  it("should render Send button linking to /send", () => {
    // given
    render(<ActionButtons />);

    // then
    const sendLink = screen.getByRole("link", { name: /send/i });
    expect(sendLink).toHaveAttribute("href", "/send");
  });

  it("should render Add funds button linking to /add-funds", () => {
    // given
    render(<ActionButtons />);

    // then
    const addFundsLink = screen.getByRole("link", { name: /add funds/i });
    expect(addFundsLink).toHaveAttribute("href", "/add-funds");
  });
});
