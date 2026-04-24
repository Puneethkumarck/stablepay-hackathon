"use client";

import { Copy } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { NETWORK_FEE } from "@/lib/constants";
import { truncateAddress } from "@/lib/format";
import { cn } from "@/lib/utils";

interface DetailInfoRowsProps {
  remittanceId: string;
  escrowPda: string;
  fxRate: string;
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      toast.success("Copied to clipboard");
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast.error("Failed to copy");
    }
  }

  return (
    <button
      type="button"
      onClick={handleCopy}
      className="flex items-center gap-1.5 text-sm font-mono text-fg-2 hover:text-fg-1 transition-colors"
      aria-label="Copy remittance ID"
    >
      {truncateAddress(text)}
      <Copy size={12} className={cn("text-fg-3", copied && "text-success")} />
    </button>
  );
}

export function DetailInfoRows({ remittanceId, escrowPda, fxRate }: DetailInfoRowsProps) {
  return (
    <div className="flex flex-col rounded-2xl bg-surface-2">
      <div className="flex items-center justify-between px-4 py-3 border-b border-border-1">
        <span className="text-sm text-fg-3">Remittance ID</span>
        <CopyButton text={remittanceId} />
      </div>
      <div className="flex items-center justify-between px-4 py-3 border-b border-border-1">
        <span className="text-sm text-fg-3">Escrow PDA</span>
        <span className="text-sm font-mono text-fg-2">{truncateAddress(escrowPda)}</span>
      </div>
      <div className="flex items-center justify-between px-4 py-3 border-b border-border-1">
        <span className="text-sm text-fg-3">On-chain fee</span>
        <span className="text-sm font-mono text-success">${NETWORK_FEE}</span>
      </div>
      <div className="flex items-center justify-between px-4 py-3">
        <span className="text-sm text-fg-3">FX rate</span>
        <span className="text-sm font-mono text-fg-2">{fxRate}</span>
      </div>
    </div>
  );
}
