import type { ErrorResponse } from "@/types/api";

const BACKEND_URL = process.env.STABLEPAY_BACKEND_URL ?? "http://localhost:8080";

interface RequestOptions {
  token?: string;
  body?: unknown;
  cache?: RequestCache;
  revalidate?: number;
}

export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string;

  constructor(status: number, errorCode: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
  }
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  const fetchOptions: RequestInit & { next?: { revalidate?: number } } = {
    method,
    headers,
    ...(options.body != null && { body: JSON.stringify(options.body) }),
    ...(options.cache != null && { cache: options.cache }),
    ...(options.revalidate !== undefined && { next: { revalidate: options.revalidate } }),
  };

  const response = await fetch(`${BACKEND_URL}${path}`, fetchOptions);

  if (!response.ok) {
    const error: ErrorResponse = await response.json().catch(() => ({
      errorCode: "SP-9999",
      message: "Request failed",
      timestamp: new Date().toISOString(),
      path,
    }));
    throw new ApiError(response.status, error.errorCode, error.message);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>("GET", path, options),
  post: <T>(path: string, options?: RequestOptions) => request<T>("POST", path, options),
  put: <T>(path: string, options?: RequestOptions) => request<T>("PUT", path, options),
  delete: <T>(path: string, options?: RequestOptions) => request<T>("DELETE", path, options),
};
