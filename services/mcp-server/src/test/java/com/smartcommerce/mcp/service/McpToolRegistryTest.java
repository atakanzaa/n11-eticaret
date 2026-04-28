package com.smartcommerce.mcp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolRegistryTest {

    private final McpToolRegistry registry = new McpToolRegistry();

    @Test
    @DisplayName("registry exposes all five tools with input schemas")
    void registry_exposesFiveTools() {
        var tools = registry.listTools();

        assertThat(tools).hasSize(5);
        assertThat(tools).extracting("name").containsExactly(
            "search_products", "get_product_details", "get_recommendations",
            "get_user_order_history", "get_user_cart");
        assertThat(tools).allSatisfy(t -> assertThat(t.inputSchema()).isNotEmpty());
    }

    @Test
    @DisplayName("findByName returns matching tool")
    void findByName_returnsMatchingTool() {
        var tool = registry.findByName("search_products");
        assertThat(tool).isPresent();
        assertThat(tool.get().description()).contains("Turkish");
    }

    @Test
    @DisplayName("findByName returns empty for unknown tool")
    void findByName_emptyForUnknown() {
        assertThat(registry.findByName("unknown_tool")).isEmpty();
    }
}
