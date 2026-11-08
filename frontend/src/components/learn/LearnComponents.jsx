import { ArrowUpRight, Play, Search, Sparkles } from 'lucide-react'

export function HeroCard() {
  return <section className="learn-hero">
    <div className="learn-hero-copy">
      <span className="learn-eyebrow">Your wellbeing, your pace</span>
      <h1>Learn a little.<br /><em>Feel a little stronger.</em></h1>
      <p>Practical, supportive resources for the things student life brings.</p>
    </div>
    <div className="breath-circle" aria-label="Take a breath">
      <span className="breath-star">✦</span><strong>Take a<br />breath.</strong><i />
    </div>
  </section>
}

export function SectionHeader({ eyebrow, title, aside, description }) {
  return <div className="learn-section-heading">
    <div><span className="learn-section-eyebrow">{eyebrow}</span><div className="learn-title-row"><h2>{title}</h2>{aside && <span className="learn-aside">{aside}</span>}</div>
      {description && <p>{description}</p>}
    </div>
  </div>
}

export function TopicCard({ title, subtitle, onClick }) {
  return <button type="button" className="learn-topic-card" onClick={onClick} aria-label={`Explore ${title}`}>
    <div className="learn-topic-icons"><Sparkles size={17} /><ArrowUpRight size={16} /></div>
    <div><strong>{title}</strong><span>{subtitle}</span></div>
  </button>
}

export function TopicGrid({ topics, onSelect }) {
  return <div className="learn-topic-grid">{topics.map(topic => <TopicCard key={topic.slug} {...topic} onClick={() => onSelect(topic)} />)}</div>
}

export function VideoSearch({ value, onChange }) {
  return <label className="learn-search"><Search size={18} /><input value={value} onChange={event => onChange(event.target.value)} placeholder="Search videos or topics…" aria-label="Search videos or topics" /></label>
}

export function CategoryChips({ categories, active, onSelect }) {
  return <div className="learn-category-chips" aria-label="Filter videos by category">{categories.map(category => <button type="button" key={category} className={active === category ? 'is-active' : ''} aria-pressed={active === category} onClick={() => onSelect(category)}>{category}</button>)}</div>
}

export function VideoCard({ category, title, description, url }) {
  return <article className="learn-video-card">
    <a className="learn-video-thumb" href={url} target="_blank" rel="noopener noreferrer" aria-label={`Watch ${title}`}>
      <span className="learn-video-glow" /><span className="learn-play"><Play size={19} fill="currentColor" /></span><span className="learn-thumb-category">{category}</span>
    </a>
    <div className="learn-video-copy"><span className="learn-video-category">{category}</span><h3>{title}</h3><p>{description}</p>
      <a className="learn-watch-link" href={url} target="_blank" rel="noopener noreferrer">Watch video <ArrowUpRight size={16} /></a>
    </div>
  </article>
}

export function VideoGrid({ videos }) {
  if (!videos.length) return <div className="learn-empty-state"><span>✦</span><strong>No videos match your search.</strong><p>Try another keyword or choose a different topic.</p></div>
  return <div className="learn-video-grid">{videos.map(video => <VideoCard key={video.id} {...video} />)}</div>
}
