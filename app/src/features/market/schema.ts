import { z } from 'zod';

export const marketAreasQuerySchema = z.object({
  districtCode: z.string().trim().min(1).max(20).optional(),
});
export const marketSummaryQuerySchema = z.object({
  areaCode: z.string().trim().min(1).max(20),
  industryCode: z.string().trim().min(1).max(20),
  areaType: z.string().trim().min(1).max(4).optional(),
});
