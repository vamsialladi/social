import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  MapPin, 
  Briefcase, 
  CheckCircle2, 
  ArrowLeft, 
  Film, 
  Award, 
  Play, 
  UserPlus, 
  Share2, 
  Lock, 
  Calendar, 
  Video, 
  ExternalLink 
} from 'lucide-react';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';
import { FEATURED_TALENTS } from '../services/mockData';

export default function TalentDetailsPage() {
  const { id } = useParams();
  const [talent, setTalent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showConnectModal, setShowConnectModal] = useState(false);
  const [connectNote, setConnectNote] = useState('');
  const [connected, setConnected] = useState(false);
  const { showNotification } = useNotification();

  useEffect(() => {
    async function fetchTalent() {
      setLoading(true);
      try {
        const data = await api.talents.getById(id);
        setTalent(data || FEATURED_TALENTS[0]);
      } catch (e) {
        setTalent(FEATURED_TALENTS[0]);
      } finally {
        setLoading(false);
      }
    }
    fetchTalent();
  }, [id]);

  const handleConnect = async (e) => {
    e.preventDefault();
    try {
      await api.talents.connect(talent.id, connectNote);
      setConnected(true);
      setShowConnectModal(false);
      showNotification(`Invitation sent to ${talent.name}. Private messages unlock upon acceptance.`, 'success');
    } catch (e) {
      showNotification('Failed to send request', 'error');
    }
  };

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      showNotification('Profile URL copied to clipboard!', 'info');
    }
  };

  if (loading || !talent) {
    return (
      <div className="container" style={{ padding: '80px 20px', textAlign: 'center' }}>
        <p style={{ color: 'var(--color-text-muted)' }}>Loading film professional portfolio...</p>
      </div>
    );
  }

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Top Navigation Breadcrumb */}
      <div style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)', padding: '12px 0' }}>
        <div className="container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Link to="/talents" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>
            <ArrowLeft size={16} /> Back to Talents Directory
          </Link>
          <button onClick={handleShare} className="btn btn-sm btn-ghost" style={{ fontSize: '12px' }}>
            <Share2 size={14} /> Share Profile
          </button>
        </div>
      </div>

      <div className="container" style={{ marginTop: '24px' }}>
        
        {/* Profile Card Header Banner */}
        <div 
          className="card"
          style={{
            borderRadius: 'var(--radius-lg)',
            overflow: 'hidden',
            marginBottom: '28px',
            backgroundColor: 'var(--color-white)',
            border: '1px solid var(--color-border)'
          }}
        >
          {/* Cover */}
          <div 
            style={{
              height: '200px',
              backgroundImage: `url(${talent.coverImage || 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1200&q=80'})`,
              backgroundSize: 'cover',
              backgroundPosition: 'center',
              position: 'relative'
            }}
          >
            <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(10, 25, 47, 0.2) 0%, rgba(10, 25, 47, 0.6) 100%)' }} />
          </div>

          {/* Profile Header Details */}
          <div style={{ padding: '0 32px 32px 32px', position: 'relative' }}>
            
            <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: '-60px', marginBottom: '20px', gap: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: '20px', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative' }}>
                  <img 
                    src={talent.avatar} 
                    alt={talent.name}
                    style={{
                      width: '120px',
                      height: '120px',
                      borderRadius: '50%',
                      objectFit: 'cover',
                      border: '4px solid var(--color-white)',
                      boxShadow: 'var(--shadow-card)',
                      backgroundColor: '#E2E8F0'
                    }}
                  />
                  {talent.verified && (
                    <div 
                      style={{
                        position: 'absolute',
                        bottom: '6px',
                        right: '6px',
                        backgroundColor: 'var(--color-white)',
                        borderRadius: '50%',
                        display: 'flex'
                      }}
                      title="Verified Cinema Professional"
                    >
                      <CheckCircle2 size={24} fill="#2563EB" color="#FFFFFF" />
                    </div>
                  )}
                </div>

                <div style={{ paddingBottom: '4px' }}>
                  <h1 style={{ fontSize: '28px', color: 'var(--color-navy)', marginBottom: '4px' }}>
                    {talent.name}
                  </h1>
                  <div style={{ fontSize: '18px', fontWeight: 600, color: 'var(--color-maroon)' }}>
                    {talent.role}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '13px', color: 'var(--color-text-muted)', marginTop: '6px' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <MapPin size={14} /> {talent.location}
                    </span>
                    <span>•</span>
                    <span>{talent.experience} Experience</span>
                    <span>•</span>
                    <span style={{ color: 'var(--color-success)', fontWeight: 600 }}>{talent.availability || 'Available'}</span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', gap: '10px' }}>
                <button 
                  onClick={() => setShowConnectModal(true)}
                  disabled={connected}
                  className={`btn btn-lg ${connected ? 'btn-outline-subtle' : 'btn-maroon'}`}
                >
                  <UserPlus size={18} />
                  <span>{connected ? 'Request Sent' : 'Connect on TrueHunt'}</span>
                </button>
              </div>
            </div>

          </div>
        </div>

        {/* Profile Multi-Column Content */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '28px' }}>
          
          {/* Main Content Column (Left) */}
          <div style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '28px' }}>
            
            {/* About Section */}
            <div className="card" style={{ padding: '28px', borderRadius: 'var(--radius-md)' }}>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '14px' }}>
                About
              </h3>
              <p style={{ fontSize: '15px', color: 'var(--color-text-secondary)', lineHeight: '1.7' }}>
                {talent.bio}
              </p>
            </div>

            {/* Filmography & Project Credits */}
            <div className="card" style={{ padding: '28px', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)' }}>
                  Filmography & Theatrical Credits
                </h3>
                <span className="badge badge-navy">Verified Film Records</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {talent.credits?.map((credit, idx) => (
                  <div 
                    key={idx}
                    style={{
                      padding: '16px',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--color-bg-page)',
                      border: '1px solid var(--color-border)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                      <div 
                        style={{
                          width: '42px',
                          height: '42px',
                          borderRadius: 'var(--radius-xs)',
                          backgroundColor: 'var(--color-navy)',
                          color: 'white',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}
                      >
                        <Film size={20} />
                      </div>
                      <div>
                        <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', marginBottom: '2px' }}>
                          {credit.title}
                        </h4>
                        <div style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
                          Role: <span style={{ fontWeight: 600, color: 'var(--color-navy-surface)' }}>{credit.role}</span>
                        </div>
                      </div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <span className="badge badge-maroon" style={{ fontSize: '11px', display: 'block', marginBottom: '4px' }}>
                        {credit.format}
                      </span>
                      <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Year: {credit.year}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Showreel & Portfolio Preview */}
            <div className="card" style={{ padding: '28px', borderRadius: 'var(--radius-md)' }}>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '14px' }}>
                Showreel & Video Reel
              </h3>
              
              <div 
                style={{
                  height: '240px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: '#0F172A',
                  position: 'relative',
                  overflow: 'hidden',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  border: '1px solid var(--color-border)'
                }}
              >
                <img 
                  src={talent.coverImage} 
                  alt="Showreel Thumbnail" 
                  style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: 0.6 }}
                />
                <div style={{ position: 'relative', textAlign: 'center', zIndex: 1 }}>
                  <div 
                    style={{
                      width: '64px',
                      height: '64px',
                      borderRadius: '50%',
                      backgroundColor: 'var(--color-maroon)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      margin: '0 auto 12px auto',
                      boxShadow: '0 8px 24px rgba(0, 0, 0, 0.4)',
                      cursor: 'pointer'
                    }}
                    onClick={() => showNotification('Playing showreel in demo player', 'info')}
                  >
                    <Play size={28} fill="white" style={{ marginLeft: '4px' }} />
                  </div>
                  <div style={{ fontWeight: 700, fontSize: '16px' }}>{talent.name} — Official Cinema Reel</div>
                  <div style={{ fontSize: '12px', color: '#CBD5E1' }}>4K Anamorphic Sequence Reel (03:45)</div>
                </div>
              </div>
            </div>

          </div>

          {/* Sidebar Column (Right) */}
          <aside style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            
            {/* Skills & Specializations */}
            <div className="card" style={{ padding: '24px', borderRadius: 'var(--radius-md)' }}>
              <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', marginBottom: '14px' }}>
                Skills & Disciplines
              </h4>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {talent.skills?.map((skill, idx) => (
                  <span key={idx} className="badge" style={{ padding: '6px 12px', fontSize: '12px', backgroundColor: '#F1F5F9', color: '#1E293B', fontWeight: 500 }}>
                    {skill}
                  </span>
                ))}
              </div>
            </div>

            {/* Privacy & Contact Preference Notice */}
            <div 
              className="card" 
              style={{ 
                padding: '24px', 
                borderRadius: 'var(--radius-md)', 
                backgroundColor: '#FAF5FF', 
                border: '1px solid #E9D5FF' 
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#6B21A8', fontWeight: 700, marginBottom: '8px' }}>
                <Lock size={16} />
                <span>Protected Contact Channel</span>
              </div>
              <p style={{ fontSize: '13px', color: '#4B5563', lineHeight: '1.5', margin: 0 }}>
                In accordance with TrueHunt privacy protection, personal telephone numbers and home studio locations 
                are concealed from public crawling. Contact is initiated securely via connection invitations.
              </p>
            </div>

            {/* Direct Connect Action Card */}
            <div className="card" style={{ padding: '24px', borderRadius: 'var(--radius-md)', textAlign: 'center' }}>
              <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', marginBottom: '6px' }}>
                Casting or Hiring {talent.name}?
              </h4>
              <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '18px' }}>
                Send a direct project note specifying your shoot schedule, dates, and production company.
              </p>
              <button 
                onClick={() => setShowConnectModal(true)} 
                className="btn btn-maroon" 
                style={{ width: '100%' }}
              >
                Send Message / Project Pitch
              </button>
            </div>

          </aside>

        </div>

      </div>

      {/* Connect Modal */}
      {showConnectModal && (
        <div className="modal-overlay" onClick={() => setShowConnectModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '8px' }}>
              Connect with {talent.name}
            </h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '16px' }}>
              Project proposals receive highest priority. Include role requirements or shooting dates.
            </p>

            <form onSubmit={handleConnect}>
              <div className="form-group">
                <label className="form-label">Message / Collaboration Proposal</label>
                <textarea 
                  className="form-control"
                  rows={4}
                  value={connectNote}
                  onChange={(e) => setConnectNote(e.target.value)}
                  placeholder={`Hello ${talent.name}, I would love to discuss an upcoming feature project with you...`}
                  required
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button type="button" onClick={() => setShowConnectModal(false)} className="btn btn-outline-subtle">
                  Cancel
                </button>
                <button type="submit" className="btn btn-maroon">
                  Submit Invitation
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
