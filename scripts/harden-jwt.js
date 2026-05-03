#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

const services = fs.readdirSync('services').filter(d => fs.statSync(path.join('services', d)).isDirectory());
const before = 'secret: ${JWT_SECRET:local-dev-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm}';
const after = 'secret: ${JWT_SECRET:?JWT_SECRET env var must be set (256+ bits)}';

let changed = 0;
for (const svc of services) {
  const f = path.join('services', svc, 'src', 'main', 'resources', 'application.yml');
  if (!fs.existsSync(f)) continue;
  const src = fs.readFileSync(f, 'utf8');
  if (src.includes(before)) {
    fs.writeFileSync(f, src.split(before).join(after));
    console.log(`updated: ${f}`);
    changed++;
  }
}
console.log(`${changed} files updated`);
