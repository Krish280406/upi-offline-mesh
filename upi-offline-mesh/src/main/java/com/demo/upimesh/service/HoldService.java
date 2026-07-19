package com.demo.upimesh.service;

import com.demo.upimesh.model.Account;
import com.demo.upimesh.model.AccountRepository;
import com.demo.upimesh.model.Hold;
import com.demo.upimesh.model.HoldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class HoldService {

    @Autowired private HoldRepository holds;
    @Autowired private AccountRepository accounts;

    @Value("${upi.mesh.packet-max-age-seconds:86400}")
    private long maxAgeSeconds;

    @Transactional
    public boolean tryReserve(String senderVpa, BigDecimal amount, String packetHash) {
        Account sender = accounts.findById(senderVpa)
                .orElseThrow(() -> new IllegalArgumentException("Unknown sender VPA: " + senderVpa));

        BigDecimal alreadyHeld = holds.findBySenderVpa(senderVpa).stream()
                .map(Hold::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal available = sender.getBalance().subtract(alreadyHeld);
        if (available.compareTo(amount) < 0) {
            return false;
        }

        Hold hold = new Hold();
        hold.setPacketHash(packetHash);
        hold.setSenderVpa(senderVpa);
        hold.setAmount(amount);
        hold.setCreatedAt(Instant.now());
        holds.save(hold);
        return true;
    }

    @Transactional
    public void release(String packetHash) {
        holds.findByPacketHash(packetHash).ifPresent(holds::delete);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        Instant cutoff = Instant.now().minusSeconds(maxAgeSeconds);
        holds.findAll().stream()
                .filter(h -> h.getCreatedAt().isBefore(cutoff))
                .forEach(holds::delete);
    }
}