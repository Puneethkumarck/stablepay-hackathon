import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { server } from "@/test-utils/msw-server";
import { SignOutButton } from "../sign-out-button";

const pushMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}));

describe("SignOutButton", () => {
  beforeEach(() => {
    pushMock.mockClear();
  });

  it("should render sign out text", () => {
    // given
    render(<SignOutButton />);

    // then
    expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
  });

  it("should call logout API and redirect to login", async () => {
    // given
    const user = userEvent.setup();
    server.use(http.post("/api/auth/logout", () => HttpResponse.json({ redirectTo: "/login" })));
    render(<SignOutButton />);

    // when
    await user.click(screen.getByRole("button", { name: /sign out/i }));

    // then
    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith("/login");
    });
  });

  it("should show pending state while signing out", async () => {
    // given
    const user = userEvent.setup();
    server.use(
      http.post("/api/auth/logout", async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
        return HttpResponse.json({ redirectTo: "/login" });
      }),
    );
    render(<SignOutButton />);

    // when
    await user.click(screen.getByRole("button", { name: /sign out/i }));

    // then
    expect(screen.getByText("Signing out…")).toBeInTheDocument();
    expect(screen.getByRole("button")).toBeDisabled();
  });

  it("should redirect even if logout API fails", async () => {
    // given
    const user = userEvent.setup();
    server.use(http.post("/api/auth/logout", () => HttpResponse.error()));
    render(<SignOutButton />);

    // when
    await user.click(screen.getByRole("button", { name: /sign out/i }));

    // then
    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith("/login");
    });
  });
});
