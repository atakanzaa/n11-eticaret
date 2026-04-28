package com.smartcommerce.mcp.service;

import com.smartcommerce.mcp.domain.McpTool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class McpToolRegistry {

    private final List<McpTool> tools = List.of(
        new McpTool(
            "search_products",
            "Search the SmartCommerce marketplace for products. Use Turkish queries for best results.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query in Turkish"),
                    "categoryId", Map.of("type", "string", "description", "Optional category UUID"),
                    "minPrice", Map.of("type", "number"),
                    "maxPrice", Map.of("type", "number"),
                    "limit", Map.of("type", "integer", "default", 10)
                ),
                "required", List.of("query")
            )
        ),
        new McpTool(
            "get_product_details",
            "Get detailed information about a specific product including all seller offers.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "productId", Map.of("type", "string", "description", "Product UUID")
                ),
                "required", List.of("productId")
            )
        ),
        new McpTool(
            "get_recommendations",
            "Get personalized recommendations for a user based on their behavior history.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "userId", Map.of("type", "string"),
                    "limit", Map.of("type", "integer", "default", 5)
                ),
                "required", List.of("userId")
            )
        ),
        new McpTool(
            "get_user_order_history",
            "Get a user's order history including items and statuses.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "userId", Map.of("type", "string"),
                    "limit", Map.of("type", "integer", "default", 10)
                ),
                "required", List.of("userId")
            )
        ),
        new McpTool(
            "get_user_cart",
            "Get the current cart contents for a user with price snapshots.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "userId", Map.of("type", "string")
                ),
                "required", List.of("userId")
            )
        )
    );

    public List<McpTool> listTools() {
        return tools;
    }

    public Optional<McpTool> findByName(String name) {
        return tools.stream().filter(t -> t.name().equals(name)).findFirst();
    }
}
