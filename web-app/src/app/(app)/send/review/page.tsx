"use client";

import { ShieldCheck } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { toast } from "sonner";
import { TopBar } from "@/components/shared/top-bar";
import { Button } from "@/components/ui/button";
import {
  CreateRemittanceError,
  useCreateRemittance,
} from "@/features/remittance/hooks/use-create-remittance";
import { CLAIM_EXPIRY_HOURS, NETWORK_FEE } from "@/lib/constants";
import { getErrorMessage } from "@/lib/errors";
import { cn } from "@/lib/utils";
import { useSendFlowStore } from "@/stores/send-flow";

function formatInr(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export default function SendReviewPage() {
  const router = useRouter();
  const submitted = useRef(false);
  const amountUsdc = useSendFlowStore((s) => s.amountUsdc);
  const recipientPhone = useSendFlowStore((s) => s.recipientPhone);
  const recipientName = useSendFlowStore((s) => s.recipientName);
  const fxRate = useSendFlowStore((s) => s.fxRate);
  const fxRateExpiresAt = useSendFlowStore((s) => s.fxRateExpiresAt);
  const reset = useSendFlowStore((s) => s.reset);
  const { mutate, isPending } = useCreateRemittance();

  useEffect(() => {
    if (!amountUsdc || !recipientPhone || !fxRate) {
      if (!submitted.current) {
        router.replace("/send");
      }
    }
  }, [amountUsdc, recipientPhone, fxRate, router]);

  const parsedAmount = Number.parseFloat(amountUsdc || "0");
  const rate = fxRate ? Number.parseFloat(fxRate) : 0;

  const inrAmount = useMemo(() => {
    if (!fxRate || !(parsedAmount > 0)) return "0.00";
    return formatInr(parsedAmount * rate);
  }, [parsedAmount, rate, fxRate]);

  const isRateExpired = useMemo(() => {
    if (!fxRateExpiresAt) return false;
    return new Date(fxRateExpiresAt).getTime() < Date.now();
  }, [fxRateExpiresAt]);

  const handleConfirm = useCallback(() => {
    mutate(
      {
        recipientPhone,
        amountUsdc,
        ...(recipientName ? { recipientName } : {}),
      },
      {
        onSuccess: (data) => {
          submitted.current = true;
          const params = new URLSearchParams({
            remittanceId: data.remittanceId,
            amount: amountUsdc,
            phone: recipientPhone,
            fxRate: fxRate ?? "",
          });
          reset();
          router.push(`/send/sending?${params.toString()}`);
        },
        onError: (error) => {
          if (error instanceof CreateRemittanceError) {
            toast.error(getErrorMessage(error.errorCode));
          } else {
            toast.error("Something went wrong. Please try again.");
          }
        },
      },
    );
  }, [amountUsdc, recipientPhone, recipientName, fxRate, mutate, reset, router]);

  if (!amountUsdc || !recipientPhone || !fxRate) return null;

  return (
    <div className="flex flex-col px-5 pt-2 pb-6">
      <TopBar
        title="Review"
        onBack={() => router.push("/send/recipient")}
        right={
          <span className="text-[11px] tracking-[0.14em] uppercase text-fg-3">Step 3 of 3</span>
        }
      />

      <div className="flex flex-col items-center gap-1 pt-6 pb-4">
        <span className="text-xs tracking-[0.14em] uppercase text-fg-3">They receive</span>
        <div className="font-mono text-[40px] leading-none text-fg-1">
          <span className="text-[28px] text-fg-3">₹</span>
          {inrAmount}
        </div>
        <span className="text-sm text-fg-3">to {recipientPhone}</span>
      </div>

      <div className="flex flex-col rounded-2xl bg-surface-2 px-4">
        <InfoRow label="You send" value={`$${parsedAmount.toFixed(2)} USDC`} />
        <InfoRow label="FX rate" value={`${rate.toFixed(2)} INR / USD`} />
        <InfoRow label="Network fee" value={`$${NETWORK_FEE}`} valueClassName="text-green-400" />
        <InfoRow label="Delivery" value="Instant on-chain + UPI" />
        <InfoRow label="Claim expires" value={`${CLAIM_EXPIRY_HOURS} hours`} last />
      </div>

      {isRateExpired && (
        <div className="mt-3 rounded-xl border border-warning/30 bg-warning/10 px-4 py-3 text-xs text-warning">
          FX rate has expired. Please go back and get a fresh quote.
        </div>
      )}

      <div className="mt-4 flex gap-2.5 rounded-xl border border-accent/20 bg-accent/10 p-3">
        <div className="shrink-0 text-accent">
          <ShieldCheck size={18} />
        </div>
        <p className="text-xs leading-relaxed text-[#D6B4FF]">
          Funds are held in a Solana escrow until the recipient enters their UPI ID. If unclaimed
          within 48h, you get an automatic refund.
        </p>
      </div>

      <div className="mt-6">
        <Button
          size="lg"
          className="w-full"
          disabled={isPending || isRateExpired}
          onClick={handleConfirm}
        >
          {isPending ? "Signing…" : `Confirm & send $${parsedAmount.toFixed(2)}`}
        </Button>
      </div>
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
