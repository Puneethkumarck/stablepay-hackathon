import { ArrowLeftRight, Plus } from "lucide-react";
import Link from "next/link";
import { cn } from "@/lib/utils";

export function ActionButtons() {
  return (
    <div className="grid grid-cols-2 gap-3">
      <Link
        href="/send"
        className={cn(
          "flex flex-col items-center gap-2 rounded-xl border border-accent/30 bg-surface-2 px-4 py-4",
          "transition-colors hover:bg-surface-3",
        )}
      >
        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-accent/10 text-accent">
          <ArrowLeftRight size={18} />
        </div>
        <span className="text-sm font-medium text-fg-1">Send</span>
      </Link>
      <Link
        href="/add-funds"
        className={cn(
          "flex flex-col items-center gap-2 rounded-xl border border-border-1 bg-surface-2 px-4 py-4",
          "transition-colors hover:bg-surface-3",
        )}
      >
        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-3 text-fg-2">
          <Plus size={18} />
        </div>
        <span className="text-sm font-medium text-fg-1">Add funds</span>
      </Link>
    </div>
  );
}
