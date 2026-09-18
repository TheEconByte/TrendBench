import Link from 'next/link';

const steps = [
  ['01', '상권 살펴보기', '관심 지역과 업종의 공개 지표를 확인합니다.'],
  ['02', '창업 계획 세우기', '초기 비용과 매출·운영비 가정을 정리합니다.'],
  ['03', '자금과 상환 검토하기', '부족자금과 가정한 대출의 부담을 살펴봅니다.'],
  ['04', '저장하고 비교하기', '가정을 바꾸어 계획의 차이를 확인합니다.'],
];

export default function Home() {
  return (
    <main>
      <header><Link href="/" className="brand">TrendBench<span>창업 준비의 기준</span></Link><span className="badge">개발 중</span></header>
      <section className="intro" aria-labelledby="title">
        <p className="eyebrow">나의 가정에서 시작하는 창업 계획</p>
        <h1 id="title">창업의 숫자를,<br />하나씩 확인하세요.</h1>
        <p className="description">관심 상권의 지표를 살펴보고, 예상 비용과 매출을 바탕으로<br className="desktop" /> 필요한 자금과 운영 계획을 정리합니다.</p>
        <p className="notice">서비스를 준비하고 있습니다. 현재는 계획 작성·계산·저장 기능을 제공하지 않습니다.</p>
      </section>
      <section aria-label="예정된 서비스 흐름" className="steps">
        {steps.map(([number, title, text]) => <article key={number}><span className="number">{number}</span><h2>{title}</h2><p>{text}</p></article>)}
      </section>
      <footer>TrendBench · 예비 창업자를 위한 계획 도구</footer>
    </main>
  );
}
