import { useEffect, useRef, useState } from 'react'
import './App.css'
import Messages from './assets/Components/Messages'
import Home from './assets/Components/Home'
import NetworkPage from './assets/Components/NetworkPage'
import Endorsement from './assets/Components/Endorsement'
import ProfilePage, { emptyProfileDetails } from './assets/Components/ProfilePage'
import { authApi, profileApi } from './api/client'

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

function Navigation({
  activeNavigation,
  setActiveNavigation,
  isProfileMenuOpen,
  setIsProfileMenuOpen,
  profilePreview,
  profileDetails,
}) {
  const profileMenuRef = useRef(null)

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
          href="#home"
          aria-label="TrueHunt home"
          onClick={(event) => {
            event.preventDefault()
            setActiveNavigation('home')
          }}
        >
          <img className="truehunt-brand" src="/truehunt-logo.png" alt="TrueHunt" />
        </a>
        
        <label className="linkedin-search">
          <NavigationIcon name="search" />
          <input type="search" placeholder="Search" aria-label="Search profiles" />
        </label>
        <div className="linkedin-nav-items">
          <a
            className={`linkedin-nav-item${activeNavigation === 'home' ? ' is-active' : ''}`}
            href="#home"
            onClick={(event) => {
              event.preventDefault()
              setActiveNavigation('home')
            }}
          >
            <NavigationIcon name="home" /><small>Home</small>
          </a>
          <a
            className={`linkedin-nav-item${activeNavigation === 'network' ? ' is-active' : ''}`}
            href="#network"
            onClick={(event) => {
              event.preventDefault()
              setActiveNavigation('network')
            }}
          >
            <NavigationIcon name="network" /><small>Network</small>
          </a>
          <a
            className={`linkedin-nav-item${activeNavigation === 'messaging' ? ' is-active' : ''}`}
            href="#messages"
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
                  : profileDetails?.name?.charAt(0).toUpperCase() || '?'}
              </span>
              <small>Me⌄</small>
            </button>
            {isProfileMenuOpen && (
              <div className="linkedin-profile-dropdown" role="menu">
                <a
                  className="profile-dropdown-person"
                  href="#profile"
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
                      : profileDetails?.name?.charAt(0).toUpperCase() || '?'}
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
                  className="profile-dropdown-item"
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setActiveNavigation('account-settings')
                    setIsProfileMenuOpen(true)
                  }}
                >
                  Account settings
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

function App() {
  const [activeNavigation, setActiveNavigationState] = useState(getNavigationFromHash)
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false)
  const [profileDetails, setProfileDetails] = useState(emptyProfileDetails)
  const [profilePreview, setProfilePreview] = useState('')

  function setActiveNavigation(navigation) {
    const hash = navigationHashes[navigation]
    if (!hash) return

    const nextUrl = `#${hash}`
    if (window.location.hash !== nextUrl) {
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
    async function initProfile() {
      try {
        let token = localStorage.getItem('truehunt_token')
        if (!token) {
          try {
            const loginRes = await authApi.login({ email: 'demo@truehunt.com', password: 'Password123!' })
            if (loginRes?.token) token = loginRes.token
          } catch {
            const regRes = await authApi.register({
              email: 'demo@truehunt.com',
              password: 'Password123!',
              name: 'Chandrabose',
            })
            if (regRes?.token) token = regRes.token
          }
        }
        if (token) {
          const profile = await profileApi.getProfile('me')
          if (profile) {
            setProfileDetails({
              userId: profile.userId || profile.id || '',
              name: profile.name || '',
              phone: profile.phone || '',
              email: profile.email || '',
              experience: profile.experience || '',
              address: profile.address || '',
              languages: Array.isArray(profile.languages) ? profile.languages : [],
              otherLanguage: profile.otherLanguage || '',
              talents: Array.isArray(profile.talents) ? profile.talents : [],
              workTypes: Array.isArray(profile.workTypes) ? profile.workTypes : [],
              videos: Array.isArray(profile.demoVideos) ? profile.demoVideos : [],
              videoUrls: Array.isArray(profile.videoUrls) ? profile.videoUrls : [],
            })
            if (profile.profileImageUrl) {
              setProfilePreview(profile.profileImageUrl)
            }
          }
        }
      } catch (err) {
        console.warn('Initial profile load failed:', err)
      }
    }
    initProfile()
  }, [])

  return (
    <main className={`cinemans-page is-preview${activeNavigation === 'home' ? ' is-home' : ''}`}>
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
        />
      </div>
      <div hidden={activeNavigation !== 'profile'}>
        <ProfilePage
          profileDetails={profileDetails}
          setProfileDetails={setProfileDetails}
          profilePreview={profilePreview}
          setProfilePreview={setProfilePreview}
        />
      </div>

      {['settings', 'account-settings', 'business'].includes(activeNavigation) && (
        <section className="navigation-placeholder">
          <h1>
            {activeNavigation === 'settings'
              ? 'Settings'
              : activeNavigation === 'business' ? 'For Business' : 'Account settings'}
          </h1>
          <p>
            {activeNavigation === 'settings'
              ? 'Profile settings will appear here.'
              : activeNavigation === 'business'
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
                  <button className="home-promotion-button" type="button">Explore film careers</button>
                </div>
              </section>
            </>
          )}
        </section>
      )}
    </main>
  )
}

export default App
