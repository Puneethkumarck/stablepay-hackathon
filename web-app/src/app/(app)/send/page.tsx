"use client";

import { useRouter } from "next/navigation";
import { useCallback, useMemo } from "react";
import { toast } from "sonner";
import { TopBar } from "@/components/shared/top-bar";
import { Button } from "@/components/ui/button";
import { useFxRate } from "@/features/fx/hooks/use-fx-rate";
import {
  CORRIDOR,
  MAX_AMOUNT_USDC,
  MIN_AMOUNT_USDC,
  NETWORK_FEE,
  SETTLEMENT_TIME,
} from "@/lib/constants";
import { cn } from "@/lib/utils";
import { useSendFlowStore } from "@/stores/send-flow";

function formatInr(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export default function SendAmountPage() {
  const router = useRouter();
  const amountUsdc = useSendFlowStore((s) => s.amountUsdc);
  const setAmount = useSendFlowStore((s) => s.setAmount);
  const setFxRate = useSendFlowStore((s) => s.setFxRate);
  const { data: fxData, isLoading: fxLoading, isError: fxError, refetch } = useFxRate();

  const handleAmountChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const value = e.target.value.replace(/[^\d.]/g, "");
      const parts = value.split(".");
      if (parts.length > 2) return;
      if (parts[1] !== undefined && parts[1].length > 2) return;
      setAmount(value);
    },
    [setAmount],
  );

  const parsedAmount = Number.parseFloat(amountUsdc || "0");
  const rate = fxData ? Number.parseFloat(fxData.rate) : 0;

  const inrAmount = useMemo(() => {
    if (!fxData || !(parsedAmount > 0)) return "0.00";
    return formatInr(parsedAmount * rate);
  }, [parsedAmount, rate, fxData]);

  const isExpired = useMemo(() => {
    if (!fxData?.expiresAt) return false;
    return new Date(fxData.expiresAt).getTime() < Date.now();
  }, [fxData?.expiresAt]);

  const validationError = useMemo(() => {
    if (!amountUsdc || parsedAmount === 0) return null;
    if (parsedAmount < MIN_AMOUNT_USDC) return `Min $${MIN_AMOUNT_USDC.toFixed(2)}`;
    if (parsedAmount > MAX_AMOUNT_USDC)
      return `Max $${MAX_AMOUNT_USDC.toLocaleString("en-US", { minimumFractionDigits: 2 })}`;
    return null;
  }, [amountUsdc, parsedAmount]);

  const canContinue =
    parsedAmount >= MIN_AMOUNT_USDC &&
    parsedAmount <= MAX_AMOUNT_USDC &&
    !fxError &&
    !fxLoading &&
    fxData != null &&
    !isExpired;

  const handleContinue = useCallback(async () => {
    if (!fxData) return;

    if (isExpired) {
      const result = await refetch();
      if (result.error) {
        toast.error("Failed to refresh FX rate. Please try again.");
        return;
      }
      if (result.data) {
        setFxRate(result.data.rate, result.data.expiresAt);
      }
    } else {
      setFxRate(fxData.rate, fxData.expiresAt);
    }

    router.push("/send/recipient");
  }, [fxData, isExpired, refetch, setFxRate, router]);

  return (
    <div className="flex flex-col gap-6 px-5 pt-2 pb-6">
      <TopBar
        title="Send"
        onBack={() => router.push("/home")}
        right={
          <span className="text-[11px] tracking-[0.14em] uppercase text-fg-3">Step 1 of 3</span>
        }
      />

      <div className="flex flex-col items-center gap-3 pt-4">
        <span className="text-xs tracking-[0.14em] uppercase text-fg-3">You send</span>
        <div className="flex items-baseline justify-center gap-1">
          <span className="font-mono text-[40px] text-fg-3">$</span>
          <input
            type="text"
            inputMode="decimal"
            value={amountUsdc}
            onChange={handleAmountChange}
            placeholder="0"
            aria-label="Amount in USD"
            className={cn(
              "bg-transparent font-mono text-[56px] leading-none text-fg-1",
              "outline-none placeholder:text-fg-3/40 tabular-nums",
            )}
            style={{ width: `${Math.max(1, amountUsdc.length || 1)}ch` }}
          />
        </div>
        {validationError && <span className="text-sm text-danger">{validationError}</span>}
        <div className="flex flex-col items-center gap-1 text-sm text-fg-3">
          <span>
            They receive <span className="font-semibold text-fg-1">₹{inrAmount}</span>
          </span>
          {fxLoading && <span className="text-xs text-fg-3">Fetching rate...</span>}
          {fxError && (
            <span className="text-xs text-danger">
              Failed to load rate.{" "}
              <button type="button" onClick={() => refetch()} className="underline">
                Retry
              </button>
            </span>
          )}
          {fxData && !fxError && (
            <span className="text-xs text-fg-3">
              Rate locked · {Number.parseFloat(fxData.rate).toFixed(2)} INR / USD
            </span>
          )}
          {isExpired && !fxLoading && (
            <span className="text-xs text-warning">
              Rate expired.{" "}
              <button type="button" onClick={() => refetch()} className="underline">
                Refresh
              </button>
            </span>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-0 rounded-2xl bg-surface-2 px-4">
        <InfoRow label="Network fee" value={`$${NETWORK_FEE}`} valueClassName="text-green-400" />
        <InfoRow label="Settlement" value={SETTLEMENT_TIME} />
        <InfoRow label="Corridor" value={CORRIDOR.replace("_", " → ")} last />
      </div>

      <Button size="lg" className="w-full" disabled={!canContinue} onClick={handleContinue}>
        Continue
      </Button>
    </div>
  );
}

interface InfoRowProps {
  label: string;
  value: string;
  valueClassName?: string;
  last?: boolean;
}

function InfoRow({ label, value, valueClassName, last }: InfoRowProps) {
  return (
    <div
      className={cn("flex items-center justify-between py-3", !last && "border-b border-border-1")}
    >
      <span className="text-sm text-fg-3">{label}</span>
      <span className={cn("text-sm font-medium text-fg-1", valueClassName)}>{value}</span>
    </div>
  );
}
