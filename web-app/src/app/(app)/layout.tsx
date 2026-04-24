import { BottomNav } from "@/components/shared/bottom-nav";
import { requireAuth } from "@/lib/auth";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  await requireAuth();

  return (
    <div className="flex min-h-screen flex-col pb-16">
      <main className="flex-1">{children}</main>
      <BottomNav />
    </div>
  );
}
