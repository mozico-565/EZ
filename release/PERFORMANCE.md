# Transfer measurements, not radio benchmarks

The user's 4.6MB / approximately10 second versus SHAREit approximately2 second observation includes unknown discovery, approval, first-byte, storage and verification costs. It does not establish TCP throughput. No proprietary SHAREit internals are asserted.

Protocol2 retains 64KiB payload chunks, AES-GCM per record, SAS approval, length and SHA-256 checks, SAF readback, verified receipt, cancellation and partial-file abort. It uses a 256KiB bounded output buffer, flushes the first data chunk for latency, then allows buffered records, and flushes at each terminal/approval/receipt boundary. It reuses one encrypted connection and manifest for the batch rather than re-pairing each file. No encryption/authentication is removed. Source buffers remain bounded independently of file size.

`BatchResult.timings` distinguishes cryptographic handshake, human approval, first wire chunk, per-file streaming, receiver write time within streaming, receiver readback/publication, sender receipt/remote verification and total session. Controller adds connection and first-device discovery timing when available. Streaming speed uses actual byte deltas and monotonic time. Batch-average active streaming excludes approval and readback intervals. Receiver storage writes occur within streaming; they are additionally measured, not subtracted into a fabricated 'pure Wi-Fi speed'. Timings are shown in the transfer screen after success.

## Executed synthetic JVM loopback

`loopback.csv` records real executions in this environment with a synthetic generated stream and null destination, Java17, `-Xmx256m`. `core/src/benchmark/.../LoopbackBenchmark.kt` runs protocol1 (existing single-file) and protocol2 (batch) at4MB/100MB/1GB, two trials each, in alternating order. It exercises real sockets, ECDH, AES-GCM, record framing, streamed SHA and receipts. It does NOT exercise Android, a wireless link, MediaStore/SAF storage, or actual humans; the test fixture approves automatically. No stream's full file is held in memory. Trial1 includes JIT/cold-start effects; trial2 is warm, not a controlled device benchmark.

Warm1GB total elapsed: protocol1 1736.96ms, protocol2 1553.37ms on localhost (~10.6% lower). These are environment-specific observations, not user-visible promises and not comparable with SHAREit.4MB startup variance is substantial. Full raw rows are retained; no fixed2second claim is made.

## Physical methodology — NOT PERFORMED

Use the same two phones, same files and SHA hashes, identical local network, airplane mode with Wi-Fi re-enabled as appropriate. Record Wi-Fi band2.4/5GHz, signal, free space, storage provider, thermal/battery state and app build/signature. Run at least5 trials (first cold, remaining warm) for4MB,100MB,1GB and64small files. Alternate sender/receiver; separate approval waiting time, wire/stream interval, storage verification and total elapsed. Test router and local hotspot separately. Capture the displayed timing breakdown and receiver hash. Report median and range, not a single best case. OEM background behavior, radio and storage may dominate.

Wi-Fi Direct/local-only hotspot are candidates for compatibility experiments, not enabled transports or throughput guarantees. Nearby Connections would add a Google-dependent option and is not used as the mandatory engine, especially for non-GMS Huawei devices. Trusted-session reuse would require persistent identity authentication; it is not implemented by skipping SAS.

`small-batch-loopback.csv` additionally records64x64KiB files. Original protocol1 uses64 separate approved test connections; protocol2 uses one manifest/connection. Warm total392.77ms versus41.79ms on localhost. This mostly demonstrates avoiding repeated connection/key-exchange setup; approvals are automatic test fixtures and no wireless/SAF storage is included. It is not a phone throughput claim. Source: SmallBatchBenchmark.kt.
