package com.demo.upimesh;

import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.service.MeshSimulatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = "upi.mesh.gossip-contact-probability=1.0")
class MeshSimulatorServiceTest {
    
    @Autowired private MeshSimulatorService mesh;

    @Test
    void withFullContactProbabilityEveryDeviceEventuallyHoldsThePacket() {
        mesh.resetMesh();
        MeshPacket packet = new MeshPacket();
        packet.setPacketId("test-packet-1");
        packet.setTtl(5);
        packet.setCreatedAt(System.currentTimeMillis());
        packet.setCiphertext("dummy-ciphertext");
        
        mesh.inject("phone-alice", packet);
        mesh.gossipOnce();
        
        long deviceCount = mesh.getDevices().size();
        long holdingCount = mesh.getDevices().stream().filter(d -> d.holds("test-packet-1")).count();
        assertEquals(deviceCount, holdingCount);
    }
}
