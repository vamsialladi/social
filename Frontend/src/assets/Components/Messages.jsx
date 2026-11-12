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

const categories = ['Opportunities', 'Unread', 'Connections', 'InMail', 'Starred']
const mailboxOptions = ['Focused', 'Other', 'Archived', 'Spam']
const emojis = ['😀', '😂', '😍', '😊', '👍', '🎉', '❤️', '🙏']
const genericContactNames = new Set(['truehunt member', 'cinema professional', 'member'])

function computeInitials(name = '') {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'TH'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
}

function readStoredUser() {
  try {
    const raw = localStorage.getItem('truehunt_user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function getConversationContactName(conversation, user) {
  const myIdentifiers = new Set([
    user?.email,
    user?.userId,
    user?.publicUserId,
    user?.id,
  ].filter(Boolean).map((value) => String(value).trim().toLowerCase()))
  const user1IsCurrent = myIdentifiers.has(String(conversation.user1Id || '').trim().toLowerCase())
  const user2IsCurrent = myIdentifiers.has(String(conversation.user2Id || '').trim().toLowerCase())
  const otherParticipantName = user1IsCurrent
    ? conversation.user2Name
    : user2IsCurrent ? conversation.user1Name : ''
  const candidates = [
    otherParticipantName,
    conversation.name,
    conversation.profile?.name,
    conversation.user1Name,
    conversation.user2Name,
  ]

  return candidates
    .map((candidate) => String(candidate || '').trim())
    .find((name) => name && !genericContactNames.has(name.toLowerCase())) || ''
}

function getConversationActivityTime(conversation) {
  const thread = Array.isArray(conversation.thread) ? conversation.thread : []
  const lastMessageTime = thread[thread.length - 1]?.sentAt
  const timestamp = Date.parse(conversation.updatedAt || lastMessageTime || '')
  return Number.isNaN(timestamp) ? 0 : timestamp
}

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

function getUnreadConversationCount(conversations) {
  return conversations.filter((conversation) => {
    if (!conversation.unread) return false

    const thread = Array.isArray(conversation.thread) ? conversation.thread : []
    const latestMessage = thread[thread.length - 1]
    return !latestMessage || !isMessageOutgoing(latestMessage, conversation)
  }).length
}

function Messages({
  conversationToOpen,
  openConversationRef,
  onProfileSelect,
  onUnreadCountChange,
  isActive,
  resetToInboxRequest = 0,
}) {
  const initialProfileConversation = createProfileConversation(conversationToOpen)
  const [messages, setMessages] = useState(() => (
    initialProfileConversation ? [initialProfileConversation] : []
  ))
  const [selectedId, setSelectedId] = useState(initialProfileConversation?.id || null)
  const [activeCategory, setActiveCategory] = useState('')
  const [mailbox, setMailbox] = useState('Focused')
  const [isMailboxOpen, setIsMailboxOpen] = useState(false)
  const [isConversationMenuOpen, setIsConversationMenuOpen] = useState(false)
  const [editingMessageId, setEditingMessageId] = useState('')
  const [editMessageDraft, setEditMessageDraft] = useState('')
  const [isUpdatingMessage, setIsUpdatingMessage] = useState(false)
  const [isClearingChat, setIsClearingChat] = useState(false)
  const [conversationActionError, setConversationActionError] = useState('')
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
  const conversationMenuRef = useRef(null)
  const previousInboxRequest = useRef(resetToInboxRequest)

  const currentUser = useMemo(() => readStoredUser(), [])

  const loadBackendConversations = useCallback(async (currentMailbox = 'Focused') => {
    try {
      const latestUser = readStoredUser() || currentUser
      const data = await api.getConversations(currentMailbox)
      if (Array.isArray(data)) {
        const namedConversations = data.map((conversation) => {
          const resolvedName = getConversationContactName(conversation, latestUser) || 'Profile'
          return {
            ...conversation,
            name: resolvedName,
            initials: computeInitials(resolvedName),
          }
        })
        setMessages((prev) => {
          const profileConversations = prev.filter((m) => String(m.id).startsWith('profile-'))
          const backendIds = new Set(namedConversations.map((d) => String(d.id)))
          const merged = [
            ...namedConversations,
            ...profileConversations.filter(
              (p) => !backendIds.has(String(p.id))
                && !namedConversations.some((d) => d.name.toLowerCase() === p.name.toLowerCase())
            ),
          ]
          return merged
        })
        setSelectedId((current) => {
          const matchingConversation = namedConversations.find((conversation) => (
            String(conversation.id) === String(current)
          ))
          if (matchingConversation) return matchingConversation.id
          if (current == null) return null
          if (String(current).startsWith('profile-')) return current
          return namedConversations[0]?.id || null
        })
      }

      const allConversations = currentMailbox ? await api.getConversations() : data
      if (Array.isArray(allConversations)) {
        onUnreadCountChange?.(getUnreadConversationCount(allConversations))
      }
    } catch (err) {
      console.warn('Could not load conversations from backend:', err)
    }
  }, [currentUser, onUnreadCountChange])

  useEffect(() => {
    if (previousInboxRequest.current === resetToInboxRequest) return

    previousInboxRequest.current = resetToInboxRequest
    setSelectedId(null)
    setIsMobileDetailOpen(false)
    setIsDesktopChatOpen(false)
    setActiveCategory('')
    setSearch('')
    setIsComposeOpen(false)
  }, [resetToInboxRequest])

  const handleIncomingWebSocketMessage = useCallback((newMsg) => {
    if (!newMsg) return

    const convId = newMsg.conversationId
    if (newMsg.type === 'CONVERSATION_CLEARED') {
      setMessages((prevMessages) => prevMessages.map((conversation) => (
        String(conversation.id) === String(convId)
          ? {
            ...conversation,
            thread: [],
            preview: 'Start a conversation',
            date: 'Now',
            unread: false,
          }
          : conversation
      )))
      return
    }
    if (newMsg.type === 'MESSAGE_EDITED') {
      const messageId = String(newMsg.id || '')
      setMessages((prevMessages) => prevMessages.map((conversation) => {
        if (String(conversation.id) !== String(convId)) return conversation
        const thread = (conversation.thread || []).map((message) => (
          String(message.id) === messageId
            ? { ...message, content: newMsg.content, edited: Boolean(newMsg.edited) }
            : message
        ))
        return {
          ...conversation,
          thread,
          preview: String(thread[thread.length - 1]?.id) === messageId
            ? newMsg.content
            : conversation.preview,
        }
      }))
      return
    }
    if (!newMsg.content) return

    let currentUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) currentUser = JSON.parse(u)
    } catch {}

    const content = newMsg.content
    const msgId = newMsg.id || `msg-${Date.now()}`
    const activityAt = newMsg.sentAt || new Date().toISOString()

    const myEmail = (currentUser?.email || '').trim().toLowerCase()
    const myId = String(currentUser?.userId || currentUser?.publicUserId || currentUser?.id || '').trim().toLowerCase()

    const senderEmail = (newMsg.senderId || '').trim().toLowerCase()
    const receiverEmail = (newMsg.receiverId || '').trim().toLowerCase()
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
    const isSelectedConversationVisible = isActive
      && String(selectedId) === String(convId)
      && (window.innerWidth <= 740 ? isMobileDetailOpen : isDesktopChatOpen)

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
                  updatedAt: activityAt,
                }
              }
            }

            const hasFreshPartnerName = Boolean(
              otherName && !genericContactNames.has(otherName.trim().toLowerCase())
            )
            const nextName = hasFreshPartnerName ? otherName.trim() : m.name
            const nextInitials = hasFreshPartnerName ? computeInitials(nextName) : m.initials
            const updatedPartnerFields = hasFreshPartnerName && !isOutgoing
              ? {
                ...(m.user1Id && senderEmail && String(m.user1Id).trim().toLowerCase() === senderEmail ? { user1Name: nextName } : {}),
                ...(m.user2Id && senderEmail && String(m.user2Id).trim().toLowerCase() === senderEmail ? { user2Name: nextName } : {}),
              }
              : {}

            return {
              ...m,
              id: convId || m.id,
              name: nextName,
              initials: nextInitials,
              ...updatedPartnerFields,
              thread: [...thread, threadItem],
              preview: content,
              date: 'Just now',
              updatedAt: activityAt,
              unread: !isOutgoing && !isSelectedConversationVisible,
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
          updatedAt: activityAt,
          category: 'Connections',
          mailbox: 'Focused',
          unread: !isOutgoing && !isSelectedConversationVisible,
          user1Id: newMsg.senderId,
          user1Name: newMsg.senderName,
          user2Id: newMsg.receiverId,
          user2Name: newMsg.receiverName,
          thread: [threadItem],
        }
        return [newConv, ...prevMessages]
      }
    })

    if (!isOutgoing) {
      const refreshUnreadCount = isSelectedConversationVisible && /^\d+$/.test(String(convId))
        ? api.markConversationAsRead(convId).then(() => loadBackendConversations(mailbox))
        : loadBackendConversations(mailbox)
      refreshUnreadCount.catch((error) => {
        console.warn('Could not update unread message count:', error)
      })
    }
  }, [
    isActive,
    isDesktopChatOpen,
    isMobileDetailOpen,
    loadBackendConversations,
    mailbox,
    selectedId,
  ])

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
    if (existingConversation?.unread) {
      void markConversationRead(existingConversation.id)
    }
    setActiveCategory('')
    setSearch('')
    setIsMobileDetailOpen(window.innerWidth <= 740)
    setIsDesktopChatOpen(window.innerWidth > 740)
  }, [markConversationRead, messages])

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

  useEffect(() => {
    if (!isConversationMenuOpen) return undefined

    function closeConversationMenu(event) {
      if (!conversationMenuRef.current?.contains(event.target)) {
        setIsConversationMenuOpen(false)
      }
    }

    document.addEventListener('pointerdown', closeConversationMenu)
    return () => document.removeEventListener('pointerdown', closeConversationMenu)
  }, [isConversationMenuOpen])

  const visibleMessages = useMemo(() => {
    const query = search.trim().toLowerCase()

    const filteredMessages = messages.filter((message) => {
      const matchesSearch = !query
        || `${message.name} ${message.preview} ${message.subject}`.toLowerCase().includes(query)
      const matchesCategory = !activeCategory
        || (activeCategory === 'Unread' ? message.unread : activeCategory === 'Starred'
          ? message.starred
          : message.category === activeCategory)

      return matchesSearch && matchesCategory
    })

    return filteredMessages
      .map((message, index) => ({ message, index }))
      .sort((first, second) => (
        Number(Boolean(second.message.starred)) - Number(Boolean(first.message.starred))
        || getConversationActivityTime(second.message) - getConversationActivityTime(first.message)
        || first.index - second.index
      ))
      .map(({ message }) => message)
  }, [activeCategory, messages, search])

  const selectedMessage = messages.find((message) => (
    String(message.id) === String(selectedId)
  ))
  const lastOutgoingMessage = selectedMessage
    ? [...(selectedMessage.thread || [])]
      .reverse()
      .find((message) => isMessageOutgoing(message, selectedMessage, currentUser))
    : null
  const lastEditableMessage = lastOutgoingMessage && !lastOutgoingMessage.edited
    ? lastOutgoingMessage
    : null

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

  async function markConversationRead(id) {
    const selectedMessage = messages.find((message) => String(message.id) === String(id))
    if (!selectedMessage?.unread) return

    setMessages((currentMessages) => currentMessages.map((message) => (
      String(message.id) === String(id) ? { ...message, unread: false } : message
    )))

    if (!/^\d+$/.test(String(id))) return

    try {
      await api.markConversationAsRead(id)
      await loadBackendConversations(mailbox)
    } catch (error) {
      setMessages((currentMessages) => currentMessages.map((message) => (
        String(message.id) === String(id) ? { ...message, unread: true } : message
      )))
      console.warn('Could not mark conversation as read:', error)
    }
  }

  function handleSelectMessage(id) {
    void markConversationRead(id)
    setEditingMessageId('')
    setConversationActionError('')
    setIsConversationMenuOpen(false)

    if (window.innerWidth <= 740) {
      setSelectedId(id)
      setIsMobileDetailOpen(true)
      return
    }

    setSelectedId(id)
    setIsDesktopChatOpen(true)
  }

  async function toggleStar(id) {
    const conversation = messages.find((message) => String(message.id) === String(id))
    if (!conversation) return

    const starred = !conversation.starred
    setMessages((currentMessages) => currentMessages.map((message) => (
      String(message.id) === String(id) ? { ...message, starred } : message
    )))

    if (!/^\d+$/.test(String(id))) return

    try {
      const updatedConversation = await api.starConversation(id)
      setMessages((currentMessages) => currentMessages.map((message) => (
        String(message.id) === String(id)
          ? { ...message, starred: Boolean(updatedConversation.starred) }
          : message
      )))
    } catch (error) {
      setMessages((currentMessages) => currentMessages.map((message) => (
        String(message.id) === String(id) ? { ...message, starred: conversation.starred } : message
      )))
      console.warn('Could not update pinned conversation:', error)
    }
  }

  function startEditingLastMessage() {
    if (!lastEditableMessage) return
    setEditingMessageId(String(lastEditableMessage.id))
    setEditMessageDraft(lastEditableMessage.content || '')
    setConversationActionError('')
    setIsConversationMenuOpen(false)
  }

  async function saveEditedMessage(event) {
    event.preventDefault()
    const content = editMessageDraft.trim()
    if (!selectedMessage || !editingMessageId || !content || isUpdatingMessage) return

    setIsUpdatingMessage(true)
    setConversationActionError('')
    try {
      if (/^\d+$/.test(String(selectedMessage.id))) {
        await api.editMessage(selectedMessage.id, editingMessageId, content)
      }

      setMessages((currentMessages) => currentMessages.map((conversation) => {
        if (String(conversation.id) !== String(selectedMessage.id)) return conversation

        const thread = (conversation.thread || []).map((message) => (
          String(message.id) === editingMessageId ? { ...message, content, edited: true } : message
        ))
        const wasLastMessage = String(thread[thread.length - 1]?.id) === editingMessageId
        return {
          ...conversation,
          thread,
          preview: wasLastMessage ? content : conversation.preview,
        }
      }))
      setEditingMessageId('')
      setEditMessageDraft('')
    } catch (error) {
      console.error('Could not edit message:', error)
      setConversationActionError(error.message || 'Could not edit this message. Please try again.')
    } finally {
      setIsUpdatingMessage(false)
    }
  }

  async function clearSelectedChat() {
    if (!selectedMessage || isClearingChat) return
    const confirmed = window.confirm('Clear all messages for both people in this chat? This cannot be undone.')
    if (!confirmed) return

    setIsClearingChat(true)
    setConversationActionError('')
    try {
      if (/^\d+$/.test(String(selectedMessage.id))) {
        await api.clearConversationMessages(selectedMessage.id)
      }
      setMessages((currentMessages) => currentMessages.map((conversation) => (
        String(conversation.id) === String(selectedMessage.id)
          ? {
            ...conversation,
            thread: [],
            preview: 'Start a conversation',
            date: 'Now',
            unread: false,
          }
          : conversation
      )))
      setEditingMessageId('')
      setEditMessageDraft('')
      setIsConversationMenuOpen(false)
    } catch (error) {
      console.error('Could not clear conversation:', error)
      setConversationActionError(error.message || 'Could not clear this chat. Please try again.')
    } finally {
      setIsClearingChat(false)
    }
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

    const sentAt = new Date().toISOString()
    const outgoingItem = {
      id: messageId,
      content,
      attachment,
      direction: 'outgoing',
      sentAt,
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
          updatedAt: sentAt,
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
        updatedAt: created?.updatedAt || new Date().toISOString(),
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
                className={`message-preview${String(selectedId) === String(message.id) ? ' is-active' : ''}${message.unread ? ' is-unread' : ''}`}
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
                    {message.unread && <span className="message-unread-badge">New</span>}
                    {message.starred && <Star className="message-preview-star" size={14} aria-label="Pinned" />}
                    <time>{message.date}</time>
                  </span>
                  <span className="message-preview-text">{message.preview}</span>
                </button>
              </div>
            )) : (
              <p className="messages-empty">
                {messages.length ? 'No messages match your filters.' : 'No messages yet.'}
              </p>
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
                    <div className="conversation-menu-wrap" ref={conversationMenuRef}>
                      <button
                        className="messages-icon-button"
                        type="button"
                        aria-label="More conversation options"
                        aria-expanded={isConversationMenuOpen}
                        onClick={() => {
                          setConversationActionError('')
                          setIsConversationMenuOpen((isOpen) => !isOpen)
                        }}
                      >
                        <MoreHorizontal size={26} strokeWidth={2.8} />
                      </button>
                      {isConversationMenuOpen && (
                        <div className="conversation-options-menu" role="menu">
                          <button
                            type="button"
                            role="menuitem"
                            disabled={!lastEditableMessage || isUpdatingMessage}
                            onClick={startEditingLastMessage}
                          >
                            Edit last message
                          </button>
                          <button
                            type="button"
                            role="menuitem"
                            disabled={isClearingChat}
                            onClick={clearSelectedChat}
                          >
                            {isClearingChat ? 'Clearing…' : 'Clear chat'}
                          </button>
                          {conversationActionError && (
                            <p role="alert">{conversationActionError}</p>
                          )}
                        </div>
                      )}
                    </div>
                    <button
                      className={`messages-icon-button star-button${selectedMessage.starred ? ' is-starred' : ''}`}
                      type="button"
                      aria-label={selectedMessage.starred ? 'Unpin conversation' : 'Pin conversation'}
                      title={selectedMessage.starred ? 'Unpin conversation' : 'Pin conversation'}
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
                    {!selectedMessage.thread?.length && !selectedMessage.description && (
                      <p className="messages-empty-chat">No messages yet.</p>
                    )}
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
                            {editingMessageId === String(message.id) ? (
                              <form className="conversation-message-edit" onSubmit={saveEditedMessage}>
                                <textarea
                                  aria-label="Edit message"
                                  value={editMessageDraft}
                                  onChange={(event) => setEditMessageDraft(event.target.value)}
                                  maxLength={5000}
                                  autoFocus
                                />
                                {conversationActionError && (
                                  <p role="alert">{conversationActionError}</p>
                                )}
                                <div>
                                  <button type="submit" disabled={!editMessageDraft.trim() || isUpdatingMessage}>
                                    {isUpdatingMessage ? 'Saving…' : 'Save'}
                                  </button>
                                  <button
                                    type="button"
                                    disabled={isUpdatingMessage}
                                    onClick={() => {
                                      setEditingMessageId('')
                                      setEditMessageDraft('')
                                      setConversationActionError('')
                                    }}
                                  >
                                    Cancel
                                  </button>
                                </div>
                              </form>
                            ) : (
                              message.content && <span className="conversation-bubble-text">{message.content}</span>
                            )}
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
