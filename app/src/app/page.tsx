import FinancePlanner from '@/features/finance/FinancePlanner';

export default function Home() {
  return (
    <main>
      <header><a href="#top" className="brand">TrendBench<span>창업 준비의 기준</span></a><span className="badge">재무계획 v1</span></header>
      <div id="top"><FinancePlanner /></div>
      <footer>TrendBench · 예비 창업자를 위한 계획 도구</footer>
    </main>
  );
}
