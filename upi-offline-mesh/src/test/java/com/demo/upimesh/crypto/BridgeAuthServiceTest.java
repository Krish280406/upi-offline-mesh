package com.demo.upimesh.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BridgeAuthServiceTest {

    @Autowired private BridgeAuthService bridgeAuth;

    @Test
    void validSignatureIsAccepted() throws Exception {
        String body = "{\"test\":\"payload\"}";
        String sig = bridgeAuth.sign(body);
        assertTrue(bridgeAuth.isValidSignature(body, sig));
    }

    @Test
    void tamperedBodyIsRejected() throws Exception {
        String body = "{\"test\":\"payload\"}";
        String sig = bridgeAuth.sign(body);
        assertFalse(bridgeAuth.isValidSignature("{\"test\":\"tampered\"}", sig));
    }
}   