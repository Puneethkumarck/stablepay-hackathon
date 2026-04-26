import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface SkeletonCardProps {
  className?: string;
}

export function SkeletonCard({ className }: SkeletonCardProps) {
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label="Loading"
      className={cn("rounded-xl bg-surface-2 p-5 space-y-3", className)}
    >
      <Skeleton className="h-3 w-24 bg-surface-4" />
      <Skeleton className="h-8 w-36 bg-surface-4" />
      <Skeleton className="h-3 w-20 bg-surface-4" />
    </div>
  );
}
