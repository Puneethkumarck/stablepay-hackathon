import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { BottomNav } from "../bottom-nav";

vi.mock("next/navigation", () => ({
  usePathname: vi.fn().mockReturnValue("/home"),
}));

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

const { usePathname } = await import("next/navigation");

describe("BottomNav", () => {
  it("should render all four tabs", () => {
    // given
    render(<BottomNav />);

    // then
    expect(screen.getByText("Home")).toBeInTheDocument();
    expect(screen.getByText("Send")).toBeInTheDocument();
    expect(screen.getByText("Activity")).toBeInTheDocument();
    expect(screen.getByText("Me")).toBeInTheDocument();
  });

  it("should mark Home tab as active when on /home", () => {
    // given
    vi.mocked(usePathname).mockReturnValue("/home");
    render(<BottomNav />);

    // then
    const homeLink = screen.getByText("Home").closest("a");
    expect(homeLink).toHaveAttribute("aria-current", "page");
  });

  it("should mark Activity tab as active when on /activity", () => {
    // given
    vi.mocked(usePathname).mockReturnValue("/activity");
    render(<BottomNav />);

    // then
    const activityLink = screen.getByText("Activity").closest("a");
    expect(activityLink).toHaveAttribute("aria-current", "page");
  });

  it("should not mark Home as active when on /send", () => {
    // given
    vi.mocked(usePathname).mockReturnValue("/send");
    render(<BottomNav />);

    // then
    const homeLink = screen.getByText("Home").closest("a");
    expect(homeLink).not.toHaveAttribute("aria-current");
  });

  it("should mark Send tab as active for nested send routes", () => {
    // given
    vi.mocked(usePathname).mockReturnValue("/send/recipient");
    render(<BottomNav />);

    // then
    const sendLink = screen.getByText("Send").closest("a");
    expect(sendLink).toHaveAttribute("aria-current", "page");
  });

  it("should render navigation landmark", () => {
    // given
    render(<BottomNav />);

    // then
    expect(screen.getByRole("navigation", { name: "Main navigation" })).toBeInTheDocument();
  });

  it("should link to correct paths", () => {
    // given
    render(<BottomNav />);

    // then
    expect(screen.getByText("Home").closest("a")).toHaveAttribute("href", "/home");
    expect(screen.getByText("Send").closest("a")).toHaveAttribute("href", "/send");
    expect(screen.getByText("Activity").closest("a")).toHaveAttribute("href", "/activity");
    expect(screen.getByText("Me").closest("a")).toHaveAttribute("href", "/me");
  });
});
