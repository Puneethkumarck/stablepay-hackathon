"use client";

import Link from "next/link";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function AppError({
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
        <h1 className="text-xl font-semibold text-fg-1">Something went wrong</h1>
        <p className="text-sm text-fg-3">{error.message || "We couldn't load this page."}</p>
        <div className="flex flex-col gap-2">
          <Button onClick={() => unstable_retry()}>Try again</Button>
          <Button variant="ghost" render={<Link href="/home" />}>
            Go home
          </Button>
        </div>
      </div>
    </div>
  );
}
