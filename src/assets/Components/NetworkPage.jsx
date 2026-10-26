import { useEffect, useState } from 'react'
import api from '../../api'
import {
  CalendarDays,
  MessageCircle,
  UserRound,
  UsersRound,
} from 'lucide-react'
import './NetworkPage.css'

const networkSections = [
  { label: 'Connections', count: 0, Icon: UsersRound },
  { label: 'Following & followers', Icon: UserRound },
  { label: 'Groups', count: 0, Icon: UsersRound },
  { label: 'Events', count: 0, Icon: CalendarDays },
]

const suggestedProfiles = [
  {
    id: 'maya',
    name: 'Maya Krishnan',
    initials: 'MK',
    status: 'active',
    role: 'Actor · Voice artist',
    location: 'Hyderabad, Telangana',
    mutualConnections: '12 mutual connections',
    avatarClass: 'network-avatar-coral',
  },
  {
    id: 'rohan',
    name: 'Rohan Mehta',
    initials: 'RM',
    status: 'offline',
    role: 'Cinematographer · Editor',
    location: 'Mumbai, Maharashtra',
    mutualConnections: '8 mutual connections',
    avatarClass: 'network-avatar-blue',
  },
]

function NetworkPage({ onMessage }) {
  const [activeTab, setActiveTab] = useState('grow')
  const [activeSection, setActiveSection] = useState('grow')
  const [profiles, setProfiles] = useState(suggestedProfiles)
  const [connectedProfiles, setConnectedProfiles] = useState([])
  const [followedProfiles, setFollowedProfiles] = useState([])

  useEffect(() => {
    async function loadNetworkData() {
      try {
        const data = await api.getNetworkSuggestions()
        if (Array.isArray(data) && data.length > 0) {
          setProfiles(data)
          const connected = data.filter((p) => p.isConnected).map((p) => p.id)
          const followed = data.filter((p) => p.isFollowing).map((p) => p.id)
          setConnectedProfiles(connected)
          setFollowedProfiles(followed)
        }
      } catch (err) {
        console.warn('Network suggestions load error:', err)
      }
    }
    loadNetworkData()
  }, [])

  async function toggleConnection(profileId) {
    setConnectedProfiles((current) => (
      current.includes(profileId)
        ? current.filter((id) => id !== profileId)
        : [...current, profileId]
    ))
    try {
      await api.toggleConnection(profileId)
    } catch (err) {
      console.warn('Backend toggle connection error:', err)
    }
  }

  async function toggleFollow(profileId) {
    setFollowedProfiles((current) => (
      current.includes(profileId)
        ? current.filter((id) => id !== profileId)
        : [...current, profileId]
    ))
    try {
      await api.toggleFollow(profileId)
    } catch (err) {
      console.warn('Backend toggle follow error:', err)
    }
  }

  function renderProfileCard(profile) {
    const isConnected = connectedProfiles.includes(profile.id)
    const isFollowing = followedProfiles.includes(profile.id)

    return (
      <article className="network-profile-card" key={profile.id}>
        <div className={`network-profile-cover ${profile.avatarClass}`} />
        <div className="network-profile-avatar-wrap">
          <div className={`network-profile-avatar ${profile.avatarClass}`} aria-hidden="true">
            {profile.initials}
          </div>
          <span
            className={`network-presence-dot is-${profile.status}`}
            role="img"
            aria-label={`${profile.name} is ${profile.status}`}
          />
        </div>
        <div className="network-profile-content">
          <h3>{profile.name}</h3>
          <p className="network-profile-role">{profile.role}</p>
          <p className="network-profile-location">{profile.location}</p>
          <p className="network-mutual-connections">
            <UsersRound size={14} aria-hidden="true" />
            {profile.mutualConnections}
          </p>
          <div className="network-profile-actions">
            <button
              className={`network-connect-button${isConnected ? ' is-connected' : ''}`}
              type="button"
              aria-pressed={isConnected}
              onClick={() => toggleConnection(profile.id)}
            >
              {isConnected ? 'Connected' : 'Connect'}
            </button>
            <button
              className={`network-follow-button${isFollowing ? ' is-following' : ''}`}
              type="button"
              aria-pressed={isFollowing}
              onClick={() => toggleFollow(profile.id)}
            >
              {isFollowing ? 'Following' : 'Follow'}
            </button>
          </div>
          <button
            className="network-message-button"
            type="button"
            onClick={() => onMessage && onMessage(profile)}
          >
            <MessageCircle size={15} aria-hidden="true" />
            Message
          </button>
        </div>
      </article>
    )
  }

  return (
    <main className="network-page" id="network" aria-label="My network">
      <div className="network-layout">
        <aside className="network-sidebar">
          <section className="network-manage-card" aria-labelledby="network-manage-title">
            <h1 id="network-manage-title">Manage my network</h1>
            <nav aria-label="Network sections">
              {networkSections.map(({ label, count, Icon }) => (
                <button
                  className={`network-manage-item${activeSection === 'connections' && label === 'Connections' ? ' is-selected' : ''}`}
                  key={label}
                  type="button"
                  aria-pressed={activeSection === 'connections' && label === 'Connections'}
                  onClick={() => setActiveSection(label === 'Connections' ? 'connections' : 'grow')}
                >
                  <Icon size={19} strokeWidth={2.2} aria-hidden="true" />
                  <span>{label}</span>
                  <small>
                    {label === 'Connections'
                      ? connectedProfiles.length
                      : label === 'Following & followers'
                        ? followedProfiles.length
                        : count ?? 0}
                  </small>
                </button>
              ))}
            </nav>
          </section>
          <section className="network-tip-card">
            <span className="network-tip-eyebrow">YOUR CREATIVE CIRCLE</span>
            <h2>Good connections lead to great collaborations.</h2>
            <p>Meet more people working across film and entertainment.</p>
          </section>
          <section className="home-card home-promotion-card" aria-label="Sponsored movie industry promotion">
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
                    const res = await api.clickPromotion("Network Promo - Northstar Films")
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

        <div className="network-main">
          {activeSection === 'connections' ? (
            <section className="network-suggestions" aria-labelledby="network-connections-title">
              <header className="network-section-heading">
                <div>
                  <span className="network-section-eyebrow">YOUR NETWORK</span>
                  <h2 id="network-connections-title">Connections</h2>
                  <p>People you’ve connected with.</p>
                </div>
                <span className="network-suggestion-count">{connectedProfiles.length} connections</span>
              </header>
              {connectedProfiles.length > 0 ? (
                <div className="network-profile-grid">
                  {profiles
                    .filter((profile) => connectedProfiles.includes(profile.id))
                    .map(renderProfileCard)}
                </div>
              ) : (
                <div className="network-connections-empty">
                  <div className="network-catch-up-icon"><UsersRound size={24} aria-hidden="true" /></div>
                  <h3>No connections yet</h3>
                  <p>Explore suggested profiles and connect with people in your community.</p>
                  <button type="button" onClick={() => setActiveSection('grow')}>Find people</button>
                </div>
              )}
            </section>
          ) : (
            <>
          <div className="network-tabs" role="tablist" aria-label="Network suggestions">
            <button
              className={`network-tab${activeTab === 'grow' ? ' is-selected' : ''}`}
              type="button"
              role="tab"
              aria-selected={activeTab === 'grow'}
              onClick={() => setActiveTab('grow')}
            >
              Grow
            </button>
            <button
              className={`network-tab${activeTab === 'catch-up' ? ' is-selected' : ''}`}
              type="button"
              role="tab"
              aria-selected={activeTab === 'catch-up'}
              onClick={() => setActiveTab('catch-up')}
            >
              Catch up
            </button>
          </div>

          {activeTab === 'grow' ? (
            <>
              <section className="network-invitations">
                <div>
                  <strong>No pending invitations</strong>
                  <p>Invitations you receive will appear here.</p>
                </div>
                <span aria-hidden="true">✓</span>
              </section>

              <section className="network-suggestions" aria-labelledby="network-suggestions-title">
                <header className="network-section-heading">
                  <div>
                    <span className="network-section-eyebrow">PEOPLE TO MEET</span>
                    <h2 id="network-suggestions-title">People you may know</h2>
                    <p>Connect with artists and professionals in your community.</p>
                  </div>
                  <span className="network-suggestion-count">{profiles.length} suggestions</span>
                </header>
                <div className="network-profile-grid">
                  {profiles.map(renderProfileCard)}
                </div>
              </section>
            </>
          ) : (
            <section className="network-catch-up">
              <div className="network-catch-up-icon"><UsersRound size={24} aria-hidden="true" /></div>
              <h2>You’re all caught up</h2>
              <p>New updates from your network will show up here.</p>
              <button type="button" onClick={() => setActiveTab('grow')}>Explore people</button>
            </section>
          )}
            </>
          )}
        </div>
      </div>
    </main>
  )
}

export default NetworkPage