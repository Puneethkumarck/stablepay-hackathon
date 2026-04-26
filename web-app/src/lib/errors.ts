const ERROR_MESSAGES: Record<string, string> = {
  "SP-0001": "Wallet not found. Please try again.",
  "SP-0002": "Insufficient balance. Add funds to continue.",
  "SP-0003": "FX rate expired. We'll get a fresh quote.",
  "SP-0004": "Remittance not found.",
  "SP-0005": "Invalid phone number. Please check and try again.",
  "SP-0006": "Invalid ID token. Please sign in again.",
  "SP-0007": "Email not verified. Please verify your Google email.",
  "SP-0008": "Unsupported auth provider.",
  "SP-0009": "Wallet already exists.",
  "SP-0010": "Remittance not found.",
  "SP-0011": "Claim token not found or expired.",
  "SP-0012": "Claim token already used.",
  "SP-0013": "Invalid refresh token. Please sign in again.",
  "SP-0014": "Unsupported corridor.",
  "SP-0015": "User not found.",
  "SP-0020": "Funding order not found.",
  "SP-0021": "Funding failed. Please try again.",
  "SP-0022": "A funding order is already in progress.",
  "SP-9999": "Something went wrong. Please try again.",
};

export function getErrorMessage(code: string): string {
  return ERROR_MESSAGES[code] ?? ERROR_MESSAGES["SP-9999"] ?? "Something went wrong.";
}
