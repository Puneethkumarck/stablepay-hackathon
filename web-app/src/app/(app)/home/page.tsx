import { Button } from "@/components/ui/button";

export default function HomePage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-6 p-8">
      <h1 className="text-2xl font-bold text-fg-1">
        stable
        <span className="bg-gradient-to-r from-solana-teal via-solana-purple to-solana-magenta bg-clip-text text-transparent">
          pay
        </span>
      </h1>
      <p className="text-fg-3 text-center text-sm">Real-time remittances on Solana</p>
      <Button>Get Started</Button>
      <div className="flex gap-3 text-xs font-mono text-fg-3">
        <span className="text-solana-teal">teal</span>
        <span className="text-solana-purple">purple</span>
        <span className="text-solana-magenta">magenta</span>
      </div>
      <div className="flex gap-2">
        <div className="w-12 h-8 rounded-md bg-surface-2 border border-border-1" />
        <div className="w-12 h-8 rounded-md bg-surface-3 border border-border-2" />
        <div className="w-12 h-8 rounded-md bg-surface-4 border border-border-3" />
      </div>
    </div>
  );
}
