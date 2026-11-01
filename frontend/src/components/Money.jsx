import { useAuth } from '../auth.jsx';

export default function Money({ v, className = '' }) {
  const { privacy } = useAuth();
  const n = Number(v || 0);
  const text = '₹' + n.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  return <span className={`${privacy ? 'blur-sm select-none' : ''} ${className}`}>{text}</span>;
}
