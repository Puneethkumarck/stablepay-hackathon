const FEED_ITEMS = [
  { amt: "$200", recv: "₹16,900", time: "12s ago" },
  { amt: "$75", recv: "₹6,337", time: "1m ago" },
  { amt: "$500", recv: "₹42,250", time: "3m ago" },
  { amt: "$120", recv: "₹10,140", time: "5m ago" },
  { amt: "$350", recv: "₹29,575", time: "8m ago" },
];

export function LiveFeed() {
  return (
    <div>
      <div className="mb-2.5 flex items-center gap-1.5 font-mono text-[10px] uppercase tracking-widest text-fg-3">
        <span className="inline-block h-[5px] w-[5px] rounded-full bg-success shadow-[0_0_6px_var(--success)]" />
        Live transfers
      </div>
      <div className="flex flex-col gap-1.5">
        {FEED_ITEMS.map((item, i) => (
          <div
            key={item.amt}
            className="flex items-center justify-between rounded-[10px] border border-white/[0.07] bg-white/[0.04] px-3.5 py-2.5"
            style={{ opacity: 1 - i * 0.15 }}
          >
            <div className="flex items-center gap-2">
              <span className="text-sm">🇺🇸</span>
              <span className="text-xs text-fg-3">→</span>
              <span className="text-sm">🇮🇳</span>
              <span className="font-mono text-[13px] font-semibold text-fg-1">{item.amt}</span>
            </div>
            <div className="text-right">
              <div className="font-mono text-xs font-semibold text-green-300">{item.recv}</div>
              <div className="mt-0.5 text-[10px] text-fg-3">{item.time}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
