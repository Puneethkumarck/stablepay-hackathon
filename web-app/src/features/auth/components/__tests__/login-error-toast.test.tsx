"use client";

import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useSearchParams: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn() },
}));

import { useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { LoginErrorToast } from "../login-error-toast";

describe("LoginErrorToast", () => {
  it("should render without error when no error param", () => {
    // given
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams() as never);

    // when
    const { container } = render(<LoginErrorToast />);

    // then
    expect(container.innerHTML).toBe("");
    expect(toast.error).not.toHaveBeenCalled();
  });

  it("should show toast with mapped message when error param is present", () => {
    // given
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams("error=auth_failed") as never);

    // when
    render(<LoginErrorToast />);

    // then
    expect(toast.error).toHaveBeenCalledWith("Sign-in failed. Please try again.");
  });

  it("should show fallback message for unknown error codes", () => {
    // given
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams("error=unknown_error") as never);

    // when
    render(<LoginErrorToast />);

    // then
    expect(toast.error).toHaveBeenCalledWith("Something went wrong. Please try again.");
  });
});
