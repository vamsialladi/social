import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MapPin, CheckCircle2, UserPlus, Eye, Check } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api';

export default function TalentCard({ talent }) {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [isConnected, setIsConnected] = useState(Boolean(talent.isConnected || talent.connected));
  const [isConnecting, setIsConnecting] = useState(false);

  useEffect(() => {
    const targetId = String(talent.userId || talent.id || '');
    function onConnectionChanged(event) {
      const { connected, profile } = event.detail || {};
      const changedId = String(profile?.userId || profile?.id || '');
      if (changedId && changedId === targetId) {
        setIsConnected(Boolean(connected));
      }
    }
    window.addEventListener('truehunt-connection-changed', onConnectionChanged);
    return () => window.removeEventListener('truehunt-connection-changed', onConnectionChanged);
  }, [talent.userId, talent.id]);

  const openSocialProfile = () => {
    const destination = isAuthenticated ? '/social/profile' : '/sign-in';
    navigate(destination, { state: { connectTalent: talent } });
  };

  const handleConnect = async (e) => {
    e.stopPropagation();
    if (!isAuthenticated) {
      navigate('/sign-in');
      return;
    }
    const targetId = talent.userId || talent.id;
    if (!targetId || isConnecting) return;

    setIsConnecting(true);
    try {
      const res = await api.toggleConnection(targetId);
      const nextConnected = typeof res?.connected === 'boolean' ? res.connected : !isConnected;
      setIsConnected(nextConnected);
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: {
          connected: nextConnected,
          profile: { ...talent, id: targetId, userId: targetId }
        }
      }));
    } catch (err) {
      console.error('Failed to toggle connection on talent card:', err);
    } finally {
      setIsConnecting(false);
    }
  };

  return (
      <div 
        className="card card-hoverable"
        style={{
          display: 'flex',
          flexDirection: 'column',
          height: '100%',
          overflow: 'hidden',
          backgroundColor: 'var(--color-white)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--color-border)',
          position: 'relative'
        }}
      >
        {/* Cover / Profile Header */}
        <div 
          style={{
            height: '84px',
            backgroundImage: `url(${talent.coverImage || 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=600&q=80'})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            position: 'relative'
          }}
        >
          <div 
            style={{
              position: 'absolute',
              inset: 0,
              backgroundColor: 'rgba(10, 25, 47, 0.45)'
            }}
          />
          {talent.availability && (
            <span 
              style={{
                position: 'absolute',
                top: '10px',
                right: '10px',
                fontSize: '11px',
                fontWeight: 600,
                backgroundColor: 'rgba(255, 255, 255, 0.92)',
                color: 'var(--color-navy)',
                padding: '3px 8px',
                borderRadius: 'var(--radius-full)',
                boxShadow: 'var(--shadow-subtle)'
              }}
            >
              {talent.availability}
            </span>
          )}
        </div>

        {/* Profile Avatar & Details Body */}
        <div style={{ padding: '0 20px 20px 20px', flex: 1, display: 'flex', flexDirection: 'column' }}>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: '-36px', marginBottom: '12px' }}>
            <div style={{ position: 'relative' }}>
              <img 
                src={talent.avatar} 
                alt={talent.name}
                style={{
                  width: '72px',
                  height: '72px',
                  borderRadius: '50%',
                  objectFit: 'cover',
                  border: '3px solid var(--color-white)',
                  boxShadow: 'var(--shadow-card)',
                  backgroundColor: '#E2E8F0'
                }}
              />
              {talent.verified && (
                <div 
                  style={{
                    position: 'absolute',
                    bottom: '2px',
                    right: '2px',
                    backgroundColor: 'var(--color-white)',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                  title="Verified Film Professional"
                >
                  <CheckCircle2 size={18} fill="#2563EB" color="#FFFFFF" />
                </div>
              )}
            </div>

            <span className="badge badge-navy" style={{ fontSize: '11px', fontWeight: 600 }}>
              {talent.experience} Exp
            </span>
          </div>

          {/* Name & Primary Role */}
          <div style={{ marginBottom: '10px' }}>
            <Link 
              to={`/talents/${talent.id}`} 
              style={{ textDecoration: 'none' }}
            >
              <h3 
                style={{
                  fontSize: '18px',
                  fontWeight: 700,
                  color: 'var(--color-navy)',
                  marginBottom: '2px',
                  transition: 'color var(--transition-fast)'
                }}
                className="talent-name-link"
              >
                {talent.name}
              </h3>
            </Link>
            <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--color-maroon)' }}>
              {talent.role}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '4px' }}>
              <MapPin size={13} />
              <span>{talent.location}</span>
            </div>
          </div>

          {/* Short Bio / Intro */}
          <p 
            style={{
              fontSize: '13px',
              color: 'var(--color-text-secondary)',
              lineHeight: '1.5',
              marginBottom: '14px',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden'
            }}
          >
            {talent.bio}
          </p>

          {/* Skills / Specializations Tags */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '18px', flex: 1, alignContent: 'flex-start' }}>
            {talent.skills?.slice(0, 3).map((skill, i) => (
              <span key={i} className="badge" style={{ fontSize: '11px', backgroundColor: '#F1F5F9', color: '#334155' }}>
                {skill}
              </span>
            ))}
            {talent.skills?.length > 3 && (
              <span className="badge" style={{ fontSize: '11px', color: '#64748B' }}>
                +{talent.skills.length - 3}
              </span>
            )}
          </div>

          {/* Card CTA Buttons */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', paddingTop: '12px', borderTop: '1px solid var(--color-border)' }}>
            <button
              type="button"
              onClick={openSocialProfile}
              className="btn btn-sm btn-outline-subtle"
              style={{ width: '100%' }}
            >
              <Eye size={14} />
              <span>View Profile</span>
            </button>

            <button
              type="button"
              onClick={handleConnect}
              disabled={isConnecting}
              className={`btn btn-sm ${isConnected ? 'btn-success' : 'btn-maroon'}`}
              style={{
                width: '100%',
                ...(isConnected ? { backgroundColor: '#10B981', color: '#FFFFFF', borderColor: '#10B981' } : {})
              }}
            >
              {isConnected ? <Check size={14} /> : <UserPlus size={14} />}
              <span>{isConnecting ? 'Updating…' : (isConnected ? 'Connected' : 'Connect')}</span>
            </button>
          </div>

        </div>
      </div>
  );
}
