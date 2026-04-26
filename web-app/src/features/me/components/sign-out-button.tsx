"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

export function SignOutButton() {
  const router = useRouter();
  const [isPending, setIsPending] = useState(false);

  async function handleSignOut() {
    setIsPending(true);

    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch {}

    router.push("/login");
  }

  return (
    <button
      type="button"
      onClick={handleSignOut}
      disabled={isPending}
      className="flex w-full items-center justify-between rounded-lg bg-surface-2 px-4 py-3.5 transition-colors hover:bg-surface-3 disabled:opacity-50"
    >
      <span className="text-sm text-red-500">{isPending ? "Signing out…" : "Sign out"}</span>
    </button>
  );
}
