import Link from "next/link";

export default function NotFound() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-1 text-fg-1">
      <div className="text-center space-y-4 px-6">
        <h1 className="text-5xl font-bold sp-gradient-text">404</h1>
        <p className="text-fg-3 text-sm">This page doesn't exist.</p>
        <Link
          href="/home"
          className="inline-block rounded-xl bg-accent px-6 py-3 text-sm font-medium text-white"
        >
          Go home
        </Link>
      </div>
    </div>
  );
}
