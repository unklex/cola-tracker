# Design System Specification: The Effervescent Archive

## 1. Overview & Creative North Star
This design system is built to transform a utilitarian tracking utility into a premium family experience. Our Creative North Star is **"The Effervescent Archive."** 

We are moving away from the "standard app" aesthetic—characterized by rigid grids and heavy borders—toward an editorial layout that feels as light as carbonation. We achieve this through **intentional asymmetry**, where large `display-lg` typography pushes against the edges of the frame, and **overlapping surfaces** that create a sense of tactile depth. The goal is a digital environment that feels like a high-end lifestyle magazine: clean, spacious, and undeniably sophisticated, yet warm enough for a family to call home.

## 2. Colors: The Tonal Palette
The palette is a curated balance of high-energy "Cola" reds and grounding, soft neutrals.

*   **The Primary Core:** Use `primary` (#ba0012) for moments of peak interaction. This isn't just a color; it’s the "fizz" of the brand. Use `primary_container` (#ff766a) for large, welcoming surfaces that require a softer touch than the pure brand red.
*   **The Tertiary Accent:** `tertiary` (#7842a5) provides a sophisticated counter-point to the reds, used for secondary tracking metrics or "special" family milestones.
*   **The "No-Line" Rule:** We do not use 1px solid borders to define sections. Period. Boundaries must be defined solely through background color shifts. For example, a `surface_container_low` (#f5eff0) section should sit directly on a `surface` (#faf5f5) background to create a "soft-edge" division.
*   **Surface Hierarchy & Nesting:** Treat the UI as a series of physical layers. A `surface_container_lowest` (#ffffff) card should be nested within a `surface_container` (#ece7e7) area. This "stacked paper" effect creates intuitive hierarchy without visual clutter.
*   **The "Glass & Gradient" Rule:** For floating navigation or top bars, use `surface_bright` (#faf5f5) at 80% opacity with a `backdrop-blur` of 20px. main CTAs should utilize a subtle linear gradient from `primary` (#ba0012) to `primary_dim` (#a4000f) at a 145-degree angle to provide a "signature" polish.

## 3. Typography: Editorial Authority
We use **Plus Jakarta Sans** across the entire system. Its geometric yet rounded terminals perfectly bridge the gap between "playful family app" and "premium digital product."

*   **Display Scale:** Use `display-lg` (3.5rem) for hero numbers (e.g., total weekly intake). This should be tracked tightly (-0.02em) to feel like a magazine headline.
*   **The Title/Body Relationship:** Use `title-lg` (1.375rem) for card headers. Ensure there is ample breathing room between a `title-lg` and the following `body-md` (0.875rem). The contrast in scale is what creates the high-end feel.
*   **Labeling:** Labels (`label-md`) should always be in `on_surface_variant` (#5d5b5b) to ensure they sit back in the visual hierarchy, allowing the data (the tracking) to take center stage.

## 4. Elevation & Depth: Tonal Layering
In this design system, shadows are a last resort, not a default.

*   **The Layering Principle:** Depth is achieved by "stacking" the surface-container tiers. Use the **Spacing Scale 4 (1rem)** to create consistent gaps between nested layers.
*   **Ambient Shadows:** When an element must float (like a FAB or a modal), use an ultra-diffused shadow: `box-shadow: 0 20px 40px rgba(48, 46, 47, 0.06)`. Note the use of `on_surface` (#302e2f) as the shadow base—never use pure black.
*   **The "Ghost Border" Fallback:** If accessibility requires a container definition on a similar background, use a **Ghost Border**: `outline` (#797676) at 15% opacity. This provides a whisper of a boundary without breaking the "No-Line" rule.

## 5. Components

### Buttons
*   **Primary:** Rounded `full` (9999px). Background is the `primary` to `primary_dim` gradient. Text is `on_primary` (#ffefed).
*   **Secondary:** Rounded `xl` (3rem). Background is `secondary_container` (#ffc2c9) with `on_secondary_container` (#852138) text.
*   **Interaction:** On hover, shift the background to `primary_fixed_dim` (#ff5b50). No heavy lifts or glows—just a subtle tonal shift.

### Input Fields
*   **Style:** Use `surface_container_low` (#f5eff0) as the field background with a `rounded-md` (1.5rem) corner radius. 
*   **States:** On focus, transition the background to `surface_container_highest` (#e0dcdc). Avoid high-contrast focus rings; use a 2px `surface_tint` (#ba0012) "Ghost Border" at 40% opacity.

### Cards & Progress Trackers
*   **The Bubble Tracker:** Use the `rounded-full` (9999px) token for progress bars. A tracking bar should have a background of `surface_container_high` (#e6e1e1) and a fill of `primary` (#ba0012).
*   **Content Spacing:** Forbid the use of divider lines in cards. Separate content using **Spacing Scale 6 (1.5rem)** or by placing secondary info inside a `surface_variant` (#e0dcdc) sub-container.

### Signature Component: The Family Hub Card
*   A large `rounded-lg` (2rem) container using `tertiary_container` (#d199ff). It uses `display-sm` for a family "Daily Goal" count. This card should overlap the section below it by **Spacing Scale -10 (-2.5rem)** to break the vertical grid and create the signature editorial look.

## 6. Do's and Don'ts

*   **DO** use whitespace as a functional tool. If two elements feel cluttered, increase the spacing to **Scale 12 (3rem)** before considering a divider.
*   **DO** use `rounded-full` for all icons and small interactive elements to reinforce the "friendly/cola" theme.
*   **DON'T** use `on_background` (#302e2f) at 100% opacity for long-form body text; it’s too heavy. Use `on_surface_variant` (#5d5b5b) for a softer, more premium read.
*   **DON'T** use sharp corners. The minimum radius allowed for any visible container is `sm` (0.5rem), but `md` (1.5rem) is the preferred standard.
*   **DON'T** use standard Material shadows. If it looks like a "default" shadow, it is too dark. Lighten and diffuse.