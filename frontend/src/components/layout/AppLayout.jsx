import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { AlertTriangle, GraduationCap, Home, MessageCircle, User } from 'lucide-react'
import { api } from '../../services/api'

const navItems = [
  { to: '/learn', label: 'Learn', Icon: GraduationCap },
  { to: '/home', label: 'Home', Icon: Home },
  { to: '/support', label: 'Support', Icon: MessageCircle },
  { to: '/me', label: 'Me', Icon: User },
]

function EmergencyButton({ onClick = () => {} }) {
  return <button className="student-emergency" type="button" onClick={onClick}>
    <AlertTriangle size={17} strokeWidth={2.2} /> <span>Emergency session</span>
  </button>
}

function Logo() {
  return <NavLink to="/learn" className="student-logo" aria-label="CareConnect Learn">
    <svg viewBox="0 0 64 64" role="img" aria-hidden="true">
      <circle cx="32" cy="32" r="26" fill="none" stroke="currentColor" strokeWidth="5" strokeDasharray="148 16" strokeLinecap="round" transform="rotate(38 32 32)" />
      <path d="M32 45s-14-8.7-14-18a8 8 0 0 1 14-5 8 8 0 0 1 14 5c0 9.3-14 18-14 18Z" fill="#D9594C" />
    </svg>
  </NavLink>
}

function BottomNav() {
  return <nav className="student-bottom-nav" aria-label="Student navigation">
    {navItems.map(({ to, label, Icon }) => <NavLink key={to} to={to} className={({ isActive }) => `student-nav-item${isActive ? ' is-active' : ''}`}>
      <Icon size={21} strokeWidth={1.9} /><span>{label}</span>
    </NavLink>)}
  </nav>
}

export default function AppLayout() {
  const navigate = useNavigate()
  const [checking, setChecking] = useState(true)
  const [authorized, setAuthorized] = useState(false)
  const active = useRef(true)

  useEffect(() => {
    active.current = true
    async function checkStudent() {
      if (!api.token) {
        navigate('/login', { replace: true })
        return
      }
      try {
        const account = await api.me()
        if (!active.current) return
        const roles = (account.roles || []).map(role => String(role).replace(/^ROLE_/, '').toUpperCase())
        if (!roles.includes('STUDENT')) {
          api.logout()
          navigate('/login', { replace: true })
          return
        }
        setAuthorized(true)
      } catch {
        if (active.current) {
          api.logout()
          navigate('/login', { replace: true })
        }
      } finally {
        if (active.current) setChecking(false)
      }
    }
    checkStudent()
    return () => { active.current = false }
  }, [navigate])

  if (checking || !authorized) return <main className="student-auth-loading"><span className="student-spinner" />Checking your student session…</main>

  return <div className="student-app-shell">
    <div className="student-content-column">
      <div className="student-topbar"><Logo /><EmergencyButton /></div>
      <Outlet />
    </div>
    <BottomNav />
  </div>
}
