import { useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';

export default function QuickAdd({ onSaved }) {
  const [text, setText] = useState('');
  const [draft, setDraft] = useState(null);
  const [err, setErr] = useState('');
  const [accounts, setAccounts] = useState([]);
  const [cats, setCats] = useState([]);
  const [listening, setListening] = useState(false);

  useEffect(() => {
    api.get('/accounts').then((r) => setAccounts(r.data));
    api.get('/categories').then((r) => setCats(r.data));
  }, []);

  const parse = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      const { data } = await api.post('/transactions/quick-add/parse', { text });
      setDraft(data);
    } catch (ex) {
      setErr(errMsg(ex, 'Could not read that sentence'));
    }
  };

  const save = async () => {
    try {
      await api.post('/transactions', draft);
      setDraft(null);
      setText('');
      onSaved?.();
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  const listen = () => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) return setErr('Voice input is not supported in this browser. Try Chrome.');
    const rec = new SR();
    rec.lang = 'en-IN';
    rec.onstart = () => setListening(true);
    rec.onend = () => setListening(false);
    rec.onresult = (ev) => setText(ev.results[0][0].transcript);
    rec.start();
  };

  const set = (k, v) => setDraft((d) => ({ ...d, [k]: v }));

  return (
    <div className="panel">
      <form onSubmit={parse} className="flex gap-2">
        <input className="input" value={text} onChange={(e) => setText(e.target.value)}
          placeholder='Type or speak: "Paid 1200 for petrol yesterday using HDFC"' aria-label="Quick add" />
        <button type="button" className="btn-ghost" onClick={listen}>{listening ? 'Listening' : 'Speak'}</button>
        <button className="btn" disabled={!text.trim()}>Add</button>
      </form>
      {err && <p className="mt-2 text-sm text-brick">{err}</p>}

      {draft && (
        <div className="mt-4 rounded-md bg-mist p-4">
          <p className="mb-3 text-sm font-medium">Check the details, then save.</p>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
            <div><label className="label">Amount</label>
              <input className="input" type="number" value={draft.amount} onChange={(e) => set('amount', e.target.value)} /></div>
            <div><label className="label">Category</label>
              <select className="input" value={draft.categoryId} onChange={(e) => set('categoryId', Number(e.target.value))}>
                {cats.filter((c) => c.type === draft.type).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select></div>
            <div><label className="label">Account</label>
              <select className="input" value={draft.accountId} onChange={(e) => set('accountId', Number(e.target.value))}>
                {accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
              </select></div>
            <div><label className="label">Date</label>
              <input className="input" type="date" value={draft.date} onChange={(e) => set('date', e.target.value)} /></div>
            <div><label className="label">Merchant</label>
              <input className="input" value={draft.merchant || ''} onChange={(e) => set('merchant', e.target.value)} /></div>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <span className="text-sm text-slate-600">{draft.type === 'INCOME' ? 'Income' : 'Expense'}</span>
            <button className="btn ml-auto" onClick={save}>Save transaction</button>
            <button className="btn-ghost" onClick={() => setDraft(null)}>Discard</button>
          </div>
        </div>
      )}
    </div>
  );
}
