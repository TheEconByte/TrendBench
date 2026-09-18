import 'dotenv/config';
import { defineConfig } from 'prisma/config';

export default defineConfig({
  schema: 'prisma/schema.prisma',
  migrations: { path: 'prisma/migrations' },
  datasource: {
    // Client generation and production builds do not open a connection. Runtime
    // requests still fail closed in src/lib/prisma.ts when DATABASE_URL is absent.
    url: process.env.DATABASE_URL ?? 'postgresql://build:build@127.0.0.1:5433/build',
  },
});
