import { NextResponse } from 'next/server';
import type { Prisma } from '@/generated/prisma/client';
import { INPUT_SCHEMA_VERSION } from '@/features/finance/types';
import { planWriteSchema } from '@/features/plans/schema';
import { apiError, internalError, invalidZod } from '@/lib/api';
import { getPrisma } from '@/lib/prisma';
import { currentUser } from '@/lib/session';

export async function GET() {
  try {
    const user = await currentUser();
    if (!user) return apiError(401, 'UNAUTHORIZED', '로그인이 필요합니다.');
    const plans = await getPrisma().plan.findMany({
      where: { userId: user.id },
      orderBy: { updatedAt: 'desc' },
      select: { id: true, title: true, revision: true, inputSchemaVersion: true, createdAt: true, updatedAt: true, _count: { select: { results: true } } },
    });
    return NextResponse.json({ plans });
  } catch (error) {
    return internalError(error);
  }
}

export async function POST(request: Request) {
  try {
    const user = await currentUser();
    if (!user) return apiError(401, 'UNAUTHORIZED', '로그인이 필요합니다.');
    const parsed = planWriteSchema.safeParse(await request.json());
    if (!parsed.success) return invalidZod(parsed.error);
    const plan = await getPrisma().plan.create({
      data: {
        userId: user.id,
        title: parsed.data.title,
        inputJson: parsed.data.input as Prisma.InputJsonValue,
        inputSchemaVersion: INPUT_SCHEMA_VERSION,
      },
    });
    return NextResponse.json({ plan }, { status: 201 });
  } catch (error) {
    if (error instanceof SyntaxError) return apiError(400, 'INVALID_INPUT', '올바른 JSON 요청이 아닙니다.');
    return internalError(error);
  }
}
