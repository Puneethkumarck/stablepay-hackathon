import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { GoogleLoginButton } from "../google-login-button";

describe("GoogleLoginButton", () => {
  it("should render the continue with google text", () => {
    // given
    // when
    render(<GoogleLoginButton callbackUrl="http://localhost:3000/auth/callback" />);

    // then
    expect(screen.getByRole("button", { name: /continue with google/i })).toBeInTheDocument();
  });

  it("should redirect to google oauth on click", async () => {
    // given
    const user = userEvent.setup();
    let capturedHref = "";
    Object.defineProperty(window, "location", {
      value: {
        get href() {
          return capturedHref;
        },
        set href(url: string) {
          capturedHref = url;
        },
      },
      writable: true,
    });
    render(<GoogleLoginButton callbackUrl="http://localhost:3000/auth/callback" />);

    // when
    await user.click(screen.getByRole("button", { name: /continue with google/i }));

    // then
    expect(capturedHref).toContain("accounts.google.com/o/oauth2/v2/auth");
  });

  it("should include required oauth parameters in redirect url", async () => {
    // given
    const user = userEvent.setup();
    let capturedUrl = "";
    Object.defineProperty(window, "location", {
      value: {
        get href() {
          return capturedUrl;
        },
        set href(url: string) {
          capturedUrl = url;
        },
      },
      writable: true,
    });
    render(<GoogleLoginButton callbackUrl="http://localhost:3000/auth/callback" />);

    // when
    await user.click(screen.getByRole("button", { name: /continue with google/i }));

    // then
    expect(capturedUrl).toContain("response_type=code");
    expect(capturedUrl).toContain("scope=openid+email+profile");
    expect(capturedUrl).toContain("redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Fauth%2Fcallback");
  });
});
