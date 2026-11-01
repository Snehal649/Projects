import { useCallback, useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import Money from '../components/Money.jsx';
import QuickAdd from '../components/QuickAdd.jsx';

const today = () => new Date().toISOString().slice(0, 10);
const blank = { type: 'EXPENSE', amount: '', date: today(), accountId: '', categoryId: '', merchant: '', note: '' };

export default function Transactions() {
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState({ page: 0, totalPages: 0 });
  const [accounts, setAccounts] = useState([]);
  const [cats, setCats] = useState([]);
  const [filter, setFilter] = useState({ from: '', to: '', category: '', type: '' });
  const [form, setForm] = useState(blank);
  const [editId, setEditId] = useState(null);
  const [err, setErr] = useState('');

  const load = useCallback((page = 0) => {
    const params = { page, size: 15 };
    Object.entries(filter).forEach(([k, v]) => v && (params[k] = v));
    api.get('/transactions', { params }).then((r) => {
      setRows(r.data.content);
      setMeta({ page: r.data.page, totalPages: r.data.totalPages });
    });
  }, [filter]);

  useEffect(() => {
    api.get('/accounts').then((r) => setAccounts(r.data));
    api.get('/categories').then((r) => setCats(r.data));
  }, []);
  useEffect(() => { load(0); }, [load]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const catOptions = cats.filter((c) => c.type === form.type);

  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    const body = { ...form, accountId: Number(form.accountId || accounts[0]?.id), categoryId: Number(form.categoryId || catOptions[0]?.id) };
    try {
      if (editId) await api.put(`/transactions/${editId}`, body);
      else await api.post('/transactions', body);
      setForm(blank);
      setEditId(null);
      load(meta.page);
    } catch (ex) { setErr(errMsg(ex)); }
  };

  const edit = (t) => {
    setEditId(t.id);
    setForm({ type: t.type, amount: t.amount, date: t.date, accountId: t.account.id, categoryId: t.category.id,
      merchant: t.merchant || '', note: t.note || '' });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const remove = async (id) => {
    if (!confirm('Delete this transaction? The account balance will be adjusted.')) return;
    await api.delete(`/transactions/${id}`);
    load(meta.page);
  };

  const exportCsv = async () => {
    const from = filter.from || '2000-01-01', to = filter.to || today();
    const res = await api.get('/reports/export', { params: { from, to }, responseType: 'blob' });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url; a.download = `fintrack-${from}-to-${to}.csv`; a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <h1 className="font-display text-3xl font-bold">Transactions</h1>
      <QuickAdd onSaved={() => load(0)} />

      <form onSubmit={submit} className="panel grid grid-cols-2 gap-3 md:grid-cols-4">
        <h2 className="h2 col-span-full">{editId ? 'Edit transaction' : 'Add transaction'}</h2>
        <div><label className="label">Type</label>
          <select className="input" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value, categoryId: '' })}>
            <option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select></div>
        <div><label className="label">Amount</label><input className="input" type="number" step="0.01" min="0.01" required value={form.amount} onChange={set('amount')} /></div>
        <div><label className="label">Date</label><input className="input" type="date" required value={form.date} onChange={set('date')} /></div>
        <div><label className="label">Account</label>
          <select className="input" value={form.accountId || accounts[0]?.id || ''} onChange={set('accountId')}>
            {accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}</select></div>
        <div><label className="label">Category</label>
          <select className="input" value={form.categoryId || catOptions[0]?.id || ''} onChange={set('categoryId')}>
            {catOptions.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
        <div><label className="label">Merchant</label><input className="input" value={form.merchant} onChange={set('merchant')} /></div>
        <div className="col-span-2"><label className="label">Note</label><input className="input" value={form.note} onChange={set('note')} /></div>
        {err && <p className="col-span-full text-sm text-brick">{err}</p>}
        <div className="col-span-full flex gap-2">
          <button className="btn">{editId ? 'Save changes' : 'Add transaction'}</button>
          {editId && <button type="button" className="btn-ghost" onClick={() => { setEditId(null); setForm(blank); }}>Cancel</button>}
        </div>
      </form>

      <div className="panel">
        <div className="mb-4 grid grid-cols-2 items-end gap-3 md:grid-cols-5">
          <div><label className="label">From</label><input type="date" className="input" value={filter.from} onChange={(e) => setFilter({ ...filter, from: e.target.value })} /></div>
          <div><label className="label">To</label><input type="date" className="input" value={filter.to} onChange={(e) => setFilter({ ...filter, to: e.target.value })} /></div>
          <div><label className="label">Category</label>
            <select className="input" value={filter.category} onChange={(e) => setFilter({ ...filter, category: e.target.value })}>
              <option value="">All</option>{cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
          <div><label className="label">Type</label>
            <select className="input" value={filter.type} onChange={(e) => setFilter({ ...filter, type: e.target.value })}>
              <option value="">All</option><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select></div>
          <button className="btn-ghost" onClick={exportCsv}>Download CSV</button>
        </div>

        {rows.length === 0 ? <p className="text-sm text-slate-600">No transactions match. Add one above.</p> : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-slate-500"><tr><th className="py-2">Date</th><th>Details</th><th>Account</th><th className="text-right">Amount</th><th></th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((t) => (
                  <tr key={t.id}>
                    <td className="py-2 whitespace-nowrap">{t.date}</td>
                    <td>{t.merchant || t.category.name}<span className="block text-xs text-slate-500">{t.category.name}</span></td>
                    <td>{t.account.name}</td>
                    <td className={`text-right ${t.type === 'INCOME' ? 'text-leaf' : ''}`}>{t.type === 'INCOME' ? '+' : '-'}<Money v={t.amount} /></td>
                    <td className="whitespace-nowrap text-right">
                      <button className="mr-3 text-leaf underline" onClick={() => edit(t)}>Edit</button>
                      <button className="text-brick underline" onClick={() => remove(t.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {meta.totalPages > 1 && (
          <div className="mt-4 flex items-center gap-3 text-sm">
            <button className="btn-ghost" disabled={meta.page === 0} onClick={() => load(meta.page - 1)}>Previous</button>
            <span>Page {meta.page + 1} of {meta.totalPages}</span>
            <button className="btn-ghost" disabled={meta.page + 1 >= meta.totalPages} onClick={() => load(meta.page + 1)}>Next</button>
          </div>
        )}
      </div>
    </div>
  );
}
