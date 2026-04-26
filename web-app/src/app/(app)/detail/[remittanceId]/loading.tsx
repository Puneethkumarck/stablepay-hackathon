import { Skeleton } from "@/components/ui/skeleton";

const TIMELINE_KEYS = ["initiated", "escrowed", "sms", "claim", "delivery"];
const INFO_KEYS = ["remittance-id", "escrow-pda", "fee", "fx-rate"];

export default function DetailLoading() {
  return (
    <div className="flex flex-col gap-4 px-5 pt-14 pb-6">
      <div className="flex flex-col items-center gap-2 pt-2 pb-2">
        <Skeleton className="h-3 w-16 bg-surface-4" />
        <Skeleton className="h-12 w-36 bg-surface-4" />
        <Skeleton className="h-4 w-44 bg-surface-4" />
        <Skeleton className="mt-2 h-6 w-28 rounded-full bg-surface-4" />
      </div>

      <div className="flex flex-col gap-4 rounded-2xl bg-surface-2 p-4">
        {TIMELINE_KEYS.map((key) => (
          <div key={key} className="flex gap-3">
            <Skeleton className="h-6 w-6 shrink-0 rounded-full bg-surface-4" />
            <div className="flex-1 space-y-1.5">
              <Skeleton className="h-4 w-32 bg-surface-4" />
              <Skeleton className="h-3 w-24 bg-surface-4" />
            </div>
          </div>
        ))}
      </div>

      <div className="flex flex-col rounded-2xl bg-surface-2">
        {INFO_KEYS.map((key) => (
          <div
            key={key}
            className="flex items-center justify-between px-4 py-3 border-b border-border-1 last:border-b-0"
          >
            <Skeleton className="h-4 w-24 bg-surface-4" />
            <Skeleton className="h-4 w-20 bg-surface-4" />
          </div>
        ))}
      </div>
    </div>
  );
}
