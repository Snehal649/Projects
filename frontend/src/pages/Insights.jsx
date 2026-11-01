import { useEffect, useState } from 'react';
import api from '../api.js';
import Money from '../components/Money.jsx';

export default function Insights() {
  const [fc, setFc] = useState(null);
  const [an, setAn] = useState([]);
  const [subs, setSubs] = useState([]);
  const [hs, setHs] = useState(null);

  useEffect(() => {
    api.get('/analytics/forecast').then((r) => setFc(r.data));
    api.get('/analytics/anomalies').then((r) => setAn(r.data));
    api.get('/analytics/subscriptions').then((r) => setSubs(r.data));
    api.get('/analytics/health-score').then((r) => setHs(r.data));
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="font-display text-3xl font-bold">Insights</h1>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="panel">
          <h2 className="h2 mb-2">Month-end forecast</h2>
          {fc && <>
            <p className="text-sm">Spent so far <Money v={fc.spentSoFar} />. At this pace you will spend about <b><Money v={fc.projectedExpense} /></b> by month end.</p>
            {fc.warnings.map((w) => <p key={w} className="mt-2 text-sm text-amber">{w}</p>)}
            {fc.warnings.length === 0 && <p className="mt-2 text-sm text-leaf">Every budget is on track.</p>}
          </>}
        </div>
        <div className="panel">
          <h2 className="h2 mb-2">How to improve your health score</h2>
          {hs && (hs.tips.length ? <ul className="list-disc space-y-1 pl-5 text-sm">{hs.tips.map((t) => <li key={t}>{t}</li>)}</ul>
            : <p className="text-sm text-leaf">You are doing well. Keep it up.</p>)}
          {hs && <p className="mt-3 text-sm text-slate-600">Emergency fund covers {hs.emergencyMonths} months of spending.</p>}
        </div>
      </div>

      <div className="panel">
        <h2 className="h2 mb-2">Unusual transactions</h2>
        {an.length === 0 ? <p className="text-sm text-slate-600">Nothing unusual in the last 30 days.</p> : (
          <ul className="divide-y divide-slate-100">
            {an.map((a) => (
              <li key={a.id} className="py-2 text-sm"><b>{a.merchant}</b> for <Money v={a.amount} /> on {a.date}
                <span className="block text-slate-600">{a.reason}</span></li>
            ))}
          </ul>
        )}
      </div>

      <div className="panel">
        <h2 className="h2 mb-2">Subscriptions we found</h2>
        {subs.length === 0 ? <p className="text-sm text-slate-600">No repeating monthly charges yet. They appear after two months of the same merchant and amount.</p> : (
          <table className="w-full text-left text-sm">
            <thead className="text-slate-500"><tr><th className="py-2">Merchant</th><th>Monthly</th><th>Next charge</th><th className="text-right">Per year</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {subs.map((s) => (
                <tr key={s.merchant}><td className="py-2">{s.merchant}</td><td><Money v={s.amount} /></td>
                  <td>{s.nextExpected}</td><td className="text-right"><Money v={s.yearlyCost} /></td></tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
