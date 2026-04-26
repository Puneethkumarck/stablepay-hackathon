import { cookies } from "next/headers";
import { redirect } from "next/navigation";

const BACKEND_URL = process.env.STABLEPAY_BACKEND_URL ?? "http://localhost:8080";
const ACCESS_TOKEN_COOKIE = "accessToken";
const REFRESH_TOKEN_COOKIE = "refreshToken";
const USER_DATA_COOKIE = "userData";

interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserData {
  id: string;
  email: string;
  name?: string | null;
  createdAt: string;
}

export async function getAccessToken(): Promise<string | undefined> {
  const cookieStore = await cookies();
  return cookieStore.get(ACCESS_TOKEN_COOKIE)?.value;
}

export async function requireAuth(): Promise<string> {
  const token = await getAccessToken();
  if (token) return token;

  const refreshed = await refreshAccessToken();
  if (refreshed) return refreshed;

  await clearAuthCookies();
  redirect("/login");
}

export async function refreshAccessToken(): Promise<string | null> {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(REFRESH_TOKEN_COOKIE)?.value;
  if (!refreshToken) return null;

  try {
    const res = await fetch(`${BACKEND_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) return null;

    const data = (await res.json()) as TokenPair;
    await setAuthCookies(data);
    return data.accessToken;
  } catch {
    return null;
  }
}

export async function setAuthCookies(tokens: TokenPair): Promise<void> {
  const cookieStore = await cookies();
  const secure = process.env.NODE_ENV === "production";

  cookieStore.set(ACCESS_TOKEN_COOKIE, tokens.accessToken, {
    httpOnly: true,
    secure,
    sameSite: "lax",
    path: "/",
    maxAge: tokens.expiresIn,
  });

  cookieStore.set(REFRESH_TOKEN_COOKIE, tokens.refreshToken, {
    httpOnly: true,
    secure,
    sameSite: "lax",
    path: "/",
    maxAge: 30 * 24 * 60 * 60,
  });
}

export async function clearAuthCookies(): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.delete(ACCESS_TOKEN_COOKIE);
  cookieStore.delete(REFRESH_TOKEN_COOKIE);
  cookieStore.delete(USER_DATA_COOKIE);
}

export async function setUserDataCookie(user: UserData): Promise<void> {
  const cookieStore = await cookies();
  const secure = process.env.NODE_ENV === "production";

  cookieStore.set(USER_DATA_COOKIE, JSON.stringify(user), {
    httpOnly: true,
    secure,
    sameSite: "strict",
    path: "/",
    maxAge: 30 * 24 * 60 * 60,
  });
}

export async function getUserData(): Promise<UserData | null> {
  const cookieStore = await cookies();
  const raw = cookieStore.get(USER_DATA_COOKIE)?.value;
  if (!raw) return null;

  try {
    return JSON.parse(raw) as UserData;
  } catch {
    return null;
  }
}
