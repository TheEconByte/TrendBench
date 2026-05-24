import { useEffect, useState } from "react";
import { getHealth } from "./api/healthApi";

function App() {
  const [status, setStatus] = useState<string>("loading");

  useEffect(() => {
    getHealth()
      .then((data) => setStatus(data.status))
      .catch(() => setStatus("error"));
  }, []);

  return (
    <main style={{ padding: 24 }}>
      <h1>TrendBench</h1>
      <p>Backend status: {status}</p>
    </main>
  );
}

export default App;