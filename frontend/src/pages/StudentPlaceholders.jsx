import { ArrowRight, HeartHandshake, House, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'

function Placeholder({ eyebrow, title, description, Icon }) {
  return <main className="student-placeholder">
    <span className="placeholder-icon"><Icon size={25} /></span><span className="learn-section-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p>
    <Link to="/learn">Explore the learning library <ArrowRight size={17} /></Link>
  </main>
}

export function Home() { return <Placeholder eyebrow="Your space" title="Welcome to CareConnect." description="Your student wellbeing home is being prepared. In the meantime, explore practical resources and gentle ways to care for yourself." Icon={House} /> }
export function Support() { return <Placeholder eyebrow="We're here to help" title="Support that listens." description="Your counselling and support booking workspace is coming next. If you need urgent help, contact a trusted person or local emergency service now." Icon={HeartHandshake} /> }
export function Me() { return <Placeholder eyebrow="Your account" title="A space that's yours." description="Your profile and wellbeing preferences will appear here." Icon={UserRound} /> }
