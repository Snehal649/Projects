import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';

const links = [
  ['/', 'Dashboard'],
  ['/transactions', 'Transactions'],
  ['/budgets', 'Budgets'],
  ['/goals', 'Goals'],
  ['/accounts', 'Accounts'],
  ['/insights', 'Insights'],
];

export default function Layout() {
  const { user, logout, privacy, togglePrivacy } = useAuth();
  const nav = useNavigate();

  return (
    <div className="min-h-screen md:flex">
      <aside className="bg-ink px-4 py-4 text-white md:sticky md:top-0 md:h-screen md:w-56 md:shrink-0 md:py-8">
        <div className="mb-4 font-display text-2xl font-bold md:mb-8">FinTrack</div>
        <nav className="flex flex-wrap gap-1 md:flex-col">
          {links.map(([to, label]) => (
            <NavLink key={to} to={to} end={to === '/'}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 text-sm ${isActive ? 'bg-white/15 font-medium' : 'text-white/70 hover:bg-white/10'}`}>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-4 border-t border-white/15 pt-4 text-sm md:mt-10">
          <p className="mb-2 truncate text-white/70">{user?.name}</p>
          <button onClick={togglePrivacy} className="mr-3 text-white/80 underline">
            {privacy ? 'Show amounts' : 'Hide amounts'}
          </button>
          <button onClick={() => { logout(); nav('/login'); }} className="text-white/80 underline">Log out</button>
        </div>
      </aside>
      <main className="mx-auto w-full max-w-6xl flex-1 p-4 md:p-8">
        <Outlet />
      </main>
    </div>
  );
}
