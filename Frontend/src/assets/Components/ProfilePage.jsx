import { useEffect, useRef, useState } from 'react'
import { Building2, Check, ChevronRight, Eye, EyeOff, Trash2, UserPlus } from 'lucide-react'
import './StylePage.css'
import api from '../../api'
import { useAuth } from '../../context/AuthContext'
import OpportunitiesPage from './OpportunitiesPage.jsx'
import { EXPERIENCE_OPTIONS, REGISTRATION_CRAFTS, SUB_CRAFTS_BY_PRIMARY_CRAFT } from '../../services/mockData'

export const emptyProfileDetails = {
  userId: '',
  name: '',
  phone: '',
  professionalPhone: '',
  email: '',
  bio: '',
  experience: '',
  primaryCraft: '',
  subCraft: '',
  secondaryCrafts: [],
  secondarySubCrafts: [],
  dateOfBirth: '',
  height: '',
  weight: '',
  bodyBuild: '',
  gender: '',
  facialHair: '',
  skinTone: '',
  address: '',
  languages: [],
  otherLanguage: '',
  talents: [],
  workTypes: [],
  videos: [],
  videoUrls: [],
  experiences: [],
  posts: [],
}

const MAX_VIDEO_SIZE = 5 * 1024 * 1024
const MAX_VIDEO_DURATION = 3 * 60
const MAX_ABOUT_WORDS = 300
const PROFILE_IMAGE_CROP_SIZE = 360
const PROFILE_IMAGE_OUTPUT_SIZE = 512
const PHONE_ACCESS_EVENT = 'truehunt-professional-phone-access-changed'
const CINEMA_ORGANIZATIONS = [
  { name: 'Netflix', domain: 'netflix.com' },
  { name: 'Disney', domain: 'disney.com' },
  { name: 'Marvel Studios', domain: 'marvel.com' },
  { name: 'Warner Bros. Pictures', domain: 'warnerbros.com' },
  { name: 'Universal Pictures', domain: 'universalpictures.com' },
  { name: 'Sony Pictures', domain: 'sonypictures.com' },
  { name: 'Paramount Pictures', domain: 'paramount.com' },
  { name: 'Amazon MGM Studios', domain: 'amazon.com' },
  { name: 'HBO', domain: 'hbo.com' },
  { name: 'A24', domain: 'a24films.com' },
  { name: 'Lionsgate', domain: 'lionsgate.com' },
  { name: 'Yash Raj Films', domain: 'yrf.com' },
  { name: 'Dharma Productions', domain: 'dharmamovies.com' },
  { name: 'Red Chillies Entertainment', domain: 'redchillies.com' },
  { name: 'Sun Pictures', domain: 'sunpictures.in' },
  { name: 'Mythri Movie Makers', domain: 'mythrimoviemakers.com' },
  { name: 'Hombale Films', domain: 'hombalefilms.com' },
  { name: 'Geetha Arts', domain: 'geethaarts.com' },
]

function getCinemaOrganizationLogoUrl(organizationName) {
  const normalizedName = String(organizationName || '').trim().toLowerCase()
  if (!normalizedName) return ''

  const organization = CINEMA_ORGANIZATIONS.find(({ name }) => (
    name.toLowerCase() === normalizedName
    || (normalizedName.length >= 4 && name.toLowerCase().includes(normalizedName))
    || (name.toLowerCase().length >= 4 && normalizedName.includes(name.toLowerCase()))
  ))

  return organization
    ? `https://www.google.com/s2/favicons?domain=${organization.domain}&sz=128`
    : ''
}

function getOrganizationLogoUrl(organizationName, experiences = []) {
  const normalizedName = String(organizationName || '').trim().toLowerCase()
  if (!normalizedName) return ''

  const savedExperience = experiences.find((experience) => (
    String(experience.organizationName || experience.company || experience.name || '').trim().toLowerCase()
      === normalizedName
  ))

  const savedLogoUrl = savedExperience?.organizationLogoUrl
    || savedExperience?.companyLogoUrl
    || savedExperience?.logoUrl

  return savedLogoUrl
    || getCinemaOrganizationLogoUrl(organizationName)
    || ''
}

function OrganizationPicker({ options, value, onChange }) {
  const [isOpen, setIsOpen] = useState(false)
  const [isOtherSelected, setIsOtherSelected] = useState(false)
  const matchingOptions = options.filter((option) => (
    !value.trim() || option.name.toLowerCase().includes(value.trim().toLowerCase())
  ))
  const matchingLogo = getOrganizationLogoUrl(value, options)

  if (isOtherSelected) {
    return (
      <div className="organization-picker">
        <input
          id="experience-organization"
          name="organizationName"
          type="text"
          value={value}
          placeholder="Enter organization name"
          autoComplete="organization"
          onChange={(event) => onChange(event.target.value)}
          required
        />
        <button
          className="organization-picker-option"
          type="button"
          onClick={() => {
            onChange('')
            setIsOtherSelected(false)
            setIsOpen(true)
          }}
        >
          Choose from organizations
        </button>
      </div>
    )
  }

  return (
    <div
      className="organization-picker"
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setIsOpen(false)
      }}
    >
      <input
        id="experience-organization"
        name="organizationName"
        type="text"
        value={value}
        placeholder="Search or enter a studio"
        autoComplete="off"
        onFocus={() => {
          setIsOpen(true)
        }}
        onChange={(event) => {
          onChange(event.target.value)
          setIsOpen(true)
        }}
        onKeyDown={(event) => {
          if (event.key === 'Enter' && isOpen && matchingOptions.length > 0) {
            event.preventDefault()
            onChange(matchingOptions[0].name)
            setIsOpen(false)
          }
          if (event.key === 'Escape') setIsOpen(false)
        }}
        required
      />
      {isOpen && (
        <div className="organization-picker-options" role="listbox" aria-label="Organizations">
          {matchingOptions.map((option) => (
            <button
              className="organization-picker-option"
              type="button"
              role="option"
              aria-selected={option.name.toLowerCase() === value.trim().toLowerCase()}
              key={option.name.toLowerCase()}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => {
                onChange(option.name)
                setIsOpen(false)
              }}
            >
              <span className="experience-organization-logo" aria-hidden="true">
                <Building2 size={15} />
                {option.logoUrl && (
                  <img
                    src={option.logoUrl}
                    alt=""
                    onError={(event) => {
                      event.currentTarget.hidden = true
                    }}
                  />
                )}
              </span>
              <span>{option.name}</span>
            </button>
          ))}
          <button
            className="organization-picker-option"
            type="button"
            role="option"
            aria-selected={false}
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => {
              onChange('')
              setIsOtherSelected(true)
              setIsOpen(false)
            }}
          >
            Other
          </button>
        </div>
      )}
      {value.trim() && (
        <span className="organization-logo-preview">
          <span className="experience-organization-logo experience-organization-logo-large" aria-hidden="true">
            <Building2 size={15} />
            {matchingLogo && (
              <img
                src={matchingLogo}
                alt=""
                onError={(event) => {
                  event.currentTarget.hidden = true
                }}
              />
            )}
          </span>
          {matchingLogo ? 'Organization logo selected' : 'No logo available for this organization'}
        </span>
      )}
    </div>
  )
}

function getPhoneAccessStorageKey(profileId) {
  return `truehunt-professional-phone-requests:${encodeURIComponent(profileId)}`
}

function readPhoneAccessRequests(profileId) {
  try {
    const savedRequests = localStorage.getItem(getPhoneAccessStorageKey(profileId))
    const requests = savedRequests ? JSON.parse(savedRequests) : []
    return Array.isArray(requests) ? requests : []
  } catch (error) {
    console.warn('Could not read professional phone access requests:', error)
    return []
  }
}

function savePhoneAccessRequests(profileId, requests) {
  localStorage.setItem(getPhoneAccessStorageKey(profileId), JSON.stringify(requests))
  window.dispatchEvent(new CustomEvent(PHONE_ACCESS_EVENT, { detail: { profileId } }))
}

function getCurrentUser() {
  try {
    const savedUser = localStorage.getItem('truehunt_user')
    return savedUser ? JSON.parse(savedUser) : null
  } catch (error) {
    console.warn('Could not identify the signed-in user for phone access:', error)
    return null
  }
}

function getCurrentUserIdentity() {
  const user = getCurrentUser()
  const userId = user?.id || user?.email
  if (userId) return String(userId)

  try {
    let anonymousId = localStorage.getItem('truehunt_phone_access_requester_id')
    if (!anonymousId) {
      anonymousId = `visitor-${globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`}`
      localStorage.setItem('truehunt_phone_access_requester_id', anonymousId)
    }
    return anonymousId
  } catch (error) {
    console.warn('Could not create an identity for professional phone access:', error)
    return ''
  }
}

function getAgeGroup(dateOfBirth) {
  if (!dateOfBirth) return ''

  const [year, month, day] = dateOfBirth.split('-').map(Number)
  const birthDate = new Date(year, month - 1, day)
  const today = new Date()

  if (
    !year
    || birthDate.getFullYear() !== year
    || birthDate.getMonth() !== month - 1
    || birthDate.getDate() !== day
    || birthDate > today
  ) {
    return ''
  }

  let age = today.getFullYear() - year
  if (
    today.getMonth() < month - 1
    || (today.getMonth() === month - 1 && today.getDate() < day)
  ) {
    age -= 1
  }

  if (age <= 12) return 'Child'
  if (age <= 20) return 'Teenager'
  if (age <= 50) return 'Adult'
  return 'Senior'
}

function getTodayDateString() {
  const today = new Date()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${today.getFullYear()}-${month}-${day}`
}

function getProfilePostsStorageKey(profileId) {
  return `truehunt-social-posts:${encodeURIComponent(profileId || 'me')}`
}

function readProfilePosts(profileId, fallbackPosts = []) {
  try {
    const savedPosts = localStorage.getItem(getProfilePostsStorageKey(profileId))
    const posts = savedPosts ? JSON.parse(savedPosts) : null
    return Array.isArray(posts) ? posts : fallbackPosts
  } catch (error) {
    console.warn('Could not load saved profile posts:', error)
    return fallbackPosts
  }
}

function getProfilePostMedia(post) {
  const media = post.media && typeof post.media === 'object' ? post.media : {}
  const url = media.url || post.mediaUrl
  if (!url) return null

  const mediaType = String(media.contentType || media.type || post.mediaType || '').toLowerCase()
  return {
    ...media,
    url,
    kind: media.kind || (mediaType.includes('video') ? 'video' : 'image'),
  }
}

function getProfileCompletion(profileDetails, hasProfileImage) {
  const hasValue = (value) => String(value ?? '').trim().length > 0
  const completedFields = [
    hasProfileImage,
    hasValue(profileDetails.name),
    hasValue(profileDetails.phone),
    hasValue(profileDetails.email),
    hasValue(profileDetails.experience),
    hasValue(profileDetails.dateOfBirth),
    hasValue(profileDetails.height),
    hasValue(profileDetails.weight),
    hasValue(profileDetails.gender),
    hasValue(profileDetails.skinTone),
    hasValue(profileDetails.address),
    (profileDetails.languages || []).length > 0,
    (profileDetails.talents || []).length > 0,
    (profileDetails.workTypes || []).length > 0,
    (profileDetails.experiences || []).length > 0,
  ]

  return Math.round((completedFields.filter(Boolean).length / completedFields.length) * 100)
}

const languages = [
  'Telugu',
  'Hindi',
  'English',
  'Tamil',
  'Kannada',
  'Malayalam',
  'Bengali',
  'Marathi',
  'Other',
]

const talents = REGISTRATION_CRAFTS

const workTypes = [
  'series',
  'Cinema',
  'webseries',
  'shortflim',
  'Reels',
  'Advertisements',
  'others'
]

function SelectionGroup({ label, options, selected, onChange }) {
  function toggleOption(option) {
    onChange(
      selected.includes(option)
        ? selected.filter((item) => item !== option)
        : [...selected, option],
    )
  }

  return (
    <fieldset className="selection-group">
      <legend>{label}</legend>
      <div className="option-list">
        {options.map((option) => (
          <label className="choice-chip" key={option}>
            <input
              type="checkbox"
              checked={selected.includes(option)}
              onChange={() => toggleOption(option)}
            />
            <span>{option}</span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}

function PreviewSectionHeading({ children, isOwner, onEdit, sectionId }) {
  return (
    <div className="preview-section-heading">
      <h3>{children}</h3>
      {isOwner && (
        <button
          className="language-edit-button"
          type="button"
          onClick={() => onEdit(sectionId)}
          aria-label={`Edit ${children}`}
        >
          Edit
        </button>
      )}
    </div>
  )
}

function ProfilePage({
  isOwner = true,
  isActive = true,
  accountUserId = '',
  profileDetails,
  setProfileDetails,
  profilePreview,
  setProfilePreview,
  onMessage,
  onViewConnections,
}) {
  const { user: authUser, updateUser } = useAuth()
  const [profilePic, setProfilePic] = useState(null)
  const [isProfileImageRemoved, setIsProfileImageRemoved] = useState(false)
  const [pendingProfileCrop, setPendingProfileCrop] = useState(null)
  const [cropImageDimensions, setCropImageDimensions] = useState(null)
  const [cropStageSize, setCropStageSize] = useState(PROFILE_IMAGE_CROP_SIZE)
  const [cropFrame, setCropFrame] = useState({
    x: 0,
    y: 0,
    size: PROFILE_IMAGE_CROP_SIZE,
  })
  const [cropZoom, setCropZoom] = useState(1)
  const [cropPosition, setCropPosition] = useState({ x: 0, y: 0 })
  const profilePictureInput = useRef(null)
  const cropStageRef = useRef(null)
  const cropImageRef = useRef(null)
  const cropInteractionRef = useRef(null)
  const [selectedLanguages, setSelectedLanguages] = useState([])
  const [otherLanguageDraft, setOtherLanguageDraft] = useState('')
  const [selectedTalents, setSelectedTalents] = useState([])
  const [selectedSubCraft, setSelectedSubCraft] = useState('')
  const [secondaryCraftToAdd, setSecondaryCraftToAdd] = useState('')
  const [selectedWorkTypes, setSelectedWorkTypes] = useState([])
  const [otherWorkTypeDraft, setOtherWorkTypeDraft] = useState('')
  const [uploadedVideos, setUploadedVideos] = useState([null, null, null])
  const uploadedVideoUrls = useRef([])
  const [videoUrlInput, setVideoUrlInput] = useState('')
  const [videoUrlError, setVideoUrlError] = useState('')
  const [savedVideoUrls, setSavedVideoUrls] = useState([])
  const [validationError, setValidationError] = useState('')
  const [aboutDraft, setAboutDraft] = useState(profileDetails.bio || '')
  const [isSavingAbout, setIsSavingAbout] = useState(false)
  const [isSubmitted, setIsSubmitted] = useState(false)
  const [isSubmittingProfile, setIsSubmittingProfile] = useState(false)
  const [profileFormKey, setProfileFormKey] = useState(0)
  const [isEditingProfile, setIsEditingProfile] = useState(false)
  const [dateOfBirthDraft, setDateOfBirthDraft] = useState('')
  const [genderDraft, setGenderDraft] = useState(profileDetails.gender || '')
  const [isExperienceModalOpen, setIsExperienceModalOpen] = useState(false)
  const [editingExperienceId, setEditingExperienceId] = useState(null)
  const [experienceAttachmentType, setExperienceAttachmentType] = useState('')
  const [experienceSaveError, setExperienceSaveError] = useState('')
  const [isSavingExperience, setIsSavingExperience] = useState(false)
  const [organizationDraft, setOrganizationDraft] = useState('')
  const [isContactModalOpen, setIsContactModalOpen] = useState(false)
  const [phoneAccessState, setPhoneAccessState] = useState(() => {
    const profileId = String(profileDetails.userId || 'me')
    return { profileId, requests: readPhoneAccessRequests(profileId) }
  })
  const [phoneRequestMessage, setPhoneRequestMessage] = useState('')
  const [editingSection, setEditingSection] = useState('')
  const [openProfileActionsFor, setOpenProfileActionsFor] = useState(null)
  const [connectionState, setConnectionState] = useState({
    profileId: String(profileDetails.userId || profileDetails.id || ''),
    connected: Boolean(profileDetails.isConnected),
    pending: Boolean(profileDetails.isPending || profileDetails.isOutgoingPending),
  })
  const [connectedProfiles, setConnectedProfiles] = useState(null)
  const [followState, setFollowState] = useState({
    profileId: String(profileDetails.userId || profileDetails.id || ''),
    followed: Boolean(profileDetails.isFollowing),
  })
  const [relationshipActionState, setRelationshipActionState] = useState({ profileId: '', action: '' })
  const [relationshipErrorState, setRelationshipErrorState] = useState({ profileId: '', message: '' })
  const profileActionsRef = useRef(null)
  const profilePostsListRef = useRef(null)
  const profileAccessId = String(profileDetails.userId || 'me')
  const targetProfileId = String(profileDetails.userId || profileDetails.id || '')
  const isOwnerView = isOwner
  const phoneAccessRequests = phoneAccessState.profileId === profileAccessId
    ? phoneAccessState.requests
    : readPhoneAccessRequests(profileAccessId)
  const [currentUser] = useState(getCurrentUser)
  const [currentUserId] = useState(getCurrentUserIdentity)
  const profilePostOwnerId = String(
    profileDetails.userId || profileDetails.id || accountUserId || currentUserId || 'me',
  ).trim()
  const [profilePostsState, setProfilePostsState] = useState(() => ({
    profileId: profilePostOwnerId,
    posts: readProfilePosts(profilePostOwnerId, profileDetails.posts || []),
  }))
  const profilePosts = (profilePostsState.profileId === profilePostOwnerId
    ? profilePostsState.posts
    : profileDetails.posts || [])
  const [editingProfilePostId, setEditingProfilePostId] = useState('')
  const [editingOpportunityPost, setEditingOpportunityPost] = useState(null)
  const [profilePostTitleDraft, setProfilePostTitleDraft] = useState('')
  const [profilePostContentDraft, setProfilePostContentDraft] = useState('')
  const [profilePostMediaDraft, setProfilePostMediaDraft] = useState(null)
  const profilePostMediaPreviewRef = useRef('')
  const [isSavingProfilePost, setIsSavingProfilePost] = useState(false)
  const [isProfileUserIdVisible, setIsProfileUserIdVisible] = useState(false)
  const [profilePostError, setProfilePostError] = useState('')

  useEffect(() => () => {
    if (profilePostMediaPreviewRef.current) {
      URL.revokeObjectURL(profilePostMediaPreviewRef.current)
    }
  }, [])
  const currentPhoneRequest = phoneAccessRequests.find((request) => (
    request.requesterId === currentUserId
  ))
  const displayedUserId = String(
    accountUserId
      || profileDetails.userId
      || profileDetails.id
      || currentUser?.userId
      || currentUser?.id
      || '',
  ).trim()
  const rawConnectionCount = isOwnerView
    ? (connectedProfiles?.length ?? profileDetails.connectionsCount)
    : profileDetails.connectionsCount
  const parsedConnectionCount = Number(rawConnectionCount)
  const connectionCount = Number.isFinite(parsedConnectionCount) && parsedConnectionCount !== 128
    ? parsedConnectionCount
    : (connectedProfiles?.length ?? 0)
  const formattedConnectionCount = connectionCount >= 500 ? '500+' : connectionCount
  const hasProfessionalPhoneAccess = isOwnerView || currentPhoneRequest?.status === 'approved'
  const experienceBeingEdited = (profileDetails.experiences || [])
    .find((experience) => experience.id === editingExperienceId)
  const experiences = profileDetails.experiences || []
  const hasFeaturedExperience = experiences.some((experience) => (
    experience.featuredOnProfile
    && String(experience.organizationName || experience.company || '').trim()
  ))
  const latestExperienceWithOrganization = [...experiences].reverse().find((experience) => (
    String(experience.organizationName || experience.company || '').trim()
  ))
  const preferredProfileExperience = experiences.find((experience) => (
    experience.featuredOnProfile
    && String(experience.organizationName || experience.company || '').trim()
  ))
  const profileOrganization = preferredProfileExperience
    ? {
        name: String(
          preferredProfileExperience.organizationName || preferredProfileExperience.company,
        ).trim(),
        logoUrl: getOrganizationLogoUrl(
          preferredProfileExperience.organizationName || preferredProfileExperience.company,
          experiences,
        ),
      }
    : null
  const organizationOptionMap = new Map()
  for (const experience of experiences) {
    const name = String(experience.organizationName || experience.company || '').trim()
    const logoUrl = experience.organizationLogoUrl || experience.companyLogoUrl || ''
    if (name && !organizationOptionMap.has(name.toLowerCase())) {
      organizationOptionMap.set(name.toLowerCase(), {
        name,
        logoUrl,
      })
    }
  }
  for (const organization of CINEMA_ORGANIZATIONS) {
    if (!organizationOptionMap.has(organization.name.toLowerCase())) {
      organizationOptionMap.set(organization.name.toLowerCase(), {
        name: organization.name,
        logoUrl: getCinemaOrganizationLogoUrl(organization.name),
      })
    }
  }
  const organizationOptions = Array.from(organizationOptionMap.values())
  const primaryCraft = profileDetails.primaryCraft || profileDetails.talents?.[0] || ''

  useEffect(() => {
    if (openProfileActionsFor !== targetProfileId) return undefined

    function dismissProfileActions(event) {
      if (event.type === 'keydown' && event.key !== 'Escape') return
      if (event.type === 'pointerdown' && profileActionsRef.current?.contains(event.target)) return
      setOpenProfileActionsFor(null)
    }

    document.addEventListener('pointerdown', dismissProfileActions)
    document.addEventListener('keydown', dismissProfileActions)
    return () => {
      document.removeEventListener('pointerdown', dismissProfileActions)
      document.removeEventListener('keydown', dismissProfileActions)
    }
  }, [openProfileActionsFor, targetProfileId])

  useEffect(() => {
    function refreshPhoneAccessRequests(event) {
      if (!event.detail?.profileId || event.detail.profileId === profileAccessId) {
        setPhoneAccessState({
          profileId: profileAccessId,
          requests: readPhoneAccessRequests(profileAccessId),
        })
      }
    }

    function refreshFromOtherTab(event) {
      if (!event.key || event.key === getPhoneAccessStorageKey(profileAccessId)) {
        setPhoneAccessState({
          profileId: profileAccessId,
          requests: readPhoneAccessRequests(profileAccessId),
        })
      }
    }

    window.addEventListener(PHONE_ACCESS_EVENT, refreshPhoneAccessRequests)
    window.addEventListener('storage', refreshFromOtherTab)
    return () => {
      window.removeEventListener(PHONE_ACCESS_EVENT, refreshPhoneAccessRequests)
      window.removeEventListener('storage', refreshFromOtherTab)
    }
  }, [profileAccessId])

  function getSecondaryCrafts() {
    const savedSecondaryCrafts = profileDetails.secondaryCrafts
      || (profileDetails.talents || []).filter((talent) => talent !== primaryCraft)
    return savedSecondaryCrafts.filter((talent) => talent !== primaryCraft)
  }
  const secondaryCrafts = getSecondaryCrafts()
  const subCraftOptions = [...(SUB_CRAFTS_BY_PRIMARY_CRAFT[primaryCraft] || [])]
    .sort((first, second) => first.localeCompare(second))

  useEffect(() => {
    if (!isExperienceModalOpen) return undefined

    function handleExperienceModalKeyDown(event) {
      if (event.key === 'Escape') setIsExperienceModalOpen(false)
    }

    document.addEventListener('keydown', handleExperienceModalKeyDown)
    return () => document.removeEventListener('keydown', handleExperienceModalKeyDown)
  }, [isExperienceModalOpen])

  const displayedProfileImage = isProfileImageRemoved
    ? ''
    : isOwnerView
      ? profilePreview || profileDetails.profileImageUrl || ''
      : profileDetails.profileImageUrl || profileDetails.avatar || ''
  const cropBaseScale = cropImageDimensions
    ? Math.max(
        cropStageSize / cropImageDimensions.width,
        cropStageSize / cropImageDimensions.height,
      )
    : 1
  const cropImageWidth = (cropImageDimensions?.width || 0) * cropBaseScale * cropZoom
  const cropImageHeight = (cropImageDimensions?.height || 0) * cropBaseScale * cropZoom
  const cropImageLeft = Math.min(
    cropFrame.x,
    Math.max(cropFrame.x + cropFrame.size - cropImageWidth, cropPosition.x),
  )
  const cropImageTop = Math.min(
    cropFrame.y,
    Math.max(cropFrame.y + cropFrame.size - cropImageHeight, cropPosition.y),
  )
  const profileCompletion = getProfileCompletion(profileDetails, Boolean(displayedProfileImage))
  const isProfileActionsOpen = openProfileActionsFor === targetProfileId
  const isProfileConnected = !isOwnerView && (connectionState.profileId === targetProfileId
    ? connectionState.connected
    : Boolean(profileDetails.isConnected))
  const isProfilePending = !isOwnerView && !isProfileConnected && (connectionState.profileId === targetProfileId
    ? connectionState.pending
    : Boolean(profileDetails.isPending || profileDetails.isOutgoingPending))
  const isProfileFollowed = !isOwnerView && (followState.profileId === targetProfileId
    ? followState.followed
    : Boolean(profileDetails.isFollowing))
  const relationshipAction = relationshipActionState.profileId === targetProfileId
    ? relationshipActionState.action
    : ''
  const relationshipError = relationshipErrorState.profileId === targetProfileId
    ? relationshipErrorState.message
    : ''
  const canMessageProfile = isProfileConnected || isProfileFollowed

  useEffect(() => {
    function syncConnectionState(event) {
      const { connected, pending, profile } = event.detail || {}
      const changedProfileId = String(
        profile?.userId || profile?.id || profile?.user_id || profile?._id || '',
      ).trim().toLowerCase()
      if (!changedProfileId) return
      const targetId = String(targetProfileId || '').trim().toLowerCase()
      if (changedProfileId === targetId) {
        setConnectionState({
          profileId: targetProfileId,
          connected: Boolean(connected),
          pending: typeof pending === 'boolean' ? Boolean(pending) : false,
        })
      }

      setConnectedProfiles((current) => {
        const currentProfiles = current || []
        const matchesProfile = (item) => String(
          item.userId || item.id || item.user_id || item._id || '',
        ).trim().toLowerCase() === changedProfileId
        if (!connected) return currentProfiles.filter((item) => !matchesProfile(item))
        if (currentProfiles.some(matchesProfile)) return currentProfiles
        return [...currentProfiles, { ...profile, id: changedProfileId, userId: changedProfileId }]
      })

      if (isOwnerView) {
        api.getConnectedProfiles().then((list) => {
          if (Array.isArray(list)) setConnectedProfiles(list)
        }).catch(() => {})
      }
    }

    window.addEventListener('truehunt-connection-changed', syncConnectionState)
    return () => window.removeEventListener('truehunt-connection-changed', syncConnectionState)
  }, [isOwnerView, targetProfileId])

  useEffect(() => {
    function syncFollowState(event) {
      const { following, profile } = event.detail || {}
      const changedProfileId = String(
        profile?.userId || profile?.id || profile?.user_id || profile?._id || '',
      )
      if (changedProfileId === targetProfileId) {
        setFollowState({ profileId: targetProfileId, followed: Boolean(following) })
      }
    }

    window.addEventListener('truehunt-follow-changed', syncFollowState)
    return () => window.removeEventListener('truehunt-follow-changed', syncFollowState)
  }, [targetProfileId])

  useEffect(() => {
    const nextProfileId = String(profileDetails.userId || profileDetails.id || '')
    if (nextProfileId) {
      setConnectionState({
        profileId: nextProfileId,
        connected: Boolean(profileDetails.isConnected),
        pending: Boolean(profileDetails.isPending || profileDetails.isOutgoingPending),
      })
      setFollowState({
        profileId: nextProfileId,
        followed: Boolean(profileDetails.isFollowing),
      })
    }
  }, [profileDetails.userId, profileDetails.id, profileDetails.isConnected, profileDetails.isFollowing, profileDetails.isPending, profileDetails.isOutgoingPending])

  useEffect(() => {
    if (isOwnerView || !targetProfileId || !isActive) return undefined

    let isCurrent = true
    async function loadCandidateProfileStatus() {
      try {
        const freshProfile = await api.getProfileById(targetProfileId)
        if (isCurrent && freshProfile) {
          const isConn = Boolean(freshProfile.isConnected)
          const isPend = Boolean(freshProfile.isPending || freshProfile.isOutgoingPending)
          const isFoll = Boolean(freshProfile.isFollowing)
          setConnectionState({
            profileId: targetProfileId,
            connected: isConn,
            pending: isPend,
          })
          setFollowState({
            profileId: targetProfileId,
            followed: isFoll,
          })
        }
      } catch (err) {
        console.warn('Could not refresh candidate profile relationship:', err)
      }
    }

    loadCandidateProfileStatus()
    return () => {
      isCurrent = false
    }
  }, [isOwnerView, targetProfileId, isActive])

  useEffect(() => {
    if (!isOwnerView) return undefined

    let isCurrent = true
    async function loadConnectionsCount() {
      try {
        const profiles = await api.getConnectedProfiles()
        if (isCurrent && Array.isArray(profiles)) {
          setConnectedProfiles(profiles)
        }
      } catch (error) {
        console.error('Failed to load profile connections count:', error)
      }
    }

    if (isActive) {
      loadConnectionsCount()
      api.getProfile().then((fresh) => {
        if (isCurrent && fresh && fresh.name) {
          setProfileDetails?.((prev) => ({ ...prev, ...fresh }))
        }
      }).catch(() => {})
    }

    return () => {
      isCurrent = false
    }
  }, [isOwnerView, isActive])

  useEffect(() => {
    if (!isActive || !isOwnerView) return undefined

    let isCurrent = true
    async function loadProfilePosts() {
      try {
        const posts = await api.getPosts()
        if (!isCurrent || !Array.isArray(posts)) return

        const ownerId = profilePostOwnerId.toLowerCase()
        const ownPosts = posts.filter((post) => {
          const authorId = String(post.authorId || '').trim().toLowerCase()
          return ownerId && authorId === ownerId
        })
        const cachedPosts = readProfilePosts(profilePostOwnerId, [])
        const loadedIds = new Set(ownPosts.map((post) => String(post.id)))
        const savedPosts = [
          ...ownPosts,
          ...cachedPosts.filter((post) => post.localOnly && !loadedIds.has(String(post.id))),
        ]
        setProfilePostsState({ profileId: profilePostOwnerId, posts: savedPosts })
        try {
          localStorage.setItem(getProfilePostsStorageKey(profilePostOwnerId), JSON.stringify(savedPosts))
        } catch (error) {
          console.warn('Could not save profile posts:', error)
        }
      } catch (error) {
        console.warn('Could not load profile posts:', error)
      }
    }

    loadProfilePosts()
    return () => {
      isCurrent = false
    }
  }, [isActive, isOwnerView, profilePostOwnerId])

  useEffect(() => {
    function handleSocialPostDeleted(event) {
      const { postId, profileId } = event.detail || {}
      if (
        !postId
        || String(profileId || '').trim().toLowerCase() !== profilePostOwnerId.toLowerCase()
      ) {
        return
      }

      const deletedPostId = String(postId)
      setProfilePostsState((current) => (
        current.profileId === profilePostOwnerId
          ? {
              ...current,
              posts: current.posts.filter((post) => String(post.id) !== deletedPostId),
            }
          : current
      ))
      setProfileDetails((details) => ({
        ...details,
        posts: (details.posts || []).filter((post) => String(post.id) !== deletedPostId),
      }))
    }

    window.addEventListener('truehunt-social-post-deleted', handleSocialPostDeleted)
    return () => window.removeEventListener('truehunt-social-post-deleted', handleSocialPostDeleted)
  }, [profilePostOwnerId, setProfileDetails])

  function startEditingProfilePost(post) {
    if (String(post.type || '').toLowerCase() === 'opportunity') {
      cancelEditingProfilePost()
      setEditingOpportunityPost(post)
      return
    }

    if (profilePostMediaPreviewRef.current) {
      URL.revokeObjectURL(profilePostMediaPreviewRef.current)
      profilePostMediaPreviewRef.current = ''
    }
    setProfilePostMediaDraft(null)
    setProfilePostError('')
    setEditingProfilePostId(String(post.id))
    setProfilePostTitleDraft(post.title || '')
    setProfilePostContentDraft(post.content || '')
  }

  function cancelEditingProfilePost() {
    if (profilePostMediaPreviewRef.current) {
      URL.revokeObjectURL(profilePostMediaPreviewRef.current)
      profilePostMediaPreviewRef.current = ''
    }
    setProfilePostMediaDraft(null)
    setEditingProfilePostId('')
    setProfilePostTitleDraft('')
    setProfilePostContentDraft('')
  }

  function selectProfilePostMedia(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
      setProfilePostError('Choose an image or video file.')
      return
    }

    if (profilePostMediaPreviewRef.current) {
      URL.revokeObjectURL(profilePostMediaPreviewRef.current)
    }
    const url = URL.createObjectURL(file)
    profilePostMediaPreviewRef.current = url
    setProfilePostMediaDraft({
      file,
      url,
      kind: file.type.startsWith('video/') ? 'video' : 'image',
    })
    setProfilePostError('')
  }

  async function saveProfilePostEdit(event, post) {
    event.preventDefault()
    if (!isOwnerView || isSavingProfilePost) return
    setProfilePostError('')
    setIsSavingProfilePost(true)
    try {
      let media
      if (profilePostMediaDraft?.file) {
        const uploadedMedia = await api.uploadMedia(profilePostMediaDraft.file, 'post')
        media = {
          url: uploadedMedia.url,
          fileName: uploadedMedia.fileName,
          contentType: uploadedMedia.contentType,
          kind: uploadedMedia.kind || profilePostMediaDraft.kind,
        }
      }

      const updates = {
        title: profilePostTitleDraft.trim(),
        content: profilePostContentDraft.trim(),
        ...(media ? { media } : {}),
      }
      const response = await api.updatePost(post.id, updates)
      const updatedPost = { ...post, ...response, ...updates }
      const updatedPosts = profilePosts.map((item) => (
        String(item.id) === String(post.id) ? updatedPost : item
      ))
      setProfilePostsState({ profileId: profilePostOwnerId, posts: updatedPosts })
      setProfileDetails((details) => ({
        ...details,
        posts: (details.posts || []).map((item) => (
          String(item.id) === String(post.id) ? updatedPost : item
        )),
      }))
      window.dispatchEvent(new CustomEvent('truehunt-social-post-updated', {
        detail: { post: updatedPost },
      }))
      try {
        localStorage.setItem(getProfilePostsStorageKey(profilePostOwnerId), JSON.stringify(updatedPosts))
      } catch (storageError) {
        console.warn('Could not cache the updated profile posts:', storageError)
      }
      cancelEditingProfilePost()
    } catch (error) {
      console.error('Could not save profile post changes:', error)
      setProfilePostError(error instanceof Error
        ? error.message
        : 'Could not save this post. Please try again.')
    } finally {
      setIsSavingProfilePost(false)
    }
  }

  async function saveProfileOpportunityEdit(form) {
    if (!editingOpportunityPost) return

    const content = [
      form.projectType,
      form.actingRole,
      form.locationPreference,
    ].filter(Boolean).join(' · ')
    const changes = {
      title: form.projectName,
      content,
      opportunityDetails: form,
    }
    const response = await api.updateOpportunity(editingOpportunityPost.id, changes)
    const updatedPost = {
      ...editingOpportunityPost,
      ...response,
      ...changes,
      type: 'opportunity',
    }
    const updatedPosts = profilePosts.map((post) => (
      String(post.id) === String(updatedPost.id) ? updatedPost : post
    ))
    setProfilePostsState({ profileId: profilePostOwnerId, posts: updatedPosts })
    setProfileDetails((details) => ({
      ...details,
      posts: (details.posts || []).map((post) => (
        String(post.id) === String(updatedPost.id) ? updatedPost : post
      )),
    }))
    window.dispatchEvent(new CustomEvent('truehunt-social-post-updated', {
      detail: { post: updatedPost },
    }))
    try {
      localStorage.setItem(getProfilePostsStorageKey(profilePostOwnerId), JSON.stringify(updatedPosts))
    } catch (error) {
      console.warn('Could not cache the updated profile posts:', error)
    }
    setEditingOpportunityPost(null)
  }

  async function deleteProfilePost(postId) {
    if (!isOwnerView) return

    setProfilePostError('')
    try {
      const postToDelete = profilePosts.find((post) => String(post.id) === String(postId))
      if (!postToDelete?.localOnly) {
        const serverPosts = await api.getPosts()
        const existsOnServer = serverPosts.some(
          (post) => String(post.id) === String(postId),
        )
        if (existsOnServer) {
          await api.deletePost(postId)
        }
      }

      const remainingPosts = profilePosts.filter((post) => String(post.id) !== String(postId))
      setProfilePostsState({ profileId: profilePostOwnerId, posts: remainingPosts })
      setProfileDetails((details) => ({
        ...details,
        posts: (details.posts || []).filter((post) => String(post.id) !== String(postId)),
      }))
      if (editingProfilePostId === String(postId)) cancelEditingProfilePost()
      try {
        localStorage.setItem(getProfilePostsStorageKey(profilePostOwnerId), JSON.stringify(remainingPosts))
      } catch (storageError) {
        console.warn('Could not update saved profile posts after deletion:', storageError)
      }
    } catch (error) {
      console.error('Could not delete profile post:', error)
      setProfilePostError('Could not delete this post. Please try again.')
    }
  }

  async function toggleProfileConnection() {
    const effectiveProfileId = String(
      targetProfileId || profileDetails.userId || profileDetails.id || profileDetails.referId || profileDetails.name || ''
    ).trim()
    if (!effectiveProfileId || relationshipAction) return

    setRelationshipActionState({ profileId: effectiveProfileId, action: 'connect' })
    setRelationshipErrorState({ profileId: effectiveProfileId, message: '' })
    try {
      const result = await api.toggleConnection(effectiveProfileId)
      const connected = Boolean(result?.connected)
      const pending = Boolean(result?.isPending)
      const profile = {
        ...profileDetails,
        id: effectiveProfileId,
        userId: effectiveProfileId,
      }
      setConnectionState({ profileId: effectiveProfileId, connected, pending })
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected, pending, profile },
      }))
    } catch (error) {
      console.error('Could not update profile connection:', error)
      setRelationshipErrorState({
        profileId: effectiveProfileId,
        message: isProfileConnected
          ? 'Could not remove this connection. Please try again.'
          : 'Could not send the connection request. Please try again.',
      })
    } finally {
      setRelationshipActionState({ profileId: effectiveProfileId, action: '' })
    }
  }

  async function followProfile() {
    const effectiveProfileId = String(
      targetProfileId || profileDetails.userId || profileDetails.id || profileDetails.referId || profileDetails.name || ''
    ).trim()
    if (!effectiveProfileId || relationshipAction) return

    setRelationshipActionState({ profileId: effectiveProfileId, action: 'follow' })
    setRelationshipErrorState({ profileId: effectiveProfileId, message: '' })
    try {
      const result = await api.toggleFollow(effectiveProfileId)
      const followed = typeof result?.following === 'boolean'
        ? result.following
        : !isProfileFollowed
      setFollowState({ profileId: effectiveProfileId, followed })
      setOpenProfileActionsFor('')
      window.dispatchEvent(new CustomEvent('truehunt-follow-changed', {
        detail: {
          following: followed,
          profile: { ...profileDetails, id: effectiveProfileId, userId: effectiveProfileId },
        },
      }))
    } catch (error) {
      console.error('Could not update profile follow state:', error)
      setRelationshipErrorState({
        profileId: effectiveProfileId,
        message: isProfileFollowed
          ? 'Could not unfollow this profile. Please try again.'
          : 'Could not follow this profile. Please try again.',
      })
    } finally {
      setRelationshipActionState({ profileId: effectiveProfileId, action: '' })
    }
  }

  useEffect(() => {
    if (profileDetails.profileImageUrl && !profilePreview && !isProfileImageRemoved) {
      setProfilePreview(profileDetails.profileImageUrl)
    }
  }, [profileDetails.profileImageUrl, profilePreview, isProfileImageRemoved, setProfilePreview])

  useEffect(() => {
    const stage = cropStageRef.current
    if (!pendingProfileCrop || !stage) return undefined

    const updateStageSize = () => {
      setCropStageSize(stage.getBoundingClientRect().width)
    }
    updateStageSize()
    const observer = new ResizeObserver(updateStageSize)
    observer.observe(stage)
    return () => observer.disconnect()
  }, [pendingProfileCrop])

  useEffect(() => {
    return () => {
      if (profilePreview && profilePreview.startsWith('blob:')) URL.revokeObjectURL(profilePreview)
    }
  }, [profilePreview])

  useEffect(() => {
    const videoUrls = uploadedVideoUrls.current
    return () => {
      videoUrls.forEach((url) => {
        if (url) URL.revokeObjectURL(url)
      })
    }
  }, [])

  function handleProfileChange(event) {
    const file = event.target.files?.[0] ?? null
    event.target.value = ''
    if (!file) return

    setValidationError('')
    if (pendingProfileCrop?.url) URL.revokeObjectURL(pendingProfileCrop.url)
    setCropImageDimensions(null)
    setCropFrame({ x: 0, y: 0, size: cropStageSize })
    setCropZoom(1)
    setPendingProfileCrop({ file, url: URL.createObjectURL(file) })
  }

  function cancelProfileCrop() {
    if (pendingProfileCrop?.url) URL.revokeObjectURL(pendingProfileCrop.url)
    setPendingProfileCrop(null)
    setCropImageDimensions(null)
    setCropPosition({ x: 0, y: 0 })
    setCropFrame({ x: 0, y: 0, size: cropStageSize })
  }

  async function applyProfileCrop() {
    const image = cropImageRef.current
    if (!pendingProfileCrop || !image || !cropImageDimensions) return

    const scale = cropBaseScale * cropZoom
    const sourceSize = cropFrame.size / scale
    const sourceX = (cropFrame.x - cropImageLeft) / scale
    const sourceY = (cropFrame.y - cropImageTop) / scale
    const canvas = document.createElement('canvas')
    canvas.width = PROFILE_IMAGE_OUTPUT_SIZE
    canvas.height = PROFILE_IMAGE_OUTPUT_SIZE
    const context = canvas.getContext('2d')
    if (!context) {
      setValidationError('Could not crop the image. Please try another photo.')
      return
    }

    context.drawImage(
      image,
      sourceX,
      sourceY,
      sourceSize,
      sourceSize,
      0,
      0,
      PROFILE_IMAGE_OUTPUT_SIZE,
      PROFILE_IMAGE_OUTPUT_SIZE,
    )

    try {
      const croppedBlob = await new Promise((resolve, reject) => {
        canvas.toBlob(
          (blob) => (blob ? resolve(blob) : reject(new Error('Could not create cropped image'))),
          'image/jpeg',
          0.92,
        )
      })
      const croppedName = pendingProfileCrop.file.name.replace(/\.[^.]+$/, '') || 'profile-photo'
      const croppedFile = new File([croppedBlob], `${croppedName}-cropped.jpg`, { type: 'image/jpeg' })
      setProfilePic(croppedFile)
      setIsProfileImageRemoved(false)
      setProfilePreview(URL.createObjectURL(croppedFile))
      cancelProfileCrop()
    } catch (error) {
      console.error('Could not crop profile image:', error)
      setValidationError('Could not crop the image. Please try again.')
    }
  }

  function handleCropPointerDown(event) {
    if (!cropImageDimensions) return
    event.currentTarget.setPointerCapture(event.pointerId)
    const handle = event.target.closest('[data-crop-handle]')?.dataset.cropHandle || ''
    cropInteractionRef.current = {
      mode: handle ? 'resize' : (event.target.closest('.profile-image-crop-selection') ? 'move-frame' : 'move-image'),
      handle,
      pointerX: event.clientX,
      pointerY: event.clientY,
      imageLeft: cropImageLeft,
      imageTop: cropImageTop,
      frame: cropFrame,
    }
  }

  function handleCropPointerMove(event) {
    const interaction = cropInteractionRef.current
    if (!interaction) return
    const rect = cropStageRef.current?.getBoundingClientRect()
    if (!rect) return
    const scale = cropStageSize / rect.width
    const dx = (event.clientX - interaction.pointerX) * scale
    const dy = (event.clientY - interaction.pointerY) * scale

    if (interaction.mode === 'move-image') {
      setCropPosition({
        x: interaction.imageLeft + dx,
        y: interaction.imageTop + dy,
      })
      return
    }

    if (interaction.mode === 'move-frame') {
      const { frame } = interaction
      const minX = Math.max(0, interaction.imageLeft)
      const minY = Math.max(0, interaction.imageTop)
      const maxX = Math.min(cropStageSize - frame.size, interaction.imageLeft + cropImageWidth - frame.size)
      const maxY = Math.min(cropStageSize - frame.size, interaction.imageTop + cropImageHeight - frame.size)
      setCropPosition({ x: interaction.imageLeft, y: interaction.imageTop })
      setCropFrame({
        ...frame,
        x: Math.min(maxX, Math.max(minX, frame.x + dx)),
        y: Math.min(maxY, Math.max(minY, frame.y + dy)),
      })
      return
    }

    const { frame, handle } = interaction
    const left = handle.includes('left')
    const right = handle.includes('right')
    const top = handle.includes('top')
    const bottom = handle.includes('bottom')
    const horizontalDelta = left ? -dx : right ? dx : 0
    const verticalDelta = top ? -dy : bottom ? dy : 0
    const sizeDelta = (horizontalDelta + verticalDelta) / (horizontalDelta && verticalDelta ? 2 : 1)
    const maxSize = Math.min(
      left ? frame.x + frame.size : cropStageSize - frame.x,
      top ? frame.y + frame.size : cropStageSize - frame.y,
    )
    const size = Math.min(maxSize, Math.max(cropStageSize * 0.25, frame.size + sizeDelta))
    setCropFrame({
      x: left ? frame.x + frame.size - size : frame.x,
      y: top ? frame.y + frame.size - size : frame.y,
      size,
    })
  }

  function handleCropPointerUp() {
    cropInteractionRef.current = null
  }

  function handleCropImageLoad(event) {
    const { naturalWidth, naturalHeight } = event.currentTarget
    const stageSize = cropStageRef.current?.getBoundingClientRect().width || PROFILE_IMAGE_CROP_SIZE
    const frameSize = stageSize * 0.84
    const frameOffset = (stageSize - frameSize) / 2
    const baseScale = Math.max(
      stageSize / naturalWidth,
      stageSize / naturalHeight,
    )
    setCropStageSize(stageSize)
    setCropFrame({ x: frameOffset, y: frameOffset, size: frameSize })
    setCropImageDimensions({ width: naturalWidth, height: naturalHeight })
    setCropZoom(1)
    setCropPosition({
      x: (stageSize - naturalWidth * baseScale) / 2,
      y: (stageSize - naturalHeight * baseScale) / 2,
    })
  }

  function removeProfileImage(event) {
    event.preventDefault()
    event.stopPropagation()
    setProfilePic(null)
    setProfilePreview('')
    setIsProfileImageRemoved(true)
    if (profilePictureInput.current) profilePictureInput.current.value = ''
  }

  function closeProfileEditor() {
    cancelProfileCrop()
    if (profilePreview?.startsWith('blob:')) URL.revokeObjectURL(profilePreview)
    setProfilePic(null)
    setProfilePreview(profileDetails.profileImageUrl || '')
    setIsProfileImageRemoved(false)
    setIsEditingProfile(false)
  }

  function handleVideoChange(event, index) {
    const file = event.target.files?.[0] ?? null
    const previousUrl = uploadedVideoUrls.current[index]

    if (previousUrl) URL.revokeObjectURL(previousUrl)

    uploadedVideoUrls.current[index] = ''
    setUploadedVideos((videos) => {
      const nextVideos = [...videos]
      nextVideos[index] = null
      return nextVideos
    })
    setValidationError('')
    setIsSubmitted(false)

    if (!file) return

    if (file.size > MAX_VIDEO_SIZE) {
      event.target.value = ''
      setValidationError('Each video must be no larger than 5 MB. Please choose a smaller video file.')
      return
    }

    const url = URL.createObjectURL(file)
    uploadedVideoUrls.current[index] = url
    setUploadedVideos((videos) => {
      const nextVideos = [...videos]
      nextVideos[index] = { file, url }
      return nextVideos
    })
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      if (uploadedVideoUrls.current[index] !== url || event.target.files?.[0] !== file) {
        URL.revokeObjectURL(url)
        return
      }

      if (!Number.isFinite(video.duration) || video.duration > MAX_VIDEO_DURATION) {
        URL.revokeObjectURL(url)
        uploadedVideoUrls.current[index] = ''
        event.target.value = ''
        setUploadedVideos((videos) => {
          const nextVideos = [...videos]
          nextVideos[index] = null
          return nextVideos
        })
        setValidationError('Each video must be no longer than 3 minutes. Please choose a shorter video.')
        return
      }

      setUploadedVideos((videos) => {
        const nextVideos = [...videos]
        nextVideos[index] = { file, url, duration: video.duration }
        return nextVideos
      })
    }
    video.onerror = () => {
      if (uploadedVideoUrls.current[index] !== url || event.target.files?.[0] !== file) {
        URL.revokeObjectURL(url)
        return
      }

      URL.revokeObjectURL(url)
      uploadedVideoUrls.current[index] = ''
      event.target.value = ''
      setUploadedVideos((videos) => {
        const nextVideos = [...videos]
        nextVideos[index] = null
        return nextVideos
      })
      setValidationError('This video could not be read. Please choose a valid video file.')
    }
    video.src = url
  }

  function handleAddVideoUrl() {
    if (savedVideoUrls.length >= 3) return
    let parsedUrl

    try {
      parsedUrl = new URL(videoUrlInput.trim())
    } catch {
      setVideoUrlError('Enter a valid video URL.')
      return
    }

    if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
      setVideoUrlError('Use a link starting with http:// or https://.')
      return
    }

    if (savedVideoUrls.includes(parsedUrl.href)) {
      setVideoUrlError('This video URL has already been added.')
      return
    }

    setSavedVideoUrls((urls) => [...urls, parsedUrl.href])
    setVideoUrlInput('')
    setVideoUrlError('')
  }

  function handleRemoveVideoUrl(index) {
    setSavedVideoUrls((urls) => urls.filter((_, urlIndex) => urlIndex !== index))
  }

  function openSectionEditor(sectionId) {
    if (editingSection === sectionId) {
      closeSectionEditor()
      return
    }

    uploadedVideoUrls.current.forEach((url) => {
      if (url) URL.revokeObjectURL(url)
    })
    uploadedVideoUrls.current = []
    setSelectedLanguages(profileDetails.languages)
    setOtherLanguageDraft(profileDetails.otherLanguage)
    setSelectedTalents(getSecondaryCrafts())
    setSelectedSubCraft(
      subCraftOptions.includes(profileDetails.subCraft) ? profileDetails.subCraft : '',
    )
    setSecondaryCraftToAdd('')
    const savedWorkTypes = profileDetails.workTypes || []
    const savedCustomWorkType = savedWorkTypes.find((workType) => workType.startsWith('Other: '))
    setSelectedWorkTypes(savedWorkTypes.filter((workType) => !workType.startsWith('Other: ')))
    setOtherWorkTypeDraft(savedCustomWorkType?.slice('Other: '.length) || '')
    setSavedVideoUrls(profileDetails.videoUrls)
    setVideoUrlInput('')
    setVideoUrlError('')
    setValidationError('')
    setAboutDraft(profileDetails.bio || '')
    setUploadedVideos([null, null, null])
    setEditingSection(sectionId)
  }

  function closeSectionEditor() {
    uploadedVideoUrls.current.forEach((url) => {
      if (url) URL.revokeObjectURL(url)
    })
    uploadedVideoUrls.current = []
    setUploadedVideos([null, null, null])
    setEditingSection('')
    setValidationError('')
    setSecondaryCraftToAdd('')
  }

  async function persistProfileDetails(nextDetails) {
    const savedDetails = await api.updateProfile(nextDetails)
    const persistedDetails = {
      ...nextDetails,
      ...savedDetails,
      posts: nextDetails.posts,
    }
    if (persistedDetails.userId && persistedDetails.userId !== authUser?.userId) {
      updateUser({
        userId: persistedDetails.userId,
        ...(authUser?.id ? { id: persistedDetails.userId } : {}),
      })
    }
    setProfileDetails(persistedDetails)
    return persistedDetails
  }

  async function saveSectionEditor() {
    let nextDetails = { ...profileDetails }

    if (editingSection === 'about') {
      const aboutWords = aboutDraft.trim().match(/\S+/g) || []
      if (aboutWords.length > MAX_ABOUT_WORDS) {
        setValidationError(`About can contain up to ${MAX_ABOUT_WORDS} words.`)
        return
      }

      nextDetails.bio = aboutDraft.trim()
      setIsSavingAbout(true)
      setValidationError('')
      try {
        await persistProfileDetails(nextDetails)
        setEditingSection('')
      } catch (error) {
        console.error('Failed to save profile About section:', error)
        setValidationError('Could not save your About section. Please try again.')
      } finally {
        setIsSavingAbout(false)
      }
      return
    } else if (editingSection === 'talents') {
      nextDetails.primaryCraft = primaryCraft
      nextDetails.subCraft = selectedSubCraft
      nextDetails.secondaryCrafts = selectedTalents
      nextDetails.talents = [...(primaryCraft ? [primaryCraft] : []), ...selectedTalents]
    } else if (editingSection === 'workTypes') {
      const customWorkType = otherWorkTypeDraft.trim()
      nextDetails.workTypes = [
        ...selectedWorkTypes,
        ...(selectedWorkTypes.includes('others') && customWorkType
          ? [`Other: ${customWorkType}`]
          : []),
      ]
    } else if (editingSection === 'languages') {
      nextDetails.languages = selectedLanguages
      nextDetails.otherLanguage = selectedLanguages.includes('Other') ? otherLanguageDraft.trim() : ''
    } else if (editingSection === 'videoLinks') {
      nextDetails.videoUrls = savedVideoUrls
    } else if (editingSection === 'videos') {
      const pendingVideos = uploadedVideos.filter(Boolean)
      const invalidVideo = pendingVideos.find((video) => (
        video.file.size > MAX_VIDEO_SIZE
        || video.duration === undefined
        || video.duration > MAX_VIDEO_DURATION
      ))

      if (invalidVideo) {
        setValidationError(
          invalidVideo.duration === undefined
            ? 'Please wait for the selected video to finish processing.'
            : 'Each video must be no larger than 5 MB and no longer than 3 minutes.',
        )
        return
      }

      // Upload newly added videos to the backend media endpoint
      const uploadedList = []
      for (const item of pendingVideos) {
        try {
          if (item.file) {
            const uploaded = await api.uploadMedia(item.file, 'demo-video')
            uploadedList.push({
              url: uploaded.url,
              fileName: uploaded.fileName,
              duration: item.duration || 0,
              sizeBytes: uploaded.sizeBytes,
              contentType: uploaded.contentType,
            })
          } else {
            uploadedList.push(item)
          }
        } catch (err) {
          console.warn('Video upload error:', err)
          uploadedList.push(item)
        }
      }

      nextDetails.videos = [...(nextDetails.videos || []), ...uploadedList].slice(0, 3)
      uploadedVideoUrls.current = []
    }

    try {
      await persistProfileDetails(nextDetails)
      setEditingSection('')
      setValidationError('')
    } catch (error) {
      console.error('Failed to save profile section to backend:', error)
      setValidationError('Could not save your selection. Please try again.')
    }
  }

  function handleEditProfile() {
    if (!isOwnerView) return
    setEditingSection('')
    setSelectedLanguages(profileDetails.languages)
    setOtherLanguageDraft(profileDetails.otherLanguage)
    setSelectedTalents(getSecondaryCrafts())
    setSecondaryCraftToAdd('')
    setSavedVideoUrls(profileDetails.videoUrls)
    setVideoUrlInput('')
    setVideoUrlError('')
    setDateOfBirthDraft(profileDetails.dateOfBirth || '')
    setGenderDraft(profileDetails.gender || '')
    setProfilePreview(profileDetails.profileImageUrl || profilePreview)
    setProfileFormKey((key) => key + 1)
    setIsEditingProfile(true)
  }

  function requestProfessionalPhone() {
    if (!currentUserId) {
      setPhoneRequestMessage('Could not identify your request. Please try again.')
      return
    }
    if (currentPhoneRequest?.status === 'approved') {
      setPhoneRequestMessage('You already have permission to view this number.')
      return
    }
    if (currentPhoneRequest?.status === 'pending') {
      setPhoneRequestMessage('Your request is waiting for approval.')
      return
    }

    const nextRequests = [
      ...phoneAccessRequests.filter((request) => request.requesterId !== currentUserId),
      {
        id: `${currentUserId}-${Date.now()}`,
        requesterId: currentUserId,
        requesterName: currentUser?.name || currentUser?.email || 'Social member',
        status: 'pending',
        requestedAt: new Date().toISOString(),
      },
    ]

    try {
      savePhoneAccessRequests(profileAccessId, nextRequests)
      setPhoneAccessState({ profileId: profileAccessId, requests: nextRequests })
      setPhoneRequestMessage('Request sent. The profile owner must approve it before the number is shown.')
    } catch (error) {
      console.error('Could not save professional phone access request:', error)
      setPhoneRequestMessage('Could not send your request. Please try again.')
    }
  }

  function decidePhoneAccessRequest(requestId, status) {
    const nextRequests = phoneAccessRequests.map((request) => (
      request.id === requestId ? { ...request, status } : request
    ))

    try {
      savePhoneAccessRequests(profileAccessId, nextRequests)
      setPhoneAccessState({ profileId: profileAccessId, requests: nextRequests })
    } catch (error) {
      console.error('Could not update professional phone access request:', error)
      setPhoneRequestMessage('Could not update the request. Please try again.')
    }
  }

  function openExperienceModal(experience = null) {
    setEditingExperienceId(experience?.id ?? null)
    setExperienceAttachmentType(experience?.attachment?.type || '')
    setExperienceSaveError('')
    setOrganizationDraft(experience?.organizationName || '')
    setIsExperienceModalOpen(true)
  }

  async function handleExperienceSubmit(event) {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)
    const startDate = formData.get('startDate')
    const endDate = formData.get('endDate')

    if (endDate < startDate) {
      const endDateInput = event.currentTarget.elements.namedItem('endDate')
      endDateInput.setCustomValidity('End date must be on or after the start date.')
      endDateInput.reportValidity()
      return
    }

    const attachmentFile = formData.get('attachmentFile')
    const existingAttachment = experienceBeingEdited?.attachment || null
    if (
      experienceAttachmentType
      && experienceAttachmentType !== existingAttachment?.type
      && (!(attachmentFile instanceof File) || attachmentFile.size === 0)
    ) {
      setExperienceSaveError(`Choose a ${experienceAttachmentType} file to upload.`)
      return
    }

    setIsSavingExperience(true)
    setExperienceSaveError('')
    try {
      let attachment = experienceAttachmentType
        ? existingAttachment
        : null
      if (attachmentFile instanceof File && attachmentFile.size > 0) {
        const uploaded = await api.uploadMedia(attachmentFile, `experience-${experienceAttachmentType}`)
        attachment = {
          type: experienceAttachmentType,
          url: uploaded.url,
          fileName: uploaded.fileName,
          contentType: uploaded.contentType,
        }
      }

      const organizationName = formData.get('organizationName')?.toString().trim() || ''
      const existingOrganizationLogo = getOrganizationLogoUrl(organizationName, profileDetails.experiences)
        || (organizationName.toLowerCase() === experienceBeingEdited?.organizationName?.toLowerCase()
          ? experienceBeingEdited?.organizationLogoUrl || experienceBeingEdited?.companyLogoUrl || ''
          : '')
      const experience = {
        id: editingExperienceId ?? `${Date.now()}`,
        projectName: formData.get('projectName')?.toString().trim() || '',
        organizationName,
        organizationLogoUrl: existingOrganizationLogo,
        companyLogoUrl: existingOrganizationLogo,
        featuredOnProfile: formData.get('featuredOnProfile') === 'on',
        location: formData.get('location')?.toString().trim() || '',
        workedAs: formData.get('workedAs')?.toString().trim() || '',
        startDate,
        endDate,
        highlights: formData.get('highlights')?.toString().trim() || '',
        attachment,
      }

      let updatedExperiences = editingExperienceId
        ? (profileDetails.experiences || []).map((item) => (
          item.id === editingExperienceId ? experience : item
        ))
        : [...(profileDetails.experiences || []), experience]
      if (experience.featuredOnProfile) {
        updatedExperiences = updatedExperiences.map((item) => ({
          ...item,
          featuredOnProfile: item.id === experience.id,
        }))
      } else if (experienceBeingEdited?.featuredOnProfile) {
        updatedExperiences = updatedExperiences.map((item) => ({
          ...item,
          featuredOnProfile: false,
        }))
      }
      const nextDetails = { ...profileDetails, experiences: updatedExperiences }
      await persistProfileDetails(nextDetails)
      setIsExperienceModalOpen(false)
      setEditingExperienceId(null)
    } catch (err) {
      console.error('Failed to save experience:', err)
      setExperienceSaveError('Could not save this experience. Please try again.')
    } finally {
      setIsSavingExperience(false)
    }
  }

  async function handleExperienceDelete() {
    if (editingExperienceId === null) return

    setIsSavingExperience(true)
    setExperienceSaveError('')
    const updatedExperiences = (profileDetails.experiences || []).filter(
      (experience) => experience.id !== editingExperienceId,
    )

    const nextDetails = { ...profileDetails, experiences: updatedExperiences }
    try {
      await persistProfileDetails(nextDetails)
      setIsExperienceModalOpen(false)
      setEditingExperienceId(null)
    } catch (err) {
      console.error('Failed to delete experience from backend:', err)
      setExperienceSaveError('Could not delete this experience. Please try again.')
    } finally {
      setIsSavingExperience(false)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (isSubmittingProfile) return
    setValidationError('')
    setIsSubmitted(false)

    const form = event.currentTarget
    const addressField = form.elements.namedItem('address')
    if (addressField.value.trim()) {
      addressField.setCustomValidity('')
    } else {
      addressField.setCustomValidity('Enter your address.')
      addressField.reportValidity()
      return
    }

    const videoInputs = Array.from(form.querySelectorAll('input[type="file"][data-video]'))
    const invalidVideoIndex = uploadedVideos.findIndex((video) => video && (
      video.file.size > MAX_VIDEO_SIZE
      || video.duration === undefined
      || video.duration > MAX_VIDEO_DURATION
    ))

    if (invalidVideoIndex !== -1) {
      videoInputs[invalidVideoIndex].focus()
      setValidationError(
        uploadedVideos[invalidVideoIndex].duration === undefined
          ? 'Please wait for the selected video to finish processing.'
          : 'Each video must be no larger than 5 MB and no longer than 3 minutes.',
      )
      return
    }

    setIsSubmittingProfile(true)
    try {
      const formData = new FormData(form)
      let profileImageUrl = isProfileImageRemoved ? '' : profileDetails.profileImageUrl || ''

      if (profilePic) {
        const uploaded = await api.uploadMedia(profilePic, 'profile-image')
        profileImageUrl = uploaded.url
      }

      const nextDetails = {
        ...profileDetails,
        userId: formData.get('userId')?.toString().trim() || '',
        name: formData.get('name') || '',
        phone: formData.get('phone')?.trim() || '',
        professionalPhone: formData.get('professionalPhone')?.trim() || '',
        email: formData.get('email')?.trim() || '',
        experience: formData.get('experience') || '',
        dateOfBirth: formData.get('dateOfBirth') || '',
        height: formData.get('height') || '',
        weight: formData.get('weight') || '',
        bodyBuild: formData.get('bodyBuild') || '',
        gender: formData.get('gender') || '',
        facialHair: formData.get('facialHair') || '',
        skinTone: formData.get('skinTone') || '',
        address: formData.get('address') || '',
        languages: selectedLanguages,
        otherLanguage: selectedLanguages.includes('Other') ? otherLanguageDraft.trim() : '',
        profileImageUrl,
      }

      const savedDetails = await persistProfileDetails(nextDetails)
      if (profilePic) {
        const updatedDetails = {
          ...savedDetails,
          profileImageUrl,
          avatar: profileImageUrl,
        }
        setProfileDetails(updatedDetails)
        window.dispatchEvent(new CustomEvent('truehunt-profile-updated', {
          detail: { profile: updatedDetails },
        }))
      }
      setProfilePreview(profileImageUrl)
      setIsProfileImageRemoved(false)

      setIsEditingProfile(false)
    } catch (err) {
      console.error('Failed to save profile:', err)
      setValidationError(err.message || 'Could not save your profile. Please try again.')
    } finally {
      setIsSubmittingProfile(false)
    }
  }

  return (
    <div className="profile-page-content">
        <section className="artist-profile" id="artist-profile" aria-labelledby="preview-title">
          <div className="artist-cover has-default-banner">
            <video
              className="artist-cover-video"
              autoPlay
              muted
              loop
              playsInline
              preload="metadata"
              aria-hidden="true"
            >
              <source src="/assets/profile-banner.mp4" type="video/mp4" />
            </video>
            <div className="artist-cover-brand" aria-label="TrueHunt">
              <img className="artist-cover-emblem" src="/assets/profile-banner-emblem.png" alt="" aria-hidden="true" />
              <span className="artist-cover-wordmark">TrueHunt</span>
            </div>
          </div>
          <div className="artist-identity">
            <div className="profile-avatar-column">
              <div
                className={`artist-avatar-progress${isOwnerView ? '' : ' is-readonly'}`}
                style={{ '--profile-completion': `${profileCompletion}%` }}
                role={isOwnerView ? 'button' : undefined}
                aria-label={isOwnerView ? 'Edit profile photo' : undefined}
                onClick={isOwnerView ? handleEditProfile : undefined}
                onKeyDown={isOwnerView ? (event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    handleEditProfile()
                  }
                } : undefined}
                tabIndex={isOwnerView ? 0 : undefined}
                title={isOwnerView ? 'Click to edit your profile' : undefined}
              >
                {displayedProfileImage
                  ? <img className="artist-avatar" src={displayedProfileImage} alt={`${profileDetails.name || 'Artist'}'s profile`} />
                  : (
                    <div className="artist-avatar avatar-fallback" aria-hidden="true">
                      <svg className="profile-avatar-placeholder" viewBox="0 0 100 100" focusable="false">
                        <circle cx="50" cy="35" r="21" />
                        <path d="M10 96c2-23 18-39 40-39s38 16 40 39H10Z" />
                      </svg>
                    </div>
                  )}
                {isOwnerView && <span className="artist-avatar-progress-label">{profileCompletion}%</span>}
              </div>
              {isOwnerView && (
                <div className="profile-user-id">
                  <span className="profile-user-id-label">TrueHunt ID:</span>
                  <span className="profile-user-id-value">{isProfileUserIdVisible ? displayedUserId || 'Unavailable' : '••••••••'}</span>
                  <button
                    type="button"
                    aria-label={isProfileUserIdVisible ? 'Hide user ID' : 'Show user ID'}
                    aria-pressed={isProfileUserIdVisible}
                    onClick={() => setIsProfileUserIdVisible((visible) => !visible)}
                  >
                    {isProfileUserIdVisible
                      ? <EyeOff size={16} aria-hidden="true" />
                      : <Eye size={16} aria-hidden="true" />}
                  </button>
                </div>
              )}
            </div>
            <div className="artist-identity-content">
              <div className="artist-name-row">
                <h2 id="preview-title">{profileDetails.name || 'Your name'}</h2>
                {!isOwnerView && (
                  <div className="profile-relationship-menu" ref={profileActionsRef}>
                    <button
                      className="profile-actions-trigger"
                      type="button"
                      aria-label="More profile actions"
                      aria-haspopup="true"
                      aria-expanded={isProfileActionsOpen}
                      onClick={() => {
                        setRelationshipErrorState({ profileId: targetProfileId, message: '' })
                        setOpenProfileActionsFor(isProfileActionsOpen ? null : targetProfileId)
                      }}
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="5" cy="12" r="1.7" />
                        <circle cx="12" cy="12" r="1.7" />
                        <circle cx="19" cy="12" r="1.7" />
                      </svg>
                    </button>
                    {isProfileActionsOpen && (
                      <div className="profile-relationship-popover" role="group" aria-label="Profile actions">
                        <button
                          className={`profile-relationship-action${isProfileConnected ? ' is-active' : isProfilePending ? ' is-pending' : ''}`}
                          type="button"
                          aria-pressed={isProfileConnected}
                          disabled={relationshipAction !== '' || !targetProfileId}
                          onClick={toggleProfileConnection}
                        >
                          {isProfileConnected ? <Check size={17} /> : <UserPlus size={17} />}
                          <span>
                            {relationshipAction === 'connect'
                              ? isProfileConnected ? 'Removing connection…' : isProfilePending ? 'Canceling…' : 'Connecting…'
                              : isProfileConnected ? 'Remove connection' : isProfilePending ? 'Waiting for response' : 'Connect'}
                          </span>
                        </button>
                        <button
                          className={`profile-relationship-action${isProfileFollowed ? ' is-active' : ''}`}
                          type="button"
                          aria-pressed={isProfileFollowed}
                          disabled={relationshipAction !== '' || !targetProfileId}
                          onClick={followProfile}
                        >
                          {isProfileFollowed ? <Check size={17} /> : <UserPlus size={17} />}
                          <span>
                            {relationshipAction === 'follow'
                              ? isProfileFollowed ? 'Unfollowing…' : 'Following…'
                              : isProfileFollowed ? 'Unfollow' : 'Follow'}
                          </span>
                        </button>
                        {relationshipError && <p className="profile-relationship-error" role="alert">{relationshipError}</p>}
                      </div>
                    )}
                  </div>
                )}
                {profileOrganization && (
                  <div className="profile-organization-list" aria-label="Preferred organization">
                    <div className="profile-organization-item">
                      <span className="experience-organization-logo" aria-hidden="true">
                        <Building2 size={13} />
                        {profileOrganization.logoUrl && (
                          <img
                            src={profileOrganization.logoUrl}
                            alt=""
                            onError={(event) => {
                              event.currentTarget.hidden = true
                            }}
                          />
                        )}
                      </span>
                      <span>{profileOrganization.name}</span>
                    </div>
                  </div>
                )}
              </div>
              <p className="artist-headline">
                {profileDetails.talents.length > 0
                  ? profileDetails.talents.join('  |  ')
                  : 'Add your talents in Edit profile'}
              </p>
              <p className="artist-location">
                {profileDetails.address}
              </p>
              {profileDetails.experience && (
                <p className="artist-experience">
                  {profileDetails.experience.toLowerCase() === 'fresher'
                    ? profileDetails.experience
                    : profileDetails.experience.toLowerCase().includes('year')
                    ? profileDetails.experience
                    : `${profileDetails.experience} years of experience`}
                </p>
              )}
              {profileDetails.languages.length > 0 && (
                <p className="artist-languages">
                  Languages: {profileDetails.languages.map((language) => (
                    language === 'Other' && profileDetails.otherLanguage
                      ? `Other: ${profileDetails.otherLanguage}`
                      : language
                  )).join(', ')}
                </p>
              )}
              {(profileDetails.height || profileDetails.weight || profileDetails.skinTone || profileDetails.facialHair) && (
                <dl className="profile-appearance-details" aria-label="Appearance details">
                  {profileDetails.height && (
                    <div><dt>Height</dt><dd>{profileDetails.height} cm</dd></div>
                  )}
                  {profileDetails.weight && (
                    <div><dt>Weight</dt><dd>{profileDetails.weight} kg</dd></div>
                  )}
                  {profileDetails.skinTone && (
                    <div><dt>Skin tone</dt><dd>{profileDetails.skinTone}</dd></div>
                  )}
                  {profileDetails.facialHair && (
                    <div><dt>Facial hair</dt><dd>{profileDetails.facialHair}</dd></div>
                  )}
                </dl>
              )}
              {formattedConnectionCount !== null && (
                isOwnerView ? (
                  <button
                    className="profile-connections-link"
                    type="button"
                    onClick={onViewConnections}
                    aria-label={`View ${formattedConnectionCount} connections`}
                  >
                    {formattedConnectionCount} connections
                    <ChevronRight size={17} aria-hidden="true" />
                  </button>
                ) : (
                  <p className="profile-connections-count">
                    {formattedConnectionCount} connections
                  </p>
                )
              )}
            </div>
            <div className="identity-actions">
              <button
                className="identity-contact-button"
                type="button"
                onClick={() => {
                  setPhoneRequestMessage('')
                  setIsContactModalOpen(true)
                }}
              >
                Contact
              </button>
              {!isOwnerView && canMessageProfile && (
                <button
                  className="identity-contact-button"
                  type="button"
                  onClick={() => onMessage?.({
                    id: profileDetails.userId,
                    name: profileDetails.name || 'Cinema Professional',
                    avatar: displayedProfileImage,
                  })}
                >
                  Message
                </button>
              )}
              {isOwnerView && (
                <button
                  className="identity-edit-button"
                  type="button"
                  onClick={handleEditProfile}
                >
                  <span aria-hidden="true">✎</span> Edit profile
                </button>
              )}
            </div>
          </div>
          <div className="artist-content">
            <div className="artist-main">
              {(profileDetails.experience || profileDetails.languages.length > 0 || profileDetails.videos.length > 0) && (
                <section className="artist-section insights-section" id="artist-highlights">
                  <h3>Profile highlights</h3>
                  <div className="insights-grid">
                    {profileDetails.experience && (
                      <div className="insight-item">
                        <span className="insight-icon" aria-hidden="true">✦</span>
                        <div>
                          <strong>{profileDetails.experience.toLowerCase() === 'fresher' || profileDetails.experience.toLowerCase().includes('year') ? profileDetails.experience : `${profileDetails.experience} years`}</strong>
                          <span>Experience</span>
                        </div>
                      </div>
                    )}
                    {profileDetails.languages.length > 0 && (
                      <div className="insight-item">
                        <span className="insight-icon" aria-hidden="true">◎</span>
                        <div><strong>{profileDetails.languages.length}</strong><span>Languages</span></div>
                      </div>
                    )}
                    {profileDetails.videos.length > 0 && (
                      <div className="insight-item">
                        <span className="insight-icon" aria-hidden="true">▶</span>
                        <div><strong>{profileDetails.videos.length}</strong><span>Demo videos</span></div>
                      </div>
                    )}
                  </div>
                </section>
              )}

              <section className="artist-section profile-about-section" id="artist-about">
                <PreviewSectionHeading isOwner={isOwnerView} onEdit={openSectionEditor} sectionId="about">
                  About
                </PreviewSectionHeading>
                {profileDetails.bio?.trim() ? (
                  <p className="profile-about-content">{profileDetails.bio}</p>
                ) : (
                  <p className="profile-empty-hint">
                    {isOwnerView ? 'Add an introduction about yourself.' : 'No About information added yet.'}
                  </p>
                )}
                {editingSection === 'about' && (
                  <div className="preview-inline-editor profile-about-editor">
                    <label className="form-field" htmlFor="profile-about-text">
                      About you
                      <textarea
                        id="profile-about-text"
                        rows={5}
                        value={aboutDraft}
                        maxLength={5000}
                        aria-describedby="profile-about-word-count"
                        onChange={(event) => {
                          const words = event.target.value.trim().match(/\S+/g) || []
                          if (words.length > MAX_ABOUT_WORDS) {
                            setAboutDraft(words.slice(0, MAX_ABOUT_WORDS).join(' '))
                            setValidationError(`About is limited to ${MAX_ABOUT_WORDS} words.`)
                            return
                          }
                          setAboutDraft(event.target.value)
                          setValidationError('')
                        }}
                        placeholder="Write a short introduction about your experience, skills, and creative work."
                      />
                    </label>
                    <div className="profile-about-editor-footer">
                      <span id="profile-about-word-count" aria-live="polite">
                        {(aboutDraft.trim().match(/\S+/g) || []).length}/{MAX_ABOUT_WORDS} words
                      </span>
                      {validationError && <p className="submit-error" role="alert">{validationError}</p>}
                    </div>
                    <div className="language-edit-actions">
                      <button className="secondary-button" type="button" onClick={closeSectionEditor} disabled={isSavingAbout}>
                        Cancel
                      </button>
                      <button className="contact-button" type="button" onClick={saveSectionEditor} disabled={isSavingAbout}>
                        {isSavingAbout ? 'Saving…' : 'Save'}
                      </button>
                    </div>
                  </div>
                )}
              </section>

              <section className="artist-section" id="artist-posts">
                <div className="preview-section-heading">
                  <h3>Posts</h3>
                </div>
                {profilePostError && <p className="submit-error" role="alert">{profilePostError}</p>}
                {profilePosts.length > 0 ? (
                  <div className="profile-post-carousel">
                    <div className="profile-post-list" ref={profilePostsListRef}>
                      {profilePosts.map((post) => {
                        const media = getProfilePostMedia(post)
                        const displayedMedia = editingProfilePostId === String(post.id)
                          && profilePostMediaDraft
                          ? profilePostMediaDraft
                          : media
                        const postType = String(post.type || '').toLowerCase()
                        const postLabel = postType === 'media'
                          ? 'Media'
                          : postType
                            ? `${postType.charAt(0).toUpperCase()}${postType.slice(1)}`
                            : 'Post'

                        return (
                          <article className="profile-post-card" key={post.id}>
                            <header className="profile-post-card-header">
                              <span>{postLabel}</span>
                              {post.createdAt && <time>{post.createdAt}</time>}
                            </header>
                            <div className="profile-post-body">
                              {editingProfilePostId === String(post.id) ? (
                                <form
                                  className="profile-post-edit-form"
                                  onSubmit={(event) => saveProfilePostEdit(event, post)}
                                >
                                  <label>
                                    Title
                                    <input
                                      type="text"
                                      value={profilePostTitleDraft}
                                      onChange={(event) => setProfilePostTitleDraft(event.target.value)}
                                      maxLength={200}
                                    />
                                  </label>
                                  <label>
                                    Post
                                    <textarea
                                      value={profilePostContentDraft}
                                      onChange={(event) => setProfilePostContentDraft(event.target.value)}
                                      rows={5}
                                      maxLength={10000}
                                    />
                                  </label>
                                  <label>
                                    Replace image or video
                                    <input
                                      type="file"
                                      accept="image/*,video/*"
                                      onChange={selectProfilePostMedia}
                                    />
                                  </label>
                                  {profilePostMediaDraft && (
                                    <small>
                                      Selected {profilePostMediaDraft.kind}. Save the post to publish this replacement.
                                    </small>
                                  )}
                                  <div className="profile-post-edit-actions">
                                    <button type="submit" disabled={isSavingProfilePost}>
                                      {isSavingProfilePost ? 'Saving…' : 'Save post'}
                                    </button>
                                    <button type="button" onClick={cancelEditingProfilePost} disabled={isSavingProfilePost}>
                                      Cancel
                                    </button>
                                  </div>
                                </form>
                              ) : (
                                <>
                                  {post.title && <h4>{post.title}</h4>}
                                  {post.content && <p>{post.content}</p>}
                                  {isOwnerView && (
                                    <div className="profile-post-owner-actions">
                                      <button
                                        className="profile-post-edit-button"
                                        type="button"
                                        onClick={() => startEditingProfilePost(post)}
                                      >
                                        Edit
                                      </button>
                                      <button
                                        className="profile-post-delete-button"
                                        type="button"
                                        onClick={() => deleteProfilePost(post.id)}
                                        aria-label={`Delete post ${post.title || post.content || ''}`}
                                        title="Delete post"
                                      >
                                        <Trash2 size={15} aria-hidden="true" />
                                        Delete
                                      </button>
                                    </div>
                                  )}
                                </>
                              )}
                            </div>
                            {displayedMedia?.kind === 'video' && (
                              <video className="profile-post-media" src={displayedMedia.url} controls />
                            )}
                            {displayedMedia?.kind !== 'video' && displayedMedia && (
                              <img
                                className="profile-post-media"
                                src={displayedMedia.url}
                                alt="Post attachment"
                                onError={(event) => {
                                  event.currentTarget.hidden = true
                                }}
                              />
                            )}
                            {post.likesCount > 0 && (
                              <div className="profile-post-reactions" aria-label={`${post.likesCount} likes`}>
                                👍 {post.likesCount}
                              </div>
                            )}
                          </article>
                        )
                      })}
                    </div>
                    {profilePosts.length > 1 && (
                      <button
                        className="profile-post-next-button"
                        type="button"
                        aria-label="Scroll to more posts"
                        title="More posts"
                        onClick={() => profilePostsListRef.current?.scrollBy({
                          left: profilePostsListRef.current.clientWidth * 0.85,
                          behavior: 'smooth',
                        })}
                      >
                        <ChevronRight size={22} aria-hidden="true" />
                      </button>
                    )}
                  </div>
                ) : (
                  <p className="profile-empty-hint">
                    {isOwnerView ? 'Your Social posts will appear here.' : 'No posts shared yet.'}
                  </p>
                )}
              </section>
              {(isOwnerView || (profileDetails.workTypes || []).length > 0) && (
                <section className="artist-section">
                  <PreviewSectionHeading isOwner={isOwnerView} onEdit={openSectionEditor} sectionId="workTypes">
                    Worked in
                  </PreviewSectionHeading>
                  {(profileDetails.workTypes || []).length > 0 && (
                    <div className="profile-tags">
                      {(profileDetails.workTypes || [])
                        .filter((workType) => !(
                          workType === 'others'
                          && profileDetails.workTypes.some((item) => item.startsWith('Other: '))
                        ))
                        .map((workType) => (
                        <span key={workType}>
                          {workType.startsWith('Other: ')
                            ? `Others: ${workType.slice('Other: '.length)}`
                            : workType}
                        </span>
                      ))}
                    </div>
                  )}
                  {editingSection === 'workTypes' && (
                    <div className="preview-inline-editor">
                      <SelectionGroup
                        label="Previous Worked in"
                        options={workTypes}
                        selected={selectedWorkTypes}
                        onChange={setSelectedWorkTypes}
                      />
                      {selectedWorkTypes.includes('others') && (
                        <div className="form-field">
                          <label htmlFor="inline-other-work-type">Specify where you worked</label>
                          <input
                            id="inline-other-work-type"
                            type="text"
                            value={otherWorkTypeDraft}
                            onChange={(event) => setOtherWorkTypeDraft(event.target.value)}
                            placeholder="Enter another type of work"
                          />
                        </div>
                      )}
                      {validationError && <p className="submit-error" role="alert">{validationError}</p>}
                      <div className="language-edit-actions">
                        <button className="secondary-button" type="button" onClick={closeSectionEditor}>Cancel</button>
                        <button className="contact-button" type="button" onClick={saveSectionEditor}>Save</button>
                      </div>
                    </div>
                  )}
                </section>
              )}
              <section className="artist-section">
                <PreviewSectionHeading isOwner={isOwnerView} onEdit={openSectionEditor} sectionId="talents">
                  Crafts
                </PreviewSectionHeading>
                <div className="profile-craft-summary">
                  <div>
                    <strong>Primary Craft</strong>
                    {primaryCraft
                      ? <div className="profile-tags"><span>{primaryCraft}</span></div>
                      : <p className="profile-empty-hint">No primary craft added yet.</p>}
                  </div>
                  <div>
                    <strong>Sub Craft</strong>
                    {profileDetails.subCraft
                      ? <div className="profile-tags"><span>{profileDetails.subCraft}</span></div>
                      : <p className="profile-empty-hint">No sub craft selected yet.</p>}
                  </div>
                  <div>
                    <strong>Secondary Crafts</strong>
                    {secondaryCrafts.length > 0
                      ? <div className="profile-tags">{secondaryCrafts.map((talent) => <span key={talent}>{talent}</span>)}</div>
                      : <p className="profile-empty-hint">No secondary crafts added yet.</p>}
                  </div>
                </div>
                {editingSection === 'talents' && (
                  <div className="preview-inline-editor">
                    <div className="form-field">
                      <label htmlFor="profile-sub-craft">Sub Craft</label>
                      <select
                        id="profile-sub-craft"
                        value={selectedSubCraft}
                        onChange={(event) => setSelectedSubCraft(event.target.value)}
                      >
                        <option value="">Select a sub craft</option>
                        {subCraftOptions.map((subCraftOption) => (
                          <option key={subCraftOption} value={subCraftOption}>{subCraftOption}</option>
                        ))}
                      </select>
                    </div>
                    <fieldset className="secondary-craft-editor">
                      <legend>Secondary Crafts</legend>
                      {selectedTalents.length > 0 ? (
                        <div className="profile-tags">
                          {selectedTalents.map((talent) => (
                            <span className="secondary-craft-tag" key={talent}>
                              {talent}
                              <button
                                type="button"
                                aria-label={`Remove ${talent}`}
                                onClick={() => setSelectedTalents((selected) => selected.filter((item) => item !== talent))}
                              >
                                ×
                              </button>
                            </span>
                          ))}
                        </div>
                      ) : (
                        <p className="profile-empty-hint">No secondary crafts selected.</p>
                      )}
                      <div className="secondary-craft-add">
                        <label className="visually-hidden" htmlFor="add-secondary-craft">Choose a craft to add</label>
                        <select
                          id="add-secondary-craft"
                          className="secondary-craft-add-select"
                          value={secondaryCraftToAdd}
                          onChange={(event) => setSecondaryCraftToAdd(event.target.value)}
                        >
                          <option value="">Select a craft to add</option>
                          {talents
                            .filter((talent) => talent !== primaryCraft && !selectedTalents.includes(talent))
                            .map((talent) => <option key={talent} value={talent}>{talent}</option>)}
                        </select>
                        <button
                          className="contact-button"
                          type="button"
                          disabled={!secondaryCraftToAdd}
                          onClick={() => {
                            setSelectedTalents((selected) => (
                              selected.includes(secondaryCraftToAdd)
                                ? selected
                                : [...selected, secondaryCraftToAdd]
                            ))
                            setSecondaryCraftToAdd('')
                          }}
                        >
                          Add
                        </button>
                      </div>
                    </fieldset>
                    <div className="language-edit-actions">
                      <button className="secondary-button" type="button" onClick={closeSectionEditor}>Cancel</button>
                      <button className="contact-button" type="button" onClick={saveSectionEditor}>Save</button>
                    </div>
                  </div>
                )}
              </section>

              {(isOwnerView || profileDetails.languages.length > 0) && (
                <section className="artist-section" id="artist-languages">
                  <PreviewSectionHeading isOwner={isOwnerView} onEdit={openSectionEditor} sectionId="languages">
                    Languages
                  </PreviewSectionHeading>
                  {profileDetails.languages.length > 0 && (
                    <div className="profile-tags">
                      {profileDetails.languages.map((language) => (
                        <span key={language}>
                          {language === 'Other' && profileDetails.otherLanguage
                            ? `Other: ${profileDetails.otherLanguage}`
                            : language}
                        </span>
                      ))}
                    </div>
                  )}
                  {editingSection === 'languages' && (
                    <div className="preview-inline-editor">
                      <SelectionGroup
                        label="Languages you know"
                        options={languages}
                        selected={selectedLanguages}
                        onChange={setSelectedLanguages}
                      />
                      {selectedLanguages.includes('Other') && (
                        <div className="form-field language-other-field">
                          <label htmlFor="inline-other-language">Specify your language</label>
                          <input
                            id="inline-other-language"
                            type="text"
                            value={otherLanguageDraft}
                            onChange={(event) => setOtherLanguageDraft(event.target.value)}
                            placeholder="Enter another language"
                          />
                        </div>
                      )}
                      {validationError && <p className="submit-error" role="alert">{validationError}</p>}
                      <div className="language-edit-actions">
                        <button className="secondary-button" type="button" onClick={closeSectionEditor}>Cancel</button>
                        <button className="contact-button" type="button" onClick={saveSectionEditor}>Save</button>
                      </div>
                    </div>
                  )}
                </section>
              )}

              <section className="artist-section" id="artist-demo-videos">
                <PreviewSectionHeading isOwner={isOwnerView} onEdit={openSectionEditor} sectionId="videos">
                  Demo videos
                </PreviewSectionHeading>
                {profileDetails.videos.length > 0 ? <div className="demo-video-list">
                  {profileDetails.videos.map((video, index) => {
                    const videoName = video.file?.name || video.fileName || `Demo video ${index + 1}`
                    return (
                      <div className="demo-video-item" key={`${video.url || videoName}-${index}`}>
                        <video
                          controls
                          playsInline
                          preload="metadata"
                          src={video.url}
                          aria-label={`Play demo video ${index + 1}: ${videoName}`}
                        />
                        <div><strong>Demo video {index + 1}</strong><span>{videoName}</span></div>
                      </div>
                    )
                  })}
                </div> : <p className="profile-empty-hint">Select Edit to add demo videos max 5MB or 3 miniutes.</p>}
                {editingSection === 'videos' && (
                  <div className="preview-inline-editor">
                    {profileDetails.videos.map((video, index) => (
                      <div className="inline-video-existing" key={`${video.url}-${index}`}>
                        <span>{video.file?.name || video.fileName || `Demo video ${index + 1}`}</span>
                        <button
                          className="delete-video-url-button"
                          type="button"
                          onClick={() => setProfileDetails((details) => ({
                            ...details,
                            videos: details.videos.filter((_, videoIndex) => videoIndex !== index),
                          }))}
                        >
                          Remove
                        </button>
                      </div>
                    ))}
                    {Array.from(
                      { length: Math.max(0, 3 - profileDetails.videos.length) },
                      (_, index) => index,
                    ).map((index) => (
                      <div className="video-upload-card" key={index}>
                        <label htmlFor={`inline-video-${index}`}>
                          <span className="video-label">Add demo video</span>
                          <span className="video-prompt">Choose a video to upload</span>
                        </label>
                        <input
                          id={`inline-video-${index}`}
                          type="file"
                          accept="video/*"
                          onChange={(event) => handleVideoChange(event, index)}
                        />
                        {uploadedVideos[index] && (
                          <span className="field-hint">{uploadedVideos[index].file.name}</span>
                        )}
                      </div>
                    ))}
                    {validationError && <p className="submit-error" role="alert">{validationError}</p>}
                    <div className="language-edit-actions">
                      <button className="secondary-button" type="button" onClick={closeSectionEditor}>Cancel</button>
                      <button className="contact-button" type="button" onClick={saveSectionEditor}>Save</button>
                    </div>
                  </div>
                )}
              </section>
              <section className="artist-section experience-section" id="artist-experience">
                <div className="experience-section-heading">
                  <h3>Experience</h3>
                  {isOwnerView && (
                    <button
                      className="language-edit-button"
                      type="button"
                      onClick={() => openExperienceModal()}
                    >
                      Add
                    </button>
                  )}
                </div>
                {(profileDetails.experiences || []).length > 0 ? (
                  <div className="experience-list">
                    {profileDetails.experiences.map((experience) => {
                      const organizationName = String(
                        experience.organizationName || experience.company || '',
                      ).trim()
                      const organizationLogoUrl = getOrganizationLogoUrl(
                        organizationName,
                        profileDetails.experiences,
                      )
                      return (
                      <article className="experience-entry" key={experience.id}>
                        <div className="experience-entry-heading">
                          <div>
                            <h4>{experience.projectName || experience.movieName}</h4>
                            <div className="experience-entry-role">
                              <span>{experience.workedAs}</span>
                              {organizationName && (
                                <>
                                  <span className="experience-organization-separator" aria-hidden="true">/</span>
                                  <span className="experience-organization-inline">
                                    <span className="experience-organization-logo" aria-hidden="true">
                                      <Building2 size={13} />
                                      {organizationLogoUrl && (
                                        <img
                                          src={organizationLogoUrl}
                                          alt=""
                                          onError={(event) => {
                                            event.currentTarget.hidden = true
                                          }}
                                        />
                                      )}
                                    </span>
                                    <span>{organizationName}</span>
                                  </span>
                                </>
                              )}
                            </div>
                          </div>
                          {isOwnerView && (
                            <button
                              className="language-edit-button"
                              type="button"
                              onClick={() => openExperienceModal(experience)}
                            >
                              Edit
                            </button>
                          )}
                        </div>
                        <p className="experience-entry-meta">
                          {experience.location} · {experience.startDate} – {experience.endDate}
                        </p>
                        {experience.highlights && (
                          <p className="experience-entry-highlights">{experience.highlights}</p>
                        )}
                        {experience.attachment && (
                          <div className="experience-entry-attachment">
                            {experience.attachment.type === 'image' ? (
                              <a href={experience.attachment.url} target="_blank" rel="noreferrer">
                                <img src={experience.attachment.url} alt={experience.attachment.fileName || 'Experience attachment'} />
                              </a>
                            ) : experience.attachment.type === 'video' ? (
                              <video controls src={experience.attachment.url}>
                                Your browser does not support video playback.
                              </video>
                            ) : (
                              <a href={experience.attachment.url} target="_blank" rel="noreferrer">
                                {experience.attachment.fileName || 'View text attachment'}
                              </a>
                            )}
                          </div>
                        )}
                      </article>
                      )
                    })}
                  </div>
                ) : null}
              </section>
              <section className="artist-section" id="artist-video-links">
                <PreviewSectionHeading isOwner={isOwnerView} onEdit={openSectionEditor} sectionId="videoLinks">
                  Video links
                </PreviewSectionHeading>
                {profileDetails.videoUrls.length > 0 ? (
                  <div className="artist-links">
                    {profileDetails.videoUrls.map((url, index) => (
                      <a href={url} target="_blank" rel="noreferrer" key={`${url}-${index}`}>
                        Video {index + 1}: {url}
                      </a>
                    ))}
                  </div>
                ) : (
                  <p className="profile-empty-hint">Select Edit to add video links.</p>
                )}
                {editingSection === 'videoLinks' && (
                  <div className="preview-inline-editor">
                    <div className="video-url-entry">
                      <div className="form-field">
                        <label htmlFor="inline-video-url">Video URL</label>
                        <input
                          id="inline-video-url"
                          type="url"
                          value={videoUrlInput}
                          disabled={savedVideoUrls.length >= 3}
                          placeholder="Paste video URL (https://...)"
                          onChange={(event) => {
                            setVideoUrlInput(event.target.value)
                            setVideoUrlError('')
                          }}
                        />
                        {videoUrlError && <span className="field-error">{videoUrlError}</span>}
                      </div>
                      <button
                        className="add-video-url-button"
                        type="button"
                        onClick={handleAddVideoUrl}
                        disabled={savedVideoUrls.length >= 3 || !videoUrlInput.trim()}
                      >
                        <span aria-hidden="true">+</span> Add URL
                      </button>
                    </div>
                    {savedVideoUrls.map((url, index) => (
                      <div className="saved-video-url" key={`${url}-${index}`}>
                        <a href={url} target="_blank" rel="noreferrer">Video {index + 1}: {url}</a>
                        <button
                          className="delete-video-url-button"
                          type="button"
                          onClick={() => handleRemoveVideoUrl(index)}
                          aria-label={`Delete video URL ${index + 1}`}
                        >
                          Delete
                        </button>
                      </div>
                    ))}
                    <div className="language-edit-actions">
                      <button className="secondary-button" type="button" onClick={closeSectionEditor}>Cancel</button>
                      <button className="contact-button" type="button" onClick={saveSectionEditor}>Save</button>
                    </div>
                  </div>
                )}
              </section>
            </div>

            <aside className="artist-sidebar" id="artist-work">
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
                        const res = await api.clickPromotion("Profile Artist Promo - Northstar Films")
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
            </aside>
          </div>
        </section>

      {isEditingProfile && (
        <button
          className="profile-modal-backdrop"
          type="button"
          aria-label="Close profile editor"
          onClick={closeProfileEditor}
        />
      )}
      {isExperienceModalOpen && (
        <>
          <button
            className="experience-modal-backdrop"
            type="button"
            aria-label="Close experience form"
            onClick={() => setIsExperienceModalOpen(false)}
          />
          <section
            className="experience-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="experience-modal-title"
          >
            <header className="profile-modal-heading">
              <div>
                <span className="eyebrow">WORK EXPERIENCE</span>
                <h2 id="experience-modal-title">
                  {experienceBeingEdited ? 'Edit experience' : 'Add experience'}
                </h2>
              </div>
              <button
                className="profile-modal-close"
                type="button"
                onClick={() => setIsExperienceModalOpen(false)}
                aria-label="Close experience form"
              >
                ×
              </button>
            </header>
            <form className="experience-form" onSubmit={handleExperienceSubmit}>
              <div className="experience-form-grid">
                <label className="form-field">
                  Attachment type
                  <select
                    name="attachmentType"
                    value={experienceAttachmentType}
                    onChange={(event) => {
                      setExperienceAttachmentType(event.target.value)
                      setExperienceSaveError('')
                    }}
                  >
                    <option value="">No attachment</option>
                    <option value="video">Video</option>
                    <option value="image">Image</option>
                    <option value="text">Text</option>
                  </select>
                </label>
                {experienceAttachmentType && (
                  <label className="form-field">
                    Upload {experienceAttachmentType}
                    <input
                      name="attachmentFile"
                      type="file"
                      accept={{
                        video: 'video/*',
                        image: 'image/*',
                        text: 'text/plain,.txt,.md,.rtf',
                      }[experienceAttachmentType]}
                      required={experienceAttachmentType !== experienceBeingEdited?.attachment?.type}
                    />
                    {experienceBeingEdited?.attachment?.type === experienceAttachmentType && (
                      <span className="field-hint">
                        Current file: {experienceBeingEdited.attachment.fileName}. Choose a new file to replace it.
                      </span>
                    )}
                  </label>
                )}
                <label className="form-field">
                  Project Name
                  <input
                    name="projectName"
                    type="text"
                    defaultValue={experienceBeingEdited?.projectName || experienceBeingEdited?.movieName || ''}
                    placeholder="Enter project name"
                    required
                  />
                </label>
                <div className="form-field">
                  <label className="organization-field-label" htmlFor="experience-organization">
                    <Building2 size={15} /> Production organization
                  </label>
                  <OrganizationPicker
                    options={organizationOptions}
                    value={organizationDraft}
                    onChange={setOrganizationDraft}
                  />
                </div>
                <label className="profile-featured-organization-field">
                  <input
                    name="featuredOnProfile"
                    type="checkbox"
                    defaultChecked={
                      Boolean(experienceBeingEdited?.featuredOnProfile)
                      || (!hasFeaturedExperience
                        && latestExperienceWithOrganization === experienceBeingEdited)
                      || (!experienceBeingEdited && !hasFeaturedExperience)
                    }
                  />
                  <span>Show this organization below my profile header</span>
                </label>
                <label className="form-field">
                  Location
                  <input
                    name="location"
                    type="text"
                    defaultValue={experienceBeingEdited?.location || ''}
                    placeholder="City or region"
                    required
                  />
                </label>
                <label className="form-field">
                  Worked as
                  <input
                    name="workedAs"
                    type="text"
                    defaultValue={experienceBeingEdited?.workedAs || ''}
                    placeholder="Your role on this project"
                    required
                  />
                </label>
                <label className="form-field">
                  Start date
                  <input
                    name="startDate"
                    type="date"
                    defaultValue={experienceBeingEdited?.startDate || ''}
                    required
                  />
                </label>
                <label className="form-field">
                  End date
                  <input
                    name="endDate"
                    type="date"
                    defaultValue={experienceBeingEdited?.endDate || ''}
                    onChange={(event) => event.currentTarget.setCustomValidity('')}
                    required
                  />
                </label>
                <label className="form-field experience-highlights-field">
                  Highlights
                  <textarea
                    name="highlights"
                    defaultValue={experienceBeingEdited?.highlights || ''}
                    placeholder="Projects, contributions, or results you achieved"
                    rows="5"
                  />
                </label>
              </div>
              {experienceSaveError && <p className="submit-error" role="alert">{experienceSaveError}</p>}
              <footer className="experience-form-footer">
                {experienceBeingEdited && (
                  <button
                    className="delete-video-url-button"
                    type="button"
                    onClick={handleExperienceDelete}
                  >
                    Delete
                  </button>
                )}
                <div className="experience-form-actions">
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => setIsExperienceModalOpen(false)}
                    disabled={isSavingExperience}
                  >
                    Cancel
                  </button>
                  <button className="contact-button" type="submit" disabled={isSavingExperience}>
                    {isSavingExperience ? 'Saving…' : 'Save experience'}
                  </button>
                </div>
              </footer>
            </form>
          </section>
        </>
      )}
      {isContactModalOpen && (
        <>
          <button
            className="reference-modal-backdrop"
            type="button"
            aria-label="Close reference ID form"
            onClick={() => setIsContactModalOpen(false)}
          />
          <section
            className="reference-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="reference-modal-title"
          >
            <div className="reference-modal-heading">
              <div>
                <span className="eyebrow">CONTACT ARTIST</span>
                <h2 id="reference-modal-title">{isOwnerView ? 'Your contact details' : 'Professional phone access'}</h2>
              </div>
              <button
                className="profile-modal-close"
                type="button"
                onClick={() => setIsContactModalOpen(false)}
                aria-label="Close reference ID form"
              >
                ×
              </button>
            </div>
            {isOwnerView ? (
              <div className="reference-modal-success" role="status">
                <p>Contact details for {profileDetails.name || 'this artist'}:</p>
                <dl className="reference-contact-details">
                  <div>
                    <dt>Phone</dt>
                    <dd>
                      {profileDetails.phone
                        ? <a href={`tel:${profileDetails.phone}`}>{profileDetails.phone}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>
                      {profileDetails.email
                        ? <a href={`mailto:${profileDetails.email}`}>{profileDetails.email}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                  <div>
                    <dt>Professional phone</dt>
                    <dd>
                      {profileDetails.professionalPhone
                        ? <a href={`tel:${profileDetails.professionalPhone}`}>{profileDetails.professionalPhone}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                </dl>
                <section className="phone-access-requests" aria-labelledby="phone-access-requests-title">
                  <h3 id="phone-access-requests-title">Professional phone requests</h3>
                  {phoneAccessRequests.filter((request) => request.status === 'pending').length > 0 ? (
                    phoneAccessRequests
                      .filter((request) => request.status === 'pending')
                      .map((request) => (
                        <div className="phone-access-request" key={request.id}>
                          <span>{request.requesterName} requested access to your professional phone.</span>
                          <div className="phone-access-request-actions">
                            <button
                              className="contact-button"
                              type="button"
                              onClick={() => decidePhoneAccessRequest(request.id, 'approved')}
                            >
                              Accept
                            </button>
                            <button
                              className="secondary-button"
                              type="button"
                              onClick={() => decidePhoneAccessRequest(request.id, 'declined')}
                            >
                              Decline
                            </button>
                          </div>
                        </div>
                      ))
                  ) : (
                    <p>No pending requests.</p>
                  )}
                  {phoneRequestMessage && <p role="status">{phoneRequestMessage}</p>}
                </section>
                <button
                  className="contact-button"
                  type="button"
                  onClick={() => setIsContactModalOpen(false)}
                >
                  Done
                </button>
              </div>
            ) : hasProfessionalPhoneAccess ? (
              <div className="reference-modal-success" role="status">
                <p>The profile owner approved your request to view their contact details.</p>
                <dl className="reference-contact-details">
                  <div>
                    <dt>Phone</dt>
                    <dd>
                      {profileDetails.phone
                        ? <a href={`tel:${profileDetails.phone}`}>{profileDetails.phone}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>
                      {profileDetails.email
                        ? <a href={`mailto:${profileDetails.email}`}>{profileDetails.email}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                  <div>
                    <dt>Professional phone</dt>
                    <dd>
                      {profileDetails.professionalPhone
                        ? <a href={`tel:${profileDetails.professionalPhone}`}>{profileDetails.professionalPhone}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                </dl>
                <button
                  className="contact-button"
                  type="button"
                  onClick={() => setIsContactModalOpen(false)}
                >
                  Done
                </button>
              </div>
            ) : (
              <div className="reference-modal-success">
                <p>Personal contact details are available below. The professional phone stays hidden unless the profile owner accepts your request.</p>
                <dl className="reference-contact-details">
                  <div>
                    <dt>Phone</dt>
                    <dd>
                      {profileDetails.phone
                        ? <a href={`tel:${profileDetails.phone}`}>{profileDetails.phone}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>
                      {profileDetails.email
                        ? <a href={`mailto:${profileDetails.email}`}>{profileDetails.email}</a>
                        : 'Not provided'}
                    </dd>
                  </div>
                  <div>
                    <dt>Professional phone</dt>
                    <dd>Hidden until the profile owner accepts your request.</dd>
                  </div>
                </dl>
                {currentPhoneRequest?.status === 'pending' && (
                  <p role="status">Your request is waiting for the profile owner’s approval.</p>
                )}
                {currentPhoneRequest?.status === 'declined' && (
                  <p role="status">The profile owner declined your request. The professional phone remains hidden.</p>
                )}
                {!currentPhoneRequest && (
                  <button className="contact-button" type="button" onClick={requestProfessionalPhone}>
                    Send request
                  </button>
                )}
                {phoneRequestMessage && <p role="status">{phoneRequestMessage}</p>}
                <div className="reference-form-actions">
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => setIsContactModalOpen(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </section>
        </>
      )}
      {/* This is profile edit page */}
      <form
        key={profileFormKey}
        className={`profile-form${isEditingProfile ? ' profile-modal-form' : ''}`}
        onSubmit={handleSubmit}
        hidden={!isOwnerView || !isEditingProfile}
      >
        {isEditingProfile && (
          <div className="profile-modal-heading">
            <div>
              <span className="eyebrow">UPDATE YOUR DETAILS</span>
              <h2>Edit artist profile</h2>
            </div>
            <button
              className="profile-modal-close"
              type="button"
              onClick={closeProfileEditor}
              aria-label="Close profile editor"
            >
              ×
            </button>
          </div>
        )}
        <section className="form-section profile-intro">
          <div className="section-heading">
            <span className="section-number">01</span>
            <div>
              <h2>Profile details</h2>
              <p>Tell us who you are and what you bring to the screen.</p>
            </div>
          </div>

          <div className="profile-fields">
            <div className="photo-field">
              <label htmlFor="profile-pic">Profile picture</label>
              <div style={{ position: 'relative' }}>
                <label className="photo-upload" htmlFor="profile-pic">
                  {displayedProfileImage ? (
                    <img src={displayedProfileImage} alt="Profile preview" />
                  ) : (
                    <svg
                      className="profile-avatar-placeholder"
                      viewBox="0 0 100 100"
                      aria-hidden="true"
                      style={{ width: '78px', height: '78px', borderRadius: '50%' }}
                    >
                      <circle cx="50" cy="50" r="50" style={{ fill: '#e5e7e8' }} />
                      <circle cx="50" cy="35" r="21" />
                      <path d="M10 100c2-24 18-42 40-42s38 18 40 42H10Z" />
                    </svg>
                  )}
                  <span className="photo-upload-text">{profilePic ? 'Change photo' : 'Upload photo'}</span>
                  <input
                    ref={profilePictureInput}
                    id="profile-pic"
                    type="file"
                    accept="image/*"
                    onChange={handleProfileChange}
                  />
                </label>
                {displayedProfileImage && (
                  <button
                    className="profile-image-remove-button"
                    type="button"
                    onClick={removeProfileImage}
                    aria-label="Remove profile picture"
                    title="Remove profile picture"
                    style={{
                      position: 'absolute',
                      top: '22px',
                      left: 'calc(50% + 16px)',
                      display: 'grid',
                      width: '24px',
                      height: '24px',
                      placeItems: 'center',
                      padding: 0,
                      border: '2px solid #fff',
                      borderRadius: '50%',
                      background: '#b42318',
                      color: '#fff',
                      fontSize: '17px',
                      lineHeight: 1,
                      cursor: 'pointer',
                    }}
                  >
                    ×
                  </button>
                )}
              </div>
              <span className="field-hint">JPG or PNG, preferably a clear headshot</span>
            </div>

            <div className="field-stack">
              <div className="form-field">
                <label htmlFor="user-id">User ID</label>
                <input
                  id="user-id"
                  name="userId"
                  type="text"
                  defaultValue={displayedUserId}
                  maxLength={14}
                  pattern="[A-Za-z0-9_-]{3,14}"
                  title="Use 3 to 14 letters, numbers, underscores, or hyphens."
                  required
                  autoComplete="off"
                  placeholder="Enter a unique TrueHunt ID"
                />
              </div>
              <div className="form-field">
                <label htmlFor="full-name">Full name</label>
                <input
                  id="full-name"
                  name="name"
                  type="text"
                  defaultValue={profileDetails.name}
                  placeholder="e.g. Arjun Kumar"
                />
              </div>
              <div className="form-field">
                <label htmlFor="phone">Personal phone number</label>
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  autoComplete="tel"
                  defaultValue={profileDetails.phone}
                  placeholder="e.g. +91 98765 43210"
                />
              </div>
              <div  className="form-field">
                <label htmlFor="professionalPhone">Professional phone number</label>
                <input
                  id="professionalPhone"
                  name="professionalPhone"
                  type="tel"
                  autoComplete="tel"
                  defaultValue={profileDetails.professionalPhone}
                  placeholder="e.g. +91 98765 43210"
                />
              </div>
              <div className="form-field">
                <label htmlFor="email">Email address</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  defaultValue={profileDetails.email}
                  placeholder="e.g. artist@example.com"
                />
              </div>
              <div className="form-field">
                <label htmlFor="experience">Experience</label>
                <select
                  id="experience"
                  name="experience"
                  defaultValue={profileDetails.experience || ''}
                >
                  <option value="">Select experience</option>
                  {profileDetails.experience && !EXPERIENCE_OPTIONS.includes(profileDetails.experience) && (
                    <option value={profileDetails.experience}>{profileDetails.experience}</option>
                  )}
                  {EXPERIENCE_OPTIONS.map((experienceOption) => (
                    <option key={experienceOption} value={experienceOption}>{experienceOption}</option>
                  ))}
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="date-of-birth">Date of birth</label>
                <input
                  id="date-of-birth"
                  name="dateOfBirth"
                  type="date"
                  max={getTodayDateString()}
                  value={dateOfBirthDraft}
                  onChange={(event) => setDateOfBirthDraft(event.target.value)}
                />
                {getAgeGroup(dateOfBirthDraft) && (
                  <span className="field-hint">Age group: {getAgeGroup(dateOfBirthDraft)}</span>
                )}
              </div>
              <div className="form-field">
                <label htmlFor="candidate-height">Height (cm)</label>
                <input
                  id="candidate-height"
                  name="height"
                  type="number"
                  min="0"
                  step="0.1"
                  defaultValue={profileDetails.height}
                  placeholder="e.g. 170"
                />
              </div>
              <div className="form-field">
                <label htmlFor="candidate-weight">Weight (kg)</label>
                <input
                  id="candidate-weight"
                  name="weight"
                  type="number"
                  min="0"
                  step="0.1"
                  defaultValue={profileDetails.weight}
                  placeholder="e.g. 65"
                />
              </div>
              <div className="form-field">
                <label htmlFor="candidate-body-build">Body build</label>
                <select
                  id="candidate-body-build"
                  name="bodyBuild"
                  defaultValue={profileDetails.bodyBuild || ''}
                >
                  <option value="">Select body build</option>
                  <option value="Slim">Slim</option>
                  <option value="Average">Average</option>
                  <option value="Athletic">Athletic</option>
                  <option value="Plus-size">Plus-size</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="candidate-gender">Gender</label>
                <select
                  id="candidate-gender"
                  name="gender"
                  value={genderDraft}
                  onChange={(event) => setGenderDraft(event.target.value)}
                >
                  <option value="">Select gender</option>
                  <option value="Female">Female</option>
                  <option value="Male">Male</option>
                  <option value="Non-binary">Non-binary</option>
                  <option value="Transgender">Transgender</option> 
                  <option value="Other">Other</option>
                  <option value="Prefer not to say">Prefer not to say</option>
                </select>
              </div>
              {genderDraft === 'Male' && (
                <div className="form-field">
                  <label htmlFor="candidate-facial-hair">Facial hair</label>
                  <select
                    id="candidate-facial-hair"
                    name="facialHair"
                    defaultValue={profileDetails.facialHair || ''}
                  >
                    <option value="">Select facial hair</option>
                    <option value="Clean-shaven">Clean-shaven</option>
                    <option value="Mustache">Mustache</option>
                    <option value="Beard">Beard</option>
                    <option value="Goatee">Goatee</option>
                    <option value="Stubble">Stubble</option>
                  </select>
                </div>
              )}
              <div className="form-field">
                <label htmlFor="candidate-skin-tone">Skin tone</label>
                <select
                  id="candidate-skin-tone"
                  name="skinTone"
                  defaultValue={profileDetails.skinTone}
                >
                  <option value="">Select skin tone</option>
                  <option value="White">White</option>
                  <option value="Brown">Brown</option>
                  <option value="Black">Black</option>
                  <option value="Olive">Olive</option>
                  <option value="Light brown">Light brown</option>
                  <option value="Dark">Dark</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="candidate-address">Enter the Full Address <span aria-hidden="true">*</span></label>
                <textarea
                  id="candidate-address"
                  name="address"
                  defaultValue={profileDetails.address}
                  placeholder="Street address, city, state, postal code"
                  rows="3"
                  required
                  aria-required="true"
                  onChange={(event) => {
                    event.currentTarget.setCustomValidity(
                      event.currentTarget.value.trim() ? '' : 'Enter your address.',
                    )
                  }}
                />
              </div>
            </div>
          </div>
        </section>

        <footer className="form-footer">
          <div className="form-feedback" aria-live="polite">
            {validationError && <p className="submit-error">{validationError}</p>}
            {isSubmitted && <p className="submit-success">Your profile is ready. Thanks for reaching out!</p>}
            {!validationError && !isSubmitted && <p>Your details help us connect you with the right opportunities.</p>}
          </div>
          <button className="submit-button" type="submit" disabled={isSubmittingProfile}>
            {isSubmittingProfile && <span className="submit-spinner" aria-hidden="true" />}
            {isSubmittingProfile ? 'Saving…' : 'Submit'}
          </button>
        </footer>
      </form>
      {pendingProfileCrop && (
        <div
          className="profile-image-crop-backdrop"
          onClick={cancelProfileCrop}
        >
          <section
            className="profile-image-crop-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="profile-image-crop-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="profile-image-crop-heading">
              <div>
                <span className="eyebrow">PROFILE PHOTO</span>
                <h2 id="profile-image-crop-title">Crop your photo</h2>
                <p>Drag the photo or crop frame, resize it with the handles, and adjust zoom.</p>
              </div>
              <button type="button" className="profile-modal-close" onClick={cancelProfileCrop} aria-label="Cancel crop">
                ×
              </button>
            </div>
            <div
              ref={cropStageRef}
              className="profile-image-crop-stage"
              onPointerDown={handleCropPointerDown}
              onPointerMove={handleCropPointerMove}
              onPointerUp={handleCropPointerUp}
              onPointerCancel={handleCropPointerUp}
              onLostPointerCapture={handleCropPointerUp}
              aria-label="Drag the photo or crop frame to adjust the image"
            >
              <img
                ref={cropImageRef}
                src={pendingProfileCrop.url}
                alt="Crop preview"
                onLoad={handleCropImageLoad}
                draggable="false"
                style={{
                  width: cropImageWidth,
                  height: cropImageHeight,
                  left: cropImageLeft,
                  top: cropImageTop,
                }}
              />
              <div
                className="profile-image-crop-selection"
                aria-hidden="true"
                style={{
                  left: cropFrame.x,
                  top: cropFrame.y,
                  width: cropFrame.size,
                  height: cropFrame.size,
                }}
              >
                <div className="profile-image-crop-grid">
                  <span className="profile-image-crop-grid-line is-vertical" />
                  <span className="profile-image-crop-grid-line is-vertical" />
                  <span className="profile-image-crop-grid-line is-horizontal" />
                  <span className="profile-image-crop-grid-line is-horizontal" />
                </div>
                <span className="profile-image-crop-handle is-top-left" data-crop-handle="top-left" />
                <span className="profile-image-crop-handle is-top-right" data-crop-handle="top-right" />
                <span className="profile-image-crop-handle is-bottom-left" data-crop-handle="bottom-left" />
                <span className="profile-image-crop-handle is-bottom-right" data-crop-handle="bottom-right" />
                <span className="profile-image-crop-edge-handle is-top" data-crop-handle="top" />
                <span className="profile-image-crop-edge-handle is-right" data-crop-handle="right" />
                <span className="profile-image-crop-edge-handle is-bottom" data-crop-handle="bottom" />
                <span className="profile-image-crop-edge-handle is-left" data-crop-handle="left" />
              </div>
            </div>
            <label className="profile-image-crop-zoom">
              Zoom
              <input
                type="range"
                min="1"
                max="3"
                step="0.01"
                value={cropZoom}
                onChange={(event) => setCropZoom(Number(event.target.value))}
              />
            </label>
            <div className="profile-image-crop-actions">
              <button className="secondary-button" type="button" onClick={cancelProfileCrop}>Cancel</button>
              <button
                className="contact-button"
                type="button"
                onClick={applyProfileCrop}
                disabled={!cropImageDimensions}
              >
                Use photo
              </button>
            </div>
          </section>
        </div>
      )}
      {editingOpportunityPost && (
        <OpportunitiesPage
          onClose={() => setEditingOpportunityPost(null)}
          onPublish={saveProfileOpportunityEdit}
          initialData={{
            ...editingOpportunityPost.opportunityDetails,
            projectName: editingOpportunityPost.opportunityDetails?.projectName
              || editingOpportunityPost.title
              || '',
          }}
          isEditing
        />
      )}
    </div>
  )
}

export default ProfilePage
