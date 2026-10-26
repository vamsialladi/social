import { useEffect, useRef, useState } from 'react'
import '../../App.css'
import Messages from './Messages'
import Home from './Home'
import NetworkPage from './NetworkPage'
import Endorsement from './Endorsement'
import ProfilePage, { emptyProfileDetails } from './ProfilePage'
import Settings from './Settings'
import api from '../../api'

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
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState(null)
  const [isSearching, setIsSearching] = useState(false)
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const searchContainerRef = useRef(null)

  useEffect(() => {
    function handleOutside(e) {
      if (!searchContainerRef.current?.contains(e.target)) {
        setIsSearchOpen(false)
      }
    }
    document.addEventListener('pointerdown', handleOutside)
    return () => document.removeEventListener('pointerdown', handleOutside)
  }, [])

  async function handleSearch(event) {
    if (event.key === 'Enter') {
      event.preventDefault()
      const q = searchQuery.trim()
      if (!q) return
      setIsSearching(true)
      setIsSearchOpen(true)
      try {
        const res = await api.search(q)
        setSearchResults(res)
      } catch (err) {
        console.warn('Search error:', err)
      } finally {
        setIsSearching(false)
      }
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
        
        <div className="linkedin-search-wrap" ref={searchContainerRef} style={{ position: 'relative' }}>
          <label className="linkedin-search">
            <NavigationIcon name="search" />
            <input
              type="search"
              placeholder="Search people, posts..."
              aria-label="Search profiles"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={handleSearch}
              onFocus={() => {
                if (searchResults) setIsSearchOpen(true)
              }}
            />
          </label>
          {isSearchOpen && (
            <div
              className="search-results-dropdown"
              style={{
                position: 'absolute',
                top: 'calc(100% + 4px)',
                left: 0,
                width: '320px',
                background: 'var(--color-surface, #fff)',
                border: '1px solid #ccc',
                borderRadius: '8px',
                boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
                zIndex: 100,
                padding: '12px',
                maxHeight: '380px',
                overflowY: 'auto',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px', borderBottom: '1px solid #eee', paddingBottom: '6px' }}>
                <strong style={{ fontSize: '13px' }}>
                  {isSearching ? 'Searching...' : `Results (${searchResults?.totalResults || 0})`}
                </strong>
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', color: '#666' }}
                  onClick={() => setIsSearchOpen(false)}
                >
                  ✕
                </button>
              </div>

              {searchResults?.people && searchResults.people.length > 0 && (
                <div style={{ marginBottom: '12px' }}>
                  <div style={{ fontSize: '11px', fontWeight: 'bold', textTransform: 'uppercase', color: '#888', marginBottom: '6px' }}>
                    People
                  </div>
                  {searchResults.people.map((p) => (
                    <div
                      key={p.id}
                      style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid #f5f5f5' }}
                    >
                      <div>
                        <div style={{ fontSize: '13px', fontWeight: '600' }}>{p.name}</div>
                        <div style={{ fontSize: '11px', color: '#666' }}>{p.role}</div>
                      </div>
                      <button
                        type="button"
                        style={{ background: '#1a73e8', color: '#fff', border: 'none', borderRadius: '12px', padding: '3px 10px', fontSize: '12px', cursor: 'pointer' }}
                        onClick={() => {
                          setIsSearchOpen(false)
                          setActiveNavigation('network')
                        }}
                      >
                        View
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {searchResults?.posts && searchResults.posts.length > 0 && (
                <div>
                  <div style={{ fontSize: '11px', fontWeight: 'bold', textTransform: 'uppercase', color: '#888', marginBottom: '6px' }}>
                    Posts & Opportunities
                  </div>
                  {searchResults.posts.map((post) => (
                    <div
                      key={post.id}
                      style={{ padding: '6px 0', borderBottom: '1px solid #f5f5f5', cursor: 'pointer' }}
                      onClick={() => {
                        setIsSearchOpen(false)
                        setActiveNavigation(post.type === 'opportunity' ? 'opportunities' : 'home')
                      }}
                    >
                      <div style={{ fontSize: '12px', fontWeight: '600' }}>{post.authorName || 'Post'}</div>
                      <div style={{ fontSize: '12px', color: '#444' }}>{post.title || post.content?.slice(0, 50)}...</div>
                    </div>
                  ))}
                </div>
              )}

              {(!searchResults || (searchResults.people.length === 0 && searchResults.posts.length === 0)) && !isSearching && (
                <div style={{ fontSize: '12px', color: '#888', padding: '12px 0', textAlign: 'center' }}>
                  Press Enter to search for "{searchQuery}".
                </div>
              )}
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
              <small>Me⌄</small>
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
