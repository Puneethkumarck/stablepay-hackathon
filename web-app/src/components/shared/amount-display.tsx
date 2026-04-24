import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";

interface AmountDisplayProps {
  amount: string;
  currency: "USD" | "INR";
  size?: "lg" | "xl";
}

export function AmountDisplay({ amount, currency, size = "lg" }: AmountDisplayProps) {
  const formatted = formatCurrency(amount, currency);
  const symbol = currency === "USD" ? "$" : "₹";
  const numericPart = formatted.replace(/^[₹$]/, "");

  return (
    <div
      className={cn(
        "font-mono tabular-nums tracking-tight text-fg-1",
        size === "xl" ? "text-[56px] leading-none" : "text-[40px] leading-none",
      )}
    >
      <span className={cn("text-fg-3", size === "xl" ? "text-[36px]" : "text-[28px]")}>
        {symbol}
      </span>
      {numericPart}
    </div>
  );
}
