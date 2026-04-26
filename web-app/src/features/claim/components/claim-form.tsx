"use client";

import { useState } from "react";
import type { ClaimResponse } from "@/types/api";

interface ClaimFormProps {
  token: string;
  amountInr: string;
  children?: React.ReactNode;
}

export function ClaimForm({ token, amountInr, children }: ClaimFormProps) {
  const [upiId, setUpiId] = useState("");
  const [isPending, setIsPending] = useState(false);
  const [result, setResult] = useState<ClaimResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const formattedInr = Number(amountInr).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  const isValidUpi = upiId.includes("@") && upiId.length >= 5;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isValidUpi || isPending) return;

    setIsPending(true);
    setError(null);

    try {
      const res = await fetch(`/api/claims/${token}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ upiId }),
      });

      const data = await res.json();

      if (!res.ok) {
        setError(data.message ?? "Something went wrong");
        return;
      }

      setResult(data as ClaimResponse);
    } catch {
      setError("Network error. Please try again.");
    } finally {
      setIsPending(false);
    }
  }

  if (result) {
    return <ClaimSuccess amountInr={formattedInr} upiId={upiId} />;
  }

  return (
    <>
      {children}
      <form onSubmit={handleSubmit}>
        <div className="mb-3.5">
          <label htmlFor="upi" className="mb-1.5 block text-[13px] font-medium text-fg-2">
            UPI ID
          </label>
          <input
            id="upi"
            type="text"
            placeholder="yourname@upi"
            value={upiId}
            onChange={(e) => setUpiId(e.target.value)}
            className="w-full rounded-xl border border-border-2 bg-surface-3 px-4 py-3.5 font-mono text-[17px] text-fg-1 outline-none placeholder:text-fg-4 focus:border-accent focus:ring-[3px] focus:ring-accent-soft"
          />
          <p className="mt-1.5 text-xs text-fg-3">
            Enter the UPI ID where you'd like to receive {"₹"}
            {formattedInr} — credited within a minute.
          </p>
        </div>

        {error && (
          <p className="mb-3 rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">{error}</p>
        )}

        <button
          type="submit"
          disabled={!isValidUpi || isPending}
          className="flex w-full items-center justify-center gap-2 rounded-[14px] bg-[image:var(--solana-gradient)] px-5 py-4 text-base font-semibold text-[#0B1020] shadow-[var(--glow-solana-soft)] disabled:opacity-50"
        >
          {isPending ? (
            "Claiming..."
          ) : (
            <>
              Claim {"₹"}
              {formattedInr}
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#0B1020"
                strokeWidth="2"
                strokeLinecap="round"
                aria-hidden="true"
              >
                <path d="M5 12h14M13 6l6 6l-6 6" />
              </svg>
            </>
          )}
        </button>

        <div className="mt-6 flex items-start gap-2.5 rounded-xl border border-border-1 bg-white/[0.04] p-3.5 text-xs leading-relaxed text-fg-2">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            className="mt-0.5 shrink-0 text-success"
            aria-hidden="true"
          >
            <path d="M12 3L4 6v6c0 5 4 8 8 9c4 -1 8 -4 8 -9V6z" />
            <path d="M9 12l2 2l4 -4" />
          </svg>
          Funds are held in a Solana escrow until you enter your UPI ID. No wallet, no app, no fees
          to you.
        </div>
      </form>
    </>
  );
}

function ClaimSuccess({ amountInr, upiId }: { amountInr: string; upiId: string }) {
  return (
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
      <p className="mb-4 text-[11px] uppercase tracking-[0.14em] text-fg-3">Claim submitted</p>
      <p className="mb-2 font-mono text-[56px] font-semibold leading-none tracking-tight">
        <span className="text-[28px] font-normal text-fg-3">{"₹"}</span>
        {amountInr}
      </p>
      <p className="text-[15px] text-fg-2">
        is on its way to <span className="font-semibold text-fg-1">{upiId}</span>
      </p>

      <div className="mx-auto mt-6 rounded-xl border border-border-1 bg-surface-3 px-4">
        <StatusRow
          label="Escrow released"
          value="Confirmed"
          valueClassName="text-success font-mono"
        />
        <StatusRow
          label="UPI disbursement"
          value="Processing..."
          valueClassName="text-fg-1 font-mono"
        />
        <StatusRow
          label="Delivery ETA"
          value="Under 60s"
          valueClassName="text-fg-1 font-mono"
          last
        />
      </div>

      <p className="mt-5 text-xs text-fg-3">
        You'll get an SMS from your bank when the money lands.
      </p>
    </div>
  );
}

function StatusRow({
  label,
  value,
  valueClassName,
  last,
}: {
  label: string;
  value: string;
  valueClassName?: string;
  last?: boolean;
}) {
  return (
    <div
      className={`flex items-center justify-between py-3 text-[13px] ${last ? "" : "border-b border-border-1"}`}
    >
      <span className="text-fg-3">{label}</span>
      <span className={valueClassName}>{value}</span>
    </div>
  );
}
