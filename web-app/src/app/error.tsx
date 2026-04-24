"use client";

import { Button } from "@/components/ui/button";

export default function RootError({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  return (
    <div className="flex min-h-screen items-center justify-center px-6">
      <div className="text-center space-y-4">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-danger/10">
          <span className="text-2xl">!</span>
        </div>
        <h1 className="text-xl font-semibold text-fg-1">Something went wrong</h1>
        <p className="text-sm text-fg-3">{error.message || "An unexpected error occurred."}</p>
        <Button onClick={() => unstable_retry()}>Try again</Button>
      </div>
    </div>
  );
}
