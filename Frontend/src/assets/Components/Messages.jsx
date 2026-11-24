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
} from 'lucide-react'
import './Messages.css'
import api from '../../api'
import { websocketService } from '../../services/websocket'

const categories = ['Opportunities', 'Unread', 'Connections', 'InMail', 'Starred']
const mailboxOptions = ['Focused', 'Archived', ]
const emojis = ['😀', '😂', '😍', '😊', '👍', '🎉', '❤️', '🙏', '🤔']
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

function getHiddenConversationStorageKey(user) {
  const identifier = user?.email || user?.userId || user?.publicUserId || user?.id
  return identifier
    ? `truehunt:hidden-conversations:${String(identifier).trim().toLowerCase()}`
    : ''
}

function readHiddenConversationData(storageKey) {
  if (!storageKey) return { ids: [], deletedAt: {} }

  try {
    const stored = JSON.parse(localStorage.getItem(storageKey) || '[]')
    if (Array.isArray(stored)) {
      return { ids: stored.map(String), deletedAt: {} }
    }
    return {
      ids: Array.isArray(stored?.ids) ? stored.ids.map(String) : [],
      deletedAt: stored?.deletedAt && typeof stored.deletedAt === 'object'
        ? stored.deletedAt
        : {},
    }
  } catch (error) {
    console.warn('Could not read deleted chats from this device:', error)
    return { ids: [], deletedAt: {} }
  }
}

function writeHiddenConversationData(storageKey, data) {
  localStorage.setItem(storageKey, JSON.stringify(data))
}

function getConversationContactName(conversation, user) {
  if (conversation?.isGroupConversation) {
    return String(conversation.name || '').trim()
  }

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

function normalizeConnectedRecipient(profile) {
  const name = String(
    profile?.name || profile?.fullName || profile?.displayName || profile?.userName || '',
  ).trim()
  const userId = String(
    profile?.userId || profile?.publicUserId || profile?.id || profile?._id || profile?.email || '',
  ).trim()
  if (!name || !userId) return null

  return {
    ...profile,
    id: profile.id || userId,
    userId,
    name,
    profileImageUrl: profile.profileImageUrl || profile.avatar || '',
    avatar: profile.profileImageUrl || profile.avatar || '',
  }
}

function getConversationActivityTime(conversation) {
  const thread = Array.isArray(conversation.thread) ? conversation.thread : []
  const lastMessageTime = thread[thread.length - 1]?.sentAt
  const timestamp = Date.parse(conversation.updatedAt || lastMessageTime || '')
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function formatConversationActivity(conversation, now) {
  const timestamp = getConversationActivityTime(conversation)
  if (!timestamp) return conversation.date || ''

  const elapsed = Math.max(0, now - timestamp)
  const minutes = Math.floor(elapsed / 60000)
  if (minutes < 1) return 'Just now'
  if (minutes < 60) return `${minutes}m ago`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`

  const days = Math.floor(hours / 24)
  if (days === 1) return 'Yesterday'
  if (days < 7) return `${days}d ago`
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(timestamp)
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
    mailbox: 'Focused',
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
    } catch (error) {
      console.warn('Could not read the cached user for message identity:', error)
    }
  }

  const myEmail = (effectiveUser?.email || '').trim().toLowerCase()
  const myId = String(effectiveUser?.userId || effectiveUser?.publicUserId || effectiveUser?.id || '').trim().toLowerCase()
  const myName = (effectiveUser?.name || effectiveUser?.fullName || '').trim().toLowerCase()

  const msgSender = String(msg.senderId || '').trim().toLowerCase()
  const msgSenderName = String(msg.senderName || '').trim().toLowerCase()
  const msgReceiver = String(msg.receiverId || '').trim().toLowerCase()

  // 1. Primary check: Unique sender ID (email, publicUserId, user ID, name, or 'me')
  if (msgSender) {
    if ((myEmail && msgSender === myEmail) || (myId && msgSender === myId) || (myName && msgSender === myName) || msgSender === 'me') {
      return true
    }
    if (myName && msgSenderName && msgSenderName === myName) {
      return true
    }
    // Sender ID is present and does not match current user -> incoming message
    return false
  }

  // 2. Primary check: Unique receiver ID
  if (msgReceiver) {
    if ((myEmail && msgReceiver === myEmail) || (myId && msgReceiver === myId) || (myName && msgReceiver === myName)) {
      return false
    }
    return true
  }

  // 3. Fallback to message.direction property (which backend toDto computes accurately)
  if (msg.direction === 'incoming') return false
  if (msg.direction === 'outgoing') return true

  return false
}

export function getUnreadConversationCount(conversations) {
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
  const [isUpdatingMailbox, setIsUpdatingMailbox] = useState(false)
  const [isClearingChat, setIsClearingChat] = useState(false)
  const [conversationActionError, setConversationActionError] = useState('')
  const [search, setSearch] = useState('')
  const [isComposeOpen, setIsComposeOpen] = useState(false)
  const [recipient, setRecipient] = useState('')
  const [selectedRecipient, setSelectedRecipient] = useState(null)
  const [connectedRecipients, setConnectedRecipients] = useState([])
  const [isLoadingRecipients, setIsLoadingRecipients] = useState(false)
  const [isRecipientSuggestionsOpen, setIsRecipientSuggestionsOpen] = useState(false)
  const [recipientError, setRecipientError] = useState('')
  const [isSendingMessage, setIsSendingMessage] = useState(false)
  const [messageText, setMessageText] = useState('')
  const [replyText, setReplyText] = useState('')
  const [replyValidationError, setReplyValidationError] = useState('')
  const [replyAttachment, setReplyAttachment] = useState(null)
  const [isSendingReply, setIsSendingReply] = useState(false)
  const [isConnectPromptOpen, setIsConnectPromptOpen] = useState(false)
  const [isSendingConnectionRequest, setIsSendingConnectionRequest] = useState(false)
  const [connectionRequestError, setConnectionRequestError] = useState('')
  const [isEmojiPickerOpen, setIsEmojiPickerOpen] = useState(false)
  const [isMobileDetailOpen, setIsMobileDetailOpen] = useState(
    () => Boolean(initialProfileConversation && window.innerWidth <= 740),
  )
  const [isDesktopChatOpen, setIsDesktopChatOpen] = useState(
    () => Boolean(initialProfileConversation && window.innerWidth > 740),
  )
  const [isWsConnected, setIsWsConnected] = useState(false)
  const [activityClock, setActivityClock] = useState(() => Date.now())
  const replyInputRef = useRef(null)
  const messagesRef = useRef(messages)
  const recipientInputRef = useRef(null)
  const attachmentInputRef = useRef(null)
  const emojiPickerRef = useRef(null)
  const conversationMenuRef = useRef(null)
  const previousInboxRequest = useRef(resetToInboxRequest)
  const autoReadAttemptRef = useRef({ conversationId: null, attempted: false })

  const currentUser = useMemo(() => readStoredUser(), [])
  const hiddenConversationStorageKey = getHiddenConversationStorageKey(currentUser)
  const [hiddenConversationData, setHiddenConversationData] = useState(
    () => readHiddenConversationData(hiddenConversationStorageKey),
  )
  const hiddenConversationIds = hiddenConversationData.ids

  messagesRef.current = messages

  useEffect(() => {
    const timer = window.setInterval(() => setActivityClock(Date.now()), 60000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    function handleConnectionChanged(event) {
      const detail = event.detail || {}
      const eventProfiles = [
        detail.profile,
        detail.senderProfile,
        detail.receiverProfile,
      ].filter(Boolean)
      const connected = typeof detail.connected === 'boolean'
        ? detail.connected
        : detail.isConnected

      if (typeof connected !== 'boolean') return

      const pending = typeof detail.pending === 'boolean'
        ? detail.pending
        : Boolean(detail.isPending)
      const identities = [
        ...eventProfiles.flatMap((profile) => [
          profile.id,
          profile.userId,
          profile.user_id,
          profile.profileId,
          profile.senderId,
          profile.targetProfileId,
          profile.publicUserId,
          profile.referId,
          profile._id,
          profile.email,
        ]),
        detail.profileId,
        detail.userId,
        detail.senderId,
        detail.receiverId,
      ]
        .filter(Boolean)
        .map((identity) => String(identity).trim().toLowerCase())
      identities.push(...eventProfiles
        .map((profile) => profile.name || profile.fullName || profile.displayName)
        .filter(Boolean)
        .map((name) => `name:${String(name).trim().toLowerCase()}`))
      if (!identities.length) return

      setMessages((currentMessages) => currentMessages.map((message) => {
        const messageProfile = message.profile || {}
        const messageIdentities = [
          messageProfile.id,
          messageProfile.userId,
          messageProfile.user_id,
          messageProfile.profileId,
          messageProfile.senderId,
          messageProfile.targetProfileId,
          messageProfile.publicUserId,
          messageProfile.referId,
          messageProfile._id,
          messageProfile.email,
        ]
          .filter(Boolean)
          .map((identity) => String(identity).trim().toLowerCase())
        messageIdentities.push(`name:${String(messageProfile.name || message.name || '').trim().toLowerCase()}`)
        if (!messageIdentities.some((identity) => identities.includes(identity))) return message

        const outgoingCount = (message.thread || [])
          .filter((threadMessage) => isMessageOutgoing(threadMessage, message, currentUser))
          .length
        const resetCount = connected
          ? 0
          : messageProfile.isConnected
            ? outgoingCount
            : messageProfile.networkMessageResetCount

        return {
          ...message,
          profile: {
            ...messageProfile,
            isConnected: connected,
            isPending: pending,
            networkMessageResetCount: messageProfile.networkMessageLimit
              ? resetCount
              : messageProfile.networkMessageResetCount,
          },
        }
      }))
    }

    window.addEventListener('truehunt-connection-changed', handleConnectionChanged)
    return () => window.removeEventListener('truehunt-connection-changed', handleConnectionChanged)
  }, [currentUser])

  const loadBackendConversations = useCallback(async (currentMailbox = 'Focused') => {
    try {
      const latestUser = readStoredUser() || currentUser
      const data = await api.getConversations(currentMailbox)
      let effectiveHiddenIds = hiddenConversationIds
      let reactivatedAt = new Map()
      if (Array.isArray(data)) {
        const newlyActiveIds = data
          .filter((conversation) => {
            const conversationId = String(conversation.id)
            if (!hiddenConversationIds.includes(conversationId)) return false

            const deletedAt = Date.parse(hiddenConversationData.deletedAt[conversationId] || '')
            const thread = Array.isArray(conversation.thread) ? conversation.thread : []
            const lastMessage = thread[thread.length - 1]
            const lastMessageAt = Date.parse(lastMessage?.sentAt || conversation.updatedAt || '')
            return Number.isFinite(deletedAt) && Number.isFinite(lastMessageAt) && lastMessageAt > deletedAt
          })
          .map((conversation) => String(conversation.id))
        if (newlyActiveIds.length) {
          reactivatedAt = new Map(newlyActiveIds.map((id) => [
            id,
            Date.parse(hiddenConversationData.deletedAt[id] || ''),
          ]))
          const newlyActiveSet = new Set(newlyActiveIds)
          effectiveHiddenIds = hiddenConversationIds.filter((id) => !newlyActiveSet.has(id))
          const nextHiddenData = {
            ids: effectiveHiddenIds,
            deletedAt: { ...hiddenConversationData.deletedAt },
          }
          try {
            writeHiddenConversationData(hiddenConversationStorageKey, nextHiddenData)
          } catch (error) {
            console.error('Could not restore a new chat to this device:', error)
            setConversationActionError('A new message arrived, but this device could not update your deleted-chat list.')
          }
          setHiddenConversationData(nextHiddenData)
        }
        const hiddenIds = new Set(effectiveHiddenIds)
        const namedConversations = data.map((conversation) => {
          const resolvedName = getConversationContactName(conversation, latestUser) || 'Profile'
          const conversationId = String(conversation.id)
          const cutoff = reactivatedAt.get(conversationId)
            ?? Date.parse(hiddenConversationData.deletedAt[conversationId] || '')
          const fullThread = Array.isArray(conversation.thread) ? conversation.thread : []
          const thread = Number.isFinite(cutoff)
            ? fullThread.filter((message, index) => {
              const sentAt = Date.parse(message.sentAt || '')
              if (Number.isFinite(sentAt)) return sentAt > cutoff
              return index === fullThread.length - 1
                && Date.parse(conversation.updatedAt || '') > cutoff
            })
            : fullThread
          return {
            ...conversation,
            name: resolvedName,
            initials: computeInitials(resolvedName),
            avatar: conversation.profileImageUrl || conversation.avatar || '',
            thread,
            preview: thread.length
              ? thread[thread.length - 1].content
              : Number.isFinite(cutoff) ? 'Start a conversation' : conversation.preview,
          }
        }).filter((conversation) => !hiddenIds.has(String(conversation.id)))
        const conversationsWithNetworkState = namedConversations.map((conversation) => {
          const participantIds = [
              conversation.user1Id,
              conversation.user2Id,
          ]
              .filter(Boolean)
              .map((identity) => String(identity).trim().toLowerCase())
          const previousConversation = messagesRef.current.find((message) => {
              const profileIds = [
                message.profile?.id,
                message.profile?.userId,
                message.profile?.user_id,
                message.profile?.profileId,
                message.profile?.publicUserId,
                message.profile?.email,
              ]
                .filter(Boolean)
                .map((identity) => String(identity).trim().toLowerCase())
              return profileIds.some((identity) => participantIds.includes(identity))
                || message.name.trim().toLowerCase() === conversation.name.trim().toLowerCase()
          })
          if (!previousConversation) return conversation

          return {
              ...conversation,
              avatar: conversation.profileImageUrl
                || conversation.avatar
                || previousConversation.avatar
                || previousConversation.profile?.profileImageUrl
                || previousConversation.profile?.avatar
                || '',
              profileImageUrl: conversation.profileImageUrl
                || previousConversation.profileImageUrl
                || previousConversation.avatar
                || previousConversation.profile?.profileImageUrl
                || previousConversation.profile?.avatar
                || '',
              ...(previousConversation.profile?.networkMessageLimit
                ? { profile: previousConversation.profile }
                : {}),
          }
        })
        const unreadConversation = conversationsWithNetworkState.find((conversation) => (
          getUnreadConversationCount([conversation]) > 0
        ))
        setMessages((prev) => {
          const profileConversations = prev.filter((m) => (
            String(m.id).startsWith('profile-') && !hiddenIds.has(String(m.id))
          ))
          const backendIds = new Set(conversationsWithNetworkState.map((d) => String(d.id)))
          const merged = [
            ...conversationsWithNetworkState,
            ...profileConversations.filter(
              (p) => !backendIds.has(String(p.id))
                && !conversationsWithNetworkState.some((d) => d.name.toLowerCase() === p.name.toLowerCase())
            ),
          ]
          return merged
        })
        setSelectedId((current) => {
          const matchingConversation = conversationsWithNetworkState.find((conversation) => (
            String(conversation.id) === String(current)
          ))
          if (matchingConversation) return matchingConversation.id
          if (current == null) {
            return conversationsWithNetworkState.find((conversation) => (
              String(conversation.id) === newlyActiveIds[0]
            ))?.id || unreadConversation?.id || null
          }
          if (String(current).startsWith('profile-')) return current
          return conversationsWithNetworkState[0]?.id || null
        })
        if (isActive && (newlyActiveIds.length || unreadConversation)) {
          setIsMobileDetailOpen(window.innerWidth <= 740)
          setIsDesktopChatOpen(window.innerWidth > 740)
        }
      }

      const allConversations = currentMailbox ? await api.getConversations() : data
      if (Array.isArray(allConversations)) {
        const hiddenIds = new Set(effectiveHiddenIds)
        onUnreadCountChange?.(getUnreadConversationCount(
          allConversations.filter((conversation) => !hiddenIds.has(String(conversation.id))),
        ))
      }
    } catch (err) {
      console.warn('Could not load conversations from backend:', err)
    }
  }, [
    currentUser,
    hiddenConversationData,
    hiddenConversationIds,
    hiddenConversationStorageKey,
    isActive,
    onUnreadCountChange,
  ])

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
    const conversationId = String(convId)
    if (hiddenConversationIds.includes(conversationId)) {
      if (!newMsg.content || ['CONVERSATION_CLEARED', 'MESSAGE_EDITED', 'READ_RECEIPT'].includes(newMsg.type)) {
        return
      }

      const nextHiddenData = {
        ids: hiddenConversationIds.filter((id) => id !== conversationId),
        deletedAt: { ...hiddenConversationData.deletedAt },
      }
      try {
        writeHiddenConversationData(hiddenConversationStorageKey, nextHiddenData)
      } catch (error) {
        console.error('Could not restore a new chat to this device:', error)
        setConversationActionError('A new message arrived, but this device could not update your deleted-chat list.')
      }
      setHiddenConversationData(nextHiddenData)
    }
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
    if (newMsg.type === 'READ_RECEIPT') {
      const readMessageIds = new Set((newMsg.messageIds || []).map(String))
      setMessages((prevMessages) => prevMessages.map((conversation) => {
        if (String(conversation.id) !== String(newMsg.conversationId)) return conversation
        return {
          ...conversation,
          thread: (conversation.thread || []).map((message) => (
            readMessageIds.has(String(message.id))
              || readMessageIds.has(String(message.messageId))
              ? { ...message, status: 'READ', readAt: newMsg.readAt }
              : message
          )),
        }
      }))
      return
    }
    if (!newMsg.content) return

    let currentUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) currentUser = JSON.parse(u)
    } catch (error) {
      console.warn('Could not read the cached user for incoming messages:', error)
    }

    const content = newMsg.content
    const msgId = newMsg.id || `msg-${Date.now()}`
    const activityAt = newMsg.sentAt || new Date().toISOString()

    const myEmail = (currentUser?.email || '').trim().toLowerCase()
    const myId = String(currentUser?.userId || currentUser?.publicUserId || currentUser?.id || '').trim().toLowerCase()
    const myName = (currentUser?.name || currentUser?.fullName || '').trim().toLowerCase()

    const senderEmail = (newMsg.senderId || '').trim().toLowerCase()
    const senderName = (newMsg.senderName || '').trim().toLowerCase()
    const receiverEmail = (newMsg.receiverId || '').trim().toLowerCase()
    const isSenderMe = Boolean(
      (myEmail && senderEmail === myEmail) ||
      (myId && senderEmail === myId) ||
      (myName && (senderEmail === myName || senderName === myName)) ||
      senderEmail === 'me'
    )
    const isReceiverMe = Boolean(
      (myEmail && receiverEmail === myEmail) ||
      (myId && receiverEmail === myId) ||
      (myName && receiverEmail === myName)
    )

    const isOutgoing = isSenderMe ? true : isReceiverMe ? false : Boolean(newMsg.direction === 'outgoing')

    const otherName = isOutgoing ? (newMsg.receiverName || 'Cinema Professional') : (newMsg.senderName || 'Cinema Professional')
    const otherId = isOutgoing ? (newMsg.receiverId || '') : (newMsg.senderId || '')
    const isSelectedConversationVisible = isActive
      && String(selectedId) === String(convId)
      && (window.innerWidth <= 740 ? isMobileDetailOpen : isDesktopChatOpen)
    if (isOutgoing && convId != null && String(selectedId || '').startsWith('profile-')) {
      const selectedConversation = messagesRef.current.find((message) => (
        String(message.id) === String(selectedId)
      ))
      const selectedIdentities = [
        selectedConversation?.name,
        selectedConversation?.profile?.id,
        selectedConversation?.profile?.userId,
        selectedConversation?.profile?.email,
      ]
        .filter(Boolean)
        .map((identity) => String(identity).trim().toLowerCase())
      const incomingIdentities = [otherName, otherId]
        .filter(Boolean)
        .map((identity) => String(identity).trim().toLowerCase())
      if (selectedIdentities.some((identity) => incomingIdentities.includes(identity))) {
        setSelectedId(convId)
      }
    }

    const threadItem = {
      id: msgId,
      content,
      direction: isOutgoing ? 'outgoing' : 'incoming',
      status: newMsg.status || 'SENT',
      readAt: newMsg.readAt || null,
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

            const hasFreshPartnerName = !m.isGroupConversation && Boolean(
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

    if (!isOutgoing && isActive && selectedId == null && convId != null) {
      setSelectedId(convId)
      setIsMobileDetailOpen(window.innerWidth <= 740)
      setIsDesktopChatOpen(window.innerWidth > 740)
    }

    if (!isOutgoing) {
      const isVisibleAndFocused = isSelectedConversationVisible
        && typeof document !== 'undefined'
        && !document.hidden
        && (typeof document.hasFocus !== 'function' || document.hasFocus())

      if (isVisibleAndFocused && /^\d+$/.test(String(convId))) {
        // Debounce read receipt so incoming messages do not instantly snap to Seen at 0ms
        const timer = setTimeout(() => {
          const stillFocused = typeof document !== 'undefined'
            && !document.hidden
            && (typeof document.hasFocus !== 'function' || document.hasFocus())
          if (stillFocused) {
            api.markConversationAsRead(convId)
              .then(() => loadBackendConversations(mailbox))
              .catch((error) => console.warn('Could not update unread message count:', error))
          }
        }, 1200)
        return () => clearTimeout(timer)
      } else {
        loadBackendConversations(mailbox).catch((error) => {
          console.warn('Could not update unread message count:', error)
        })
      }
    }
  }, [
    isActive,
    isDesktopChatOpen,
    hiddenConversationData,
    isMobileDetailOpen,
    loadBackendConversations,
    mailbox,
    selectedId,
    hiddenConversationIds,
    hiddenConversationStorageKey,
  ])

  useEffect(() => {
    let storedUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) storedUser = JSON.parse(u)
    } catch (error) {
      console.warn('Could not read the cached user for reply identity:', error)
    }

    const token = localStorage.getItem('truehunt_token') || ''
    const email = storedUser?.email || 'pro@truehunt.in'

    websocketService.connect(token, email)
    const unsubStatus = websocketService.onStatusChange(setIsWsConnected)
    const unsubMsg = websocketService.onMessage(handleIncomingWebSocketMessage)

    const handleFocus = () => {
      if (!document.hidden) {
        loadBackendConversations(mailbox)
        if (selectedId && /^\d+$/.test(String(selectedId))) {
          void markConversationRead(selectedId)
        }
      }
    }
    window.addEventListener('focus', handleFocus)

    return () => {
      window.removeEventListener('focus', handleFocus)
      unsubStatus()
      unsubMsg()
    }
  }, [mailbox, loadBackendConversations, handleIncomingWebSocketMessage])

  useEffect(() => {
    if (!isActive) return undefined

    const refreshTimer = window.setTimeout(() => {
      void loadBackendConversations(mailbox)
    }, 0)
    return () => window.clearTimeout(refreshTimer)
  }, [isActive, loadBackendConversations, mailbox])

  const openConversation = useCallback((conversation) => {
    setIsConnectPromptOpen(false)
    setConnectionRequestError('')
    const conversationId = String(conversation?.id || '')
    if (conversationId && !conversationId.startsWith('profile-') && Array.isArray(conversation.thread)) {
      const contactName = getConversationContactName(conversation, currentUser)
        || String(conversation.name || '').trim()
        || 'Profile'
      const persistedConversation = {
        ...conversation,
        id: conversation.id,
        name: contactName,
        initials: computeInitials(contactName),
        avatar: conversation.profileImageUrl || conversation.avatar || '',
        unread: false,
      }

      setMessages((currentMessages) => [
        persistedConversation,
        ...currentMessages.filter((message) => (
          String(message.id) !== conversationId
          && !(String(message.id).startsWith('profile-')
            && String(message.name || '').trim().toLowerCase() === contactName.toLowerCase())
        )),
      ])
      setSelectedId(conversation.id)
      setActiveCategory('')
      setSearch('')
      setIsMobileDetailOpen(window.innerWidth <= 740)
      setIsDesktopChatOpen(window.innerWidth > 740)
      void markConversationRead(conversation.id)
      return
    }

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
            profile: {
              ...profileConversation.profile,
              networkMessageResetCount: profileConversation.profile.isConnected
                ? 0
                : message.profile?.isConnected
                  ? (message.thread || []).filter((threadMessage) => (
                    isMessageOutgoing(threadMessage, message, currentUser)
                  )).length
                  : message.profile?.networkMessageResetCount,
            },
          }
          : message
      ))
    })

    setSelectedId(existingConversation?.id || profileConversation.id)
    if (existingConversation) {
      void markConversationRead(existingConversation.id)
    }
    setActiveCategory('')
    setSearch('')
    setIsMobileDetailOpen(window.innerWidth <= 740)
    setIsDesktopChatOpen(window.innerWidth > 740)
  }, [currentUser, markConversationRead, messages])

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
      const matchesMailbox = String(message.mailbox || 'Focused').toLowerCase() === mailbox.toLowerCase()
      const matchesCategory = !activeCategory
        || (activeCategory === 'Unread' ? message.unread : activeCategory === 'Starred'
          ? message.starred
          : message.category === activeCategory)

      return matchesSearch && matchesMailbox && matchesCategory
    })

    return filteredMessages
      .map((message, index) => ({ message, index }))
      .sort((first, second) => (
        Number(Boolean(second.message.starred)) - Number(Boolean(first.message.starred))
        || getConversationActivityTime(second.message) - getConversationActivityTime(first.message)
        || first.index - second.index
      ))
      .map(({ message }) => message)
  }, [activeCategory, mailbox, messages, search])

  const filteredRecipients = useMemo(() => {
    const query = recipient.trim().toLowerCase()
    if (!query) return []
    return connectedRecipients.filter((profile) => (
      profile.name.toLowerCase().includes(query)
    ))
  }, [connectedRecipients, recipient])

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
    const wasUnread = Boolean(selectedMessage?.unread)
    if (wasUnread) {
      setMessages((currentMessages) => currentMessages.map((message) => (
        String(message.id) === String(id) ? { ...message, unread: false } : message
      )))
    }

    if (!/^\d+$/.test(String(id))) return

    try {
      await api.markConversationAsRead(id)
      await loadBackendConversations(mailbox)
    } catch (error) {
      if (wasUnread) {
        setMessages((currentMessages) => currentMessages.map((message) => (
          String(message.id) === String(id) ? { ...message, unread: true } : message
        )))
      }
      console.warn('Could not mark conversation as read:', error)
    }
  }

  useEffect(() => {
    const conversationId = selectedId == null ? '' : String(selectedId)
    if (autoReadAttemptRef.current.conversationId !== conversationId) {
      autoReadAttemptRef.current = { conversationId, attempted: false }
    }

    const isConversationVisible = isActive
      && (window.innerWidth <= 740 ? isMobileDetailOpen : isDesktopChatOpen)
    if (!conversationId || !isConversationVisible || autoReadAttemptRef.current.attempted) return

    const selectedConversation = messages.find((message) => String(message.id) === conversationId)
    if (!selectedConversation?.unread) return

    autoReadAttemptRef.current.attempted = true
    void markConversationRead(selectedId)
  }, [isActive, isDesktopChatOpen, isMobileDetailOpen, mailbox, messages, selectedId])

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

  async function moveSelectedChatToMailbox(targetMailbox) {
    if (!selectedMessage || isUpdatingMailbox) return

    setIsUpdatingMailbox(true)
    setConversationActionError('')
    try {
      if (/^\d+$/.test(String(selectedMessage.id))) {
        await api.updateMailbox(selectedMessage.id, targetMailbox)
      }

      setMessages((currentMessages) => currentMessages.map((message) => (
        String(message.id) === String(selectedMessage.id)
          ? { ...message, mailbox: targetMailbox }
          : message
      )))
      setMailbox(targetMailbox)
      setIsConversationMenuOpen(false)
    } catch (error) {
      console.error(`Could not move conversation to ${targetMailbox}:`, error)
      setConversationActionError(error.message || `Could not move this chat to ${targetMailbox}. Please try again.`)
    } finally {
      setIsUpdatingMailbox(false)
    }
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

  function deleteSelectedChatForMe() {
    if (!selectedMessage) return

    const confirmed = window.confirm(
      'Delete this chat from your inbox? The other participant will still have their chat.',
    )
    if (!confirmed) return

    if (!hiddenConversationStorageKey) {
      setConversationActionError('Could not identify your account. Please sign in again and retry.')
      return
    }

    const conversationId = String(selectedMessage.id)
    const nextHiddenData = {
      ids: [...new Set([...hiddenConversationIds, conversationId])],
      deletedAt: {
        ...hiddenConversationData.deletedAt,
        [conversationId]: new Date().toISOString(),
      },
    }
    try {
      writeHiddenConversationData(hiddenConversationStorageKey, nextHiddenData)
    } catch (error) {
      console.error('Could not delete chat from this device:', error)
      setConversationActionError('Could not delete this chat from your inbox. Please try again.')
      return
    }

    setHiddenConversationData(nextHiddenData)
    setMessages((currentMessages) => currentMessages.filter(
      (conversation) => String(conversation.id) !== conversationId,
    ))
    if (/^\d+$/.test(conversationId)) {
      api.hideConversation(conversationId).catch((error) => {
        console.warn('Could not persist hidden conversation on backend:', error)
      })
    }
    setSelectedId(null)
    setIsMobileDetailOpen(false)
    setIsDesktopChatOpen(false)
    setEditingMessageId('')
    setEditMessageDraft('')
    setIsConversationMenuOpen(false)
    setConversationActionError('')
  }

  async function sendReply(event) {
    event.preventDefault()
    const content = replyText.trim()

    if (isSendingReply || !selectedMessage || (!content && !replyAttachment)) return

    let currentUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) currentUser = JSON.parse(u)
    } catch (error) {
      console.warn('Could not read the cached user for reply identity:', error)
    }

    const myIdentity = currentUser?.email || currentUser?.userId || 'me'
    const myEmail = (currentUser?.email || '').trim().toLowerCase()
    const myId = String(currentUser?.userId || currentUser?.publicUserId || currentUser?.id || '').trim().toLowerCase()
    const isMe = (val) => {
      if (!val) return false
      const v = String(val).trim().toLowerCase()
      return v === myEmail || v === myId
    }

    const totalOutgoingMessageCount = (selectedMessage.thread || [])
      .filter((message) => isMessageOutgoing(message, selectedMessage, currentUser))
      .length
    let isRecipientConnected = Boolean(selectedMessage.profile?.isConnected)
    let resetMessageCount = Number(selectedMessage.profile?.networkMessageResetCount || 0)
    if (selectedMessage.profile?.networkMessageLimit) {
      try {
        const connectedProfiles = await api.getConnectedProfiles()
        if (!Array.isArray(connectedProfiles)) {
          throw new Error('Connected profiles returned an invalid response.')
        }

        const targetIdentities = new Set([
          selectedMessage.profile.id,
          selectedMessage.profile.userId,
          selectedMessage.profile.user_id,
          selectedMessage.profile.profileId,
          selectedMessage.profile.publicUserId,
          selectedMessage.profile.email,
        ]
          .filter(Boolean)
          .map((identity) => String(identity).trim().toLowerCase()))
        isRecipientConnected = connectedProfiles.some((profile) => [
          profile.id,
          profile.userId,
          profile.user_id,
          profile.profileId,
          profile.publicUserId,
          profile.email,
        ]
          .filter(Boolean)
          .some((identity) => targetIdentities.has(String(identity).trim().toLowerCase())))

        if (isRecipientConnected) {
          resetMessageCount = 0
        } else if (selectedMessage.profile.isConnected) {
          resetMessageCount = totalOutgoingMessageCount
        }
        setMessages((currentMessages) => currentMessages.map((message) => (
          message.id === selectedMessage.id
            ? {
              ...message,
              profile: {
                ...message.profile,
                isConnected: isRecipientConnected,
                networkMessageResetCount: resetMessageCount,
              },
            }
            : message
        )))
      } catch (error) {
        console.error('Could not verify connection status before sending:', error)
        setReplyValidationError('Could not verify this connection. Please try again.')
        return
      }
    }

    const outgoingMessageCount = Math.max(0, totalOutgoingMessageCount - resetMessageCount)
    const isUnconnectedNetworkConversation = selectedMessage.profile?.networkMessageLimit
      && !isRecipientConnected
    if (isUnconnectedNetworkConversation) {
      if (outgoingMessageCount >= 1) {
        setReplyValidationError('')
        setConnectionRequestError('')
        setIsConnectPromptOpen(true)
        return
      }

      if (content.split(/\s+/).filter(Boolean).length !== 1) {
        setReplyValidationError('Before connecting, you can send one message containing only one word.')
        return
      }
    }
    setReplyValidationError('')

    const targetReceiverId = (() => {
      if (selectedMessage.isGroupConversation) return `group:${selectedMessage.connectionGroupId}`
      if (selectedMessage.profile?.email && !isMe(selectedMessage.profile.email)) {
        return selectedMessage.profile.email
      }
      if (selectedMessage.profile?.userId && !isMe(selectedMessage.profile.userId)) {
        return selectedMessage.profile.userId
      }
      if (selectedMessage.user1Id && !isMe(selectedMessage.user1Id)) return selectedMessage.user1Id
      if (selectedMessage.user2Id && !isMe(selectedMessage.user2Id)) return selectedMessage.user2Id
      if (selectedMessage.profile?.id && !isMe(selectedMessage.profile.id)) return selectedMessage.profile.id
      if (selectedMessage.name && !isMe(selectedMessage.name)) return selectedMessage.name
      return selectedMessage.user2Id || selectedMessage.user1Id || selectedMessage.name
    })()

    const messageId = `msg-${Date.now()}`
    let attachment = replyAttachment
    if (typeof File !== 'undefined' && replyAttachment instanceof File) {
      try {
        const uploaded = await api.uploadMedia(replyAttachment, 'chat-attachment')
        attachment = {
          name: uploaded?.fileName || replyAttachment.name,
          fileName: uploaded?.fileName || replyAttachment.name,
          url: uploaded?.url || '',
          contentType: uploaded?.contentType || replyAttachment.type || '',
          sizeBytes: uploaded?.sizeBytes || replyAttachment.size || 0,
          kind: uploaded?.kind || 'file',
        }
      } catch (uploadError) {
        console.warn('Chat attachment upload fallback:', uploadError)
        attachment = {
          name: replyAttachment.name,
          fileName: replyAttachment.name,
          contentType: replyAttachment.type || '',
          sizeBytes: replyAttachment.size || 0,
        }
      }
    }

    const sentAt = new Date().toISOString()
    const outgoingItem = {
      id: messageId,
      content,
      attachment,
      direction: 'outgoing',
      status: 'SENT',
      readAt: null,
      sentAt,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      senderId: myIdentity,
      senderName: currentUser?.name || 'You',
      receiverId: targetReceiverId,
      receiverName: selectedMessage.name,
    }

    const isTempId = String(selectedMessage.id).startsWith('profile-')
      || String(selectedMessage.id).startsWith('conv-')
    if (isTempId) {
      setIsSendingReply(true)
      try {
        const sentMessage = await api.sendDirectMessage(
          targetReceiverId,
          selectedMessage.name,
          content,
          attachment,
          messageId,
        )
        if (sentMessage?.conversationId == null) {
          throw new Error('The message could not be added to a conversation. Please try again.')
        }

        const persistedMessage = {
          ...outgoingItem,
          ...sentMessage,
          id: sentMessage.id || messageId,
          direction: 'outgoing',
          attachment: sentMessage.attachment ?? attachment,
          sentAt: sentMessage.sentAt || sentAt,
        }
        setMessages((currentMessages) => currentMessages.map((message) => {
          if (
            String(message.id) !== String(selectedMessage.id)
            && String(message.id) !== String(sentMessage.conversationId)
          ) {
            return message
          }

          const thread = message.thread || []
          const existingMessageIndex = thread.findIndex((threadMessage) => (
            String(threadMessage.id) === String(persistedMessage.id)
          ))
          return {
            ...message,
            id: sentMessage.conversationId,
            thread: existingMessageIndex >= 0
              ? thread.map((threadMessage, index) => (
                index === existingMessageIndex
                  ? { ...threadMessage, ...persistedMessage }
                  : threadMessage
              ))
              : [...thread, persistedMessage],
            preview: content || (attachment ? `Attachment: ${attachment.name}` : ''),
            date: 'Now',
            updatedAt: persistedMessage.sentAt,
            unread: false,
          }
        }))
        setSelectedId(sentMessage.conversationId)
        setReplyText('')
        setReplyAttachment(null)
        setIsEmojiPickerOpen(false)
        setReplyValidationError('')
      } catch (error) {
        console.error('Could not send the first message:', error)
        setReplyValidationError(error.message || 'Could not send the message. Please try again.')
      } finally {
        setIsSendingReply(false)
      }
      return
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
    const numericConvId = !Number.isNaN(Number(selectedMessage.id)) ? Number(selectedMessage.id) : null

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

    const sentOverWs = websocketService.sendMessage('/app/chat.private', wsPayload)

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

  async function sendConnectionRequestForConversation() {
    const profile = selectedMessage?.profile
    const profileId = profile?.id || profile?.userId || profile?._id || profile?.email
    if (!profileId) {
      setConnectionRequestError('Could not identify this profile. Please try again from Network.')
      return
    }
    if (isSendingConnectionRequest || profile?.isPending) return

    setConnectionRequestError('')
    setIsSendingConnectionRequest(true)
    try {
      const result = await api.sendConnectionRequest(profileId, '')
      const connected = Boolean(result?.connected || result?.isConnected)
      const pending = !connected
      const updatedProfile = {
        ...profile,
        isConnected: connected,
        isPending: pending,
      }

      setMessages((currentMessages) => currentMessages.map((message) => (
        message.id === selectedMessage.id
          ? { ...message, profile: updatedProfile }
          : message
      )))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected, pending, profile: updatedProfile },
      }))
      if (connected) setIsConnectPromptOpen(false)
    } catch (error) {
      console.error('Could not send connection request from Messages:', error)
      setConnectionRequestError(error.message || 'Could not send the connection request. Please try again.')
    } finally {
      setIsSendingConnectionRequest(false)
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
    const content = messageText.trim()

    if (!selectedRecipient || !content || isSendingMessage) return

    setRecipientError('')
    setIsSendingMessage(true)
    try {
      const created = await api.createConversation(selectedRecipient.userId, content)
      if (!created?.id) {
        throw new Error('Could not start this conversation. Please try again.')
      }
      if (!Array.isArray(created.thread)) {
        throw new Error('The conversation started, but its messages could not be loaded.')
      }

      const convId = created.id
      const contactName = created.name || selectedRecipient.name

      const newMessage = {
        ...created,
        id: convId,
        name: contactName,
        initials: computeInitials(contactName),
        color: created.color || 'purple',
        avatar: created.profileImageUrl || created.avatar || selectedRecipient.profileImageUrl || '',
        preview: created.preview || content,
        date: created.date || 'Now',
        updatedAt: created?.updatedAt || new Date().toISOString(),
        category: created.category || 'Connections',
        mailbox: created.mailbox || 'Focused',
        unread: false,
        thread: created.thread,
      }

      setMessages((currentMessages) => [
        newMessage,
        ...currentMessages.filter((message) => (
          String(message.id) !== String(convId)
          && !(String(message.id).startsWith('profile-')
            && (message.name || '').toLowerCase() === contactName.toLowerCase())
        )),
      ])
      setSelectedId(convId)
      setActiveCategory('')
      setIsMobileDetailOpen(window.innerWidth <= 740)
      setIsDesktopChatOpen(window.innerWidth > 740)
      setRecipient('')
      setSelectedRecipient(null)
      setMessageText('')
      setIsComposeOpen(false)
    } catch (err) {
      console.error('Failed to create conversation:', err)
      setRecipientError(err instanceof Error
        ? err.message
        : 'Could not send this message. Please try again.')
    } finally {
      setIsSendingMessage(false)
    }
  }

  async function openCompose() {
    setRecipient('')
    setSelectedRecipient(null)
    setMessageText('')
    setRecipientError('')
    setConnectedRecipients([])
    setIsRecipientSuggestionsOpen(true)
    setIsComposeOpen(true)
    setIsLoadingRecipients(true)
    try {
      const profiles = await api.getConnectedProfiles()
      if (!Array.isArray(profiles)) {
        throw new Error('Connected profiles returned an invalid response.')
      }
      setConnectedRecipients(profiles.map(normalizeConnectedRecipient).filter(Boolean))
    } catch (error) {
      console.error('Could not load connected recipients:', error)
      setConnectedRecipients([])
      setRecipientError(error instanceof Error
        ? error.message
        : 'Could not load your connections. Please try again.')
    } finally {
      setIsLoadingRecipients(false)
    }
  }

  function selectRecipient(profile) {
    setSelectedRecipient(profile)
    setRecipient(profile.name)
    setRecipientError('')
    setIsRecipientSuggestionsOpen(false)
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
              onClick={openCompose}
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
                    {message.starred && <Star className="message-preview-star" size={14} aria-label="Starred" />}
                    <time
                      dateTime={message.updatedAt || message.thread?.[message.thread.length - 1]?.sentAt}
                      title={getConversationActivityTime(message)
                        ? formatMessageTimestamp(getConversationActivityTime(message))
                        : undefined}
                    >
                      {formatConversationActivity(message, activityClock)}
                    </time>
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
                            disabled={isUpdatingMailbox}
                            onClick={() => moveSelectedChatToMailbox(
                              selectedMessage.mailbox === 'Archived' ? 'Focused' : 'Archived',
                            )}
                          >
                            {isUpdatingMailbox
                              ? selectedMessage.mailbox === 'Archived' ? 'Unarchiving…' : 'Archiving…'
                              : selectedMessage.mailbox === 'Archived' ? 'Unarchive' : 'Archive'}
                          </button>
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
                          <button
                            type="button"
                            role="menuitem"
                            onClick={deleteSelectedChatForMe}
                          >
                            Delete chat for me
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
                      aria-label={selectedMessage.starred ? 'Unstar conversation' : 'Star conversation'}
                      title={selectedMessage.starred ? 'Unstar conversation' : 'Star conversation'}
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
                      const readTime = message.readAt ? formatMessageTime(message.readAt) : ''
                      const isRead = isOutgoing && (
                        String(message.status || '').toUpperCase() === 'READ' || Boolean(message.readAt)
                      )

                      return (
                        <Fragment key={message.id}>
                          {showDate && (
                            <time className="conversation-timestamp" dateTime={message.sentAt}>
                              {formatMessageDate(message.sentAt)}
                            </time>
                          )}
                          <div className={bubbleClassName}>
                            {selectedMessage.isGroupConversation && !isOutgoing && message.senderName && (
                              <span className="conversation-group-sender-name">{message.senderName}</span>
                            )}
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
                              {isOutgoing && (
                                <span
                                  className={`conversation-check${isRead ? ' is-read' : ''}`}
                                  aria-label={isRead ? 'Seen' : 'Sent'}
                                  title={isRead ? 'Seen' : 'Sent'}
                                >
                                  {isRead ? '✓✓' : '✓'}
                                </span>
                              )}
                              {isRead && (
                                <span
                                  className="conversation-read-receipt"
                                  title={message.readAt ? formatMessageTimestamp(message.readAt) : 'Seen'}
                                >
                                  Seen{readTime ? ` ${readTime}` : ''}
                                </span>
                              )}
                            </span>
                          </div>
                        </Fragment>
                      )
                    })}
                  </article>
                )}

                {isConnectPromptOpen
                  && selectedMessage.profile?.networkMessageLimit
                  && !selectedMessage.profile.isConnected && (
                    <section className="message-connect-prompt" aria-label="Connect to continue messaging">
                      <div className="message-connect-prompt-copy">
                        <strong>Connect to keep messaging</strong>
                        <p>
                          {selectedMessage.profile.isPending
                            ? 'Your connection request is pending. You can send more messages after it is accepted.'
                            : 'You can send one message to this profile. Send a connection request to continue.'}
                        </p>
                        {connectionRequestError && (
                          <p className="message-connect-prompt-error" role="alert">
                            {connectionRequestError}
                          </p>
                        )}
                      </div>
                      <div className="message-connect-prompt-actions">
                        {!selectedMessage.profile.isPending && (
                          <button
                            type="button"
                            className="message-connect-prompt-connect"
                            onClick={sendConnectionRequestForConversation}
                            disabled={isSendingConnectionRequest}
                          >
                            {isSendingConnectionRequest ? 'Sending…' : 'Connect'}
                          </button>
                        )}
                        <button
                          type="button"
                          className="message-connect-prompt-dismiss"
                          onClick={() => setIsConnectPromptOpen(false)}
                        >
                          Not now
                        </button>
                      </div>
                    </section>
                  )}
                <form className="message-reply" onSubmit={sendReply}>
                  <label className="message-reply-input">
                    <span className="visually-hidden">Write a message</span>
                    <input
                      ref={replyInputRef}
                      placeholder="Write a message..."
                      value={replyText}
                      onChange={(event) => {
                        setReplyText(event.target.value)
                        setReplyValidationError('')
                      }}
                      onFocus={() => {
                        if (selectedMessage?.id && selectedMessage?.unread) {
                          void markConversationRead(selectedMessage.id)
                        }
                      }}
                    />
                  </label>
                  {replyValidationError && (
                    <p className="message-reply-validation-error" role="alert">
                      {replyValidationError}
                    </p>
                  )}
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
                      aria-label={isSendingReply ? 'Sending message' : 'Send message'}
                      disabled={isSendingReply || (!replyText.trim() && !replyAttachment)}
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
            <label className="compose-recipient-label" htmlFor="compose-recipient">
              <span>To</span>
              <input
                id="compose-recipient"
                ref={recipientInputRef}
                value={recipient}
                autoComplete="off"
                aria-autocomplete="list"
                aria-controls="compose-recipient-results"
                aria-expanded={isRecipientSuggestionsOpen && !selectedRecipient}
                placeholder="Search connected profiles by name"
                onFocus={() => {
                  if (!selectedRecipient) setIsRecipientSuggestionsOpen(true)
                }}
                onChange={(event) => {
                  setRecipient(event.target.value)
                  setSelectedRecipient(null)
                  setRecipientError('')
                  setIsRecipientSuggestionsOpen(true)
                }}
              />
            </label>
            {selectedRecipient && (
              <div className="compose-selected-recipient">
                <span className="compose-recipient-avatar">
                  {selectedRecipient.profileImageUrl
                    ? <img src={selectedRecipient.profileImageUrl} alt="" />
                    : computeInitials(selectedRecipient.name)}
                </span>
                <span className="compose-recipient-name">{selectedRecipient.name}</span>
                <span className="compose-connection-label">Connection</span>
                <button
                  type="button"
                  className="compose-change-recipient"
                  onClick={() => {
                    setSelectedRecipient(null)
                    setRecipient('')
                    setIsRecipientSuggestionsOpen(true)
                    recipientInputRef.current?.focus()
                  }}
                >
                  Change
                </button>
              </div>
            )}
            {isRecipientSuggestionsOpen && !selectedRecipient && (
              <div className="compose-recipient-results" id="compose-recipient-results" role="listbox" aria-label="Connected profiles">
                {isLoadingRecipients ? (
                  <p className="compose-recipient-empty">Loading connections...</p>
                ) : filteredRecipients.length > 0 ? (
                  filteredRecipients.map((profile) => (
                    <button
                      type="button"
                      role="option"
                      aria-selected="false"
                      className="compose-recipient-option"
                      key={profile.userId}
                      onClick={() => selectRecipient(profile)}
                    >
                      <span className="compose-recipient-avatar">
                        {profile.profileImageUrl
                          ? <img src={profile.profileImageUrl} alt="" />
                          : computeInitials(profile.name)}
                      </span>
                      <span className="compose-recipient-option-details">
                        <strong>{profile.name}</strong>
                        {profile.email && <small>{profile.email}</small>}
                      </span>
                    </button>
                  ))
                ) : (
                  <p className="compose-recipient-empty">
                    {!recipient.trim()
                      ? 'Search by name to find a connection.'
                      : connectedRecipients.length
                        ? 'No connected profiles match that name.'
                        : 'No connections are available to message.'}
                  </p>
                )}
              </div>
            )}
            <label>
              Message
              <textarea value={messageText} onChange={(event) => setMessageText(event.target.value)} required rows={5} />
            </label>
            {recipientError && <p className="compose-error" role="alert">{recipientError}</p>}
            <button
              className="compose-send"
              type="submit"
              disabled={!selectedRecipient || !messageText.trim() || isSendingMessage}
            >
              {isSendingMessage ? 'Sending...' : 'Send message'}
            </button>
          </form>
        </div>
      )}
    </main>
  )
}

export default Messages
