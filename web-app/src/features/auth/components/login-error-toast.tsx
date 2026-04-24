"use client";

import { useSearchParams } from "next/navigation";
import { useEffect, useRef } from "react";
import { toast } from "sonner";

const ERROR_MESSAGES: Record<string, string> = {
  missing_code: "Google sign-in was cancelled.",
  token_exchange_failed: "Could not verify your Google account. Please try again.",
  auth_failed: "Sign-in failed. Please try again.",
  access_denied: "Access was denied. Please try again.",
  unexpected: "Something went wrong. Please try again.",
};

export function LoginErrorToast() {
  const searchParams = useSearchParams();
  const error = searchParams.get("error");
  const shownRef = useRef<string | null>(null);

  useEffect(() => {
    if (error && shownRef.current !== error) {
      shownRef.current = error;
      toast.error(ERROR_MESSAGES[error] ?? ERROR_MESSAGES.unexpected);
    }
  }, [error]);

  return null;
}
