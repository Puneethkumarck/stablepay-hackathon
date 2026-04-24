"use client";

import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

interface SendFlowState {
  amountUsdc: string;
  recipientPhone: string;
  recipientName: string;
  fxRate: string | null;
  fxRateExpiresAt: string | null;
  setAmount: (amount: string) => void;
  setRecipient: (phone: string, name?: string) => void;
  setFxRate: (rate: string, expiresAt: string) => void;
  reset: () => void;
}

const INITIAL_STATE = {
  amountUsdc: "",
  recipientPhone: "",
  recipientName: "",
  fxRate: null,
  fxRateExpiresAt: null,
} as const;

export const useSendFlowStore = create<SendFlowState>()(
  persist(
    (set) => ({
      ...INITIAL_STATE,
      setAmount: (amount: string) => set({ amountUsdc: amount }),
      setRecipient: (phone: string, name?: string) =>
        set({ recipientPhone: phone, recipientName: name ?? "" }),
      setFxRate: (rate: string, expiresAt: string) =>
        set({ fxRate: rate, fxRateExpiresAt: expiresAt }),
      reset: () => set({ ...INITIAL_STATE }),
    }),
    {
      name: "stablepay-send-flow",
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);
