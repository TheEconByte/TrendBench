import { z } from 'zod';

const wonString = z
  .string()
  .regex(/^(0|[1-9]\d*)$/, '0 이상의 원 단위 정수로 입력해 주세요.')
  .nullable();

const nonNegativeDecimalString = z
  .string()
  .regex(/^\d+(\.\d+)?$/, '0 이상의 숫자로 입력해 주세요.')
  .refine((value) => !value.startsWith('-'), '음수는 입력할 수 없습니다.')
  .nullable();

const rateString = nonNegativeDecimalString.refine(
  (value) => value === null || Number(value) < 1,
  '변동비율은 0 이상 1 미만이어야 합니다.',
);

export const financeInputSchema = z
  .object({
    openingExpenses: z.object({
      deposit: wonString,
      facilities: wonString,
      initialInventory: wonString,
      otherPreparation: wonString,
    }),
    targetReserve: wonString,
    equity: wonString,
    monthlyRevenue: wonString,
    monthlyFixedCosts: z.object({
      rent: wonString,
      labor: wonString,
      other: wonString,
    }),
    variableCostRate: rateString,
    existingMonthlyDebtPayment: wonString,
    newLoan: z.object({
      principal: wonString,
      annualInterestRatePercent: nonNegativeDecimalString,
      totalMonths: z.number().int('전체 상환개월은 정수여야 합니다.').positive('전체 상환개월은 1 이상이어야 합니다.').nullable(),
      graceMonths: z.number().int('거치개월은 정수여야 합니다.').nonnegative('거치개월은 0 이상이어야 합니다.').nullable(),
      repaymentMethod: z.enum(['EQUAL_PAYMENT', 'EQUAL_PRINCIPAL']).nullable(),
    }),
    cashBalanceMonths: z.number().int('현금잔액 기간은 정수여야 합니다.').positive('현금잔액 기간은 1개월 이상이어야 합니다.').max(120, '현금잔액 기간은 120개월 이하여야 합니다.'),
  })
  .superRefine((input, context) => {
    const { principal, totalMonths, graceMonths } = input.newLoan;
    if (principal !== null && principal !== '0' && totalMonths !== null && graceMonths !== null && graceMonths >= totalMonths) {
      context.addIssue({
        code: 'custom',
        path: ['newLoan', 'graceMonths'],
        message: '거치개월은 전체 상환개월보다 작아야 합니다.',
      });
    }
  });

export type ParsedFinanceInput = z.infer<typeof financeInputSchema>;
