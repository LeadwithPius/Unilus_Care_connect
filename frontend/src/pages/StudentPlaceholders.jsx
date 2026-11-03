import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowRight,
  CalendarDays,
  Clock3,
  HeartHandshake,
  House,
  LoaderCircle,
  LogOut,
  ShieldCheck,
  UserRound,
  Video,
} from 'lucide-react'
import { api } from '../services/api'

function formatDate(value, options = {}) {
  if (!value) return 'To be arranged'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    ...options,
  }).format(date)
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? ''
    : new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(date)
}

function StatusBadge({ status }) {
  const normalized = String(status || 'REQUESTED').toUpperCase()
  return <span className={`journey-status status-${normalized.toLowerCase().replaceAll('_', '-')}`}>{normalized.replaceAll('_', ' ')}</span>
}

function Message({ type = 'error', children }) {
  if (!children) return null
  return <div className={`journey-message journey-message-${type}`} role="status">{children}</div>
}

function LoadingState({ label = 'Loading your CareConnect information…' }) {
  return <div className="journey-loading"><LoaderCircle className="journey-spin" size={20} />{label}</div>
}

function AppointmentCard({ appointment, onCancel, cancellingId, showCancel = true }) {
  const canCancel = ['REQUESTED', 'CONFIRMED'].includes(String(appointment.status).toUpperCase())
  return <article className="journey-appointment-card">
    <div className="journey-appointment-top">
      <div className="journey-appointment-icon"><CalendarDays size={19} /></div>
      <StatusBadge status={appointment.status} />
    </div>
    <h3>{formatDate(appointment.scheduledStart)}</h3>
    <p className="journey-appointment-time"><Clock3 size={14} /> {formatTime(appointment.scheduledStart)}{appointment.scheduledEnd ? ` – ${formatTime(appointment.scheduledEnd)}` : ''}</p>
    <div className="journey-appointment-meta">
      <span>{String(appointment.counsellingMode || 'FACE_TO_FACE').replaceAll('_', ' ')}</span>
      {appointment.supportCategory && <span>{appointment.supportCategory}</span>}
    </div>
    {showCancel && canCancel && <button className="journey-text-button journey-cancel-button" type="button" disabled={cancellingId === appointment.id} onClick={() => onCancel(appointment.id)}>
      {cancellingId === appointment.id ? 'Cancelling…' : 'Cancel appointment'}
    </button>}
  </article>
}

export function Home() {
  const [profile, setProfile] = useState(null)
  const [appointments, setAppointments] = useState([])
  const [questionnaire, setQuestionnaire] = useState(null)
  const [checkIns, setCheckIns] = useState([])
  const [checkInResponses, setCheckInResponses] = useState({})
  const [savingCheckIn, setSavingCheckIn] = useState(false)
  const [checkInMessage, setCheckInMessage] = useState('')
  const [checkInError, setCheckInError] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cancellingId, setCancellingId] = useState(null)

  async function loadDashboard() {
    setLoading(true)
    setError('')
    try {
      const [student, bookings, questionnaireList, checkInList] = await Promise.all([
        api.studentProfile(),
        api.myAppointments(),
        api.wellbeingQuestionnaire(),
        api.myWellbeingCheckIns(),
      ])
      setProfile(student)
      setAppointments(Array.isArray(bookings) ? bookings : [])
      setQuestionnaire(Array.isArray(questionnaireList) ? questionnaireList[0] || null : null)
      setCheckIns(Array.isArray(checkInList) ? checkInList : [])
    } catch (err) {
      setError(err.message || 'We could not load your dashboard.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadDashboard() }, [])

  const upcoming = useMemo(() => appointments
    .filter(item => !['COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW'].includes(String(item.status).toUpperCase()))
    .sort((a, b) => new Date(a.scheduledStart) - new Date(b.scheduledStart)), [appointments])


  async function submitCheckIn(event) {
    event.preventDefault()
    setCheckInError('')
    setCheckInMessage('')
    const questions = questionnaire?.questions || []
    const missing = questions.some(question =>
      question.required && !Object.hasOwn(checkInResponses, question.key)
    )
    if (missing) {
      setCheckInError('Please answer each required question before saving your check-in.')
      return
    }

    setSavingCheckIn(true)
    try {
      const result = await api.submitWellbeingCheckIn(
        questionnaire.version,
        checkInResponses
      )
      setCheckInMessage(result.message || 'Your check-in has been saved.')
      setCheckInResponses({})
      const history = await api.myWellbeingCheckIns()
      setCheckIns(Array.isArray(history) ? history : [])
    } catch (err) {
      setCheckInError(err.message || 'Your check-in could not be saved.')
    } finally {
      setSavingCheckIn(false)
    }
  }

  async function cancelAppointment(id) {
    if (!window.confirm('Cancel this counselling appointment?')) return
    setCancellingId(id)
    setError('')
    try {
      const updated = await api.cancelAppointment(id)
      setAppointments(current => current.map(item => item.id === id ? updated : item))
    } catch (err) {
      setError(err.message || 'The appointment could not be cancelled.')
    } finally {
      setCancellingId(null)
    }
  }

  return <main className="journey-page">
    <section className="journey-welcome">
      <div>
        <span className="learn-section-eyebrow">YOUR WELLBEING SPACE</span>
        <h1>Welcome back{profile?.fullName ? `, ${profile.fullName.split(' ')[0]}` : ''}.</h1>
        <p>Take things one step at a time. Your support and wellbeing tools are here whenever you need them.</p>
      </div>
      <div className="journey-welcome-mark"><House size={29} /></div>
    </section>

    <Message>{error}</Message>
    {loading ? <LoadingState /> : <>
      <section className="journey-quick-grid">
        <Link to="/support" className="journey-quick-card">
          <span className="journey-quick-icon"><HeartHandshake size={21} /></span>
          <strong>Find support</strong>
          <small>Connect with a counsellor</small>
          <ArrowRight size={17} />
        </Link>
        <Link to="/learn" className="journey-quick-card">
          <span className="journey-quick-icon"><Video size={21} /></span>
          <strong>Explore Learn</strong>
          <small>Articles, videos and tools</small>
          <ArrowRight size={17} />
        </Link>
      </section>

      <section className="journey-section">
        <div className="journey-section-heading">
          <div><span className="learn-section-eyebrow">YOUR SCHEDULE</span><h2>Upcoming appointments</h2></div>
          <span className="journey-count">{upcoming.length} upcoming</span>
        </div>
        {upcoming.length ? <div className="journey-appointment-grid">
          {upcoming.map(item => <AppointmentCard key={item.id} appointment={item} onCancel={cancelAppointment} cancellingId={cancellingId} />)}
        </div> : <div className="journey-empty">
          <CalendarDays size={25} />
          <h3>No upcoming appointments</h3>
          <p>If you would like to talk to someone, you can browse available counsellors and request a session.</p>
          <Link to="/support">Find a counsellor <ArrowRight size={16} /></Link>
        </div>}
      </section>


      <section className="journey-section journey-checkin-section">
        <div className="journey-section-heading">
          <div><span className="learn-section-eyebrow">A MOMENT FOR YOURSELF</span><h2>Wellbeing check-in</h2></div>
          <span className="journey-count">Private reflection</span>
        </div>
        {questionnaire ? <>
          <p className="journey-checkin-intro">{questionnaire.description || 'A short reflection on how things have been feeling lately.'}</p>
          <form className="journey-checkin-form" onSubmit={submitCheckIn}>
            {(questionnaire.questions || []).map(question => (
              <fieldset className="journey-checkin-question" key={question.key}>
                <legend>{question.prompt}{question.required ? ' *' : ''}</legend>
                <div className="journey-scale-options">
                  {[1, 2, 3, 4, 5].map(value => (
                    <button
                      key={value}
                      type="button"
                      className={Number(checkInResponses[question.key]) === value ? 'is-selected' : ''}
                      aria-pressed={Number(checkInResponses[question.key]) === value}
                      onClick={() => setCheckInResponses(current => ({ ...current, [question.key]: value }))}
                    >{value}</button>
                  ))}
                </div>
                <small>Choose a number from 1 (low) to 5 (high).</small>
              </fieldset>
            ))}
            <Message>{checkInError}</Message>
            <Message type="success">{checkInMessage}</Message>
            <button className="journey-primary-button" type="submit" disabled={savingCheckIn || !(questionnaire.questions || []).length}>
              {savingCheckIn ? <><LoaderCircle className="journey-spin" size={16} /> Saving…</> : 'Save my check-in'}
            </button>
          </form>
        </> : <div className="journey-empty"><HeartHandshake size={23} /><h3>No check-in is available right now</h3><p>You can still explore the learning library or request support from a counsellor.</p></div>}
        {checkIns.length > 0 && <div className="journey-checkin-history">
          <strong>Recent reflections</strong>
          {checkIns.slice(0, 3).map(item => <div key={item.id}><span>Wellbeing check-in</span><time>{formatDate(item.createdAt)}</time></div>)}
        </div>}
        <p className="journey-disclaimer">This check-in is for personal reflection. It is not a diagnosis, a risk assessment or a substitute for professional support. If you need help, you can reach out to a counsellor.</p>
      </section>

      <section className="journey-reassurance">
        <ShieldCheck size={21} />
        <div><strong>Your support is confidential.</strong><p>Counsellors see a student reference for appointments rather than your name or student number. CareConnect keeps the account link securely for your own records and appointment administration.</p></div>
      </section>
    </>}
  </main>
}

export function Support() {
  const [categories, setCategories] = useState([])
  const [counsellors, setCounsellors] = useState([])
  const [consent, setConsent] = useState(null)
  const [categoryCode, setCategoryCode] = useState('')
  const [slotId, setSlotId] = useState('')
  const [mode, setMode] = useState('FACE_TO_FACE')
  const [reason, setReason] = useState('')
  const [accepted, setAccepted] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([
      api.appointmentCategories(),
      api.counsellors(),
      api.currentCounsellingConsent(),
    ]).then(([categoryList, counsellorList, consentDocument]) => {
      if (!active) return
      const availableCategories = Array.isArray(categoryList) ? categoryList : []
      setCategories(availableCategories)
      setCategoryCode(availableCategories[0]?.code || '')
      setCounsellors(Array.isArray(counsellorList) ? counsellorList : [])
      setConsent(consentDocument)
    }).catch(err => {
      if (active) setError(err.message || 'We could not load the counselling booking information.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const availableSlots = useMemo(() => counsellors.flatMap(person =>
    (person.availability || []).map(slot => ({
      ...slot,
      counsellorId: person.profileId,
      counsellorName: person.fullName,
      counsellorTitle: person.professionalTitle,
    }))
  ).sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt)), [counsellors])

  async function submitBooking(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (!accepted) {
      setError('Please read and accept the counselling privacy and consent statement to continue.')
      return
    }
    if (!slotId || !categoryCode) {
      setError('Choose a support category and an available appointment time.')
      return
    }
    if (mode !== 'FACE_TO_FACE' && !reason) {
      setError('Please select why you need an alternative counselling mode.')
      return
    }

    setSubmitting(true)
    try {
      await api.recordConsent(consent.id, 'GRANTED')
      const appointment = await api.bookAppointment({
        availabilitySlotId: slotId,
        supportCategoryCode: categoryCode,
        counsellingMode: mode,
        alternativeModeReason: mode === 'FACE_TO_FACE' ? null : reason,
        consentDocumentId: consent.id,
      })
      setSuccess(`Your appointment request has been submitted. Current status: ${String(appointment.status || 'REQUESTED').replaceAll('_', ' ')}.`)
      setAccepted(false)
      setSlotId('')
    } catch (err) {
      setError(err.message || 'Your appointment request could not be submitted.')
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="journey-page">
    <section className="journey-page-heading">
      <span className="learn-section-eyebrow">WE'RE HERE TO HELP</span>
      <h1>Support that listens.</h1>
      <p>Choose a support area, find a counsellor with an available time, and request a session that works for you.</p>
    </section>

    <Message>{error}</Message>
    <Message type="success">{success}</Message>
    {loading ? <LoadingState label="Finding counsellors and available times…" /> : <>
      <section className="journey-section">
        <div className="journey-section-heading"><div><span className="learn-section-eyebrow">MEET THE TEAM</span><h2>Available counsellors</h2></div><span className="journey-count">{counsellors.length} available</span></div>
        {counsellors.length ? <div className="journey-counsellor-grid">
          {counsellors.map(person => <article className="journey-counsellor-card" key={person.profileId}>
            <span className="journey-counsellor-avatar"><UserRound size={22} /></span>
            <div><h3>{person.fullName}</h3><p>{person.professionalTitle || 'CareConnect counsellor'}</p></div>
            {person.biography && <p className="journey-counsellor-bio">{person.biography}</p>}
            <span className="journey-availability-label"><span /> Accepting appointments</span>
          </article>)}
        </div> : <div className="journey-empty"><HeartHandshake size={25} /><h3>No counsellors are accepting appointments right now</h3><p>Please check again later or contact the appropriate university support service if you need help sooner.</p></div>}
      </section>

      <section className="journey-section journey-booking-section">
        <div className="journey-section-heading"><div><span className="learn-section-eyebrow">REQUEST A SESSION</span><h2>Book counselling</h2></div></div>
        <form className="journey-form" onSubmit={submitBooking}>
          <label className="journey-field"><span>What would you like support with?</span>
            <select value={categoryCode} onChange={event => setCategoryCode(event.target.value)} required>
              <option value="" disabled>Select a support area</option>
              {categories.map(category => <option key={category.code} value={category.code}>{category.displayName}</option>)}
            </select>
          </label>

          <label className="journey-field"><span>Available appointment</span>
            <select value={slotId} onChange={event => setSlotId(event.target.value)} required>
              <option value="">Choose a counsellor and time</option>
              {availableSlots.map(slot => <option key={slot.id} value={slot.id}>
                {slot.counsellorName}{slot.counsellorTitle ? ` · ${slot.counsellorTitle}` : ''} — {formatDate(slot.startsAt)} at {formatTime(slot.startsAt)}
              </option>)}
            </select>
          </label>

          <label className="journey-field"><span>How would you prefer to meet?</span>
            <select value={mode} onChange={event => { setMode(event.target.value); setReason('') }}>
              <option value="FACE_TO_FACE">Face to face</option>
              <option value="CALL">Voice call</option>
              <option value="TRIAGE_CHAT">Chat</option>
            </select>
          </label>

          {mode !== 'FACE_TO_FACE' && <label className="journey-field"><span>Why do you need an alternative mode?</span>
            <select value={reason} onChange={event => setReason(event.target.value)} required>
              <option value="">Select a reason</option>
              <option value="ACCESSIBILITY">Accessibility needs</option>
              <option value="OFF_CAMPUS">I am off campus</option>
              <option value="COUNSELLOR_RECOMMENDED">Recommended by a counsellor</option>
            </select>
          </label>}

          {consent && <div className="journey-consent-box">
            <div className="journey-consent-title"><ShieldCheck size={19} /><div><strong>{consent.title}</strong><small>Version {consent.versionLabel}</small></div></div>
            <p>{consent.body}</p>
            <label className="journey-consent-check"><input type="checkbox" checked={accepted} onChange={event => setAccepted(event.target.checked)} /><span>I have read and understood this information, and I agree to proceed.</span></label>
          </div>}

          <button className="journey-primary-button" type="submit" disabled={submitting || !consent || !availableSlots.length}>
            {submitting ? <><LoaderCircle className="journey-spin" size={17} /> Submitting request…</> : <>Request appointment <ArrowRight size={17} /></>}
          </button>
          {!consent && <p className="journey-inline-note">The counselling consent document is not available. Booking is paused until it is published.</p>}
        </form>
      </section>
    </>}
  </main>
}

export function Me() {
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [appointments, setAppointments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([api.studentProfile(), api.myAppointments()])
      .then(([student, bookings]) => {
        if (!active) return
        setProfile(student)
        setAppointments(Array.isArray(bookings) ? bookings : [])
      })
      .catch(err => { if (active) setError(err.message || 'We could not load your account details.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  function signOut() {
    api.logout()
    navigate('/login', { replace: true })
  }

  return <main className="journey-page">
    <section className="journey-page-heading">
      <span className="learn-section-eyebrow">YOUR ACCOUNT</span>
      <h1>A space that's yours.</h1>
      <p>Review your account information and keep track of your CareConnect activity.</p>
    </section>
    <Message>{error}</Message>
    {loading ? <LoadingState /> : profile && <>
      <section className="journey-profile-card">
        <span className="journey-profile-avatar"><UserRound size={28} /></span>
        <div><span className="learn-section-eyebrow">STUDENT PROFILE</span><h2>{profile.fullName}</h2><p>{profile.email}</p></div>
      </section>
      <section className="journey-section">
        <div className="journey-section-heading"><div><span className="learn-section-eyebrow">ACCOUNT DETAILS</span><h2>Your information</h2></div></div>
        <dl className="journey-profile-details">
          <div><dt>Full name</dt><dd>{profile.fullName}</dd></div>
          <div><dt>University email</dt><dd>{profile.email}</dd></div>
          <div><dt>Student number</dt><dd>{profile.studentNumber}</dd></div>
          <div><dt>Member since</dt><dd>{formatDate(profile.createdAt, { year: 'numeric' })}</dd></div>
        </dl>
      </section>
      <section className="journey-section">
        <div className="journey-section-heading"><div><span className="learn-section-eyebrow">YOUR HISTORY</span><h2>Appointments</h2></div><span className="journey-count">{appointments.length} total</span></div>
        {appointments.length ? <div className="journey-appointment-grid">
          {[...appointments].sort((a, b) => new Date(b.scheduledStart) - new Date(a.scheduledStart)).map(item => <AppointmentCard key={item.id} appointment={item} showCancel={false} />)}
        </div> : <div className="journey-empty"><CalendarDays size={24} /><h3>No appointment history yet</h3><p>Your counselling appointments will appear here.</p><Link to="/support">Explore support <ArrowRight size={16} /></Link></div>}
      </section>
      <section className="journey-reassurance">
        <ShieldCheck size={21} />
        <div><strong>Privacy matters.</strong><p>Your identity is used to manage your account and appointments. Counsellors see a counsellor-specific student reference in their appointment workspace.</p></div>
      </section>
      <button className="journey-signout-button" type="button" onClick={signOut}><LogOut size={17} /> Sign out of CareConnect</button>
    </>}
  </main>
}
