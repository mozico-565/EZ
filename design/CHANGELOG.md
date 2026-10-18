# 0.1.1-alpha-design

- Replaced stretched drawable launcher with aspect-preserving adaptive foreground/background, API33 monochrome, density fallbacks and branded splash. Approved letterforms are unchanged.
- Restored source tokens, Phosphor icon paths, bundled Noto Arabic, approved hero illustration, RTL home asymmetry, category cards, floating four-tab capsule and spring/page/press motion.
- Applied the source system to Files/Send, discovery, Receive, SAS confirmation, real progress, history/detail, Settings, dark/system appearance, privacy/share sheets and explicit coming-soon scanner.
- Kept actual SAF, NSD/manual connection, encrypted transport, real byte/speed samples, file SHA/readback verification, notifications and cancellation. Core files/tests unchanged; hashes recorded.
- Added persistent local name/avatar/appearance/reduced-motion preferences and real transfer history, search/filter and clear confirmation.
- Added native Compose render/interaction tests and original Alpha home as a test-only before fixture. CI is read-only and builds ordinary source.
- Package ID unchanged; versionCode2. Old ephemeral debug key is not recoverable from an APK, so Android may reject an in-place debug upgrade. Future debug jobs attempt cache reuse at an explicit signing path; cache scope/eviction is not a production signing guarantee. Production signing still needs user-managed credentials.
