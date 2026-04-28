package com.smartcommerce.shipment.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component("MNG")
public class MockMngAdapter implements CargoProvider {
    @Override
    public CargoCreateResult create(CargoCreateRequest req) {
        var tn = "MN" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
        return new CargoCreateResult(true, tn,
            "https://mock-mng.local/track/" + tn,
            LocalDate.now().plusDays(4), null);
    }

    @Override
    public String getName() { return "MNG"; }
}
