import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, Briefcase, Award, CheckCircle2, UserPlus, Eye, MessageSquare } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';
import { api } from '../../services/api';

export default function TalentCard({ talent }) {
  const [connecting, setConnecting] = useState(false);
  const [connected, setConnected] = useState(false);
  const [showConnectModal, setShowConnectModal] = useState(false);
  const [connectMessage, setConnectMessage] = useState(`Hi ${talent.name}, I would love to connect and discuss potential cinema collaborations.`);
  const { showNotification } = useNotification();

  const handleConnectSubmit = async (e) => {
    e.preventDefault();
    setConnecting(true);
    try {
      await api.talents.connect(talent.id, connectMessage);
      setConnected(true);
      setShowConnectModal(false);
      showNotification(`Connection request sent to ${talent.name}!`, 'success');
    } catch (err) {
      showNotification('Failed to send connection request', 'error');
    } finally {
      setConnecting(false);
    }
  };

  return (
    <>
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
            <Link 
              to={`/talents/${talent.id}`} 
              className="btn btn-sm btn-outline-subtle"
              style={{ width: '100%' }}
            >
              <Eye size={14} />
              <span>View Profile</span>
            </Link>

            <button
              onClick={() => connected ? null : setShowConnectModal(true)}
              disabled={connected}
              className={`btn btn-sm ${connected ? 'btn-outline-subtle' : 'btn-maroon'}`}
              style={{ width: '100%' }}
            >
              <UserPlus size={14} />
              <span>{connected ? 'Connected' : 'Connect'}</span>
            </button>
          </div>

        </div>
      </div>

      {/* Connect Modal */}
      {showConnectModal && (
        <div className="modal-overlay" onClick={() => setShowConnectModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
              <img 
                src={talent.avatar} 
                alt={talent.name} 
                style={{ width: '48px', height: '48px', borderRadius: '50%', objectFit: 'cover' }} 
              />
              <div>
                <h3 style={{ fontSize: '18px', color: 'var(--color-navy)' }}>Connect with {talent.name}</h3>
                <p style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>{talent.role} • {talent.location}</p>
              </div>
            </div>

            <form onSubmit={handleConnectSubmit}>
              <div className="form-group">
                <label className="form-label">Invitation Message</label>
                <textarea 
                  className="form-control"
                  rows={4}
                  value={connectMessage}
                  onChange={(e) => setConnectMessage(e.target.value)}
                  placeholder="Introduce yourself or mention project details..."
                  required
                />
                <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                  A personalized note significantly improves response rates on TrueHunt.
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button 
                  type="button" 
                  onClick={() => setShowConnectModal(false)}
                  className="btn btn-outline-subtle"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={connecting}
                  className="btn btn-maroon"
                >
                  {connecting ? 'Sending...' : 'Send Connection Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
