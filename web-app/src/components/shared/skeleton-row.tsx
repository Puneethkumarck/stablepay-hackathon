import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface SkeletonRowProps {
  className?: string;
}

export function SkeletonRow({ className }: SkeletonRowProps) {
  return (
    <div className={cn("flex items-center gap-3 px-5 py-3", className)}>
      <Skeleton className="h-10 w-10 shrink-0 rounded-full bg-surface-4" />
      <div className="flex-1 space-y-2">
        <Skeleton className="h-3 w-28 bg-surface-4" />
        <Skeleton className="h-3 w-20 bg-surface-4" />
      </div>
      <Skeleton className="h-4 w-16 bg-surface-4" />
    </div>
  );
}
