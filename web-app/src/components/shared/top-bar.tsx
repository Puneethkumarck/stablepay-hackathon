"use client";

import { ArrowLeft, Bell, User } from "lucide-react";
import { cn } from "@/lib/utils";

interface TopBarProps {
  title: string;
  onBack?: () => void;
  right?: React.ReactNode;
}

export function TopBar({ title, onBack, right }: TopBarProps) {
  return (
    <div className="flex items-center justify-between px-5 py-3">
      {onBack ? (
        <button
          type="button"
          onClick={onBack}
          className={cn(
            "flex h-9 w-9 items-center justify-center rounded-full",
            "bg-surface-2 text-fg-2 transition-colors hover:bg-surface-3",
          )}
          aria-label="Go back"
        >
          <ArrowLeft size={18} />
        </button>
      ) : (
        <button
          type="button"
          className={cn(
            "flex h-9 w-9 items-center justify-center rounded-full",
            "bg-surface-2 text-fg-2 transition-colors hover:bg-surface-3",
          )}
          aria-label="User menu"
        >
          <User size={18} />
        </button>
      )}
      <div className="text-[15px] font-semibold text-fg-1">{title}</div>
      {right !== undefined ? (
        right
      ) : (
        <button
          type="button"
          className={cn(
            "flex h-9 w-9 items-center justify-center rounded-full",
            "bg-surface-2 text-fg-2 transition-colors hover:bg-surface-3",
          )}
          aria-label="Notifications"
        >
          <Bell size={18} />
        </button>
      )}
    </div>
  );
}
