import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { LoaderCircle, MessageCircle, RefreshCw, Send, ShieldCheck } from 'lucide-react'
import { api } from '../../services/api'
import { useNavigate } from 'react-router-dom'

function formatConversationDate(value) {
  if (!value) return 'Scheduled session'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Scheduled session'
  return new Intl.DateTimeFormat(undefined, {
    weekday: 'short', day: 'numeric', month: 'short',
  }).format(date)
}

function formatMessageTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(undefined, {
    hour: 'numeric', minute: '2-digit',
  }).format(date)
}

function ChatNotice({ children, type = 'error' }) {
  return children ? <p className={`care-chat-notice care-chat-notice-${type}`} role="status">{children}</p> : null
}

export default function ChatWorkspace({ role = 'STUDENT', embedded = false }) {
  const navigate = useNavigate()
  const [conversations, setConversations] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [loading, setLoading] = useState(true)
  const [messagesLoading, setMessagesLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [messageError, setMessageError] = useState('')
  const bottomRef = useRef(null)
  const activeConversationRef = useRef('')
  const selected = useMemo(
    () => conversations.find(item => item.conversationId === selectedId) || null,
    [conversations, selectedId],
  )

  const loadConversations = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true)
    try {
      const result = await api.chatConversations(role)
      const items = Array.isArray(result) ? result : []
      setConversations(items)
      setSelectedId(current => {
        if (current && items.some(item => item.conversationId === current)) return current
        return items[0]?.conversationId || ''
      })
      setError('')
    } catch (err) {
      if (err.status === 401 || err.status === 403) { api.logout(); navigate('/login', { replace: true }); return }
      setError(err.message || 'Your conversations could not be loaded.')
    } finally {
      if (showLoading) setLoading(false)
      else setLoading(false)
    }
  }, [navigate, role])

  const loadMessages = useCallback(async (conversationId, showLoading = false) => {
    if (!conversationId) {
      setMessages([])
      return
    }
    if (showLoading) setMessagesLoading(true)
    try {
      const result = await api.chatMessages(role, conversationId)
      if (activeConversationRef.current === conversationId) setMessages(Array.isArray(result) ? result : [])
      setMessageError('')
    } catch (err) {
      if (err.status === 401 || err.status === 403) { api.logout(); navigate('/login', { replace: true }); return }
      if (activeConversationRef.current === conversationId) {
        setMessageError(err.message || 'Messages could not be loaded.')
      }
    } finally {
      if (showLoading) setMessagesLoading(false)
    }
  }, [navigate, role])

  useEffect(() => {
    activeConversationRef.current = selectedId
  }, [selectedId])

  useEffect(() => {
    let active = true
    const timer = window.setInterval(() => { void loadConversations(false) }, 15000)
    queueMicrotask(() => {
      if (active) void loadConversations(true)
    })
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [loadConversations])

  useEffect(() => {
    if (!selectedId) return undefined

    let active = true
    const conversationId = selectedId
    const timer = window.setInterval(() => {
      void loadMessages(conversationId, false)
    }, 4000)
    queueMicrotask(() => {
      if (active) void loadMessages(conversationId, true)
    })
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [loadMessages, selectedId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages])

  async function sendMessage(event) {
    event.preventDefault()
    const content = draft.trim()
    if (!content || !selected || sending || !selected.canSend) return
    setSending(true)
    setMessageError('')
    try {
      const created = await api.sendChatMessage(role, selectedId, content)
      setMessages(current => current.some(message => message.messageId === created.messageId)
        ? current
        : [...current, created])
      setDraft('')
      void loadConversations(false)
    } catch (err) {
      if (err.status === 401 || err.status === 403) { api.logout(); navigate('/login', { replace: true }); return }
      setMessageError(err.message || 'Your message could not be sent.')
    } finally {
      setSending(false)
    }
  }

  const heading = role === 'COUNSELLOR' ? 'Student conversations' : 'Your conversations'
  const emptyCopy = role === 'COUNSELLOR'
    ? 'Confirmed counselling sessions will appear here. Student identities are shown as private references.'
    : 'Once a counsellor confirms your appointment, your private conversation will appear here.'

  return <section className={`care-chat-workspace${embedded ? ' care-chat-embedded' : ''}`}>
    <div className="care-chat-heading">
      <div>
        <span className="learn-section-eyebrow">{role === 'COUNSELLOR' ? 'SECURE STUDENT MESSAGING' : 'PRIVATE COUNSELLING MESSAGES'}</span>
        <h1>{heading}</h1>
        <p>Messages are linked to a confirmed counselling session and are only visible to its participants.</p>
      </div>
      <button type="button" className="care-chat-refresh" onClick={() => { void loadConversations(true); if (selectedId) void loadMessages(selectedId, true) }} aria-label="Refresh conversations">
        <RefreshCw size={16} /> Refresh
      </button>
    </div>

    <ChatNotice>{error}</ChatNotice>
    <div className="care-chat-layout">
      <aside className="care-chat-sidebar" aria-label="Conversations">
        <div className="care-chat-sidebar-title"><strong>Conversations</strong><span>{conversations.length}</span></div>
        {loading ? <div className="care-chat-empty-small"><LoaderCircle className="journey-spin" size={18} /> Loading…</div>
          : conversations.length ? conversations.map(item => (
            <button
              className={`care-chat-conversation${selectedId === item.conversationId ? ' is-active' : ''}`}
              key={item.conversationId}
              type="button"
              onClick={() => {
                activeConversationRef.current = item.conversationId
                setMessages([])
                setMessageError('')
                setSelectedId(item.conversationId)
              }}
            >
              <span className="care-chat-avatar"><MessageCircle size={17} /></span>
              <span className="care-chat-conversation-copy">
                <strong>{item.participantName || 'CareConnect participant'}</strong>
                <small>{formatConversationDate(item.scheduledStart)}</small>
                <span>{item.lastMessage || 'Your conversation is ready.'}</span>
              </span>
              <i className={item.canSend ? 'is-open' : ''} aria-label={item.canSend ? 'Messaging open' : 'Read only'} />
            </button>
          )) : <div className="care-chat-empty-small"><MessageCircle size={22} /><p>{emptyCopy}</p></div>}
      </aside>

      <div className="care-chat-panel">
        {selected ? <>
          <header className="care-chat-panel-header">
            <span className="care-chat-avatar"><MessageCircle size={18} /></span>
            <div><strong>{selected.participantName}</strong><small>{formatConversationDate(selected.scheduledStart)} · {String(selected.sessionStatus || '').replaceAll('_', ' ')}</small></div>
            <span className={`care-chat-state${selected.canSend ? ' is-open' : ''}`}>{selected.canSend ? 'Open' : 'Read only'}</span>
          </header>
          <div className="care-chat-messages" aria-live="polite">
            {messagesLoading && !messages.length ? <div className="care-chat-empty-small"><LoaderCircle className="journey-spin" size={18} /> Loading messages…</div>
              : messages.length ? messages.map(message => (
                <article className={`care-chat-message${message.mine ? ' is-mine' : ''}`} key={message.messageId}>
                  <div><p>{message.content}</p><time>{formatMessageTime(message.createdAt)}</time></div>
                </article>
              )) : <div className="care-chat-first-message"><MessageCircle size={27} /><strong>This is your private conversation</strong><p>You can use this space to coordinate your confirmed counselling session. If you need urgent help, do not wait for a chat reply.</p></div>}
            <div ref={bottomRef} />
          </div>
          <ChatNotice>{messageError}</ChatNotice>
          {selected.canSend ? <form className="care-chat-composer" onSubmit={sendMessage}>
            <label className="care-chat-sr-only" htmlFor="care-chat-message">Write a message</label>
            <textarea id="care-chat-message" rows="2" maxLength="4000" value={draft} onChange={event => setDraft(event.target.value)} placeholder="Write a message…" onKeyDown={event => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit() } }} />
            <button type="submit" disabled={sending || !draft.trim()} aria-label="Send message">{sending ? <LoaderCircle className="journey-spin" size={18} /> : <Send size={18} />}</button>
          </form> : <div className="care-chat-closed"><ShieldCheck size={17} /> This conversation is read-only because the appointment is no longer active.</div>}
        </> : <div className="care-chat-no-selection"><MessageCircle size={34} /><h2>No conversation selected</h2><p>{emptyCopy}</p></div>}
      </div>
    </div>
    <p className="care-chat-safety"><ShieldCheck size={15} /> CareConnect chat is not monitored continuously and is not an emergency service. For immediate danger, contact local emergency services or a trusted university support contact.</p>
  </section>
}
