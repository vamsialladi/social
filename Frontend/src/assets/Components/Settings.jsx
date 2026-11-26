import { useEffect, useState } from 'react'
import api from '../../api'
import PasswordField from '../../components/common/PasswordField'
import {
  getPasswordValidationMessage,
  isPasswordValid,
} from '../../components/common/passwordValidation'
import {
  ArrowRight,
  Ban,
  Bell,
  Bookmark,
  Flag,
  FileText,
  Heart,
  Image,
  LockKeyhole,
  Shield,
  ShieldAlert,
  UserRound,
  Video,
} from 'lucide-react'
import './Settings.css'

const settingsSections = [
  {
    id: 'preferences',
    label: 'Account preferences',
    icon: UserRound,
    description: 'Manage your profile details and contact information.',
  },
  {
    id: 'security',
    label: 'Sign in & security',
    icon: LockKeyhole,
    description: 'Review sign-in and security information for your account.',
  },
  {
    id: 'privacy',
    label: 'Privacy & settings',
    icon: Shield,
    description: 'Control who can see your profile and activity.',
  },
  {
    id: 'block',
    label: 'Block',
    icon: Ban,
    description: 'Manage people who are blocked from contacting or viewing your profile.',
  },
  {
    id: 'restricted',
    label: 'Restricted content',
    icon: ShieldAlert,
    description: 'Choose the limits applied to sensitive or restricted content.',
  },
  {
    id: 'photo-video-url',
    label: 'Visibility',
    icon: Image,
    description: 'Manage photo, video, and external link content on your posts.',
  },
  {
    id: 'like-share-counts',
    label: 'Like & share counts',
    icon: Heart,
    description: 'Choose whether like and share counts are displayed.',
  },
  {
    id: 'notifications',
    label: 'Notifications',
    icon: Bell,
    description: 'Choose which notifications you receive.',
  },
  {
    id: 'saved',
    label: 'Saved',
    icon: Bookmark,
    description: 'Posts you saved to revisit later.',
  },
  {
    id: 'reports',
    label: 'Reports',
    icon: Flag,
    description: 'Posts you have reported.',
  },
]

const notificationCategories = [
  {
    id: 'connections',
    label: 'Connecting with others',
    settingLabel: 'Allow connections related notifications',
  },
  {
    id: 'network-catch-up',
    label: 'Network catch-up updates',
    settingLabel: 'Allow network catch-up update notifications',
  },
  {
    id: 'posting',
    label: 'Posting and commenting',
    settingLabel: 'Allow posting and commenting notifications',
  },
  {
    id: 'messaging',
    label: 'Messaging',
    settingLabel: 'Allow messaging notifications',
  },
  {
    id: 'news',
    label: 'News and reports',
    settingLabel: 'Allow news and reports notifications',
  },
  {
    id: 'profile',
    label: 'Updating your profile',
    settingLabel: 'Allow profile update notifications',
  },
  {
    id: 'verifications',
    label: 'Verifications',
    settingLabel: 'Allow verification notifications',
  },
]

const blockReasons = [
  'Spam or unwanted promotion',
  'Harassment or abusive behavior',
  'Fake or misleading profile',
  'Inappropriate content',
  'Privacy or safety concern',
  'Custom reason',
]

function getSavedPostMedia(post) {
  let media = post.media
  if (typeof media === 'string') {
    try {
      media = JSON.parse(media)
    } catch {
      media = { url: post.media }
    }
  }
  if (!media || typeof media !== 'object' || Array.isArray(media)) media = {}

  const urls = [
    media.url,
    media.src,
    media.srcUrl,
    media.fileUrl,
    media.downloadUrl,
    media.path,
    media.videoUrl,
    media.imageUrl,
    post.mediaUrl,
    post.imageUrl,
    post.videoUrl,
    post.photoUrl,
    post.attachmentUrl,
  ].filter((url, index, values) => (
    typeof url === 'string' && url.trim() && values.indexOf(url) === index
  ))
  if (!urls.length) return null

  const mediaTypes = [
    media.kind,
    media.type,
    media.contentType,
    media.mimeType,
    post.mediaType,
  ].map((value) => String(value || '').toLowerCase())
  const fileNames = [media.fileName, post.mediaFileName, ...urls]
  const isVideo = mediaTypes.some((type) => type.includes('video'))
    || fileNames.some((fileName) => /\.(mp4|mov|webm|m4v|m3u8)(?:$|[?#])/i.test(fileName || ''))
  return { url: urls[0], isVideo }
}

function Settings({
  profileDetails,
  profilePreview,
  onEditProfile,
  onOpenSavedPost,
  theme,
  onThemeChange,
  onAccountDeleted,
}) {
  const [activeSection, setActiveSection] = useState('preferences')
  const [blockedUserInput, setBlockedUserInput] = useState('')
  const [blockedUsers, setBlockedUsers] = useState([])
  const [savedPosts, setSavedPosts] = useState([])
  const [savedPostFilter, setSavedPostFilter] = useState('images')
  const [reportedPosts, setReportedPosts] = useState([])
  const [removingReportId, setRemovingReportId] = useState('')
  const [reportRemovalError, setReportRemovalError] = useState('')
  const [profileDirectory, setProfileDirectory] = useState([])
  const [isProfileDirectoryLoading, setIsProfileDirectoryLoading] = useState(false)
  const [blockTarget, setBlockTarget] = useState(null)
  const [blockReason, setBlockReason] = useState('')
  const [customBlockReason, setCustomBlockReason] = useState('')
  const [privacyPreferences, setPrivacyPreferences] = useState({
    profileVisibility: true,
    activityStatus: true,
    searchVisibility: true,
  })
  const [restrictedPreferences, setRestrictedPreferences] = useState({
    sensitiveContent: false,
    restrictedMessages: true,
  })
  const [mediaPreferences, setMediaPreferences] = useState({
    photoPosts: 'public',
    videoPosts: 'public',
    externalUrls: 'public',
  })
  const [engagementPreferences, setEngagementPreferences] = useState({
    likeCounts: true,
    shareCounts: true,
  })
  const [isPasswordFormOpen, setIsPasswordFormOpen] = useState(false)
  const [isThemeOptionsOpen, setIsThemeOptionsOpen] = useState(false)
  const [expandedNotificationCategories, setExpandedNotificationCategories] = useState({})
  const [notificationPreferences, setNotificationPreferences] = useState(() =>
    Object.fromEntries(notificationCategories.map(({ id }) => [id, true])),
  )
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(false)
  const [securityMessage, setSecurityMessage] = useState('')
  const [passwordMessage, setPasswordMessage] = useState('')
  const [passwordIsError, setPasswordIsError] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false)
  const name = profileDetails.name || 'Your name'
  const email = profileDetails.email || 'Not added yet'
  const phone = profileDetails.phone || 'Not added yet'
  const address = profileDetails.address || 'Not added yet'
  const selectedSection = settingsSections.find((section) => section.id === activeSection)
  const filteredSavedPosts = savedPosts.filter((post) => {
    const media = getSavedPostMedia(post)
    if (savedPostFilter === 'videos') return media?.isVideo
    if (savedPostFilter === 'text') return !media
    return media && !media.isVideo
  })
  const accountStorageId = profileDetails.userId || profileDetails.id || profileDetails.email || email
  const accountStorageKey = encodeURIComponent(String(accountStorageId || 'me'))
  const blockedIdentitySet = new Set(
    blockedUsers.map((blocked) => String(blocked.identity || blocked).trim().toLowerCase()),
  )
  const blockSearch = blockedUserInput.trim().toLowerCase()
  const searchedProfiles = blockSearch
    ? profileDirectory.filter((profile) => {
        const identity = [
          profile.name,
          profile.email,
          profile.userId,
          profile.id,
          profile.primaryCraft,
          profile.craft,
        ].filter(Boolean).join(' ').toLowerCase()
        return identity.includes(blockSearch)
      }).slice(0, 8)
    : []

  useEffect(() => {
    let isActive = true
    const storageKey = `truehunt-social-settings:${String(email).trim().toLowerCase()}`
    try {
      const stored = localStorage.getItem(storageKey)
      if (stored) {
        const saved = JSON.parse(stored)
        setBlockedUsers(Array.isArray(saved.blockedUsers)
          ? saved.blockedUsers.map((blocked) => (
              typeof blocked === 'string'
                ? { name: blocked, reason: 'Not specified', identity: blocked }
                : blocked
            ))
          : [])
        if (saved.notifications) {
          setNotificationPreferences((prev) => ({ ...prev, ...saved.notifications }))
        }
        setPrivacyPreferences((prev) => ({ ...prev, ...saved.privacyPreferences }))
        setRestrictedPreferences((prev) => ({ ...prev, ...saved.restrictedPreferences }))
        if (saved.mediaPreferences) {
          setMediaPreferences((prev) => Object.fromEntries(
            Object.keys(prev).map((id) => {
              const value = saved.mediaPreferences[id]
              return [id, value === 'private' || value === false ? 'private' : 'public']
            }),
          ))
        }
        setEngagementPreferences((prev) => ({ ...prev, ...saved.engagementPreferences }))
      }
    } catch (err) {
      console.warn('Could not load local Social settings:', err)
    }

    async function loadSettings() {
      try {
        const data = await api.getSettings()
        if (!isActive || !data) return
        if (data.notifications) {
          setNotificationPreferences((prev) => ({ ...prev, ...data.notifications }))
        }
        if (data.privacyPreferences) {
          setPrivacyPreferences((prev) => ({ ...prev, ...data.privacyPreferences }))
        }
        if (data.restrictedPreferences) {
          setRestrictedPreferences((prev) => ({ ...prev, ...data.restrictedPreferences }))
        }
        if (data.mediaPreferences) {
          setMediaPreferences((prev) => ({ ...prev, ...data.mediaPreferences }))
        }
        if (data.engagementPreferences) {
          setEngagementPreferences((prev) => ({ ...prev, ...data.engagementPreferences }))
        }
        if (typeof data.twoFactorEnabled === 'boolean') {
          setTwoFactorEnabled(data.twoFactorEnabled)
        }
        if (Array.isArray(data.blockedUsers)) {
          setBlockedUsers(data.blockedUsers)
        }
        try {
          const existing = JSON.parse(localStorage.getItem(storageKey) || '{}')
          localStorage.setItem(storageKey, JSON.stringify({
            ...existing,
            ...(data.notifications ? { notifications: data.notifications } : {}),
            ...(data.privacyPreferences ? { privacyPreferences: data.privacyPreferences } : {}),
            ...(data.restrictedPreferences ? { restrictedPreferences: data.restrictedPreferences } : {}),
            ...(data.mediaPreferences ? { mediaPreferences: data.mediaPreferences } : {}),
            ...(data.engagementPreferences ? { engagementPreferences: data.engagementPreferences } : {}),
            ...(Array.isArray(data.blockedUsers) ? { blockedUsers: data.blockedUsers } : {}),
          }))
        } catch {
          // ignore localStorage write errors
        }
      } catch (err) {
        console.warn('Backend settings load note:', err)
      }
    }
    loadSettings()
    return () => {
      isActive = false
    }
  }, [email])

  useEffect(() => {
    if (activeSection !== 'block') return
    let isCurrent = true
    async function loadProfiles() {
      setIsProfileDirectoryLoading(true)
      try {
        const profiles = await api.getProfileDirectory()
        if (isCurrent && Array.isArray(profiles)) setProfileDirectory(profiles)
      } catch (err) {
        console.warn('Could not load profiles for the block section:', err)
      } finally {
        if (isCurrent) setIsProfileDirectoryLoading(false)
      }
    }
    loadProfiles()
    return () => {
      isCurrent = false
    }
  }, [activeSection])

  useEffect(() => {
    let isCurrent = true

    function loadSavedItems() {
      try {
        const saved = JSON.parse(localStorage.getItem(`truehunt-social-saved-posts:${accountStorageKey}`) || '[]')
        const reports = JSON.parse(localStorage.getItem(`truehunt-social-reported-posts:${accountStorageKey}`) || '[]')
        setSavedPosts(Array.isArray(saved) ? saved : [])
        setReportedPosts(Array.isArray(reports) ? reports : [])
      } catch (err) {
        console.warn('Could not load saved posts and reports:', err)
      }
    }

    async function markUnavailableSavedPosts() {
      try {
        const storageKey = `truehunt-social-saved-posts:${accountStorageKey}`
        const stored = JSON.parse(localStorage.getItem(storageKey) || '[]')
        if (!Array.isArray(stored)) {
          throw new Error('Saved posts are not a list.')
        }
        if (!stored.some((post) => post && !post.localOnly && !post.ownerRemoved)) return

        const currentPosts = await api.getPosts()
        if (!Array.isArray(currentPosts)) {
          throw new Error('The feed response is not a list.')
        }
        if (!isCurrent) return

        const availablePostIds = new Set(currentPosts.map((post) => String(post.id)))
        let changed = false
        const next = stored.map((post) => {
          if (!post || post.localOnly || post.ownerRemoved || availablePostIds.has(String(post.id))) {
            return post
          }
          changed = true
          return { ...post, ownerRemoved: true }
        })
        if (changed) {
          localStorage.setItem(storageKey, JSON.stringify(next))
          setSavedPosts(next)
        }
      } catch (err) {
        console.warn('Could not check whether saved posts are still available:', err)
      }
    }

    function markSavedPostOwnerRemoved(event) {
      const postId = String(event.detail?.postId || '').trim()
      if (!postId) return

      try {
        const storageKey = `truehunt-social-saved-posts:${accountStorageKey}`
        const stored = JSON.parse(localStorage.getItem(storageKey) || '[]')
        if (!Array.isArray(stored)) {
          throw new Error('Saved posts are not a list.')
        }

        let changed = false
        const next = stored.map((post) => {
          if (String(post.id) !== postId || post.ownerRemoved) return post
          changed = true
          return { ...post, ownerRemoved: true }
        })
        if (changed) {
          localStorage.setItem(storageKey, JSON.stringify(next))
          setSavedPosts(next)
        }
      } catch (err) {
        console.warn('Could not mark deleted saved post:', err)
      }
    }

    loadSavedItems()
    async function syncBackendSavedAndReports() {
      try {
        if (activeSection === 'saved') {
          const backendSaved = await api.getSavedPosts()
          if (isCurrent && Array.isArray(backendSaved) && backendSaved.length > 0) {
            setSavedPosts(backendSaved)
            try {
              localStorage.setItem(`truehunt-social-saved-posts:${accountStorageKey}`, JSON.stringify(backendSaved))
            } catch {
              // ignore quota errors
            }
          } else {
            await markUnavailableSavedPosts()
          }
        }
        if (activeSection === 'reports') {
          const backendReports = await api.getMyReports()
          if (isCurrent && Array.isArray(backendReports) && backendReports.length > 0) {
            setReportedPosts(backendReports)
            try {
              localStorage.setItem(`truehunt-social-reported-posts:${accountStorageKey}`, JSON.stringify(backendReports))
            } catch {
              // ignore quota errors
            }
          }
        }
      } catch (err) {
        console.warn('Backend saved/reports sync note:', err)
        if (activeSection === 'saved') {
          await markUnavailableSavedPosts()
        }
      }
    }
    if (activeSection === 'saved' || activeSection === 'reports') {
      syncBackendSavedAndReports()
    }
    window.addEventListener('truehunt-saved-post-changed', loadSavedItems)
    window.addEventListener('truehunt-reported-post-changed', loadSavedItems)
    window.addEventListener('truehunt-social-post-deleted', markSavedPostOwnerRemoved)
    return () => {
      isCurrent = false
      window.removeEventListener('truehunt-saved-post-changed', loadSavedItems)
      window.removeEventListener('truehunt-reported-post-changed', loadSavedItems)
      window.removeEventListener('truehunt-social-post-deleted', markSavedPostOwnerRemoved)
    }
  }, [accountStorageKey, activeSection])

  function removeSavedPost(postId) {
    const next = savedPosts.filter((post) => String(post.id) !== String(postId))
    setSavedPosts(next)
    try {
      localStorage.setItem(`truehunt-social-saved-posts:${accountStorageKey}`, JSON.stringify(next))
      window.dispatchEvent(new CustomEvent('truehunt-saved-post-changed'))
    } catch (err) {
      console.warn('Could not remove saved post:', err)
    }
    if (/^\d+$/.test(String(postId))) {
      api.unsavePost(postId).catch((err) => {
        console.warn('Could not remove saved post on backend:', err)
      })
    }
  }

  async function removeReportedPost(report, index) {
    const reportId = report?.id == null ? '' : String(report.id)
    if (reportId && /^\d+$/.test(reportId)) {
      if (removingReportId) return
      setRemovingReportId(reportId)
      setReportRemovalError('')
      try {
        await api.deleteMyReport(reportId)
      } catch (error) {
        console.error('Could not remove post report:', error)
        setReportRemovalError(error.message || 'Could not remove this report. Please try again.')
        setRemovingReportId('')
        return
      }
    }

    const reportKey = reportId || `${report?.postId || ''}-${report?.reportedAt || index}`
    const next = reportedPosts.filter((item, itemIndex) => {
      const itemId = item?.id == null ? '' : String(item.id)
      const itemKey = itemId || `${item?.postId || ''}-${item?.reportedAt || itemIndex}`
      return itemKey !== reportKey
    })
    setReportedPosts(next)
    try {
      localStorage.setItem(`truehunt-social-reported-posts:${accountStorageKey}`, JSON.stringify(next))
    } catch (error) {
      console.warn('Could not update cached post reports:', error)
    }
    setRemovingReportId('')
    setReportRemovalError('')
  }

  function saveLocalPreferences(next) {
    const storageKey = `truehunt-social-settings:${String(email).trim().toLowerCase()}`
    try {
      const stored = JSON.parse(localStorage.getItem(storageKey) || '{}')
      localStorage.setItem(storageKey, JSON.stringify({ ...stored, ...next }))
    } catch (err) {
      console.warn('Could not save local Social settings:', err)
    }
  }

  function openBlockReason(profile) {
    setBlockTarget(profile)
    setBlockReason('')
    setCustomBlockReason('')
  }

  function handleConfirmBlock(event) {
    event.preventDefault()
    if (!blockTarget || !blockReason) return
    const reason = blockReason === 'Custom reason'
      ? (customBlockReason.trim() || 'Custom reason')
      : blockReason
    const identity = String(
      blockTarget.userId || blockTarget.id || blockTarget.email || blockTarget.name,
    ).trim()
    const blockedEntry = {
      identity,
      name: blockTarget.name || 'Unknown profile',
      email: blockTarget.email || '',
      photo: blockTarget.profileImageUrl || blockTarget.avatar || blockTarget.profileImage || '',
      reason,
    }
    const next = [
      ...blockedUsers.filter((item) => (
        String(item.identity || item).trim().toLowerCase() !== identity.toLowerCase()
      )),
      blockedEntry,
    ]
    setBlockedUsers(next)
    setBlockedUserInput('')
    setBlockTarget(null)
    setBlockReason('')
    setCustomBlockReason('')
    saveLocalPreferences({ blockedUsers: next })
    api.blockUser(blockedEntry).catch((err) => {
      console.warn('Could not persist blocked user on backend:', err)
    })
  }

  function handleRemoveBlockedUser(identity) {
    const next = blockedUsers.filter((item) => String(item.identity || item) !== identity)
    setBlockedUsers(next)
    saveLocalPreferences({ blockedUsers: next })
    api.unblockUser(identity).catch((err) => {
      console.warn('Could not unblock user on backend:', err)
    })
  }

  function updatePreference(setter, state, id, key) {
    const nextVal = !state[id]
    const next = { ...state, [id]: nextVal }
    setter(next)
    saveLocalPreferences({ [key]: next })
    if (key === 'privacyPreferences') {
      const payload = {
        ...next,
        ...(id === 'profileVisibility' ? {
          'publicProfile': nextVal,
          'public-profile': nextVal,
          'profile_visibility': nextVal
        } : {}),
        ...(id === 'activityStatus' || id === 'activeStatus' ? {
          'activityStatus': nextVal,
          'activeStatus': nextVal,
          'activity-status': nextVal,
          'active_status': nextVal
        } : {})
      }
      api.updatePrivacySettings(payload)
        .then(() => {
          window.dispatchEvent(new CustomEvent('truehunt-settings-updated', { detail: { privacyPreferences: payload } }))
        })
        .catch((err) => console.warn('Privacy API note:', err))
    } else if (key === 'restrictedPreferences') {
      api.updateRestrictedSettings(next).catch((err) => console.warn('Restricted API note:', err))
    } else if (key === 'engagementPreferences') {
      api.updateEngagementSettings(next)
        .then(() => {
          window.dispatchEvent(new CustomEvent('truehunt-settings-updated', { detail: { engagementPreferences: next } }))
        })
        .catch((err) => console.warn('Engagement API note:', err))
    }
  }

  function updateMediaVisibility(id, visibility) {
    const next = { ...mediaPreferences, [id]: visibility }
    setMediaPreferences(next)
    saveLocalPreferences({ mediaPreferences: next })
    api.updateMediaVisibilitySettings({ [id]: visibility })
      .then(() => {
        window.dispatchEvent(new CustomEvent('truehunt-settings-updated', { detail: { mediaPreferences: next } }))
      })
      .catch((err) => console.warn('Media visibility API note:', err))
  }

  const isChangePasswordValid =
    currentPassword.length > 0 &&
    isPasswordValid(newPassword) &&
    newPassword === confirmPassword

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    setPasswordMessage('')
    setPasswordIsError(false)

    if (!currentPassword) {
      setPasswordIsError(true)
      setPasswordMessage('Current password is required.')
      return
    }

    const passwordError = getPasswordValidationMessage(newPassword)
    if (passwordError) {
      setPasswordIsError(true)
      setPasswordMessage(passwordError)
      return
    }

    if (newPassword !== confirmPassword) {
      setPasswordIsError(true)
      setPasswordMessage('Password and confirm password do not match.')
      return
    }

    setIsUpdatingPassword(true)
    try {
      await api.changePassword({
        'current-password': currentPassword,
        'new-password': newPassword,
        'confirm-password': confirmPassword,
      })
      setPasswordIsError(false)
      setPasswordMessage('Password updated successfully!')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setTimeout(() => setIsPasswordFormOpen(false), 2000)
    } catch (err) {
      setPasswordIsError(true)
      setPasswordMessage(err.message || 'Failed to update password.')
    } finally {
      setIsUpdatingPassword(false)
    }
  }

  async function handleToggleNotification(id) {
    const nextVal = !notificationPreferences[id]
    const nextPrefs = {
      ...notificationPreferences,
      [id]: nextVal,
      ...(id === 'connections' ? { 'connecting-with-others': nextVal } : {}),
      ...(id === 'connecting-with-others' ? { connections: nextVal } : {}),
      ...(id === 'posting' ? { 'posting-and-commenting': nextVal, postingAndCommenting: nextVal } : {}),
      ...(id === 'posting-and-commenting' ? { posting: nextVal, postingAndCommenting: nextVal } : {}),
      ...(id === 'profile' ? { 'updating-your-profile': nextVal, updatingYourProfile: nextVal } : {}),
      ...(id === 'updating-your-profile' ? { profile: nextVal, updatingYourProfile: nextVal } : {}),
    }
    setNotificationPreferences(nextPrefs)
    saveLocalPreferences({ notifications: nextPrefs })
    window.dispatchEvent(new CustomEvent('truehunt-settings-updated', {
      detail: { notifications: nextPrefs, notificationPreferences: nextPrefs },
    }))
    try {
      await api.updateNotificationPreferences({
        [id]: nextVal,
        ...(id === 'connections' ? { 'connecting-with-others': nextVal } : {}),
        ...(id === 'posting' ? { 'posting-and-commenting': nextVal } : {}),
        ...(id === 'profile' ? { 'updating-your-profile': nextVal } : {}),
      })
    } catch (err) {
      console.warn('Notification preference API note:', err)
    }
  }

  async function handleThemeChange(mode) {
    onThemeChange(mode)
    try {
      await api.updateTheme(mode)
    } catch (err) {
      console.warn('Theme update API note:', err)
    }
  }

  async function handleDeleteAccount() {
    if (!window.confirm("Are you sure you want to close and delete your TrueHunt account? This action cannot be undone.")) {
      return
    }
    try {
      await api.deleteAccount()
    } catch (err) {
      window.alert(err.message || 'Failed to schedule account deletion.')
    } finally {
      onAccountDeleted()
    }
  }

  return (
    <section className="settings-page" aria-labelledby="settings-title">
      <header className="settings-page-header">
        <p className="settings-eyebrow">YOUR ACCOUNT</p>
        <h1 id="settings-title">Settings</h1>
        <p>Manage your account preferences, privacy, and notifications.</p>
      </header>

      <div className="settings-layout">
        <aside className="settings-sidebar" aria-label="Settings navigation">
          <section className="settings-sidebar-profile" aria-label="Your profile">
            <span className="settings-avatar" aria-hidden="true">
              {profilePreview
                ? <img src={profilePreview} alt="" />
                : name.charAt(0).toUpperCase()}
            </span>
            <strong>{name}</strong>
            <p>{profileDetails.talents.length > 0 ? profileDetails.talents.join(' · ') : 'Add your professional headline'}</p>
          </section>
          <nav className="settings-sidebar-nav" aria-label="Settings sections">
            {settingsSections.map(({ id, label, icon: Icon }) => (
              <button
                className={`settings-nav-item${activeSection === id ? ' is-active' : ''}`}
                key={id}
                type="button"
                aria-current={activeSection === id ? 'page' : undefined}
                onClick={() => setActiveSection(id)}
              >
                <Icon size={19} strokeWidth={2.3} aria-hidden="true" />
                <span>{label}</span>
              </button>
            ))}
          </nav>
        </aside>

        <div className="settings-content" aria-live="polite">
          <section className="settings-card" aria-labelledby="settings-section-title">
            <div className="settings-section-heading">
              <div>
                <h2 id="settings-section-title">
                  {activeSection === 'notifications' ? 'Notifications you receive' : selectedSection.label}
                </h2>
                {activeSection !== 'notifications' && <p>{selectedSection.description}</p>}
              </div>
              {activeSection === 'preferences' && (
                <button className="settings-edit-button" type="button" onClick={onEditProfile}>
                  Edit profile
                </button>
              )}
            </div>
            {activeSection === 'preferences' && (
              <dl className="settings-details-list">
                <div className="settings-detail-row">
                  <dt>Name</dt>
                  <dd>{name}</dd>
                </div>
                <div className="settings-detail-row">
                  <dt>Email</dt>
                  <dd>{email}</dd>
                </div>
                <div className="settings-detail-row">
                  <dt>Phone</dt>
                  <dd>{phone}</dd>
                </div>
                <div className="settings-detail-row">
                  <dt>Location</dt>
                  <dd>{address}</dd>
                </div>
              </dl>
            )}
            {activeSection === 'security' && (
              <div className="settings-access-list">
                {securityMessage && (
                  <p style={{ padding: '8px 12px', background: 'var(--color-surface, #f0f4f8)', borderRadius: '6px', fontSize: '13px', color: '#1a73e8', marginBottom: '8px' }}>
                    {securityMessage}
                  </p>
                )}
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSecurityMessage(`Verified primary email: ${email}`)}
                >
                  <span>Email addresses</span>
                  <span className="settings-access-value">
                    {email}
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSecurityMessage(`Verified primary phone: ${phone}`)}
                >
                  <span>Phone numbers</span>
                  <span className="settings-access-value">
                    {phone}
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSecurityMessage('Passkeys active: 1 device key (Windows Hello / FIDO2).')}
                >
                  <span>Passkeys</span>
                  <span className="settings-access-value">
                    1 registered
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
                <div className="settings-access-row">
                  <button
                    className="settings-access-action"
                    type="button"
                    aria-expanded={isPasswordFormOpen}
                    aria-controls="settings-password-form"
                    onClick={() => {
                      setIsPasswordFormOpen((open) => !open)
                      setPasswordMessage('')
                      setPasswordIsError(false)
                      setCurrentPassword('')
                      setNewPassword('')
                      setConfirmPassword('')
                      setSecurityMessage('')
                    }}
                  >
                    <span>Change password</span>
                    <ArrowRight size={18} aria-hidden="true" />
                  </button>
                </div>
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => {
                    const next = !twoFactorEnabled
                    setTwoFactorEnabled(next)
                    setSecurityMessage(`Two-factor authentication is now ${next ? 'enabled' : 'disabled'}.`)
                    api.updateSecuritySettings({ twoFactorEnabled: next }).catch((err) => {
                      console.warn('Security settings API note:', err)
                    })
                  }}
                >
                  <span>Two-factor authentication</span>
                  <span className="settings-access-value">
                    {twoFactorEnabled ? 'On (SMS/App)' : 'Off'}
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
              </div>
            )}
            {activeSection === 'security' && isPasswordFormOpen && (
              <form
                className="settings-password-form"
                id="settings-password-form"
                onSubmit={handlePasswordSubmit}
              >
                <h3>Change password</h3>
                <PasswordField
                  id="settings-current-password"
                  name="current-password"
                  label="Current password"
                  inputClassName=""
                  value={currentPassword}
                  onChange={(e) => {
                    setCurrentPassword(e.target.value)
                    setPasswordMessage('')
                  }}
                  placeholder="Enter your current password"
                  autoComplete="current-password"
                  maxLength={100}
                />
                <PasswordField
                  id="settings-new-password"
                  name="new-password"
                  label="New password"
                  inputClassName=""
                  value={newPassword}
                  onChange={(e) => {
                    setNewPassword(e.target.value)
                    setPasswordMessage('')
                  }}
                  placeholder="8-20 characters with A-Z, a-z, 0-9, and @#$%!&*"
                  autoComplete="new-password"
                  showValidation
                />
                <PasswordField
                  id="settings-confirm-password"
                  name="confirm-password"
                  label="Confirm new password"
                  inputClassName=""
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value)
                    setPasswordMessage('')
                  }}
                  placeholder="Re-enter your new password"
                  autoComplete="new-password"
                  isConfirm
                  compareValue={newPassword}
                />
                {passwordMessage && (
                  <p
                    className={passwordIsError ? 'settings-password-error' : 'settings-password-note'}
                    role="status"
                  >
                    {passwordMessage}
                  </p>
                )}
                <button
                  className="settings-edit-button"
                  type="submit"
                  disabled={isUpdatingPassword || !isChangePasswordValid}
                >
                  {isUpdatingPassword ? 'Updating...' : 'Update password'}
                </button>
              </form>
            )}
            {activeSection === 'privacy' && (
              <div className="settings-toggle-list">
                {[
                  ['profileVisibility', 'Public profile', 'Allow other users to view your professional profile.'],
                  ['activityStatus', 'Activity status', 'Show when you are active on TrueHunt Social.'],
                  ['searchVisibility', 'Search visibility', 'Allow your profile to appear in search and suggestions.'],
                ].map(([id, label, description]) => (
                  <div className="settings-toggle-row" key={id}>
                    <span>
                      <strong>{label}</strong>
                      <small>{description}</small>
                    </span>
                    <button
                      className={`settings-notification-switch${privacyPreferences[id] ? ' is-on' : ''}`}
                      type="button"
                      role="switch"
                      aria-checked={privacyPreferences[id]}
                      aria-label={label}
                      onClick={() => updatePreference(setPrivacyPreferences, privacyPreferences, id, 'privacyPreferences')}
                    >
                      <span>{privacyPreferences[id] ? 'On' : 'Off'}</span>
                      <span className="settings-notification-switch-track" aria-hidden="true"><span /></span>
                    </button>
                  </div>
                ))}
              </div>
            )}
            {activeSection === 'block' && (
              <div className="settings-management-section">
                <div className="settings-block-search">
                  <label htmlFor="settings-blocked-user">Search profiles to block</label>
                  <input
                    id="settings-blocked-user"
                    type="search"
                    value={blockedUserInput}
                    onChange={(event) => setBlockedUserInput(event.target.value)}
                    placeholder="Search by name, email, user ID, or craft"
                    autoComplete="off"
                  />
                </div>
                {isProfileDirectoryLoading && (
                  <p className="settings-block-search-note">Loading profiles...</p>
                )}
                {blockSearch && !isProfileDirectoryLoading && (
                  <div className="settings-block-results" aria-label="Profile search results">
                    {searchedProfiles.length === 0 ? (
                      <p className="settings-block-search-note">No matching profiles found.</p>
                    ) : searchedProfiles.map((profile) => {
                      const identity = String(profile.userId || profile.id || profile.email || profile.name).trim()
                      const isBlocked = blockedIdentitySet.has(identity.toLowerCase())
                      const photo = profile.profileImageUrl || profile.avatar || profile.profileImage || ''
                      return (
                        <div className="settings-profile-result" key={identity}>
                          <span className="settings-profile-photo" aria-hidden="true">
                            {photo
                              ? <img src={photo} alt="" />
                              : (profile.name || 'U').charAt(0).toUpperCase()}
                          </span>
                          <span className="settings-profile-result-info">
                            <strong>{profile.name || 'Unknown profile'}</strong>
                            <small>{profile.primaryCraft || profile.craft || profile.email || identity}</small>
                          </span>
                          {isBlocked ? (
                            <span className="settings-blocked-label">Blocked</span>
                          ) : (
                            <button type="button" className="settings-block-button" onClick={() => openBlockReason(profile)}>
                              <Ban size={15} aria-hidden="true" />
                              Block
                            </button>
                          )}
                        </div>
                      )
                    })}
                  </div>
                )}
                <div className="settings-block-list">
                  {blockedUsers.length === 0 ? (
                    <p>You have not blocked anyone.</p>
                  ) : blockedUsers.map((blockedUser) => {
                    const identity = String(blockedUser.identity || blockedUser)
                    return (
                      <div className="settings-block-row" key={identity}>
                        <span className="settings-blocked-profile">
                          <span className="settings-profile-photo is-small" aria-hidden="true">
                            {blockedUser.photo
                              ? <img src={blockedUser.photo} alt="" />
                              : (blockedUser.name || 'U').charAt(0).toUpperCase()}
                          </span>
                          <span>
                            <strong>{blockedUser.name || identity}</strong>
                            <small>Reason: {blockedUser.reason || 'Not specified'}</small>
                          </span>
                        </span>
                        <button type="button" onClick={() => handleRemoveBlockedUser(identity)}>
                          Unblock
                        </button>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}
            {activeSection === 'restricted' && (
              <div className="settings-toggle-list">
                {[
                  ['sensitiveContent', 'Sensitive content', 'Filter content that may be sensitive or contain mature themes.'],
                  ['restrictedMessages', 'Restrict messages', 'Move messages from people outside your network to a restricted list.'],
                ].map(([id, label, description]) => (
                  <div className="settings-toggle-row" key={id}>
                    <span>
                      <strong>{label}</strong>
                      <small>{description}</small>
                    </span>
                    <button
                      className={`settings-notification-switch${restrictedPreferences[id] ? ' is-on' : ''}`}
                      type="button"
                      role="switch"
                      aria-checked={restrictedPreferences[id]}
                      aria-label={label}
                      onClick={() => updatePreference(setRestrictedPreferences, restrictedPreferences, id, 'restrictedPreferences')}
                    >
                      <span>{restrictedPreferences[id] ? 'On' : 'Off'}</span>
                      <span className="settings-notification-switch-track" aria-hidden="true"><span /></span>
                    </button>
                  </div>
                ))}
              </div>
            )}
            {activeSection === 'photo-video-url' && (
              <div className="settings-toggle-list">
                {[
                  ['photoPosts', 'Photo posts', 'Choose who can view photos in your posts.'],
                  ['videoPosts', 'Video posts', 'Choose who can view videos in your posts.'],
                  ['externalUrls', 'URLs', 'Choose who can view links in your posts.'],
                ].map(([id, label, description]) => (
                  <div className="settings-toggle-row" key={id}>
                    <span>
                      <strong>{label}</strong>
                      <small>{description}</small>
                    </span>
                    <fieldset className="settings-visibility-options">
                      <legend className="settings-sr-only">{label} visibility</legend>
                      {['public', 'private'].map((visibility) => (
                        <label
                          className={`settings-visibility-option${mediaPreferences[id] === visibility ? ' is-selected' : ''}`}
                          key={visibility}
                        >
                          <input
                            type="radio"
                            name={`visibility-${id}`}
                            value={visibility}
                            checked={mediaPreferences[id] === visibility}
                            onChange={() => updateMediaVisibility(id, visibility)}
                          />
                          <span>{visibility === 'public' ? 'Public' : 'Private'}</span>
                        </label>
                      ))}
                    </fieldset>
                  </div>
                ))}
              </div>
            )}
            {activeSection === 'like-share-counts' && (
              <div className="settings-toggle-list">
                {[
                  ['likeCounts', 'Like counts', 'Show the number of likes on your posts.'],
                  ['shareCounts', 'Share counts', 'Show the number of times your posts are shared.'],
                ].map(([id, label, description]) => (
                  <div className="settings-toggle-row" key={id}>
                    <span>
                      <strong>{label}</strong>
                      <small>{description}</small>
                    </span>
                    <button
                      className={`settings-notification-switch${engagementPreferences[id] ? ' is-on' : ''}`}
                      type="button"
                      role="switch"
                      aria-checked={engagementPreferences[id]}
                      aria-label={label}
                      onClick={() => updatePreference(setEngagementPreferences, engagementPreferences, id, 'engagementPreferences')}
                    >
                      <span>{engagementPreferences[id] ? 'On' : 'Off'}</span>
                      <span className="settings-notification-switch-track" aria-hidden="true"><span /></span>
                    </button>
                  </div>
                ))}
              </div>
            )}
            {activeSection === 'notifications' && (
              <ul className="settings-notification-list" aria-label="Notification categories">
                {notificationCategories.map(({ id, label, settingLabel }) => {
                  const isExpanded = Boolean(expandedNotificationCategories[id])
                  const settingId = `notification-setting-${id}`

                  return (
                    <li className="settings-notification-item" key={id}>
                      <button
                        className="settings-notification-row"
                        type="button"
                        aria-expanded={isExpanded}
                        aria-controls={settingId}
                        onClick={() => {
                          setExpandedNotificationCategories((expanded) => ({
                            ...expanded,
                            [id]: !expanded[id],
                          }))
                        }}
                      >
                        <span>{label}</span>
                        <ArrowRight
                          className={isExpanded ? 'is-expanded' : ''}
                          size={18}
                          aria-hidden="true"
                        />
                      </button>
                      <div
                        className="settings-notification-setting"
                        id={settingId}
                        hidden={!isExpanded}
                      >
                        <span>{settingLabel}</span>
                        <button
                          className={`settings-notification-switch${notificationPreferences[id] ? ' is-on' : ''}`}
                          type="button"
                          role="switch"
                          aria-checked={notificationPreferences[id]}
                          aria-label={settingLabel}
                          onClick={() => handleToggleNotification(id)}
                        >
                          <span>{notificationPreferences[id] ? 'On' : 'Off'}</span>
                          <span className="settings-notification-switch-track" aria-hidden="true">
                            <span />
                          </span>
                        </button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
            {activeSection === 'saved' && (
              <>
                <div className="settings-saved-filters" aria-label="Filter saved posts">
                  {[
                    { id: 'images', label: 'Images', Icon: Image },
                    { id: 'videos', label: 'Videos', Icon: Video },
                    { id: 'text', label: 'Text posts', Icon: FileText },
                  ].map(({ id, label, Icon }) => (
                    <button
                      type="button"
                      key={id}
                      aria-pressed={savedPostFilter === id}
                      onClick={() => setSavedPostFilter(id)}
                    >
                      <Icon size={16} aria-hidden="true" />
                      {label}
                    </button>
                  ))}
                </div>
                <div className="settings-saved-list">
                  {savedPosts.length === 0 ? (
                    <p className="settings-empty-state">You have no saved posts yet.</p>
                  ) : filteredSavedPosts.length === 0 ? (
                    <p className="settings-empty-state">
                      No saved {savedPostFilter === 'text' ? 'text posts' : savedPostFilter} yet.
                    </p>
                  ) : filteredSavedPosts.map((post) => {
                    const media = getSavedPostMedia(post)
                    const isOwnerRemoved = Boolean(post.ownerRemoved)
                    const authorName = post.authorName || 'TrueHunt member'
                    const authorInitials = authorName
                      .split(/\s+/)
                      .filter(Boolean)
                      .slice(0, 2)
                      .map((part) => part[0])
                      .join('')
                    const authorAvatar = post.authorAvatar || post.profileImageUrl || post.avatar
                    const postText = post.content || post.text || post.body || post.caption || post.description
                    return (
                      <article
                        className={`settings-saved-item${media ? '' : ' is-text-only'}${isOwnerRemoved ? ' is-owner-removed' : ' is-clickable'}`}
                        key={post.id}
                        onClick={(event) => {
                          if (isOwnerRemoved || event.target.closest('button, video, a')) return
                          onOpenSavedPost?.(post)
                        }}
                      >
                        {media ? (
                          isOwnerRemoved ? (
                            <div className="settings-saved-media settings-saved-media-removed">
                              <span>This post is unavailable</span>
                            </div>
                          ) : (
                            <div className="settings-saved-media">
                              {media.isVideo
                                ? <video src={media.url} controls preload="metadata" playsInline aria-label="Saved post video" />
                                : <img src={media.url} alt="Saved post" />}
                            </div>
                          )
                        ) : (
                          <div className="settings-saved-text-post">
                            <header className="settings-saved-text-author">
                              {authorAvatar
                                ? <img src={authorAvatar} alt="" />
                                : <span aria-hidden="true">{authorInitials || 'T'}</span>}
                              <div>
                                <strong>{authorName}</strong>
                                {(post.authorHeadline || post.authorRole || post.headline) && (
                                  <small>{post.authorHeadline || post.authorRole || post.headline}</small>
                                )}
                                {post.createdAt && <time>{post.createdAt}</time>}
                              </div>
                              {!isOwnerRemoved && (
                                <button
                                  className="settings-saved-view-post"
                                  type="button"
                                  onClick={() => onOpenSavedPost?.(post)}
                                >
                                  View post
                                </button>
                              )}
                              <button
                                className="settings-saved-text-remove"
                                type="button"
                                onClick={() => removeSavedPost(post.id)}
                              >
                                Remove
                              </button>
                            </header>
                            {isOwnerRemoved ? (
                              <p className="settings-saved-removed-message">This post is unavailable.</p>
                            ) : (
                              <>
                                {post.title && <h3>{post.title}</h3>}
                                <p>{postText || 'Shared a post'}</p>
                              </>
                            )}
                          </div>
                        )}
                        {media && (
                          <div className="settings-saved-details">
                            <strong>{authorName}</strong>
                            <p>{isOwnerRemoved ? 'This post is unavailable.' : post.title || post.content || 'Shared a post'}</p>
                          </div>
                        )}
                        {media && (
                          !isOwnerRemoved && (
                            <button
                              className="settings-saved-view-post"
                              type="button"
                              onClick={() => onOpenSavedPost?.(post)}
                            >
                              View post
                            </button>
                          )
                        )}
                        {media && (
                          <button type="button" onClick={() => removeSavedPost(post.id)}>
                            Remove
                          </button>
                        )}
                      </article>
                    )
                  })}
                </div>
              </>
            )}
            {activeSection === 'reports' && (
              <div className="settings-reports-list">
                {reportRemovalError && <p className="settings-report-error" role="alert">{reportRemovalError}</p>}
                {reportedPosts.length === 0 ? (
                  <p className="settings-empty-state">You have not reported any posts.</p>
                ) : reportedPosts.map((report, index) => (
                  <article className="settings-report-item" key={report.reportedAt || `${report.postId}-${index}`}>
                    <div>
                      <strong>Post by {report.authorName || 'TrueHunt member'}</strong>
                      <p>{report.title || report.content || 'Shared a post'}</p>
                      <small>Reason: {report.reason}</small>
                    </div>
                    <div className="settings-report-actions">
                      <time>{report.reportedAt ? new Date(report.reportedAt).toLocaleDateString() : ''}</time>
                      <button
                        className="settings-report-remove"
                        type="button"
                        onClick={() => void removeReportedPost(report, index)}
                        disabled={Boolean(removingReportId)}
                      >
                        {removingReportId === String(report.id) ? 'Removing…' : 'Remove'}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
          {activeSection === 'preferences' && (
            <section className="settings-card settings-display-card" aria-labelledby="settings-display-title">
              <button
                className="settings-display-toggle"
                type="button"
                aria-expanded={isThemeOptionsOpen}
                aria-controls="settings-theme-options"
                onClick={() => setIsThemeOptionsOpen((open) => !open)}
              >
                <span>
                  <strong id="settings-display-title">Display</strong>
                  <small>Choose how TrueHunt looks on this device.</small>
                </span>
                <span className="settings-current-theme">
                  {theme === 'light' ? 'Light mode' : 'Dark mode'}
                  <span aria-hidden="true">{isThemeOptionsOpen ? '⌃' : '⌄'}</span>
                </span>
              </button>
              {isThemeOptionsOpen && (
                <fieldset className="settings-theme-options" id="settings-theme-options">
                  <legend>Choose a theme</legend>
                  {['light', 'dark'].map((mode) => (
                    <label className={`settings-theme-option${theme === mode ? ' is-selected' : ''}`} key={mode}>
                      <input
                        type="radio"
                        name="theme"
                        value={mode}
                        checked={theme === mode}
                        onChange={() => handleThemeChange(mode)}
                      />
                      <span>{mode === 'light' ? 'Light mode' : 'Dark mode'}</span>
                    </label>
                  ))}
                </fieldset>
              )}
            </section>
          )}
          {activeSection === 'preferences' && (
            <section className="settings-card settings-management-card" aria-labelledby="settings-management-title">
              <h2 id="settings-management-title">Account management</h2>
              <div className="settings-management-list">
                <button
                  className="settings-management-action"
                  type="button"
                  onClick={handleDeleteAccount}
                >
                  <span>Close and delete account</span>
                  <ArrowRight size={18} aria-hidden="true" />
                </button>
              </div>
            </section>
          )}
          {blockTarget && (
            <div className="settings-modal-backdrop" role="presentation" onMouseDown={(event) => {
              if (event.target === event.currentTarget) setBlockTarget(null)
            }}>
              <form className="settings-block-modal" role="dialog" aria-modal="true" aria-labelledby="block-profile-title" onSubmit={handleConfirmBlock}>
                <h2 id="block-profile-title">Block {blockTarget.name || 'this profile'}?</h2>
                <div className="settings-block-modal-profile">
                  <span className="settings-profile-photo" aria-hidden="true">
                    {(blockTarget.profileImageUrl || blockTarget.avatar || blockTarget.profileImage)
                      ? <img src={blockTarget.profileImageUrl || blockTarget.avatar || blockTarget.profileImage} alt="" />
                      : (blockTarget.name || 'U').charAt(0).toUpperCase()}
                  </span>
                  <span>
                    <strong>{blockTarget.name || 'Unknown profile'}</strong>
                    <small>{blockTarget.primaryCraft || blockTarget.craft || blockTarget.email || ''}</small>
                  </span>
                </div>
                <fieldset className="settings-block-reasons">
                  <legend>Select a reason</legend>
                  {blockReasons.map((reason) => (
                    <label key={reason} className={blockReason === reason ? 'is-selected' : ''}>
                      <input
                        type="radio"
                        name="block-reason"
                        value={reason}
                        checked={blockReason === reason}
                        onChange={(event) => setBlockReason(event.target.value)}
                      />
                      <span>{reason}</span>
                    </label>
                  ))}
                </fieldset>
                {blockReason === 'Custom reason' && (
                  <label className="settings-custom-block-reason" htmlFor="custom-block-reason">
                    Custom reason
                    <textarea
                      id="custom-block-reason"
                      value={customBlockReason}
                      onChange={(event) => setCustomBlockReason(event.target.value)}
                      placeholder="Describe why you are blocking this profile"
                      maxLength={240}
                      required
                    />
                  </label>
                )}
                <div className="settings-block-modal-actions">
                  <button type="button" className="settings-edit-button" onClick={() => setBlockTarget(null)}>
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="settings-block-confirm"
                    disabled={!blockReason || (blockReason === 'Custom reason' && !customBlockReason.trim())}
                  >
                    Block profile
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}

export default Settings