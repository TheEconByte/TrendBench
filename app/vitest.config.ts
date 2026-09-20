import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

// Mutation tests never reuse the application's DATABASE_URL. They run only
// when an explicit TEST_DATABASE_URL is supplied by the environment or the
// test-only env file.
function testDatabaseUrl(): string {
  if (process.env.TEST_DATABASE_URL) return process.env.TEST_DATABASE_URL;
  if (!existsSync('.env.test.local')) return '';
  return readFileSync('.env.test.local', 'utf8').match(/^TEST_DATABASE_URL=(.*)$/m)?.[1]?.trim() ?? '';
}

export default defineConfig({
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  test: { env: { TEST_DATABASE_URL: testDatabaseUrl() } },
});
