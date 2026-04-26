import { AmountDisplay } from "@/components/shared/amount-display";
import { truncateAddress } from "@/lib/format";
import type { WalletResponse } from "@/types/api";

interface BalanceCardProps {
  wallet: WalletResponse;
}

export function BalanceCard({ wallet }: BalanceCardProps) {
  const truncatedAddress = truncateAddress(wallet.solanaAddress);

  return (
    <div className="relative overflow-hidden rounded-2xl bg-surface-2 p-5">
      <div className="absolute -top-16 -right-16 h-48 w-48 rounded-full bg-gradient-to-br from-solana-teal/20 via-solana-purple/15 to-solana-magenta/10 blur-3xl" />
      <div className="relative">
        <p className="text-xs font-medium uppercase tracking-widest text-fg-3">
          USDC balance · Solana
        </p>
        <div className="mt-2">
          <AmountDisplay amount={wallet.availableBalance} currency="USD" size="xl" />
        </div>
        <p className="mt-2 font-mono text-xs text-fg-3">{truncatedAddress} · Available to send</p>
      </div>
    </div>
  );
}
