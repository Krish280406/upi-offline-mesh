package com.demo.upimesh.crypto;

import com.demo.upimesh.model.PaymentInstruction;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

@Service
public class SignatureService {

    private static final String ALGORITHM = "Ed25519";

    public String sign(PaymentInstruction instruction, PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance(ALGORITHM);
        sig.initSign(privateKey);
        sig.update(canonicalBytes(instruction));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    public boolean verify(PaymentInstruction instruction, String signatureBase64, PublicKey publicKey) throws Exception {
        if (signatureBase64 == null) return false;
        Signature sig = Signature.getInstance(ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(canonicalBytes(instruction));
        return sig.verify(Base64.getDecoder().decode(signatureBase64));
    }

    private byte[] canonicalBytes(PaymentInstruction i) {
        String canonical = String.join("|",
                i.getSenderVpa(),
                i.getReceiverVpa(),
                i.getAmount().toPlainString(),
                i.getPinHash(),
                i.getNonce(),
                String.valueOf(i.getSignedAt()));
        return canonical.getBytes(StandardCharsets.UTF_8);
    }
}