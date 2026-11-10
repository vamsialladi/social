import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  ChevronDown,
  ChevronLeft,
  MoreHorizontal,
  Paperclip,
  Search,
  Send,
  Smile,
  SquarePen,
  Star,
  X,
  Wifi,
  WifiOff,
} from 'lucide-react'
import './Messages.css'
import api from '../../api'
import { websocketService } from '../../services/websocket'

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
      { id: 'akshay-4', content: 'Great! I am looking forward to learning more about the role.', direction: 'outgoing' },
      { id: 'akshay-5', content: 'The Chapter Support Associate role involves supporting our local chapters and helping them grow.', direction: 'incoming' },
      { id: 'akshay-6', content: 'That sounds interesting. What are the key responsibilities?', direction: 'outgoing' },
      { id: 'akshay-7', content: 'The key responsibilities include coordinating events, managing communications, and assisting with chapter development.', direction: 'incoming' },
      { id: 'akshay-8', content: 'Thank you for the information. I am excited about this opportunity.', direction: 'outgoing' },
      { id: 'akshay-9', content: 'We are excited to have you on board! We will be in touch with the next steps.', direction: 'incoming' },
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
  {
    id: 5,
    name: 'Aditya Singh',
    initials: 'AS',
    color: 'orange',
    preview: 'Your insights on the recent project were invaluable.',
    date: 'Sep 29',
    category: 'Starred',
    unread: false,
    sponsored: false,
    subject: 'Appreciation for your insights',
    description: 'Your insights on the recent project were invaluable.',
    thread: [{ id: 5, content: 'Your insights on the recent project were invaluable.', direction: 'incoming' }],
  },
  {
    id: 6,
    name: 'Ananya Verma',
    initials: 'AV',
    color: 'teal',
    preview: 'Looking forward to our meeting next week.',
    date: 'Sep 25',
    category: 'Unread',
    unread: true,
    sponsored: false,
    subject: 'Meeting next week',
    description: 'Looking forward to our meeting next week.',
    thread: [{ id: 6, content: 'Looking forward to our meeting next week.', direction: 'incoming' }],
  },
]

const categories = ['Opportunities', 'Unread', 'Connections', 'InMail', 'Starred']
const mailboxOptions = ['Focused', 'Other', 'Archived', 'Spam']
const emojis = ['😀', '😂', '😍', '😊', '👍', '🎉', '❤️', '🙏']

function formatMessageTimestamp(timestamp) {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''

  return new Intl.DateTimeFormat(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date)
}

function formatMessageDate(timestamp) {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''

  return new Intl.DateTimeFormat(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date)
}

function formatMessageTime(timestamp) {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''

  return new Intl.DateTimeFormat(undefined, {
    hour: 'numeric',
    minute: '2-digit',
  }).format(date)
}

function createProfileConversation(conversation) {
  const name = conversation?.name?.trim()
  if (!name) return null

  const id = `profile-${conversation.id || name.toLowerCase()}`
  const avatar = conversation.profileImageUrl || conversation.avatar || ''
  return {
    id,
    name,
    initials: name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase(),
    color: 'purple',
    avatar,
    profile: {
      ...conversation,
      userId: conversation.userId || conversation.user_id || conversation.id || conversation._id || '',
      profileImageUrl: avatar,
    },
    preview: 'Start a conversation',
    date: 'Now',
    category: 'Connections',
    unread: false,
    sponsored: false,
    subject: `Conversation with ${name}`,
    description: '',
    thread: [],
  }
}

function isMessageOutgoing(msg, currentConversation, user) {
  if (!msg) return false

  let effectiveUser = user
  if (!effectiveUser) {
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) effectiveUser = JSON.parse(u)
    } catch {}
  }

  const myEmail = (effectiveUser?.email || '').trim().toLowerCase()
  const myId = String(effectiveUser?.userId || effectiveUser?.publicUserId || effectiveUser?.id || '').trim().toLowerCase()

  const msgSender = String(msg.senderId || '').trim().toLowerCase()
  const msgReceiver = String(msg.receiverId || '').trim().toLowerCase()

  // 1. Primary check: Unique sender ID (email, publicUserId, or user ID)
  if (msgSender) {
    if ((myEmail && msgSender === myEmail) || (myId && msgSender === myId) || msgSender === 'me') {
      return true
    }
    // Sender ID is present and does not match current user -> incoming message
    return false
  }

  // 2. Primary check: Unique receiver ID
  if (msgReceiver) {
    if ((myEmail && msgReceiver === myEmail) || (myId && msgReceiver === myId)) {
      return false
    }
    return true
  }

  // 3. Fallback to message.direction property (which backend toDto computes accurately)
  if (msg.direction === 'incoming') return false
  if (msg.direction === 'outgoing') return true

  return false
}

function Messages({ conversationToOpen, openConversationRef, onProfileSelect }) {
  const initialProfileConversation = createProfileConversation(conversationToOpen)
  const initialExistingConversation = initialProfileConversation && initialMessages.find((message) => (
    message.id === initialProfileConversation.id
    || message.name.trim().toLowerCase() === initialProfileConversation.name.toLowerCase()
  ))
  const [messages, setMessages] = useState(() => (
    initialProfileConversation && !initialExistingConversation
      ? [initialProfileConversation, ...initialMessages]
      : initialProfileConversation
        ? initialMessages.map((message) => (
          message.id === initialExistingConversation.id
            ? {
              ...message,
              avatar: initialProfileConversation.avatar || message.avatar,
              profile: initialProfileConversation.profile,
            }
            : message
        ))
        : initialMessages
  ))
  const [selectedId, setSelectedId] = useState(
    initialExistingConversation?.id || initialProfileConversation?.id || initialMessages[0].id,
  )
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
  const [expandedMessageTimeId, setExpandedMessageTimeId] = useState(null)
  const [isMobileDetailOpen, setIsMobileDetailOpen] = useState(
    () => Boolean(initialProfileConversation && window.innerWidth <= 740),
  )
  const [isDesktopChatOpen, setIsDesktopChatOpen] = useState(
    () => Boolean(initialProfileConversation && window.innerWidth > 740),
  )
  const [isWsConnected, setIsWsConnected] = useState(false)
  const replyInputRef = useRef(null)
  const attachmentInputRef = useRef(null)
  const emojiPickerRef = useRef(null)

  const currentUser = useMemo(() => {
    try {
      const u = localStorage.getItem('truehunt_user')
      return u ? JSON.parse(u) : null
    } catch {
      return null
    }
  }, [])

  const loadBackendConversations = useCallback(async (currentMailbox = 'Focused') => {
    try {
      const data = await api.getConversations(currentMailbox)
      if (Array.isArray(data) && data.length > 0) {
        setMessages((prev) => {
          const profileConversations = prev.filter((m) => String(m.id).startsWith('profile-'))
          const backendIds = new Set(data.map((d) => String(d.id)))
          const merged = [
            ...data,
            ...profileConversations.filter(
              (p) => !backendIds.has(String(p.id)) && !data.some((d) => d.name.toLowerCase() === p.name.toLowerCase())
            ),
          ]
          return merged
        })
        setSelectedId((current) => {
          const exists = data.some((d) => String(d.id) === String(current)) || String(current).startsWith('profile-')
          return exists ? current : (data[0]?.id || current)
        })
      }
    } catch (err) {
      console.warn('Could not load conversations from backend:', err)
    }
  }, [])

  const handleIncomingWebSocketMessage = useCallback((newMsg) => {
    if (!newMsg || !newMsg.content) return

    let currentUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) currentUser = JSON.parse(u)
    } catch {}

    const convId = newMsg.conversationId
    const content = newMsg.content
    const msgId = newMsg.id || `msg-${Date.now()}`

    const myEmail = (currentUser?.email || '').trim().toLowerCase()
    const myId = String(currentUser?.userId || currentUser?.publicUserId || currentUser?.id || '').trim().toLowerCase()

    const senderEmail = (newMsg.senderId || '').trim().toLowerCase()
    const receiverEmail = (newMsg.receiverId || '').trim().toLowerCase()

    // Explicit unique ID checks
    const isSenderMe = Boolean(
      (myEmail && senderEmail === myEmail) ||
      (myId && senderEmail === myId) ||
      senderEmail === 'me'
    )

    const isReceiverMe = Boolean(
      (myEmail && receiverEmail === myEmail) ||
      (myId && receiverEmail === myId)
    )

    const isOutgoing = isSenderMe ? true : isReceiverMe ? false : Boolean(newMsg.direction === 'outgoing')

    const otherName = isOutgoing ? (newMsg.receiverName || 'Cinema Professional') : (newMsg.senderName || 'Cinema Professional')
    const otherId = isOutgoing ? (newMsg.receiverId || '') : (newMsg.senderId || '')

    const threadItem = {
      id: msgId,
      content,
      direction: isOutgoing ? 'outgoing' : 'incoming',
      sentAt: newMsg.sentAt || new Date().toISOString(),
      createdAt: newMsg.createdAt || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      attachment: newMsg.attachment,
      senderId: newMsg.senderId,
      senderName: newMsg.senderName,
      receiverId: newMsg.receiverId,
      receiverName: newMsg.receiverName,
    }

    setMessages((prevMessages) => {
      const otherNameClean = otherName.trim().toLowerCase()
      const otherIdClean = String(otherId).trim().toLowerCase()

      // Find matching conversation across ID, name, or participant user ID
      const matchedIndex = prevMessages.findIndex((m) => {
        if (convId && (String(m.id) === String(convId) || m.id === convId)) return true
        if (otherNameClean && m.name && m.name.trim().toLowerCase() === otherNameClean) return true
        const u1 = String(m.user1Id || '').trim().toLowerCase()
        const u2 = String(m.user2Id || '').trim().toLowerCase()
        if (otherIdClean && (u1 === otherIdClean || u2 === otherIdClean)) return true
        if (m.profile) {
          const pId = String(m.profile.id || '').trim().toLowerCase()
          const pUserId = String(m.profile.userId || '').trim().toLowerCase()
          const pEmail = String(m.profile.email || '').trim().toLowerCase()
          if (otherIdClean && (pId === otherIdClean || pUserId === otherIdClean || pEmail === otherIdClean)) return true
        }
        return false
      })

      if (matchedIndex !== -1) {
        return prevMessages.map((m, idx) => {
          if (idx === matchedIndex) {
            const thread = m.thread || []

            // 1. Exact ID check
            if (thread.some((t) => String(t.id) === String(msgId))) {
              return m
            }

            // 2. Reconciliation for optimistic outgoing message:
            // If this is an outgoing message (sent by ME), find if an optimistic pending item exists
            if (isOutgoing) {
              const pendingIdx = thread.findIndex((t) =>
                (t.direction === 'outgoing' || isMessageOutgoing(t, m, currentUser)) &&
                t.content === content &&
                (String(t.id).startsWith('msg-') || !t.sentAt || (Math.abs(new Date(t.sentAt || 0).getTime() - new Date(newMsg.sentAt || Date.now()).getTime()) < 60000))
              )
              if (pendingIdx !== -1) {
                // Reconcile optimistic item with server confirmation (avoids duplicate bubble)
                const updatedThread = [...thread]
                updatedThread[pendingIdx] = {
                  ...updatedThread[pendingIdx],
                  ...threadItem,
                  id: msgId,
                }
                return {
                  ...m,
                  id: convId || m.id,
                  thread: updatedThread,
                  preview: content,
                  date: 'Just now',
                }
              }
            }

            return {
              ...m,
              id: convId || m.id,
              thread: [...thread, threadItem],
              preview: content,
              date: 'Just now',
              unread: !isOutgoing && String(selectedId) !== String(m.id) && String(selectedId) !== String(convId),
            }
          }
          return m
        })
      } else {
        const newConv = {
          id: convId || `conv-${Date.now()}`,
          name: otherName,
          initials: otherName.split(/\s+/).map((p) => p[0]).join('').slice(0, 2).toUpperCase() || 'CP',
          color: 'purple',
          preview: content,
          date: 'Just now',
          category: 'Connections',
          mailbox: 'Focused',
          unread: !isOutgoing,
          user1Id: newMsg.senderId,
          user1Name: newMsg.senderName,
          user2Id: newMsg.receiverId,
          user2Name: newMsg.receiverName,
          thread: [threadItem],
        }
        return [newConv, ...prevMessages]
      }
    })
  }, [selectedId])

  useEffect(() => {
    let storedUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) storedUser = JSON.parse(u)
    } catch {}

    const token = localStorage.getItem('truehunt_token') || ''
    const email = storedUser?.email || 'pro@truehunt.in'

    websocketService.connect(token, email)
    const unsubStatus = websocketService.onStatusChange(setIsWsConnected)
    const unsubMsg = websocketService.onMessage(handleIncomingWebSocketMessage)

    loadBackendConversations(mailbox)

    const handleMsgEvent = (event) => {
      if (event.detail) handleIncomingWebSocketMessage(event.detail)
    }
    window.addEventListener('truehunt-message-received', handleMsgEvent)

    const handleFocus = () => {
      if (!document.hidden) loadBackendConversations(mailbox)
    }
    window.addEventListener('focus', handleFocus)

    return () => {
      window.removeEventListener('focus', handleFocus)
      window.removeEventListener('truehunt-message-received', handleMsgEvent)
      unsubStatus()
      unsubMsg()
    }
  }, [mailbox, loadBackendConversations, handleIncomingWebSocketMessage])

  useEffect(() => {
    if (!selectedId) return undefined
    const unsubTopic = websocketService.subscribe(`/topic/conversations/${selectedId}`, handleIncomingWebSocketMessage)
    return () => {
      unsubTopic?.()
    }
  }, [selectedId, handleIncomingWebSocketMessage])

  const openConversation = useCallback((conversation) => {
    const profileConversation = createProfileConversation(conversation)
    if (!profileConversation) return

    const existingConversation = messages.find((message) => (
      message.id === profileConversation.id
      || message.name.trim().toLowerCase() === profileConversation.name.toLowerCase()
    ))

    setMessages((currentMessages) => {
      const currentMatch = currentMessages.find((message) => (
        message.id === profileConversation.id
        || message.name.trim().toLowerCase() === profileConversation.name.toLowerCase()
      ))
      if (!currentMatch) return [profileConversation, ...currentMessages]
      return currentMessages.map((message) => (
        message === currentMatch
          ? {
            ...message,
            avatar: profileConversation.avatar || message.avatar,
            profile: profileConversation.profile,
          }
          : message
      ))
    })

    setSelectedId(existingConversation?.id || profileConversation.id)
    setActiveCategory('')
    setSearch('')
    setIsMobileDetailOpen(window.innerWidth <= 740)
    setIsDesktopChatOpen(window.innerWidth > 740)
  }, [messages])

  useEffect(() => {
    if (!openConversationRef) return undefined

    openConversationRef.current = openConversation
    return () => {
      openConversationRef.current = null
    }
  }, [openConversationRef, openConversation])

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

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth > 740) {
        setIsMobileDetailOpen(false)
      } else {
        setIsDesktopChatOpen(false)
      }
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  function openMessageProfile(message) {
    onProfileSelect?.(message.profile || {
      name: message.name,
      profileImageUrl: message.avatar || '',
    })
  }

  function handleSelectMessage(id) {
    if (window.innerWidth <= 740) {
      setSelectedId(id)
      setIsMobileDetailOpen(true)
      return
    }

    setSelectedId(id)
    setIsDesktopChatOpen(true)
  }

  function toggleStar(id) {
    setMessages((currentMessages) => currentMessages.map((message) => (
      message.id === id ? { ...message, starred: !message.starred } : message
    )))
  }

  async function sendReply(event) {
    event.preventDefault()
    const content = replyText.trim()

    if (!selectedMessage || (!content && !replyAttachment)) return

    let currentUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) currentUser = JSON.parse(u)
    } catch {}

    const myIdentity = currentUser?.email || currentUser?.userId || 'me'
    const myEmail = (currentUser?.email || '').trim().toLowerCase()
    const myId = String(currentUser?.userId || currentUser?.publicUserId || currentUser?.id || '').trim().toLowerCase()
    const isMe = (val) => {
      if (!val) return false
      const v = String(val).trim().toLowerCase()
      return v === myEmail || v === myId
    }

    let targetReceiverId = ''
    if (selectedMessage.profile?.email && !isMe(selectedMessage.profile.email)) {
      targetReceiverId = selectedMessage.profile.email
    } else if (selectedMessage.profile?.userId && !isMe(selectedMessage.profile.userId)) {
      targetReceiverId = selectedMessage.profile.userId
    } else if (selectedMessage.user1Id && !isMe(selectedMessage.user1Id)) {
      targetReceiverId = selectedMessage.user1Id
    } else if (selectedMessage.user2Id && !isMe(selectedMessage.user2Id)) {
      targetReceiverId = selectedMessage.user2Id
    } else if (selectedMessage.profile?.id && !isMe(selectedMessage.profile.id)) {
      targetReceiverId = selectedMessage.profile.id
    } else if (selectedMessage.name && !isMe(selectedMessage.name)) {
      targetReceiverId = selectedMessage.name
    } else {
      targetReceiverId = selectedMessage.user2Id || selectedMessage.user1Id || selectedMessage.name
    }

    const messageId = `msg-${Date.now()}`
    const attachment = replyAttachment

    const outgoingItem = {
      id: messageId,
      content,
      attachment,
      direction: 'outgoing',
      sentAt: new Date().toISOString(),
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      senderId: myIdentity,
      senderName: currentUser?.name || 'You',
      receiverId: targetReceiverId,
      receiverName: selectedMessage.name,
    }

    // 1. Optimistic update
    setMessages((currentMessages) => currentMessages.map((message) => (
      message.id === selectedMessage.id
        ? {
          ...message,
          thread: [
            ...(message.thread ?? []),
            ...(!message.thread?.length && !message.sponsored && message.description
              ? [{ id: `${message.id}-initial`, content: message.description, direction: 'incoming' }]
              : []),
            outgoingItem,
          ],
          preview: content || (attachment ? `Attachment: ${attachment.name}` : ''),
          date: 'Now',
          unread: false,
        }
        : message
    )))
    setReplyText('')
    setReplyAttachment(null)
    setIsEmojiPickerOpen(false)

    // 2. Real-time WebSocket transmission
    const isTempId = String(selectedMessage.id).startsWith('profile-') || String(selectedMessage.id).startsWith('conv-')
    const numericConvId = !isTempId && !Number.isNaN(Number(selectedMessage.id)) ? Number(selectedMessage.id) : null

    const wsPayload = {
      id: messageId,
      conversationId: numericConvId,
      senderId: myIdentity,
      senderName: currentUser?.name || 'You',
      receiverId: targetReceiverId,
      receiverName: selectedMessage.name,
      content,
      attachment,
      direction: 'outgoing',
      type: 'CHAT',
    }

    const sentOverWs = websocketService.sendMessage('/app/chat.send', wsPayload)

    // 3. Fallback to REST persistence only if WebSocket is disconnected
    if (!sentOverWs) {
      try {
        if (numericConvId) {
          await api.sendMessage(numericConvId, content, attachment)
        } else {
          const created = await api.createConversation(targetReceiverId, content)
          if (created?.id) {
            setSelectedId(created.id)
            setMessages((prev) => prev.map((m) => m.id === selectedMessage.id ? { ...m, id: created.id } : m))
          }
        }
      } catch (err) {
        console.warn('REST message persist fallback note:', err)
      }
    }
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

    let currentUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) currentUser = JSON.parse(u)
    } catch {}

    const myIdentity = currentUser?.email || currentUser?.userId || 'me'
    const messageId = `msg-${Date.now()}`

    try {
      const created = await api.createConversation(name, content)
      const convId = created?.id || Date.now()

      const newMessage = {
        ...created,
        id: convId,
        name,
        initials: name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase(),
        color: 'purple',
        preview: content,
        date: 'Now',
        category: 'Direct',
        mailbox: 'Focused',
        unread: false,
        thread: [{ id: messageId, content, direction: 'outgoing', sentAt: new Date().toISOString() }],
      }

      setMessages((currentMessages) => [newMessage, ...currentMessages.filter((m) => m.id !== convId)])
      setSelectedId(convId)

      websocketService.sendMessage('/app/chat.send', {
        conversationId: typeof convId === 'number' ? convId : null,
        senderId: myIdentity,
        senderName: currentUser?.name || 'You',
        receiverId: name,
        receiverName: name,
        content,
        type: 'CHAT',
      })
    } catch (err) {
      console.error('Failed to create conversation:', err)
    }

    setActiveCategory('')
    setRecipient('')
    setMessageText('')
    setIsComposeOpen(false)
  }

  return (
    <main className="messages-page" id="messages" aria-label="Messages">
      <div className="messages-layout">
        <div className="messages-main-column">
          <section className="messages-shell">
        <header className="messages-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h1>Messaging</h1>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
                fontSize: '11px',
                fontWeight: 600,
                color: isWsConnected ? '#10B981' : '#9CA3AF',
                padding: '2px 8px',
                borderRadius: '999px',
                backgroundColor: isWsConnected ? 'rgba(16, 185, 129, 0.1)' : 'rgba(156, 163, 175, 0.1)',
              }}
              title={isWsConnected ? 'Real-time WebSocket Live' : 'Connecting to WebSocket...'}
            >
              <span
                style={{
                  width: 6,
                  height: 6,
                  borderRadius: '50%',
                  backgroundColor: isWsConnected ? '#10B981' : '#9CA3AF',
                }}
              />
              {isWsConnected ? 'Live' : 'Connecting'}
            </span>
          </div>
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
          <div className="messages-header-actions">
            <button className="messages-icon-button" type="button" aria-label="More message options">
              <MoreHorizontal size={27} strokeWidth={2.8} />
            </button>
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

        <nav className={`messages-filters${isMobileDetailOpen ? ' is-detail-open' : ''}`} aria-label="Message filters">
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

        <div className={`messages-content${isMobileDetailOpen ? ' is-detail-open' : ''}${isDesktopChatOpen ? ' is-chat-open' : ''}`}>
          <aside className="messages-list" aria-label="Conversation list">
            {visibleMessages.length ? visibleMessages.map((message) => (
              <div
                className={`message-preview${selectedId === message.id ? ' is-active' : ''}${message.unread ? ' is-unread' : ''}`}
                key={message.id}
              >
                <button
                  className={`message-avatar message-profile-button avatar-${message.color}`}
                  type="button"
                  aria-label={`View ${message.name}'s profile`}
                  onClick={() => openMessageProfile(message)}
                >
                  <span aria-hidden="true">{message.initials}</span>
                  {message.avatar && (
                    <img
                      src={message.avatar}
                      alt=""
                      onError={(event) => {
                        event.currentTarget.hidden = true
                      }}
                    />
                  )}
                </button>
                <button
                  className="message-preview-copy"
                  type="button"
                  onClick={() => handleSelectMessage(message.id)}
                >
                  <span className="message-preview-heading">
                    <strong>{message.name}</strong>
                    <time>{message.date}</time>
                  </span>
                  <span className="message-preview-text">{message.preview}</span>
                </button>
              </div>
            )) : (
              <p className="messages-empty">No messages match your filters.</p>
            )}
          </aside>

          <section className="message-detail" aria-label="Selected conversation">
            {selectedMessage ? (
              <>
                <header className="message-detail-header">
                  <button
                    type="button"
                    className="message-back-button"
                    aria-label="Back to message list"
                    onClick={() => {
                      setIsMobileDetailOpen(false)
                    }}
                  >
                    <ChevronLeft size={18} strokeWidth={2.5} />
                    <span>Messages</span>
                  </button>
                  <div className="message-contact">
                    <button
                      className={`message-avatar message-profile-button avatar-${selectedMessage.color}`}
                      type="button"
                      aria-label={`View ${selectedMessage.name}'s profile`}
                      onClick={() => openMessageProfile(selectedMessage)}
                    >
                      <span aria-hidden="true">{selectedMessage.initials}</span>
                      {selectedMessage.avatar && (
                        <img
                          src={selectedMessage.avatar}
                          alt=""
                          onError={(event) => {
                            event.currentTarget.hidden = true
                          }}
                        />
                      )}
                    </button>
                    <div>
                      <h2>{selectedMessage.name}</h2>
                      <p>{selectedMessage.sponsored
                        ? 'Passionate about solving business challenges, driving growth, and creating impact.'
                        : selectedMessage.description}</p>
                    </div>
                  </div>
                  <div className="message-detail-actions">
                    <button className="messages-icon-button" type="button" aria-label="More conversation options">
                      <MoreHorizontal size={26} strokeWidth={2.8} />
                    </button>
                    <button
                      className={`messages-icon-button star-button${selectedMessage.starred ? ' is-starred' : ''}`}
                      type="button"
                      aria-label={selectedMessage.starred ? 'Remove star' : 'Star conversation'}
                      aria-pressed={Boolean(selectedMessage.starred)}
                      onClick={() => toggleStar(selectedMessage.id)}
                    >
                      <Star size={29} strokeWidth={1.8} fill={selectedMessage.starred ? 'currentColor' : 'none'} />
                    </button>
                    <button
                      className="messages-icon-button message-close-button"
                      type="button"
                      aria-label="Close chat"
                      onClick={() => setIsDesktopChatOpen(false)}
                    >
                      <X size={23} strokeWidth={2.2} />
                    </button>
                  </div>
                </header>

                {(!selectedMessage.sponsored || selectedMessage.thread?.length > 0) && (
                  <article className="message-conversation">
                    {!(selectedMessage.thread || []).some((message) => message.sentAt) && (
                      <p className="conversation-date">{selectedMessage.date}</p>
                    )}
                    {(selectedMessage.thread?.length
                      ? selectedMessage.thread
                      : selectedMessage.description
                        ? [{ id: `${selectedMessage.id}-initial`, content: selectedMessage.description, direction: 'incoming' }]
                        : []
                    ).map((message, index, thread) => {
                      const sentDate = message.sentAt ? new Date(message.sentAt) : null
                      const dateKey = sentDate && !Number.isNaN(sentDate.getTime())
                        ? `${sentDate.getFullYear()}-${sentDate.getMonth()}-${sentDate.getDate()}`
                        : ''
                      const previousSentDate = index > 0 && thread[index - 1].sentAt
                        ? new Date(thread[index - 1].sentAt)
                        : null
                      const previousDateKey = previousSentDate && !Number.isNaN(previousSentDate.getTime())
                        ? `${previousSentDate.getFullYear()}-${previousSentDate.getMonth()}-${previousSentDate.getDate()}`
                        : ''
                      const showDate = Boolean(dateKey && dateKey !== previousDateKey)
                      const isOutgoing = isMessageOutgoing(message, selectedMessage, currentUser)
                      const bubbleClassName = `conversation-bubble ${isOutgoing ? 'is-outgoing' : 'is-incoming'}`
                      const timeString = message.createdAt || (message.sentAt ? formatMessageTime(message.sentAt) : '')

                      return (
                        <Fragment key={message.id}>
                          {showDate && (
                            <time className="conversation-timestamp" dateTime={message.sentAt}>
                              {formatMessageDate(message.sentAt)}
                            </time>
                          )}
                          <div className={bubbleClassName}>
                            {message.content && <span className="conversation-bubble-text">{message.content}</span>}
                            {message.attachment && (
                              <span className="conversation-attachment">
                                <Paperclip size={14} aria-hidden="true" />
                                {message.attachment.name}
                              </span>
                            )}
                            <span className="conversation-bubble-footer">
                              {timeString && <span className="conversation-bubble-time">{timeString}</span>}
                              {isOutgoing && <span className="conversation-check" aria-hidden="true">✓✓</span>}
                            </span>
                          </div>
                        </Fragment>
                      )
                    })}
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
        </div>

        <aside className="messages-promotion-column" aria-label="Promotion">
          <section className="home-promotion-card" aria-label="Promoted film career opportunity">
            <div className="home-promotion-art">
              <span className="home-promotion-label">PROMOTED</span>
              <span className="home-promotion-art-title">THE NEXT<br />BIG PICTURE</span>
              <span className="home-promotion-studio">NORTHSTAR FILMS</span>
            </div>
            <div className="home-promotion-content">
              <h2>Bring your next story to the screen</h2>
              <p>Northstar Films is meeting new writers, actors, and independent filmmakers for upcoming productions.</p>
              <span className="home-promotion-caption">Make your mark in the movies.</span>
            </div>
          </section>
          <footer className="messages-sidebar-footer" aria-label="Messages information">
            <a href="/" onClick={(event) => event.preventDefault()}>About</a>
            <a href="/" onClick={(event) => event.preventDefault()}>Help Center</a>
            <a href="/" onClick={(event) => event.preventDefault()}>Privacy &amp; Terms</a>
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
