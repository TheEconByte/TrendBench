'use client';

import { useCallback, useEffect, useState, type FormEvent } from 'react';
import FinancePlanner from '@/features/finance/FinancePlanner';
import type { FinanceInput, FinanceResult } from '@/features/finance/types';
import { authClient } from '@/lib/auth-client';

type PlanSummary = { id: string; title: string; revision: number; updatedAt: string; _count: { results: number } };
type ResultSummary = { id: string; inputRevision: number; calculationVersion: string; calculatedAt: string };
type PlanDetail = { id: string; title: string; revision: number; inputJson: FinanceInput; results: ResultSummary[] };
type StoredResult = { id: string; inputRevision: number; resultJson: FinanceResult };

async function api<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { ...init, headers: { 'content-type': 'application/json', ...init?.headers } });
  const body = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    const error = new Error(body?.error?.message ?? '요청을 처리하지 못했습니다.') as Error & { code?: string };
    error.code = body?.error?.code;
    throw error;
  }
  return body as T;
}

function AuthScreen() {
  const [mode, setMode] = useState<'signin' | 'signup'>('signin');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setMessage('');
    const result = mode === 'signup'
      ? await authClient.signUp.email({ name, email, password })
      : await authClient.signIn.email({ email, password });
    setBusy(false);
    if (result.error) setMessage(mode === 'signin' ? '이메일 또는 비밀번호를 확인해 주세요.' : result.error.message ?? '회원가입에 실패했습니다.');
  }
  return <main><header><span className="brand">TrendBench<span>창업 준비의 기준</span></span><div className="header-actions"><a className="link-button" href="/markets">상권 탐색</a><span className="badge">인증 필요</span></div></header><section className="auth-shell"><div><p className="eyebrow">개인 계획 보호</p><h1>계획을 저장하려면<br />로그인해 주세요.</h1><p>이메일은 로그인 식별에만 사용합니다. 실제 이메일 발송과 비밀번호 복구는 아직 제공하지 않습니다.</p></div><form className="auth-card" onSubmit={submit}><h2>{mode === 'signin' ? '로그인' : '회원가입'}</h2>{mode === 'signup' && <label className="field"><span>이름</span><span className="input-wrap"><input className="text-input" autoComplete="name" value={name} onChange={(e) => setName(e.target.value)} required /></span></label>}<label className="field"><span>이메일</span><span className="input-wrap"><input className="text-input" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></span></label><label className="field"><span>비밀번호</span><span className="input-wrap"><input className="text-input" type="password" minLength={8} autoComplete={mode === 'signin' ? 'current-password' : 'new-password'} value={password} onChange={(e) => setPassword(e.target.value)} required /></span><small>8자 이상 입력해 주세요.</small></label>{message && <p className="inline-error" role="alert">{message}</p>}<button disabled={busy}>{busy ? '처리 중…' : mode === 'signin' ? '로그인' : '회원가입'}</button><button className="link-button" type="button" onClick={() => { setMode(mode === 'signin' ? 'signup' : 'signin'); setMessage(''); }}>{mode === 'signin' ? '계정이 없나요? 회원가입' : '이미 계정이 있나요? 로그인'}</button></form></section></main>;
}

function AuthenticatedWorkspace({ email }: { email: string }) {
  const [plans, setPlans] = useState<PlanSummary[]>([]);
  const [plan, setPlan] = useState<PlanDetail | null>(null);
  const [storedResult, setStoredResult] = useState<StoredResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  const refreshPlans = useCallback(async () => {
    const data = await api<{ plans: PlanSummary[] }>('/api/plans');
    setPlans(data.plans);
  }, []);
  const openPlan = useCallback(async (id: string) => {
    setBusy(true); setMessage('');
    try {
      const data = await api<{ plan: PlanDetail }>(`/api/plans/${id}`);
      setPlan(data.plan); setStoredResult(null);
      if (data.plan.results[0]) {
        const resultData = await api<{ result: StoredResult }>(`/api/plans/${id}/results/${data.plan.results[0].id}`);
        setStoredResult(resultData.result);
      }
    } catch (error) { setMessage(error instanceof Error ? error.message : '계획을 불러오지 못했습니다.'); }
    finally { setBusy(false); }
  }, []);
  useEffect(() => {
    const timer = window.setTimeout(() => void refreshPlans().catch((error) => setMessage(error.message)), 0);
    return () => window.clearTimeout(timer);
  }, [refreshPlans]);

  async function saveDraft(title: string, input: FinanceInput) {
    setBusy(true); setMessage('');
    try {
      const data = plan
        ? await api<{ plan: PlanDetail }>(`/api/plans/${plan.id}`, { method: 'PUT', body: JSON.stringify({ title, input, revision: plan.revision }) })
        : await api<{ plan: PlanDetail }>('/api/plans', { method: 'POST', body: JSON.stringify({ title, input }) });
      setPlan({ ...data.plan, inputJson: input, results: plan?.results ?? [] });
      await refreshPlans(); setMessage('초안을 저장했습니다.');
      return data.plan;
    } catch (error) {
      const typed = error as Error & { code?: string };
      setMessage(typed.code === 'REVISION_CONFLICT' ? '다른 화면에서 먼저 수정되었습니다. 계획을 다시 열어 최신 내용을 확인해 주세요.' : typed.message);
      throw error;
    } finally { setBusy(false); }
  }
  async function calculate(title: string, input: FinanceInput) {
    const saved = await saveDraft(title, input);
    setBusy(true); setMessage('서버에서 저장된 입력을 계산하고 있습니다.');
    try {
      const data = await api<{ result: StoredResult; reused: boolean }>(`/api/plans/${saved.id}/calculations`, { method: 'POST' });
      setStoredResult(data.result);
      await openPlan(saved.id);
      setStoredResult(data.result);
      setMessage(data.reused ? '동일 revision의 기존 계산 결과를 불러왔습니다.' : '새 계산 결과를 저장했습니다.');
      return data.result.resultJson;
    } finally { setBusy(false); }
  }
  async function removePlan() {
    if (!plan || !window.confirm(`“${plan.title}” 계획과 저장 결과를 삭제할까요?`)) return;
    setBusy(true);
    try { await api(`/api/plans/${plan.id}`, { method: 'DELETE' }); setPlan(null); setStoredResult(null); await refreshPlans(); setMessage('계획을 삭제했습니다.'); }
    catch (error) { setMessage(error instanceof Error ? error.message : '삭제하지 못했습니다.'); }
    finally { setBusy(false); }
  }
  async function openResult(resultId: string) {
    if (!plan) return;
    const data = await api<{ result: StoredResult }>(`/api/plans/${plan.id}/results/${resultId}`);
    setStoredResult(data.result);
  }

  return <main><header><a href="#top" className="brand">TrendBench<span>창업 준비의 기준</span></a><div className="account-actions"><a className="link-button" href="/markets">상권 탐색</a><span>{email}</span><button className="link-button" onClick={() => void authClient.signOut()}>로그아웃</button></div></header><div className="workspace" id="top"><aside className="plan-sidebar"><div className="sidebar-heading"><h2>내 계획</h2><button onClick={() => { setPlan(null); setStoredResult(null); setMessage('새 계획을 작성합니다.'); }}>새 계획</button></div>{plans.length === 0 ? <p className="empty-state">저장된 계획이 없습니다.<br />첫 초안을 만들어 보세요.</p> : <ul>{plans.map((item) => <li key={item.id}><button className={plan?.id === item.id ? 'active' : ''} onClick={() => void openPlan(item.id)}><strong>{item.title}</strong><span>revision {item.revision} · 결과 {item._count.results}개</span></button></li>)}</ul>}{plan && <button className="danger-button" disabled={busy} onClick={() => void removePlan()}>이 계획 삭제</button>}</aside><div className="planner-column">{message && <div className="status-message" role="status">{message}</div>}{plan && plan.results.length > 0 && <div className="result-history"><strong>저장 결과</strong>{plan.results.map((result) => <button key={result.id} className={storedResult?.id === result.id ? 'active' : ''} onClick={() => void openResult(result.id)}>revision {result.inputRevision} · {new Date(result.calculatedAt).toLocaleString('ko-KR')}</button>)}</div>}<FinancePlanner key={plan?.id ?? 'new'} initialTitle={plan?.title} initialInput={plan?.inputJson} initialResult={storedResult?.resultJson} revision={plan?.revision ?? null} resultRevision={storedResult?.inputRevision ?? null} busy={busy} onSave={saveDraft} onCalculate={calculate} /></div></div><footer>TrendBench · 내부 MVP · 공개 출시 준비 완료 상태가 아닙니다.</footer></main>;
}

export default function TrendBenchApp() {
  const { data: session, isPending } = authClient.useSession();
  if (isPending) return <main><p className="loading-state">로그인 상태를 확인하고 있습니다.</p></main>;
  if (!session) return <AuthScreen />;
  return <AuthenticatedWorkspace key={session.user.id} email={session.user.email} />;
}
