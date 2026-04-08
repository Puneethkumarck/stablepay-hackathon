#!/usr/bin/env node

/**
 * Waits for the backend health endpoint to return UP before running tests.
 * Usage: node scripts/wait-for-backend.js [url] [timeout-seconds]
 */

const url = process.argv[2] || 'http://localhost:8080/actuator/health';
const timeoutSec = parseInt(process.argv[3] || '120', 10);
const pollIntervalMs = 3000;

async function check() {
  try {
    const res = await fetch(url);
    if (res.ok) {
      const body = await res.json();
      return body.status === 'UP';
    }
  } catch {
    // connection refused — backend not up yet
  }
  return false;
}

async function main() {
  const deadline = Date.now() + timeoutSec * 1000;
  process.stdout.write(`Waiting for backend at ${url} (timeout: ${timeoutSec}s)`);

  while (Date.now() < deadline) {
    if (await check()) {
      console.log('\nBackend is UP — starting tests.');
      process.exit(0);
    }
    process.stdout.write('.');
    await new Promise(r => setTimeout(r, pollIntervalMs));
  }

  console.error(`\nBackend did not become healthy within ${timeoutSec}s.`);
  process.exit(1);
}

main();
