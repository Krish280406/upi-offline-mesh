package com.demo.upimesh;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.crypto.ServerKeyHolder;
import com.demo.upimesh.crypto.SenderKeyService;
import com.demo.upimesh.crypto.SignatureService;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.PaymentInstruction;
import com.demo.upimesh.service.BridgeIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PipelineEdgeCaseTest {
    @Autowired private BridgeIngestionService bridge;
    @Autowired private HybridCryptoService crypto;
    @Autowired private ServerKeyHolder serverKey;
    @Autowired private SenderKeyService senderKeys;
    @Autowired private SignatureService signatures;

    private MeshPacket buildPacket(String senderVpa, String receiverVpa, BigDecimal amount,
                                    long signedAt, boolean sign) throws Exception {
        PaymentInstruction instruction = new PaymentInstruction(
                senderVpa, receiverVpa, amount, "pinhash",
                UUID.randomUUID().toString(), signedAt);
        if (sign) {
            instruction.setSignature(signatures.sign(instruction, senderKeys.getPrivateKey(senderVpa)));
        }
        String ciphertext = crypto.encrypt(instruction, serverKey.getPublicKey());
        MeshPacket packet = new MeshPacket();
        packet.setPacketId(UUID.randomUUID().toString());
        packet.setTtl(5);
        packet.setCreatedAt(Instant.now().toEpochMilli());
        packet.setCiphertext(ciphertext);
        return packet;
    }

    @Test
    void expiredPacketIsRejected() throws Exception {
        long twoDaysAgo = Instant.now().minusSeconds(2 * 86400).toEpochMilli();
        var packet = buildPacket("alice@demo", "bob@demo", new BigDecimal("10.00"), twoDaysAgo, true);
        var result = bridge.ingest(packet, "bridge-test", 1);
        assertEquals("INVALID", result.outcome());
        assertEquals("stale_packet", result.reason());
    }

    @Test
    void futureDatedPacketIsRejected() throws Exception {
        long inFuture = Instant.now().plusSeconds(3600).toEpochMilli();
        var packet = buildPacket("alice@demo", "bob@demo", new BigDecimal("10.00"), inFuture, true);
        var result = bridge.ingest(packet, "bridge-test", 1);
        assertEquals("INVALID", result.outcome());
        assertEquals("future_dated", result.reason());
    }

    @Test
    void unsignedPacketIsRejected() throws Exception {
        var packet = buildPacket("alice@demo", "bob@demo", new BigDecimal("10.00"),
                Instant.now().toEpochMilli(), false);
        var result = bridge.ingest(packet, "bridge-test", 1);
        assertEquals("INVALID", result.outcome());
        assertEquals("invalid_signature", result.reason());
    }

    @Test
    void insufficientBalanceReportsRejectedNotSettled() throws Exception {
        var packet = buildPacket("dave@demo", "alice@demo", new BigDecimal("999999.00"),
                Instant.now().toEpochMilli(), true);
        var result = bridge.ingest(packet, "bridge-test", 1);
        assertEquals("REJECTED", result.outcome());
    }
}