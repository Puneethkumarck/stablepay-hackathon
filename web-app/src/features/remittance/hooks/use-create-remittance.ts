"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { CreateRemittanceRequest, RemittanceResponse } from "@/types/api";

export class CreateRemittanceError extends Error {
  readonly errorCode: string;

  constructor(errorCode: string, message: string) {
    super(message);
    this.name = "CreateRemittanceError";
    this.errorCode = errorCode;
  }
}

async function createRemittance(request: CreateRemittanceRequest): Promise<RemittanceResponse> {
  const response = await fetch("/api/remittances", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({
      errorCode: "SP-9999",
      message: "Request failed",
    }));
    throw new CreateRemittanceError(body.errorCode, body.message);
  }

  return response.json() as Promise<RemittanceResponse>;
}

export function useCreateRemittance() {
  const queryClient = useQueryClient();

  return useMutation<RemittanceResponse, CreateRemittanceError, CreateRemittanceRequest>({
    mutationFn: createRemittance,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["remittances"] });
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
    },
  });
}
