import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';

export default function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [err, setErr] = useState('');
  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      const { data } = await api.post(`/auth/${mode}`, form);
      login(data);
      nav('/');
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <div className="grid min-h-screen md:grid-cols-2">
      <div className="hidden flex-col justify-end bg-ink p-12 text-white md:flex">
        <h1 className="font-display text-5xl font-bold leading-tight">Know what you can spend today.</h1>
        <p className="mt-4 max-w-md text-white/70">
          FinTrack tracks every rupee and turns it into a daily spending number you can trust.
        </p>
      </div>
      <div className="flex items-center justify-center p-6">
        <form onSubmit={submit} className="w-full max-w-sm space-y-4">
          <h2 className="font-display text-2xl font-semibold">{mode === 'login' ? 'Log in' : 'Create your account'}</h2>
          {mode === 'register' && (
            <div><label className="label">Name</label><input className="input" value={form.name} onChange={set('name')} required /></div>
          )}
          <div><label className="label">Email</label><input className="input" type="email" value={form.email} onChange={set('email')} required /></div>
          <div><label className="label">Password</label><input className="input" type="password" minLength={6} value={form.password} onChange={set('password')} required /></div>
          {err && <p className="text-sm text-brick">{err}</p>}
          <button className="btn w-full">{mode === 'login' ? 'Log in' : 'Create account'}</button>
          <button type="button" className="text-sm text-leaf underline" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setErr(''); }}>
            {mode === 'login' ? 'New here? Create an account' : 'Already have an account? Log in'}
          </button>
        </form>
      </div>
    </div>
  );
}
