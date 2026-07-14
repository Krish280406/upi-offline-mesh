package com.demo.upimesh.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies a bridge-node upload actually came from a device holding the
 * shared secret. In production each bridge node would have its own secret
 * or client certificate; a single shared secret is the simplest version
 * that still demonstrates the pattern.
 */
@Service
public class BridgeAuthService {

    @Value("${upi.mesh.bridge-shared-secret:}")
    private String sharedSecret;

    public String sign(String rawBody) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean isValidSignature(String rawBody, String providedSignatureHex) {
        if (providedSignatureHex == null) return false;
        try {
            String expectedHex = sign(rawBody);
            return MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    providedSignatureHex.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}