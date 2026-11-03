import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Activity, BookOpen, CalendarDays, ChevronRight, FileText,
  GraduationCap, LayoutDashboard, LoaderCircle, LogOut, Plus, RefreshCw,
  Search, ShieldCheck, Users, UserRoundCog,
  Video, X,
} from 'lucide-react'
import { api } from '../services/api'
import './AdminWorkspace.css'

const sections = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'students', label: 'Students', icon: GraduationCap },\n  { id: 'appointments', label: 'Appointments', icon: CalendarDays },
  { id: 'counsellors', label: 'Counsellors', icon: Users },
  { id: 'education', label: 'Education Hub', icon: BookOpen },
  { id: 'consent', label: 'Consent & policies', icon: ShieldCheck },
]

const emptyEducation = {
  slug: '', title: '', topic: '', summary: '', content: '',
  contentType: 'ARTICLE', resourceUrl: '', displayOrder: 0,
}

function normalizeRoles(roles = []) {
  return roles.map(role => String(role).replace(/^ROLE_/, '').toUpperCase())
}

function dateLabel(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '—'
    : new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' }).format(date)
}

function Notice({ kind = 'error', children }) {
  return children ? <div className={`admin-notice admin-notice-${kind}`} role="status">{children}</div> : null
}

function Metric({ icon: Icon, label, value, detail }) {
  return <article className="admin-metric">
    <span className="admin-metric-icon"><Icon size={19} /></span>
    <div><small>{label}</small><strong>{value ?? '—'}</strong><span>{detail}</span></div>
  </article>
}

function StatusPill({ enabled }) {
  return <span className={`admin-status ${enabled ? 'is-enabled' : 'is-disabled'}`}>{enabled ? 'Active' : 'Disabled'}</span>
}

function AdminWorkspace() {
  const navigate = useNavigate()
  const [section, setSection] = useState('overview')
  const [account, setAccount] = useState(null)
  const [overview, setOverview] = useState(null)
  const [students, setStudents] = useState([])\n  const [appointments, setAppointments] = useState([])
  const [counsellors, setCounsellors] = useState([])
  const [resources, setResources] = useState([])
  const [consentDocuments, setConsentDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [search, setSearch] = useState('')
  const [showCounsellorForm, setShowCounsellorForm] = useState(false)
  const [showEducationForm, setShowEducationForm] = useState(false)
  const [editingResource, setEditingResource] = useState(null)
  const [counsellorForm, setCounsellorForm] = useState({
    email: '', password: '', displayName: '', professionalTitle: '', biography: '',
  })
  const [educationForm, setEducationForm] = useState(emptyEducation)
  const [consentForm, setConsentForm] = useState({
    purposeCode: 'PLATFORM_ACCESS', versionLabel: '', title: '', body: '',
  })

  const loadAll = useCallback(async () => {
    try {
      const [me, dashboard, studentRows, counsellorRows, appointmentRows, resourceRows, consentRows] = await Promise.all([
        api.me(),
        api.adminOverview(),
        api.adminStudents(),
        api.adminCounsellors(),
        api.adminEducation(),
        api.adminConsentDocuments(),
      ])
      if (!normalizeRoles(me.roles).includes('ADMIN')) {
        api.logout()
        navigate('/login', { replace: true })
        return
      }
      setError('')
      setAccount(me)
      setOverview(dashboard)
      setStudents(Array.isArray(studentRows) ? studentRows : [])
      setCounsellors(Array.isArray(counsellorRows) ? counsellorRows : [])
      setResources(Array.isArray(resourceRows) ? resourceRows : [])
      setConsentDocuments(Array.isArray(consentRows) ? consentRows : [])
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        api.logout()
        navigate('/login', { replace: true })
      } else {
        setError(err.message || 'The administration workspace could not be loaded.')
      }
    } finally {
      setLoading(false)
    }
  }, [navigate])

  useEffect(() => { void loadAll() }, [loadAll])

  const filteredStudents = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return students
    return students.filter(student =>
      [student.fullName, student.email, student.studentNumber]
        .some(value => String(value || '').toLowerCase().includes(term)))
  }, [students, search])

  const filteredCounsellors = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return counsellors
    return counsellors.filter(person =>
      [person.displayName, person.email, person.professionalTitle]
        .some(value => String(value || '').toLowerCase().includes(term)))
  }, [counsellors, search])

  async function changeAccountStatus(kind, person) {
    const label = kind === 'student' ? person.fullName : person.displayName
    const next = !person.enabled
    const action = next ? 'enable' : 'disable'
    if (!window.confirm(`Are you sure you want to ${action} ${label}'s account?`)) return
    setBusy(person.userId)
    setError('')
    setSuccess('')
    try {
      if (kind === 'student') await api.setStudentStatus(person.userId, next)
      else await api.setCounsellorStatus(person.userId, next)
      setSuccess(`${label}'s account has been ${next ? 'enabled' : 'disabled'}.`)
      await loadAll()
    } catch (err) {
      setError(err.message || 'The account status could not be changed.')
    } finally {
      setBusy('')
    }
  }

  async function createCounsellor(event) {
    event.preventDefault()
    setBusy('create-counsellor')
    setError('')
    setSuccess('')
    try {
      await api.createCounsellor(counsellorForm)
      setCounsellorForm({ email: '', password: '', displayName: '', professionalTitle: '', biography: '' })
      setShowCounsellorForm(false)
      setSuccess('Counsellor account created. The account is active but is not accepting appointments until the counsellor enables availability.')
      await loadAll()
    } catch (err) {
      setError(err.message || 'Counsellor account could not be created.')
    } finally {
      setBusy('')
    }
  }

  function startEditResource(resource) {
    setEditingResource(resource)
    setEducationForm({
      slug: resource.slug || '',
      title: resource.title || '',
      topic: resource.topic || '',
      summary: resource.summary || '',
      content: resource.content || '',
      contentType: resource.contentType || 'ARTICLE',
      resourceUrl: resource.resourceUrl || '',
      displayOrder: resource.displayOrder ?? 0,
    })
    setShowEducationForm(true)
  }

  async function saveResource(event) {
    event.preventDefault()
    setBusy('save-resource')
    setError('')
    setSuccess('')
    try {
      await api.saveEducationResource({ ...educationForm, id: editingResource?.id })
      setSuccess(editingResource ? 'Education resource updated.' : 'Education resource created as a draft.')
      setShowEducationForm(false)
      setEditingResource(null)
      setEducationForm(emptyEducation)
      await loadAll()
    } catch (err) {
      setError(err.message || 'Education resource could not be saved.')
    } finally {
      setBusy('')
    }
  }

  async function togglePublication(resource) {
    setBusy(resource.id)
    setError('')
    setSuccess('')
    try {
      await api.setEducationPublication(resource.id, !resource.published)
      setSuccess(resource.published ? 'Resource unpublished.' : 'Resource published to the student Learn library.')
      await loadAll()
    } catch (err) {
      setError(err.message || 'Publication status could not be changed.')
    } finally {
      setBusy('')
    }
  }

  async function publishConsent(event) {
    event.preventDefault()
    setBusy('publish-consent')
    setError('')
    setSuccess('')
    try {
      await api.publishConsentDocument(consentForm)
      setConsentForm(current => ({ ...current, versionLabel: '', title: '', body: '' }))
      setSuccess('A new consent version has been published. Students will be asked to accept the current version the next time they sign in.')
      await loadAll()
    } catch (err) {
      setError(err.message || 'The consent document could not be published.')
    } finally {
      setBusy('')
    }
  }

  function signOut() {
    api.logout()
    navigate('/login', { replace: true })
  }

  const counts = overview?.counts || {}
  const statusRows = overview?.appointmentStatuses || []

  if (loading) return <main className="admin-loading"><LoaderCircle className="admin-spin" size={25} />Loading administration workspace…</main>

  return <main className="admin-shell">
    <aside className="admin-sidebar">
      <a className="admin-brand" href="/"><span className="admin-brand-mark"><ShieldCheck size={22} /></span><span>CareConnect<small>ADMINISTRATION</small></span></a>
      <div className="admin-sidebar-label">WORKSPACE</div>
      <nav className="admin-nav" aria-label="Administration sections">
        {sections.map(({ id, label, icon: Icon }) => <button key={id} type="button" className={section === id ? 'is-active' : ''} onClick={() => { setSection(id); setSearch('') }}><Icon size={18} /><span>{label}</span><ChevronRight size={15} /></button>)}
      </nav>
      <div className="admin-sidebar-footer"><ShieldCheck size={17} /><span>Restricted administrator access</span></div>
    </aside>

    <section className="admin-main">
      <header className="admin-topbar">
        <div><span>UNIVERSITY OF LUSAKA</span><strong>{sections.find(item => item.id === section)?.label}</strong></div>
        <div className="admin-top-actions"><span className="admin-user"><span className="admin-user-avatar"><UserRoundCog size={17} /></span><span>{account?.email || 'Administrator'}<small>ADMINISTRATOR</small></span></span><button type="button" onClick={loadAll} aria-label="Refresh workspace"><RefreshCw size={17} /></button><button type="button" onClick={signOut}><LogOut size={16} /> Sign out</button></div>
      </header>

      <div className="admin-page">
        <div className="admin-page-heading"><div><span className="admin-eyebrow">CARECONNECT CONTROL CENTRE</span><h1>{section === 'overview' ? 'Good to see you.' : sections.find(item => item.id === section)?.label}</h1><p>{section === 'overview' ? 'A clear view of platform activity and the people it supports.' : 'Manage this part of the CareConnect platform.'}</p></div></div>
        <Notice>{error}</Notice><Notice kind="success">{success}</Notice>

        {section === 'overview' && <>
          <div className="admin-metrics-grid">
            <Metric icon={GraduationCap} label="REGISTERED STUDENTS" value={counts.students} detail="Student accounts" />
            <Metric icon={Users} label="ACTIVE COUNSELLORS" value={counts.activeCounsellors} detail="Enabled staff accounts" />
            <Metric icon={CalendarDays} label="APPOINTMENTS" value={counts.appointments} detail="All recorded appointments" />
            <Metric icon={BookOpen} label="PUBLISHED RESOURCES" value={counts.publishedResources} detail="Visible in the Learn library" />
          </div>
          <div className="admin-overview-grid">
            <section className="admin-panel"><div className="admin-panel-heading"><div><span>APPOINTMENT ACTIVITY</span><h2>Booking status overview</h2></div><Activity size={19} /></div>
              {statusRows.length ? <div className="admin-status-list">{statusRows.map(row => <div key={row.status}><span>{String(row.status).replaceAll('_', ' ')}</span><strong>{row.total}</strong></div>)}</div> : <div className="admin-empty">No appointment activity has been recorded yet.</div>}
            </section>
            <section className="admin-panel admin-quick-panel"><div className="admin-panel-heading"><div><span>QUICK ACCESS</span><h2>Manage the platform</h2></div><LayoutDashboard size={19} /></div>
              <button type="button" onClick={() => setSection('students')}><GraduationCap size={18} /><span>Review student accounts</span><ChevronRight size={16} /></button>
              <button type="button" onClick={() => setSection('counsellors')}><Users size={18} /><span>Manage counsellors</span><ChevronRight size={16} /></button>
              <button type="button" onClick={() => setSection('education')}><BookOpen size={18} /><span>Update education resources</span><ChevronRight size={16} /></button>
              <button type="button" onClick={() => setSection('consent')}><ShieldCheck size={18} /><span>Review platform guidelines</span><ChevronRight size={16} /></button>
              <div className="admin-open-slots"><CalendarDays size={17} /><span>Open future availability slots</span><strong>{counts.openSlots ?? 0}</strong></div>
            </section>
          </div>
          <div className="admin-privacy-note"><ShieldCheck size={18} /><p>Administration provides operational oversight. Counselling content and private session notes are not displayed in this dashboard.</p></div>
        </>}

        {section === 'students' && <section className="admin-panel admin-table-panel">
          <div className="admin-panel-heading"><div><span>STUDENT DIRECTORY</span><h2>Student accounts</h2></div><span className="admin-count">{students.length} accounts</span></div>
          <label className="admin-search"><Search size={17} /><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Search name, email or student number" /></label>
          <div className="admin-table-wrap"><table><thead><tr><th>STUDENT</th><th>STUDENT NUMBER</th><th>REGISTERED</th><th>APPOINTMENTS</th><th>STATUS</th><th>ACTION</th></tr></thead><tbody>
            {filteredStudents.map(person => <tr key={person.userId}><td><strong>{person.fullName}</strong><small>{person.email}</small></td><td>{person.studentNumber}</td><td>{dateLabel(person.registeredAt)}</td><td>{person.appointmentCount}</td><td><StatusPill enabled={person.enabled} /></td><td><button className="admin-row-action" type="button" disabled={busy === person.userId} onClick={() => changeAccountStatus('student', person)}>{busy === person.userId ? 'Saving…' : person.enabled ? 'Disable' : 'Enable'}</button></td></tr>)}
          </tbody></table>{!filteredStudents.length && <div className="admin-empty">No student accounts match your search.</div>}</div>
        </section>}

        {section === 'appointments' && <section className="admin-panel admin-table-panel">
          <div className="admin-panel-heading"><div><span>OPERATIONAL OVERSIGHT</span><h2>Appointment activity</h2></div><span className="admin-count">Latest {appointments.length} records</span></div>
          <div className="admin-privacy-note"><ShieldCheck size={18} /><p>This view contains scheduling and status information only. It does not expose student identities, counselling conversations or clinical notes.</p></div>
          <div className="admin-table-wrap"><table><thead><tr><th>BOOKING REF</th><th>SCHEDULED</th><th>COUNSELLOR</th><th>SUPPORT CATEGORY</th><th>MODE</th><th>STATUS</th></tr></thead><tbody>
            {appointments.map(item => <tr key={item.appointmentId}><td><strong>#{String(item.appointmentId).slice(0, 8).toUpperCase()}</strong><small>Created {dateLabel(item.createdAt)}</small></td><td>{dateLabel(item.scheduledStart)}<small>{item.scheduledStart ? new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(new Date(item.scheduledStart)) : '—'} – {item.scheduledEnd ? new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(new Date(item.scheduledEnd)) : '—'}</small></td><td>{item.counsellorName}</td><td>{item.supportCategory}</td><td>{String(item.counsellingMode).replaceAll('_', ' ')}</td><td><span className={`admin-status ${['COMPLETED', 'CONFIRMED'].includes(item.status) ? 'is-enabled' : 'is-disabled'}`}>{String(item.status).replaceAll('_', ' ')}</span></td></tr>)}
          </tbody></table>{!appointments.length && <div className="admin-empty">No appointments have been recorded yet.</div>}</div>
        </section>}

        {section === 'counsellors' && <section className="admin-panel admin-table-panel">
          <div className="admin-panel-heading"><div><span>COUNSELLING TEAM</span><h2>Counsellor accounts</h2></div><button className="admin-primary-button" type="button" onClick={() => setShowCounsellorForm(value => !value)}>{showCounsellorForm ? <X size={16} /> : <Plus size={16} />}{showCounsellorForm ? 'Close form' : 'Add counsellor'}</button></div>
          {showCounsellorForm && <form className="admin-form-grid" onSubmit={createCounsellor}>
            <label><span>Display name</span><input required maxLength={150} value={counsellorForm.displayName} onChange={e => setCounsellorForm(v => ({ ...v, displayName: e.target.value }))} /></label>
            <label><span>Professional title</span><input maxLength={150} value={counsellorForm.professionalTitle} onChange={e => setCounsellorForm(v => ({ ...v, professionalTitle: e.target.value }))} placeholder="e.g. University Counsellor" /></label>
            <label><span>Email address</span><input type="email" required value={counsellorForm.email} onChange={e => setCounsellorForm(v => ({ ...v, email: e.target.value }))} /></label>
            <label><span>Temporary password</span><input type="password" minLength={12} maxLength={72} required value={counsellorForm.password} onChange={e => setCounsellorForm(v => ({ ...v, password: e.target.value }))} /></label>
            <label className="admin-form-wide"><span>Biography</span><textarea rows="3" maxLength={5000} value={counsellorForm.biography} onChange={e => setCounsellorForm(v => ({ ...v, biography: e.target.value }))} /></label>
            <div className="admin-form-actions"><button className="admin-primary-button" type="submit" disabled={busy === 'create-counsellor'}>{busy === 'create-counsellor' ? 'Creating…' : 'Create counsellor account'}</button></div>
            <p className="admin-form-help">Accounts are created as active. Counsellors must sign in and enable appointment requests before students can book.</p>
          </form>}
          <label className="admin-search"><Search size={17} /><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Search counsellor name, email or title" /></label>
          <div className="admin-table-wrap"><table><thead><tr><th>COUNSELLOR</th><th>PROFILE</th><th>OPEN SLOTS</th><th>APPOINTMENTS</th><th>ACCOUNT</th><th>ACTION</th></tr></thead><tbody>
            {filteredCounsellors.map(person => <tr key={person.userId}><td><strong>{person.displayName}</strong><small>{person.email}</small></td><td>{person.professionalTitle || 'Counsellor'}<small>{person.acceptingAppointments ? 'Accepting requests' : 'Not accepting requests'}</small></td><td>{person.openSlots}</td><td>{person.appointmentCount}</td><td><StatusPill enabled={person.enabled} /></td><td><button className="admin-row-action" type="button" disabled={busy === person.userId} onClick={() => changeAccountStatus('counsellor', person)}>{busy === person.userId ? 'Saving…' : person.enabled ? 'Disable' : 'Enable'}</button></td></tr>)}
          </tbody></table>{!filteredCounsellors.length && <div className="admin-empty">No counsellor accounts match your search.</div>}</div>
          <p className="admin-form-help">Disabling a counsellor also turns off acceptance of new appointment requests. Existing appointment history is retained.</p>
        </section>}

        {section === 'education' && <section className="admin-panel admin-table-panel">
          <div className="admin-panel-heading"><div><span>STUDENT LEARNING LIBRARY</span><h2>Education resources</h2></div><button className="admin-primary-button" type="button" onClick={() => { setEditingResource(null); setEducationForm(emptyEducation); setShowEducationForm(value => !value) }}>{showEducationForm ? <X size={16} /> : <Plus size={16} />}{showEducationForm ? 'Close editor' : 'New resource'}</button></div>
          {showEducationForm && <form className="admin-form-grid" onSubmit={saveResource}>
            <label><span>Title</span><input required maxLength={150} value={educationForm.title} onChange={e => setEducationForm(v => ({ ...v, title: e.target.value }))} /></label>
            <label><span>URL slug</span><input required maxLength={120} pattern="[a-z0-9]+([_-][a-z0-9]+)*" value={educationForm.slug} onChange={e => setEducationForm(v => ({ ...v, slug: e.target.value }))} /></label>
            <label><span>Topic</span><input required maxLength={100} value={educationForm.topic} onChange={e => setEducationForm(v => ({ ...v, topic: e.target.value }))} /></label>
            <label><span>Content type</span><select value={educationForm.contentType} onChange={e => setEducationForm(v => ({ ...v, contentType: e.target.value }))}><option value="ARTICLE">Article</option><option value="VIDEO">Video</option><option value="EXERCISE">Exercise</option></select></label>
            <label className="admin-form-wide"><span>Summary</span><input required maxLength={500} value={educationForm.summary} onChange={e => setEducationForm(v => ({ ...v, summary: e.target.value }))} /></label>
            <label className="admin-form-wide"><span>Content</span><textarea required rows="5" value={educationForm.content} onChange={e => setEducationForm(v => ({ ...v, content: e.target.value }))} /></label>
            <label><span>Resource URL (required for videos)</span><input type="url" value={educationForm.resourceUrl} onChange={e => setEducationForm(v => ({ ...v, resourceUrl: e.target.value }))} placeholder="https://" /></label>
            <label><span>Display order</span><input type="number" min="0" value={educationForm.displayOrder} onChange={e => setEducationForm(v => ({ ...v, displayOrder: Number(e.target.value) }))} /></label>
            <div className="admin-form-actions"><button className="admin-primary-button" type="submit" disabled={busy === 'save-resource'}>{busy === 'save-resource' ? 'Saving…' : editingResource ? 'Save changes' : 'Save draft'}</button></div>
            <p className="admin-form-help">New resources are saved as drafts. Publish them when they are ready for students.</p>
          </form>}
          <div className="admin-resource-list">{resources.map(resource => <article key={resource.id} className="admin-resource-row"><span className="admin-resource-icon">{resource.contentType === 'VIDEO' ? <Video size={18} /> : <FileText size={18} />}</span><div className="admin-resource-copy"><strong>{resource.title}</strong><small>{resource.topic} · {resource.contentType} · {resource.status || (resource.published ? 'PUBLISHED' : 'DRAFT')}</small></div><span className={`admin-status ${resource.published ? 'is-enabled' : 'is-disabled'}`}>{resource.published ? 'Published' : 'Draft'}</span><button type="button" className="admin-text-button" onClick={() => startEditResource(resource)}>Edit</button><button type="button" className="admin-row-action" disabled={busy === resource.id} onClick={() => togglePublication(resource)}>{resource.published ? 'Unpublish' : 'Publish'}</button></article>)}{!resources.length && <div className="admin-empty">No education resources have been created.</div>}</div>
        </section>}

        {section === 'consent' && <div className="admin-consent-layout">
          <section className="admin-panel"><div className="admin-panel-heading"><div><span>POLICY VERSIONING</span><h2>Publish a new guideline</h2></div><ShieldCheck size={19} /></div>
            <form className="admin-consent-form" onSubmit={publishConsent}>
              <label><span>Purpose</span><select value={consentForm.purposeCode} onChange={e => setConsentForm(v => ({ ...v, purposeCode: e.target.value }))}><option value="PLATFORM_ACCESS">Platform access guidelines</option><option value="COUNSELLING">Counselling consent</option></select></label>
              <label><span>Version label</span><input required maxLength={30} value={consentForm.versionLabel} onChange={e => setConsentForm(v => ({ ...v, versionLabel: e.target.value }))} placeholder="e.g. 1.1" /></label>
              <label><span>Document title</span><input required maxLength={200} value={consentForm.title} onChange={e => setConsentForm(v => ({ ...v, title: e.target.value }))} /></label>
              <label><span>Full document text</span><textarea required rows="9" value={consentForm.body} onChange={e => setConsentForm(v => ({ ...v, body: e.target.value }))} /></label>
              <p className="admin-form-help">Published documents are immutable to preserve the consent audit trail. Publish a new version instead of editing an existing one.</p>
              <button className="admin-primary-button" type="submit" disabled={busy === 'publish-consent'}>{busy === 'publish-consent' ? 'Publishing…' : 'Publish new version'}</button>
            </form>
          </section>
          <section className="admin-panel"><div className="admin-panel-heading"><div><span>DOCUMENT REGISTER</span><h2>Published versions</h2></div><span className="admin-count">{consentDocuments.length} versions</span></div>
            <div className="admin-resource-list">{consentDocuments.map(document => <article key={document.id} className="admin-resource-row admin-consent-row"><span className="admin-resource-icon"><ShieldCheck size={18} /></span><div className="admin-resource-copy"><strong>{document.title}</strong><small>{document.purposeCode} · v{document.versionLabel} · {dateLabel(document.publishedAt)}</small></div><span className="admin-status is-enabled">Published</span></article>)}{!consentDocuments.length && <div className="admin-empty">No consent documents have been published.</div>}</div>
          </section>
        </div>}
      </div>
    </section>
  </main>
}

export default AdminWorkspace
