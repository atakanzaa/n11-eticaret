package com.smartcommerce.shipment.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component("YURTICI")
public class MockYurticiAdapter implements CargoProvider {
    @Override
    public CargoCreateResult create(CargoCreateRequest req) {
        var tn = "YK" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
        return new CargoCreateResult(true, tn,
            "https://mock-yurtici.local/track/" + tn,
            LocalDate.now().plusDays(3), null);
    }

    @Override
    public String getName() { return "YURTICI"; }
}
