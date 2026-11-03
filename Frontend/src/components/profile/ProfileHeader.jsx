import { useState } from 'react';
import { 
  Camera, 
  MapPin, 
  Briefcase, 
  Building2, 
  CheckCircle2, 
  Pencil, 
  Share2, 
  UserPlus, 
  MessageSquare, 
  Globe, 
  Sparkles,
  Check
} from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

function ProfileAvatar({ src, alt, style }) {
  if (src) {
    return <img src={src} alt={alt} style={style} />;
  }

  return (
    <svg viewBox="0 0 100 100" aria-hidden="true" style={style}>
      <circle cx="50" cy="50" r="50" fill="#e5e7e8" />
      <circle cx="50" cy="35" r="21" fill="#adb3b6" />
      <path d="M10 100c2-24 18-42 40-42s38 18 40 42H10Z" fill="#adb3b6" />
    </svg>
  );
}

export default function ProfileHeader({ profile, isOwner, onEditProfile, onConnect, onMessage, connected }) {
  const { showNotification } = useNotification();
  const [copied, setCopied] = useState(false);

  // Compute profile completion score based on real fields
  const calculateCompletionScore = () => {
    let score = 0;
    if (profile?.name) score += 15;
    if (profile?.avatar || profile?.profileImageUrl) score += 15;
    if (profile?.headline) score += 15;
    if (profile?.bio) score += 15;
    if (profile?.experiences?.length > 0) score += 15;
    if (profile?.educations?.length > 0) score += 10;
    if (profile?.skills?.length >= 3) score += 15;
    return Math.min(score, 100);
  };

  const completionScore = calculateCompletionScore();

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      showNotification('Profile URL copied to clipboard!', 'info');
      setTimeout(() => setCopied(false), 2500);
    }
  };

  const defaultCover = 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1400&q=80';
  return (
    <div 
      className="card"
      style={{
        borderRadius: 'var(--radius-lg)',
        overflow: 'hidden',
        backgroundColor: 'var(--color-white)',
        border: '1px solid var(--color-border)',
        boxShadow: 'var(--shadow-card)',
        marginBottom: '20px',
        position: 'relative'
      }}
    >
      {/* 1. Cover / Banner */}
      <div 
        style={{
          height: '210px',
          backgroundImage: `url(${profile?.coverImage || defaultCover})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          position: 'relative'
        }}
      >
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(10, 25, 47, 0.1) 0%, rgba(10, 25, 47, 0.55) 100%)' }} />
        
        {/* Owner Edit Cover button */}
        {isOwner && (
          <button 
            onClick={onEditProfile}
            style={{
              position: 'absolute',
              top: '16px',
              right: '16px',
              backgroundColor: 'rgba(255, 255, 255, 0.92)',
              color: 'var(--color-navy)',
              padding: '7px 14px',
              borderRadius: 'var(--radius-full)',
              fontSize: '13px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
              backdropFilter: 'blur(4px)',
              transition: 'all var(--transition-fast)'
            }}
            title="Edit cover photo"
          >
            <Camera size={15} />
            <span className="hide-on-mobile">Edit cover</span>
          </button>
        )}
      </div>

      {/* 2. Header Content Area */}
      <div style={{ padding: '0 28px 24px 28px', position: 'relative' }}>
        
        {/* Avatar and Top Actions Row */}
        <div 
          style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'flex-end', 
            marginTop: '-72px', 
            marginBottom: '16px', 
            flexWrap: 'wrap', 
            gap: '16px' 
          }}
        >
          {/* Avatar with Verified Badge & Edit button */}
          <div style={{ position: 'relative' }}>
            <div 
              style={{
                width: '136px',
                height: '136px',
                borderRadius: '50%',
                border: '4px solid var(--color-white)',
                backgroundColor: '#E2E8F0',
                overflow: 'hidden',
                boxShadow: '0 4px 14px rgba(10, 25, 47, 0.14)',
                position: 'relative'
              }}
            >
              <ProfileAvatar
                src={profile?.avatar || profile?.profileImageUrl}
                alt={profile?.name || 'Profile'}
                style={{ display: 'block', width: '100%', height: '100%', objectFit: 'cover' }}
              />
            </div>

            {profile?.verified && (
              <div 
                style={{
                  position: 'absolute',
                  bottom: '8px',
                  right: '8px',
                  backgroundColor: 'var(--color-white)',
                  borderRadius: '50%',
                  display: 'flex',
                  boxShadow: '0 2px 5px rgba(0,0,0,0.2)'
                }}
                title="Verified Cinema Professional"
              >
                <CheckCircle2 size={24} fill="#2563EB" color="#FFFFFF" />
              </div>
            )}

            {isOwner && (
              <button
                onClick={onEditProfile}
                style={{
                  position: 'absolute',
                  bottom: '4px',
                  left: '4px',
                  backgroundColor: 'var(--color-white)',
                  color: 'var(--color-navy)',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 2px 6px rgba(0,0,0,0.18)',
                  border: '1px solid var(--color-border)'
                }}
                title="Update photo"
              >
                <Camera size={16} />
              </button>
            )}
          </div>

          {/* Action Buttons Right */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
            {isOwner ? (
              <>
                <button 
                  onClick={onEditProfile}
                  className="btn btn-maroon"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '8px',
                    borderRadius: 'var(--radius-full)',
                    padding: '8px 18px',
                    fontWeight: 600,
                    boxShadow: '0 2px 8px rgba(128, 24, 35, 0.2)'
                  }}
                >
                  <Pencil size={15} />
                  <span>Edit Profile</span>
                </button>
                <button 
                  onClick={handleShare}
                  className="btn btn-outline-subtle"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                    borderRadius: 'var(--radius-full)',
                    padding: '8px 16px'
                  }}
                >
                  {copied ? <Check size={16} color="var(--color-success)" /> : <Share2 size={16} />}
                  <span>{copied ? 'Copied' : 'Share'}</span>
                </button>
              </>
            ) : (
              <>
                <button 
                  onClick={onConnect}
                  disabled={connected}
                  className={`btn ${connected ? 'btn-outline-subtle' : 'btn-maroon'}`}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '8px',
                    borderRadius: 'var(--radius-full)',
                    padding: '8px 20px',
                    fontWeight: 600
                  }}
                >
                  {connected ? <Check size={16} color="var(--color-success)" /> : <UserPlus size={16} />}
                  <span>{connected ? 'Connection Sent' : 'Connect'}</span>
                </button>
                <button 
                  onClick={() => onMessage?.(profile)}
                  className="btn btn-outline-navy"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                    borderRadius: 'var(--radius-full)',
                    padding: '8px 16px'
                  }}
                >
                  <MessageSquare size={16} />
                  <span>Message</span>
                </button>
                <button 
                  onClick={handleShare}
                  className="btn btn-outline-subtle"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                    borderRadius: 'var(--radius-full)',
                    padding: '8px 14px'
                  }}
                >
                  <Share2 size={16} />
                </button>
              </>
            )}
          </div>
        </div>

        {/* Name, Headline, Company & Metadata */}
        <div style={{ maxWidth: '820px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap', marginBottom: '4px' }}>
            <h1 style={{ fontSize: '26px', color: 'var(--color-navy)', fontWeight: 700, margin: 0 }}>
              {profile?.name || 'Cinema Professional'}
            </h1>
            {profile?.category && (
              <span className="badge badge-navy" style={{ fontSize: '12px', padding: '3px 10px' }}>
                {profile.category}
              </span>
            )}
          </div>

          {/* Professional Headline */}
          <p style={{ fontSize: '16px', color: profile?.headline ? 'var(--color-text-primary)' : (isOwner ? 'var(--color-text-muted)' : 'var(--color-text-primary)'), lineHeight: 1.5, margin: '6px 0 10px 0', fontWeight: 500, fontStyle: (!profile?.headline && isOwner) ? 'italic' : 'normal' }}>
            {profile?.headline || (isOwner ? 'Add your headline' : `${profile?.role || 'Cinema Professional'} in Film & Entertainment Industry`)}
          </p>

          {/* Location, Company, Website details */}
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '14px', fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '14px' }}>
            {profile?.company ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', color: 'var(--color-navy)', fontWeight: 600 }}>
                <Building2 size={15} color="var(--color-maroon)" />
                {profile.company}
              </span>
            ) : isOwner ? (
              <span 
                onClick={onEditProfile} 
                style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', color: 'var(--color-text-muted)', cursor: 'pointer', fontStyle: 'italic' }}
                title="Click to add company"
              >
                <Building2 size={15} /> Add company
              </span>
            ) : null}

            {profile?.location ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                <MapPin size={14} />
                {profile.location}
              </span>
            ) : isOwner ? (
              <span 
                onClick={onEditProfile} 
                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--color-text-muted)', cursor: 'pointer', fontStyle: 'italic' }}
                title="Click to add location"
              >
                <MapPin size={14} /> Add location
              </span>
            ) : null}

            {profile?.experience && (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                <Briefcase size={14} />
                {profile.experience}
              </span>
            )}
            {profile?.websiteUrl ? (
              <a 
                href={profile.websiteUrl} 
                target="_blank" 
                rel="noreferrer"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--color-info)', fontWeight: 600 }}
              >
                <Globe size={14} /> Portfolio
              </a>
            ) : isOwner ? (
              <span 
                onClick={onEditProfile} 
                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--color-text-muted)', cursor: 'pointer', fontStyle: 'italic' }}
                title="Click to add portfolio"
              >
                <Globe size={14} /> Add portfolio URL
              </span>
            ) : null}
            {profile?.availability && (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: 'var(--color-success)', fontWeight: 600 }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--color-success)', display: 'inline-block' }} />
                {profile.availability}
              </span>
            )}
          </div>

          {/* Connections & Followers Count */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '13px', paddingTop: '4px', borderTop: '1px solid var(--color-border-subtle)' }}>
            <span style={{ fontWeight: 700, color: 'var(--color-navy)' }}>
              {profile?.connectionsCount || 0}{' '}
              <span style={{ fontWeight: 500, color: 'var(--color-text-muted)' }}>connections</span>
            </span>
            <span style={{ color: 'var(--color-border)' }}>•</span>
            <span style={{ fontWeight: 700, color: 'var(--color-navy)' }}>
              {profile?.followersCount || 0}{' '}
              <span style={{ fontWeight: 500, color: 'var(--color-text-muted)' }}>followers</span>
            </span>
          </div>

        </div>

        {/* 3. Profile Completion Meter (Shown for Owner) */}
        {isOwner && completionScore < 100 && (
          <div 
            style={{ 
              marginTop: '18px', 
              padding: '14px 18px', 
              backgroundColor: 'var(--color-maroon-light)', 
              borderRadius: 'var(--radius-md)', 
              border: '1px solid var(--color-maroon-border)' 
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Sparkles size={16} color="var(--color-maroon)" />
                <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--color-maroon)' }}>
                  Profile Strength: {completionScore}%
                </span>
              </div>
              <span style={{ fontSize: '12px', color: 'var(--color-maroon)', fontWeight: 600 }}>
                {completionScore >= 80 ? 'All-Star Professional' : 'Intermediate'}
              </span>
            </div>
            
            {/* Progress bar */}
            <div style={{ height: '6px', backgroundColor: 'rgba(128, 24, 35, 0.15)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
              <div 
                style={{ 
                  height: '100%', 
                  width: `${completionScore}%`, 
                  backgroundColor: 'var(--color-maroon)', 
                  borderRadius: 'var(--radius-full)',
                  transition: 'width 0.5s ease-out'
                }} 
              />
            </div>
            <p style={{ fontSize: '12px', color: 'var(--color-maroon-hover)', margin: '8px 0 0 0' }}>
              Add credits, experiences, and education to attract verified film producers and casting directors.
            </p>
          </div>
        )}

      </div>
    </div>
  );
}
