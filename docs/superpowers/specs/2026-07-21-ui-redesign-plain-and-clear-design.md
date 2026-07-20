# UI Redesign: Plain & Clear (Light, Corporate Clean)

**Date:** 2026-07-21
**Status:** Approved (pending spec review)
**Scope:** Client only

## Goal

Replace the current "AIish" aesthetic (dark mode + radial-gradient dot pattern, purple + gold color scheme, glassmorphism, glow shadows, gradient text, excessive framer-motion flourishes) with a plain, clear, corporate-clean light UI in the style of Google Workspace / Linear / Stripe. The app should read as a normal business web app, not an AI demo.

## Decisions

| Decision | Choice |
|---|---|
| Visual direction | Light, corporate clean |
| Accent color | Blue (`#2563EB`) |
| Motion policy | Minimal (remove gimmicks, keep route fade + functional Timer ring) |

## Non-goals

- No layout / information-architecture changes (same routes, same component tree, same data flow).
- No new components. No new dependencies.
- No copy/text changes except replacing the animated logo with a plain wordmark.
- No backend changes.
- No new tests (visual change only; existing Vitest suites cover behavior).

## Token system

Replaces the `@theme` block in `client/src/index.css`:

| Token | Value | Replaces |
|---|---|---|
| `--color-bg` | `#FFFFFF` | `--color-dark` (#0D0C22) |
| `--color-surface` | `#F9FAFB` (gray-50) | glass-card slate-900/40 |
| `--color-surface-2` | `#F3F4F6` (gray-100) | purple-950/50 inputs |
| `--color-border` | `#E5E7EB` (gray-200) | glass-border white |
| `--color-text-main` | `#111827` (gray-900) | #E2E8F0 |
| `--color-text-muted` | `#6B7280` (gray-500) | #94A3B8 |
| `--color-primary` | `#2563EB` (blue-600) | #7C3AED purple |
| `--color-primary-hover` | `#1D4ED8` (blue-700) | — |
| `--color-danger` | `#DC2626` (red-600) | — |

Dropped tokens: `--color-secondary` (gold), `--color-gold`, `--color-glass-border`. The `--color-primary` name is kept so existing `text-primary` / `bg-primary` Tailwind classes continue to resolve.

### CSS cleanup (`index.css`)

- Remove `.glass` and `.glass-card` utilities.
- Remove the radial-gradient dot-pattern body background.
- Remove the custom scrollbar styling (purple hover).
- Update `@theme` to the new token values above; drop the deleted tokens.
- Body: `background-color: var(--color-bg); color: var(--color-text-main); font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;`.

## Surface & border rules

- **Radius scale:** `rounded-lg` (8px) for cards, inputs, buttons, table containers. `rounded-full` only for the Timer SVG container and the bot avatar circle. Drop all `rounded-2xl` / `rounded-3xl`.
- **Cards / panels:** `bg-white border border-[--color-border] rounded-lg shadow-sm`. No `backdrop-blur`, no semi-transparent fills.
- **Inputs:** `bg-white border border-[--color-border] rounded-lg focus:ring-2 focus:ring-primary focus:border-primary`. No purple-950 fills.
- **Tables (Dashboard):** `bg-white border border-[--color-border] rounded-lg divide-y divide-[--color-border]`. Header row `bg-[--color-surface] text-[--color-text-muted] text-xs uppercase tracking-wide font-medium`. Rows `hover:bg-[--color-surface]`. No motion stagger.
- **Buttons (primary):** `bg-primary hover:bg-primary-hover text-white rounded-lg font-medium`. No `whileHover`/`whileTap` spring.
- **Buttons (secondary):** `bg-[--color-surface-2] hover:bg-[--color-border] text-[--color-text-main] rounded-lg font-medium`.
- **ScoreBadge:** `bg-green-100 text-green-800`, `bg-yellow-100 text-yellow-800`, `bg-red-100 text-red-800` (solid pastels, not /20 opacity on dark). Keep the same >=7 / >=4 / else thresholds.

## Motion policy

**Remove:**
- `StretchCard.jsx` (mouse-stretch gimmick) — delete the file.
- `UnfoldOnScroll.jsx` (decorative scroll-triggered scaleY unfold) — delete the file.
- `AnimatedText.jsx` (gradient text + char animation for the logo) — delete the file.
- Infinite logo pulse (scale + rotate, `repeat: Infinity`).
- `animate-bounce` on the UploadCloud icon in NewInterview.
- Spring scale on every button (`whileHover={{scale:1.05}} whileTap={{scale:0.95}}`).
- Gradient text on the logo (`bg-gradient-to-r from-secondary to-purple-400 bg-clip-text text-transparent`).
- Staggered list animations in Dashboard (`motion.tr` with `transition delay`) — use plain `<tr>`.

**Keep:**
- Route transition fade in `PageWrapper` (opacity 0→1, y 8→0, 0.2s ease-out). Drop the exit y:-20 — just fade out.
- `AnimatePresence` for step transitions in NewInterview (positional slide, `tween` ease 0.2s, not spring).
- `motion.circle` on Timer (functional — animates the countdown ring).

**Replace:**
- Button hover/active states via plain Tailwind classes (`hover:bg-primary-hover active:bg-primary`), not framer-motion.

## Page-by-page changes

### `App.jsx`
- Header: `bg-white border-b border-[--color-border] py-3 px-6`. Drop `bg-purple-950/40`.

### `components/Logo.jsx`
- Drop the Bot icon box, infinite animation, and gradient text.
- Replace with a plain wordmark: `text-xl font-semibold text-[--color-text-main]` reading "AI Interviewer".
- `handleHome` → use `useNavigate` + `navigate('/')` (fixes existing `window.location.href` issue).
- Remove `AnimatedText` import. Remove `motion` import if no longer used.
- Remove `Bot` icon import from lucide-react if no longer used.

### `pages/Home.jsx`
- Tab bar container: `bg-[--color-surface] rounded-lg p-1 inline-flex gap-1`.
- Active tab: `bg-white shadow-sm text-[--color-text-main] font-medium rounded-md`. Icon in `text-primary`.
- Inactive tab: `text-[--color-text-muted] hover:text-[--color-text-main]`. Icon in `text-[--color-text-muted]`.
- Drop `bg-purple-900`, `text-gold`, `.glass` usage.
- Keep `UsersRoundIcon` + `LayoutDashboard` from lucide-react.

### `pages/NewInterview.jsx`
- Upload box: `border-2 border-dashed border-[--color-border] rounded-lg p-10 bg-[--color-surface] hover:border-primary text-center`. Drop `bg-purple-950/80`, `animate-bounce`, `whileHover scale 1.05`. UploadCloud icon: static, `text-[--color-text-muted]`.
- Form inputs: per surface rules (`bg-white border border-[--color-border] rounded-lg focus:ring-2 focus:ring-primary focus:border-primary`). Drop `bg-purple-950/50`, `rounded-2xl`, `focus:ring-primary` color stays.
- Submit button: `bg-primary hover:bg-primary-hover text-white rounded-lg px-4 py-2 font-medium`. No spring.
- Toaster: remove the hardcoded `{background:'#1a1a1a',color:'#fff'}` style object — let react-hot-toast use defaults.
- Step transition: keep `AnimatePresence` for the slide, but use `tween` ease 0.2s, not spring.

### `pages/IntervieweeChat.jsx`
- Chat container: `bg-white border border-[--color-border] rounded-lg`. Drop `bg-purple-600/20 shadow-fuchsia-950/50`.
- Bot avatar: `bg-[--color-surface-2] text-[--color-text-muted] rounded-full w-8 h-8 flex items-center justify-center text-sm font-medium` with a single letter "AI" or "I". Drop the Bot icon.
- Bot bubble: `bg-[--color-surface] text-[--color-text-main] rounded-lg rounded-tl-none`. Drop `bg-gold/30`.
- User bubble: `bg-primary text-white rounded-lg rounded-tr-none`. (Was already `bg-primary` — keep the color, change the radius from rounded-xl to rounded-lg.)
- Send button: `bg-primary hover:bg-primary-hover rounded-lg` — no spring.
- Input: `bg-white border border-[--color-border] rounded-lg focus:ring-2 focus:ring-primary`. Drop `bg-slate-950/40 rounded-2xl`.
- Difficulty pill: `bg-blue-50 text-primary text-xs font-medium rounded-md px-2 py-0.5`. Drop `text-secondary bg-primary/20 rounded-full`.
- Completed Home button: `bg-primary text-white rounded-lg`. Drop `bg-secondary text-dark shadow-secondary/20`.
- Timer panel: move from the floating `fixed w-[20vw] my-[15%] right-0` position into a normal flex item in the chat header: `bg-white border border-[--color-border] rounded-lg p-4 shadow-sm`.

### `components/Dashboard.jsx`
- Table container: `bg-white border border-[--color-border] rounded-lg divide-y divide-[--color-border]`.
- Header row: `bg-[--color-surface] text-[--color-text-muted] text-xs uppercase tracking-wide font-medium`.
- Rows: `hover:bg-[--color-surface]`. Plain `<tr>` — drop `motion.tr` + stagger.
- ScoreBadge: per surface rules (solid pastels).
- Drop `bg-purple-950 rounded-3xl`, `bg-purple-800`, `text-gold`, `border-glass-border/50`, `hover:bg-primary/40`.

### `components/Timer.jsx`
- Keep the SVG ring + `motion.circle` (functional).
- Track: `text-[--color-surface-2]` (was `text-purple-900/50`).
- Progress: `text-primary` (was `text-primary` — now blue, was purple).
- MM:SS text: `text-[--color-text-main] font-medium` (was white).

### `components/WelcomeBackModal.jsx`
- Backdrop: `bg-black/40`. Drop `backdrop-blur-sm`.
- Panel: `bg-white border border-[--color-border] rounded-lg shadow-xl`. Drop `glass-card`.
- Title: `text-[--color-text-main] text-lg font-semibold`. Drop `text-secondary`.
- Primary button: `bg-primary hover:bg-primary-hover text-white rounded-lg`. Drop `bg-primary/80`.
- Secondary button: `bg-[--color-surface-2] hover:bg-[--color-border] text-[--color-text-main] rounded-lg`. Drop `bg-slate-700 hover:bg-slate-600`.

### `components/PageWrapper.jsx`
- Simplify: `opacity: 0 → 1`, `y: 8 → 0` (was 20), `duration: 0.2`, `ease: "easeOut"`.
- Drop the exit `y:-20` animation — just fade out (`opacity: 1 → 0`, same duration).
- `container mx-auto px-4 py-8` stays.

### `pages/CandidateDetail.jsx`
- Header card: `bg-white border border-[--color-border] rounded-lg p-6`. Drop `bg-primary/30 rounded-2xl`.
- Final Score box: `bg-[--color-surface] border border-[--color-border] rounded-lg p-4` with `text-3xl font-semibold text-[--color-text-main]`. Drop `bg-purple-300/30 border-primary/40 rounded-xl text-secondary text-5xl`.
- Section headers: `text-[--color-text-main] font-semibold`. Drop `text-secondary`.
- Analysis cards: `bg-white border border-[--color-border] rounded-lg p-4`. Drop `bg-slate-500/40 rounded-xl`. Remove `<StretchCard>` wrappers — render cards directly.
- Remove `<UnfoldOnScroll>` wrapper around the transcript section — render it directly.
- Skill bars: track `bg-[--color-surface-2]`, fill `bg-primary`. Drop `bg-glass-border` track, `bg-primary` fill (color changes from purple to blue via the token).
- Pie chart Tooltip: `bg-white border border-[--color-border] rounded shadow-sm text-[--color-text-main]`. Drop `rgba(26,26,26,0.9)`.
- Transcript Q: `text-[--color-text-muted] font-medium`. Drop `text-secondary`.
- Transcript A: `bg-[--color-surface] border-l-2 border-primary p-3`. Drop `bg-white/20 border-l-2 border-glass-border`.
- Remove imports: `StretchCard`, `UnfoldOnScroll`.

## Toaster
- Remove the hardcoded style object in `NewInterview.jsx` (`{background:'#1a1a1a',color:'#fff'}`).
- Let react-hot-toast use its default light theme (white bg, dark text, subtle shadow).
- If a `<Toaster>` is mounted elsewhere, ensure it uses defaults (no custom style prop).

## File deletions

- `client/src/components/StretchCard.jsx`
- `client/src/components/UnfoldOnScroll.jsx`
- `client/src/components/AnimatedText.jsx`

## Verification

1. `npm run lint` — 0 errors, 0 warnings.
2. `npm run build` — succeeds.
3. `npm run test` — all existing Vitest suites pass. **Caveat:** any test that asserts on specific Tailwind class names (e.g. `bg-purple-950`, `text-gold`, `glass`) will fail and must be updated to the new class names. Audit affected tests during implementation and update them. No new tests are required.

## Risks / trade-offs

- **Test churn:** Tests that assert on class names will break. Mitigation: update them as part of the same change; prefer assertions on text content / roles over class names where possible.
- **Token rename safety:** Keeping `--color-primary` as the token name (just changing its value from purple to blue) means existing `bg-primary` / `text-primary` classes keep resolving — minimizes class-name churn across files. Gold/secondary classes (`text-secondary`, `bg-gold`) have no replacement and must be explicitly rewritten to the new tokens.
- **Framer-motion still a dependency:** We're not removing the library (PageWrapper, NewInterview step transition, Timer ring still use it). If the user later wants zero motion, a follow-up can remove it entirely.
- **No visual mockups produced:** The user declined the visual companion; design is specified in prose + token tables. Implementation will be verified by build + lint + test, plus a manual smoke test if the user wants to run the dev server.
