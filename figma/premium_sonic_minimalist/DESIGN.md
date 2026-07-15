---
name: Premium Sonic Minimalist
colors:
  surface: '#121414'
  surface-dim: '#121414'
  surface-bright: '#38393a'
  surface-container-lowest: '#0d0e0f'
  surface-container-low: '#1a1c1c'
  surface-container: '#1e2020'
  surface-container-high: '#292a2a'
  surface-container-highest: '#343535'
  on-surface: '#e3e2e2'
  on-surface-variant: '#bccbb9'
  inverse-surface: '#e3e2e2'
  inverse-on-surface: '#2f3131'
  outline: '#869585'
  outline-variant: '#3d4a3d'
  surface-tint: '#53e076'
  primary: '#53e076'
  on-primary: '#003914'
  primary-container: '#1db954'
  on-primary-container: '#004118'
  inverse-primary: '#006e2d'
  secondary: '#c8c6c5'
  on-secondary: '#313030'
  secondary-container: '#4a4949'
  on-secondary-container: '#bab8b7'
  tertiary: '#ffb3b3'
  on-tertiary: '#680114'
  tertiary-container: '#ff767b'
  on-tertiary-container: '#730a1b'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#72fe8f'
  primary-fixed-dim: '#53e076'
  on-primary-fixed: '#002108'
  on-primary-fixed-variant: '#005320'
  secondary-fixed: '#e5e2e1'
  secondary-fixed-dim: '#c8c6c5'
  on-secondary-fixed: '#1c1b1b'
  on-secondary-fixed-variant: '#474646'
  tertiary-fixed: '#ffdad9'
  tertiary-fixed-dim: '#ffb3b3'
  on-tertiary-fixed: '#400009'
  on-tertiary-fixed-variant: '#881d28'
  background: '#121414'
  on-background: '#e3e2e2'
  surface-variant: '#343535'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.04em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  container-padding-mobile: 16px
  container-padding-desktop: 32px
  gutter: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
---

## Brand & Style

This design system is built for a premium music experience that prioritizes content immersion and high-fidelity aesthetics. The brand personality is **sophisticated, energetic, and atmospheric**, blending the utility of a high-end tool with the emotional resonance of a concert hall.

The visual style is a fusion of **Corporate Minimalism** and **Glassmorphism**. It utilizes a "True Black" canvas to allow vibrant primary accents and rich album art to take center stage. Glassmorphism is applied selectively to functional overlays (like the player and summaries) to create a sense of depth and physical presence, suggesting a refined, translucent material that floats above the content. The interface should evoke a sense of high-end luxury audio equipment—precise, clean, and effortlessly cool.

## Colors

The palette is strictly optimized for OLED screens and low-light environments. 

- **Background:** A "True Black" (#000000) is mandatory for the main viewport to maximize contrast and eliminate visual borders between the device bezel and the UI.
- **Primary:** The signature Green (#1DB954) is used exclusively for active states, playback controls, and primary CTAs. It should be used sparingly to maintain its impact.
- **Surfaces:** Deep Charcoal (#121212) provides a soft lift from the pure black background for cards and secondary containers.
- **Typography:** Primary text is Pure White (#FFFFFF) for maximum legibility. Secondary text uses Light Gray (#B3B3B3) to establish a clear information hierarchy.
- **Glassmorphism:** Overlays use a semi-transparent dark tint with a heavy backdrop-blur (20px-30px) to maintain legibility of the content beneath.

## Typography

This design system utilizes **Inter** for its modern, neutral, and highly legible characteristics. The typography relies on heavy weight variances rather than color to define hierarchy.

- **Display & Headlines:** Use Bold or Extra Bold weights with tighter letter-spacing to create a "locked-in" editorial look.
- **Body Text:** Medium weight (500) is preferred for primary body content to ensure it stands out against the dark background. 
- **Labels:** Small labels use a semi-bold weight and slight tracking (letter-spacing) to maintain clarity at small scales.
- **Hierarchy:** Use white for titles and Light Gray (#B3B3B3) for metadata (artist names, view counts, duration).

## Layout & Spacing

The layout follows a strict **8px base grid**. All margins, paddings, and component heights must be multiples of 8.

- **Mobile:** Uses a single-column fluid layout with 16px side margins. 
- **Grid Systems:** Content cards (Albums/Artists) should use a flexible grid. On mobile, this typically translates to a 2-column or 3-column layout for "Artists" and "Albums."
- **Safe Areas:** Ensure a 44px safe area at the top and bottom of mobile screens to prevent interference with system gestures and the floating mini-player.
- **Rhythm:** Use "Generous Whitespace" between sections (32px - 48px) to allow the "Premium" feel to breathe, while keeping internal component spacing tight (8px or 12px) to maintain perceived connectivity.

## Elevation & Depth

Depth is conveyed through **material transparency and layering** rather than traditional drop shadows.

1.  **Base (0dp):** The True Black background (#000000).
2.  **Surface (1dp):** Primary cards and list items using Deep Charcoal (#121212). No shadows; edge definition is created by the color contrast.
3.  **Floating (2dp):** Glassmorphic overlays (Mini-player, Listening Summary). These use a 1px inner border (white at 10% opacity) to simulate a glass edge and a 40px background blur.
4.  **Interaction:** When a card is pressed, it should scale down slightly (0.98x) rather than changing elevation, maintaining the sleek, tactile feel.

## Shapes

The shape language is defined by the type of content it contains, providing immediate visual cues to the user:

- **Artists:** Always rendered as **perfect circles**. This follows industry standards and helps users distinguish persons from products at a glance.
- **Albums/Playlists:** Always **squares** with a `rounded-lg` (16px) corner radius.
- **Interaction Elements:** Buttons and Chips use a `rounded-xl` or full pill shape to differentiate them from content containers.
- **Containers:** Large "Listening Summary" or "Top Tracks" containers use a consistent 16px radius to match the album art style.

## Components

### Buttons & Chips
- **Primary Action:** Pill-shaped, Primary Green background with black text for maximum visibility.
- **Chips (Filters):** Dark charcoal background with white text. Active state switches to Primary Green background or a white border.

### Cards
- **Standard Card:** 16px corner radius. Image on top (square or circle), primary title in white, secondary text in gray.
- **Glass Summary Card:** Background-blur (30px), semi-transparent charcoal fill, 1px subtle white border. Used for high-level statistics or "Now Playing" context.

### List Items
- **Song Row:** 64px height. Square artwork (8px radius). Title (white, 16px), Subtitle (gray, 14px). Action icon (three dots) on the far right.

### Input Fields
- Search bars should be pill-shaped with a Deep Charcoal (#121212) fill and a search icon set to Light Gray.

### Playback Controls
- **Mini-player:** A floating glassmorphic bar that sits 8px above the bottom navigation. It should appear to "float" over the content with a subtle 1px top border.
- **Progress Bar:** 4px height. Background is gray (20% opacity), progress is Primary Green. The handle (thumb) only appears on hover or active touch.