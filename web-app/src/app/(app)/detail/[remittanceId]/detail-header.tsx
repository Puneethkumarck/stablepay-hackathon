"use client";

import { useRouter } from "next/navigation";
import { TopBar } from "@/components/shared/top-bar";

export function DetailHeader() {
  const router = useRouter();

  return (
    <TopBar title="Remittance" onBack={() => router.back()} right={<span className="w-9" />} />
  );
}
