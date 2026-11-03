import { useEffect, useRef, useState } from 'react'
import { Gamepad2, Sprout, Wind, X } from 'lucide-react'

const BREATHING_PHASES = [
  { label: 'Breathe in', seconds: 4, className: 'is-inhaling' },
  { label: 'Hold gently', seconds: 2, className: 'is-holding' },
  { label: 'Breathe out', seconds: 6, className: 'is-exhaling' },
]

function BreathingExercise({ onClose }) {
  const [phaseIndex, setPhaseIndex] = useState(0)
  const [seconds, setSeconds] = useState(BREATHING_PHASES[0].seconds)
  const phase = BREATHING_PHASES[phaseIndex]

  useEffect(() => {
    const timer = window.setInterval(() => {
      setSeconds(current => {
        if (current > 1) return current - 1
        setPhaseIndex(index => (index + 1) % BREATHING_PHASES.length)
        return BREATHING_PHASES[(phaseIndex + 1) % BREATHING_PHASES.length].seconds
      })
    }, 1000)
    return () => window.clearInterval(timer)
  }, [phaseIndex])

  return <Modal title="A moment to breathe" onClose={onClose}>
    <p className="tool-modal-intro">Follow the circle gently. Breathe in as it expands, pause softly, then breathe out as it contracts.</p>
    <div className={`guided-breath-orb ${phase.className}`} aria-live="polite">
      <strong>{phase.label}</strong><span>{seconds}</span>
    </div>
    <p className="tool-modal-note">The rhythm is 4 seconds in, a gentle 2-second pause, and 6 seconds out. There is no need to force your breath. If this feels uncomfortable, stop and try noticing the room around you instead.</p>
  </Modal>
}

function SnakeGame({ onClose }) {
  const cooldownMs = 12 * 60 * 60 * 1000
  const durationMs = 5 * 60 * 1000
  const [blockedUntil, setBlockedUntil] = useState(() => Number(localStorage.getItem('careconnect-snake-blocked-until') || 0))
  const [now, setNow] = useState(0)
  const [remaining, setRemaining] = useState(durationMs)
  const [snake, setSnake] = useState([[5, 5], [4, 5], [3, 5]])
  const [food, setFood] = useState([11, 9])
  const [direction, setDirection] = useState([1, 0])
  const [score, setScore] = useState(0)
  const [finished, setFinished] = useState(false)
  const snakeRef = useRef([[5, 5], [4, 5], [3, 5]])
  const foodRef = useRef([11, 9])
  const directionRef = useRef([1, 0])
  const coolingDown = blockedUntil > now

  useEffect(() => {
    const updateClock = () => setNow(Date.now())
    updateClock()
    const clock = window.setInterval(updateClock, 60000)
    return () => window.clearInterval(clock)
  }, [])

  useEffect(() => { snakeRef.current = snake }, [snake])
  useEffect(() => { foodRef.current = food }, [food])
  useEffect(() => { directionRef.current = direction }, [direction])

  useEffect(() => {
    if (coolingDown || finished) return undefined

    const handleKey = event => {
      const keys = { ArrowUp: [0, -1], ArrowDown: [0, 1], ArrowLeft: [-1, 0], ArrowRight: [1, 0] }
      const next = keys[event.key]
      const current = directionRef.current
      if (next && (next[0] !== -current[0] || next[1] !== -current[1])) {
        event.preventDefault()
        directionRef.current = next
        setDirection(next)
      }
    }

    const startedAt = Date.now()
    window.addEventListener('keydown', handleKey)
    const timer = window.setInterval(() => {
      const elapsed = Date.now() - startedAt
      setRemaining(Math.max(0, durationMs - elapsed))
      if (elapsed >= durationMs) {
        const until = Date.now() + cooldownMs
        localStorage.setItem('careconnect-snake-blocked-until', String(until))
        setBlockedUntil(until)
        setNow(Date.now())
        setFinished(true)
        return
      }

      const current = snakeRef.current
      const vector = directionRef.current
      const head = [(current[0][0] + vector[0] + 20) % 20, (current[0][1] + vector[1] + 20) % 20]
      if (current.some(([x, y]) => x === head[0] && y === head[1])) {
        const reset = [[5, 5], [4, 5], [3, 5]]
        snakeRef.current = reset
        setSnake(reset)
        setScore(0)
        return
      }

      const next = [head, ...current]
      const currentFood = foodRef.current
      if (head[0] === currentFood[0] && head[1] === currentFood[1]) {
        const nextFood = [Math.floor(Math.random() * 20), Math.floor(Math.random() * 20)]
        foodRef.current = nextFood
        setFood(nextFood)
        setScore(value => value + 1)
      } else {
        next.pop()
      }
      snakeRef.current = next
      setSnake(next)
    }, 180)

    return () => {
      window.removeEventListener('keydown', handleKey)
      window.clearInterval(timer)
    }
  }, [coolingDown, finished, cooldownMs, durationMs])

  if (coolingDown) return <Modal title="Snake Xenzia is resting" onClose={onClose}>
    <p className="tool-modal-intro">You have reached the play limit for this session. You can play again in {Math.ceil((blockedUntil - now) / 3600000)} hour(s).</p>
  </Modal>

  return <Modal title="Snake Xenzia" onClose={onClose}>
    <p className="tool-modal-intro">Use the arrow keys to guide the snake. Each play session is limited to five minutes. Take a break whenever you need one.</p>
    <div className="snake-board" role="img" aria-label={`Snake game, score ${score}`}>
      {Array.from({ length: 400 }, (_, index) => {
        const x = index % 20, y = Math.floor(index / 20)
        const isSnake = snake.some(([sx, sy]) => sx === x && sy === y)
        return <span key={index} className={isSnake ? 'snake-cell' : food[0] === x && food[1] === y ? 'food-cell' : ''} />
      })}
    </div>
    <div className="snake-score">Score: {score} · {Math.ceil(remaining / 60000)} min left</div>
    <div className="snake-controls">{[['↑',[0,-1]],['←',[-1,0]],['↓',[0,1]],['→',[1,0]]].map(([label, vector]) => <button key={label} type="button" onClick={() => {
      const current = directionRef.current
      if (vector[0] !== -current[0] || vector[1] !== -current[1]) {
        directionRef.current = vector
        setDirection(vector)
      }
    }}>{label}</button>)}</div>
    {finished && <p className="tool-modal-note">Your five-minute play session has ended. Snake Xenzia will be available again in 12 hours.</p>}
  </Modal>
}

function GoalAchiever({ onClose }) {
  const [goal, setGoal] = useState(() => localStorage.getItem('careconnect-goal') || '')
  const [steps, setSteps] = useState(() => {
    try { return JSON.parse(localStorage.getItem('careconnect-goal-steps') || '[]') } catch { return [] }
  })
  const [newStep, setNewStep] = useState('')
  useEffect(() => { localStorage.setItem('careconnect-goal', goal); localStorage.setItem('careconnect-goal-steps', JSON.stringify(steps)) }, [goal, steps])
  return <Modal title="Goal achiever" onClose={onClose}>
    <p className="tool-modal-intro">Break something that matters to you into small, achievable steps.</p>
    <label className="goal-field">My goal<input value={goal} onChange={event => setGoal(event.target.value)} placeholder="What would you like to work towards?" maxLength={100} /></label>
    <form className="goal-add" onSubmit={event => { event.preventDefault(); if (newStep.trim()) { setSteps(current => [...current, { id: Date.now(), title: newStep.trim(), done: false }]); setNewStep('') } }}>
      <input value={newStep} onChange={event => setNewStep(event.target.value)} placeholder="Add a small step…" maxLength={100} />
      <button type="submit">Add</button>
    </form>
    <div className="goal-steps">{steps.map(step => <label key={step.id}><input type="checkbox" checked={step.done} onChange={() => setSteps(current => current.map(item => item.id === step.id ? { ...item, done: !item.done } : item))} /><span>{step.title}</span></label>)}</div>
    <p className="tool-modal-note">{steps.filter(step => step.done).length} of {steps.length} steps completed. Progress at your own pace.</p>
  </Modal>
}

function FinchGame({ onClose }) {
  const [checked, setChecked] = useState([])
  const tasks = ['Drink a glass of water', 'Write one journal line', 'Step outside for five minutes', 'Message someone you trust']
  const pets = ['🥚', '🐣', '🐥', '🐤', '🦜']
  return <Modal title="Finch" onClose={onClose}>
    <div className="finch-pet" aria-live="polite">{pets[checked.length]}</div>
    <p className="tool-modal-intro">Complete a gentle self-care task and watch your little companion grow.</p>
    <div className="finch-tasks">{tasks.map((task, index) => <label key={task}><input type="checkbox" checked={checked.includes(index)} onChange={() => setChecked(current => current.includes(index) ? current.filter(item => item !== index) : [...current, index])} />{task}</label>)}</div>
  </Modal>
}

function Modal({ title, children, onClose }) {
  return <div className="tool-modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
    <section className="tool-modal" role="dialog" aria-modal="true" aria-label={title}>
      <div className="tool-modal-heading"><h3>{title}</h3><button type="button" onClick={onClose} aria-label="Close"><X size={19} /></button></div>{children}
    </section>
  </div>
}

export default function WellnessToolkit() {
  const [tool, setTool] = useState(null)
  const tools = [
    { id: 'breathing', title: 'Guided breathing', description: 'A gentle four-count breathing rhythm.', Icon: Wind },
    { id: 'snake', title: 'Snake Xenzia', description: 'A familiar, light-hearted game break.', Icon: Gamepad2 },
    { id: 'goal', title: 'Goal achiever', description: 'Grow a goal through small steps.', Icon: Sprout },
    { id: 'finch', title: 'Finch', description: 'A tiny companion for everyday self-care.', Icon: SparklesIcon },
  ]
  return <section className="wellness-toolkit">
    <div className="toolkit-heading"><span>PAUSE & RESET</span><h2>Wellness toolkit</h2><p>Your breathing exercises and wellbeing games are still here whenever you need them.</p></div>
    <div className="toolkit-grid">{tools.map(({ id, title, description, Icon }) => <button key={id} type="button" className="toolkit-card" onClick={() => setTool(id)}><span className="toolkit-icon"><Icon size={20} /></span><strong>{title}</strong><small>{description}</small><span className="toolkit-open">Open tool ↗</span></button>)}</div>
    {tool === 'breathing' && <BreathingExercise onClose={() => setTool(null)} />}
    {tool === 'snake' && <SnakeGame onClose={() => setTool(null)} />}
    {tool === 'goal' && <GoalAchiever onClose={() => setTool(null)} />}
    {tool === 'finch' && <FinchGame onClose={() => setTool(null)} />}
  </section>
}

function SparklesIcon(props) { return <Sprout {...props} />}
