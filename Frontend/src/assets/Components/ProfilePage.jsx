  import { Fragment, useCallback, useEffect, useRef, useState } from 'react'
  import Cropper from 'react-easy-crop'
  import { Building2, Check, Eye, EyeOff, Trash2, UserPlus } from 'lucide-react'
  import './StylePage.css'
  import api from '../../api'
  import { useAuth } from '../../context/AuthContext'
  import MediaSafetyStatus from '../../components/common/MediaSafetyStatus.jsx'
  import { useMediaSafety } from '../../hooks/useMediaSafety.js'
  import OpportunitiesPage from './OpportunitiesPage.jsx'
  import { EXPERIENCE_OPTIONS, REGISTRATION_CRAFTS, SUB_CRAFTS_BY_PRIMARY_CRAFT } from '../../services/mockData'
  import ProfileBanner from './ProfileBanner.jsx'
  import PromotionCard from './PromotionCard'

  function formatHeightInFeet(heightCm) {
    const centimeters = Number(heightCm)
    if (!Number.isFinite(centimeters) || centimeters <= 0) return ''

    const totalInches = Math.round(centimeters / 2.54)
    const feet = Math.floor(totalInches / 12)
    const inches = totalInches % 12

    if (!feet) return `${inches} in`
    return inches ? `${feet} ft ${inches} in` : `${feet} ft`
  }

  function parseHeightToCentimeters(height) {
    const value = String(height ?? '').trim()
    if (!value) return ''

    const centimetersMatch = value.match(/^(\d+(?:\.\d+)?)\s*(?:cm)?$/i)
    if (centimetersMatch) {
      const centimeters = Number(centimetersMatch[1])
      return centimeters > 0 ? String(centimeters) : null
    }

    const feetMatch = value.match(/^(\d+)\s*(?:ft|feet|')\s*(?:(\d+)\s*(?:in|inches|")?)?$/i)
    if (!feetMatch) return null

    const feet = Number(feetMatch[1])
    const inches = Number(feetMatch[2] || 0)
    if (inches > 11 || (feet === 0 && inches === 0)) return null

    return String(Math.round((feet * 12 + inches) * 2.54))
  }

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
  const PROFILE_IMAGE_OUTPUT_SIZE = 512
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

  function extractDigits(val) {
    if (!val) return ''
    const digits = String(val).replace(/\D/g, '')
    if (digits.length === 12 && digits.startsWith('91')) {
      return digits.slice(2)
    }
    if (digits.length === 11 && digits.startsWith('0')) {
      return digits.slice(1)
    }
    return digits.slice(0, 10)
  }

  function validatePhoneNumber(value, isRequired = false, fieldName = 'Phone number') {
    const trimmed = String(value || '').trim()
    if (!trimmed) {
      if (isRequired) {
        return `${fieldName} is required.`
      }
      return ''
    }
    if (!/^\d+$/.test(trimmed)) {
      return `${fieldName} must contain numbers only.`
    }
    if (trimmed.length !== 10) {
      return `${fieldName} must be exactly 10 digits.`
    }
    if (!/^[6-9]/.test(trimmed)) {
      return `${fieldName} must start with 6, 7, 8, or 9.`
    }
    return ''
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

  function getProfileEndorsementsStorageKey(profileId) {
    const normalizedProfileId = String(profileId || 'me').trim().toLowerCase()
    return `truehunt-profile-endorsements:${encodeURIComponent(normalizedProfileId)}`
  }

  function readProfileEndorsements(profileId) {
    try {
      const savedEndorsements = localStorage.getItem(getProfileEndorsementsStorageKey(profileId))
      const endorsements = savedEndorsements ? JSON.parse(savedEndorsements) : []
      return Array.isArray(endorsements)
        ? endorsements.filter((item) => item && typeof item === 'object')
        : []
    } catch (error) {
      console.warn('Could not load local profile endorsements:', error)
      return []
    }
  }

  function mergeProfileEndorsements(...endorsementLists) {
    const endorsementsById = new Map()
    endorsementLists.forEach((list) => {
      if (!Array.isArray(list)) return
      list.forEach((item) => {
        if (!item || typeof item !== 'object') return
        const id = String(item.endorsementId || item.id || '').trim()
        const key = id || [
          item.endorserProfileId || item.endorserId || item.endorserName || '',
          item.createdDate || item.createdAt || '',
          item.endorsementMessage || '',
        ].join(':')
        endorsementsById.set(key, { ...endorsementsById.get(key), ...item })
      })
    })
    return [...endorsementsById.values()].sort((first, second) => {
      const firstDate = Date.parse(first.createdDate || first.createdAt || '') || 0
      const secondDate = Date.parse(second.createdDate || second.createdAt || '') || 0
      return secondDate - firstDate
    })
  }

  function getProfileIdentityKeys(value) {
    const identityFields = [
      'userId',
      'id',
      'user_id',
      '_id',
      'profileId',
      'publicUserId',
      'email',
      'referId',
      'senderId',
      'receiverId',
      'targetProfileId',
    ]
    const values = Array.isArray(value)
      ? value
      : typeof value === 'object' && value !== null
        ? identityFields.map((field) => value[field])
        : [value]
    return values.filter(Boolean).map((id) => String(id).trim().toLowerCase())
  }

  function isProfileInList(profile, profiles) {
    if (!Array.isArray(profiles)) return false
    const profileKeys = new Set(getProfileIdentityKeys(profile))
    return profiles.some((candidate) => (
      getProfileIdentityKeys(candidate).some((key) => profileKeys.has(key))
    ))
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

  const endorsementFormGroups = [
    {
      title: 'Endorsement basics',
      fields: [
        { name: 'receiverName', label: 'Receiver Name', readOnly: true },
        { name: 'receiverProfileId', label: 'Receiver Profile ID', readOnly: true },
        { name: 'receiverProfileImage', label: 'Receiver Profile Image', type: 'image' },
      ],
    },
    {
      title: 'Craft and skills',
      fields: [
        { name: 'primaryCraft', label: 'Primary Craft' },
        { name: 'subCraft', label: 'Sub Craft' },
        { name: 'specificSkill', label: 'Specific Skill' },
        { name: 'skillDescription', label: 'Skill Description', type: 'textarea' },
      ],
    },
    {
      title: 'Project details',
      fields: [
        { name: 'projectName', label: 'Project Name' },
        {
          name: 'projectType',
          label: 'Project Type',
          options: ['Movie', 'Short Film', 'Web Series', 'Advertisement'],
        },
        { name: 'projectRole', label: 'Project Role' },
        {
          name: 'projectYear',
          label: 'Project Year',
          type: 'text',
          inputMode: 'numeric',
          maxLength: 4,
          pattern: '[0-9]{4}',
          title: 'Enter a four-digit year',
        },
        { name: 'collaborationType', label: 'Collaboration Type' },
      ],
    },
    {
      title: 'Endorsement details',
      fields: [
        { name: 'endorsementMessage', label: 'Endorsement Message', type: 'textarea' },
        {
          name: 'professionalRelationship',
          label: 'Professional Relationship',
          options: ['Colleague', 'Collaborator', 'Client', 'Manager', 'Other'],
        },
        { name: 'overallRating', label: 'Overall Rating', options: ['1', '2', '3', '4', '5'] },
        { name: 'professionalStrengths', label: 'Professional Strengths', type: 'textarea' },
        { name: 'workQuality', label: 'Work Quality', type: 'textarea' },
        { name: 'communicationSkills', label: 'Communication Skills', type: 'textarea' },
      ],
    },
    {
      title: 'Verification',
      fields: [
        {
          name: 'projectCollaborationVerification',
          label: 'Project Collaboration Verification',
          type: 'checkbox',
        },
        {
          name: 'verificationStatus',
          label: 'Verification Status',
          options: ['Pending', 'Verified', 'Rejected'],
        },
        { name: 'supportingEvidence', label: 'Supporting Evidence (Optional)', type: 'url' },
        {
          name: 'endorsementStatus',
          label: 'Endorsement Status',
          options: ['Pending', 'Accepted', 'Rejected'],
        },
      ],
    },
    {
      title: 'System details',
      fields: [
        { name: 'createdDate', label: 'Created Date', readOnly: true },
        { name: 'updatedDate', label: 'Updated Date', readOnly: true },
        { name: 'acceptedDate', label: 'Accepted Date', type: 'date' },
        { name: 'visibility', label: 'Visibility', options: ['Public', 'Private'] },
        { name: 'reportStatus', label: 'Report Status', options: ['Not Reported', 'Reported'] },
      ],
    },
  ]

  const endorsementResponseFormGroups = [
    endorsementFormGroups[0],
    {
      ...endorsementFormGroups[1],
      fields: endorsementFormGroups[1].fields.filter(
        (field) => ['primaryCraft', 'subCraft'].includes(field.name),
      ),
    },
    {
      ...endorsementFormGroups[2],
      fields: endorsementFormGroups[2].fields.filter(
        (field) => ['projectName', 'projectType'].includes(field.name),
      ),
    },
    {
      ...endorsementFormGroups[3],
      fields: endorsementFormGroups[3].fields.filter(
        (field) => ['endorsementMessage', 'professionalRelationship'].includes(field.name),
      ),
    },
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
    endorsementResponseProfileId = '',
    onEndorsementResponseRequestHandled,
    profileDetails,
    setProfileDetails,
    profilePreview,
    setProfilePreview,
    onMessage,
    onViewConnections,
    onViewConnection,
    onViewFollowers,
    onViewMutualConnections,
  }) {
    const { user: authUser, updateUser } = useAuth()
    const profileLocation = getProfileLocationSummary(profileDetails)
    const [profilePic, setProfilePic] = useState(null)
    const profilePhotoSafety = useMediaSafety(profilePic)
    const isProfilePhotoBlocked = Boolean(profilePic && profilePhotoSafety.isBlocked)
    const [isProfileImageViewerOpen, setIsProfileImageViewerOpen] = useState(false)
    const [isSavingProfilePhoto, setIsSavingProfilePhoto] = useState(false)
    const [isProfileImageRemoved, setIsProfileImageRemoved] = useState(false)
    const [pendingProfileCrop, setPendingProfileCrop] = useState(null)
    const [cropZoom, setCropZoom] = useState(1)
    const [crop, setCrop] = useState({ x: 0, y: 0 })
    const [croppedAreaPixels, setCroppedAreaPixels] = useState(null)
    const profilePictureInput = useRef(null)
    const profileImageViewerInput = useRef(null)
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
    const [phoneDraft, setPhoneDraft] = useState(() => extractDigits(profileDetails.phone))
    const [professionalPhoneDraft, setProfessionalPhoneDraft] = useState(() => extractDigits(profileDetails.professionalPhone))
    const [phoneError, setPhoneError] = useState('')
    const [professionalPhoneError, setProfessionalPhoneError] = useState('')

    useEffect(() => {
      if (!isEditingProfile) {
        setPhoneDraft(extractDigits(profileDetails.phone))
        setProfessionalPhoneDraft(extractDigits(profileDetails.professionalPhone))
      }
    }, [profileDetails.phone, profileDetails.professionalPhone, isEditingProfile])

    function handlePhoneKeyDown(e) {
      if (
        ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Enter', 'Home', 'End'].includes(e.key) ||
        (e.ctrlKey || e.metaKey)
      ) {
        return
      }
      if (!/^[0-9]$/.test(e.key)) {
        e.preventDefault()
      }
    }

    function handlePhoneChange(e) {
      const raw = e.target.value
      const onlyDigits = raw.replace(/\D/g, '').slice(0, 10)
      setPhoneDraft(onlyDigits)
      if (phoneError) {
        setPhoneError(validatePhoneNumber(onlyDigits, true, 'Personal phone number'))
      }
    }

    function handleProfessionalPhoneChange(e) {
      const raw = e.target.value
      const onlyDigits = raw.replace(/\D/g, '').slice(0, 10)
      setProfessionalPhoneDraft(onlyDigits)
      if (professionalPhoneError) {
        setProfessionalPhoneError(validatePhoneNumber(onlyDigits, false, 'Professional phone number'))
      }
    }
    const [dateOfBirthDraft, setDateOfBirthDraft] = useState('')
    const [genderDraft, setGenderDraft] = useState(profileDetails.gender || '')
    const [isExperienceModalOpen, setIsExperienceModalOpen] = useState(false)
    const [editingExperienceId, setEditingExperienceId] = useState(null)
    const [experienceAttachmentType, setExperienceAttachmentType] = useState('')
    const [experienceSaveError, setExperienceSaveError] = useState('')
    const [isSavingExperience, setIsSavingExperience] = useState(false)
    const [organizationDraft, setOrganizationDraft] = useState('')
    const [isContactModalOpen, setIsContactModalOpen] = useState(false)
    const [contactAccessState, setContactAccessState] = useState({
      profileId: '',
      status: 'NONE',
      message: '',
    })
    const [isSubmittingContactAccess, setIsSubmittingContactAccess] = useState(false)
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
    const [isEndorsementFormOpen, setIsEndorsementFormOpen] = useState(false)
    const [isEndorsementsFullPage, setIsEndorsementsFullPage] = useState(false)
    const [isResponseRequestOpen, setIsResponseRequestOpen] = useState(false)
    const [selectedResponseRecipientIds, setSelectedResponseRecipientIds] = useState([])
    const [isLoadingResponseConnections, setIsLoadingResponseConnections] = useState(false)
    const [isSendingResponseRequests, setIsSendingResponseRequests] = useState(false)
    const [responseRequestError, setResponseRequestError] = useState('')
    const [responseRequestSuccess, setResponseRequestSuccess] = useState('')
    const [responseRequestCooldowns, setResponseRequestCooldowns] = useState({})
    const [responseRequestCooldownClock, setResponseRequestCooldownClock] = useState(Date.now())
    const [endorsementDraft, setEndorsementDraft] = useState(null)
    const [editingEndorsementId, setEditingEndorsementId] = useState('')
    const [localEndorsementsByProfile, setLocalEndorsementsByProfile] = useState({})
    const [remoteEndorsementsByProfile, setRemoteEndorsementsByProfile] = useState({})
    const [endorsementLoadErrorsByProfile, setEndorsementLoadErrorsByProfile] = useState({})
    const [endorsementError, setEndorsementError] = useState('')
    const [isSubmittingEndorsement, setIsSubmittingEndorsement] = useState(false)
    const setProfileDetailsRef = useRef(setProfileDetails)
    setProfileDetailsRef.current = setProfileDetails
    const profileActionsRef = useRef(null)
    const profilePostsListRef = useRef(null)
    const profileAccessId = String(profileDetails.userId || 'me')
    const targetProfileId = String(profileDetails.userId || profileDetails.id || '')
    const isOwnerView = isOwner
    const [currentUser] = useState(getCurrentUser)
    const [currentUserId] = useState(getCurrentUserIdentity)
    const [mutualConnectionState, setMutualConnectionState] = useState({
      profileId: '',
      count: null,
      error: '',
    })
    const [profilePeoplePage, setProfilePeoplePage] = useState('')
    const [profilePeopleState, setProfilePeopleState] = useState({
      profileId: '',
      page: '',
      people: [],
      error: '',
    })
    const viewerIdentifier = String(
      authUser?.userId
        || authUser?.publicUserId
        || authUser?.id
        || authUser?.email
        || currentUser?.userId
        || currentUser?.publicUserId
        || currentUser?.id
        || currentUser?.email
        || '',
    ).trim()
    const ownerProfileIdentifier = String(
      profileDetails.userId || profileDetails.id || currentUserId || '',
    ).trim()
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
    const savedEndorsements = Array.isArray(profileDetails.endorsements)
      ? profileDetails.endorsements.filter((item) => item && typeof item === 'object')
      : []
    const localEndorsements = localEndorsementsByProfile[targetProfileId]
      ?? readProfileEndorsements(targetProfileId)
    const remoteEndorsements = remoteEndorsementsByProfile[targetProfileId] || []
    const endorsementLoadError = endorsementLoadErrorsByProfile[targetProfileId] || ''
    const isLoadingEndorsements = Boolean(
      targetProfileId
      && !Object.prototype.hasOwnProperty.call(remoteEndorsementsByProfile, targetProfileId)
      && !endorsementLoadError,
    )
    const endorsements = mergeProfileEndorsements(
      savedEndorsements,
      localEndorsements,
      remoteEndorsements,
    )
    const currentUserProfileIds = new Set([
      currentUserId,
      currentUser?.userId,
      currentUser?.id,
      currentUser?.email,
      authUser?.userId,
      authUser?.id,
      authUser?.email,
    ].filter(Boolean).map((id) => String(id).trim().toLowerCase()))
    const hasResponded = endorsements.some((item) => {
      const endorserId = String(
        item.endorserProfileId || item.endorserId || item.authorId || '',
      ).trim().toLowerCase()
      return Boolean(endorserId && currentUserProfileIds.has(endorserId))
    })
    const endorsementPeopleCount = new Set(endorsements.map((item) => (
      String(item.endorserProfileId || item.endorserName || '').trim().toLowerCase()
    )).filter(Boolean)).size
    const endorsementCraftCount = new Set(endorsements.map((item) => (
      String(item.primaryCraft || '').trim().toLowerCase()
    )).filter(Boolean)).size
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

    useEffect(() => {
      const savedEndorsements = localEndorsementsByProfile[targetProfileId]
      if (!targetProfileId || !Array.isArray(savedEndorsements)) return

      try {
        localStorage.setItem(
          getProfileEndorsementsStorageKey(targetProfileId),
          JSON.stringify(savedEndorsements),
        )
      } catch (error) {
        console.warn('Could not save local profile endorsements:', error)
      }
    }, [localEndorsementsByProfile, targetProfileId])

    useEffect(() => {
      if (!targetProfileId || !isActive) return undefined

      let isCurrent = true
      let isLoading = false
      const loadEndorsements = async () => {
        if (isLoading) return
        isLoading = true
        try {
          const items = await api.getProfileEndorsements(targetProfileId)
          if (!isCurrent) return
          if (!Array.isArray(items)) {
            throw new Error('The server returned an invalid endorsements list.')
          }
          setRemoteEndorsementsByProfile((current) => ({
            ...current,
            [targetProfileId]: items.filter((item) => item && typeof item === 'object'),
          }))
          setEndorsementLoadErrorsByProfile((current) => {
            const next = { ...current }
            delete next[targetProfileId]
            return next
          })
        } catch (error) {
          console.error('Could not load profile endorsements:', error)
          if (isCurrent) {
            setEndorsementLoadErrorsByProfile((current) => ({
              ...current,
              [targetProfileId]: error instanceof Error ? error.message : 'Could not load endorsements.',
            }))
          }
        } finally {
          isLoading = false
        }
      }

      const refreshWhenVisible = () => {
        if (document.visibilityState === 'visible') loadEndorsements()
      }

      loadEndorsements()
      window.addEventListener('focus', refreshWhenVisible)
      document.addEventListener('visibilitychange', refreshWhenVisible)

      return () => {
        isCurrent = false
        window.removeEventListener('focus', refreshWhenVisible)
        document.removeEventListener('visibilitychange', refreshWhenVisible)
      }
    }, [targetProfileId, isActive])

    useEffect(() => {
      const storageKey = getProfileEndorsementsStorageKey(targetProfileId)
      function syncEndorsementsFromAnotherTab(event) {
        if (event.key !== storageKey || !event.newValue) return

        try {
          const savedEndorsements = JSON.parse(event.newValue)
          if (!Array.isArray(savedEndorsements)) {
            console.warn('Could not sync local profile endorsements: stored data is not a list.')
            return
          }
          setLocalEndorsementsByProfile((current) => ({
            ...current,
            [targetProfileId]: savedEndorsements.filter((item) => item && typeof item === 'object'),
          }))
        } catch (error) {
          console.warn('Could not sync local profile endorsements:', error)
        }
      }

      window.addEventListener('storage', syncEndorsementsFromAnotherTab)
      return () => window.removeEventListener('storage', syncEndorsementsFromAnotherTab)
    }, [targetProfileId])

    useEffect(() => {
      if (!isEndorsementFormOpen) return undefined

      function closeOnEscape(event) {
        if (event.key === 'Escape') closeEndorsementForm()
      }

      document.addEventListener('keydown', closeOnEscape)
      return () => document.removeEventListener('keydown', closeOnEscape)
    }, [isEndorsementFormOpen])
    useEffect(() => {
      if (!isResponseRequestOpen) return undefined

      function closeOnEscape(event) {
        if (event.key === 'Escape') setIsResponseRequestOpen(false)
      }

      document.addEventListener('keydown', closeOnEscape)
      const cooldownClockId = window.setInterval(
        () => setResponseRequestCooldownClock(Date.now()),
        15_000,
      )
      return () => {
        document.removeEventListener('keydown', closeOnEscape)
        window.clearInterval(cooldownClockId)
      }
    }, [isResponseRequestOpen])
    const contactAccessStatus = contactAccessState.profileId === profileAccessId
      ? contactAccessState.status
      : 'NONE'
    const isLoadingContactAccess = !isOwnerView
      && contactAccessState.profileId === profileAccessId
      && contactAccessStatus === 'LOADING'
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
    const parsedFollowerCount = Number(profileDetails.followersCount)
    const followerCount = Number.isFinite(parsedFollowerCount) && parsedFollowerCount >= 0
      ? parsedFollowerCount
      : 0
    const formattedFollowerCount = followerCount >= 500 ? '500+' : followerCount
    const mutualConnectionCount = mutualConnectionState.profileId === targetProfileId
      ? mutualConnectionState.count
      : null
    const mutualConnectionError = mutualConnectionState.profileId === targetProfileId
      ? mutualConnectionState.error
      : ''
    const hasProfessionalPhoneAccess = isOwnerView || contactAccessStatus === 'ACCEPTED'
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
      if (!isContactModalOpen || isOwnerView || !targetProfileId) return undefined

      let isCurrent = true
      api.getContactAccessStatus(targetProfileId)
        .then((status) => {
          if (isCurrent) {
            setContactAccessState({
              profileId: profileAccessId,
              status: status.status || 'NONE',
              message: status.message || '',
              phone: status.phone || '',
              professionalPhone: status.professionalPhone || '',
              email: status.email || '',
            })
          }
        })
        .catch((error) => {
          console.error('Could not load contact access status:', error)
          if (isCurrent) {
            setContactAccessState({
              profileId: profileAccessId,
              status: 'ERROR',
              message: 'Could not load contact access status. Please try again.',
            })
          }
        })

      return () => {
        isCurrent = false
      }
    }, [isContactModalOpen, isOwnerView, profileAccessId, targetProfileId])

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

    useEffect(() => {
      if (!isProfileImageViewerOpen) return undefined

      const previousOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      function closeOnEscape(event) {
        if (event.key === 'Escape' && !pendingProfileCrop) {
          setIsProfileImageViewerOpen(false)
        }
      }
      document.addEventListener('keydown', closeOnEscape)
      return () => {
        document.body.style.overflow = previousOverflow
        document.removeEventListener('keydown', closeOnEscape)
      }
    }, [isProfileImageViewerOpen, pendingProfileCrop])

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
        const detail = event.detail || {}
        const {
          connected: connectedValue,
          isConnected,
          pending: pendingValue,
          isPending,
          profile,
          senderProfile,
          receiverProfile,
          senderId,
          receiverId,
        } = detail
        const connected = typeof connectedValue === 'boolean' ? connectedValue : Boolean(isConnected)
        const pending = typeof pendingValue === 'boolean' ? pendingValue
          : typeof isPending === 'boolean' ? isPending : false
        const viewedProfileIds = new Set(getProfileIdentityKeys([
          targetProfileId,
          profileDetails.userId,
          profileDetails.id,
          profileDetails.email,
          profileDetails.referId,
        ]))
        const ownerProfileIds = new Set(getProfileIdentityKeys([
          accountUserId,
          currentUserId,
          currentUser?.userId,
          currentUser?.id,
          currentUser?.email,
        ]))
        const matchesIdentifiers = (value, identifiers) => getProfileIdentityKeys(value)
          .some((id) => identifiers.has(id))
        const hasParticipants = Boolean(senderId || receiverId || senderProfile || receiverProfile)
        const relevantIdentifiers = isOwnerView ? ownerProfileIds : viewedProfileIds
        const relevant = hasParticipants
          ? matchesIdentifiers(senderProfile, relevantIdentifiers) || matchesIdentifiers(senderId, relevantIdentifiers)
            || matchesIdentifiers(receiverProfile, relevantIdentifiers) || matchesIdentifiers(receiverId, relevantIdentifiers)
          : isOwnerView || matchesIdentifiers(profile, relevantIdentifiers)
        if (!relevant) return

        if (!isOwnerView && targetProfileId) {
          setConnectionState({
            profileId: targetProfileId,
            connected,
            pending,
          })
        }

        if (isOwnerView) {
          api.getConnectedProfiles().then((list) => {
            if (Array.isArray(list)) setConnectedProfiles(list)
          }).catch((error) => {
            console.warn('Could not refresh profile connections:', error)
          })
        } else if (targetProfileId) {
          api.getProfileById(targetProfileId).then((freshProfile) => {
            if (!freshProfile) return
            setConnectionState({
              profileId: targetProfileId,
              connected,
              pending: connected ? false : pending,
            })
            setProfileDetailsRef.current?.((currentProfile) => {
              if (!matchesIdentifiers(currentProfile, viewedProfileIds)) return currentProfile
              return {
                ...currentProfile,
                ...freshProfile,
                isConnected: connected,
                connected,
                isPending: connected ? false : pending,
                isOutgoingPending: connected ? false : pending,
              }
            })
          }).catch((error) => {
            console.warn('Could not refresh viewed profile after connection change:', error)
          })
        }
      }

      window.addEventListener('truehunt-connection-changed', syncConnectionState)
      return () => window.removeEventListener('truehunt-connection-changed', syncConnectionState)
    }, [
      accountUserId,
      currentUser?.email,
      currentUser?.id,
      currentUser?.userId,
      currentUserId,
      isOwnerView,
      profileDetails.email,
      profileDetails.id,
      profileDetails.referId,
      profileDetails.userId,
      targetProfileId,
    ])

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
      let lastRefreshStartedAt = 0
      async function loadCandidateProfileStatus() {
        try {
          const [profileResult, connectionsResult] = await Promise.allSettled([
            api.getProfileById(targetProfileId),
            api.getConnectedProfiles(),
          ])
          if (profileResult.status === 'rejected') {
            throw profileResult.reason
          }
          if (connectionsResult.status === 'rejected') {
            console.warn('Could not verify candidate connection from the connections list:', connectionsResult.reason)
          }
          const freshProfile = profileResult.value
          if (isCurrent && freshProfile) {
            const candidateIdentityKeys = new Set(getProfileIdentityKeys([
              targetProfileId,
              profileDetails.userId,
              profileDetails.id,
              profileDetails.email,
              profileDetails.referId,
            ]))
            const isConn = Boolean(
              freshProfile.isConnected
              || freshProfile.connected
              || (connectionsResult.status === 'fulfilled'
                && isProfileInList([
                  targetProfileId,
                  profileDetails.userId,
                  profileDetails.id,
                  profileDetails.email,
                  profileDetails.referId,
                ], connectionsResult.value)),
            )
            const isPend = !isConn && Boolean(freshProfile.isPending || freshProfile.isOutgoingPending)
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
            setProfileDetailsRef.current?.((currentProfile) => {
              if (!getProfileIdentityKeys(currentProfile).some((key) => candidateIdentityKeys.has(key))) {
                return currentProfile
              }
              return {
                ...currentProfile,
                ...freshProfile,
                isConnected: isConn,
                connected: isConn,
                isPending: isPend,
                isOutgoingPending: isPend,
              }
            })
          }
        } catch (err) {
          console.warn('Could not refresh candidate profile relationship:', err)
        }
      }

      function refreshWhenVisible() {
        if (document.hidden || Date.now() - lastRefreshStartedAt < 1000) return
        lastRefreshStartedAt = Date.now()
        void loadCandidateProfileStatus()
      }

      void loadCandidateProfileStatus()
      window.addEventListener('focus', refreshWhenVisible)
      document.addEventListener('visibilitychange', refreshWhenVisible)
      return () => {
        isCurrent = false
        window.removeEventListener('focus', refreshWhenVisible)
        document.removeEventListener('visibilitychange', refreshWhenVisible)
      }
    }, [
      isOwnerView,
      targetProfileId,
      isActive,
      profileDetails.userId,
      profileDetails.id,
      profileDetails.email,
      profileDetails.referId,
    ])

    useEffect(() => {
      if (
        isOwnerView
        || !targetProfileId
        || !isActive
        || !viewerIdentifier
        || profileDetails.connectionsVisible === false
      ) {
        setMutualConnectionState({ profileId: targetProfileId, count: null, error: '' })
        return undefined
      }

      let isCurrent = true
      setMutualConnectionState({ profileId: targetProfileId, count: null, error: '' })
      api.getMutualConnectionsCount(targetProfileId)
        .then((result) => {
          const count = Number(result?.count)
          if (!Number.isFinite(count) || count < 0) {
            throw new Error('The mutual connections response was invalid.')
          }
          if (isCurrent) {
            setMutualConnectionState({ profileId: targetProfileId, count, error: '' })
          }
        })
        .catch((error) => {
          console.error('Could not load mutual connection count:', error)
          if (isCurrent) {
            setMutualConnectionState({
              profileId: targetProfileId,
              count: null,
              error: 'Mutual connections could not be loaded.',
            })
          }
        })

      return () => {
        isCurrent = false
      }
    }, [isOwnerView, targetProfileId, isActive, viewerIdentifier, profileDetails.connectionsVisible])

    useEffect(() => {
      if (!profilePeoplePage || !targetProfileId || !isActive) return undefined

      let isCurrent = true
      const request = profilePeoplePage === 'followers'
        ? api.getProfileFollowers(targetProfileId)
        : api.getProfileMutualConnections(targetProfileId)
      request
        .then((people) => {
          if (!Array.isArray(people)) {
            throw new Error('The profile connections response was invalid.')
          }
          if (isCurrent) {
            setProfilePeopleState({
              profileId: targetProfileId,
              page: profilePeoplePage,
              people,
              error: '',
            })
          }
        })
        .catch((error) => {
          console.error(`Could not load profile ${profilePeoplePage}:`, error)
          if (isCurrent) {
            setProfilePeopleState({
              profileId: targetProfileId,
              page: profilePeoplePage,
              people: [],
              error: `Could not load ${profilePeoplePage === 'followers' ? 'followers' : 'mutual connections'}.`,
            })
          }
        })

      return () => {
        isCurrent = false
      }
    }, [profilePeoplePage, targetProfileId, isActive])

    useEffect(() => {
      if (!isOwnerView) return undefined

      let isCurrent = true
      let lastRefreshStartedAt = 0
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

      async function refreshOwnerProfile() {
        if (!isCurrent || !isActive || Date.now() - lastRefreshStartedAt < 1000) return
        lastRefreshStartedAt = Date.now()
        loadConnectionsCount()
        try {
          const fresh = await api.getProfile()
          if (isCurrent && fresh && fresh.name) {
            setProfileDetailsRef.current?.((prev) => ({ ...prev, ...fresh }))
          }
        } catch (error) {
          console.warn('Could not refresh the owner profile:', error)
        }
      }

      function refreshWhenVisible() {
        if (!document.hidden) void refreshOwnerProfile()
      }

      if (isActive) {
        void refreshOwnerProfile()
        window.addEventListener('focus', refreshWhenVisible)
        document.addEventListener('visibilitychange', refreshWhenVisible)
      }

      return () => {
        isCurrent = false
        window.removeEventListener('focus', refreshWhenVisible)
        document.removeEventListener('visibilitychange', refreshWhenVisible)
      }
    }, [isOwnerView, isActive])

    useEffect(() => {
      if (!isActive || !isOwnerView) return undefined

      let isCurrent = true
      let lastRefreshStartedAt = 0
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

      function refreshWhenVisible() {
        if (document.hidden || Date.now() - lastRefreshStartedAt < 1000) return
        lastRefreshStartedAt = Date.now()
        void loadProfilePosts()
      }

      void loadProfilePosts()
      window.addEventListener('focus', refreshWhenVisible)
      document.addEventListener('visibilitychange', refreshWhenVisible)
      return () => {
        isCurrent = false
        window.removeEventListener('focus', refreshWhenVisible)
        document.removeEventListener('visibilitychange', refreshWhenVisible)
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
        window.dispatchEvent(new CustomEvent('truehunt-social-post-deleted', {
          detail: { postId: String(postId), profileId: profilePostOwnerId },
        }))
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
          isConnected: connected,
          connected,
        }
        setConnectionState({ profileId: effectiveProfileId, connected, pending })
        window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
          detail: {
            connected,
            isConnected: connected,
            pending,
            isPending: pending,
            profile,
          },
        }))
      } catch (error) {
        console.error('Could not update profile connection:', error)
        setRelationshipErrorState({
          profileId: effectiveProfileId,
          message: error?.message || (isProfileConnected
            ? 'Could not remove this connection. Please try again.'
            : 'Could not send the connection request. Please try again.'),
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
      setCropZoom(1)
      setCrop({ x: 0, y: 0 })
      setCroppedAreaPixels(null)
      setPendingProfileCrop({ file, url: URL.createObjectURL(file) })
    }

    function cancelProfileCrop() {
      if (pendingProfileCrop?.url) URL.revokeObjectURL(pendingProfileCrop.url)
      setPendingProfileCrop(null)
      setCrop({ x: 0, y: 0 })
      setCropZoom(1)
      setCroppedAreaPixels(null)
    }

    const onCropComplete = useCallback((_croppedArea, croppedAreaPx) => {
      setCroppedAreaPixels(croppedAreaPx)
    }, [])

    async function applyProfileCrop() {
      if (!pendingProfileCrop || !croppedAreaPixels) return

      const image = new Image()
      image.src = pendingProfileCrop.url
      await new Promise((resolve) => { image.onload = resolve })

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
        croppedAreaPixels.x,
        croppedAreaPixels.y,
        croppedAreaPixels.width,
        croppedAreaPixels.height,
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

    function removeProfileImage(event) {
      event.preventDefault()
      event.stopPropagation()
      setProfilePic(null)
      setProfilePreview('')
      setIsProfileImageRemoved(true)
      if (profilePictureInput.current) profilePictureInput.current.value = ''
    }

    function closeProfileImageViewer() {
      if (profilePic) {
        setProfilePic(null)
        setProfilePreview(profileDetails.profileImageUrl || profileDetails.avatar || '')
      }
      setValidationError('')
      setIsProfileImageViewerOpen(false)
    }

    async function saveProfilePhoto() {
      if (!isOwnerView || !profilePic || isSavingProfilePhoto || isProfilePhotoBlocked) return

      setIsSavingProfilePhoto(true)
      setValidationError('')
      try {
        const uploaded = await api.uploadMedia(profilePic, 'profile-image')
        if (!uploaded?.url) {
          throw new Error('The profile photo could not be uploaded. Please try again.')
        }
        const savedDetails = await persistProfileDetails({
          ...profileDetails,
          userId: null,
          profileImageUrl: uploaded.url,
          avatar: uploaded.url,
        })
        setProfilePreview(savedDetails.profileImageUrl || uploaded.url)
        setProfilePic(null)
        setIsProfileImageViewerOpen(false)
        window.dispatchEvent(new CustomEvent('truehunt-profile-updated', {
          detail: { profile: savedDetails },
        }))
      } catch (error) {
        console.error('Could not save profile photo:', error)
        setValidationError(error.message || 'Could not save the profile photo. Please try again.')
      } finally {
        setIsSavingProfilePhoto(false)
      }
    }

    function closeProfileEditor() {
      cancelProfileCrop()
      if (profilePreview?.startsWith('blob:')) URL.revokeObjectURL(profilePreview)
      setProfilePic(null)
      setProfilePreview(profileDetails.profileImageUrl || '')
      setIsProfileImageRemoved(false)
      setPhoneDraft(extractDigits(profileDetails.phone))
      setProfessionalPhoneDraft(extractDigits(profileDetails.professionalPhone))
      setPhoneError('')
      setProfessionalPhoneError('')
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
      const nextAuthFields = {}
      if (persistedDetails.userId
        && (persistedDetails.userId !== authUser?.userId
          || persistedDetails.userId !== authUser?.publicUserId)) {
        nextAuthFields.userId = persistedDetails.userId
        nextAuthFields.publicUserId = persistedDetails.userId
        if (authUser?.id) {
          nextAuthFields.id = persistedDetails.userId
        }
      }
      if (persistedDetails.name && persistedDetails.name !== authUser?.name) {
        nextAuthFields.name = persistedDetails.name
      }
      if (persistedDetails.headline !== undefined && persistedDetails.headline !== authUser?.headline) {
        nextAuthFields.headline = persistedDetails.headline
      }
      if (persistedDetails.profileImageUrl !== undefined && persistedDetails.profileImageUrl !== authUser?.profileImageUrl) {
        nextAuthFields.profileImageUrl = persistedDetails.profileImageUrl
      }
      if (Object.keys(nextAuthFields).length > 0) {
        updateUser(nextAuthFields)
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
      setPhoneDraft(extractDigits(profileDetails.phone))
      setProfessionalPhoneDraft(extractDigits(profileDetails.professionalPhone))
      setPhoneError('')
      setProfessionalPhoneError('')
      setProfileFormKey((key) => key + 1)
      setIsEditingProfile(true)
    }

    async function requestProfessionalPhone() {
      if (!targetProfileId || isSubmittingContactAccess) return

      setIsSubmittingContactAccess(true)
      try {
        const request = await api.requestContactAccess(targetProfileId)
        setContactAccessState({
          profileId: profileAccessId,
          status: request.status || 'PENDING',
          message: request.message || 'Request sent. The profile owner must accept it before your contact details are shown.',
        })
      } catch (error) {
        console.error('Could not send contact access request:', error)
        setContactAccessState({
          profileId: profileAccessId,
          status: 'ERROR',
          message: error.message || 'Could not send your request. Please try again.',
        })
      } finally {
        setIsSubmittingContactAccess(false)
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
      if (isProfilePhotoBlocked) {
        setValidationError('This profile photo has high adult content and cannot be used.')
        return
      }
      setValidationError('')
      setIsSubmitted(false)

      const form = event.currentTarget
      const userIdInput = form.elements.namedItem('userId')
      const submittedUserId = userIdInput?.value.trim() || displayedUserId
      const userIdError = 'User ID must be 3 to 11 letters, numbers, underscores, or hyphens.'
      if (submittedUserId !== displayedUserId
        && !/^[A-Za-z0-9_-]{3,11}$/.test(submittedUserId)) {
        userIdInput.setCustomValidity(userIdError)
        userIdInput.reportValidity()
        setValidationError(userIdError)
        return
      }
      userIdInput.setCustomValidity('')

      const addressField = form.elements.namedItem('address')
      if (addressField.value.trim()) {
        addressField.setCustomValidity('')
      } else {
        addressField.setCustomValidity('Enter your address.')
        addressField.reportValidity()
        return
      }

      const personalPhoneErr = validatePhoneNumber(phoneDraft, true, 'Personal phone number')
      if (personalPhoneErr) {
        setPhoneError(personalPhoneErr)
        const phoneInput = form.elements.namedItem('phone')
        if (phoneInput) {
          phoneInput.focus()
          phoneInput.setCustomValidity(personalPhoneErr)
          phoneInput.reportValidity()
        }
        setValidationError(personalPhoneErr)
        return
      } else {
        setPhoneError('')
        const phoneInput = form.elements.namedItem('phone')
        if (phoneInput) phoneInput.setCustomValidity('')
      }

      const professionalPhoneErr = validatePhoneNumber(professionalPhoneDraft, false, 'Professional phone number')
      if (professionalPhoneErr) {
        setProfessionalPhoneError(professionalPhoneErr)
        const profInput = form.elements.namedItem('professionalPhone')
        if (profInput) {
          profInput.focus()
          profInput.setCustomValidity(professionalPhoneErr)
          profInput.reportValidity()
        }
        setValidationError(professionalPhoneErr)
        return
      } else {
        setProfessionalPhoneError('')
        const profInput = form.elements.namedItem('professionalPhone')
        if (profInput) profInput.setCustomValidity('')
      }

      const heightField = form.elements.namedItem('height')
      const heightInCentimeters = parseHeightToCentimeters(heightField.value)
      if (heightInCentimeters === null) {
        const message = 'Enter height in feet and inches or centimeters, e.g. 5 ft 8 in or 170 cm.'
        heightField.setCustomValidity(message)
        heightField.reportValidity()
        setValidationError(message)
        return
      }
      heightField.setCustomValidity('')

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
          userId: formData.get('userId')?.toString().trim() || displayedUserId,
          name: formData.get('name') || '',
          phone: phoneDraft.trim(),
          professionalPhone: professionalPhoneDraft.trim(),
          email: formData.get('email')?.trim() || '',
          experience: formData.get('experience') || '',
          dateOfBirth: formData.get('dateOfBirth') || '',
          height: heightInCentimeters,
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

    async function openResponseRequestDialog() {
      if (!isOwnerView) return
      setIsResponseRequestOpen(true)
      setSelectedResponseRecipientIds([])
      setResponseRequestError('')
      setResponseRequestSuccess('')
      setIsLoadingResponseConnections(true)
      try {
        if (!ownerProfileIdentifier) {
          throw new Error('Your profile ID could not be determined. Refresh your profile and try again.')
        }
        const [connections, cooldowns] = await Promise.all([
          api.getConnectedProfiles(),
          api.getEndorsementRequestCooldowns(ownerProfileIdentifier),
        ])
        if (!Array.isArray(connections) || !Array.isArray(cooldowns)) {
          throw new Error('The server returned invalid connection or request cooldown data.')
        }
        const now = Date.now()
        const cooldownByRecipient = Object.fromEntries(cooldowns
          .filter((cooldown) => cooldown?.recipientProfileId)
          .map((cooldown) => [
            String(cooldown.recipientProfileId).trim().toLowerCase(),
            now + Math.max(0, Number(cooldown.remainingSeconds) || 0) * 1000,
          ]))
        setConnectedProfiles(connections)
        setResponseRequestCooldowns(cooldownByRecipient)
        setResponseRequestCooldownClock(now)
      } catch (error) {
        console.error('Could not load connections for endorsement requests:', error)
        setResponseRequestError(error.message || 'Could not load your connections. Please try again.')
      } finally {
        setIsLoadingResponseConnections(false)
      }
    }

    function toggleResponseRecipient(profileId) {
      setResponseRequestSuccess('')
      setSelectedResponseRecipientIds((current) => (
        current.includes(profileId)
          ? current.filter((id) => id !== profileId)
          : [...current, profileId]
      ))
    }

    async function sendResponseRequests(event) {
      event.preventDefault()
      if (!isOwnerView || isSendingResponseRequests || selectedResponseRecipientIds.length === 0) return

      const senderProfileId = ownerProfileIdentifier
      if (!senderProfileId) {
        setResponseRequestError('Your profile ID could not be determined. Refresh your profile and try again.')
        return
      }

      setIsSendingResponseRequests(true)
      setResponseRequestError('')
      setResponseRequestSuccess('')
      try {
        const createdNotifications = await api.sendEndorsementRequests(
          senderProfileId,
          selectedResponseRecipientIds,
        )
        const sentCount = Array.isArray(createdNotifications)
          ? createdNotifications.length
          : selectedResponseRecipientIds.length

        const now = Date.now()
        const cooldownExpiry = now + 86_400 * 1000
        setResponseRequestCooldowns((current) => ({
          ...current,
          ...Object.fromEntries(
            selectedResponseRecipientIds.map((id) => [String(id).trim().toLowerCase(), cooldownExpiry])
          ),
        }))
        setResponseRequestCooldownClock(now)

        setSelectedResponseRecipientIds([])
        setResponseRequestSuccess(`Request${sentCount === 1 ? '' : 's'} sent to ${sentCount} connection${sentCount === 1 ? '' : 's'}.`)
      } catch (error) {
        console.error('Could not send endorsement requests:', error)
        setResponseRequestError(error.message || 'Could not send your requests. Please try again.')
      } finally {
        setIsSendingResponseRequests(false)
      }
    }

    const openEndorsementResponseForm = useCallback(() => {
      if (hasResponded) return

      setEndorsementError('')
      setEditingEndorsementId('')
      setEndorsementDraft({
        receiverName: profileDetails.name || '',
        receiverProfileId: targetProfileId,
        receiverProfileImage: profileDetails.profileImageUrl || profileDetails.avatar || '',
        primaryCraft: '',
        subCraft: '',
        projectName: '',
        projectType: '',
        endorsementMessage: '',
        professionalRelationship: '',
        visibility: 'Public',
      })
      setIsEndorsementFormOpen(true)
      setOpenProfileActionsFor(null)
    }, [
      hasResponded,
      profileDetails.avatar,
      profileDetails.name,
      profileDetails.profileImageUrl,
      targetProfileId,
    ])

    useEffect(() => {
      if (!endorsementResponseProfileId || !isActive || isOwnerView) return
      if (endorsementResponseProfileId.trim().toLowerCase() !== targetProfileId.trim().toLowerCase()) return

      openEndorsementResponseForm()
      onEndorsementResponseRequestHandled?.()
    }, [
      endorsementResponseProfileId,
      hasResponded,
      isActive,
      isOwnerView,
      openEndorsementResponseForm,
      onEndorsementResponseRequestHandled,
      targetProfileId,
    ])

    function closeEndorsementForm() {
      setIsEndorsementFormOpen(false)
      setEditingEndorsementId('')
      setEndorsementError('')
    }

    function openEndorsementEditForm(item) {
      const endorsementId = String(item.endorsementId || item.id || '').trim()
      if (!endorsementId) return

      const now = new Date().toLocaleString()
      setEditingEndorsementId(endorsementId)
      setEndorsementDraft({
        ...item,
        endorsementId,
        receiverName: item.receiverName || profileDetails.name || '',
        receiverProfileId: item.receiverProfileId || targetProfileId,
        receiverProfileImage: item.receiverProfileImage || profileDetails.profileImageUrl || profileDetails.avatar || '',
        projectYear: item.projectYear === null || item.projectYear === undefined ? '' : String(item.projectYear),
        createdDate: item.createdDate || item.createdAt || now,
        updatedDate: item.updatedDate || now,
        acceptedDate: String(item.acceptedDate || '').slice(0, 10),
      })
      setIsEndorsementFormOpen(true)
      setOpenProfileActionsFor(null)
    }

    function updateEndorsementDraft(event) {
      const { name, type, value, checked } = event.target
      const nextValue = type === 'checkbox'
        ? checked
        : name === 'projectYear'
          ? value.replace(/\D/g, '').slice(0, 4)
          : value
      setEndorsementDraft((current) => ({
        ...current,
        [name]: nextValue,
        updatedDate: new Date().toLocaleString(),
      }))
    }

    function renderEndorsementField(field) {
      if (!endorsementDraft) return null
      const fieldId = `endorsement-${field.name}`
      const value = endorsementDraft[field.name] ?? ''
      const today = new Date()
      const minimumAcceptedDate = new Date(
        today.getTime() - today.getTimezoneOffset() * 60_000,
      ).toISOString().slice(0, 10)

      if (field.type === 'checkbox') {
        return (
          <label
            className="endorsement-form-field is-checkbox"
            htmlFor={fieldId}
            key={field.name}
          >
            <input
              id={fieldId}
              name={field.name}
              type="checkbox"
              className="endorsement-form-checkbox"
              checked={Boolean(value)}
              onChange={updateEndorsementDraft}
            />
            <span>{field.label}</span>
          </label>
        )
      }

      if (field.type === 'image') {
        return (
          <div className="endorsement-form-field" key={field.name}>
            <span>{field.label}</span>
            {value
              ? <img className="endorsement-form-image-preview" src={String(value)} alt={`${field.label} preview`} />
              : <span className="endorsement-form-image-empty">No profile image available</span>}
          </div>
        )
      }

      let control
      if (field.options) {
        control = (
          <select
            id={fieldId}
            name={field.name}
            className="endorsement-form-control"
            value={value}
            onChange={updateEndorsementDraft}
            required={Boolean(field.required)}
          >
            <option value="">Select</option>
            {field.options.map((option) => (
              <option value={option} key={option}>{option}</option>
            ))}
          </select>
        )
      } else if (field.type === 'textarea') {
        control = (
          <textarea
            id={fieldId}
            name={field.name}
            className="endorsement-form-control"
            value={value}
            onChange={updateEndorsementDraft}
            required={Boolean(field.required)}
            rows={3}
          />
        )
      } else {
        control = (
          <input
            id={fieldId}
            name={field.name}
            className="endorsement-form-control"
            type={field.type || 'text'}
            value={value}
            onChange={updateEndorsementDraft}
            required={Boolean(field.required)}
            readOnly={Boolean(field.readOnly)}
            min={field.name === 'acceptedDate' ? minimumAcceptedDate : field.min}
            max={field.max}
            maxLength={field.maxLength}
            pattern={field.pattern}
            inputMode={field.inputMode}
            title={field.title}
          />
        )
      }

      return (
        <label
          className="endorsement-form-field"
          htmlFor={fieldId}
          key={field.name}
        >
          <span>
            {field.label}
            {field.required && <span aria-hidden="true"> *</span>}
          </span>
          {control}
        </label>
      )
    }

    async function submitEndorsement(event) {
      event.preventDefault()
      if (isSubmittingEndorsement) return
      if (hasResponded && !editingEndorsementId) {
        setEndorsementError('You have already responded to this profile.')
        return
      }

      const endorsementMessage = String(endorsementDraft.endorsementMessage || '').trim()
      if (!endorsementMessage) {
        setEndorsementError('An endorsement message is required.')
        return
      }
      const submittedDraft = { ...endorsementDraft, endorsementMessage }

      const today = new Date()
      const minimumAcceptedDate = new Date(
        today.getTime() - today.getTimezoneOffset() * 60_000,
      ).toISOString().slice(0, 10)
      if (endorsementDraft.acceptedDate && endorsementDraft.acceptedDate < minimumAcceptedDate) {
        setEndorsementError('Accepted date cannot be in the past.')
        return
      }
      if (endorsementDraft.projectYear && !/^\d{4}$/.test(endorsementDraft.projectYear)) {
        setEndorsementError('Enter a valid four-digit project year.')
        return
      }

      const now = new Date().toLocaleString()
      setIsSubmittingEndorsement(true)
      setEndorsementError('')
      try {
        if (editingEndorsementId) {
          const updatedEndorsement = {
            ...submittedDraft,
            endorsementId: editingEndorsementId,
            updatedDate: now,
          }
          const isRemoteEndorsement = remoteEndorsements.some(
            (item) => String(item.endorsementId || item.id || '') === editingEndorsementId,
          )
          if (isRemoteEndorsement) {
            const updated = await api.updateProfileEndorsement(
              targetProfileId,
              editingEndorsementId,
              updatedEndorsement,
            )
            setRemoteEndorsementsByProfile((current) => ({
              ...current,
              [targetProfileId]: (current[targetProfileId] ?? remoteEndorsements).map((item) => (
                String(item.endorsementId || item.id || '') === editingEndorsementId
                  ? updated
                  : item
              )),
            }))
          } else if (localEndorsements.some(
            (item) => String(item.endorsementId || item.id || '') === editingEndorsementId,
          )) {
            setLocalEndorsementsByProfile((current) => ({
              ...current,
              [targetProfileId]: (current[targetProfileId] ?? localEndorsements).map((item) => (
                String(item.endorsementId || item.id || '') === editingEndorsementId
                  ? updatedEndorsement
                  : item
              )),
            }))
          } else {
            setProfileDetails((current) => ({
              ...current,
              endorsements: (Array.isArray(current.endorsements) ? current.endorsements : []).map((item) => (
                String(item.endorsementId || item.id || '') === editingEndorsementId
                  ? updatedEndorsement
                  : item
              )),
            }))
          }
        } else {
          const createdEndorsement = await api.createProfileEndorsement(
            targetProfileId,
            submittedDraft,
          )
          setRemoteEndorsementsByProfile((current) => ({
            ...current,
            [targetProfileId]: mergeProfileEndorsements(
              [createdEndorsement],
              current[targetProfileId] ?? remoteEndorsements,
            ),
          }))
          setEndorsementLoadErrorsByProfile((current) => {
            const next = { ...current }
            delete next[targetProfileId]
            return next
          })
          try {
            const refreshedEndorsements = await api.getProfileEndorsements(targetProfileId)
            if (!Array.isArray(refreshedEndorsements)) {
              throw new Error('The server returned an invalid endorsements list.')
            }
            setRemoteEndorsementsByProfile((current) => ({
              ...current,
              [targetProfileId]: mergeProfileEndorsements(
                [createdEndorsement],
                refreshedEndorsements,
              ),
            }))
          } catch (refreshError) {
            console.error('Endorsement was saved, but the profile could not be refreshed:', refreshError)
            setEndorsementLoadErrorsByProfile((current) => ({
              ...current,
              [targetProfileId]: refreshError instanceof Error
                ? refreshError.message
                : 'Endorsement was saved, but the profile could not be refreshed.',
            }))
          }
        }

        closeEndorsementForm()
        requestAnimationFrame(() => {
          document.getElementById('artist-highlights')?.scrollIntoView({
            behavior: 'smooth',
            block: 'start',
          })
        })
      } catch (error) {
        console.error('Could not save profile endorsement:', error)
        setEndorsementError(
          error instanceof Error ? error.message : 'Could not save this endorsement. Please try again.',
        )
      } finally {
        setIsSubmittingEndorsement(false)
      }
    }

    async function deleteEndorsement(item) {
      const endorsementId = String(item.endorsementId || item.id || '').trim()
      const endorserId = String(
        item.endorserProfileId || item.endorserId || item.authorId || '',
      ).trim().toLowerCase()
      if (!currentUserProfileIds.has(endorserId)) return

      const matchesTarget = (entry) => {
        if (!entry) return false
        const entryId = String(entry.endorsementId || entry.id || '').trim()
        if (endorsementId && entryId && entryId === endorsementId) return true
        const entryEndorser = String(entry.endorserProfileId || entry.endorserId || entry.authorId || '').trim().toLowerCase()
        return Boolean(endorserId && entryEndorser && entryEndorser === endorserId)
      }

      if (endorsementId && remoteEndorsements.some((e) => String(e.endorsementId || e.id || '').trim() === endorsementId)) {
        try {
          await api.deleteProfileEndorsement(targetProfileId, endorsementId)
        } catch (error) {
          console.error('Could not delete profile endorsement:', error)
          setEndorsementError(
            error instanceof Error ? error.message : 'Could not delete this endorsement. Please try again.',
          )
          return
        }
      }

      setRemoteEndorsementsByProfile((current) => ({
        ...current,
        [targetProfileId]: (current[targetProfileId] ?? remoteEndorsements).filter(
          (endorsement) => !matchesTarget(endorsement),
        ),
      }))

      setLocalEndorsementsByProfile((current) => {
        const nextList = (current[targetProfileId] ?? localEndorsements).filter(
          (endorsement) => !matchesTarget(endorsement),
        )
        try {
          localStorage.setItem(
            getProfileEndorsementsStorageKey(targetProfileId),
            JSON.stringify(nextList),
          )
        } catch (storageError) {
          console.warn('Could not update localStorage after deleting endorsement:', storageError)
        }
        return {
          ...current,
          [targetProfileId]: nextList,
        }
      })

      setProfileDetails((current) => ({
        ...current,
        endorsements: (Array.isArray(current.endorsements) ? current.endorsements : []).filter(
          (endorsement) => !matchesTarget(endorsement),
        ),
      }))
      setEndorsementError('')
    }

    function renderEndorsementCards(items, showDetails = false) {
      if (!items.length) {
        return (
          <div>
            {endorsementError && <p className="profile-empty-hint" role="alert">{endorsementError}</p>}
            {endorsementLoadError && <p className="profile-empty-hint" role="alert">{endorsementLoadError}</p>}
            <p className="profile-empty-hint">
              {isLoadingEndorsements ? 'Loading endorsements…' : 'No endorsements yet.'}
            </p>
          </div>
        )
      }

      return (
        <>
        {endorsementError && <p className="profile-empty-hint" role="alert">{endorsementError}</p>}
        {endorsementLoadError && <p className="profile-empty-hint" role="alert">{endorsementLoadError}</p>}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 290px), 1fr))', gap: 12 }}>
          {items.map((item, index) => {
            const rating = Math.min(5, Math.max(0, Math.round(Number(item.overallRating) || 0)))
            const endorserImage = item.endorserProfileImage || item.endorserAvatar || ''
            const createdDate = item.createdDate || item.createdAt || item.date || ''
            const formattedDate = createdDate && Number.isNaN(Date.parse(createdDate))
              ? createdDate
              : createdDate ? new Date(createdDate).toLocaleDateString() : ''
            const isVerified = Boolean(item.projectCollaborationVerification)
              || String(item.verificationStatus || '').toLowerCase() === 'verified'
            const endorserId = String(
              item.endorserProfileId || item.endorserId || item.authorId || '',
            ).trim().toLowerCase()
            const canDeleteEndorsement = Boolean(endorserId && currentUserProfileIds.has(endorserId))
            const detailRows = [
              ['Endorsement ID', item.endorsementId || item.id],
              ['Receiver Name', item.receiverName],
              ['Receiver Profile ID', item.receiverProfileId],
              ['Receiver Profile Image', item.receiverProfileImage],
              ['Primary Craft', item.primaryCraft],
              ['Sub Craft', item.subCraft],
              ['Specific Skill', item.specificSkill],
              ['Skill Description', item.skillDescription],
              ['Project Name', item.projectName],
              ['Project Type', item.projectType],
              ['Project Role', item.projectRole],
              ['Project Year', item.projectYear],
              ['Collaboration Type', item.collaborationType],
              ['Endorsement Message', item.endorsementMessage],
              ['Overall Rating', item.overallRating],
              ['Professional Relationship', item.professionalRelationship],
              ['Professional Strengths', item.professionalStrengths],
              ['Work Quality', item.workQuality],
              ['Communication Skills', item.communicationSkills],
              ['Project Collaboration Verification', item.projectCollaborationVerification ? 'Verified' : 'Not verified'],
              ['Verification Status', item.verificationStatus],
              ['Supporting Evidence', item.supportingEvidence],
              ['Endorsement Status', item.endorsementStatus],
              ['Created Date', item.createdDate || item.createdAt],
              ['Updated Date', item.updatedDate],
              ['Accepted Date', item.acceptedDate],
              ['Visibility', item.visibility],
              ['Report Status', item.reportStatus],
            ].filter(([, value]) => value !== null && value !== undefined && String(value).trim())

            return (
              <article
                key={item.endorsementId || item.id || `${item.endorserProfileId || 'endorser'}-${index}`}
                style={{
                  display: 'grid',
                  gap: 10,
                  padding: 14,
                  border: '1px solid #e3e1ef',
                  borderRadius: 12,
                  background: '#fff',
                }}
              >
                <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 9, minWidth: 0 }}>
                    {endorserImage
                      ? (
                        <img
                          src={endorserImage}
                          alt=""
                          style={{ width: 42, height: 42, borderRadius: '50%', objectFit: 'cover' }}
                        />
                      )
                      : (
                        <span
                          aria-hidden="true"
                          style={{
                            display: 'grid',
                            width: 42,
                            height: 42,
                            flex: '0 0 42px',
                            placeItems: 'center',
                            borderRadius: '50%',
                            background: '#ece8ff',
                            color: '#6238d8',
                            fontWeight: 700,
                          }}
                        >
                          {String(item.endorserName || '?').trim().slice(0, 1).toUpperCase()}
                        </span>
                      )}
                    <span style={{ display: 'grid', minWidth: 0 }}>
                      <strong>{item.endorserName || 'Cinema professional'}</strong>
                      <small>{item.endorserProfession || item.endorserRole || ''}</small>
                      {(item.endorserProfileId || item.endorserId || item.authorId) && (
                        <small>Profile ID: {item.endorserProfileId || item.endorserId || item.authorId}</small>
                      )}
                    </span>
                  </div>
                  <span aria-label={`${rating} out of 5 stars`} style={{ color: '#f2a900', whiteSpace: 'nowrap' }}>
                    {'★'.repeat(rating)}{'☆'.repeat(5 - rating)}
                  </span>
                </header>
                {(item.projectName || item.projectYear) && (
                  <small>
                    Project: {item.projectName || 'Unnamed project'}
                    {item.projectYear ? ` (${item.projectYear})` : ''}
                  </small>
                )}
                {item.endorsementMessage && (
                  <p style={{ margin: 0, fontStyle: 'italic' }}>{item.endorsementMessage}</p>
                )}
                <footer style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, fontSize: 12 }}>
                  <span style={{ color: isVerified ? '#14734b' : '#697386' }}>
                    {isVerified ? '✓ Verified Collaboration' : 'Verification pending'}
                  </span>
                  {formattedDate && <time>{formattedDate}</time>}
                  {canDeleteEndorsement && (
                    <span style={{ display: 'flex', gap: 10, marginLeft: 'auto' }}>
                      <button
                        type="button"
                        onClick={() => openEndorsementEditForm(item)}
                        aria-label="Edit your endorsement"
                        style={{
                          padding: 0,
                          border: 0,
                          background: 'transparent',
                          color: '#6238d8',
                          cursor: 'pointer',
                          font: 'inherit',
                          fontWeight: 650,
                        }}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => deleteEndorsement(item)}
                        aria-label="Delete your endorsement"
                        style={{
                          padding: 0,
                          border: 0,
                          background: 'transparent',
                          color: '#b42318',
                          cursor: 'pointer',
                          font: 'inherit',
                          fontWeight: 650,
                        }}
                      >
                        Delete
                      </button>
                    </span>
                  )}
                </footer>
                {showDetails && (
                  <details>
                    <summary style={{ cursor: 'pointer', color: '#6238d8', fontSize: 12, fontWeight: 650 }}>
                      View Endorsement Details
                    </summary>
                    <dl style={{ display: 'grid', gridTemplateColumns: 'minmax(125px, 0.8fr) minmax(0, 1.2fr)', gap: '6px 12px', margin: '10px 0 0', fontSize: 12 }}>
                      {detailRows.map(([label, value]) => (
                        <Fragment key={label}>
                          <dt style={{ color: '#697386' }}>{label}</dt>
                          <dd style={{ margin: 0, overflowWrap: 'anywhere' }}>
                            {label.endsWith('Profile Image') && value
                              ? (
                                <img
                                  src={String(value)}
                                  alt={label}
                                  loading="lazy"
                                  style={{
                                    display: 'block',
                                    maxWidth: 140,
                                    maxHeight: 100,
                                    borderRadius: 8,
                                    objectFit: 'contain',
                                    background: '#f3f4f6',
                                  }}
                                />
                              )
                              : String(value)}
                          </dd>
                        </Fragment>
                      ))}
                    </dl>
                  </details>
                )}
              </article>
            )
          })}
        </div>
        </>
      )
    }

    if (profilePeoplePage) {
      const isFollowersPage = profilePeoplePage === 'followers'
      const pageTitle = isFollowersPage ? 'Followers' : 'Mutual connections'
      const hasLoadedPeople = profilePeopleState.profileId === targetProfileId
        && profilePeopleState.page === profilePeoplePage
      const people = hasLoadedPeople ? profilePeopleState.people : []
      const isLoadingPeople = !hasLoadedPeople
      const peopleError = hasLoadedPeople ? profilePeopleState.error : ''

      return (
        <main className="profile-people-page" aria-labelledby="profile-people-title">
          <div className="profile-people-content">
            <header className="profile-people-header">
              <div>
                <p className="profile-people-eyebrow">{profileDetails.name || 'Profile'}</p>
                <h1 id="profile-people-title">{pageTitle}</h1>
              </div>
              <div className="profile-people-header-actions">
                {!isOwnerView && profileDetails.connectionsVisible !== false && (
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => setProfilePeoplePage(isFollowersPage ? 'mutual' : 'followers')}
                  >
                    {isFollowersPage ? 'Mutual connections' : 'Followers'}
                  </button>
                )}
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setProfilePeoplePage('')}
                >
                  Back to profile
                </button>
              </div>
            </header>
            {isLoadingPeople ? (
              <p className="profile-people-message" role="status">Loading {pageTitle.toLowerCase()}...</p>
            ) : peopleError ? (
              <p className="profile-people-message is-error" role="alert">{peopleError}</p>
            ) : people.length === 0 ? (
              <p className="profile-people-message">
                {isFollowersPage ? 'No followers to show yet.' : 'No mutual connections to show yet.'}
              </p>
            ) : (
              <ul className="profile-people-list">
                {people.map((person, index) => {
                  const name = person.name || person.fullName || 'TrueHunt Member'
                  const avatar = person.profileImageUrl || person.avatar || person.profilePicture || person.imageUrl
                  const personId = person.userId || person.publicUserId || person.profileId || person.id || person.email || `${name}-${index}`
                  return (
                    <li key={personId}>
                      <button
                        type="button"
                        className="profile-people-person"
                        disabled={!onViewConnection}
                        aria-label={`View ${name}'s profile`}
                        onClick={() => {
                          setProfilePeoplePage('')
                          onViewConnection?.(person)
                        }}
                      >
                        {avatar ? (
                          <img src={avatar} alt="" />
                        ) : (
                          <span className="profile-people-avatar-fallback" aria-hidden="true">
                            {name.slice(0, 1).toUpperCase()}
                          </span>
                        )}
                        <span className="profile-people-person-copy">
                          <strong>{name}</strong>
                          {(person.headline || person.role) && <span>{person.headline || person.role}</span>}
                        </span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </main>
      )
    }

    function renderResponseRequestDialog() {
      if (!isResponseRequestOpen) return null
      return (
        <div
          className="endorsement-form-backdrop"
          onClick={() => {
            if (!isSendingResponseRequests) setIsResponseRequestOpen(false)
          }}
        >
          <section
            className="endorsement-form-dialog response-request-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="response-request-title"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="endorsement-form-header">
              <div>
                <span className="eyebrow">YOUR CONNECTIONS</span>
                <h2 id="response-request-title">Send response</h2>
              </div>
              <button
                type="button"
                className="profile-modal-close"
                onClick={() => setIsResponseRequestOpen(false)}
                disabled={isSendingResponseRequests}
                aria-label="Close response request dialog"
              >
                ×
              </button>
            </header>
            <form className="endorsement-form-body" onSubmit={sendResponseRequests}>
              <p className="endorsement-form-intro">
                Choose connections to ask for an endorsement on your profile.
              </p>
              {responseRequestError && <p className="profile-empty-hint" role="alert">{responseRequestError}</p>}
              {responseRequestSuccess && <p className="response-request-success" role="status">{responseRequestSuccess}</p>}
              {isLoadingResponseConnections ? (
                <p role="status">Loading your connections…</p>
              ) : connectedProfiles?.length ? (
                <div className="response-request-list">
                  {connectedProfiles.map((connection, index) => {
                    const recipientId = String(
                      connection.userId
                        || connection.publicUserId
                        || connection.profileId
                        || connection.id
                        || connection.email
                        || '',
                    ).trim()
                    if (!recipientId) return null
                    const displayName = connection.name || connection.fullName || 'Cinema Professional'
                    const image = connection.profileImageUrl || connection.avatar || ''
                    const cooldownExpiresAt = responseRequestCooldowns[recipientId.toLowerCase()] || 0
                    const cooldownRemainingSeconds = Math.max(
                      0,
                      Math.ceil((cooldownExpiresAt - responseRequestCooldownClock) / 1000),
                    )
                    const cooldownHours = Math.floor(cooldownRemainingSeconds / 3600)
                    const cooldownMinutes = Math.ceil((cooldownRemainingSeconds % 3600) / 60)
                    return (
                      <label className="response-request-person" key={`${recipientId}-${index}`}>
                        <input
                          type="checkbox"
                          checked={selectedResponseRecipientIds.includes(recipientId)}
                          onChange={() => toggleResponseRecipient(recipientId)}
                          disabled={isSendingResponseRequests || cooldownRemainingSeconds > 0}
                        />
                        {image ? (
                          <img src={image} alt="" />
                        ) : (
                          <span className="response-request-avatar" aria-hidden="true">
                            {displayName.charAt(0).toUpperCase()}
                          </span>
                        )}
                        <span>
                          <strong>{displayName}</strong>
                          {cooldownRemainingSeconds > 0 ? (
                            <small>
                              You can send another request in {cooldownHours > 0 ? `${cooldownHours}h ` : ''}{cooldownMinutes}m
                            </small>
                          ) : connection.role ? (
                            <small>{connection.role}</small>
                          ) : null}
                        </span>
                      </label>
                    )
                  })}
                </div>
              ) : (
                !responseRequestError && (
                  <p className="profile-empty-hint">
                    You do not have any connections to request an endorsement from yet.
                  </p>
                )
              )}
              <div className="endorsement-form-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setIsResponseRequestOpen(false)}
                  disabled={isSendingResponseRequests}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="contact-button"
                  disabled={
                    isLoadingResponseConnections
                    || isSendingResponseRequests
                    || selectedResponseRecipientIds.length === 0
                  }
                >
                  {isSendingResponseRequests ? 'Sending response…' : 'Send response'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )
    }

    function renderEndorsementFormDialog() {
      if (!isEndorsementFormOpen || !endorsementDraft) return null
      return (
        <div
          className="endorsement-form-backdrop"
          onClick={closeEndorsementForm}
        >
          <section
            className="endorsement-form-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="endorsement-response-title"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="endorsement-form-header">
              <div>
                <span className="eyebrow">PROFILE RESPONSE</span>
                <h2 id="endorsement-response-title">
                  {editingEndorsementId ? 'Edit your endorsement' : 'Respond with an endorsement'}
                </h2>
              </div>
              <button
                type="button"
                className="profile-modal-close"
                onClick={closeEndorsementForm}
                aria-label="Close endorsement form"
              >
                ×
              </button>
            </header>
            <form className="endorsement-form-body" onSubmit={submitEndorsement}>
              <p className="endorsement-form-intro">
                {editingEndorsementId
                  ? 'Update your endorsement for this profile.'
                  : 'Your endorsement will be saved on this profile and visible to profile visitors.'}
              </p>
              {endorsementError && <p className="profile-empty-hint" role="alert">{endorsementError}</p>}
              {(editingEndorsementId ? endorsementFormGroups : endorsementResponseFormGroups).map((group) => (
                <section className="endorsement-form-section" key={group.title}>
                  <h3>{group.title}</h3>
                  <div className="endorsement-form-grid">
                    {group.fields.map(renderEndorsementField)}
                  </div>
                </section>
              ))}
              <div className="endorsement-form-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={closeEndorsementForm}
                >
                  Cancel
                </button>
                <button type="submit" className="contact-button" disabled={isSubmittingEndorsement}>
                  {isSubmittingEndorsement
                    ? 'Saving…'
                    : editingEndorsementId ? 'Update Response' : 'Submit'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )
    }

    if (isEndorsementsFullPage) {
      return (
        <main
          aria-labelledby="all-endorsements-title"
          style={{
            position: 'fixed',
            zIndex: 1300,
            inset: 0,
            overflow: 'auto',
            padding: 24,
            background: '#f7f6fc',
          }}
        >
          <div style={{ maxWidth: 1100, margin: '0 auto', display: 'grid', gap: 20 }}>
            <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
              <div>
                <p style={{ margin: '0 0 4px', color: '#6e4dd8' }}>ENDORSEMENTS</p>
                <h1 id="all-endorsements-title" style={{ margin: 0 }}>
                  {profileDetails.name || 'Profile'}'s endorsements
                </h1>
              </div>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {isOwnerView && (
                  <button
                    className="contact-button"
                    type="button"
                    onClick={openResponseRequestDialog}
                  >
                    Send response
                  </button>
                )}
                {!isOwnerView && (
                  <button
                    type="button"
                    className="contact-button"
                    disabled={hasResponded}
                    onClick={openEndorsementResponseForm}
                  >
                    {hasResponded ? 'Responded' : 'Respond'}
                  </button>
                )}
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setIsEndorsementsFullPage(false)}
                >
                  Back to profile
                </button>
              </div>
            </header>
            {renderEndorsementCards(endorsements, true)}
          </div>
          {renderResponseRequestDialog()}
          {renderEndorsementFormDialog()}
        </main>
      )
    }

    return (
      <div className="profile-page-content">
          <section className="artist-profile" id="artist-profile" aria-labelledby="preview-title">
            <div className="artist-cover has-default-banner">
              <ProfileBanner variant="profile" />
            </div>
            <div className="artist-identity">
              <div className="profile-avatar-column">
                <div
                  className={`artist-avatar-progress${isOwnerView ? '' : ' is-readonly'}`}
                  style={{ '--profile-completion': `${profileCompletion}%` }}
                  role="button"
                  aria-label={`View ${profileDetails.name || 'profile'} photo`}
                  onClick={() => setIsProfileImageViewerOpen(true)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault()
                      setIsProfileImageViewerOpen(true)
                    }
                  }}
                  tabIndex={0}
                  title="View profile photo"
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
                          {(isProfileConnected || profileDetails.connectionsVisible !== false) && (
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
                          )}
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
                {profileLocation && <p className="artist-location">{profileLocation}</p>}
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
                      <div><dt>Height</dt><dd>{formatHeightInFeet(profileDetails.height)}</dd></div>
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
                <div className="profile-social-metrics">
                  {formattedConnectionCount !== null && (isOwnerView || profileDetails.connectionsVisible !== false) && (
                    isOwnerView ? (
                      <button
                        className="profile-connections-link"
                        type="button"
                        onClick={onViewConnections}
                        aria-label={`View ${formattedConnectionCount} connections`}
                      >
                        {formattedConnectionCount} connections
                      </button>
                    ) : (
                      <p className="profile-connections-count">
                        {formattedConnectionCount} connections
                      </p>
                    )
                  )}
                  <button
                    className="profile-social-metric profile-social-metric-button"
                    type="button"
                    onClick={() => {
                      if (onViewFollowers) onViewFollowers()
                      else setProfilePeoplePage('followers')
                    }}
                    aria-label={`View ${formattedFollowerCount} followers`}
                  >
                    {formattedFollowerCount} followers
                  </button>
                  {!isOwnerView && profileDetails.connectionsVisible !== false && (
                    mutualConnectionCount !== null
                      ? (
                        <button
                          className="profile-social-metric profile-social-metric-button"
                          type="button"
                          onClick={() => {
                            if (onViewMutualConnections) onViewMutualConnections()
                            else setProfilePeoplePage('mutual')
                          }}
                          aria-label={`View ${mutualConnectionCount} mutual connections`}
                        >
                          {mutualConnectionCount >= 500 ? '500+' : mutualConnectionCount} mutual connections
                        </button>
                      )
                      : mutualConnectionError
                        ? <p className="profile-social-metric profile-social-metric-error" role="status">{mutualConnectionError}</p>
                        : viewerIdentifier
                          ? <p className="profile-social-metric" role="status">Loading mutual connections…</p>
                          : <p className="profile-social-metric">Sign in to see mutual connections</p>
                  )}
                </div>
              </div>
              <div className="identity-actions">
                <button
                  className="identity-contact-button"
                  type="button"
                  onClick={() => {
                    setContactAccessState({
                      profileId: profileAccessId,
                      status: isOwnerView ? 'NONE' : 'LOADING',
                      message: '',
                    })
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
                <section className="artist-section insights-section" id="artist-highlights">
                  <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
                    <div>
                      <h3 style={{ marginBottom: 4 }}>Endorsements</h3>
                      <p style={{ margin: 0 }}>Recognitions from fellow cinema professionals</p>
                    </div>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                      {isOwnerView && (
                        <button
                          className="contact-button"
                          type="button"
                          onClick={openResponseRequestDialog}
                        >
                          Send response
                        </button>
                      )}
                      {!isOwnerView && (
                        <button
                          className="contact-button"
                          type="button"
                          disabled={!targetProfileId || hasResponded}
                          onClick={openEndorsementResponseForm}
                        >
                          {hasResponded ? 'Responded' : 'Respond'}
                        </button>
                      )}
                      <button
                        className="secondary-button"
                        type="button"
                        onClick={() => setIsEndorsementsFullPage(true)}
                      >
                        View All Endorsements
                      </button>
                    </div>
                  </header>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 8, margin: '14px 0' }}>
                    {[
                      { label: 'Total Endorsements', value: endorsements.length, icon: '★' },
                      { label: 'People', value: endorsementPeopleCount, icon: '●' },
                      { label: 'Crafts', value: endorsementCraftCount, icon: '▣' },
                    ].map((stat) => (
                      <div
                        key={stat.label}
                        className="profile-endorsement-stat"
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 12,
                          padding: 12,
                        }}
                      >
                        <span aria-hidden="true" style={{ color: '#6238d8', fontSize: 20 }}>{stat.icon}</span>
                        <span style={{ display: 'grid' }}>
                          <strong>{stat.value}</strong>
                          <small>{stat.label}</small>
                        </span>
                      </div>
                    ))}
                  </div>
                  {renderEndorsementCards(endorsements.slice(0, 3))}
                </section>

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
                              {post.likesCount > 0 && (isOwnerView || post.isAuthor || (post.showLikesCount !== false && !post.likeCountDisabled)) && (
                                <div className="profile-post-reactions" aria-label={`${post.likesCount} likes`}>
                                  👍 {post.likesCount}
                                </div>
                              )}
                            </article>
                          )
                        })}
                      </div>
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
                      Languages you know
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
                    Showreel
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
                    <h3><PreviewSectionHeading>Experience</PreviewSectionHeading></h3>
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
                    Video URL
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
                <PromotionCard ariaLabel="Sponsored movie industry promotion">
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
                </PromotionCard>
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
        {isProfileImageViewerOpen && (
          <>
            <button
              className="profile-photo-viewer-backdrop"
              type="button"
              aria-label="Close profile photo"
              onClick={closeProfileImageViewer}
            />
            <section
              className="profile-photo-viewer"
              role="dialog"
              aria-modal="true"
              aria-label={`${profileDetails.name || 'Profile'} photo`}
            >
              <button
                type="button"
                className="profile-photo-viewer-close"
                onClick={closeProfileImageViewer}
                aria-label="Close profile photo"
              >
                ×
              </button>
              {displayedProfileImage ? (
                <img
                  className="profile-photo-viewer-image"
                  src={displayedProfileImage}
                  alt={`${profileDetails.name || 'Profile'} photo`}
                />
              ) : (
                <div className="profile-photo-viewer-empty">No profile photo</div>
              )}
              <p className="profile-photo-viewer-notice" role="note">
                Screenshots are not allowed.
              </p>
              {profilePic && (
                <MediaSafetyStatus
                  safety={profilePhotoSafety}
                  blockedMessage="This profile photo has high adult content and cannot be used."
                  safeMessage="Safe content verified. Ready to use."
                />
              )}
              {isOwnerView && (
                <div className="profile-photo-viewer-actions">
                  <input
                    ref={profileImageViewerInput}
                    type="file"
                    accept="image/*"
                    hidden
                    onChange={handleProfileChange}
                  />
                  <button
                    type="button"
                    className="profile-photo-viewer-change"
                    onClick={() => profileImageViewerInput.current?.click()}
                    disabled={isSavingProfilePhoto}
                  >
                    {profilePic ? 'Choose a different photo' : 'Change photo'}
                  </button>
                  {profilePic && (
                    <button
                      type="button"
                      className="profile-photo-viewer-save"
                      onClick={saveProfilePhoto}
                      disabled={isSavingProfilePhoto || isProfilePhotoBlocked}
                    >
                      {isSavingProfilePhoto ? 'Saving…' : 'Save photo'}
                    </button>
                  )}
                  {validationError && <p role="alert">{validationError}</p>}
                </div>
              )}
            </section>
          </>
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
                  <h2 id="reference-modal-title">{isOwnerView ? 'Your contact details' : 'Contact details'}</h2>
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
                  <p>{contactAccessState.message || 'The profile owner approved your request to view their contact details.'}</p>
                  <dl className="reference-contact-details">
                    {isOwnerView && (
                    <div>
                      <dt>Phone</dt>
                      <dd>
                          {contactAccessState.phone
                            ? <a href={`tel:${contactAccessState.phone}`}>{contactAccessState.phone}</a>
                          : 'Not provided'}
                      </dd>
                    </div>
                    )}
                    <div>
                      <dt>Email</dt>
                      <dd>
                          {contactAccessState.email
                            ? <a href={`mailto:${contactAccessState.email}`}>{contactAccessState.email}</a>
                          : 'Not provided'}
                      </dd>
                    </div>
                    <div>
                      <dt>Professional phone</dt>
                      <dd>
                          {contactAccessState.professionalPhone
                            ? <a href={`tel:${contactAccessState.professionalPhone}`}>{contactAccessState.professionalPhone}</a>
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
                  {isLoadingContactAccess ? (
                    <p role="status">Checking contact access…</p>
                  ) : (
                    <>
                      <p>
                        {contactAccessState.message
                          || 'Phone numbers and email are hidden until the profile owner accepts your request.'}
                      </p>
                    </>
                  )}
                  {(contactAccessStatus === 'NONE' || contactAccessStatus === 'REJECTED')
                    && !isLoadingContactAccess && (
                    <button
                      className="contact-button"
                      type="button"
                      onClick={requestProfessionalPhone}
                      disabled={isSubmittingContactAccess}
                    >
                      {isSubmittingContactAccess
                        ? 'Sending…'
                        : contactAccessStatus === 'REJECTED' ? 'Send request again' : 'Send request'}
                    </button>
                  )}
                  {contactAccessStatus === 'ERROR' && !isLoadingContactAccess && (
                    <button
                      className="contact-button"
                      type="button"
                      onClick={requestProfessionalPhone}
                      disabled={isSubmittingContactAccess}
                    >
                      {isSubmittingContactAccess ? 'Sending…' : 'Try again'}
                    </button>
                  )}
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
          className={`profile-form${isEditingProfile ? ' profile-modal-form profile-modal-form--light' : ''}`}
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
                {profilePic && (
                  <MediaSafetyStatus
                    safety={profilePhotoSafety}
                    blockedMessage="This profile photo has high adult content and cannot be used."
                    safeMessage="Safe content verified. Ready to use."
                  />
                )}
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
                    maxLength={Math.max(11, displayedUserId.length)}
                    title="Use 3 to 11 letters, numbers, underscores, or hyphens."
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
                <div className={`form-field${phoneError ? ' has-error' : ''}`}>
                  <label htmlFor="phone">Personal phone number</label>
                  <input
                    id="phone"
                    name="phone"
                    type="tel"
                    inputMode="numeric"
                    autoComplete="tel"
                    maxLength={10}
                    value={phoneDraft}
                    onChange={handlePhoneChange}
                    onKeyDown={handlePhoneKeyDown}
                    onBlur={() => setPhoneError(validatePhoneNumber(phoneDraft, true, 'Personal phone number'))}
                    placeholder="e.g. 9876543210"
                    className={phoneError ? 'is-invalid' : ''}
                    aria-invalid={Boolean(phoneError)}
                    aria-describedby={phoneError ? 'phone-error' : undefined}
                  />
                  {phoneError && (
                    <span id="phone-error" className="field-error" role="alert">
                      {phoneError}
                    </span>
                  )}
                </div>
                <div className={`form-field${professionalPhoneError ? ' has-error' : ''}`}>
                  <label htmlFor="professionalPhone">Professional phone number</label>
                  <input
                    id="professionalPhone"
                    name="professionalPhone"
                    type="tel"
                    inputMode="numeric"
                    autoComplete="tel"
                    maxLength={10}
                    value={professionalPhoneDraft}
                    onChange={handleProfessionalPhoneChange}
                    onKeyDown={handlePhoneKeyDown}
                    onBlur={() => setProfessionalPhoneError(validatePhoneNumber(professionalPhoneDraft, false, 'Professional phone number'))}
                    placeholder="e.g. 9876543210"
                    className={professionalPhoneError ? 'is-invalid' : ''}
                    aria-invalid={Boolean(professionalPhoneError)}
                    aria-describedby={professionalPhoneError ? 'professional-phone-error' : undefined}
                  />
                  {professionalPhoneError && (
                    <span id="professional-phone-error" className="field-error" role="alert">
                      {professionalPhoneError}
                    </span>
                  )}
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
                  <label htmlFor="candidate-height">Height (ft/in or cm)</label>
                  <input
                    id="candidate-height"
                    name="height"
                    type="text"
                    defaultValue={formatHeightInFeet(profileDetails.height)}
                    placeholder="e.g. 5 ft 8 in or 170 cm"
                    onChange={(event) => event.currentTarget.setCustomValidity('')}
                    onBlur={(event) => {
                      const centimeters = parseHeightToCentimeters(event.currentTarget.value)
                      if (centimeters !== null) {
                        event.currentTarget.value = formatHeightInFeet(centimeters)
                      }
                    }}
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
                    <option value="Wheatish Brown">Wheatish Brown</option>
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
            <button className="submit-button" type="submit" disabled={isSubmittingProfile || isProfilePhotoBlocked}>
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
                  <p>Pinch or scroll to zoom, drag to reposition.</p>
                </div>
                <button type="button" className="profile-modal-close" onClick={cancelProfileCrop} aria-label="Cancel crop">
                  ×
                </button>
              </div>
              <div className="profile-image-crop-stage">
                <Cropper
                  image={pendingProfileCrop.url}
                  crop={crop}
                  zoom={cropZoom}
                  aspect={1}
                  cropShape="round"
                  showGrid={true}
                  onCropChange={setCrop}
                  onZoomChange={setCropZoom}
                  onCropComplete={onCropComplete}
                />
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
                  disabled={!croppedAreaPixels}
                >
                  Use photo
                </button>
              </div>
            </section>
          </div>
        )}
        {renderResponseRequestDialog()}
        {renderEndorsementFormDialog()}
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
