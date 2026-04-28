package com.smartcommerce.mcp.domain;

import java.util.Map;

public record McpTool(String name, String description, Map<String, Object> inputSchema) {
}
