package com.demo.upimesh.service;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.crypto.SenderKeyService;
import com.demo.upimesh.crypto.SignatureService;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.PaymentInstruction;
import com.demo.upimesh.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BridgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BridgeIngestionService.class);

    @Autowired private HybridCryptoService crypto;
    @Autowired private IdempotencyService idempotency;
    @Autowired private SettlementService settlement;
    @Autowired private SenderKeyService senderKeys;
    @Autowired private SignatureService signatures;
    @Autowired private HoldService holdService;
    @Autowired private IngestionMetrics metrics;

    @Value("${upi.mesh.packet-max-age-seconds:86400}")
    private long maxAgeSeconds;

    public IngestResult ingest(MeshPacket packet, String bridgeNodeId, int hopCount) {
        try {
            String packetHash = crypto.hashCiphertext(packet.getCiphertext());

            if (!idempotency.claim(packetHash)) {
                log.info("DUPLICATE packet {} from bridge {} — dropped",
                        packetHash.substring(0, 12) + "...", bridgeNodeId);
                metrics.recordDuplicate();
                return IngestResult.duplicate(packetHash);
            }

            holdService.release(packetHash);

            PaymentInstruction instruction;
            try {
                instruction = crypto.decrypt(packet.getCiphertext());
            } catch (Exception e) {
                log.warn("Decryption failed for packet {}: {}",
                        packetHash.substring(0, 12) + "...", e.getMessage());
                metrics.recordInvalid();
                return IngestResult.invalid(packetHash, "decryption_failed");
            }

            try {
                boolean validSignature = signatures.verify(
                        instruction, instruction.getSignature(),
                        senderKeys.getPublicKey(instruction.getSenderVpa()));
                if (!validSignature) {
                    metrics.recordInvalid();
                    return IngestResult.invalid(packetHash, "invalid_signature");
                }
            } catch (Exception e) {
                metrics.recordInvalid();
                return IngestResult.invalid(packetHash, "invalid_signature");
            }

            long ageSeconds = (Instant.now().toEpochMilli() - instruction.getSignedAt()) / 1000;
            if (ageSeconds > maxAgeSeconds) {
                log.warn("Packet {} too old ({}s), rejected", packetHash.substring(0, 12) + "...", ageSeconds);
                metrics.recordInvalid();
                return IngestResult.invalid(packetHash, "stale_packet");
            }
            if (ageSeconds < -300) {
                metrics.recordInvalid();
                return IngestResult.invalid(packetHash, "future_dated");
            }

            Transaction tx = settlement.settle(instruction, packetHash, bridgeNodeId, hopCount);
            if (tx.getStatus() == Transaction.Status.REJECTED) {
                metrics.recordRejected();
                return IngestResult.rejected(packetHash, tx);
            }
            metrics.recordSettled();
            return IngestResult.settled(packetHash, tx);

        } catch (Exception e) {
            log.error("Ingestion error: {}", e.getMessage(), e);
            metrics.recordInvalid();
            return IngestResult.invalid("?", "internal_error: " + e.getMessage());
        }
    }

    public record IngestResult(String outcome, String packetHash, String reason, Long transactionId) {
        public static IngestResult settled(String hash, Transaction tx) {
            return new IngestResult("SETTLED", hash, null, tx.getId());
        }
        public static IngestResult rejected(String hash, Transaction tx) {
            return new IngestResult("REJECTED", hash, "insufficient_funds", tx.getId());
        }
        public static IngestResult duplicate(String hash) {
            return new IngestResult("DUPLICATE_DROPPED", hash, null, null);
        }
        public static IngestResult invalid(String hash, String reason) {
            return new IngestResult("INVALID", hash, reason, null);
        }
    }
}