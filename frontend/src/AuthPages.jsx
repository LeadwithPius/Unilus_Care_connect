import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'motion/react'
import {
  ArrowLeft,
  ArrowRight,
  Heart,
  ShieldCheck,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  LoaderCircle,
  LogOut,
} from 'lucide-react'
import { api } from './services/api'
import './AuthPages.css'

const roles = [
  { value: 'STUDENT', label: 'Student' },
  { value: 'COUNSELLOR', label: 'Counsellor' },
  { value: 'ADMIN', label: 'Administrator' },
]

// Verification tokens are single-use. Reuse an in-flight request when React
// Strict Mode replays an effect during development.
const emailVerificationRequests = new Map()

function verifyEmailOnce(token) {
  if (!emailVerificationRequests.has(token)) {
    const request = api.verifyEmail(token).catch((error) => {
      emailVerificationRequests.delete(token)
      throw error
    })

    emailVerificationRequests.set(token, request)
  }

  return emailVerificationRequests.get(token)
}

function AuthShell({ children, heading, description }) {
  return (
    <main className="auth-page">
      <div className="auth-decoration auth-decoration-one" />
      <div className="auth-decoration auth-decoration-two" />

      <Link to="/" className="auth-brand">
        <img
          src="/assets/branding/careconnect-logo.png"
          alt=""
        />
        <span>CareConnect</span>
      </Link>

      <div className="auth-layout">
        <motion.section
          className="auth-intro"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5 }}
        >
          <div className="auth-intro-icon">
            <Heart size={28} />
          </div>

          <span className="auth-kicker">UNIVERSITY OF LUSAKA</span>

          <h1>
            Your wellbeing
            <br />
            matters <span>here.</span>
          </h1>

          <p>
            A supportive space for your university journey.
            Access wellbeing resources, counselling support
            and tools to help you take care of yourself.
          </p>

          <div className="auth-trust-note">
            <ShieldCheck size={19} />
            <span>A welcoming space for UNILUS students.</span>
          </div>
        </motion.section>

        <motion.section
          className="auth-card"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
        >
          <div className="auth-card-heading">
            <span className="auth-kicker">CARECONNECT ACCOUNT</span>
            <h2>{heading}</h2>
            <p>{description}</p>
          </div>

          {children}
        </motion.section>
      </div>

      <footer className="auth-footer">
        <span>© {new Date().getFullYear()} CareConnect</span>
        <span>University of Lusaka</span>
      </footer>
    </main>
  )
}

function FormMessage({ type = 'error', children }) {
  if (!children) return null

  return (
    <div className={`form-message form-message-${type}`} role="alert">
      {type === 'success' ? (
        <CheckCircle2 size={18} />
      ) : (
        <AlertCircle size={18} />
      )}
      <span>{children}</span>
    </div>
  )
}

function PasswordInput({
  label = 'Password',
  value,
  onChange,
  autoComplete = 'current-password',
  required = true,
}) {
  const [visible, setVisible] = useState(false)

  return (
    <label className="form-field">
      <span>{label}</span>
      <div className="password-input-wrap">
        <input
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          required={required}
        />
        <button
          type="button"
          className="password-toggle"
          aria-label={visible ? 'Hide password' : 'Show password'}
          onClick={() => setVisible((current) => !current)}
        >
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
    </label>
  )
}

function SubmitButton({ children, loading }) {
  return (
    <button className="auth-submit" type="submit" disabled={loading}>
      {loading ? (
        <>
          <LoaderCircle className="spinner" size={18} />
          Please wait...
        </>
      ) : (
        <>
          {children}
          <ArrowRight size={18} />
        </>
      )}
    </button>
  )
}

function LoginPage() {
  const navigate = useNavigate()
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('STUDENT')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [searchParams] = useSearchParams()

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const result = await api.login(userId.trim(), password, role)

      const accountRoles = (result.roles || []).map((item) =>
        String(item).replace(/^ROLE_/, '').toUpperCase()
      )

      if (!accountRoles.includes(role)) {
        api.logout()
        throw new Error(
          'Your account does not have the selected role. Please choose the correct account type.'
        )
      }

      const destination = {
        STUDENT: '/student',
        COUNSELLOR: '/counsellor',
        ADMIN: '/admin',
      }[role]

      navigate(destination, { replace: true })
    } catch (err) {
      setError(err.message || 'Unable to sign in. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      heading="Welcome back"
      description="Sign in to continue to your CareConnect account."
    >
      {searchParams.get('registered') === '1' && (
        <FormMessage type="success">
          Your account has been created. You can now sign in with your UNILUS account.
        </FormMessage>
      )}

      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Account type</span>
          <select
            value={role}
            onChange={(event) => setRole(event.target.value)}
          >
            {roles.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>

        <label className="form-field">
          <span>User ID or email address</span>
          <input
            type="text"
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
            placeholder="Enter your user ID"
            autoComplete="username"
            required
          />
        </label>

        <PasswordInput
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        <div className="form-options">
          <Link to="/forgot-password">Forgot password?</Link>
        </div>

        <FormMessage>{error}</FormMessage>

        <SubmitButton loading={loading}>Sign in</SubmitButton>
      </form>

      <div className="auth-switch">
        New to CareConnect? <Link to="/register">Create an account</Link>
      </div>
    </AuthShell>
  )
}

function RegisterPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    studentNumber: '',
    password: '',
    confirmPassword: '',
  })

  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function updateField(event) {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    const email = form.email.trim()

    if (!/^[^@\s]+@(?:[a-z0-9-]+\.)*unilus\.ac\.zm$/i.test(email)) {
      setError('Please use your UNILUS university email address.')
      return
    }

    if (
      form.password.length < 12 ||
      form.password.length > 72
    ) {
      setError('Your password must contain 12 to 72 characters.')
      return
    }

    if (!/[A-Za-z]/.test(form.password) || !/[0-9]/.test(form.password)) {
      setError('Your password must contain at least one letter and one number.')
      return
    }

    if (form.password !== form.confirmPassword) {
      setError('Your passwords do not match.')
      return
    }

    setLoading(true)

    try {
      await api.register({
        fullName: form.fullName.trim(),
        email,
        studentNumber: form.studentNumber.trim(),
        password: form.password,
      })

      navigate('/login?registered=1', { replace: true })
    } catch (err) {
      setError(err.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      heading="Create your account"
      description="Get started with your CareConnect student account."
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Full name</span>
          <input
            name="fullName"
            value={form.fullName}
            onChange={updateField}
            placeholder="Your full name"
            autoComplete="name"
            maxLength={150}
            required
          />
        </label>

        <label className="form-field">
          <span>UNILUS email address</span>
          <input
            name="email"
            type="email"
            value={form.email}
            onChange={updateField}
            placeholder="you@unilus.ac.zm"
            autoComplete="email"
            maxLength={254}
            required
          />
        </label>

        <label className="form-field">
          <span>Student number</span>
          <input
            name="studentNumber"
            value={form.studentNumber}
            onChange={updateField}
            placeholder="Enter your student number"
            maxLength={50}
            required
          />
        </label>

        <PasswordInput
          label="Create password"
          value={form.password}
          onChange={(event) =>
            setForm((current) => ({
              ...current,
              password: event.target.value,
            }))
          }
          autoComplete="new-password"
        />

        <PasswordInput
          label="Confirm password"
          value={form.confirmPassword}
          onChange={(event) =>
            setForm((current) => ({
              ...current,
              confirmPassword: event.target.value,
            }))
          }
          autoComplete="new-password"
        />

        <p className="password-hint">
          Use 12–72 characters, including at least one letter and one number.
        </p>

        <FormMessage>{error}</FormMessage>

        <SubmitButton loading={loading}>Create account</SubmitButton>
      </form>

      <div className="auth-switch">
        Already registered? <Link to="/login">Sign in</Link>
      </div>
    </AuthShell>
  )
}

function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    setLoading(true)

    try {
      const result = await api.forgotPassword(email.trim())

      setMessage(
        result?.message ||
          'If an eligible account exists for that email, a password-reset link will be sent.'
      )
    } catch (err) {
      setError(err.message || 'Unable to submit your request.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      heading="Forgot your password?"
      description="Enter the email address associated with your account."
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Email address</span>
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="email"
            placeholder="Enter your email address"
            required
          />
        </label>

        {message && <FormMessage type="success">{message}</FormMessage>}
        <FormMessage>{error}</FormMessage>

        <SubmitButton loading={loading}>Send reset link</SubmitButton>
      </form>

      <div className="auth-switch">
        <Link to="/login">
          <ArrowLeft size={15} /> Back to login
        </Link>
      </div>
    </AuthShell>
  )
}

function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setMessage('')

    if (!token) {
      setError('The password-reset link is missing its token.')
      return
    }

    if (password.length < 12 || password.length > 72) {
      setError('Your password must contain 12 to 72 characters.')
      return
    }

    if (!/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
      setError('Your password must contain at least one letter and one number.')
      return
    }

    if (password !== confirmPassword) {
      setError('Your passwords do not match.')
      return
    }

    setLoading(true)

    try {
      const result = await api.resetPassword(token, password)
      setMessage(result?.message || 'Your password has been reset.')

      setTimeout(() => navigate('/login', { replace: true }), 1800)
    } catch (err) {
      setError(err.message || 'Unable to reset your password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      heading="Reset your password"
      description="Choose a new password for your CareConnect account."
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <PasswordInput
          label="New password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="new-password"
        />

        <PasswordInput
          label="Confirm new password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          autoComplete="new-password"
        />

        <p className="password-hint">
          Use 12–72 characters, including at least one letter and one number.
        </p>

        {message && <FormMessage type="success">{message}</FormMessage>}
        <FormMessage>{error}</FormMessage>

        <SubmitButton loading={loading}>Update password</SubmitButton>
      </form>

      <div className="auth-switch">
        <Link to="/login">Back to login</Link>
      </div>
    </AuthShell>
  )
}

function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const [status, setStatus] = useState('loading')
  const [message, setMessage] = useState('Verifying your email address...')

  useEffect(() => {
    let active = true

    async function verify() {
      if (!token) {
        setStatus('error')
        setMessage('The verification link is missing its token.')
        return
      }

      try {
        const result = await verifyEmailOnce(token)

        if (active) {
          setStatus('success')
          setMessage(result?.message || 'Your email address has been verified.')
        }
      } catch (err) {
        if (active) {
          setStatus('error')
          setMessage(err.message || 'Email verification failed.')
        }
      }
    }

    verify()

    return () => {
      active = false
    }
  }, [token])

  return (
    <AuthShell
      heading="Email verification"
      description="Confirming your CareConnect account email address."
    >
      <div className={`verification-status verification-${status}`}>
        {status === 'loading' ? (
          <LoaderCircle className="spinner" size={28} />
        ) : status === 'success' ? (
          <CheckCircle2 size={30} />
        ) : (
          <AlertCircle size={30} />
        )}

        <p>{message}</p>
      </div>

      {status !== 'loading' && (
        <Link to="/login" className="auth-submit auth-submit-link">
          Continue to login <ArrowRight size={18} />
        </Link>
      )}
    </AuthShell>
  )
}

function PortalPage({ expectedRole, title, description }) {
  const navigate = useNavigate()
  const [status, setStatus] = useState('loading')
  const [email, setEmail] = useState('')

  useEffect(() => {
    let active = true

    async function verifySession() {
      if (!api.token) {
        navigate('/login', { replace: true })
        return
      }

      try {
        const account = await api.me()

        if (!active) {
          return
        }

        const accountRoles = (account.roles || []).map((item) =>
          String(item).replace(/^ROLE_/, '').toUpperCase()
        )

        if (!accountRoles.includes(expectedRole)) {
          api.logout()
          navigate('/login', { replace: true })
          return
        }

        if (active) {
          setEmail(account.email || '')
          setStatus('ready')
        }
      } catch {
        if (active) {
          api.logout()
          navigate('/login', { replace: true })
        }
      }
    }

    verifySession()

    return () => {
      active = false
    }
  }, [expectedRole, navigate])

  function handleLogout() {
    api.logout()
    navigate('/login', { replace: true })
  }

  if (status === 'loading') {
    return (
      <main className="portal-loading">
        <LoaderCircle className="spinner" size={28} />
        <p>Checking your CareConnect session...</p>
      </main>
    )
  }

  return (
    <main className="portal-entry">
      <div className="portal-entry-card">
        <img
          src="/assets/branding/careconnect-logo.png"
          alt="CareConnect"
        />

        <span className="auth-kicker">{expectedRole} PORTAL</span>
        <h1>{title}</h1>
        <p>{description}</p>
        {email && <span className="portal-email">{email}</span>}

        <div className="portal-notice">
          Your account has been authenticated successfully. The full
          {` ${expectedRole.toLowerCase()} `}
          dashboard is the next stage of the frontend migration.
        </div>

        <button
          type="button"
          className="auth-submit"
          onClick={handleLogout}
        >
          Sign out <LogOut size={18} />
        </button>
      </div>
    </main>
  )
}

export function StudentPortal() {
  return (
    <PortalPage
      expectedRole="STUDENT"
      title="Welcome to your student space."
      description="Your wellbeing resources and support will be available here."
    />
  )
}

export function CounsellorPortal() {
  return (
    <PortalPage
      expectedRole="COUNSELLOR"
      title="Welcome to your counsellor space."
      description="Your counselling workspace will be available here."
    />
  )
}

export function AdminPortal() {
  return (
    <PortalPage
      expectedRole="ADMIN"
      title="Welcome to your administration space."
      description="Your CareConnect administration workspace will be available here."
    />
  )
}

export {
  LoginPage,
  RegisterPage,
  ForgotPasswordPage,
  ResetPasswordPage,
  VerifyEmailPage,
}