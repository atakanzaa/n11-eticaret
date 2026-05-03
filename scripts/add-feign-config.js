#!/usr/bin/env node
// Adds Feign timeout + Resilience4j default config to a service's application.yml.
// Idempotent: skips if already present.
const fs = require('fs');
const path = require('path');

const svc = process.argv[2];
if (!svc) { console.error('usage: add-feign-config.js <service-name>'); process.exit(1); }
const yml = path.join('services', svc, 'src', 'main', 'resources', 'application.yml');

let src = fs.readFileSync(yml, 'utf8');

if (src.includes('openfeign:')) {
  console.log(`${svc}: already has openfeign config, skipping`);
  process.exit(0);
}

const cloudBlock = `  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 3000
            readTimeout: 10000
            loggerLevel: BASIC

`;

const r4j = `resilience4j:
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
`;

// Find end of top-level 'spring:' block — first line at column 0 (non-space) after 'spring:' that isn't blank
const lines = src.split(/\r?\n/);
let inSpring = false, insertIdx = null;
for (let i = 0; i < lines.length; i++) {
  const ln = lines[i];
  if (ln.startsWith('spring:')) { inSpring = true; continue; }
  if (inSpring && ln.length && !/^\s/.test(ln) && ln.trim()) {
    insertIdx = i;
    break;
  }
}
if (insertIdx === null) { insertIdx = lines.length; }

// Insert cloudBlock right before insertIdx (preserve blank line)
const newLines = lines.slice(0, insertIdx).concat(cloudBlock.split('\n')).concat(lines.slice(insertIdx));
let out = newLines.join('\n');

// Append resilience4j top-level if absent
if (!out.includes('resilience4j:')) {
  out = out.replace(/\n+$/, '') + '\n\n' + r4j;
}

fs.writeFileSync(yml, out);
console.log(`${svc}: updated`);
