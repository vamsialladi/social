import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Smartphone, 
  MapPin, 
  Users, 
  Camera, 
  FileText, 
  Download, 
  CheckCircle2, 
  Shield, 
  Clock, 
  Sparkles,
  ArrowRight
} from 'lucide-react';
import { LCM_APP_FEATURES } from '../services/mockData';
import { useNotification } from '../context/NotificationContext';

export default function LcmAppPage() {
  const [betaEmail, setBetaEmail] = useState('');
  const [betaJoined, setBetaJoined] = useState(false);
  const { showNotification } = useNotification();

  const handleBetaSignup = (e) => {
    e.preventDefault();
    if (!betaEmail || !betaEmail.includes('@')) {
      showNotification('Please enter a valid email address', 'error');
      return;
    }
    setBetaJoined(true);
    showNotification('Thank you! You are on the priority beta invite list for LCM App.', 'success');
  };

  const featureIcons = {
    MapPin,
    Users,
    Camera,
    FileText
  };

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Hero Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '60px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '40px', alignItems: 'center' }}>
            
            <div style={{ maxWidth: '600px' }}>
              <div 
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '6px 14px',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: 'rgba(255, 255, 255, 0.1)',
                  color: '#93C5FD',
                  fontSize: '13px',
                  fontWeight: 600,
                  marginBottom: '18px'
                }}
              >
                <Smartphone size={15} />
                <span>TrueHunt Companion Ecosystem • LCM Suite</span>
              </div>

              <h1 style={{ fontSize: 'clamp(32px, 4vw, 46px)', color: 'white', fontWeight: 800, lineHeight: '1.2', marginBottom: '16px' }}>
                LCM App — Location, Crew & Movie Management
              </h1>

              <p style={{ fontSize: '16px', color: '#CBD5E1', lineHeight: '1.6', marginBottom: '28px' }}>
                The mission control system for cinema production. Digitize on-set call sheets, 
                streamline department logistics, track high-end equipment chains, and log Daily Production Reports (DPR) 
                directly from your smartphone or tablet.
              </p>

              {/* Beta invitation action */}
              {betaJoined ? (
                <div style={{ padding: '14px 20px', borderRadius: 'var(--radius-sm)', backgroundColor: 'rgba(21, 128, 61, 0.25)', border: '1px solid #16A34A', color: '#86EFAC', fontSize: '14px' }}>
                  ✓ You have secured priority access for the upcoming iOS & Android production launch!
                </div>
              ) : (
                <form onSubmit={handleBetaSignup} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                  <input 
                    type="email"
                    value={betaEmail}
                    onChange={(e) => setBetaEmail(e.target.value)}
                    placeholder="Enter production email for Beta access"
                    style={{
                      flex: '1 1 260px',
                      padding: '12px 16px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid #334155',
                      backgroundColor: '#0F233D',
                      color: 'white',
                      fontSize: '14px',
                      outline: 'none'
                    }}
                    required
                  />
                  <button type="submit" className="btn btn-maroon">
                    <span>Request Early Access</span>
                    <ArrowRight size={16} />
                  </button>
                </form>
              )}

              <span style={{ fontSize: '12px', color: '#94A3B8', display: 'block', marginTop: '12px' }}>
                * Official download links for App Store and Google Play will be made available upon stable platform release.
              </span>

            </div>

            {/* Mobile App Visual Mockup Preview */}
            <div style={{ display: 'flex', justifyContent: 'center' }}>
              <div 
                style={{
                  width: '280px',
                  height: '520px',
                  backgroundColor: '#07101E',
                  borderRadius: '36px',
                  border: '6px solid #1E293B',
                  boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
                  padding: '16px',
                  display: 'flex',
                  flexDirection: 'column',
                  position: 'relative',
                  overflow: 'hidden'
                }}
              >
                {/* Speaker notch */}
                <div style={{ width: '80px', height: '14px', backgroundColor: '#1E293B', borderRadius: 'var(--radius-full)', margin: '0 auto 16px auto' }} />

                {/* App Screen Content Mock */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                  <span style={{ fontSize: '14px', fontWeight: 800, color: 'white' }}>LCM Mobile</span>
                  <span style={{ fontSize: '10px', backgroundColor: 'var(--color-maroon)', color: 'white', padding: '2px 6px', borderRadius: 'var(--radius-xs)' }}>LIVE SET</span>
                </div>

                <div style={{ backgroundColor: '#132A4A', padding: '12px', borderRadius: 'var(--radius-sm)', marginBottom: '12px' }}>
                  <div style={{ fontSize: '11px', color: '#93C5FD' }}>TODAY'S CALL SHEET</div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: 'white' }}>Scene 42 - The Harbour Dock</div>
                  <div style={{ fontSize: '11px', color: '#CBD5E1' }}>Call: 06:00 AM • Camera Roll: 06:45 AM</div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }}>
                  <div style={{ backgroundColor: '#0D1E36', padding: '10px', borderRadius: 'var(--radius-xs)', fontSize: '11px', color: '#E2E8F0', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <MapPin size={13} color="#60A5FA" />
                    <span>Location: Mangalore Pier 4</span>
                  </div>
                  <div style={{ backgroundColor: '#0D1E36', padding: '10px', borderRadius: 'var(--radius-xs)', fontSize: '11px', color: '#E2E8F0', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Users size={13} color="#34D399" />
                    <span>Unit: 48 Crew Checked In (100%)</span>
                  </div>
                  <div style={{ backgroundColor: '#0D1E36', padding: '10px', borderRadius: 'var(--radius-xs)', fontSize: '11px', color: '#E2E8F0', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Camera size={13} color="#FBBF24" />
                    <span>A-Cam Alexa LF #4802 Assigned</span>
                  </div>
                </div>

                <div style={{ borderTop: '1px solid #1E293B', paddingTop: '10px', textAlign: 'center', fontSize: '11px', color: '#64748B' }}>
                  TrueHunt Engine v1.0
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* Feature Breakdown Grid */}
      <section className="section-padding">
        <div className="container">
          
          <div className="section-header">
            <span className="badge badge-maroon" style={{ marginBottom: '8px' }}>Production Architecture</span>
            <h2 className="section-title">Built for Modern Film Sets</h2>
            <p className="section-subtitle">
              Eliminate lost paper call sheets, delayed equipment logistics, and chaotic coordination between shooting units.
            </p>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              gap: '24px'
            }}
          >
            {LCM_APP_FEATURES.map((feat) => {
              const IconComp = featureIcons[feat.icon] || Smartphone;
              return (
                <div 
                  key={feat.id}
                  className="card"
                  style={{
                    padding: '28px',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--color-white)',
                    border: '1px solid var(--color-border)'
                  }}
                >
                  <div 
                    style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--color-maroon-light)',
                      color: 'var(--color-maroon)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      marginBottom: '16px'
                    }}
                  >
                    <IconComp size={24} />
                  </div>
                  <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px' }}>
                    {feat.title}
                  </h3>
                  <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6' }}>
                    {feat.description}
                  </p>
                </div>
              );
            })}
          </div>

        </div>
      </section>

      {/* Production Houses Integration Banner */}
      <div className="container">
        <div 
          style={{
            backgroundColor: 'var(--color-white)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-lg)',
            padding: '40px',
            textAlign: 'center',
            boxShadow: 'var(--shadow-card)'
          }}
        >
          <h3 style={{ fontSize: '24px', color: 'var(--color-navy)', marginBottom: '10px' }}>
            Are you a Line Producer or Production Coordinator?
          </h3>
          <p style={{ fontSize: '15px', color: 'var(--color-text-muted)', maxWidth: '620px', margin: '0 auto 24px auto' }}>
            Schedule an on-set pilot demonstration of LCM App for your upcoming theatrical feature or streaming project.
          </p>
          <Link to="/contact-us" className="btn btn-outline-navy">
            Contact Production Solutions Team
          </Link>
        </div>
      </div>

    </div>
  );
}
