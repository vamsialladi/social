import trueHuntBannerImage from '../truehunt-banner-new.png'
import './ProfileBanner.css'

function ProfileBanner({ variant = 'card' }) {
  return (
    <div
      className={`profile-banner profile-banner--${variant}`}
      aria-hidden="true"
    >
      <img
        src={trueHuntBannerImage}
        alt="TrueHunt Banner"
        className="profile-banner-image"
      />
      <span className="profile-banner-brand sr-only">
        <span>TrueHunt</span>
      </span>
    </div>
  )
}

export default ProfileBanner
