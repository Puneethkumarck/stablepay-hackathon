"use client";

export default function GlobalError({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <html lang="en">
      <body className="flex min-h-screen items-center justify-center bg-[#070B1A] text-white font-sans">
        <div className="text-center space-y-4 px-6">
          <h1 className="text-2xl font-semibold">Something went wrong</h1>
          <p className="text-white/60 text-sm">
            {error.message || "An unexpected error occurred."}
          </p>
          <button
            type="button"
            onClick={reset}
            className="rounded-xl bg-[#9945FF] px-6 py-3 text-sm font-medium text-white"
          >
            Try again
          </button>
        </div>
      </body>
    </html>
  );
}
