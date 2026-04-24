---
name: StablePay Design System
description: |
  Load this skill when designing any StablePay surface — the sender mobile app, the
  recipient SMS-link claim page, or the marketing site. StablePay is a Solana-native
  remittance product (USD→INR corridor) with a confident, technical, benefit-led
  voice. Default theme is dark navy; accent is the three-stop Solana gradient
  (teal → purple → magenta). Typography is Space Grotesk + JetBrains Mono. No
  emoji in product UI; numbers over adjectives; placeholders over invented content.
---

# StablePay Design System — Agent Skill

Use this skill any time you design, mock, or write code for a StablePay surface.
The full design rationale, content principles, and visual rules live in `README.md`
next to this file — read it fully before making decisions.

## Where to start

1. **Read `README.md`** end-to-end. It covers brand, voice/tone, casing, visual
   foundations (color, type, spacing, motion, borders, shadows, radii, layout),
   and iconography rules.
2. **Load `colors_and_type.css`** into whatever HTML file you produce. All tokens
   (colors, type scale, radii, shadows, solana-gradient, glow) are defined there
   as CSS variables. Do not hardcode hex values; reference `var(--token)`.
3. **Pick the right UI kit as a starting point:**
   - `ui_kits/sender_app/index.html` — iOS 26 mobile app (auth, home, send flow,
     transaction detail). Use this for any sender-side feature.
   - `ui_kits/recipient_web/index.html` — mobile-first SMS claim page (no wallet,
     no app). Use this for any recipient-side claim flow.
   - `ui_kits/marketing_web/index.html` — desktop landing page (hero + corridor
     viz + metrics + how-it-works + trust + footer). Use this for public site
     work.

## Non-negotiables

- **Voice:** confident, technical, benefit-led. Short declarative sentences.
  Quantify every claim. Never use "revolutionary", "seamless", "leverage",
  "game-changing", or exclamation marks. Direct address ("you"/"they").
- **No emoji in product UI.** Flag placeholders instead (`[SMS preview]`,
  `IMAGE`, `[UPI confirmation]`) rather than inventing imagery.
- **Typography:** Space Grotesk for everything; JetBrains Mono for amounts,
  addresses, tx IDs, and status codes. Numbers always use `tabular-nums`.
- **Color:** the three-stop Solana gradient is an *accent*, not a background.
  Reserve it for the wordmark "pay", primary CTAs, and live/active states.
  Never use flat purple alone.
- **Amounts:** `$100.00` and `₹8,450.00` (always two decimals). USDC is
  `100.00 USDC`, never `$100.00 USDC`.
- **Wordmark:** `stablepay` — lowercase, one word, "stable" white + "pay" in
  gradient. Never `StablePay` in running UI (that casing is only for prose).
- **Icons:** Lucide only, stroke 1.5-2px, `currentColor`. No mixing sets.
- **Radii:** pills 999px, buttons/inputs 12px, cards 20px. Never nest mismatched
  radii (a 20px card must not contain a 24px child).
- **Motion:** ease-out-expo for entrances (`cubic-bezier(0.16, 1, 0.3, 1)`),
  120/240/400ms durations, respect `prefers-reduced-motion`.

## What NOT to do

- Do not use stock gradient backgrounds (flat purple, generic blue-purple).
- Do not add hand-drawn SVG illustrations. Use schematic diagrams or labeled
  `IMAGE` placeholder boxes until real photography exists.
- Do not invent marketing claims with numbers. Use the repo's documented
  figures or clearly-marked placeholders.
- Do not add a seed-phrase screen, a buy-crypto flow, or any crypto-facing
  language on the recipient side — the whole point is "no crypto questions
  for your family".
- Do not add light-theme UI unless explicitly asked; dark is default.

## Tokens cheat sheet

```
Surfaces:      --surface-1 (#0B1020)   --surface-2 (#141A2E)   --surface-3 (#1C2340)
Text:          --fg-1 (white)   --fg-2 (0.72)   --fg-3 (0.48)   --fg-4 (0.32)
Borders:       --border-1 (0.08)   --border-2 (0.14)
Accent:        --accent (#9945FF)   --accent-soft   --accent-border
Gradient:      --solana-gradient (#00FFA3 → #9945FF → #DC1FFF)
Functional:    --success (#10B981)   --warning (#F59E0B)   --danger (#EF4444)   --info (#2775CA / USDC)
Radii:         --radius-sm 8   --radius-md 12   --radius-lg 20   --radius-pill 999
Shadows:       --shadow-1 / --shadow-2 / --shadow-3   --glow-solana   --glow-solana-soft
Fonts:         --font-sans (Space Grotesk)   --font-mono (JetBrains Mono)
```

## Example load

```html
<link rel="stylesheet" href="/path/to/colors_and_type.css">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
```

When in doubt, read `README.md` section-by-section — it's written for exactly
this purpose.
