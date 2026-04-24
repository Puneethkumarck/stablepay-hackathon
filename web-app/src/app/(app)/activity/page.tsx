import { TopBar } from "@/components/shared/top-bar";
import { ActivityList } from "@/features/remittance/components/activity-list";
import { apiClient } from "@/lib/api-client";
import { requireAuth } from "@/lib/auth";
import type { PageResponse, RemittanceResponse } from "@/types/api";

export default async function ActivityPage() {
  const token = await requireAuth();

  const remittancePage = await apiClient.get<PageResponse<RemittanceResponse>>(
    "/api/remittances?sort=createdAt,desc",
    { token },
  );

  return (
    <div className="flex flex-col gap-4 px-5 pt-2 pb-6">
      <TopBar title="Activity" />
      <ActivityList remittances={remittancePage.content} />
    </div>
  );
}
