import { cn } from "@/lib/utils";
import type { RemittanceStatus } from "@/types/api";

interface StatusBadgeProps {
  status: RemittanceStatus;
}

const STATUS_CONFIG: Record<
  RemittanceStatus,
  { label: string; className: string; pulse: boolean }
> = {
  INITIATED: { label: "Initiated", className: "bg-surface-3 text-fg-3", pulse: false },
  ESCROWED: {
    label: "Escrowed",
    className: "bg-accent/16 text-[#D6B4FF]",
    pulse: true,
  },
  CLAIMED: { label: "Claimed", className: "bg-info/16 text-info", pulse: true },
  DELIVERED: {
    label: "Delivered",
    className: "bg-success/16 text-success",
    pulse: false,
  },
  DISBURSEMENT_FAILED: {
    label: "Disbursement Failed",
    className: "bg-danger/16 text-danger",
    pulse: false,
  },
  DEPOSIT_FAILED: {
    label: "Deposit Failed",
    className: "bg-danger/16 text-danger",
    pulse: false,
  },
  CLAIM_FAILED: {
    label: "Claim Failed",
    className: "bg-danger/16 text-danger",
    pulse: false,
  },
  REFUND_FAILED: {
    label: "Refund Failed",
    className: "bg-danger/16 text-danger",
    pulse: false,
  },
  REFUNDED: {
    label: "Refunded",
    className: "bg-warning/16 text-warning",
    pulse: false,
  },
  CANCELLED: { label: "Cancelled", className: "bg-surface-3 text-fg-3", pulse: false },
};

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = STATUS_CONFIG[status];

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium",
        config.className,
      )}
    >
      {config.pulse && (
        <span className="relative flex h-1.5 w-1.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-75" />
          <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
        </span>
      )}
      {config.label}
    </span>
  );
}
