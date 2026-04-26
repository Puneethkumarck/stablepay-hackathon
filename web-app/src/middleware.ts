import { type NextRequest, NextResponse } from "next/server";
import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  REFRESH_TOKEN_MAX_AGE,
  USER_DATA_COOKIE,
} from "@/lib/constants";

const PUBLIC_PATHS = ["/login", "/auth/callback", "/claim"];
const BACKEND_URL = process.env.STABLEPAY_BACKEND_URL ?? "http://localhost:8080";

function isTokenExpired(token: string): boolean {
  try {
    const parts = token.split(".");
    const encodedPayload = parts[1];
    if (parts.length !== 3 || !encodedPayload) return true;
    const payload = JSON.parse(atob(encodedPayload));
    return typeof payload.exp === "number" && payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

async function refreshTokens(
  refreshToken: string,
  request: NextRequest,
): Promise<NextResponse | null> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) return null;

    const data = (await res.json()) as {
      accessToken: string;
      refreshToken: string;
      expiresIn: number;
    };
    const secure = process.env.NODE_ENV === "production";
    const response = NextResponse.redirect(request.nextUrl);

    response.cookies.set(ACCESS_TOKEN_COOKIE, data.accessToken, {
      httpOnly: true,
      secure,
      sameSite: "lax",
      path: "/",
      maxAge: data.expiresIn,
    });

    response.cookies.set(REFRESH_TOKEN_COOKIE, data.refreshToken, {
      httpOnly: true,
      secure,
      sameSite: "lax",
      path: "/",
      maxAge: REFRESH_TOKEN_MAX_AGE,
    });

    return response;
  } catch (error) {
    console.error("[middleware] token refresh failed:", error);
    return null;
  }
}

function redirectToLogin(request: NextRequest): NextResponse {
  const response = NextResponse.redirect(new URL("/login", request.url));
  response.cookies.delete(ACCESS_TOKEN_COOKIE);
  response.cookies.delete(REFRESH_TOKEN_COOKIE);
  response.cookies.delete(USER_DATA_COOKIE);
  return response;
}

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p));

  const accessToken = request.cookies.get(ACCESS_TOKEN_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_TOKEN_COOKIE)?.value;
  const hasValidAccessToken = accessToken != null && !isTokenExpired(accessToken);

  if (!isPublic && !hasValidAccessToken && refreshToken) {
    const refreshed = await refreshTokens(refreshToken, request);
    return refreshed ?? redirectToLogin(request);
  }

  if (!isPublic && !hasValidAccessToken && !refreshToken) {
    return redirectToLogin(request);
  }

  if (pathname === "/login" && (hasValidAccessToken || refreshToken)) {
    return NextResponse.redirect(new URL("/home", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
