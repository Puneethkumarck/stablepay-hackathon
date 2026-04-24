"use client";

import Link from "next/link";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function SendError({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

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
          <Button onClick={() => unstable_retry()}>Retry</Button>
          <Button variant="ghost" render={<Link href="/send" />}>
            Start over
          </Button>
        </div>
      </div>
    </div>
  );
}
