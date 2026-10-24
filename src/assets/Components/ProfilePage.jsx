import { useEffect, useRef, useState } from 'react'
import './StylePage.css'
import api from '../../api'

export const emptyProfileDetails = {
  userId: '',
  name: '',
  phone: '',
  email: '',
  experience: '',
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

const talents = [
  'Acting', 'Direction', 'Cinematography',' Editing', 'Screenwriting',
  'Art Direction', 'Sound Design', 'Music', 'Dance & Choreography', 'Production Management', 'Makeup & Costume', 'VFX & Animation',
]

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
  profileDetails,
  setProfileDetails,
  profilePreview,
  setProfilePreview,
}) {
  const [profilePic, setProfilePic] = useState(null)
  const [selectedLanguages, setSelectedLanguages] = useState([])
  const [otherLanguageDraft, setOtherLanguageDraft] = useState('')
  const [selectedTalents, setSelectedTalents] = useState([])
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
  const [isExperienceModalOpen, setIsExperienceModalOpen] = useState(false)
  const [editingExperienceId, setEditingExperienceId] = useState(null)
  const [isContactModalOpen, setIsContactModalOpen] = useState(false)
  const [referenceId, setReferenceId] = useState('')
  const [submittedReferenceId, setSubmittedReferenceId] = useState('')
  const [editingSection, setEditingSection] = useState('')
  const experienceBeingEdited = (profileDetails.experiences || [])
    .find((experience) => experience.id === editingExperienceId)

  useEffect(() => {
    if (!isExperienceModalOpen) return undefined

    function handleExperienceModalKeyDown(event) {
      if (event.key === 'Escape') setIsExperienceModalOpen(false)
    }

    document.addEventListener('keydown', handleExperienceModalKeyDown)
    return () => document.removeEventListener('keydown', handleExperienceModalKeyDown)
  }, [isExperienceModalOpen])

  useEffect(() => {
    return () => {
      if (profilePreview) URL.revokeObjectURL(profilePreview)
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
    setProfilePic(file)
    setProfilePreview(file ? URL.createObjectURL(file) : '')
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
    setSelectedTalents(profileDetails.talents)
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
  }

  async function saveSectionEditor() {
    let nextDetails = { ...profileDetails }

    if (editingSection === 'talents') {
      nextDetails.talents = selectedTalents
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
    setSelectedTalents(profileDetails.talents)
    setSavedVideoUrls(profileDetails.videoUrls)
    setVideoUrlInput('')
    setVideoUrlError('')
    setProfileFormKey((key) => key + 1)
    setIsEditingProfile(true)
  }

  async function handleReferenceSubmit(event) {
    event.preventDefault()
    const trimmed = referenceId.trim()
    setSubmittedReferenceId(trimmed)
    try {
      await api.verifyReference(trimmed)
    } catch (err) {
      console.warn('Reference verify API note:', err)
    }
  }

  function openExperienceModal(experience = null) {
    setEditingExperienceId(experience?.id ?? null)
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

    const experience = {
      id: editingExperienceId ?? `${Date.now()}`,
      projectName: formData.get('projectName')?.toString().trim() || '',
      organizationName: formData.get('organizationName')?.toString().trim() || '',
      location: formData.get('location')?.toString().trim() || '',
      workedAs: formData.get('workedAs')?.toString().trim() || '',
      startDate,
      endDate,
      highlights: formData.get('highlights')?.toString().trim() || '',
    }

    const updatedExperiences = editingExperienceId
      ? (profileDetails.experiences || []).map((item) => (
        item.id === editingExperienceId ? experience : item
      ))
      : [...(profileDetails.experiences || []), experience]

    const nextDetails = { ...profileDetails, experiences: updatedExperiences }
    setProfileDetails(nextDetails)
    try {
      await api.updateProfile(nextDetails)
    } catch (err) {
      console.warn('Failed to save experience to backend:', err)
    }

    setIsExperienceModalOpen(false)
    setEditingExperienceId(null)
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
    let profileImageUrl = profileDetails.profileImageUrl || ''

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
      userId: formData.get('userId')?.trim() || profileDetails.userId || 'me',
      name: formData.get('name') || '',
      phone: formData.get('phone')?.trim() || '',
      email: formData.get('email')?.trim() || '',
      experience: formData.get('experience') || '',
      address: formData.get('address') || '',
      languages: selectedLanguages,
      otherLanguage: selectedLanguages.includes('Other') ? otherLanguageDraft.trim() : '',
      profileImageUrl,
    }

    setProfileDetails(nextDetails)
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
            {profilePreview
              ? <img className="artist-avatar" src={profilePreview} alt={`${profileDetails.name || 'Artist'}'s profile`} />
              : (
                <div className="artist-avatar avatar-fallback" aria-hidden="true">
                  <svg className="profile-avatar-placeholder" viewBox="0 0 100 100" focusable="false">
                    <circle cx="50" cy="35" r="21" />
                    <path d="M10 96c2-23 18-39 40-39s38 16 40 39H10Z" />
                  </svg>
                </div>
              )}
            <div className="artist-identity-content">
              <h2 id="preview-title">{profileDetails.name || 'Your name'}</h2>
              <p className="artist-headline">
                {profileDetails.talents.length > 0 ? profileDetails.talents.join(' · ') : 'Add your talents in Edit profile'}
              </p>
              <p className="artist-location">{profileDetails.address || 'Add your location in Edit profile'}</p>
              <p className="artist-experience">
                {profileDetails.experience ? `${profileDetails.experience} years of experience` : 'Add your experience in Edit profile'}
              </p>
              {isSubmitted && <p className="profile-confirmation" role="status">Your profile preview is confirmed.</p>}
            </div>
            <div className="identity-actions">
              <button
                className="identity-contact-button contact-button"
                type="button"
                onClick={() => {
                  setReferenceId('')
                  setSubmittedReferenceId('')
                  setIsContactModalOpen(true)
                }}
              >
                Contact
              </button>
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
                    <div><strong>{profileDetails.experience ? `${profileDetails.experience} years` : '—'}</strong><span>Experience</span></div>
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
                  Talents
                </PreviewSectionHeading>
                {profileDetails.talents.length > 0
                  ? <div className="profile-tags">{profileDetails.talents.map((talent) => <span key={talent}>{talent}</span>)}</div>
                  : <p className="profile-empty-hint">Select Edit to add your talents.</p>}
                {editingSection === 'talents' && (
                  <div className="preview-inline-editor">
                    <SelectionGroup
                      label="Your talents"
                      options={talents}
                      selected={selectedTalents}
                      onChange={setSelectedTalents}
                    />
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
                  {profileDetails.videos.map((video, index) => (
                    <div className="demo-video-item" key={`${video}-${index}`}>
                      <video
                        controls
                        playsInline
                        preload="metadata"
                        src={video.url}
                        aria-label={`Play demo video ${index + 1}: ${video.file.name}`}
                      />
                      <div><strong>Demo video {index + 1}</strong><span>{video.file.name}</span></div>
                    </div>
                  ))}
                </div> : <p className="profile-empty-hint">Select Edit to add demo videos.</p>}
                {editingSection === 'videos' && (
                  <div className="preview-inline-editor">
                    {profileDetails.videos.map((video, index) => (
                      <div className="inline-video-existing" key={`${video.url}-${index}`}>
                        <span>{video.file.name}</span>
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
          onClick={() => setIsEditingProfile(false)}
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
                  >
                    Cancel
                  </button>
                  <button className="contact-button" type="submit">
                    Save experience
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
            {/* Reference ID form */}
            <div className="reference-modal-heading">
              <div>
                <span className="eyebrow">CONTACT ARTIST</span>
                <h2 id="reference-modal-title">Enter your reference code</h2>
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
            {submittedReferenceId ? (
              <div className="reference-modal-success" role="status">
                <p>Reference ID submitted: <strong>{submittedReferenceId}</strong></p>
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
              <form className="reference-form" onSubmit={handleReferenceSubmit}>
                <label htmlFor="reference-id">Reference code / Reference ID</label>
                <input
                  id="reference-id"
                  name="referenceId"
                  type="text"
                  value={referenceId}
                  onChange={(event) => setReferenceId(event.target.value)}
                  placeholder="Enter your reference code or ID"
                  autoComplete="off"
                  required
                />
                <p>Enter the code or ID you were given to contact this artist.</p>
                <div className="reference-form-actions">
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => setIsContactModalOpen(false)}
                  >
                    Cancel
                  </button>
                  <button className="contact-button" type="submit">
                    Submit <span aria-hidden="true">→</span>
                  </button>
                </div>
              </form>
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
              onClick={() => setIsEditingProfile(false)}
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
              <label className="photo-upload" htmlFor="profile-pic">
                {profilePreview ? (
                  <img src={profilePreview} alt="Profile preview" />
                ) : (
                  <span className="photo-placeholder" aria-hidden="true">+</span>
                )}
                <span className="photo-upload-text">{profilePic ? 'Change photo' : 'Upload photo'}</span>
                <input
                  id="profile-pic"
                  type="file"
                  accept="image/*"
                  onChange={handleProfileChange}
                />
              </label>
              <span className="field-hint">JPG or PNG, preferably a clear headshot</span>
            </div>

            <div className="field-stack">
              <div className="form-field">
                <label htmlFor="user-id">User ID</label>
                <input
                  id="user-id"
                  name="userId"
                  type="text"
                  autoComplete="username"
                  defaultValue={profileDetails.userId}
                  placeholder="Enter your user ID"
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
                <label htmlFor="phone">Phone number</label>
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  autoComplete="tel"
                  defaultValue={profileDetails.phone}
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
                <div className="experience-input">
                  <input
                    id="experience"
                    name="experience"
                    type="number"
                    min="0"
                    step="1"
                    defaultValue={profileDetails.experience}
                    placeholder="0"
                  />
                  <span>years</span>
                </div>
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
