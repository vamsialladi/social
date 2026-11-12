import { useEffect, useRef, useState } from 'react'
import { ArrowUpRight, BriefcaseBusiness, Eye, MessageCircle, Search, ThumbsUp, UsersRound, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import './Home.css'
import Article from './Article.jsx'
import OpportunitiesPage from './OpportunitiesPage.jsx'
import ProfileBanner from './ProfileBanner.jsx'
import api from '../../api'
import { hasVisitedFilmMaker, markFilmMakerVisited } from '../../services/filmMakerAccess'

function ProfileAvatarPlaceholder() {
  return (
    <svg className="home-avatar-placeholder" viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="50" r="50" style={{ fill: '#e5e7e8' }} />
      <circle cx="50" cy="35" r="21" />
      <path d="M10 100c2-24 18-42 40-42s38 18 40 42H10Z" />
    </svg>
  )
}

function getDismissedPostsStorageKey(userId) {
  const accountId = String(userId || 'guest').trim().toLowerCase()
  return `truehunt-not-interested-posts:${encodeURIComponent(accountId)}`
}

function getDismissedInvestorListingsStorageKey(userId) {
  const accountId = String(userId || 'guest').trim().toLowerCase()
  return `truehunt-not-interested-investors:${encodeURIComponent(accountId)}`
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
  return { url, isVideo: type.includes('video') }
}

function formatOpportunityFieldLabel(field) {
  return field
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]/g, ' ')
    .replace(/^./, (letter) => letter.toUpperCase())
}

function formatOpportunityFieldValue(value) {
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (Array.isArray(value)) return value.length ? value.join(', ') : 'None selected'
  if (value && typeof value === 'object') return JSON.stringify(value)
  return value === null || value === undefined || value === '' ? 'Not provided' : String(value)
}

const opportunityDetailGroups = [
  {
    title: 'Project details',
    fields: ['projectName', 'projectType', 'craft', 'openings'],
  },
  {
    title: 'Role and requirements',
    fields: [
      'actingRole',
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
  accountEmail,
  onOwnPostCreated,
  activeNavigation,
  setActiveNavigation,
  hidden,
  onViewProfile,
  onViewConnection,
}) {
  const [posts, setPosts] = useState([])
  const [focusedPostId, setFocusedPostId] = useState('')
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
  const [isConnectionsOpen, setIsConnectionsOpen] = useState(false)
  const [isProfileViewersOpen, setIsProfileViewersOpen] = useState(false)
  const [applyingOpportunity, setApplyingOpportunity] = useState(null)
  const [applicationName, setApplicationName] = useState('')
  const [applicationExperience, setApplicationExperience] = useState('')
  const [isSubmittingApplication, setIsSubmittingApplication] = useState(false)
  const [pendingApplicationPostIds, setPendingApplicationPostIds] = useState([])
  const [applicationError, setApplicationError] = useState('')
  const [isWritingArticle, setIsWritingArticle] = useState(false)
  const [isOpportunityWizardOpen, setIsOpportunityWizardOpen] = useState(false)
  const [editingOpportunity, setEditingOpportunity] = useState(null)
  const [draft, setDraft] = useState('')
  const [draftType, setDraftType] = useState('post')
  const [draftMedia, setDraftMedia] = useState(null)
  const [isComposerOpen, setIsComposerOpen] = useState(false)
  const imageInputRef = useRef(null)
  const videoInputRef = useRef(null)
  const mediaUrls = useRef([])

  const name = profileDetails.name.trim() || 'Your name'
  const dismissedPostsStorageKey = getDismissedPostsStorageKey(currentUserId)
  const dismissedInvestorListingsStorageKey = getDismissedInvestorListingsStorageKey(currentUserId)
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]

  useEffect(() => () => {
    mediaUrls.current.forEach((url) => URL.revokeObjectURL(url))
  }, [])

  useEffect(() => {
    setDismissedPostIds(readDismissedPostIds(dismissedPostsStorageKey))
  }, [dismissedPostsStorageKey])

  useEffect(() => {
    setDismissedInvestorListingIds(readDismissedPostIds(dismissedInvestorListingsStorageKey))
  }, [dismissedInvestorListingsStorageKey])

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
    let isCurrent = true
    async function loadPosts() {
      try {
        const data = activeNavigation === 'opportunities'
          ? await api.getOpportunities()
          : await api.getPosts()
        if (!isCurrent || !Array.isArray(data)) return

        const dismissedIds = new Set(readDismissedPostIds(dismissedPostsStorageKey))
        setDismissedPostIds([...dismissedIds])
        setPosts((current) => {
          const localPosts = current.filter((post) => post.localOnly)
          const refreshedPosts = data.filter((post) => !dismissedIds.has(String(post.id)))
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
    return () => {
      isCurrent = false
      window.removeEventListener('focus', handleFocus)
    }
  }, [activeNavigation, dismissedPostsStorageKey])

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
        if (isCurrent) setConnectedProfiles(Array.isArray(data) ? data : [])
      } catch (error) {
        console.error('Failed to load connections:', error)
        if (isCurrent) setConnectionsError('Could not load your connections. Please try again.')
      }
    }

    loadConnections()
    return () => {
      isCurrent = false
    }
  }, [activeNavigation])

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
      const { connected, profile } = event.detail || {}
      const profileId = String(
        profile?.userId || profile?.id || profile?.user_id || profile?._id || '',
      )
      if (!profileId) return

      setConnectedProfiles((current) => {
        const matchesProfile = (item) => String(
          item.userId || item.id || item.user_id || item._id || '',
        ) === profileId
        if (!connected) return current.filter((item) => !matchesProfile(item))
        if (current.some(matchesProfile)) return current
        return [...current, { ...profile, id: profileId, userId: profile.userId || profileId }]
      })
    }

    window.addEventListener('truehunt-connection-changed', handleConnectionChanged)
    return () => window.removeEventListener('truehunt-connection-changed', handleConnectionChanged)
  }, [])

  function selectMedia(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    if (draftMedia) URL.revokeObjectURL(draftMedia.url)
    setDraftMedia({
      file,
      url: URL.createObjectURL(file),
      kind: file.type.startsWith('video/') ? 'video' : 'image',
    })
    setIsComposerOpen(true)
  }

  function removeDraftMedia() {
    if (draftMedia) URL.revokeObjectURL(draftMedia.url)
    setDraftMedia(null)
  }

  async function publishPost(event) {
    event.preventDefault()
    const content = draft.trim()
    if (!content && !draftMedia) return

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
      }
    }

    try {
      const response = await api.createPost({
        content,
        type: draftType,
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
        media: draftMedia,
        createdAt: new Date().toLocaleString(),
      }
      setPosts((currentPosts) => [created, ...currentPosts])
      onOwnPostCreated?.(created)
    }

    setDraft('')
    setDraftMedia(null)
    setDraftType('post')
    setIsComposerOpen(false)
  }

  function startOpportunityPost() {
    setEditingOpportunity(null)
    setIsOpportunityWizardOpen(true)
  }

  function startPost() {
    setDraftType(activeNavigation === 'opportunities' ? 'opportunity' : 'post')
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

  function openInvestorRequest() {
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
  }

  async function openInvestorSearch() {
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
  }

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
    const previousCount = Number(post.likesCount) || 0
    const optimisticLike = {
      liked: !wasLiked,
      likesCount: Math.max(0, previousCount + (wasLiked ? -1 : 1)),
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
      setPosts((current) => current.map((item) => (
        String(item.id) === postId ? { ...item, ...updated } : item
      )))
    } catch (err) {
      console.warn('Like error:', err)
      setPosts((current) => current.map((item) => (
        String(item.id) === postId
          ? { ...item, liked: wasLiked, likesCount: previousCount }
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

  async function handleApply(postId) {
    const updated = await api.applyToOpportunity(postId)
    setPosts((current) => current.map((post) => (
      String(post.id) === String(postId) ? { ...post, ...updated } : post
    )))
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
    if (!applyingOpportunity || isSubmittingApplication) return

    setApplicationError('')
    setIsSubmittingApplication(true)
    try {
      await handleApply(applyingOpportunity.id)
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
  }

  function isOwnPost(post) {
    const authorId = String(post.authorId || '').trim().toLowerCase()
    const ownIds = [currentUserId, profileDetails.userId, profileDetails.id, profileDetails.email]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
    if (authorId && authorId !== 'me' && ownIds.includes(authorId)) return true

    const authorName = String(post.authorName || '').trim().toLowerCase()
    const ownName = String(profileDetails.name || '').trim().toLowerCase()
    return Boolean(authorName && ownName && authorName === ownName)
  }

  function showPost(post) {
    const postId = String(post.id)
    setPosts((current) => (
      current.some((item) => String(item.id) === postId) ? current : [post, ...current]
    ))
    setFocusedPostId(postId)
    if (activeNavigation !== 'home') setActiveNavigation('home')
  }

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
      text,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
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

  async function handlePromoClick(campaign) {
    try {
      const res = await api.clickPromotion(campaign)
      alert(res.message || 'Inquiry registered with Northstar Films casting office.')
    } catch (err) {
      console.warn('Promo error:', err)
    }
  }

  const visiblePosts = posts.filter((post) => (
    !dismissedPostIds.includes(String(post.id))
    && (activeNavigation === 'opportunities'
      ? post.type === 'opportunity'
      : post.type !== 'opportunity')
  ))
  const visibleInvestorListings = investorListings.filter(
    (listing) => !dismissedInvestorListingIds.includes(String(listing.id)),
  )
  const selectedOpportunityPost = activeNavigation === 'opportunities'
    ? visiblePosts.find((post) => String(post.id) === String(selectedOpportunity?.id))
    : null
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
  const allMyPosts = [
    ...posts.filter(isOwnPost),
    ...(Array.isArray(profileDetails.posts) ? profileDetails.posts : []),
  ].filter((post, index, allPosts) => (
    allPosts.findIndex((candidate) => String(candidate.id) === String(post.id)) === index
  ))
  const myPosts = allMyPosts.slice(0, 3)
  const newestPost = allMyPosts[0]
  function renderPostPreview(post) {
    const text = getPostPreviewText(post)
    const media = getPostPreviewMedia(post)
    return (
      <>
        {media && !media.isVideo && (
          <img className="home-my-post-thumbnail" src={media.url} alt="" />
        )}
        {text && <span className="home-my-post-copy">{text}</span>}
        {!text && media?.isVideo && <span className="home-my-post-copy">Video post</span>}
        {!text && !media && <span className="home-my-post-copy">Content unavailable</span>}
      </>
    )
  }

  return (
    <>
      <section
        className={`home-page${activeNavigation === 'opportunities' ? ' is-opportunities' : ''}`}
        hidden={hidden || isWritingArticle}
        aria-label={activeNavigation === 'opportunities' ? 'Opportunities' : 'Home feed'}
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
              {profileDetails.address && <p>{profileDetails.address}</p>}
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
              onClick={() => {
                setIsProfileViewersOpen(false)
                setIsConnectionsOpen(true)
              }}
              aria-haspopup="dialog"
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
                setIsConnectionsOpen(false)
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
          {connectionsError && <p className="home-connections-error" role="alert">{connectionsError}</p>}
          {profileViewersError && <p className="home-connections-error" role="alert">{profileViewersError}</p>}
          {activeNavigation === 'home' && (
            <section className="home-card home-sidebar-stats" aria-labelledby="home-post-impressions-heading">
              <div className="home-connections-card home-post-impressions-row">
                <span className="home-connections-icon"><ThumbsUp size={20} aria-hidden="true" /></span>
                <span className="home-connections-copy">
                  <strong id="home-post-impressions-heading">Post impressions</strong>
                  <small>
                    <span aria-label={`${newestPost?.likesCount || 0} likes`}>
                      {newestPost?.likesCount || 0}
                    </span>
                  </small>
                </span>
              </div>
            </section>
          )}
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
            <Link className="home-explore-promo home-explore-promo-tickets" to="/entertainments">
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
                    <article className="home-card home-investor-listing" key={listing.id}>
                      <header>
                        <div>
                          <h2>{listing.name}</h2>
                          <span>{listing.role}</span>
                        </div>
                        {listing.budget && <strong>{listing.budget}</strong>}
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
                        <button
                          className="home-investor-listing-dismiss"
                          type="button"
                          onClick={() => handleNotInterestedInvestorListing(listing.id)}
                        >
                          Not interested
                        </button>
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
              >
                Start a post
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
                    ? 'Describe the job or opportunity you are sharing'
                    : 'What would you like to share?'}
                  rows={4}
                  autoFocus
                />
                {draftMedia && (
                  <div className="home-media-preview">
                    {draftMedia.kind === 'video'
                      ? <video src={draftMedia.url} controls />
                      : <img src={draftMedia.url} alt="Selected post attachment" />}
                    <button type="button" onClick={removeDraftMedia}>Remove attachment</button>
                  </div>
                )}
                <div className="home-publish-row">
                  <span>{draftType === 'opportunity' ? 'Opportunity' : 'Post'}</span>
                  <button
                    className="home-publish-button"
                    type="submit"
                    disabled={!draft.trim() && !draftMedia}
                  >
                    {draftType === 'opportunity' ? 'Share' : 'Post'}
                  </button>
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
            <span>{activeNavigation === 'opportunities' ? 'Opportunities' : 'Posts'}</span>
            {activeNavigation === 'opportunities' ? (
              <button className="home-opportunity-button" type="button" onClick={startOpportunityPost}>
                Share an opportunity
              </button>
            ) : (
              <span className="home-feed-sort">Recent</span>
            )}
          </div>

          {visiblePosts.length === 0 ? (
            <section className="home-card home-empty-feed">
              <h2>{activeNavigation === 'opportunities'
                ? 'No opportunities shared yet'
                : 'Your feed starts here'}</h2>
              <p>{activeNavigation === 'opportunities'
                ? 'When someone shares a job or opportunity, it will appear here.'
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
                const isOwnOpportunity = post.type === 'opportunity' && isOwnPost(post)
                const isOpportunityExpanded = String(selectedOpportunity?.id) === postId
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
                  </header>
                  {post.type === 'opportunity' ? (
                    <div className="home-post-opportunity-summary">
                      <button
                        type="button"
                        className="home-post-opportunity-trigger"
                        onClick={() => setSelectedOpportunity(isOpportunityExpanded ? null : post)}
                        aria-label={`View opportunity: ${post.title || getPostPreviewText(post) || 'Details'}`}
                        aria-expanded={isOpportunityExpanded}
                      >
                        {post.title && <span className="home-post-title">{post.title}</span>}
                        {post.content && <span className="home-post-content">{post.content}</span>}
                        {postMedia && !postMedia.isVideo && (
                          <img className="home-post-media" src={postMedia.url} alt="Opportunity attachment" />
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
                      {post.media?.kind === 'video'
                        ? <video className="home-post-media" src={post.media.url} controls />
                        : post.media && (
                          <img className="home-post-media" src={post.media.url} alt="Post attachment" />
                        )}
                    </>
                  )}
                  {post.type === 'article' && <span className="home-post-type">Article</span>}

                  {/* Post interactive action buttons wired to backend APIs */}
                  <div className="home-post-action-bar" style={{ display: 'flex', gap: '8px', marginTop: '12px', borderTop: '1px solid var(--color-border, #eee)', paddingTop: '8px', flexWrap: 'wrap' }}>
                    {!isOwnOpportunity && (
                      <>
                    <button
                      type="button"
                      onClick={() => handleLike(post)}
                      disabled={pendingLikePostIds.includes(String(post.id))}
                      style={{
                        background: post.liked ? '#e8f0fe' : 'transparent',
                        color: post.liked ? '#1a73e8' : 'inherit',
                        border: '1px solid #ddd',
                        padding: '6px 14px',
                        borderRadius: '20px',
                        cursor: pendingLikePostIds.includes(String(post.id)) ? 'wait' : 'pointer',
                        fontWeight: '500',
                        fontSize: '13px'
                      }}
                    >
                      👍 {post.likesCount || 0} {post.liked ? 'Liked' : 'Like'}
                    </button>
                    {likeErrors[String(post.id)] && (
                      <span role="alert" style={{ alignSelf: 'center', color: '#b3261e', fontSize: '12px' }}>
                        {likeErrors[String(post.id)]}
                      </span>
                    )}

                    <button
                      type="button"
                      onClick={() => setOpenCommentsPostId(openCommentsPostId === post.id ? null : post.id)}
                      style={{
                        background: 'transparent',
                        border: '1px solid #ddd',
                        padding: '6px 14px',
                        borderRadius: '20px',
                        cursor: 'pointer',
                        fontSize: '13px'
                      }}
                    >
                      💬 Comments ({post.commentsCount ?? post.comments?.length ?? 0})
                    </button>

                    {post.type === 'opportunity' && (
                      <button
                        type="button"
                        onClick={() => {
                          if (post.applied) {
                            void handleCancelApplication(post)
                            return
                          }
                          setApplicationError('')
                          setApplicationName(profileDetails.name.trim())
                          setApplicationExperience(profileDetails.experience || '')
                          setApplyingOpportunity(post)
                        }}
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
                      </>
                    )}

                    {isOwnOpportunity ? (
                      <div className="home-opportunity-owner-actions">
                      <button
                        type="button"
                        onClick={() => startEditingOpportunity(post)}
                        title="Edit opportunity"
                      >
                        Edit opportunity
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(post.id)}
                        title="Delete opportunity"
                      >
                        Delete
                      </button>
                      </div>
                    ) : isOwnPost(post) ? (
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
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleNotInterested(post.id)}
                        style={{
                          marginLeft: 'auto',
                          background: 'transparent',
                          border: 'none',
                          color: 'var(--color-text-muted, #666)',
                          cursor: 'pointer',
                          fontSize: '12px'
                        }}
                        title="Hide this post from your feed"
                      >
                        Not interested
                      </button>
                    )}
                  </div>

                  {postComments.length > 0 && (
                    <div className="home-post-comments">
                      {commentsToShow.map((c) => (
                        <div className="home-post-comment" key={c.id}>
                          <strong>{c.authorName}</strong> <small style={{ color: '#888' }}>({c.createdAt})</small>: {c.text}
                        </div>
                      ))}
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
              <section className="home-opportunity-detail home-card" aria-label="Opportunity details">
                <header className="home-opportunity-detail-header">
                  <span className="home-opportunity-detail-icon">
                    <BriefcaseBusiness size={20} aria-hidden="true" />
                  </span>
                  <div>
                    <strong>{selectedOpportunityPost.authorName || name}</strong>
                    <span>
                      Opportunity
                      {selectedOpportunityPost.createdAt ? ` · ${selectedOpportunityPost.createdAt}` : ''}
                    </span>
                  </div>
                  <button
                    className="home-opportunity-detail-close"
                    type="button"
                    aria-label="Close opportunity details"
                    onClick={() => setSelectedOpportunity(null)}
                  >
                    <X size={18} aria-hidden="true" />
                  </button>
                </header>
                <h1>{selectedOpportunityPost.title || selectedOpportunityDetails.projectName || 'Opportunity'}</h1>
                <p className="home-opportunity-detail-meta">
                  {[
                    selectedOpportunityDetails.projectType,
                    selectedOpportunityDetails.actingRole,
                    selectedOpportunityDetails.locationPreference,
                  ].filter(Boolean).join(' · ') || 'Opportunity details'}
                </p>
                {selectedOpportunityPost.content && (
                  <section className="home-opportunity-detail-section">
                    <h2>About this opportunity</h2>
                    <p className="home-opportunity-description">{selectedOpportunityPost.content}</p>
                  </section>
                )}
                {selectedOpportunityMedia?.isVideo
                  ? <video className="home-opportunity-modal-media" src={selectedOpportunityMedia.url} controls />
                  : selectedOpportunityMedia && (
                    <img className="home-opportunity-modal-media" src={selectedOpportunityMedia.url} alt="Opportunity attachment" />
                  )}
                {opportunityDetailGroups.map((group) => (
                  <section className="home-opportunity-detail-section" key={group.title}>
                    <h2>{group.title}</h2>
                    <dl className="home-opportunity-facts">
                      {group.fields.map((field) => (
                        <div key={field}>
                          <dt>{formatOpportunityFieldLabel(field)}</dt>
                          <dd>{formatOpportunityFieldValue(selectedOpportunityDetails[field])}</dd>
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
              </section>
            )}
            </div>
          )}
          </>
          )}
        </div>

        <aside className="home-promotion-column" aria-label="Promotions and Explore">
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
                onClick={() => handlePromoClick("Northstar Films Production")}
              >
                Explore film careers
              </button>
            </div>
          </section>
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
                <strong>{filmMakerVisited ? 'Manage Film' : 'Are you a filmmaker?'}</strong>
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
                <strong>Are you an investor and earner ?</strong>
                <small>Discover creative opportunities</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </button>
            <a className="home-explore-promo home-explore-promo-distribute" href="/distributions">
              <span className="home-explore-promo-art">
                <img src="/assets/explore-distribution.png" alt="" />
              </span>
              <span className="home-explore-promo-copy">
                <strong>Are you a distributor ?</strong>
                <small>Get stories in front of audiences</small>
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
            </a>
          </section>
        </aside>
      </div>
    </section>
    {isConnectionsOpen && (
      <div
        className="home-connections-backdrop"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) setIsConnectionsOpen(false)
        }}
      >
        <section
          className="home-connections-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-connections-title"
        >
          <header className="home-connections-dialog-header">
            <div>
              <h2 id="home-connections-title">Your connections</h2>
              <p>{connectedProfiles.length} people in your network</p>
            </div>
            <button
              type="button"
              className="home-connections-close"
              aria-label="Close connections"
              onClick={() => setIsConnectionsOpen(false)}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          {connectedProfiles.length ? (
            <ul className="home-connections-list">
              {connectedProfiles.map((profile) => {
                const profileName = profile.name || 'TrueHunt member'
                const profileImage = profile.profileImageUrl || profile.avatar || ''
                const initials = profile.initials || profileName
                  .split(/\s+/)
                  .map((part) => part[0])
                  .join('')
                  .slice(0, 2)

                return (
                  <li key={profile.id || profile.userId || profileName}>
                    <button
                      type="button"
                      className="home-connection-person"
                      onClick={() => {
                        setIsConnectionsOpen(false)
                        onViewConnection?.({
                          ...profile,
                          userId: profile.userId || profile.id || '',
                          profileImageUrl: profileImage,
                        })
                      }}
                    >
                      <span className={`home-avatar home-connection-avatar${profileImage ? '' : ' is-placeholder'}`}>
                        {profileImage ? <img src={profileImage} alt="" /> : initials}
                      </span>
                      <span className="home-connection-person-name">{profileName}</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          ) : (
            <div className="home-connections-empty">
              <UsersRound size={24} aria-hidden="true" />
              <p>No connections yet.</p>
            </div>
          )}
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
              <h2 id="home-application-title">Apply for opportunity</h2>
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
          >
            <p className="home-application-note">Review and edit your details before submitting.</p>
            <label className="home-application-field">
              <span>Name</span>
              <input
                type="text"
                value={applicationName}
                onChange={(event) => setApplicationName(event.target.value)}
                placeholder="Add your name to your profile"
                disabled={isSubmittingApplication}
              />
            </label>
            <label className="home-application-field">
              <span>Experience</span>
              <input
                type="text"
                value={applicationExperience}
                onChange={(event) => setApplicationExperience(event.target.value)}
                placeholder="Add your experience to your profile"
                disabled={isSubmittingApplication}
              />
            </label>
            {applicationError && <p className="home-application-error" role="alert">{applicationError}</p>}
          </form>
          <div className="home-application-actions">
            <button
              className="home-application-cancel"
              type="button"
              onClick={() => setApplyingOpportunity(null)}
              disabled={isSubmittingApplication}
            >
              Cancel
            </button>
            <button
              className="home-application-submit"
              type="submit"
              form="home-application-form"
              disabled={isSubmittingApplication}
            >
              {isSubmittingApplication ? 'Submitting…' : 'Submit application'}
            </button>
          </div>
        </section>
      </div>
    )}
    {isProfileViewersOpen && (
      <div
        className="home-connections-backdrop"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) setIsProfileViewersOpen(false)
        }}
      >
        <section
          className="home-connections-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-profile-viewers-title"
        >
          <header className="home-connections-dialog-header">
            <div>
              <h2 id="home-profile-viewers-title">Profile viewers</h2>
              <p>{profileViewers.length} people viewed your profile</p>
            </div>
            <button
              type="button"
              className="home-connections-close"
              aria-label="Close profile viewers"
              onClick={() => setIsProfileViewersOpen(false)}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          {profileViewersError ? (
            <p className="home-profile-viewers-message" role="alert">{profileViewersError}</p>
          ) : profileViewers.length ? (
            <ul className="home-connections-list">
              {profileViewers.map((profile) => {
                const profileName = profile.name || 'TrueHunt member'
                const profileImage = profile.profileImageUrl || profile.avatar || ''
                const initials = profileName
                  .split(/\s+/)
                  .map((part) => part[0])
                  .join('')
                  .slice(0, 2)

                return (
                  <li key={profile.userId || profile.id || profileName}>
                    <button
                      type="button"
                      className="home-connection-person"
                      onClick={() => {
                        setIsProfileViewersOpen(false)
                        onViewConnection?.({
                          ...profile,
                          userId: profile.userId || profile.id || '',
                          profileImageUrl: profileImage,
                        })
                      }}
                    >
                      <span className={`home-avatar home-connection-avatar${profileImage ? '' : ' is-placeholder'}`}>
                        {profileImage ? <img src={profileImage} alt="" /> : initials}
                      </span>
                      <span className="home-connection-person-name">{profileName}</span>
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