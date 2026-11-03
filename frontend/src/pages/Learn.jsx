import { useEffect, useMemo, useState } from 'react'
import { api } from '../services/api'
import { topics } from '../data/topics'
import { fallbackVideos } from '../data/videos'
import { CategoryChips, HeroCard, SectionHeader, TopicGrid, VideoGrid, VideoSearch } from '../components/learn/LearnComponents'
import WellnessToolkit from '../components/learn/WellnessToolkit'

const categories = ['All', 'Grounding', 'Anxiety', 'Stress', 'Sleep']

function normalizeVideo(video, index) {
  const topic = video.topic || video.category || 'Wellbeing'
  return {
    id: video.id || video.slug || `resource-${index}`,
    category: topic,
    title: video.title || 'Wellbeing video',
    description: video.summary || video.description || 'A CareConnect learning resource.',
    url: video.resourceUrl || video.url || '#',
  }
}

export default function Learn() {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('All')
  const [videos, setVideos] = useState(fallbackVideos)
  const [selectedTopic, setSelectedTopic] = useState(null)

  useEffect(() => {
    let active = true
    api.education().then(resources => {
      const list = Array.isArray(resources) ? resources : Array.isArray(resources?.content) ? resources.content : []
      const publishedVideos = list.filter(resource => String(resource.contentType || '').toUpperCase() === 'VIDEO' && resource.resourceUrl)
      if (active && publishedVideos.length) setVideos(publishedVideos.map(normalizeVideo))
    }).catch(error => {
      console.warn('Education video resources could not be loaded:', error)
    })
    return () => { active = false }
  }, [])

  const filteredVideos = useMemo(() => videos.filter(video => {
    const matchesCategory = category === 'All' || video.category.toLowerCase() === category.toLowerCase()
    const term = query.trim().toLowerCase()
    const matchesQuery = !term || [video.title, video.description, video.category].some(value => value.toLowerCase().includes(term))
    return matchesCategory && matchesQuery
  }), [videos, category, query])

  const videoCategories = useMemo(() => ['All', ...new Set([...categories.slice(1), ...videos.map(video => video.category)])], [videos])

  return <main className="learn-page">
    <HeroCard />
    <section className="learn-library-section">
      <SectionHeader eyebrow="Explore" title="Learning library" aside="Small steps count." />
      <TopicGrid topics={topics} onSelect={setSelectedTopic} />
      {selectedTopic && <div className="topic-detail" role="dialog" aria-label={selectedTopic.title}>
        <div><span>LEARNING NOTE</span><h3>{selectedTopic.title}</h3><p>{selectedTopic.description}</p></div>
        <button type="button" onClick={() => setSelectedTopic(null)}>Close</button>
      </div>}
    </section>
    <section className="learn-videos-section">
      <SectionHeader eyebrow="Watch & practise" title="Video tutorials" aside={`${videos.length} videos`} description="Short, expert-led videos to help with grounding, stress, breathing and emotional wellbeing." />
      <VideoSearch value={query} onChange={setQuery} />
      <CategoryChips categories={videoCategories} active={category} onSelect={setCategory} />
      <VideoGrid videos={filteredVideos} />
      <p className="learn-disclaimer">Videos are educational resources from their original publishers. They do not replace professional counselling or emergency support.</p>
    </section>
    <WellnessToolkit />
  </main>
}
