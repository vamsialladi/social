import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import '../../App.css'
import Messages from './Messages'
import Home from './Home'
import NetworkPage from './NetworkPage'
import Endorsement from './Endorsement'
import ProfilePage, { emptyProfileDetails } from './ProfilePage'
import Settings from './Settings'
import { useAuth } from '../../context/AuthContext'
import api from '../../api'
import { api as directoryApi } from '../../services/api'
import { FILM_CRAFTS } from '../../services/mockData'

const navigationHashes = {
  home: 'home',
  network: 'network',
  messaging: 'messages',
  endorsement: 'endorsement',
  opportunities: 'opportunities',
  profile: 'profile',
  settings: 'settings',
  'account-settings': 'account-settings',
  business: 'business',
}

const cinemaTypeFilters = [
  { label: 'Hero / Lead', value: 'Hero', terms: ['hero', 'lead actor', 'leading actor', 'lead actress', 'leading actress', 'protagonist'] },
  { label: 'Child artist', value: 'Child Artist', terms: ['child artist', 'child actor', 'child actress', 'child performer'] },
  { label: 'Side character', value: 'Side Character', terms: ['side character', 'supporting character', 'supporting actor', 'supporting actress', 'character actor'] },
  { label: 'Mother character', value: 'Mother', terms: ['mother character', 'mother role', 'mother'] },
  { label: 'Father character', value: 'Father Character', terms: ['father character', 'father role', 'father']
   },
  { label: 'Villain / Antagonist', value: 'Villain', terms: ['villain', 'antagonist', 'bad guy', 'bad character'] },
  { label: 'Comedian / Comic role', value: 'Comedian', terms: ['comedian', 'comic role', 'comic actor', 'comic actress'] },
  { label: 'Supporting role', value: 'Supporting Role', terms: ['supporting role', 'supporting actor', 'supporting actress'] },
  { label: 'Cameo / Guest appearance', value: 'Cameo', terms: ['cameo', 'guest appearance', 'guest role'] },
  { label: 'Extra / Background artist', value: 'Extra', terms: ['extra', 'background artist', 'background actor', 'background actress'] },
]

const genderFilters = [
  'Female',
  'Male',
  'Non-binary',
  'Other',
  'Prefer not to say',
]

const skinToneFilters = [
  'White',
  'Brown',
  'Light brown',
  'Dark',
]

const bodyBuildFilters = ['Slim', 'Average', 'Athletic', 'Plus-size']

const experienceFilters = [
  { label: '0–2 years', value: '0-2', min: 0, max: 2 },
  { label: '3–5 years', value: '3-5', min: 3, max: 5 },
  { label: '6–10 years', value: '6-10', min: 6, max: 10 },
  { label: '11 - 20 years', value: '11-20', min: 11, max: 20 },
  { label: '20+ years', value: '20+', min: 20, max: Infinity },
]

function getNavigationFromHash() {
  const hash = window.location.hash.slice(1)
  const navigation = Object.entries(navigationHashes)
    .find(([, route]) => route === hash)?.[0]

  return navigation || (hash === 'artist-profile' ? 'profile' : 'home')
}

function NavigationIcon({ name }) {
  const iconPaths = {
    search: <><circle cx="11" cy="11" r="7" /><path d="m16 16 4 4" /></>,
    home: <><path d="m3 10 9-7 9 7" /><path d="M5 9v11h14V9M9 20v-6h6v6" /></>,
    network: <><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3.5 20v-1.5A5.5 5.5 0 0 1 9 13h0a5.5 5.5 0 0 1 5.5 5.5V20zM15 14a4.5 4.5 0 0 1 5.5 4.5V20h-4" /></>,
    messaging: <><path d="M16 4H8a7 7 0 0 0 0 14h4v4l8.16-5.39A6.78 6.78 0 0 0 23 11a7 7 0 0 0-7-7m-8 8.25A1.25 1.25 0 1 1 9.25 11 1.25 1.25 0 0 1 8 12.25m4 0A1.25 1.25 0 1 1 13.25 11 1.25 1.25 0 0 1 12 12.25m4 0A1.25 1.25 0 1 1 17.25 11 1.25 1.25 0 0 1 16 12.25"/><path d="M7.5 11.5h.01M12 11.5h.01M16.5 11.5h.01" /></>,
    endorsement: <path d="M21 19v1H3v-1l2-2v-6c0-3.1 2.03-5.83 5-6.71V4a2 2 0 0 1 2-2a2 2 0 0 1 2 2v.29c2.97.88 5 3.61 5 6.71v6zm-7 2a2 2 0 0 1-2 2a2 2 0 0 1-2-2" />,
    opportunities: <><rect x="3" y="7" width="18" height="13" rx="2" /><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 12h18M10 12v2h4v-2" /></>,
    business: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  }

  return (
    <svg
      className="linkedin-nav-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {iconPaths[name]}
    </svg>
  )
}

function ProfileAvatarPlaceholder() {
  return (
    <svg viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="35" r="21" />
      <path d="M10 96c2-23 18-39 40-39s38 16 40 39H10Z" />
    </svg>
  )
}

function Navigation({
  activeNavigation,
  setActiveNavigation,
  isProfileMenuOpen,
  setIsProfileMenuOpen,
  profilePreview,
  profileDetails,
  onCandidateSelected,
  onShowMyProfile,
}) {
  const profileMenuRef = useRef(null)
  const searchTriggerRef = useRef(null)
  const searchQueryInputRef = useRef(null)
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [talentCategoryDraft, setTalentCategoryDraft] = useState('All')
  const [cinemaTypeDraft, setCinemaTypeDraft] = useState([])
  const [genderDraft, setGenderDraft] = useState([])
  const [skinToneDraft, setSkinToneDraft] = useState([])
  const [bodyBuildDraft, setBodyBuildDraft] = useState([])
  const [heightRangeDraft, setHeightRangeDraft] = useState({ min: '', max: '' })
  const [weightRangeDraft, setWeightRangeDraft] = useState({ min: '', max: '' })
  const [experienceDraft, setExperienceDraft] = useState([])
  const [searchQueryDraft, setSearchQueryDraft] = useState('')
  const [searchLocationDraft, setSearchLocationDraft] = useState('')
  const [talentDirectory, setTalentDirectory] = useState([])
  const [hasSearched, setHasSearched] = useState(false)
  const [isSearching, setIsSearching] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [referralId, setReferralId] = useState('')
  const [referralSearchMessage, setReferralSearchMessage] = useState('')
  const [isReferralSearching, setIsReferralSearching] = useState(false)

  const searchResults = useMemo(() => {
    if (!hasSearched) return []

    const keyword = searchQueryDraft.trim().toLowerCase()
    const location = searchLocationDraft.trim().toLowerCase()
    const selectedCinemaTypes = cinemaTypeFilters.filter((filter) => cinemaTypeDraft.includes(filter.value))
    const selectedExperienceRanges = experienceFilters.filter((filter) => experienceDraft.includes(filter.value))

    return talentDirectory.filter((talent) => {
      const searchableText = [
        talent.name,
        talent.role,
        talent.headline,
        talent.category,
        talent.company,
        talent.industry,
        talent.location,
        talent.bio,
        ...(talent.skills || []),
      ].filter(Boolean).join(' ').toLowerCase()
      const matchesKeyword = !keyword || searchableText.includes(keyword)
      const matchesLocation = !location || (talent.location || '').toLowerCase().includes(location)
      const matchesCategory = talentCategoryDraft === 'All'
        || (talent.category || '').toLowerCase() === talentCategoryDraft.toLowerCase()
      const matchesCinemaType = selectedCinemaTypes.length === 0
        || selectedCinemaTypes.some((filter) => filter.terms.some((term) => searchableText.includes(term)))
      const matchesGender = genderDraft.length === 0
        || genderDraft.some((gender) => (talent.gender || '').toLowerCase() === gender.toLowerCase())
      const matchesSkinTone = skinToneDraft.length === 0
        || skinToneDraft.some((skinTone) => (talent.skinTone || '').toLowerCase() === skinTone.toLowerCase())
      const bodyBuild = (talent.bodyBuild || '').toLowerCase()
      const matchesBodyBuild = bodyBuildDraft.length === 0
        || bodyBuildDraft.some((build) => bodyBuild === build.toLowerCase())
      const height = Number.parseFloat(String(talent.height ?? ''))
      const minHeight = heightRangeDraft.min === '' ? null : Number(heightRangeDraft.min)
      const maxHeight = heightRangeDraft.max === '' ? null : Number(heightRangeDraft.max)
      const matchesHeight = (minHeight === null && maxHeight === null)
        || (Number.isFinite(height)
          && (minHeight === null || height >= minHeight)
          && (maxHeight === null || height <= maxHeight))
      const weight = Number.parseFloat(String(talent.weight ?? ''))
      const minWeight = weightRangeDraft.min === '' ? null : Number(weightRangeDraft.min)
      const maxWeight = weightRangeDraft.max === '' ? null : Number(weightRangeDraft.max)
      const matchesWeight = (minWeight === null && maxWeight === null)
        || (Number.isFinite(weight)
          && (minWeight === null || weight >= minWeight)
          && (maxWeight === null || weight <= maxWeight))
      const experienceYears = Number.parseFloat(String(talent.experience || '').match(/\d+(?:\.\d+)?/)?.[0])
      const matchesExperience = selectedExperienceRanges.length === 0
        || (Number.isFinite(experienceYears)
          && selectedExperienceRanges.some((range) => experienceYears >= range.min && experienceYears <= range.max))

      return matchesKeyword
        && matchesLocation
        && matchesCategory
        && matchesCinemaType
        && matchesGender
        && matchesSkinTone
        && matchesBodyBuild
        && matchesHeight
        && matchesWeight
        && matchesExperience
    })
  }, [
    cinemaTypeDraft,
    genderDraft,
    hasSearched,
    experienceDraft,
    bodyBuildDraft,
    heightRangeDraft,
    searchLocationDraft,
    searchQueryDraft,
    skinToneDraft,
    talentCategoryDraft,
    talentDirectory,
    weightRangeDraft,
  ])

  useEffect(() => {
    if (!isSearchOpen) return undefined

    searchQueryInputRef.current?.focus()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleSearchKeyDown(event) {
      if (event.key === 'Escape') {
        setIsSearchOpen(false)
        searchTriggerRef.current?.focus()
      }
    }

    document.addEventListener('keydown', handleSearchKeyDown)
    return () => {
      document.removeEventListener('keydown', handleSearchKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [isSearchOpen])

  async function handleSearchSubmit(event) {
    event.preventDefault()
    setIsSearching(true)
    setSearchError('')

    try {
      const [talents, profiles] = await Promise.all([
        directoryApi.talents.getAll(),
        directoryApi.profile.getDirectory(),
      ])
      if (!Array.isArray(talents) || !Array.isArray(profiles)) {
        throw new Error('Talent search returned an invalid response.')
      }

      const directory = [...talents]
      profiles.forEach((profile) => {
        const existingIndex = directory.findIndex((talent) => (
          talent.id === profile.id
          || talent.name?.trim().toLowerCase() === profile.name?.trim().toLowerCase()
        ))

        if (existingIndex === -1) {
          directory.push(profile)
        } else {
          directory[existingIndex] = { ...directory[existingIndex], ...profile }
        }
      })

      setTalentDirectory(directory)
      setHasSearched(true)
    } catch (err) {
      console.error('Failed to search talent profiles:', err)
      setSearchError('Could not search talent profiles. Please try again.')
      setTalentDirectory([])
      setHasSearched(false)
    } finally {
      setIsSearching(false)
    }
  }

  async function handleReferralSearch(event) {
    event.preventDefault()
    const query = referralId.trim().toLowerCase()
    if (!query) {
      setReferralSearchMessage('Enter a Refer ID to search.')
      return
    }

    setIsReferralSearching(true)
    setReferralSearchMessage('')
    try {
      const profiles = await directoryApi.profile.getDirectory()
      if (!Array.isArray(profiles)) {
        throw new Error('Refer ID search returned an invalid response.')
      }

      const person = profiles.find((entry) => (
        ['referId', 'referID', 'refer_id', 'referralId', 'referralID', 'referral_id', 'referenceId', 'referenceID', 'referralCode', 'userId', 'id']
          .some((key) => String(entry?.[key] ?? '').trim().toLowerCase() === query)
      ))
      const profileId = person?.userId || person?.id
      if (!profileId) {
        setReferralSearchMessage('No profile was found for that Refer ID.')
        return
      }

      const candidateProfile = await api.getProfileById(profileId)
      if (!candidateProfile?.name) {
        setReferralSearchMessage('No candidate profile was found for that Refer ID.')
        return
      }

      onCandidateSelected(candidateProfile)
    } catch (error) {
      console.error('Failed to search profile by Refer ID:', error)
      setReferralSearchMessage('Could not search by Refer ID. Please try again.')
    } finally {
      setIsReferralSearching(false)
    }
  }

  useEffect(() => {
    if (!isProfileMenuOpen) return undefined

    function handlePointerDown(event) {
      if (!profileMenuRef.current?.contains(event.target)) {
        setIsProfileMenuOpen(false)
      }
    }

    function handleKeyDown(event) {
      if (event.key === 'Escape') setIsProfileMenuOpen(false)
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isProfileMenuOpen, setIsProfileMenuOpen])

  return (
    <nav className="linkedin-navbar" aria-label="Profile navigation">
      <div className="linkedin-navbar-inner">
        <a
          href="/social#home"
          aria-label="TrueHunt home"
          onClick={(event) => {
            event.preventDefault()
            setActiveNavigation('home')
          }}
        >
          <img className="truehunt-brand" src="/truehunt-logo.png" alt="TrueHunt" />
        </a>
        
        <div
          className="linkedin-search-wrap"
          style={{ position: 'relative' }}
        >
          <label className="linkedin-search">
            <NavigationIcon name="search" />
            <input
              ref={searchTriggerRef}
              type="text"
              placeholder="Search talent profiles..."
              aria-label="Search talent profiles"
              aria-haspopup="dialog"
              aria-expanded={isSearchOpen}
              readOnly
              onClick={() => {
                setTalentDirectory([])
                setSearchError('')
                setHasSearched(false)
                setIsSearchOpen(true)
              }}
            />
          </label>
          {isSearchOpen && (
            <div
              className="social-talent-search-overlay"
            >
              <form
                role="dialog"
                aria-modal="true"
                aria-labelledby="social-talent-search-title"
                onSubmit={handleSearchSubmit}
                className={`social-talent-search${hasSearched ? ' has-results' : ''}`}
              >
                  <header className="social-talent-search-topbar">
                    <a
                      className="social-talent-search-brand"
                      href="/social#home"
                      aria-label="TrueHunt home"
                      onClick={(event) => {
                        event.preventDefault()
                        setIsSearchOpen(false)
                        setActiveNavigation('home')
                      }}
                    >
                      <img src="/truehunt-logo.png" alt="TrueHunt" />
                    </a>
                    <button className="social-talent-search-close" type="button" onClick={() => setIsSearchOpen(false)} aria-label="Close search">
                      ×
                    </button>
                  </header>
                  <h2 className="social-talent-search-title" id="social-talent-search-title">Find cinema professionals</h2>
                  <div className={`social-talent-search-layout${hasSearched ? ' has-results' : ''}`}>
                    {hasSearched && (
                    <aside className="social-talent-filters" aria-label="Candidate filters">
                    <div className="social-talent-filter-heading">
                      <h3>Filters</h3>
                      <button
                        type="button"
                        onClick={() => {
                          setTalentCategoryDraft('All')
                          setCinemaTypeDraft([])
                          setGenderDraft([])
                          setSkinToneDraft([])
                          setBodyBuildDraft([])
                          setHeightRangeDraft({ min: '', max: '' })
                          setWeightRangeDraft({ min: '', max: '' })
                          setExperienceDraft([])
                          setSearchQueryDraft('')
                          setSearchLocationDraft('')
                          setHasSearched(false)
                        }}
                      >
                        Clear all
                      </button>
                    </div>
                    <fieldset className="social-talent-filter-group">
                      <legend>Cinema type</legend>
                      {cinemaTypeFilters.map((filter) => (
                        <label className="social-talent-filter-option" key={filter.value}>
                          <input
                            type="checkbox"
                            name="cinema-type"
                            value={filter.value}
                            checked={cinemaTypeDraft.includes(filter.value)}
                            onChange={() => setCinemaTypeDraft((selected) => (
                              selected.includes(filter.value)
                                ? selected.filter((value) => value !== filter.value)
                                : [...selected, filter.value]
                            ))}
                          />
                          <span>{filter.label}</span>
                        </label>
                      ))}
                    </fieldset>
                    <fieldset className="social-talent-filter-group">
                      <legend>Gender</legend>
                      {genderFilters.map((gender) => (
                        <label className="social-talent-filter-option" key={gender}>
                          <input
                            type="checkbox"
                            name="talent-gender"
                            value={gender}
                            checked={genderDraft.includes(gender)}
                            onChange={() => setGenderDraft((selected) => (
                              selected.includes(gender)
                                ? selected.filter((value) => value !== gender)
                                : [...selected, gender]
                            ))}
                          />
                          <span>{gender}</span>
                        </label>
                      ))}
                    </fieldset>
                    <fieldset className="social-talent-filter-group">
                      <legend>Skin tone</legend>
                      {skinToneFilters.map((skinTone) => (
                        <label className="social-talent-filter-option" key={skinTone}>
                          <input
                            type="checkbox"
                            name="talent-skin-tone"
                            value={skinTone}
                            checked={skinToneDraft.includes(skinTone)}
                            onChange={() => setSkinToneDraft((selected) => (
                              selected.includes(skinTone)
                                ? selected.filter((value) => value !== skinTone)
                                : [...selected, skinTone]
                            ))}
                          />
                          <span>{skinTone}</span>
                        </label>
                      ))}
                    </fieldset>
                    <fieldset className="social-talent-filter-group">
                      <legend>Body build</legend>
                      {bodyBuildFilters.map((build) => (
                        <label className="social-talent-filter-option" key={build}>
                          <input
                            type="checkbox"
                            name="talent-body-build"
                            value={build}
                            checked={bodyBuildDraft.includes(build)}
                            onChange={() => setBodyBuildDraft((selected) => (
                              selected.includes(build)
                                ? selected.filter((value) => value !== build)
                                : [...selected, build]
                            ))}
                          />
                          <span>{build}</span>
                        </label>
                      ))}
                    </fieldset>
                    <fieldset className="social-talent-filter-group">
                      <legend>Height (cm)</legend>
                      <div className="social-talent-filter-range">
                        <label>
                          Min
                          <input
                            type="number"
                            min="0"
                            step="0.1"
                            aria-label="Minimum height in centimeters"
                            placeholder="Min"
                            value={heightRangeDraft.min}
                            onChange={(event) => setHeightRangeDraft((range) => ({ ...range, min: event.target.value }))}
                          />
                        </label>
                        <label>
                          Max
                          <input
                            type="number"
                            min="0"
                            step="0.1"
                            aria-label="Maximum height in centimeters"
                            placeholder="Max"
                            value={heightRangeDraft.max}
                            onChange={(event) => setHeightRangeDraft((range) => ({ ...range, max: event.target.value }))}
                          />
                        </label>
                      </div>
                    </fieldset>
                    <fieldset className="social-talent-filter-group">
                      <legend>Weight (kg)</legend>
                      <div className="social-talent-filter-range">
                        <label>
                          Min
                          <input
                            type="number"
                            min="0"
                            step="0.1"
                            aria-label="Minimum weight in kilograms"
                            placeholder="Min"
                            value={weightRangeDraft.min}
                            onChange={(event) => setWeightRangeDraft((range) => ({ ...range, min: event.target.value }))}
                          />
                        </label>
                        <label>
                          Max
                          <input
                            type="number"
                            min="0"
                            step="0.1"
                            aria-label="Maximum weight in kilograms"
                            placeholder="Max"
                            value={weightRangeDraft.max}
                            onChange={(event) => setWeightRangeDraft((range) => ({ ...range, max: event.target.value }))}
                          />
                        </label>
                      </div>
                    </fieldset>
                    <fieldset className="social-talent-filter-group">
                      <legend>Experience</legend>
                      {experienceFilters.map((filter) => (
                        <label className="social-talent-filter-option" key={filter.value}>
                          <input
                            type="checkbox"
                            name="talent-experience"
                            value={filter.value}
                            checked={experienceDraft.includes(filter.value)}
                            onChange={() => setExperienceDraft((selected) => (
                              selected.includes(filter.value)
                                ? selected.filter((value) => value !== filter.value)
                                : [...selected, filter.value]
                            ))}
                          />
                          <span>{filter.label}</span>
                        </label>
                      ))}
                    </fieldset>
                  </aside>
                  )}
                  <main className="social-talent-search-main">
                    <div className="social-talent-search-fields">
                      <div className="social-talent-search-field">
                        <label htmlFor="social-search-category">Film craft</label>
                        <select
                          id="social-search-category"
                          aria-label="Film craft"
                          value={talentCategoryDraft}
                          onChange={(event) => setTalentCategoryDraft(event.target.value)}
                        >
                          <option value="All">All crafts</option>
                          {FILM_CRAFTS.map((craft) => (
                            <option key={craft.id} value={craft.title}>{craft.title}</option>
                          ))}
                        </select>
                      </div>
                      <div className="social-talent-search-field">
                        <label htmlFor="social-search-keyword">Role, name, or skill</label>
                        <input ref={searchQueryInputRef} id="social-search-keyword" type="search" placeholder="e.g. cinematographer, editor" value={searchQueryDraft} onChange={(event) => setSearchQueryDraft(event.target.value)} />
                      </div>
                      <div className="social-talent-search-field">
                        <label htmlFor="social-search-location">Location</label>
                        <input id="social-search-location" type="text" placeholder="City or region" value={searchLocationDraft} onChange={(event) => setSearchLocationDraft(event.target.value)} />
                      </div>
                      <button type="submit" className="social-talent-search-submit" disabled={isSearching}>
                        <NavigationIcon name="search" />
                        {isSearching ? 'Searching…' : 'Search'}
                      </button>
                    </div>
                    {searchError && <p className="social-talent-search-error" role="alert">{searchError}</p>}
                    {isSearching && (
                      <p className="social-talent-search-status" role="status">Finding matching professionals…</p>
                    )}
                    {!isSearching && hasSearched && searchResults.length > 0 && (
                      <section className="social-talent-results" aria-live="polite">
                        <div className="social-talent-results-heading">
                          <div>
                            <h3>Matching professionals</h3>
                            <p>Profiles matching your selected filters</p>
                          </div>
                          <span>{searchResults.length} {searchResults.length === 1 ? 'profile' : 'profiles'}</span>
                        </div>
                        <div className="social-talent-results-list">
                          {searchResults.map((talent) => (
                            <article className="social-talent-result-card" key={talent.id}>
                              {talent.avatar
                                ? <img className="social-talent-result-avatar" src={talent.avatar} alt="" />
                                : (
                                  <span className="social-talent-result-avatar social-talent-result-initials" aria-hidden="true">
                                    {(talent.name || 'Talent').split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()}
                                  </span>
                                )}
                              <div className="social-talent-result-content">
                                <div className="social-talent-result-name-row">
                                  <h4>{talent.name || 'Creative professional'}</h4>
                                  {talent.verified && <span className="social-talent-verified">Verified</span>}
                                </div>
                                <p className="social-talent-result-role">{talent.role || talent.headline || talent.category}</p>
                                <p className="social-talent-result-meta">
                                  {[talent.category, talent.location, talent.experience].filter(Boolean).join(' · ')}
                                </p>
                                {talent.bio && <p className="social-talent-result-bio">{talent.bio}</p>}
                                {talent.skills?.length > 0 && (
                                  <div className="social-talent-result-skills">
                                    {talent.skills.slice(0, 3).map((skill) => <span key={skill}>{skill}</span>)}
                                  </div>
                                )}
                              </div>
                            </article>
                          ))}
                        </div>
                      </section>
                    )}
                    {!isSearching && hasSearched && searchResults.length === 0 && !searchError && (
                      <div className="social-talent-search-empty" role="status">
                        <strong>No matching profiles yet</strong>
                        <span>Try a different cinema type, craft, keyword, or location.</span>
                      </div>
                    )}
                  </main>
                </div>
              </form>
            </div>
          )}
        </div>
        <div className="linkedin-refer-search-wrap">
          <form
            className="linkedin-refer-search"
            onSubmit={handleReferralSearch}
            role="search"
            aria-label="Search profiles by Refer ID"
            aria-busy={isReferralSearching}
          >
            <input
              type="search"
              placeholder={isReferralSearching ? 'Searching...' : 'Search by Refer ID'}
              aria-label="Search by Refer ID"
              value={referralId}
              onChange={(event) => {
                setReferralId(event.target.value)
                setReferralSearchMessage('')
              }}
            />
            <button type="submit" aria-label="Search by Refer ID" disabled={isReferralSearching}>
              <NavigationIcon name="search" />
            </button>
          </form>
          {referralSearchMessage && (
            <span className="linkedin-refer-search-message" role="status">
              {referralSearchMessage}
            </span>
          )}
        </div>
        <div className="linkedin-nav-items">
          <a
            className={`linkedin-nav-item${activeNavigation === 'home' ? ' is-active' : ''}`}
            href="/social#home"
            onClick={(event) => {
              event.preventDefault()
              setActiveNavigation('home')
            }}
          >
            <NavigationIcon name="home" /><small>Home</small>
          </a>
          <a
            className={`linkedin-nav-item${activeNavigation === 'network' ? ' is-active' : ''}`}
            href="/social#network"
            onClick={(event) => {
              event.preventDefault()
              setActiveNavigation('network')
            }}
          >
            <NavigationIcon name="network" /><small>Network</small>
          </a>
          <a
            className={`linkedin-nav-item${activeNavigation === 'messaging' ? ' is-active' : ''}`}
            href="/social#messages"
            onClick={(event) => {
              event.preventDefault()
              setActiveNavigation('messaging')
            }}
          >
            <NavigationIcon name="messaging" /><small>Messaging</small>
          </a>
          <button
            className={`linkedin-nav-item${activeNavigation === 'endorsement' ? ' is-active' : ''}`}
            type="button"
            onClick={() => setActiveNavigation('endorsement')}
            aria-current={activeNavigation === 'endorsement' ? 'page' : undefined}
          >
            <NavigationIcon name="endorsement" /><small>Endorsement</small>
          </button>
          <button
            className={`linkedin-nav-item${activeNavigation === 'opportunities' ? ' is-active' : ''}`}
            type="button"
            onClick={() => setActiveNavigation('opportunities')}
            aria-current={activeNavigation === 'opportunities' ? 'page' : undefined}
          >
            <NavigationIcon name="opportunities" /><small>Opportunities</small>
          </button>
          <div className={`linkedin-profile-menu${isProfileMenuOpen ? ' is-open' : ''}`} ref={profileMenuRef}>
            <button
              className={`linkedin-nav-item linkedin-me${['profile', 'settings', 'account-settings'].includes(activeNavigation) ? ' is-active' : ''}`}
              type="button"
              onClick={() => setIsProfileMenuOpen((open) => !open)}
              aria-expanded={isProfileMenuOpen}
              aria-haspopup="menu"
            >
              <span className="linkedin-mini-avatar" aria-hidden="true">
                {profilePreview
                  ? <img src={profilePreview} alt="" />
                  : <ProfileAvatarPlaceholder />}
              </span>
              <small>Profile</small>
            </button>
            {isProfileMenuOpen && (
              <div className="linkedin-profile-dropdown" role="menu">
                <a
                  className="profile-dropdown-person"
                  href="/social#profile"
                  role="menuitem"
                  onClick={(event) => {
                    event.preventDefault()
                    onShowMyProfile()
                    setIsProfileMenuOpen(false)
                  }}
                >
                  <span className="linkedin-mini-avatar" aria-hidden="true">
                    {profilePreview
                      ? <img src={profilePreview} alt="" />
                      : <ProfileAvatarPlaceholder />}
                  </span>
                  <span>
                    <strong>{profileDetails.name || 'Your name'}</strong>
                    <small>My profile</small>
                  </span>
                </a>
                <div className="profile-dropdown-divider" />
                <button
                  className="profile-dropdown-item"
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setActiveNavigation('settings')
                    setIsProfileMenuOpen(false)
                  }}
                >
                  Settings
                </button>
                <button
                  className="profile-dropdown-item profile-dropdown-logout"
                  type="button"
                  role="menuitem"
                  onClick={async () => {
                    try {
                      await api.logout()
                    } catch (e) {
                      console.warn('Logout note:', e)
                    }
                    localStorage.removeItem('truehunt_token');
                    localStorage.removeItem('truehunt_user');
                    setIsProfileMenuOpen(false)
                    window.location.href = '/';
                  }}
                >
                  Log out
                </button>
              </div>
            )}
          </div>
          <span className="linkedin-nav-divider" aria-hidden="true" />
          <button
            className={`linkedin-nav-item linkedin-business${activeNavigation === 'business' ? ' is-active' : ''}`}
            type="button"
            onClick={() => setActiveNavigation('business')}
            aria-current={activeNavigation === 'business' ? 'page' : undefined}
          >
            <NavigationIcon name="business" /><small>For Business</small>
          </button>
        </div>
      </div>
    </nav>
  )
}

export default function SocialApp() {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()
  const [activeNavigation, setActiveNavigationState] = useState(getNavigationFromHash)
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false)
  const [viewingCandidateProfile, setViewingCandidateProfile] = useState(null)
  const [networkConversation, setNetworkConversation] = useState(() => {
    const requestedConversation = location.state?.openNetworkConversation
    if (requestedConversation) {
      return { profile: requestedConversation, requestId: Date.now() }
    }

    try {
      const savedConversation = sessionStorage.getItem('truehunt_network_conversation')
      if (!savedConversation) return null

      sessionStorage.removeItem('truehunt_network_conversation')
      return { profile: JSON.parse(savedConversation), requestId: Date.now() }
    } catch (error) {
      console.warn('Could not restore the requested network conversation:', error)
      return null
    }
  })
  const openConversationRef = useRef(null)
  const [theme, setTheme] = useState('light')
  const [profileDetails, setProfileDetails] = useState(() => {
    try {
      const savedLocal = localStorage.getItem('truehunt_social_profile') || localStorage.getItem('truehunt_profile_details')
      if (savedLocal) {
        return { ...emptyProfileDetails, ...JSON.parse(savedLocal) }
      }
    } catch (e) {
      // fallback
    }
    return emptyProfileDetails
  })
  const [profilePreview, setProfilePreview] = useState(() => {
    try {
      const savedLocal = localStorage.getItem('truehunt_social_profile') || localStorage.getItem('truehunt_profile_details')
      if (savedLocal) {
        const parsed = JSON.parse(savedLocal)
        return parsed.profileImageUrl || ''
      }
    } catch (e) {
      // fallback
    }
    return ''
  })

  function setActiveNavigation(navigation) {
    const hash = navigationHashes[navigation]
    if (!hash) return

    const nextUrl = `/social#${hash}`
    if (window.location.hash !== `#${hash}`) {
      window.history.pushState({ navigation }, '', nextUrl)
    }
    setActiveNavigationState(navigation)
  }

  function showMyProfile() {
    setViewingCandidateProfile(null)
    setActiveNavigation('profile')
  }

  function showCandidateProfile(candidateProfile) {
    setViewingCandidateProfile({
      ...emptyProfileDetails,
      ...candidateProfile,
      userId: candidateProfile.userId || candidateProfile.id || '',
    })
    setActiveNavigation('profile')
  }

  useEffect(() => {
    function restoreNavigation() {
      setActiveNavigationState(getNavigationFromHash())
    }

    window.addEventListener('popstate', restoreNavigation)
    window.addEventListener('hashchange', restoreNavigation)
    return () => {
      window.removeEventListener('popstate', restoreNavigation)
      window.removeEventListener('hashchange', restoreNavigation)
    }
  }, [])

  useEffect(() => {
    // 1. Immediately read cached social profile from localStorage (e.g. from JoinNow registration)
    const savedLocal = localStorage.getItem('truehunt_social_profile') || localStorage.getItem('truehunt_profile_details')
    if (savedLocal) {
      try {
        const parsed = JSON.parse(savedLocal)
        setProfileDetails((prev) => ({ ...prev, ...parsed }))
        if (parsed.profileImageUrl) {
          setProfilePreview(parsed.profileImageUrl)
        }
      } catch (e) {
        console.warn('Error reading saved local profile:', e)
      }
    }

    // 2. Also sync with backend API
    async function loadBackendData() {
      try {
        const profile = await api.getProfile()
        if (profile && profile.name) {
          setProfileDetails((prev) => ({ ...prev, ...profile }))
          if (profile.profileImageUrl) {
            setProfilePreview(profile.profileImageUrl)
          }
        }
      } catch (err) {
        console.warn('Backend profile fetch note:', err)
      }
    }
    loadBackendData()
  }, [])

  return (
    <main className={`cinemans-page is-preview${activeNavigation === 'home' ? ' is-home' : ''}${theme === 'dark' ? ' is-dark' : ''}`}>
      <Navigation
        activeNavigation={activeNavigation}
        setActiveNavigation={setActiveNavigation}
        isProfileMenuOpen={isProfileMenuOpen}
        setIsProfileMenuOpen={setIsProfileMenuOpen}
        profilePreview={profilePreview}
        profileDetails={profileDetails}
        onCandidateSelected={showCandidateProfile}
        onShowMyProfile={showMyProfile}
      />

      <Home
        profileDetails={profileDetails}
        profilePreview={profilePreview}
        activeNavigation={activeNavigation}
        setActiveNavigation={setActiveNavigation}
        hidden={!['home', 'opportunities'].includes(activeNavigation)}
        onViewProfile={showMyProfile}
      />

      <div hidden={activeNavigation !== 'network'}>
        <NetworkPage
          key={networkConversation?.requestId || 'network-page'}
          initialConversation={networkConversation?.profile || location.state?.openNetworkConversation}
        />
      </div>
      <div hidden={activeNavigation !== 'messaging'}>
        <Messages
          conversationToOpen={location.state?.openConversation}
          openConversationRef={openConversationRef}
        />
      </div>
      <div hidden={activeNavigation !== 'endorsement'}>
        <Endorsement
          profileDetails={profileDetails}
          profilePreview={profilePreview}
          onViewProfile={showMyProfile}
          onManageNotifications={() => setActiveNavigation('settings')}
        />
      </div>
      <div hidden={activeNavigation !== 'profile'}>
        <ProfilePage
          isOwner={!viewingCandidateProfile && isAuthenticated}
          accountUserId={user?.userId || ''}
          profileDetails={viewingCandidateProfile || profileDetails}
          setProfileDetails={(updater) => {
            setProfileDetails((prev) => {
              const updated = typeof updater === 'function' ? updater(prev) : updater
              try {
                localStorage.setItem('truehunt_social_profile', JSON.stringify(updated))
                localStorage.setItem('truehunt_profile_details', JSON.stringify(updated))
              } catch (e) {
                // ignore
              }
              return updated
            })
          }}
          profilePreview={viewingCandidateProfile?.profileImageUrl || viewingCandidateProfile?.avatar || profilePreview}
          setProfilePreview={setProfilePreview}
          onMessage={(recipient) => {
            setNetworkConversation({
              profile: recipient,
              requestId: Date.now(),
            })
            setActiveNavigation('network')
          }}
        />
      </div>

      {activeNavigation === 'settings' && (
        <Settings
          profileDetails={profileDetails}
          profilePreview={profilePreview}
          onEditProfile={showMyProfile}
          theme={theme}
          onThemeChange={setTheme}
        />
      )}

      {['account-settings', 'business'].includes(activeNavigation) && (
        <section className="navigation-placeholder">
          <h1>
            {activeNavigation === 'business' ? 'For Business' : 'Account settings'}
          </h1>
          <p>
            {activeNavigation === 'business'
              ? 'Business tools will appear here.'
              : 'Account settings will appear here.'}
          </p>
          {activeNavigation === 'business' && (
            <>
              <button className="secondary-button" type="button" onClick={() => setActiveNavigation('profile')}>
                Return to profile
              </button>
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
                        const res = await api.clickPromotion("Business Promotion - Northstar Films")
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
            </>
          )}
        </section>
      )}
    </main>
  )
}
