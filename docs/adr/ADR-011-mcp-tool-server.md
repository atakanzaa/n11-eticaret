# ADR-011: MCP-style Tool Server (HTTP, not WebSocket/stdio)

**Status:** Accepted

## Context
The platform needs to expose its catalog/order/cart capabilities to AI agents
(Day 6 ai-orchestrator + Anthropic API). The official Model Context Protocol
spec uses WebSocket or stdio transports, both of which need extra plumbing
that does not pay off in a 7-day showcase.

## Decision
- Build `mcp-server` (port 8097) as a Spring Boot HTTP service that mimics the
  MCP shape: `GET /api/mcp/tools` lists tools with JSON-Schema input contracts,
  `POST /api/mcp/tools/{name}/invoke` executes a tool with a JSON arg map.
- Tool registry is a static Java list of 5 tools: `search_products`,
  `get_product_details`, `get_recommendations`, `get_user_order_history`,
  `get_user_cart`. Adding new tools = add a record to the list and a case to
  the executor switch.
- The server is a **thin gateway** — every tool maps to a Feign call into
  another service (catalog / product / recommendation / order / cart). No
  business logic lives in mcp-server.
- mcp-server is stateless: no DB, no Kafka, no outbox. Auto-config excludes
  `DataSourceAutoConfiguration` and `HibernateJpaAutoConfiguration` so it
  starts without Postgres.
- All MCP endpoints are `permitAll()` for the showcase. Production would
  add an internal-only API key check at the controller layer.

## Trade-offs
- We give up two things vs real MCP: (1) bidirectional streaming, and (2) the
  official tool-discovery handshake. Neither matters for the n11 demo where
  Claude calls these endpoints with a generic HTTP tool definition.
- Tools are defined in code (no DB-backed registry). Easy to read in one file;
  no config UI. For an interview talking point, this is intentional: "v1 is
  static, v2 would back it with a config table + admin endpoints."
- Day 6 ai-orchestrator will register these tool definitions verbatim with
  the Anthropic API tool-use schema.
