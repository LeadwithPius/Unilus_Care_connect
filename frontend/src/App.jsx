import { useState } from 'react'

const CURRENT_YEAR = new Date().getFullYear()
import { motion, AnimatePresence } from 'motion/react'
import {
  ArrowRight,
  ArrowUpRight,
  Heart,
  Menu,
  X,
  ShieldCheck,
  BookOpen,
  MessageCircleHeart,
  Wind,
  Activity,
  Sparkles,
} from 'lucide-react'
import './App.css'
import './StudentExperience.css'
import AppLayout from './components/layout/AppLayout.jsx'
import Learn from './pages/Learn.jsx'
import CounsellorWorkspace from './pages/CounsellorWorkspace.jsx'
import AdminWorkspace from './pages/AdminWorkspace.jsx'
import { Home, Support, Me } from './pages/StudentPlaceholders.jsx'

import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
} from 'react-router-dom'

import {
  LoginPage,
  RegisterPage,
  ForgotPasswordPage,
  ResetPasswordPage,
  VerifyEmailPage,
  AdminPortal,
} from './AuthPages.jsx'


const features = [
  {
    id: 1,
    title: 'Take a breath',
    description:
      'Make space for yourself with simple breathing and relaxation exercises.',
    label: 'MINDFULNESS',
    icon: Wind,
    className: 'feature-mint',
    link: 'Explore exercises',
  },
  {
    id: 2,
    title: 'Learn about wellbeing',
    description:
      'Explore useful articles and videos designed to support your mental wellbeing.',
    label: 'EDUCATION HUB',
    icon: BookOpen,
    className: 'feature-cream',
    link: 'Explore resources',
  },
  {
    id: 3,
    title: 'Talk to someone',
    description:
      'Find support and connect with a counsellor when you need someone to talk to.',
    label: 'COUNSELLING',
    icon: MessageCircleHeart,
    className: 'feature-lavender',
    link: 'Find support',
  },
  {
    id: 4,
    title: 'Check in with yourself',
    description:
      'Reflect on how you are feeling and keep track of your wellbeing over time.',
    label: 'WELLBEING',
    icon: Activity,
    className: 'feature-peach',
    link: 'Start a check-in',
  },
  {
    id: 5,
    title: 'A little more balance',
    description:
      'Discover practical ways to manage stress, build healthy habits and recharge.',
    label: 'SELF-CARE',
    icon: Heart,
    className: 'feature-blue',
    link: 'Discover more',
  },
  {
    id: 6,
    title: 'Support when it matters',
    description:
      'Find guidance on accessing help and taking the next step towards support.',
    label: 'STUDENT SUPPORT',
    icon: ShieldCheck,
    className: 'feature-green',
    link: 'Get support',
  },
]

const navLinks = [
  { label: 'Home', href: '#home' },
  { label: 'What we offer', href: '#features' },
  { label: 'About CareConnect', href: '#about' },
]

function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <header className="site-header">
      <nav className="navbar">
        <a href="#home" className="brand" aria-label="CareConnect home">
          <img
            src="/assets/branding/careconnect-logo.png"
            alt="CareConnect"
            className="brand-logo"
          />
          <span className="brand-name">CareConnect</span>
        </a>

        <div className={`nav-links ${menuOpen ? 'nav-links-open' : ''}`}>
          {navLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              onClick={() => setMenuOpen(false)}
            >
              {link.label}
            </a>
          ))}
          <a
            href="/login"
            className="mobile-login-link"
            onClick={() => setMenuOpen(false)}
          >
            Log in
          </a>
        </div>

        <div className="nav-actions">
          <a href="/login" className="nav-login">
            Log in
          </a>
          <a href="/register" className="button button-nav">
            Get started <ArrowUpRight size={16} />
          </a>
        </div>

        <button
          className="mobile-menu-button"
          type="button"
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? <X size={23} /> : <Menu size={23} />}
        </button>
      </nav>
    </header>
  )
}

function Hero() {
  return (
    <section className="hero-section" id="home">
      <div className="hero-background-shape hero-shape-one" />
      <div className="hero-background-shape hero-shape-two" />

      <div className="hero-content">
        <motion.div
          className="hero-copy"
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
        >
          <div className="eyebrow">
            <span className="eyebrow-dot" />
            YOUR WELLBEING MATTERS
          </div>

          <h1>
            Support that fits
            <br />
            around your <span>timetable.</span>
          </h1>

          <p className="hero-description">
            University life comes with its own challenges. CareConnect gives
            you a welcoming space to find support, take care of yourself and
            focus on what matters.
          </p>

          <div className="hero-actions">
            <a href="/register" className="button button-primary">
              Get started <ArrowRight size={18} />
            </a>
            <a href="#features" className="button button-secondary">
              Explore CareConnect
            </a>
          </div>

          <div className="hero-assurance">
            <div className="assurance-icon">
              <ShieldCheck size={18} />
            </div>
            <span>A supportive space for UNILUS students.</span>
          </div>
        </motion.div>

        <motion.div
          className="hero-visual"
          initial={{ opacity: 0, scale: 0.92, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ duration: 0.85, delay: 0.15 }}
        >
          <div className="visual-orbit orbit-one" />
          <div className="visual-orbit orbit-two" />

          <div className="logo-glow">
            <img
              src="/assets/branding/careconnect-animated-logo.gif"
              alt="CareConnect animated heart logo"
              className="hero-logo"
            />
          </div>

          <motion.div
            className="floating-note note-top"
            animate={{ y: [0, -8, 0] }}
            transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
          >
            <div className="note-icon note-icon-green">
              <Heart size={18} />
            </div>
            <div>
              <strong>Your wellbeing first</strong>
              <span>One step at a time.</span>
            </div>
          </motion.div>

          <motion.div
            className="floating-note note-bottom"
            animate={{ y: [0, 8, 0] }}
            transition={{
              duration: 4.5,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          >
            <div className="note-icon note-icon-cream">
              <Sparkles size={18} />
            </div>
            <div>
              <strong>A space for you</strong>
              <span>Support without judgement.</span>
            </div>
          </motion.div>

          <div className="visual-caption">
            <span>CARE</span>
            <span className="caption-divider" />
            <span>CONNECT</span>
            <span className="caption-divider" />
            <span>GROW</span>
          </div>
        </motion.div>
      </div>

      <div className="hero-bottom">
        <span>MADE WITH STUDENTS IN MIND</span>
        <span className="hero-bottom-line" />
        <span>UNIVERSITY OF LUSAKA</span>
      </div>
    </section>
  )
}

function Features() {
  const [activeIndex, setActiveIndex] = useState(0)
  const visibleFeatures = [
    features[activeIndex],
    features[(activeIndex + 1) % features.length],
    features[(activeIndex + 2) % features.length],
  ]

  const next = () => setActiveIndex((index) => (index + 1) % features.length)
  const previous = () =>
    setActiveIndex((index) => (index - 1 + features.length) % features.length)

  return (
    <section className="features-section" id="features">
      <div className="section-heading">
        <div>
          <div className="eyebrow">
            <span className="eyebrow-dot" />
            A LITTLE SUPPORT GOES A LONG WAY
          </div>
          <h2>
            Your wellbeing,
            <br />
            <span>all in one place.</span>
          </h2>
        </div>

        <div className="feature-controls">
          <button
            type="button"
            aria-label="Previous features"
            onClick={previous}
            className="carousel-control"
          >
            <ArrowRight className="arrow-back" size={20} />
          </button>
          <button
            type="button"
            aria-label="Next features"
            onClick={next}
            className="carousel-control"
          >
            <ArrowRight size={20} />
          </button>
        </div>
      </div>

      <div className="feature-grid">
        <AnimatePresence mode="wait">
          {visibleFeatures.map((feature, index) => {
            const Icon = feature.icon

            return (
              <motion.article
                key={`${feature.id}-${activeIndex}`}
                className={`feature-card ${feature.className}`}
                initial={{ opacity: 0, y: 18 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -12 }}
                transition={{ duration: 0.3, delay: index * 0.07 }}
              >
                <div className="feature-card-top">
                  <span className="feature-label">{feature.label}</span>
                  <span className="feature-icon">
                    <Icon size={22} strokeWidth={1.8} />
                  </span>
                </div>

                <div className="feature-card-content">
                  <h3>{feature.title}</h3>
                  <p>{feature.description}</p>
                </div>

                <a href="/login" className="feature-link">
                  {feature.link} <ArrowUpRight size={17} />
                </a>
              </motion.article>
            )
          })}
        </AnimatePresence>
      </div>

      <div className="carousel-pagination">
        {features.map((feature, index) => (
          <button
            key={feature.id}
            type="button"
            aria-label={`Show feature ${index + 1}`}
            aria-current={activeIndex === index ? 'true' : undefined}
            className={`pagination-dot ${
              activeIndex === index ? 'pagination-dot-active' : ''
            }`}
            onClick={() => setActiveIndex(index)}
          />
        ))}
      </div>
    </section>
  )
}

function AboutSection() {
  return (
    <section className="about-section" id="about">
      <div className="about-logo-wrap">
        <img
          src="/assets/branding/careconnect-animated-logo.gif"
          alt=""
          className="about-logo"
        />
      </div>

      <div className="about-copy">
        <div className="eyebrow">
          <span className="eyebrow-dot" />
          A CONNECTION THAT CARES
        </div>

        <h2>
          You don't have to
          <br />
          figure it all out <span>alone.</span>
        </h2>

        <p>
          CareConnect is a student wellbeing platform created to make
          information, self-care tools and counselling support easier to
          access throughout your university journey.
        </p>

        <a href="/register" className="text-link">
          Find your starting point <ArrowRight size={18} />
        </a>
      </div>
    </section>
  )
}

function CallToAction() {
  return (
    <section className="cta-section">
      <div className="cta-content">
        <span className="cta-kicker">YOUR NEXT STEP STARTS HERE</span>
        <h2>
          Make room for
          <br />
          <span>your wellbeing.</span>
        </h2>
        <p>
          Explore the support and resources available to you through
          CareConnect.
        </p>
        <a href="/register" className="button button-light">
          Join CareConnect <ArrowRight size={18} />
        </a>
      </div>

      <div className="cta-decoration cta-decoration-one" />
      <div className="cta-decoration cta-decoration-two" />
      <Heart className="cta-heart" size={170} strokeWidth={0.7} />
    </section>
  )
}

function Footer() {
  return (
    <footer className="site-footer">
      <a href="#home" className="footer-brand">
        <img
          src="/assets/branding/careconnect-logo.png"
          alt=""
          className="footer-logo"
        />
        <span>CareConnect</span>
      </a>

      <p>Supporting your wellbeing throughout your university journey.</p>

      <div className="footer-links">
        <a href="#home">Home</a>
        <a href="#features">What we offer</a>
        <a href="/login">Log in</a>
      </div>

      <div className="footer-bottom">
        <span>© {CURRENT_YEAR} CareConnect</span>
        <span>University of Lusaka</span>
      </div>
    </footer>
  )
}

function LandingPage() {
  return (
    <>
      <Navbar />
      <main>
        <Hero />
        <Features />
        <AboutSection />
        <CallToAction />
      </main>
      <Footer />
    </>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />

        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />

        <Route element={<AppLayout />}>
          <Route path="/student" element={<Navigate to="/learn" replace />} />
          <Route path="/learn" element={<Learn />} />
          <Route path="/home" element={<Home />} />
          <Route path="/support" element={<Support />} />
          <Route path="/me" element={<Me />} />
        </Route>
        <Route path="/counsellor" element={<CounsellorWorkspace />} />
        <Route path="/admin" element={<AdminWorkspace />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App