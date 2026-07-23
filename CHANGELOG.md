# Changelog

## v1.0.0

Initial stable release, built on top of
[perryvegehan/UPI_Without_Internet](https://github.com/perryvegehan/UPI_Without_Internet).

### Added
- Ed25519 sender-signature verification
- HMAC bridge-node authentication
- Per-bridge-node rate limiting (bucket4j)
- PostgreSQL + Flyway profile
- Redis-backed idempotency profile
- Balance-hold reservations narrowing the double-spend window
- Probabilistic, multi-bridge gossip model
- Actuator health/metrics endpoints and INVALID-spike alerting
- GitHub Actions CI
- MockMvc integration tests and edge-case coverage

### Fixed
- Insufficient-funds settlements were incorrectly reported as SETTLED
  instead of REJECTED