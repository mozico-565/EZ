# EZ 1.0.0 release work

Authoritative execution brief: MASTER-PROMPT.md. Continue PR #2 at 7445daef30b80d1cab9a354b1a0b649b0345cdff; do not reset to the historical failing commit. Existing CI is green (31 tests, run39091776955). Do not change fixed navigation/scroll behavior.

In progress: header dark integration, corrected layered Receive vector, native sharing, MediaStore/SAF browser, QR, versioned batch transfers, timing and release pipeline.

Stable publication gates remain closed until all required tests pass, a persistent production signing identity is configured, and the physical two-phone matrix is completed. No production keystore exists in the source. No stable v1.0.0 tag or public release should be created while gates are unmet.
