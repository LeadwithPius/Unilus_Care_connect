import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  CalendarDays,
  Check,
  CheckCircle2,
  Clock3,
  LoaderCircle,
  LogOut,
  Plus,
  RefreshCw,
  ShieldCheck,
  Trash2,
  UserRound,
  Users,
  X,
} from 'lucide-react'
import { api } from '../services/api'
import ChatWorkspace from '../components/chat/ChatWorkspace.jsx'

function formatDate(value) {
  if (!value) return 'Date unavailable'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Date unavailable' : new Intl.DateTimeFormat(undefined, {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric',
  }).format(date)
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat(undefined, {
    hour: 'numeric', minute: '2-digit',
  }).format(date)
}

function StatusBadge({ status }) {
  const normalized = String(status || 'REQUESTED').toUpperCase()
  return <span className={`journey-status status-${normalized.toLowerCase().replaceAll('_', '-')}`}>{normalized.replaceAll('_', ' ')}</span>
}

function WorkspaceMessage({ type = 'error', children }) {
  if (!children) return null
  return <div className={`journey-message journey-message-${type}`} role="status">{children}</div>
}

function AppointmentActions({ appointment, onDecision, onAttendance, busy }) {
  const status = String(appointment.status).toUpperCase()
  if (status === 'REQUESTED') return <div className="counsellor-actions">
    <button className="counsellor-action-approve" type="button" disabled={busy} onClick={() => onDecision(appointment.appointmentId, 'CONFIRMED')}><Check size={15} /> Confirm</button>
    <button className="counsellor-action-reject" type="button" disabled={busy} onClick={() => onDecision(appointment.appointmentId, 'REJECTED')}><X size={15} /> Decline</button>
  </div>
  if (status === 'CONFIRMED') return <div className="counsellor-actions">
    <button className="counsellor-action-approve" type="button" disabled={busy} onClick={() => onAttendance(appointment.appointmentId, 'CHECK_IN')}><CheckCircle2 size={15} /> Check in</button>
    <button className="counsellor-action-reject" type="button" disabled={busy} onClick={() => onAttendance(appointment.appointmentId, 'NO_SHOW')}>Mark no-show</button>
  </div>
  if (status === 'CHECKED_IN') return <div className="counsellor-actions">
    <button className="counsellor-action-approve" type="button" disabled={busy} onClick={() => onAttendance(appointment.appointmentId, 'COMPLETE')}><CheckCircle2 size={15} /> Complete session</button>
  </div>
  return null
}

function AppointmentCard({ appointment, onDecision, onAttendance, busy }) {
  return <article className="counsellor-appointment-card">
    <div className="counsellor-appointment-top">
      <div className="counsellor-student-ref"><span className="counsellor-student-avatar"><UserRound size={17} /></span><div><small>STUDENT REFERENCE</small><strong>{appointment.studentReference || 'Student reference pending'}</strong></div></div>
      <StatusBadge status={appointment.status} />
    </div>
    <div className="counsellor-appointment-date"><CalendarDays size={15} /><strong>{formatDate(appointment.scheduledStart)}</strong><span>{formatTime(appointment.scheduledStart)} – {formatTime(appointment.scheduledEnd)}</span></div>
    <div className="counsellor-appointment-tags">
      <span>{String(appointment.counsellingMode || 'FACE_TO_FACE').replaceAll('_', ' ')}</span>
      {appointment.supportCategory && <span>{appointment.supportCategory}</span>}
      {appointment.alternativeModeReason && <span>{String(appointment.alternativeModeReason).replaceAll('_', ' ')}</span>}
    </div>
    <AppointmentActions appointment={appointment} onDecision={onDecision} onAttendance={onAttendance} busy={busy} />
  </article>
}

export default function CounsellorWorkspace() {
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [appointments, setAppointments] = useState([])
  const [availability, setAvailability] = useState([])
  const [loadedAt, setLoadedAt] = useState(0)
  const [filter, setFilter] = useState('ACTIVE')
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState('')
  const [savingAvailability, setSavingAvailability] = useState(false)
  const [acceptingBusy, setAcceptingBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showProfileForm, setShowProfileForm] = useState(false)
  const [savingProfile, setSavingProfile] = useState(false)
  const [profileForm, setProfileForm] = useState({ displayName: '', professionalTitle: '', biography: '' })
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')

  const loadWorkspace = useCallback(async () => {
    try {
      const [account, bookings, slots] = await Promise.all([
        api.me(),
        api.counsellorAppointments(),
        api.counsellorAvailability(false),
      ])
      const roles = (account.roles || []).map(role => String(role).replace(/^ROLE_/, '').toUpperCase())
      if (!roles.includes('COUNSELLOR')) {
        api.logout()
        navigate('/login', { replace: true })
        return
      }
      const counsellor = await api.counsellorProfile()
      setProfile(counsellor)
      setProfileForm({ displayName: counsellor.displayName || '', professionalTitle: counsellor.professionalTitle || '', biography: counsellor.biography || '' })
      setAppointments(Array.isArray(bookings) ? bookings : [])
      setAvailability(Array.isArray(slots) ? slots : [])
      setLoadedAt(Date.now())
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        api.logout()
        navigate('/login', { replace: true })
      } else {
        setError(err.message || 'We could not load your counselling workspace.')
      }
    } finally {
      setLoading(false)
    }
  }, [navigate])

  const refreshOperationalData = useCallback(async () => {
    try {
      const [bookings, slots] = await Promise.all([
        api.counsellorAppointments(),
        api.counsellorAvailability(false),
      ])
      setAppointments(Array.isArray(bookings) ? bookings : [])
      setAvailability(Array.isArray(slots) ? slots : [])
      setLoadedAt(Date.now())
      setError('')
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        api.logout()
        navigate('/login', { replace: true })
      } else {
        setError(err.message || 'Appointment updates could not be refreshed.')
      }
    }
  }, [navigate])

  useEffect(() => {
    const timer = window.setInterval(() => { void refreshOperationalData() }, 20000)
    return () => window.clearInterval(timer)
  }, [refreshOperationalData])

  // This effect fetches remote workspace data; state updates occur only after the API promises settle.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void loadWorkspace() }, [loadWorkspace])

  const visibleAppointments = useMemo(() => {
    if (filter === 'ACTIVE') {
      return appointments.filter(item => ['REQUESTED', 'CONFIRMED', 'CHECKED_IN'].includes(String(item.status).toUpperCase()))
    }
    if (filter === 'ALL') return appointments
    return appointments.filter(item => String(item.status).toUpperCase() === filter)
  }, [appointments, filter])

  const pendingCount = appointments.filter(item => item.status === 'REQUESTED').length
  const activeCount = appointments.filter(item => ['CONFIRMED', 'CHECKED_IN'].includes(item.status)).length
  const upcomingSlots = availability.filter(slot => Date.parse(slot.startsAt) > loadedAt)
    .sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt))

  async function handleDecision(id, decision) {
    setBusyId(id)
    setError('')
    setSuccess('')
    try {
      await api.decideCounsellorAppointment(id, decision)
      setSuccess(decision === 'CONFIRMED' ? 'Appointment confirmed.' : 'Appointment request declined.')
      await loadWorkspace()
    } catch (err) {
      setError(err.message || 'The appointment could not be updated.')
    } finally {
      setBusyId('')
    }
  }

  async function handleAttendance(id, action) {
    setBusyId(id)
    setError('')
    setSuccess('')
    try {
      await api.updateCounsellorAttendance(id, action)
      const messages = {
        CHECK_IN: 'Student checked in. The session is now in progress.',
        COMPLETE: 'Counselling session marked as completed.',
        NO_SHOW: 'Appointment marked as a no-show.',
      }
      setSuccess(messages[action])
      await loadWorkspace()
    } catch (err) {
      setError(err.message || 'The appointment could not be updated.')
    } finally {
      setBusyId('')
    }
  }

  async function saveProfile(event) {
    event.preventDefault()
    setSavingProfile(true)
    setError('')
    setSuccess('')
    try {
      const updated = await api.updateCounsellorProfile(profileForm)
      setProfile(current => ({ ...current, ...updated }))
      setProfileForm({
        displayName: updated.displayName || '',
        professionalTitle: updated.professionalTitle || '',
        biography: updated.biography || '',
      })
      setShowProfileForm(false)
      setSuccess('Your professional profile has been updated.')
    } catch (err) {
      setError(err.message || 'Your profile could not be updated.')
    } finally {
      setSavingProfile(false)
    }
  }

  async function toggleAccepting() {
    setAcceptingBusy(true)
    setError('')
    setSuccess('')
    try {
      const result = await api.setAcceptingAppointments(!profile.acceptingAppointments)
      setProfile(current => ({ ...current, acceptingAppointments: result.acceptingAppointments }))
      setSuccess(result.message)
    } catch (err) {
      setError(err.message || 'Your appointment status could not be updated.')
    } finally {
      setAcceptingBusy(false)
    }
  }

  async function addAvailability(event) {
    event.preventDefault()
    setSavingAvailability(true)
    setError('')
    setSuccess('')
    try {
      const start = new Date(startsAt)
      const end = new Date(endsAt)
      if (!startsAt || !endsAt || Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) {
        throw new Error('Choose a valid start and end time. The end must be after the start.')
      }
      await api.addCounsellorAvailability(start.toISOString(), end.toISOString())
      setStartsAt('')
      setEndsAt('')
      setSuccess('Your availability slot has been added.')
      await loadWorkspace()
    } catch (err) {
      setError(err.message || 'Availability could not be saved.')
    } finally {
      setSavingAvailability(false)
    }
  }

  async function removeAvailability(id) {
    if (!window.confirm('Remove this availability slot?')) return
    setError('')
    setSuccess('')
    try {
      await api.deleteCounsellorAvailability(id)
      setAvailability(current => current.filter(slot => slot.id !== id))
      setSuccess('Availability slot removed.')
    } catch (err) {
      setError(err.message || 'This slot could not be removed. It may already have an appointment.')
    }
  }

  function signOut() {
    api.logout()
    navigate('/login', { replace: true })
  }

  if (loading && !profile) return <main className="counsellor-loading"><LoaderCircle className="journey-spin" size={22} />Loading your counselling workspace…</main>

  return <main className="counsellor-workspace">
    <header className="counsellor-header">
      <div className="counsellor-brand"><span className="counsellor-brand-mark"><HeartHandshakeIcon /></span><span>CareConnect <small>COUNSELLOR WORKSPACE</small></span></div>
      <button className="counsellor-signout" type="button" onClick={signOut}><LogOut size={16} /> Sign out</button>
    </header>

    <section className="counsellor-welcome">
      <div><span>YOUR WORKSPACE</span><h1>Welcome, {profile?.displayName || 'Counsellor'}.</h1><p>Manage your appointment requests, schedule and counselling sessions in one place.</p></div>
      <div className="counsellor-welcome-actions"><button className="counsellor-profile-edit" type="button" onClick={() => setShowProfileForm(value => !value)}>{showProfileForm ? 'Close profile' : 'Edit profile'}</button><button className={`counsellor-accept-toggle ${profile?.acceptingAppointments ? 'is-on' : ''}`} type="button" disabled={acceptingBusy} onClick={toggleAccepting}>
        <span className="counsellor-toggle-dot" />
        {acceptingBusy ? 'Updating…' : profile?.acceptingAppointments ? 'Accepting appointments' : 'Not accepting appointments'}
      </button></div>
    </section>

    {showProfileForm && <form className="counsellor-profile-form" onSubmit={saveProfile}>
      <label><span>Display name</span><input required maxLength={150} value={profileForm.displayName} onChange={event => setProfileForm(current => ({ ...current, displayName: event.target.value }))} /></label>
      <label><span>Professional title</span><input maxLength={150} value={profileForm.professionalTitle} onChange={event => setProfileForm(current => ({ ...current, professionalTitle: event.target.value }))} /></label>
      <label className="counsellor-profile-bio"><span>Professional biography</span><textarea rows="4" maxLength={5000} value={profileForm.biography} onChange={event => setProfileForm(current => ({ ...current, biography: event.target.value }))} /></label>
      <button type="submit" disabled={savingProfile}>{savingProfile ? 'Saving…' : 'Save profile'}</button>
    </form>}

    <WorkspaceMessage>{error}</WorkspaceMessage>
    <WorkspaceMessage type="success">{success}</WorkspaceMessage>

    <section className="counsellor-metrics">
      <article><span className="counsellor-metric-icon"><CalendarDays size={19} /></span><small>TOTAL APPOINTMENTS</small><strong>{appointments.length}</strong></article>
      <article><span className="counsellor-metric-icon"><Clock3 size={19} /></span><small>PENDING REQUESTS</small><strong>{pendingCount}</strong></article>
      <article><span className="counsellor-metric-icon"><Users size={19} /></span><small>ACTIVE / UPCOMING</small><strong>{activeCount}</strong></article>
    </section>

    <section className="counsellor-section">
      <div className="counsellor-section-heading"><div><span>APPOINTMENT MANAGEMENT</span><h2>Student requests & sessions</h2></div><button type="button" className="counsellor-refresh" onClick={loadWorkspace}><RefreshCw size={15} /> Refresh</button></div>
      <div className="counsellor-filters" role="group" aria-label="Filter appointments">
        {['ACTIVE', 'ALL', 'REQUESTED', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'REJECTED', 'NO_SHOW'].map(status => <button key={status} type="button" className={filter === status ? 'is-active' : ''} onClick={() => setFilter(status)}>{status.replaceAll('_', ' ')}</button>)}
      </div>
      {visibleAppointments.length ? <div className="counsellor-appointment-grid">
        {visibleAppointments.map(appointment => <AppointmentCard key={appointment.appointmentId} appointment={appointment} onDecision={handleDecision} onAttendance={handleAttendance} busy={busyId === appointment.appointmentId} />)}
      </div> : <div className="counsellor-empty"><CalendarDays size={24} /><strong>No appointments in this view</strong><p>New requests and scheduled sessions will appear here.</p></div>}
    </section>

    <section className="counsellor-section">
      <div className="counsellor-section-heading"><div><span>YOUR CALENDAR</span><h2>Manage availability</h2></div><span className="journey-count">{upcomingSlots.length} upcoming slots</span></div>
      <form className="counsellor-availability-form" onSubmit={addAvailability}>
        <label><span>Start date & time</span><input type="datetime-local" value={startsAt} onChange={event => setStartsAt(event.target.value)} required /></label>
        <label><span>End date & time</span><input type="datetime-local" value={endsAt} onChange={event => setEndsAt(event.target.value)} required /></label>
        <button type="submit" disabled={savingAvailability}>{savingAvailability ? <LoaderCircle className="journey-spin" size={16} /> : <Plus size={16} />} Add slot</button>
      </form>
      {upcomingSlots.length ? <div className="counsellor-slot-list">
        {upcomingSlots.map(slot => <article className="counsellor-slot-card" key={slot.id}>
          <span className="counsellor-slot-icon"><CalendarDays size={17} /></span>
          <div><strong>{formatDate(slot.startsAt)}</strong><small>{formatTime(slot.startsAt)} – {formatTime(slot.endsAt)}</small><StatusBadge status={slot.status} /></div>
          {slot.status === 'OPEN' && <button type="button" aria-label="Remove availability slot" onClick={() => removeAvailability(slot.id)}><Trash2 size={16} /></button>}
        </article>)}
      </div> : <div className="counsellor-empty"><CalendarDays size={23} /><strong>No upcoming availability</strong><p>Add a time slot so students can request an appointment with you.</p></div>}
    </section>

    <section className="counsellor-section"><ChatWorkspace role="COUNSELLOR" embedded /></section>

    <footer className="counsellor-footer"><ShieldCheck size={17} /><span>Student identities are protected in this workspace. Appointment records use counsellor-specific student references rather than names or student numbers.</span></footer>
  </main>
}

function HeartHandshakeIcon() {
  return <svg viewBox="0 0 32 32" width="23" height="23" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m5 13 5-5 5 2 4-2 8 7-8 9-5-3-4 2-5-5Z" /><path d="m10 8-3-2-4 4 3 3M22 8l3-2 4 4-3 3M12 16l4 4 3-3M15 12l4 4 3-3" /></svg>
}
