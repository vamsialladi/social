import { useEffect, useRef, useState } from 'react'
import './StylePage.css'

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
}

const MAX_VIDEO_SIZE = 5 * 1024 * 1024

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
  'Acting',
  'Dancing',
  'Singing',
  'Direction',
  'Cinematography',
  'Writing',
  'Editing',
  'Music',
  'Production',
]

const workTypes = [
  'Cinema',
  'Web series',
  'Short films',
  'Reels',
  'Advertisements',
  'serials',
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

function ProfilePage({
  isOwner = true,
  profileDetails,
  setProfileDetails,
  profilePreview,
  setProfilePreview,
}) {
  const [profilePic, setProfilePic] = useState(null)
  const [coverPreview, setCoverPreview] = useState('')
  const [selectedLanguages, setSelectedLanguages] = useState([])
  const [languageDraft, setLanguageDraft] = useState([])
  const [otherLanguageDraft, setOtherLanguageDraft] = useState('')
  const [isEditingLanguages, setIsEditingLanguages] = useState(false)
  const [selectedTalents, setSelectedTalents] = useState([])
  const [selectedWorkTypes, setSelectedWorkTypes] = useState([])
  const [uploadedVideos, setUploadedVideos] = useState([null, null, null])
  const uploadedVideoUrls = useRef([])
  const [videoUrlInput, setVideoUrlInput] = useState('')
  const [videoUrlError, setVideoUrlError] = useState('')
  const [savedVideoUrls, setSavedVideoUrls] = useState([])
  const [validationError, setValidationError] = useState('')
  const [isSubmitted, setIsSubmitted] = useState(false)
  const [isEditingProfile, setIsEditingProfile] = useState(false)
  const [isContactModalOpen, setIsContactModalOpen] = useState(false)
  const [referenceId, setReferenceId] = useState('')
  const [submittedReferenceId, setSubmittedReferenceId] = useState('')
  useEffect(() => {
    return () => {
      if (profilePreview) URL.revokeObjectURL(profilePreview)
    }
  }, [profilePreview])

  useEffect(() => {
    return () => {
      if (coverPreview) URL.revokeObjectURL(coverPreview)
    }
  }, [coverPreview])

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

  function handleCoverChange(event) {
    const file = event.target.files?.[0]
    if (file) setCoverPreview(URL.createObjectURL(file))
  }

  function handleVideoChange(event, index) {
    const file = event.target.files?.[0] ?? null
    const previousUrl = uploadedVideoUrls.current[index]

    if (previousUrl) URL.revokeObjectURL(previousUrl)

    const video = file ? { file, url: URL.createObjectURL(file) } : null
    uploadedVideoUrls.current[index] = video?.url ?? ''
    setUploadedVideos((videos) => {
      const nextVideos = [...videos]
      nextVideos[index] = video
      return nextVideos
    })
    setIsSubmitted(false)
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

  function handleEditLanguages() {
    setLanguageDraft(profileDetails.languages)
    setOtherLanguageDraft(profileDetails.otherLanguage)
    setIsEditingLanguages(true)
  }

  function handleSaveLanguages() {
    setProfileDetails((details) => ({
      ...details,
      languages: languageDraft,
      otherLanguage: languageDraft.includes('Other') ? otherLanguageDraft.trim() : '',
    }))
    setSelectedLanguages(languageDraft)
    setIsEditingLanguages(false)
  }

  function handleReferenceSubmit(event) {
    event.preventDefault()
    setSubmittedReferenceId(referenceId.trim())
  }

  function handleSubmit(event) {
    event.preventDefault()
    setValidationError('')
    setIsSubmitted(false)

    const form = event.currentTarget
    const videoInputs = Array.from(form.querySelectorAll('input[type="file"][data-video]'))
    const oversizedVideoIndex = uploadedVideos.findIndex(
      (video) => video && video.file.size > MAX_VIDEO_SIZE,
    )

    if (oversizedVideoIndex !== -1) {
      videoInputs[oversizedVideoIndex].focus()
      setValidationError('Each video must be no larger than 5 MB. Please choose a smaller video file.')
      return
    }

    const formData = new FormData(form)
    setProfileDetails({
      userId: formData.get('userId')?.trim() || '',
      name: formData.get('name') || '',
      phone: formData.get('phone')?.trim() || '',
      email: formData.get('email')?.trim() || '',
      experience: formData.get('experience') || '',
      address: formData.get('address') || '',
      languages: selectedLanguages,
      otherLanguage: profileDetails.otherLanguage,
      talents: selectedTalents,
      workTypes: selectedWorkTypes,
      videos: uploadedVideos.filter(Boolean),
      videoUrls: savedVideoUrls,
    })
    setIsEditingProfile(false)
  }

  return (
    <div className="profile-page-content">
        <section className="artist-profile" id="artist-profile" aria-labelledby="preview-title">
          <div
            className={`artist-cover ${coverPreview ? 'has-cover-image' : 'has-default-banner'}`}
            style={{
              backgroundImage: coverPreview
                ? `linear-gradient(rgba(37, 25, 25, 0.12), rgba(37, 25, 25, 0.12)), url("${coverPreview}")`
                : 'url("/Logo.png")',
            }}
          >
          </div>

          <div className="artist-identity">
            {profilePreview
              ? <img className="artist-avatar" src={profilePreview} alt={`${profileDetails.name || 'Artist'}'s profile`} />
              : <div className="artist-avatar avatar-fallback" aria-hidden="true">{profileDetails.name?.charAt(0).toUpperCase() || '?'}</div>}
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
                  onClick={() => setIsEditingProfile(true)}
                >
                  <span aria-hidden="true">✎</span> Edit
                </button>
              )}
            </div>
          </div>

          <div className="artist-content">
            <div className="artist-main">
              <aside className="work-status-card">
                <span className="status-dot" aria-hidden="true" />
                <div>
                  <strong>Worked in</strong>
                  <p>{profileDetails.workTypes.length > 0 ? profileDetails.workTypes.join(' · ') : 'Add your previous work in Edit profile'}</p>
                </div>
              </aside>

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
                <h3>Talents</h3>
                {profileDetails.talents.length > 0
                  ? <div className="profile-tags">{profileDetails.talents.map((talent) => <span key={talent}>{talent}</span>)}</div>
                  : <p className="profile-empty-hint">Add your talents in Edit profile.</p>}
              </section>

              <section className="artist-section" id="artist-languages">
                <div className="language-section-heading">
                  <h3>Languages</h3>
                  {isOwner && !isEditingLanguages && (
                    <button className="language-edit-button" type="button" onClick={handleEditLanguages}>
                      Edit
                    </button>
                  )}
                </div>
                {isOwner && isEditingLanguages ? (
                  <>
                    <SelectionGroup
                      label="Languages you know"
                      options={languages}
                      selected={languageDraft}
                      onChange={setLanguageDraft}
                    />
                    {languageDraft.includes('Other') && (
                      <div className="language-other-field">
                        <label htmlFor="other-language">Specify your language</label>
                        <input
                          id="other-language"
                          type="text"
                          value={otherLanguageDraft}
                          onChange={(event) => setOtherLanguageDraft(event.target.value)}
                          placeholder="Enter another language"
                        />
                      </div>
                    )}
                    <div className="language-edit-actions">
                      <button
                        className="secondary-button"
                        type="button"
                        onClick={() => setIsEditingLanguages(false)}
                      >
                        Cancel
                      </button>
                      <button className="contact-button" type="button" onClick={handleSaveLanguages}>
                        Save
                      </button>
                    </div>
                  </>
                ) : profileDetails.languages.length > 0 ? (
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
                    {isOwner ? 'Select Edit to add your languages.' : 'No languages added yet.'}
                  </p>
                )}
              </section>

              <section className="artist-section" id="artist-demo-videos">
                <h3>Demo videos</h3>
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
                </div> : <p className="profile-empty-hint">Add your demo videos in Edit profile.</p>}
              </section>
              {profileDetails.videoUrls.length > 0 && (
                <section className="artist-section" id="artist-video-links">
                  <h3>Video links</h3>
                  <div className="artist-links">
                    {profileDetails.videoUrls.map((url, index) => (
                      <a href={url} target="_blank" rel="noreferrer" key={`${url}-${index}`}>
                        Video {index + 1}: {url}
                      </a>
                    ))}
                  </div>
                </section>
              )}
            </div>

            <aside className="artist-sidebar" id="artist-work">
              <section className="sidebar-card">
                <span className="sidebar-eyebrow">WORK EXPERIENCE</span>
                <h3>Previous work</h3>
                <p>{profileDetails.workTypes.length > 0 ? 'Has worked in these formats:' : 'Add your previous work in Edit profile.'}</p>
                {profileDetails.workTypes.length > 0 && <div className="profile-tags">
                  {profileDetails.workTypes.map((workType) => <span key={workType}>{workType}</span>)}
                </div>}
                <div className="sidebar-location">
                  <span aria-hidden="true">⌖</span>
                  <p>{profileDetails.address || 'Add your location in Edit profile'}</p>
                </div>
              </section>
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
                  <button className="home-promotion-button" type="button">Explore film careers</button>
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
                <input id="full-name" name="name" type="text" placeholder="e.g. Arjun Kumar" />
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
                  placeholder="Street address, city, state, postal code"
                  rows="3"
                />
              </div>
            </div>
          </div>
          <div className="video-url-section">
            <div className="video-url-heading">
              <div>
                <h3>Video URLs <span className="optional-label">Optional</span></h3>
                <p>Paste a video link and click Add URL. You can add up to three links.</p>
              </div>
            </div>
            <div className="video-url-entry">
              <div className="form-field">
                <label htmlFor="video-url-input">Video URL</label>
                <input
                  id="video-url-input"
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
            {savedVideoUrls.length > 0 && (
              <div className="saved-video-urls" aria-live="polite">
                <strong>Saved temporarily ({savedVideoUrls.length})</strong>
                {savedVideoUrls.map((url, index) => (
                  <div className="saved-video-url" key={`${url}-${index}`}>
                    <a href={url} target="_blank" rel="noreferrer">
                      <span aria-hidden="true">▶</span> Video {index + 1} · {url}
                    </a>
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
              </div>
            )}
          </div>
        </section>

        <section className="form-section">
          <div className="section-heading">
            <span className="section-number">02</span>
            <div>
              <h2>Your skills &amp; interests</h2>
              <p>Select all talents and work types that apply.</p>
            </div>
          </div>
          <SelectionGroup
            label="Your talents"
            options={talents}
            selected={selectedTalents}
            onChange={setSelectedTalents}
          />
          <SelectionGroup
            label="Worked in"
            options={workTypes}
            selected={selectedWorkTypes}
            onChange={setSelectedWorkTypes}
          />
        </section>

        <section className="form-section video-section">
          <div className="section-heading">
            <span className="section-number">03</span>
            <div>
              <h2>Show us what you can do</h2>
              <p>Upload up to three videos so casting teams can see you in action.</p>
            </div>
          </div>
          <div className="video-grid">
            {[1, 2, 3].map((number) => (
              <div className="video-upload-card" key={number}>
                <label htmlFor={`video-${number}`}>
                  <span className="video-icon" aria-hidden="true">▶</span>
                  <span className="video-label">Video {number}</span>
                  <span className="video-prompt">Choose a video to upload</span>
                </label>
                <input
                  id={`video-${number}`}
                  name={`video-${number}`}
                  type="file"
                  accept="video/*"
                  data-video
                  onChange={(event) => handleVideoChange(event, number - 1)}
                />
                {uploadedVideos[number - 1] && (
                  <video
                    className="video-upload-preview"
                    controls
                    playsInline
                    preload="metadata"
                    src={uploadedVideos[number - 1].url}
                    aria-label={`Preview ${uploadedVideos[number - 1].file.name}`}
                  />
                )}
                <span className="field-hint">Video file · 5 MB maximum</span>
              </div>
            ))}
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
