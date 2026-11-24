import './ProfileBanner.css'

function ProfileBanner({ variant = 'card' }) {
  return (
    <div className={`profile-banner profile-banner--${variant}`} aria-hidden="true">
      <img
        className="profile-banner-image"
        src="/assets/truehunt-banner.jpg"
        alt=""
      />
    </div>
  )
}

export default ProfileBanner
