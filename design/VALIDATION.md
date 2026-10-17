# Native design validation

The first ordinary-source build (07b15ba, GitHub Actions run39072932603) passed core tests, native Compose render/interaction tests, Android lint and APK assembly. It generated16 native review images. This is build evidence, not physical-device validation.

Visual review found and corrected the Home headline wrapping into3 lines instead of2. The next build uses the exact Arabic/Latin font embedded in the supplied HTML, an explicit two-line default headline with width assertion, edge-to-edge test activity setup, a150% font-scale capture, and adaptive card/header sizing. Text may wrap further when the user enlarges system fonts, to preserve accessibility. Transfer metrics use weighted columns to avoid crowding.

The source downloaded from GitHub was compared to the local Android project after initial upload; core protocol and tests matched their recorded SHA-256 hashes exactly. Controller changes are limited to visible peer/direction/name preferences, routing progress to the native transfer screen and distinguishing cancellation from failure. Existing socket/crypto/SAF engine remains wired.

Icon packaging was checked programmatically:432x432 foreground, unchanged mark aspect, visible mark bounding box (96,160)-(336,271), maximum opaque radius126px inside the132px adaptive safe circle. All XML resources parse. Preview images show generated launcher mask compositions, not screenshots from a physical launcher.

The browser blocked local file:// previews, so no HTML browser screenshot was captured. Comparisons use the full supplied HTML/CSS/assets, its matching source structure, the user's visible approved Home reference and native before/after renders. Recorded renders are review captures; they are not approved pixel goldens.

Remaining physical checks: installed launcher/splash, upgrade signing compatibility, two-phone transfer/rejection/interruption, SAF providers, notification denial, OEM background behavior, system gesture/cutout insets and large system fonts. See TESTING.md. A first Alpha ephemeral debug key cannot be recovered from the APK; reliable production updates require user-managed signing credentials.
