import { useEffect, useState } from 'react'
import './Endorsement.css'
import api from '../../api'

function Notifications({ profileDetails, profilePreview, onViewProfile, onManageNotifications }) {
  const [endorsements, setEndorsements] = useState([])
  const name = profileDetails.name.trim() || 'Your name'
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]
  const profileTags = [...languages, ...profileDetails.workTypes]

  useEffect(() => {
    async function loadNotifications() {
      try {
        const data = await api.getNotifications()
        if (Array.isArray(data)) {
          setEndorsements(data)
        }
      } catch (err) {
        console.warn('Backend notifications load error:', err)
      }
    }
    loadNotifications()
  }, [])

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
                : (
                  <svg viewBox="0 0 100 100" focusable="false">
                    <circle cx="50" cy="50" r="50" fill="#e5e7e8" />
                    <circle cx="50" cy="35" r="21" fill="#adb3b6" />
                    <path d="M10 100c2-24 18-42 40-42s38 18 40 42H10Z" fill="#adb3b6" />
                  </svg>
                )}
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
            <button
              className="home-promotion-button"
              type="button"
              onClick={async () => {
                try {
                  const res = await api.clickPromotion("Endorsement Promo - Northstar Films")
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

      <section className="notifications-content" aria-labelledby="notifications-heading">
        <h2 id="notifications-heading">Endorsement</h2>
        {endorsements.length > 0 ? (
          <div className="notifications-list" style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '16px' }}>
            {endorsements.map((item) => (
              <div
                key={item.id}
                style={{
                  background: 'var(--color-surface, #fff)',
                  padding: '16px',
                  borderRadius: '12px',
                  border: '1px solid var(--color-border, #e0e0e0)',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <strong style={{ fontSize: '15px' }}>{item.title}</strong>
                  <small style={{ color: '#888' }}>{item.createdAt}</small>
                </div>
                <p style={{ margin: 0, color: 'var(--color-text-muted, #555)', fontSize: '14px', lineHeight: '1.4' }}>
                  {item.message}
                </p>
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