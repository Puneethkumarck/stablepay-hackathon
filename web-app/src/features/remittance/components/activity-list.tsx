import { TransactionRow } from "@/components/shared/transaction-row";
import type { RemittanceResponse } from "@/types/api";

interface ActivityListProps {
  remittances: RemittanceResponse[];
}

export function ActivityList({ remittances }: ActivityListProps) {
  if (remittances.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 py-8 text-center">
        <p className="text-sm text-fg-3">No transfers yet.</p>
        <p className="text-xs text-fg-3">Send your first remittance!</p>
      </div>
    );
  }

  return (
    <div>
      <h3 className="px-4 py-2 text-xs font-semibold uppercase tracking-widest text-fg-3">
        All transfers
      </h3>
      <div className="space-y-1">
        {remittances.map((remittance) => (
          <TransactionRow key={remittance.remittanceId} remittance={remittance} />
        ))}
      </div>
    </div>
  );
}
