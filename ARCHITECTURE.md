# Architecture

## The three original hard problems

1. **Untrusted intermediaries** — hybrid RSA-OAEP + AES-GCM encryption.
   A fresh AES-256 key per packet encrypts the payload; the AES key itself
   is RSA-wrapped for the server. GCM's authentication tag means any
   tampering breaks decryption outright.
2. **Duplicate delivery** — `IdempotencyService.claim()` does an atomic
   compare-and-set on the SHA-256 hash of the ciphertext, so exactly one of
   N simultaneous deliveries of the same packet proceeds.
3. **Replay attacks** — a nonce plus a 24-hour freshness window on the
   `signedAt` timestamp, both inside the encrypted envelope.

## What this version adds

- **Authenticity** (`SenderKeyService`, `SignatureService`) — closes the gap
  where confidentiality alone doesn't prove *who* sent a payment.
- **Bridge authentication** (`BridgeAuthService`) — HMAC-signed requests on
  the real ingestion endpoint.
- **Balance holds** (`HoldService`) — reserves funds at injection time,
  released once the backend makes a first attempt on that packet.
- **Realistic mesh** (`MeshSimulatorService`) — probabilistic per-round
  contact, two bridge nodes by default.
- **Observability** (`IngestionMetrics`, `IngestionAlertWatcher`) — outcome
  counters and a spike alert on `INVALID` results.

## Request lifecycle

Sender phone → hybrid encrypt (+ sign) → mesh gossip → bridge node → hash →
idempotency claim → decrypt/verify → signature verify → freshness check →
settle (debit/credit, ledger write) → release hold.

## Honest limitations that remain

- No cryptographic proof of funds offline — a real hardware-backed wallet
  (like UPI Lite) would be needed to fully close the double-spend window;
  the hold mechanism here only narrows it.
- The mesh simulation still isn't real BLE — Android's background-scan
  throttling and iOS peripheral-mode restrictions aren't modeled.
- Metadata privacy (a stranger's phone knows *something* is being carried)
  isn't addressed.