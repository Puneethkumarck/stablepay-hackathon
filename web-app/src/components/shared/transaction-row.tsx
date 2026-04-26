import Link from "next/link";
import { formatCurrency, formatRelativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { RemittanceResponse } from "@/types/api";
import { StatusBadge } from "./status-badge";

interface TransactionRowProps {
  remittance: RemittanceResponse;
}

function getInitials(name: string | null, phone: string): string {
  if (name) {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  }
  return phone.slice(-2);
}

function getDisplayName(name: string | null, phone: string): string {
  if (name) return name;
  return phone;
}

export function TransactionRow({ remittance }: TransactionRowProps) {
  const initials = getInitials(remittance.recipientName, remittance.recipientPhone);
  const displayName = getDisplayName(remittance.recipientName, remittance.recipientPhone);
  const relativeTime = formatRelativeTime(remittance.createdAt);
  const formattedAmount = formatCurrency(remittance.amountUsdc, "USD");

  return (
    <Link
      href={`/detail/${remittance.remittanceId}`}
      className={cn(
        "flex items-center gap-3 rounded-xl px-4 py-3 transition-colors hover:bg-surface-2",
      )}
    >
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-surface-3 text-xs font-semibold text-fg-2">
        {initials}
      </div>
      <div className="min-w-0 flex-1">
        <div className="truncate text-[15px] font-medium text-fg-1">{displayName}</div>
        <div className="truncate text-xs text-fg-3">
          {remittance.recipientName ? remittance.recipientPhone : null}
          {remittance.recipientName ? " · " : ""}
          {relativeTime}
        </div>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1">
        <div className="font-mono text-sm font-medium text-fg-1">-{formattedAmount}</div>
        <StatusBadge status={remittance.status} />
      </div>
    </Link>
  );
}
