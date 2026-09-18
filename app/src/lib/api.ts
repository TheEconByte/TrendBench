import { NextResponse } from 'next/server';
import { ZodError } from 'zod';

export type ApiErrorCode = 'UNAUTHORIZED' | 'NOT_FOUND' | 'INVALID_INPUT' | 'REVISION_CONFLICT' | 'INTERNAL_ERROR';

export function apiError(status: number, code: ApiErrorCode, message: string, fields?: unknown) {
  return NextResponse.json({ error: { code, message, ...(fields ? { fields } : {}) } }, { status });
}

export function invalidZod(error: ZodError) {
  return apiError(400, 'INVALID_INPUT', '요청 값을 확인해 주세요.', error.flatten());
}

export function internalError(error: unknown) {
  console.error('Request failed', error instanceof Error ? { name: error.name, message: error.message } : { name: 'UnknownError' });
  return apiError(500, 'INTERNAL_ERROR', '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.');
}
