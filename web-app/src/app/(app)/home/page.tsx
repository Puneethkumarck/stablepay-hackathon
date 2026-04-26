import { TopBar } from "@/components/shared/top-bar";
import { ActionButtons } from "@/features/home/components/action-buttons";
import { BalanceCard } from "@/features/home/components/balance-card";
import { RecentTransactions } from "@/features/home/components/recent-transactions";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { PageResponse, RemittanceResponse, WalletResponse } from "@/types/api";

export default async function HomePage() {
  const token = await requireAuth();

  const [wallet, remittancePage] = await Promise.all([
    apiClient.get<WalletResponse>("/api/wallets/me", { token }),
    apiClient.get<PageResponse<RemittanceResponse>>("/api/remittances?size=3&sort=createdAt,desc", {
      token,
    }),
  ]);

  return (
    <div className="flex flex-col gap-6 px-5 pt-2 pb-6">
      <TopBar title="stablepay" />
      <BalanceCard wallet={wallet} />
      <ActionButtons />
      <RecentTransactions remittances={remittancePage.content} />
    </div>
  );
}
