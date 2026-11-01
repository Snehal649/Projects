import { useCallback, useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import Money from '../components/Money.jsx';

export default function Budgets() {
  const now = new Date();
  const [month] = useState(now.getMonth() + 1);
  const [year] = useState(now.getFullYear());
  const [list, setList] = useState([]);
  const [cats, setCats] = useState([]);
  const [form, setForm] = useState({ categoryId: '', amount: '' });
  const [err, setErr] = useState('');

  const load = useCallback(() => api.get('/budgets', { params: { month, year } }).then((r) => setList(r.data)), [month, year]);
  useEffect(() => {
    load();
    api.get('/categories').then((r) => setCats(r.data.filter((c) => c.type === 'EXPENSE')));
  }, [load]);

  const save = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      await api.post('/budgets', { categoryId: Number(form.categoryId || cats[0]?.id), amount: form.amount, month, year });
      setForm({ categoryId: '', amount: '' });
      load();
    } catch (ex) { setErr(errMsg(ex)); }
  };
  const remove = async (id) => { await api.delete(`/budgets/${id}`); load(); };

  return (
    <div className="space-y-6">
      <h1 className="font-display text-3xl font-bold">Budgets for {now.toLocaleString('en-IN', { month: 'long', year: 'numeric' })}</h1>
      <form onSubmit={save} className="panel flex flex-wrap items-end gap-3">
        <div><label className="label">Category</label>
          <select className="input" value={form.categoryId || cats[0]?.id || ''} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
            {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
        <div><label className="label">Monthly limit</label>
          <input className="input" type="number" min="1" required value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></div>
        <button className="btn">Set budget</button>
        {err && <p className="w-full text-sm text-brick">{err}</p>}
      </form>

      {list.length === 0 && <p className="text-sm text-slate-600">No budgets yet. Pick a category and set a monthly limit.</p>}
      <div className="grid gap-4 md:grid-cols-2">
        {list.map((b) => {
          const color = b.percent >= 100 ? 'bg-brick' : b.percent >= 80 ? 'bg-amber' : 'bg-leaf';
          return (
            <div key={b.id} className="panel">
              <div className="flex justify-between"><h2 className="h2">{b.category}</h2>
                <button className="text-sm text-brick underline" onClick={() => remove(b.id)}>Remove</button></div>
              <p className="mt-1 text-sm"><Money v={b.spent} /> of <Money v={b.budget} /> ({b.percent}%)</p>
              <div className="mt-2 h-2 rounded-full bg-slate-200"><div className={`h-2 rounded-full ${color}`} style={{ width: `${Math.min(b.percent, 100)}%` }} /></div>
              {b.percent >= 100 && <p className="mt-2 text-sm text-brick">Over budget by <Money v={b.spent - b.budget} />.</p>}
              {b.percent >= 80 && b.percent < 100 && <p className="mt-2 text-sm text-amber">You have used over 80% of this budget.</p>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
