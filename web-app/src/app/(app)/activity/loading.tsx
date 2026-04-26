import { SkeletonRow } from "@/components/shared/skeleton-row";
import { Skeleton } from "@/components/ui/skeleton";

export default function ActivityLoading() {
  return (
    <div className="flex flex-col gap-4 pt-14 pb-6">
      <div className="px-5">
        <Skeleton className="h-5 w-20 bg-surface-4" />
      </div>
      <div className="space-y-1">
        <SkeletonRow />
        <SkeletonRow />
        <SkeletonRow />
        <SkeletonRow />
        <SkeletonRow />
      </div>
    </div>
  );
}
