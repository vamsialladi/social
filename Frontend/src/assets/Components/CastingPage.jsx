import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, Camera, Heart, Pencil, Trash2, UserRound, X } from 'lucide-react'
import { api } from '../../api'
import './CastingPage.css'

const MAX_CASTING_PHOTO_BYTES = 5 * 1024 * 1024
const MAX_CASTING_PHOTOS_BYTES = 12 * 1024 * 1024
const MODELLING_REQUIREMENT_STEPS = [
  'Production Information',
  'Modelling Requirements',
  'Shoot Information',
  'Project Description',
  'Application Settings',
]

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
    fullName: details.fullName || post.title || 'Modelling Talent',
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

function ModellingRequirementWizard({ onCancel, onPublished, editingPost }) {
  const [step, setStep] = useState(1)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState(() => {
    const details = editingPost?.opportunityDetails
      && typeof editingPost.opportunityDetails === 'object'
      ? editingPost.opportunityDetails
      : {}
    const initialForm = {
      companyName: '',
      contactPersonName: '',
      contactEmail: '',
      contactMobile: '',
      projectCampaignName: '',
      projectType: '',
      postTitle: '',
      modellingCategory: '',
      minimumAge: '',
      maximumAge: '',
      genderPreference: '',
      minimumHeight: '',
      maximumHeight: '',
      experienceRequired: '',
      physicalRequirements: '',
      shootingLocation: '',
      auditionDate: '',
      shootingStartDate: '',
      shootingEndDate: '',
      numberOfModels: '',
      paymentType: '',
      budget: '',
      projectDescription: '',
      referenceVideoUrl: '',
      referenceImageUrl: '',
      applicationDeadline: '',
      additionalInstructions: '',
      confirmsGenuine: false,
      ...details,
    }
    initialForm.postTitle = details.postTitle || editingPost?.title || ''
    initialForm.projectDescription = details.projectDescription || editingPost?.content || ''
    initialForm.confirmsGenuine = Boolean(details.confirmsGenuine)
    return initialForm
  })

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
    setError('')
  }

  function validateStep() {
    if (step === 1 && form.contactEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.contactEmail.trim())) {
      setError('Enter a valid contact email address.')
      return false
    }

    if (step === 2) {
      const minimumAge = Number(form.minimumAge)
      const maximumAge = Number(form.maximumAge)
      if (form.minimumAge && form.maximumAge && minimumAge > maximumAge) {
        setError('Maximum age must be greater than or equal to minimum age.')
        return false
      }
      if (form.minimumHeight && form.maximumHeight && Number(form.minimumHeight) > Number(form.maximumHeight)) {
        setError('Maximum height must be greater than or equal to minimum height.')
        return false
      }
    }

    if (step === 3 && form.shootingStartDate && form.shootingEndDate && form.shootingEndDate < form.shootingStartDate) {
      setError('Shooting end date must be on or after the start date.')
      return false
    }

    if (step === 5 && !form.confirmsGenuine) {
      setError('Please confirm that this modelling requirement is genuine.')
      return false
    }

    setError('')
    return true
  }

  function continueToNextStep() {
    if (validateStep()) setStep((current) => Math.min(MODELLING_REQUIREMENT_STEPS.length, current + 1))
  }

  async function publishRequirement(event) {
    event.preventDefault()
    if (step !== MODELLING_REQUIREMENT_STEPS.length || isSubmitting || !validateStep()) return

    setIsSubmitting(true)
    setError('')
    try {
      const postData = {
        title: form.postTitle.trim() || 'Modelling Requirement',
        content: form.projectDescription.trim() || 'Modelling opportunity details to be announced.',
        type: 'opportunity',
        audience: 'public',
        opportunityDetails: { ...form },
      }
      const response = editingPost
        ? await api.updatePost(editingPost.id, postData)
        : await api.createPost(postData)
      const post = editingPost
        ? {
            ...editingPost,
            ...response,
            id: response?.id || editingPost.id,
            type: 'opportunity',
            opportunityDetails: response?.opportunityDetails || postData.opportunityDetails,
          }
        : response
      if (!post?.id) throw new Error('The modelling requirement was not published. Please try again.')
      onPublished(post)
    } catch (submitError) {
      console.error('Could not publish modelling requirement:', submitError)
      setError(submitError instanceof Error
        ? submitError.message
        : 'Could not publish the modelling requirement. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  function field(label, name, options = {}) {
    const { type = 'text', placeholder = label, choices, fullWidth = false, min, max } = options
    const className = `modelling-requirement-field${fullWidth ? ' is-full' : ''}`
    return (
      <label className={className} key={name}>
        <span>{label}</span>
        {choices ? (
          <select
            value={form[name]}
            onChange={(event) => updateField(name, event.target.value)}
            disabled={isSubmitting}
          >
            <option value="">{placeholder}</option>
            {choices.map((choice) => <option key={choice}>{choice}</option>)}
          </select>
        ) : (
          <input
            type={type}
            value={form[name]}
            onChange={(event) => updateField(name, event.target.value)}
            placeholder={placeholder}
            min={min}
            max={max}
            disabled={isSubmitting}
          />
        )}
      </label>
    )
  }

  return (
    <section className="modelling-requirement-wizard" aria-labelledby="modelling-requirement-title">
      <button className="modelling-requirement-back" type="button" onClick={onCancel} disabled={isSubmitting}>
        <ArrowLeft size={17} aria-hidden="true" /> Back to Modelling Talent
      </button>
      <header className="modelling-requirement-header">
        <p className="modelling-requirement-eyebrow">MODELLING</p>
        <h1 id="modelling-requirement-title">{editingPost ? 'Edit Modelling Requirement' : 'Post a Modelling Requirement'}</h1>
        <p>{editingPost ? 'Update the details for this modelling project.' : 'Share a modelling project with suitable talent.'}</p>
        <div className="modelling-requirement-progress" aria-label={`Step ${step} of ${MODELLING_REQUIREMENT_STEPS.length}`}>
          <div><span style={{ width: `${(step / MODELLING_REQUIREMENT_STEPS.length) * 100}%` }} /></div>
          <small>Step {step} of {MODELLING_REQUIREMENT_STEPS.length} · {MODELLING_REQUIREMENT_STEPS[step - 1]}</small>
        </div>
      </header>

      <form className="modelling-requirement-form" onSubmit={publishRequirement}>
        {step === 1 && (
          <section className="modelling-requirement-step">
            <h2>01. Production Information</h2>
            <div className="modelling-requirement-grid">
              {field('Production / Company Name', 'companyName', { fullWidth: true })}
              {field('Contact Person Name', 'contactPersonName', { fullWidth: true })}
              {field('Contact Email', 'contactEmail', { type: 'email', fullWidth: true })}
              {field('Contact Mobile Number', 'contactMobile', { type: 'tel', fullWidth: true })}
              {field('Project / Campaign Name', 'projectCampaignName', { fullWidth: true })}
              {field('Project Type', 'projectType', {
                choices: ['Fashion Shoot', 'Commercial', 'Print / Catalogue', 'Runway', 'Music Video', 'Film / TV', 'Brand Campaign', 'Other'],
                fullWidth: true,
              })}
            </div>
          </section>
        )}

        {step === 2 && (
          <section className="modelling-requirement-step">
            <h2>02. Modelling Requirements</h2>
            <div className="modelling-requirement-grid">
              {field('Post Title', 'postTitle', { placeholder: 'e.g. Fashion Model Required', fullWidth: true })}
              {field('Modelling Category', 'modellingCategory', {
                choices: ['Fashion', 'Commercial', 'Print', 'Runway', 'Fitness', 'Beauty', 'Lifestyle', 'Plus-size', 'Other'],
                fullWidth: true,
              })}
              {field('Minimum Age', 'minimumAge', { type: 'number', min: '1', max: '100' })}
              {field('Maximum Age', 'maximumAge', { type: 'number', min: '1', max: '100' })}
              {field('Gender Preference', 'genderPreference', {
                choices: ['Any', 'Female', 'Male', 'Non-binary'],
                fullWidth: true,
              })}
              {field('Minimum Height (cm)', 'minimumHeight', { type: 'number', min: '50', max: '250' })}
              {field('Maximum Height (cm)', 'maximumHeight', { type: 'number', min: '50', max: '250' })}
              {field('Experience Required', 'experienceRequired', {
                choices: ['Any experience', 'Beginner', 'Intermediate', 'Experienced', 'Professional'],
                fullWidth: true,
              })}
              <label className="modelling-requirement-field is-full">
                <span>Physical Requirements / Special Skills</span>
                <textarea
                  value={form.physicalRequirements}
                  onChange={(event) => updateField('physicalRequirements', event.target.value)}
                  placeholder="Physical Requirements / Special Skills"
                  disabled={isSubmitting}
                />
              </label>
            </div>
          </section>
        )}

        {step === 3 && (
          <section className="modelling-requirement-step">
            <h2>03. Shoot Information</h2>
            <div className="modelling-requirement-grid">
              {field('Shooting Location', 'shootingLocation', { fullWidth: true })}
              {field('Audition Date', 'auditionDate', { type: 'date', fullWidth: true })}
              {field('Shooting Start Date', 'shootingStartDate', { type: 'date', fullWidth: true })}
              {field('Shooting End Date', 'shootingEndDate', { type: 'date', fullWidth: true })}
              {field('Number of Models Required', 'numberOfModels', { type: 'number', min: '1', fullWidth: true })}
              {field('Payment Type', 'paymentType', {
                choices: ['Paid', 'Unpaid', 'Negotiable'],
                fullWidth: true,
              })}
              {field('Budget / Remuneration', 'budget', { fullWidth: true })}
            </div>
          </section>
        )}

        {step === 4 && (
          <section className="modelling-requirement-step">
            <h2>04. Project Description</h2>
            <div className="modelling-requirement-grid">
              <label className="modelling-requirement-field is-full">
                <span>Project Description</span>
                <textarea
                  value={form.projectDescription}
                  onChange={(event) => updateField('projectDescription', event.target.value)}
                  placeholder="Describe the modelling project, expectations and responsibilities."
                  disabled={isSubmitting}
                />
              </label>
              {field('Audition / Reference Video URL (Optional)', 'referenceVideoUrl', { type: 'url', fullWidth: true })}
              {field('Reference Image URL (Optional)', 'referenceImageUrl', { type: 'url', fullWidth: true })}
            </div>
          </section>
        )}

        {step === 5 && (
          <section className="modelling-requirement-step">
            <h2>05. Application Settings</h2>
            <div className="modelling-requirement-grid">
              {field('Application Deadline', 'applicationDeadline', { type: 'date', fullWidth: true })}
              <label className="modelling-requirement-field is-full">
                <span>Contact / Additional Instructions</span>
                <textarea
                  value={form.additionalInstructions}
                  onChange={(event) => updateField('additionalInstructions', event.target.value)}
                  placeholder="Contact / Additional Instructions"
                  disabled={isSubmitting}
                />
              </label>
            </div>
            <label className="modelling-requirement-confirmation">
              <input
                type="checkbox"
                checked={form.confirmsGenuine}
                onChange={(event) => updateField('confirmsGenuine', event.target.checked)}
                required
                disabled={isSubmitting}
              />
              <span>I confirm that this modelling requirement and its requirements are genuine.</span>
            </label>
          </section>
        )}

        {error && <p className="modelling-requirement-error" role="alert">{error}</p>}
        <footer className="modelling-requirement-actions">
          {step === 1 ? (
            <button type="button" className="modelling-requirement-cancel" onClick={onCancel} disabled={isSubmitting}>Cancel</button>
          ) : (
            <button type="button" className="modelling-requirement-previous" onClick={() => setStep((current) => current - 1)} disabled={isSubmitting}>Previous</button>
          )}
          {step < MODELLING_REQUIREMENT_STEPS.length ? (
            <button type="button" className="modelling-requirement-next" onClick={continueToNextStep} disabled={isSubmitting}>Next</button>
          ) : (
            <button
              type="submit"
              className="modelling-requirement-next"
              disabled={isSubmitting || !form.confirmsGenuine}
            >
              {isSubmitting
                ? (editingPost ? 'Saving…' : 'Publishing…')
                : (editingPost ? 'Save Changes' : 'Publish Modelling Requirement')}
            </button>
          )}
        </footer>
      </form>
    </section>
  )
}

export default function CastingPage({
  onBack,
  onProfileSelect,
  currentUserId,
  currentUserIdentifiers = [],
  currentUserAvatar,
  isAuthenticated,
  selectedModellingRequirement,
}) {
  const [photos, setPhotos] = useState([])
  const [heightCm, setHeightCm] = useState('')
  const photoUrlsRef = useRef([])
  const [photoError, setPhotoError] = useState('')
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [isRequirementWizardOpen, setIsRequirementWizardOpen] = useState(false)
  const [editingRequirement, setEditingRequirement] = useState(null)
  const [talents, setTalents] = useState([])
  const [modellingRequirements, setModellingRequirements] = useState(() => (
    isModellingRequirementPost(selectedModellingRequirement) ? [selectedModellingRequirement] : []
  ))
  const [isLoadingRequirements, setIsLoadingRequirements] = useState(true)
  const [requirementsError, setRequirementsError] = useState('')
  const [editingTalent, setEditingTalent] = useState(null)
  const [interestedTalentIds, setInterestedTalentIds] = useState([])
  const [submittedMessage, setSubmittedMessage] = useState('')
  const [pageError, setPageError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingTalentId, setDeletingTalentId] = useState('')
  const [deletingRequirementId, setDeletingRequirementId] = useState('')
  const ownerId = String(currentUserId || 'guest').trim().toLowerCase()
  const ownerIdentityKeys = new Set(
    [currentUserId, ...currentUserIdentifiers]
      .filter(Boolean)
      .map((identity) => String(identity).trim().toLowerCase())
      .filter((identity) => identity !== 'me'),
  )
  const selectedModellingRequirementId = String(selectedModellingRequirement?.id || '')

  useEffect(() => {
    if (!isAuthenticated) return undefined

    let isCurrent = true
    api.getCastingProfiles()
      .then((profiles) => {
        if (!Array.isArray(profiles)) {
          throw new Error('Modelling profiles returned an invalid response.')
        }
        if (isCurrent) {
          setTalents(profiles.map((profile) => mapCastingProfile(profile, ownerId, currentUserAvatar)))
        }
      })
      .catch((error) => {
        console.error('Could not load modelling profiles:', error)
        if (isCurrent) setPageError(error instanceof Error ? error.message : 'Could not load modelling profiles.')
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })

    return () => {
      isCurrent = false
    }
  }, [currentUserAvatar, isAuthenticated, ownerId])

  useEffect(() => {
    if (!isAuthenticated) return undefined

    let isCurrent = true
    api.getOpportunities()
      .then((posts) => {
        if (!Array.isArray(posts)) {
          throw new Error('Modelling requirements returned an invalid response.')
        }
        if (isCurrent) {
          const requirements = posts.filter(isModellingRequirementPost)
          if (isModellingRequirementPost(selectedModellingRequirement)) {
            const selectedId = String(selectedModellingRequirement.id)
            setModellingRequirements([
              selectedModellingRequirement,
              ...requirements.filter((post) => String(post.id) !== selectedId),
            ])
          } else {
            setModellingRequirements(requirements)
          }
          setRequirementsError('')
        }
      })
      .catch((error) => {
        console.error('Could not load modelling requirements:', error)
        if (isCurrent) {
          setRequirementsError(error instanceof Error
            ? error.message
            : 'Could not load modelling requirements.')
        }
      })
      .finally(() => {
        if (isCurrent) setIsLoadingRequirements(false)
      })

    return () => {
      isCurrent = false
    }
  }, [isAuthenticated, selectedModellingRequirement])

  useEffect(() => {
    if (!selectedModellingRequirementId || isLoadingRequirements) return
    document.getElementById(`modelling-requirement-${encodeURIComponent(selectedModellingRequirementId)}`)
      ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [isLoadingRequirements, selectedModellingRequirementId])

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

  function startModellingRequirement() {
    setEditingRequirement(null)
    setIsRequirementWizardOpen(true)
    setPageError('')
    setSubmittedMessage('')
  }

  function startEditingModellingRequirement(post) {
    setEditingRequirement(post)
    setIsRequirementWizardOpen(true)
    setPageError('')
    setSubmittedMessage('')
  }

  function cancelModellingRequirement() {
    setIsRequirementWizardOpen(false)
    setEditingRequirement(null)
  }

  function finishModellingRequirement(post) {
    const wasEditing = Boolean(editingRequirement)
    setIsRequirementWizardOpen(false)
    setEditingRequirement(null)
    if (post?.id) {
      setModellingRequirements((current) => [
        post,
        ...current.filter((item) => String(item.id) !== String(post.id)),
      ])
      setRequirementsError('')
      window.dispatchEvent(new CustomEvent('truehunt-opportunity-published', {
        detail: { post },
      }))
    }
    setSubmittedMessage(wasEditing
      ? 'Modelling requirement updated.'
      : 'Modelling requirement published. It is available on the Home feed and Modelling page.')
  }

  async function deleteModellingRequirement(post) {
    const postId = String(post?.id || '')
    if (!postId || !isOwnModellingRequirement(post)) return
    if (!window.confirm('Delete this modelling requirement?')) return

    setPageError('')
    setDeletingRequirementId(postId)
    try {
      if (!post.localOnly) await api.deletePost(post.id)
      setModellingRequirements((current) => current.filter((item) => String(item.id) !== postId))
      setSubmittedMessage('Modelling requirement deleted.')
      window.dispatchEvent(new CustomEvent('truehunt-opportunity-deleted', {
        detail: { postId },
      }))
    } catch (error) {
      console.error('Could not delete modelling requirement:', error)
      setPageError(error instanceof Error
        ? error.message
        : 'Could not delete modelling requirement. Please try again.')
    } finally {
      setDeletingRequirementId('')
    }
  }

  function isOwnModellingRequirement(post) {
    if (!post || post.isAuthor === false) return false

    const authorIdentifiers = [post.authorId, post.authorEmail]
      .filter(Boolean)
      .map((identifier) => String(identifier).trim().toLowerCase())
    if (authorIdentifiers.some((identifier) => ownerIdentityKeys.has(identifier))) return true

    const legacyAccountIds = ['me', '1000001', 'pro@truehunt.in']
    return authorIdentifiers.some((identifier) => legacyAccountIds.includes(identifier))
      && [...ownerIdentityKeys].some((identifier) => legacyAccountIds.includes(identifier))
  }

  function shareModellingRequirement(post) {
    window.dispatchEvent(new CustomEvent('truehunt-share-opportunity', {
      detail: { post },
    }))
  }

  function applyForModellingRequirement(post) {
    window.dispatchEvent(new CustomEvent('truehunt-apply-opportunity', {
      detail: { post },
    }))
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
        throw new Error('The modelling profile was not saved. Please try again.')
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
        ? `${talent.fullName}'s modelling profile was updated.`
        : `${talent.fullName}'s modelling talent profile was added to this page.`)
      setIsFormOpen(false)
    } catch (error) {
      console.error('Could not submit modelling profile:', error)
      setPageError(error instanceof Error ? error.message : 'Could not submit modelling profile. Please try again.')
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
    if (!window.confirm(`Delete ${talent.fullName}'s modelling profile?`)) return

    setPageError('')
    setDeletingTalentId(talent.id)
    try {
      await api.deletePost(talent.id)
      setTalents((current) => current.filter((entry) => entry.id !== talent.id))
      setInterestedTalentIds((current) => current.filter((id) => id !== talent.id))
    } catch (error) {
      console.error('Could not delete modelling profile:', error)
      setPageError(error instanceof Error ? error.message : 'Could not delete modelling profile. Please try again.')
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
      <main style={pageStyle}>
        <section style={cardStyle} aria-labelledby="casting-sign-in-title">
          <button type="button" onClick={onBack} style={backButtonStyle}>
            <ArrowLeft size={17} aria-hidden="true" /> Back to social
          </button>
          <h1 id="casting-sign-in-title" style={{ ...titleStyle, marginTop: '24px' }}>Sign in to view Modelling Talent</h1>
          <p style={{ color: '#586270', lineHeight: 1.6 }}>
            Modelling profiles are available to signed-in users only.
          </p>
        </section>
      </main>
    )
  }

  return (
    <main style={pageStyle}>
      <section style={{ ...cardStyle, width: 'min(980px, 100%)' }} aria-labelledby="casting-page-title">
        {isRequirementWizardOpen ? (
          <ModellingRequirementWizard
            editingPost={editingRequirement}
            onCancel={cancelModellingRequirement}
            onPublished={finishModellingRequirement}
          />
        ) : !isFormOpen ? (
          <>
            <button type="button" onClick={onBack} style={backButtonStyle}>
              <ArrowLeft size={17} aria-hidden="true" /> Back to social
            </button>
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '20px', marginTop: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                <span style={iconStyle}><Camera size={22} aria-hidden="true" /></span>
                <div>
                  <p style={eyebrowStyle}>MODELLING</p>
                  <h1 id="casting-page-title" style={titleStyle}>Modelling Talent</h1>
                </div>
              </div>
              <button
                className="casting-add-button"
                type="button"
                onClick={startModellingRequirement}
              >
                Post a Modelling Requirement
              </button>
            </div>
            <p style={{ margin: '16px 0 0', color: '#586270', lineHeight: 1.6 }}>
              Discover talent profiles shared for modelling consideration.
            </p>
            {submittedMessage && (
              <p role="status" style={{ margin: '16px 0 0', color: '#237547', fontSize: '14px' }}>
                {submittedMessage}
              </p>
            )}
            {pageError && <p role="alert" style={{ ...errorStyle, marginTop: '16px' }}>{pageError}</p>}
            <section className="modelling-requirements-section" aria-labelledby="modelling-requirements-heading">
              <header className="modelling-requirements-header">
                <h2 id="modelling-requirements-heading">Modelling Requirements</h2>
                <p>Browse modelling projects and apply to the ones that fit you.</p>
              </header>
              {requirementsError && <p className="modelling-requirements-error" role="alert">{requirementsError}</p>}
              {isLoadingRequirements ? (
                <p className="modelling-requirements-empty">Loading modelling requirements…</p>
              ) : modellingRequirements.length > 0 ? (
                <div className="modelling-requirements-grid">
                  {modellingRequirements.map((post) => {
                    const details = post.opportunityDetails || {}
                    const isOwnPost = isOwnModellingRequirement(post)
                    const metadata = [
                      details.modellingCategory,
                      details.shootingLocation,
                      details.numberOfModels ? `${details.numberOfModels} models` : '',
                    ].filter(Boolean).join(' · ')

                    return (
                      <article
                        className={`modelling-requirement-card${String(post.id) === selectedModellingRequirementId ? ' is-selected' : ''}`}
                        id={`modelling-requirement-${encodeURIComponent(String(post.id))}`}
                        key={post.id}
                      >
                        <div className="modelling-requirement-card-byline">
                          <strong>{post.authorName || 'Cinema Professional'}</strong>
                          {isOwnPost && <span>Your post</span>}
                        </div>
                        <h3>{post.title || details.projectCampaignName || 'Modelling Requirement'}</h3>
                        {metadata && <p className="modelling-requirement-card-meta">{metadata}</p>}
                        {post.content && <p className="modelling-requirement-card-copy">{post.content}</p>}
                        <div className="modelling-requirement-card-actions">
                          <button
                            type="button"
                            className="modelling-requirement-share-button"
                            onClick={() => shareModellingRequirement(post)}
                          >
                            Share
                          </button>
                          {!isOwnPost && (
                            <button
                              type="button"
                              className="modelling-requirement-apply-button"
                              onClick={() => applyForModellingRequirement(post)}
                            >
                              {post.applied ? '✓ Applied' : 'Apply'}
                            </button>
                          )}
                        </div>
                        {isOwnPost && (
                          <div className="modelling-requirement-owner-actions">
                            <button
                              type="button"
                              className="modelling-requirement-edit-button"
                              onClick={() => startEditingModellingRequirement(post)}
                              disabled={deletingRequirementId === String(post.id)}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="modelling-requirement-delete-button"
                              onClick={() => void deleteModellingRequirement(post)}
                              disabled={deletingRequirementId === String(post.id)}
                            >
                              {deletingRequirementId === String(post.id) ? 'Deleting…' : 'Delete'}
                            </button>
                          </div>
                        )}
                      </article>
                    )
                  })}
                </div>
              ) : requirementsError ? null : (
                <p className="modelling-requirements-empty">No modelling requirements have been posted yet.</p>
              )}
            </section>
            {isLoading ? (
              <p style={{ marginTop: '24px', color: '#586270' }}>Loading modelling profiles…</p>
            ) : talents.length === 0 ? (
              <div style={{ marginTop: '24px', padding: '28px 20px', border: '1px dashed #cfd6de', borderRadius: '12px', textAlign: 'center', color: '#586270' }}>
                No modelling talent profiles yet. Add your profile to get started.
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
                            aria-label={`Edit ${talent.fullName}'s modelling profile`}
                          >
                            <Pencil size={14} aria-hidden="true" />
                            Edit
                          </button>
                          <button
                            type="button"
                            style={deleteButtonStyle}
                            onClick={() => deleteTalent(talent)}
                            disabled={deletingTalentId === talent.id}
                            aria-label={`Delete ${talent.fullName}'s modelling profile`}
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
              Modelling profiles are available to signed-in users. Interest responses are local to this session and don’t send notifications.
            </p>
          </>
        ) : (
          <>
            <button type="button" onClick={cancelForm} style={backButtonStyle}>
              <ArrowLeft size={17} aria-hidden="true" /> Back to Modelling Talent
            </button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px', margin: '24px 0 8px' }}>
              <span style={iconStyle}><Camera size={22} aria-hidden="true" /></span>
              <div>
                <p style={eyebrowStyle}>MODELLING</p>
                <h1 id="casting-page-title" style={titleStyle}>
                  {editingTalent ? 'Edit Modelling Profile' : 'Initial Profile Screening'}
                </h1>
              </div>
            </div>
            <p style={{ margin: '0 0 24px', color: '#586270', lineHeight: 1.6 }}>
              Share your details and recent photographs for modelling consideration.
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
                  : editingTalent ? 'Save Changes' : 'Submit Modelling Talent'}
              </button>
              <p style={{ margin: 0, color: '#737d89', fontSize: '12px', lineHeight: 1.5 }}>
                Modelling details and photos will be visible to signed-in users.
              </p>
            </form>
          </>
        )}
      </section>
    </main>
  )
}