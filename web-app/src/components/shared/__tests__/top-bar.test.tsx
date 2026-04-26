import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TopBar } from "../top-bar";

describe("TopBar", () => {
  it("should display the title", () => {
    // given
    render(<TopBar title="stablepay" />);

    // then
    expect(screen.getByText("stablepay")).toBeInTheDocument();
  });

  it("should show back arrow when onBack is provided", () => {
    // given
    render(<TopBar title="Send" onBack={vi.fn()} />);

    // then
    expect(screen.getByRole("button", { name: "Go back" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "User menu" })).not.toBeInTheDocument();
  });

  it("should show user icon when onBack is not provided", () => {
    // given
    render(<TopBar title="stablepay" />);

    // then
    expect(screen.getByRole("button", { name: "User menu" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Go back" })).not.toBeInTheDocument();
  });

  it("should call onBack when back button is clicked", async () => {
    // given
    const onBack = vi.fn();
    const user = userEvent.setup();
    render(<TopBar title="Send" onBack={onBack} />);

    // when
    await user.click(screen.getByRole("button", { name: "Go back" }));

    // then
    expect(onBack).toHaveBeenCalledOnce();
  });

  it("should render custom right content when provided", () => {
    // given
    render(<TopBar title="Send" right={<span>STEP 1 OF 3</span>} />);

    // then
    expect(screen.getByText("STEP 1 OF 3")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Notifications" })).not.toBeInTheDocument();
  });

  it("should show notifications button when no right content is provided", () => {
    // given
    render(<TopBar title="stablepay" />);

    // then
    expect(screen.getByRole("button", { name: "Notifications" })).toBeInTheDocument();
  });
});
