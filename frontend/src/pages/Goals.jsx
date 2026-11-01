import { useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import Money from '../components/Money.jsx';

function Goal({ g, reload }) {
  const [monthly, setMonthly] = useState(5000);
  const [sim, setSim] = useState(null);
  const pct = Math.min(100, Math.round((g.savedAmount / g.targetAmount) * 100));

  useEffect(() => {
    api.get(`/goals/${g.id}/simulate`, { params: { monthly } }).then((r) => setSim(r.data));
  }, [g.id, g.savedAmount, monthly]);

  const add = async () => {
    const v = prompt('How much did you save towards this goal?');
    if (!v || Number(v) <= 0) return;
    await api.post(`/goals/${g.id}/contribute`, { amount: v });
    reload();
  };
  const remove = async () => {
    if (confirm('Delete this goal?')) { await api.delete(`/goals/${g.id}`); reload(); }
  };

  return (
    <div className="panel">
      <div className="flex justify-between"><h2 className="h2">{g.name}</h2>
        <button className="text-sm text-brick underline" onClick={remove}>Delete</button></div>
      <p className="mt-1 text-sm"><Money v={g.savedAmount} /> of <Money v={g.targetAmount} /> {g.deadline && `by ${g.deadline}`}</p>
      <div className="mt-2 h-2 rounded-full bg-slate-200"><div className="h-2 rounded-full bg-leaf" style={{ width: `${pct}%` }} /></div>
      <button className="btn mt-3" onClick={add}>Add savings</button>

      <div className="mt-5 rounded-md bg-mist p-4">
        <label className="label" htmlFor={`s${g.id}`}>What if I save <Money v={monthly} /> a month?</label>
        <input id={`s${g.id}`} type="range" min="500" max="100000" step="500" value={monthly}
          onChange={(e) => setMonthly(Number(e.target.value))} className="w-full accent-leaf" />
        {sim && (
          <p className="mt-2 text-sm">
            {sim.monthsNeeded === 0 ? 'Goal already reached.' : <>You will reach it in {sim.monthsNeeded} month(s), around <b>{sim.completionDate}</b>. </>}
            {g.deadline && (sim.onTrack ? <span className="text-leaf">That meets your deadline.</span> : <span className="text-brick">That misses your deadline.</span>)}
            {sim.requiredMonthly && <span className="block text-slate-600">To hit the deadline, save <Money v={sim.requiredMonthly} /> a month.</span>}
          </p>
        )}
      </div>
    </div>
  );
}

export default function Goals() {
  const [goals, setGoals] = useState([]);
  const [form, setForm] = useState({ name: '', targetAmount: '', deadline: '' });
  const [err, setErr] = useState('');
  const load = () => api.get('/goals').then((r) => setGoals(r.data));
  useEffect(() => { load(); }, []);

  const create = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      await api.post('/goals', { ...form, deadline: form.deadline || null });
      setForm({ name: '', targetAmount: '', deadline: '' });
      load();
    } catch (ex) { setErr(errMsg(ex)); }
  };

  return (
    <div className="space-y-6">
      <h1 className="font-display text-3xl font-bold">Savings goals</h1>
      <form onSubmit={create} className="panel flex flex-wrap items-end gap-3">
        <div><label className="label">Goal</label><input className="input" required value={form.name} placeholder="Laptop" onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
        <div><label className="label">Target amount</label><input className="input" type="number" min="1" required value={form.targetAmount} onChange={(e) => setForm({ ...form, targetAmount: e.target.value })} /></div>
        <div><label className="label">Deadline (optional)</label><input className="input" type="date" value={form.deadline} onChange={(e) => setForm({ ...form, deadline: e.target.value })} /></div>
        <button className="btn">Create goal</button>
        {err && <p className="w-full text-sm text-brick">{err}</p>}
      </form>
      {goals.length === 0 && <p className="text-sm text-slate-600">No goals yet. Create one to see when you can reach it.</p>}
      <div className="grid gap-4 md:grid-cols-2">{goals.map((g) => <Goal key={g.id} g={g} reload={load} />)}</div>
    </div>
  );
}
