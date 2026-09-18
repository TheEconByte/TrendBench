'use client';

import Decimal from 'decimal.js';
import { useState, type FormEvent } from 'react';
import { calculateFinance } from './calculate';
import { financeInputSchema } from './schema';
import type { FinanceInput, FinanceResult, ScenarioResult, ValueResult } from './types';

type FormState = Record<string, string>;
const initialForm: FormState = {
  deposit: '30000000', facilities: '40000000', initialInventory: '5000000', otherPreparation: '5000000', targetReserve: '10000000', equity: '40000000',
  monthlyRevenue: '20000000', rent: '3000000', labor: '3000000', otherFixed: '1000000', variableCostPercent: '35', existingDebt: '0',
  loanPrincipal: '50000000', annualRatePercent: '5', totalMonths: '60', graceMonths: '0', repaymentMethod: 'EQUAL_PAYMENT', cashBalanceMonths: '12',
};
const labels: Record<string, string> = {
  'openingExpenses.deposit': '보증금', 'openingExpenses.facilities': '시설비', 'openingExpenses.initialInventory': '초기 재고', 'openingExpenses.otherPreparation': '기타 준비비',
  targetReserve: '목표 운영예비금', equity: '자기자금', monthlyRevenue: '월매출 가정', 'monthlyFixedCosts.rent': '월 임대료',
  'monthlyFixedCosts.labor': '월 인건비', 'monthlyFixedCosts.other': '기타 월 고정비', variableCostRate: '변동비율', existingMonthlyDebtPayment: '기존 월 상환액',
  'newLoan.principal': '신규 대출금', 'newLoan.annualInterestRatePercent': '연 금리', 'newLoan.totalMonths': '전체 상환개월', 'newLoan.graceMonths': '원금 거치개월', 'newLoan.repaymentMethod': '상환방식',
  cashBalanceMonths: '현금잔액 계산기간',
};
const moneyFields = new Set(['deposit', 'facilities', 'initialInventory', 'otherPreparation', 'targetReserve', 'equity', 'monthlyRevenue', 'rent', 'labor', 'otherFixed', 'existingDebt', 'loanPrincipal']);
const nullable = (value: string) => value.trim() === '' ? null : value.replaceAll(',', '').trim();
const integerOrNull = (value: string) => value.trim() === '' ? null : Number(value);
const percentToRatio = (value: string) => { if (value.trim() === '') return null; try { return new Decimal(value).div(100).toString(); } catch { return value; } };
const formatInput = (key: string, value: string) => moneyFields.has(key) && /^\d+$/.test(value.replaceAll(',', '')) ? new Intl.NumberFormat('ko-KR').format(BigInt(value.replaceAll(',', ''))) : value;
const formatWon = (value: string) => `${new Intl.NumberFormat('ko-KR').format(BigInt(value))}원`;

function toInput(form: FormState): FinanceInput {
  return {
    openingExpenses: { deposit: nullable(form.deposit), facilities: nullable(form.facilities), initialInventory: nullable(form.initialInventory), otherPreparation: nullable(form.otherPreparation) },
    targetReserve: nullable(form.targetReserve), equity: nullable(form.equity), monthlyRevenue: nullable(form.monthlyRevenue),
    monthlyFixedCosts: { rent: nullable(form.rent), labor: nullable(form.labor), other: nullable(form.otherFixed) },
    variableCostRate: percentToRatio(form.variableCostPercent), existingMonthlyDebtPayment: nullable(form.existingDebt),
    newLoan: { principal: nullable(form.loanPrincipal), annualInterestRatePercent: nullable(form.annualRatePercent), totalMonths: integerOrNull(form.totalMonths), graceMonths: integerOrNull(form.graceMonths), repaymentMethod: form.repaymentMethod === '' ? null : form.repaymentMethod as FinanceInput['newLoan']['repaymentMethod'] },
    cashBalanceMonths: Number(form.cashBalanceMonths),
  };
}

function MoneyInput({ id, label, form, setForm, help }: { id: string; label: string; form: FormState; setForm: (next: FormState) => void; help?: string }) {
  return <label className="field" htmlFor={id}><span>{label}</span><span className="input-wrap"><input id={id} inputMode="numeric" value={formatInput(id, form[id])} onChange={(event) => setForm({ ...form, [id]: event.target.value.replaceAll(',', '') })} aria-describedby={help ? `${id}-help` : undefined} /><b>원</b></span>{help && <small id={`${id}-help`}>{help}</small>}</label>;
}
function showValue<T>(result: ValueResult<T>, render: (value: T) => string) {
  if (result.status === 'READY') return render(result.value);
  if (result.status === 'UNSUPPORTED') return `계산 불가 · ${result.reason}`;
  return `입력 필요 · ${result.missingFields.map((field) => labels[field] ?? field).join(', ')}`;
}
const scenarioLabel = { BASE: '기본', ADVERSE: '악화', IMPROVED: '개선' };
const fundingLabel = {
  INITIAL_EXPENSE_SHORTFALL: '초기 지출 자체를 충당하지 못합니다.',
  RESERVE_SHORTFALL: '초기 지출은 충당하지만 목표 예비금이 부족합니다.',
  TARGET_RESERVE_MET: '가정한 조달 후 목표 예비금까지 충족합니다.',
};
function ScenarioCard({ scenario }: { scenario: ScenarioResult }) {
  return <article className="scenario-card"><div className="scenario-heading"><h3>{scenarioLabel[scenario.name]}</h3><span>{scenario.name === 'BASE' ? '입력한 원래 가정' : '스트레스 가정'}</span></div>
    {scenario.changes.length > 0 && <ul className="changes">{scenario.changes.map((change) => <li key={change.field}>{change.description}</li>)}</ul>}
    <dl><div><dt>월 운영수지</dt><dd>{showValue(scenario.monthlyOperatingBalance, formatWon)}</dd></div><div><dt>운영비 충당 매출</dt><dd>{showValue(scenario.operatingBreakEvenRevenue, formatWon)}</dd></div>
      <div><dt>상환 포함 균형 매출</dt><dd>{showValue(scenario.debtInclusiveBreakEvenRevenue, formatWon)}</dd></div><div><dt>첫 달 월 상환액</dt><dd>{showValue(scenario.firstMonthLoanPayment, formatWon)}</dd></div>
      <div><dt>첫 달 상환 후 잔여현금</dt><dd>{showValue(scenario.firstMonthCashAfterDebtPayment, formatWon)}</dd></div><div><dt>현금잔액 첫 음수 월</dt><dd>{showValue(scenario.firstNegativeCashMonth, (month) => month === null ? '기간 내 없음' : `${month}개월 차`)}</dd></div></dl>
  </article>;
}

export default function FinancePlanner() {
  const [form, setForm] = useState(initialForm); const [errors, setErrors] = useState<string[]>([]); const [result, setResult] = useState<FinanceResult | null>(null);
  function submit(event: FormEvent) {
    event.preventDefault(); const parsed = financeInputSchema.safeParse(toInput(form));
    if (!parsed.success) { setErrors(parsed.error.issues.map((issue) => `${labels[issue.path.join('.')] ?? issue.path.join('.')}: ${issue.message}`)); setResult(null); requestAnimationFrame(() => document.querySelector<HTMLElement>('#validation-errors')?.focus()); return; }
    setErrors([]); setResult(calculateFinance(parsed.data as FinanceInput)); requestAnimationFrame(() => document.querySelector<HTMLElement>('#results')?.focus());
  }
  const base = result?.scenarios[0];
  return <><section className="hero"><p className="eyebrow">DB·외부 API 없이 계산하는 첫 계획</p><h1>내 가정으로 확인하는<br />창업 재무계획</h1><p>모르는 선택 항목은 비워 두세요. 입력한 값만으로 계산 가능한 결과를 먼저 보여드립니다.</p></section>
    <form onSubmit={submit} noValidate>
      <section className="form-section"><div className="section-title"><span>01</span><div><h2>개업 전 자금</h2><p>개업 전에 실제로 나갈 지출과 남겨 둘 예비금을 구분합니다.</p></div></div><div className="field-grid">
        <MoneyInput id="deposit" label="보증금" form={form} setForm={setForm} /><MoneyInput id="facilities" label="시설비" form={form} setForm={setForm} /><MoneyInput id="initialInventory" label="초기 재고" form={form} setForm={setForm} /><MoneyInput id="otherPreparation" label="기타 준비비" form={form} setForm={setForm} /><MoneyInput id="targetReserve" label="목표 운영예비금" form={form} setForm={setForm} /><MoneyInput id="equity" label="자기자금" form={form} setForm={setForm} />
      </div></section>
      <section className="form-section"><div className="section-title"><span>02</span><div><h2>월 운영 가정</h2><p>공공 상권 매출이 아닌, 사용자가 세운 월매출 가정입니다.</p></div></div><div className="field-grid">
        <MoneyInput id="monthlyRevenue" label="월매출 가정 (선택)" form={form} setForm={setForm} help="모르면 비워도 초기 자금 결과는 계산됩니다." /><MoneyInput id="rent" label="월 임대료" form={form} setForm={setForm} /><MoneyInput id="labor" label="월 인건비" form={form} setForm={setForm} /><MoneyInput id="otherFixed" label="기타 월 고정비" form={form} setForm={setForm} />
        <label className="field" htmlFor="variableCostPercent"><span>매출 대비 변동비율</span><span className="input-wrap"><input id="variableCostPercent" inputMode="decimal" value={form.variableCostPercent} onChange={(e) => setForm({ ...form, variableCostPercent: e.target.value })} /><b>%</b></span><small>재료비처럼 매출에 따라 변하는 비용입니다. 100% 미만.</small></label><MoneyInput id="existingDebt" label="기존 월 상환액" form={form} setForm={setForm} />
      </div></section>
      <section className="form-section"><div className="section-title"><span>03</span><div><h2>신규 대출 가정</h2><p>부족자금이 대출금으로 자동 입력되지 않습니다. 무차입은 0원 또는 빈칸으로 두세요.</p></div></div><div className="field-grid">
        <MoneyInput id="loanPrincipal" label="신규 대출금" form={form} setForm={setForm} /><label className="field" htmlFor="annualRatePercent"><span>연 금리</span><span className="input-wrap"><input id="annualRatePercent" inputMode="decimal" value={form.annualRatePercent} onChange={(e) => setForm({ ...form, annualRatePercent: e.target.value })} /><b>%</b></span></label>
        <label className="field" htmlFor="totalMonths"><span>전체 상환개월</span><span className="input-wrap"><input id="totalMonths" inputMode="numeric" value={form.totalMonths} onChange={(e) => setForm({ ...form, totalMonths: e.target.value })} /><b>개월</b></span></label><label className="field" htmlFor="graceMonths"><span>원금 거치개월</span><span className="input-wrap"><input id="graceMonths" inputMode="numeric" value={form.graceMonths} onChange={(e) => setForm({ ...form, graceMonths: e.target.value })} /><b>개월</b></span><small>거치 중에는 이자만 내며 전체 기간에 포함됩니다.</small></label>
        <label className="field" htmlFor="repaymentMethod"><span>상환방식</span><select id="repaymentMethod" value={form.repaymentMethod} onChange={(e) => setForm({ ...form, repaymentMethod: e.target.value })}><option value="EQUAL_PAYMENT">원리금균등</option><option value="EQUAL_PRINCIPAL">원금균등</option></select></label>
        <label className="field" htmlFor="cashBalanceMonths"><span>현금잔액 계산기간</span><span className="input-wrap"><input id="cashBalanceMonths" inputMode="numeric" value={form.cashBalanceMonths} onChange={(e) => setForm({ ...form, cashBalanceMonths: e.target.value })} /><b>개월</b></span><small>1~120개월, 기본 12개월입니다.</small></label>
      </div></section>
      {errors.length > 0 && <section id="validation-errors" className="error-box" tabIndex={-1} aria-live="assertive"><h2>입력을 확인해 주세요</h2><ul>{errors.map((error) => <li key={error}>{error}</li>)}</ul></section>}
      <div className="calculate-bar"><div><strong>입력을 바꾸어 다시 계산할 수 있습니다.</strong><span>저장이나 외부 전송은 하지 않습니다.</span></div><button type="submit">재무계획 계산하기</button></div>
    </form>
    {result && base && <section id="results" className="results" tabIndex={-1} aria-live="polite"><div className="results-head"><p className="eyebrow">계산 결과</p><h2>가정의 결과를 나란히 확인하세요.</h2><p>아래 값은 예측이나 승인 결과가 아니라, 입력한 가정에 따른 단순 재무계획입니다.</p></div>
      <div className="summary-grid"><div><span>목표 초기 필요자금</span><strong>{showValue(result.targetInitialFunding, formatWon)}</strong></div><div><span>계획상 부족자금</span><strong>{showValue(result.plannedFundingGap, formatWon)}</strong></div><div><span>조달 후 개업 직전 현금</span><strong>{showValue(result.preOpeningCashAfterFunding, formatWon)}</strong></div><div><span>목표 예비금 미달액</span><strong>{showValue(result.reserveShortfall, formatWon)}</strong></div></div>
      <p className="funding-position">초기 자금 상태 · {showValue(result.fundingPosition, (value) => fundingLabel[value])}</p>
      <div className="scenario-grid">{result.scenarios.map((scenario) => <ScenarioCard key={scenario.name} scenario={scenario} />)}</div>
      {base.cashBalances.status === 'READY' && <div className="table-card"><h3>기본 가정 · {form.cashBalanceMonths}개월 현금잔액</h3><div className="table-scroll"><table><thead><tr><th>월</th><th>신규 대출 상환액</th><th>상환 후 잔여현금</th><th>월말 현금잔액</th></tr></thead><tbody>{base.cashBalances.value.map((row) => <tr key={row.month}><td>{row.month}개월</td><td>{formatWon(row.loanPayment)}</td><td>{formatWon(row.cashAfterDebtPayment)}</td><td>{formatWon(row.closingCashBalance)}</td></tr>)}</tbody></table></div></div>}
      {result.loan.status === 'READY' && result.loan.value.schedule.length > 0 && <div className="table-card"><div className="loan-summary"><h3>월별 대출 일정</h3><p>첫 달 {formatWon(result.loan.value.firstPayment)} · 거치 종료 직후 {formatWon(result.loan.value.firstPaymentAfterGrace)} · 최대 {formatWon(result.loan.value.maximumPayment)} · 총 이자 {formatWon(result.loan.value.totalInterest)}</p></div><div className="table-scroll"><table><thead><tr><th>월</th><th>구간</th><th>원금</th><th>이자</th><th>상환액</th><th>남은 원금</th></tr></thead><tbody>{result.loan.value.schedule.map((row) => <tr key={row.month}><td>{row.month}</td><td>{row.phase === 'GRACE' ? '거치' : '상환'}</td><td>{formatWon(row.principal)}</td><td>{formatWon(row.interest)}</td><td>{formatWon(row.payment)}</td><td>{formatWon(row.remainingPrincipal)}</td></tr>)}</tbody></table></div></div>}
      <aside className="scope-note"><h3>계산에 포함되지 않은 범위</h3><p>{result.excludedScope.join(' · ')}</p><ul><li>공공 상권 매출을 개인 예상매출로 사용하지 않습니다.</li><li>결과는 자금 후보나 대출 승인 결과가 아닙니다.</li><li>원 단위 결과는 0.5원 이상 올림으로 반올림하며, 거치기간은 전체 상환기간에 포함합니다.</li></ul><small>입력 {result.inputSchemaVersion} · 계산 {result.calculationVersion}</small></aside>
    </section>}
  </>;
}
