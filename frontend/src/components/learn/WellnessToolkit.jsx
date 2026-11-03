import { useEffect, useRef, useState } from 'react'
import { Gamepad2, Sprout, Wind, X } from 'lucide-react'

function BreathingExercise({ onClose }) {
  const [phase, setPhase] = useState('Breathe in')
  const [seconds, setSeconds] = useState(4)
  useEffect(() => {
    const timer = window.setInterval(() => {
      setSeconds(value => {
        if (value <= 1) {
          setPhase(current => current === 'Breathe in' ? 'Breathe out' : 'Breathe in')
          return 4
        }
        return value - 1
      })
    }, 1000)
    return () => window.clearInterval(timer)
  }, [])
  return <Modal title="A moment to breathe" onClose={onClose}>
    <p className="tool-modal-intro">Follow the circle gently. There is no need to force your breath.</p>
    <div className={`guided-breath-orb ${phase === 'Breathe in' ? 'is-inhaling' : 'is-exhaling'}`}><strong>{phase}</strong><span>{seconds}</span></div>
    <p className="tool-modal-note">If focusing on your breathing feels uncomfortable, stop and try noticing the room around you instead.</p>
  </Modal>
}

function SnakeGame({ onClose }) {
  const [snake, setSnake] = useState([[5, 5], [4, 5], [3, 5]])
  const [food, setFood] = useState([11, 9])
  const [direction, setDirection] = useState([1, 0])
  const [score, setScore] = useState(0)
  const directionRef = useRef(direction)
  directionRef.current = direction
  useEffect(() => {
    const handleKey = event => {
      const keys = { ArrowUp: [0, -1], ArrowDown: [0, 1], ArrowLeft: [-1, 0], ArrowRight: [1, 0] }
      const next = keys[event.key]
      if (next && (next[0] !== -directionRef.current[0] || next[1] !== -directionRef.current[1])) {
        event.preventDefault()
        setDirection(next)
      }
    }
    window.addEventListener('keydown', handleKey)
    const timer = window.setInterval(() => {
      setSnake(current => {
        const head = [(current[0][0] + directionRef.current[0] + 20) % 20, (current[0][1] + directionRef.current[1] + 20) % 20]
        if (current.some(([x, y]) => x === head[0] && y === head[1])) {
          setScore(0)
          return [[5, 5], [4, 5], [3, 5]]
        }
        const next = [head, ...current]
        if (head[0] === food[0] && head[1] === food[1]) {
          setScore(value => value + 1)
          setFood([Math.floor(Math.random() * 20), Math.floor(Math.random() * 20)])
        } else next.pop()
        return next
      })
    }, 180)
    return () => { window.removeEventListener('keydown', handleKey); window.clearInterval(timer) }
  }, [food])
  return <Modal title="Snake Xenzia" onClose={onClose}>
    <p className="tool-modal-intro">Use the arrow keys to guide the snake. Take a break whenever you need one.</p>
    <div className="snake-board" role="img" aria-label={`Snake game, score ${score}`}>
      {Array.from({ length: 400 }, (_, index) => {
        const x = index % 20, y = Math.floor(index / 20)
        const isSnake = snake.some(([sx, sy]) => sx === x && sy === y)
        return <span key={index} className={isSnake ? 'snake-cell' : food[0] === x && food[1] === y ? 'food-cell' : ''} />
      })}
    </div>
    <div className="snake-score">Score: {score}</div>
    <div className="snake-controls">{[['↑',[0,-1]],['←',[-1,0]],['↓',[0,1]],['→',[1,0]]].map(([label, vector]) => <button key={label} type="button" onClick={() => { if (vector[0] !== -directionRef.current[0] || vector[1] !== -directionRef.current[1]) setDirection(vector) }}>{label}</button>)}</div>
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

function SparklesIcon(props) { return <Sprout {...props} /> }
