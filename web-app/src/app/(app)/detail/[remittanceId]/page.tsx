import { StatusBadge } from "@/components/shared/status-badge";
import { DetailInfoRows } from "@/features/remittance/components/detail-info-rows";
import { DetailTimeline } from "@/features/remittance/components/detail-timeline";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import { formatCurrency } from "@/lib/format";
import type { RemittanceResponse, RemittanceTimelineResponse } from "@/types/api";
import { DetailHeader } from "./detail-header";

interface DetailPageProps {
  params: Promise<{ remittanceId: string }>;
}

export default async function DetailPage({ params }: DetailPageProps) {
  const { remittanceId } = await params;
  const token = await requireAuth();

  const [remittance, timeline] = await Promise.all([
    apiClient.get<RemittanceResponse>(`/api/remittances/${remittanceId}`, { token }),
    apiClient.get<RemittanceTimelineResponse>(`/api/remittances/${remittanceId}/timeline`, {
      token,
    }),
  ]);

  const displayName = remittance.recipientName ?? remittance.recipientPhone;
  const inrFormatted = formatCurrency(remittance.amountInr, "INR");

  return (
    <div className="flex flex-col gap-4 px-5 pt-2 pb-6">
      <DetailHeader />

      <div className="flex flex-col items-center gap-1 pt-2 pb-2">
        <span className="text-xs tracking-[0.14em] uppercase text-fg-3">You sent</span>
        <div className="font-mono text-[40px] leading-none text-fg-1">
          <span className="text-[28px] text-fg-3">$</span>
          {Number.parseFloat(remittance.amountUsdc).toFixed(2)}
        </div>
        <span className="text-sm text-fg-3">
          to {displayName} · {inrFormatted}
        </span>
        <div className="mt-2">
          <StatusBadge status={remittance.status} />
        </div>
      </div>

      <DetailTimeline timeline={timeline} remittance={remittance} />

      <DetailInfoRows
        remittanceId={remittance.remittanceId}
        escrowPda={remittance.escrowPda}
        fxRate={remittance.fxRate}
      />
    </div>
  );
}
