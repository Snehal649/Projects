import { useCallback, useEffect, useState } from 'react';
import { Bar, BarChart, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api from '../api.js';
import Money from '../components/Money.jsx';
import QuickAdd from '../components/QuickAdd.jsx';

const COLORS = ['#0f7b5f', '#11212f', '#d98e04', '#5b7c99', '#b83a2e', '#7d9d3c', '#8a6fb0', '#3c9db5', '#999'];
const barColor = (p) => (p >= 100 ? 'bg-brick' : p >= 80 ? 'bg-amber' : 'bg-leaf');

function HealthRing({ score }) {
  const r = 34, c = 2 * Math.PI * r;
  return (
    <svg width="90" height="90" viewBox="0 0 90 90" role="img" aria-label={`Health score ${score} out of 100`}>
      <circle cx="45" cy="45" r={r} fill="none" stroke="#e2e8f0" strokeWidth="9" />
      <circle cx="45" cy="45" r={r} fill="none" stroke="#0f7b5f" strokeWidth="9" strokeLinecap="round"
        strokeDasharray={`${(score / 100) * c} ${c}`} transform="rotate(-90 45 45)" />
      <text x="45" y="51" textAnchor="middle" className="font-display" fontSize="22" fontWeight="700">{score}</text>
    </svg>
  );
}

export default function Dashboard() {
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [d, setD] = useState(null);
  const [sts, setSts] = useState(null);
  const [hs, setHs] = useState(null);
  const [fc, setFc] = useState(null);

  const load = useCallback(() => {
    api.get('/dashboard/summary', { params: { month } }).then((r) => setD(r.data));
    api.get('/analytics/safe-to-spend').then((r) => setSts(r.data));
    api.get('/analytics/health-score').then((r) => setHs(r.data));
    api.get('/analytics/forecast').then((r) => setFc(r.data));
  }, [month]);
  useEffect(() => { load(); }, [load]);

  if (!d) return <p className="text-slate-600">Loading your numbers</p>;
  const hasBudgetsOver = d.budgets.filter((b) => b.percent >= 80);

  return (
    <div className="space-y-6">
      <QuickAdd onSaved={load} />

      <section className="grid gap-6 md:grid-cols-[1fr_auto] md:items-end">
        <div>
          <p className="text-sm text-slate-600">Safe to spend</p>
          <p className="font-display text-6xl font-bold tracking-tight md:text-7xl">
            {sts ? <Money v={sts.perDay} /> : '...'}<span className="ml-2 text-xl font-medium text-slate-500">a day</span>
          </p>
          {sts && <p className="mt-1 text-sm text-slate-600">
            For each of the next {sts.daysLeft} days, based on {sts.basis}. You have <Money v={Math.max(sts.remaining, 0)} /> left this month.
          </p>}
        </div>
        {hs && (
          <div className="flex items-center gap-3">
            <HealthRing score={hs.score} />
            <div className="text-sm"><p className="font-medium">Financial health</p><p className="text-slate-600">Saving {hs.savingsRate}% of income</p></div>
          </div>
        )}
      </section>

      <section className="grid grid-cols-2 gap-6 border-y border-slate-300 py-5 md:grid-cols-4">
        {[['Income', d.income], ['Spent', d.expense], ['Saved', d.savings], ['Net worth', d.netWorth]].map(([k, v]) => (
          <div key={k}><p className="text-sm text-slate-600">{k}</p><p className="font-display text-2xl font-semibold"><Money v={v} /></p></div>
        ))}
        <div className="col-span-2 md:col-span-4">
          <label className="label" htmlFor="m">Month</label>
          <input id="m" type="month" className="input max-w-44" value={month} onChange={(e) => setMonth(e.target.value)} />
        </div>
      </section>

      {fc?.warnings?.length > 0 && (
        <div className="rounded-lg border border-amber/40 bg-amber/10 p-4 text-sm">
          {fc.warnings.map((w) => <p key={w}>{w}</p>)}
        </div>
      )}

      <section className="grid gap-6 md:grid-cols-2">
        <div className="panel">
          <h2 className="h2 mb-3">Income and spending, last 6 months</h2>
          <div className="h-64">
            <ResponsiveContainer>
              <BarChart data={d.trend}>
                <XAxis dataKey="month" tickFormatter={(m) => m.slice(5)} />
                <YAxis width={50} />
                <Tooltip formatter={(v) => '₹' + Number(v).toLocaleString('en-IN')} />
                <Legend />
                <Bar dataKey="income" name="Income" fill="#0f7b5f" radius={[3, 3, 0, 0]} />
                <Bar dataKey="expense" name="Spent" fill="#11212f" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="panel">
          <h2 className="h2 mb-3">Where the money went</h2>
          {d.byCategory.length === 0 ? <p className="text-sm text-slate-600">No spending yet this month. Add one above.</p> : (
            <div className="h-64">
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={d.byCategory} dataKey="amount" nameKey="name" innerRadius={55} outerRadius={90}>
                    {d.byCategory.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                  </Pie>
                  <Tooltip formatter={(v) => '₹' + Number(v).toLocaleString('en-IN')} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </section>

      <section className="grid gap-6 md:grid-cols-2">
        <div className="panel">
          <h2 className="h2 mb-3">Budgets</h2>
          {d.budgets.length === 0 && <p className="text-sm text-slate-600">No budgets for this month. Set one on the Budgets page.</p>}
          <div className="space-y-4">
            {d.budgets.map((b) => (
              <div key={b.id}>
                <div className="mb-1 flex justify-between text-sm">
                  <span>{b.category}</span>
                  <span><Money v={b.spent} /> of <Money v={b.budget} /></span>
                </div>
                <div className="h-2 rounded-full bg-slate-200">
                  <div className={`h-2 rounded-full ${barColor(b.percent)}`} style={{ width: `${Math.min(b.percent, 100)}%` }} />
                </div>
              </div>
            ))}
          </div>
          {hasBudgetsOver.length > 0 && <p className="mt-3 text-sm text-amber">{hasBudgetsOver.length} budget(s) above 80%.</p>}
        </div>
        <div className="panel">
          <h2 className="h2 mb-3">Recent</h2>
          {d.recent.length === 0 && <p className="text-sm text-slate-600">Nothing here yet.</p>}
          <ul className="divide-y divide-slate-100">
            {d.recent.map((t) => (
              <li key={t.id} className="flex justify-between py-2 text-sm">
                <span>{t.merchant || t.category.name}<span className="block text-xs text-slate-500">{t.category.name}, {t.date}</span></span>
                <span className={t.type === 'INCOME' ? 'text-leaf' : ''}>{t.type === 'INCOME' ? '+' : '-'}<Money v={t.amount} /></span>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </div>
  );
}
