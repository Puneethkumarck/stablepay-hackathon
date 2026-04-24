import { SkeletonRow } from "@/components/shared/skeleton-row";
import { Skeleton } from "@/components/ui/skeleton";

export default function HomeLoading() {
  return (
    <div className="flex flex-col gap-6 px-5 pt-14 pb-6">
      <div className="rounded-2xl bg-surface-2 p-5 space-y-3">
        <Skeleton className="h-3 w-28 bg-surface-4" />
        <Skeleton className="h-12 w-40 bg-surface-4" />
        <Skeleton className="h-3 w-36 bg-surface-4" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Skeleton className="h-20 rounded-xl bg-surface-2" />
        <Skeleton className="h-20 rounded-xl bg-surface-2" />
      </div>
      <div className="space-y-1">
        <div className="flex items-center justify-between px-4 py-2">
          <Skeleton className="h-3 w-14 bg-surface-4" />
          <Skeleton className="h-3 w-12 bg-surface-4" />
        </div>
        <SkeletonRow />
        <SkeletonRow />
        <SkeletonRow />
      </div>
    </div>
  );
}
