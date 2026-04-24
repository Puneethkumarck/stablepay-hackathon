import { SkeletonCard } from "@/components/shared/skeleton-card";
import { SkeletonRow } from "@/components/shared/skeleton-row";

export default function HomeLoading() {
  return (
    <div className="flex flex-col gap-6 px-5 pt-14 pb-6">
      <SkeletonCard />
      <div className="space-y-1">
        <SkeletonRow />
        <SkeletonRow />
        <SkeletonRow />
      </div>
    </div>
  );
}
