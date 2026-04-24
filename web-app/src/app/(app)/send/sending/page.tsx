"use client";

import { Check } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { TopBar } from "@/components/shared/top-bar";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface Step {
  title: string;
  sub: string | null;
  subTemplate: boolean;
}

const STEPS: readonly Step[] = [
  { title: "Authorising transfer", sub: "Securely signing your transaction", subTemplate: false },
  { title: "Locking funds", sub: "Held safely until recipient claims", subTemplate: false },
  { title: "Notifying recipient", sub: null, subTemplate: true },
];

const STEP_DELAY_MS = 900;

function formatInr(amount: number, rate: number): string {
  return new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount * rate);
}

export default function SendingSendingPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const remittanceId = searchParams.get("remittanceId");
  const amount = searchParams.get("amount");
  const phone = searchParams.get("phone");
  const fxRate = searchParams.get("fxRate");

  const [step, setStep] = useState(0);
  const complete = step >= 3;

  useEffect(() => {
    if (!remittanceId) {
      router.replace("/home");
    }
  }, [remittanceId, router]);

  useEffect(() => {
    const timers = [
      setTimeout(() => setStep(1), STEP_DELAY_MS),
      setTimeout(() => setStep(2), STEP_DELAY_MS * 2),
      setTimeout(() => setStep(3), STEP_DELAY_MS * 3),
    ];
    return () => {
      for (const t of timers) clearTimeout(t);
    };
  }, []);

  const parsedAmount = Number.parseFloat(amount ?? "0");
  const parsedRate = Number.parseFloat(fxRate ?? "0");

  const inrAmount = useMemo(() => {
    if (!(parsedAmount > 0) || !(parsedRate > 0)) return "0.00";
    return formatInr(parsedAmount, parsedRate);
  }, [parsedAmount, parsedRate]);

  if (!remittanceId) return null;

  return (
    <div className="flex flex-col px-5 pt-2 pb-6">
      <TopBar title={complete ? "Transfer sent" : "Sending…"} />

      <div className="flex flex-col items-center gap-1 pt-6 pb-4">
        <span className="text-xs tracking-[0.14em] uppercase text-fg-3">
          {complete ? "Sent" : "Sending"}
        </span>
        <div className="font-mono text-[40px] leading-none text-fg-1">
          <span className="text-[28px] text-fg-3">$</span>
          {parsedAmount.toFixed(2)}
        </div>
        <span className="text-sm text-fg-3">
          to {phone} · ₹{inrAmount}
        </span>
      </div>

      <div className="flex flex-col rounded-2xl bg-surface-2 p-4">
        <div className="flex flex-col gap-0">
          {STEPS.map((s, i) => {
            const state = step > i ? "done" : step === i ? "live" : "pending";
            const isLast = i === STEPS.length - 1;
            return (
              <div key={s.title} className="flex gap-3">
                <div className="flex flex-col items-center">
                  <StepDot state={state} />
                  {!isLast && (
                    <div
                      className={cn(
                        "w-px flex-1 min-h-6",
                        state === "done" ? "bg-success" : "bg-border-1",
                      )}
                    />
                  )}
                </div>
                <div className={cn("pb-5", isLast && "pb-0")}>
                  <div
                    className={cn(
                      "text-sm font-medium",
                      state === "done" ? "text-fg-1" : state === "live" ? "text-fg-1" : "text-fg-3",
                    )}
                  >
                    {s.title}
                  </div>
                  <div className="text-xs text-fg-3 mt-0.5">
                    {s.subTemplate ? `Claim link sent to ${phone}` : s.sub}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <AnimatePresence>
        {complete && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            className="flex flex-col items-center gap-1 pt-4 pb-2"
          >
            <span className="text-sm font-semibold text-success">Sent — awaiting claim</span>
            <span className="text-xs font-mono text-fg-3">48h claim window</span>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="mt-6">
        {complete ? (
          <Button size="lg" className="w-full" onClick={() => router.push("/home")}>
            Done
          </Button>
        ) : (
          <Button size="lg" className="w-full" variant="secondary" disabled>
            Processing…
          </Button>
        )}
      </div>
    </div>
  );
}

interface StepDotProps {
  state: "pending" | "live" | "done";
}

function StepDot({ state }: StepDotProps) {
  return (
    <div
      className={cn(
        "flex h-6 w-6 items-center justify-center rounded-full shrink-0",
        state === "done" && "bg-success",
        state === "live" && "bg-success",
        state === "pending" && "bg-surface-3",
      )}
    >
      {state === "done" && (
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: "spring", stiffness: 400, damping: 15 }}
        >
          <Check size={12} strokeWidth={3} className="text-surface-0" />
        </motion.div>
      )}
      {state === "live" && (
        <span className="relative flex h-2.5 w-2.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-surface-0 opacity-60" />
          <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-surface-0" />
        </span>
      )}
      {state === "pending" && <span className="h-2 w-2 rounded-full bg-fg-4" />}
    </div>
  );
}
