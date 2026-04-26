import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { LiveFeed } from "../live-feed";

describe("LiveFeed", () => {
  it("should render the live transfers header with green dot", () => {
    // given
    // when
    render(<LiveFeed />);

    // then
    expect(screen.getByText("Live transfers")).toBeInTheDocument();
  });

  it("should render all five feed items", () => {
    // given
    // when
    render(<LiveFeed />);

    // then
    expect(screen.getByText("$200")).toBeInTheDocument();
    expect(screen.getByText("$75")).toBeInTheDocument();
    expect(screen.getByText("$500")).toBeInTheDocument();
    expect(screen.getByText("$120")).toBeInTheDocument();
    expect(screen.getByText("$350")).toBeInTheDocument();
  });

  it("should render INR amounts for each transfer", () => {
    // given
    // when
    render(<LiveFeed />);

    // then
    expect(screen.getByText("₹16,900")).toBeInTheDocument();
    expect(screen.getByText("₹42,250")).toBeInTheDocument();
  });

  it("should render country flag emojis", () => {
    // given
    // when
    render(<LiveFeed />);

    // then
    expect(screen.getAllByText("🇺🇸")).toHaveLength(5);
    expect(screen.getAllByText("🇮🇳")).toHaveLength(5);
  });
});
