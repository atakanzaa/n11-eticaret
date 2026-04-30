package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.BrandResponse;
import com.smartcommerce.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @GetMapping
    public List<BrandResponse> list() {
        return brandService.listAll();
    }

    @GetMapping("/{slug}")
    public BrandResponse getBySlug(@PathVariable String slug) {
        return brandService.getBySlug(slug);
    }
}
