package com.smartcommerce.ai.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "mcp-server", url = "${services.mcp.url:http://localhost:8097}")
public interface McpClient {

    @GetMapping("/api/mcp/tools")
    @CircuitBreaker(name = "mcp-server")
    List<McpTool> listTools();

    @PostMapping("/api/mcp/tools/{name}/invoke")
    @CircuitBreaker(name = "mcp-server")
    McpInvokeResponse invokeTool(@PathVariable("name") String name, @RequestBody Map<String, Object> args);

    record McpTool(String name, String description, Map<String, Object> inputSchema) {
    }

    record McpInvokeResponse(boolean success, Object data, String error) {
    }
}
