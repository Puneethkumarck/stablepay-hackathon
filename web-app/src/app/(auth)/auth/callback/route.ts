import { type NextRequest, NextResponse } from "next/server";
import type { UserData } from "@/lib/auth";
import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  REFRESH_TOKEN_MAX_AGE,
  USER_DATA_COOKIE,
} from "@/lib/constants";

const BACKEND_URL = process.env.STABLEPAY_BACKEND_URL ?? "http://localhost:8080";
const PUBLIC_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:3000";
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID ?? "";
const GOOGLE_CLIENT_SECRET = process.env.GOOGLE_CLIENT_SECRET ?? "";

interface GoogleTokenResponse {
  id_token: string;
  access_token: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserData | null;
}

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code");
  const error = request.nextUrl.searchParams.get("error");

  if (error || !code) {
    const loginUrl = new URL("/login", PUBLIC_URL);
    loginUrl.searchParams.set("error", error ?? "missing_code");
    return NextResponse.redirect(loginUrl);
  }

  try {
    const redirectUri = new URL("/auth/callback", PUBLIC_URL).toString();
    const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        code,
        client_id: GOOGLE_CLIENT_ID,
        client_secret: GOOGLE_CLIENT_SECRET,
        redirect_uri: redirectUri,
        grant_type: "authorization_code",
      }),
    });

    if (!tokenRes.ok) {
      const loginUrl = new URL("/login", PUBLIC_URL);
      loginUrl.searchParams.set("error", "token_exchange_failed");
      return NextResponse.redirect(loginUrl);
    }

    const googleTokens = (await tokenRes.json()) as GoogleTokenResponse;

    const authRes = await fetch(`${BACKEND_URL}/api/auth/social`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ provider: "google", idToken: googleTokens.id_token }),
    });

    if (!authRes.ok) {
      const loginUrl = new URL("/login", PUBLIC_URL);
      loginUrl.searchParams.set("error", "auth_failed");
      return NextResponse.redirect(loginUrl);
    }

    const authData = (await authRes.json()) as AuthResponse;
    const secure = process.env.NODE_ENV === "production";

    const response = NextResponse.redirect(new URL("/home", PUBLIC_URL));

    response.cookies.set(ACCESS_TOKEN_COOKIE, authData.accessToken, {
      httpOnly: true,
      secure,
      sameSite: "lax",
      path: "/",
      maxAge: authData.expiresIn,
    });

    response.cookies.set(REFRESH_TOKEN_COOKIE, authData.refreshToken, {
      httpOnly: true,
      secure,
      sameSite: "lax",
      path: "/",
      maxAge: REFRESH_TOKEN_MAX_AGE,
    });

    if (authData.user) {
      response.cookies.set(USER_DATA_COOKIE, JSON.stringify(authData.user), {
        httpOnly: true,
        secure,
        sameSite: "lax",
        path: "/",
        maxAge: REFRESH_TOKEN_MAX_AGE,
      });
    }

    return response;
  } catch {
    const loginUrl = new URL("/login", PUBLIC_URL);
    loginUrl.searchParams.set("error", "unexpected");
    return NextResponse.redirect(loginUrl);
  }
}
