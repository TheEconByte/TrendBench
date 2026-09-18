import { prismaAdapter } from '@better-auth/prisma-adapter';
import { betterAuth } from 'better-auth/minimal';
import { getPrisma } from './prisma';

export const auth = betterAuth({
  appName: 'TrendBench',
  database: prismaAdapter(getPrisma(), { provider: 'postgresql' }),
  emailAndPassword: {
    enabled: true,
    minPasswordLength: 8,
  },
  advanced: {
    database: {
      joins: true,
    },
  },
});
