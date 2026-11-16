import './ProfileBanner.css'

function ProfileBanner({ variant = 'card' }) {
  return (
    <div className={`profile-banner profile-banner--${variant}`} aria-hidden="true">
      <video autoPlay muted loop playsInline preload="metadata">
        <source src="/assets/profile-banner.mp4" type="video/mp4" />
      </video>
      <span className="profile-banner-brand">
        <img src="/assets/profile-banner-emblem.png" alt="" />
        <span>TrueHunt</span>
      </span>
    </div>
  )
}

export default ProfileBanner
