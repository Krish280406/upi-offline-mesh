package com.demo.upimesh.service;

import com.demo.upimesh.model.MeshPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MeshSimulatorService {
    private static final Logger log = LoggerFactory.getLogger(MeshSimulatorService.class);
    private final Map<String, VirtualDevice> devices = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Value("${upi.mesh.gossip-contact-probability:0.6}")
    private double contactProbability;

    public MeshSimulatorService() {
        seedDefaultDevices();
    }

    private void seedDefaultDevices() {
        devices.put("phone-alice",     new VirtualDevice("phone-alice", false));
        devices.put("phone-stranger1", new VirtualDevice("phone-stranger1", false));
        devices.put("phone-stranger2", new VirtualDevice("phone-stranger2", false));
        devices.put("phone-stranger3", new VirtualDevice("phone-stranger3", false));
        devices.put("phone-stranger4", new VirtualDevice("phone-stranger4", false));
        devices.put("phone-bridge1",   new VirtualDevice("phone-bridge1", true));
        devices.put("phone-bridge2",   new VirtualDevice("phone-bridge2", true));
    }

    public Collection<VirtualDevice> getDevices() { return devices.values(); }
    public VirtualDevice getDevice(String id) { return devices.get(id); }

    public void inject(String senderDeviceId, MeshPacket packet) {
        VirtualDevice sender = devices.get(senderDeviceId);
        if (sender == null) throw new IllegalArgumentException("Unknown device: " + senderDeviceId);
        sender.hold(packet);
        log.info("Packet {} injected at {} (TTL={})",
                packet.getPacketId().substring(0, 8), senderDeviceId, packet.getTtl());
    }

    public GossipResult gossipOnce() {
        int transfers = 0;
        List<VirtualDevice> deviceList = new ArrayList<>(devices.values());
        Map<String, List<MeshPacket>> snapshot = new HashMap<>();

        for (VirtualDevice d : deviceList) {
            snapshot.put(d.getDeviceId(), new ArrayList<>(d.getHeldPackets()));
        }

        for (VirtualDevice src : deviceList) {
            for (MeshPacket pkt : snapshot.get(src.getDeviceId())) {
                if (pkt.getTtl() <= 0) continue;
                for (VirtualDevice dst : deviceList) {
                    if (dst == src) continue;
                    if (dst.holds(pkt.getPacketId())) continue;
                    if (random.nextDouble() >= contactProbability) continue;
                    
                    MeshPacket copy = new MeshPacket();
                    copy.setPacketId(pkt.getPacketId());
                    copy.setTtl(pkt.getTtl() - 1);
                    copy.setCreatedAt(pkt.getCreatedAt());
                    copy.setCiphertext(pkt.getCiphertext());
                    dst.hold(copy);
                    transfers++;
                }
            }
        }
        log.info("Gossip round complete: {} transfers (contact probability={})", transfers, contactProbability);
        return new GossipResult(transfers, snapshotMap());
    }

    public Map<String, Integer> snapshotMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (VirtualDevice d : devices.values()) m.put(d.getDeviceId(), d.packetCount());
        return m;
    }

    public List<BridgeUpload> collectBridgeUploads() {
        List<BridgeUpload> out = new ArrayList<>();
        for (VirtualDevice d : devices.values()) {
            if (!d.hasInternet()) continue;
            for (MeshPacket pkt : d.getHeldPackets()) out.add(new BridgeUpload(d.getDeviceId(), pkt));
        }
        return out;
    }

    public void resetMesh() {
        devices.values().forEach(VirtualDevice::clear);
    }

    public record GossipResult(int transfers, Map<String, Integer> deviceCounts) {}
    public record BridgeUpload(String bridgeNodeId, MeshPacket packet) {}
}