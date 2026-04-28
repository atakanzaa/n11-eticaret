package com.smartcommerce.shipment.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component("ARAS")
public class MockArasAdapter implements CargoProvider {
    @Override
    public CargoCreateResult create(CargoCreateRequest req) {
        var tn = "AR" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
        return new CargoCreateResult(true, tn,
            "https://mock-aras.local/track/" + tn,
            LocalDate.now().plusDays(2), null);
    }

    @Override
    public String getName() { return "ARAS"; }
}
