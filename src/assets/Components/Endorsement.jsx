import { useEffect, useState } from 'react'
import './Endorsement.css'
import { notificationApi } from '../../api/client'

function Notifications({ profileDetails, profilePreview, onViewProfile, onManageNotifications }) {
  const [notifications, setNotifications] = useState([])

  useEffect(() => {
    async function loadNotifications() {
      try {
        const res = await notificationApi.getNotifications()
        if (res && res.data) {
          setNotifications(res.data)
        }
      } catch (err) {
        console.warn('Could not load notifications:', err)
      }
    }
    loadNotifications()
  }, [])
  const name = profileDetails.name.trim() || 'Your name'
  const initials = name === 'Your name' ? '?' : name.charAt(0).toUpperCase()
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]
  const profileTags = [...languages, ...profileDetails.workTypes]

  return (
    <section className="notifications-page" aria-label="Notifications page">
      <aside className="notifications-sidebar" aria-label="Your profile summary">
        <section
          className="notifications-profile-card"
          role="button"
          tabIndex={0}
          aria-label={`View ${name}'s profile`}
          onClick={onViewProfile}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault()
              onViewProfile()
            }
          }}
        >
          <div className="notifications-profile-cover" />
          <div className="notifications-profile-content">
            <div className="notifications-avatar" aria-hidden="true">
              {profilePreview
                ? <img src={profilePreview} alt="" />
                : initials}
            </div>
            <h1>{name}</h1>
            {profileDetails.talents.length > 0 && (
              <p className="notifications-profile-headline">{profileDetails.talents.join(' · ')}</p>
            )}
            {profileDetails.address && <p>{profileDetails.address}</p>}
            {profileDetails.experience && (
              <p>{profileDetails.experience} years of experience</p>
            )}
            {profileTags.length > 0 && (
              <div className="notifications-profile-tags">
                {profileTags.map((item) => <span key={item}>{item}</span>)}
              </div>
            )}
          </div>
        </section>

        <button
          className="notifications-manage-button"
          type="button"
          onClick={onManageNotifications}
        >
          Manage notifications
          <span aria-hidden="true">›</span>
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
      </aside>

      <section className="notifications-content" aria-labelledby="notifications-heading">
        <h2 id="notifications-heading">Notifications &amp; Endorsements</h2>
        {notifications.length > 0 ? (
          <div className="notifications-list" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '1rem' }}>
            {notifications.map((n) => (
              <div key={n.id} style={{ padding: '0.85rem 1rem', background: '#fff', borderRadius: '8px', border: '1px solid #e0e0e0' }}>
                <p style={{ margin: 0, fontWeight: 600 }}>{n.title || n.type}</p>
                <p style={{ margin: '0.25rem 0', color: '#555', fontSize: '0.9rem' }}>{n.message || n.content}</p>
                <small style={{ color: '#888' }}>{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</small>
              </div>
            ))}
          </div>
        ) : (
          <div className="notifications-empty-space" aria-label="No endorsements yet" />
        )}
      </section>
    </section>
  )
}

export default Notifications