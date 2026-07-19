package com.demo.upimesh;

import com.demo.upimesh.service.DemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class HoldServiceTest {
    @Autowired private DemoService demoService;

    @Test
    void secondOfflineSendExceedingAvailableBalanceIsBlocked() throws Exception {
        assertDoesNotThrow(() ->
                demoService.createPacket("dave@demo", "alice@demo", new BigDecimal("400.00"), "1234", 5));
        
        assertThrows(IllegalArgumentException.class, () ->
                demoService.createPacket("dave@demo", "alice@demo", new BigDecimal("400.00"), "1234", 5));
    }
}