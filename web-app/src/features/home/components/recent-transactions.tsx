import Link from "next/link";
import { TransactionRow } from "@/components/shared/transaction-row";
import type { RemittanceResponse } from "@/types/api";

interface RecentTransactionsProps {
  remittances: RemittanceResponse[];
}

export function RecentTransactions({ remittances }: RecentTransactionsProps) {
  if (remittances.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 py-8 text-center">
        <p className="text-sm text-fg-3">No transfers yet.</p>
        <p className="text-xs text-fg-4">Send your first remittance!</p>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between px-4 py-2">
        <h3 className="text-xs font-semibold uppercase tracking-widest text-fg-3">Recent</h3>
        <Link href="/activity" className="text-xs font-medium text-accent hover:underline">
          See all
        </Link>
      </div>
      <div className="space-y-1">
        {remittances.map((remittance) => (
          <TransactionRow key={remittance.remittanceId} remittance={remittance} />
        ))}
      </div>
    </div>
  );
}
