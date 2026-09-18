import { NextResponse } from 'next/server';
import type { Prisma } from '@/generated/prisma/client';
import { INPUT_SCHEMA_VERSION } from '@/features/finance/types';
import { planUpdateSchema } from '@/features/plans/schema';
import { apiError, internalError, invalidZod } from '@/lib/api';
import { getPrisma } from '@/lib/prisma';
import { currentUser } from '@/lib/session';

type Context = { params: Promise<{ planId: string }> };

export async function GET(_request: Request, context: Context) {
  try {
    const user = await currentUser();
    if (!user) return apiError(401, 'UNAUTHORIZED', '로그인이 필요합니다.');
    const { planId } = await context.params;
    const plan = await getPrisma().plan.findFirst({
      where: { id: planId, userId: user.id },
      include: { results: { orderBy: { calculatedAt: 'desc' }, select: { id: true, inputRevision: true, calculationVersion: true, calculatedAt: true } } },
    });
    if (!plan) return apiError(404, 'NOT_FOUND', '계획을 찾을 수 없습니다.');
    return NextResponse.json({ plan });
  } catch (error) {
    return internalError(error);
  }
}

export async function PUT(request: Request, context: Context) {
  try {
    const user = await currentUser();
    if (!user) return apiError(401, 'UNAUTHORIZED', '로그인이 필요합니다.');
    const parsed = planUpdateSchema.safeParse(await request.json());
    if (!parsed.success) return invalidZod(parsed.error);
    const { planId } = await context.params;
    const prisma = getPrisma();
    const result = await prisma.$transaction(async (tx) => {
      const changed = await tx.plan.updateMany({
        where: { id: planId, userId: user.id, revision: parsed.data.revision },
        data: {
          title: parsed.data.title,
          inputJson: parsed.data.input as Prisma.InputJsonValue,
          inputSchemaVersion: INPUT_SCHEMA_VERSION,
          revision: { increment: 1 },
        },
      });
      if (changed.count === 1) return tx.plan.findUniqueOrThrow({ where: { id: planId } });
      const exists = await tx.plan.findFirst({ where: { id: planId, userId: user.id }, select: { revision: true } });
      return exists ? { conflict: true as const, revision: exists.revision } : null;
    });
    if (!result) return apiError(404, 'NOT_FOUND', '계획을 찾을 수 없습니다.');
    if ('conflict' in result) return apiError(409, 'REVISION_CONFLICT', '다른 화면에서 계획이 먼저 수정되었습니다.', { currentRevision: result.revision });
    return NextResponse.json({ plan: result });
  } catch (error) {
    if (error instanceof SyntaxError) return apiError(400, 'INVALID_INPUT', '올바른 JSON 요청이 아닙니다.');
    return internalError(error);
  }
}

export async function DELETE(_request: Request, context: Context) {
  try {
    const user = await currentUser();
    if (!user) return apiError(401, 'UNAUTHORIZED', '로그인이 필요합니다.');
    const { planId } = await context.params;
    const deleted = await getPrisma().plan.deleteMany({ where: { id: planId, userId: user.id } });
    if (deleted.count === 0) return apiError(404, 'NOT_FOUND', '계획을 찾을 수 없습니다.');
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    return internalError(error);
  }
}
