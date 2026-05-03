#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

const services = fs.readdirSync('services').filter(d => fs.statSync(path.join('services', d)).isDirectory());
const block = `  tracing:
    sampling:
      probability: \${TRACING_SAMPLE:1.0}
  otlp:
    tracing:
      endpoint: \${OTLP_ENDPOINT:http://tempo:4318/v1/traces}
`;

let changed = 0;
for (const svc of services) {
  const f = path.join('services', svc, 'src', 'main', 'resources', 'application.yml');
  if (!fs.existsSync(f)) continue;
  const src = fs.readFileSync(f, 'utf8');
  if (src.includes('otlp:\n    tracing:') || src.includes('  tracing:\n    sampling:')) {
    console.log(`SKIP ${svc}: already configured`);
    continue;
  }
  // Find management: block and inject after metrics:.tags section. Look for a
  // line `      application: ${spring.application.name}` (the metrics tag).
  const lines = src.split(/\r?\n/);
  let mgmtStart = -1, insertIdx = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i] === 'management:') { mgmtStart = i; continue; }
    if (mgmtStart !== -1 && lines[i].length && !/^\s/.test(lines[i]) && lines[i].trim()) {
      insertIdx = i;
      break;
    }
  }
  if (mgmtStart === -1) {
    console.log(`SKIP ${svc}: no management: block`);
    continue;
  }
  if (insertIdx === -1) insertIdx = lines.length;
  const newLines = lines.slice(0, insertIdx).concat(block.split('\n')).concat(lines.slice(insertIdx));
  fs.writeFileSync(f, newLines.join('\n'));
  console.log(`OK ${svc}`);
  changed++;
}
console.log(`${changed} services updated`);
