package com.demo.upimesh.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each demo account gets its own Ed25519 signing keypair, generated at
 * startup — standing in for "the key the user's phone generated the first
 * time they set up the app." This is separate from the server's RSA keypair:
 * that one is for confidentiality (encrypting TO the server), this one is
 * for authenticity (proving WHO sent it).
 */
@Component
public class SenderKeyService {

    private final Map<String, KeyPair> keyPairs = new ConcurrentHashMap<>();

    @PostConstruct
    public void seedKeys() throws Exception {
        for (String vpa : List.of("alice@demo", "bob@demo", "carol@demo", "dave@demo")) {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
            keyPairs.put(vpa, gen.generateKeyPair());
        }
    }

    public PrivateKey getPrivateKey(String vpa) {
        KeyPair kp = keyPairs.get(vpa);
        if (kp == null) throw new IllegalArgumentException("No signing key for VPA: " + vpa);
        return kp.getPrivate();
    }

    public PublicKey getPublicKey(String vpa) {
        KeyPair kp = keyPairs.get(vpa);
        if (kp == null) throw new IllegalArgumentException("No signing key for VPA: " + vpa);
        return kp.getPublic();
    }
}