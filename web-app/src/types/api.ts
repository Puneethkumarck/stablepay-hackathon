export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse | null;
  wallet: WalletResponse | null;
}

export interface UserResponse {
  id: string;
  email: string;
  name: string | null;
  createdAt: string;
}

export interface WalletResponse {
  id: number;
  solanaAddress: string;
  availableBalance: string;
  totalBalance: string;
  createdAt: string;
  updatedAt: string;
}

export interface RemittanceResponse {
  id: number;
  remittanceId: string;
  recipientPhone: string;
  amountUsdc: string;
  amountInr: string;
  fxRate: string;
  status: RemittanceStatus;
  escrowPda: string;
  claimTokenId: string;
  smsNotificationFailed: boolean;
  createdAt: string;
  updatedAt: string;
  expiresAt: string;
  recipientName: string | null;
}

export type RemittanceStatus =
  | "INITIATED"
  | "ESCROWED"
  | "CLAIMED"
  | "DELIVERED"
  | "DISBURSEMENT_FAILED"
  | "DEPOSIT_FAILED"
  | "CLAIM_FAILED"
  | "REFUND_FAILED"
  | "REFUNDED"
  | "CANCELLED";

export interface CreateRemittanceRequest {
  recipientPhone: string;
  amountUsdc: string;
  recipientName?: string;
}

export interface FxRateResponse {
  rate: string;
  source: string;
  timestamp: string;
  expiresAt: string;
}

export interface RemittanceTimelineResponse {
  steps: TimelineStep[];
  failed: boolean;
}

export interface TimelineStep {
  step: RemittanceStatus;
  status: TimelineStepStatus;
  message: string;
  completedAt: string | null;
}

export type TimelineStepStatus = "COMPLETED" | "CURRENT" | "PENDING" | "FAILED";

export interface FundingOrderResponse {
  fundingId: string;
  walletId: number;
  amountUsdc: string;
  status: FundingStatus;
  stripePaymentIntentId: string;
  stripeClientSecret?: string;
  createdAt: string;
}

export type FundingStatus =
  | "PAYMENT_CONFIRMED"
  | "FUNDED"
  | "FAILED"
  | "REFUND_INITIATED"
  | "REFUNDED"
  | "REFUND_FAILED";

export interface ClaimResponse {
  remittanceId: string;
  senderDisplayName: string;
  amountUsdc: string;
  amountInr: string;
  fxRate: string;
  status: RemittanceStatus;
  claimed: boolean;
  expiresAt: string;
}

export interface RecentRecipient {
  name: string;
  phone: string;
  lastSentAt: string;
}

export interface ErrorResponse {
  errorCode: string;
  message: string;
  timestamp: string;
  path: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
