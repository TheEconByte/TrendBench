import { z } from 'zod';
import { financeInputSchema } from '@/features/finance/schema';

export const planWriteSchema = z.object({
  title: z.string().trim().min(1, '계획 제목을 입력해 주세요.').max(100, '계획 제목은 100자 이하여야 합니다.'),
  input: financeInputSchema,
});

export const planUpdateSchema = planWriteSchema.extend({
  revision: z.number().int().positive(),
});

export type PlanWriteInput = z.infer<typeof planWriteSchema>;
