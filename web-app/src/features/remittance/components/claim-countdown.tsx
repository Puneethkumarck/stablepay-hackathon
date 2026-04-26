"use client";

import { useEffect, useState } from "react";

interface ClaimCountdownProps {
  expiresAt: string;
}

function computeRemaining(expiresAt: string): string {
  const diff = new Date(expiresAt).getTime() - Date.now();
  if (diff <= 0) return "Expired";

  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  return `Expires in ${hours}h ${minutes}m`;
}

export function ClaimCountdown({ expiresAt }: ClaimCountdownProps) {
  const [text, setText] = useState("");

  useEffect(() => {
    setText(computeRemaining(expiresAt));
    const interval = setInterval(() => {
      setText(computeRemaining(expiresAt));
    }, 60_000);
    return () => clearInterval(interval);
  }, [expiresAt]);

  return <span>{text}</span>;
}
