import './Endorsement.css'

function Notifications({ profileDetails, profilePreview, onViewProfile, onManageNotifications }) {
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
        <h2 id="notifications-heading">Endorsement</h2>
        <div className="notifications-empty-space" aria-label="No endorsements yet" />
      </section>
    </section>
  )
}

export default Notifications