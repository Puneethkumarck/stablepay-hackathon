import { TopBar } from "@/components/shared/top-bar";
import { SignOutButton } from "@/features/me/components/sign-out-button";
import { apiClient } from "@/lib/api-client";
import { getUserData, requireAuth } from "@/lib/auth";
import { formatCurrency, truncateAddress } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { WalletResponse } from "@/types/api";

function getInitial(email: string, name: string | null): string {
  if (name) return name.charAt(0).toUpperCase();
  return email.charAt(0).toUpperCase();
}

function getDisplayName(email: string, name: string | null): string {
  if (name) return name;
  return email.split("@")[0] ?? email;
}

function formatMemberSince(isoDate: string): string {
  const date = new Date(isoDate);
  return date.toLocaleDateString("en-US", { month: "short", year: "numeric" });
}

interface InfoRowProps {
  label: string;
  value: string;
  valueClassName?: string;
}

function InfoRow({ label, value, valueClassName }: InfoRowProps) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-surface-2 px-4 py-3.5">
      <span className="text-sm text-fg-3">{label}</span>
      <span className={cn("text-sm", valueClassName ?? "text-fg-2")}>{value}</span>
    </div>
  );
}

export default async function MePage() {
  const token = await requireAuth();

  const [userData, wallet] = await Promise.all([
    getUserData(),
    apiClient.get<WalletResponse>("/api/wallets/me", { token }),
  ]);

  const email = userData?.email || "—";
  const name = userData?.name ?? null;
  const initial = getInitial(email, name);
  const displayName = getDisplayName(email, name);
  const memberSince = userData?.createdAt ? formatMemberSince(userData.createdAt) : "—";

  return (
    <div className="flex flex-col gap-6 px-5 pt-2 pb-6">
      <TopBar title="Account" right={<div className="h-9 w-9" />} />

      <div className="flex flex-col items-center gap-3 pt-2 pb-1">
        <div className="grid h-[72px] w-[72px] place-items-center rounded-full bg-[image:var(--solana-gradient)] text-[28px] font-bold text-surface-1">
          {initial}
        </div>
        <div className="text-center">
          <p className="text-lg font-semibold text-fg-1">{displayName}</p>
          <p className="mt-1 font-mono text-[13px] text-fg-3">{email}</p>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <InfoRow
          label="USD Balance"
          value={formatCurrency(wallet.availableBalance, "USD")}
          valueClassName="text-fg-1"
        />
        <InfoRow label="Member since" value={memberSince} />
      </div>

      <div className="flex flex-col gap-2">
        <InfoRow
          label="Wallet"
          value={truncateAddress(wallet.solanaAddress)}
          valueClassName="font-mono text-[11px] text-fg-2"
        />
        <InfoRow label="Network" value="Solana Mainnet" />
        <InfoRow label="KYC status" value="Verified" valueClassName="text-green-300" />
      </div>

      <div className="flex flex-col gap-2">
        <InfoRow label="Notifications" value="On" />
        <a
          href="mailto:support@stablepay.app"
          className="flex items-center justify-between rounded-lg bg-surface-2 px-4 py-3.5 transition-colors hover:bg-surface-3"
        >
          <span className="text-sm text-fg-3">Support</span>
          <span className="text-sm text-fg-2">→</span>
        </a>
        <SignOutButton />
      </div>
    </div>
  );
}
