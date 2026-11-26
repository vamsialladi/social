import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowLeft, ArrowUpRight, Ban, Bookmark, BriefcaseBusiness, ChevronDown, Eye, EyeOff, Flag, Globe, Link2, LockKeyhole, MessageCircle, MoreVertical, Pencil, Search, Share2, ThumbsUp, Trash2, UsersRound, X } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import './Home.css'
import Article from './Article.jsx'
import MediaSafetyStatus from '../../components/common/MediaSafetyStatus.jsx'
import OpportunitiesPage from './OpportunitiesPage.jsx'
import ProfileBanner from './ProfileBanner.jsx'
import api from '../../api'
import { hasVisitedFilmMaker, markFilmMakerVisited } from '../../services/filmMakerAccess'
import { useMediaSafety } from '../../hooks/useMediaSafety.js'
import { useAuth } from '../../context/AuthContext'

function formatInvestorBudget(value) {
  const budget = String(value || '').trim()
  if (!budget) return ''
  if (budget.startsWith('₹')) return budget
  const amount = budget.replace(/^(?:INR\b|Rs\.?|[$€£¥])\s*/i, '')
  return `₹${amount}`
}

const postBlockReasons = [
  'Spam or unwanted promotion',
  'Harassment or abusive behavior',
  'Fake or misleading profile',
  'Inappropriate content',
  'Privacy or safety concern',
  'Custom reason',
]

const postReportReasons = [
  'Spam, scams or misleading content',
  'Harassment or bullying',
  'Hate speech',
  'Violence or dangerous content',
  'Nudity or sexual content',
  'False information',
  'Other',
]

function postMatchesBlockedUser(post, blockedUsers) {
  const authorIdentifiers = [post.authorId, post.authorEmail, post.authorName]
    .filter(Boolean)
    .map((value) => String(value).trim().toLowerCase())
  return blockedUsers.some((blocked) => {
    const identifiers = typeof blocked === 'string'
      ? [blocked]
      : [blocked.identity, blocked.email, blocked.name]
    return identifiers
      .filter(Boolean)
      .some((value) => authorIdentifiers.includes(String(value).trim().toLowerCase()))
  })
}

function isModellingRequirementPost(post) {
  const details = post?.opportunityDetails
  return Boolean(
    details
    && typeof details === 'object'
    && (
      Object.prototype.hasOwnProperty.call(details, 'modellingCategory')
      || Object.prototype.hasOwnProperty.call(details, 'numberOfModels')
      || Object.prototype.hasOwnProperty.call(details, 'minimumHeight')
    )
  )
}

function ProfileAvatarPlaceholder() {
  return (
    <svg className="home-avatar-placeholder" viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="50" r="50" style={{ fill: '#e5e7e8' }} />
      <circle cx="50" cy="35" r="21" />
      <path d="M10 100c2-24 18-42 40-42s38 18 40 42H10Z" />
    </svg>
  )
}

function getProfileLocationSummary(profile) {
  const structuredParts = [profile?.district, profile?.state, profile?.country]
    .map((part) => String(part || '').trim())
    .filter(Boolean)

  if (structuredParts.length) return structuredParts.join(', ')

  const parts = String(profile?.location || profile?.address || '')
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
  if (parts.length < 2) return ''

  const summaryParts = parts.slice(-3)
  if (/\b(?:old village|street|road|lane|colony|apartment|flat|plot|door no|house no)\b|\d/i.test(summaryParts[0])) {
    summaryParts.shift()
  }
  return summaryParts.join(', ')
}

function getConnectionProfileId(profile) {
  return String(
    profile?.userId
      || profile?.id
      || profile?.profileId
      || profile?.senderId
      || profile?.targetProfileId
      || profile?.user_id
      || profile?._id
    || profile?.email
      || '',
  ).trim()
}

function getConnectionIdentityKeys(profile) {
  return [
    profile?.userId,
    profile?.id,
    profile?.profileId,
    profile?.senderId,
    profile?.targetProfileId,
    profile?.user_id,
    profile?._id,
    profile?.publicUserId,
    profile?.email,
  ]
    .filter(Boolean)
    .map((value) => String(value).trim().toLowerCase())
}

function normalizeConnectionProfile(profile) {
  const profileId = getConnectionProfileId(profile)
  if (!profileId) return null

  const name = String(profile?.name || '').trim() || 'TrueHunt Member'
  const profileImageUrl = profile?.profileImageUrl || profile?.avatar || ''
  const initials = profile?.initials
    || name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()

  return {
    ...profile,
    id: profile?.id || profileId,
    userId: profile?.userId || profile?.id || profileId,
    name,
    initials: initials || 'TH',
    profileImageUrl,
    avatar: profileImageUrl,
  }
}

function getProfilePhotoByName(name, profiles) {
  const normalizedName = String(name || '').trim().toLowerCase()
  if (!normalizedName || ['truehunt member', 'cinema professional', 'member'].includes(normalizedName)) {
    return ''
  }
  const profile = profiles.find((candidate) => (
    [candidate?.name, candidate?.fullName, candidate?.displayName]
      .some((candidateName) => String(candidateName || '').trim().toLowerCase() === normalizedName)
  ))
  return profile?.profileImageUrl || profile?.avatar || profile?.profileImage || ''
}

function isSameConnectionProfile(first, second) {
  const secondKeys = new Set(getConnectionIdentityKeys(second))
  return getConnectionIdentityKeys(first).some((key) => secondKeys.has(key))
}

function getDismissedPostsStorageKey(userId) {
  const accountId = String(userId || 'guest').trim().toLowerCase()
  return `truehunt-not-interested-posts:${encodeURIComponent(accountId)}`
}

function getDismissedInvestorListingsStorageKey(userId) {
  const accountId = String(userId || 'guest').trim().toLowerCase()
  return `truehunt-not-interested-investors:${encodeURIComponent(accountId)}`
}

function getSavedPostsStorageKey(userId) {
  return `truehunt-social-saved-posts:${encodeURIComponent(String(userId || 'me'))}`
}

function readSavedPostIds(storageKey) {
  try {
    const savedPosts = JSON.parse(localStorage.getItem(storageKey) || '[]')
    return Array.isArray(savedPosts) ? savedPosts.map((post) => String(post.id)) : []
  } catch (error) {
    console.warn('Could not read saved feed posts:', error)
    return []
  }
}

function readDismissedPostIds(storageKey) {
  try {
    const savedIds = localStorage.getItem(storageKey)
    const parsedIds = savedIds ? JSON.parse(savedIds) : []
    return Array.isArray(parsedIds) ? parsedIds.map(String) : []
  } catch (error) {
    console.warn('Could not read dismissed feed posts:', error)
    return []
  }
}

function getPostPreviewText(post) {
  const candidates = [
    post.content,
    post.text,
    post.body,
    post.caption,
    post.description,
    post.title,
  ]
  return candidates
    .map((value) => (typeof value === 'string' ? value.trim() : ''))
    .find((value) => value && value.toLowerCase() !== 'post' && value.toLowerCase() !== 'shared a post') || ''
}

function getPostPreviewMedia(post) {
  const media = post.media && typeof post.media === 'object' ? post.media : {}
  const url = media.url || post.mediaUrl || post.imageUrl || post.videoUrl
  if (!url) return null

  const type = String(media.kind || media.type || media.contentType || post.mediaType || '').toLowerCase()
  return {
    url,
    isVideo: type.includes('video'),
    isAudio: type.includes('audio'),
  }
}

function formatOpportunityFieldLabel(field) {
  return field
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]/g, ' ')
    .replace(/^./, (letter) => letter.toUpperCase())
}

function formatOpportunityFieldValue(value, field = '') {
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (Array.isArray(value)) return value.length ? value.join(', ') : 'None selected'
  if (field === 'openingsByGender' && value && typeof value === 'object') {
    return Object.entries(value).map(([gender, count]) => `${gender}: ${count}`).join(' · ')
  }
  if (value && typeof value === 'object') return JSON.stringify(value)
  return value === null || value === undefined || value === '' ? 'Not provided' : String(value)
}

const opportunityDetailGroups = [
  {
    title: 'Project details',
    fields: ['projectName', 'projectType', 'craft', 'openings', 'openingsByGender'],
  },
  {
    title: 'Role and requirements',
    fields: [
      'actingRole',
      'genderPreferences',
      'genderPreference',
      'minimumAge',
      'maximumAge',
      'height',
      'weight',
      'experienceLevel',
      'languagesKnown',
      'otherLanguage',
      'actingSkills',
      'otherActingSkill',
      'locationPreference',
    ],
  },
  {
    title: 'Role-specific details',
    fields: [
      'guardianName',
      'guardianPhone',
      'guardianRelationship',
      'guardianPermission',
      'actingExperience',
      'showreelUrl',
      'actionSkills',
      'negativeCharacterPerformance',
      'comedyTiming',
      'performanceVideoUrl',
      'groupApplications',
      'danceStyles',
      'otherDanceStyle',
    ],
  },
  {
    title: 'Character and audition details',
    fields: [
      'characterDescription',
      'appearanceRequirements',
      'costumeRequirements',
      'specialInstructions',
      'auditionDate',
      'applicationDeadline',
    ],
  },
]

function Home({
  profileDetails,
  profilePreview,
  currentUserId,
  currentUserIdentifiers = [],
  accountEmail,
  onOwnPostCreated,
  activeNavigation,
  setActiveNavigation,
  selectedOpportunityId,
  selectedInvestorId,
  investorRequestOpenSignal,
  hidden,
  onViewProfile,
  onViewConnection,
  onOpenConnections,
  onOpenModellingRequirement,
}) {
  const navigate = useNavigate()
  const { isAuthenticated, loading: authLoading } = useAuth()
  const [posts, setPosts] = useState([])
  const [isPromotionHidden, setIsPromotionHidden] = useState(false)
  const [isHidePromotionPromptOpen, setIsHidePromotionPromptOpen] = useState(false)
  const [postLikesDialog, setPostLikesDialog] = useState(null)
  const [postLikers, setPostLikers] = useState([])
  const [isLoadingPostLikers, setIsLoadingPostLikers] = useState(false)
  const [postLikersError, setPostLikersError] = useState('')
  const [connectingLikerIds, setConnectingLikerIds] = useState([])
  const [likerConnectionErrors, setLikerConnectionErrors] = useState({})
  const [savedPostIds, setSavedPostIds] = useState(() => (
    readSavedPostIds(getSavedPostsStorageKey(currentUserId))
  ))
  const [focusedPostId, setFocusedPostId] = useState('')
  const [openPostMenuId, setOpenPostMenuId] = useState('')
  const [postMenuPosition, setPostMenuPosition] = useState({ top: 0, left: 0 })
  const [blockTarget, setBlockTarget] = useState(null)
  const [blockReason, setBlockReason] = useState('')
  const [customBlockReason, setCustomBlockReason] = useState('')
  const [reportTarget, setReportTarget] = useState(null)
  const [reportReason, setReportReason] = useState('')
  const [customReportReason, setCustomReportReason] = useState('')
  const [reportError, setReportError] = useState('')
  const [isSubmittingReport, setIsSubmittingReport] = useState(false)
  const [selectedOpportunity, setSelectedOpportunity] = useState(null)
  const [dismissedPostIds, setDismissedPostIds] = useState(() => (
    readDismissedPostIds(getDismissedPostsStorageKey(currentUserId))
  ))
  const [dismissedInvestorListingIds, setDismissedInvestorListingIds] = useState(() => (
    readDismissedPostIds(getDismissedInvestorListingsStorageKey(currentUserId))
  ))
  const [profileDirectory, setProfileDirectory] = useState([])
  const [connectedProfiles, setConnectedProfiles] = useState([])
  const [profileViewers, setProfileViewers] = useState([])
  const [investorListings, setInvestorListings] = useState([])
  const [isInvestorSearchOpen, setIsInvestorSearchOpen] = useState(false)
  const [isLoadingInvestorListings, setIsLoadingInvestorListings] = useState(false)
  const [investorListingsError, setInvestorListingsError] = useState('')
  const [connectionsError, setConnectionsError] = useState('')
  const [profileViewersError, setProfileViewersError] = useState('')
  const [isProfileViewersOpen, setIsProfileViewersOpen] = useState(false)
  const [applyingOpportunity, setApplyingOpportunity] = useState(null)
  const [applicationsPost, setApplicationsPost] = useState(null)
  const [opportunityApplications, setOpportunityApplications] = useState([])
  const [isLoadingOpportunityApplications, setIsLoadingOpportunityApplications] = useState(false)
  const [opportunityApplicationsError, setOpportunityApplicationsError] = useState('')
  const [updatingApplicationIds, setUpdatingApplicationIds] = useState([])
  const [applicationStep, setApplicationStep] = useState(1)
  const [applicationData, setApplicationData] = useState({})
  const [isSubmittingApplication, setIsSubmittingApplication] = useState(false)
  const [pendingApplicationPostIds, setPendingApplicationPostIds] = useState([])
  const [applicationError, setApplicationError] = useState('')
  const [isWritingArticle, setIsWritingArticle] = useState(false)
  const [isOpportunityWizardOpen, setIsOpportunityWizardOpen] = useState(false)
  const [editingOpportunity, setEditingOpportunity] = useState(null)
  const [investorInterestStatus, setInvestorInterestStatus] = useState({})
  const [draft, setDraft] = useState('')
  const [draftType, setDraftType] = useState('post')
  const [draftAudience, setDraftAudience] = useState('public')
  const [draftMedia, setDraftMedia] = useState(null)
  const [mediaSelectionError, setMediaSelectionError] = useState('')
  const [editingPost, setEditingPost] = useState(null)
  const [isComposerOpen, setIsComposerOpen] = useState(false)
  const [isAudienceMenuOpen, setIsAudienceMenuOpen] = useState(false)
  const audienceMenuRef = useRef(null)
  const imageInputRef = useRef(null)
  const videoInputRef = useRef(null)
  const mediaUrls = useRef([])
  const mediaSafety = useMediaSafety(draftMedia?.file)

  const name = profileDetails.name.trim() || 'Your name'
  const profileLocation = getProfileLocationSummary(profileDetails)
  const dismissedPostsStorageKey = getDismissedPostsStorageKey(currentUserId)
  const dismissedInvestorListingsStorageKey = getDismissedInvestorListingsStorageKey(currentUserId)
  const savedPostsStorageKey = getSavedPostsStorageKey(currentUserId)
  const accountSettingsStorageKey = `truehunt-social-settings:${String(accountEmail || profileDetails.email || '').trim().toLowerCase()}`
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]
  const isDraftMediaBlocked = Boolean(draftMedia?.file && mediaSafety.isBlocked)

  useEffect(() => () => {
    mediaUrls.current.forEach((url) => URL.revokeObjectURL(url))
  }, [])

  useEffect(() => {
    if (!isAudienceMenuOpen) return undefined
    function closeAudienceMenu(event) {
      if (!audienceMenuRef.current?.contains(event.target)) {
        setIsAudienceMenuOpen(false)
      }
    }
    document.addEventListener('pointerdown', closeAudienceMenu)
    return () => document.removeEventListener('pointerdown', closeAudienceMenu)
  }, [isAudienceMenuOpen])

  useEffect(() => {
    if (!openPostMenuId) return undefined
    function closePostMenu(event) {
      if (!event.target.closest('.home-post-menu')
        && !event.target.closest('.home-post-menu-popover')) {
        setOpenPostMenuId('')
      }
    }
    function closePostMenuOnViewportChange() {
      setOpenPostMenuId('')
    }
    document.addEventListener('pointerdown', closePostMenu)
    window.addEventListener('resize', closePostMenuOnViewportChange)
    window.addEventListener('scroll', closePostMenuOnViewportChange, true)
    return () => {
      document.removeEventListener('pointerdown', closePostMenu)
      window.removeEventListener('resize', closePostMenuOnViewportChange)
      window.removeEventListener('scroll', closePostMenuOnViewportChange, true)
    }
  }, [openPostMenuId])

  useEffect(() => {
    setDismissedPostIds(readDismissedPostIds(dismissedPostsStorageKey))
  }, [dismissedPostsStorageKey])

  useEffect(() => {
    setDismissedInvestorListingIds(readDismissedPostIds(dismissedInvestorListingsStorageKey))
  }, [dismissedInvestorListingsStorageKey])

  useEffect(() => {
    function refreshSavedPostIds() {
      setSavedPostIds(readSavedPostIds(savedPostsStorageKey))
    }
    refreshSavedPostIds()
    window.addEventListener('truehunt-saved-post-changed', refreshSavedPostIds)
    return () => window.removeEventListener('truehunt-saved-post-changed', refreshSavedPostIds)
  }, [savedPostsStorageKey])

  useEffect(() => {
    function handleSocialPostUpdated(event) {
      const updatedPost = event.detail?.post
      if (!updatedPost?.id) return
      setPosts((current) => current.map((post) => (
        String(post.id) === String(updatedPost.id) ? { ...post, ...updatedPost } : post
      )))
    }

    window.addEventListener('truehunt-social-post-updated', handleSocialPostUpdated)
    return () => window.removeEventListener('truehunt-social-post-updated', handleSocialPostUpdated)
  }, [])

  useEffect(() => {
    if (!postLikesDialog) return undefined

    let isCurrent = true
    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function closeOnEscape(event) {
      if (event.key === 'Escape') setPostLikesDialog(null)
    }

    document.addEventListener('keydown', closeOnEscape)
    setPostLikers([])
    setPostLikersError('')

    if (postLikesDialog.localOnly || !/^\d+$/.test(String(postLikesDialog.id))) {
      setIsLoadingPostLikers(false)
      return () => {
        isCurrent = false
        document.removeEventListener('keydown', closeOnEscape)
        document.body.style.overflow = previousBodyOverflow
      }
    }

    async function loadPostLikers() {
      setIsLoadingPostLikers(true)
      try {
        const likers = await api.getPostLikes(postLikesDialog.id)
        if (!Array.isArray(likers)) {
          throw new Error('The likes list returned an invalid response.')
        }
        if (isCurrent) setPostLikers(likers)
      } catch (error) {
        if (isCurrent) {
          setPostLikersError(error instanceof Error
            ? error.message
            : 'Could not load the people who liked this post.')
        }
      } finally {
        if (isCurrent) setIsLoadingPostLikers(false)
      }
    }

    loadPostLikers()
    return () => {
      isCurrent = false
      document.removeEventListener('keydown', closeOnEscape)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [postLikesDialog])

  useEffect(() => {
    let isCurrent = true
    async function loadPosts() {
      try {
        const data = activeNavigation === 'opportunities'
          ? await api.getOpportunities()
          : await api.getPosts()
        if (!isCurrent || !Array.isArray(data)) return

        const dismissedIds = new Set(readDismissedPostIds(dismissedPostsStorageKey))
        let blockedUsers = []
        try {
          const settings = JSON.parse(localStorage.getItem(accountSettingsStorageKey) || '{}')
          blockedUsers = Array.isArray(settings.blockedUsers) ? settings.blockedUsers : []
        } catch (error) {
          console.warn('Could not read blocked profiles for the feed:', error)
        }
        setDismissedPostIds([...dismissedIds])
        const serverSavedIds = data.filter((post) => post?.saved).map((post) => String(post.id))
        if (serverSavedIds.length > 0) {
          setSavedPostIds((prev) => [...new Set([...prev, ...serverSavedIds])])
        }
        setPosts((current) => {
          const localPosts = current.filter((post) => (
            post.localOnly && !postMatchesBlockedUser(post, blockedUsers)
          ))
          const refreshedPosts = data.filter((post) => (
            !dismissedIds.has(String(post.id)) && !postMatchesBlockedUser(post, blockedUsers)
          ))
          const refreshedIds = new Set(refreshedPosts.map((post) => String(post.id)))
          return [...localPosts.filter((post) => !refreshedIds.has(String(post.id))), ...refreshedPosts]
        })
      } catch (err) {
        console.warn('Failed to load posts from API:', err)
      }
    }

    loadPosts()
    const handleFocus = () => {
      if (document.visibilityState === 'visible') loadPosts()
    }
    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleFocus)
    window.addEventListener('truehunt-settings-updated', loadPosts)
    const refreshInterval = activeNavigation === 'home'
      ? window.setInterval(() => {
        if (document.visibilityState === 'visible') loadPosts()
      }, 15000)
      : null
    return () => {
      isCurrent = false
      window.removeEventListener('focus', handleFocus)
      document.removeEventListener('visibilitychange', handleFocus)
      window.removeEventListener('truehunt-settings-updated', loadPosts)
      if (refreshInterval) window.clearInterval(refreshInterval)
    }
  }, [activeNavigation, accountSettingsStorageKey, dismissedPostsStorageKey])

  useEffect(() => {
    if (activeNavigation !== 'opportunities' || !selectedOpportunityId) return

    const opportunity = posts.find((post) => (
      post.type === 'opportunity'
      && String(post.id) === String(selectedOpportunityId)
    ))
    if (opportunity) setSelectedOpportunity(opportunity)
  }, [activeNavigation, posts, selectedOpportunityId])

  useEffect(() => {
    let isCurrent = true
    async function loadProfileDirectory() {
      try {
        const directory = await api.getProfileDirectory()
        if (isCurrent && Array.isArray(directory)) {
          setProfileDirectory(directory)
        }
      } catch (error) {
        console.warn('Could not load profile photos for feed posts:', error)
      }
    }

    loadProfileDirectory()
    return () => {
      isCurrent = false
    }
  }, [])

  function getPostAuthorAvatar(post) {
    const authorId = String(post.authorId || '').trim().toLowerCase()
    const authorName = String(post.authorName || '').trim().toLowerCase()
    const authorProfile = profileDirectory.find((profile) => (
      authorId && [
        profile.userId,
        profile.publicUserId,
        profile.id,
        profile.referId,
        profile.user_id,
        profile._id,
      ].some((id) => String(id || '').trim().toLowerCase() === authorId)
    )) || profileDirectory.find((profile) => (
      authorName && String(profile.name || '').trim().toLowerCase() === authorName
    ))

    const directoryAvatar = authorProfile?.profileImageUrl
      || authorProfile?.avatar
      || authorProfile?.profileImage
      || ''
    if (directoryAvatar) return directoryAvatar

    const ownProfileIds = [currentUserId, profileDetails.userId, profileDetails.id]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
    if (authorId && ownProfileIds.includes(authorId)) return profilePreview || post.authorAvatar || ''

    return post.authorAvatar || ''
  }

  useEffect(() => {
    if (activeNavigation !== 'home') return undefined

    let isCurrent = true
    async function loadConnections() {
      setConnectionsError('')
      try {
        const data = await api.getConnectedProfiles()
        if (isCurrent) {
          const uniqueConnections = new Map()
          ;(Array.isArray(data) ? data : []).forEach((profile) => {
            const normalizedProfile = normalizeConnectionProfile(profile)
            if (!normalizedProfile) return
            const existing = Array.from(uniqueConnections.values())
              .find((item) => isSameConnectionProfile(item, normalizedProfile))
            if (existing) {
              uniqueConnections.set(getConnectionProfileId(existing), {
                ...existing,
                ...normalizedProfile,
                name: normalizedProfile.name === 'TrueHunt Member'
                  ? existing.name
                  : normalizedProfile.name,
                profileImageUrl: normalizedProfile.profileImageUrl || existing.profileImageUrl,
                avatar: normalizedProfile.profileImageUrl || existing.avatar,
              })
            } else {
              uniqueConnections.set(getConnectionProfileId(normalizedProfile), normalizedProfile)
            }
          })
          setConnectedProfiles(Array.from(uniqueConnections.values()))
        }
      } catch (error) {
        console.error('Failed to load connections:', error)
        if (isCurrent) setConnectionsError('Could not load your connections. Please try again.')
      }
    }

    loadConnections()
    window.addEventListener('truehunt-settings-updated', loadConnections)
    window.addEventListener('truehunt-connection-changed', loadConnections)
    return () => {
      isCurrent = false
      window.removeEventListener('truehunt-settings-updated', loadConnections)
      window.removeEventListener('truehunt-connection-changed', loadConnections)
    }
  }, [activeNavigation, accountEmail, currentUserId])

  useEffect(() => {
    if (activeNavigation !== 'home') return undefined

    let isCurrent = true
    async function loadProfileViewers() {
      setProfileViewersError('')
      try {
        const data = await api.getProfileViewers()
        if (!Array.isArray(data)) {
          throw new Error('Profile viewers returned an invalid response.')
        }
        if (isCurrent) setProfileViewers(data)
      } catch (error) {
        console.error('Failed to load profile viewers:', error)
        if (isCurrent) setProfileViewersError('Could not load profile viewers. Please try again.')
      }
    }

    loadProfileViewers()
    const refreshInterval = window.setInterval(loadProfileViewers, 30000)
    return () => {
      isCurrent = false
      window.clearInterval(refreshInterval)
    }
  }, [activeNavigation, currentUserId])

  useEffect(() => {
    function handleConnectionChanged(event) {
      const detail = event.detail || {}
      const {
        connected: connectedValue,
        isConnected,
        pending: pendingValue,
        isPending,
        senderId,
        receiverId,
        senderProfile,
        receiverProfile,
      } = detail
      const connected = typeof connectedValue === 'boolean' ? connectedValue : Boolean(isConnected)
      const pending = typeof pendingValue === 'boolean' ? pendingValue
        : typeof isPending === 'boolean' ? isPending : false
      const currentIds = new Set([
        currentUserId,
        profileDetails.userId,
        profileDetails.id,
        profileDetails.email,
        accountEmail,
      ].filter(Boolean).map((id) => String(id).trim().toLowerCase()))
      const matchesCurrentUser = (value) => getConnectionIdentityKeys(value)
        .some((key) => currentIds.has(key))
        || currentIds.has(String(value || '').trim().toLowerCase())
      const hasParticipants = Boolean(senderId || receiverId || senderProfile || receiverProfile)
      let targetProfile
      if (hasParticipants) {
        const isSender = matchesCurrentUser(senderProfile) || matchesCurrentUser(senderId)
        const isReceiver = matchesCurrentUser(receiverProfile) || matchesCurrentUser(receiverId)
        if (isSender === isReceiver) return
        targetProfile = isSender
          ? receiverProfile || { id: receiverId }
          : senderProfile || { id: senderId }
      } else {
        targetProfile = detail.profile
          || (detail.profileId ? { id: detail.profileId } : null)
      }
      if (!targetProfile || (!connected && pending)) return

      const profile = normalizeConnectionProfile(
        targetProfile,
      )
      if (!profile) return

      setConnectedProfiles((current) => {
        const existing = current.find((item) => isSameConnectionProfile(item, profile))
        if (!connected) return current.filter((item) => !isSameConnectionProfile(item, profile))
        if (existing) {
          return current.map((item) => (
            isSameConnectionProfile(item, profile)
              ? {
                  ...item,
                  ...profile,
                  name: profile.name === 'TrueHunt Member' ? item.name : profile.name,
                  profileImageUrl: profile.profileImageUrl || item.profileImageUrl,
                  avatar: profile.profileImageUrl || item.avatar,
                }
              : item
          ))
        }
        return [...current, profile]
      })
    }

    window.addEventListener('truehunt-connection-changed', handleConnectionChanged)
    return () => window.removeEventListener('truehunt-connection-changed', handleConnectionChanged)
  }, [accountEmail, currentUserId, profileDetails.email, profileDetails.id, profileDetails.userId])

  function selectMedia(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    if (draftMedia?.file) URL.revokeObjectURL(draftMedia.url)
    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
      setMediaSelectionError('Choose an image or video to attach to your post.')
      setDraftMedia(null)
      return
    }
    setMediaSelectionError('')
    setDraftMedia({
      file,
      url: URL.createObjectURL(file),
      kind: file.type.startsWith('video/') ? 'video' : 'image',
    })
    setIsComposerOpen(true)
  }

  function removeDraftMedia() {
    if (draftMedia?.file) URL.revokeObjectURL(draftMedia.url)
    setDraftMedia(null)
    setMediaSelectionError('')
  }

  function startEditingPost(post) {
    if (draftMedia?.file) URL.revokeObjectURL(draftMedia.url)
    const media = getPostPreviewMedia(post)
    setEditingPost(post)
    setDraft(post.content || post.text || post.body || post.caption || post.description || '')
    setDraftType(post.type || 'post')
    setDraftAudience(post.audience || 'public')
    setDraftMedia(media
      ? {
        url: media.url,
        kind: media.isVideo ? 'video' : media.isAudio ? 'audio' : 'image',
        isExisting: true,
      }
      : null)
    setIsAudienceMenuOpen(false)
    setIsComposerOpen(true)
    requestAnimationFrame(() => {
      document.querySelector('.home-composer')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }

  function cancelEditingPost() {
    if (draftMedia?.file) URL.revokeObjectURL(draftMedia.url)
    setDraft('')
    setDraftMedia(null)
    setDraftType('post')
    setDraftAudience('public')
    setEditingPost(null)
    setIsAudienceMenuOpen(false)
    setIsComposerOpen(false)
  }

  async function publishPost(event) {
    event.preventDefault()
    const content = draft.trim()
    if (!content && !draftMedia) return
    if (isDraftMediaBlocked) return

    let uploadedMedia = null
    if (draftMedia?.file) {
      try {
        const res = await api.uploadMedia(draftMedia.file)
        uploadedMedia = {
          url: res.url,
          fileName: res.fileName,
          contentType: res.contentType,
          kind: res.kind,
        }
      } catch (err) {
        console.warn('Failed to upload post media:', err)
        if (err.code === 'MEDIA_CONTENT_REJECTED' || err.code === 'MEDIA_SCAN_FAILED') {
          setMediaSelectionError(err.message || 'The selected image could not be approved.')
          return
        }
        if (editingPost) {
          window.alert('Could not upload the attachment. Your post was not updated.')
          return
        }
      }
    }

    if (editingPost) {
      try {
        const media = uploadedMedia || (draftMedia
          ? { url: draftMedia.url, kind: draftMedia.kind }
          : null)
        const response = editingPost.localOnly
          ? {}
          : await api.updatePost(editingPost.id, {
              content,
              title: editingPost.title || null,
              type: draftType,
              media,
              audience: draftAudience,
            })
        const updated = {
          ...editingPost,
          ...response,
          id: editingPost.id,
          authorId: response.authorId || editingPost.authorId || currentUserId,
          authorAvatar: response.authorAvatar || editingPost.authorAvatar || profilePreview,
          content,
          type: draftType,
          media: media || response.media || editingPost.media,
          audience: draftAudience,
        }
        setPosts((currentPosts) => currentPosts.map((post) => (
          String(post.id) === String(updated.id) ? updated : post
        )))
        onOwnPostCreated?.(updated)
        window.dispatchEvent(new CustomEvent('truehunt-social-post-updated', {
          detail: { post: updated },
        }))

        try {
          const savedPosts = JSON.parse(localStorage.getItem(savedPostsStorageKey) || '[]')
          if (!Array.isArray(savedPosts)) throw new Error('Saved posts are not a list.')
          const nextSavedPosts = savedPosts.map((post) => (
            String(post.id) === String(updated.id) ? { ...post, ...updated } : post
          ))
          if (nextSavedPosts.some((post, index) => post !== savedPosts[index])) {
            localStorage.setItem(savedPostsStorageKey, JSON.stringify(nextSavedPosts))
            window.dispatchEvent(new CustomEvent('truehunt-saved-post-changed'))
          }
        } catch (error) {
          console.warn('Could not update the saved copy of this post:', error)
        }
      } catch (err) {
        console.error('Could not update post:', err)
        window.alert(err instanceof Error ? err.message : 'Could not update this post. Please try again.')
        return
      }
    } else {
      try {
        const response = await api.createPost({
          content,
          type: draftType,
          media: uploadedMedia,
          audience: draftAudience,
        })
        const created = {
          ...response,
          authorId: response.authorId || currentUserId,
          authorAvatar: response.authorAvatar || profilePreview,
          audience: draftAudience,
        }
        setPosts((currentPosts) => [created, ...currentPosts])
        onOwnPostCreated?.(created)
      } catch (err) {
        console.warn('Backend post creation note:', err)
        if (draftMedia) mediaUrls.current.push(draftMedia.url)
        const created = {
          id: `${Date.now()}`,
          authorId: currentUserId,
          authorAvatar: profilePreview,
          authorName: name,
          localOnly: true,
          content,
          type: draftType,
          audience: draftAudience,
          media: draftMedia,
          createdAt: new Date().toLocaleString(),
        }
        setPosts((currentPosts) => [created, ...currentPosts])
        onOwnPostCreated?.(created)
      }
    }

    setDraft('')
    setDraftMedia(null)
    setDraftType('post')
    setDraftAudience('public')
    setEditingPost(null)
    setIsAudienceMenuOpen(false)
    setIsComposerOpen(false)
  }

  function startOpportunityPost() {
    setEditingOpportunity(null)
    setIsOpportunityWizardOpen(true)
  }

  function startPost() {
    if (isComposerOpen) {
      setIsAudienceMenuOpen(false)
      setIsComposerOpen(false)
      return
    }

    if (!editingPost) {
      setDraftType(activeNavigation === 'opportunities' ? 'opportunity' : 'post')
      setDraftAudience('public')
    }
    setIsAudienceMenuOpen(false)
    setIsComposerOpen(true)
  }

  async function publishArticle(article) {
    let uploadedMedia = null
    if (article.media?.file) {
      try {
        const res = await api.uploadMedia(article.media.file)
        uploadedMedia = {
          url: res.url,
          fileName: res.fileName,
          contentType: res.contentType,
          kind: res.kind,
        }
      } catch (err) {
        console.warn('Failed to upload article media:', err)
        if (err.code === 'MEDIA_CONTENT_REJECTED' || err.code === 'MEDIA_SCAN_FAILED') {
          throw err
        }
      }
    }

    try {
      const response = await api.createPost({
        title: article.title,
        content: article.content,
        type: 'article',
        media: uploadedMedia,
      })
      const created = {
        ...response,
        authorId: response.authorId || currentUserId,
        authorAvatar: response.authorAvatar || profilePreview,
      }
      setPosts((currentPosts) => [created, ...currentPosts])
      onOwnPostCreated?.(created)
    } catch (err) {
      console.warn('Backend article creation note:', err)
      const created = {
        id: `${Date.now()}`,
        ...article,
        authorId: currentUserId,
        authorAvatar: profilePreview,
        authorName: name,
        localOnly: true,
        type: 'article',
        createdAt: new Date().toLocaleString(),
      }
      setPosts((currentPosts) => [created, ...currentPosts])
      onOwnPostCreated?.(created)
    }
    setIsWritingArticle(false)
  }

  async function publishOpportunity(form) {
    const content = [
      form.projectType,
      form.actingRole,
      form.locationPreference,
    ].filter(Boolean).join(' · ')
    const opportunity = {
      title: form.projectName,
      content,
      opportunityDetails: form,
    }
    const response = editingOpportunity
      ? await api.updateOpportunity(editingOpportunity.id, opportunity)
      : await api.createPost({ ...opportunity, type: 'opportunity' })
    const created = {
      ...(editingOpportunity || {}),
      ...response,
      authorId: response.authorId || currentUserId,
      authorAvatar: response.authorAvatar || profilePreview,
      type: 'opportunity',
      opportunityDetails: response.opportunityDetails || form,
    }

    setPosts((currentPosts) => editingOpportunity
      ? currentPosts.map((post) => String(post.id) === String(created.id) ? created : post)
      : [created, ...currentPosts])
    if (!editingOpportunity) onOwnPostCreated?.(created)
    setEditingOpportunity(null)
    setIsOpportunityWizardOpen(false)
  }

  function startEditingOpportunity(post) {
    setSelectedOpportunity(null)
    setEditingOpportunity(post)
    setIsOpportunityWizardOpen(true)
  }

  const [openCommentsPostId, setOpenCommentsPostId] = useState(null)
  const [commentText, setCommentText] = useState('')
  const [commentErrors, setCommentErrors] = useState({})
  const [isCommentSubmitting, setIsCommentSubmitting] = useState(false)
  const [replyingToCommentKey, setReplyingToCommentKey] = useState('')
  const [replyText, setReplyText] = useState('')
  const [replyErrors, setReplyErrors] = useState({})
  const [commentLikeErrors, setCommentLikeErrors] = useState({})
  const [commentDeleteErrors, setCommentDeleteErrors] = useState({})
  const [pendingReplyKey, setPendingReplyKey] = useState('')
  const [pendingCommentLikeKeys, setPendingCommentLikeKeys] = useState([])
  const [pendingCommentDeleteKeys, setPendingCommentDeleteKeys] = useState([])
  const [expandedCommentPostIds, setExpandedCommentPostIds] = useState([])
  const [pendingLikePostIds, setPendingLikePostIds] = useState([])
  const [likeErrors, setLikeErrors] = useState({})
  const [isInvestorRequestOpen, setIsInvestorRequestOpen] = useState(false)
  const [investorRequest, setInvestorRequest] = useState({
    name: '',
    userId: '',
    budget: '',
    role: 'Investor',
    description: '',
    consentAccepted: false,
  })
  const [filmMakerVisited, setFilmMakerVisited] = useState(() => hasVisitedFilmMaker(accountEmail))
  const [investorRequestStatus, setInvestorRequestStatus] = useState('')
  const [investorRequestError, setInvestorRequestError] = useState('')
  const [isSubmittingInvestorRequest, setIsSubmittingInvestorRequest] = useState(false)

  useEffect(() => {
    let isCurrent = true
    api.getInvestorAccessStatus()
      .then((result) => {
        if (isCurrent) setInvestorRequestStatus(result.status || 'NONE')
      })
      .catch((error) => {
        console.error('Could not load investor access status:', error)
      })
    return () => {
      isCurrent = false
    }
  }, [])

  useEffect(() => {
    function handleInvestorProfileDeleted(event) {
      const { feedPostId, investorListingId } = event.detail || {}
      if (feedPostId) {
        setPosts((current) => current.filter((post) => String(post.id) !== String(feedPostId)))
      }
      if (investorListingId) {
        setInvestorListings((current) => current.filter((listing) => String(listing.id) !== String(investorListingId)))
      }
      setInvestorRequest({
        name: profileDetails.name || '',
        userId: profileDetails.userId || currentUserId || '',
        budget: '',
        role: 'Investor',
        description: '',
        consentAccepted: false,
      })
      setInvestorRequestStatus('NONE')
      setInvestorRequestError('')
      setIsInvestorSearchOpen(false)
      setActiveNavigation('home')
      setIsInvestorRequestOpen(true)
    }

    window.addEventListener('truehunt-investor-profile-deleted', handleInvestorProfileDeleted)
    return () => window.removeEventListener('truehunt-investor-profile-deleted', handleInvestorProfileDeleted)
  }, [currentUserId, profileDetails.name, profileDetails.userId, setActiveNavigation])

  useEffect(() => {
    function handleSocialNavigationChanged() {
      setIsInvestorSearchOpen(false)
    }

    window.addEventListener('truehunt-social-navigation-changed', handleSocialNavigationChanged)
    return () => window.removeEventListener('truehunt-social-navigation-changed', handleSocialNavigationChanged)
  }, [])

  const openInvestorRequest = useCallback(() => {
    setInvestorRequest({
      name: profileDetails.name || '',
      userId: profileDetails.userId || currentUserId || '',
      budget: '',
      role: 'Investor',
      description: '',
      consentAccepted: false,
    })
    setInvestorRequestError('')
    setIsInvestorRequestOpen(true)
    api.getInvestorAccessStatus()
      .then((result) => {
        setInvestorRequestStatus(result.status || 'NONE')
        if (result.status === 'PUBLISHED') {
          setInvestorRequest((current) => ({
            ...current,
            name: result.name || current.name,
            budget: result.budget || '',
            role: result.role || 'Investor',
            description: result.description || '',
          }))
        }
      })
      .catch((error) => {
        console.error('Could not load investor access status:', error)
        setInvestorRequestError('Could not verify your current request status. Please try again.')
      })
  }, [currentUserId, profileDetails.name, profileDetails.userId])

  useEffect(() => {
    if (investorRequestOpenSignal) openInvestorRequest()
  }, [investorRequestOpenSignal, openInvestorRequest])

  const openInvestorSearch = useCallback(async () => {
    setIsInvestorSearchOpen(true)
    setIsLoadingInvestorListings(true)
    setInvestorListingsError('')
    try {
      const listings = await api.getInvestorListings()
      if (!Array.isArray(listings)) {
        throw new Error('Investor search returned an invalid response.')
      }
      setInvestorListings(listings)
    } catch (error) {
      console.error('Could not load investor listings:', error)
      setInvestorListingsError(error instanceof Error
        ? error.message
        : 'Could not load investor listings. Please try again.')
    } finally {
      setIsLoadingInvestorListings(false)
    }
  }, [])

  useEffect(() => {
    if (selectedInvestorId) void openInvestorSearch()
  }, [openInvestorSearch, selectedInvestorId])

  useEffect(() => {
    if (!selectedInvestorId || !isInvestorSearchOpen || isLoadingInvestorListings) return
    const listingElement = document.getElementById(
      `social-investor-listing-${encodeURIComponent(String(selectedInvestorId))}`,
    )
    listingElement?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [investorListings, isInvestorSearchOpen, isLoadingInvestorListings, selectedInvestorId])

  async function submitInvestorRequest(event) {
    event.preventDefault()
    setInvestorRequestError('')
    setIsSubmittingInvestorRequest(true)
    try {
      const result = await api.submitInvestorAccessRequest(investorRequest)
      setInvestorRequestStatus(result.status || 'PUBLISHED')
      if (result.feedPost) {
        setPosts((current) => [
          result.feedPost,
          ...current.filter((post) => String(post.id) !== String(result.feedPost.id)),
        ])
        onOwnPostCreated?.(result.feedPost)
      }
      setIsInvestorRequestOpen(false)
      setIsInvestorSearchOpen(false)
      setActiveNavigation('home')
    } catch (error) {
      console.error('Could not submit investor access request:', error)
      setInvestorRequestError(error instanceof Error
        ? error.message
        : 'Could not submit your request. Please try again.')
    } finally {
      setIsSubmittingInvestorRequest(false)
    }
  }

  async function handleLike(post) {
    const postId = String(post.id)
    if (pendingLikePostIds.includes(postId)) return

    const wasLiked = Boolean(post.liked)
    const canShowCount = shouldShowPostLikeCount(post)
    const previousCount = Number(post.likesCount) || 0
    const optimisticLike = {
      liked: !wasLiked,
      likesCount: canShowCount ? Math.max(0, previousCount + (wasLiked ? -1 : 1)) : 0,
      showLikesCount: canShowCount,
      likeCountDisabled: !canShowCount,
    }

    setLikeErrors((current) => {
      const next = { ...current }
      delete next[postId]
      return next
    })
    setPosts((current) => current.map((item) => (
      String(item.id) === postId ? { ...item, ...optimisticLike } : item
    )))

    if (post.localOnly) return

    setPendingLikePostIds((current) => [...current, postId])
    try {
      const updated = await api.likePost(postId)
      setPosts((current) => current.map((item) => {
        if (String(item.id) !== postId) return item
        const merged = { ...item, ...updated }
        if (!shouldShowPostLikeCount(merged)) {
          merged.likesCount = 0
          merged.showLikesCount = false
          merged.likeCountDisabled = true
        }
        return merged
      }))
    } catch (err) {
      console.warn('Like error:', err)
      setPosts((current) => current.map((item) => (
        String(item.id) === postId
          ? { ...item, liked: wasLiked, likesCount: canShowCount ? previousCount : 0 }
          : item
      )))
      setLikeErrors((current) => ({
        ...current,
        [postId]: err instanceof Error ? err.message : 'Could not update this like. Please try again.',
      }))
    } finally {
      setPendingLikePostIds((current) => current.filter((id) => id !== postId))
    }
  }

  async function connectWithLiker(liker) {
    const profileId = getConnectionProfileId(liker)
    if (!profileId || connectingLikerIds.includes(profileId)) return

    setLikerConnectionErrors((current) => {
      const next = { ...current }
      delete next[profileId]
      return next
    })
    setConnectingLikerIds((current) => [...current, profileId])
    try {
      const result = await api.sendConnectionRequest(profileId)
      const connected = Boolean(result?.connected)
      const pending = Boolean(result?.isPending)
      const updatedLiker = {
        ...liker,
        isConnected: connected,
        isPending: pending,
        isOutgoingPending: pending,
      }
      setPostLikers((current) => current.map((profile) => (
        isSameConnectionProfile(profile, liker) ? { ...profile, ...updatedLiker } : profile
      )))
      if (connected) {
        const normalizedLiker = normalizeConnectionProfile(updatedLiker)
        if (normalizedLiker) {
          setConnectedProfiles((current) => current.some((profile) => (
            isSameConnectionProfile(profile, normalizedLiker)
          )) ? current : [...current, normalizedLiker])
        }
      }
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected, pending, profile: updatedLiker },
      }))
    } catch (error) {
      console.error('Could not connect with a post liker:', error)
      setLikerConnectionErrors((current) => ({
        ...current,
        [profileId]: error instanceof Error ? error.message : 'Could not send this connection request.',
      }))
    } finally {
      setConnectingLikerIds((current) => current.filter((id) => id !== profileId))
    }
  }

  async function handleApply(postId, details = null) {
    const updated = await api.applyToOpportunity(postId, details)
    setPosts((current) => current.map((post) => (
      String(post.id) === String(postId) ? { ...post, ...updated } : post
    )))
  }

  function openOpportunityApplication(post) {
    if (post.applied) {
      void handleCancelApplication(post)
      return
    }

    setApplicationError('')
    setApplicationStep(1)
    setApplicationData({
      fullName: profileDetails.name || '',
      age: '',
      gender: profileDetails.gender || '',
      mobileNumber: profileDetails.phone || profileDetails.professionalPhone || '',
      emailAddress: profileDetails.email || accountEmail || '',
      currentLocation: profileDetails.address || '',
      languagesKnown: [
        ...(profileDetails.languages || []),
        ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
      ].filter((language, index, values) => values.indexOf(language) === index).join(', '),
      height: profileDetails.height || '',
      weight: profileDetails.weight || '',
      bodyType: profileDetails.bodyBuild || '',
      experienceLevel: profileDetails.experience || '',
      previousWork: '',
      actingSkills: '',
      profilePhotoUrl: profileDetails.profileImageUrl || profileDetails.avatar || '',
      fullLengthPhotoUrl: '',
      portfolioUrl: '',
      introductionVideoUrl: '',
      auditionVideoUrl: '',
      accurateInformation: false,
      termsAccepted: false,
    })
    setApplyingOpportunity(post)
  }

  async function openOpportunityApplications(post) {
    setApplicationsPost(post)
    setOpportunityApplications([])
    setOpportunityApplicationsError('')
    setIsLoadingOpportunityApplications(true)
    try {
      const applications = await api.getOpportunityApplications(post.id)
      if (!Array.isArray(applications)) {
        throw new Error('The applications service returned an invalid response.')
      }
      setOpportunityApplications(applications)
    } catch (error) {
      console.error('Could not load casting applications:', error)
      setOpportunityApplicationsError(error instanceof Error
        ? error.message
        : 'Could not load applications. Please try again.')
    } finally {
      setIsLoadingOpportunityApplications(false)
    }
  }

  async function updateApplicationShortlist(application, shortlisted) {
    const applicationId = String(application.id)
    setOpportunityApplicationsError('')
    setUpdatingApplicationIds((current) => [...current, applicationId])
    try {
      const updated = await api.updateOpportunityApplicationShortlist(
        applicationsPost.id,
        applicationId,
        shortlisted,
      )
      setOpportunityApplications((current) => current.map((entry) => (
        String(entry.id) === applicationId
          ? {
              ...entry,
              shortlisted: Boolean(updated.shortlisted),
              shortlistDecisionMade: true,
            }
          : entry
      )))
    } catch (error) {
      console.error('Could not update application shortlist status:', error)
      setOpportunityApplicationsError(error instanceof Error
        ? error.message
        : 'Could not update shortlist status. Please try again.')
    } finally {
      setUpdatingApplicationIds((current) => current.filter((id) => id !== applicationId))
    }
  }

  function updateApplicationData(field, value) {
    setApplicationData((current) => ({ ...current, [field]: value }))
  }

  function goToNextApplicationStep() {
    if (applicationStep === 3 && (!applicationData.accurateInformation || !applicationData.termsAccepted)) {
      setApplicationError('Please confirm both declarations before continuing.')
      return
    }

    setApplicationError('')
    setApplicationStep((step) => step + 1)
  }

  function getInvestorInterestKey(post) {
    return String(post.investorProfileId || post.id)
  }

  async function sendInvestorInterest(post) {
    const interestKey = getInvestorInterestKey(post)
    const status = investorInterestStatus[interestKey]?.status
    if (status === 'sending' || status === 'sent') return

    const investorProfileId = String(post.investorProfileId || post.id || '').trim()
    if (!investorProfileId) {
      setInvestorInterestStatus((current) => ({
        ...current,
        [interestKey]: {
          status: 'error',
          message: 'Could not identify the investor profile owner.',
        },
      }))
      return
    }

    if (!isAuthenticated) {
      navigate('/sign-in?role=STUDIO', {
        state: {
          returnTo: `/social/opportunities?investorId=${encodeURIComponent(investorProfileId)}`,
        },
      })
      return
    }

    setInvestorInterestStatus((current) => ({
      ...current,
      [interestKey]: { status: 'sending', message: '' },
    }))

    try {
      const interest = await api.sendInvestorInterest(investorProfileId)
      const conversation = interest?.conversation
      const messageText = interest?.message
      const messageWasSent = Array.isArray(conversation?.thread)
        && messageText
        && conversation.thread.some((message) => message.content === messageText)
      if (!conversation?.id || !messageWasSent) {
        throw new Error('Could not send your interest message. Please try again.')
      }

      setInvestorInterestStatus((current) => ({
        ...current,
        [interestKey]: { status: 'sent', message: '' },
      }))
      setActiveNavigation('notifications')
    } catch (error) {
      console.error('Could not send investor interest message:', error)
      setInvestorInterestStatus((current) => ({
        ...current,
        [interestKey]: {
          status: 'error',
          message: error instanceof Error
            ? error.message
            : 'Could not send your interest message. Please try again.',
        },
      }))
    }
  }

  async function handleCancelApplication(post) {
    if (!window.confirm('Do you want to cancel your application?')) return

    const postId = String(post.id)
    setPendingApplicationPostIds((current) => [...current, postId])
    try {
      const updated = await api.cancelOpportunityApplication(postId)
      setPosts((current) => current.map((item) => (
        String(item.id) === postId ? { ...item, ...updated } : item
      )))
    } catch (error) {
      console.error('Failed to cancel opportunity application:', error)
      window.alert(error instanceof Error
        ? error.message
        : 'Could not cancel your application. Please try again.')
    } finally {
      setPendingApplicationPostIds((current) => current.filter((id) => id !== postId))
    }
  }

  async function submitOpportunityApplication(event) {
    event.preventDefault()
    if (!applyingOpportunity || isSubmittingApplication || applicationStep !== 4) return
    if (!applicationData.accurateInformation || !applicationData.termsAccepted) {
      setApplicationError('Please confirm both declarations before submitting.')
      return
    }

    setApplicationError('')
    setIsSubmittingApplication(true)
    try {
      await handleApply(applyingOpportunity.id, {
        applicantName: applicationData.fullName.trim(),
        experience: applicationData.experienceLevel.trim(),
        note: JSON.stringify(applicationData),
      })
      setApplyingOpportunity(null)
    } catch (error) {
      console.error('Failed to apply for opportunity:', error)
      setApplicationError(error instanceof Error
        ? error.message
        : 'Could not submit your application. Please try again.')
    } finally {
      setIsSubmittingApplication(false)
    }
  }

  async function handleDelete(postId) {
    try {
      const postToDelete = posts.find((post) => String(post.id) === String(postId))
      if (!postToDelete?.localOnly) {
        await api.deletePost(postId)
      }

      setPosts((current) => current.filter((p) => String(p.id) !== String(postId)))
      setSelectedOpportunity((current) => (
        String(current?.id) === String(postId) ? null : current
      ))

      const ownerId = String(currentUserId || 'me').trim()
      const profilePostsStorageKey = `truehunt-social-posts:${encodeURIComponent(ownerId)}`
      try {
        const savedPosts = localStorage.getItem(profilePostsStorageKey)
        const parsedPosts = savedPosts ? JSON.parse(savedPosts) : []
        if (!Array.isArray(parsedPosts)) {
          throw new Error('Saved profile posts are not a list.')
        }
        localStorage.setItem(
          profilePostsStorageKey,
          JSON.stringify(parsedPosts.filter((post) => String(post.id) !== String(postId))),
        )
      } catch (error) {
        console.warn('Could not update saved profile posts after feed deletion:', error)
      }

      window.dispatchEvent(new CustomEvent('truehunt-social-post-deleted', {
        detail: { postId: String(postId), profileId: ownerId },
      }))
      return true
    } catch (err) {
      console.error('Delete error:', err)
      window.alert(err instanceof Error ? err.message : 'Could not delete this post. Please try again.')
      return false
    }
  }

  async function handleDeleteInvestorListing(listing) {
    if (!window.confirm('Delete your investor profile and its home-feed post permanently?')) return
    try {
      await api.deleteInvestorListing(listing.id)
    } catch (error) {
      console.error('Investor profile delete error:', error)
      window.alert(error instanceof Error
        ? error.message
        : 'Could not delete your investor profile. Please try again.')
    }
  }

  function handleNotInterested(postId) {
    const postIdString = String(postId)
    const dismissedIds = new Set(readDismissedPostIds(dismissedPostsStorageKey))
    dismissedIds.add(postIdString)
    try {
      localStorage.setItem(dismissedPostsStorageKey, JSON.stringify([...dismissedIds]))
    } catch (error) {
      console.warn('Could not save dismissed feed post:', error)
    }
    setDismissedPostIds([...dismissedIds])
    setPosts((current) => current.filter((post) => String(post.id) !== postIdString))
    if (isAuthenticated && /^\d+$/.test(postIdString)) {
      api.markPostNotInterested(postIdString).catch((err) => {
        console.warn('Could not persist not-interested post on backend:', err)
      })
    }
  }

  function handleSavePost(post) {
    const postId = String(post.id)
    let isAlreadySaved
    try {
      const savedPosts = JSON.parse(localStorage.getItem(savedPostsStorageKey) || '[]')
      const existing = Array.isArray(savedPosts) ? savedPosts : []
      isAlreadySaved = existing.some((saved) => String(saved.id) === postId)
      const next = isAlreadySaved
        ? existing.filter((saved) => String(saved.id) !== postId)
        : [post, ...existing]
      localStorage.setItem(savedPostsStorageKey, JSON.stringify(next))
      setSavedPostIds(next.map((saved) => String(saved.id)))
      window.dispatchEvent(new CustomEvent('truehunt-saved-post-changed'))
    } catch (error) {
      console.warn('Could not save feed post:', error)
      setOpenPostMenuId('')
      return
    }
    if (isAuthenticated && !post?.localOnly && /^\d+$/.test(postId)) {
      const request = isAlreadySaved ? api.unsavePost(postId) : api.savePost(postId)
      request.catch((err) => {
        console.warn('Could not sync saved post on backend:', err)
      })
    }
    setOpenPostMenuId('')
  }

  async function handleCopyPostLink(postId) {
    const link = `${window.location.origin}/social/home?postId=${encodeURIComponent(String(postId))}`
    try {
      await navigator.clipboard.writeText(link)
    } catch {
      window.prompt('Copy this post link:', link)
    }
    setOpenPostMenuId('')
  }

  async function recordPostShare(post) {
    if (!post?.localOnly && /^\d+$/.test(String(post.id))) {
      try {
        const res = await api.sharePost(post.id)
        if (res) {
          setPosts((current) => current.map((item) => {
            if (String(item.id) !== String(post.id)) return item
            const merged = { ...item, ...res }
            if (!shouldShowPostShareCount(merged)) {
              merged.sharesCount = 0
              merged.showSharesCount = false
              merged.shareCountDisabled = true
            }
            return merged
          }))
        }
      } catch (err) {
        console.warn('Could not record post share:', err)
      }
    }
  }

  async function handleSharePost(post) {
    const link = `${window.location.origin}/social/home?postId=${encodeURIComponent(String(post.id))}`
    if (navigator.share) {
      try {
        await navigator.share({
          title: post.title || 'TrueHunt Social post',
          text: post.content || '',
          url: link,
        })
        await recordPostShare(post)
        return
      } catch (error) {
        if (error?.name === 'AbortError') return
      }
    }
    try {
      await navigator.clipboard.writeText(link)
      await recordPostShare(post)
      window.alert('Post link copied to clipboard.')
    } catch {
      window.prompt('Share this post link:', link)
    }
  }

  function handleHidePostsByAuthor(post) {
    const authorId = String(post.authorId || '').trim()
    const authorName = String(post.authorName || '').trim().toLowerCase()
    const dismissedIds = new Set(readDismissedPostIds(dismissedPostsStorageKey))
    setPosts((current) => current.filter((item) => {
      const itemAuthorId = String(item.authorId || '').trim()
      const itemAuthorName = String(item.authorName || '').trim().toLowerCase()
      if (authorId && itemAuthorId === authorId) {
        dismissedIds.add(String(item.id))
        return false
      }
      if (!authorId && authorName && itemAuthorName === authorName) {
        dismissedIds.add(String(item.id))
        return false
      }
      return true
    }))
    try {
      localStorage.setItem(dismissedPostsStorageKey, JSON.stringify([...dismissedIds]))
    } catch (error) {
      console.warn('Could not save dismissed posts:', error)
    }
    setDismissedPostIds([...dismissedIds])
    const targetAuthor = authorId || authorName
    if (isAuthenticated && targetAuthor) {
      api.hideAuthorPosts(targetAuthor).catch((err) => {
        console.warn('Could not persist hidden author on backend:', err)
      })
    }
    setOpenPostMenuId('')
  }

  function openBlockPostAuthor(post) {
    setBlockTarget(post)
    setBlockReason('')
    setCustomBlockReason('')
    setOpenPostMenuId('')
  }

  function handleConfirmBlockPostAuthor(event) {
    event.preventDefault()
    if (!blockTarget || !blockReason) return

    const reason = blockReason === 'Custom reason'
      ? customBlockReason.trim()
      : blockReason
    if (!reason) return

    const identity = String(
      blockTarget.authorId || blockTarget.authorEmail || blockTarget.authorName || '',
    ).trim()
    if (!identity) {
      window.alert('Could not identify this post author to block.')
      return
    }

    const blockedUser = {
      identity,
      name: blockTarget.authorName || 'TrueHunt member',
      email: blockTarget.authorEmail || '',
      photo: getPostAuthorAvatar(blockTarget),
      reason,
    }

    try {
      const settings = JSON.parse(localStorage.getItem(accountSettingsStorageKey) || '{}')
      const existing = Array.isArray(settings.blockedUsers) ? settings.blockedUsers : []
      const blockedUsers = [
        ...existing.filter((blocked) => {
          const blockedIdentity = typeof blocked === 'string' ? blocked : blocked.identity
          return String(blockedIdentity || '').trim().toLowerCase() !== identity.toLowerCase()
        }),
        blockedUser,
      ]
      localStorage.setItem(
        accountSettingsStorageKey,
        JSON.stringify({ ...settings, blockedUsers }),
      )
    } catch (error) {
      console.error('Could not save blocked profile:', error)
      window.alert('Could not block this profile. Please try again.')
      return
    }

    setPosts((current) => current.filter((post) => !postMatchesBlockedUser(post, [blockedUser])))
    if (isAuthenticated) {
      api.blockUser(blockedUser).catch((err) => {
        console.warn('Could not persist blocked user on backend:', err)
      })
    }
    setBlockTarget(null)
    setBlockReason('')
    setCustomBlockReason('')
  }

  function handleReportPost(post) {
    setReportTarget(post)
    setReportReason('')
    setCustomReportReason('')
    setReportError('')
    setOpenPostMenuId('')
  }

  async function handleSubmitPostReport(event) {
    event.preventDefault()
    if (!reportTarget || isSubmittingReport) return

    const reason = reportReason === 'Other'
      ? customReportReason.trim()
      : reportReason
    if (!reason) {
      setReportError(reportReason === 'Other'
        ? 'Please describe the reason for this report.'
        : 'Choose a reason for reporting this post.')
      return
    }
    if (!isAuthenticated) {
      setReportError('Sign in before reporting a post.')
      return
    }

    const reportDetails = {
      postId: String(reportTarget.id),
      authorId: reportTarget.authorId || '',
      authorName: reportTarget.authorName || '',
      title: reportTarget.title || '',
      content: reportTarget.content || '',
      reason,
    }

    setIsSubmittingReport(true)
    setReportError('')
    try {
      const savedReport = await api.reportPost(reportTarget.id, reportDetails)
      const report = {
        ...reportDetails,
        ...savedReport,
        postId: String(savedReport?.postId || reportDetails.postId),
        reportedAt: savedReport?.reportedAt || new Date().toISOString(),
      }
      const accountStorageId = profileDetails.userId
        || profileDetails.id
        || profileDetails.email
        || accountEmail
        || currentUserId
        || 'me'
      const reportsKey = `truehunt-social-reported-posts:${encodeURIComponent(String(accountStorageId))}`
      try {
        const reports = JSON.parse(localStorage.getItem(reportsKey) || '[]')
        const existing = Array.isArray(reports) ? reports : []
        localStorage.setItem(reportsKey, JSON.stringify([report, ...existing]))
      } catch (error) {
        console.warn('Could not cache the submitted post report:', error)
      }
      window.dispatchEvent(new CustomEvent('truehunt-reported-post-changed'))
      setReportTarget(null)
      setReportReason('')
      setCustomReportReason('')
      window.alert('Your report was sent to the moderation team for review.')
    } catch (error) {
      console.error('Could not submit post report:', error)
      setReportError(error.message || 'Could not submit this report. Please try again.')
    } finally {
      setIsSubmittingReport(false)
    }
  }

  function handleNotInterestedInvestorListing(listingId) {
    const listingIdString = String(listingId)
    const dismissedIds = new Set(readDismissedPostIds(dismissedInvestorListingsStorageKey))
    dismissedIds.add(listingIdString)
    try {
      localStorage.setItem(dismissedInvestorListingsStorageKey, JSON.stringify([...dismissedIds]))
    } catch (error) {
      console.warn('Could not save dismissed investor listing:', error)
    }
    setDismissedInvestorListingIds([...dismissedIds])
    if (isAuthenticated) {
      api.markInvestorNotInterested(listingIdString).catch((err) => {
        console.warn('Could not persist dismissed investor listing on backend:', err)
      })
    }
  }

  function isOwnPost(post) {
    if (!post) return false
    if (post.isAuthor === true) return true
    if (post.isAuthor === false) return false

    const authorId = String(post.authorId || '').trim().toLowerCase()
    const ownIds = [
      currentUserId,
      ...currentUserIdentifiers,
      profileDetails.userId,
      profileDetails.id,
      profileDetails.email,
      accountEmail,
    ]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
      .filter((id) => id !== 'me')
    if (authorId && authorId !== 'me' && ownIds.includes(authorId)) return true

    const authorName = String(post.authorName || '').trim().toLowerCase()
    const ownName = String(profileDetails.name || '').trim().toLowerCase()
    const genericNames = ['cinema professional', 'truehunt member', 'truehunt user', 'your name', 'member', 'talent scout', 'talent']
    if (authorName && ownName && authorName === ownName && !genericNames.includes(authorName)) return true
    return false
  }

  function isPostByCurrentUser(post) {
    if (!post) return false
    const authorIds = [post.authorId, post.authorEmail]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
    const ownIds = [
      currentUserId,
      ...currentUserIdentifiers,
      profileDetails.userId,
      profileDetails.id,
      profileDetails.email,
      accountEmail,
    ]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
      .filter((id) => id !== 'me')
    if (authorIds.some((authorId) => ownIds.includes(authorId))) return true

    const legacyAccountIds = ['1000001', 'pro@truehunt.in']
    return authorIds.some((authorId) => authorId === 'me' || legacyAccountIds.includes(authorId))
      && ownIds.some((ownId) => legacyAccountIds.includes(ownId))
  }

  function canManagePost(post) {
    if (!post || post.isAuthor === false) return false

    const authorIds = [post.authorId, post.authorEmail]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
    const ownIds = [currentUserId, profileDetails.userId, profileDetails.id, profileDetails.email, accountEmail]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())

    if (authorIds.some((authorId) => ownIds.includes(authorId))) return true

    const legacyAccountIds = ['me', '1000001', 'pro@truehunt.in']
    return authorIds.some((authorId) => legacyAccountIds.includes(authorId))
      && ownIds.some((ownId) => legacyAccountIds.includes(ownId))
  }

  function shouldShowPostLikeCount(post) {
    if (!post) return false
    if (post.showLikesCount === false || post.likeCountDisabled) {
      return Boolean(post.isAuthor === true || (isOwnPost(post) && post.isAuthor !== false))
    }
    return true
  }

  function shouldShowPostShareCount(post) {
    if (!post) return false
    if (post.showSharesCount === false || post.shareCountDisabled) {
      return Boolean(post.isAuthor === true || (isOwnPost(post) && post.isAuthor !== false))
    }
    return true
  }

  function canDeletePostComment(comment, post) {
    const ownIds = [
      currentUserId,
      profileDetails.userId,
      profileDetails.id,
      profileDetails.email,
      accountEmail,
    ].filter(Boolean).map((id) => String(id).trim().toLowerCase())
    const commentAuthorId = String(comment?.authorId || '').trim().toLowerCase()
    const postAuthorId = String(post.authorId || '').trim().toLowerCase()
    return Boolean(comment?.canDelete)
      || Boolean(commentAuthorId && ownIds.includes(commentAuthorId))
      || Boolean(postAuthorId && ownIds.includes(postAuthorId))
  }

  const showPost = useCallback((post) => {
    if (!post?.id) return
    const postId = String(post.id)
    setPosts((current) => (
      current.some((item) => String(item.id) === postId) ? current : [post, ...current]
    ))
    setFocusedPostId(postId)
    if (activeNavigation !== 'home') setActiveNavigation('home')
  }, [activeNavigation, setActiveNavigation])

  useEffect(() => {
    function handleOpenSavedPost(event) {
      showPost(event.detail?.post)
    }

    window.addEventListener('truehunt-open-saved-post', handleOpenSavedPost)
    return () => window.removeEventListener('truehunt-open-saved-post', handleOpenSavedPost)
  }, [showPost])

  const opportunityActionsRef = useRef(null)
  useEffect(() => {
    opportunityActionsRef.current = {
      apply: openOpportunityApplication,
      share: handleSharePost,
      setActiveNavigation,
    }
  })

  useEffect(() => {
    function addOpportunityPost(post) {
      if (!post?.id) return
      const postId = String(post.id)
      setPosts((current) => {
        const existing = current.some((item) => String(item.id) === postId)
        return existing
          ? current.map((item) => String(item.id) === postId ? { ...item, ...post } : item)
          : [post, ...current]
      })
    }

    function handleOpportunityPublished(event) {
      addOpportunityPost(event.detail?.post)
    }

    function handleOpportunityDeleted(event) {
      const postId = String(event.detail?.postId || '')
      if (!postId) return
      setPosts((current) => current.filter((post) => String(post.id) !== postId))
      setSelectedOpportunity((current) => (
        String(current?.id) === postId ? null : current
      ))
    }

    function handleApplyToOpportunity(event) {
      const post = event.detail?.post
      if (!post?.id) return
      addOpportunityPost(post)
      opportunityActionsRef.current?.setActiveNavigation('home')
      setSelectedOpportunity(null)
      opportunityActionsRef.current?.apply(post)
    }

    function handleShareOpportunity(event) {
      const post = event.detail?.post
      if (!post?.id) return
      addOpportunityPost(post)
      void opportunityActionsRef.current?.share(post)
    }

    window.addEventListener('truehunt-opportunity-published', handleOpportunityPublished)
    window.addEventListener('truehunt-opportunity-deleted', handleOpportunityDeleted)
    window.addEventListener('truehunt-apply-opportunity', handleApplyToOpportunity)
    window.addEventListener('truehunt-share-opportunity', handleShareOpportunity)
    return () => {
      window.removeEventListener('truehunt-opportunity-published', handleOpportunityPublished)
      window.removeEventListener('truehunt-opportunity-deleted', handleOpportunityDeleted)
      window.removeEventListener('truehunt-apply-opportunity', handleApplyToOpportunity)
      window.removeEventListener('truehunt-share-opportunity', handleShareOpportunity)
    }
  }, [])

  useEffect(() => {
    if (!focusedPostId || activeNavigation !== 'home') return undefined

    const frameId = requestAnimationFrame(() => {
      document.getElementById(`home-post-${encodeURIComponent(focusedPostId)}`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
    return () => cancelAnimationFrame(frameId)
  }, [activeNavigation, focusedPostId, posts])

  function viewPostAuthor(post) {
    if (isOwnPost(post)) {
      onViewProfile?.()
      return
    }

    const authorId = String(post.authorId || '').trim()
    const authorName = String(post.authorName || '').trim()
    const authorProfile = profileDirectory.find((profile) => (
      authorId && authorId.toLowerCase() !== 'me'
      && String(profile.userId || profile.id || profile.referId || '').trim().toLowerCase()
        === authorId.toLowerCase()
    )) || profileDirectory.find((profile) => (
      authorName && String(profile.name || '').trim().toLowerCase() === authorName.toLowerCase()
    ))
    const resolvedId = String(
      authorProfile?.userId
      || authorProfile?.id
      || authorProfile?.referId
      || (authorId && authorId.toLowerCase() !== 'me' ? authorId : '')
      || authorProfile?.email
      || authorName
      || 'member'
    ).trim()
    const authorAvatar = authorProfile?.profileImageUrl
      || authorProfile?.avatar
      || post.authorAvatar
      || ''

    onViewConnection?.({
      ...authorProfile,
      id: resolvedId,
      userId: resolvedId,
      name: authorProfile?.name || authorName || 'TrueHunt member',
      profileImageUrl: authorAvatar,
      avatar: authorAvatar,
    })
  }

  async function handleCommentSubmit(event, postId) {
    event.preventDefault()
    const text = commentText.trim()
    if (!text || isCommentSubmitting) return

    const postIdString = String(postId)
    const post = posts.find((item) => String(item.id) === postIdString)
    if (!post) return

    const previousComments = Array.isArray(post.comments) ? post.comments : []
    const optimisticComment = {
      id: `comment-${Date.now()}-${Math.random().toString(36).slice(2)}`,
      authorName: name,
      authorId: currentUserId || profileDetails.userId || accountEmail,
      canDelete: true,
      text,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      likesCount: 0,
      likedByMe: false,
      replies: [],
    }

    setPosts((current) => current.map((item) => (
      String(item.id) === postIdString
        ? {
            ...item,
            comments: [...(Array.isArray(item.comments) ? item.comments : []), optimisticComment],
            commentsCount: (Number(item.commentsCount) || previousComments.length) + 1,
          }
        : item
    )))
    setCommentText('')
    setOpenCommentsPostId(postIdString)
    setExpandedCommentPostIds((current) => current.filter((id) => id !== postIdString))
    setCommentErrors((current) => {
      const next = { ...current }
      delete next[postIdString]
      return next
    })

    if (post.localOnly) return

    setIsCommentSubmitting(true)
    try {
      const updated = await api.commentOnPost(postId, text)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString
          ? {
              ...item,
              ...updated,
              comments: Array.isArray(updated.comments) ? updated.comments : item.comments,
              commentsCount: updated.commentsCount ?? item.commentsCount,
            }
          : item
      )))
    } catch (err) {
      console.warn('Comment error:', err)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString
          ? { ...item, comments: previousComments, commentsCount: post.commentsCount ?? previousComments.length }
          : item
      )))
      setCommentText(text)
      setCommentErrors((current) => ({
        ...current,
        [postIdString]: err instanceof Error ? err.message : 'Could not post your comment. Please try again.',
      }))
    } finally {
      setIsCommentSubmitting(false)
    }
  }

  async function handleCommentLike(postId, comment) {
    const postIdString = String(postId)
    const commentId = String(comment.id)
    const actionKey = `${postIdString}:${commentId}`
    if (pendingCommentLikeKeys.includes(actionKey)) return

    const post = posts.find((item) => String(item.id) === postIdString)
    if (!post) return

    const previousComments = Array.isArray(post.comments) ? post.comments : []
    const wasLiked = Boolean(comment.likedByMe)
    const previousLikesCount = Number(comment.likesCount) || 0
    setPosts((current) => current.map((item) => (
      String(item.id) === postIdString
        ? {
          ...item,
          comments: (item.comments || []).map((itemComment) => (
            String(itemComment.id) === commentId
              ? {
                ...itemComment,
                likedByMe: !wasLiked,
                likesCount: Math.max(0, previousLikesCount + (wasLiked ? -1 : 1)),
              }
              : itemComment
          )),
        }
        : item
    )))

    if (post.localOnly) return

    setCommentLikeErrors((current) => {
      const next = { ...current }
      delete next[actionKey]
      return next
    })
    setPendingCommentLikeKeys((current) => [...current, actionKey])
    try {
      const updated = await api.toggleCommentLike(postId, commentId)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString
          ? {
            ...item,
            ...updated,
            comments: Array.isArray(updated.comments) ? updated.comments : item.comments,
          }
          : item
      )))
    } catch (error) {
      console.warn('Comment like error:', error)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString ? { ...item, comments: previousComments } : item
      )))
      setCommentLikeErrors((current) => ({
        ...current,
        [actionKey]: error instanceof Error ? error.message : 'Could not update this like. Please try again.',
      }))
    } finally {
      setPendingCommentLikeKeys((current) => current.filter((key) => key !== actionKey))
    }
  }

  async function handleCommentReplySubmit(event, postId, commentId) {
    event.preventDefault()
    const text = replyText.trim()
    const postIdString = String(postId)
    const commentIdString = String(commentId)
    const actionKey = `${postIdString}:${commentIdString}`
    if (!text || pendingReplyKey) return

    const post = posts.find((item) => String(item.id) === postIdString)
    if (!post) return

    const previousComments = Array.isArray(post.comments) ? post.comments : []
    const optimisticReply = {
      id: `reply-${Date.now()}-${Math.random().toString(36).slice(2)}`,
      authorName: name,
      authorId: currentUserId || profileDetails.userId || accountEmail,
      canDelete: true,
      text,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    }
    setPosts((current) => current.map((item) => (
      String(item.id) === postIdString
        ? {
          ...item,
          comments: (item.comments || []).map((comment) => (
            String(comment.id) === commentIdString
              ? { ...comment, replies: [...(comment.replies || []), optimisticReply] }
              : comment
          )),
        }
        : item
    )))
    setReplyText('')
    setReplyErrors((current) => {
      const next = { ...current }
      delete next[actionKey]
      return next
    })

    if (post.localOnly) {
      setReplyingToCommentKey('')
      return
    }

    setPendingReplyKey(actionKey)
    try {
      const updated = await api.replyToPostComment(postId, commentIdString, text)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString
          ? {
            ...item,
            ...updated,
            comments: Array.isArray(updated.comments) ? updated.comments : item.comments,
          }
          : item
      )))
      setReplyingToCommentKey('')
    } catch (error) {
      console.warn('Comment reply error:', error)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString ? { ...item, comments: previousComments } : item
      )))
      setReplyText(text)
      setReplyErrors((current) => ({
        ...current,
        [actionKey]: error instanceof Error ? error.message : 'Could not reply to this comment. Please try again.',
      }))
    } finally {
      setPendingReplyKey('')
    }
  }

  async function handleCommentDelete(postId, commentId, replyId = '') {
    const postIdString = String(postId)
    const commentIdString = String(commentId)
    const replyIdString = replyId ? String(replyId) : ''
    const actionKey = `${postIdString}:${replyIdString ? 'reply' : 'comment'}:${replyIdString || commentIdString}`
    if (pendingCommentDeleteKeys.includes(actionKey)) return

    const post = posts.find((item) => String(item.id) === postIdString)
    if (!post) return

    const previousComments = Array.isArray(post.comments) ? post.comments : []
    setPosts((current) => current.map((item) => {
      if (String(item.id) !== postIdString) return item

      const comments = replyIdString
        ? (item.comments || []).map((comment) => (
          String(comment.id) === commentIdString
            ? {
              ...comment,
              replies: (comment.replies || []).filter((reply) => String(reply.id) !== replyIdString),
            }
            : comment
        ))
        : (item.comments || []).filter((comment) => String(comment.id) !== commentIdString)

      return {
        ...item,
        comments,
        commentsCount: replyIdString
          ? item.commentsCount
          : Math.max(0, (Number(item.commentsCount) || previousComments.length) - 1),
      }
    }))
    setCommentDeleteErrors((current) => {
      const next = { ...current }
      delete next[actionKey]
      return next
    })
    setPendingCommentDeleteKeys((current) => [...current, actionKey])

    try {
      if (post.localOnly) return

      const updated = replyIdString
        ? await api.deletePostCommentReply(postId, commentIdString, replyIdString)
        : await api.deletePostComment(postId, commentIdString)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString
          ? {
            ...item,
            ...updated,
            comments: Array.isArray(updated.comments) ? updated.comments : item.comments,
            commentsCount: updated.commentsCount ?? item.commentsCount,
          }
          : item
      )))
    } catch (error) {
      console.error('Could not delete comment:', error)
      setPosts((current) => current.map((item) => (
        String(item.id) === postIdString
          ? {
            ...item,
            comments: previousComments,
            commentsCount: post.commentsCount ?? previousComments.length,
          }
          : item
      )))
      setCommentDeleteErrors((current) => ({
        ...current,
        [actionKey]: error instanceof Error ? error.message : 'Could not delete this comment. Please try again.',
      }))
    } finally {
      setPendingCommentDeleteKeys((current) => current.filter((key) => key !== actionKey))
    }
  }

  async function handlePromoClick(campaign) {
    try {
      const res = await api.clickPromotion(campaign)
      alert(res.message || 'Inquiry registered with Northstar Films casting office.')
    } catch (err) {
      console.warn('Promo error:', err)
    }
  }

  const visiblePosts = posts.filter((post) => (
    (!dismissedPostIds.includes(String(post.id))
      || String(post.id) === focusedPostId
      || String(post.id) === String(selectedOpportunityId))
    && (activeNavigation === 'opportunities'
      ? post.type === 'opportunity' && !isModellingRequirementPost(post)
      : true)
  ))
  const visibleInvestorListings = investorListings.filter(
    (listing) => (
      !dismissedInvestorListingIds.includes(String(listing.id))
      || String(listing.id) === String(selectedInvestorId)
    ),
  )
  const selectedOpportunityPost = activeNavigation === 'opportunities'
    ? visiblePosts.find((post) => String(post.id) === String(selectedOpportunity?.id))
    : null
  const isOwnSelectedOpportunity = selectedOpportunityPost
    ? canManagePost(selectedOpportunityPost)
    : false
  const selectedOpportunityDetails = selectedOpportunityPost?.opportunityDetails
    && typeof selectedOpportunityPost.opportunityDetails === 'object'
    ? selectedOpportunityPost.opportunityDetails
    : {}
  const selectedOpportunityKnownFields = new Set(
    opportunityDetailGroups.flatMap((group) => group.fields),
  )
  const selectedOpportunityAdditionalDetails = Object.entries(selectedOpportunityDetails)
    .filter(([field]) => !selectedOpportunityKnownFields.has(field))
  const selectedOpportunityMedia = selectedOpportunityPost
    ? getPostPreviewMedia(selectedOpportunityPost)
    : null

  function openOpportunityPost(post, toggleIfExpanded = false) {
    if (isModellingRequirementPost(post)) {
      onOpenModellingRequirement?.(post)
      return
    }

    const isExpanded = String(selectedOpportunity?.id) === String(post.id)
    if (toggleIfExpanded && activeNavigation === 'opportunities' && isExpanded) {
      setSelectedOpportunity(null)
      return
    }

    setSelectedOpportunity(post)
    if (activeNavigation !== 'opportunities') {
      setActiveNavigation?.('opportunities')
    }
  }
  const allMyPosts = [
    ...posts,
    ...(Array.isArray(profileDetails.posts) ? profileDetails.posts : []),
  ].filter(isPostByCurrentUser).filter((post, index, allPosts) => (
    allPosts.findIndex((candidate) => String(candidate.id) === String(post.id)) === index
  ))
  const myPosts = allMyPosts.slice(0, 3)
  const totalPostLikes = allMyPosts.reduce(
    (total, post) => total + (Number(post.likesCount) || 0),
    0,
  )
  function renderPostPreview(post) {
    const text = getPostPreviewText(post)
    const media = getPostPreviewMedia(post)
    return (
      <>
        {media && !media.isVideo && !media.isAudio && (
          <img className="home-my-post-thumbnail" src={media.url} alt="" />
        )}
        {text && <span className="home-my-post-copy">{text}</span>}
        {!text && media?.isVideo && <span className="home-my-post-copy">Video post</span>}
        {!text && media?.isAudio && <span className="home-my-post-copy">Audio post</span>}
        {!text && !media && <span className="home-my-post-copy">Content unavailable</span>}
      </>
    )
  }

  return (
    <>
      <section
        className={`home-page${activeNavigation === 'opportunities' ? ' is-opportunities' : ''}`}
        hidden={hidden || isWritingArticle}
        aria-label={activeNavigation === 'opportunities' ? 'Casting' : 'Home feed'}
      >
      <div className={`home-layout${activeNavigation === 'opportunities' ? ' is-opportunities' : ''}`}>
        <aside className="home-sidebar" aria-label="Your profile summary">
          <section
            className="home-card home-profile-card"
            role="button"
            tabIndex={0}
            aria-label={`View ${name}'s profile`}
            onClick={onViewProfile}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                onViewProfile()
              }
            }}
          >
            <div className="home-profile-cover">
              <ProfileBanner variant="card" />
            </div>
            <div className="home-profile-content">
              <div className={`home-avatar home-profile-avatar${profilePreview ? '' : ' is-placeholder'}`}>
                {profilePreview
                  ? <img src={profilePreview} alt={`${name}'s profile`} />
                  : <ProfileAvatarPlaceholder />}
              </div>
              <h1>{name}</h1>
              {profileDetails.talents.length > 0 && (
                <p className="home-profile-headline">{profileDetails.talents.join(' · ')}</p>
              )}
              {profileLocation && <p>{profileLocation}</p>}
              {profileDetails.experience && (
                <p>{profileDetails.experience.toLowerCase().includes('year') ? `${profileDetails.experience} experience` : `${profileDetails.experience} years of experience`}</p>
              )}
              {profileDetails.workTypes.length > 0 && (
                <div className="home-profile-tags">
                  {profileDetails.workTypes.map((workType) => (
                    <span key={workType}>
                      {workType.startsWith('Other: ')
                        ? `Others: ${workType.slice('Other: '.length)}`
                        : workType}
                    </span>
                  ))}
                </div>
              )}
              {languages.length > 0 && (
                <div className="home-profile-tags">
                  {languages.map((language) => (
                    <span key={language}>{language}</span>
                  ))}
                </div>
              )}
            </div>
          </section>
          <div className="home-card home-sidebar-stats">
            <button
              className="home-connections-card"
              type="button"
              onClick={() => onOpenConnections?.()}
            >
              <span className="home-connections-icon"><UsersRound size={20} aria-hidden="true" /></span>
              <span className="home-connections-copy">
                <strong>Connections</strong>
                <small>View your professional network</small>
              </span>
              <span className="home-connections-count">{connectedProfiles.length}</span>
            </button>
            <button
              className="home-profile-viewers-card"
              type="button"
              onClick={() => {
                setIsProfileViewersOpen(true)
                api.getProfileViewers().then((data) => {
                  if (!Array.isArray(data)) {
                    throw new Error('Profile viewers returned an invalid response.')
                  }
                  setProfileViewers(data)
                  setProfileViewersError('')
                }).catch((error) => {
                  console.error('Failed to refresh profile viewers:', error)
                  setProfileViewersError('Could not load profile viewers. Please try again.')
                })
              }}
              aria-haspopup="dialog"
            >
              <span className="home-connections-icon"><Eye size={20} aria-hidden="true" /></span>
              <span className="home-connections-copy">
                <strong>Profile viewers</strong>
                <small>Profile visibility insights</small>
              </span>
              <span className="home-connections-count">{profileViewers.length}</span>
            </button>
            {activeNavigation === 'home' && (
              <div className="home-connections-card home-post-impressions-row" aria-labelledby="home-post-impressions-heading">
                <span className="home-connections-icon"><ThumbsUp size={20} aria-hidden="true" /></span>
                <span className="home-connections-copy">
                  <strong id="home-post-impressions-heading">Post impressions</strong>
                  <small>
                    <span aria-label={`${totalPostLikes} likes across all posts`}>
                      {totalPostLikes}
                    </span>
                  </small>
                </span>
              </div>
            )}
            {activeNavigation === 'opportunities' && (
              <button
                className="home-profile-viewers-card home-investor-search-button"
                type="button"
                onClick={openInvestorSearch}
                aria-pressed={isInvestorSearchOpen}
              >
                <span className="home-connections-icon"><Search size={20} aria-hidden="true" /></span>
                <span className="home-connections-copy">
                  <strong>Investor search</strong>
                  <small>Explore published investor profiles</small>
                </span>
                <span className="home-connections-count">{investorListings.length}</span>
              </button>
            )}
          </div>
          {activeNavigation === 'opportunities' && (
            <Link className="home-explore-promo home-explore-promo-tickets home-explore-promo-tickets-opportunities" to="/entertainments">
              <span className="home-explore-promo-art">
                <img src="/assets/explore-tickets.png" alt="" />
              </span>
              <span className="home-explore-promo-copy">
                <strong>Book a ticket</strong>
                <small>Explore films and showtimes</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </Link>
          )}
          {connectionsError && <p className="home-connections-error" role="alert">{connectionsError}</p>}
          {profileViewersError && <p className="home-connections-error" role="alert">{profileViewersError}</p>}
          {activeNavigation === 'home' && (
            <section className="home-card home-my-posts-card" aria-labelledby="home-my-posts-heading">
              <header className="home-my-posts-header">
                <h2 id="home-my-posts-heading">My Posts</h2>
                <button type="button" onClick={onViewProfile}>View all</button>
              </header>
              {myPosts.length > 0 ? (
                <ol className="home-my-posts-list">
                  {myPosts.map((post) => (
                    <li key={post.id}>
                      <button type="button" onClick={() => showPost(post)}>
                        {renderPostPreview(post)}
                        <span className="home-my-post-metrics">
                          <span><ThumbsUp size={13} aria-hidden="true" /> {post.likesCount || 0}</span>
                          <span><MessageCircle size={13} aria-hidden="true" /> {post.commentsCount ?? post.comments?.length ?? 0}</span>
                        </span>
                      </button>
                    </li>
                  ))}
                </ol>
              ) : (
                <p className="home-my-posts-empty">Your posts will appear here.</p>
              )}
            </section>
          )}
          {activeNavigation === 'home' && (
            <Link className="home-explore-promo home-explore-promo-tickets home-explore-promo-tickets-desktop" to="/entertainments">
              <span className="home-explore-promo-art">
                <img src="/assets/explore-tickets.png" alt="" />
              </span>
              <span className="home-explore-promo-copy">
                <strong>Book a ticket</strong>
                <small>Explore films and showtimes</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </Link>
          )}
        </aside>

        <div className="home-feed-column">
          {isInvestorSearchOpen ? (
            <section className="home-investor-search-view">
              <header className="home-investor-search-header">
                <div>
                  <h1>Investor Search</h1>
                  <p>Explore profiles shared by the TrueHunt community.</p>
                </div>
                <button type="button" onClick={() => setIsInvestorSearchOpen(false)}>
                  Back to opportunities
                </button>
              </header>
              {isLoadingInvestorListings ? (
                <p className="home-investor-search-message" role="status">Loading investor profiles…</p>
              ) : investorListingsError ? (
                <div className="home-investor-search-message" role="alert">
                  <p>{investorListingsError}</p>
                  <button type="button" onClick={openInvestorSearch}>Try again</button>
                </div>
              ) : visibleInvestorListings.length ? (
                <div className="home-investor-listings">
                  {visibleInvestorListings.map((listing) => (
                    <article
                      className={`home-card home-investor-listing${String(listing.id) === String(selectedInvestorId) ? ' is-selected' : ''}`}
                      id={`social-investor-listing-${encodeURIComponent(String(listing.id))}`}
                      key={listing.id}
                    >
                      <header>
                        <div>
                          <h2>{listing.name}</h2>
                          <span>{listing.role}</span>
                        </div>
                        {listing.budget && <strong>{formatInvestorBudget(listing.budget)}</strong>}
                      </header>
                      <p>{listing.description}</p>
                      {listing.isMine && (
                        <button
                          className="home-investor-listing-delete"
                          type="button"
                          onClick={() => void handleDeleteInvestorListing(listing)}
                        >
                          Delete my investor post
                        </button>
                      )}
                      {!listing.isMine && (
                        <>
                          <div className="home-investor-listing-actions">
                            <button
                              className="home-investor-interest"
                              type="button"
                              onClick={() => void sendInvestorInterest({
                                id: listing.id,
                                investorProfileId: listing.id,
                              })}
                              disabled={authLoading || ['sending', 'sent'].includes(investorInterestStatus[String(listing.id)]?.status)}
                            >
                              {investorInterestStatus[String(listing.id)]?.status === 'sending'
                                ? 'Sending…'
                                : investorInterestStatus[String(listing.id)]?.status === 'sent'
                                  ? 'Interest sent'
                                  : 'Interested'}
                            </button>
                            <button
                              className="home-investor-listing-dismiss"
                              type="button"
                              onClick={() => handleNotInterestedInvestorListing(listing.id)}
                            >
                              Not interested
                            </button>
                          </div>
                          {investorInterestStatus[String(listing.id)]?.status === 'error' && (
                            <span className="home-investor-interest-error" role="alert">
                              {investorInterestStatus[String(listing.id)].message}
                            </span>
                          )}
                        </>
                      )}
                    </article>
                  ))}
                </div>
              ) : (
                <section className="home-card home-empty-feed">
                  <h2>No investor profiles yet</h2>
                  <p>Investor profiles will appear here after they are published.</p>
                </section>
              )}
            </section>
          ) : (
          <>
          {activeNavigation === 'home' && (
          <section className="home-card home-composer" aria-label="Create a post">
            <div className="home-composer-top">
              <div className={`home-avatar${profilePreview ? '' : ' is-placeholder'}`} aria-hidden="true">
                {profilePreview
                  ? <img src={profilePreview} alt="" />
                  : <ProfileAvatarPlaceholder />}
              </div>
              <button
                className="home-start-post"
                type="button"
                onClick={startPost}
                aria-expanded={isComposerOpen}
              >
                {editingPost ? 'Continue editing' : 'Start a post'}
              </button>
            </div>

            {isComposerOpen && (
              <form className="home-post-form" onSubmit={publishPost}>
                <label className="home-sr-only" htmlFor="home-post-content">
                  Write your post
                </label>
                <textarea
                  id="home-post-content"
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder={draftType === 'opportunity'
                    ? 'Describe the casting call you are sharing'
                    : 'What would you like to share?'}
                  rows={4}
                  autoFocus
                />
                {draftMedia && (
                  <div className="home-media-preview">
                    {draftMedia.file
                      ? (mediaSafety.isAnalyzing
                        ? <div className="home-media-scanning">Scanning media for adult content...</div>
                        : mediaSafety.isBlocked
                          ? <div className="home-media-scanning">Media preview hidden until content is verified.</div>
                          : draftMedia.kind === 'video'
                            ? <video src={draftMedia.url} controls />
                            : <img src={draftMedia.url} alt="Selected post attachment" />)
                      : draftMedia.kind === 'video'
                        ? <video src={draftMedia.url} controls />
                        : <img src={draftMedia.url} alt="Selected post attachment" />}
                    {(!editingPost || !draftMedia.isExisting) && (
                      <button type="button" onClick={removeDraftMedia}>Remove attachment</button>
                    )}
                  </div>
                )}
                {draftMedia?.file && (
                  <MediaSafetyStatus
                    safety={mediaSafety}
                    blockedMessage={`This ${draftMedia.kind} has high adult content, so we can't post this ${draftMedia.kind}.`}
                    safeMessage="Safe content verified. Ready to post."
                  />
                )}
                {mediaSelectionError && (
                  <p className="home-post-error" role="alert">{mediaSelectionError}</p>
                )}
                <div className="home-publish-row">
                  <span>{draftType === 'opportunity' ? 'Casting call' : 'Post'}</span>
                  <div className="home-publish-controls">
                    <button
                      className="home-cancel-edit-button"
                      type="button"
                      onClick={cancelEditingPost}
                    >
                      Cancel
                    </button>
                    <div className="home-audience-selector" ref={audienceMenuRef}>
                      <button
                        className="home-audience-trigger"
                        type="button"
                        aria-haspopup="menu"
                        aria-expanded={isAudienceMenuOpen}
                        onClick={() => setIsAudienceMenuOpen((isOpen) => !isOpen)}
                      >
                        {draftAudience === 'private'
                          ? <LockKeyhole size={15} aria-hidden="true" />
                          : draftAudience === 'connections'
                            ? <UsersRound size={15} aria-hidden="true" />
                            : <Globe size={15} aria-hidden="true" />}
                        {draftAudience === 'connections' ? 'Connections only' : draftAudience === 'private' ? 'Private' : 'Public'}
                        <ChevronDown size={14} aria-hidden="true" />
                      </button>
                      {isAudienceMenuOpen && (
                        <div className="home-audience-menu" role="menu" aria-label="Post audience">
                          {[
                            { value: 'public', label: 'Public', Icon: Globe },
                            { value: 'connections', label: 'Connections only', Icon: UsersRound },
                            { value: 'private', label: 'Private', Icon: LockKeyhole },
                          ].map(({ value, label, Icon }) => (
                            <button
                              className="home-audience-option"
                              key={value}
                              type="button"
                              role="menuitemradio"
                              aria-checked={draftAudience === value}
                              onClick={() => {
                                setDraftAudience(value)
                                setIsAudienceMenuOpen(false)
                              }}
                            >
                              <Icon size={16} aria-hidden="true" />
                              <span>
                                <strong>{label}</strong>
                              </span>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                    <button
                      className="home-publish-button"
                      type="submit"
                      disabled={(!draft.trim() && !draftMedia) || isDraftMediaBlocked}
                    >
                      {editingPost ? 'Save changes' : draftType === 'opportunity' ? 'Share' : 'publish'}
                    </button>
                  </div>
                </div>
              </form>
            )}

            <div className="home-composer-actions">
              <button type="button" onClick={() => videoInputRef.current?.click()}>
                <svg className="home-action-icon home-video-icon" viewBox="0 0 20 20" aria-hidden="true">
                  <rect x="1" y="3" width="18" height="14" rx="3" fill="currentColor" />
                  <path d="m8 6.5 5 3.5-5 3.5z" fill="#fff" />
                </svg>
                Video
              </button>
              <button type="button" onClick={() => imageInputRef.current?.click()}>
                <svg className="home-action-icon home-photo-icon" viewBox="0 0 20 20" aria-hidden="true">
                  <rect x="1.5" y="2.5" width="17" height="15" rx="2" fill="none" stroke="currentColor" strokeWidth="2" />
                  <circle cx="6.5" cy="7" r="1.5" fill="currentColor" />
                  <path d="m3.5 15 4.2-4.2 2.7 2.5 2.2-2 4 3.7z" fill="currentColor" />
                </svg>
                Photo
              </button>
              <button
                type="button"
                onClick={() => {
                  setIsWritingArticle(true)
                }}
              >
                <svg className="home-action-icon home-article-icon" viewBox="0 0 20 20" aria-hidden="true">
                  <path d="M1 3h18v2H1zm0 4h18v2H1zm0 4h18v2H1zm0 4h12v2H1z" fill="currentColor" />
                </svg>
                Write article
              </button>
              <input
                ref={videoInputRef}
                className="home-sr-only"
                type="file"
                accept="video/*"
                aria-label="Choose a video"
                onChange={selectMedia}
              />
              <input
                ref={imageInputRef}
                className="home-sr-only"
                type="file"
                accept="image/*"
                aria-label="Choose a photo"
                onChange={selectMedia}
              />
            </div>
          </section>
          )}

          <div className="home-feed-heading">
            <span>{activeNavigation === 'opportunities' ? 'Casting' : 'Posts'}</span>
            {activeNavigation === 'opportunities' ? (
              <button className="home-opportunity-button" type="button" onClick={startOpportunityPost}>
                Share a casting call
              </button>
            ) : (
              <span className="home-feed-sort">Recent</span>
            )}
          </div>

          {visiblePosts.length === 0 ? (
            <section className="home-card home-empty-feed">
              <h2>{activeNavigation === 'opportunities'
                ? 'No casting calls shared yet'
                : 'Your feed starts here'}</h2>
              <p>{activeNavigation === 'opportunities'
                ? 'When someone shares a casting call, it will appear here.'
                : 'Share an update, photo, or video to create your first post.'}</p>
            </section>
          ) : (
            <div className={`home-opportunity-browser${activeNavigation === 'opportunities' ? ' is-active' : ''}${selectedOpportunityPost ? ' has-selection' : ''}`}>
            <div className={`home-post-list${activeNavigation === 'opportunities' ? ' home-opportunity-results' : ''}`}>
              {visiblePosts.map((post) => {
                const authorAvatar = getPostAuthorAvatar(post)
                const authorName = post.authorName || name
                const postId = String(post.id)
                const postMedia = getPostPreviewMedia(post)
                const commentProfiles = [...profileDirectory, ...connectedProfiles]
                const isOwnOpportunity = post.type === 'opportunity' && canManagePost(post)
                const isOpportunityExpanded = String(selectedOpportunity?.id) === postId
                const opportunityDetails = post.opportunityDetails && typeof post.opportunityDetails === 'object'
                  ? post.opportunityDetails
                  : {}
                const selectedGenders = Array.isArray(opportunityDetails.genderPreferences)
                  ? opportunityDetails.genderPreferences
                  : String(opportunityDetails.genderPreference || '').split(',').map((gender) => gender.trim()).filter(Boolean)
                const genderPreferenceBadge = selectedGenders.length === 1 && selectedGenders[0] !== 'Any'
                  ? `${selectedGenders[0]} preference`
                  : ''
                const postComments = Array.isArray(post.comments) ? post.comments : []
                const areCommentsExpanded = expandedCommentPostIds.includes(postId)
                const commentsToShow = areCommentsExpanded ? postComments : postComments.slice(-1)
                const remainingCommentCount = postComments.length - commentsToShow.length
                return (
                <article
                  className={`home-card home-post${focusedPostId === String(post.id) ? ' is-focused' : ''}${isOpportunityExpanded ? ' is-selected' : ''}`}
                  id={`home-post-${encodeURIComponent(String(post.id))}`}
                  key={post.id}
                >
                  <header className="home-post-header">
                    <button
                      className="home-post-author-avatar"
                      type="button"
                      aria-label={`View ${authorName}'s profile`}
                      onClick={() => viewPostAuthor(post)}
                    >
                      <span className={`home-avatar${authorAvatar ? '' : ' is-placeholder'}`} aria-hidden="true">
                        {authorAvatar
                          ? <img src={authorAvatar} alt="" />
                          : <ProfileAvatarPlaceholder />}
                      </span>
                    </button>
                    <div>
                      <button
                        className="home-post-author-name"
                        type="button"
                        onClick={() => viewPostAuthor(post)}
                      >
                        <strong>{authorName}</strong>
                      </button>
                      {post.authorHeadline && (
                        <span>{post.authorHeadline}</span>
                      )}
                      <time>{post.createdAt}</time>
                    </div>
                    {!isOpportunityExpanded && canManagePost(post) && !isOwnOpportunity && (
                      <button
                        className="home-post-edit-trigger"
                        type="button"
                        aria-label="Edit post"
                        title="Edit post"
                        onClick={() => startEditingPost(post)}
                      >
                        <Pencil size={17} aria-hidden="true" />
                      </button>
                    )}
                    {!isOpportunityExpanded && !canManagePost(post) && (
                      <div className="home-post-menu">
                        <button
                          className="home-post-menu-trigger"
                          type="button"
                          aria-label="Post options"
                          aria-expanded={openPostMenuId === postId}
                          onClick={(event) => {
                            event.stopPropagation()
                            if (openPostMenuId === postId) {
                              setOpenPostMenuId('')
                              return
                            }
                            const rect = event.currentTarget.getBoundingClientRect()
                            const menuWidth = 230
                            const menuHeight = 238
                            const viewportPadding = 12
                            setPostMenuPosition({
                              top: rect.bottom + menuHeight + viewportPadding <= window.innerHeight
                                ? rect.bottom + 6
                                : Math.max(viewportPadding, rect.top - menuHeight - 6),
                              left: Math.max(viewportPadding, Math.min(
                                rect.right - menuWidth,
                                window.innerWidth - menuWidth - viewportPadding,
                              )),
                            })
                            setOpenPostMenuId(postId)
                          }}
                        >
                          <MoreVertical size={19} aria-hidden="true" />
                        </button>
                      </div>
                    )}
                  </header>
                  {savedPostIds.includes(postId) && (
                    <span className="home-post-saved-badge" role="status">
                      <Bookmark size={14} fill="currentColor" aria-hidden="true" />
                      Saved
                    </span>
                  )}
                  {post.type === 'opportunity' ? (
                    <div className={`home-post-opportunity-summary${genderPreferenceBadge ? ' has-gender-preference' : ''}`}>
                      {genderPreferenceBadge && (
                        <span className="home-post-gender-preference">{genderPreferenceBadge}</span>
                      )}
                      <button
                        type="button"
                        className="home-post-opportunity-trigger"
                        onClick={() => openOpportunityPost(post, true)}
                        aria-label={`View ${isModellingRequirementPost(post) ? 'modelling requirement' : 'casting call'}: ${post.title || getPostPreviewText(post) || 'Details'}`}
                        aria-expanded={isOpportunityExpanded}
                      >
                        {post.title && <span className="home-post-title">{post.title}</span>}
                        {post.content && <span className="home-post-content">{post.content}</span>}
                        {postMedia && !postMedia.isVideo && (
                          <img
                            className="home-post-media"
                            src={postMedia.url}
                            alt={isModellingRequirementPost(post) ? 'Modelling requirement attachment' : 'Casting call attachment'}
                          />
                        )}
                      </button>
                      {postMedia?.isVideo && (
                        <video className="home-post-media" src={postMedia.url} controls />
                      )}
                    </div>
                  ) : (
                    <>
                      {post.title && <h2 className="home-post-title">{post.title}</h2>}
                      {post.content && <p className="home-post-content">{post.content}</p>}
                      {postMedia?.isVideo
                        ? <video className="home-post-media" src={postMedia.url} controls />
                        : postMedia?.isAudio
                          ? <audio className="home-post-media" src={postMedia.url} controls />
                          : postMedia && (
                            <img className="home-post-media" src={postMedia.url} alt="Post attachment" />
                          )}
                    </>
                  )}
                  {post.type === 'article' && <span className="home-post-type">Article</span>}
                  {post.type === 'opportunity' && (
                    <button
                      type="button"
                      className="home-post-type home-post-type-button"
                      aria-label={`Open ${isModellingRequirementPost(post) ? 'modelling requirement' : 'casting call'}: ${post.title || getPostPreviewText(post) || 'Details'}`}
                      onClick={() => openOpportunityPost(post)}
                    >
                      {isModellingRequirementPost(post) ? 'Modelling requirement' : 'Casting call'}
                    </button>
                  )}

                  {/* Post interactive action buttons wired to backend APIs */}
                  <div className="home-post-action-bar" style={{ display: 'flex', gap: '8px', marginTop: '12px', borderTop: '1px solid var(--color-border, #eee)', paddingTop: '8px', flexWrap: 'wrap' }}>
                    {!isOwnOpportunity && (
                      <>
                    <div className="home-post-like-actions">
                      <button
                        type="button"
                        onClick={() => handleLike(post)}
                        disabled={pendingLikePostIds.includes(String(post.id))}
                        className="home-post-icon-action"
                        aria-label={`${post.liked ? 'Unlike' : 'Like'} post`}
                        title={post.liked ? 'Unlike' : 'Like'}
                        style={{
                          background: post.liked ? '#e8f0fe' : 'transparent',
                          color: post.liked ? '#1a73e8' : 'inherit',
                          border: 'none',
                          width: '36px',
                          minWidth: '36px',
                          height: '36px',
                          padding: '0 8px',
                          cursor: pendingLikePostIds.includes(String(post.id)) ? 'wait' : 'pointer',
                          display: 'inline-flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '6px',
                          borderRadius: '18px'
                        }}
                      >
                        <ThumbsUp size={18} fill={post.liked ? 'currentColor' : 'none'} aria-hidden="true" />
                      </button>
                      {shouldShowPostLikeCount(post) && (
                        <button
                          type="button"
                          className="home-post-like-count-button"
                          onClick={() => setPostLikesDialog(post)}
                          aria-label={`View ${post.likesCount ?? 0} likes`}
                          title="View people who liked this post"
                        >
                          <span className="home-post-action-count">{post.likesCount ?? 0}</span>
                        </button>
                      )}
                    </div>
                    {likeErrors[String(post.id)] && (
                      <span role="alert" style={{ alignSelf: 'center', color: '#b3261e', fontSize: '12px' }}>
                        {likeErrors[String(post.id)]}
                      </span>
                    )}

                    <button
                      type="button"
                      onClick={() => setOpenCommentsPostId(openCommentsPostId === String(post.id) ? null : String(post.id))}
                      className="home-post-icon-action"
                      aria-label={`Comments (${post.commentsCount ?? post.comments?.length ?? 0})`}
                      title="Comments"
                      style={{
                        background: 'transparent',
                        border: 'none',
                        width: 'auto',
                        minWidth: '36px',
                        height: '36px',
                        padding: '0 8px',
                        cursor: 'pointer',
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px',
                        borderRadius: '18px'
                      }}
                    >
                      <MessageCircle size={18} aria-hidden="true" />
                      <span className="home-post-action-count">{post.commentsCount ?? post.comments?.length ?? 0}</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleSharePost(post)}
                      className="home-post-icon-action"
                      aria-label={`Share post${shouldShowPostShareCount(post) ? ` (${post.sharesCount ?? 0} shares)` : ''}`}
                      title="Share"
                      style={{
                        background: 'transparent',
                        border: 'none',
                        width: 'auto',
                        minWidth: '36px',
                        height: '36px',
                        padding: '0 8px',
                        cursor: 'pointer',
                        opacity: 1,
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px',
                        borderRadius: '18px'
                      }}
                    >
                      <Share2 size={18} aria-hidden="true" />
                      {shouldShowPostShareCount(post) && (
                        <span className="home-post-action-count">{post.sharesCount ?? 0}</span>
                      )}
                    </button>

                    {post.type === 'opportunity' && (
                      <button
                        type="button"
                        onClick={() => openOpportunityApplication(post)}
                        disabled={pendingApplicationPostIds.includes(String(post.id))}
                        style={{
                          background: post.applied ? '#e6f4ea' : '#1a73e8',
                          color: post.applied ? '#137333' : '#fff',
                          border: 'none',
                          padding: '6px 16px',
                          borderRadius: '20px',
                          cursor: pendingApplicationPostIds.includes(String(post.id)) ? 'wait' : 'pointer',
                          fontWeight: '600',
                          fontSize: '13px'
                        }}
                      >
                        {post.applied ? '✓ Applied' : '⚡ Apply Now'}
                      </button>
                    )}
                    {post.type !== 'opportunity' && post.investorProfileId && !isOwnPost(post) && (
                      <>
                        <button
                          className="home-investor-interest"
                          type="button"
                          onClick={() => void sendInvestorInterest(post)}
                          disabled={['sending', 'sent'].includes(investorInterestStatus[getInvestorInterestKey(post)]?.status)}
                        >
                          {investorInterestStatus[getInvestorInterestKey(post)]?.status === 'sending'
                            ? 'Sending…'
                            : investorInterestStatus[getInvestorInterestKey(post)]?.status === 'sent'
                              ? 'Interest sent'
                              : 'Interested'}
                        </button>
                        {investorInterestStatus[getInvestorInterestKey(post)]?.status === 'error' && (
                          <span className="home-investor-interest-error" role="alert">
                            {investorInterestStatus[getInvestorInterestKey(post)].message}
                          </span>
                        )}
                      </>
                    )}
                      </>
                    )}

                    {isOwnOpportunity ? (
                      <div className="home-opportunity-owner-actions">
                      <button
                        type="button"
                        onClick={() => startEditingOpportunity(post)}
                        title="Edit casting call"
                      >
                        Edit casting call
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(post.id)}
                        title="Delete casting call"
                      >
                        Delete
                      </button>
                      </div>
                    ) : canManagePost(post) ? (
                      <button
                        type="button"
                        onClick={() => handleDelete(post.id)}
                        style={{
                          marginLeft: 'auto',
                          background: 'transparent',
                          border: 'none',
                          color: '#d93025',
                          cursor: 'pointer',
                          fontSize: '12px'
                        }}
                        title="Delete post"
                      >
                        🗑️ Delete
                      </button>
                    ) : null}
                  </div>

                  {postComments.length > 0 && (
                    <div className="home-post-comments">
                      {commentsToShow.map((comment) => {
                        const commentId = String(comment.id)
                        const actionKey = `${postId}:${commentId}`
                        const deleteActionKey = `${postId}:comment:${commentId}`
                        const replies = Array.isArray(comment.replies) ? comment.replies : []
                        const isReplying = replyingToCommentKey === actionKey
                        const isLikePending = pendingCommentLikeKeys.includes(actionKey)
                        const isDeletePending = pendingCommentDeleteKeys.includes(deleteActionKey)
                        const commentAuthorName = comment.authorName || 'Cinema Professional'
                        const commentAuthorAvatar = comment.authorAvatar
                          || comment.profileImageUrl
                          || comment.avatar
                          || getProfilePhotoByName(commentAuthorName, commentProfiles)
                        const commentAuthorInitial = commentAuthorName.trim().charAt(0).toUpperCase() || 'T'
                        return (
                          <div className="home-post-comment" key={comment.id}>
                            <div className="home-post-comment-body">
                              <div className="home-post-comment-heading">
                                <span className="home-post-comment-avatar" aria-hidden="true">
                                  {commentAuthorInitial}
                                  {commentAuthorAvatar && (
                                    <img
                                      src={commentAuthorAvatar}
                                      alt=""
                                      onError={(event) => {
                                        event.currentTarget.hidden = true
                                      }}
                                    />
                                  )}
                                </span>
                                <strong>{commentAuthorName}</strong>
                                {comment.createdAt && <small>{comment.createdAt}</small>}
                              </div>
                              <p>{comment.text}</p>
                            </div>
                            <div className="home-post-comment-actions">
                              <button
                                type="button"
                                className={comment.likedByMe ? 'is-liked' : ''}
                                aria-pressed={Boolean(comment.likedByMe)}
                                disabled={isLikePending}
                                onClick={() => handleCommentLike(post.id, comment)}
                              >
                                <ThumbsUp size={13} aria-hidden="true" />
                                {comment.likedByMe ? 'Liked' : 'Like'}
                                {Number(comment.likesCount) > 0 && <span>{comment.likesCount}</span>}
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  setReplyingToCommentKey((current) => current === actionKey ? '' : actionKey)
                                  setReplyText('')
                                }}
                                aria-expanded={isReplying}
                              >
                                <MessageCircle size={13} aria-hidden="true" />
                                Reply
                              </button>
                              {canDeletePostComment(comment, post) && (
                                <button
                                  type="button"
                                  className="home-post-comment-delete"
                                  onClick={() => void handleCommentDelete(post.id, comment.id)}
                                  disabled={isDeletePending}
                                  aria-label={`Delete comment by ${commentAuthorName}`}
                                >
                                  <Trash2 size={13} aria-hidden="true" />
                                  {isDeletePending ? 'Deleting…' : 'Delete'}
                                </button>
                              )}
                            </div>
                            {commentLikeErrors[actionKey] && (
                              <p className="home-post-comment-error" role="alert">
                                {commentLikeErrors[actionKey]}
                              </p>
                            )}
                            {commentDeleteErrors[deleteActionKey] && (
                              <p className="home-post-comment-error" role="alert">
                                {commentDeleteErrors[deleteActionKey]}
                              </p>
                            )}
                            {replies.length > 0 && (
                              <div className="home-post-replies">
                                {replies.map((reply) => {
                                  const replyDeleteActionKey = `${postId}:reply:${reply.id}`
                                  const isReplyDeletePending = pendingCommentDeleteKeys.includes(replyDeleteActionKey)
                                  const replyAuthorName = reply.authorName || 'Cinema Professional'
                                  const replyAuthorAvatar = reply.authorAvatar
                                    || reply.profileImageUrl
                                    || reply.avatar
                                    || getProfilePhotoByName(replyAuthorName, commentProfiles)
                                  const replyAuthorInitial = replyAuthorName.trim().charAt(0).toUpperCase() || 'T'
                                  return (
                                    <div className="home-post-reply" key={reply.id}>
                                      <div className="home-post-reply-heading">
                                        <span className="home-post-comment-avatar is-reply" aria-hidden="true">
                                          {replyAuthorInitial}
                                          {replyAuthorAvatar && (
                                            <img
                                              src={replyAuthorAvatar}
                                              alt=""
                                              onError={(event) => {
                                                event.currentTarget.hidden = true
                                              }}
                                            />
                                          )}
                                        </span>
                                        <strong>{replyAuthorName}</strong>
                                        {reply.createdAt && <small>{reply.createdAt}</small>}
                                      </div>
                                      <p>{reply.text}</p>
                                      {canDeletePostComment(reply, post) && (
                                        <div className="home-post-reply-actions">
                                          <button
                                            type="button"
                                            className="home-post-comment-delete"
                                            onClick={() => void handleCommentDelete(post.id, comment.id, reply.id)}
                                            disabled={isReplyDeletePending}
                                            aria-label={`Delete reply by ${replyAuthorName}`}
                                          >
                                            <Trash2 size={13} aria-hidden="true" />
                                            {isReplyDeletePending ? 'Deleting…' : 'Delete'}
                                          </button>
                                          {commentDeleteErrors[replyDeleteActionKey] && (
                                            <p className="home-post-comment-error" role="alert">
                                              {commentDeleteErrors[replyDeleteActionKey]}
                                            </p>
                                          )}
                                        </div>
                                      )}
                                    </div>
                                  )
                                })}
                              </div>
                            )}
                            {isReplying && (
                              <div className="home-post-reply-box">
                                <form
                                  className="home-post-reply-form"
                                  onSubmit={(event) => handleCommentReplySubmit(event, post.id, comment.id)}
                                >
                                  <input
                                    type="text"
                                    value={replyText}
                                    onChange={(event) => setReplyText(event.target.value)}
                                    placeholder={`Reply to ${comment.authorName || 'comment'}...`}
                                    aria-label={`Reply to ${comment.authorName || 'comment'}`}
                                  />
                                  <button
                                    type="submit"
                                    disabled={!replyText.trim() || Boolean(pendingReplyKey)}
                                  >
                                    {pendingReplyKey === actionKey ? 'Replying…' : 'Reply'}
                                  </button>
                                </form>
                                {replyErrors[actionKey] && (
                                  <p className="home-post-comment-error" role="alert">
                                    {replyErrors[actionKey]}
                                  </p>
                                )}
                              </div>
                            )}
                          </div>
                        )
                      })}
                      {postComments.length > 1 && (
                        <button
                          className="home-post-comments-toggle"
                          type="button"
                          onClick={() => setExpandedCommentPostIds((current) => (
                            areCommentsExpanded
                              ? current.filter((id) => id !== postId)
                              : [...current, postId]
                          ))}
                          aria-expanded={areCommentsExpanded}
                        >
                          {areCommentsExpanded
                            ? 'Show less'
                            : `Show more comments (${remainingCommentCount})`}
                        </button>
                      )}
                    </div>
                  )}

                  {/* Comment input remains optional; existing comments stay visible below the post. */}
                  {openCommentsPostId === String(post.id) && (
                    <div style={{ marginTop: '12px', background: '#f9f9f9', padding: '12px', borderRadius: '8px' }}>
                      <form onSubmit={(e) => handleCommentSubmit(e, post.id)} style={{ display: 'flex', gap: '8px' }}>
                        <input
                          type="text"
                          value={commentText}
                          onChange={(e) => setCommentText(e.target.value)}
                          placeholder="Write a comment..."
                          style={{ flex: 1, padding: '8px 12px', borderRadius: '20px', border: '1px solid #ccc', fontSize: '13px' }}
                        />
                        <button
                          type="submit"
                          disabled={!commentText.trim() || isCommentSubmitting}
                          style={{ background: '#1a73e8', color: '#fff', border: 'none', padding: '8px 16px', borderRadius: '20px', cursor: !commentText.trim() || isCommentSubmitting ? 'wait' : 'pointer', fontSize: '13px' }}
                        >
                          {isCommentSubmitting ? 'Posting...' : 'Post'}
                        </button>
                      </form>
                      {commentErrors[String(post.id)] && (
                        <p role="alert" style={{ margin: '8px 0 0', color: '#b3261e', fontSize: '12px' }}>
                          {commentErrors[String(post.id)]}
                        </p>
                      )}
                    </div>
                  )}
                </article>
                )
              })}
            </div>
            {selectedOpportunityPost && (
              <section className="home-opportunity-detail home-card" aria-label="Casting call details">
                <header className="home-opportunity-detail-header">
                  <span className="home-opportunity-detail-icon">
                    <BriefcaseBusiness size={20} aria-hidden="true" />
                  </span>
                  <div>
                    <strong>{selectedOpportunityPost.authorName || name}</strong>
                    <span>
                      Casting call
                      {selectedOpportunityPost.createdAt ? ` · ${selectedOpportunityPost.createdAt}` : ''}
                    </span>
                  </div>
                  <button
                    className="home-opportunity-detail-close"
                    type="button"
                    aria-label="Close casting call details"
                    onClick={() => setSelectedOpportunity(null)}
                  >
                    <X size={18} aria-hidden="true" />
                  </button>
                </header>
                <h1>{selectedOpportunityPost.title || selectedOpportunityDetails.projectName || 'Casting call'}</h1>
                <p className="home-opportunity-detail-meta">
                  {[
                    selectedOpportunityDetails.projectType,
                    selectedOpportunityDetails.actingRole,
                    selectedOpportunityDetails.locationPreference,
                  ].filter(Boolean).join(' · ') || 'Casting call details'}
                </p>
                {selectedOpportunityPost.content && (
                  <section className="home-opportunity-detail-section">
                    <h2>About this casting call</h2>
                    <p className="home-opportunity-description">{selectedOpportunityPost.content}</p>
                  </section>
                )}
                {selectedOpportunityMedia?.isVideo
                  ? <video className="home-opportunity-modal-media" src={selectedOpportunityMedia.url} controls />
                  : selectedOpportunityMedia && (
                    <img className="home-opportunity-modal-media" src={selectedOpportunityMedia.url} alt="Casting call attachment" />
                  )}
                {opportunityDetailGroups.map((group) => (
                  <section className="home-opportunity-detail-section" key={group.title}>
                    <h2>{group.title}</h2>
                    <dl className="home-opportunity-facts">
                      {group.fields
                        .filter((field) => {
                          const hasGenderPreferences = Array.isArray(selectedOpportunityDetails.genderPreferences)
                            && selectedOpportunityDetails.genderPreferences.length > 0
                          if (field === 'genderPreferences') return hasGenderPreferences
                          if (field === 'genderPreference') return !hasGenderPreferences
                          if (field === 'openingsByGender') {
                            return selectedOpportunityDetails.openingsByGender
                              && Object.keys(selectedOpportunityDetails.openingsByGender).length > 0
                          }
                          return true
                        })
                        .map((field) => (
                        <div key={field}>
                          <dt>{formatOpportunityFieldLabel(field)}</dt>
                          <dd>{formatOpportunityFieldValue(selectedOpportunityDetails[field], field)}</dd>
                        </div>
                      ))}
                    </dl>
                  </section>
                ))}
                {selectedOpportunityAdditionalDetails.length > 0 && (
                  <section className="home-opportunity-detail-section">
                    <h2>Additional details</h2>
                    <dl className="home-opportunity-facts">
                      {selectedOpportunityAdditionalDetails.map(([field, value]) => (
                        <div key={field}>
                          <dt>{formatOpportunityFieldLabel(field)}</dt>
                          <dd>{formatOpportunityFieldValue(value)}</dd>
                        </div>
                      ))}
                    </dl>
                  </section>
                )}
                <div className="home-opportunity-detail-actions">
                  {isOwnSelectedOpportunity ? (
                    <>
                      <span className="home-opportunity-owner-note">You posted this casting call</span>
                      <button
                        className="home-opportunity-apply"
                        type="button"
                        onClick={() => void openOpportunityApplications(selectedOpportunityPost)}
                      >
                        View Applications ({selectedOpportunityPost.applicationsCount || 0})
                      </button>
                    </>
                  ) : (
                    <button
                      className="home-opportunity-apply"
                      type="button"
                      onClick={() => openOpportunityApplication(selectedOpportunityPost)}
                      disabled={pendingApplicationPostIds.includes(String(selectedOpportunityPost.id))}
                    >
                      {selectedOpportunityPost.applied ? '✓ Applied' : '⚡ Apply Now'}
                    </button>
                  )}
                </div>
              </section>
            )}
            </div>
          )}
          </>
          )}
        </div>

        <aside className="home-promotion-column" aria-label="Promotions and Explore">
          {!isPromotionHidden && (
            <section className="home-promotion-card" aria-label="Sponsored movie industry promotion">
              <div className="home-promotion-art">
                <span className="home-promotion-label">PROMOTED</span>
                <button
                  className="home-promotion-menu-icon"
                  type="button"
                  aria-label="Ad options"
                  aria-haspopup="dialog"
                  aria-expanded={isHidePromotionPromptOpen}
                  onClick={() => setIsHidePromotionPromptOpen((isOpen) => !isOpen)}
                >
                  <MoreVertical size={18} aria-hidden="true" />
                </button>
                <span className="home-promotion-art-title">THE NEXT<br />BIG PICTURE</span>
                <span className="home-promotion-studio">NORTHSTAR FILMS</span>
              </div>
              {isHidePromotionPromptOpen && (
                <div className="home-promotion-hide-prompt" role="dialog" aria-labelledby="home-promotion-hide-title">
                  <h2 id="home-promotion-hide-title">Hide this ad?</h2>
                  <p>This promotion will be removed from your sidebar.</p>
                  <div className="home-promotion-hide-actions">
                    <button type="button" onClick={() => setIsHidePromotionPromptOpen(false)}>Cancel</button>
                    <button
                      type="button"
                      onClick={() => {
                        setIsPromotionHidden(true)
                        setIsHidePromotionPromptOpen(false)
                      }}
                    >
                      Hide ad
                    </button>
                  </div>
                </div>
              )}
              <div className="home-promotion-content">
                <h2>Bring your next story to the screen</h2>
                <p>Northstar Films is meeting new writers, actors, and independent filmmakers for upcoming productions.</p>
                <span className="home-promotion-caption">Make your mark in the movies.</span>
                <button
                  className="home-promotion-button"
                  type="button"
                  onClick={() => handlePromoClick("Northstar Films Production")}
                >
                  Explore film careers
                </button>
              </div>
            </section>
          )}
          <section className="home-card home-sidebar-stats home-discovery-card" aria-labelledby="home-discovery-heading">
            <h2 className="home-discovery-heading" id="home-discovery-heading">Explore</h2>
            <Link
              className="home-explore-promo home-explore-promo-film"
              to={filmMakerVisited ? '/protect-producer' : '/entertainments'}
              onClick={() => {
                if (!filmMakerVisited) {
                  markFilmMakerVisited(accountEmail)
                  setFilmMakerVisited(true)
                }
              }}
            >
              <span className="home-explore-promo-art">
                <img src="/assets/explore-filmmakers.png" alt="" />
              </span>
              <span className="home-explore-promo-copy">
                <strong>{filmMakerVisited ? 'Manage Films' : 'Are you a Filmmaker?'}</strong>
                <small>{filmMakerVisited ? 'Manage your film projects' : 'Bring your story to the screen'}</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </Link>
            <button
              className="home-explore-promo home-explore-promo-invest home-explore-promo-button"
              type="button"
              onClick={openInvestorRequest}
            >
              <span className="home-explore-promo-art">
                <img src="/assets/explore-investors.png" alt="" />
              </span>
              <span className="home-explore-promo-copy">
                <strong>Are you an Investor / Producer ?</strong>
                <small>Discover creative opportunities</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </button>
            <a className="home-explore-promo home-explore-promo-distribute" href="/distributions">
              <span className="home-explore-promo-art">
                <img src="/assets/explore-distribution.png" alt="" />
              </span>
              <span className="home-explore-promo-copy">
                <strong>Are you a Distributor ?</strong>
                <small>Get stories in front of audiences</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </a>
            {activeNavigation === 'opportunities' && (
              <Link className="home-explore-promo home-explore-promo-tickets home-explore-promo-tickets-mobile" to="/entertainments">
                <span className="home-explore-promo-art">
                  <img src="/assets/explore-tickets.png" alt="" />
                </span>
                <span className="home-explore-promo-copy">
                  <strong>Book a ticket</strong>
                  <small>Explore films and showtimes</small>
                  <ArrowUpRight size={16} aria-hidden="true" />
                </span>
              </Link>
            )}
            <section className="home-explore-promo home-explore-promo-asset" aria-label="For film asset owners">
              <span className="home-explore-promo-art">
                <img src="/assets/explore-assets.svg" alt="" />
              </span>
              <span className="home-explore-promo-copy">
                <strong>Are you an Asset manager ?</strong>
                <small>Showcase your production assets to the film community</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </section>
            {activeNavigation === 'home' && (
              <Link className="home-explore-promo home-explore-promo-tickets home-explore-promo-tickets-mobile" to="/entertainments">
                <span className="home-explore-promo-art">
                  <img src="/assets/explore-tickets.png" alt="" />
                </span>
                <span className="home-explore-promo-copy">
                  <strong>Book a ticket</strong>
                  <small>Explore films and showtimes</small>
                  <ArrowUpRight size={16} aria-hidden="true" />
                </span>
              </Link>
            )}
          </section>
        </aside>
      </div>
    </section>
    {applicationsPost && (
      <div
        className="home-connections-backdrop"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) setApplicationsPost(null)
        }}
      >
        <section
          className="home-connections-dialog home-casting-applications-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-casting-applications-title"
          onMouseDown={(event) => event.stopPropagation()}
        >
          <header className="home-connections-dialog-header">
            <div>
              <h2 id="home-casting-applications-title">Applications</h2>
              <p>{applicationsPost.title || 'Casting call'}</p>
            </div>
            <button
              className="home-connections-close"
              type="button"
              aria-label="Close applications"
              onClick={() => setApplicationsPost(null)}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          <div className="home-casting-applications-content">
            {isLoadingOpportunityApplications ? (
              <p className="home-casting-applications-message">Loading applications…</p>
            ) : opportunityApplicationsError ? (
              <p className="home-casting-applications-message is-error" role="alert">
                {opportunityApplicationsError}
              </p>
            ) : opportunityApplications.length === 0 ? (
              <p className="home-casting-applications-message">No applications received yet.</p>
            ) : (
              opportunityApplications.map((application) => {
                const applicantName = application.applicantName || 'Applicant'
                const details = application.applicationDetails
                  && typeof application.applicationDetails === 'object'
                  ? application.applicationDetails
                  : {}
                const detailFields = [
                  ['Age', 'age'],
                  ['Gender', 'gender'],
                  ['Mobile', 'mobileNumber'],
                  ['Email', 'emailAddress'],
                  ['Location', 'currentLocation'],
                  ['Languages', 'languagesKnown'],
                  ['Height', 'height'],
                  ['Weight', 'weight'],
                  ['Experience', 'experienceLevel'],
                  ['Previous work', 'previousWork'],
                  ['Skills', 'actingSkills'],
                  ['Portfolio', 'portfolioUrl'],
                ].filter(([, field]) => details[field])
                const hasShortlistDecision = application.shortlistDecisionMade === true
                  || application.shortlisted === true
                const shortlistStatus = application.shortlisted
                  ? 'shortlisted'
                  : 'not-shortlisted'
                const shortlistStatusClass = !hasShortlistDecision
                  ? ''
                  : application.shortlisted
                    ? 'is-shortlisted'
                    : 'is-not-shortlisted'

                return (
                  <article className="home-casting-application-card" key={application.id}>
                    <div className="home-casting-application-heading">
                      <button
                        className="home-casting-applicant-profile"
                        type="button"
                        onClick={() => {
                          setApplicationsPost(null)
                          viewPostAuthor({
                            authorId: application.applicantId,
                            authorName: applicantName,
                            authorAvatar: application.profileImageUrl,
                          })
                        }}
                      >
                        {application.profileImageUrl
                          ? <img src={application.profileImageUrl} alt="" />
                          : <span aria-hidden="true">{applicantName.charAt(0).toUpperCase()}</span>}
                        <span className="home-casting-applicant-name">
                          <strong>{applicantName}</strong>
                          {application.headline && <small>{application.headline}</small>}
                        </span>
                      </button>
                      <label className="home-casting-shortlist-field">
                        <span className="home-sr-only">Shortlist status for {applicantName}</span>
                        <select
                          className={shortlistStatusClass}
                          value={hasShortlistDecision ? shortlistStatus : ''}
                          onChange={(event) => {
                            if (!event.target.value) return
                            void updateApplicationShortlist(
                              application,
                              event.target.value === 'shortlisted',
                            )
                          }}
                          disabled={updatingApplicationIds.includes(String(application.id)) || hasShortlistDecision}
                        >
                          <option value="" disabled>Select category</option>
                          <option value="not-shortlisted">Not shortlisted</option>
                          <option value="shortlisted">Shortlisted</option>
                        </select>
                      </label>
                    </div>
                    {application.experience && (
                      <p className="home-casting-application-experience">
                        <strong>Experience:</strong> {application.experience}
                      </p>
                    )}
                    {detailFields.length > 0 && (
                      <dl className="home-casting-application-details">
                        {detailFields.map(([label, field]) => (
                          <div key={field}>
                            <dt>{label}</dt>
                            <dd>{String(details[field])}</dd>
                          </div>
                        ))}
                      </dl>
                    )}
                    {application.appliedAt && (
                      <small className="home-casting-application-date">
                        Applied {new Date(application.appliedAt).toLocaleString()}
                      </small>
                    )}
                  </article>
                )
              })
            )}
          </div>
        </section>
      </div>
    )}
    {applyingOpportunity && (
      <div
        className="home-connections-backdrop"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget && !isSubmittingApplication) {
            setApplyingOpportunity(null)
          }
        }}
      >
        <section
          className="home-connections-dialog home-application-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-application-title"
        >
          <header className="home-connections-dialog-header">
            <div>
              <h2 id="home-application-title">Apply for casting</h2>
              <p>{applyingOpportunity.title || 'Review your profile information'}</p>
            </div>
            <button
              className="home-connections-close"
              type="button"
              aria-label="Close application form"
              onClick={() => setApplyingOpportunity(null)}
              disabled={isSubmittingApplication}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          <form
            className="home-application-form"
            id="home-application-form"
            onSubmit={submitOpportunityApplication}
            onKeyDown={(event) => {
              if (
                event.key === 'Enter'
                && event.target instanceof HTMLElement
                && event.target.tagName !== 'TEXTAREA'
                && event.target.tagName !== 'BUTTON'
              ) {
                event.preventDefault()
              }
            }}
          >
            <div className="home-application-progress" aria-label={`Application step ${applicationStep} of 4`}>
              {Array.from({ length: 4 }, (_, index) => (
                <span
                  key={index}
                  className={index + 1 <= applicationStep ? 'is-active' : ''}
                >
                  {index + 1}
                </span>
              ))}
            </div>

            {applicationStep === 1 && (
              <>
                <h3 className="home-application-step-title">01. Applicant Information</h3>
                <div className="home-application-grid">
                  <label className="home-application-field home-application-full">
                    <span>Full Name</span>
                    <input value={applicationData.fullName || ''} onChange={(event) => updateApplicationData('fullName', event.target.value)} placeholder="Full Name" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field">
                    <span>Age</span>
                    <input type="number" value={applicationData.age || ''} onChange={(event) => updateApplicationData('age', event.target.value)} placeholder="Age" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field">
                    <span>Gender</span>
                    <select value={applicationData.gender || ''} onChange={(event) => updateApplicationData('gender', event.target.value)} disabled={isSubmittingApplication}>
                      <option value="">Gender</option>
                      <option>Male</option>
                      <option>Female</option>
                      <option>Non-binary</option>
                      <option>Prefer not to say</option>
                    </select>
                  </label>
                  <label className="home-application-field home-application-full">
                    <span>Mobile Number</span>
                    <input value={applicationData.mobileNumber || ''} onChange={(event) => updateApplicationData('mobileNumber', event.target.value)} placeholder="Mobile Number" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field home-application-full">
                    <span>Email Address</span>
                    <input type="email" value={applicationData.emailAddress || ''} onChange={(event) => updateApplicationData('emailAddress', event.target.value)} placeholder="Email Address" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field home-application-full">
                    <span>Current Location</span>
                    <input value={applicationData.currentLocation || ''} onChange={(event) => updateApplicationData('currentLocation', event.target.value)} placeholder="Current Location" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field home-application-full">
                    <span>Languages Known</span>
                    <input value={applicationData.languagesKnown || ''} onChange={(event) => updateApplicationData('languagesKnown', event.target.value)} placeholder="Languages Known" disabled={isSubmittingApplication} />
                  </label>
                </div>
                <h3 className="home-application-step-title">02. Physical Details</h3>
                <div className="home-application-grid">
                  <label className="home-application-field">
                    <span>Height (cm)</span>
                    <input value={applicationData.height || ''} onChange={(event) => updateApplicationData('height', event.target.value)} placeholder="Height (cm)" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field">
                    <span>Weight (kg)</span>
                    <input value={applicationData.weight || ''} onChange={(event) => updateApplicationData('weight', event.target.value)} placeholder="Weight (kg)" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field home-application-full">
                    <span>Body Type (Optional)</span>
                    <input value={applicationData.bodyType || ''} onChange={(event) => updateApplicationData('bodyType', event.target.value)} placeholder="Body Type (Optional)" disabled={isSubmittingApplication} />
                  </label>
                </div>
                <h3 className="home-application-step-title">03. Acting Experience</h3>
                <div className="home-application-grid">
                  <label className="home-application-field home-application-full">
                    <span>Experience Level</span>
                    <select value={applicationData.experienceLevel || ''} onChange={(event) => updateApplicationData('experienceLevel', event.target.value)} disabled={isSubmittingApplication}>
                      <option value="">Experience Level</option>
                      <option>Newcomer</option>
                      <option>Beginner</option>
                      <option>Intermediate</option>
                      <option>Experienced</option>
                      <option>Professional</option>
                    </select>
                  </label>
                  <label className="home-application-field home-application-full">
                    <span>Previous Movies / Projects</span>
                    <textarea value={applicationData.previousWork || ''} onChange={(event) => updateApplicationData('previousWork', event.target.value)} placeholder="Previous Movies / Projects" disabled={isSubmittingApplication} />
                  </label>
                  <label className="home-application-field home-application-full">
                    <span>Acting Skills and Special Talents</span>
                    <input value={applicationData.actingSkills || ''} onChange={(event) => updateApplicationData('actingSkills', event.target.value)} placeholder="Acting Skills and Special Talents" disabled={isSubmittingApplication} />
                  </label>
                </div>
              </>
            )}

            {applicationStep === 2 && (
              <>
                <h3 className="home-application-step-title">04. Portfolio &amp; Audition</h3>
                <div className="home-application-grid">
                  {[
                    ['profilePhotoUrl', 'Profile Photo URL'],
                    ['fullLengthPhotoUrl', 'Full-Length Photo URL'],
                    ['portfolioUrl', 'Portfolio URL'],
                    ['introductionVideoUrl', 'Introduction Video URL'],
                    ['auditionVideoUrl', 'Audition Video URL'],
                  ].map(([field, label]) => (
                    <label className="home-application-field home-application-full" key={field}>
                      <span>{label}</span>
                      <input value={applicationData[field] || ''} onChange={(event) => updateApplicationData(field, event.target.value)} placeholder={label} disabled={isSubmittingApplication} />
                    </label>
                  ))}
                </div>
              </>
            )}

            {applicationStep === 3 && (
              <>
                <h3 className="home-application-step-title">05. Applicant Declaration</h3>
                <label className="home-application-check">
                  <input type="checkbox" checked={Boolean(applicationData.accurateInformation)} onChange={(event) => updateApplicationData('accurateInformation', event.target.checked)} disabled={isSubmittingApplication} />
                  <span>I confirm that my information and portfolio are accurate.</span>
                </label>
                <label className="home-application-check">
                  <input type="checkbox" checked={Boolean(applicationData.termsAccepted)} onChange={(event) => updateApplicationData('termsAccepted', event.target.checked)} disabled={isSubmittingApplication} />
                  <span>I agree to the casting terms and privacy policy.</span>
                </label>
              </>
            )}

            {applicationStep === 4 && (
              <>
                <h3 className="home-application-step-title">Review Application</h3>
                <div className="home-application-review">
                  {Object.entries(applicationData)
                    .filter(([field, value]) => !['accurateInformation', 'termsAccepted'].includes(field) && value)
                    .map(([field, value]) => (
                      <div key={field}><span>{field.replace(/([A-Z])/g, ' $1').replace(/^./, (letter) => letter.toUpperCase())}</span><strong>{String(value)}</strong></div>
                    ))}
                </div>
              </>
            )}

            {applicationError && <p className="home-application-error" role="alert">{applicationError}</p>}
          </form>
          <div className="home-application-actions">
            {applicationStep === 1 ? (
              <button
                className="home-application-cancel"
                type="button"
                onClick={() => setApplyingOpportunity(null)}
                disabled={isSubmittingApplication}
              >
                Cancel
              </button>
            ) : (
              <button
                className="home-application-previous"
                type="button"
                onClick={() => setApplicationStep((step) => Math.max(1, step - 1))}
                disabled={isSubmittingApplication}
              >
                Previous
              </button>
            )}
            {applicationStep < 4 ? (
              <button
                className="home-application-submit"
                type="button"
                onClick={goToNextApplicationStep}
                disabled={isSubmittingApplication}
              >
                Next
              </button>
            ) : (
              <button
                className="home-application-submit"
                type="submit"
                form="home-application-form"
                disabled={isSubmittingApplication}
              >
                {isSubmittingApplication ? 'Submitting…' : 'Submit Application'}
              </button>
            )}
          </div>
        </section>
      </div>
    )}
    {isProfileViewersOpen && (
      <div className="home-profile-viewers-backdrop">
        <button
          className="home-profile-viewers-back"
          type="button"
          onClick={() => setIsProfileViewersOpen(false)}
        >
          <ArrowLeft size={17} aria-hidden="true" />
          Back to Home
        </button>
        <section
          className="home-connections-dialog home-profile-viewers-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-profile-viewers-title"
        >
          <header className="home-connections-dialog-header home-profile-viewers-header">
            <span className="home-profile-viewers-heading-icon">
              <Eye size={20} aria-hidden="true" />
            </span>
            <div>
              <h2 id="home-profile-viewers-title">Profile viewers</h2>
              <p>
                {profileViewers.length} {profileViewers.length === 1 ? 'person' : 'people'} viewed your profile
              </p>
            </div>
          </header>
          {profileViewersError ? (
            <p className="home-profile-viewers-message" role="alert">{profileViewersError}</p>
          ) : profileViewers.length ? (
            <ul className="home-profile-viewers-list">
              {profileViewers.map((profile) => {
                const profileName = profile.name || 'TrueHunt member'
                const profileImage = profile.profileImageUrl || profile.avatar || ''
                const profileHeadline = profile.headline || profile.role || ''
                const initials = profileName
                  .split(/\s+/)
                  .map((part) => part[0])
                  .join('')
                  .slice(0, 2)
                const openProfile = () => {
                  setIsProfileViewersOpen(false)
                  onViewConnection?.({
                    ...profile,
                    userId: profile.userId || profile.id || '',
                    profileImageUrl: profileImage,
                  })
                }

                return (
                  <li className="home-profile-viewer-item" key={profile.userId || profile.id || profileName}>
                    <button
                      type="button"
                      className={`home-avatar home-profile-viewer-avatar${profileImage ? '' : ' is-placeholder'}`}
                      aria-label={`View ${profileName}'s profile`}
                      onClick={openProfile}
                    >
                      {profileImage ? <img src={profileImage} alt="" /> : initials}
                    </button>
                    <div className="home-profile-viewer-details">
                      <strong>{profileName}</strong>
                      {profileHeadline && <small>{profileHeadline}</small>}
                    </div>
                    <button className="home-profile-viewer-action" type="button" onClick={openProfile}>
                      <Eye size={14} aria-hidden="true" />
                      View profile
                    </button>
                  </li>
                )
              })}
            </ul>
          ) : (
            <div className="home-connections-empty">
              <Eye size={24} aria-hidden="true" />
              <p>No profile viewers yet.</p>
            </div>
          )}
        </section>
      </div>
    )}
    {isInvestorRequestOpen && (
      <div
        className="home-connections-backdrop"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget && !isSubmittingInvestorRequest) {
            setIsInvestorRequestOpen(false)
          }
        }}
      >
        <section
          className="home-connections-dialog home-investor-request-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-investor-request-title"
        >
          <header className="home-connections-dialog-header">
            <div>
              <h2 id="home-investor-request-title">Request investor access</h2>
              <p>Publish your profile to Investor Search and the home feed.</p>
            </div>
            <button
              className="home-connections-close"
              type="button"
              aria-label="Close investor request"
              onClick={() => setIsInvestorRequestOpen(false)}
              disabled={isSubmittingInvestorRequest}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          <form className="home-investor-request-form" onSubmit={submitInvestorRequest}>
            <fieldset className="home-investor-role-field">
              <legend>I am a</legend>
              <div className="home-investor-role-options">
                {['Producer', 'Investor', 'Crowd funder'].map((role) => (
                  <label key={role}>
                    <input
                      type="radio"
                      name="investor-request-role"
                      value={role}
                      checked={investorRequest.role === role}
                      onChange={(event) => setInvestorRequest((current) => ({ ...current, role: event.target.value }))}
                      required
                      disabled={isSubmittingInvestorRequest || investorRequestStatus === 'PUBLISHED'}
                    />
                    <span>{role}</span>
                  </label>
                ))}
              </div>
            </fieldset>
            <label className="home-application-field">
              <span>Name</span>
              <input
                type="text"
                value={investorRequest.name}
                onChange={(event) => setInvestorRequest((current) => ({ ...current, name: event.target.value }))}
                maxLength={120}
                required
                disabled={isSubmittingInvestorRequest || investorRequestStatus === 'PUBLISHED'}
              />
            </label>
            <label className="home-application-field">
              <span>Investment budget</span>
              <input
                type="text"
                value={investorRequest.budget}
                onChange={(event) => setInvestorRequest((current) => ({ ...current, budget: event.target.value }))}
                placeholder="For example, ₹10,00,000"
                maxLength={100}
                required
                disabled={isSubmittingInvestorRequest || investorRequestStatus === 'PUBLISHED'}
              />
            </label>
            <label className="home-application-field">
              <span>Description</span>
              <textarea
                value={investorRequest.description}
                onChange={(event) => setInvestorRequest((current) => ({ ...current, description: event.target.value }))}
                placeholder="Tell creators what you are looking to support."
                maxLength={1000}
                rows={4}
                required
                disabled={isSubmittingInvestorRequest || investorRequestStatus === 'PUBLISHED'}
              />
            </label>
            <label className="home-investor-consent">
              <input
                type="checkbox"
                checked={investorRequest.consentAccepted}
                onChange={(event) => setInvestorRequest((current) => ({ ...current, consentAccepted: event.target.checked }))}
                required
                disabled={isSubmittingInvestorRequest || investorRequestStatus === 'PUBLISHED'}
              />
              <span>I confirm these details are correct and agree to publish my name, role, description, and budget on TrueHunt. Contact details are not requested or published.</span>
            </label>
            {investorRequestStatus === 'PUBLISHED' && (
              <p className="home-investor-request-status" role="status">
                Your profile is published in Investor Search and the home feed.
              </p>
            )}
            {investorRequestError && <p className="home-application-error" role="alert">{investorRequestError}</p>}
            <div className="home-investor-request-actions">
              <button
                className="home-application-cancel"
                type="button"
                onClick={() => setIsInvestorRequestOpen(false)}
                disabled={isSubmittingInvestorRequest}
              >
                Close
              </button>
              <button
                className="home-application-submit"
                type="submit"
                disabled={isSubmittingInvestorRequest || investorRequestStatus === 'PUBLISHED'}
              >
                {isSubmittingInvestorRequest
                  ? 'Publishing…'
                  : investorRequestStatus === 'PUBLISHED' ? 'Published' : 'Publish profile'}
              </button>
            </div>
          </form>
        </section>
      </div>
    )}
    {openPostMenuId && (() => {
      const menuPost = visiblePosts.find((post) => String(post.id) === openPostMenuId)
      if (!menuPost) return null
      const isMenuPostSaved = savedPostIds.includes(String(menuPost.id))
      return (
        <div
          className="home-post-menu-popover"
          role="menu"
          style={{ top: `${postMenuPosition.top}px`, left: `${postMenuPosition.left}px` }}
        >
          <button type="button" role="menuitem" onClick={() => handleSavePost(menuPost)}>
            <Bookmark size={18} fill={isMenuPostSaved ? 'currentColor' : 'none'} aria-hidden="true" />
            <span>
              <strong>{isMenuPostSaved ? 'Remove from saved' : 'Save'}</strong>
              <small>{isMenuPostSaved ? 'Remove this post from your saved items' : 'Save for later'}</small>
            </span>
          </button>
          <button type="button" role="menuitem" onClick={() => handleCopyPostLink(menuPost.id)}>
            <Link2 size={18} aria-hidden="true" />
            <span><strong>Copy link to post</strong></span>
          </button>
          <button type="button" role="menuitem" onClick={() => handleHidePostsByAuthor(menuPost)}>
            <EyeOff size={18} aria-hidden="true" />
            <span><strong>Hide posts </strong></span>
          </button>
          {!isOwnPost(menuPost) && (
            <button type="button" role="menuitem" onClick={() => openBlockPostAuthor(menuPost)}>
              <Ban size={18} aria-hidden="true" />
              <span><strong>Block </strong></span>
            </button>
          )}
          <button type="button" role="menuitem" onClick={() => handleNotInterested(menuPost.id)}>
            <Ban size={18} aria-hidden="true" />
            <span><strong>Not interested</strong></span>
          </button>
          {!isOwnPost(menuPost) && (
            <button type="button" role="menuitem" onClick={() => handleReportPost(menuPost)}>
              <Flag size={18} aria-hidden="true" />
              <span><strong>Report post</strong></span>
            </button>
          )}
        </div>
      )
    })()}
    {blockTarget && (
      <div
        className="home-block-dialog-backdrop"
        role="presentation"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) setBlockTarget(null)
        }}
      >
        <form
          className="home-block-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-block-dialog-title"
          onSubmit={handleConfirmBlockPostAuthor}
        >
          <h2 id="home-block-dialog-title">Block {blockTarget.authorName || 'this profile'}?</h2>
          <div className="home-block-dialog-profile">
            <span className={`home-avatar${getPostAuthorAvatar(blockTarget) ? '' : ' is-placeholder'}`} aria-hidden="true">
              {getPostAuthorAvatar(blockTarget)
                ? <img src={getPostAuthorAvatar(blockTarget)} alt="" />
                : <ProfileAvatarPlaceholder />}
            </span>
            <strong>{blockTarget.authorName}</strong>
          </div>
          <fieldset className="home-block-reasons">
            <legend>Choose a reason</legend>
            {postBlockReasons.map((reason) => (
              <label className={blockReason === reason ? 'is-selected' : ''} key={reason}>
                <input
                  type="radio"
                  name="post-block-reason"
                  value={reason}
                  checked={blockReason === reason}
                  onChange={(event) => setBlockReason(event.target.value)}
                />
                <span>{reason}</span>
              </label>
            ))}
          </fieldset>
          {blockReason === 'Custom reason' && (
            <label className="home-block-custom-reason" htmlFor="home-block-custom-reason">
              Custom reason
              <textarea
                id="home-block-custom-reason"
                value={customBlockReason}
                onChange={(event) => setCustomBlockReason(event.target.value)}
                placeholder="Tell us why you are blocking this profile"
                maxLength={240}
                required
              />
            </label>
          )}
          <div className="home-block-dialog-actions">
            <button type="button" onClick={() => setBlockTarget(null)}>Cancel</button>
            <button
              className="home-block-confirm"
              type="submit"
              disabled={!blockReason || (blockReason === 'Custom reason' && !customBlockReason.trim())}
            >
              Block profile
            </button>
          </div>
        </form>
      </div>
    )}
    {reportTarget && (
      <div
        className="home-block-dialog-backdrop"
        role="presentation"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget && !isSubmittingReport) setReportTarget(null)
        }}
      >
        <form
          className="home-block-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-report-dialog-title"
          onSubmit={handleSubmitPostReport}
        >
          <h2 id="home-report-dialog-title">Report post</h2>
          <p className="home-report-dialog-description">
            Choose the reason that best describes your concern. Reports are sent to our moderation team.
          </p>
          <fieldset className="home-block-reasons">
            <legend>Why are you reporting this post?</legend>
            {postReportReasons.map((reason) => (
              <label className={reportReason === reason ? 'is-selected' : ''} key={reason}>
                <input
                  type="radio"
                  name="post-report-reason"
                  value={reason}
                  checked={reportReason === reason}
                  onChange={(event) => {
                    setReportReason(event.target.value)
                    setReportError('')
                  }}
                  required
                />
                <span>{reason}</span>
              </label>
            ))}
          </fieldset>
          {reportReason === 'Other' && (
            <label className="home-block-custom-reason" htmlFor="home-report-custom-reason">
              Describe the issue
              <textarea
                id="home-report-custom-reason"
                value={customReportReason}
                onChange={(event) => {
                  setCustomReportReason(event.target.value)
                  setReportError('')
                }}
                placeholder="Tell us why this post should be reviewed"
                maxLength={500}
                required
              />
            </label>
          )}
          {reportError && <p className="home-report-error" role="alert">{reportError}</p>}
          <div className="home-block-dialog-actions">
            <button type="button" onClick={() => setReportTarget(null)} disabled={isSubmittingReport}>
              Cancel
            </button>
            <button
              className="home-block-confirm"
              type="submit"
              disabled={
                isSubmittingReport
                || !reportReason
                || (reportReason === 'Other' && !customReportReason.trim())
              }
            >
              {isSubmittingReport ? 'Submitting…' : 'Submit report'}
            </button>
          </div>
        </form>
      </div>
    )}
    {postLikesDialog && (
      <div
        className="home-post-likes-backdrop"
        role="presentation"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) setPostLikesDialog(null)
        }}
      >
        <section
          className="home-post-likes-sheet"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-post-likes-title"
        >
          <div className="home-post-likes-handle" aria-hidden="true" />
          <header className="home-post-likes-header">
            <div>
              <h2 id="home-post-likes-title">Likes</h2>
              <p>{postLikesDialog.likesCount ?? 0} {Number(postLikesDialog.likesCount) === 1 ? 'like' : 'likes'}</p>
            </div>
            <button
              type="button"
              className="home-post-likes-close"
              aria-label="Close likes"
              onClick={() => setPostLikesDialog(null)}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          <div className="home-post-likes-content">
            {isLoadingPostLikers ? (
              <p className="home-post-likes-message" role="status">Loading likes…</p>
            ) : postLikersError ? (
              <div className="home-post-likes-message" role="alert">
                <p>{postLikersError}</p>
                <button type="button" onClick={() => setPostLikesDialog((current) => ({ ...current }))}>
                  Try again
                </button>
              </div>
            ) : postLikers.length ? (
              <ul className="home-post-likers-list">
                {postLikers.map((liker) => {
                  const likerName = String(liker.name || 'TrueHunt member').trim()
                  const likerImage = liker.profileImageUrl || liker.avatar || ''
                  const likerId = getConnectionProfileId(liker)
                  const isCurrentUser = isSameConnectionProfile(liker, {
                    userId: currentUserId,
                    id: profileDetails.id,
                    email: accountEmail || profileDetails.email,
                  })
                  const isConnected = connectedProfiles.some((profile) => (
                    isSameConnectionProfile(profile, liker)
                  ))
                  const isPending = Boolean(liker.isOutgoingPending)
                    || (Boolean(liker.isPending) && !liker.isIncomingPending)
                  const isConnecting = connectingLikerIds.includes(likerId)
                  const connectionError = likerConnectionErrors[likerId]
                  const initials = likerName
                    .split(/\s+/)
                    .map((part) => part[0])
                    .join('')
                    .slice(0, 2)

                  return (
                    <li key={liker.userId || liker.id || likerName}>
                      <div className="home-post-liker-row">
                      <div className="home-post-liker-profile">
                        <span className={`home-post-liker-avatar${likerImage ? '' : ' is-placeholder'}`}>
                          {likerImage ? <img src={likerImage} alt="" /> : initials || 'TH'}
                        </span>
                        <span className="home-post-liker-details">
                          <strong>{likerName}</strong>
                          {(liker.headline || liker.role) && <small>{liker.headline || liker.role}</small>}
                        </span>
                      </div>
                      {!isCurrentUser && (isConnected ? (
                        <button
                          type="button"
                          className="home-post-liker-action is-profile"
                          onClick={() => {
                            setPostLikesDialog(null)
                            onViewConnection?.({
                              ...liker,
                              userId: liker.userId || liker.id || '',
                              profileImageUrl: likerImage,
                            })
                          }}
                        >
                          View profile
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="home-post-liker-action"
                          disabled={isPending || isConnecting || !likerId}
                          onClick={() => connectWithLiker(liker)}
                        >
                          {isConnecting ? 'Connecting…' : isPending ? 'Requested' : 'Connect'}
                        </button>
                      ))}
                      </div>
                      {connectionError && <p className="home-post-liker-error" role="alert">{connectionError}</p>}
                    </li>
                  )
                })}
              </ul>
            ) : (
              <p className="home-post-likes-message">
                {postLikesDialog.localOnly || !/^\d+$/.test(String(postLikesDialog.id))
                  ? 'Likes are not available for this post.'
                  : Number(postLikesDialog.likesCount) > 0
                    ? 'Liker profiles are not available to display.'
                    : 'No likes yet.'}
              </p>
            )}
          </div>
        </section>
      </div>
    )}
    <Article
      profileDetails={profileDetails}
      profilePreview={profilePreview}
      hidden={hidden || !isWritingArticle}
      onBack={() => setIsWritingArticle(false)}
      onPublish={publishArticle}
    />
    {isOpportunityWizardOpen && (
      <OpportunitiesPage
        onClose={() => {
          setIsOpportunityWizardOpen(false)
          setEditingOpportunity(null)
        }}
        onPublish={publishOpportunity}
        initialData={editingOpportunity
          ? {
              ...editingOpportunity.opportunityDetails,
              projectName: editingOpportunity.opportunityDetails?.projectName || editingOpportunity.title || '',
            }
          : undefined}
        isEditing={Boolean(editingOpportunity)}
      />
    )}
    </>
  )
}

export default Home