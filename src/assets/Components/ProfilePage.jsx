import { useEffect, useRef, useState } from 'react'
import './StylePage.css'
import api from '../../api'
import { EXPERIENCE_OPTIONS, REGISTRATION_CRAFTS, SUB_CRAFTS_BY_PRIMARY_CRAFT } from '../../services/mockData'

export const emptyProfileDetails = {
  userId: '',
  name: '',
  phone: '',
  professionalPhone: '',
  email: '',
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
  skinTone: '',
  address: '',
  languages: [],
  otherLanguage: '',
  talents: [],
  workTypes: [],
  videos: [],
  videoUrls: [],
  experiences: [],
}

const MAX_VIDEO_SIZE = 5 * 1024 * 1024
const MAX_VIDEO_DURATION = 3 * 60
const PHONE_ACCESS_EVENT = 'truehunt-professional-phone-access-changed'

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
  'English',
  'Telugu',
  'Hindi',
  'Tamil',
  'Kannada',
  'Malayalam',
  'Bengali',
  'Marathi',
  'Other',
]

const talents = REGISTRATION_CRAFTS

const workTypes = [
  'Cinema',
  'Web series',
  'Short films',
  'Reels',
  'Advertisements',
  'Serials',
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
  accountUserId = '',
  profileDetails,
  setProfileDetails,
  profilePreview,
  setProfilePreview,
  onMessage,
}) {
  const [profilePic, setProfilePic] = useState(null)
  const [isProfileImageRemoved, setIsProfileImageRemoved] = useState(false)
  const profilePictureInput = useRef(null)
  const [selectedLanguages, setSelectedLanguages] = useState([])
  const [otherLanguageDraft, setOtherLanguageDraft] = useState('')
  const [selectedTalents, setSelectedTalents] = useState([])
  const [selectedSubCraft, setSelectedSubCraft] = useState('')
  const [secondaryCraftToAdd, setSecondaryCraftToAdd] = useState('')
  const [selectedWorkTypes, setSelectedWorkTypes] = useState([])
  const [uploadedVideos, setUploadedVideos] = useState([null, null, null])
  const uploadedVideoUrls = useRef([])
  const [videoUrlInput, setVideoUrlInput] = useState('')
  const [videoUrlError, setVideoUrlError] = useState('')
  const [savedVideoUrls, setSavedVideoUrls] = useState([])
  const [validationError, setValidationError] = useState('')
  const [isSubmitted, setIsSubmitted] = useState(false)
  const [profileFormKey, setProfileFormKey] = useState(0)
  const [isEditingProfile, setIsEditingProfile] = useState(false)
  const [dateOfBirthDraft, setDateOfBirthDraft] = useState('')
  const [isExperienceModalOpen, setIsExperienceModalOpen] = useState(false)
  const [editingExperienceId, setEditingExperienceId] = useState(null)
  const [experienceAttachmentType, setExperienceAttachmentType] = useState('')
  const [experienceSaveError, setExperienceSaveError] = useState('')
  const [isSavingExperience, setIsSavingExperience] = useState(false)
  const [isContactModalOpen, setIsContactModalOpen] = useState(false)
  const [phoneAccessState, setPhoneAccessState] = useState(() => {
    const profileId = String(profileDetails.userId || 'me')
    return { profileId, requests: readPhoneAccessRequests(profileId) }
  })
  const [phoneRequestMessage, setPhoneRequestMessage] = useState('')
  const [editingSection, setEditingSection] = useState('')
  const profileAccessId = String(profileDetails.userId || 'me')
  const phoneAccessRequests = phoneAccessState.profileId === profileAccessId
    ? phoneAccessState.requests
    : readPhoneAccessRequests(profileAccessId)
  const [currentUser] = useState(getCurrentUser)
  const [currentUserId] = useState(getCurrentUserIdentity)
  const currentPhoneRequest = phoneAccessRequests.find((request) => (
    request.requesterId === currentUserId
  ))
  const hasProfessionalPhoneAccess = isOwner || currentPhoneRequest?.status === 'approved'
  const experienceBeingEdited = (profileDetails.experiences || [])
    .find((experience) => experience.id === editingExperienceId)
  const primaryCraft = profileDetails.primaryCraft || profileDetails.talents?.[0] || ''

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
    : profilePreview || profileDetails.profileImageUrl || ''
  const profileCompletion = getProfileCompletion(profileDetails, Boolean(displayedProfileImage))

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
    if (!file) return

    setProfilePic(file)
    setIsProfileImageRemoved(false)
    setProfilePreview(URL.createObjectURL(file))
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
    setSelectedWorkTypes(profileDetails.workTypes || [])
    setSavedVideoUrls(profileDetails.videoUrls)
    setVideoUrlInput('')
    setVideoUrlError('')
    setValidationError('')
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

  async function saveSectionEditor() {
    let nextDetails = { ...profileDetails }

    if (editingSection === 'talents') {
      nextDetails.primaryCraft = primaryCraft
      nextDetails.subCraft = selectedSubCraft
      nextDetails.secondaryCrafts = selectedTalents
      nextDetails.talents = [...(primaryCraft ? [primaryCraft] : []), ...selectedTalents]
    } else if (editingSection === 'workTypes') {
      nextDetails.workTypes = selectedWorkTypes
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

    setProfileDetails(nextDetails)
    try {
      await api.updateProfile(nextDetails)
    } catch (err) {
      console.warn('Failed to save profile section to backend:', err)
    }

    setEditingSection('')
  }

  function handleEditProfile() {
    setEditingSection('')
    setSelectedLanguages(profileDetails.languages)
    setOtherLanguageDraft(profileDetails.otherLanguage)
    setSelectedTalents(getSecondaryCrafts())
    setSecondaryCraftToAdd('')
    setSavedVideoUrls(profileDetails.videoUrls)
    setVideoUrlInput('')
    setVideoUrlError('')
    setDateOfBirthDraft(profileDetails.dateOfBirth || '')
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

      const experience = {
        id: editingExperienceId ?? `${Date.now()}`,
        projectName: formData.get('projectName')?.toString().trim() || '',
        organizationName: formData.get('organizationName')?.toString().trim() || '',
        location: formData.get('location')?.toString().trim() || '',
        workedAs: formData.get('workedAs')?.toString().trim() || '',
        startDate,
        endDate,
        highlights: formData.get('highlights')?.toString().trim() || '',
        attachment,
      }

      const updatedExperiences = editingExperienceId
        ? (profileDetails.experiences || []).map((item) => (
          item.id === editingExperienceId ? experience : item
        ))
        : [...(profileDetails.experiences || []), experience]

      const nextDetails = { ...profileDetails, experiences: updatedExperiences }
      await api.updateProfile(nextDetails)
      setProfileDetails(nextDetails)
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

    const updatedExperiences = (profileDetails.experiences || []).filter(
      (experience) => experience.id !== editingExperienceId,
    )

    const nextDetails = { ...profileDetails, experiences: updatedExperiences }
    setProfileDetails(nextDetails)
    try {
      await api.updateProfile(nextDetails)
    } catch (err) {
      console.warn('Failed to delete experience from backend:', err)
    }

    setIsExperienceModalOpen(false)
    setEditingExperienceId(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setValidationError('')
    setIsSubmitted(false)

    const form = event.currentTarget
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

    const formData = new FormData(form)
    let profileImageUrl = isProfileImageRemoved ? '' : profileDetails.profileImageUrl || ''

    if (profilePic) {
      try {
        const uploaded = await api.uploadMedia(profilePic)
        profileImageUrl = uploaded.url
        setProfilePreview(uploaded.url)
      } catch (err) {
        console.warn('Profile picture upload note:', err)
      }
    }

    const nextDetails = {
      ...profileDetails,
      userId: profileDetails.userId || 'me',
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
      skinTone: formData.get('skinTone') || '',
      address: formData.get('address') || '',
      languages: selectedLanguages,
      otherLanguage: selectedLanguages.includes('Other') ? otherLanguageDraft.trim() : '',
      profileImageUrl,
    }

    setProfileDetails(nextDetails)
    setIsProfileImageRemoved(false)
    try {
      await api.updateProfile(nextDetails)
    } catch (err) {
      console.warn('Failed to save profile to backend:', err)
    }

    setIsEditingProfile(false)
  }

  return (
    <div className="profile-page-content">
        <section className="artist-profile" id="artist-profile" aria-labelledby="preview-title">
          <div className="artist-cover has-default-banner" />
          <div className="artist-identity">
            <div
              className={`artist-avatar-progress${isOwner ? '' : ' is-readonly'}`}
              style={{ '--profile-completion': `${profileCompletion}%` }}
              role={isOwner ? 'img' : undefined}
              aria-label={isOwner ? `Profile ${profileCompletion}% complete` : undefined}
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
              {isOwner && <span className="artist-avatar-progress-label">{profileCompletion}%</span>}
            </div>
            <div className="artist-identity-content">
              <h2 id="preview-title">{profileDetails.name || 'Your name'}</h2>
              <p className="artist-headline">
                {profileDetails.talents.length > 0
                  ? profileDetails.talents.join(' · ')
                  : 'Add your talents in Edit profile'}
              </p>
              <p className="artist-location">
                {profileDetails.address || 'Add your location in Edit profile'}
              </p>
              <p className="artist-experience">
                {profileDetails.experience
                  ? (profileDetails.experience.toLowerCase() === 'fresher'
                    ? profileDetails.experience
                    : profileDetails.experience.toLowerCase().includes('year')
                    ? profileDetails.experience
                    : `${profileDetails.experience} years of experience`)
                  : 'Add your experience in Edit profile'}
              </p>
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
              {!isOwner && (
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
              {isOwner && (
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
              <section className="artist-section">
                <PreviewSectionHeading isOwner={isOwner} onEdit={openSectionEditor} sectionId="workTypes">
                  Worked in
                </PreviewSectionHeading>
                {(profileDetails.workTypes || []).length > 0 ? (
                  <div className="profile-tags">
                    {(profileDetails.workTypes || []).map((workType) => <span key={workType}>{workType}</span>)}
                  </div>
                ) : (
                  <p className="profile-empty-hint">
                    {isOwner ? 'Select Edit to add your previous work types.' : 'No previous work types added yet.'}
                  </p>
                )}
                {editingSection === 'workTypes' && (
                  <div className="preview-inline-editor">
                    <SelectionGroup
                      label="Previous Worked in"
                      options={workTypes}
                      selected={selectedWorkTypes}
                      onChange={setSelectedWorkTypes}
                    />
                    <div className="language-edit-actions">
                      <button className="secondary-button" type="button" onClick={closeSectionEditor}>Cancel</button>
                      <button className="contact-button" type="button" onClick={saveSectionEditor}>Save</button>
                    </div>
                  </div>
                )}
              </section>
              <section className="artist-section insights-section" id="artist-highlights">
                <h3>Profile highlights</h3>
                <div className="insights-grid">
                  <div className="insight-item">
                    <span className="insight-icon" aria-hidden="true">✦</span>
                    <div><strong>{profileDetails.experience ? (profileDetails.experience.toLowerCase() === 'fresher' || profileDetails.experience.toLowerCase().includes('year') ? profileDetails.experience : `${profileDetails.experience} years`) : '—'}</strong><span>Experience</span></div>
                  </div>
                  <div className="insight-item">
                    <span className="insight-icon" aria-hidden="true">◎</span>
                    <div><strong>{profileDetails.languages.length || '—'}</strong><span>Languages</span></div>
                  </div>
                  <div className="insight-item">
                    <span className="insight-icon" aria-hidden="true">▶</span>
                    <div><strong>{profileDetails.videos.length || '—'}</strong><span>Demo videos</span></div>
                  </div>
                </div>
              </section>

              <section className="artist-section">
                <PreviewSectionHeading isOwner={isOwner} onEdit={openSectionEditor} sectionId="talents">
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
                  <div>
                    <strong>Secondary Sub Craft</strong>
                    {profileDetails.secondarySubCrafts?.length > 0
                      ? <div className="profile-tags">{profileDetails.secondarySubCrafts.map((item) => <span key={item}>{item}</span>)}</div>
                      : <p className="profile-empty-hint">No secondary sub crafts added yet.</p>}
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

              <section className="artist-section" id="artist-languages">
                <PreviewSectionHeading isOwner={isOwner} onEdit={openSectionEditor} sectionId="languages">
                  Languages
                </PreviewSectionHeading>
                {profileDetails.languages.length > 0 ? (
                  <div className="profile-tags">
                    {profileDetails.languages.map((language) => (
                      <span key={language}>
                        {language === 'Other' && profileDetails.otherLanguage
                          ? `Other: ${profileDetails.otherLanguage}`
                          : language}
                      </span>
                    ))}
                  </div>
                ) : (
                  <p className="profile-empty-hint">
                    {isOwner ? 'Add languages in Edit profile.' : 'No languages added yet.'}
                  </p>
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
                    <div className="language-edit-actions">
                      <button className="secondary-button" type="button" onClick={closeSectionEditor}>Cancel</button>
                      <button className="contact-button" type="button" onClick={saveSectionEditor}>Save</button>
                    </div>
                  </div>
                )}
              </section>

              <section className="artist-section" id="artist-demo-videos">
                <PreviewSectionHeading isOwner={isOwner} onEdit={openSectionEditor} sectionId="videos">
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
                </div> : <p className="profile-empty-hint">Select Edit to add demo videos.</p>}
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
                  {isOwner && (
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
                    {profileDetails.experiences.map((experience) => (
                      <article className="experience-entry" key={experience.id}>
                        <div className="experience-entry-heading">
                          <div>
                            <h4>{experience.projectName || experience.movieName}</h4>
                            <p>
                              {experience.workedAs}
                              {experience.organizationName && ` · ${experience.organizationName}`}
                            </p>
                          </div>
                          {isOwner && (
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
                    ))}
                  </div>
                ) : (
                  <p className="profile-empty-hint">
                    {isOwner ? 'Add a movie or project to your experience.' : 'No experience added yet.'}
                  </p>
                )}
              </section>
              <section className="artist-section" id="artist-video-links">
                <PreviewSectionHeading isOwner={isOwner} onEdit={openSectionEditor} sectionId="videoLinks">
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
                <label className="form-field">
                  Organization name
                  <input
                    name="organizationName"
                    type="text"
                    defaultValue={experienceBeingEdited?.organizationName || ''}
                    placeholder="Enter production or organization"
                    required
                  />
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
                <h2 id="reference-modal-title">{isOwner ? 'Your contact details' : 'Professional phone access'}</h2>
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
            {isOwner ? (
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
        hidden={!isOwner || !isEditingProfile}
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
                      style={{ width: '58px', height: '58px', borderRadius: '50%' }}
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
                  type="text"
                  value={accountUserId}
                  readOnly
                  aria-readonly="true"
                  placeholder="Your TrueHunt ID is assigned at registration"
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
                  defaultValue={profileDetails.gender}
                >
                  <option value="">Select gender</option>
                  <option value="Female">Female</option>
                  <option value="Male">Male</option>
                  <option value="Non-binary">Non-binary</option>
                  <option value="Other">Other</option>
                  <option value="Prefer not to say">Prefer not to say</option>
                </select>
              </div>
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
                  <option value="Light brown">Light brown</option>
                  <option value="Dark">Dark</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="candidate-address">Candidate address</label>
                <textarea
                  id="candidate-address"
                  name="address"
                  defaultValue={profileDetails.address}
                  placeholder="Street address, city, state, postal code"
                  rows="3"
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
          <button className="submit-button" type="submit">Submit</button>
        </footer>
      </form>
    </div>
  )
}

export default ProfilePage
