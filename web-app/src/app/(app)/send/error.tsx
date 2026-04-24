"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function SendError({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <div className="flex min-h-screen items-center justify-center px-6">
      <div className="text-center space-y-4">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-danger/10">
          <span className="text-2xl">!</span>
        </div>
        <h1 className="text-xl font-semibold text-fg-1">Send failed</h1>
        <p className="text-sm text-fg-3">
          {error.message || "Something went wrong while preparing your transfer."}
        </p>
        <div className="flex flex-col gap-2">
          <Button onClick={reset}>Retry</Button>
          <Link
            href="/send"
            className="inline-flex items-center justify-center rounded-lg px-2.5 h-8 text-sm font-medium text-fg-2 hover:bg-muted hover:text-fg-1 transition-colors"
          >
            Start over
          </Link>
        </div>
      </div>
    </div>
  );
}
