package com.smartcommerce.mcp.api;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.mcp.domain.McpTool;
import com.smartcommerce.mcp.service.McpToolExecutor;
import com.smartcommerce.mcp.service.McpToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {

    private final McpToolRegistry registry;
    private final McpToolExecutor executor;

    @GetMapping("/tools")
    public List<McpTool> listTools() {
        return registry.listTools();
    }

    @PostMapping("/tools/{name}/invoke")
    public McpInvokeResponse invoke(@PathVariable String name, @RequestBody(required = false) Map<String, Object> args) {
        registry.findByName(name).orElseThrow(() ->
            new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Unknown MCP tool: " + name));
        var safeArgs = args == null ? Map.<String, Object>of() : args;
        try {
            return new McpInvokeResponse(true, executor.execute(name, safeArgs), null);
        } catch (Exception e) {
            log.error("MCP tool {} invocation failed: {}", name, e.getMessage(), e);
            return new McpInvokeResponse(false, null, e.getMessage());
        }
    }

    public record McpInvokeResponse(boolean success, Object data, String error) {
    }
}
