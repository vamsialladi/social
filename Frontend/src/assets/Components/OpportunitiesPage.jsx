import { useEffect, useState } from 'react'
import { ArrowLeft, ArrowRight, BriefcaseBusiness, Check, Film, X } from 'lucide-react'
import './OpportunitiesPage.css'

const roles = [
  'Hero / Lead',
  'Heroine / Lead Actress',
  'Child Artist',
  'Side Character',
  'Mother Character',
  'Father Character',
  'Villain / Antagonist',
  'Comedian / Comic Role',
  'Supporting Role',
  'Cameo / Guest Appearance',
  'Extra / Background Artist',
  'Dancer',
]

const projectTypes = [
  'Feature Film',
  'Short Film',
  'Web Series',
  'Television',
  'Advertisement',
  'Music Video',
  'Theatre',
  'Other',
]

const languages = ['Telugu', 'Hindi', 'Tamil', 'Kannada', 'Malayalam', 'English', 'Other']
const actingSkills = [
  'Emotional performance',
  'Improvisation',
  'Dialogue delivery',
  'Voice modulation',
  'Screen presence',
  'Dance',
  'Action',
  'Comedy',
]
const danceStyles = ['Classical', 'Contemporary', 'Hip-hop', 'Folk', 'Western', 'Other']

const initialForm = {
  projectName: '',
  projectType: 'Feature Film',
  craft: 'Acting',
  openings: '1',
  actingRole: roles[0],
  genderPreference: 'Any',
  minimumAge: '',
  maximumAge: '',
  height: '',
  weight: '',
  experienceLevel: 'Any experience',
  languagesKnown: [],
  otherLanguage: '',
  actingSkills: [],
  otherActingSkill: '',
  locationPreference: '',
  guardianName: '',
  guardianPhone: '',
  guardianRelationship: '',
  guardianPermission: false,
  actingExperience: '',
  showreelUrl: '',
  actionSkills: '',
  negativeCharacterPerformance: '',
  comedyTiming: '',
  performanceVideoUrl: '',
  groupApplications: false,
  danceStyles: [],
  otherDanceStyle: '',
  characterDescription: '',
  appearanceRequirements: '',
  costumeRequirements: '',
  specialInstructions: '',
  auditionDate: '',
  applicationDeadline: '',
}

function Field({ label, required = false, optional = false, children, className = '' }) {
  return (
    <div className={`opportunity-field ${className}`}>
      <span className="opportunity-field-label">
        {label}
        {required && <span className="opportunity-required"> *</span>}
        {optional && <span className="opportunity-optional"> (optional)</span>}
      </span>
      {children}
    </div>
  )
}

function CheckOptions({ label, options, selected, onChange, otherValue, onOtherChange }) {
  return (
    <Field label={label}>
      <div className="opportunity-checkbox-grid">
        {options.map((option) => (
          <label className="opportunity-choice" key={option}>
            <input
              type="checkbox"
              checked={selected.includes(option)}
              onChange={() => onChange(option)}
            />
            <span>{option}</span>
          </label>
        ))}
      </div>
      {selected.includes('Other') && (
        <input
          className="opportunity-input opportunity-other-input"
          value={otherValue}
          onChange={(event) => onOtherChange(event.target.value)}
          placeholder="Please specify"
          aria-label={`Other ${label.toLowerCase()}`}
        />
      )}
    </Field>
  )
}

export default function OpportunitiesPage({ onClose, onPublish, initialData, isEditing = false }) {
  const [form, setForm] = useState(() => ({ ...initialForm, ...initialData }))
  const [step, setStep] = useState(1)
  const [error, setError] = useState('')
  const [isPublishing, setIsPublishing] = useState(false)

  useEffect(() => {
    function handleEscape(event) {
      if (event.key === 'Escape' && !isPublishing) onClose()
    }

    document.addEventListener('keydown', handleEscape)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handleEscape)
      document.body.style.overflow = ''
    }
  }, [isPublishing, onClose])

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
    setError('')
  }

  function toggleOption(field, option) {
    setForm((current) => ({
      ...current,
      [field]: current[field].includes(option)
        ? current[field].filter((item) => item !== option)
        : [...current[field], option],
    }))
    setError('')
  }

  function validateStep() {
    if (step === 1 && !form.projectName.trim()) {
      setError('Enter a project name to continue.')
      return false
    }

    if (step === 1 && (!Number(form.openings) || Number(form.openings) < 1)) {
      setError('Add at least one opening.')
      return false
    }

    if (step === 2 && form.minimumAge && form.maximumAge
      && Number(form.minimumAge) > Number(form.maximumAge)) {
      setError('Maximum age must be greater than or equal to minimum age.')
      return false
    }

    if (step === 3) {
      if (form.actingRole === 'Child Artist'
        && (!form.guardianName.trim() || !form.guardianPermission)) {
        setError('Add a guardian name and confirm guardian permission for a child artist.')
        return false
      }
      if (['Hero / Lead', 'Heroine / Lead Actress'].includes(form.actingRole)
        && (!form.actingExperience.trim() || !form.showreelUrl.trim())) {
        setError('Add acting experience and a showreel for a lead role.')
        return false
      }
      if (form.actingRole === 'Villain / Antagonist'
        && (!form.actionSkills.trim() || !form.negativeCharacterPerformance.trim())) {
        setError('Describe the action skills and negative-character performance required.')
        return false
      }
      if (form.actingRole === 'Comedian / Comic Role'
        && (!form.comedyTiming.trim() || !form.performanceVideoUrl.trim())) {
        setError('Add comedy timing details and a performance video requirement.')
        return false
      }
      if (form.actingRole === 'Dancer'
        && (form.danceStyles.length === 0 || !form.performanceVideoUrl.trim())) {
        setError('Select at least one dance style and add a performance video requirement.')
        return false
      }
    }

    setError('')
    return true
  }

  function continueToNextStep() {
    if (validateStep()) setStep((current) => Math.min(5, current + 1))
  }

  async function publish(event) {
    event.preventDefault()
    if (step < 5) {
      continueToNextStep()
      return
    }

    setError('')
    setIsPublishing(true)
    try {
      await onPublish(form)
    } catch (publishError) {
      setError(publishError instanceof Error
        ? publishError.message
        : 'Could not publish this opportunity. Please try again.')
    } finally {
      setIsPublishing(false)
    }
  }

  const hasLeadRequirements = ['Hero / Lead', 'Heroine / Lead Actress'].includes(form.actingRole)
  const isBackgroundArtist = form.actingRole === 'Extra / Background Artist'

  return (
    <div
      className="opportunity-modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isPublishing) onClose()
      }}
    >
      <section
        className="opportunity-wizard"
        role="dialog"
        aria-modal="true"
        aria-labelledby="opportunity-wizard-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
      <header className="opportunity-wizard-header">
        <button
          className="opportunity-modal-close"
          type="button"
          onClick={onClose}
          disabled={isPublishing}
          aria-label="Close opportunity form"
        >
          <X size={19} aria-hidden="true" />
        </button>
        <button className="opportunity-back-link" type="button" onClick={onClose}>
          <ArrowLeft size={17} aria-hidden="true" /> Back to opportunities
        </button>
        <div className="opportunity-header-title">
          <span className="opportunity-title-icon"><Film size={20} aria-hidden="true" /></span>
          <div>
            <span className="opportunity-eyebrow">CASTING CALL</span>
            <h1 id="opportunity-wizard-title">{isEditing ? 'Edit opportunity' : 'Share an opportunity'}</h1>
          </div>
        </div>
        <div className="opportunity-progress" aria-label={`Step ${step} of 5`}>
          <div className="opportunity-progress-top">
            <span>Step {step} of 5</span>
            <span>{Math.round((step / 5) * 100)}% complete</span>
          </div>
          <div className="opportunity-progress-track">
            <span style={{ width: `${(step / 5) * 100}%` }} />
          </div>
          <div className="opportunity-step-labels">
            {['Job details', 'Role & requirements', 'Role-specific', 'Character details', 'Review'].map((label, index) => (
              <span className={index + 1 <= step ? 'is-complete' : ''} key={label}>{label}</span>
            ))}
          </div>
        </div>
      </header>

      <form className="opportunity-wizard-form" onSubmit={publish}>
        <div className="opportunity-wizard-content">
          <div className="opportunity-form-panel">
            {step === 1 && (
              <section className="opportunity-step-content">
                <span className="opportunity-eyebrow">THE PROJECT</span>
                <h2>Basic Job Details</h2>
                <p className="opportunity-step-description">Enter the project and job information.</p>

                <Field label="Cinema Name" required>
                  <input
                    className="opportunity-input"
                    value={form.projectName}
                    onChange={(event) => updateField('projectName', event.target.value)}
                    placeholder="e.g. Upcoming Telugu Movie"
                    required
                    autoFocus
                  />
                </Field>
                <div className="opportunity-form-grid">
                  <Field label="Project Type" required>
                    <select className="opportunity-input" value={form.projectType} onChange={(event) => updateField('projectType', event.target.value)}>
                      {projectTypes.map((type) => <option key={type}>{type}</option>)}
                    </select>
                  </Field>
                  <Field label="Select Craft" required>
                    <select className="opportunity-input" value={form.craft} onChange={(event) => updateField('craft', event.target.value)}>
                      {['Acting', 'Dance', 'Junior Artists & Agents', 'Other'].map((craft) => <option key={craft}>{craft}</option>)}
                    </select>
                  </Field>
                </div>
                <Field label="Number of Openings" required>
                  <input
                    className="opportunity-input"
                    type="number"
                    min="1"
                    value={form.openings}
                    onChange={(event) => updateField('openings', event.target.value)}
                    required
                  />
                </Field>
              </section>
            )}

            {step === 2 && (
              <section className="opportunity-step-content">
                <span className="opportunity-eyebrow">WHO ARE YOU CASTING?</span>
                <h2>Acting Role & Candidate Requirements</h2>
                <p className="opportunity-step-description">Choose a role and set the requirements for applicants.</p>

                <Field label="Acting Role" required>
                  <select className="opportunity-input" value={form.actingRole} onChange={(event) => updateField('actingRole', event.target.value)}>
                    {roles.map((role) => <option key={role}>{role}</option>)}
                  </select>
                </Field>
                <Field label="Gender preference" required>
                  <div className="opportunity-radio-row">
                    {['Any', 'Male', 'Female', 'Non-binary'].map((gender) => (
                      <label className="opportunity-choice" key={gender}>
                        <input
                          type="radio"
                          name="genderPreference"
                          value={gender}
                          checked={form.genderPreference === gender}
                          onChange={(event) => updateField('genderPreference', event.target.value)}
                        />
                        <span>{gender}</span>
                      </label>
                    ))}
                  </div>
                </Field>
                <div className="opportunity-form-grid">
                  <Field label="Minimum age" optional>
                    <input className="opportunity-input" type="number" min="0" max="120" value={form.minimumAge} onChange={(event) => updateField('minimumAge', event.target.value)} placeholder="e.g. 18" />
                  </Field>
                  <Field label="Maximum age" optional>
                    <input className="opportunity-input" type="number" min="0" max="120" value={form.maximumAge} onChange={(event) => updateField('maximumAge', event.target.value)} placeholder="e.g. 35" />
                  </Field>
                  <Field label="Height" optional>
                    <input className="opportunity-input" value={form.height} onChange={(event) => updateField('height', event.target.value)} placeholder="e.g. 5 ft 8 in" />
                  </Field>
                  <Field label="Weight" optional>
                    <input className="opportunity-input" value={form.weight} onChange={(event) => updateField('weight', event.target.value)} placeholder="e.g. 65 kg" />
                  </Field>
                </div>
                <Field label="Experience level">
                  <select className="opportunity-input" value={form.experienceLevel} onChange={(event) => updateField('experienceLevel', event.target.value)}>
                    {['Any experience', 'Beginner', 'Some experience', 'Professional', 'Experienced'].map((level) => <option key={level}>{level}</option>)}
                  </select>
                </Field>
                <CheckOptions
                  label="Languages known"
                  options={languages}
                  selected={form.languagesKnown}
                  onChange={(value) => toggleOption('languagesKnown', value)}
                  otherValue={form.otherLanguage}
                  onOtherChange={(value) => updateField('otherLanguage', value)}
                />
                <CheckOptions
                  label="Required acting skills"
                  options={actingSkills}
                  selected={form.actingSkills}
                  onChange={(value) => toggleOption('actingSkills', value)}
                  otherValue={form.otherActingSkill}
                  onOtherChange={(value) => updateField('otherActingSkill', value)}
                />
                <Field label="Location preference" optional>
                  <input className="opportunity-input" value={form.locationPreference} onChange={(event) => updateField('locationPreference', event.target.value)} placeholder="e.g. Hyderabad, India" />
                </Field>
              </section>
            )}

            {step === 3 && (
              <section className="opportunity-step-content">
                <span className="opportunity-eyebrow">TAILORED TO THE ROLE</span>
                <h2>Role-specific requirements</h2>
                <p className="opportunity-step-description">These details change based on the selected role.</p>

                {form.actingRole === 'Child Artist' && (
                  <div className="opportunity-dynamic-card">
                    <h3>Guardian details & permissions</h3>
                    <p>A parent or legal guardian must be involved in the casting process.</p>
                    <Field label="Guardian name" required>
                      <input className="opportunity-input" value={form.guardianName} onChange={(event) => updateField('guardianName', event.target.value)} placeholder="Parent or legal guardian" />
                    </Field>
                    <div className="opportunity-form-grid">
                      <Field label="Relationship to child" optional>
                        <input className="opportunity-input" value={form.guardianRelationship} onChange={(event) => updateField('guardianRelationship', event.target.value)} placeholder="e.g. Parent" />
                      </Field>
                      <Field label="Guardian phone" optional>
                        <input className="opportunity-input" type="tel" value={form.guardianPhone} onChange={(event) => updateField('guardianPhone', event.target.value)} placeholder="Contact number" />
                      </Field>
                    </div>
                    <label className="opportunity-choice opportunity-permission">
                      <input type="checkbox" checked={form.guardianPermission} onChange={(event) => updateField('guardianPermission', event.target.checked)} />
                      <span>Guardian permission is required and will be confirmed before participation.</span>
                    </label>
                  </div>
                )}

                {hasLeadRequirements && (
                  <div className="opportunity-dynamic-card">
                    <h3>Lead role experience & showreel</h3>
                    <Field label="Acting experience" required>
                      <textarea className="opportunity-input opportunity-textarea" value={form.actingExperience} onChange={(event) => updateField('actingExperience', event.target.value)} placeholder="Describe the experience needed for this lead role" rows={3} />
                    </Field>
                    <Field label="Showreel link" required>
                      <input className="opportunity-input" type="url" value={form.showreelUrl} onChange={(event) => updateField('showreelUrl', event.target.value)} placeholder="https://..." />
                    </Field>
                  </div>
                )}

                {form.actingRole === 'Villain / Antagonist' && (
                  <div className="opportunity-dynamic-card">
                    <h3>Antagonist performance requirements</h3>
                    <Field label="Action skills" required>
                      <textarea className="opportunity-input opportunity-textarea" value={form.actionSkills} onChange={(event) => updateField('actionSkills', event.target.value)} placeholder="e.g. Stage combat, martial arts, stunt experience" rows={3} />
                    </Field>
                    <Field label="Negative-character performance" required>
                      <textarea className="opportunity-input opportunity-textarea" value={form.negativeCharacterPerformance} onChange={(event) => updateField('negativeCharacterPerformance', event.target.value)} placeholder="Describe the performance style required" rows={3} />
                    </Field>
                  </div>
                )}

                {form.actingRole === 'Comedian / Comic Role' && (
                  <div className="opportunity-dynamic-card">
                    <h3>Comedy performance requirements</h3>
                    <Field label="Comedy timing & style" required>
                      <textarea className="opportunity-input opportunity-textarea" value={form.comedyTiming} onChange={(event) => updateField('comedyTiming', event.target.value)} placeholder="Describe the comic timing or style needed" rows={3} />
                    </Field>
                    <Field label="Performance video requirement" required>
                      <input className="opportunity-input" value={form.performanceVideoUrl} onChange={(event) => updateField('performanceVideoUrl', event.target.value)} placeholder="Describe the video to submit or paste a link" />
                    </Field>
                  </div>
                )}

                {isBackgroundArtist && (
                  <div className="opportunity-dynamic-card">
                    <h3>Background artist applications</h3>
                    <p>Set multiple openings above and let applicants know whether group applications are welcome.</p>
                    <label className="opportunity-choice opportunity-permission">
                      <input type="checkbox" checked={form.groupApplications} onChange={(event) => updateField('groupApplications', event.target.checked)} />
                      <span>Accept group applications</span>
                    </label>
                  </div>
                )}

                {form.actingRole === 'Dancer' && (
                  <div className="opportunity-dynamic-card">
                    <h3>Dance styles & performance video</h3>
                    <CheckOptions
                      label="Dance styles"
                      options={danceStyles}
                      selected={form.danceStyles}
                      onChange={(value) => toggleOption('danceStyles', value)}
                      otherValue={form.otherDanceStyle}
                      onOtherChange={(value) => updateField('otherDanceStyle', value)}
                    />
                    <Field label="Performance video requirement" required>
                      <input className="opportunity-input" value={form.performanceVideoUrl} onChange={(event) => updateField('performanceVideoUrl', event.target.value)} placeholder="Describe the video to submit or paste a link" />
                    </Field>
                  </div>
                )}

                {!['Child Artist', 'Villain / Antagonist', 'Comedian / Comic Role', 'Extra / Background Artist', 'Dancer'].includes(form.actingRole) && !hasLeadRequirements && (
                  <div className="opportunity-dynamic-card opportunity-info-card">
                    <Check size={18} aria-hidden="true" />
                    <p>No additional role-specific fields are needed. Continue to add character details.</p>
                  </div>
                )}
              </section>
            )}

            {step === 4 && (
              <section className="opportunity-step-content">
                <span className="opportunity-eyebrow">BRING THE CHARACTER TO LIFE</span>
                <h2>Additional details</h2>
                <p className="opportunity-step-description">Give applicants a clear picture of the part and the process.</p>
                <Field label="Character description" optional>
                  <textarea className="opportunity-input opportunity-textarea" value={form.characterDescription} onChange={(event) => updateField('characterDescription', event.target.value)} placeholder="Describe the character, story context, and performance..." rows={4} />
                </Field>
                <Field label="Appearance requirements" optional>
                  <textarea className="opportunity-input opportunity-textarea" value={form.appearanceRequirements} onChange={(event) => updateField('appearanceRequirements', event.target.value)} placeholder="Any relevant appearance or look requirements..." rows={3} />
                </Field>
                <Field label="Costume requirements" optional>
                  <textarea className="opportunity-input opportunity-textarea" value={form.costumeRequirements} onChange={(event) => updateField('costumeRequirements', event.target.value)} placeholder="Wardrobe, fittings, or costume notes..." rows={3} />
                </Field>
                <Field label="Special instructions" optional>
                  <textarea className="opportunity-input opportunity-textarea" value={form.specialInstructions} onChange={(event) => updateField('specialInstructions', event.target.value)} placeholder="Audition process, preparation, or other information..." rows={3} />
                </Field>
                <div className="opportunity-form-grid">
                  <Field label="Audition date" optional>
                    <input className="opportunity-input" type="date" value={form.auditionDate} onChange={(event) => updateField('auditionDate', event.target.value)} />
                  </Field>
                  <Field label="Application deadline" optional>
                    <input className="opportunity-input" type="date" value={form.applicationDeadline} onChange={(event) => updateField('applicationDeadline', event.target.value)} />
                  </Field>
                </div>
              </section>
            )}

            {step === 5 && (
              <section className="opportunity-step-content">
                <span className="opportunity-eyebrow">READY TO SHARE</span>
                <h2>Review your opportunity</h2>
                <p className="opportunity-step-description">Check the details before publishing your casting call.</p>
                <div className="opportunity-review-card">
                  <h3>{form.projectName}</h3>
                  <p>{form.projectType} <span>·</span> {form.craft}</p>
                  <div className="opportunity-review-row"><strong>Role</strong><span>{form.actingRole}</span></div>
                  <div className="opportunity-review-row"><strong>Openings</strong><span>{form.openings}</span></div>
                  <div className="opportunity-review-row"><strong>Gender</strong><span>{form.genderPreference}</span></div>
                  {(form.minimumAge || form.maximumAge) && (
                    <div className="opportunity-review-row"><strong>Age range</strong><span>{form.minimumAge || 'Any'} – {form.maximumAge || 'Any'}</span></div>
                  )}
                  {form.locationPreference && <div className="opportunity-review-row"><strong>Location</strong><span>{form.locationPreference}</span></div>}
                  {form.experienceLevel !== 'Any experience' && <div className="opportunity-review-row"><strong>Experience</strong><span>{form.experienceLevel}</span></div>}
                  {form.auditionDate && <div className="opportunity-review-row"><strong>Audition</strong><span>{form.auditionDate}</span></div>}
                  {form.applicationDeadline && <div className="opportunity-review-row"><strong>Deadline</strong><span>{form.applicationDeadline}</span></div>}
                  {form.characterDescription && <p className="opportunity-review-description">{form.characterDescription}</p>}
                </div>
                <p className="opportunity-publish-note">Your casting call will be visible to the TrueHunt community in Opportunities.</p>
              </section>
            )}

            {error && <p className="opportunity-form-error" role="alert">{error}</p>}
            <footer className="opportunity-form-actions">
              <button
                className="opportunity-previous-button"
                type="button"
                onClick={() => {
                  setError('')
                  if (step === 1) onClose()
                  else setStep((current) => current - 1)
                }}
              >
                <ArrowLeft size={16} aria-hidden="true" /> {step === 1 ? 'Cancel' : 'Previous'}
              </button>
              {step < 5 ? (
                <button className="opportunity-continue-button" type="button" onClick={continueToNextStep}>
                  Continue <ArrowRight size={17} aria-hidden="true" />
                </button>
              ) : (
                <button className="opportunity-continue-button" type="submit" disabled={isPublishing}>
                  <BriefcaseBusiness size={17} aria-hidden="true" />
                  {isPublishing ? 'Saving…' : isEditing ? 'Save changes' : 'Publish opportunity'}
                </button>
              )}
            </footer>
          </div>
        </div>
      </form>
      </section>
    </div>
  )
}
