#!/usr/bin/env bash
# Adds Feign timeout + Resilience4j default config to a service's application.yml.
# Idempotent: skips if already present.
set -euo pipefail

SVC=$1
YML="services/$SVC/src/main/resources/application.yml"

if grep -q "openfeign:" "$YML"; then
  echo "$SVC: already has openfeign config, skipping"
  exit 0
fi

python3 - <<PY
import re, pathlib
p = pathlib.Path("$YML")
src = p.read_text()
block = """  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 3000
            readTimeout: 10000
            loggerLevel: BASIC

"""
r4j = """resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 8s
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 5
        registerHealthIndicator: true
  timelimiter:
    configs:
      default:
        timeoutDuration: 12s
        cancelRunningFuture: true

"""
# Find end of top-level 'spring:' block — first line that starts with non-space (other than 'spring:')
lines = src.splitlines(keepends=True)
in_spring = False
insert_idx_spring = None
for i, line in enumerate(lines):
    if line.startswith("spring:"):
        in_spring = True
        continue
    if in_spring and line and not line.startswith((" ", "\t")) and line.strip():
        insert_idx_spring = i
        break

# Insert spring.cloud at end of spring: block
new_lines = lines[:insert_idx_spring] + [block] + lines[insert_idx_spring:]
new_src = "".join(new_lines)

# Append top-level resilience4j (before EOF, but ensure separated by blank line)
if not new_src.endswith("\n"):
    new_src += "\n"
if "resilience4j:" not in new_src:
    new_src = new_src.rstrip() + "\n\n" + r4j

p.write_text(new_src)
print(f"$SVC: updated")
PY
