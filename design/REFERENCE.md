> Historical0.1.1 design record. Current QR/browser/batch/sharing and release gate changes are documented in release/ and README.md. Original tokens/motion and navigation remain authoritative.

# EZ native design correction

Source: user supplied `EZ-Prototype.html`, title `EZ — النموذج التفاعلي 02`.
The bundled React/CSS, embedded assets and local matching Prototype.tsx were inspected. HTML SHA-256: 2109bf89545be9d92b8bcfe874f1251c01ef56bb6bdadc4963b9487b5fe6aa81.
The browser does not permit file:// rendering here. No claim of a browser-rendered HTML screenshot is made. Native renders are produced using Robolectric/Roborazzi, not physical devices.

| HTML screen | Original Alpha mismatch | Native correction | Priority |
|---|---|---|---|
| Home | Symmetric actions, generic icons, missing categories, different text/header/nav | Same source tokens, Phosphor paths, Noto Arabic, art, 1.08/.92 asymmetry, category row, share header, floating 4-tab capsule | P0 |
| Files / Send | Plain picker and connect form | Editorial title, storage, search, category chips, sort menus, selected-file row and pinned action; actual SAF file only | P0 |
| Receive | Plain buttons/address card | Identity, centered heading, connection card, folder action and real listening/manual address; QR explicitly coming soon | P0 |
| Discover | Form inside Send | Dedicated nearby-device list, empty/search/connecting/error states, manual fallback | P0 |
| Confirm | Generic Material alert | Source confirmation layout, actual live six-digit code, explicit accept/reject | P0 |
| Transfer | Compact generic progress card | Source device/percentage/bar/metrics geometry, actual bytes/speed/integrity, cancellation and retry as new session | P0 |
| History / detail | Single Result tab | Real local history, sent/received/status/search filters, detail, clear confirmation without deleting files | P1 |
| Settings | About text | Device name/avatar editor, local preferences, light/dark/system, reduced motion, privacy sheet | P1 |
| Share / QR / scanner | Missing | Source sheets/screen hierarchy; future functionality plainly identified, no fake networking | P1 |

## Tokens and components
White #FFFFFF; ink #111111; blue #2385E8; hero #BAA3F6; send #FFC568; receive #BBD8FF; privacy #F3B4EB. Categories #F7D9E8/#E4DAFF/#D4EBFF/#FFE1C8. Source dark palette #191C22/#20232A/#292D35 with off-white text and muted branded cards. Page padding24dp, gap8/10/12/16dp, card28dp, actions26dp, capsule74dp. Native targets are >=44dp, main actions48dp. Typography: Noto Sans Arabic; static weights reconstructed from the exact Arabic and Latin WOFF2 embedded in the supplied HTML (400/550/650/700/750), no server font fetch.
EzTheme/EzPalette/EzTokens, EzIcon, EzButton, EzRound, EzTag, EzBadge, EzNotice, EzEmpty, EzAvatar, EzDevice, EzSection, FileRow, HistoryCard, StorageSummary, EzNavigation are shared.

## Motion
Source spring stiffness380/damping34/mass.85 maps to Compose damping ratio.87 and stiffness380. Press .96 scale, page280ms cubic(.22,1,.36,1), short fade140ms; active capsule position animates. Progress400ms interpolation uses actual samples. Local reduced motion and Android Animator duration-scale0 switch to snap. No endless scan animation for unimplemented scanner. System haptics are not synthesized.

## Honest Alpha adaptations
No bundled fake file list or fake transfer history. File picker supplies one real SAF URI. Search/filter apply to that selection; sorting is meaningful only for a future multi-file catalog. Storage reads Android StatFs data partition; it is not a claim about opaque SAF destination capacity. QR, pause/resume, APK/link sharing remain coming soon. Retry is new session. Privacy wording retains reference home copy, with explicit Alpha/security caveat in its sheet. Engine, SAF stream verification, foreground service and permissions remain in use. No WebView.

## Brand packaging
Header and launcher use the exact approved bitmap mark; source aspect1200:555 preserved. Foreground is108dp with60dp-wide centered mark inside the adaptive safe area. White background, separate monochrome layer on API33+, density-specific legacy fallbacks, icon and roundIcon both reference mipmap resources. API31 splash uses the same padded mark on white. Launcher preview is a generated packaging illustration, not an installed-device screenshot.

## Validation scope
CI runs existing12 JVM transport tests, native Compose interaction/render tests, lint and debug assembly. Native screenshots at412x892dp, compact360x800dp, 150% font scale and dark mode; old Alpha composable is retained only under test/ for before comparison. Recorded native images are review captures, not approved pixel-perfect goldens against HTML. Physical launcher masks, actual font scaling, screen cutouts, SAF providers, two-phone transfers and OEM background behavior still require device testing. Original connection/security limitations in README/SECURITY still apply.
