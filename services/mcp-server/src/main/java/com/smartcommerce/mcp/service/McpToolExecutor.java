package com.smartcommerce.mcp.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.mcp.client.CartClient;
import com.smartcommerce.mcp.client.CatalogClient;
import com.smartcommerce.mcp.client.OrderClient;
import com.smartcommerce.mcp.client.ProductClient;
import com.smartcommerce.mcp.client.RecommendationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpToolExecutor {

    private final CatalogClient catalogClient;
    private final ProductClient productClient;
    private final RecommendationClient recommendationClient;
    private final OrderClient orderClient;
    private final CartClient cartClient;

    public Object execute(String tool, Map<String, Object> args) {
        log.info("MCP tool invoked: {} with args: {}", tool, redactArgs(args));
        return switch (tool) {
            case "search_products" -> catalogClient.search(
                stringArg(args, "query"),
                stringArg(args, "categoryId"),
                bigDecimalArg(args, "minPrice"),
                bigDecimalArg(args, "maxPrice"),
                0,
                intArg(args, "limit", 10));
            case "get_product_details" -> {
                var pid = uuidArg(args, "productId");
                yield Map.of(
                    "product", productClient.getProduct(pid),
                    "offers", productClient.getOffers(pid));
            }
            case "get_recommendations" -> recommendationClient.getRecommendations(
                uuidArg(args, "userId"), intArg(args, "limit", 5));
            case "get_user_order_history" -> orderClient.getOrdersByUser(
                uuidArg(args, "userId"), intArg(args, "limit", 10));
            case "get_user_cart" -> cartClient.getCart(uuidArg(args, "userId"));
            default -> throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Unknown tool: " + tool);
        };
    }

    private static String stringArg(Map<String, Object> args, String key) {
        var v = args.get(key);
        return v == null ? null : v.toString();
    }

    private static UUID uuidArg(Map<String, Object> args, String key) {
        var v = stringArg(args, key);
        if (v == null || v.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Missing required argument: " + key);
        }
        try {
            return UUID.fromString(v);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid UUID for " + key + ": " + v);
        }
    }

    private static int intArg(Map<String, Object> args, String key, int defaultValue) {
        var v = args.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    private static BigDecimal bigDecimalArg(Map<String, Object> args, String key) {
        var v = args.get(key);
        if (v == null) return null;
        return new BigDecimal(v.toString());
    }

    private static Map<String, Object> redactArgs(Map<String, Object> args) {
        return args;
    }
}
