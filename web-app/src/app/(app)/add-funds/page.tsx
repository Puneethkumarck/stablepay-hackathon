"use client";

import { Check, CreditCard } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { TopBar } from "@/components/shared/top-bar";
import { Button } from "@/components/ui/button";
import { FundWalletError, useFundWallet } from "@/features/wallet/hooks/use-fund-wallet";
import { usePollFundingOrder } from "@/features/wallet/hooks/use-poll-funding-order";
import { useWallet } from "@/features/wallet/hooks/use-wallet";
import { MAX_AMOUNT_USDC, MIN_AMOUNT_USDC } from "@/lib/constants";
import { getErrorMessage } from "@/lib/errors";
import { cn } from "@/lib/utils";
import type { FundingStatus } from "@/types/api";

type Step = "amount" | "processing" | "done" | "failed";

const PRESETS = ["25", "50", "100", "250"] as const;

const SUCCESS_STATUSES: ReadonlySet<FundingStatus> = new Set(["FUNDED"]);
const FAILURE_STATUSES: ReadonlySet<FundingStatus> = new Set([
  "FAILED",
  "REFUNDED",
  "REFUND_FAILED",
]);

export default function AddFundsPage() {
  const router = useRouter();
  const [amount, setAmount] = useState("50");
  const [step, setStep] = useState<Step>("amount");
  const [fundingId, setFundingId] = useState<string | null>(null);
  const { data: wallet, refetch: refetchWallet } = useWallet();
  const { mutate, isPending } = useFundWallet();

  const { data: fundingOrder } = usePollFundingOrder(step === "processing" ? fundingId : null);

  useEffect(() => {
    if (step !== "processing" || !fundingOrder) return;
    if (SUCCESS_STATUSES.has(fundingOrder.status)) {
      refetchWallet();
      setStep("done");
    } else if (FAILURE_STATUSES.has(fundingOrder.status)) {
      setStep("failed");
    }
  }, [step, fundingOrder, refetchWallet]);

  const handleAmountChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/[^\d.]/g, "");
    if (value === ".") return;
    const parts = value.split(".");
    if (parts.length > 2) return;
    if (parts[1] !== undefined && parts[1].length > 2) return;
    const sanitized = parts[0] !== undefined ? parts[0].replace(/^0+(\d)/, "$1") : value;
    setAmount(parts.length === 2 ? `${sanitized}.${parts[1]}` : sanitized);
  }, []);

  const parsedAmount = Number.parseFloat(amount || "0");

  const validationError = useMemo(() => {
    if (!amount || parsedAmount === 0) return null;
    if (parsedAmount < MIN_AMOUNT_USDC) return `Min $${MIN_AMOUNT_USDC.toFixed(2)}`;
    if (parsedAmount > MAX_AMOUNT_USDC)
      return `Max $${MAX_AMOUNT_USDC.toLocaleString("en-US", { minimumFractionDigits: 2 })}`;
    return null;
  }, [amount, parsedAmount]);

  const isValid = parsedAmount >= MIN_AMOUNT_USDC && parsedAmount <= MAX_AMOUNT_USDC;

  const handlePay = useCallback(() => {
    if (!wallet) return;

    mutate(
      { walletId: wallet.id, amount },
      {
        onSuccess: (data) => {
          setFundingId(data.fundingId);
          setStep("processing");
        },
        onError: (error) => {
          if (error instanceof FundWalletError) {
            toast.error(getErrorMessage(error.errorCode));
          } else {
            toast.error("Something went wrong. Please try again.");
          }
        },
      },
    );
  }, [wallet, amount, mutate]);

  const handleRetry = useCallback(() => {
    setStep("amount");
    setFundingId(null);
  }, []);

  if (step === "processing") {
    return (
      <div className="flex flex-col px-5 pt-2 pb-6">
        <TopBar title="Add Funds" onBack={() => router.push("/home")} right={<span />} />
        <div className="flex flex-1 flex-col items-center justify-center gap-5 pt-10">
          <div
            className={cn(
              "grid h-14 w-14 place-items-center rounded-full",
              "bg-[image:var(--solana-gradient)] shadow-[var(--glow-solana-soft)]",
              "animate-pulse",
            )}
          >
            <CreditCard size={24} className="text-surface-0" />
          </div>
          <div className="text-center">
            <div className="text-[15px] font-semibold text-fg-1">Processing payment…</div>
            <div className="mt-1.5 font-mono text-xs text-fg-3">
              Stripe · ${parsedAmount.toFixed(2)} USD
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (step === "done") {
    return (
      <div className="flex flex-col px-5 pt-2 pb-6">
        <TopBar title="Add Funds" onBack={() => router.push("/home")} right={<span />} />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 pt-10">
          <div
            className={cn(
              "grid h-[72px] w-[72px] place-items-center rounded-full",
              "border border-success/30 bg-success/10",
            )}
          >
            <Check size={32} className="text-success" />
          </div>
          <div className="text-center">
            <div className="font-mono text-[22px] font-bold text-fg-1">
              ${parsedAmount.toFixed(2)} USDC
            </div>
            <div className="mt-1.5 text-sm text-fg-2">Added to your wallet</div>
          </div>
          <Button size="lg" className="mt-4 w-4/5" onClick={() => router.push("/home")}>
            Done
          </Button>
        </div>
      </div>
    );
  }

  if (step === "failed") {
    return (
      <div className="flex flex-col px-5 pt-2 pb-6">
        <TopBar title="Add Funds" onBack={() => router.push("/home")} right={<span />} />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 pt-10">
          <div
            className={cn(
              "grid h-[72px] w-[72px] place-items-center rounded-full",
              "border border-danger/30 bg-danger/10",
            )}
          >
            <CreditCard size={32} className="text-danger" />
          </div>
          <div className="text-center">
            <div className="text-[15px] font-semibold text-fg-1">Payment failed</div>
            <div className="mt-1.5 text-sm text-fg-3">
              Your card was not charged. Please try again.
            </div>
          </div>
          <Button size="lg" className="mt-4 w-4/5" onClick={handleRetry}>
            Try again
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 px-5 pt-2 pb-6">
      <TopBar title="Add Funds" onBack={() => router.push("/home")} right={<span />} />

      <div className="flex flex-col items-center gap-3 pt-4">
        <span className="text-xs tracking-[0.14em] uppercase text-fg-3">Amount (USD)</span>
        <div className="flex items-baseline justify-center gap-1">
          <span className="font-mono text-[40px] text-fg-3">$</span>
          <input
            type="text"
            inputMode="decimal"
            value={amount}
            onChange={handleAmountChange}
            placeholder="0"
            aria-label="Amount in USD"
            className={cn(
              "bg-transparent font-mono text-[56px] leading-none text-fg-1",
              "outline-none placeholder:text-fg-3/40 tabular-nums",
            )}
            style={{ width: `${Math.max(1, amount.length || 1)}ch` }}
          />
        </div>
        <span className={cn("font-mono text-xs", validationError ? "text-danger" : "text-fg-3")}>
          Min ${MIN_AMOUNT_USDC.toFixed(2)} · Max $
          {MAX_AMOUNT_USDC.toLocaleString("en-US", { minimumFractionDigits: 2 })}
        </span>
      </div>

      <div className="grid grid-cols-4 gap-2">
        {PRESETS.map((preset) => (
          <button
            key={preset}
            type="button"
            onClick={() => setAmount(preset)}
            className={cn(
              "rounded-[10px] border px-1 py-2.5 font-mono text-sm font-semibold transition-colors",
              amount === preset
                ? "border-accent bg-accent/10 text-[#D6B4FF]"
                : "border-border-2 bg-surface-2 text-fg-2 hover:border-border-3",
            )}
          >
            ${preset}
          </button>
        ))}
      </div>

      <div className="flex flex-col rounded-2xl bg-surface-2 px-4">
        <InfoRow label="Payment method" value="Credit / Debit card" />
        <InfoRow label="Powered by" value="Stripe" last />
      </div>

      <Button
        size="lg"
        className="w-full"
        disabled={!isValid || isPending || !wallet}
        onClick={handlePay}
      >
        {isPending ? "Submitting…" : `Pay $${isValid ? parsedAmount.toFixed(2) : "—"} with Stripe`}
      </Button>

      <p className="text-center font-mono text-[11px] text-fg-3">
        Secured by Stripe · Funds appear instantly
      </p>
    </div>
  );
}

interface InfoRowProps {
  label: string;
  value: string;
  last?: boolean;
}

function InfoRow({ label, value, last }: InfoRowProps) {
  return (
    <div
      className={cn("flex items-center justify-between py-3", !last && "border-b border-border-1")}
    >
      <span className="text-sm text-fg-3">{label}</span>
      <span className="text-sm font-medium text-fg-1">{value}</span>
    </div>
  );
}
