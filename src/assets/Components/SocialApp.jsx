import { useEffect, useMemo, useRef, useState } from 'react'
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
  { label: 'All cinema types', value: 'All', terms: [] },
  { label: 'Hero / Lead', value: 'Hero', terms: ['hero', 'lead actor', 'leading actor', 'lead actress', 'leading actress', 'protagonist'] },
  { label: 'Child artist', value: 'Child Artist', terms: ['child artist', 'child actor', 'child actress', 'child performer'] },
  { label: 'Side character', value: 'Side Character', terms: ['side character', 'supporting character', 'supporting actor', 'supporting actress', 'character actor'] },
  { label: 'Mother character', value: 'Mother', terms: ['mother character', 'mother role', 'mother'] },
  { label: 'Father character', value: 'Father Character', terms: ['father character', 'father role', 'father'] },
]

const genderFilters = [
  'All genders',
  'Female',
  'Male',
  'Non-binary',
  'Other',
  'Prefer not to say',
]

const skinToneFilters = [
  'All skin tones',
  'White',
  'Brown',
  'Light brown',
  'Dark',
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
}) {
  const profileMenuRef = useRef(null)
  const searchTriggerRef = useRef(null)
  const searchQueryInputRef = useRef(null)
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [talentCategoryDraft, setTalentCategoryDraft] = useState('All')
  const [cinemaTypeDraft, setCinemaTypeDraft] = useState('All')
  const [genderDraft, setGenderDraft] = useState('All genders')
  const [skinToneDraft, setSkinToneDraft] = useState('All skin tones')
  const [searchQueryDraft, setSearchQueryDraft] = useState('')
  const [searchLocationDraft, setSearchLocationDraft] = useState('')
  const [talentDirectory, setTalentDirectory] = useState([])
  const [hasSearched, setHasSearched] = useState(false)
  const [isSearching, setIsSearching] = useState(false)
  const [searchError, setSearchError] = useState('')

  const searchResults = useMemo(() => {
    if (!hasSearched) return []

    const keyword = searchQueryDraft.trim().toLowerCase()
    const location = searchLocationDraft.trim().toLowerCase()
    const selectedCinemaType = cinemaTypeFilters.find((filter) => filter.value === cinemaTypeDraft)

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
      const matchesCinemaType = !selectedCinemaType?.terms.length
        || selectedCinemaType.terms.some((term) => searchableText.includes(term))
      const matchesGender = genderDraft === 'All genders'
        || (talent.gender || '').toLowerCase() === genderDraft.toLowerCase()
      const matchesSkinTone = skinToneDraft === 'All skin tones'
        || (talent.skinTone || '').toLowerCase() === skinToneDraft.toLowerCase()

      return matchesKeyword
        && matchesLocation
        && matchesCategory
        && matchesCinemaType
        && matchesGender
        && matchesSkinTone
    })
  }, [
    cinemaTypeDraft,
    genderDraft,
    hasSearched,
    searchLocationDraft,
    searchQueryDraft,
    skinToneDraft,
    talentCategoryDraft,
    talentDirectory,
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
      const talents = await directoryApi.talents.getAll()
      if (!Array.isArray(talents)) {
        throw new Error('Talent search returned an invalid response.')
      }

      setTalentDirectory(talents)
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
                          setCinemaTypeDraft('All')
                          setGenderDraft('All genders')
                          setSkinToneDraft('All skin tones')
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
                            type="radio"
                            name="cinema-type"
                            value={filter.value}
                            checked={cinemaTypeDraft === filter.value}
                            onChange={() => setCinemaTypeDraft(filter.value)}
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
                            type="radio"
                            name="talent-gender"
                            value={gender}
                            checked={genderDraft === gender}
                            onChange={() => setGenderDraft(gender)}
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
                            type="radio"
                            name="talent-skin-tone"
                            value={skinTone}
                            checked={skinToneDraft === skinTone}
                            onChange={() => setSkinToneDraft(skinTone)}
                          />
                          <span>{skinTone}</span>
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
                    setActiveNavigation('profile')
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
          <a
            className="linkedin-nav-item"
            href="/"
            style={{ textDecoration: 'none', color: 'inherit' }}
            title="Return to TrueHunt Main Dashboard"
          >
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
              <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
            <small>Main Portal</small>
          </a>
        </div>
      </div>
    </nav>
  )
}

export default function SocialApp() {
  const { isAuthenticated } = useAuth()
  const [activeNavigation, setActiveNavigationState] = useState(getNavigationFromHash)
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false)
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
      />

      <Home
        profileDetails={profileDetails}
        profilePreview={profilePreview}
        activeNavigation={activeNavigation}
        setActiveNavigation={setActiveNavigation}
        hidden={!['home', 'opportunities'].includes(activeNavigation)}
        onViewProfile={() => setActiveNavigation('profile')}
      />

      <div hidden={activeNavigation !== 'network'}>
        <NetworkPage onMessage={() => setActiveNavigation('messaging')} />
      </div>
      <div hidden={activeNavigation !== 'messaging'}>
        <Messages />
      </div>
      <div hidden={activeNavigation !== 'endorsement'}>
        <Endorsement
          profileDetails={profileDetails}
          profilePreview={profilePreview}
          onViewProfile={() => setActiveNavigation('profile')}
          onManageNotifications={() => setActiveNavigation('settings')}
        />
      </div>
      <div hidden={activeNavigation !== 'profile'}>
        <ProfilePage
          isOwner={isAuthenticated}
          profileDetails={profileDetails}
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
          profilePreview={profilePreview}
          setProfilePreview={setProfilePreview}
        />
      </div>

      {activeNavigation === 'settings' && (
        <Settings
          profileDetails={profileDetails}
          profilePreview={profilePreview}
          onEditProfile={() => setActiveNavigation('profile')}
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
