package com.demo.upimesh;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.crypto.ServerKeyHolder;
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
class SignatureVerificationTest {

    @Autowired private BridgeIngestionService bridge;
    @Autowired private HybridCryptoService crypto;
    @Autowired private ServerKeyHolder serverKey;

    @Test
    void forgedPacketWithoutSenderPrivateKeyIsRejected() throws Exception {
        // An "attacker" who only has the server's PUBLIC key (which is public
        // by design) tries to impersonate alice@demo, with no signature at all.
        PaymentInstruction forged = new PaymentInstruction(
                "alice@demo", "bob@demo", new BigDecimal("500.00"),
                "pinhash", UUID.randomUUID().toString(), Instant.now().toEpochMilli());
        // no forged.setSignature(...) â€” attacker doesn't have alice's private key

        String ciphertext = crypto.encrypt(forged, serverKey.getPublicKey());
        MeshPacket packet = new MeshPacket();
        packet.setPacketId(UUID.randomUUID().toString());
        packet.setTtl(5);
        packet.setCreatedAt(Instant.now().toEpochMilli());
        packet.setCiphertext(ciphertext);

        var result = bridge.ingest(packet, "attacker-bridge", 1);

        assertEquals("INVALID", result.outcome());
        assertEquals("invalid_signature", result.reason());
    }
}