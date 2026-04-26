"use client";

import { Activity, ArrowLeftRight, Home, User } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const TABS = [
  { key: "home", label: "Home", href: "/home", icon: Home },
  { key: "send", label: "Send", href: "/send", icon: ArrowLeftRight },
  { key: "activity", label: "Activity", href: "/activity", icon: Activity },
  { key: "me", label: "Me", href: "/me", icon: User },
] as const;

export function BottomNav() {
  const pathname = usePathname();

  function isActive(href: string): boolean {
    if (href === "/home") return pathname === "/home";
    return pathname.startsWith(href);
  }

  return (
    <>
      <svg width="0" height="0" className="absolute" aria-hidden="true">
        <defs>
          <linearGradient id="sp-tab-grad" x1="0" x2="1" y1="0" y2="0">
            <stop offset="0%" stopColor="#00FFA3" />
            <stop offset="50%" stopColor="#9945FF" />
            <stop offset="100%" stopColor="#DC1FFF" />
          </linearGradient>
        </defs>
      </svg>
      <nav
        className="fixed bottom-0 left-1/2 z-50 flex w-full max-w-[430px] -translate-x-1/2 items-center justify-around border-t border-border-1 bg-surface-1/95 pb-[env(safe-area-inset-bottom)] pt-2 backdrop-blur-md"
        aria-label="Main navigation"
      >
        {TABS.map((tab) => {
          const active = isActive(tab.href);
          const Icon = tab.icon;
          return (
            <Link
              key={tab.key}
              href={tab.href}
              className={cn(
                "flex flex-col items-center gap-0.5 px-3 py-1 text-[11px] transition-colors",
                active ? "text-fg-1" : "text-fg-3 hover:text-fg-2",
              )}
              aria-current={active ? "page" : undefined}
            >
              <Icon
                size={22}
                strokeWidth={1.8}
                {...(active ? { stroke: "url(#sp-tab-grad)" } : {})}
              />
              <span>{tab.label}</span>
            </Link>
          );
        })}
      </nav>
    </>
  );
}
