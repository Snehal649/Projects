import { useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import Money from '../components/Money.jsx';

export default function Accounts() {
  const [list, setList] = useState([]);
  const [form, setForm] = useState({ name: '', type: 'BANK', balance: '' });
  const [err, setErr] = useState('');
  const load = () => api.get('/accounts').then((r) => setList(r.data));
  useEffect(() => { load(); }, []);

  const create = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      await api.post('/accounts', { ...form, balance: form.balance || 0 });
      setForm({ name: '', type: 'BANK', balance: '' });
      load();
    } catch (ex) { setErr(errMsg(ex)); }
  };
  const remove = async (id) => {
    try { await api.delete(`/accounts/${id}`); load(); }
    catch (ex) { setErr(errMsg(ex, 'Could not delete. Accounts with transactions cannot be removed.')); }
  };

  const total = list.reduce((s, a) => s + Number(a.balance), 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-bold">Accounts</h1>
        <p className="text-sm text-slate-600">Net worth <Money v={total} className="font-medium" /></p>
      </div>
      <form onSubmit={create} className="panel flex flex-wrap items-end gap-3">
        <div><label className="label">Name</label><input className="input" required placeholder="HDFC Bank" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
        <div><label className="label">Type</label>
          <select className="input" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
            {['CASH', 'BANK', 'CARD', 'WALLET'].map((t) => <option key={t}>{t}</option>)}</select></div>
        <div><label className="label">Opening balance</label><input className="input" type="number" value={form.balance} onChange={(e) => setForm({ ...form, balance: e.target.value })} /></div>
        <button className="btn">Add account</button>
      </form>
      {err && <p className="text-sm text-brick">{err}</p>}
      <div className="grid gap-4 md:grid-cols-3">
        {list.map((a) => (
          <div key={a.id} className="panel">
            <p className="text-xs text-slate-500">{a.type}</p>
            <h2 className="h2">{a.name}</h2>
            <p className="font-display text-2xl font-semibold"><Money v={a.balance} /></p>
            <button className="mt-2 text-sm text-brick underline" onClick={() => remove(a.id)}>Delete</button>
          </div>
        ))}
      </div>
    </div>
  );
}
