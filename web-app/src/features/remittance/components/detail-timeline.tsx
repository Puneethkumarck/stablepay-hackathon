import { Check, X } from "lucide-react";
import { formatRelativeTime, truncateAddress } from "@/lib/format";
import { cn } from "@/lib/utils";
import type {
  RemittanceResponse,
  RemittanceTimelineResponse,
  TimelineStep,
  TimelineStepStatus,
} from "@/types/api";
import { ClaimCountdown } from "./claim-countdown";

interface DetailTimelineProps {
  timeline: RemittanceTimelineResponse;
  remittance: RemittanceResponse;
}

type VisualState = "done" | "live" | "pending" | "failed";

interface VisualStep {
  title: string;
  sub: string | null;
  state: VisualState;
}

function findStep(steps: TimelineStep[], status: string): TimelineStep | undefined {
  return steps.find((s) => s.step === status);
}

function mapStatus(status: TimelineStepStatus): VisualState {
  switch (status) {
    case "COMPLETED":
      return "done";
    case "CURRENT":
      return "live";
    case "FAILED":
      return "failed";
    case "PENDING":
      return "pending";
  }
}

function buildVisualSteps(
  timeline: RemittanceTimelineResponse,
  remittance: RemittanceResponse,
): VisualStep[] {
  const initiated = findStep(timeline.steps, "INITIATED");
  const escrowed = findStep(timeline.steps, "ESCROWED");
  const claimed = findStep(timeline.steps, "CLAIMED");
  const delivered = findStep(timeline.steps, "DELIVERED");

  const escrowDone = escrowed?.status === "COMPLETED";
  const smsState: VisualState = escrowDone
    ? remittance.smsNotificationFailed
      ? "failed"
      : "done"
    : "pending";

  return [
    {
      title: "Initiated",
      sub: initiated?.completedAt ? formatRelativeTime(initiated.completedAt) : null,
      state: initiated ? mapStatus(initiated.status) : "pending",
    },
    {
      title: "Escrowed on Solana",
      sub: escrowed?.completedAt
        ? `${truncateAddress(remittance.escrowPda)} · ${formatRelativeTime(escrowed.completedAt)}`
        : null,
      state: escrowed ? mapStatus(escrowed.status) : "pending",
    },
    {
      title: remittance.smsNotificationFailed ? "SMS delivery failed" : "Claim SMS delivered",
      sub: smsState === "done" ? remittance.recipientPhone : null,
      state: smsState,
    },
    {
      title: "Awaiting recipient claim",
      sub: null,
      state: claimed ? mapStatus(claimed.status) : "pending",
    },
    {
      title: "Delivery via UPI",
      sub: delivered?.completedAt ? formatRelativeTime(delivered.completedAt) : null,
      state: delivered ? mapStatus(delivered.status) : "pending",
    },
  ];
}

export function DetailTimeline({ timeline, remittance }: DetailTimelineProps) {
  const steps = buildVisualSteps(timeline, remittance);

  return (
    <div className="flex flex-col rounded-2xl bg-surface-2 p-4">
      {steps.map((step, i) => {
        const isLast = i === steps.length - 1;
        const isClaimStep = step.title === "Awaiting recipient claim";
        const showCountdown = isClaimStep && step.state === "live";

        return (
          <div key={step.title} className="flex gap-3">
            <div className="flex flex-col items-center">
              <TimelineDot state={step.state} />
              {!isLast && (
                <div
                  className={cn(
                    "w-px min-h-6 flex-1",
                    step.state === "done" ? "bg-success" : "bg-border-1",
                  )}
                />
              )}
            </div>
            <div className={cn("pb-5", isLast && "pb-0")}>
              <div
                className={cn(
                  "text-sm font-medium",
                  step.state === "failed"
                    ? "text-danger"
                    : step.state === "pending"
                      ? "text-fg-3"
                      : "text-fg-1",
                )}
              >
                {step.title}
              </div>
              {(step.sub || showCountdown) && (
                <div className="mt-0.5 text-xs text-fg-3">
                  {showCountdown ? <ClaimCountdown expiresAt={remittance.expiresAt} /> : step.sub}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

interface TimelineDotProps {
  state: VisualState;
}

function TimelineDot({ state }: TimelineDotProps) {
  return (
    <div
      className={cn(
        "flex h-6 w-6 shrink-0 items-center justify-center rounded-full",
        state === "done" && "bg-success",
        state === "live" && "bg-success",
        state === "failed" && "bg-danger",
        state === "pending" && "bg-surface-3",
      )}
    >
      {state === "done" && <Check size={12} strokeWidth={3} className="text-surface-0" />}
      {state === "live" && (
        <span className="relative flex h-2.5 w-2.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-surface-0 opacity-60" />
          <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-surface-0" />
        </span>
      )}
      {state === "failed" && <X size={12} strokeWidth={3} className="text-surface-0" />}
      {state === "pending" && <span className="h-2 w-2 rounded-full bg-fg-3" />}
    </div>
  );
}
