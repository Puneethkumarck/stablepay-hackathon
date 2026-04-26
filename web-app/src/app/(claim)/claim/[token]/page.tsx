import { ClaimForm } from "@/features/claim/components/claim-form";
import { ApiError, apiClient } from "@/lib/api-client";
import type { ClaimResponse } from "@/types/api";

interface ClaimPageProps {
  params: Promise<{ token: string }>;
}

function formatInr(value: string): string {
  return Number(value).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatTimeRemaining(expiresAt: string): string {
  const diff = new Date(expiresAt).getTime() - Date.now();
  if (diff <= 0) return "Expired";
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  return `${hours}h ${minutes}m`;
}

export default async function ClaimPage({ params }: ClaimPageProps) {
  const { token } = await params;

  let claim: ClaimResponse;
  try {
    claim = await apiClient.get<ClaimResponse>(`/api/claims/${token}`, {
      cache: "no-store",
    });
  } catch (error) {
    if (error instanceof ApiError) {
      if (error.status === 404) {
        return (
          <ErrorState
            title="Link not found"
            message="This claim link is invalid or has already been used."
          />
        );
      }
      if (error.status === 410) {
        return (
          <ErrorState
            title="Link expired"
            message="This claim link has expired. The funds have been returned to the sender."
          />
        );
      }
    }
    return (
      <ErrorState
        title="Something went wrong"
        message="We couldn't load this claim. Please try the link from your SMS again."
      />
    );
  }

  if (claim.claimed) {
    return (
      <ClaimShell>
        <div className="text-center">
          <div className="mx-auto mb-5 grid h-[72px] w-[72px] place-items-center rounded-full border border-success-border bg-success-soft">
            <svg
              width="36"
              height="36"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="text-success"
              aria-hidden="true"
            >
              <path d="M5 12l5 5l9 -10" />
            </svg>
          </div>
          <p className="mb-2 text-[11px] uppercase tracking-[0.14em] text-fg-3">Already claimed</p>
          <p className="font-mono text-[56px] font-semibold leading-none tracking-tight">
            <span className="text-[28px] font-normal text-fg-3">{"₹"}</span>
            {formatInr(claim.amountInr)}
          </p>
          <p className="mt-3 text-sm text-fg-2">
            This payment has already been claimed and is being delivered.
          </p>
        </div>
      </ClaimShell>
    );
  }

  const timeRemaining = formatTimeRemaining(claim.expiresAt);

  return (
    <ClaimShell>
      <ClaimForm token={token} amountInr={claim.amountInr}>
        <div className="mb-2.5 text-center">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-warning-border bg-warning-soft px-2.5 py-1 font-mono text-xs text-[#FCD34D]">
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 2" />
            </svg>
            Expires in {timeRemaining}
          </span>
        </div>

        <p className="mb-4 text-center text-[11px] uppercase tracking-[0.14em] text-fg-3">
          You're receiving money
        </p>
        <p className="mb-2 text-center text-[15px] text-fg-2">
          <span className="font-semibold text-fg-1">{claim.senderDisplayName}</span> sent you
        </p>
        <p className="mb-2 text-center font-mono text-[64px] font-semibold leading-none tracking-tight">
          <span className="text-[32px] font-normal text-fg-3">{"₹"}</span>
          {formatInr(claim.amountInr)}
        </p>
        <p className="mb-7 text-center font-mono text-xs text-fg-3">
          {"≈ $"}
          {Number(claim.amountUsdc).toFixed(2)} USDC · locked at {Number(claim.fxRate).toFixed(2)}{" "}
          INR/USD
        </p>
      </ClaimForm>
    </ClaimShell>
  );
}

function ClaimShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative min-h-screen px-6 py-6">
      <div className="pointer-events-none fixed inset-0 z-0">
        <div className="absolute -left-40 -top-30 h-[500px] w-[500px] rounded-full bg-solana-purple/30 blur-[80px]" />
        <div className="absolute -bottom-20 -right-30 h-[420px] w-[420px] rounded-full bg-solana-teal/[0.18] blur-[80px]" />
      </div>

      <div className="relative z-10">
        <div className="mb-8 flex items-center justify-between">
          <p className="text-[22px] font-bold tracking-tight">
            stable
            <span className="bg-[image:var(--solana-gradient)] bg-clip-text text-transparent">
              pay
            </span>
          </p>
          <span className="rounded-full border border-border-1 bg-surface-2 px-3 py-1.5 font-mono text-[11px] font-semibold tracking-wider text-fg-2">
            CLAIM · USD → INR
          </span>
        </div>

        <div className="rounded-3xl border border-border-1 bg-surface-2 p-7 shadow-3">
          {children}
        </div>

        <p className="mt-5 text-center font-mono text-[11px] tracking-wider text-fg-3">
          powered by solana <span className="mx-1.5 text-fg-4">·</span> usdc{" "}
          <span className="mx-1.5 text-fg-4">·</span> razorpay upi
        </p>
      </div>
    </div>
  );
}

function ErrorState({ title, message }: { title: string; message: string }) {
  return (
    <ClaimShell>
      <div className="text-center">
        <div className="mx-auto mb-5 grid h-[72px] w-[72px] place-items-center rounded-full border border-border-1 bg-surface-3">
          <svg
            width="32"
            height="32"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            className="text-fg-3"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v4M12 16h.01" />
          </svg>
        </div>
        <h2 className="mb-2 text-xl font-semibold text-fg-1">{title}</h2>
        <p className="text-sm text-fg-2">{message}</p>
      </div>
    </ClaimShell>
  );
}
