import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, Camera, Heart, Pencil, Trash2, UserRound, X } from 'lucide-react'
import { api } from '../../api'
import './CastingPage.css'

const MAX_CASTING_PHOTO_BYTES = 5 * 1024 * 1024
const MAX_CASTING_PHOTOS_BYTES = 12 * 1024 * 1024

const pageStyle = {
  minHeight: 'calc(100vh - 70px)',
  padding: '32px 20px',
  background: '#f4f6f8',
  color: '#202124',
}

const cardStyle = {
  width: 'min(760px, 100%)',
  margin: '0 auto',
  padding: '32px',
  border: '1px solid #e3e7ec',
  borderRadius: '16px',
  background: '#fff',
  boxShadow: '0 8px 28px rgba(27, 39, 51, 0.08)',
  boxSizing: 'border-box',
}

const fieldStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: '8px',
  minWidth: 0,
}

const labelStyle = { color: '#303843', fontSize: '14px', fontWeight: 600 }

const inputStyle = {
  width: '100%',
  minHeight: '44px',
  padding: '10px 12px',
  border: '1px solid #cfd6de',
  borderRadius: '8px',
  background: '#fff',
  color: '#202124',
  font: 'inherit',
  boxSizing: 'border-box',
}

const backButtonStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: '7px',
  padding: 0,
  border: 0,
  background: 'none',
  color: '#586270',
  font: 'inherit',
  cursor: 'pointer',
}

const eyebrowStyle = {
  margin: 0,
  color: '#8b2332',
  fontSize: '12px',
  fontWeight: 700,
  letterSpacing: '0.1em',
}

const titleStyle = {
  margin: '4px 0 0',
  color: '#202124',
  fontSize: 'clamp(22px, 4vw, 30px)',
  lineHeight: 1.2,
}

const iconStyle = {
  display: 'grid',
  width: '48px',
  height: '48px',
  placeItems: 'center',
  borderRadius: '12px',
  background: '#f8ecee',
  color: '#8b2332',
  flexShrink: 0,
}

const formGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))',
  gap: '16px',
}

const uploadStyle = {
  position: 'relative',
  display: 'flex',
  minHeight: '52px',
  alignItems: 'center',
  justifyContent: 'center',
  gap: '9px',
  border: '1px dashed #aeb8c4',
  borderRadius: '9px',
  color: '#34404d',
  fontSize: '14px',
  fontWeight: 600,
  cursor: 'pointer',
}

const photoGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(100px, 1fr))',
  gap: '10px',
}

const photoCardStyle = {
  position: 'relative',
  overflow: 'hidden',
  aspectRatio: '1',
  borderRadius: '8px',
  background: '#edf0f3',
}

const photoImageStyle = { width: '100%', height: '100%', objectFit: 'cover' }

const removePhotoStyle = {
  position: 'absolute',
  top: '5px',
  right: '5px',
  display: 'grid',
  width: '26px',
  height: '26px',
  placeItems: 'center',
  border: 0,
  borderRadius: '50%',
  background: 'rgba(0, 0, 0, 0.68)',
  color: '#fff',
  cursor: 'pointer',
}

const errorStyle = { margin: 0, color: '#b42318', fontSize: '13px' }

const submitButtonStyle = {
  minHeight: '46px',
  border: 0,
  borderRadius: '9px',
  background: '#8b2332',
  color: '#fff',
  font: 'inherit',
  fontWeight: 700,
  cursor: 'pointer',
}

const talentGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 300px), 1fr))',
  gap: '16px',
  marginTop: '22px',
}

const talentCardStyle = {
  padding: '20px',
  border: '1px solid #e3e7ec',
  borderRadius: '12px',
  background: '#fff',
}

const talentPhotoGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(72px, 1fr))',
  gap: '8px',
  marginTop: '14px',
}

const deleteButtonStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: '6px',
  padding: '7px 10px',
  border: '1px solid #d6a5a9',
  borderRadius: '7px',
  background: '#fff',
  color: '#a3212d',
  font: 'inherit',
  fontSize: '13px',
  fontWeight: 600,
  cursor: 'pointer',
}

function Field({ label, children }) {
  return (
    <label style={fieldStyle}>
      <span style={labelStyle}>{label}</span>
      {children}
    </label>
  )
}

function mapCastingProfile(post, currentUserIdentity, currentUserAvatar) {
  const details = post.opportunityDetails || {}
  const ownerProfileId = String(post.authorId || '').trim()
  const ownerId = ownerProfileId.toLowerCase()
  const savedAvatar = post.authorAvatar || post.authorProfileImage || ''
  return {
    id: String(post.id),
    ownerProfileId,
    ownerId,
    ownerAvatar: ownerId === currentUserIdentity
      ? currentUserAvatar || savedAvatar
      : savedAvatar,
    fullName: details.fullName || post.title || 'Casting Talent',
    age: details.age || '',
    gender: details.gender || '',
    city: details.city || '',
    height: details.height || '',
    experience: details.experience || post.content || '',
    portfolio: details.portfolio || '',
    previousWork: details.previousWork || '',
    photos: Array.isArray(details.photos) ? details.photos : [],
  }
}

function formatCastingHeight(height) {
  const rawHeight = String(height || '').trim()
  const centimeters = Number(rawHeight)
  if (!rawHeight || !Number.isFinite(centimeters) || centimeters <= 0) return rawHeight

  const totalInches = Math.round(centimeters / 2.54)
  const feet = Math.floor(totalInches / 12)
  const inches = totalInches % 12
  return `${feet} ft ${inches} in (${Math.round(centimeters)} cm)`
}

function getCastingHeightInCm(height) {
  const rawHeight = String(height || '').trim()
  const centimeters = Number(rawHeight.replace(/\s*cm$/i, ''))
  if (Number.isFinite(centimeters) && centimeters > 0) return String(Math.round(centimeters))

  const feetAndInches = rawHeight.match(/^\s*(\d+)\s*(?:ft|feet|')\s*(?:(\d+)\s*(?:in|inches|")?)?\s*$/i)
  if (!feetAndInches) return ''

  const inches = (Number(feetAndInches[1]) * 12) + Number(feetAndInches[2] || 0)
  return String(Math.round(inches * 2.54))
}

export default function CastingPage({
  onBack,
  onProfileSelect,
  currentUserId,
  currentUserAvatar,
  isAuthenticated,
}) {
  const [photos, setPhotos] = useState([])
  const [heightCm, setHeightCm] = useState('')
  const photoUrlsRef = useRef([])
  const [photoError, setPhotoError] = useState('')
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [talents, setTalents] = useState([])
  const [editingTalent, setEditingTalent] = useState(null)
  const [interestedTalentIds, setInterestedTalentIds] = useState([])
  const [submittedMessage, setSubmittedMessage] = useState('')
  const [pageError, setPageError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingTalentId, setDeletingTalentId] = useState('')
  const ownerId = String(currentUserId || 'guest').trim().toLowerCase()

  useEffect(() => {
    if (!isAuthenticated) return undefined

    let isCurrent = true
    api.getCastingProfiles()
      .then((profiles) => {
        if (!Array.isArray(profiles)) {
          throw new Error('Casting profiles returned an invalid response.')
        }
        if (isCurrent) {
          setTalents(profiles.map((profile) => mapCastingProfile(profile, ownerId, currentUserAvatar)))
        }
      })
      .catch((error) => {
        console.error('Could not load casting profiles:', error)
        if (isCurrent) setPageError(error instanceof Error ? error.message : 'Could not load casting profiles.')
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })

    return () => {
      isCurrent = false
    }
  }, [currentUserAvatar, isAuthenticated, ownerId])

  useEffect(() => () => {
    photoUrlsRef.current.forEach((url) => URL.revokeObjectURL(url))
  }, [])

  function choosePhotos(event) {
    const selectedPhotos = Array.from(event.target.files || [])
    event.target.value = ''
    setPhotoError('')

    if (selectedPhotos.length + photos.length > 5) {
      setPhotoError('Please choose no more than 5 photographs.')
      return
    }

    if (selectedPhotos.some((file) => !file.type.startsWith('image/'))) {
      setPhotoError('Please select image files only.')
      return
    }

    if (selectedPhotos.some((file) => file.size > MAX_CASTING_PHOTO_BYTES)) {
      setPhotoError('Each photograph must be no larger than 5 MB.')
      return
    }

    const totalPhotoBytes = photos.reduce((total, photo) => total + (photo.file?.size || 0), 0)
      + selectedPhotos.reduce((total, file) => total + file.size, 0)
    if (totalPhotoBytes > MAX_CASTING_PHOTOS_BYTES) {
      setPhotoError('The combined size of your photographs must be no larger than 12 MB.')
      return
    }

    const addedPhotos = selectedPhotos.map((file) => ({ file, preview: URL.createObjectURL(file) }))
    photoUrlsRef.current.push(...addedPhotos.map((photo) => photo.preview))
    setPhotos((current) => [...current, ...addedPhotos])
  }

  function removePhoto(index) {
    const removedPhoto = photos[index]
    if (removedPhoto?.file) {
      URL.revokeObjectURL(removedPhoto.preview)
      photoUrlsRef.current = photoUrlsRef.current.filter((url) => url !== removedPhoto.preview)
    }
    setPhotos((current) => current.filter((_, photoIndex) => photoIndex !== index))
    setPhotoError('')
  }

  function cancelForm() {
    photos.forEach((photo) => {
      if (photo.file) URL.revokeObjectURL(photo.preview)
    })
    photoUrlsRef.current = photoUrlsRef.current.filter(
      (url) => !photos.some((photo) => photo.file && photo.preview === url),
    )
    setPhotos([])
    setHeightCm('')
    setPhotoError('')
    setEditingTalent(null)
    setIsFormOpen(false)
  }

  function startNewProfile() {
    setEditingTalent(null)
    setPhotos([])
    setHeightCm('')
    setPhotoError('')
    setPageError('')
    setSubmittedMessage('')
    setIsFormOpen(true)
  }

  function startEditingTalent(talent) {
    setEditingTalent(talent)
    setPhotos(talent.photos.map((url) => ({ file: null, preview: url, url })))
    setHeightCm(getCastingHeightInCm(talent.height))
    setPhotoError('')
    setPageError('')
    setSubmittedMessage('')
    setIsFormOpen(true)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (isSubmitting) return
    if (photos.length < 3 || photos.length > 5) {
      setPhotoError('Please add 3–5 recent clear photographs.')
      return
    }
    setPageError('')
    setPhotoError('')
    setIsSubmitting(true)
    try {
      const formData = new FormData(event.currentTarget)
      const uploadedPhotos = await Promise.all(photos.map(async ({ file, url }) => {
        if (!file) return url
        const uploaded = await api.uploadMedia(file, 'profile-image')
        if (!uploaded?.url || typeof uploaded.url !== 'string') {
          throw new Error('A photograph could not be uploaded. Please try again.')
        }
        return uploaded.url
      }))
      const profileDetails = {
        fullName: String(formData.get('fullName') || '').trim(),
        age: String(formData.get('age') || ''),
        gender: String(formData.get('gender') || ''),
        city: String(formData.get('city') || '').trim(),
        height: String(formData.get('height') || '').trim(),
        experience: String(formData.get('experience') || '').trim(),
        portfolio: String(formData.get('portfolio') || '').trim(),
        previousWork: String(formData.get('previousWork') || '').trim(),
        photos: uploadedPhotos,
      }
      const postChanges = {
        title: profileDetails.fullName,
        content: profileDetails.experience,
        opportunityDetails: profileDetails,
      }
      const savedPost = editingTalent
        ? await api.updatePost(editingTalent.id, postChanges)
        : await api.createCastingProfile({
        title: profileDetails.fullName,
        content: profileDetails.experience,
        opportunityDetails: profileDetails,
      })
      if (!savedPost?.id) {
        throw new Error('The casting profile was not saved. Please try again.')
      }
      const talent = mapCastingProfile(savedPost, ownerId, currentUserAvatar)
      setTalents((current) => [talent, ...current.filter((entry) => entry.id !== talent.id)])
      photos.forEach((photo) => {
        if (photo.file) URL.revokeObjectURL(photo.preview)
      })
      photoUrlsRef.current = photoUrlsRef.current.filter(
        (url) => !photos.some((photo) => photo.file && photo.preview === url),
      )
      setPhotos([])
      setHeightCm('')
      setEditingTalent(null)
      setSubmittedMessage(editingTalent
        ? `${talent.fullName}'s casting profile was updated.`
        : `${talent.fullName}'s casting talent profile was added to this page.`)
      setIsFormOpen(false)
    } catch (error) {
      console.error('Could not submit casting profile:', error)
      setPageError(error instanceof Error ? error.message : 'Could not submit casting profile. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  function openTalentOwnerProfile(talent) {
    if (!talent.ownerProfileId) return
    onProfileSelect?.({
      userId: talent.ownerProfileId,
      name: talent.fullName,
      profileImageUrl: talent.ownerAvatar,
    })
  }

  async function deleteTalent(talent) {
    if (talent.ownerId !== ownerId) return
    if (!window.confirm(`Delete ${talent.fullName}'s casting profile?`)) return

    setPageError('')
    setDeletingTalentId(talent.id)
    try {
      await api.deletePost(talent.id)
      setTalents((current) => current.filter((entry) => entry.id !== talent.id))
      setInterestedTalentIds((current) => current.filter((id) => id !== talent.id))
    } catch (error) {
      console.error('Could not delete casting profile:', error)
      setPageError(error instanceof Error ? error.message : 'Could not delete casting profile. Please try again.')
    } finally {
      setDeletingTalentId('')
    }
  }

  function toggleInterest(talentId) {
    const talent = talents.find((entry) => entry.id === talentId)
    if (!talent || talent.ownerId === ownerId) return

    setInterestedTalentIds((current) => current.includes(talentId)
      ? current.filter((id) => id !== talentId)
      : [...current, talentId])
  }

  if (!isAuthenticated) {
    return (
      <main className="casting-page" style={pageStyle}>
        <section className="casting-page-card" style={cardStyle} aria-labelledby="casting-sign-in-title">
          <button type="button" onClick={onBack} style={backButtonStyle}>
            <ArrowLeft size={17} aria-hidden="true" /> Back to social
          </button>
          <h1 id="casting-sign-in-title" style={{ ...titleStyle, marginTop: '24px' }}>Sign in to view Casting Talent</h1>
          <p style={{ color: '#586270', lineHeight: 1.6 }}>
            Casting profiles are available to signed-in users only.
          </p>
        </section>
      </main>
    )
  }

  return (
    <main className="casting-page" style={pageStyle}>
      <section className="casting-page-card" style={{ ...cardStyle, width: 'min(980px, 100%)' }} aria-labelledby="casting-page-title">
        {!isFormOpen ? (
          <>
            <button type="button" onClick={onBack} style={backButtonStyle}>
              <ArrowLeft size={17} aria-hidden="true" /> Back to social
            </button>
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '20px', marginTop: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                <span style={iconStyle}><Camera size={22} aria-hidden="true" /></span>
                <div>
                  <p style={eyebrowStyle}>CASTING</p>
                  <h1 id="casting-page-title" style={titleStyle}>Casting Talent</h1>
                </div>
              </div>
              <button
                className="casting-add-button"
                type="button"
                onClick={startNewProfile}
              >
                Add Your Casting Talent
              </button>
            </div>
            <p style={{ margin: '16px 0 0', color: '#586270', lineHeight: 1.6 }}>
              Discover talent profiles shared for casting consideration.
            </p>
            {submittedMessage && (
              <p role="status" style={{ margin: '16px 0 0', color: '#237547', fontSize: '14px' }}>
                {submittedMessage}
              </p>
            )}
            {pageError && <p role="alert" style={{ ...errorStyle, marginTop: '16px' }}>{pageError}</p>}
            {isLoading ? (
              <p style={{ marginTop: '24px', color: '#586270' }}>Loading casting profiles…</p>
            ) : talents.length === 0 ? (
              <div style={{ marginTop: '24px', padding: '28px 20px', border: '1px dashed #cfd6de', borderRadius: '12px', textAlign: 'center', color: '#586270' }}>
                No casting talent profiles yet. Add your profile to get started.
              </div>
            ) : (
              <div style={talentGridStyle}>
                {talents.map((talent) => (
                  <article key={talent.id} style={talentCardStyle}>
                    <div className="casting-card-header">
                      <div className="casting-talent-identity">
                        <button
                          type="button"
                          className="casting-profile-avatar casting-profile-link"
                          onClick={() => openTalentOwnerProfile(talent)}
                          disabled={!onProfileSelect || !talent.ownerProfileId}
                          aria-label={`View ${talent.fullName}'s profile`}
                        >
                          {talent.ownerAvatar
                            ? <img src={talent.ownerAvatar} alt="" />
                            : <UserRound size={22} aria-hidden="true" />}
                        </button>
                        <h2 className="casting-talent-name">
                          <button
                            type="button"
                            className="casting-profile-name-link"
                            onClick={() => openTalentOwnerProfile(talent)}
                            disabled={!onProfileSelect || !talent.ownerProfileId}
                          >
                            {talent.fullName}
                          </button>
                        </h2>
                      </div>
                      {talent.ownerId === ownerId && (
                        <div className="casting-owner-actions">
                          <button
                            type="button"
                            className="casting-edit-button"
                            onClick={() => startEditingTalent(talent)}
                            disabled={Boolean(deletingTalentId)}
                            aria-label={`Edit ${talent.fullName}'s casting profile`}
                          >
                            <Pencil size={14} aria-hidden="true" />
                            Edit
                          </button>
                          <button
                            type="button"
                            style={deleteButtonStyle}
                            onClick={() => deleteTalent(talent)}
                            disabled={deletingTalentId === talent.id}
                            aria-label={`Delete ${talent.fullName}'s casting profile`}
                          >
                            <Trash2 size={15} aria-hidden="true" />
                            {deletingTalentId === talent.id ? 'Deleting…' : 'Delete'}
                          </button>
                        </div>
                      )}
                    </div>
                    <p style={{ margin: '7px 0 14px', color: '#586270', fontSize: '14px' }}>
                      {talent.age} years · {talent.gender} · {talent.city} · {formatCastingHeight(talent.height)}
                    </p>
                    <p style={{ margin: '0 0 10px', color: '#303843', lineHeight: 1.5 }}>
                      <strong>Experience:</strong> {talent.experience}
                    </p>
                    {talent.portfolio && (
                      <p style={{ margin: '0 0 10px', overflowWrap: 'anywhere' }}>
                        <a href={talent.portfolio} target="_blank" rel="noreferrer" style={{ color: '#8b2332' }}>
                          View portfolio
                        </a>
                      </p>
                    )}
                    {talent.previousWork && (
                      <p style={{ margin: '0 0 10px', color: '#303843', lineHeight: 1.5 }}>
                        <strong>Previous work:</strong> {talent.previousWork}
                      </p>
                    )}
                    <div style={talentPhotoGridStyle}>
                      {talent.photos.map((photo, index) => (
                        <img
                          key={photo}
                          src={photo}
                          alt={`${talent.fullName}, photo ${index + 1}`}
                          style={{ ...photoCardStyle, ...photoImageStyle }}
                        />
                      ))}
                    </div>
                    {talent.ownerId !== ownerId && (
                      <div className="casting-interest-row">
                        <button
                          type="button"
                          className="home-investor-interest"
                          style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
                          aria-pressed={interestedTalentIds.includes(talent.id)}
                          onClick={() => toggleInterest(talent.id)}
                        >
                          <Heart size={16} aria-hidden="true" />
                          {interestedTalentIds.includes(talent.id) ? 'Interest Shown' : 'Show Interest'}
                        </button>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
            <p style={{ margin: '20px 0 0', color: '#737d89', fontSize: '12px', lineHeight: 1.5 }}>
              Casting profiles are available to signed-in users. Interest responses are local to this session and don’t send notifications.
            </p>
          </>
        ) : (
          <>
            <button type="button" onClick={cancelForm} style={backButtonStyle}>
              <ArrowLeft size={17} aria-hidden="true" /> Back to Casting Talent
            </button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px', margin: '24px 0 8px' }}>
              <span style={iconStyle}><Camera size={22} aria-hidden="true" /></span>
              <div>
                <p style={eyebrowStyle}>CASTING</p>
                <h1 id="casting-page-title" style={titleStyle}>
                  {editingTalent ? 'Edit Casting Profile' : 'Initial Profile Screening'}
                </h1>
              </div>
            </div>
            <p style={{ margin: '0 0 24px', color: '#586270', lineHeight: 1.6 }}>
              Share your details and recent photographs for casting consideration.
            </p>
            <form key={editingTalent?.id || 'new-casting-profile'} onSubmit={handleSubmit} style={{ display: 'grid', gap: '18px' }}>
              <div style={formGridStyle}>
                <Field label="Full Name">
                  <input
                    name="fullName"
                    autoComplete="name"
                    defaultValue={editingTalent?.fullName || ''}
                    required
                    style={inputStyle}
                  />
                </Field>
                <Field label="Age">
                  <input
                    name="age"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]{1,2}"
                    maxLength={2}
                    title="Enter an age using one or two digits."
                    defaultValue={editingTalent?.age || ''}
                    required
                    style={inputStyle}
                  />
                </Field>
                <Field label="Gender">
                  <select name="gender" defaultValue={editingTalent?.gender || ''} required style={inputStyle}>
                    <option value="" disabled>Select gender</option>
                    <option>Female</option>
                    <option>Male</option>
                    <option>Non-binary</option>
                    <option>Prefer not to say</option>
                  </select>
                </Field>
                <Field label="City">
                  <input
                    name="city"
                    autoComplete="address-level2"
                    defaultValue={editingTalent?.city || ''}
                    required
                    style={inputStyle}
                  />
                </Field>
                <Field label="Height (cm)">
                  <input
                    name="height"
                    type="number"
                    min="50"
                    max="250"
                    step="1"
                    value={heightCm}
                    onChange={(event) => setHeightCm(event.target.value)}
                    placeholder="e.g. 173"
                    required
                    style={inputStyle}
                  />
                  <span className="casting-height-conversion">
                    {heightCm && Number(heightCm) > 0
                      ? `Approx. ${formatCastingHeight(heightCm)}`
                      : 'Height will be converted to feet and inches.'}
                  </span>
                </Field>
                <Field label="Portfolio (optional)">
                  <input
                    name="portfolio"
                    type="url"
                    placeholder="https://"
                    defaultValue={editingTalent?.portfolio || ''}
                    style={inputStyle}
                  />
                </Field>
              </div>
              <Field label="Acting/Modeling Experience">
                <textarea
                  name="experience"
                  rows={4}
                  defaultValue={editingTalent?.experience || ''}
                  required
                  style={{ ...inputStyle, resize: 'vertical' }}
                />
              </Field>
              <Field label="Previous Work (if any)">
                <textarea
                  name="previousWork"
                  rows={3}
                  defaultValue={editingTalent?.previousWork || ''}
                  style={{ ...inputStyle, resize: 'vertical' }}
                />
              </Field>
              <div style={fieldStyle}>
                <span style={labelStyle}>Portfolio or recent photos</span>
                <p style={{ margin: 0, color: '#586270', fontSize: '13px', lineHeight: 1.5 }}>
                  Please select 3–5 recent, clear photographs.
                </p>
                <label style={uploadStyle}>
                  <Camera size={20} aria-hidden="true" />
                  <span>{photos.length ? `Add photos (${photos.length}/5)` : 'Choose photographs'}</span>
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={choosePhotos}
                    style={{ position: 'absolute', width: 1, height: 1, opacity: 0 }}
                    aria-label="Choose 3 to 5 recent photographs"
                  />
                </label>
                {photos.length > 0 && (
                  <div style={photoGridStyle}>
                    {photos.map((photo, index) => (
                      <div
                        key={`${photo.url || photo.file?.name || photo.preview}-${index}`}
                        style={photoCardStyle}
                      >
                        <img src={photo.preview} alt={`Selected photo ${index + 1}`} style={photoImageStyle} />
                        <button
                          type="button"
                          onClick={() => removePhoto(index)}
                          aria-label={`Remove photo ${index + 1}`}
                          style={removePhotoStyle}
                        >
                          <X size={15} aria-hidden="true" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
                {photoError && <p role="alert" style={errorStyle}>{photoError}</p>}
              </div>
              {pageError && <p role="alert" style={errorStyle}>{pageError}</p>}
              <button type="submit" style={{ ...submitButtonStyle, opacity: isSubmitting ? 0.65 : 1 }} disabled={isSubmitting}>
                {isSubmitting
                  ? editingTalent ? 'Saving…' : 'Submitting…'
                  : editingTalent ? 'Save Changes' : 'Submit Casting Talent'}
              </button>
              <p style={{ margin: 0, color: '#737d89', fontSize: '12px', lineHeight: 1.5 }}>
                Casting details and photos will be visible to signed-in users.
              </p>
            </form>
          </>
        )}
      </section>
    </main>
  )
}