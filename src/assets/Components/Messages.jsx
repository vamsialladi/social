import { useEffect, useMemo, useRef, useState } from 'react'
import {
  ChevronDown,
  MoreHorizontal,
  Paperclip,
  Search,
  Send,
  Smile,
  SquarePen,
  Star,
  X,
} from 'lucide-react'
import './Messages.css'
import api from '../../api'

const initialMessages = [
  {
    id: 1,
    name: 'Akshay Chamola',
    initials: 'AC',
    color: 'blue',
    preview: 'Unionpoint is adding a Chapter Support Associate...',
    date: 'Oct 19',
    category: 'Jobs',
    unread: true,
    sponsored: true,
    subject: 'Unionpoint is adding a Chapter Support Associate',
    description: 'Make an impact with a team that is building stronger communities.',
    thread: [
      { id: 'akshay-1', content: 'Hi Chandrabose, thanks for your interest in this opportunity.', direction: 'incoming' },
      { id: 'akshay-2', content: 'Thanks for reaching out. Could you share more details?', direction: 'outgoing' },
      { id: 'akshay-3', content: 'Absolutely! I would be happy to tell you more.', direction: 'incoming' },
    ],
  },
  {
    id: 2,
    name: 'Priya Sharma',
    initials: 'PS',
    color: 'rose',
    preview: 'Thanks for sharing your profile. Let’s connect soon!',
    date: 'Oct 11',
    category: 'Connections',
    unread: true,
    sponsored: false,
    subject: 'Great connecting with you',
    description: 'Thanks for sharing your profile. Let’s connect soon!',
    thread: [{ id: 2, content: 'Thanks for sharing your profile. Let’s connect soon!', direction: 'incoming' }],
  },
  {
    id: 3,
    name: 'Rohan Mehta',
    initials: 'RM',
    color: 'green',
    preview: 'Are you available for a quick chat this week?',
    date: 'Oct 08',
    category: 'InMail',
    unread: false,
    sponsored: false,
    subject: 'A quick chat this week?',
    description: 'Are you available for a quick chat this week?',
    thread: [{ id: 3, content: 'Are you available for a quick chat this week?', direction: 'incoming' }],
  },
  {
    id: 4,
    name: 'Meera Kapoor',
    initials: 'MK',
    color: 'purple',
    preview: 'I loved your recent work. Would love to collaborate.',
    date: 'Oct 03',
    category: 'Connections',
    unread: false,
    sponsored: false,
    subject: 'Let’s collaborate',
    description: 'I loved your recent work. Would love to collaborate.',
    thread: [{ id: 4, content: 'I loved your recent work. Would love to collaborate.', direction: 'incoming' }],
  },
]

const categories = ['Opportunities', 'Unread', 'Connections', 'InMail', 'Starred']
const mailboxOptions = ['Focused', 'Other', 'Archived', 'Spam']
const emojis = ['😀', '😂', '😍', '😊', '👍', '🎉', '❤️', '🙏']

function Messages() {
  const [messages, setMessages] = useState(initialMessages)
  const [selectedId, setSelectedId] = useState(initialMessages[0].id)
  const [activeCategory, setActiveCategory] = useState('')
  const [mailbox, setMailbox] = useState('Focused')
  const [isMailboxOpen, setIsMailboxOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [isComposeOpen, setIsComposeOpen] = useState(false)
  const [recipient, setRecipient] = useState('')
  const [messageText, setMessageText] = useState('')
  const [replyText, setReplyText] = useState('')
  const [replyAttachment, setReplyAttachment] = useState(null)
  const [isEmojiPickerOpen, setIsEmojiPickerOpen] = useState(false)
  const [isHeaderMenuOpen, setIsHeaderMenuOpen] = useState(false)
  const [isConvMenuOpen, setIsConvMenuOpen] = useState(false)
  const replyInputRef = useRef(null)
  const attachmentInputRef = useRef(null)
  const emojiPickerRef = useRef(null)

  useEffect(() => {
    if (!isEmojiPickerOpen) return undefined

    function closeOnOutsideClick(event) {
      if (!emojiPickerRef.current?.contains(event.target)) {
        setIsEmojiPickerOpen(false)
      }
    }

    document.addEventListener('pointerdown', closeOnOutsideClick)
    return () => document.removeEventListener('pointerdown', closeOnOutsideClick)
  }, [isEmojiPickerOpen])

  useEffect(() => {
    async function loadConversations() {
      try {
        const data = await api.getConversations(mailbox)
        if (Array.isArray(data) && data.length > 0) {
          setMessages(data)
          if (!data.some((m) => m.id === selectedId)) {
            setSelectedId(data[0].id)
          }
        }
      } catch (err) {
        console.warn('Backend conversations load error:', err)
      }
    }
    loadConversations()
  }, [mailbox])

  const visibleMessages = useMemo(() => {
    const query = search.trim().toLowerCase()

    return messages.filter((message) => {
      const matchesSearch = !query
        || `${message.name} ${message.preview} ${message.subject}`.toLowerCase().includes(query)
      const matchesCategory = !activeCategory
        || (activeCategory === 'Unread' ? message.unread : activeCategory === 'Starred'
          ? message.starred
          : message.category === activeCategory)

      return matchesSearch && matchesCategory
    })
  }, [activeCategory, messages, search])

  const selectedMessage = messages.find((message) => message.id === selectedId)

  async function toggleStar(id) {
    try {
      const updated = await api.starConversation(id)
      setMessages((currentMessages) => currentMessages.map((message) => (
        message.id === id ? { ...message, starred: updated.starred } : message
      )))
    } catch (err) {
      console.warn('Backend star note:', err)
      setMessages((currentMessages) => currentMessages.map((message) => (
        message.id === id ? { ...message, starred: !message.starred } : message
      )))
    }
  }

  async function handleSelectConversation(id) {
    setSelectedId(id)
    setIsConvMenuOpen(false)
    const target = messages.find((m) => m.id === id)
    if (target && target.unread) {
      setMessages((current) => current.map((m) => (m.id === id ? { ...m, unread: false } : m)))
      try {
        await api.markConversationAsRead(id)
      } catch (err) {
        console.warn('Backend mark read note:', err)
      }
    }
  }

  async function handleMoveMailbox(targetMailbox) {
    if (!selectedMessage) return
    setIsConvMenuOpen(false)
    try {
      await api.updateMailbox(selectedMessage.id, targetMailbox)
      setMessages((current) => current.map((m) => (m.id === selectedMessage.id ? { ...m, mailbox: targetMailbox } : m)))
      alert(`Conversation moved to ${targetMailbox}.`)
    } catch (err) {
      console.warn('Backend mailbox note:', err)
    }
  }

  async function handleDeleteSelectedConversation() {
    if (!selectedMessage) return
    if (!window.confirm(`Delete conversation with ${selectedMessage.name}?`)) return
    setIsConvMenuOpen(false)
    try {
      await api.deleteConversation(selectedMessage.id)
      const remaining = messages.filter((m) => m.id !== selectedMessage.id)
      setMessages(remaining)
      if (remaining.length > 0) setSelectedId(remaining[0].id)
    } catch (err) {
      console.warn('Backend delete conversation note:', err)
    }
  }

  async function handleMarkAllAsRead() {
    setIsHeaderMenuOpen(false)
    setMessages((current) => current.map((m) => ({ ...m, unread: false })))
    for (const m of messages) {
      if (m.unread) {
        api.markConversationAsRead(m.id).catch(() => {})
      }
    }
  }

  async function sendReply(event) {
    event.preventDefault()
    const content = replyText.trim()

    if (!selectedMessage || (!content && !replyAttachment)) return

    let attachmentPayload = null
    if (replyAttachment) {
      try {
        const res = await api.uploadMedia(replyAttachment)
        attachmentPayload = { name: res.fileName, url: res.url, kind: res.kind }
      } catch (err) {
        console.warn('Reply attachment upload error:', err)
        attachmentPayload = { name: replyAttachment.name }
      }
    }

    try {
      const updated = await api.sendMessage(selectedMessage.id, content, attachmentPayload)
      setMessages((currentMessages) => currentMessages.map((msg) => (
        msg.id === selectedMessage.id ? updated : msg
      )))
    } catch (err) {
      console.warn('Backend send reply note:', err)
      const messageId = Date.now()
      setMessages((currentMessages) => currentMessages.map((message) => (
        message.id === selectedMessage.id
          ? {
            ...message,
            thread: [
              ...(message.thread ?? []),
              { id: messageId, content, attachment: attachmentPayload, direction: 'outgoing' },
            ],
            preview: content || `Attachment: ${replyAttachment.name}`,
            date: 'Now',
            unread: false,
          }
          : message
      )))
    }

    setReplyText('')
    setReplyAttachment(null)
    setIsEmojiPickerOpen(false)
  }

  function insertEmoji(emoji) {
    const input = replyInputRef.current
    const start = input?.selectionStart ?? replyText.length
    const end = input?.selectionEnd ?? replyText.length
    const nextText = `${replyText.slice(0, start)}${emoji}${replyText.slice(end)}`

    setReplyText(nextText)
    setIsEmojiPickerOpen(false)
    requestAnimationFrame(() => {
      input?.focus()
      input?.setSelectionRange(start + emoji.length, start + emoji.length)
    })
  }

  async function sendMessage(event) {
    event.preventDefault()
    const name = recipient.trim()
    const content = messageText.trim()

    if (!name || !content) return

    try {
      const created = await api.createConversation(name, content)
      setMessages((currentMessages) => [created, ...currentMessages])
      setSelectedId(created.id)
    } catch (err) {
      console.warn('Backend create conversation note:', err)
      const messageId = Date.now()
      const newMessage = {
        id: messageId,
        name,
        initials: name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase(),
        color: 'purple',
        preview: content,
        date: 'Now',
        category: 'Connections',
        unread: false,
        sponsored: false,
        subject: 'New message',
        description: content,
        thread: [{ id: messageId, content, direction: 'outgoing' }],
      }
      setMessages((currentMessages) => [newMessage, ...currentMessages])
      setSelectedId(newMessage.id)
    }

    setActiveCategory('')
    setRecipient('')
    setMessageText('')
    setIsComposeOpen(false)
  }

  return (
    <main className="messages-page" id="messages" aria-label="Messages">
      <div className="messages-layout">
        <section className="messages-shell">
        <header className="messages-header">
          <h1>Messaging</h1>
          <label className="messages-search">
            <Search size={20} strokeWidth={2.8} aria-hidden="true" />
            <input
              type="search"
              placeholder="Search messages"
              aria-label="Search messages"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
          <div className="messages-header-actions" style={{ position: 'relative' }}>
            <button
              className="messages-icon-button"
              type="button"
              aria-label="More message options"
              onClick={() => setIsHeaderMenuOpen((open) => !open)}
            >
              <MoreHorizontal size={27} strokeWidth={2.8} />
            </button>
            {isHeaderMenuOpen && (
              <div
                style={{
                  position: 'absolute',
                  top: '100%',
                  right: 0,
                  background: 'var(--color-surface, #fff)',
                  border: '1px solid #ccc',
                  borderRadius: '8px',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                  zIndex: 20,
                  minWidth: '160px',
                  padding: '6px 0',
                }}
              >
                <button
                  type="button"
                  style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px' }}
                  onClick={handleMarkAllAsRead}
                >
                  ✓ Mark all as read
                </button>
                <button
                  type="button"
                  style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px' }}
                  onClick={() => {
                    setActiveCategory('')
                    setSearch('')
                    setIsHeaderMenuOpen(false)
                  }}
                >
                  ↺ Reset filters
                </button>
              </div>
            )}
            <button
              className="messages-icon-button"
              type="button"
              aria-label="Compose a message"
              onClick={() => setIsComposeOpen(true)}
            >
              <SquarePen size={27} strokeWidth={2.5} />
            </button>
          </div>
        </header>

        <nav className="messages-filters" aria-label="Message filters">
          <div className="mailbox-filter">
            <button
              className="mailbox-trigger"
              type="button"
              aria-expanded={isMailboxOpen}
              onClick={() => setIsMailboxOpen((isOpen) => !isOpen)}
            >
              {mailbox}<ChevronDown size={18} fill="currentColor" />
            </button>
            {isMailboxOpen && (
              <div className="mailbox-menu" role="menu">
                {mailboxOptions.map((option) => (
                  <button
                    key={option}
                    type="button"
                    role="menuitem"
                    onClick={() => {
                      setMailbox(option)
                      setIsMailboxOpen(false)
                    }}
                  >
                    {option}
                  </button>
                ))}
              </div>
            )}
          </div>
          <span className="messages-filter-divider" aria-hidden="true" />
          {categories.map((category) => (
            <button
              className={`messages-filter-chip${activeCategory === category ? ' is-selected' : ''}`}
              type="button"
              key={category}
              aria-pressed={activeCategory === category}
              onClick={() => setActiveCategory((current) => current === category ? '' : category)}
            >
              {category}
            </button>
          ))}
        </nav>

        <div className="messages-content">
          <aside className="messages-list" aria-label="Conversation list">
            {visibleMessages.length ? visibleMessages.map((message) => (
              <button
                className={`message-preview${selectedId === message.id ? ' is-active' : ''}${message.unread ? ' is-unread' : ''}`}
                type="button"
                key={message.id}
                onClick={() => handleSelectConversation(message.id)}
              >
                <span className={`message-avatar avatar-${message.color}`} aria-hidden="true">
                  {message.initials}
                </span>
                <span className="message-preview-copy">
                  <span className="message-preview-heading">
                    <strong>{message.name}</strong>
                    <time>{message.date}</time>
                  </span>
                  <span className="message-preview-text">{message.preview}</span>
                </span>
              </button>
            )) : (
              <p className="messages-empty">No messages match your filters.</p>
            )}
          </aside>

          <section className="message-detail" aria-label="Selected conversation">
            {selectedMessage ? (
              <>
                <header className="message-detail-header">
                  <div className="message-contact">
                    <span className={`message-avatar avatar-${selectedMessage.color}`} aria-hidden="true">
                      {selectedMessage.initials}
                    </span>
                    <div>
                      <h2>{selectedMessage.name}</h2>
                      <p>{selectedMessage.sponsored
                        ? 'Passionate about solving business challenges, driving growth, and creating impact.'
                        : selectedMessage.description}</p>
                    </div>
                  </div>
                  <div className="message-detail-actions" style={{ position: 'relative' }}>
                    <button
                      className="messages-icon-button"
                      type="button"
                      aria-label="More conversation options"
                      onClick={() => setIsConvMenuOpen((open) => !open)}
                    >
                      <MoreHorizontal size={26} strokeWidth={2.8} />
                    </button>
                    {isConvMenuOpen && (
                      <div
                        style={{
                          position: 'absolute',
                          top: '100%',
                          right: '35px',
                          background: 'var(--color-surface, #fff)',
                          border: '1px solid #ccc',
                          borderRadius: '8px',
                          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                          zIndex: 20,
                          minWidth: '180px',
                          padding: '6px 0',
                        }}
                      >
                        <button
                          type="button"
                          style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px' }}
                          onClick={() => handleMoveMailbox('Archived')}
                        >
                          📦 Move to Archived
                        </button>
                        <button
                          type="button"
                          style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px' }}
                          onClick={() => handleMoveMailbox('Spam')}
                        >
                          ⚠️ Move to Spam
                        </button>
                        <button
                          type="button"
                          style={{ display: 'block', width: '100%', padding: '8px 16px', textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px', color: '#d93025' }}
                          onClick={handleDeleteSelectedConversation}
                        >
                          🗑️ Delete conversation
                        </button>
                      </div>
                    )}
                    <button
                      className={`messages-icon-button star-button${selectedMessage.starred ? ' is-starred' : ''}`}
                      type="button"
                      aria-label={selectedMessage.starred ? 'Remove star' : 'Star conversation'}
                      aria-pressed={Boolean(selectedMessage.starred)}
                      onClick={() => toggleStar(selectedMessage.id)}
                    >
                      <Star size={29} strokeWidth={1.8} fill={selectedMessage.starred ? 'currentColor' : 'none'} />
                    </button>
                  </div>
                </header>

                {selectedMessage.sponsored && (
                  <article className="message-sponsored">
                    <div className="sponsored-meta">
                      <span>Sponsored</span>
                      <time>{selectedMessage.date}</time>
                    </div>
                    <div className="sponsored-content">
                      <h3>{selectedMessage.subject}</h3>
                      <p>{selectedMessage.description}</p>
                      <a href="#opportunities">View role</a>
                    </div>
                  </article>
                )}

                {(!selectedMessage.sponsored || selectedMessage.thread?.length > 0) && (
                  <article className="message-conversation">
                    <p className="conversation-date">{selectedMessage.date}</p>
                    {(selectedMessage.thread?.length
                      ? selectedMessage.thread
                      : [{ id: `${selectedMessage.id}-initial`, content: selectedMessage.description, direction: 'incoming' }]
                    ).map((message) => (
                      <div
                        className={`conversation-bubble${message.direction === 'outgoing' ? ' is-outgoing' : ''}`}
                        key={message.id}
                      >
                        {message.content && <span>{message.content}</span>}
                        {message.attachment && (
                          <span className="conversation-attachment">
                            <Paperclip size={14} aria-hidden="true" />
                            {message.attachment.name}
                          </span>
                        )}
                      </div>
                    ))}
                  </article>
                )}

                <form className="message-reply" onSubmit={sendReply}>
                  <label className="message-reply-input">
                    <span className="visually-hidden">Write a message</span>
                    <input
                      ref={replyInputRef}
                      placeholder="Write a message..."
                      value={replyText}
                      onChange={(event) => setReplyText(event.target.value)}
                    />
                  </label>
                  {replyAttachment && (
                    <div className="reply-attachment">
                      <Paperclip size={15} aria-hidden="true" />
                      <span>{replyAttachment.name}</span>
                      <button
                        type="button"
                        aria-label="Remove attached file"
                        onClick={() => {
                          setReplyAttachment(null)
                          if (attachmentInputRef.current) attachmentInputRef.current.value = ''
                        }}
                      >
                        <X size={15} />
                      </button>
                    </div>
                  )}
                  <div className="message-reply-actions">
                    <div>
                      <input
                        ref={attachmentInputRef}
                        className="visually-hidden"
                        type="file"
                        onChange={(event) => setReplyAttachment(event.target.files?.[0] ?? null)}
                        tabIndex={-1}
                        aria-hidden="true"
                      />
                      <button
                        type="button"
                        aria-label="Attach a file"
                        onClick={() => attachmentInputRef.current?.click()}
                      >
                        <Paperclip size={18} />
                      </button>
                      <div className="message-emoji-control" ref={emojiPickerRef}>
                        <button
                          type="button"
                          aria-label="Add emoji"
                          aria-expanded={isEmojiPickerOpen}
                          onClick={() => setIsEmojiPickerOpen((isOpen) => !isOpen)}
                        >
                          <Smile size={18} />
                        </button>
                        {isEmojiPickerOpen && (
                          <div className="message-emoji-picker" aria-label="Choose an emoji">
                            {emojis.map((emoji) => (
                              <button
                                key={emoji}
                                type="button"
                                aria-label={`Insert ${emoji}`}
                                onClick={() => insertEmoji(emoji)}
                              >
                                {emoji}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                    <button
                      className="reply-send-button"
                      type="submit"
                      aria-label="Send message"
                      disabled={!replyText.trim() && !replyAttachment}
                    >
                      <Send size={17} />
                    </button>
                  </div>
                </form>
              </>
            ) : (
              <div className="messages-no-selection">Select a conversation to read your messages.</div>
            )}
          </section>
        </div>
        </section>

        <aside className="messages-sidebar" aria-label="Discover opportunities">
          <section className="home-promotion-card" aria-label="Sponsored movie industry promotion">
            <div className="home-promotion-art">
              <span className="home-promotion-label">PROMOTED</span>
              <span className="home-promotion-art-title">THE NEXT<br />BIG PICTURE</span>
              <span className="home-promotion-studio">NORTHSTAR FILMS</span>
            </div>
            <div className="home-promotion-content">
              <h2>Bring your next story to the screen</h2>
              <p>Northstar Films is meeting new writers, actors, and independent filmmakers for upcoming productions.</p>
              <span className="home-promotion-caption">Make your mark in the movies.</span>
              <button
                className="home-promotion-button"
                type="button"
                onClick={async () => {
                  try {
                    const res = await api.clickPromotion("Messages Promo - Northstar Films")
                    alert(res.message || 'Inquiry registered with studio casting office.')
                  } catch (e) {
                    console.warn(e)
                  }
                }}
              >
                Explore film careers
              </button>
            </div>
          </section>
          <footer className="messages-sidebar-footer">
            <a href="#about">About</a>
            <a href="#help">Help Center</a>
            <a href="#privacy">Privacy &amp; Terms</a>
            <span>TrueHunt © 2026</span>
          </footer>
        </aside>
      </div>

      {isComposeOpen && (
        <div className="compose-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) setIsComposeOpen(false)
        }}>
          <form className="compose-dialog" aria-labelledby="compose-title" onSubmit={sendMessage}>
            <header>
              <h2 id="compose-title">New message</h2>
              <button type="button" className="messages-icon-button" aria-label="Close compose" onClick={() => setIsComposeOpen(false)}>
                <X size={22} />
              </button>
            </header>
            <label>
              To
              <input value={recipient} onChange={(event) => setRecipient(event.target.value)} required />
            </label>
            <label>
              Message
              <textarea value={messageText} onChange={(event) => setMessageText(event.target.value)} required rows={5} />
            </label>
            <button className="compose-send" type="submit">Send message</button>
          </form>
        </div>
      )}
    </main>
  )
}

export default Messages
