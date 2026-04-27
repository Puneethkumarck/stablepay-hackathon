"use client";

import { useQuery } from "@tanstack/react-query";
import { ChevronRight, Phone, User } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect } from "react";
import { TopBar } from "@/components/shared/top-bar";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useSendFlowStore } from "@/stores/send-flow";
import type { RecentRecipient } from "@/types/api";

function getInitials(name: string): string {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase();
}

export default function SendRecipientPage() {
  const router = useRouter();
  const amountUsdc = useSendFlowStore((s) => s.amountUsdc);
  const recipientPhone = useSendFlowStore((s) => s.recipientPhone);
  const recipientName = useSendFlowStore((s) => s.recipientName);
  const setRecipient = useSendFlowStore((s) => s.setRecipient);
  const setRecipientName = useCallback(
    (name: string) => setRecipient(recipientPhone, name),
    [setRecipient, recipientPhone],
  );

  const { data: recentRecipients } = useQuery<RecentRecipient[]>({
    queryKey: ["recipients", "recent"],
    queryFn: async () => {
      const res = await fetch("/api/recipients/recent");
      if (!res.ok) return [];
      return res.json() as Promise<RecentRecipient[]>;
    },
  });

  useEffect(() => {
    if (!amountUsdc) {
      router.replace("/send");
    }
  }, [amountUsdc, router]);

  const handlePhoneChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setRecipient(e.target.value, recipientName);
    },
    [setRecipient, recipientName],
  );

  const handleNameChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setRecipientName(e.target.value);
    },
    [setRecipientName],
  );

  const handleContactTap = useCallback(
    (contact: RecentRecipient) => {
      setRecipient(contact.phone, contact.name);
      router.push("/send/review");
    },
    [setRecipient, router],
  );

  const handleContinue = useCallback(() => {
    router.push("/send/review");
  }, [router]);

  if (!amountUsdc) return null;

  return (
    <div className="flex flex-col px-5 pt-2 pb-6">
      <TopBar
        title="Recipient"
        onBack={() => router.push("/send")}
        right={
          <span className="text-[11px] tracking-[0.14em] uppercase text-fg-3">Step 2 of 3</span>
        }
      />

      <div className="mt-4 flex flex-col gap-3">
        <div className="relative">
          <input
            type="text"
            placeholder="Recipient name"
            value={recipientName}
            onChange={handleNameChange}
            aria-label="Recipient name"
            className={cn(
              "w-full rounded-xl border border-border-2 bg-surface-2 py-3.5 pr-4 pl-11",
              "text-[15px] text-fg-1 outline-none",
              "placeholder:text-fg-3/40",
              "focus:border-accent",
            )}
          />
          <div className="absolute top-3.5 left-3.5 text-fg-3">
            <User size={16} />
          </div>
        </div>

        <div className="relative">
          <input
            type="tel"
            placeholder="+91 98765 43210"
            value={recipientPhone}
            onChange={handlePhoneChange}
            aria-label="Recipient phone number"
            className={cn(
              "w-full rounded-xl border border-border-2 bg-surface-2 py-3.5 pr-4 pl-11",
              "font-mono text-[15px] text-fg-1 outline-none",
              "placeholder:text-fg-3/40",
              "focus:border-accent",
            )}
          />
          <div className="absolute top-3.5 left-3.5 text-fg-3">
            <Phone size={16} />
          </div>
        </div>
      </div>

      {recentRecipients && recentRecipients.length > 0 && (
        <>
          <div className="mb-2.5 mt-6 text-xs tracking-[0.14em] uppercase text-fg-3">Recent</div>
          <div className="flex flex-col">
            {recentRecipients.map((contact) => (
              <button
                key={contact.phone}
                type="button"
                onClick={() => handleContactTap(contact)}
                className={cn(
                  "flex items-center gap-3 rounded-xl px-2 py-3",
                  "transition-colors hover:bg-surface-2",
                )}
              >
                <div
                  className={cn(
                    "flex h-10 w-10 shrink-0 items-center justify-center rounded-full",
                    "bg-gradient-to-br from-solana-teal via-solana-purple to-solana-magenta",
                    "text-xs font-semibold text-white",
                  )}
                >
                  {getInitials(contact.name)}
                </div>
                <div className="flex flex-col items-start gap-0.5 min-w-0 flex-1">
                  <span className="text-sm font-medium text-fg-1">{contact.name}</span>
                  <span className="text-xs text-fg-3 font-mono">{contact.phone}</span>
                </div>
                <ChevronRight size={14} className="text-fg-3 shrink-0" />
              </button>
            ))}
          </div>
        </>
      )}

      <div className="mt-auto pt-6">
        <Button
          size="lg"
          className="w-full"
          disabled={!recipientPhone.trim()}
          onClick={handleContinue}
        >
          Continue
        </Button>
      </div>
    </div>
  );
}
