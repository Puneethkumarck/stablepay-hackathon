# StablePay Design System

> Instant cross-border remittances on Solana. No seed phrases. No app for recipients. Guaranteed delivery.

StablePay is a consumer remittance app for the **USD → INR corridor**, built on USDC/Solana. The sender uses a mobile app; the recipient claims funds via an SMS link (no wallet, no app install). Under the hood: MPC wallet abstraction (no seed phrases), a custom Anchor escrow program, and Temporal durable workflows that guarantee every remittance reaches a terminal state — delivered, refunded, or failed.

This design system is a **proposed visual direction** synthesized from the product's story, existing hero/architecture PNGs, and Solana ecosystem conventions. The backend exists; the frontend does not. Everything here is inferred — please review and direct iteration.

---

## Sources

- **Codebase:** [github.com/Puneethkumarck/stablepay-hackathon](https://github.com/Puneethkumarck/stablepay-hackathon) — Spring Boot API, Rust/Anchor escrow program, Go MPC sidecar. No frontend code.
- **Visual anchors:** `docs/images/hero-banner.png`, `docs/images/stablepay-vs-traditional-remittance.png`, `docs/images/platform-architecture.png`, `docs/images/payment-lifecycle.png`, `docs/images/solana-escrow.png` — these are product marketing / architecture illustrations but establish the brand's color and typographic feel.
- **Product copy:** `README.md` (of the source repo), `docs/BRAINSTORM.md` — confident, technical, benefit-led voice.
- **No Figma.** No design tokens file in the repo. No existing React/React Native app.

---

## What's in this folder

- `README.md` — this file
- `SKILL.md` — skill manifest for Claude Code / agent use
- `colors_and_type.css` — CSS variables for all color and type tokens
- `fonts/` — web font files (with Google Fonts fallbacks flagged)
- `assets/` — logos, icons, product imagery copied from source repo
- `preview/` — per-token preview cards that populate the Design System tab
- `ui_kits/`
  - `sender_app/` — React Native-style mobile UI kit for the sender
  - `recipient_web/` — responsive web UI for the SMS-link recipient claim page
  - `marketing/` — marketing site recreation (landing, hero, feature grids)

---

## Brand & Product Overview

| Attribute | Value |
|---|---|
| **Product** | StablePay |
| **Wordmark** | `stablepay` — lowercase, two-tone ("stable" white, "pay" in purple→magenta gradient) |
| **Tagline** | *Instant cross-border remittances on Solana.* |
| **Primary corridor** | USD → INR (expand later to PHP, MXN) |
| **Users** | Indian diaspora in US sending to family in India |
| **Surfaces** | Sender mobile app · Recipient claim web page · Marketing site |
| **Chain** | Solana (devnet for hackathon) |

---

## Content Fundamentals

StablePay's voice is **confident, technical, and benefit-led** — directly borrowed from the source repo. It rewards literacy (crypto, fintech, engineering) but never assumes gatekeeping. It speaks to two very different audiences: senders who are tech-comfortable but not crypto-native, and recipients who may have zero crypto knowledge.

### Tone dimensions

- **Confident, not breathless.** Short declarative sentences. No "revolutionary" / "game-changing" buzzwords. When a claim is made, it's immediately quantified.
- **Proof-heavy.** Real numbers over adjectives. `$0.002 total on-chain fees`, `10/10 customers completing in 22–60 seconds`, `48h claim window`. Numbers appear inline in body copy, not just in charts.
- **Technical where it matters, plain-spoken where it doesn't.** The marketing hero says "No seed phrases." The architecture doc says "2-of-2 MPC DKG ceremony producing an Ed25519 keypair." Same product, different audience, both voices legit.
- **Direct address.** Uses "you" for the sender flow ("You send $100. They receive ₹8,450."). Uses "we" sparingly, mostly for company positioning.

### Casing

- **Wordmark:** `stablepay` — always lowercase, one word.
- **Product UI:** Title Case for screen titles ("Send Money", "Claim Your Funds"), sentence case for buttons and labels ("Send $100", "Enter your UPI ID"). Never ALL CAPS except on very short eyebrow labels (`STEP 2 OF 3` is OK; button labels never are).
- **Amounts:** `$100.00` and `₹8,450.00` — always two decimals for USD fiat, two decimals for INR over ₹100, zero decimals for small INR amounts. USDC shown as `100.00 USDC` (not `$100.00 USDC`).
- **Status strings:** UPPERCASE monospace in debug / developer contexts (`ESCROWED`, `DELIVERED`, `REFUNDED`), but rendered as sentence-case badges in user UI ("Escrowed", "Delivered").

### Emoji & symbols

- **No emoji in product UI.** StablePay is a financial product; emoji undermine trust. The one exception is the `⚡` bolt as a decorative accent in marketing contexts only (hero sections, speed claims), and even there it's drawn as an SVG inline icon, not the emoji codepoint.
- **Unicode arrows are OK** for flow diagrams (`→`, `↔`), but prefer SVG arrows in UI.
- **Checkmarks** are SVG icons with brand color, never ✅ emoji.

### Example copy

**Hero (marketing):**
> Send money home in under a minute.
> $0.002 total fees. Guaranteed delivery. No seed phrases.

**Send flow confirmation:**
> You send $100.00
> They receive ₹8,450.00
> Rate locked · 84.50 INR per USD

**Recipient claim page:**
> Raj sent you ₹8,450.00 via StablePay.
> Enter your UPI ID to receive it instantly.

**Error, insufficient balance:**
> Not enough USDC. You have $42.18 available; this remittance requires $100.00. [Add funds]

**What we DON'T write:**
- ~~"🚀 Transform your money transfers forever!"~~ (salesy, emoji, unquantified)
- ~~"Leverage blockchain technology for seamless transactions."~~ (jargon, no benefit)
- ~~"Simply click here to begin your journey!"~~ (empty words, infantilizing)

---

## Visual Foundations

The visual system is built to carry two feelings simultaneously: **Solana-native technical legitimacy** (deep navy, neon gradients, precise geometry) and **consumer-grade trust** (generous spacing, clear typographic hierarchy, no glitch/grunge aesthetic).

### Color

Three palettes work together:
1. **Surface** — deep navy to near-black. Most UI surfaces. Evokes the terminal / on-chain / "serious money" feel.
2. **Solana gradient** — the teal→purple→magenta three-stop Solana brand gradient. Used as an accent: wordmark "pay", primary CTAs, active states, and signature flourishes.
3. **Functional** — USDC blue, success green, warning amber, critical red. Semantic-only.

The default theme is **dark**. A light theme exists but is secondary (for daytime / printed-doc / onboarding contexts). When used on light surfaces, the Solana gradient saturates and darkens ~10% to retain contrast.

**Never use flat purple alone** — always the full gradient, or paired with another stop. A flat `#9945FF` panel looks off-brand.

### Typography

- **Display / UI sans:** **Space Grotesk** (Google Fonts) — slightly geometric, condensed-ish, feels fintech-modern without being generic. Used for wordmark, headlines, buttons, body.
- **Numeric / tabular:** **JetBrains Mono** (Google Fonts) — used for amounts, addresses, transaction IDs, status codes, anywhere alignment or monospace legibility matters. Amounts in particular are *always* set in JetBrains Mono with tabular-nums.
- **Body at small sizes:** also Space Grotesk (no second text family) — the system uses size, weight, and color to establish hierarchy, not additional families.

No serifs. No Inter. No system fonts.

### Spacing & rhythm

Built on a **4px base grid**. Scale: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96. Component internal padding is always on this scale; screen margins are 16 (mobile) / 24 (tablet) / 40 (desktop).

Vertical rhythm uses **24px as the default gap between content blocks** within a card, and **64px as the default gap between sections** on a marketing page. 12px is the default gap between label and input.

### Backgrounds

- **Primary surface:** deep navy `#0B1020`, nearly flat. Optional subtle grid texture (repeating 32px dotted grid at 4% opacity) — matches the repo's architecture diagrams.
- **Elevated cards:** one shade lighter (`#141A2E`) with a 1px border at 8% white.
- **Hero / feature sections:** full-bleed navy with a large, soft radial glow in Solana purple (`#9945FF` at 20% opacity, blurred 200px) positioned off-center. Used sparingly — one hero glow per page, max.
- **No illustrations.** The brand does not use hand-drawn illustration. When an image is needed, use architecture-style schematic diagrams (existing repo PNGs) or product-shot imagery (mobile phone renders, abstract flow arrows).
- **No photography** in the system yet — placeholder slots are geometric (labeled `IMAGE` boxes) until product/lifestyle photography exists.

### Animation

- **Easing:** `cubic-bezier(0.16, 1, 0.3, 1)` (ease-out-expo) for entrances and state changes. `cubic-bezier(0.4, 0, 1, 1)` (ease-in) for exits. No bouncy / spring-y overshoot.
- **Durations:** 120ms for hover / press / toggle micro-states; 240ms for card/modal transitions; 400ms for route changes; 600ms for gradient shimmer on live status.
- **Signature motion:** a slow gradient shimmer along the Solana-gradient on "live" elements (status badge while workflow is ESCROWED, wordmark on hover). Subtle — not decorative noise.
- **Fade + subtle rise** (12px translateY) is the default entrance. No slide-across-screen, no scale-from-zero.
- **Reduced-motion:** honor `prefers-reduced-motion`; replace all transitions with instant state changes except opacity fades <=120ms.

### Interactive states

- **Hover (buttons, primary):** brighten the gradient ~8% + 0→40% glow halo appears behind.
- **Hover (cards, list rows):** border `rgba(255,255,255,0.08)` → `rgba(255,255,255,0.16)`; no scale, no translate.
- **Press:** scale 0.98, glow halo dims. 80ms.
- **Focus (keyboard):** 2px outline in Solana purple `#9945FF`, 2px offset. Never remove focus rings.
- **Disabled:** 40% opacity, `cursor: not-allowed`, no hover state.

### Borders

- **Default divider:** 1px, `rgba(255,255,255,0.08)`.
- **Emphasized divider (section break):** 1px, `rgba(255,255,255,0.14)`.
- **Card border:** 1px, `rgba(255,255,255,0.08)` at rest, `rgba(255,255,255,0.16)` on hover.
- **Input border:** 1px, `rgba(255,255,255,0.14)` default; Solana purple `#9945FF` on focus.
- **No gradient borders** except on the primary CTA button and on the "live" status ring around the hero logomark.

### Shadows & elevation

Shadow system is subtle because most surfaces are already dark-on-dark. Elevation is mostly implied via border brightening and glow.

- `--shadow-1`: `0 1px 2px rgba(0,0,0,0.4)` — sits-on-surface elements.
- `--shadow-2`: `0 8px 24px rgba(0,0,0,0.4)` — popovers, menus.
- `--shadow-3`: `0 24px 64px rgba(0,0,0,0.5)` — modals, full-screen overlays.
- `--glow-solana`: `0 0 48px rgba(153,69,255,0.35)` — reserved for primary CTAs and the live-status ring. Use sparingly.

### Protection gradients vs capsules

When text must sit over a busy background (e.g. an SMS-share card over the architecture diagram), prefer a **capsule** (rounded-rect bg with 90% opacity and 8% white border) rather than a vertical gradient. Protection gradients are reserved for full-bleed hero imagery where a dark-to-transparent vertical fade from bottom makes text legible.

### Transparency & blur

- **Modals:** backdrop is `rgba(11,16,32,0.72)` with `backdrop-filter: blur(12px)`. The modal surface itself is opaque.
- **Toolbars / floating nav:** `rgba(20,26,46,0.72)` with `blur(16px)`.
- **Never blur body content** behind a non-modal surface; too distracting.

### Corner radii

- **Pills / chips / status badges:** `999px` (full round).
- **Buttons:** `12px`.
- **Inputs, small cards:** `12px`.
- **Large cards, modals:** `20px`.
- **Full-bleed sections:** `0px`.
- **Avatars:** `999px`.

Corner radii are consistent within a component family. A card with 20px radius never contains a child card with 24px radius.

### Card anatomy

A default card is:
- background `#141A2E`
- 1px border `rgba(255,255,255,0.08)`
- radius `20px`
- padding `24px` (mobile) / `32px` (desktop)
- no drop shadow at rest; `--shadow-2` only when raised (modal, popover)
- on hover: border brightens; no scale, no lift

A "highlighted" card (e.g. primary plan, featured remittance) gets a **1.5px Solana-gradient border** via a padded wrapper trick, plus a `--glow-solana` halo at 60% intensity.

### Layout rules

- **Fixed elements:** mobile tab bar (bottom, 64px tall, `backdrop-filter: blur(16px)`), marketing top nav (translucent, 72px). Everything else scrolls.
- **Max content width (marketing):** `1200px` centered, 40px gutter.
- **Max content width (app surfaces):** `480px` (mobile frame) or `640px` (desktop view of mobile flow, e.g. recipient claim page).
- **Gutter grid:** 12 columns, 24px gutter, desktop only. Mobile uses a single-column stack with 16px outer margin.

### What to avoid

- Bluish-purple-only gradient backgrounds (the "Midjourney default"). If a purple is used, pair it with the full Solana tri-stop gradient, not alone.
- Glass-morphism stacks with 5+ layered frosted panels.
- Glitch / grunge / terminal-hacker aesthetic (even though the product is on-chain).
- Emoji in UI.
- Left-colored-border card variants.
- SVG hand-drawn squiggles.

---

## Iconography

StablePay uses **Lucide** (CDN, v0.400+) for all UI icons — clean 1.5px stroke, geometrically consistent, widely covered set. Lucide was chosen because it is:
- Open-source (ISC license), CDN-available
- Geometrically consistent with Space Grotesk's slight geometric tilt
- Has all the financial/transaction icons we need (arrow-left-right, wallet, lock, clock, check-circle, alert-triangle, copy, qr-code)

Usage rules:
- **Stroke only**, never filled. 1.5px at all sizes 16-24px; 2px for ≥32px.
- **Sizes:** 16px inline with body text, 20px in buttons, 24px in navigation, 32px in empty-state callouts.
- **Color:** inherit `currentColor`. Semantic colors (success / warn / error) only on status icons.
- **Never use two icon families** — no mixing Lucide + Heroicons + emoji.

### Brand marks (copied into `assets/`)

- `assets/logo-wordmark.svg` — inferred StablePay wordmark: "stable" in white, "pay" in Solana gradient. Derived from the repo's hero banner.
- `assets/logo-mark.svg` — inferred "S" monogram with gradient ring. Derived.
- `assets/usdc-mark.svg` — inline USDC circle logo (blue `#2775CA`).
- `assets/solana-mark.svg` — inline Solana wordmark gradient.
- `assets/rupee-mark.svg` — inline ₹ glyph in orange `#FF9933` (from hero banner).
- `assets/hero-banner.png`, `assets/platform-architecture.png`, `assets/stablepay-vs-traditional.png`, `assets/payment-lifecycle.png`, `assets/solana-escrow.png` — from the source repo, used in marketing site recreation.

### Emoji

Not used in product UI. Marketing materials occasionally use a `⚡` drawn as SVG icon (not the emoji).

### Substitutions flagged

- **StablePay logo** — I do not have an official vector file. The wordmark and monogram in `assets/` are recreations from the hero banner. **Please provide the real vector logo files.**
- **Custom fonts** — Space Grotesk and JetBrains Mono are Google Fonts; both are loaded from the Google Fonts CDN. If you have paid/licensed display fonts, flag and I'll swap.
- **Solana and USDC marks** — I have reconstructed these from public branding. Replace with official assets from Solana Foundation and Circle respectively for production use.

---

## Index

| File | Purpose |
|---|---|
| `README.md` | This document — overview, fundamentals, index |
| `SKILL.md` | Agent-skill manifest (Claude Code compatible) |
| `colors_and_type.css` | Tokenized CSS: colors, type scale, spacing, radii, shadows |
| `fonts/` | Google Fonts `@import` URLs + any local ttf/woff2 |
| `assets/` | Logos, icons, product imagery |
| `preview/` | Per-token/component cards, registered to Design System tab |
| `ui_kits/sender_app/` | Mobile sender app UI kit (React + iOS frame) |
| `ui_kits/recipient_web/` | Recipient SMS claim web page UI kit |
| `ui_kits/marketing/` | Marketing site UI kit (hero, features, footer) |

---

## Caveats & Open Questions

- **No source Figma or frontend code existed.** Every visual decision here is inferred from hero banners and product copy. Treat this as v0 — expect real iteration once you review.
- **The wordmark and monogram are recreations**, not authoritative files. Please replace with originals.
- **Solana and USDC marks need official vector files.**
- **No real product photography or illustration.** Imagery placeholders are used throughout.
- **Light theme is defined but not exercised** — all UI kits are dark-first. If you need a light-theme pass (e.g. for onboarding / docs), tell me.
- **Typography stack** uses Google Fonts. Swap if you have licensed faces.
