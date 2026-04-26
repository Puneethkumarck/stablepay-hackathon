import { Suspense } from "react";
import { GoogleLoginButton } from "@/features/auth/components/google-login-button";
import { LiveFeed } from "@/features/auth/components/live-feed";
import { LoginErrorToast } from "@/features/auth/components/login-error-toast";

const APP_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:3000";

export default function LoginPage() {
  const callbackUrl = `${APP_URL}/auth/callback`;

  return (
    <div className="relative flex min-h-screen flex-col justify-end pb-7">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-1/4 left-1/2 h-[500px] w-[500px] -translate-x-1/2 rounded-full bg-solana-purple/20 blur-[120px]" />
        <div className="absolute -bottom-1/4 left-1/4 h-[400px] w-[400px] rounded-full bg-solana-teal/10 blur-[100px]" />
      </div>

      <div className="relative z-10 flex flex-1 flex-col justify-start px-7 pt-12">
        <div className="mb-6 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-solana-teal via-solana-purple to-solana-magenta">
          <span className="text-2xl font-bold text-white">S</span>
        </div>

        <div className="mb-3 font-mono text-[11px] uppercase tracking-[0.14em] text-accent">
          stablepay
        </div>

        <h1 className="mb-3 text-[34px] font-bold leading-[1.1] tracking-[-0.03em] text-fg-1">
          Send money home.
          <br />
          <span className="bg-gradient-to-r from-solana-teal via-solana-purple to-solana-magenta bg-clip-text text-transparent">
            Under a minute.
          </span>
        </h1>

        <p className="mb-6 text-sm leading-relaxed text-fg-2">
          Real-time remittances on Solana.
          <br />
          No app needed for your family.
        </p>

        <LiveFeed />
      </div>

      <div className="relative z-10 flex flex-col gap-2.5 px-6">
        <GoogleLoginButton callbackUrl={callbackUrl} />
        <p className="text-center font-mono text-[11px] text-fg-3">
          By continuing you agree to StablePay&apos;s Terms &amp; Privacy
        </p>
      </div>

      <Suspense>
        <LoginErrorToast />
      </Suspense>
    </div>
  );
}
